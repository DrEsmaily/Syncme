# Offline Android TLS verifier

Source: rustls/rustls-platform-verifier at
`8becdc1beaee80afee396a8c769d42fe9bd9cb4e`, compatible with Android verifier 0.2.0.
Upstream MIT and Apache-2.0 licenses are retained here.

Only online OCSP/CRL fetching and unused root-cache helpers are removed. Android
chain, date and usage validation and Rust hostname validation remain enabled.
The JNI class names, method descriptor, result fields and numeric status codes
must remain compatible with the pinned Matrix Rust SDK. Consumer rules preserve
them in optimized APKs. Tests invoke the private JNI method by its full signature.

Do not replace this module with the Maven verifier without reconsidering external
revocation fetching. Do not restore the permissive Matrix/OkHttp experiments.
