#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
backup_dir="${BACKUP_DIR:-$root_dir/backups}"
status_dir="${OPS_STATUS_DIR:-$backup_dir/.status}"
target="${RESTORE_DRILL_TARGET:-ops_erp_restore_drill}"
production_db="${DB_NAME:-ops_erp}"
mkdir -p "$status_dir"

[[ "$target" =~ ^[A-Za-z0-9_]+_restore_drill$ ]] || {
  echo "RESTORE_DRILL_TARGET must end with _restore_drill." >&2; exit 1;
}
[[ "$target" != "$production_db" ]] || { echo "Restore drill target must differ from DB_NAME." >&2; exit 1; }
command -v dropdb >/dev/null 2>&1 || { echo "dropdb is required." >&2; exit 1; }
command -v createdb >/dev/null 2>&1 || { echo "createdb is required." >&2; exit 1; }

latest="$(find "$backup_dir" -maxdepth 1 -type f -name 'ops-erp-backup-*.tar.gz.age' -print | sort | tail -n 1)"
[[ -n "$latest" ]] || { echo "No encrypted backup found in $backup_dir." >&2; exit 1; }

db_args=(--host "${DB_HOST:-localhost}" --port "${DB_PORT:-5432}" --username "${DB_USERNAME:-ops_erp}")
export PGPASSWORD="${DB_PASSWORD:?DB_PASSWORD is required}"
dropdb "${db_args[@]}" --if-exists "$target"
createdb "${db_args[@]}" "$target"

cleanup() {
  if [[ "${RESTORE_DRILL_KEEP_DATABASE:-false}" != "true" ]]; then
    dropdb "${db_args[@]}" --if-exists "$target"
  fi
}
trap cleanup EXIT

# 可选对象存储恢复演练：恢复到独立演练桶（绝不触碰生产桶），验证对象恢复链路
restore_env=(RESTORE_OBJECTS=false)
drill_bucket="${RESTORE_DRILL_OBJECTS_BUCKET:-}"
if [[ -n "$drill_bucket" ]]; then
  [[ "$drill_bucket" =~ ^[A-Za-z0-9._-]+$ ]] || {
    echo "RESTORE_DRILL_OBJECTS_BUCKET may only contain [A-Za-z0-9._-]." >&2; exit 1;
  }
  command -v mc >/dev/null 2>&1 || { echo "mc is required for RESTORE_DRILL_OBJECTS_BUCKET." >&2; exit 1; }
  : "${MINIO_ENDPOINT:?MINIO_ENDPOINT is required for object drill}"
  : "${MINIO_ACCESS_KEY:?MINIO_ACCESS_KEY is required for object drill}"
  : "${MINIO_SECRET_KEY:?MINIO_SECRET_KEY is required for object drill}"
  restore_env=(RESTORE_OBJECTS=true RESTORE_OBJECTS_BUCKET="$drill_bucket" RESTORE_OBJECTS_CONFIRM="$drill_bucket")
  mc_config="$(mktemp -d "${TMPDIR:-/tmp}/ops-erp-drill-mc.XXXXXX")"
  MC_CONFIG_DIR="$mc_config" mc alias set erp-drill "$MINIO_ENDPOINT" "$MINIO_ACCESS_KEY" "$MINIO_SECRET_KEY" >/dev/null
  MC_CONFIG_DIR="$mc_config" mc rb --force "erp-drill/$drill_bucket" >/dev/null 2>&1 || true
  rm -rf -- "$mc_config"
fi

RESTORE_TARGET="$target" RESTORE_CONFIRM="$target" "${restore_env[@]}" \
  "$root_dir/scripts/restore-backup.sh" "$latest"

if [[ -n "$drill_bucket" ]]; then
  mc_config="$(mktemp -d "${TMPDIR:-/tmp}/ops-erp-drill-mc.XXXXXX")"
  MC_CONFIG_DIR="$mc_config" mc alias set erp-drill "$MINIO_ENDPOINT" "$MINIO_ACCESS_KEY" "$MINIO_SECRET_KEY" >/dev/null
  MC_CONFIG_DIR="$mc_config" mc rb --force "erp-drill/$drill_bucket" >/dev/null 2>&1 || true
  rm -rf -- "$mc_config"
fi

row_count="$(psql "${db_args[@]}" --dbname "$target" --tuples-only --no-align \
  --command 'select count(*) from flyway_schema_history where success = true')"
[[ "$row_count" =~ ^[1-9][0-9]*$ ]] || { echo "Restore drill schema verification failed." >&2; exit 1; }
cleanup
trap - EXIT
marker_tmp="$status_dir/.restore-drill-last-success.$$.tmp"
date +%s > "$marker_tmp"
mv -f -- "$marker_tmp" "$status_dir/restore-drill-last-success.epoch"
echo "Restore drill completed from $(basename "$latest") with $row_count successful migrations."
