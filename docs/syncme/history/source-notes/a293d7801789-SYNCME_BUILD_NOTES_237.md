# SyncMe Build 237 — Blank Server Field Fix

## Build identity
- **Settings footer:** `SyncMe by 0x07c4 • Build 237 • Optional Diagnostics`
- **Android versionName:** `237`
- **Base versionCode:** `20260907`; **ARM64 versionCode:** `202609072`
- **Target:** Android ARM64 (`arm64-v8a`)
- **Exact APK file:** `SyncMe-237-ARM64-Blank-Server-Optional-Diagnostics.apk`
- **GitHub artifact:** `SyncMe-237-ARM64-Blank-Server-Optional-Diagnostics`
- **Release notes:** `SyncMe-237-Build-Notes.md`
- **SHA-256:** `SyncMe-237-SHA256.txt`
- **Validation report:** `SyncMe-237-APK-Validation.txt`

## Important fix compared with Build 236
The original Build 236 account-provider screen only removed the implicit `syncme.ir` value when the server-history list was empty. When `syncme.ir` was already in remembered history, the screen continued to prefill it.

Build 237 **always renders an implicitly selected `syncme.ir` provider as an empty editable field**, independently of history, including app installations with a prior `syncme.ir` login.

- Typing `syncme.ir` manually still works, is displayed exactly as entered, and can be submitted.
- Other explicitly configured providers and the server history remain intact.
- History remains available as autocomplete when the user begins typing.
- Sign-in server validation and authentication behavior remain unchanged.
- A regression test was added for `syncme.ir` in saved history and subsequent explicit typing.

## Preserved from Build 236
1. Removed the right-side arrow from built-in Favorite tag and tag preview rows. Navigation and tag functionality unchanged.
2. In Developer Options > SyncMe diagnostics, independent **Enable startup logs** and **Enable SSL / TLS logs** switches are retained. Both default OFF, and persist.
3. The Startup report and TLS audit viewers, Copy All, and clearing behavior remain in Developer Options.
4. Native Rust SDK tracing requires a full app restart to follow the TLS toggle. The in-app dedicated audit switches immediately.
5. Refined Rust trace labeling: distinguish true WARN/ERROR log events from generic DEBUG mentions of 'error'; group repetitive event logs, report hints without claiming trace-line counts are connection counts.
6. Five-second UID-level byte sampler runs only when TLS audit is enabled, and cannot reveal destination IPs.
7. Matrix Rust SDK TLS verification remains disabled in the experimental compatibility build; Kotlin OkHttp uses permissive verification. **TLS is encrypted but server authenticity is not checked on those paths.** No SSL/CA/server changes made.
8. Startup logs are opt-in to avoid unnecessary diagnostic overhead.

## How to verify
1. Install the named ARM64 APK; confirm Settings footer shows **Build 237**.
2. On the Account provider entry screen, check the input is **blank**, even on devices where `syncme.ir` was previously used.
3. Type `syncme.ir` and confirm it remains visible and Continue initiates the expected login flow.
4. Check Favorite tag and Create tag preview rows show no trailing arrow.
5. Open Settings > tap version five times > Developer Options; confirm both logging switches are OFF by default and can be independently enabled.
6. With both toggles off, observe normal chat startup and performance; test logging only when collecting a diagnostic report.
7. Test login, chats, media, and notifications on POCO F3 and F5. GitHub Actions compilation alone does not verify runtime UI and network stability.

## CI verification
- Require the corrected login-input expression in source and reject the original incorrect `homeserverHistory.isEmpty()` predicate.
- Run unit regression test targeting the account-provider screen.
- Build ARM64 release APK on JDK 21; check actual APK manifest versionName / versionCode; validate archive and compute SHA-256.
- Attach exact APK, notes, hash and CI verification summary.

## Known limitations
- Existing credentials/Matrix sessions are not modified, and Android previously saved UI state can persist if a live login screen is restored from an old task. Testing a freshly opened login screen is recommended.
- Build 237 still deliberately skips TLS certificate authenticity checks for Matrix Rust SDK and Kotlin OkHttp: an active MITM attacker may impersonate a server.
- No physical-device testing is possible inside GitHub Actions; no claims of real-device success until separately tested.
- Network tracing is selective, not complete network packet capture.

## Provenance
The build step appends the exact source SHA, GitHub run, APK size, checksum and test results to the attached notes.
