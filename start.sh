#!/usr/bin/env bash
set -euo pipefail

echo "=== 启动 ERP 前后端 ==="

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

echo "启动 PostgreSQL、Redis 和 MinIO..."
npm run infra:up

echo "启动后端 (Spring Boot + PostgreSQL)..."
npm run api:dev &
API_PID=$!

echo "启动前端 (Vite)..."
npm run admin:dev &
ADMIN_PID=$!

cleanup() {
  kill "$API_PID" "$ADMIN_PID" 2>/dev/null || true
}
trap cleanup EXIT INT TERM

sleep 5
echo ""
echo "✅ 后端: http://localhost:8080"
echo "✅ 前端: http://localhost:5174"
echo ""
echo "按 Ctrl+C 停止所有服务"
wait "$API_PID" "$ADMIN_PID"
