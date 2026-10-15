# SyncMe Build 236 — Tag UI, Blank First-Login Server, Opt-In Diagnostics

## Exact release identity
- **Settings footer**: `SyncMe by 0x07c4 • Build 236 • Optional Diagnostics`
- **Android versionName**: `236`
- **Android base versionCode**: `20260906`, ABI-specific ARM64 APK versionCode `202609062`
- **CPU**: Android ARM64 / `arm64-v8a`
- **APK filename**: `SyncMe-236-ARM64-Tags-Login-Optional-Diagnostics.apk`
- **GitHub artifact**: `SyncMe-236-ARM64-Tags-Login-Optional-Diagnostics`
- **Release notes**: `SyncMe-236-Build-Notes.md`
- **Checksum**: `SyncMe-236-SHA256.txt`
- **Build verification**: `SyncMe-236-APK-Validation.txt`

## Changes relative to successful experimental Build 235
1. **Tag manager chevrons removed.** The shared tag manager row no longer displays a right-facing chevron (`›`). This removes the chevrons circled in the supplied screenshots, on the built-in Favorite row and the tag creation/rename preview. Row taps and tag functionality are preserved.
2. **Empty first-login homeserver field.** On first setup with no prior homeserver history, the editable account provider input is blank rather than displaying `syncme.ir`. Stored server history, enterprise-enforced providers, user-entered values, Matrix server selection and the actual sign-in process are left unchanged. Nothing is automatically connected merely to show the blank field.
3. **Independent Developer Options switches.** Under Settings > five taps on the version > Developer Options > SyncMe diagnostics: `Enable startup logs` and `Enable SSL / TLS logs`, independently persisted and OFF by default.
4. **Startup logging truly opt-in.** With startup logs OFF, its in-app watchdog thread and dedicated file writes are not started. Enabling it starts capture, and disabling stops future recording.
5. **TLS logging truly opt-in.** With TLS logs OFF, no dedicated TLS audit files are written and the five-second app-UID traffic sampler is not started; enabling the toggle activates dedicated Kotlin-layer recording. Detailed Rust HTTP tracing targets, native disk writer and cleartext StrictMode hooks follow the persisted setting on the next full app launch. **Restart SyncMe after turning TLS logging on or off** for the native tracing policy to match the switch.
6. **Native log clearing maintained.** Native SDK files are not unsafely truncated while open; a persistent cutoff hides earlier Rust events after the `Clear TLS Logs` action even when TLS capture is disabled.
7. **Previous refined classifier retained.** Rust summary differentiates reported ERROR/WARN events, HTTP status clues, repeats and heuristically extracted hosts without treating every DEBUG line mentioning `error` as a genuine failed connection.
8. **Improved release discipline.** Number shown inside Settings, APK package version metadata, artifact title and exact filename match Build 236. Full descriptions, APK SHA-256 and a validation note are bundled.

## Intentionally unchanged
- Matrix Rust SDK uses `ClientBuilder().disableSslVerification()`.
- Kotlin OkHttp continues using the previously established permissive trust manager and hostname verifier.
- TLS transport remains encrypted but **server identity is NOT validated** in those stacks. This experimental compatibility approach is exposed to active man-in-the-middle attacks. Do not advertise it as secure.
- No changes to server config, Synapse, Matrix ID, Nextcloud, TURN, LiveKit, room storage, message send logic, upload code, account system, tag limits or tag behavior.
- No private CA, OCSP, CRL, AIA or certificate-pinning changes.

## In-app test procedure
1. Install the ARM64 APK. Check the last Settings line exactly shows `Build 236 • Optional Diagnostics`.
2. Open Chats > Tags > Manage tags; verify Favorite row has no right arrow. Tap Create new tag; verify the preview also has no right arrow. Test normal navigation and tag creation.
3. On a fresh installation with **no saved homeserver history**, open the login server field; it must be blank. Type `syncme.ir` and verify that login still works. An existing install may appropriately display a previously used server.
4. Open Developer Options (tap Settings version 5 times). Verify both diagnostic switches start OFF and remain OFF after process restart.
5. Turn ON only Startup: reproduce a chat-opening stall and verify startup log captures it. Turn it OFF: no new startup entries.
6. Turn ON only SSL/TLS; fully restart app; verify Rust network summaries and UID byte counters appear. Turn it OFF and restart: no fresh SDK trace file writer or UID sampler should run.
7. Clear TLS logs then export; prior native events should remain excluded.
8. Test POCO F3 and F5, login, chat transitions, media send, notifications and local-network-only service availability. CI does **not** substitute for these physical-device tests.

## Known limitations
- The developer switches control **the dedicated SyncMe diagnostics**; Android OS logs and emergency crash telemetry outside these two facilities may still exist.
- The native Rust log-level change needs an application process restart; toggling without restart changes the dedicated Kotlin capture immediately, but not the already-initialized Rust tracing subscriber.
- Startup capture itself adds some overhead when ON; default OFF avoids the dedicated watchdog workload.
- Only SDK-emitted Rust data is summarized. App-UID traffic counters do not reveal destinations and do not prove that outside requests are absent.
- Rust event summaries are heuristic; log lines are not proof of separate network calls.
- APK compilation/ZIP validation does not certify UI correctness or network performance on actual phones.

## Build provenance
GitHub Actions appends the precise commit SHA, run ID, APK SHA-256 hash, output size and validation outcome to the bundled notes.

## Build-specific provenance and results

- UTC date: 2026-10-12T18:14:51Z
- GitHub run: https://github.com/DrEsmaily/element-x-android/actions/runs/38287593658
- Run attempt: 1
- Branch: syncme-244-fcm-registration-race-20261012
- Exact source commit: eb8042a1e353fc88913108db0f902d4353ca9167
- CI verification: Gradle compilation and APK ZIP container passed
- Device runtime verification: NOT PERFORMED in this workflow

### APK hash and size

Build 243 expected Android versionName: 243
Build 243 expected ARM64 versionCode: 202609102
Source APK: app/build/outputs/apk/gplay/release/app-gplay-arm64-v8a-release.apk
-rw-r--r-- 1 runner runner 71M Oct 12 18:14 build-checkpoint/SyncMe-243-ARM64-Reset-Full-Diagnostics.apk
f46e5044d166cf5ebe585f45c53a15b48e1975a7f1963a89109e37a2e9983c13  build-checkpoint/SyncMe-243-ARM64-Reset-Full-Diagnostics.apk

### Recent source changes

- eb8042a1e3 Build isolated SyncMe 244 FCM registration race fix
- 9c277b5956 Wait for initial FCM token before registering Matrix pusher
- eaa89a8893 Attempt guarded backup initialization after successful identity reset when no server backup exists
- f49bdb2b26 Build 243 ARM64 native Rust recovery diagnostic variant
- 20352fbb9b Include bounded privacy-safe Rust crypto and recovery trace summary in copyable reset diagnostics
- 60e181dbfc Capture native Matrix Rust crypto and recovery traces in bounded cache files for diagnostic build
- 914b974c87 Fix Kotlin coroutine inference failure in reset diagnostics
- 86b61c09e3 Fix reset diagnostic suspend lambda return type Unit
- 41f09c366f Fix nested coroutine receiver resolution in reset diagnostic presenter
- 9662bf6328 Require session lifecycle traces in final diagnostic APK
- a033dc0c78 Trace first-login and restored session creation stages
- 0f94b8992c Build 242 isolated ARM64 reset root cause diagnostics
- cb0c187973 Inject encryption service for reset read-only backup diagnosis
- 193fe5ccae Capture read-only server backup existence after identity reset
- de8ef1233f Record bounded backup and recovery heartbeat while reset waits
- 076762c687 Record Rust identity reset handle lifecycle without credentials
- 22140750ee Trace encryption reset, backup state and explicit backup calls
- 34ee2c04cb Correct workflow push branch to launch SyncMe 240
- bd7bdba607 Build SyncMe 240 safe reset diagnostics without bypassing backup gate
- fdd42fc232 Fix newline delimiters in reset diagnostic export
