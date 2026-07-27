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
npm ci
npm ci --prefix apps/admin
npm ci --prefix apps/mobile
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

必须配置 `JWT_SECRET`、`DATA_ENCRYPTION_KEY`、`REDIS_PASSWORD`、数据库密码和首次启动管理员密码 `BOOTSTRAP_ADMIN_PASSWORD`；两个应用密钥建议分别用 `openssl rand -hex 32` 生成。JWT 密钥默认按原始文本使用，只有 Base64 密钥才添加 `base64:` 前缀。管理员只会在用户名不存在时创建，后续重启不会覆盖已经修改过的密码。将证书链和私钥安装为 `/etc/nginx/tls/fullchain.pem`、`/etc/nginx/tls/privkey.pem` 后再启用 Nginx。

登录安全还需配置 `LOGIN_MAX_ATTEMPTS`、`LOGIN_LOCK_MINUTES` 和 `TRUSTED_PROXY_CIDRS`。可信代理只填写实际 Nginx、Ingress 或负载均衡器网段，不得使用全网段。`AUDIT_RETENTION_DAYS` 控制审计日志保留期，默认 365 天；`AUDIT_CLEANUP_BATCH_SIZE` 和 `AUDIT_CLEANUP_MAX_BATCHES` 限制单次任务的事务规模和最大工作量。

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

微信小程序的 AppID、域名、构建和审核步骤见 `docs/WECHAT_MINIPROGRAM_DEPLOYMENT.md`。小程序使用同一套 Spring Boot API，不需要单独部署应用服务器。

全新空库首先执行 `B77__fresh_install_baseline.sql`，一次性创建截至 V77 的完整结构和基础权限配置，随后继续执行 V78 之后的增量迁移。当前增量版本为 V90。

已有 V77 数据库会跳过 B77 基线并继续执行增量迁移。应用默认仅忽略已合并且不再随包发布的历史迁移缺失记录，仍校验当前发布迁移的顺序和校验和。升级前必须备份数据库，并先在预发布副本验证 V78 至 V90；不要关闭 Flyway 当前迁移校验。

```bash
# 后端健康检查
curl http://127.0.0.1:8080/actuator/health
# 生产 profile 使用 MinIO 时，health 结果中应包含 minio 存储检查；MinIO 控制台默认在 http://localhost:9001

# Prometheus 与后端同机时直接抓取；Nginx 也只允许回环地址访问指标
curl http://127.0.0.1:8080/actuator/prometheus

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

# 生成并校验 PostgreSQL + MinIO 联合备份（生产必须安装 mc）
BACKUP_OBJECTS=required DB_PASSWORD='...' \
  MINIO_ENDPOINT='http://127.0.0.1:9000' MINIO_ACCESS_KEY='...' \
  MINIO_SECRET_KEY='...' MINIO_BUCKET='ops-erp' \
  ../scripts/backup-data.sh

# 恢复到预先创建的演练库和隔离 bucket（对象先恢复，数据库后恢复）
RESTORE_TARGET=ops_erp_restore_drill RESTORE_CONFIRM=ops_erp_restore_drill \
  RESTORE_OBJECTS_CONFIRM=ops-erp-drill MINIO_BUCKET=ops-erp-drill \
  DB_PASSWORD='...' MINIO_ENDPOINT='http://127.0.0.1:9000' \
  MINIO_ACCESS_KEY='...' MINIO_SECRET_KEY='...' \
  ../scripts/restore-backup.sh ../backups/ops-erp-backup-YYYYMMDD-HHMMSS.tar.gz
```

备份包包含 `manifest.env`、内部逐文件校验、`postgres.dump` 和可选的 `objects/`，外层另有 `.sha256`。`verify-backup.sh` 仍兼容旧 `.dump`，但新生产备份应统一使用 V2 联合包。恢复演练不得使用生产数据库名或生产 bucket；若只需演练数据库，可显式设置 `RESTORE_OBJECTS=false`。

### 数据加密密钥轮换

数据库密文新格式为 `ENC2:<keyId>:<payload>`，旧 `ENC1` 仍可读。轮换必须按以下顺序进行：

1. 备份并验证联合备份，在预发布环境确认旧密文可读取。
2. 将当前密钥加入 `DATA_ENCRYPTION_PREVIOUS_KEYS`，例如 `primary=<旧密钥>`。
3. 生成新密钥，设置新的 `DATA_ENCRYPTION_KEY_ID`（例如 `2026q3`）和 `DATA_ENCRYPTION_KEY`，滚动重启应用。
4. 验证旧数据读取和新增/更新数据写入；新写入应使用新的 `ENC2:2026q3:` 前缀。
5. 在所有旧密文经受控读写完成重加密前，保留历史密钥。不得提前删除 `ENC1` 所需密钥或仍被引用的 key ID。

历史密钥格式为分号分隔的 `keyId=secret`。密钥应由密钥管理系统或权限为 `600` 的环境文件注入，不得提交仓库。

### Prometheus

生产应用只在 `127.0.0.1:8080` 监听，Actuator 只暴露 `health,prometheus`。示例抓取配置位于 `deploy/monitoring/prometheus.yml`，告警规则位于同目录 `alerts.yml`。Prometheus 与应用同机时直接抓取 `127.0.0.1:8080`；跨主机部署应通过防火墙、私网或 mTLS 单独授权，不要放开当前 Nginx 的公网限制。

## 生产安全清单

- [ ] 修改 `/etc/ops-erp/ops-erp.env` 中所有默认密码
- [ ] 生成随机 JWT 密钥：`openssl rand -hex 32`
- [ ] 配置登录失败阈值和锁定窗口，并验证 Redis 正常工作
- [ ] `TRUSTED_PROXY_CIDRS` 只包含实际反向代理网段，不包含全公网
- [ ] 验证修改密码、重置密码和停用账号后旧 JWT 立即失效
- [ ] 生成独立的数据加密密钥并离线托管；轮换时配置新 key ID，并保留仍被密文引用的历史密钥
- [ ] 确认 `application-prod.yml` 中 `ddl-auto: validate`（不会自动改表结构）
- [ ] 确认 Nginx 已配置 HTTPS（Let's Encrypt 或自签名）
- [ ] 确认 `/actuator/health` 不对外暴露敏感信息，`/actuator/prometheus` 仅监控主机可访问
- [ ] 确认 Actuator 仅暴露 `health,prometheus`，其他 `/actuator/` 路径返回 404
- [ ] 配置防火墙，仅开放 80/443 端口对外
- [ ] 使用隔离数据库和 bucket 定期运行联合恢复演练，而不只是检查备份文件存在
- [ ] 关闭 Docker 基础设施端口的外部访问（已配置 `127.0.0.1:`）
- [ ] MinIO bucket 保持私有读写，通过后端接口或预签名链接访问附件
- [ ] 配置 MinIO 生命周期策略，按公司档案保留制度清理临时/过期对象
- [ ] 按公司制度设置审计保留期、批大小和最大批次数，确认每日短事务清理任务正常运行
- [ ] 使用两个账号验证通知已读状态隔离和审批通知私有投递
