# SyncMe 254 — Cumulative Development and Continuation Notes

This release rebuilds the application source of the latest successful **SyncMe 253** build,
commit `d101f1244a03c82f545dd9da0de1c477ae887ba7`. Changes for this release concern documentation,
the repository homepage, release metadata and packaging. They do not introduce new application behavior.
The ten previous successful build runs remain protected in addition to this new build.

## Project identity and origin

**SyncMe** is an independent fork of [Element X Android](https://github.com/element-hq/element-x-android).
It inherits the Matrix protocol, Matrix Rust SDK, Jetpack Compose UI foundation and Appyx architecture.
The shared upstream fork point for the current lineage is
`def8f7f34824dcc8b05483086c333ce2b5cb08d9`.
Original copyright notices, attribution and license files remain intact.
SyncMe is not an official Element distribution or an official Element store release.

## Features added or customized in SyncMe

“SyncMe addition” means a recorded difference from the upstream version used as this fork's base.
It does not claim invention of the feature across all messaging applications, or that future Element versions cannot provide it.

| SyncMe feature | Where it appears and what it does | Evidence and limits |
|---|---|---|
| **Online and Away presence** | Shared presence indicators on home DM avatars, chat headers, profiles and People; green Online and orange Away. | Authenticated Matrix responses, without inventing a status for unknown users. Publication, retry and subscriptions were refined in builds 246, 247 and 252. |
| **Last seen and group online counts** | English approximate last-seen labels and qualified online-member counts in group UI. | Incomplete membership/presence produces an explicit “at least” count. Server policy and other sessions affect results. |
| **Upload percentage and transferred size** | Real percentage, transferred/total bytes and a progress bar within the conversation, sized to the message bubble. | SyncMe presentation uses SDK send-queue progress. The underlying media transfer is inherited from Matrix; a UI completion state is not independent proof of remote delivery. |
| **Media controls and upload cancellation** | Customized quality choices, original-file behavior in the applicable paths, upload cancellation and pending-message layout. | Historical changes and reversions are recorded. This does not establish that every VPN upload retry or stall has been fixed. |
| **Personal conversation tags** | Persistent home filters, dedicated create/manage/rename/details screens, and conversation assignments; up to three personal tags. | This is SyncMe's management and UI layer. Removing a tag does not delete conversations or messages. |
| **SyncMe branding and interface** | SM artwork, launcher name, welcome screen, cobalt accents, black media surfaces, simpler home navigation, menus, reactions and settings. | The owner-supplied real welcome screenshot is included at `images/welcome.jpg`; its build number is not visible. |
| **Optional diagnostic tools** | Developer Options, unlocked by five taps on the version, with startup, TLS and decryption diagnostics. | Default-off, bounded capture. Some native tracing changes require restarting the application. |
| **Refined read-receipt presentation and timing** | Reader avatars anchored to the relevant outgoing message, retries after failed acknowledgements, and automatic receipts limited to the active chat and visible event. | The receipt protocol itself comes from Matrix/Element. SyncMe changes its UI projection and the conditions/timing of automatic calls. |
| **Client-side group closure** | A confirmed management action, subject to Matrix permissions, that removes eligible members and leaves as implemented. | It does not erase every federated or device-local copy of historical messages. |

### Capabilities inherited from Element X and Matrix

Matrix accounts and login, direct and group conversations, the end-to-end encryption foundation,
message/media transport, receipt protocol, synchronization and Matrix Rust SDK are inherited capabilities.
SyncMe adds or customizes the layers listed above; it does not present the entire underlying messenger stack as newly built from scratch.

### Technical fixes, separate from visible features

Identity-reset completion, FCM token handling, local certificate-verification policy, presence/receipt lifecycle
and regression coverage are described separately below. Source-level fixes, successful CI and confirmation on real devices are distinct forms of evidence.

## How to use this record

This document describes the current cumulative state. The [current implementation history](history/current-lineage.md)
contains every recorded commit from the fork point to the application source used for this release,
including complete commit messages and changed file paths.

The [Actions inventory](history/runs.md) and [all build-lineage commits](history/commits.json)
also record historical failures and separate experiments. An experiment listed there is not automatically included in the current APK.
Distinct checked-in note revisions are preserved under `history/source-notes/`, identified by Git blob SHA.
Original artifact notes from the ten protected builds are under `history/artifact-notes/`.
Older notes can describe intentions, unresolved problems or superseded behavior; assess them against the corresponding commit and newer evidence.

## Branding, appearance, login and settings

- Display names, home title, launcher artwork and onboarding were adapted to SyncMe. Cobalt accents, dark surfaces and black media backgrounds were developed through successive UI changes.
- Onboarding uses dedicated SyncMe artwork and a focused login path. Some Spaces and public-invitation entry points were hidden to simplify the interface; that is not deletion of the underlying Matrix capability.
- Build 253 hides About in the settings root and simplifies version text. Five-tap developer access remains; regression fixtures were aligned with the actual production provider constructor.
- The account-provider input is blank for the applicable fresh-login path. Saved server history and enterprise policy can select different paths. A blank field does not silently connect to a server.
- Settings placement, moderation/safety organization and removal of certain persistent red indicators are recorded in the implementation history. Hidden UI entries do not imply deletion of account or server data.

## Home, tags and conversation management

- Personal tags, conversation assignments and selected filters are persisted through the preferences layer.
- Dedicated management, create, rename and details flows replace earlier tag-popup iterations. Removing tags or assignments does not remove chat history.
- The three-personal-tag limit, context-menu assignment, Favorite placement, avoiding automatic activation of a new tag, and selection/scroll behavior were implemented in this lineage.
- Group closure depends on the user's Matrix permissions and performs client-side operations. It is not a server-wide or federation-wide deletion guarantee.
- Faster chat-opening and transition changes target perceived navigation latency. Compilation does not certify smooth frame pacing on every device.

## Messages, reactions, media and uploads

- Upload presentation uses the SDK's actual send-queue state for percentage and transferred bytes. Duplicate observers and experimental application-side send paths were corrected or reverted over time.
- Media-quality choices, original-file handling in the relevant paths, and guarding against a compressed estimate larger than the original are recorded in the commit history.
- Reactions, RTL alignment, bubble size, one-line upload status and progress-track width received multiple layout refinements. A historical patch is not by itself confirmation of the resulting appearance on a phone.
- Cancellation is connected to the underlying send queue in the relevant paths. Message/conversation sheets were adjusted for full visibility and Back handling.
- Large-file behavior under VPN requires controlled, same-routing comparisons. Successful CI does not prove every transfer stall or internal retry has been fixed. The preserved continuation-master revisions retain earlier findings and reported device observations.

## TLS, startup, identity reset and push — through build 245

- Older branches contain different TLS experiments, including configurations that disabled certificate authentication. Those are historical diagnostic branches, not a description of current verification policy.
- The current lineage uses a checked-in Android certificate-verifier module compatible with the rustls JNI contract. System trust, expiry and EKU checks remain, with hostname verification on the Rust side. Online OCSP/CRL revocation checking was removed to avoid external revocation requests during the callback.
- Consequently, an otherwise valid but revoked certificate is not rejected on the basis of an OCSP/CRL response. This policy is a specific tradeoff, not a claim that all TLS/network delays are eliminated.
- Standard OkHttp certificate authentication was restored. The [root-cause review](root-cause-review.md) documents the difference from older permissive experiments.
- Identity-reset completion now follows SDK success, rather than waiting indefinitely for backup to become enabled. Reset success does not mean backup is enabled; backup creation/replacement is not forced.
- Duplicate password submissions and completion-observer replacement/cancellation were handled. Reset tracing follows the relevant diagnostic preference.
- FCM keeps a bounded token wait, rejects blank cached/emitted tokens, and returns controlled failure when a finite flow ends without a usable token. This does not establish that Firebase configuration or background delivery is correct on a particular phone.

## Presence — builds 246, 247 and 252

- Home, chat headers, profiles and member views share one presence stream. Reads use authenticated `GET /presence/{user}/status`, never a parallel `/sync`, because a second sync consumer can acknowledge to-device encryption messages before the main SDK consumes them.
- Online and Away come from valid Matrix responses. Unknown, forbidden or expired observations are hidden instead of being guessed as Offline. Online is green and Away is orange.
- Reads are bounded and cancellable, share HTTP 429 cooldowns, and use visible-user subscriptions. Expired queued work is discarded.
- Publication uses the existing SDK `setPresence` path. Foreground, sustained background and reconnection are handled. Build 247 wired the actual production callback; build 252 added recovery after failed publication.
- Group counts include eligible joined members with known state. Incomplete data is qualified as “at least”; unknown zero is not displayed as an exact count.
- Build 252 adds English approximate last-seen intervals, including days, weeks and months, and removes Offline dots. Real behavior still depends on server policy and other signed-in sessions.

## Decryption, timeline following and receipts — builds 247–251

- Decryption diagnostics are independent of startup/TLS capture, default off, and expose Copy All and Clear. Queues/files are bounded. Identifiers are fingerprinted; plaintext, ciphertext and encryption keys are not exported.
- This is a UTD investigation tool, not proof that all decryption failures are fixed or that a recipient received a room key.
- Live-edge following observes new event identity and preserves the user's pre-insertion follow intent during bursts. Reading history should not cause every incoming message to pull the user to the bottom.
- Build 248 separates the upward unread-marker arrow from the downward live-edge button and hides stale unread UI after a valid acknowledgement. Failed acknowledgements must not erase genuine unread state.
- Build 249 observes resolved visible event IDs and retries failed receipts. SDK-call completion is not independent confirmation of network delivery.
- Build 250 projects reader avatars to the nearest loaded outgoing message at or before the real receipt position. Thread separation, pagination, duplicate readers and deleted targets are accounted for.
- Build 251 limits automatic receipts to a RESUMED chat and the captured visible event ID. Public/private receipt preferences remain respected; late older acknowledgements must not replace a newer position.

## Ten protected builds and historical numbering

Exact run IDs, artifact IDs and expiry metadata are listed in [protected-builds.json](history/protected-builds.json).
Each `history/build-RUN_ID.md` contains its own cumulative implementation history through that build's exact commit.

The run titled **244** actually published an artifact named **SyncMe-243-ARM64-Reset-Full-Diagnostics**.
That successful run is intentionally protected; this discrepancy is recorded rather than silently renumbered.
The new build is retained in addition to all ten original successful runs.

## Validation, evidence and continuation

Builds 245–253 ran the related regression suites. Build 254 runs the same suite again, builds the ARM64 APK,
checks its manifest/version/ABI/JNI class, and records SHA-256, exact commit and Actions URL alongside the output.
The Actions result is authoritative; this document does not predeclare that an unfinished run has succeeded.

Device confirmation remains separate: multi-session presence, target-network TLS behavior, backup intentionally disabled,
background notifications, large files under VPN, and two-phone receipt behavior need real runtime testing.

For further development, read this document, `presence-review.md`, `root-cause-review.md`,
the relevant per-build history and preserved original notes before changing application behavior.
Deleting an Actions execution does not delete Git history, but it removes that run's logs, Summary and artifacts.
Cleanup remains a separate, explicitly approved operation.
