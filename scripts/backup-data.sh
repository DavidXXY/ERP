#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_DIR="${BACKUP_DIR:-$ROOT_DIR/backups}"
STAMP="$(date +%Y%m%d-%H%M%S)"
mkdir -p "$BACKUP_DIR"

if ! command -v pg_dump >/dev/null 2>&1; then
  echo "pg_dump is required for PostgreSQL backup."
  exit 1
fi
output="$BACKUP_DIR/ops-erp-postgres-$STAMP.dump"
PGPASSWORD="${DB_PASSWORD:-ops_erp}" pg_dump \
  --host "${DB_HOST:-localhost}" \
  --port "${DB_PORT:-5432}" \
  --username "${DB_USERNAME:-ops_erp}" \
  --dbname "${DB_NAME:-ops_erp}" \
  --format custom \
  --file "$output"

shasum -a 256 "$output" > "${output}.sha256"
"$ROOT_DIR/scripts/verify-backup.sh" "$output"
echo "$output"
