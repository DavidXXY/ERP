# 系统架构说明

## 建设原则

当前采用模块化单体，不拆微服务。代码按业务域拆分，部署上保持一个后端服务，使 CRM、合同、项目、采购、库存、财务、工单、审批和风险治理共享同一事务边界。只有在容量、团队边界和独立发布需求明确后才考虑拆分服务。

## 分层

- `apps/admin`：PC 管理后台，覆盖 CRM、供应链采购、项目管理、库存管理、财务、OA、系统设置。
- `apps/mobile`：uni-app 移动办公端，发布为微信小程序或 H5，共用后端认证、审批、通知、工单和库存能力。
- `services/api`：统一业务 API，负责权限、事务、审批、数据落库和外部服务集成。
- `infra`：本地开发基础设施，包括 PostgreSQL、Redis、MinIO。

## 后端模块边界

- `common`：统一响应、异常、基础实体、租户上下文、存储、删除治理、安全和通用工具。
- `config`：Spring Security、异步执行器、Web MVC、OpenAPI、分布式任务锁等全局配置。
- `modules.crm`：客户池、联系人、项目地址、线索商机、报价、客户合同、应收联动。
- `modules.procurement`：供应商、采购申请、询价、采购订单、到货、三单匹配、入库和应付联动。
- `modules.project`：项目立项、预算、进度、成本归集、验收、质保。
- `modules.maintenance`：设备、维保计划、工单派工、现场执行、验收、移动端离线动作和成本归集。
- `modules.inventory`：物料、库存、出入库、盘点、低库存采购联动。
- `modules.qualification`：公司资质、资质人员、人员证书、项目业绩、投标组合查询与到期预警。
- `modules.finance`：应收、应付、开票、回款核销、凭证。
- `modules.office`：条件审批、费用报销、出差、用章、外包、档案、通知和操作审计。
- `modules.collaboration`：跨部门责任绑定、协作待办、动作日志和导出。
- `modules.risk`：统一风险项、规则、责任人、SLA、升级、闭环和快照。
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

## 数据策略

- 所有业务主表包含 `tenant_id`，JPA 查询和写入通过租户上下文隔离。
- 所有核心表保留 `created_at`、`updated_at`、`created_by`、`updated_by`。
- 使用 Flyway 管理数据库结构，禁止生产环境手工改表。
- 列表接口优先使用数据库分页，所有 Spring Data 分页请求单页最多 200 条。
- V86 引入按用户通知回执、审批申请人 ID、JWT 认证版本和移动审批查询索引。
- 文件只在数据库保存元数据。当前实现通过统一存储接口保存原文件，并对文件大小、扩展名和路径穿越做统一校验；`ops.storage.type=local` 使用本地磁盘，`ops.storage.type=minio` 使用 MinIO/S3 兼容对象存储，生产 profile 默认启用 MinIO。

## 权限策略

- 认证采用无状态 JWT + Spring Security 方法级权限。JWT 带 `auth_version`，密码或账号状态变化后旧令牌失效。
- 登录限流优先使用 Redis，并有受限的本地降级计数器；客户端 IP 只接受可信代理网段提供的转发链。
- 前端菜单按权限展示，后端接口使用 `@PreAuthorize` 兜底。
- 维修工单、证书和排班已拆分为细粒度权限；新增权限通过 Flyway 和启动初始化双路径补齐，历史 ADMIN 角色会自动获得缺失权限。
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

## 通知与审计

- 系统级提醒可以全员可见，但已读状态由 `system_notification_reads` 按用户独立保存。
- 审批等业务通知必须解析到明确接收人；无法解析时不创建全员通知。
- 审计拦截器在请求结束时采集信息，并交给有界异步执行器写库。
- 审计日志默认保留 365 天，由每日任务清理，保留期通过 `AUDIT_RETENTION_DAYS` 配置。

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
- `GET /api/finance/payments`：付款记录
- `GET /api/finance/ledger/overview`：总账科目概览
- `GET /api/finance/ledger/vouchers`：会计凭证列表
- `GET /api/finance/ledger/statements`：财务报表（资产/负债/收入/费用）

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
