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

RESTORE_TARGET="$target" RESTORE_CONFIRM="$target" RESTORE_OBJECTS=false \
  "$root_dir/scripts/restore-backup.sh" "$latest"
row_count="$(psql "${db_args[@]}" --dbname "$target" --tuples-only --no-align \
  --command 'select count(*) from flyway_schema_history where success = true')"
[[ "$row_count" =~ ^[1-9][0-9]*$ ]] || { echo "Restore drill schema verification failed." >&2; exit 1; }
cleanup
trap - EXIT
marker_tmp="$status_dir/.restore-drill-last-success.$$.tmp"
date +%s > "$marker_tmp"
mv -f -- "$marker_tmp" "$status_dir/restore-drill-last-success.epoch"
echo "Restore drill completed from $(basename "$latest") with $row_count successful migrations."
