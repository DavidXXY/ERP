#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 || $# -gt 5 ]]; then
  echo "Usage: $0 <user@host> [remote-app-dir] [remote-admin-dir] [remote-supplier-dir] [remote-mobile-dir]" >&2
  exit 1
fi

remote_host="$1"
remote_dir="${2:-/opt/engineering-ops-erp}"
admin_dir="${3:-/var/www/ops-erp-admin}"
supplier_dir="${4:-/var/www/ops-erp-supplier-portal}"
mobile_dir="${5:-/var/www/ops-erp-mobile}"
root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
release_id="$(date -u +%Y%m%dT%H%M%SZ)-$(git -C "$root_dir" rev-parse --short=8 HEAD)"

[[ "$remote_host" =~ ^[A-Za-z0-9._@:-]+$ ]] || { echo "Invalid remote host." >&2; exit 1; }
for path in "$remote_dir" "$admin_dir" "$supplier_dir" "$mobile_dir"; do
  [[ "$path" =~ ^/[A-Za-z0-9._/-]+$ && "$path" != "/" ]] || {
    echo "Remote directories must be explicit absolute paths without shell metacharacters." >&2
    exit 1
  }
done

backend_jar="$root_dir/services/api/target/ops-erp-api-0.1.0.jar"
admin_dist="$root_dir/apps/admin/dist"
supplier_dist="$root_dir/apps/supplier-portal/dist"
mobile_dist="$root_dir/apps/mobile/dist/build/h5"
[[ -f "$backend_jar" ]] || { echo "Backend artifact missing; run ./deploy/build.sh first." >&2; exit 1; }
for pair in "admin:$admin_dist" "supplier-portal:$supplier_dist" "mobile-h5:$mobile_dist"; do
  name="${pair%%:*}"
  dist="${pair#*:}"
  [[ -f "$dist/index.html" ]] || {
    echo "$name frontend artifact missing ($dist/index.html); run ./deploy/build.sh first." >&2
    exit 1
  }
done

app_release="$remote_dir/releases/$release_id"
admin_release="$admin_dir/releases/$release_id"
supplier_release="$supplier_dir/releases/$release_id"
mobile_release="$mobile_dir/releases/$release_id"

echo "Deploying release $release_id to $remote_host"
ssh "$remote_host" "mkdir -p '$app_release' '$admin_release' '$supplier_release' '$mobile_release'"
rsync -ahz --no-owner --no-group "$backend_jar" "$remote_host:$app_release/api.jar"
rsync -ahz --delete --no-perms --no-owner --no-group "$admin_dist/" "$remote_host:$admin_release/"
rsync -ahz --delete --no-perms --no-owner --no-group "$supplier_dist/" "$remote_host:$supplier_release/"
rsync -ahz --delete --no-perms --no-owner --no-group "$mobile_dist/" "$remote_host:$mobile_release/"

# Nginx：主站 + 多端口入口 + 公共片段 + 限流 zone + 反向代理片段
ssh "$remote_host" "mkdir -p /etc/nginx/sites-available /etc/nginx/sites-enabled /etc/nginx/snippets /etc/nginx/conf.d"
rsync -ahz "$root_dir/deploy/ops-erp.nginx.conf" "$remote_host:/etc/nginx/sites-available/ops-erp"
rsync -ahz "$root_dir/deploy/ops-erp-ports.nginx.conf" "$remote_host:/etc/nginx/sites-available/ops-erp-ports"
rsync -ahz "$root_dir/deploy/ops-erp-common.conf" "$remote_host:/etc/nginx/snippets/ops-erp-common.conf"
rsync -ahz "$root_dir/deploy/ops-erp-proxy.conf" "$remote_host:/etc/nginx/snippets/ops-erp-proxy.conf"
rsync -ahz "$root_dir/deploy/ops-erp-http.conf" "$remote_host:/etc/nginx/conf.d/ops-erp-http.conf"
rsync -ahz "$root_dir/deploy/ops-erp-api.service" "$remote_host:/etc/systemd/system/ops-erp-api.service"
ssh "$remote_host" "mkdir -p '$remote_dir/scripts'"
rsync -ahz "$root_dir/scripts/backup-data.sh" "$root_dir/scripts/verify-backup.sh" \
  "$root_dir/scripts/restore-backup.sh" "$root_dir/scripts/backup-restore-drill.sh" \
  "$remote_host:$remote_dir/scripts/"
rsync -ahz "$root_dir/deploy/ops-erp-backup.service" "$root_dir/deploy/ops-erp-backup.timer" \
  "$root_dir/deploy/ops-erp-restore-drill.service" "$root_dir/deploy/ops-erp-restore-drill.timer" \
  "$remote_host:/etc/systemd/system/"

ssh "$remote_host" bash -s -- "$remote_dir" "$admin_dir" "$supplier_dir" "$mobile_dir" "$release_id" <<'REMOTE_SCRIPT'
set -euo pipefail
remote_dir="$1"
admin_dir="$2"
supplier_dir="$3"
mobile_dir="$4"
release_id="$5"
new_api="$remote_dir/releases/$release_id/api.jar"
new_admin="$admin_dir/releases/$release_id"
new_supplier="$supplier_dir/releases/$release_id"
new_mobile="$mobile_dir/releases/$release_id"
current_api="$remote_dir/current-api.jar"
current_admin="$admin_dir/current"
current_supplier="$supplier_dir/current"
current_mobile="$mobile_dir/current"
previous_api="$(readlink "$current_api" || true)"
previous_admin="$(readlink "$current_admin" || true)"
previous_supplier="$(readlink "$current_supplier" || true)"
previous_mobile="$(readlink "$current_mobile" || true)"

test -f "$new_api"
test -f "$new_admin/index.html"
test -f "$new_supplier/index.html"
test -f "$new_mobile/index.html"
ln -sfn /etc/nginx/sites-available/ops-erp /etc/nginx/sites-enabled/ops-erp
ln -sfn /etc/nginx/sites-available/ops-erp-ports /etc/nginx/sites-enabled/ops-erp-ports
nginx -t
ln -sfn "$new_api" "$current_api"
ln -sfn "$new_admin" "$current_admin"
ln -sfn "$new_supplier" "$current_supplier"
ln -sfn "$new_mobile" "$current_mobile"
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
  if [[ -n "$previous_admin" ]]; then ln -sfn "$previous_admin" "$current_admin"; else rm -f -- "$current_admin"; fi
  if [[ -n "$previous_supplier" ]]; then ln -sfn "$previous_supplier" "$current_supplier"; else rm -f -- "$current_supplier"; fi
  if [[ -n "$previous_mobile" ]]; then ln -sfn "$previous_mobile" "$current_mobile"; else rm -f -- "$current_mobile"; fi
  systemctl restart ops-erp-api || true
  exit 1
fi

nginx -t
systemctl reload nginx
echo "Release $release_id is healthy and active."
REMOTE_SCRIPT

echo "Deployment completed: $release_id"
