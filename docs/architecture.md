# 系统架构说明

## 建设原则

第一阶段采用模块化单体，不拆微服务。代码按业务域拆分，部署上保持一个后端服务，先把合同、工单、库存、财务、审批这些核心闭环跑通。

## 分层

- `apps/admin`：PC 管理后台，覆盖 CRM、供应链采购、项目管理、库存管理、财务、OA、系统设置。
- `apps/mobile`：移动办公端，覆盖工单接单、定位签到、拍照上传、领料、客户签字。
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
- 文件只在数据库保存元数据，原文件进入 MinIO。

## 权限策略

- 第一阶段采用 JWT + Spring Security 方法级权限。
- 前端菜单按权限展示，后端接口使用 `@PreAuthorize` 兜底。
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
