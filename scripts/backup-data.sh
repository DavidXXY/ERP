#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_DIR="${BACKUP_DIR:-$ROOT_DIR/backups}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-30}"
STATUS_DIR="${OPS_STATUS_DIR:-$BACKUP_DIR/.status}"
STAMP="$(date +%Y%m%d-%H%M%S)"
mkdir -p "$BACKUP_DIR" "$STATUS_DIR"

command -v pg_dump >/dev/null 2>&1 || { echo "pg_dump is required." >&2; exit 1; }
command -v age >/dev/null 2>&1 || { echo "age is required for encrypted backups." >&2; exit 1; }
: "${BACKUP_ENCRYPTION_RECIPIENT:?BACKUP_ENCRYPTION_RECIPIENT is required}"
[[ "$BACKUP_RETENTION_DAYS" =~ ^[0-9]+$ ]] || { echo "BACKUP_RETENTION_DAYS must be a non-negative integer." >&2; exit 1; }
work_dir="$(mktemp -d "$BACKUP_DIR/.ops-erp-backup-$STAMP.XXXXXX")"
cleanup() { rm -rf -- "$work_dir"; }
trap cleanup EXIT

PGPASSWORD="${DB_PASSWORD:-ops_erp}" pg_dump \
  --host "${DB_HOST:-localhost}" \
  --port "${DB_PORT:-5432}" \
  --username "${DB_USERNAME:-ops_erp}" \
  --dbname "${DB_NAME:-ops_erp}" \
  --format custom \
  --file "$work_dir/postgres.dump"

object_mode="${BACKUP_OBJECTS:-auto}"
case "$object_mode" in
  required|auto|skip) ;;
  *) echo "BACKUP_OBJECTS must be required, auto, or skip." >&2; exit 1 ;;
esac
objects_included=false
if [[ "$object_mode" != "skip" ]]; then
  if command -v mc >/dev/null 2>&1; then
    : "${MINIO_ENDPOINT:?MINIO_ENDPOINT is required for object backup}"
    : "${MINIO_ACCESS_KEY:?MINIO_ACCESS_KEY is required for object backup}"
    : "${MINIO_SECRET_KEY:?MINIO_SECRET_KEY is required for object backup}"
    bucket="${MINIO_BUCKET:-ops-erp}"
    mc_config="$(mktemp -d "$work_dir/mc-config.XXXXXX")"
    MC_CONFIG_DIR="$mc_config" mc alias set erp-backup "$MINIO_ENDPOINT" \
      "$MINIO_ACCESS_KEY" "$MINIO_SECRET_KEY" >/dev/null
    mkdir -p "$work_dir/objects"
    MC_CONFIG_DIR="$mc_config" mc mirror --overwrite "erp-backup/$bucket" "$work_dir/objects"
    rm -rf -- "$mc_config"
    objects_included=true
  elif [[ "$object_mode" == "required" ]]; then
    echo "mc is required when BACKUP_OBJECTS=required." >&2
    exit 1
  else
    echo "Warning: mc is unavailable; creating a database-only backup. Use BACKUP_OBJECTS=required in production." >&2
  fi
fi

{
  echo "format=ops-erp-backup-v2"
  echo "created_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "database=${DB_NAME:-ops_erp}"
  echo "object_bucket=${MINIO_BUCKET:-ops-erp}"
  echo "objects_included=$objects_included"
} > "$work_dir/manifest.env"

(
  cd "$work_dir"
  shasum -a 256 postgres.dump manifest.env > checksums.sha256
  if [[ -d objects ]]; then
    find objects -type f -exec shasum -a 256 {} + >> checksums.sha256
  fi
)

plain_archive="$work_dir/ops-erp-backup-$STAMP.tar.gz"
output="$BACKUP_DIR/ops-erp-backup-$STAMP.tar.gz.age"
tar_items=(manifest.env checksums.sha256 postgres.dump)
if [[ -d "$work_dir/objects" ]]; then tar_items+=(objects); fi
tar -C "$work_dir" -czf "$plain_archive" "${tar_items[@]}"
"$ROOT_DIR/scripts/verify-backup.sh" "$plain_archive"
age --encrypt --recipient "$BACKUP_ENCRYPTION_RECIPIENT" --output "$output" "$plain_archive"
shasum -a 256 "$output" > "${output}.sha256"

if [[ -n "${BACKUP_OFFSITE_REMOTE:-}" ]]; then
  command -v rclone >/dev/null 2>&1 || { echo "rclone is required for BACKUP_OFFSITE_REMOTE." >&2; exit 1; }
  rclone copyto "$output" "${BACKUP_OFFSITE_REMOTE%/}/$(basename "$output")"
  rclone copyto "${output}.sha256" "${BACKUP_OFFSITE_REMOTE%/}/$(basename "${output}.sha256")"
fi

while IFS= read -r -d '' expired; do
  rm -f -- "$expired" "${expired}.sha256"
done < <(find "$BACKUP_DIR" -maxdepth 1 -type f -name 'ops-erp-backup-*.tar.gz.age' -mtime "+$BACKUP_RETENTION_DAYS" -print0)
marker_tmp="$STATUS_DIR/.backup-last-success.$STAMP.tmp"
date +%s > "$marker_tmp"
mv -f -- "$marker_tmp" "$STATUS_DIR/backup-last-success.epoch"
echo "$output"
