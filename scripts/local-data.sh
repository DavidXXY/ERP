#!/usr/bin/env bash
set -euo pipefail

DATA_DIR="${OPS_ERP_LOCAL_DATA_DIR:-$HOME/.ops-erp-data}"
DB_BASE="${OPS_ERP_LOCAL_DB_BASE:-$DATA_DIR/ops_erp}"
DB_FILE="$DB_BASE.mv.db"
BACKUP_DIR="$DATA_DIR/backups"

usage() {
  echo "Usage: $0 <status|backup|reset>"
  echo "  status  Show local H2 database location and size"
  echo "  backup  Copy the local H2 database to $BACKUP_DIR"
  echo "  reset   Backup then remove local H2 database files"
}

status() {
  echo "Local data directory: $DATA_DIR"
  echo "H2 database base:     $DB_BASE"
  if [[ -f "$DB_FILE" ]]; then
    ls -lh "$DB_FILE"
  else
    echo "Database file not found."
  fi
}

backup() {
  mkdir -p "$BACKUP_DIR"
  if [[ ! -f "$DB_FILE" ]]; then
    echo "Nothing to backup: $DB_FILE does not exist."
    return 0
  fi
  local stamp
  stamp="$(date '+%Y%m%d-%H%M%S')"
  cp "$DB_FILE" "$BACKUP_DIR/ops_erp-$stamp.mv.db"
  echo "Backup created: $BACKUP_DIR/ops_erp-$stamp.mv.db"
}

reset_db() {
  backup
  rm -f "$DB_BASE".mv.db "$DB_BASE".trace.db "$DB_BASE".lock.db
  echo "Local H2 database removed. It will be recreated on the next local API start."
}

case "${1:-}" in
  status) status ;;
  backup) backup ;;
  reset) reset_db ;;
  *) usage; exit 1 ;;
esac
