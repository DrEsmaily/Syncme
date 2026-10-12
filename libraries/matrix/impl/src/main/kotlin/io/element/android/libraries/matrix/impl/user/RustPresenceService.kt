package io.element.android.libraries.matrix.impl.user

import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.sync.SyncState
import io.element.android.libraries.matrix.api.user.UserPresence
import io.element.android.libraries.sessionstorage.api.SessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** One cancellable presence-only long poll per session, using normal platform TLS validation. */
internal class RustPresenceService(
    homeserver: String,
    private val sessionId: UserId,
    private val sessionStore: SessionStore,
    syncState: StateFlow<SyncState>,
    visibility: StateFlow<Boolean?>,
    private val scope: CoroutineScope,
) {
    private val base = homeserver.trimEnd('/').toHttpUrl()
    private val http = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS).readTimeout(40, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build()
    private val registrations = mutableMapOf<UserId, Int>()
    private val snapshots = Channel<Pair<UserId, Long>>(Channel.UNLIMITED)
    private var generation = 0L
    private var running = false
    private val cooldownUntilNanos = AtomicLong(0)
    private val mutableStates = MutableStateFlow<Map<UserId, UserPresence>>(emptyMap())
    val states: StateFlow<Map<UserId, UserPresence>> = mutableStates

    init {
        // Bound HTTP concurrency, rather than creating one coroutine/connection for every group member.
        repeat(4) {
            scope.launch {
                for ((user, epoch) in snapshots) {
                    if (!isCurrent(user, epoch)) continue
                    try {
                        val content = request("presence", user.value, "status") ?: continue
                        synchronized(registrations) {
                            if (isCurrent(user, epoch) && user !in mutableStates.value) {
                                mutableStates.value += user to presenceFromJson(content, System.currentTimeMillis())
                            }
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        // Missing/forbidden presence is Unknown, never Offline.
                    }
                }
            }
        }
        scope.launch {
            val stableVisibility = channelFlow {
                visibility.collectLatest { foreground ->
                    // Rotation and short Activity switches must not briefly publish Away.
                    if (foreground == false) delay(1_000)
                    send(foreground)
                }
            }
            combine(syncState, stableVisibility) { sync, foreground -> (sync == SyncState.Running && foreground != null) to foreground }
                .collectLatest { (enabled, foreground) ->
                    synchronized(registrations) {
                        generation++
                        running = enabled
                        mutableStates.value = emptyMap()
                        if (enabled) registrations.keys.forEach { snapshots.trySend(it to generation) }
                    }
                    if (enabled) {
                        try {
                            observe(if (foreground == true) "online" else "unavailable")
                        } finally {
                            synchronized(registrations) {
                                generation++
                                running = false
                                mutableStates.value = emptyMap()
                            }
                        }
                    }
                }
        }
    }

    fun track(users: Set<UserId>) = synchronized(registrations) {
        users.forEach { user ->
            val count = registrations[user] ?: 0
            registrations[user] = count + 1
            if (count == 0 && running) snapshots.trySend(user to generation)
        }
    }

    fun release(users: Set<UserId>) = synchronized(registrations) {
        users.forEach { user ->
            val count = registrations[user] ?: return@forEach
            if (count > 1) {
                registrations[user] = count - 1
            } else {
                registrations.remove(user)
                mutableStates.value -= user
            }
        }
    }

    private fun isCurrent(user: UserId, epoch: Long): Boolean = synchronized(registrations) {
        running && epoch == generation && user in registrations
    }

    private suspend fun observe(ownPresence: String) {
        var since: String? = null
        var failures = 0
        while (kotlin.coroutines.coroutineContext.isActive) {
            try {
                val payload = request("sync", query = mapOf(
                    "timeout" to "30000",
                    "set_presence" to ownPresence,
                    "filter" to """{"room":{"rooms":[],"timeline":{"limit":0}},"presence":{"types":["m.presence"]}}""",
                ) + (since?.let { mapOf("since" to it) } ?: emptyMap())) ?: return
                since = payload.getString("next_batch")
                failures = 0
                val events = payload.optJSONObject("presence")?.optJSONArray("events")
                synchronized(registrations) {
                    val updates = mutableMapOf<UserId, UserPresence>()
                    for (index in 0 until (events?.length() ?: 0)) {
                        val event = events?.optJSONObject(index) ?: continue
                        if (event.optString("type") != "m.presence") continue
                        val sender = event.optString("sender").takeIf { it.startsWith('@') && ':' in it } ?: continue
                        val user = UserId(sender)
                        if (user !in registrations) continue
                        updates[user] = presenceFromJson(event.optJSONObject("content") ?: continue, System.currentTimeMillis())
                    }
                    mutableStates.value += updates
                }
                // Protect against servers returning empty syncs immediately, avoiding a hot loop.
                delay(500)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                synchronized(registrations) {
                    generation++
                    mutableStates.value = emptyMap()
                }
                since = null // A reconnect must refresh presence instead of keeping an incomplete incremental cache.
                failures = (failures + 1).coerceAtMost(6)
                delay(if (error is RateLimited) error.retryMs else (1_000L shl failures).coerceAtMost(60_000))
            }
        }
    }

    private suspend fun request(vararg path: String, query: Map<String, String> = emptyMap()): JSONObject? {
        delay(((cooldownUntilNanos.get() - System.nanoTime()) / 1_000_000).coerceAtLeast(0))
        val session = sessionStore.getSession(sessionId.value) ?: return null
        val url = base.newBuilder().addPathSegments("_matrix/client/v3").apply {
            path.forEach(::addPathSegment)
            query.forEach { (key, value) -> addQueryParameter(key, value) }
        }.build()
        return suspendCancellableCoroutine { continuation ->
            val call = http.newCall(Request.Builder().url(url).header("Authorization", "Bearer ${session.accessToken}").build())
            if (path.firstOrNull() == "presence") call.timeout().timeout(5, TimeUnit.SECONDS)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val content = response.use {
                            val body = it.body.string()
                            if (it.code == 429) {
                                throw RateLimited(
                                runCatching { JSONObject(body).optLong("retry_after_ms", 15_000) }.getOrDefault(15_000).coerceIn(2_000, 120_000),
                            )
                            }
                            if (!it.isSuccessful) throw IOException("Presence HTTP ${it.code}")
                            JSONObject(body)
                        }
                        if (continuation.isActive) continuation.resume(content)
                    } catch (error: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
                }
            })
        }
    }

    private class RateLimited(val retryMs: Long) : IOException()
}

internal fun presenceFromJson(content: JSONObject, nowMillis: Long): UserPresence = when (content.optString("presence")) {
    "online" -> UserPresence.Online
    "unavailable" -> UserPresence.Away
    "offline" -> {
        val elapsed = content.optLong("last_active_ago", -1L)
        UserPresence.Offline(if (elapsed in 0..nowMillis) nowMillis - elapsed else null)
    }
    else -> UserPresence.Unknown
}
