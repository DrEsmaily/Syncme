# SyncMe 234 — Complete Diagnostics / ARM64

## Purpose
This experimental build retains the working SyncMe 234 baseline and adds local diagnostics to investigate certificate validation and network dependencies during loss of international connectivity.

## Changes compared with original build 234
1. Matrix Rust SDK: `ClientBuilder().disableSslVerification()` remains enabled to prevent certificate-verification failures in the experimental compatibility build.
2. Kotlin OkHttp: permissive trust manager/hostname verifier remain enabled for this experimental build, with DNS, TLS handshake, connection failure, destination host/IP metadata and call-origin instrumentation.
3. Dedicated SSL / TLS audit: Copy All and Clear TLS Logs from Settings > Developer Options > SyncMe diagnostics > SSL / TLS audit log.
4. Native Matrix SDK: targeted HTTP/TLS tracing is enabled to the Rust SDK's local file writer; the SSL / TLS audit scans bounded tails and exports redacted network-event summaries rather than raw log messages.
5. Startup diagnostics: moved from the normal Settings menu to Settings > Developer Options > SyncMe diagnostics > Startup diagnostics; existing viewer and copy functionality retained.
6. Five taps on Settings' version label unlock Developer Options.

## Security and connectivity behavior
- TLS encryption remains in use, but certificate chain and hostname authenticity verification were intentionally bypassed in Matrix Rust and Kotlin OkHttp. An active network adversary can impersonate a server and compromise confidentiality/integrity of traffic and account tokens.
- This is **not** an offline private-CA trust build. No ten-year certificate or pinned key is embedded; this is a diagnostic-only experimental build.
- Native Rust tracing and Android StrictMode are not packet capture. Lack of events **does not prove** there are no external OCSP/CRL/AIA, WebView, OS-level, or other uninstrumented network requests.
- The reports summarize host/IP metadata and sanitized event categories; raw URLs, access tokens, room/message content and HTTP bodies are excluded by design.
- No intentional changes to the Synapse, Traefik, LiveKit, Nextcloud or TURN server configuration.

## Verification
- CI checks source markers for bypass and both diagnostic menus.
- CI compiles `:app:assembleGplayRelease` on JDK 21, verifies the ARM64 APK ZIP container and prints SHA-256.
- **Not verified in CI:** physical-device runtime behavior (POCO F3/F5), ability to observe every native connection, offline national-intranet conditions, notifications, calls, upload speed, and actual certificate validation.
- The Android package is built with `.syncmerelease` application-ID suffix for experimental parallel installation.

## How to test
1. Install the ARM64 APK; open SyncMe and restore or sign in to an account.
2. Tap the version five times in Settings, open Developer Options > SyncMe diagnostics.
3. Clear TLS logs, force-stop/restart, open chats, send a message and a small file, then copy the SSL/TLS audit and Startup diagnostics independently.
4. Check the `RUST_NATIVE_NETWORK_TRACING` section; note any `RUST_NATIVE_TRACE_UNAVAILABLE` or suspicious external host records.
5. Test with a connection to your own server while international destinations are blocked (DNS and routing included), and check logs plus app behavior.
6. Do not interpret empty logs as proof of zero external traffic; independent VPN/pcap-based observation is required for that claim.

## Known limitations / follow-up
- Most native Rust and WebView network paths are not instrumented with per-packet destinations. Available SDK tracing coverage depends on the SDK version and its compiled targets.
- TLS audit sampling limits repeated Kotlin events; Rust trace summaries are bounded for performance and privacy.
- Private-CA secure offline mode is a *future, separately tested feature*, not present in this build.
- This build must not be distributed as a secure replacement for verified TLS.

## Provenance
- Original SyncMe 234 source commit: `836883110d1dbeb1f4463004bcd825658c4f9f86`.
- Original certificate-bypass experiment built separately.
- Rust SDK line declared in the project build: `26.10.05-nightly`.
- Exact experiment SHA, workflow run, APK checksum, artifact name and CI outcome are appended during build.
