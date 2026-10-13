package io.element.android.libraries.matrix.api.diagnostics

import android.content.Context
import io.element.android.libraries.matrix.api.timeline.item.event.EventTimelineItem
import io.element.android.libraries.matrix.api.timeline.item.event.LocalEventSendState
import io.element.android.libraries.matrix.api.timeline.item.event.UnableToDecryptContent
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** Opt-in structured metadata only. Never accepts message bodies, exception text or key material. */
object DecryptionDiagnostics {
    enum class Stage {
        ENABLED,
        DISABLED,
        FOREGROUND,
        BACKGROUND,
        SYNC_START,
        SYNC_STOP,
        SYNC_STATE,
        SEND_REQUEST,
        SEND_QUEUED,
        SEND_REJECTED,
        SEND_PENDING,
        SEND_FAILED,
        SEND_SENT,
        EVENT_AVAILABLE,
        EVENT_UNDECRYPTABLE,
        EVENT_SESSION,
        UTD_CALLBACK,
    }

    @Volatile var enabled = false
        private set
    @Volatile private var directory: File? = null
    private val generation = AtomicLong()
    private val dropped = AtomicLong()
    private val sequence = AtomicLong()
    private val executor by lazy {
        ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, ArrayBlockingQueue(256), { task ->
            Thread(task, "syncme-decryption-log").apply { isDaemon = true }
        }).apply { allowCoreThreadTimeOut(true) }
    }
    private val lastEvents = linkedMapOf<String, String>()

    fun configure(context: Context, value: Boolean) {
        directory = File(context.applicationContext.filesDir, "decryption-diagnostics")
        enabled = value
        generation.incrementAndGet()
        synchronized(lastEvents) { lastEvents.clear() }
        if (value) record(Stage.ENABLED)
    }

    /** Identifier inputs are hashed; detail is restricted to enum names, never free-form SDK errors. */
    fun record(stage: Stage, identifier: String? = null, detail: Enum<*>? = null, elapsedMs: Long? = null, relatedIdentifier: String? = null) {
        if (!enabled) return
        val epoch = generation.get()
        val line = "${System.currentTimeMillis()} seq=${sequence.incrementAndGet()} ${stage.name}" +
            " ref=${identifier?.let(
                ::fingerprint
            ) ?: "-"} detail=${detail?.name ?: "-"} elapsedMs=${elapsedMs ?: -1} related=${relatedIdentifier?.let(::fingerprint) ?: "-"}\n"
        enqueue {
            if (enabled && generation.get() == epoch) {
                val dir = directory ?: return@enqueue
                dir.mkdirs()
                val file = File(dir, "current.txt")
                if (file.length() + line.length > 256_000) {
                    val previous = File(dir, "previous.txt")
                    previous.delete()
                    file.renameTo(previous)
                }
                file.appendText(line)
            }
        }
    }

    fun observe(item: EventTimelineItem) {
        if (!enabled) return
        val id = item.eventId?.value ?: (item.localSendState as? LocalEventSendState.Sent)?.eventId?.value
            ?: item.transactionId?.value ?: return
        val data = (item.content as? UnableToDecryptContent)?.data
        val megolm = data as? UnableToDecryptContent.Data.MegolmV1AesSha2
        val stage = when {
            data != null -> Stage.EVENT_UNDECRYPTABLE
            item.localSendState is LocalEventSendState.Failed -> Stage.SEND_FAILED
            item.localSendState is LocalEventSendState.Sending -> Stage.SEND_PENDING
            item.localSendState is LocalEventSendState.Sent -> Stage.SEND_SENT
            else -> Stage.EVENT_AVAILABLE
        }
        val signature = "$stage:${megolm?.utdCause}"
        synchronized(lastEvents) {
            if (lastEvents[id] == signature) return
            lastEvents[id] = signature
            if (lastEvents.size > 2048) lastEvents.remove(lastEvents.keys.first())
        }
        record(stage, id, megolm?.utdCause)
        // Session identifiers are public metadata, but export only a fingerprint for grouping failures.
        val session = megolm?.sessionId ?: runCatching {
            val json = item.timelineItemDebugInfoProvider().originalJson ?: return@runCatching null
            val event = org.json.JSONObject(json)
            if (event.optString("type") == "m.room.encrypted") {
                event.optJSONObject("content")?.optString("session_id")?.takeIf { it.isNotEmpty() }
            } else {
                null
            }
        }.getOrNull()
        if (session != null) record(Stage.EVENT_SESSION, id, relatedIdentifier = session)
    }

    /** Invoke on an IO dispatcher. Serialized after pending writes; export remains available when disabled. */
    fun report(): String = executor.submit<String> {
        val dir = directory
        "SyncMe decryption diagnostics v1\nUTC epoch milliseconds; enabled=$enabled dropped=${dropped.get()}\n" +
            "References are SHA-256 fingerprints. SEND_QUEUED means accepted locally, not key delivery.\n" +
            "EVENT_AVAILABLE does not prove delivery to another device. Native key transport is not captured.\n" +
            listOf("previous.txt", "current.txt").joinToString("") { name ->
                dir?.let { File(it, name) }?.takeIf { it.exists() }?.readText().orEmpty()
            }
    }.get()

    fun clear() {
        generation.incrementAndGet()
        synchronized(lastEvents) { lastEvents.clear() }
        executor.submit {
            directory?.listFiles()?.forEach { it.delete() }
            dropped.set(0)
        }.get()
    }

    private fun enqueue(task: () -> Unit) {
        runCatching { executor.execute { runCatching(task).onFailure { dropped.incrementAndGet() } } }
            .onFailure { dropped.incrementAndGet() }
    }

    internal fun fingerprint(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).take(12).joinToString("") { "%02x".format(it) }
}
