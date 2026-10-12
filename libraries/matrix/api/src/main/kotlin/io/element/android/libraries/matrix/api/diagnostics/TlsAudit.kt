package io.element.android.libraries.matrix.api.diagnostics

import android.content.Context
import android.os.Process
import android.os.SystemClock
import android.net.TrafficStats
import java.io.File
import java.io.RandomAccessFile
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean

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
    private val trafficSamplerStarted = AtomicBoolean(false)
    @Volatile private var enabled = false
    @Volatile private var activeFile: File? = null
    @Volatile private var reportDir: File? = null
    @Volatile private var rustLogRoot: File? = null
    @Volatile private var nativeClearMarker: File? = null

    fun setEnabled(context: Context, value: Boolean) {
        reportDir = File(context.filesDir, "syncme-tls-audit")
        rustLogRoot = File(context.cacheDir, "logs")
        nativeClearMarker = File(context.filesDir, "syncme-tls-audit/native-clear-cutoff.txt")
        if (!value) {
            enabled = false
            activeFile = null
            return
        }
        if (enabled && activeFile != null) return
        enabled = true
        initialize(context)
    }

    fun initialize(context: Context) {
        if (!enabled) return
        val folder = File(context.filesDir, "syncme-tls-audit")
        folder.mkdirs()
        reportDir = folder
        // Matrix SDK's own native tracing writer (set up in PlatformInitializer).
        // It is separate from this audit and may be rotated by the SDK.
        rustLogRoot = File(context.cacheDir, "logs")
        // Persist a cutoff rather than deleting files belonging to the active Rust writer.
        nativeClearMarker = File(folder, "native-clear-cutoff.txt")
        activeFile = File(folder, "tls-" + System.currentTimeMillis() + "-" + Process.myPid() + ".txt")
        writer.execute {
            folder.listFiles { f -> f.isFile && f.name.startsWith("tls-") && f.name.endsWith(".txt") }
                ?.sortedByDescending { it.name }
                ?.drop(MAX_FILES)
                ?.forEach { runCatching { it.delete() } }
        }
        record("AUDIT_STARTED", "build=SyncMe236_OPT_IN_TLS_AUDIT scope=Matrix_Rust_builder+Kotlin_OkHttp+Android_cleartext_hints")
        record("AUDIT_LIMITATION", "Rust_tracing_logs_are_scanned_but_not_packet_capture; unlogged_native_WebView_connections_can_remain_invisible")
        record("AUDIT_PRIVACY", "host_ip_port_only; no_URL_path_query_headers_cookies_tokens_or_body")
        startUidTrafficSampler()
    }

    /**
     * Independent, low-frequency app-UID byte counters. Helps spot packets
     * from native or WebView stacks that SDK tracing did not log.
     * NO destination, URL, port or caller attribution is available from
     * TrafficStats; this is only evidence that some app traffic occurred.
     */
    private fun startUidTrafficSampler() {
        if (!enabled || !trafficSamplerStarted.compareAndSet(false, true)) return
        Thread({
            val uid = Process.myUid()
            var previousRx = TrafficStats.getUidRxBytes(uid)
            var previousTx = TrafficStats.getUidTxBytes(uid)
            if (previousRx == TrafficStats.UNSUPPORTED.toLong() ||
                previousTx == TrafficStats.UNSUPPORTED.toLong()
            ) {
                record("UID_TRAFFIC_COUNTER_UNAVAILABLE", "TrafficStats_unsupported")
                trafficSamplerStarted.set(false)
                return@Thread
            }
            record("UID_TRAFFIC_COUNTER_ENABLED", "sampling_interval_ms=5000 uid=" + uid)
            while (enabled) {
                Thread.sleep(5_000L)
                if (!enabled) break
                val rx = TrafficStats.getUidRxBytes(uid)
                val tx = TrafficStats.getUidTxBytes(uid)
                if (rx >= previousRx && tx >= previousTx &&
                    (rx > previousRx || tx > previousTx)
                ) {
                    record(
                        "UID_NETWORK_DELTA",
                        "rx_bytes=" + (rx - previousRx) + " tx_bytes=" + (tx - previousTx) +
                            " destination=UNKNOWN source=ALL_APP_STACKS",
                    )
                }
                previousRx = rx
                previousTx = tx
            }
            trafficSamplerStarted.set(false)
            if (enabled) startUidTrafficSampler()
        }, "syncme-uid-traffic-sampler").apply { isDaemon = true }.start()
    }

    fun record(kind: String, details: String = "", caller: Boolean = false) {
        if (!enabled) return
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
        if (!enabled) return
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
        val kotlinLog = if (files.isEmpty()) "No Kotlin TLS audit reports." else files.joinToString("\n\n") {
            "===== " + it.name + " =====\n" + runCatching { it.readText() }.getOrDefault("Read error.")
        }
        return kotlinLog + "\n\n" + scanNativeRustNetworkLogs()
    }


    /**
     * Correlate SDK-native tracing output with Kotlin OkHttp audit. Uses only
     * bounded tails of locally rotated tracing files, never full log bodies.
     * Never export raw Rust trace text: a dependency could put credentials,
     * user IDs, room IDs or HTTP paths in otherwise innocuous DEBUG output.
     */
    private fun scanNativeRustNetworkLogs(): String {
        val folder = rustLogRoot ?: return "RUST_NATIVE_TRACE_NOT_INITIALIZED"
        if (!folder.isDirectory) {
            return "RUST_NATIVE_TRACE_UNAVAILABLE directory_not_found=cache/logs"
        }
        val cutoffMs = nativeClearMarker?.let { marker ->
            runCatching { marker.takeIf { it.isFile }?.readText()?.trim()?.toLongOrNull() }.getOrNull()
        }
        val files = runCatching {
            folder.walkTopDown().maxDepth(3)
                .filter { file ->
                    file.isFile &&
                        (file.name.startsWith("logs") || file.extension.equals("log", true)) &&
                        !file.name.endsWith(".gz")
                }
                .sortedByDescending { it.lastModified() }
                .take(20).toList()
        }.getOrDefault(emptyList())
        val examples = LinkedHashMap<String, String>()
        val eventCounts = linkedMapOf<String, Int>()
        var candidateEvents = 0
        var scannedLines = 0
        var oldLinesFiltered = 0
        var readErrors = 0
        for (file in files) {
            runCatching {
                RandomAccessFile(file, "r").use { raf ->
                    val from = (raf.length() - 400_000L).coerceAtLeast(0L)
                    raf.seek(from)
                    if (from > 0L) raf.readLine()
                    var line = raf.readLine()
                    while (line != null && scannedLines < 15_000) {
                        scannedLines++
                        val rawTime = rustTimeRegex.find(line)?.value
                        // Do not delete or truncate live Rust logs: ignore events from
                        // before the last 'Clear TLS Logs' action, even after restart.
                        val eventMs = rawTime?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
                        if (cutoffMs != null && (eventMs == null || eventMs < cutoffMs)) {
                            oldLinesFiltered++
                        } else {
                            classifyRustLine(line)?.let { event ->
                                candidateEvents++
                                val bucket = event.substringBefore(" time=")
                                eventCounts[bucket] = (eventCounts[bucket] ?: 0) + 1
                                if (!examples.containsKey(bucket) && examples.size < 90) {
                                    examples[bucket] = event
                                }
                            }
                        }
                        line = raf.readLine()
                    }
                }
            }.onFailure { readErrors++ }
            if (scannedLines >= 15_000) break
        }
        return buildString {
            appendLine("===== RUST_NATIVE_NETWORK_TRACING (sanitized; metadata only) =====")
            appendLine("files=" + files.size + " scanned_lines=" + scannedLines +
                " candidate_trace_lines=" + candidateEvents + " read_errors=" + readErrors +
                " before_clear_filtered=" + oldLinesFiltered)
            appendLine("COUNT_NOTE: trace_lines_are_NOT_requests_or_failed_connections")
            appendLine("HOST_NOTE: observed_host_is_from_log_text_NOT_verified_socket_destination")
            appendLine("coverage=SDK_tracing_targets_only; does_NOT_prove_absence_of_external_traffic")
            appendLine("raw_urls_query_headers_tokens_message_contents=NOT_EXPORTED")
            if (files.isEmpty()) appendLine("RUST_NATIVE_TRACE_UNAVAILABLE no_tracing_files_seen")
            if (candidateEvents == 0) appendLine("NO_CLASSIFIED_NATIVE_NETWORK_EVENTS")
            appendLine("--- AGGREGATED_EVENT_CATEGORIES ---")
            eventCounts.entries.sortedByDescending { it.value }.take(100).forEach { (key, count) ->
                appendLine("count=" + count + " " + key)
            }
            appendLine("--- FIRST_EXAMPLE_PER_CATEGORY (maximum 90) ---")
            examples.values.forEach { appendLine("source=RustSDK " + it) }
        }
    }

    private val urlHostRegex = Regex(
        """(?:https?://|wss?://)([A-Za-z0-9.-]+)(?::([0-9]{1,5}))?""",
        RegexOption.IGNORE_CASE,
    )
    private val rustTimeRegex = Regex(
        """\b20[0-9]{2}-[0-9]{2}-[0-9]{2}[T ][0-9:.Z+_-]{8,28}""",
    )
    private val rustIpRegex = Regex(
        """(?:ip|peer|remote|socket|addr)=([0-9a-fA-F:.]{3,45})(?::([0-9]{2,5}))?""",
        RegexOption.IGNORE_CASE,
    )
    private val hostLabelRegex = Regex(
        """(?:host|hostname|server_name|server|domain)=([A-Za-z0-9.-]+)""",
        RegexOption.IGNORE_CASE,
    )
    private val traceTargetRegex = Regex(
        """(?:matrix_sdk::http_client|matrix_sdk::client|reqwest|hyper|rustls|hickory_resolver|trust_dns|ocsp|crl|certificate)""",
        RegexOption.IGNORE_CASE,
    )

    private val rustSeverityRegex = Regex(
        """(?:^|\s)(?:ERROR|WARN)(?:\s|:|\]|\})|(?:level|severity)\s*[:=]\s*["']?(?:error|warn)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val httpStatusRegex = Regex(
        """\b(?:status|status_code|http_status)\s*[:=]\s*["']?([1-5][0-9]{2})\b""",
        RegexOption.IGNORE_CASE,
    )

    private fun classifyRustLine(line: String): String? {
        val target = traceTargetRegex.find(line)?.value ?: return null
        val lc = line.lowercase()
        val networkContext = listOf("http", "request", "response", "connect", "host", "tls", "cert", "dns", "proxy", "socket")
            .any { lc.contains(it) }
        if (!networkContext) return null
        val status = httpStatusRegex.find(line)?.groupValues?.getOrNull(1).orEmpty()
        val severity = rustSeverityRegex.containsMatchIn(line)
        val event = when {
            lc.contains("ocsp") -> "RUST_OCSP_CANDIDATE"
            lc.contains("crl") -> "RUST_CRL_CANDIDATE"
            lc.contains("aia") && lc.contains("cert") -> "RUST_AIA_CANDIDATE"
            severity && (lc.contains("timeout") || lc.contains("error") ||
                lc.contains("failed") || lc.contains("refused")) -> "RUST_REPORTED_NETWORK_ERROR"
            status.startsWith("5") -> "RUST_HTTP_5XX"
            status.startsWith("4") -> "RUST_HTTP_4XX"
            lc.contains("dns") || lc.contains("resolve") -> "RUST_DNS_TRACE"
            lc.contains("tls") || lc.contains("handshake") || lc.contains("certificate") -> "RUST_TLS_TRACE"
            lc.contains("response") -> "RUST_HTTP_RESPONSE_TRACE"
            lc.contains("request") || lc.contains("send") -> "RUST_HTTP_REQUEST_TRACE"
            lc.contains("connect") -> "RUST_CONNECT_TRACE"
            else -> "RUST_NETWORK_TRACE"
        }
        val hostMatch = urlHostRegex.find(line) ?: hostLabelRegex.find(line)
        val host = hostMatch?.groupValues?.getOrNull(1)?.take(200).orEmpty()
        val ip = rustIpRegex.find(line)?.groupValues?.getOrNull(1).orEmpty()
        val category = when {
            host.isEmpty() -> "UNKNOWN_TARGET"
            isPossibleCertificateEndpoint(host) -> "SUSPECT_CERT_ENDPOINT"
            host.equals("syncme.ir", true) || host.endsWith(".syncme.ir", true) -> "SYNCME"
            else -> "EXTERNAL_OBSERVED"
        }
        val key = "event=" + event + " target=" + safe(target, 50) +
            " category=" + category + " observed_host=" +
            (if (host.isEmpty()) "unknown" else safe(host, 200)) +
            " status=" + (if (status.isEmpty()) "unknown" else status)
        val time = rustTimeRegex.find(line)?.value.orEmpty()
        return key + " time=" + (if (time.isEmpty()) "unknown" else safe(time, 45)) +
            " observed_ip=" + (if (ip.isEmpty()) "unknown" else safe(ip, 50))
    }

    fun clearReports(): Boolean {
        val folder = reportDir ?: return false
        return runCatching {
            writer.submit<Boolean> {
                counters.clear()
                var ok = true
                // Clearing Rust tracing via its live file writer is unsafe.
                // A persisted UTC cutoff gives clean future exports instead.
                nativeClearMarker?.let { marker ->
                    if (runCatching { marker.writeText(System.currentTimeMillis().toString()) }.isFailure) ok = false
                } ?: run { ok = false }
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
