# SyncMe 246 presence

Online and Away come only from authenticated Matrix presence responses. Away is
Matrix `unavailable`; `currently_active=false` alone does not change Online to Away.
Unknown or forbidden presence is hidden, not inferred as Offline. Labels use Android
resources. Online is green; Away uses the theme's orange warning color; Offline is gray.

One presence-only `/sync` long poll per session is necessary because the pinned SDK's
sliding-sync wrapper does not expose presence. It shares neither room pagination nor
crypto state with the SDK. Normal platform TLS and hostname validation remain enabled.
Calls are asynchronous and cancelled on lifecycle/sync changes and session shutdown.
Foreground uses `set_presence=online`, background uses `unavailable`; there is no forced
account-wide Offline timer. Server presence policy and other signed-in devices still
influence account presence; this is not a guarantee that a person is looking at a screen.

Visible user subscriptions are reference-counted and released when their screen leaves
composition. Initial snapshots have four workers and at most 128 users per screen.
Small rooms load membership once; larger rooms use available members, with full membership
available in the People view. Presence updates do not repeat encryption identity lookups.

Group counts include only distinct joined members, including self if the server reports
self presence. Away, invited, banned and unknown users are not online. A total is exact
only when membership count matches and every joined member has a known presence. Otherwise
a positive count is explicitly 'At least N online'; unknown zero is hidden. Changes to
members or presence use the same stream for chat, details and People. The existing direct
room member selector now reacts to membership arriving after the initial composition.

Offline sync and HTTP failures clear cached presence. Reconnection uses a full initial
presence sync rather than a partial incremental cache. Socket failure detection can take
up to the request timeout. An unresponsive or presence-disabled server cannot produce an
accurate count, and the UI does not invent one.

Automated checks cover parsing, count completeness, screen subscription sharing, late
snapshots after disconnect, foreground/background requests, and reconnect. Existing TLS,
reset and Firebase regressions are run alongside profile/member/details presenter tests.
Actual multi-device/homeserver runtime testing is still required.
