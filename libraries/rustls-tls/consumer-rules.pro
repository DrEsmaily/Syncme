# Rust accesses these names and members through JNI, including private result fields.
-keep class org.rustls.platformverifier.CertificateVerifier { *; }
-keep class org.rustls.platformverifier.VerificationResult { *; }
-keep class org.rustls.platformverifier.StatusCode { *; }
