#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
backup_file="${1:?Usage: RESTORE_TARGET=... RESTORE_CONFIRM=... scripts/restore-backup.sh BACKUP_FILE}"
restore_target="${RESTORE_TARGET:?RESTORE_TARGET is required}"
"$root_dir/scripts/verify-backup.sh" "$backup_file"

decrypt_dir=""
if [[ "$backup_file" == *.age ]]; then
  command -v age >/dev/null 2>&1 || { echo "age is required to restore encrypted backups." >&2; exit 1; }
  : "${BACKUP_ENCRYPTION_IDENTITY:?BACKUP_ENCRYPTION_IDENTITY is required}"
  decrypt_dir="$(mktemp -d "${TMPDIR:-/tmp}/ops-erp-restore-decrypt.XXXXXX")"
  decrypted="$decrypt_dir/$(basename "${backup_file%.age}")"
  age --decrypt --identity "$BACKUP_ENCRYPTION_IDENTITY" --output "$decrypted" "$backup_file"
  backup_file="$decrypted"
  trap 'rm -rf -- "$decrypt_dir"' EXIT
fi

[[ "${RESTORE_CONFIRM:-}" == "$restore_target" ]] || {
  echo "Refusing restore: set RESTORE_CONFIRM exactly to RESTORE_TARGET ($restore_target)." >&2; exit 1;
}
case "$restore_target" in postgres|template0|template1) echo "Refusing protected database target." >&2; exit 1;; esac
command -v pg_restore >/dev/null || { echo "pg_restore is required" >&2; exit 1; }

restore_dir=""
database_dump="$backup_file"
if [[ "$backup_file" == *.tar.gz ]]; then
  case "${RESTORE_OBJECTS:-true}" in
    true|false) ;;
    *) echo "RESTORE_OBJECTS must be true or false." >&2; exit 1 ;;
  esac
  restore_dir="$(mktemp -d "${TMPDIR:-/tmp}/ops-erp-restore.XXXXXX")"
  cleanup() { rm -rf -- "$restore_dir"; [[ -z "$decrypt_dir" ]] || rm -rf -- "$decrypt_dir"; }
  trap cleanup EXIT
  tar -xzf "$backup_file" -C "$restore_dir"
  database_dump="$restore_dir/postgres.dump"
  if [[ -d "$restore_dir/objects" && "${RESTORE_OBJECTS:-true}" == "true" ]]; then
    command -v mc >/dev/null 2>&1 || { echo "mc is required to restore bundled objects." >&2; exit 1; }
    : "${MINIO_ENDPOINT:?MINIO_ENDPOINT is required for object restore}"
    : "${MINIO_ACCESS_KEY:?MINIO_ACCESS_KEY is required for object restore}"
    : "${MINIO_SECRET_KEY:?MINIO_SECRET_KEY is required for object restore}"
    bucket="${MINIO_BUCKET:-ops-erp}"
    [[ "${RESTORE_OBJECTS_CONFIRM:-}" == "$bucket" ]] || {
      echo "Refusing object restore: set RESTORE_OBJECTS_CONFIRM=$bucket" >&2; exit 1;
    }
    mc_config="$(mktemp -d "$restore_dir/mc-config.XXXXXX")"
    MC_CONFIG_DIR="$mc_config" mc alias set erp-restore "$MINIO_ENDPOINT" \
      "$MINIO_ACCESS_KEY" "$MINIO_SECRET_KEY" >/dev/null
    MC_CONFIG_DIR="$mc_config" mc mb --ignore-existing "erp-restore/$bucket" >/dev/null
    MC_CONFIG_DIR="$mc_config" mc mirror --overwrite "$restore_dir/objects" "erp-restore/$bucket"
    rm -rf -- "$mc_config"
  fi
fi

PGPASSWORD="${DB_PASSWORD:-ops_erp}" pg_restore \
  --host "${DB_HOST:-localhost}" \
  --port "${DB_PORT:-5432}" \
  --username "${DB_USERNAME:-ops_erp}" \
  --dbname "$restore_target" \
  --exit-on-error --no-owner --no-privileges "$database_dump"

echo "Restore completed successfully: $restore_target"
