# 系统架构说明

## 建设原则

第一阶段采用模块化单体，不拆微服务。代码按业务域拆分，部署上保持一个后端服务，先把合同、工单、库存、财务、审批这些核心闭环跑通。

## 分层

- `apps/admin`：PC 管理后台，覆盖 CRM、供应链采购、项目管理、库存管理、财务、OA、系统设置。
- `apps/mobile`：移动办公端规划入口；当前仅保留需求说明，代码实现后续展开。
- `services/api`：统一业务 API，负责权限、事务、审批、数据落库和外部服务集成。
- `infra`：本地开发基础设施，包括 PostgreSQL、Redis、MinIO。

## 后端模块边界

- `common`：统一响应、异常、基础实体、审计字段、通用工具。
- `config`：安全、跨域、OpenAPI、存储、流程引擎配置。
- `modules.crm`：客户池、联系人、项目地址、线索商机、报价、客户合同、应收联动。
- `modules.procurement`：供应商、采购申请、采购订单，后续联动审批、入库和应付。
- `modules.project`：项目立项、预算、进度、成本归集、验收、质保。
- `modules.workorder`：工单来源、派工、现场执行、验收闭环、成本归集。
- `modules.inventory`：物料、库存、出入库、盘点、低库存采购联动。
- `modules.qualification`：公司资质、资质人员、人员证书、项目业绩、投标组合查询与到期预警。
- `modules.finance`：应收、应付、开票、回款核销、凭证。
- `modules.oa`：审批流程、用章、报销、采购、合同审批。
- `modules.system`：组织、用户、角色、权限、数据范围、操作日志。

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

- 所有业务主表预留 `tenant_id`，为后续多公司、多账套或集团化管理留出口。
- 所有核心表保留 `created_at`、`updated_at`、`created_by`、`updated_by`。
- 使用 Flyway 管理数据库结构，禁止生产环境手工改表。
- 文件只在数据库保存元数据。当前实现通过统一存储接口保存原文件，并对文件大小、扩展名和路径穿越做统一校验；`ops.storage.type=local` 使用本地磁盘，`ops.storage.type=minio` 使用 MinIO/S3 兼容对象存储，生产 profile 默认启用 MinIO。

## 权限策略

- 第一阶段采用 JWT + Spring Security 方法级权限。
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

## 前端原型说明

`src/` 目录下的 React 原型为产品交互参考，已实现完整的财务模块功能。
与后端通过 `/api` 代理（Vite proxy → `localhost:8080`）通信，自动 JWT 登录，
支持离线降级到 mock 数据。

### 财务模块 API 补充（2026-07-05 更新）

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

`src/api.ts` — 共 26 个 fetch 函数，覆盖 8 个后端模块：
Finance（5）、CRM（7）、Ledger（3）、Procurement（3）、Inventory（1）、
Project（1）、Office（4）、BI（1）。

运行 `npm run gen:api` 可从 OpenAPI 规范自动生成 TypeScript 类型。

### 前端初始化流程

```
应用启动 → POST /api/auth/login（自动登录）
  → Promise.all([26个API]) 并行加载
    ├─ 成功 → setState 填充数据，视图实时刷新
    └─ 失败 → 保留 mock 数据，静默降级
  → setInitializing(false) 隐藏加载遮罩
```
