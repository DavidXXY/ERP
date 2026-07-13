#!/bin/bash
set -euo pipefail

# ═══════════════════════════════════════════
#  部署推送脚本
#  用法:
#    ./deploy/deploy.sh user@10.10.10.111 [/deploy/path]
#  前提:
#    - 先在本机执行 ./deploy/build.sh
#    - 服务器上已配置好 ops-erp.env
# ═══════════════════════════════════════════

if [ $# -lt 1 ]; then
  echo "用法: $0 <user@host> [远程路径]"
  echo "示例: $0 root@10.10.10.111 /opt/engineering-ops-erp"
  exit 1
fi

REMOTE_HOST="$1"
REMOTE_DIR="${2:-/opt/engineering-ops-erp}"
FRONTEND_DIR="${3:-/var/www/ops-erp-admin}"
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BUILD_TIME="$(date '+%Y-%m-%d %H:%M:%S')"

echo "═══════════════════════════════════════════"
echo "  工程运维 ERP — 部署推送"
echo "  目标主机: $REMOTE_HOST"
echo "  构建时间: $BUILD_TIME"
echo "═══════════════════════════════════════════"

# ── 确认构建产物存在 ──
BACKEND_JAR="$ROOT_DIR/services/api/target/ops-erp-api-0.1.0.jar"
ALT_BACKEND_JAR="/private/tmp/ops-erp-build/api/ops-erp-api-0.1.0.jar"
if [ -f "$ALT_BACKEND_JAR" ]; then
  mkdir -p "$ROOT_DIR/services/api/target"
  cp "$ALT_BACKEND_JAR" "$BACKEND_JAR"
fi

if [ ! -f "$BACKEND_JAR" ]; then
  echo "❌ 未找到后端 JAR，请先执行 ./deploy/build.sh"
  exit 1
fi

FRONTEND_DIST="$ROOT_DIR/apps/admin/dist"
if [ ! -d "$FRONTEND_DIST" ]; then
  echo "❌ 未找到前端 dist/ 目录，请先执行 ./deploy/build.sh"
  exit 1
fi

# ── 前端推送 ──
echo ""
echo "▸ 推送前端到 $REMOTE_HOST:$FRONTEND_DIR ……"
ssh "$REMOTE_HOST" "mkdir -p $FRONTEND_DIR"
rsync -ahz --delete "$FRONTEND_DIST/" "$REMOTE_HOST:$FRONTEND_DIR/"
echo "  ✅ 前端部署完成"

# ── 后端推送 ──
echo ""
echo "▸ 推送后端到 $REMOTE_HOST:$REMOTE_DIR ……"
ssh "$REMOTE_HOST" "mkdir -p $REMOTE_DIR"
rsync -ahz "$BACKEND_JAR" "$REMOTE_HOST:$REMOTE_DIR/ops-erp-api.jar"
echo "  ✅ 后端部署完成"

# ── 推送 Nginx 和 Systemd 配置 ──
echo ""
echo "▸ 推送 Nginx 和 Systemd 配置 ……"
rsync -ahz "$ROOT_DIR/deploy/ops-erp.nginx.conf" "$REMOTE_HOST:/etc/nginx/sites-available/ops-erp"
echo "  ⚡ 请登录服务器执行:"
echo "     ln -sf /etc/nginx/sites-available/ops-erp /etc/nginx/sites-enabled/"
echo "     nginx -t && systemctl reload nginx"
rsync -ahz "$ROOT_DIR/deploy/ops-erp-api.service" "$REMOTE_HOST:/etc/systemd/system/"
echo "  ⚡ 请登录服务器执行: systemctl daemon-reload"

# ── 服务重启提示 ──
echo ""
echo "═══════════════════════════════════════════"
echo "  推送完成！下一步（登录服务器执行）:"
echo "═══════════════════════════════════════════"
echo ""
echo "  # 1. 确认环境变量"
echo "  cat /etc/ops-erp/ops-erp.env"
echo ""
echo "  # 2. 重启后端（生产切换）"
echo "  sudo systemctl restart ops-erp-api"
echo "  sudo journalctl -u ops-erp-api -f"
echo ""
echo "  # 3. 重载 Nginx"
echo "  sudo nginx -t && sudo systemctl reload nginx"
echo ""
echo "  # 4. 查看运行状态"
echo "  sudo systemctl status ops-erp-api"
echo "  curl http://localhost:8080/actuator/health"
echo "═══════════════════════════════════════════"
