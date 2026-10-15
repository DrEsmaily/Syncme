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
