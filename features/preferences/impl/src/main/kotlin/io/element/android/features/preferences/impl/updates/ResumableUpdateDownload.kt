/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package io.element.android.features.preferences.impl.updates

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** App-owned recovery transfer. Partial files survive process death; only verify() can authorize installation. */
internal class ResumableUpdateDownload(
    private val directory: File,
    private val connect: () -> HttpsURLConnection = { URL(UPDATE_APK).openConnection() as HttpsURLConnection },
) {
    fun file(release: UpdateRelease): File = File(directory, "${release.sha256}.apk")

    fun seed(release: UpdateRelease, source: File, downloadedBytes: Long) {
        directory.mkdirs()
        val target = file(release)
        // DownloadManager may preallocate the full file. Copy only its confirmed byte count.
        if (target.exists() || !source.exists() || downloadedBytes <= 0) return
        source.inputStream().use { input ->
            FileOutputStream(target).use { output ->
                val buffer = ByteArray(65536)
                var remaining = minOf(downloadedBytes, source.length(), release.size)
                while (remaining > 0) {
                    val count = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                    if (count < 0) break
                    output.write(buffer, 0, count)
                    remaining -= count
                }
                output.fd.sync()
            }
        }
    }

    suspend fun transfer(release: UpdateRelease, onProgress: (Int) -> Unit) {
        check(directory.isDirectory || directory.mkdirs())
        val target = file(release)
        val offset = target.length().takeIf { it <= release.size } ?: run {
            check(target.delete())
            0L
        }
        if (offset == release.size) return
        val connection = connect()
        try {
            connection.connectTimeout = 10000
            connection.readTimeout = 15000
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.setRequestProperty("Accept-Encoding", "identity")
            connection.setRequestProperty("Cache-Control", "no-cache")
            if (offset > 0) connection.setRequestProperty("Range", "bytes=$offset-")
            val status = connection.responseCode
            val append = status == 206
            if (append) {
                val range = Regex("bytes (\\d+)-(\\d+)/(\\d+)").matchEntire(connection.getHeaderField("Content-Range").orEmpty())
                    ?: throw IOException("Invalid update content range")
                if (range.groupValues[1].toLongOrNull() != offset || range.groupValues[3].toLongOrNull() != release.size ||
                    range.groupValues[2].toLongOrNull() != release.size - 1) throw IOException("Unexpected update content range")
            } else if (status != 200) {
                throw IOException("Update transfer unavailable")
            }
            val encoding = connection.getHeaderField("Content-Encoding")
            if (encoding != null && !encoding.equals("identity", ignoreCase = true)) throw IOException("Encoded update transfer")
            val expected = if (append) release.size - offset else release.size
            val length = connection.getHeaderField("Content-Length")?.toLongOrNull()
            if (length != null && length != expected) throw IOException("Update size changed")
            // A server that ignores Range returns 200: replace, never append a second APK.
            connection.inputStream.use { input ->
                FileOutputStream(target, append).use { output ->
                    val buffer = ByteArray(65536)
                    var total = if (append) offset else 0L
                    var progress = -1
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (total + count > release.size) throw IOException("Update exceeds declared size")
                        output.write(buffer, 0, count)
                        total += count
                        val percent = (total * 100 / release.size).toInt()
                        if (percent != progress) { progress = percent; onProgress(percent) }
                    }
                    output.fd.sync()
                    if (total != release.size) throw IOException("Update transfer interrupted")
                }
            }
        } finally { connection.disconnect() }
    }
}
