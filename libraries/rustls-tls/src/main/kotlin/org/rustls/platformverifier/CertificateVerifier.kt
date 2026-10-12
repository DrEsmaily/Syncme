// SPDX-License-Identifier: MIT OR Apache-2.0
// Vendored from rustls-platform-verifier 8becdc1beaee80afee396a8c769d42fe9bd9cb4e.

package org.rustls.platformverifier

import timber.log.Timber
import android.content.Context
import android.net.http.X509TrustManagerExtensions
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.cert.CertificateException
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateFactory
import java.security.cert.CertificateNotYetValidException
import java.security.cert.CertificateParsingException
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

// If this is updated, update the Rust definition too.
// Marked private as this is not meant to be used in Android code.
private enum class StatusCode(val value: Int) {
    Ok(0),
    Unavailable(1),
    Expired(2),
    UnknownCert(3),
    Revoked(4),
    InvalidEncoding(5),
    InvalidExtension(6),
}

// Marked private as this is not meant to be used in Android code.
private class VerificationResult(
    status: StatusCode,
    @Suppress("unused") val message: String? = null,
) {
    @Suppress("unused")
    private val code: Int = status.value
}

// NOTE: All TrustManager and certificate validation methods are not thread safe. These
// are all guarded by Kotlin's `Synchronized` accessors to prevent undefined behavior.

// Only JNI and test code calls this, so unused code warnings are suppressed.
// Internal for test code - no other Kotlin code should use this object directly.
@Suppress("unused")
// We want to show a difference between Kotlin-side logs and those in Rust code
internal object CertificateVerifier {
    private const val TAG = "rustls-platform-verifier-android"

    private fun createTrustManager(keystore: KeyStore?): X509TrustManagerExtensions? {
        // This can never throw since the default algorithm is used.
        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())

        factory.init(keystore)

        val availableTrustManagers = try {
            factory.trustManagers
        } catch (e: RuntimeException) {
            Timber.tag(TAG).w("exception thrown creating a TrustManager: $e")
            return null
        }

        for (manager in availableTrustManagers) {
            if (manager is X509TrustManager) {
                // Kotlin ensures this can't throw at runtime since it knows that
                // it must be the correct type by now.
                return X509TrustManagerExtensions(manager)
            }
        }

        Timber.tag(TAG).e("failed to find a usable trust manager")
        return null
    }

    private fun makeLazyTrustManager(keystore: KeyStore?): Lazy<X509TrustManagerExtensions?> {
        // Ensure the keystore is loaded. Since all of the trust managers are initialized in a
        // `Lazy`, this will only run once.
        keystore?.load(null)

        return lazy { createTrustManager(keystore) }
    }

    // -- Test only --
    // Ideally, all of this will be optimized out at compile time due to not being accessed
    // in release builds.

    @get:Synchronized
    private val mockKeystore: KeyStore = KeyStore.getInstance(KeyStore.getDefaultType())

    @get:Synchronized
    private var mockTrustManager: Lazy<X509TrustManagerExtensions?> =
        makeLazyTrustManager(mockKeystore)

    @JvmStatic
    private fun addMockRoot(root: ByteArray) {
        if (!BuildConfig.TEST) {
            throw Exception("attempted to add a mock root outside a test!")
        }

        val alias = "root_${mockKeystore.size()}"
        // Throwing here is fine since test roots should always be well-formed
        val cert = certFactory.generateCertificate(ByteArrayInputStream(root))
        mockKeystore.setCertificateEntry(alias, cert)

        reloadMockData()
    }

    @JvmStatic
    private fun clearMockRoots() {
        // Reload to get a completely fresh internal state
        mockKeystore.load(null)
        reloadMockData()
    }

    @JvmStatic
    private fun reloadMockData() {
        if (mockTrustManager.isInitialized()) {
            mockTrustManager = makeLazyTrustManager(mockKeystore)
        }
    }

    // Get a list of the system's root CAs.
    // Function is public for testing only.
    @JvmStatic
    fun getSystemRootCAs(): List<X509Certificate> {
        val rootCAs = mutableListOf<X509Certificate>()

        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        factory.init(systemKeystore)

        val availableTrustManagers = try {
            factory.trustManagers
        } catch (e: RuntimeException) {
            Timber.tag(TAG).w("exception thrown creating a TrustManager: $e")
            return rootCAs
        }

        availableTrustManagers.forEach { trustManager ->
            if (trustManager is X509TrustManager) {
                rootCAs.addAll(trustManager.acceptedIssuers)
            }
        }

        return rootCAs
    }

    // -- End testing requirements --

    private val certFactory: CertificateFactory = CertificateFactory.getInstance("X.509")

    @get:Synchronized
    private val systemKeystore: KeyStore? = try {
        KeyStore.getInstance("AndroidCAStore")
    } catch (_: KeyStoreException) {
        null
    }

    @get:Synchronized
    private val systemTrustManager: Lazy<X509TrustManagerExtensions?> =
        makeLazyTrustManager(systemKeystore)

    @JvmStatic
    @Synchronized
    private fun verifyCertificateChain(
        @Suppress("UNUSED_PARAMETER") context: Context,
        serverName: String,
        authMethod: String,
        allowedEkus: Array<String>,
        @Suppress("UNUSED_PARAMETER") ocspResponse: ByteArray?,
        time: Long,
        certChain: Array<ByteArray>,
    ): VerificationResult {
        // Convert the array of (supposedly) DER bytes into certificates.
        val certificateChain = mutableListOf<X509Certificate>()
        certChain.forEach { certBytes ->
            val certificate = try {
                certFactory.generateCertificate(ByteArrayInputStream(certBytes))
            } catch (e: CertificateException) {
                return VerificationResult(StatusCode.InvalidEncoding)
            }
            certificateChain.add(certificate as X509Certificate)
        }

        // Will never throw `ArrayIndexOutOfBoundsException` because `rustls`'s `ServerCertVerifier` trait
        // has a mandatory `end_entity` parameter in `verify_server_cert`.
        val endEntity = certificateChain.firstOrNull() ?: return VerificationResult(StatusCode.InvalidEncoding)

        // Check that the certificate is valid at the point of time provided by `rustls`.
        try {
            endEntity.checkValidity(Date(time))
        } catch (e: CertificateExpiredException) {
            return VerificationResult(StatusCode.Expired)
        } catch (e: CertificateNotYetValidException) {
            return VerificationResult(StatusCode.Expired)
        }

        // Check that this certificate can be used in a TLS server.
        if (!verifyCertUsage(endEntity, allowedEkus)) {
            return VerificationResult(StatusCode.InvalidExtension)
        }

        // Select the trust manager to use.
        //
        // We select them as follows:
        // - If built for release, only use the system trust manager. This should let all test-related
        // code be optimized out.
        // - If built for tests:
        //      - If the mock CA store has any values, use the mock trust manager.
        //      - Otherwise, use the system trust manager.
        val (trustManager, _) = if (!BuildConfig.TEST) {
            val trustManager =
                systemTrustManager.value ?: return VerificationResult(StatusCode.Unavailable)
            Pair(trustManager, systemKeystore)
        } else {
            if (mockKeystore.size() != 0) {
                val trustManager = mockTrustManager.value!!
                Pair(trustManager, mockKeystore)
            } else {
                val trustManager =
                    systemTrustManager.value ?: return VerificationResult(StatusCode.Unavailable)
                Pair(trustManager, systemKeystore)
            }
        }

        // Verify that the certificate chain is valid and correct, and nothing more.
        //
        // NOTE: This does not validate `serverName` is valid for the end-entity certificate.
        // That is handled in Rust as Android/Java do not currently provide a RFC 6125 compliant
        // hostname verifier. Additionally, even the RFC 2818 verifier is not available until API 24.
        //
        // `serverName` is only used for pinning/CT requirements.
        //
        // Returns the "the properly ordered chain used for verification as a list of X509Certificates.",
        // meaning a list from end-entity certificate to trust-anchor.
        try {
            trustManager.checkServerTrusted(certificateChain.toTypedArray(), authMethod, serverName)
        } catch (e: CertificateException) {
            // In test configurations we may see `checkServerTrusted` fail once vendored test
            // certificates pass their expiry date. We try to avoid that by using a fixed
            // verification time when calling `endEntity.checkValidity` above, however we can't
            // fix the time for the `checkServerTrusted` call.
            //
            // To make diagnosing CI test failures easier we try to find the root cause of
            // checkServerTrusted failing, returning a different `StatusCode` as appropriate.
            if (BuildConfig.TEST) {
                var rootCause: Throwable? = e
                while (rootCause?.cause != null && rootCause.cause != rootCause) {
                    rootCause = rootCause.cause
                }
                return when (rootCause) {
                    is CertificateExpiredException, is CertificateNotYetValidException -> VerificationResult(
                        StatusCode.Expired,
                        rootCause.toString(),
                    )

                    else -> VerificationResult(StatusCode.UnknownCert, rootCause.toString())
                }
            }
            // In non-test configurations we should have caught expiry errors earlier and
            // can simply return an unknown cert error without digging through the exception
            // cause chain.
            return VerificationResult(StatusCode.UnknownCert, e.toString())
        } catch (_: RuntimeException) {
            return VerificationResult(StatusCode.Unavailable, "Android trust verifier unavailable")
        }

        // Android trust validation above and Rust hostname validation remain enabled.
        // Do not add a PKIX revocation checker here: Android can synchronously fetch
        // OCSP/CRL URLs, including on the JNI caller's thread, without a bounded wait.
        return VerificationResult(StatusCode.Ok)
    }

    private fun verifyCertUsage(certificate: X509Certificate, allowedEkus: Array<String>): Boolean {
        val ekus = try {
            certificate.extendedKeyUsage
        }
        // This should be unreachable, but could happen.
        catch (_: CertificateParsingException) {
            return false
        } catch (_: NullPointerException) {
            // According to Chromium's implementation, this can crash when the EKU data is malformed.
            Timber.tag(TAG).w("exception handling certificate EKU")
            return false
        } ?: return true // If the list is empty, we have nothing to do.

        return ekus.any { allowedEkus.contains(it) }
    }

}
