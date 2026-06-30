#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_DIR="${BACKUP_DIR:-$ROOT_DIR/backups}"
STAMP="$(date +%Y%m%d-%H%M%S)"
mkdir -p "$BACKUP_DIR"

if [[ "${BACKUP_MODE:-local}" == "postgres" ]]; then
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
else
  if lsof -nP -iTCP:8080 -sTCP:LISTEN >/dev/null 2>&1; then
    echo "Stop the local API before backing up the H2 database."
    exit 1
  fi
  local_data_dir="${OPS_ERP_DATA_DIR:-$HOME/.ops-erp-data}"
  if [[ ! -d "$local_data_dir" ]]; then
    echo "Local data directory does not exist: $local_data_dir"
    exit 1
  fi
  output="$BACKUP_DIR/ops-erp-local-$STAMP.tar.gz"
  tar -czf "$output" -C "$(dirname "$local_data_dir")" "$(basename "$local_data_dir")"
fi

echo "$output"
