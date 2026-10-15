# SyncMe Build 235 — Refined Network & TLS Diagnostics

## Identifiers
- **In-app version footer:** `SyncMe by 0x07c4 • Build 235 • Network Diagnostics`
- **Android versionName:** `235`
- **Android base versionCode:** `20260905` (ABI-specific ARM64 versionCode: `202609052`)
- **Target CPU architecture:** `arm64-v8a`
- **Exact APK filename:** `SyncMe-235-ARM64-TLS-Audit-Refined.apk`
- **Release notes attachment:** `SyncMe-235-Build-Notes.md`
- **Checksum attachment:** `SyncMe-235-SHA256.txt`
- **GitHub Actions artifact name:** `SyncMe-235-ARM64-Refined-TLS-Diagnostics`
- **Source baseline:** successful Build 234 Complete Diagnostics branch, original baseline 234 source `836883110d1dbeb1f4463004bcd825658c4f9f86`.

## Changes from Build 234
1. **Real build numbering:** Set Android versionName to 235, increase versionCode over the former 234 experiment, and display the new number plus edition label in the Settings footer. This is the version printed inside the installed APK, not only in the GitHub job title.
2. **Safer Rust trace classification:** `RUST_REPORTED_NETWORK_ERROR` now requires a network-context event and structured WARN/ERROR-level indication; textual mentions of "error" on ordinary DEBUG lines are not automatically counted as network failures.
3. **Accurate counts:** `candidate_trace_lines` is explicitly a count of **trace lines, not requests, failures or retries**. Do not assume multiple Rust DEBUG messages represent different connections.
4. **Aggregated diagnostics:** Repeated Rust events are grouped by event type, trace target, observed host and HTTP status. Each group has a count plus at most one sanitized example to reduce noise and avoid the earlier 350-line truncation.
5. **Explicit destination uncertainty:** Host and IP strings parsed out of Rust log text are marked `observed_host`/`observed_ip`. They are not verified socket destinations. Kotlin OkHttp instrumentation still reports its directly observed hosts.
6. **HTTP status clues:** When Rust tracing emits a structured numeric `status`, `status_code` or `http_status`, the exported category distinguishes 4xx/5xx from ordinary network traces.
7. **Persistent native-log clearing:** "Clear TLS Logs" clears Kotlin TLS files and stores a UTC cutoff. Subsequent exports ignore Rust traces before that cutoff (including after app restart), without deleting or truncating Rust files that the native SDK is actively writing.
8. **Complete build packaging:** Each run includes precisely named APK, these build notes, a checksum file and the workflow's Markdown job summary, including the commit SHA.

## Intentionally unchanged
- **Matrix Rust:** `ClientBuilder().disableSslVerification()` remains enabled in the experimental compatibility build.
- **OkHttp:** The intentionally permissive certificate trust manager and hostname verifier remain enabled.
- **Transport:** TLS encryption is still used, but **server authenticity is not verified** on those two paths; this is vulnerable to active MITM. No CA pinning, private root certificate or TLS-security restoration is part of this change.
- No changes to Synapse, Traefik, Nextcloud, LiveKit, TURN, message formats, push notification service or account management.

## Existing diagnostic entry points
Open **Settings** → tap the version five times → **Developer Options** → **SyncMe diagnostics**:
- `Startup diagnostics`: view/copy startup log.
- `SSL / TLS audit log`: view/copy network diagnostics; Clear TLS Logs.

The TLS audit reports Kotlin/OkHttp events, Matrix Rust SDK trace-derived metadata and 5-second app-UID traffic byte deltas. TrafficStats does **not** report destination IPs. WebView and uninstrumented native activity may not be attributable.

## Build-time automated checks
- Verify disabled SSL source markers still exist, diagnostic options are in Developer Options rather than general Settings, and revised classifier/cutoff code is present.
- Compile Android ARM64 with JDK 21 and a separate application-ID suffix `.syncmerelease`.
- Check APK is a valid ZIP and compute SHA-256.
- Inspect versionName/versionCode via Android build outputs where available, and publish uniquely named artifacts.

## Required manual Android tests
1. Install ARM64 APK and confirm Settings footer says **Build 235**.
2. Open both diagnostic screens; verify Copy All works without UI lag.
3. Press Clear TLS Logs, restart and verify **old Rust events do not reappear**.
4. Sign in/restore session, open chats, send/receive small and large media, and test notifications/calls.
5. Confirm unusually frequent `RUST_REPORTED_NETWORK_ERROR` groups with complete request context before treating them as real connection failures.
6. Test under blocked international networking with direct access to `syncme.ir`; use an independent network capture to verify *absence* of external requests.

## Known limitations
- No physical Android device or emulator runtime test is performed by this GitHub build workflow. Compile success is not functional verification.
- Rust log parser is heuristic; some HTTP statuses or true errors without structured metadata may not be classified.
- Counts are counts of log **lines**, not actual requests, and an observed host in log text is **not** validated as the peer.
- SDK-native logging, OS services, WebView and background components may generate traffic not fully reflected by any one in-app source.
- Diagnostic-level logging adds runtime and storage overhead; use the production log level when diagnostics are no longer needed.
- The persistent "clear" cutoff deliberately does not erase SDK files from disk; it excludes old traces from the TLS audit export only.

## Provenance
Exact GitHub Actions run, complete commit hash, APK file size and SHA-256 are appended to the release notes during artifact packaging.
