# Working on SyncMe

[Main](https://github.com/DrEsmaily/Syncme/tree/main) is the starting point for Android development. To inspect the code that shipped in an APK, use its release tag. For example, the comparison from build 284 to 285 shows which changes reached version 1.5.3. [Compare the two releases](https://github.com/DrEsmaily/Syncme/compare/v1.5.2-build284...v1.5.3-build285)

## Where changes go

| Work | Branch or tag |
| --- | --- |
| Android features and fixes | Branch from `main`, use a short name such as `feat/chat-search` or `fix/receipt-ordering`, and open a pull request against `main`. Include the test or reproduction that demonstrates the change. |
| Documentation | Use `docs/` and a descriptive topic, such as `docs/update-hosting`. |
| Update server | The pull-based update mirror lives on [ops/syncme-update-pull](https://github.com/DrEsmaily/Syncme/tree/ops/syncme-update-pull). Its installation instructions can pin an exact commit. |
| Android releases | Use the version tag to find the source, APK, update metadata and checksums. The [versioning guide](versioning.md) explains how the public version and Android build number advance. |

## Explore the engineering history

| Investigation | What to look for |
| --- | --- |
| Chat performance | The [original timeline row](https://github.com/DrEsmaily/Syncme/tree/archive/performance/lagtest-a-original-row) and the [variant with reactions disabled](https://github.com/DrEsmaily/Syncme/tree/archive/performance/lagtest-b-no-reactions) are two of the experiments under `archive/performance/`. |
| Presence and startup | `archive/presence/` groups work on online and away states. `archive/startup/` groups startup tracing and diagnostics. |
| Connections and account recovery | `archive/transport/` covers TLS and certificate handling. `archive/account-recovery/` follows investigations into session resets and backup recovery. |
| Earlier client development | `archive/development/` contains early interface and SDK experiments. For a chronological account of changes that reached build 253, see the [implementation history](history/current-lineage.md). |

Some build workflows and source URLs reference branches by name. These branches, including the numbered `build/syncme-*` series, retain those names so historical builds remain traceable. The [branch inventory](history/branch-tips-2026-11-23.tsv) maps every name from before the reorganization to its full commit ID and its name immediately afterward. Use the commit ID to inspect that exact snapshot as branches move forward.
