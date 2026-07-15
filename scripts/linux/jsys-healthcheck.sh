#!/usr/bin/env bash
set -Eeuo pipefail

readonly service_name="${JSYS_SERVICE_NAME:-jsys.service}"
readonly health_url="${JSYS_HEALTH_URL:-http://127.0.0.1:8080/api/health}"
readonly timeout_seconds="${JSYS_HEALTH_TIMEOUT_SECONDS:-8}"

if response="$(curl --fail --silent --show-error --max-time "$timeout_seconds" "$health_url")" \
  && grep -q '"status":"ok"' <<<"$response"; then
  exit 0
fi

echo "J_Sys health check failed for ${health_url}; restarting ${service_name}" >&2
systemctl restart "$service_name"
