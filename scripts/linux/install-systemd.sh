#!/usr/bin/env bash
set -Eeuo pipefail

readonly app_dir="/opt/jsys"
readonly unit_dir="/etc/systemd/system"

if [[ $# -gt 0 && "$1" != "$app_dir" ]]; then
  echo "This deployment unit is configured for /opt/jsys; copy the application there first." >&2
  exit 1
fi

if [[ $EUID -ne 0 ]]; then
  echo "Run this installer as root: sudo bash scripts/linux/install-systemd.sh" >&2
  exit 1
fi

for command in java curl systemctl; do
  command -v "$command" >/dev/null || {
    echo "Missing required command: $command" >&2
    exit 1
  }
done

if [[ ! -f "$app_dir/backend/out/com/jsys/App.class" ]]; then
  echo "Build the application first; missing: $app_dir/backend/out/com/jsys/App.class" >&2
  exit 1
fi

install -d -m 0750 /etc/jsys
install -d -m 0755 /usr/local/lib/jsys

if [[ ! -f /etc/jsys/jsys.env ]]; then
  echo "Missing /etc/jsys/jsys.env. Create it from scripts/linux/jsys.env.example, set its four credentials, and run this installer again." >&2
  exit 1
fi

chmod 0600 /etc/jsys/jsys.env
install -m 0644 "$app_dir/scripts/linux/jsys.service" "$unit_dir/jsys.service"
install -m 0644 "$app_dir/scripts/linux/jsys-healthcheck.service" "$unit_dir/jsys-healthcheck.service"
install -m 0644 "$app_dir/scripts/linux/jsys-healthcheck.timer" "$unit_dir/jsys-healthcheck.timer"
install -m 0755 "$app_dir/scripts/linux/jsys-healthcheck.sh" /usr/local/lib/jsys/jsys-healthcheck.sh

systemctl daemon-reload
systemctl enable jsys.service
systemctl restart jsys.service
systemctl enable --now jsys-healthcheck.timer

echo "J_Sys recovery guard installed. Verify with:"
echo "  systemctl status jsys jsys-healthcheck.timer --no-pager"
echo "  systemctl list-timers jsys-healthcheck.timer --no-pager"
