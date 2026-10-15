<p align="center"><img src="https://raw.githubusercontent.com/DrEsmaily/element-x-android/syncme-cumulative-build-20261015/appicon/element/src/main/res/drawable-nodpi/syncme_launcher.webp" alt="SyncMe app icon" width="144" /></p>

# SyncMe for Android

**SyncMe** is an independent Android Matrix messenger forked from [Element X Android](https://github.com/element-hq/element-x-android).
It adds a customized interface, personal conversation tags, shared presence indicators and optional diagnostic tools on top of the Matrix Rust SDK, Jetpack Compose and Appyx foundation.

This is the SyncMe development repository, not an official Element distribution.

## Latest build and downloads

- **Latest verified APK: SyncMe 254, ARM64** — [build and download](https://github.com/DrEsmaily/element-x-android/actions/runs/38776679128). A documented rebuild of the verified SyncMe 253 application source.
- **254 verification:** regression tests, APK manifest/version/ABI/JNI checks and SHA-256 completed successfully. English cumulative notes and the supplied real screenshot are bundled. CI remains separate from device confirmation.
- [Cumulative release and continuation notes](https://github.com/DrEsmaily/element-x-android/blob/syncme-cumulative-build-20261015/docs/syncme/cumulative-release-notes.md)
- [Complete current-lineage commit messages and changed paths](https://github.com/DrEsmaily/element-x-android/blob/syncme-cumulative-build-20261015/docs/syncme/history/current-lineage.md)
- [Ten protected successful builds and artifact identities](https://github.com/DrEsmaily/element-x-android/blob/syncme-cumulative-build-20261015/docs/syncme/history/protected-builds.json)

Actions artifact downloads may require signing in to GitHub. They expire: this workflow also makes an additional copy of the ten previous builds with 90-day retention. That copy is a backup, not permanent download hosting.

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
Current TLS verifies system trust, expiry/EKU and hostname, but omits online OCSP/CRL revocation checking; see the [technical review and limits](https://github.com/DrEsmaily/element-x-android/blob/syncme-cumulative-build-20261015/docs/syncme/root-cause-review.md).

Successful CI is separate from device verification. Large-file uploads under VPN, multi-device presence, background notifications and two-phone receipt behavior require runtime testing. Historical experiments on other branches are not automatically features of the current application.

## Real SyncMe screenshot

The icon above is the actual launcher asset. The project owner supplied this real SyncMe welcome-screen screenshot; its build number is not visible.

<p align="center"><img src="https://raw.githubusercontent.com/DrEsmaily/element-x-android/develop/docs/syncme/images/welcome.jpg" alt="Real SyncMe welcome screen with SM artwork and Login button" width="288" /></p>

## Ten previous successful builds retained

| Build title | Original execution and download | Cumulative details through that build |
|---|---|---|
| 253 | [Run 38658866967](https://github.com/DrEsmaily/element-x-android/actions/runs/38658866967) | [History and details](https://github.com/DrEsmaily/element-x-android/blob/syncme-cumulative-build-20261015/docs/syncme/history/build-38658866967.md) |
| 252 | [Run 38633766604](https://github.com/DrEsmaily/element-x-android/actions/runs/38633766604) | [History and details](https://github.com/DrEsmaily/element-x-android/blob/syncme-cumulative-build-20261015/docs/syncme/history/build-38633766604.md) |
| 251 | [Run 38586470729](https://github.com/DrEsmaily/element-x-android/actions/runs/38586470729) | [History and details](https://github.com/DrEsmaily/element-x-android/blob/syncme-cumulative-build-20261015/docs/syncme/history/build-38586470729.md) |
| 250 | [Run 38547146922](https://github.com/DrEsmaily/element-x-android/actions/runs/38547146922) | [History and details](https://github.com/DrEsmaily/element-x-android/blob/syncme-cumulative-build-20261015/docs/syncme/history/build-38547146922.md) |
| 249 | [Run 38513962735](https://github.com/DrEsmaily/element-x-android/actions/runs/38513962735) | [History and details](https://github.com/DrEsmaily/element-x-android/blob/syncme-cumulative-build-20261015/docs/syncme/history/build-38513962735.md) |
| 248 | [Run 38400798130](https://github.com/DrEsmaily/element-x-android/actions/runs/38400798130) | [History and details](https://github.com/DrEsmaily/element-x-android/blob/syncme-cumulative-build-20261015/docs/syncme/history/build-38400798130.md) |
| 247 | [Run 38362221920](https://github.com/DrEsmaily/element-x-android/actions/runs/38362221920) | [History and details](https://github.com/DrEsmaily/element-x-android/blob/syncme-cumulative-build-20261015/docs/syncme/history/build-38362221920.md) |
| 246 | [Run 38345740737](https://github.com/DrEsmaily/element-x-android/actions/runs/38345740737) | [History and details](https://github.com/DrEsmaily/element-x-android/blob/syncme-cumulative-build-20261015/docs/syncme/history/build-38345740737.md) |
| 245 | [Run 38314674913](https://github.com/DrEsmaily/element-x-android/actions/runs/38314674913) | [History and details](https://github.com/DrEsmaily/element-x-android/blob/syncme-cumulative-build-20261015/docs/syncme/history/build-38314674913.md) |
| 244 (APK labelled 243) | [Run 38287593658](https://github.com/DrEsmaily/element-x-android/actions/runs/38287593658) | [History and details](https://github.com/DrEsmaily/element-x-android/blob/syncme-cumulative-build-20261015/docs/syncme/history/build-38287593658.md) |

[Exact proposed cleanup list — awaiting approval](docs/syncme/cleanup-proposal.md)

## Development and preserved history

The latest verified application source is on `fix/settings-display-cleanup`; its documented rebuild is on `syncme-cumulative-build-20261015`.
Start with the cumulative notes, presence review and root-cause review. The rebuild branch's `docs/syncme/history` records the initial 422 Actions executions, 797 commits across build lineages, distinct historical note revisions and a separate cumulative history for each protected build.

Older descriptions can record superseded behavior or unfinished work. Always check their date, exact commit and newer evidence. Actions cleanup does not remove Git history. The ten original successful builds and the new cumulative build are protected.

## Building and contributing

The application requires Android 7.0+ (API 24). Development uses JDK 21 and the Android toolchain. See [contributing](CONTRIBUTING.md) and [developer onboarding](docs/_developer_onboarding.md).

The dedicated SyncMe workflow runs regressions, builds `:app:assembleGplayRelease`, retains the existing `.syncmerelease` application identity, checks ARM64/version/manifest/JNI metadata and bundles cumulative documentation and checksums.
Report SyncMe-specific issues in [this repository](https://github.com/DrEsmaily/element-x-android/issues).

## Upstream attribution

SyncMe is derived from [element-hq/element-x-android](https://github.com/element-hq/element-x-android). Original history, attribution and the [LICENSE](LICENSE) and [LICENSE-COMMERCIAL](LICENSE-COMMERCIAL) files are preserved. SyncMe branding does not change those license terms.

## Copyright and License

Copyright (c) 2025 Element Creations Ltd.
Copyright (c) 2022 - 2025 New Vector Ltd.

This software is dual licensed by Element Creations Ltd (Element). It can be used either:

(1) for free under the terms of the GNU Affero General Public License (as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version); OR

(2) under the terms of a paid-for Element Commercial License agreement between you and Element (the terms of which may vary depending on what you and Element have agreed to).

Unless required by applicable law or agreed to in writing, software distributed under the Licenses is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licenses for the specific language governing permissions and limitations under the Licenses.
