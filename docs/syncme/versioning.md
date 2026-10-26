# SyncMe versioning

`syncme-release.properties` is the canonical release identity. CI loads the same values used by Android builds.

- `version` follows Semantic Versioning: MAJOR.MINOR.PATCH. Increment PATCH for backward-compatible fixes, MINOR for backward-compatible features, and MAJOR for breaking changes. Reset lower components when incrementing a higher component.
- Android `versionName`, the GitHub release version and updater JSON `version` must match exactly for stable releases.
- `build` is a separate increasing build number, shown in Settings and used in artifact names. It is not the public version name.
- `versionCodeBase` is an opaque increasing Android package identifier, not a date. Preserve the existing ABI suffix scheme; ARM64 receives base × 10 + 2. Never reuse or decrease a published code, and keep the final result within Android's supported limit.

Build 270 is the first release using aligned semantic Android version names: 1.1.14, Build 270, ARM64 versionCode 202611062.
Historical releases keep their original tags, names, manifests and downloads. The switch from numeric versionName does not reset application data because application ID, signing identity and increasing versionCode remain unchanged.

For each release, update the properties, select the correct semantic increment from the actual changes, and run the release workflow. Verification rejects mismatched package metadata, release version and update JSON.
The existing server fetcher recognizes the canonical APK and generated latest.json assets automatically after successful publication; no manual upload or new server setup is required.
