#!/usr/bin/env bash
set -euo pipefail

backup_file="${1:?Usage: scripts/verify-backup.sh BACKUP_FILE}"
[[ -f "$backup_file" ]] || { echo "Backup does not exist: $backup_file" >&2; exit 1; }
command -v pg_restore >/dev/null || { echo "pg_restore is required" >&2; exit 1; }

checksum_file="${backup_file}.sha256"
if [[ -f "$checksum_file" ]]; then
  expected_checksum="$(sed -n '1{s/[[:space:]].*$//;p;}' "$checksum_file")"
  [[ "$expected_checksum" =~ ^[0-9a-fA-F]{64}$ ]] || {
    echo "Backup checksum file is invalid." >&2; exit 1;
  }
  actual_checksum="$(shasum -a 256 "$backup_file" | sed 's/[[:space:]].*$//')"
  [[ "$actual_checksum" == "$expected_checksum" ]] || {
    echo "Backup archive checksum mismatch." >&2; exit 1;
  }
fi

case "$backup_file" in
  *.dump)
    pg_restore --list "$backup_file" >/dev/null
    ;;
  *.tar.gz)
    verify_dir="$(mktemp -d "${TMPDIR:-/tmp}/ops-erp-verify.XXXXXX")"
    cleanup() { rm -rf -- "$verify_dir"; }
    trap cleanup EXIT
    while IFS= read -r listing; do
      case "${listing:0:1}" in
        -|d) ;;
        *) echo "Backup contains a non-regular archive entry." >&2; exit 1 ;;
      esac
    done < <(tar -tvzf "$backup_file")
    while IFS= read -r entry; do
      case "$entry" in
        manifest.env|checksums.sha256|postgres.dump|objects|objects/*) ;;
        *) echo "Backup contains an unsafe or unexpected path: $entry" >&2; exit 1 ;;
      esac
      case "/$entry/" in
        */../*) echo "Backup contains path traversal: $entry" >&2; exit 1 ;;
      esac
    done < <(tar -tzf "$backup_file")
    tar -xzf "$backup_file" -C "$verify_dir"
    [[ -f "$verify_dir/manifest.env" && -f "$verify_dir/checksums.sha256" && -f "$verify_dir/postgres.dump" ]] || {
      echo "Backup bundle is incomplete." >&2; exit 1;
    }
    grep -qx 'format=ops-erp-backup-v2' "$verify_dir/manifest.env" || {
      echo "Unsupported backup bundle format." >&2; exit 1;
    }
    objects_included="$(sed -n 's/^objects_included=//p' "$verify_dir/manifest.env")"
    case "$objects_included" in
      true) [[ -d "$verify_dir/objects" ]] || { echo "Object payload is missing." >&2; exit 1; } ;;
      false) [[ ! -d "$verify_dir/objects" ]] || { echo "Unexpected object payload." >&2; exit 1; } ;;
      *) echo "Backup manifest has an invalid objects_included value." >&2; exit 1 ;;
    esac
    while IFS= read -r checksum_entry; do
      checksum="${checksum_entry%% *}"
      checksum_path="${checksum_entry#"$checksum"}"
      checksum_path="${checksum_path#  }"
      [[ "$checksum" =~ ^[0-9a-fA-F]{64}$ ]] || {
        echo "Backup contains an invalid internal checksum." >&2; exit 1;
      }
      case "$checksum_path" in
        manifest.env|postgres.dump|objects/*) ;;
        *) echo "Backup checksum references an unsafe path: $checksum_path" >&2; exit 1 ;;
      esac
      case "/$checksum_path/" in
        */../*) echo "Backup checksum contains path traversal." >&2; exit 1 ;;
      esac
    done < "$verify_dir/checksums.sha256"
    (cd "$verify_dir" && shasum -a 256 -c checksums.sha256) >/dev/null
    pg_restore --list "$verify_dir/postgres.dump" >/dev/null
    ;;
  *)
    echo "Unsupported backup format: expected .dump or .tar.gz" >&2
    exit 1
    ;;
esac

echo "Backup integrity verified: $backup_file"
