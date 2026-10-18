# SyncMe build 257: combined fixes

This optimized ARM64 GplayRelease APK combines both changes on build 256:

- Prevent manual scrolling near the live end from jumping to the latest message without a new event. Preserve incoming-message following, own-message scrolling and read updates.
- Render stored PNG/JPEG avatar thumbnails immediately and publish other accounts with their saved expanded/collapsed setting. Preserve Coil decoding for animated and vector formats.
- Increase both the aggregate media cache budget and the per-file admission limit to 5 GiB per account. Preserve 30-day last-access expiry and daily cleanup. Android and explicit cache clearing can still remove media; the SDK can temporarily exceed its budget between cleanups.

Fix commits: `ad9aee78dec1d7f31afddd69f63c642a7bd6231c` and `33f0d7cb684075ee9591ba062decdc62f165d0f4`.

Build identity: versionName 257, ARM64 versionCode 202610242, application ID `ir.syncme.app.syncmerelease`. The APK must verify with the same signing certificate as the published build 256.

The workflow runs the combined timeline, preferences, avatar and client-factory regressions before assembling the optimized APK. It checks the actual APK metadata, OAuth scheme, TLS JNI verifier class, signature and upgrade version code, and uploads checksums and exact source provenance.

Physical-device UI behavior and multi-gigabyte media reuse are not verified by compilation or unit tests. This artifact still requires runtime validation on the user's phone.
