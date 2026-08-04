#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 || $# -gt 3 ]]; then
  echo "Usage: $0 <user@host> [remote-app-dir] [remote-frontend-dir]" >&2
  exit 1
fi

remote_host="$1"
remote_dir="${2:-/opt/engineering-ops-erp}"
frontend_dir="${3:-/var/www/ops-erp-admin}"
root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
release_id="$(date -u +%Y%m%dT%H%M%SZ)-$(git -C "$root_dir" rev-parse --short=8 HEAD)"

[[ "$remote_host" =~ ^[A-Za-z0-9._@:-]+$ ]] || { echo "Invalid remote host." >&2; exit 1; }
for path in "$remote_dir" "$frontend_dir"; do
  [[ "$path" =~ ^/[A-Za-z0-9._/-]+$ && "$path" != "/" ]] || {
    echo "Remote directories must be explicit absolute paths without shell metacharacters." >&2
    exit 1
  }
done

backend_jar="$root_dir/services/api/target/ops-erp-api-0.1.0.jar"
frontend_dist="$root_dir/apps/admin/dist"
[[ -f "$backend_jar" ]] || { echo "Backend artifact missing; run ./deploy/build.sh first." >&2; exit 1; }
[[ -f "$frontend_dist/index.html" ]] || { echo "Frontend artifact missing; run ./deploy/build.sh first." >&2; exit 1; }

app_release="$remote_dir/releases/$release_id"
frontend_release="$frontend_dir/releases/$release_id"

echo "Deploying release $release_id to $remote_host"
ssh "$remote_host" "mkdir -p '$app_release' '$frontend_release'"
rsync -ahz --no-owner --no-group "$backend_jar" "$remote_host:$app_release/api.jar"
rsync -ahz --delete --no-perms --no-owner --no-group "$frontend_dist/" "$remote_host:$frontend_release/"
rsync -ahz "$root_dir/deploy/ops-erp.nginx.conf" "$remote_host:/etc/nginx/sites-available/ops-erp"
rsync -ahz "$root_dir/deploy/ops-erp-api.service" "$remote_host:/etc/systemd/system/ops-erp-api.service"
ssh "$remote_host" "mkdir -p '$remote_dir/scripts'"
rsync -ahz "$root_dir/scripts/backup-data.sh" "$root_dir/scripts/verify-backup.sh" \
  "$root_dir/scripts/restore-backup.sh" "$root_dir/scripts/backup-restore-drill.sh" \
  "$remote_host:$remote_dir/scripts/"
rsync -ahz "$root_dir/deploy/ops-erp-backup.service" "$root_dir/deploy/ops-erp-backup.timer" \
  "$root_dir/deploy/ops-erp-restore-drill.service" "$root_dir/deploy/ops-erp-restore-drill.timer" \
  "$remote_host:/etc/systemd/system/"

ssh "$remote_host" bash -s -- "$remote_dir" "$frontend_dir" "$release_id" <<'REMOTE_SCRIPT'
set -euo pipefail
remote_dir="$1"
frontend_dir="$2"
release_id="$3"
new_api="$remote_dir/releases/$release_id/api.jar"
new_frontend="$frontend_dir/releases/$release_id"
current_api="$remote_dir/current-api.jar"
current_frontend="$frontend_dir/current"
previous_api="$(readlink "$current_api" || true)"
previous_frontend="$(readlink "$current_frontend" || true)"

test -f "$new_api"
test -f "$new_frontend/index.html"
nginx -t
ln -sfn "$new_api" "$current_api"
ln -sfn "$new_frontend" "$current_frontend"
systemctl daemon-reload
systemctl enable --now ops-erp-backup.timer ops-erp-restore-drill.timer
systemctl restart ops-erp-api

healthy=false
for _ in $(seq 1 45); do
  if curl --fail --silent --max-time 2 http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"'; then
    healthy=true
    break
  fi
  sleep 2
done

if [[ "$healthy" != "true" ]]; then
  echo "Health check failed; rolling back release $release_id." >&2
  if [[ -n "$previous_api" ]]; then ln -sfn "$previous_api" "$current_api"; else rm -f -- "$current_api"; fi
  if [[ -n "$previous_frontend" ]]; then ln -sfn "$previous_frontend" "$current_frontend"; else rm -f -- "$current_frontend"; fi
  systemctl restart ops-erp-api || true
  exit 1
fi

nginx -t
systemctl reload nginx
echo "Release $release_id is healthy and active."
REMOTE_SCRIPT

echo "Deployment completed: $release_id"
