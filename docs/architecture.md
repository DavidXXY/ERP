# 系统架构说明

## 建设原则

当前采用模块化单体，不拆微服务。代码按业务域拆分，部署上保持一个后端服务，使 CRM、合同、项目、采购、库存、财务、工单、审批和风险治理共享同一事务边界。只有在容量、团队边界和独立发布需求明确后才考虑拆分服务。

## 分层

- `apps/admin`：PC 管理后台，覆盖 CRM、供应链采购、项目管理、库存管理、财务、OA、系统设置。
- `apps/supplier-portal`：供应商外部协作端，使用独立 JWT 与本地令牌，覆盖注册、企业资料、资质文件、受邀询价和报价。
- `apps/mobile`：uni-app 移动办公端，发布为微信小程序或 H5，共用后端认证、审批、通知、工单和库存能力。
- `services/api`：统一业务 API，负责权限、事务、审批、数据落库和外部服务集成。
- `infra`：本地开发基础设施，包括 PostgreSQL、Redis、MinIO。

## 后端模块边界

- `common`：统一响应、异常、基础实体、租户上下文、存储、删除治理、安全和通用工具。
- `config`：Spring Security、异步执行器、Web MVC、OpenAPI、分布式任务锁等全局配置。
- `modules.crm`：客户池、联系人、项目地址、线索商机、报价、客户合同、合同变更审批和应收联动。
- `modules.procurement`：供应商、采购申请、询价、采购订单、到货质检、退换货、三单匹配、入库和应付联动。
- `modules.procurement.security`：供应商门户专用认证主体与过滤器，不复用内部 `sys_users`；门户令牌仅访问 `/api/supplier-portal/**`。
- `modules.project`：项目立项、预算、进度、成本归集、验收、质保。
- `modules.maintenance`：设备、维保计划、工单派工、现场执行、客户验收、收费工单应收联动、移动端离线动作和成本归集。
- `modules.inventory`：物料、库存、出入库、盘点、低库存采购联动。
- `modules.qualification`：公司资质、资质人员、人员证书、项目业绩、投标组合查询与到期预警。
- `modules.finance`：应收、应付、开票、回款核销、付款三岗分离和凭证。
- `modules.office`：条件审批、费用报销及付款、出差、用章、外包、档案、通知和操作审计。
- `modules.collaboration`：跨部门责任绑定、协作待办、动作日志和导出。
- `modules.risk`：统一风险项、规则、责任人、SLA、升级、闭环和快照。
- `modules.governance`：跨模块经营控制、会计期间、关账守卫、银行流水和对账。
- `modules.mobile`：移动工作台聚合。
- `modules.bi`：经营驾驶舱和跨模块分析。
- `modules.system`：组织、用户、角色、权限、数据范围、认证版本和系统运行状态。

## 已落地接口

- `POST /api/auth/login`：账号密码登录，返回 JWT。
- `GET /api/auth/me`：读取当前用户、角色和权限。
- `GET/POST /api/crm/customers`：客户池列表、新增客户。
- `GET /api/crm/customers/{id}`：客户完整档案，包含开票资料、联系人、地址、合同和应收。
- `GET/POST /api/projects`：项目列表、新增项目。
- `GET/POST /api/inventory/parts`：物料台账列表、新增物料。
- `GET/POST /api/inventory/parts/{partId}/movements`：库存流水查询、出入库写入。
- `GET/POST /api/procurement/suppliers`：供应商列表、新增供应商。
- `GET/POST /api/procurement/requests`：采购申请列表、新增采购申请。
- `GET/POST /api/procurement/orders`：采购订单列表、新增采购订单。
- `POST /api/procurement/orders/{id}/cancel|close`：取消未到货订单、关闭已完成质检和退换货处理的订单。
- `POST /api/procurement/receipts/{id}/inspection`：到货质检，采购订单取消或关闭后拒绝继续入库。
- `POST /api/procurement/returns/{id}/resolve`：登记换货、折让或索赔并结案退换货。
- `POST /api/procurement/inquiries/{id}/invitations`：采购员定向邀请一个或多个已准入供应商。
- `POST /api/procurement/inquiries/{id}/deadline`：调整进行中询价的截止日期。
- `POST /api/procurement/inquiries/{id}/quotes`：采购员代录供应商报价。
- `POST /api/procurement/inquiries/{id}/quotes/{quoteId}/score`：内部技术与商务评分，不向供应商展示。
- `POST /api/supplier-portal/auth/register|login`：供应商注册与独立登录。
- `GET/PUT /api/supplier-portal/profile`：读取会话并维护企业档案。
- `GET/POST /api/supplier-portal/documents`：供应商资料清单与上传。
- `GET /api/supplier-portal/inquiries`：读取当前供应商被邀请的询价。
- `PUT/POST /api/supplier-portal/inquiries/{id}/quote/**`：保存草稿、提交、撤回或确认采购代录报价。
- `POST /api/supplier-portal/inquiries/{id}/decline`：供应商放弃响应并记录原因。
- `GET/POST /api/supplier-portal/inquiries/{id}/attachments`：报价附件管理。
- `GET/POST /api/supplier-portal/inquiries/{id}/clarifications`：询价澄清协作。
- `PUT /api/maintenance/work-orders/{id}/accept`：客户验收工单，并为非质保收费工单生成应收。
- `POST /api/office/expenses/{id}/pay`：支付审批通过的费用报销并生成付款凭证。

## 数据策略

- 所有业务主表包含 `tenant_id`，JPA 查询和写入通过租户上下文隔离。
- 软删除治理使用原生 SQL 的路径同样绑定当前租户，包括隐藏判断、回收站、审批、恢复和删除申请创建。
- 所有核心表保留 `created_at`、`updated_at`、`created_by`、`updated_by`。
- 使用 Flyway 管理数据库结构，禁止生产环境手工改表。
- 列表接口优先使用数据库分页，所有 Spring Data 分页请求单页最多 200 条。控制器通过自有 `PageResponse` 输出稳定字段，不直接暴露 Spring Data 的序列化结构。
- 财务、总账、采购、资质、BI、删除治理和提醒任务优先按当前页 ID 或业务条件批量查询，并由数据库完成求和、计数和分组，避免整表加载及逐行 N+1 查询。
- 经营驾驶舱、BI 与财务分析等热点只读接口使用 Redis 结果缓存（`@Cacheable`，默认 60 秒 TTL），大幅降低高并发下的全表聚合压力；测试环境通过 `ops.cache.enabled=false` 关闭缓存。
- V86 引入按用户通知回执、审批申请人 ID、JWT 认证版本和移动审批查询索引。
- V87 引入经营控制台账、治理动作日志、会计期间、银行对账和凭证复核/记账/冲销审计字段。交易模块通过 `AccountingPeriodGuard` 共享期间开关，不各自实现关账判断。
- V105 引入费用付款日期、流水和操作人字段，付款申请人/审批人/执行人 ID，以及会计期间待复核动作、发起人、时间和原因字段。应用服务使用这些稳定用户 ID 执行不相容职责和双人复核校验。
- V106 引入供应商门户账号、自助资料、询价邀请、报价来源/提交人/确认状态和版本快照。V107 增加安全邀请码绑定、审核前资料草稿、账号认证版本、邀请送达状态、报价附件、放弃响应、询价澄清及拆分审核权限。报价要求账号 `ACTIVE`、供应商 `APPROVED`、未冻结、已受邀且不处于强制改密状态。
- 文件只在数据库保存元数据。当前实现通过统一存储接口保存原文件，并对文件大小、扩展名和路径穿越做统一校验；`ops.storage.type=local` 使用本地磁盘，`ops.storage.type=minio` 使用 MinIO/S3 兼容对象存储，生产 profile 默认启用 MinIO。

## 权限策略

- 认证采用无状态 JWT + Spring Security 方法级权限。JWT 带 `auth_version`，密码或账号状态变化后旧令牌失效。
- 登录限流优先使用 Redis，并有受限的本地降级计数器；客户端 IP 只接受可信代理网段提供的转发链。
- 前端菜单按权限展示，后端接口使用 `@PreAuthorize` 兜底。
- 维修工单、证书和排班已拆分为细粒度权限；新增权限通过 Flyway 和启动初始化双路径补齐，历史 ADMIN 角色会自动获得缺失权限。
- 经营治理拆分查看、维护、关账和银行对账权限；手工凭证拆分制单、复核、记账和冲销权限，服务层同时校验不相容操作人。
- 付款流程分别使用 `finance:payment:apply`、`finance:payment:approve` 和 `finance:payment:execute`；申请人不得审批，申请人和审批人均不得执行付款。
- 强制关账和反结账共用 `governance:period:close` 权限，但必须由一名用户发起、另一名用户复核；普通且已满足检查项的关账仍可直接完成。
- 业务闭环提供可复用聚合接口：OA 统一待办/预警、采购三单匹配、库存补货建议、项目利润摘要，前端可直接接入形成经营看板和异常处理入口。
- 当前种子角色包括 `ADMIN` 和 `CRM_MANAGER`；`ADMIN` 拥有全部已落地权限。
- 开发环境默认管理员为 `admin / Admin@123`，生产环境必须改为正式密码策略和用户初始化流程。

## API 约定

- 所有接口统一返回：

```json
{
  "success": true,
  "message": "ok",
  "data": {}
}
```

- 后端基础路径为 `/api`。
- 健康检查走 Spring Actuator：`/actuator/health`。
- 分页响应位于 `data`，包含 `content`、`number`、`size`、`totalElements` 和 `totalPages`。
- 管理端和移动端需要完整数据时使用公共 `requestAllPages` 逐页获取。
- OA 审批列表只返回可扫描的摘要；用户打开抽屉后再读取来源单据、审批节点和动作历史，避免列表请求重复加载详情集合。

## 通知与审计

- 系统级提醒可以全员可见，但已读状态由 `system_notification_reads` 按用户独立保存。
- 审批等业务通知必须解析到明确接收人；无法解析时不创建全员通知。
- 审计拦截器在请求结束时采集信息，并交给有界异步执行器写库。
- 审计日志默认保留 365 天，由每日任务按短事务分批清理；保留期、批大小和最大批次数分别通过 `AUDIT_RETENTION_DAYS`、`AUDIT_CLEANUP_BATCH_SIZE`、`AUDIT_CLEANUP_MAX_BATCHES` 配置。

## 可观测性与灾备

- 生产后端仅绑定回环地址，由 Nginx 代理业务 API 和健康检查。
- Actuator 只暴露 `health,prometheus`；Prometheus 指标带 `application=ops-erp-api` 标签和 HTTP 请求直方图。Nginx 的指标路径只允许本机访问，其他 Actuator 路径返回 404。
- V2 备份将 PostgreSQL custom dump、MinIO/S3 对象、清单和逐文件 SHA-256 打包为 `.tar.gz`，并生成外层归档校验文件。
- 恢复先写入对象存储，再恢复数据库，避免数据库记录先出现而附件尚未就绪。恢复目标必须使用隔离数据库和 bucket，并分别显式确认。
- 敏感字段使用带 key ID 的 `ENC2` 密文；当前密钥负责新写入，历史密钥仅用于读取轮换前数据，并兼容无 key ID 的 `ENC1`。

## 质量门禁

- 管理端单元测试只收集 `src`，浏览器关键流程由 Playwright 分别在 Chromium 桌面和移动视口验证登录、路由权限和按钮权限。
- CI 安装 Chromium、上传 Playwright 报告，并检查 PostgreSQL Testcontainers 测试报告的 `skipped="0"`，防止 Docker 缺失导致迁移测试被误判为通过。

## 前端原型说明

`src/` 目录下的 React 原型仅作为早期产品交互参考，不是当前生产管理端。生产管理端是 `apps/admin`，移动端是 `apps/mobile`；两者均通过 `/api` 与 Spring Boot 后端通信。

### 财务模块 API 示例

- `GET /api/finance/overview`：财务总览数据
- `GET /api/finance/receivables`：应收列表
- `POST /api/finance/receivables/{id}/invoice`：开票
- `POST /api/finance/receivables/{id}/receipts`：回款登记
- `GET /api/finance/payables`：应付列表（含逾期标记）
- `GET /api/finance/payment-applications`：付款申请列表
- `POST /api/finance/payment-applications`：创建付款申请
- `POST /api/finance/payment-applications/{id}/approval`：审批付款申请
- `POST /api/finance/payment-applications/{id}/payment`：执行付款
- `POST /api/office/expenses/{id}/pay`：执行费用报销付款并生成 `EXPENSE_PAYMENT` 凭证
- `GET /api/finance/payments`：付款记录
- `GET /api/finance/ledger/overview`：总账科目概览
- `GET /api/finance/ledger/vouchers`：会计凭证列表
- `GET /api/finance/ledger/statements`：财务报表（资产/负债/收入/费用）
- `POST /api/finance/ledger/vouchers`：新建手工凭证草稿
- `POST /api/finance/ledger/vouchers/{id}/review|post|reverse`：凭证复核、记账和冲销
- `GET /api/governance/overview`：经营治理总览
- `GET/POST /api/governance/controls`：经营控制台账
- `POST /api/governance/periods/{year}/{month}/close`：正常关账或发起/复核强制关账
- `POST /api/governance/periods/{year}/{month}/reopen`：发起/复核反结账
- `GET/POST /api/governance/bank-lines/**`：银行流水与对账

### 前端 API 客户端

当前管理端 API 客户端位于 `apps/admin/src/api`，移动端 API 客户端位于 `apps/mobile/src/api`。不要从早期原型 `src/api.ts` 推断当前接口。

运行 `npm run gen:api` 可从 OpenAPI 规范自动生成 TypeScript 类型。

### 前端初始化流程

```text
应用启动 -> 读取本地 JWT -> GET /api/auth/me
  -> 成功：加载当前用户、角色、权限和允许访问的页面
  -> 401：清除本地令牌并跳转登录页
  -> 业务页面按需加载对应 API，不使用 mock 数据掩盖生产错误
```
