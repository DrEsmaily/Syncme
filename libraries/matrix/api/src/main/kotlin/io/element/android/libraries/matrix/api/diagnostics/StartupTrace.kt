package io.element.android.libraries.matrix.api.diagnostics

import android.content.Context
import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicBoolean
import java.io.File
import java.time.Instant
import java.util.concurrent.Executors

/**
 * Bounded, privacy-minimal startup telemetry. No credentials, IDs, network bodies, or messages.
 * The disk writer is never invoked on the UI thread.
 */
object StartupTrace {
    @Volatile private var resetTraceFile: File? = null
    @Volatile private var rustTraceDir: File? = null
    fun initResetTrace(context: Context) {
        rustTraceDir = File(context.cacheDir, "logs")
        val folder = File(context.filesDir, "syncme-reset-diagnostics")
        folder.mkdirs()
        resetTraceFile = File(folder, "reset-events.txt")
        worker.execute {
            val file = resetTraceFile ?: return@execute
            if (file.length() > 350_000L) file.writeText("")
        }
    }
    fun markReset(event: String) {
        val file = resetTraceFile ?: return
        val safe = event.replace(Regex("[^a-zA-Z0-9_.:-]"), "_").take(240)
        val row = "${Instant.now()} pid=${android.os.Process.myPid()} ms=${SystemClock.elapsedRealtime() - processStart} event=$safe\n"
        worker.execute { runCatching { if (file.length() < 350_000L) file.appendText(row) } }
    }
    private val worker = Executors.newSingleThreadExecutor { task ->
        Thread(task, "syncme-startup-trace").apply { isDaemon = true }
    }
    @Volatile private var target: File? = null
    @Volatile private var directory: File? = null
    private const val MAX_RUNS = 10
    private const val RUN_LIMIT_BYTES = 256_000L
    private val processStart = SystemClock.elapsedRealtime()
    private val watchdogStarted = AtomicBoolean(false)
    @Volatile private var enabled = false
    private val processMarker = System.currentTimeMillis().toString(36)

    fun setEnabled(context: Context, value: Boolean) {
        directory = File(context.filesDir, "syncme-startup-runs")
        if (!value) {
            enabled = false
            target = null
            return
        }
        if (enabled && target != null) return
        enabled = true
        initialize(context)
    }

    fun initialize(context: Context) {
        if (!enabled) return
        val folder = File(context.filesDir, "syncme-startup-runs")
        folder.mkdirs()
        directory = folder
        target = File(folder, "run-" + System.currentTimeMillis().toString().padStart(13, '0') +
            "-" + android.os.Process.myPid() + ".txt")
        worker.execute { prune(folder) }
        mark("application_init")
        startWatchdog()
    }

    private fun startWatchdog() {
        if (!watchdogStarted.compareAndSet(false, true)) return
        val handler = Handler(Looper.getMainLooper())
        Thread({
            while (enabled) {
                val acknowledged = AtomicBoolean(false)
                val postedAt = SystemClock.elapsedRealtime()
                handler.post { acknowledged.set(true) }
                Thread.sleep(350L)
                if (!acknowledged.get()) {
                    mark("main_thread_stall_detected_ms_" + (SystemClock.elapsedRealtime() - postedAt))
                    var samples = 0
                    while (enabled && !acknowledged.get()) {
                        // Always retain the full Java call chain, including the application SDK
                        // frames BELOW Socket.connect; 12 frames were insufficient previously.
                        if (samples < 8 || samples % 5 == 0) {
                            val main = Looper.getMainLooper().thread
                            mark("stall_sample_" + samples + "_state_" + main.state)
                            main.stackTrace.take(72).forEachIndexed { index, frame ->
                                mark("stack_" + samples + "_" + index + "_" + frame.className + "." +
                                    frame.methodName + ":" + frame.lineNumber)
                            }
                        }
                        samples++
                        Thread.sleep(400L)
                    }
                    mark("main_thread_resumed_after_ms_" + (SystemClock.elapsedRealtime() - postedAt))
                }
                Thread.sleep(150L)
            }
            watchdogStarted.set(false)
            if (enabled) startWatchdog()
        }, "syncme-startup-watchdog").apply { isDaemon = true }.start()
    }

    private fun prune(folder: File) {
        val files = folder.listFiles { f -> f.isFile && f.name.startsWith("run-") && f.name.endsWith(".txt") }
            ?.sortedByDescending { it.name } ?: return
        files.drop(MAX_RUNS).forEach { runCatching { it.delete() } }
    }

    /** Call only from a background dispatcher; includes up to ten completed/current runs. */
    fun readRecentReports(): String {
        val folder = directory ?: return "Diagnostics unavailable."
        val files = folder.listFiles { f -> f.isFile && f.name.startsWith("run-") && f.name.endsWith(".txt") }
            ?.sortedBy { it.name }?.takeLast(MAX_RUNS).orEmpty()
        val resetReport = resetTraceFile?.let { runCatching { it.readText() }.getOrDefault("Reset trace read error") } ?: "Reset trace unavailable"
        return "===== RESET IDENTITY DIAGNOSTICS =====\n" + resetReport + "\n\n" + readRustResetSummary() + "\n\n" + (if (files.isEmpty()) "No startup data." else files.joinToString("\n\n") { file ->
            "===== " + file.name + " =====\n" +
                runCatching { file.readText() }.getOrDefault("Read error")
        })
    }

    /**
     * Summarize native SDK crypto/recovery traces without exporting untrusted raw
     * text (which can include secrets, user IDs, URLs, or message content).
     */
    private fun readRustResetSummary(): String {
        val dir = rustTraceDir ?: return "RUST_RESET_TRACE_NOT_INITIALIZED"
        if (!dir.isDirectory) return "RUST_RESET_TRACE_DIRECTORY_MISSING"
        val candidates = dir.walkTopDown().maxDepth(3)
            .filter { it.isFile && it.name.startsWith("syncme-reset-rust") }
            .sortedByDescending { it.lastModified() }.take(8).toList()
        val counts = linkedMapOf<String, Int>()
        var scanned = 0
        var failures = 0
        val relevant = listOf("backup", "cross.sign", "identity", "recovery", "secret.storage", "key.upload", "crypto")
        for (file in candidates) {
            runCatching {
                java.io.RandomAccessFile(file, "r").use { raf ->
                    val offset = (raf.length() - 700_000L).coerceAtLeast(0L)
                    raf.seek(offset)
                    if (offset > 0) raf.readLine()
                    while (scanned < 25_000) {
                        val line = raf.readLine() ?: break
                        scanned++
                        val lc = line.lowercase()
                        if (relevant.none { lc.contains(it) }) continue
                        val topic = when {
                            lc.contains("backup") -> "BACKUP"
                            lc.contains("cross.sign") -> "CROSS_SIGNING"
                            lc.contains("identity") -> "IDENTITY"
                            lc.contains("recovery") -> "RECOVERY"
                            lc.contains("secret.storage") -> "SECRET_STORAGE"
                            lc.contains("key.upload") -> "KEY_UPLOAD"
                            else -> "CRYPTO"
                        }
                        val result = when {
                            lc.contains("error") || lc.contains("failed") -> "ERROR"
                            lc.contains("warn") -> "WARN"
                            lc.contains("disable") -> "DISABLE"
                            lc.contains("enable") -> "ENABLE"
                            lc.contains("creat") -> "CREATE"
                            lc.contains("upload") -> "UPLOAD"
                            lc.contains("success") || lc.contains("complete") -> "SUCCESS"
                            else -> "OTHER"
                        }
                        val key = topic + "_" + result
                        counts[key] = (counts[key] ?: 0) + 1
                    }
                }
            }.onFailure { failures++ }
            if (scanned >= 25_000) break
        }
        return buildString {
            appendLine("===== RUST_CRYPTO_RECOVERY_TRACE_SUMMARY (no raw secrets) =====")
            appendLine("files=" + candidates.size + " lines_scanned=" + scanned + " read_errors=" + failures)
            if (candidates.isEmpty()) appendLine("NATIVE_TRACE_MISSING")
            if (counts.isEmpty()) appendLine("NO_MATCHING_NATIVE_CRYPTO_EVENTS")
            counts.forEach { (key, count) -> appendLine("event=" + key + " count=" + count) }
            appendLine("NOTE: Aggregated SDK events are not a full Rust call trace.")
        }
    }

    /**
     * Clear retained diagnostic runs without touching Matrix caches, sessions or app data.
     * Must be called from a background dispatcher. Serialize with the writer so old
     * queued append operations cannot recreate deleted reports.
     */
    fun clearReports(): Boolean {
        val folder = directory ?: return false
        return runCatching {
            worker.submit<Boolean> {
                val active = target
                val files = folder.listFiles { f ->
                    f.isFile && f.name.startsWith("run-") && f.name.endsWith(".txt")
                }.orEmpty()
                var success = true
                files.forEach { file ->
                    val cleared = if (file == active) {
                        runCatching { file.writeText("") }.isSuccess
                    } else {
                        file.delete()
                    }
                    if (!cleared) success = false
                }
                success
            }.get()
        }.getOrDefault(false)
    }

    fun mark(event: String) {
        if (!enabled) return
        val file = target ?: return
        val safe = event.replace(Regex("[^a-zA-Z0-9_.:-]"), "_").take(240)
        val elapsed = SystemClock.elapsedRealtime() - processStart
        val row = "${Instant.now()} pid=${android.os.Process.myPid()} run=$processMarker ms=$elapsed thread=${Thread.currentThread().name.take(32)} event=$safe\n"
        worker.execute {
            runCatching {
                if (file.length() >= RUN_LIMIT_BYTES) return@runCatching
                file.appendText(row)
            }
        }
    }
}
