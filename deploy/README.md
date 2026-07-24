# 工程运维 ERP — 部署指南

## 部署架构

```
┌──────────┐       ┌──────────────┐       ┌────────────┐
│  浏览器   │ ───→ │   Nginx      │ ───→ │  Spring     │
│          │       │ HTTPS 443    │       │  后端 8080  │
└──────────┘       │              │       └─────┬──────┘
                   │ / → dist/    │             │
                   │ /api/ → 8080 │       ┌─────┴──────┐
                   └──────────────┘       │ PostgreSQL │
                                          │ Redis      │
                                          │ MinIO/S3   │
                                          └────────────┘
```

## 目录结构

```
deploy/
├── README.md              ← 本文档
├── build.sh               ← 本地构建脚本
├── deploy.sh              ← 部署推送脚本
├── ops-erp.nginx.conf     ← Nginx 站点配置
├── ops-erp-api.service    ← systemd 服务单元
├── ops-erp.env.example    ← 环境变量模板
├── docker-compose.yml     ← Docker 基础设施（PostgreSQL / Redis / MinIO）
└── docker-daemon.json     ← Docker 镜像加速配置
```

## 两种部署方式

### 方式一：手动部署（推荐）

**第 1 步 — 在开发机本地构建**

```bash
./deploy/build.sh
```

自动执行：
- `mvn clean package -DskipTests` → 产出 `services/api/target/ops-erp-api-0.1.0.jar`
- `npm run build` → 产出 `apps/admin/dist/`

**第 2 步 — 推送至正式服务器**

```bash
./deploy/deploy.sh root@10.10.10.111
```

脚本会自动执行：
- `rsync` 推送 `dist/` → 服务器的 `/var/www/ops-erp-admin/`
- `rsync` 推送 `ops-erp-api.jar` → 服务器的 `/opt/engineering-ops-erp/`
- 推送 Nginx 和 systemd 配置

**第 3 步 — 服务器上初始化环境（仅首次）**

```bash
# 创建操作系统用户和目录
sudo useradd -r -s /sbin/nologin -M ops-erp
sudo mkdir -p /opt/engineering-ops-erp /var/www/ops-erp-admin /etc/ops-erp
sudo mkdir -p /etc/nginx/tls
sudo chown ops-erp:ops-erp /opt/engineering-ops-erp

# 配置环境变量
sudo cp deploy/ops-erp.env.example /etc/ops-erp/ops-erp.env
sudo chmod 600 /etc/ops-erp/ops-erp.env
sudo vi /etc/ops-erp/ops-erp.env   # 修改密码等敏感信息
```

必须配置 `JWT_SECRET`、`DATA_ENCRYPTION_KEY`、`REDIS_PASSWORD`、数据库密码和首次启动管理员密码 `BOOTSTRAP_ADMIN_PASSWORD`；两个应用密钥建议分别用 `openssl rand -hex 32` 生成。管理员只会在用户名不存在时创建，后续重启不会覆盖已经修改过的密码。将证书链和私钥安装为 `/etc/nginx/tls/fullchain.pem`、`/etc/nginx/tls/privkey.pem` 后再启用 Nginx。

`STORAGE_TYPE=minio` 时后端使用 MinIO/S3 兼容对象存储，`MINIO_ENDPOINT`、`MINIO_ACCESS_KEY`、`MINIO_SECRET_KEY`、`MINIO_BUCKET` 必须与 Docker Compose 或外部对象存储服务一致。`MINIO_PRESIGNED_EXPIRY_SECONDS` 控制附件临时下载链接有效期，建议生产保持 5-15 分钟。开发或单机调试可改为 `STORAGE_TYPE=local`，文件会写入 `LOCAL_STORAGE_PATH` 或默认本地目录。

```bash
# 启动基础设施（Docker）
cd deploy
docker compose up -d
```

**第 4 步 — 启动服务**

```bash
# 启动后端
sudo systemctl daemon-reload
sudo systemctl enable --now ops-erp-api
sudo journalctl -u ops-erp-api -f   # 观察启动日志，确认 Flyway 迁移完成

# 配置 Nginx
sudo ln -sf /etc/nginx/sites-available/ops-erp /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

### 方式二：Docker 一键部署

```bash
cd deploy
docker compose -f docker-compose.yml up -d
```

这种方式仅启动 PostgreSQL、Redis、MinIO 三个基础服务。应用层仍需按方式一的步骤部署。

## 验证部署

全新空库首次启动只执行 `V33__fresh_install_baseline.sql`，一次性创建完整结构和基础权限配置。后续数据库变更从 `V100` 开始编号，以兼容曾经执行到 V77 的旧部署。

已有 V77 数据库首次切换到基线构建时，需要在环境文件中增加 `SPRING_FLYWAY_IGNORE_MIGRATION_PATTERNS=*:missing,*:ignored` 和 `SPRING_FLYWAY_VALIDATE_ON_MIGRATE=false`。这只用于兼容已执行但不再随包发布的旧迁移，以及跳过低于 V77 的 V33 基线；全新数据库不应设置这两项，仍使用严格校验。

```bash
# 后端健康检查
curl http://127.0.0.1:8080/actuator/health
# 生产 profile 使用 MinIO 时，health 结果中应包含 minio 存储检查；MinIO 控制台默认在 http://localhost:9001

# API 可达性
curl -s https://erp.example.com/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"<BOOTSTRAP_ADMIN_PASSWORD>"}'

# 前端可达性
curl -s -o /dev/null -w "%{http_code}" https://erp.example.com/
```

## 日常运维

```bash
# 查看后端日志
sudo journalctl -u ops-erp-api -f --since "1 hour ago"

# 重启后端
sudo systemctl restart ops-erp-api

# 重新部署（仅推送变动的文件）
./deploy/build.sh && ./deploy/deploy.sh root@10.10.10.111

# 生成并校验 PostgreSQL 备份
DB_PASSWORD='...' ../scripts/backup-data.sh

# 恢复到预先创建的演练库（不得使用生产库名）
RESTORE_TARGET=ops_erp_restore_drill RESTORE_CONFIRM=ops_erp_restore_drill \
  DB_PASSWORD='...' ../scripts/restore-backup.sh ../backups/ops-erp-postgres-YYYYMMDD-HHMMSS.dump
```

## 生产安全清单

- [ ] 修改 `/etc/ops-erp/ops-erp.env` 中所有默认密码
- [ ] 生成随机 JWT 密钥：`openssl rand -hex 32`
- [ ] 生成独立的数据加密密钥并离线托管；轮换前制定旧密钥解密与重加密方案
- [ ] 确认 `application-prod.yml` 中 `ddl-auto: validate`（不会自动改表结构）
- [ ] 确认 Nginx 已配置 HTTPS（Let's Encrypt 或自签名）
- [ ] 确认 `/actuator/health` 不对外暴露敏感信息
- [ ] 限制 `management.endpoints.web.exposure.include` 为最小集
- [ ] 配置防火墙，仅开放 80/443 端口对外
- [ ] 定期运行恢复演练，而不只是检查备份文件存在
- [ ] 关闭 Docker 基础设施端口的外部访问（已配置 `127.0.0.1:`）
- [ ] MinIO bucket 保持私有读写，通过后端接口或预签名链接访问附件
- [ ] 配置 MinIO 生命周期策略，按公司档案保留制度清理临时/过期对象
