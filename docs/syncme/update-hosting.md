# Update hosting

## Automatic server fetching

The tested optional installer configures the existing VPS to fetch new verified public GitHub releases every five minutes, without SSH deployment keys or GitHub secrets. It does not require a new app build.

Run this one-time command in the VPS terminal already serving the update folder:

```bash
syncme_setup_dir="$(mktemp -d)" && curl -fsSL --proto '=https' --proto-redir '=https' "https://raw.githubusercontent.com/DrEsmaily/SyncMe/f4764193a0a6deb3fe4cb3c0f12f09365cd0bfe2/ops/update-pull/install.sh" -o "$syncme_setup_dir/install.sh" && sudo bash "$syncme_setup_dir/install.sh" f4764193a0a6deb3fe4cb3c0f12f09365cd0bfe2
```

The command installs a dedicated unprivileged systemd service and timer. It starts an initial check in the background. GitHub connectivity is needed to fetch a new release; the previously hosted version remains available when GitHub is unreachable.

Setup is not activated remotely by the assistant: it becomes active only after the command succeeds on the VPS.

[Implementation, operational commands and uninstall/disable instructions](https://github.com/DrEsmaily/SyncMe/blob/f4764193a0a6deb3fe4cb3c0f12f09365cd0bfe2/ops/update-pull/README.md).

## Manual upload fallback

The user-authorized update-only endpoint is `https://updates.syncme.ir/android/latest.json`.
It is independent of Matrix login, account providers and GitHub at runtime.

Each release produces a `SyncMe-<build>-Server-Upload.zip` containing exactly `SyncMe.apk` and `latest.json`.
The JSON is generated from the verified APK: actual Android versionCode, file size and SHA-256; do not edit these values manually.

1. Extract the server-upload ZIP.
2. Upload `SyncMe.apk` to `/opt/syncme/updates/android/` first and wait for completion.
3. Upload `latest.json` last. Prefer temporary filenames followed by atomic renames for both files.
4. Fetch both public HTTPS URLs and confirm the JSON checksum matches the uploaded APK.

The first updater-enabled version must be installed manually. Subsequent versions are checked when the application enters the foreground.
The final item under Settings → App settings checks, downloads and opens the Android installer. Android requires the user to allow installation from SyncMe and confirm installation.

Downloads use Android DownloadManager and can resume after a connection interruption. The app restores an unfinished download after process restart.
Before opening the installer, it checks size, SHA-256, application ID, versionCode and signing certificate against the installed application.
The app refreshes metadata before starting a download, so replacing the fixed APK filename does not permanently strand older cached metadata.

Keep normal HTTPS certificate validation enabled. A domestic server must also have reachable DNS and a valid certificate during restricted connectivity.
The updater reports a retry state when the endpoint is unavailable or its metadata is invalid; it never treats a failed check as proof that the installed version is current.

Future release workflows must run `prepare_syncme_update.py` after APK verification and publish the JSON and server-upload ZIP alongside the canonical APK.
