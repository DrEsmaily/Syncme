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
