package io.element.android.libraries.matrix.api.diagnostics

import android.content.Context
import android.os.Process
import android.os.SystemClock
import java.io.File
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * SyncMe experimental SSL/TLS audit. Logs connection metadata only.
 *
 * Scope: Matrix Rust builder configuration; instrumented Kotlin OkHttp calls;
 * Android cleartext StrictMode hints (when available).
 * NOT a packet capture and NOT proof that uninstrumented native/WebView
 * libraries made no requests. No URLs, request bodies, tokens or cookies.
 */
object TlsAudit {
    private val writer = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "syncme-tls-audit-writer").apply { isDaemon = true }
    }
    private const val MAX_FILES = 8
    private const val MAX_FILE_BYTES = 384_000L
    private val started = SystemClock.elapsedRealtime()
    private val counters = ConcurrentHashMap<String, AtomicInteger>()
    @Volatile private var activeFile: File? = null
    @Volatile private var reportDir: File? = null

    fun initialize(context: Context) {
        val folder = File(context.filesDir, "syncme-tls-audit")
        folder.mkdirs()
        reportDir = folder
        activeFile = File(folder, "tls-" + System.currentTimeMillis() + "-" + Process.myPid() + ".txt")
        writer.execute {
            folder.listFiles { f -> f.isFile && f.name.startsWith("tls-") && f.name.endsWith(".txt") }
                ?.sortedByDescending { it.name }
                ?.drop(MAX_FILES)
                ?.forEach { runCatching { it.delete() } }
        }
        record("AUDIT_STARTED", "build=SyncMe234_TLS_AUDIT scope=Matrix_Rust_builder+Kotlin_OkHttp+Android_cleartext_hints")
        record("AUDIT_LIMITATION", "Rust_native_requests_and_WebView_network_are_not_intercepted; no_packets_does_not_prove_no_traffic")
        record("AUDIT_PRIVACY", "host_ip_port_only; no_URL_path_query_headers_cookies_tokens_or_body")
    }

    fun record(kind: String, details: String = "", caller: Boolean = false) {
        val file = activeFile ?: return
        val safeDetails = safe(details, 400)
        val origin = if (caller) " origin=" + callerFrames() else ""
        val row = Instant.now().toString() + " ms=" + (SystemClock.elapsedRealtime() - started) +
            " pid=" + Process.myPid() + " thread=" + safe(Thread.currentThread().name, 40) +
            " event=" + safe(kind, 64) + " " + safeDetails + origin + "\n"
        writer.execute {
            runCatching {
                if (file.length() < MAX_FILE_BYTES) file.appendText(row)
            }
        }
    }

    fun network(kind: String, host: String, details: String = "", caller: Boolean = false) {
        val cleanHost = safe(host.lowercase(), 200)
        val suspect = isPossibleCertificateEndpoint(host)
        val internal = host.equals("syncme.ir", ignoreCase = true) ||
            host.endsWith(".syncme.ir", ignoreCase = true)
        val category = if (suspect) "SUSPECT_CERT_ENDPOINT" else if (internal) "SYNCME" else "EXTERNAL"
        // Keep all likely OCSP/CRL/AIA requests, sample normal repetitive traffic.
        val limit = if (suspect) 150 else if (internal) 4 else 12
        val key = kind + "|" + cleanHost
        val count = counters.computeIfAbsent(key) { AtomicInteger(0) }.incrementAndGet()
        if (count <= limit) {
            record(kind, "category=" + category + " host=" + cleanHost + " " + details, caller)
        } else if (count == limit + 1) {
            record("SAMPLED_AFTER_LIMIT", "category=" + category + " host=" + cleanHost +
                " eventType=" + kind + " limit=" + limit)
        }
    }

    private fun isPossibleCertificateEndpoint(host: String): Boolean {
        val h = host.lowercase()
        return h.contains("ocsp") || h.contains("crl") || h.contains("certum") ||
            h.contains("certificates") || h.contains("sectigo") || h.contains("digicert") ||
            h.contains("lencr.org") || h.contains("pki.goog") || h.contains("aia.") ||
            h.contains("cdp.") || h.contains("geotrust") || h.contains("trust-provider")
    }

    fun readReports(): String {
        val folder = reportDir ?: return "TLS audit not initialized."
        // Drain pending writes before taking the snapshot.
        writer.submit { Unit }.get()
        val files = folder.listFiles { f -> f.isFile && f.name.startsWith("tls-") && f.name.endsWith(".txt") }
            ?.sortedBy { it.name }
            ?.takeLast(MAX_FILES)
            .orEmpty()
        return if (files.isEmpty()) "No TLS audit reports." else files.joinToString("\n\n") {
            "===== " + it.name + " =====\n" + runCatching { it.readText() }.getOrDefault("Read error.")
        }
    }

    fun clearReports(): Boolean {
        val folder = reportDir ?: return false
        return runCatching {
            writer.submit<Boolean> {
                counters.clear()
                var ok = true
                folder.listFiles { f -> f.isFile && f.name.startsWith("tls-") && f.name.endsWith(".txt") }
                    ?.forEach { file ->
                        if (file == activeFile) {
                            if (runCatching { file.writeText("") }.isFailure) ok = false
                        } else if (!file.delete()) ok = false
                    }
                ok
            }.get()
        }.getOrDefault(false)
    }

    private fun callerFrames(): String {
        // Keep a bounded, non-sensitive call origin; no arguments or message content.
        return Throwable().stackTrace
            .filterNot {
                it.className.startsWith("java.") ||
                    it.className.startsWith("android.") ||
                    it.className.contains(".TlsAudit")
            }
            .take(10)
            .joinToString(" <- ") { safe(it.className + "." + it.methodName + ":" + it.lineNumber, 140) }
            .take(900)
    }

    private fun safe(s: String, length: Int): String =
        s.replace(Regex("[^A-Za-z0-9_.,:/=+@|\\-]"), "_").take(length)
}
