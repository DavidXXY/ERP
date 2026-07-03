#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SPEC_FILE="$ROOT_DIR/openapi/schema.json"
OUT_FILE="$ROOT_DIR/src/api.gen.d.ts"

echo "=== Fetching OpenAPI spec from backend ==="
curl -sf http://localhost:8080/v3/api-docs -o "$SPEC_FILE"
if [ ! -s "$SPEC_FILE" ]; then
  echo "Error: Empty or missing spec. Is the API running on port 8080?"
  exit 1
fi
echo "Spec saved ($(wc -c < "$SPEC_FILE") bytes)"

echo "=== Generating TypeScript types ==="
cd "$ROOT_DIR"
npx openapi-typescript "$SPEC_FILE" -o "$OUT_FILE"
echo "Done! Generated $(wc -l < "$OUT_FILE") lines → $OUT_FILE"
