#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DB_HOST_VALUE="${DB_HOST:-localhost}"
DB_PORT_VALUE="${DB_PORT:-5432}"
DB_NAME_VALUE="${DB_NAME:-ops_erp}"
DB_USERNAME_VALUE="${DB_USERNAME:-ops_erp}"

if [[ "$DB_HOST_VALUE" != "localhost" && "$DB_HOST_VALUE" != "127.0.0.1" \
    && "${ALLOW_REMOTE_DEMO_DATA:-false}" != "true" ]]; then
  echo "Refusing to seed a remote database. Set ALLOW_REMOTE_DEMO_DATA=true to override." >&2
  exit 1
fi

if ! command -v psql >/dev/null 2>&1; then
  echo "psql is required to seed demo data." >&2
  exit 1
fi

export PGPASSWORD="${DB_PASSWORD:-ops_erp}"

psql \
  --host "$DB_HOST_VALUE" \
  --port "$DB_PORT_VALUE" \
  --username "$DB_USERNAME_VALUE" \
  --dbname "$DB_NAME_VALUE" \
  --set ON_ERROR_STOP=1 \
  --file "$ROOT_DIR/scripts/seed-demo-data.sql"

