# SyncMe 246 presence

Home DM avatars, chat headers, user profiles and People share the same status stream.
Online and Away come only from authenticated Matrix presence responses. Away is
Matrix `unavailable`; `currently_active=false` alone does not change Online to Away.
Unknown or forbidden presence is hidden, not inferred as Offline. Labels use Android
resources. Online is green; Away uses the theme's orange warning color; Offline is gray.

The pinned SDK does not expose incoming presence. Reading uses only authenticated
`GET /presence/{user}/status`, never a second `/sync`. A parallel `/sync` can acknowledge
and delete queued to-device encryption messages before the SDK processes them (Synapse
`SyncHandler.wait_for_sync_for_user` acknowledges `since_token.to_device_key`). The old
presence implementation used this unsafe pattern; it is removed completely.

Snapshots refresh every 15 seconds for up to eight subscribed users, or every 60 seconds
for larger sets. Four workers share HTTP 429 cooldowns; queued refreshes are deduplicated.
Calls are asynchronous, with normal platform TLS validation and five-second timeouts.
The service stops reads and clears indicators when the app is backgrounded or SDK sync
stops. Failed reads remove that user's known state; old cached states are expired.

Publication uses the existing SDK `setPresence(..., immediate=true)`, which also updates
presence in future SDK sync requests. Foreground publishes Online and sustained background
publishes Unavailable, with no forced account-wide Offline timer. Server policy and other
signed-in devices influence presence; this is not proof a person is looking at a screen.

Visible user subscriptions are reference-counted and released when their screen leaves
composition. Snapshots have four workers and at most 128 users per subscription. Home subscribes only
the current visible room range. People additionally subscribes visible rows, so scrolling
in a large group still loads individual indicators beyond the counting sample.
Small rooms load membership once; larger rooms use available members, with full membership
available in the People view. Presence updates do not repeat encryption identity lookups.

Group counts include only distinct joined members, including self if the server reports
self presence. Away, invited, banned and unknown users are not online. A total is exact
only when membership count matches and every joined member has a known presence. Otherwise
a positive count is explicitly 'At least N online'; unknown zero is hidden. Changes to
members or presence use the same stream for chat, details and People. The existing direct
room member selector now reacts to membership arriving after the initial composition.

Offline SDK sync and HTTP failures clear cached presence. Reconnection refreshes visible
users from the server. Changes can take one refresh interval plus request time to appear;
large groups and server rate limiting can take longer. Counts reflect recently observed
server states, rather than a simultaneous server-wide census. An unresponsive or presence-disabled server cannot produce an
accurate count, and the UI does not invent one.

Automated checks cover parsing, count completeness, screen subscription sharing, late
snapshots after disconnect, foreground/background requests, and reconnect. Existing TLS,
reset and Firebase regressions are run alongside profile/member/details presenter tests.
Actual multi-device/homeserver runtime testing is still required.

## SyncMe 247 corrections and diagnostics

Version 246's service tests supplied a fake publisher, but the production client omitted
that callback. Version 247 explicitly wires SDK `setPresence` and requires the callback
at construction. It republishes Online on reconnect. Away remains server-controlled,
including other active sessions and homeserver policy; a force-stopped process cannot
publish its own departure. This is not a guarantee of exact real-time presence.

Developer options now has **Enable decryption diagnostics**, disabled by default, and
**Decryption diagnostics** with Copy All and Clear. The toggle applies immediately and
is independent of startup/TLS logs. Enable it on both devices before reproducing, then
copy both reports before restarting the sender and note the affected message time.
Disable it after capturing. Clear removes only this report. No native DEBUG trace is
enabled by this switch.

The bounded report records UTC milliseconds, local sequence, sync/lifecycle changes,
local send admission, send states, immediate UTD cause, event availability and delayed
SDK UTD callbacks. Event/session identifiers are fingerprinted to correlate the two
reports; plaintext, ciphertext, credentials, exception messages and encryption keys are
never exported. The report is capped at two 256 KB files and a bounded write queue,
with dropped-event counts. Deduplication is bounded to 2048 event states.

This is diagnostic evidence, not a fix for the rare decryption failure. Native key
transport isn't exposed by this observer: queued/sent messages do not prove that a
recipient received the room key. Capture may narrow the cause without proving it.

Timeline following now keys off each newest event identity and retains the user's
pre-insertion follow intent during bursts. Read updates observe laid-out item identities,
including new messages while the list remains stationary. History readers are not
pulled to the bottom by incoming messages.


## SyncMe 248 unread-arrow correction

The upward arrow targets the fully-read marker; it is separate from the downward
live-edge button. A retained virtual read marker previously made the upward arrow
visible even after unread counts reached zero. It now requires unread messages and
hides after a successful live-edge read acknowledgement, without waiting for stale
RoomInfo to catch up. Failed read acknowledgements do not hide genuine unread state;
a later unseen event can show the arrow again. Late older acknowledgements cannot
overwrite the current read position. No message/key deletion or forced session reset
is involved. Version 247 completed CI successfully before this additional correction.

## Build 249: read receipt investigation

The reported missing reader avatar on build 248 is not yet reproduced end to end.
This candidate fixes model/layout observation gaps (including local echo event ID resolution) and only deduplicates successfully sent read receipts. Receipt sends are serialized to preserve duplicate suppression while allowing retries after failure.

The existing default-off diagnostics now records receipt request/completion/failure and SDK-mapped reader receipts. Completion means the SDK call returned successfully, not proof of server transmission or delivery. Event and reader identifiers are fingerprinted; no message content, exception text or keys are recorded. Public/private receipt preferences remain respected.

Regression tests cover unchanged list keys when the server event ID arrives, failed-send retry, and diagnostic identifier redaction. Device confirmation on both phones remains required.

## Build 250: anchor reader avatars to outgoing messages

Keep the original right alignment. Project each SDK receipt backwards to the nearest loaded outgoing message at or before its actual read position. Incoming replies no longer move the avatar underneath themselves. This is a presentation-only projection, before grouping and without changing SDK receipts or cached source items. Readers are deduplicated per thread; newer unread messages are never selected. If the outgoing target is outside the loaded window, its avatar waits for pagination. Deleted messages are skipped. Public/private settings remain unchanged.

Tests cover incoming replies, newer unread messages, multiple readers, pagination, thread separation, deleted targets and existing outgoing receipts. Device visual confirmation remains required.


### Build 251: automatic read receipts require the active chat

The viewport observer now runs only while its screen lifecycle is RESUMED and restarts on return. The presenter independently rejects hidden-screen callbacks and rechecks lifecycle after waiting for a pending receipt. Work belongs to the composition scope, not the session. Automatic receipts target the captured visible event ID, including at index zero, instead of marking the SDK live end (which may already include an unrendered message). Public/private preferences and build 250 avatar placement are preserved. Tests cover paused/background callbacks, a queued callback during departure, resume without scrolling, and exact-event receipts. This closes concrete early-read paths; confirmation of the reported two-phone behavior still requires installing this build on the recipient.
