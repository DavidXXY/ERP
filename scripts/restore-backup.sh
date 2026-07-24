#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
backup_file="${1:?Usage: RESTORE_TARGET=... RESTORE_CONFIRM=... scripts/restore-backup.sh BACKUP_FILE}"
restore_target="${RESTORE_TARGET:?RESTORE_TARGET is required}"

"$root_dir/scripts/verify-backup.sh" "$backup_file"

if [[ "${RESTORE_CONFIRM:-}" != "$restore_target" ]]; then
  echo "Refusing restore: set RESTORE_CONFIRM exactly to RESTORE_TARGET ($restore_target)." >&2
  exit 1
fi

case "$backup_file" in
  *.dump) ;;
  *)
    echo "Unsupported backup format: expected a PostgreSQL .dump file" >&2
    exit 1
    ;;
esac

command -v pg_restore >/dev/null || { echo "pg_restore is required" >&2; exit 1; }
if [[ "$restore_target" == "postgres" || "$restore_target" == "template0" || "$restore_target" == "template1" ]]; then
  echo "Refusing to restore into a protected PostgreSQL database." >&2
  exit 1
fi
PGPASSWORD="${DB_PASSWORD:-ops_erp}" pg_restore \
  --host "${DB_HOST:-localhost}" \
  --port "${DB_PORT:-5432}" \
  --username "${DB_USERNAME:-ops_erp}" \
  --dbname "$restore_target" \
  --exit-on-error \
  --no-owner \
  --no-privileges \
  "$backup_file"

echo "Restore completed successfully: $restore_target"
