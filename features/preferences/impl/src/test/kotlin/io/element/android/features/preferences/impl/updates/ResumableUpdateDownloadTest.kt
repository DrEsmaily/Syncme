/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package io.element.android.features.preferences.impl.updates

import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.security.cert.Certificate
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ResumableUpdateDownloadTest {
    @get:Rule val temporary = TemporaryFolder()
    private val bytes = "0123456789".toByteArray()
    private val release = UpdateRelease("1.3.4", 202611152, bytes.size.toLong(), "a".repeat(64), "")

    @Test
    fun `interrupted transfer survives a new downloader and resumes at saved offset`() = runTest {
        val folder = temporary.newFolder()
        val interrupted = object : InputStream() {
            private var offset = 0
            override fun read(): Int = if (offset < 4) bytes[offset++].toInt() else throw IOException("Offline")
            override fun read(buffer: ByteArray, off: Int, len: Int): Int {
                if (offset >= 4) throw IOException("Offline")
                val count = minOf(len, 4 - offset)
                bytes.copyInto(buffer, off, offset, offset + count)
                offset += count
                return count
            }
        }
        val first = FakeConnection(200, interrupted, mapOf("Content-Length" to "10"))
        val downloader = ResumableUpdateDownload(folder) { first }
        expectFailure { downloader.transfer(release) {} }
        assertThat(downloader.file(release).readBytes()).isEqualTo(bytes.copyOf(4))
        assertThat(first.disconnected).isTrue()
        val second = FakeConnection(206, ByteArrayInputStream(bytes.copyOfRange(4, 10)),
            mapOf("Content-Range" to "bytes 4-9/10", "Content-Length" to "6"))
        val recreated = ResumableUpdateDownload(folder) { second }
        recreated.transfer(release) {}
        assertThat(second.getRequestProperty("Range")).isEqualTo("bytes=4-")
        assertThat(recreated.file(release).readBytes()).isEqualTo(bytes)
        assertThat(second.disconnected).isTrue()
    }

    @Test
    fun `server ignoring range replaces the partial file instead of duplicating bytes`() = runTest {
        val connection = FakeConnection(200, ByteArrayInputStream(bytes), mapOf("Content-Length" to "10"))
        val downloader = ResumableUpdateDownload(temporary.newFolder()) { connection }
        downloader.file(release).writeBytes(bytes.copyOf(4))
        downloader.transfer(release) {}
        assertThat(downloader.file(release).readBytes()).isEqualTo(bytes)
    }

    @Test
    fun `wrong range or changed total cannot append to an existing prefix`() = runTest {
        for (range in listOf("bytes 0-9/10", "bytes 4-9/11", "invalid")) {
            val connection = FakeConnection(206, ByteArrayInputStream(bytes), mapOf("Content-Range" to range))
            val downloader = ResumableUpdateDownload(temporary.newFolder()) { connection }
            downloader.file(release).writeBytes(bytes.copyOf(4))
            expectFailure { downloader.transfer(release) {} }
            assertThat(downloader.file(release).readBytes()).isEqualTo(bytes.copyOf(4))
        }
    }

    @Test
    fun `oversized response is rejected and cannot exceed metadata size`() = runTest {
        val connection = FakeConnection(200, ByteArrayInputStream(bytes + bytes))
        val downloader = ResumableUpdateDownload(temporary.newFolder()) { connection }
        expectFailure { downloader.transfer(release) {} }
        assertThat(downloader.file(release).length()).isAtMost(release.size)
    }

    @Test
    fun `error responses preserve downloaded bytes for a later retry`() = runTest {
        val connection = FakeConnection(503, ByteArrayInputStream(byteArrayOf()))
        val downloader = ResumableUpdateDownload(temporary.newFolder()) { connection }
        downloader.file(release).writeBytes(bytes.copyOf(4))
        expectFailure { downloader.transfer(release) {} }
        assertThat(downloader.file(release).readBytes()).isEqualTo(bytes.copyOf(4))
    }

    @Test
    fun `migration copies confirmed bytes rather than preallocated file length`() {
        val source = temporary.newFile().apply { writeBytes(bytes) }
        val downloader = ResumableUpdateDownload(temporary.newFolder())
        downloader.seed(release, source, 4)
        assertThat(downloader.file(release).readBytes()).isEqualTo(bytes.copyOf(4))
        assertThat(source.readBytes()).isEqualTo(bytes)
        // An existing recovery prefix survives repeated migration after process recreation.
        downloader.seed(release, source, 8)
        assertThat(downloader.file(release).length()).isEqualTo(4L)
    }

    @Test
    fun `different release hashes cannot share a partial file`() {
        val downloader = ResumableUpdateDownload(temporary.newFolder())
        downloader.file(release).writeBytes(bytes.copyOf(4))
        assertThat(downloader.file(release.copy(sha256 = "b".repeat(64))).exists()).isFalse()
    }

    private suspend fun expectFailure(block: suspend () -> Unit) {
        var failed = false
        try { block() } catch (_: IOException) { failed = true }
        assertThat(failed).isTrue()
    }

    private class FakeConnection(
        private val status: Int,
        private val stream: InputStream,
        private val headers: Map<String, String> = emptyMap(),
    ) : HttpsURLConnection(URL(UPDATE_APK)) {
        var disconnected = false
        override fun getResponseCode() = status
        override fun getHeaderField(name: String) = headers[name]
        override fun getInputStream() = stream
        override fun disconnect() { disconnected = true }
        override fun usingProxy() = false
        override fun connect() = Unit
        override fun getCipherSuite() = "test"
        override fun getLocalCertificates(): Array<Certificate>? = null
        override fun getServerCertificates(): Array<Certificate> = emptyArray()
    }
}
