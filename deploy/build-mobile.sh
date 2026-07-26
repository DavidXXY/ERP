#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
MANIFEST="$ROOT_DIR/apps/mobile/src/manifest.json"

if [ -z "${MOBILE_API_BASE_URL:-}" ]; then
  echo "MOBILE_API_BASE_URL is required, for example https://api.erp.example.com/api"
  exit 1
fi

if rg -q 'wx0000000000000000' "$MANIFEST"; then
  echo "Replace the placeholder mp-weixin AppID in apps/mobile/src/manifest.json before release"
  exit 1
fi

cd "$ROOT_DIR"
npm run mobile:typecheck
npm run mobile:test
VITE_API_BASE_URL="$MOBILE_API_BASE_URL" npm run mobile:build

echo "WeChat build: $ROOT_DIR/apps/mobile/dist/build/mp-weixin"
