# 全系统优化与运维基线（2026-07-26）

本文记录第二轮全系统性能、可靠性、灾备和质量门禁优化。第一轮认证、通知隔离、CSV 安全和 V86 迁移见 `system-hardening-2026-07-26.md`，具体部署命令见 `../deploy/README.md`。

## 1. 接口与查询性能

- 所有分页控制器统一返回自有 `PageResponse`。字段继续兼容 `content`、`totalElements`、`totalPages`、`number`、`size`、`numberOfElements`、`first`、`last`、`empty`，但不再依赖 Spring Data 内部序列化格式。
- OA 审批列表只加载摘要；打开审批详情时再读取来源单据、运行节点和动作记录。
- 软删除可见性按当前租户一次读取隐藏 ID，不再逐行判断；新增、审批、恢复和回收站查询也统一绑定当前租户。
- 财务总览、总账总览、科目报表和库存价值由数据库聚合；凭证、应收、应付、付款申请、付款记录、资质数据使用数据库分页。
- 采购三单匹配、供应商财务汇总、付款预占金额只查询当前页或当前业务 ID 集合。
- BI 客户利润和设备绩效先聚合再加载相关实体；提醒任务通过到期、逾期、低库存等条件查询，通知去重键和恢复清理批量处理。
- 管理端需要完整数组的旧页面通过 `requestAllPages` 分批拉取，兼顾接口上限和现有交互。

## 2. 数据保护与生命周期

### 数据密钥

- 新密文格式为 `ENC2:<keyId>:<payload>`，新写入使用 `DATA_ENCRYPTION_KEY_ID` 指定的当前密钥。
- `DATA_ENCRYPTION_PREVIOUS_KEYS=old=secret;older=secret` 提供只读历史密钥环。
- 旧 `ENC1` 没有 key ID，读取时依次尝试当前和历史密钥；持久化已有 `ENC1`/`ENC2` 值时不会二次加密。
- 轮换顺序必须是“联合备份 -> 将旧密钥加入历史集合 -> 切换新 key ID 和密钥 -> 验证旧读新写 -> 完成受控重加密后再移除旧密钥”。

### 审计清理

- 每批查询有限 ID，并在独立短事务中批量删除，减少长事务、锁持有和复制延迟。
- `AUDIT_RETENTION_DAYS` 设置保留期，`AUDIT_CLEANUP_BATCH_SIZE` 设置每批数量，`AUDIT_CLEANUP_MAX_BATCHES` 限制单次任务最大批数。
- 应根据每天新增量监控积压；若连续触及最大批数，应在低峰期逐步调高，而不是一次无限删除。

## 3. 联合备份与恢复

V2 备份包为 `ops-erp-backup-<时间>.tar.gz`，包含：

- `postgres.dump`：PostgreSQL custom format。
- `objects/`：MinIO/S3 bucket 对象快照。
- `manifest.env`：格式、时间、数据库、bucket 和对象包含状态。
- `checksums.sha256`：包内逐文件校验；归档旁另有外层 `.sha256`。

生产执行 `BACKUP_OBJECTS=required scripts/backup-data.sh`。该模式要求已安装 MinIO Client `mc`，对象备份失败时整体失败。`auto` 只适合开发环境，`skip` 仅用于明确的数据库单备份场景。

恢复脚本兼容旧 `.dump`，V2 包默认恢复对象。对象恢复需要 `RESTORE_OBJECTS_CONFIRM` 与目标 bucket 完全一致，数据库恢复需要 `RESTORE_CONFIRM` 与目标数据库完全一致。演练必须使用隔离数据库和隔离 bucket；恢复顺序固定为对象后数据库。

## 4. 可观测性

- 生产应用绑定 `127.0.0.1:8080`，不直接接受外网连接。
- Actuator 只暴露 `/actuator/health` 和 `/actuator/prometheus`，健康详情仅对授权请求展示。
- Nginx 可代理健康检查；Prometheus 路径只允许 `127.0.0.1` 和 `::1`，其他 Actuator 路径返回 404。
- 指标带 `application=ops-erp-api` 标签，并启用 `http.server.requests` 直方图。
- `deploy/monitoring/prometheus.yml` 可加载同目录 `alerts.yml`。跨主机抓取必须另行配置私网、防火墙或 mTLS，不应直接放开公网 Nginx。

## 5. 自动化质量门禁

- Playwright 在 Chromium 桌面和移动视口覆盖登录后重定向、菜单权限、命令按钮权限和未授权深链重定向。
- Vitest 仅收集管理端 `src`，避免与浏览器测试重复收集。
- CI 安装 Chromium、运行 E2E、上传报告，并断言 PostgreSQL Testcontainers 报告 `skipped="0"`。
- 本地 Docker 不可用时 PostgreSQL 测试可以按现有条件跳过，但 CI 和发布门禁不得跳过。
- 根依赖安全审计、三套前端测试/类型检查/构建及管理端 bundle 预算继续作为发布前必检项。

## 6. 新增环境变量

| 变量 | 示例/默认值 | 用途 |
| --- | --- | --- |
| `DATA_ENCRYPTION_KEY_ID` | `primary` | 当前写入密钥标识 |
| `DATA_ENCRYPTION_PREVIOUS_KEYS` | `old=...;older=...` | 历史解密密钥集合 |
| `AUDIT_CLEANUP_BATCH_SIZE` | `1000` | 每个短事务删除上限 |
| `AUDIT_CLEANUP_MAX_BATCHES` | `20` | 单次任务最大批数 |
| `BACKUP_OBJECTS` | 生产用 `required` | `required`、`auto` 或 `skip` |
| `RESTORE_OBJECTS` | `true` | 是否恢复 V2 包中的对象 |
| `RESTORE_OBJECTS_CONFIRM` | 目标 bucket 名 | 对象恢复显式确认 |

MinIO 连接继续使用 `MINIO_ENDPOINT`、`MINIO_ACCESS_KEY`、`MINIO_SECRET_KEY`、`MINIO_BUCKET`。备份脚本的数据库连接使用 `DB_HOST`、`DB_PORT`、`DB_USERNAME`、`DB_PASSWORD`、`DB_NAME`；恢复数据库名由 `RESTORE_TARGET` 单独指定。

## 7. 发布检查

- 运行完整前端验证、Playwright、bundle 预算和 `mvn test`，确认 CI 中 PostgreSQL 测试零跳过。
- 在预发布数据量上比较核心列表、财务总览、总账报表、三单匹配和提醒任务的耗时与 SQL 数量。
- 使用 `BACKUP_OBJECTS=required` 创建联合备份，执行外层和包内校验，并在隔离数据库/bucket 完成恢复演练。
- 检查新写密文 key ID，验证历史密文仍可读，确认所有历史密钥已进入受控密钥存储。
- 从非监控主机验证 `/actuator/prometheus` 不可访问，从监控主机确认指标和告警规则正常。
- 观察审计每日删除量；确保任务不持续触及最大批次数，数据库无明显锁等待或复制延迟。

## 8. 本轮验证结果

- 后端 81 项：80 通过、0 失败；本机 Docker 不可用，PostgreSQL Testcontainers 基线迁移测试跳过 1 项。CI 已强制该项不得跳过。
- 管理端 lint、Prettier、TypeScript、11 项 Vitest、生产构建和 500 KB 单文件预算通过。
- Playwright 在 Chromium 桌面和移动视口共 4 项通过。
- 移动端 TypeScript、2 项 Vitest、H5 构建和微信小程序构建通过。
- 根目录、管理端、移动端 npm 高危级别审计均为 0 漏洞。
- 三个灾备脚本通过 Bash 语法检查；V2 校验器通过正常联合包及异常路径包测试。
