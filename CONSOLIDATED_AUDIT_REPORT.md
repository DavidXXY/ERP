# ERP 全系统加固与优化扫描报告

> 扫描范围：`services/api`（Spring Boot，~64k 行 Java，546 处 `@PreAuthorize`）、`apps/admin`/`apps/supplier-portal`/`apps/mobile`（Vue3/uni-app）、`deploy`/`infra`/`.github`/`scripts`（部署、CI、备份、监控）、Flyway 迁移（B77 基线 + V78–V150）。
> 方法：6 领域并行深扫 + 高危结论逐条回源码核对，标注 `文件:行号`。仅列可落地、有实际影响的项。

## 落地状态（本次会话已完成 ✅ / 遗留建议 ⬜）

**已完成 ✅**
- 租户安全：租户感知缓存键 `TenantAwareKeyGenerator` + `CacheConfig`；原生 SQL 补 `tenant_id`（ProjectRepository / ProjectService / CrmOperationsService）；编号序列 `CodeSequence` 复合主键 `(entity_type, tenant_id)`（`V149`）；JWT `claims.tenant` 与主体租户交叉校验；`DataScopeService` 改为 ID 投影查询。
- 密钥/凭据：`SecuritySecretsGuard` 生产启动拒绝已知弱密钥；`.env.example` 全部改为 `CHANGE_ME_*` 占位；临时密码改用 `PasswordPolicy.generate`；供应商注册/改密接入 `PasswordPolicy`（12–100 位）。
- infra：Redis 鉴权 + 仅绑 127.0.0.1；Postgres/MinIO 仅回环、`unless-stopped`、资源上限 + 日志轮转；MinIO 镜像锁定 digest tag。
- 数据库：`V150` 补齐采购/合同外键（先清孤儿）、13 条高频索引、请假天数 `double`→`numeric(6,2)`、`qual_performances.contract_amount` varchar→numeric、`crm_service_contracts.amount` 恢复 NOT NULL。
- 正确性：凭证生成 `@Transactional` + 原子条件领取防并发重复；财务运营核心方法 `@Transactional`；HR 调动真实加载组织（去吞异常）+ 请假余额不足校验；`ReportController` 加 `@Valid`。
- 导出注入：CSV/Excel 公式注入清洗（CRM / HR / 采购导出 + `CsvUtils.sanitizeCell`）。
- 前端：登录开放重定向加固（仅站内相对路径 + 路由命中校验）；mobile `requestAllPages` 4 路并发上限 + `maxPages` 有界拉取；审批/通知列表上限 10 页；后端 5xx 统一泛化文案（admin/supplier/mobile）。
- 会话/令牌：供应商门户 JWT 仅允许 `Authorization: Bearer` 头，删除 URL query 参数读取路径（`SupplierPortalAuthenticationFilter`）。
- 性能：`SystemUser.roles` / `SystemRole.permissions` 改 `FetchType.LAZY`（安全路径由 `@EntityGraph` 显式加载）；`CustomerService.listCustomers` 与 `FinanceContributionService` 收款/付款从 `findAll()` 全表 JVM 聚合改为按 ID 集合查询 + `GROUP BY` 聚合。
- CI/监控：Trivy 阻断（exit-code 1）+ Java SBOM SCA 扫描；Flyway `ignore-migration-patterns` 收紧为 `versioned:missing`；Dependabot 覆盖 `/infra`；Prometheus `alerting:` + `alertmanager.yml`；备份 `BACKUP_REQUIRE_OFFSITE` fail-closed；Nginx 安全头重复声明消除继承丢失 + 内网端口 allow/deny；微信 AppID 占位守卫修正；GH Action 全部固定到 commit SHA（并修正 `trivy-action@0.30.0` 为实际 `v0.30.0` 提交）。
- 验证：`mvn test` 全绿（260 用例，0 失败 0 错误）；admin/mobile/supplier `typecheck` 全绿；迁移一致性检查通过。

**遗留建议 ⬜（未改动，需产品/部署决策或较大重构）**
- 其余 `findAll()` 后 JVM 聚合的财务/CRM/治理热路径逐步下推 SQL（已处理客户列表与财务贡献，`FinanceAnalyticsService`/`GovernanceService`/`ProcurementService` 等仍有余量）。
- 前端令牌存储为 `sessionStorage`/localStorage：若需提级，改为 HttpOnly+Secure Cookie 需同源/网关配合。
- e2e/组件测试覆盖率提升；Nginx 访问日志对 `token` 参数的 scrub（query 令牌已移除，故不再是泄露面）。

---

## 优先级总览（建议处理顺序）

| # | 问题 | 严重度 | 位置 |
|---|------|--------|------|
| 1 | 跨租户缓存键冲突 → 租户间数据泄露 | 🔴 严重 | 多个 `@Cacheable` 服务 |
| 2 | 弱默认密钥入库（JWT/加密密钥/DB/MinIO）→ 可伪造登录+解密密文 | 🔴 严重 | `application.yml`、`.env.example`、`infra/docker-compose.yml` |
| 3 | 原生 SQL 绕过 `@TenantId`（跨租户读取/删除） | 🔴 严重 | `ProjectRepository.java:41`、`CustomerService.java` 等 |
| 4 | infra compose：Redis 无鉴权 + 端口绑 0.0.0.0 | 🔴 严重 | `infra/docker-compose.yml` |
| 5 | 采购核心表无外键约束（孤儿数据风险） | 🔴 严重 | `B77__fresh_install_baseline.sql` |
| 6 | 管理端登录后开放重定向 | 🔴 严重 | `LoginView.vue:148` |
| 7 | 凭证生成幂等性非原子 → 并发重复记账/伪 FAILED | 🟠 高 | `VoucherGenerationService.java` |
| 8 | 单据编号序列全局共享（无 tenant_id） | 🟠 高 | `CodeSequence.java` / `code_sequences` |
| 9 | 授权热路径每行全表扫 `sys_users` | 🟠 高 | `DataScopeService.java` |
| 10 | 多处 `findAll()` 全表加载后 JVM 聚合 | 🟠 高 | 财务/CRM/治理等 Service |
| 11 | 高频过滤/排序列缺索引 | 🟠 高 | 财务/项目/采购/工时表 |
| 12 | 后端 Java 依赖无 CVE 扫描；Trivy 仅报告不阻断 | 🟠 高 | `security.yml` / `pom.xml` |
| 13 | Flyway `ignore-migration-patterns: "*:missing"` 全局放行 | 🟠 高 | `application.yml:24` |
| 14 | 供应商门户 JWT 走 URL query 参数（日志/Referer 泄露） | 🟡 中 | `SupplierPortalAuthenticationFilter.java:48` |

---

## 1. 安全

### 1.1 密钥与凭据（最优先）

- 🔴 **弱默认密钥可伪造登录 / 解密密文** — `application.yml:72`（JWT `ops-erp-local-development-secret-please-change`）、`:78`（数据加密密钥 `...change-me`）、`:10,84-85`（DB `ops_erp`、MinIO `ops_erp_minio_password`）。`application-prod.yml` 用 `${JWT_SECRET}` 无默认（正确），但基础 profile 的这些 fallback 一旦未激活 prod 或漏配 env 即生效；且本地 `.local-deploy/ops-erp.env`（已 gitignore，但实际以 `SPRING_PROFILES_ACTIVE=prod` + 同样的弱密钥运行）。知道该字符串即可用 HS256 伪造任意用户名/租户/角色令牌（`JwtService.java:111-116`），或用 DB dump 解出 `id_card/tax_no/bank_account/mfa_secret`（`EncryptedStringConverter` 用 SHA-256(密钥) 派生 AES-GCM）。
  **建议**：移除弱默认 fallback（改成 `${VAR}` 无默认，启动即校验「等于已知默认或 <32 字符则拒绝启动」）；删除本地 env 中的真实弱值；密钥放入 KMS/secret manager。
- 🔴 **infra 硬编码口令 + 端口对外 + Redis 无鉴权** — `infra/docker-compose.yml:7-8,23,39-40,11,25,43-44`：`POSTGRES_PASSWORD: ops_erp`、`MINIO_ROOT_USER/PASSWORD: ops_erp_minio/...`，Redis `--appendonly yes` 无 `requirepass`，`5432/6379/9000/9001` 未绑 `127.0.0.1:`（`deploy/docker-compose.yml` 已正确绑回环）。
  **建议**：端口绑回环、Redis 加 `requirepass`、口令改 `${POSTGRES_PASSWORD:?}` 强制注入。
- 📄 `.env.example`、`deploy/ops-erp.env.example`：后者已用 `CHANGE_ME_...` 占位（OK）；前者 `POSTGRES_PASSWORD=ops_erp`、`MINIO_SECRET_KEY=ops_erp_minio_password` 为真实弱值，建议同步改占位。

### 1.2 多租户隔离

- 🔴 **原生 SQL 绕过 `@TenantId`** — `ProjectRepository.java:41-42`：`nativeQuery=true` 的 `select * from project_projects where contract_id = :contractId ... limit 1` 无 `tenant_id` 谓词；`CustomerService.deleteCustomer`（`CustomerService.java:343-460`）约 40 条原生 DELETE/UPDATE 仅按 `customer_id/project_id` 执行、无租户保护；`ProjectService.validateCloseout`（`ProjectService.java:1795-1810`）原生 COUNT 同样缺租户过滤。`@TenantId` 只作用于 Hibernate 自动生成查询，**原生 SQL 不继承**。UUID 主键使其难以被主动猜中，但隔离语义已破坏。
  **建议**：每条原生 SQL 显式加 `tenant_id = :tenantId`（或先经租户过滤的 JPA 查询再执行）；补跨租户 ID 的集成测试。
- 🔴 **跨租户缓存键冲突** — 见优先级表 #1；`CacheConfig` 默认开缓存，所有 `@Cacheable` key 不含 tenantId。
- 🟠 **单据编号序列全局共享** — `CodeSequence.java` 无 `@TenantId`、`code_sequences` 无 `tenant_id` → 跨租户共享计数器（编号缺口可枚举、非隔离）。
- 🟡 **JWT `tenant` claim 未与 principal 交叉校验** — `JwtService.isValid` 只校验 subject/版本/过期，`JwtAuthenticationFilter.java:56-69` 直接信任 token 里的 tenant 并写入 `TenantContext`。在弱默认密钥场景下可被利用。建议在 `isValid` 中额外要求 `claims.tenant == account.tenantId`。

### 1.3 会话 / 令牌 / 前端

- 🔴 **登录后开放重定向** — `apps/admin/src/views/system/LoginView.vue:148-152`：`router.replace(redirect)` 直接用未校验的 `route.query.redirect`，`/login?redirect=//evil.com` 会离站跳转（钓鱼）。建议校验单 `/` 前缀 + `router.resolve()` 命中内部路由，否则回退 `/dashboard`。
- 🟡 **供应商门户 JWT 走 URL query 参数** — `SupplierPortalAuthenticationFilter.java:48-56`：GET 且 URI 以 `/download|/excel|/pdf` 结尾时，从 `?token=` 读 Bearer 令牌（为 `<a>` 下载无法带头的妥协）。令牌会进访问日志/代理/Referer/浏览器历史。建议改用短时效一次性下载令牌（DB 签发的专用签名），并 scrub 日志 query。
- 🟡 **令牌存于 JS 可读存储** — `apps/admin/src/api/http.ts:25`（sessionStorage）、`apps/supplier-portal/src/api.ts:444`、`apps/mobile/src/utils/storage.ts:15`（uni.setStorageSync 持久化）。后端 `SecurityConfig.java:48` 已 `csrf(disable)`——当前 Bearer 头下尚可；若迁到 httpOnly Cookie 必须重开 CSRF。建议优先 httpOnly+SameSite Cookie；小程序/uni 场景缩短有效期 + 加 refresh + Web 端严格 CSP。

### 1.4 输入 / 输出 / 导出

- 🟡 **Excel(.xlsx) 导出未做公式注入防护** — `CrmExportService.java:55-62,94-105`、`SupplierPortalExportService.java:345-357`、`HrExportImportService.java:88-109` 直接把用户字符串 `setCellValue(...)`，未中和前导 `=+-@\t\r`；`CsvUtils.cell()`（`common/util/CsvUtils.java:8-12`）的防注入只用在 CSV。建议所有单元格先过消毒器（前导危险字符前加 `'`）。
- 🟡 **供应商自助注册口令策略弱** — `SupplierPortalDtos.java:30` 仅 `@Size(min=8,max=100)`，`SupplierPortalService.java:186-212` 直接编码、未走系统用户的 `PasswordPolicy`（12–100 + 大小写/数字/特殊）。建议统一复用强密码策略。
- 🟡 **SQL 拼接 helper** — `FinanceOperationsService.java:397` `count(table, predicate)` 字符串拼 SQL；当前 7 处均为编译期常量不可注入，属"默认不安全"隐患，改为白名单固定查询。

### 1.5 已核实为良好（无需改）

JWT 签发含 `type` 声明区分内部/供应商、`stateless` + 默认 `anyRequest().authenticated()`、546 处 `@PreAuthorize`、委托式 `PasswordEncoder`（bcrypt）、登录锁定（Redis + 有界本地回退）、TOTP MFA（恢复码哈希）、CORS `allowedOriginPatterns`（非通配 `*`，`allowCredentials` 下是安全组合）、文件上传（扩展名 + Content-Type + 可选魔数 + 大小）、文件下载路径穿越防护（拒绝绝对路径、`nameCount==2`、`normalize+ensureInside`）、资质附件下载在服务层做归属/租户校验、字段级 AES-GCM 加密 + 密钥轮换、`open-in-view: false`、全局异常不泄堆栈、CSV 防公式注入。

---

## 2. 性能与数据访问

- 🟠 **授权热路径每行全表扫 `sys_users`** — `DataScopeService.java:36-41,~190`：`canViewOwner` 内 `visibleUserIds`，凡 scopes 含 `ALL` 就 `userRepository.findAll()` 全表拉；`SystemUser.roles`（`:61`）与 `SystemRole.permissions`（`:30`）均 EAGER `@ManyToMany` 且未配 `default_batch_fetch_size` → 列表 stream 过滤每个元素一次全表扫 + N+1。**建议**：每请求算一次（或按 principal+tenant 缓存）`visibleUserIds`；roles/permissions 改 LAZY，保留现有 `@EntityGraph`。
- 🟠 **`findAll()` 全表加载后 JVM 聚合**（多处，无分页）：
  - `FinanceAnalyticsService.java:125-168`：九张表（应收/应付/收款/付款/申请/发票/凭证/银行流水/管控）全量 `findAll()` 再 stream 过滤（380-390 同模式）。
  - `FinanceContributionService.java:125-157`：收款/付款/应收全表 + 内存过滤。
  - `CustomerService.java:70-89`：`contractRepository.findAll()` + `receivableRepository.findAll()` **两次** + 内存 groupBy。
  - `GovernanceService.java:209-260`、`CollaborationService.java:425-460`、`OfficeService.java:199-203,1177-1183`、`CrmOperationsService.java:170-180,1692-1697`、`ReportService.java:79-86`（懒加载 `systemUser` 每行 N+1）。
  **建议**：下推 SQL（`group by` 聚合、`countByStatus`、投影查询），列表端点加 `Pageable`。
- 🟠 **事务内同步外部 HTTP** — `NotificationChannelService.java:74-119`：`dispatch()` `@Transactional` 内持事务跨 `restClient.post(webhook)` 网络调用；`dispatchRecent()`（`@Scheduled`）同步循环放大延迟。**建议**：持久化与投递分离、事务外交发、批次上限 + 异步超时。

---

## 3. 正确性与代码质量

- 🟠 **凭证生成幂等性非原子** — `VoucherGenerationService.java:34-62`：先读状态再无条件 `UPDATE ... set 'PROCESSING'`（无 `where status='PENDING'`）→ 并发双记账，输家 `DataIntegrityViolationException` 被 `catch (RuntimeException)` 记成 FAILED。`FinanceOperationsService.executePeriodJob`（`:123-137`）同款 check-then-act、无事务无锁。**建议**：原子抢占 `UPDATE ... WHERE status IN ('PENDING','FAILED')` + 检查影响行数；丢锁者重读返回已有凭证。
- 🟠 **财务写方法无事务** — `FinanceOperationsService.java:104,123,139,207,228,258,280,299,312,322` 多个多语句写方法无 `@Transactional`，`JdbcTemplate.update` 各自自动提交 → 半提交半失败。**建议**：统一加事务。
- 🟠 **HR 调动审批凭空 new 空组织** — `HrService.java:282-284`：`emp.setOrganization(new SystemOrganization()); setId(...)`，从不真加载组织，无效 id 时 flush 才报错且被 `catch (Exception ignored)` 吞 → 静默失败但生命周期已 APPROVED。**建议**：`organizationRepository.findById(...).orElseThrow(...)`，去吞异常。
- 🟡 **空 catch 静默丢数据** — `HrService.java:391,394,424,434,512,524`、`CrmOperationsService.java:1003-1006`：附件 JSON 解析失败→空列表、证书数变 0、删合同应收解绑吞真实 DB 错。**建议**：至少 `warn` 日志；完整性相关操作不吞。
- 🟡 **缺 `@Valid`** — `ReportController.java:42-44` `@RequestBody ReportCreateRequest` 未加 `@Valid`，DTO 的 `@NotBlank/@Size` 全部失效。
- 🟡 **请假扣减静默 no-op、不校验超额** — `HrService.java:587-594`：余额行缺失时 `ifPresent` 跳过、不查 `usedDays+days<=totalDays`。
- 🟡 **物理删除手写原生级联** — `CrmOperationsService.java:984-1016`：新增子表易成孤儿 → 优先 JPA cascade/orphan-removal 或 FK ON DELETE。

---

## 4. 数据库与迁移

- 🔴 **采购核心表无外键约束** — `procurement_supplier_invoices`（`B77:1789`）、`procurement_supplier_quotes`、`procurement_supplier_quote_lines`、`procurement_inquiry_requests`、`procurement_collaboration_events`、`procurement_supplier_reviews`、`procurement_supplier_change_requests`、`procurement_return_orders`、`crm_contract_changes` 等 NOT NULL 的 `*_id` 列在 FK 段（`B77:5302-6102`）无任何约束 → 孤儿可静默悬挂。**建议**：补齐 FK（明确 ON DELETE 语义），先清孤儿再建。
- 🟠 **财务 FK/热查列缺索引** — `fin_procurement_payables.order_id/receipt_id`（仅 supplier_id 有索引）、`fin_receivables.contract_id`（FK 无索引）、`fin_accounting_entries.voucher_id`（仅处 `idx_entry_account` 第二列，`findByVoucherId` 用不上）。**建议**：分别补 order_id、receipt_id、contract_id、voucher_id 前导索引。
- 🟠 **`double precision` 存请假天数** — `hr_leave_balances.total_days/used_days`（`B77:1028-1029`）、`hr_leave_requests.total_days`（`B77:1055`）用浮点；精确应得额度（0.5 天）会浮点漂移。**建议**：改 `numeric(6,2)`。
- 🟠 **Flyway `ignore-migration-patterns: "*:missing"`** — `application.yml:24`（+ `baseline-on-migrate: true`），`application-prod.yml:48-52` 只开 `validate-on-migrate` 未覆盖 → 生产继承通配符，任何缺失迁移被静默忽略。**建议**：精确白名单（如 V1–V77）替代通配符。
- 🟡 **V103 解除合同金额 NOT NULL** — `V103__crm_framework_orders_and_subprojects.sql:5` `ALTER COLUMN amount DROP NOT NULL` → 聚合函数静默跳过 NULL。**建议**：金额保持 NOT NULL，框架/预估另设可选列。
- 🟡 **合同金额存文本** — `qual_performances.contract_amount varchar(100)`（`B77:2114`）→ 改 `numeric(18,2)`。
- 🟡 **缺索引**（补充）：`procurement_purchase_orders/requests` 的 `project_id/part_id/department_id`、`sys_organizations.parent_id`、`biz_project_timesheets.user_id/project_id`、`crm_contract_changes.contract_id`。
- 🟡 **`cc_user_ids` 用 btree 索引 JSON 文本** — `V144__employee_reports.sql:26`：对 text JSON 数组建 btree 无法回答「是否包含某用户」→ 改 jsonb+GIN 或拆关联表。
- 🟢 **低优先**：`approval_assignee_configs` 冗余索引（`idx_approval_assignee_flow` 可删）；`V90/V91/V138` 破坏性迁移（DROP COLUMN / 数据 DELETE）无回滚路径、无 IF EXISTS 保护；`code_sequences` 缺审计列；`hr_emergency_contacts.employee_id` 可空、`fin_payment_*.supplier_id` 缺索引。

---

## 5. 前端

- 🟠 **`requestAllPages` 把整表拉进浏览器** — `apps/admin/src/api/http.ts:81-117`；消费方 `crm.ts:786`、`inventory.ts:128/142/149/167/189`、`ledger.ts:70`、`finance.ts:412/469/476/597`、`qualification.ts:218/251/319/354`、`office.ts:278/598`。`listReceivables/listVouchers/listMaterialIssues/listStockMovements/listApprovals/listNotifications` 等全量拉页，`CrmDashboardView.vue:1072`、`LedgerView.vue:937`、`MaterialIssuesView.vue:86` 在浏览器聚合；单页失败整列表挂。**建议**：列表改服务端分页+过滤，聚合走专用 dashboard/analytics 接口；`requestAllPages` 只留给小有界参照数据并必传 `maxPages`。
- 🟡 **移动端 `requestAllPages` 无并发上限** — `apps/mobile/src/utils/http.ts:78-92` `Promise.all` 一次打 `totalPages-1` 请求。建议移植 admin 的有界 worker 池。
- 🟡 **后端原始错误透传给用户** — `apps/admin/src/api/http.ts:47`、`apps/supplier-portal/src/api.ts:465`、`apps/mobile/src/utils/http.ts:61,108` `error.response.data.message` 原样展示。建议按状态码/错误码映射脱敏文案。
- 🟡 **移动端审批/通知全量渲染无虚拟化** — `apps/mobile/src/pages/approvals/index.vue:19,31`、`api/office.ts:4,8` 全量 `v-for`。建议分页 + scroll-view 虚拟化。
- 🟢 **低优先**：静默空 catch（`reports/edit.vue:56,59`、`AppLayout.vue:457`、`NotificationCenterView.vue:147`）；维修模块 `canAccessMaintenance = computed(()=>false)`（`AppLayout.vue:589`）硬禁用但仍打包全市 → 特性开关 tree-shake 或删除；供应商门户列表未分页（`api.ts:504,564-585,656`，单供应商量小、风险低）。
- ✅ **良好**：无 `v-html/innerHTML` 注入点、admin 路由懒加载、admin `requestAllPages` 有 4 路并发上限、CSV 导出防公式注入。

---

## 6. 基础设施 / 部署 / CI

- 🟠 **监控告警无接收器** — `deploy/monitoring/alerts.yml:1-39` 有 7 条规则，但 `prometheus.yml` 无 `alerting:` 块、仓库无 `alertmanager.yml` → 告警从不通知任何人。**建议**：补 alertmanager + 真实接收器（邮件/Slack/webhook）。
- 🟠 **监控仅覆盖 API** — `prometheus.yml:8-13` 只抓 `ops-erp-api`；无 node/postgres/redis/minio exporter，无磁盘/内存/DB 存活/慢查询规则。**建议**：补 exporter + 对应告警规则。
- 🟠 **部署无迁移回滚** — `deploy/deploy.sh:111-119` 失败仅回滚 JAR/前端 symlink，Flyway 迁移已跑 → 旧代码 + 新 schema。**建议**：迁移前置门禁/快照回滚/N-1 兼容策略。
- 🟠 **备份离线可选且默认同机** — `deploy/ops-erp.env.example:82`（`BACKUP_OFFSITE_REMOTE=` 空）、`ops-erp-backup.service:12`（写本地 `/var/backups/ops-erp`）。**建议**：生产 fail-closed 要求 `BACKUP_OFFSITE_REMOTE` 非空并校验 rclone 成功；恢复演练定期在独立主机执行。
- 🟡 **Nginx `add_header` 继承丢失安全头** — `deploy/ops-erp-common.conf:49-64` 各 location 设了 `add_header Cache-Control`，会丢弃顶部的 `X-Content-Type-Options/X-Frame-Options/Referrer-Policy/Permissions-Policy`（Nginx 子级 add_header 不继承父级），且内部入口无 CSP。**建议**：每个设了 add_header 的 location 重复全量安全头 + 补 CSP。
- 🟡 **内部 HTTP 入口端口无限制、无 TLS** — `deploy/ops-erp-ports.nginx.conf:17-49` `listen 87/88/89/90` 绑所有接口。**建议**：加 allow/deny 或绑私有接口 / 终结 TLS。
- 🟡 **容器/进程无资源上限、无日志轮转** — `deploy/docker-compose.yml`、`ops-erp-api.service:12`：无 mem_limit/cpus、无 logging max-size、systemd 无 MemoryMax/CPUQuota/LimitNOFILE。**建议**：补 limits + 日志轮转 + `unless-stopped`（infra compose 缺 restart）。
- 🟡 **微信 AppID 发布护栏判断错占位符** — `deploy/build-mobile.sh:12` 检查 `wx0000000000000000`，但 `manifest.json:11` 实为 `touristappid` → 护栏永不触发。**建议**：改检测 `touristappid`/非 `wx` 前缀即阻断。
- 🟡 **镜像标签浮动 + Dependabot 漏 infra** — `infra/docker-compose.yml:35` 用 `minio/minio:latest`；`deploy/docker-compose.yml` 用 `postgres:16-alpine`/`redis:7-alpine` 无 digest；`dependabot.yml:23-26` 只看 `/deploy` 不监控 `/infra`。**建议**：生产锁 digest、去 latest、Dependabot 加 `/infra`。
- 🟡 **组件/多端 e2e 覆盖薄弱** — `playwright.config.ts:4` 仅 `apps/admin/e2e`；supplier/mobile 无 e2e，无 `@vue/test-utils` 组件测试。**建议**：补组件测试 + 各端 Playwright e2e + 覆盖率阈值。
- 🟢 **GitHub Actions 用可变 major 标签** — `ci.yml:22-23,86-90`、`security.yml:38,56,83,108`（`@v5/@v4/@v2`）。建议锁 commit SHA。
- ✅ **良好**：生产 Nginx TLS + 安全头 + 仅回环 actuator、systemd 单元强化（NoNewPrivileges/ProtectSystem=strict/PrivateTmp）、备份 age 加密 + 校验和 + 30 天轮转 + 每周恢复演练、CI 前端/后端/e2e + CodeQL + gitleaks 全历史 + SBOM + bundle 预算。

---

## 建议落地顺序

1. **第 1 周（止血）**：① 租户缓存键；② 弱默认密钥（JWT/加密/DB/MinIO）移除 + 启动校验；③ 原生 SQL 补 `tenant_id`；④ infra 端口/口令/Redis 鉴权；⑤ 开放重定向；⑥ 凭证幂等原子化；⑦ 编号序列租户隔离。
2. **第 2–3 周（数据与性能）**：⑧ 采购表补 FK；⑨ 授权路径 + `findAll` 下推 SQL；⑩ 补索引（财务/项目/采购/工时）；⑪ 请假天数改 numeric、合同金额恢复 NOT NULL。
3. **第 3–4 周（供应链与迁移安全）**：⑫ Java SCA + Trivy 阻断；⑬ 镜像 digest 锁定；⑭ Flyway 白名单；⑮ 备份离线化 + 告警接收器；⑯ Nginx 安全头/CSP。
4. **持续**：前端 `requestAllPages` 改分页、Excel 公式注入防护、供应商口令策略、JWT query 参数改一次性令牌、加 `@Valid`、清空 catch、HR 调动修复、微信 AppID 护栏、补组件/e2e 测试。