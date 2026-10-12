package org.rustls.platformverifier

import android.content.Context
import android.net.http.X509TrustManagerExtensions
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.element.android.tests.testutils.robolectric.RobolectricTest
import io.mockk.clearConstructorMockk
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.verify
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class CertificateVerifierTest : RobolectricTest() {
    private val certificate = javaClass.getResourceAsStream("/server.pem")!!.use {
        CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
    }
    private val validTime = certificate.notBefore.time + 86_400_000L

    @Before
    fun setUp() {
        clearConstructorMockk(X509TrustManagerExtensions::class)
        every {
            anyConstructed<X509TrustManagerExtensions>().checkServerTrusted(any(), any(), any())
        } returns listOf(certificate)
    }

    @Test
    fun `valid chain does not attempt the unreachable CRL or OCSP endpoints`() {
        assertThat(verifyChain()).isEqualTo(0)
        verify(exactly = 1) {
            anyConstructed<X509TrustManagerExtensions>().checkServerTrusted(any(), "RSA", "localhost")
        }
    }

    @Test
    fun `untrusted chain fails closed`() {
        every {
            anyConstructed<X509TrustManagerExtensions>().checkServerTrusted(any(), any(), any())
        } throws CertificateException("untrusted")
        assertThat(verifyChain()).isEqualTo(3)
    }

    @Test
    fun `trust provider runtime error fails closed`() {
        every {
            anyConstructed<X509TrustManagerExtensions>().checkServerTrusted(any(), any(), any())
        } throws IllegalStateException("unavailable")
        assertThat(verifyChain()).isEqualTo(1)
    }

    @Test
    fun `expired and not yet valid certificates are rejected`() {
        assertThat(verifyChain(time = certificate.notAfter.time + 1_000)).isEqualTo(2)
        assertThat(verifyChain(time = certificate.notBefore.time - 1_000)).isEqualTo(2)
    }

    @Test
    fun `wrong certificate usage is rejected`() {
        assertThat(verifyChain(allowedEkus = arrayOf("1.3.6.1.5.5.7.3.2"))).isEqualTo(6)
    }

    @Test
    fun `empty and malformed chains are rejected`() {
        assertThat(verifyChain(chain = emptyArray())).isEqualTo(5)
        assertThat(verifyChain(chain = arrayOf(byteArrayOf(1, 2, 3)))).isEqualTo(5)
    }

    // Invoke the exact JNI entry point, including its private result code field.
    private fun verifyChain(
        chain: Array<ByteArray> = arrayOf(certificate.encoded),
        time: Long = validTime,
        allowedEkus: Array<String> = arrayOf("1.3.6.1.5.5.7.3.1"),
    ): Int {
        val method = CertificateVerifier::class.java.getDeclaredMethod(
            "verifyCertificateChain", Context::class.java, String::class.java, String::class.java,
            Array<String>::class.java, ByteArray::class.java, Long::class.javaPrimitiveType, Array<ByteArray>::class.java,
        ).apply { isAccessible = true }
        val result = method.invoke(
            null, ApplicationProvider.getApplicationContext<Context>(), "localhost", "RSA", allowedEkus, null, time, chain,
        )
        return result.javaClass.getDeclaredField("code").apply { isAccessible = true }.getInt(result)
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun mockTrustManager() = mockkConstructor(X509TrustManagerExtensions::class)

        @JvmStatic
        @AfterClass
        fun restoreTrustManager() = unmockkConstructor(X509TrustManagerExtensions::class)
    }
}
