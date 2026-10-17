<p align="center"><img src="https://raw.githubusercontent.com/DrEsmaily/SyncMe/syncme-cumulative-build-20261015/appicon/element/src/main/res/drawable-nodpi/syncme_launcher.webp" alt="SyncMe app icon" width="144" /></p>

# SyncMe for Android

**Version 1.1.0 · Build 255**

**SyncMe** is an independent Android Matrix messenger forked from [Element X Android](https://github.com/element-hq/element-x-android).
It adds a customized interface, personal conversation tags, shared presence indicators and optional diagnostic tools on top of the Matrix Rust SDK, Jetpack Compose and Appyx foundation.

This is the SyncMe development repository, not an official Element distribution.

## Latest release and downloads

**[Download SyncMe 1.1.0 · Build 255 — ARM64 APK](https://github.com/DrEsmaily/SyncMe/releases/download/v1.1.0-build255/SyncMe-255-ARM64.apk)**

- **Version 1.1.0 · Build 255 · Android 7.0+ · ARM64.** [Release notes](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.1.0-build255) · [Verified build](https://github.com/DrEsmaily/SyncMe/actions/runs/39000390352) · [SHA-256](https://github.com/DrEsmaily/SyncMe/releases/download/v1.1.0-build255/SHA256.txt).
- **Clearer last seen:** recently below one hour, then elapsed hours, days, weeks and months; `Last seen long time ago` from one year. [Exact rules, examples and validation](https://github.com/DrEsmaily/SyncMe/blob/syncme-255-last-seen/docs/syncme/build-255-last-seen.md).
- Regression tests and APK version/ABI/JNI/ZIP checks passed before publication. Physical device confirmation is separate from CI.
- [Source](https://github.com/DrEsmaily/SyncMe/tree/syncme-255-last-seen) · [Previous release: 1.0.0 · Build 254](https://github.com/DrEsmaily/SyncMe/releases/download/v1.0.0-build254/SyncMe-254-ARM64.apk).

Download the APK directly without a GitHub account, then install it as an update over your existing SyncMe release. Actions artifacts are additional build records; the release APK does not expire with them.

## Features added or customized in SyncMe

These are recorded differences from the upstream version used as this fork's base. They are not claims of inventing these features across all messengers, or comparisons with every future Element release.

| Feature | Where it appears and what it does |
|---|---|
| **Online and Away presence** | Shared indicators on home DM avatars, chat headers, profiles and People; green Online and orange Away, based on actual Matrix responses. |
| **Last seen and group online counts** | English approximate last-seen labels and group counts qualified as “at least” when data is incomplete; unknown status is not guessed. |
| **Upload percentage and transferred size** | Real percentage, transferred/total bytes and a progress bar within conversations, driven by SDK send-queue state and fitted to the message bubble. |
| **Media-send controls** | Customized quality/original-file behavior, upload cancellation through message actions, and pending-upload presentation. |
| **Personal conversation tags** | Dedicated create, rename and management flows, conversation assignments and persistent home filters; up to three personal tags. |
| **SyncMe identity and interface** | SM artwork, dedicated welcome screen, cobalt accents, black surfaces, simpler navigation, and customized menus, reactions and settings. |
| **Optional diagnostics** | Startup, TLS and decryption tools in Developer Options; default-off, bounded capture. |
| **Refined read-receipt behavior** | Reader avatars under the relevant outgoing message, retries after failure, and automatic receipts restricted to the active chat and visible event. |
| **Permission-based group closure** | A confirmed client-side management action; it does not erase every federated or device-local copy of history. |

### Inherited from Element X and Matrix

Matrix accounts/login, direct and group conversations, the end-to-end encryption foundation, message/media transport, synchronization and the receipt protocol come from the upstream platform. SyncMe adds or customizes the layers above rather than claiming the underlying messenger stack was built from scratch.

### Technical fixes and current limits

SyncMe includes identity-reset completion fixes, bounded FCM token handling, presence/receipt lifecycle corrections and regression coverage.
Current TLS verifies system trust, expiry/EKU and hostname, but omits online OCSP/CRL revocation checking; see the [technical review and limits](https://github.com/DrEsmaily/SyncMe/blob/syncme-cumulative-build-20261015/docs/syncme/root-cause-review.md).

Successful CI is separate from device verification. Large-file uploads under VPN, multi-device presence, background notifications and two-phone receipt behavior require runtime testing. Historical experiments on other branches are not automatically features of the current application.

## Real SyncMe screenshot

The icon above is the actual launcher asset. The project owner supplied this real SyncMe welcome-screen screenshot; its build number is not visible.

<p align="center"><img src="https://raw.githubusercontent.com/DrEsmaily/SyncMe/develop/docs/syncme/images/welcome.jpg" alt="Real SyncMe welcome screen with SM artwork and Login button" width="288" /></p>

## Latest 10 successful builds

This rolling list includes the current build and the nine preceding successful builds. Release versions and build numbers are separate: **1.1.0 / Build 255** follows **1.0.0 / Build 254**.

| Build | Verified execution | Downloads and documentation |
|---|---|---|
| 255 — Version 1.1.0 | [Run 39000390352](https://github.com/DrEsmaily/SyncMe/actions/runs/39000390352) | [APK](https://github.com/DrEsmaily/SyncMe/releases/download/v1.1.0-build255/SyncMe-255-ARM64.apk) · [Release and full notes](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.1.0-build255) |
| 254 — Version 1.0.0 | [Run 38776679128](https://github.com/DrEsmaily/SyncMe/actions/runs/38776679128) | [APK](https://github.com/DrEsmaily/SyncMe/releases/download/v1.0.0-build254/SyncMe-254-ARM64.apk) · [Release and cumulative notes](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.0.0-build254) |
| 253 | [Run 38658866967](https://github.com/DrEsmaily/SyncMe/actions/runs/38658866967) | [History and details](https://github.com/DrEsmaily/SyncMe/blob/syncme-cumulative-build-20261015/docs/syncme/history/build-38658866967.md) |
| 252 | [Run 38633766604](https://github.com/DrEsmaily/SyncMe/actions/runs/38633766604) | [History and details](https://github.com/DrEsmaily/SyncMe/blob/syncme-cumulative-build-20261015/docs/syncme/history/build-38633766604.md) |
| 251 | [Run 38586470729](https://github.com/DrEsmaily/SyncMe/actions/runs/38586470729) | [History and details](https://github.com/DrEsmaily/SyncMe/blob/syncme-cumulative-build-20261015/docs/syncme/history/build-38586470729.md) |
| 250 | [Run 38547146922](https://github.com/DrEsmaily/SyncMe/actions/runs/38547146922) | [History and details](https://github.com/DrEsmaily/SyncMe/blob/syncme-cumulative-build-20261015/docs/syncme/history/build-38547146922.md) |
| 249 | [Run 38513962735](https://github.com/DrEsmaily/SyncMe/actions/runs/38513962735) | [History and details](https://github.com/DrEsmaily/SyncMe/blob/syncme-cumulative-build-20261015/docs/syncme/history/build-38513962735.md) |
| 248 | [Run 38400798130](https://github.com/DrEsmaily/SyncMe/actions/runs/38400798130) | [History and details](https://github.com/DrEsmaily/SyncMe/blob/syncme-cumulative-build-20261015/docs/syncme/history/build-38400798130.md) |
| 247 | [Run 38362221920](https://github.com/DrEsmaily/SyncMe/actions/runs/38362221920) | [History and details](https://github.com/DrEsmaily/SyncMe/blob/syncme-cumulative-build-20261015/docs/syncme/history/build-38362221920.md) |
| 246 | [Run 38345740737](https://github.com/DrEsmaily/SyncMe/actions/runs/38345740737) | [History and details](https://github.com/DrEsmaily/SyncMe/blob/syncme-cumulative-build-20261015/docs/syncme/history/build-38345740737.md) |

### Earlier preserved archive

The original protected snapshot covers builds **244–253**. Builds [245](https://github.com/DrEsmaily/SyncMe/actions/runs/38314674913) and [244 (APK labelled 243)](https://github.com/DrEsmaily/SyncMe/actions/runs/38287593658) remain in the [protected-build archive](https://github.com/DrEsmaily/SyncMe/blob/syncme-cumulative-build-20261015/docs/syncme/history/protected-builds.json), alongside their historical notes. The rolling table above now includes 254 and 255; the archive records the earlier preservation operation.

[Historical cleanup report and preservation record](https://github.com/DrEsmaily/SyncMe/blob/syncme-cumulative-build-20261015/docs/syncme/cleanup-proposal.md)

## Development and preserved history

The latest verified application source is on `syncme-255-last-seen`, based on the verified build 254 source on `syncme-cumulative-build-20261015`. See [build 255 rules and validation](docs/syncme/build-255-last-seen.md).
Start with the cumulative notes, presence review and root-cause review. The rebuild branch's `docs/syncme/history` records the initial 422 Actions executions, 797 commits across build lineages, distinct historical note revisions and a separate cumulative history for each protected build.

Older descriptions can record superseded behavior or unfinished work. Always check their date, exact commit and newer evidence. Actions cleanup does not remove Git history. The original protected snapshot is documented separately from the rolling latest-build list and the persistent release downloads above.

## Building and contributing

The application requires Android 7.0+ (API 24). Development uses JDK 21 and the Android toolchain. See [contributing](CONTRIBUTING.md) and [developer onboarding](docs/_developer_onboarding.md).

The dedicated SyncMe workflow runs regressions, builds `:app:assembleGplayRelease`, retains the existing `.syncmerelease` application identity, checks ARM64/version/manifest/JNI metadata and bundles cumulative documentation and checksums.
Report SyncMe-specific issues in [this repository](https://github.com/DrEsmaily/SyncMe/issues).

## Upstream attribution

SyncMe is derived from [element-hq/element-x-android](https://github.com/element-hq/element-x-android). Original history, attribution and the [LICENSE](LICENSE) and [LICENSE-COMMERCIAL](LICENSE-COMMERCIAL) files are preserved. SyncMe branding does not change those license terms.

## Copyright and License

**SyncMe Modifications & Additions:**
Copyright (c) 2026 DrEsmaily (SyncMe Project). SyncMe-specific modifications are licensed under AGPL-3.0-or-later.

**Upstream Base Project (Element X Android):**
Copyright (c) 2025 Element Creations Ltd.
Copyright (c) 2022 - 2025 New Vector Ltd.

### License Terms

**SyncMe** is free and open-source software distributed under the terms of the **GNU Affero General Public License v3.0 (AGPL-3.0)** (or, at your option, any later version), as published by the Free Software Foundation.

While the upstream `element-x-android` repository is dual-licensed by Element Creations Ltd under both the AGPL-3.0 and an Element Commercial License, this independent fork incorporates upstream code specifically under the terms of the **AGPL-3.0**. The combined fork is distributed under AGPL-3.0-or-later. 

Original upstream attribution, commit history, and the `LICENSE` and `LICENSE-COMMERCIAL` files are preserved in the repository root for historical and legal compliance.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the [GNU Affero General Public License](LICENSE) for the specific language governing permissions and limitations.
