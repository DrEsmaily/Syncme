# SyncMe 1.1.0 — Build 255

SyncMe 1.1.0 (Build 255) adds hour-level last-seen labels and a long-time-ago label after one year. It builds on the verified SyncMe 1.0.0 (Build 254) source, including the settings cleanup and earlier presence/receipt fixes.

## Release identity

- Product/release version: **1.1.0** (previous release: **1.0.0**, Build 254).
- Build number: **255**.
- Canonical release tag: `v1.1.0-build255`.
- Android APK `versionName`: `255`; ARM64 `versionCode`: `202610222`. These are Android package identifiers, separate from the product release version above.
- Exact APK source commit: `86bdd79687680da17dc7cbd6011e69cd828731aa`.
- [Successful build and regression checks](https://github.com/DrEsmaily/SyncMe/actions/runs/39000390352).
- APK SHA-256: `a916f9c4511394ebe6229f4181099fa4f3609379f1d4981cf85ac7d0255db9e4`.

## Display rules

All labels are English. Durations use elapsed time since the last activity timestamp returned by the Matrix server, not calendar-date boundaries or the time the screen was opened. Counts use completed units; singular and plural forms agree with the count.

| Elapsed time | Display | Example |
|---|---|---|
| Less than 1 hour | `Last seen recently` | 45 minutes |
| 1 hour to less than 24 hours | `Last seen N hour(s) ago` | 5 hours 30 minutes → `Last seen 5 hours ago` |
| 1 day to less than 7 days | `Last seen N day(s) ago` | 3 days 12 hours → `Last seen 3 days ago` |
| 7 days to less than 30 days | `Last seen N week(s) ago` | 18 days → `Last seen 2 weeks ago` |
| 30 days to less than 365 days | `Last seen N month(s) ago` | 75 days → `Last seen 2 months ago` |
| 365 days or more | `Last seen long time ago` | Exactly 365 days |

A day means 24 hours, a week means 7 days, a month means 30 days, and the cutoff for one year is 365 days. Thus 364 days displays `Last seen 12 months ago`; 365 days switches to the final label. Exactly 1 hour, 1 day, 7 days and 30 days enter the next unit. Partial units round down.

Missing, nonpositive or future server timestamps show no last-seen text. Offline users have no status dot. Online and Away keep their existing indicators. Server presence describes recent activity reported by the server, not proof that a person is looking at the screen.

## Scope and validation

The production change is confined to the shared last-seen formatter and its Compose previews. Presence transport/publication, polling, account storage, login, encryption, notifications and receipt handling are not modified. Existing settings cleanup is retained: About and its divider are hidden, the footer omits the diagnostics suffix, and the SyncMe account-provider default stays blank.

Boundary tests cover zero elapsed time, one millisecond below and exactly at hour/day/week/month/year transitions, singular/plural counts, partial units, invalid timestamps and durations over a year. The workflow also runs the established settings/login, presence, room/user presenters, receipts, TLS, reset and Firebase regression checks before building the APK.

The release workflow validates the APK ZIP, Android version name `255`, version code `202610222`, existing `.syncmerelease` application ID, ARM64 ABI and retained JNI verifier class; it records the exact source commit, Actions run and SHA-256. It publishes a public release only after checks and packaging succeed. Automated checks do not replace physical two-phone validation; device testing is not claimed by this release.

## Downloads and provenance

- [Direct ARM64 APK](https://github.com/DrEsmaily/SyncMe/releases/download/v1.1.0-build255/SyncMe-255-ARM64.apk)
- [Release and attached SHA-256/provenance files](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.1.0-build255)
- [Build workflow](https://github.com/DrEsmaily/SyncMe/actions/workflows/syncme-last-seen.yml)
- [Source branch](https://github.com/DrEsmaily/SyncMe/tree/syncme-255-last-seen)

The APK has been published following successful regression, build and package checks. The APK targets ARM64 Android devices, Android 7.0 or later. Install it as an update over the existing SyncMe release; no account reset or data clearing is required.
