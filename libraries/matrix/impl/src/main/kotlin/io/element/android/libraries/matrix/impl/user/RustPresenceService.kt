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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
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

/** Bounded presence GETs only: never acknowledges to-device messages owned by SDK sync. */
internal class RustPresenceService(
    homeserver: String,
    private val sessionId: UserId,
    private val sessionStore: SessionStore,
    syncState: StateFlow<SyncState>,
    visibility: StateFlow<Boolean?>,
    private val scope: CoroutineScope,
    private val publishOwnPresence: suspend (String) -> Unit,
    private val refreshIntervalMillis: (Int) -> Long = { if (it <= 8) 15_000 else 60_000 },
    private val publicationRetryMillis: Long = 2_000,
) {
    private val base = homeserver.trimEnd('/').toHttpUrl()
    private val http = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS).readTimeout(40, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build()
    private val registrations = mutableMapOf<UserId, Int>()
    private val snapshots = Channel<Pair<UserId, Long>>(Channel.UNLIMITED)
    private val pending = mutableSetOf<Pair<UserId, Long>>()
    private val observedAtNanos = mutableMapOf<UserId, Long>()
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
                    try {
                        if (!isCurrent(user, epoch)) continue
                        val content = request(user, epoch) ?: continue
                        synchronized(registrations) {
                            if (isCurrent(user, epoch)) {
                                mutableStates.value += user to presenceFromJson(content, System.currentTimeMillis())
                                observedAtNanos[user] = System.nanoTime()
                            }
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        if (error is RateLimited) {
                            val until = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(error.retryMs)
                            cooldownUntilNanos.updateAndGet { previous -> maxOf(previous, until) }
                        }
                        synchronized(registrations) {
                            if (isCurrent(user, epoch)) mutableStates.value -= user
                        }
                    } finally {
                        synchronized(registrations) { pending.remove(user to epoch) }
                    }
                }
            }
        }
        val stableVisibility = channelFlow {
            visibility.collectLatest { foreground ->
                if (foreground == false) delay(1_000)
                send(foreground)
            }
        }.stateIn(scope, SharingStarted.Eagerly, null)
        scope.launch {
            combine(stableVisibility, syncState) { foreground, sync -> foreground to sync }
                .distinctUntilChanged().collectLatest { (foreground, sync) ->
                if (foreground == null || (foreground && sync != SyncState.Running)) return@collectLatest
                var retryMillis = publicationRetryMillis
                while (kotlin.coroutines.coroutineContext.isActive) {
                    try {
                        // The SDK owns publication, including presence in its future sync requests.
                        publishOwnPresence(if (foreground) "online" else "unavailable")
                        break
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        // Retry transient failures even if visibility and sync remain unchanged.
                        // collectLatest cancels this retry when the desired state changes.
                        delay(retryMillis)
                        retryMillis = (retryMillis * 2).coerceAtMost(30_000)
                    }
                }
            }
        }
        scope.launch {
            combine(syncState, stableVisibility) { sync, foreground -> sync == SyncState.Running && foreground == true }
                .distinctUntilChanged().collectLatest { enabled ->
                    synchronized(registrations) {
                        generation++
                        running = enabled
                        mutableStates.value = emptyMap()
                        observedAtNanos.clear()
                    }
                    if (enabled) {
                        try {
                            while (kotlin.coroutines.coroutineContext.isActive) {
                                val count = synchronized(registrations) {
                                    val oldest = System.nanoTime() - TimeUnit.SECONDS.toNanos(90)
                                    mutableStates.value = mutableStates.value.filterKeys { (observedAtNanos[it] ?: 0) >= oldest }
                                    registrations.keys.forEach(::enqueue)
                                    registrations.size
                                }
                                delay(refreshIntervalMillis(count))
                            }
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

    // Called only while holding registrations' monitor. Do not accumulate refreshes for slow users.
    private fun enqueue(user: UserId) {
        val entry = user to generation
        if (pending.add(entry)) snapshots.trySend(entry)
    }

    fun track(users: Set<UserId>) = synchronized(registrations) {
        users.forEach { user ->
            val count = registrations[user] ?: 0
            registrations[user] = count + 1
            if (count == 0 && running) enqueue(user)
        }
    }

    fun release(users: Set<UserId>) = synchronized(registrations) {
        users.forEach { user ->
            val count = registrations[user] ?: return@forEach
            if (count > 1) {
                registrations[user] = count - 1
            } else {
                registrations.remove(user)
                observedAtNanos.remove(user)
                mutableStates.value -= user
            }
        }
    }

    private fun isCurrent(user: UserId, epoch: Long): Boolean = synchronized(registrations) {
        running && epoch == generation && user in registrations
    }

    private suspend fun request(user: UserId, epoch: Long): JSONObject? {
        delay(((cooldownUntilNanos.get() - System.nanoTime()) / 1_000_000).coerceAtLeast(0))
        if (!isCurrent(user, epoch)) return null
        val session = sessionStore.getSession(sessionId.value) ?: return null
        if (!isCurrent(user, epoch)) return null
        val url = base.newBuilder().addPathSegments("_matrix/client/v3/presence")
            .addPathSegment(user.value).addPathSegment("status").build()
        return suspendCancellableCoroutine { continuation ->
            val call = http.newCall(Request.Builder().url(url).header("Authorization", "Bearer ${session.accessToken}").build())
            call.timeout().timeout(5, TimeUnit.SECONDS)
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
