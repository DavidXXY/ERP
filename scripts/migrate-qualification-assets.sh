#!/usr/bin/env bash
set -euo pipefail

SOURCE_DIR="${QUALIFICATION_SOURCE_DIR:-.local-data/quarantine/qualification-assets}"
SOURCE_JSON="${QUALIFICATION_SOURCE_JSON:-.local-data/quarantine/qualification-import.json}"
OUTPUT_JSON="${QUALIFICATION_OUTPUT_JSON:-.local-data/qualification-import.private.json}"
MINIO_ALIAS="${QUALIFICATION_MINIO_ALIAS:-ops-erp-migration}"
MINIO_PREFIX="${QUALIFICATION_MINIO_PREFIX:-qualification/legacy}"

for command_name in mc jq; do
  command -v "${command_name}" >/dev/null 2>&1 || {
    echo "Required command is missing: ${command_name}" >&2
    exit 1
  }
done

[[ -d "${SOURCE_DIR}" ]] || { echo "Source directory not found: ${SOURCE_DIR}" >&2; exit 1; }
[[ -f "${SOURCE_JSON}" ]] || { echo "Source manifest not found: ${SOURCE_JSON}" >&2; exit 1; }
: "${MINIO_ENDPOINT:?MINIO_ENDPOINT is required}"
: "${MINIO_ACCESS_KEY:?MINIO_ACCESS_KEY is required}"
: "${MINIO_SECRET_KEY:?MINIO_SECRET_KEY is required}"
: "${MINIO_BUCKET:?MINIO_BUCKET is required}"

mc alias set "${MINIO_ALIAS}" "${MINIO_ENDPOINT}" "${MINIO_ACCESS_KEY}" "${MINIO_SECRET_KEY}"
mc mb --ignore-existing "${MINIO_ALIAS}/${MINIO_BUCKET}"
mc anonymous set none "${MINIO_ALIAS}/${MINIO_BUCKET}"
mc mirror --overwrite "${SOURCE_DIR}" "${MINIO_ALIAS}/${MINIO_BUCKET}/${MINIO_PREFIX}"

mkdir -p "$(dirname "${OUTPUT_JSON}")"
jq 'walk(if type == "object" and has("dataUrl") and (.dataUrl | type) == "string" and (.dataUrl | startswith("/qualification-assets/")) then .dataUrl = (.dataUrl | sub("^/qualification-assets/"; "/api/qualification-files/legacy/")) else . end)' \
  "${SOURCE_JSON}" > "${OUTPUT_JSON}.tmp"
mv "${OUTPUT_JSON}.tmp" "${OUTPUT_JSON}"
chmod 600 "${OUTPUT_JSON}"

echo "Migration complete. Start the API once with:"
echo "QUALIFICATION_IMPORT_ENABLED=true QUALIFICATION_IMPORT_FILE=${OUTPUT_JSON}"
