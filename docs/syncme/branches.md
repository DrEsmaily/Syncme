# Working on SyncMe

Start from [main](https://github.com/DrEsmaily/Syncme/tree/main) for Android changes. To audit an APK, open its tag in [Releases](https://github.com/DrEsmaily/Syncme/releases). For example, [v1.5.3-build285](https://github.com/DrEsmaily/Syncme/tree/v1.5.3-build285) identifies the source used to build version 1.5.3. Documentation on main can advance between releases.

| Work | Where it belongs |
| --- | --- |
| A feature, fix or documentation change | Branch from main with a short name describing the change, such as `feat/chat-search`, `fix/receipt-ordering` or `docs/update-hosting`. Open a pull request against main. |
| Update server tooling | [ops/syncme-update-pull](https://github.com/DrEsmaily/Syncme/tree/ops/syncme-update-pull) contains the pull-based update mirror. Deployment instructions can pin a specific commit. |
| A published version | Use the existing `v<version>-build<number>` tag. The associated release provides the APK, metadata and checksums. |
| Previous Android work | Earlier build and integration branches, including `build/syncme-*` and `develop`, record the project's earlier workflow. Existing release tags, historical workflow inputs and source links retain their original meaning. |

## Finding earlier work

The [branch inventory](history/branch-tips-2026-11-23.tsv) records every branch name and its full commit ID immediately before the November 2026 reorganization. A commit ID identifies the same revision even when a branch name changes. Use the [build lineage](history/current-lineage.md) to follow the earlier Android investigations, or [browse branches](https://github.com/DrEsmaily/Syncme/branches/all) for open work and historical comparisons.

To inspect a snapshot locally, use the commit from the inventory:

```sh
git fetch origin --tags
git show <commit>
```
