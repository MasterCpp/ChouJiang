#!/usr/bin/env bash
set -Eeuo pipefail

readonly app_dir="/opt/jsys-en"
readonly unit_dir="/etc/systemd/system"

if [[ $EUID -ne 0 ]]; then
  echo "Run this installer as root: sudo bash scripts/linux/install-english-systemd.sh" >&2
  exit 1
fi

for command in java curl systemctl; do
  command -v "$command" >/dev/null || {
    echo "Missing required command: $command" >&2
    exit 1
  }
done

if [[ ! -f "$app_dir/backend/out/com/jsys/App.class" ]]; then
  echo "Build the English instance first; missing: $app_dir/backend/out/com/jsys/App.class" >&2
  exit 1
fi

install -d -m 0750 /etc/jsys-en
install -d -m 0755 /usr/local/lib/jsys-en
install -m 0644 "$app_dir/scripts/linux/jsys-en.service" "$unit_dir/jsys-en.service"
install -m 0644 "$app_dir/scripts/linux/jsys-en-healthcheck.service" "$unit_dir/jsys-en-healthcheck.service"
install -m 0644 "$app_dir/scripts/linux/jsys-en-healthcheck.timer" "$unit_dir/jsys-en-healthcheck.timer"
install -m 0755 "$app_dir/scripts/linux/jsys-healthcheck.sh" /usr/local/lib/jsys-en/jsys-healthcheck.sh

systemctl daemon-reload
systemctl enable jsys-en.service
systemctl restart jsys-en.service
systemctl enable --now jsys-en-healthcheck.timer

echo "English J_Sys instance installed. Verify with:"
echo "  systemctl status jsys-en jsys-en-healthcheck.timer --no-pager"
echo "  curl -fsS http://127.0.0.1:8081/api/health"
