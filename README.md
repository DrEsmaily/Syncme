<p align="center"><img src="https://raw.githubusercontent.com/DrEsmaily/SyncMe/syncme-cumulative-build-20261015/appicon/element/src/main/res/drawable-nodpi/syncme_launcher.webp" alt="SyncMe app icon" width="144" /></p>

# SyncMe for Android

SyncMe is a Matrix messenger for Android maintained by [DrEsmaily](https://github.com/DrEsmaily). Built on [Element X Android](https://github.com/element-hq/element-x-android), it adds personal chat filters, multi-photo albums, Persian calendar support and resumable in-app updates. Element X and the Matrix Rust SDK provide the underlying messaging, synchronization and end-to-end encryption.

**[Download the ARM64 APK](https://github.com/DrEsmaily/Syncme/releases/download/v1.5.3-build285/SyncMe-285-ARM64.apk)** · [Browse the released source](https://github.com/DrEsmaily/Syncme/tree/v1.5.3-build285) · [Build and test run](https://github.com/DrEsmaily/Syncme/actions/runs/43751178979)
Version 1.5.3 · Build 285 · Android 7.0 or later

<table>
<tr>
    <td align="center" width="33%"><img src="docs/syncme/images/welcome.jpg" alt="SyncMe welcome screen" width="180" height="400" /><br /><sub>Welcome</sub></td>
    <td align="center" width="33%"><img src="docs/syncme/images/home-presence.jpg" alt="SyncMe home with green Online indicators, orange Away indicators and unread counts" width="180" height="400" /><br /><sub>Online and Away presence</sub></td>
    <td align="center" width="33%"><img src="docs/syncme/images/home-tags.jpg" alt="Conversations filtered by the Close Friends tag" width="180" height="400" /><br /><sub>Personal tag filter</sub></td>
  </tr>
</table>

## Selected engineering work

| Problem | How SyncMe handles it | Code or tests |
| --- | --- | --- |
| An interrupted APK download can leave a partially written file. | The updater resumes from the last confirmed byte. If the server ignores the range request, it replaces the partial file. Before installation it checks the size, hash, package and signing certificate. | [Download recovery tests](https://github.com/DrEsmaily/Syncme/blob/v1.5.3-build285/features/preferences/impl/src/test/kotlin/io/element/android/features/preferences/impl/updates/ResumableUpdateDownloadTest.kt) |
| Fixed-size album tiles waste space around portrait photos or cut off their edges. | A Compose layout groups images into rows using their aspect ratios and the available chat width. Tests check that the image corners remain visible and that tapping a tile selects the right photo. | [Row layout tests](https://github.com/DrEsmaily/Syncme/blob/v1.5.3-build285/features/messages/impl/src/test/kotlin/io/element/android/features/messages/impl/timeline/components/event/GalleryRowsTest.kt) · [Rendering and tap tests](https://github.com/DrEsmaily/Syncme/blob/v1.5.3-build285/features/messages/impl/src/test/kotlin/io/element/android/features/messages/impl/timeline/components/event/GalleryImageFitTest.kt) |
| A chat date can change at local midnight or cross a Persian year boundary while the Gregorian year stays the same. | The formatter converts the device-local calendar date with Android ICU. Users can switch between Gregorian dates and Solar Hijri dates with Latin-script names. Tests cover Nowruz, leap days and daily transitions over 20 years. | [Calendar tests](https://github.com/DrEsmaily/Syncme/blob/v1.5.3-build285/libraries/dateformatter/impl/src/test/kotlin/io/element/android/libraries/dateformatter/impl/PersianDateFormatterTest.kt) |
| Opening a long conversation list can trigger repeated requests for the same person's presence. | SyncMe shares presence observations across screens, reuses fresh responses and bounds both concurrent requests and cached inactive users. | [Presence service](https://github.com/DrEsmaily/Syncme/blob/v1.5.3-build285/libraries/matrix/impl/src/main/kotlin/io/element/android/libraries/matrix/impl/user/RustPresenceService.kt) |

## More screenshots

<details>
<summary>View all nine additional screenshots of conversations, uploads, contacts and settings</summary>

<table>
<tr>
    <td align="center" width="33%"><img src="docs/syncme/images/upload-progress.jpg" alt="File upload showing percentage, transferred bytes and a progress bar in a direct conversation" width="180" height="400" /><br /><sub>Upload percentage and size</sub></td>
    <td align="center" width="33%"><img src="docs/syncme/images/group-chat.jpg" alt="Public group conversation with an online member count and reader avatars below an outgoing message" width="180" height="400" /><br /><sub>Group presence and reader avatars</sub></td>
    <td align="center" width="33%"><img src="docs/syncme/images/group-unread.jpg" alt="Public group conversation showing the online count and NEW unread divider" width="180" height="400" /><br /><sub>Unread message marker</sub></td>
  </tr>
<tr>
    <td align="center" width="33%"><img src="docs/syncme/images/manage-tags.jpg" alt="Manage tags screen with Favorite, Close Friends and a Create new tag button" width="180" height="400" /><br /><sub>Manage personal tags</sub></td>
    <td align="center" width="33%"><img src="docs/syncme/images/conversation-actions.jpg" alt="Conversation action sheet with Favorite and Close Friends tag toggles" width="180" height="400" /><br /><sub>Conversation actions</sub></td>
    <td align="center" width="33%"><img src="docs/syncme/images/start-chat.jpg" alt="Start chat screen with search, New room and Room directory" width="180" height="400" /><br /><sub>Start a chat</sub></td>
  </tr>
<tr>
    <td align="center" width="33%"><img src="docs/syncme/images/people-search.jpg" alt="People search showing a matching contact" width="180" height="400" /><br /><sub>Search for people</sub></td>
    <td align="center" width="33%"><img src="docs/syncme/images/new-contact-confirmation.jpg" alt="Confirmation sheet before starting a chat with a new contact" width="180" height="400" /><br /><sub>Confirm a new contact</sub></td>
    <td align="center" width="33%"><img src="docs/syncme/images/settings.jpg" alt="SyncMe settings showing account switching, account settings, appearance, screen lock and location sharing" width="180" height="400" /><br /><sub>Settings and account switching</sub></td>
  </tr>
</table>

</details>

## Build and release

The project uses Kotlin, Jetpack Compose, Appyx and the Matrix Rust SDK. To inspect or build the version distributed above, start from the release tag. Set up JDK 21 and the Android SDK using the [developer guide](https://github.com/DrEsmaily/Syncme/blob/v1.5.3-build285/docs/_developer_onboarding.md).

```sh
git clone --branch v1.5.3-build285 --depth 1 https://github.com/DrEsmaily/Syncme.git
cd Syncme
./gradlew :features:messages:impl:testDebugUnitTest --tests '*GalleryRowsTest'
```

The [release workflow](https://github.com/DrEsmaily/Syncme/blob/v1.5.3-build285/.github/workflows/syncme-bubble-colors-review.yml) runs the message regressions, assembles the optimized ARM64 APK and checks update compatibility. The [release page](https://github.com/DrEsmaily/Syncme/releases/tag/v1.5.3-build285) includes the APK, SHA-256 checksum and update manifest. Earlier versions are in [Releases](https://github.com/DrEsmaily/Syncme/releases). See the [self-hosted update guide](https://github.com/DrEsmaily/Syncme/blob/v1.5.3-build285/docs/syncme/update-hosting.md) for deployment and the [technical review](https://github.com/DrEsmaily/SyncMe/blob/syncme-cumulative-build-20261015/docs/syncme/root-cause-review.md) for known limits.

<details>
<summary>Build records and historical archives</summary>

This rolling list includes the current build and the nine preceding successful builds. Version 1.5.3 (Build 285) follows Version 1.5.2 (Build 284).

| Build | Verified execution | Downloads and documentation |
|---|---|---|
| 285 — Version 1.5.3 | [Run 43751178979](https://github.com/DrEsmaily/SyncMe/actions/runs/43751178979) | [APK](https://github.com/DrEsmaily/SyncMe/releases/download/v1.5.3-build285/SyncMe-285-ARM64.apk) · [Release notes](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.5.3-build285) |
| 284 — Version 1.5.2 | [Run 43661375322](https://github.com/DrEsmaily/SyncMe/actions/runs/43661375322) | [APK](https://github.com/DrEsmaily/SyncMe/releases/download/v1.5.2-build284/SyncMe-284-ARM64.apk) · [Release notes](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.5.2-build284) |
| 283 — Version 1.5.1 | [Run 43409344132](https://github.com/DrEsmaily/SyncMe/actions/runs/43409344132) | [APK](https://github.com/DrEsmaily/SyncMe/releases/download/v1.5.1-build283/SyncMe-283-ARM64.apk) · [Release notes](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.5.1-build283) |
| 282 — Version 1.5.0 | [Run 42843781934](https://github.com/DrEsmaily/SyncMe/actions/runs/42843781934) | [APK](https://github.com/DrEsmaily/SyncMe/releases/download/v1.5.0-build282/SyncMe-282-ARM64.apk) · [Release notes](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.5.0-build282) |
| 281 — Version 1.4.1 | [Run 42833442807](https://github.com/DrEsmaily/SyncMe/actions/runs/42833442807) | [APK](https://github.com/DrEsmaily/SyncMe/releases/download/v1.4.1-build281/SyncMe-281-ARM64.apk) · [Release notes](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.4.1-build281) |
| 280 — Version 1.4.0 | [Run 42823717159](https://github.com/DrEsmaily/SyncMe/actions/runs/42823717159) | [APK](https://github.com/DrEsmaily/SyncMe/releases/download/v1.4.0-build280/SyncMe-280-ARM64.apk) · [Release notes](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.4.0-build280) |
| 279 — Version 1.3.4 | [Run 42789085239](https://github.com/DrEsmaily/SyncMe/actions/runs/42789085239) | [APK](https://github.com/DrEsmaily/SyncMe/releases/download/v1.3.4-build279/SyncMe-279-ARM64.apk) · [Release notes](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.3.4-build279) |
| 278 — Version 1.3.3 | [Run 42782954520](https://github.com/DrEsmaily/SyncMe/actions/runs/42782954520) | [APK](https://github.com/DrEsmaily/SyncMe/releases/download/v1.3.3-build278/SyncMe-278-ARM64.apk) · [Release notes](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.3.3-build278) |
| 277 — Version 1.3.2 | [Run 42776171388](https://github.com/DrEsmaily/SyncMe/actions/runs/42776171388) | [APK](https://github.com/DrEsmaily/SyncMe/releases/download/v1.3.2-build277/SyncMe-277-ARM64.apk) · [Release notes](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.3.2-build277) |
| 276 — Version 1.3.1 | [Run 42703777680](https://github.com/DrEsmaily/SyncMe/actions/runs/42703777680) | [APK](https://github.com/DrEsmaily/SyncMe/releases/download/v1.3.1-build276/SyncMe-276-ARM64.apk) · [Release notes](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.3.1-build276) |

### Earlier preserved archive

[Version 1.3.0 · Build 275](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.3.0-build275) remains available as a public release with its APK and documentation.

[Version 1.2.1 · Build 274](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.2.1-build274) remains available as a public release with its APK and documentation.

[Version 1.2.0 · Build 273](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.2.0-build273) remains available as a public release with its APK and documentation.

[Version 1.1.16 · Build 272](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.1.16-build272) remains available as a public release with its APK and documentation.

[Version 1.1.15 · Build 271](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.1.15-build271) remains available as a public release with its APK and documentation.

[Version 1.1.14 · Build 270](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.1.14-build270) remains available as a public release with its APK and documentation.

[Version 1.1.13 · Build 269](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.1.13-build269) remains available as a public release with its APK and documentation.

[Version 1.1.12 · Build 268](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.1.12-build268) remains available as a public release with its APK and documentation.

[Version 1.1.11 · Build 267](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.1.11-build267) remains available as a public release with its APK and documentation.

[Version 1.1.10 · Build 266](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.1.10-build266) remains available as a public release with its APK and documentation.

[Version 1.1.9 · Build 265](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.1.9-build265) remains available as a public release with its APK and documentation.

[Version 1.1.8 · Build 264](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.1.8-build264) remains available as a public release with its APK and documentation.

[Version 1.1.7 · Build 263](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.1.7-build263) remains available as a public release with its APK and documentation.

[Version 1.1.6 · Build 262](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.1.6-build262) remains available as a public release with its APK and documentation.

[Version 1.1.5 · Build 261](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.1.5-build261) remains available as a public release with its APK and documentation.

[Version 1.1.4 · Build 260](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.1.4-build260) remains available as a public release with its APK and documentation.

[Version 1.1.3 · Build 259](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.1.3-build259) remains available as a public release with its APK and documentation.

[Version 1.1.2 · Build 258](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.1.2-build258) remains available as a public release with its APK and documentation.

[Build 257](https://github.com/DrEsmaily/SyncMe/actions/runs/39171234618/artifacts/12153525423) remains preserved as an Actions artifact with its APK and provenance.

[Version 1.1.1 · Build 256](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.1.1-build256) remains available as a public release with its APK and documentation.

[Version 1.1.0 · Build 255](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.1.0-build255) remains available as a public release with its APK and documentation.

[Version 1.0.0 · Build 254](https://github.com/DrEsmaily/SyncMe/releases/tag/v1.0.0-build254) remains available as a public release with its APK and documentation.

The original protected snapshot covers builds **244–253**. Builds [253](https://github.com/DrEsmaily/SyncMe/actions/runs/38658866967), [252](https://github.com/DrEsmaily/SyncMe/actions/runs/38633766604), [251](https://github.com/DrEsmaily/SyncMe/actions/runs/38586470729), [250](https://github.com/DrEsmaily/SyncMe/actions/runs/38547146922), [249](https://github.com/DrEsmaily/SyncMe/actions/runs/38513962735), [248](https://github.com/DrEsmaily/SyncMe/actions/runs/38400798130), [247](https://github.com/DrEsmaily/SyncMe/actions/runs/38362221920), [246](https://github.com/DrEsmaily/SyncMe/actions/runs/38345740737), [245](https://github.com/DrEsmaily/SyncMe/actions/runs/38314674913) and [244 (APK labelled 243)](https://github.com/DrEsmaily/SyncMe/actions/runs/38287593658) remain in the [protected-build archive](https://github.com/DrEsmaily/SyncMe/blob/syncme-cumulative-build-20261015/docs/syncme/history/protected-builds.json), alongside their historical notes. The rolling table above now includes builds 276 through 285; the archive records the earlier preservation operation.

[Historical cleanup report and preservation record](https://github.com/DrEsmaily/SyncMe/blob/syncme-cumulative-build-20261015/docs/syncme/cleanup-proposal.md)

</details>

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
