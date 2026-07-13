#!/bin/bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BUILD_TIME="$(date '+%Y-%m-%d %H:%M:%S')"

echo "═══════════════════════════════════════════"
echo "  工程运维 ERP — 生产构建"
echo "  构建时间: $BUILD_TIME"
echo "═══════════════════════════════════════════"
echo ""

# ── 后端构建 ──
echo "▸ 构建后端 (Spring Boot + Maven) ……"
cd "$ROOT_DIR/services/api"

mvn clean package -DskipTests -q
BACKEND_JAR="$ROOT_DIR/services/api/target/ops-erp-api-0.1.0.jar"
ALT_BACKEND_JAR="/private/tmp/ops-erp-build/api/ops-erp-api-0.1.0.jar"

if [ -f "$ALT_BACKEND_JAR" ]; then
  mkdir -p "$ROOT_DIR/services/api/target"
  cp "$ALT_BACKEND_JAR" "$BACKEND_JAR"
fi

if [ -f "$BACKEND_JAR" ]; then
  JAR_SIZE=$(du -h "$BACKEND_JAR" | cut -f1)
  echo "  ✅ 后端构建完成: $JAR_SIZE"
else
  echo "  ❌ 后端 JAR 未生成，构建失败"
  exit 1
fi

echo ""

# ── 前端构建 ──
echo "▸ 构建前端 (Vue + Vite) ……"
cd "$ROOT_DIR/apps/admin"
npm run build --silent 2>/dev/null

FRONTEND_DIST="$ROOT_DIR/apps/admin/dist"
if [ -d "$FRONTEND_DIST" ]; then
  DIST_SIZE=$(du -sh "$FRONTEND_DIST" | cut -f1)
  echo "  ✅ 前端构建完成: $DIST_SIZE"
else
  echo "  ❌ 前端 dist/ 未生成，构建失败"
  exit 1
fi

echo ""
echo "═══════════════════════════════════════════"
echo "  构建产物"
echo "═══════════════════════════════════════════"
echo "  后端 JAR: $BACKEND_JAR"
echo "  前端目录: $FRONTEND_DIST/"
echo "  后端配置: services/api/src/main/resources/application-prod.yml"
echo "  Nginx 配置: deploy/ops-erp.nginx.conf"
echo "  Systemd 单元: deploy/ops-erp-api.service"
echo "  Docker 环境: deploy/docker-compose.yml"
echo ""
echo "  部署方式选择:"
echo "    1) docker compose up -d    — Docker 一键部署（推荐）"
echo "    2) 手动部署                  — 复制 JAR + dist/ 到服务器"
echo "═══════════════════════════════════════════"
