#!/usr/bin/env bash
set -euo pipefail

backup_file="${1:?Usage: scripts/verify-backup.sh BACKUP_FILE}"
if [[ ! -f "$backup_file" ]]; then
  echo "Backup does not exist: $backup_file" >&2
  exit 1
fi

case "$backup_file" in
  *.dump)
    command -v pg_restore >/dev/null || { echo "pg_restore is required" >&2; exit 1; }
    pg_restore --list "$backup_file" >/dev/null
    ;;
  *.tar.gz|*.tgz)
    tar -tzf "$backup_file" >/dev/null
    ;;
  *)
    echo "Unsupported backup format: $backup_file" >&2
    exit 1
    ;;
esac

checksum_file="${backup_file}.sha256"
if [[ -f "$checksum_file" ]]; then
  shasum -a 256 -c "$checksum_file"
fi
echo "Backup integrity verified: $backup_file"
