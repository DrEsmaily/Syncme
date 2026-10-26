#!/usr/bin/env bash
set -euo pipefail
[[ "$(id -u)" -eq 0 ]] || { echo 'Run this installer as root.' >&2; exit 1; }
ref="${1:?An immutable setup commit SHA is required}"
[[ "$ref" =~ ^[0-9a-f]{40}$ ]] || { echo 'Invalid setup commit.' >&2; exit 1; }
destination=/opt/syncme/updates/android
[[ -d "$destination" && ! -L "$destination" ]] || { echo 'Expected update folder is missing or is a symlink.' >&2; exit 1; }
command -v curl >/dev/null
python3 -c 'import sys; assert sys.version_info >= (3, 11), "Python 3.11+ is required"'
command -v systemctl >/dev/null
systemctl show --property=Version --value >/dev/null
stage="$(mktemp -d)"
trap 'rm -rf -- "$stage"' EXIT
curl --fail --show-error --silent --location --proto '=https' --proto-redir '=https' --connect-timeout 15 --max-time 60 --retry 2 \
  "https://raw.githubusercontent.com/DrEsmaily/SyncMe/$ref/ops/update-pull/pull.py" -o "$stage/pull.py"
printf '%s  %s\n' '4be78d6432f22b9d226897707fa8f990c2ae67dbc47aa3156dd96c1f189eb9c6' "$stage/pull.py" | sha256sum --check --status
python3 -m py_compile "$stage/pull.py"
if ! id syncme-update-fetch >/dev/null 2>&1; then
  useradd --system --no-create-home --home-dir /nonexistent --shell /usr/sbin/nologin syncme-update-fetch
fi
[[ "$(id -u syncme-update-fetch)" != 0 && "$(id -Gn syncme-update-fetch)" == syncme-update-fetch ]]
[[ "$(getent passwd syncme-update-fetch | cut -d: -f6)" == /nonexistent ]]
for name in latest.json SyncMe.apk .mirror.lock; do
  [[ ! -L "$destination/$name" ]] || { echo 'Unexpected symlink in update folder.' >&2; exit 1; }
  if [[ -f "$destination/$name" ]]; then chmod 644 "$destination/$name"; fi
done
chown syncme-update-fetch:syncme-update-fetch "$destination"
chmod 755 "$destination"
if [[ -f "$destination/.mirror.lock" ]]; then chown syncme-update-fetch:syncme-update-fetch "$destination/.mirror.lock"; fi
install -d -o root -g root -m 755 /usr/local/lib/syncme-update-fetch
install -o root -g root -m 644 "$stage/pull.py" /usr/local/lib/syncme-update-fetch/pull.py
cat > "$stage/syncme-update-fetch.service" <<'UNIT'
[Unit]
Description=SyncMe verified update mirror
Wants=network-online.target
After=network-online.target

[Service]
Type=oneshot
User=syncme-update-fetch
Group=syncme-update-fetch
ExecStart=/usr/bin/python3 /usr/local/lib/syncme-update-fetch/pull.py
WorkingDirectory=/opt/syncme/updates/android
Environment=PYTHONDONTWRITEBYTECODE=1
TimeoutStartSec=15min
TimeoutStopSec=30s
UMask=0022
NoNewPrivileges=true
PrivateTmp=true
PrivateDevices=true
ProtectHome=true
ProtectSystem=strict
ReadWritePaths=/opt/syncme/updates/android
RestrictAddressFamilies=AF_INET AF_INET6 AF_UNIX
MemoryMax=256M
UNIT
cat > "$stage/syncme-update-fetch.timer" <<'UNIT'
[Unit]
Description=Check for verified SyncMe releases every five minutes

[Timer]
OnBootSec=45s
OnUnitInactiveSec=5min
AccuracySec=15s
RandomizedDelaySec=15s
Unit=syncme-update-fetch.service

[Install]
WantedBy=timers.target
UNIT
systemd-analyze verify "$stage/syncme-update-fetch.service" "$stage/syncme-update-fetch.timer"
install -o root -g root -m 644 "$stage/syncme-update-fetch.service" /etc/systemd/system/syncme-update-fetch.service
install -o root -g root -m 644 "$stage/syncme-update-fetch.timer" /etc/systemd/system/syncme-update-fetch.timer
systemctl daemon-reload
systemctl enable --now syncme-update-fetch.timer
systemctl start --no-block syncme-update-fetch.service
systemctl is-active syncme-update-fetch.timer
printf '%s\n' 'Setup installed. The initial background check has started; later checks run every five minutes.' \
  'Existing Docker, Matrix, Nextcloud and Traefik services were not restarted.' \
  'Check progress: journalctl -u syncme-update-fetch.service -n 20 --no-pager'
