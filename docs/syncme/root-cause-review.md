# SyncMe startup, identity reset and push review

Base: `eb8042a1e3` (`syncme-244-fcm-registration-race-20261012`).
Review date: 2026-10-12. 321 distinct Actions run records inspected: 203 successful,
50 failed, 67 cancelled and one running at the initial snapshot. These are build
results, not 321 device runtime tests.

## Findings supported by source/history

1. **External certificate checks were reintroduced upstream.** Commit
   `03fd288c47` replaced Element's local verifier (which had removed online
   revocation in `f62030ac5b`) with rustls-platform-verifier Android 0.2.0.
   Its PKIXRevocationChecker can synchronously contact OCSP/CRL servers inside
   the JNI TLS callback. SOFT_FAIL permits some failures; it does not make a
   blocked network call asynchronous or impose a short timeout. Earlier SyncMe
   changes moved ignored-users, capabilities and thread-list work off the main
   thread, but do not remove this external dependency.
2. **The 234 offline-verifier experiment existed only as CI source rewriting.**
   `ee68506df1` and follow-ups downloaded, patched and repackaged a verifier AAR
   on the runner. That dependency replacement was not an ordinary checked-in
   application module. The later 234/236/243/244 lineage instead calls
   `disableSslVerification()` and installs a permissive OkHttp trust manager and
   hostname verifier. This removes certificate authentication altogether.
3. **Identity reset completion was coupled to an independent backup state.**
   ResetIdentityPasswordView holds its progress dialog after SDK Success;
   ResetIdentityFlowManager dismisses it only when backup becomes ENABLED.
   RustEncryptionService additionally masks backup state as WAITING_FOR_SYNC
   unless sync is Running. A successful identity reset therefore has a
   deterministic path to a permanent spinner, including when automatic backup
   activation is disabled for the account. This is independent of Firebase.
   The 243 follow-up `eaa89a8893` only tries enableBackups when the sampled state
   is UNKNOWN and no server backup exists. It can skip WAITING_FOR_SYNC and can
   override intentionally disabled backups. It is not a reliable completion
   criterion. The 239 alternate branch briefly changed completion, but that
   branch is not an ancestor of 244.
4. **Firebase 244 is a bounded token wait, not evidence of repaired Firebase.**
   `9c277b5956` waits up to 20 seconds for the existing store. A cached blank
   token bypasses that wait; a finite flow with no usable token throws from
   first() instead of returning Result.failure (including the existing fake).
   The original Firebase token callback already stores late tokens and
   registers them for sessions whose selected provider is Firebase.
   No source evidence establishes a Firebase project, credential, gateway or
   Google Play Services failure on the user's device. Those remain unverified.
5. **Build provenance was misleading.** The workflow titled 244 still sets
   SYNCME_BUILD_NUMBER=243 and publishes a 243 filename, notes and manifest.
   Green runs mostly assemble APKs and match source strings; they do not test
   the reported runtime flow. Reset diagnostics were also forced to DEBUG and
   disk even when the diagnostic preference was off.

## Changes in this branch

- Use a checked-in Android verifier module based on the exact 0.2.0-compatible
  source at rustls/rustls-platform-verifier commit
  `8becdc1beaee80afee396a8c769d42fe9bd9cb4e`. Remove the online revocation
  checker and its unused root-cache helpers, preserve JNI signatures/status
  codes and consumer keep rules, serialize certificate-factory access, and
  reject malformed chains and trust-provider errors. System trust, expiry and
  EKU checks remain; the Rust verifier continues to check hostnames. Restore
  standard OkHttp authentication. Remove the CRL-specific cleartext allowlist.
- Complete the reset flow only on SDK success (password, OAuth or the no-auth
  return), not on backup-state observation. Do not claim backup is enabled and
  do not force creation/replacement of a backup. SDK errors remain errors.
  Reject duplicate password-submit events; replace old completion observers.
- Keep the existing bounded FCM wait, reject blank cached/emitted tokens, return
  a failure for an exhausted flow, and preserve cancellation. No Firebase
  credentials, app resource values, push gateway or token deletion changes.
- Make native reset tracing and reset-event writes obey the startup diagnostic
  preference again. Native configuration changes take effect after restart.
- Build 245 from one named branch, run regression tests before packaging, and
  verify the actual APK manifest/version/ABI/JNI class. Record exact commit,
  SHA-256 and CI URL with the artifact. Preserve the existing .syncmerelease
  application ID so this candidate does not introduce another package change.

Disabling **online revocation** means revoked-but-otherwise-valid certificates
are not rejected on the basis of a freshly fetched OCSP/CRL response. This is a
specific tradeoff requested to eliminate external revocation requests, not a
claim that all possible TLS/network delays are eliminated.

## Validation and limits

Regression coverage: success without backup activation, no-auth reset,
replacement/cancellation of completion observers, password errors and duplicate
submissions, late/blank/missing FCM tokens, completed flows and cancellation;
JNI result codes for valid/malformed/expired/wrong-EKU/untrusted certificates and
trust-provider failure. Verifier tests stub Android's trust manager to isolate
the offline-verification policy; they do not replace a real device TLS test.

The local environment has a Java runtime but no Java compiler or Android SDK.
The initial local Gradle attempt reached configuration and stopped at the
missing JAVA_COMPILER capability. The dedicated GitHub workflow supplies JDK 21
and the Android toolchain; its result must be checked before recommending APK
installation.

Still requires device confirmation: first launch on the affected network,
trusted TLS and rejection of wrong-host/untrusted TLS, identity reset with
backup intentionally disabled, and end-to-end background notification delivery.
No device trace was supplied, so the source-level regressions above must not be
presented as proof of every symptom on that device. Existing encrypted sessions
and server-side backup data are not deleted by this patch.
