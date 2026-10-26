# Automatic server fetching

This optional server-only service mirrors each new verified public SyncMe release from GitHub to the existing update folder.
It requires no SSH access from GitHub, no GitHub token and no APK rebuild. It uses outbound HTTPS; it does not open any new port.

The root installer configures a dedicated unprivileged `syncme-update-fetch` account and a systemd timer.
The service checks the public latest stable release every five minutes after a completed check.
It accepts only the expected repository assets, matching release tag and update URL, positive Android version codes, bounded file sizes and SHA-256 checksums.
It skips older releases and rejects a changed APK under the same Android version code.

Both files are staged on the same filesystem. After verifying the complete download, it atomically replaces `SyncMe.apk` first and `latest.json` last.
Download and validation failures leave the existing pair untouched. An interruption between the two final renames can leave old metadata briefly paired with a newer APK; the app rejects the checksum mismatch, and the next successful fetch repairs the pair.
The existing Android updater still verifies APK package/version/signing identity before opening the installer.

The public update folder remains `/opt/syncme/updates/android/`, served by the existing Nginx/Traefik containers.
The installer changes ownership of that directory to the dedicated service account so it can replace the two public files.
It does not restart or reconfigure Docker, Matrix, Nextcloud, Traefik or SSH.

Installation must use an immutable commit URL. Download `install.sh` from that commit, inspect it if desired, then run it as root with the same commit SHA as its first argument.
The installer fetches `pull.py` from that immutable revision and verifies its embedded SHA-256 before installing it.
Only run this on the VPS already serving the update folder.

Status and recent logs:

```bash
systemctl status syncme-update-fetch.timer --no-pager
journalctl -u syncme-update-fetch.service -n 20 --no-pager
```

Run an immediate check:

```bash
systemctl start syncme-update-fetch.service
```

Disable automation without removing the last hosted APK or metadata:

```bash
systemctl disable --now syncme-update-fetch.timer
systemctl stop syncme-update-fetch.service
```

When GitHub is unreachable, fetching fails and the timer retries later. Users can still download the previously hosted version from the domestic server.
Future releases must continue publishing the canonical ARM64 APK and `latest.json`; the existing SyncMe release workflow already produces these assets after verification.
Manual uploads remain possible through an administrator account, with the APK first and metadata last.
