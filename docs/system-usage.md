# Engineering Ops ERP 系统详细使用教程与运行逻辑

> 适用对象：业务管理员、实施人员、开发维护人员  
> 最后更新：2026-07-17  
> 当前主系统：`apps/admin` Vue 管理后台 + `services/api` Spring Boot 后端

## 1. 系统定位

Engineering Ops ERP 是一套企业一体化经营管理系统，覆盖 CRM、项目、采购、库存、人事、资质、OA、财务、风险、BI 与系统管理。系统不是单页演示，而是按可上线业务闭环建设：前端负责页面、权限菜单、表单与交互；后端负责认证、权限、业务校验、数据落库、附件存储、审计、风险与审批联动。

主要目录：

| 目录 | 作用 |
| --- | --- |
| `apps/admin` | PC 管理后台，当前主要使用入口 |
| `services/api` | Spring Boot 后端 API |
| `infra` | 本地 PostgreSQL、Redis、MinIO 基础设施 |
| `deploy` | 生产构建、部署、Nginx、systemd 与环境变量模板 |
| `docs` | 架构、使用说明与业务文档 |
| `src` | 早期 React 原型，保留作交互参考，不是当前主后台 |

## 2. 快速启动

### 2.1 本地 H2 模式

适合快速体验和开发，不需要 Docker/PostgreSQL。

```bash
npm install
npm run tools:install
npm run api:dev:local
VITE_API_PROXY_TARGET=http://localhost:8081 npm run admin:dev
```

访问地址：

- 管理后台：`http://localhost:5174`
- 本地 API：`http://localhost:8081/api`
- 健康检查：`http://localhost:8081/actuator/health`
- H2 控制台：`http://localhost:8081/h2-console`

说明：`npm run api:dev:local` 会启用 `SPRING_PROFILES_ACTIVE=local`，后端使用 `application-local.yml` 中的 H2 文件库和本地文件存储，脚本默认端口为 `8081`。管理后台的 `/api` 代理默认指向 `8080`，因此 H2 模式启动前端时需要显式设置 `VITE_API_PROXY_TARGET=http://localhost:8081`。默认数据目录为 `~/.ops-erp-data`，可通过 `OPS_ERP_LOCAL_DATA` 与 `OPS_ERP_UPLOAD_PATH` 覆盖。

### 2.2 PostgreSQL/Redis/MinIO 完整模式

适合联调完整基础设施。

```bash
npm install
npm run infra:up
npm run api:dev
npm run admin:dev
```

访问地址：

- 管理后台：`http://localhost:5174`
- API：`http://localhost:8080/api`
- Swagger UI：`http://localhost:8080/swagger-ui`
- OpenAPI JSON：`http://localhost:8080/api-docs`
- MinIO 控制台：`http://localhost:9001`

默认基础设施参数：

| 服务 | 地址 | 账号 | 密码 |
| --- | --- | --- | --- |
| PostgreSQL | `localhost:5432/ops_erp` | `ops_erp` | `ops_erp` |
| Redis | `localhost:6379` | 无 | 无 |
| MinIO | `localhost:9000` | `ops_erp_minio` | `ops_erp_minio_password` |

### 2.3 默认登录

开发环境默认管理员：

- 用户名：`admin`
- 密码：`Admin@123`

生产环境必须修改默认密码、JWT 密钥、数据库密码和对象存储密钥。

### 2.4 常用命令

| 命令 | 作用 |
| --- | --- |
| `npm run admin:dev` | 启动 Vue 管理后台 |
| `npm run admin:build` | 构建管理后台 |
| `npm run admin:preview` | 预览构建产物 |
| `npm run api:dev:local` | 启动 H2 本地后端，默认端口 8081 |
| `npm run api:dev` | 启动 PostgreSQL 模式后端，默认端口 8080 |
| `npm run api:check` | 检查 Java、Maven、端口和健康检查 |
| `npm run infra:up` | 启动 PostgreSQL、Redis、MinIO |
| `npm run infra:down` | 停止本地基础设施 |
| `npm run data:backup` | 备份本地数据 |
| `npm run gen:api` | 按 OpenAPI 生成 TypeScript 类型 |

## 3. 登录、权限与页面进入逻辑

1. 用户打开后台，Vue Router 首先进入路由守卫。
2. 如果访问 `/login`，直接展示登录页。
3. 如果访问业务页但本地没有令牌，跳转到 `/login?redirect=原路径`。
4. 登录页调用 `POST /api/auth/login`，后端校验账号密码并返回 JWT 和当前用户信息。
5. 前端把 JWT 保存到 `localStorage`，键名为 `ops_erp_admin_token`。
6. 后续所有业务请求由 Axios 拦截器自动添加 `Authorization: Bearer <token>`。
7. 刷新页面后，前端调用 `GET /api/auth/me` 重新加载用户、角色和权限。
8. 前端菜单和路由按 `permission` 或 `permissions` 判断是否可见；后端接口再用 `@PreAuthorize` 做最终权限校验。
9. 如果接口返回 401，前端清除令牌并回到登录页；403 显示无权限提示。

管理员角色 `ADMIN` 在前端被视为拥有所有权限。普通角色通过系统管理中的角色与权限配置决定菜单和操作范围。

## 4. 后台首页与全局能力

### 4.1 经营驾驶舱

入口：`经营驾驶舱` 或 `/dashboard`  
关键权限：`dashboard:view`

用于查看合同收入、回款、待收、资金净流量、在建项目、库存资产、待续约合同、低库存、客户投诉、客户利润排行、设备故障排行、人员产值排行等指标。后端主要由 `/api/bi/dashboard`、`/api/bi/company-dashboard` 聚合数据。

### 4.2 统一风险中心

入口：`统一风险中心` 或 `/risk-center`  
关键接口：`/api/risk/items`、`/api/risk/summary`、`/api/risk/workflows`

风险中心把采购、库存、项目、财务、资质、续约、审批等模块的异常汇总成统一风险项。用户可以按风险状态、责任人和业务来源跟踪处理；支持生成当天快照、扫描超时升级、保存规则、批量更新风险工作流。

### 4.3 业务待办中心

入口：`业务待办中心` 或 `/workbench/todos`

用于集中处理跨模块待办，例如待审批报价、采购申请、付款申请、风险闭环、库存预警、逾期应收等。它更偏日常工作入口，风险中心更偏异常治理入口。

### 4.4 全局搜索

顶部搜索框调用 `/api/search?q=关键词`，用于跨客户、项目、合同、审批、档案等对象快速定位。搜索结果会跳转到对应业务页面或详情页。

### 4.5 消息提醒

顶部消息角标每 30 秒调用 `/api/office/notifications/count` 刷新未读数量。消息中心可查看通知列表、手动刷新通知、标记已读。

## 5. CRM 使用教程

入口：左侧菜单 `CRM`

### 5.1 推荐业务顺序

1. 在 `客户池` 建客户档案。
2. 在 `线索商机` 创建商机并持续推进阶段。
3. 在 `报价方案` 为商机创建报价。
4. 如需成本测算，在报价详情发起成本请求，由项目/售前人员填写成本并审批。
5. 报价提交审批，通过后记录客户结果。
6. 客户接受后把报价转换为合同。
7. 合同进入审批、签署和履约。
8. 合同生成应收，财务侧进行开票、回款和核销。
9. 到期合同进入续约管理，风险中心同步续约风险。

### 5.2 客户池

入口：`CRM / 客户池`  
权限：`crm:customer:view`

可查看、新增、编辑、删除客户，维护行业、客户等级、负责人、联系人、地址和开票资料。客户详情展示客户画像、商机、合同、应收、跟进与附件等聚合信息。当前代码支持客户负责人转移。

### 5.3 线索商机

入口：`CRM / 线索商机`  
权限：`crm:opportunity:view`

可新增商机、编辑商机、进入详情、推进阶段、删除商机。商机通常从线索进入跟进、方案、谈判，再到成交或流失。阶段推进会记录业务过程，后续报价和合同均围绕商机展开。

### 5.4 报价方案

入口：`CRM / 报价方案`  
权限：`crm:quote:view`

可新建报价、编辑报价、查看修订、提交审批、审批报价、记录客户结果、转换合同、删除报价。报价成本请求相关操作包括：

- 发起成本请求：销售或报价人员请求售前/项目测算。
- 提交成本：填写人工、材料、外包、差旅、设备、风险准备金、其他成本及税率。
- 审批成本：通过或驳回成本测算。
- 报价定稿：结合成本和建议售价提交审批。

### 5.5 客户合同

入口：`CRM / 客户合同`  
权限：`crm:contract:view`

合同通常由报价转换而来，也可以在业务层按当前页面能力维护。合同详情会关联客户、商机、报价、金额、付款计划、应收、附件和审批记录。合同审批通过后，可进行签署文件审批，并联动财务应收。

### 5.6 跟进回访与续约管理

`跟进回访` 用于记录客户沟通、拜访、电话和线上跟进结果。  
`续约管理` 用于查看即将到期、已到期或需要续签的合同，并沉淀续约建议和风险。

### 5.7 合同应收

入口：`CRM / 合同应收` 或 `财务资金 / 应收管理`

CRM 侧可从合同视角查看应收，支持开票申请、登记发票、记录回款。财务侧提供更完整的应收明细、核销和资金视角。

## 6. 项目管理使用教程

入口：`项目管理`

### 6.1 推荐业务顺序

1. 报价成本请求进入 `售前支持`，项目或售前人员填报成本。
2. 报价转合同后，创建或关联项目。
3. 项目提交立项审批。
4. 审批通过后分配项目经理。
5. 按阶段推进项目：启动、执行、验收、质保等。
6. 归集人工、材料、差旅、外包等成本。
7. 项目利润摘要进入驾驶舱、风险中心和财务分析。

### 6.2 项目列表

入口：`项目管理 / 项目列表`  
权限：`project:view`

可查看分页项目列表、新建项目、查看项目详情、审批项目、分配项目经理、推进阶段、添加成本和删除项目。删除需要 `project:delete` 权限。

### 6.3 售前支持

入口：`项目管理 / 售前支持`

用于处理报价成本测算。接口包括 `/api/projects/presales-support`、`/api/projects/presales-support/{id}/cost`、`/api/projects/presales-support/{id}/approval`。

### 6.4 预算、成本和阶段

`预算执行`、`成本明细`、`阶段履历` 目前共用项目管理视图，通过路由或页面 Tab 展示不同业务切面。成本写入后会影响项目利润、采购成本归集、财务和 BI 指标。

## 7. 供应链采购使用教程

入口：`供应链采购`

### 7.1 推荐业务顺序

1. 在 `供应商` 维护供应商资料、联系人、账期和风险状态。
2. 在 `采购申请` 创建申请，填写物料/服务、数量、预算、需求日期、项目或部门。
3. 提交或处理采购申请审批。
4. 审批通过后生成采购订单。
5. 到货后在 `到货入库` 或订单详情执行收货。
6. 收货触发库存入库和采购应付。
7. 在 `采购应付` 查看应付单，后续进入财务付款申请。
8. 用 `P2P流程` 查看申请、订单、收货、应付的匹配状态。

### 7.2 供应商

入口：`供应链采购 / 供应商`  
支持分页查询、新增、编辑供应商。供应商风险状态可参与审批条件和风险中心规则。

### 7.3 采购申请

入口：`供应链采购 / 采购申请`

支持按状态、类型、项目、供应商等条件查询；可新增申请、编辑申请、审批申请。审批通过后才应进入下单。

### 7.4 采购订单与到货入库

采购订单支持创建和取消。订单收货接口会记录收货数量、到货日期、送货单号、收货人和应付到期日；收货后系统创建到货记录、更新库存，并形成应付基础数据。

### 7.5 成本归集、应付和采购分析

成本归集把采购成本分摊到项目或部门。采购应付供财务确认付款。采购分析用于查看采购结构、供应商表现、到货和付款情况。

## 8. 库存管理使用教程

入口：`库存管理`

### 8.1 库存台账

入口：`库存管理 / 库存台账`

可维护物料编码、名称、规格、单位、单价、安全库存和当前库存。库存低于安全库存时进入补货建议和风险中心。

### 8.2 库存移动

入口：`库存管理 / 库存移动`

可查看物料流水，也可手动写入入库、出库、盘盈、盘亏、调整等移动记录。每次移动会影响物料当前库存。

### 8.3 领料管理

入口：`库存管理 / 领料管理`

项目或业务可创建领料单，选择可领料项目和物料明细。未使用物料可创建退料单，退回库存并形成退料流水。

### 8.4 库存分析

入口：`库存管理 / 库存分析`

查看库存资产、低库存、补货建议、移动趋势和库存结构。

## 9. 人事与员工自助使用教程

### 9.1 人事管理

入口：`人事管理`

员工中心使用资质员工档案作为核心人员数据，配合 HR 模块维护教育经历、工作经历、紧急联系人、入转调离、请假、额度和人力分析。

常用页面：

| 页面 | 主要用途 |
| --- | --- |
| 员工中心 | 员工档案、证书、合同等基础资料 |
| 入转调离 | 创建员工生命周期记录并审批 |
| 请假管理 | 管理员工请假申请和审批 |
| 人力分析 | 查看人员结构、请假和用工指标 |
| 请假额度 | 初始化或调整员工假期额度 |

### 9.2 员工档案详情

入口：员工列表进入详情  
可维护教育经历、工作经历、紧急联系人、证书、合同和生命周期记录。

### 9.3 员工自助

入口：`员工自助`

普通员工可查看：

- 我的工作台：个人待办和审批。
- 我的档案：个人基础资料、证书、合同、教育和工作经历。
- 我的请假：提交和查看请假记录。
- 我的额度：查看假期余额。

员工自助接口位于 `/api/hr/self/**`，后端会按当前登录用户匹配人员档案。

## 10. 资质管理使用教程

入口：`资质管理`

### 10.1 资质总览

入口：`资质管理 / 资质总览`  
展示公司资质、人员证书、项目业绩、到期预警和可用于投标的人员/资质组合。

### 10.2 公司资质

维护公司资质名称、等级、专业、证书编号、发证机构、有效期和附件。到期数据会进入预警中心。

### 10.3 投标查询

按关键词、专业、注册状态、可用状态筛选人员和证书，用于快速判断投标人员配置是否满足要求。

### 10.4 预警中心

展示即将到期或已过期的公司资质、人员证书、合同等预警项。预警也会进入统一风险中心。

### 10.5 附件

资质附件通过 `/api/qualifications/attachments` 上传。后端统一检查文件大小、扩展名和路径安全，并根据存储类型保存到本地或 MinIO。

## 11. OA 协同使用教程

入口：`OA协同`

### 11.1 业务待办审批处理

入口：`业务待办中心 / 审批处理` 或 `/workbench/todos?tab=approvals`

审批能力已统一整合到业务待办中心，不再保留单独审批菜单。这里支持创建审批、处理审批、转交、加签、撤回、退回和重新提交。审批条件可以按金额、部门、业务类型、项目、供应商风险、客户等级等匹配。

常见操作：

- 创建审批：填写类型、标题、来源单号、金额、申请人、内容和条件字段。
- 处理审批：选择通过或驳回，填写审批人和意见。
- 转交：把当前审批任务转给其他用户。
- 加签：新增协同审批人。
- 撤回：申请人撤回已提交的审批。
- 退回/重新提交：处理被退回的审批。

### 11.2 费用报销

入口：`OA协同 / 费用报销`

用于登记费用申请、金额、申请人和业务说明。后续可接入审批和财务付款。

### 11.3 外包服务

入口：`OA协同 / 外包服务`

管理外包申请、供应商、金额、状态和完成情况。完成外包服务可更新业务状态，并影响成本归集。

### 11.4 电子档案

入口：`OA协同 / 电子档案`

支持按业务对象上传、批量上传、搜索、预览、下载、更新和删除档案。档案只在数据库保存元数据，原文件保存在统一存储服务。

### 11.5 消息中心

入口：`OA协同 / 消息中心`

查看系统通知、刷新通知、标记已读。顶部消息角标来自未读数量接口。

### 11.6 操作审计

入口：`OA协同 / 操作审计`

展示用户、请求路径、HTTP 方法、业务模块、对象 ID、状态码、耗时和客户端 IP。审计数据由后端拦截器自动写入。

## 12. 财务资金使用教程

入口：`财务资金`

### 12.1 推荐业务顺序

1. CRM 合同或报价转换生成应收。
2. 财务在 `应收管理` 开票、登记回款。
3. 采购收货生成采购应付。
4. 财务在 `应付管理` 查看应付。
5. 在 `付款申请` 创建付款申请。
6. 审批通过后执行付款。
7. 付款和回款进入资金概览、总账和财务报表。

### 12.2 资金概览

入口：`财务资金 / 资金概览`

展示应收、应付、回款、付款、逾期、现金流等汇总指标。

### 12.3 应收管理

入口：`财务资金 / 应收管理`

可查看应收列表和应收详情，登记发票和回款。详情视图包含合同、客户、发票、回款和核销情况。

### 12.4 应付管理与付款申请

应付管理展示采购、外包等来源的应付。付款申请可创建申请、审批申请、执行付款，并记录付款方式、流水号、付款人和付款日期。

### 12.5 总账报表

入口：`财务资金 / 总账报表`

接口位于 `/api/finance/ledger/**`，包含科目概览、凭证列表和财务报表。

## 13. 系统设置使用教程

入口：`系统设置`

### 13.1 系统运行情况

入口：`系统设置 / 系统运行情况`

查看应用版本、构建时间、数据库、Redis、存储和健康状态。也可直接访问 `/actuator/health` 做探活。

### 13.2 账号、组织、角色、权限

| 页面 | 功能 |
| --- | --- |
| 账号管理 | 新增、编辑、删除用户，重置密码，绑定角色 |
| 组织架构 | 维护树形组织和扁平组织列表 |
| 角色管理 | 新增、编辑、删除角色，分配权限 |
| 权限管理 | 查看、创建、编辑、删除权限点 |

### 13.3 审批人员配置

维护不同流程的审批人、条件、版本和发布状态。支持流程预览、批量预览、诊断、复制、发布、回滚和删除。

### 13.4 流程规则配置

用于维护风险规则和流程规则。具备 `system:role:view` 或 `risk:update` 权限的用户可进入。

### 13.5 删除回收站

入口：`系统设置 / 删除回收站`  
权限：`system:deleted-records:manage`

用于查看软删除记录、审批删除和恢复记录，减少误删风险。

## 14. 个人设置

入口：顶部用户菜单 `个人设置` 或 `/profile`

可修改个人资料、修改密码、维护个人证书和上传个人附件。个人接口位于 `/api/personal/**`。

## 15. 后端运行逻辑

### 15.1 请求链路

```text
浏览器页面
  -> Vue Router 权限守卫
  -> Axios request() 封装
  -> 自动附加 JWT
  -> Spring Security 过滤器链
  -> JwtAuthenticationFilter 解析令牌
  -> Controller 接收请求
  -> @PreAuthorize 校验权限
  -> Service 执行业务规则和事务
  -> Repository 读写数据库
  -> ApiResponse(success/message/data) 返回
  -> Axios 解包 data
  -> 页面刷新状态
```

### 15.2 Spring Boot 启动

后端入口是 `EngineeringOpsErpApplication`。启动时主要完成：

1. 读取 `application.yml` 或 `application-local.yml`。
2. 初始化数据库连接、JPA、Redis、文件存储、Spring Security、Actuator、OpenAPI。
3. 非 local 模式启用 Flyway 迁移数据库结构。
4. 扫描 Controller、Service、Repository、配置类和拦截器。
5. 注册 JWT 认证过滤器和方法级权限。
6. 注册审计拦截器，对 `/api/**` 请求记录操作日志。
7. 执行启动初始化逻辑，例如补齐 `project:delete` 权限并授权给 `ADMIN`。

### 15.3 配置优先级

常用环境变量：

| 变量 | 作用 | 默认值 |
| --- | --- | --- |
| `SERVER_PORT` | 后端端口 | 8080，local 脚本默认 8081 |
| `DB_URL` | PostgreSQL JDBC 地址 | `jdbc:postgresql://localhost:5432/ops_erp` |
| `DB_USERNAME` | 数据库用户名 | `ops_erp` |
| `DB_PASSWORD` | 数据库密码 | `ops_erp` |
| `REDIS_HOST` | Redis 地址 | `localhost` |
| `REDIS_PORT` | Redis 端口 | 6379 |
| `JWT_SECRET` | JWT 签名密钥 | 本地开发默认密钥 |
| `JWT_EXPIRE_MINUTES` | JWT 有效期 | 720 |
| `STORAGE_TYPE` | 文件存储类型 | `local` |
| `LOCAL_STORAGE_PATH` | 本地文件目录 | `.local-data/uploads` |
| `MINIO_ENDPOINT` | MinIO/S3 地址 | `http://localhost:9000` |
| `MINIO_BUCKET` | MinIO bucket | `ops-erp` |

### 15.4 统一响应和异常

所有业务接口统一返回：

```json
{
  "success": true,
  "message": "ok",
  "data": {}
}
```

业务异常、参数校验失败、认证失败、权限不足、接口不存在和未知异常由 `GlobalExceptionHandler` 转换为统一结构。前端 `request<T>()` 会检查 `success`，失败时抛出错误并显示友好提示。

### 15.5 审计逻辑

`AuditInterceptor` 会拦截 `/api/**`，排除健康检查和登录接口。每个请求结束后记录：

- 用户名
- HTTP 方法
- 请求路径与查询参数
- 操作类型：GET 为 READ，POST 为 CREATE_OR_ACTION，PUT/PATCH 为 UPDATE，DELETE 为 DELETE
- 业务模块
- URL 中的业务对象 ID
- 响应状态码
- 请求耗时
- 客户端 IP

## 16. 前端运行逻辑

### 16.1 管理后台启动

`apps/admin` 使用 Vue 3、TypeScript、Vite、Pinia、Vue Router 和 Ant Design Vue。

```text
main.ts
  -> 创建 Vue 应用
  -> 注册 Pinia
  -> 注册 Router
  -> 挂载 App.vue
  -> AppLayout 渲染菜单、顶部栏和 RouterView
```

### 16.2 API 调用

所有 API 统一经过 `apps/admin/src/api/http.ts`：

1. `baseURL` 使用 `VITE_API_BASE_URL`，未配置时为 `/api`。
2. 请求拦截器从 `localStorage` 读取 JWT。
3. 登录请求不带旧令牌，其余请求带 Bearer 令牌。
4. 响应异常按 401、403、502、504、网络错误分别转成中文提示。
5. `request<T>()` 解包后端 `ApiResponse<T>` 的 `data`。

### 16.3 菜单与路由

路由集中定义在 `apps/admin/src/router/index.ts`。每个路由通过 `meta.permission` 或 `meta.permissions` 声明权限。左侧菜单在 `AppLayout.vue` 中按当前用户权限渲染，避免用户看到无权限入口。

## 17. 核心 API 入口速查

| 模块 | 主要 API 前缀 | 说明 |
| --- | --- | --- |
| 认证 | `/api/auth` | 登录、当前用户 |
| BI | `/api/bi` | 经营驾驶舱、公司 KPI |
| CRM | `/api/crm` | 客户、商机、报价、合同、应收、附件 |
| 项目 | `/api/projects` | 项目、售前支持、阶段、成本 |
| 采购 | `/api/procurement` | 供应商、申请、订单、收货、应付、匹配 |
| 库存 | `/api/inventory` | 物料、流水、领料、退料、补货建议 |
| 人事 | `/api/hr` | 员工履历、请假、额度、导入导出 |
| 员工自助 | `/api/hr/self` | 我的档案、请假、待办、审批 |
| 资质 | `/api/qualifications` | 公司资质、人员、证书、业绩、投标、预警 |
| 资质文件 | `/qualification-files` | 资质附件访问 |
| OA | `/api/office` | 审批、费用、外包、档案、消息、审计 |
| 财务 | `/api/finance` | 资金概览、应收、应付、付款 |
| 总账 | `/api/finance/ledger` | 科目、凭证、报表 |
| 风险 | `/api/risk` | 风险项、汇总、规则、快照、升级 |
| 风险工作流 | `/api/risk/workflows` | 风险处理、批量处理、动作历史 |
| 系统 | `/api/system` | 健康、版本、审批配置、流程规则 |
| 用户/角色/权限 | `/api/system/users` 等 | 系统基础管理 |
| 全局搜索 | `/api/search` | 跨模块搜索 |
| 个人设置 | `/api/personal` | 个人资料、密码、证书、附件 |

## 18. 跨模块业务闭环

### 18.1 销售到回款

```text
客户
  -> 商机
  -> 报价
  -> 成本测算
  -> 报价审批
  -> 合同
  -> 合同审批/签署
  -> 应收
  -> 开票
  -> 回款
  -> 财务报表/驾驶舱
```

### 18.2 采购到付款

```text
供应商
  -> 采购申请
  -> 申请审批
  -> 采购订单
  -> 到货收货
  -> 库存入库
  -> 采购应付
  -> 付款申请
  -> 付款审批
  -> 执行付款
  -> 总账/报表
```

### 18.3 项目成本闭环

```text
售前成本测算
  -> 项目立项
  -> 项目经理分配
  -> 阶段推进
  -> 人工/材料/差旅/外包成本
  -> 项目利润
  -> 风险预警
  -> 经营驾驶舱
```

### 18.4 风险闭环

```text
业务模块产生异常
  -> 风险规则识别
  -> 风险中心汇总
  -> 分配责任人
  -> 处理记录/根因/预防措施
  -> 关闭或复发标记
  -> 快照趋势和驾驶舱
```

## 19. 数据、附件与安全

### 19.1 数据库

PostgreSQL 模式使用 Flyway 管理结构迁移，迁移文件位于 `services/api/src/main/resources/db/migration`。本地 H2 模式关闭 Flyway，使用 JPA `ddl-auto:update` 快速建表。

所有核心表按当前设计预留租户、创建人、更新人、创建时间和更新时间等字段，为后续多公司、多账套扩展留接口。

### 19.2 文件存储

系统通过统一 `FileStorageService` 管理附件：

- 开发默认 `local`，文件写入本地目录。
- 生产推荐 `minio`，文件写入 MinIO/S3 兼容对象存储。
- 数据库保存文件名、大小、类型、业务对象、存储 key 等元数据。
- 下载和预览通过后端接口或预签名临时 URL 完成。
- 上传会校验大小、扩展名和路径安全。

### 19.3 安全

- 后端关闭 Session，使用无状态 JWT。
- 登录、健康检查、OpenAPI 可匿名访问，其余接口需要认证。
- 业务接口通过 `@PreAuthorize` 做权限控制。
- CORS 允许本地开发端口和已配置的生产域名。
- 生产必须替换默认 JWT 密钥和所有默认密码。

## 20. 部署说明

生产部署推荐参考 `deploy/README.md`：

1. `./deploy/build.sh` 构建后端 jar 和前端 dist。
2. `./deploy/deploy.sh root@服务器` 推送产物和配置。
3. 服务器配置 `/etc/ops-erp/ops-erp.env`。
4. 启动 PostgreSQL、Redis、MinIO。
5. 用 systemd 启动 `ops-erp-api`。
6. Nginx 托管前端静态资源，并把 `/api/` 反向代理到后端。

典型生产链路：

```text
浏览器
  -> Nginx
  -> / 读取 apps/admin/dist
  -> /api/ 转发到 Spring Boot 8080
  -> PostgreSQL/Redis/MinIO
```

## 21. 排错指南

| 现象 | 可能原因 | 处理 |
| --- | --- | --- |
| 登录页提示后端不可访问 | API 未启动或端口不一致 | 执行 `npm run api:check`，确认 8080/8081 |
| H2 模式前端调不到 API | 前端代理指向 8080，但 local API 在 8081 | 开发代理模式设置 `VITE_API_PROXY_TARGET=http://localhost:8081`；直连模式设置 `VITE_API_BASE_URL=http://localhost:8081/api` |
| 401 后回到登录页 | JWT 过期、无效或被清除 | 重新登录 |
| 403 无权限 | 角色未分配对应权限 | 管理员在角色管理中补权限 |
| 文件上传失败 | 文件过大、扩展名不允许、存储不可用 | 检查文件限制和存储健康状态 |
| PostgreSQL 连接失败 | Docker 未启动或端口占用 | `npm run infra:up`，检查 5432 |
| MinIO 附件不可预览 | bucket、endpoint 或密钥错误 | 检查 `MINIO_*` 环境变量 |
| 页面看不到菜单 | 当前用户没有菜单权限 | 用 ADMIN 登录或调整角色 |
| OpenAPI 无法访问 | 后端未启动或路径错误 | 使用 `/swagger-ui` 或 `/api-docs` |

## 22. 实施建议

1. 先用 H2 模式完成演示和培训，确认业务流程。
2. 再切到 PostgreSQL 模式做多人联调和数据留存。
3. 上线前配置正式组织、角色、用户和审批流程。
4. 按业务闭环分批启用模块：CRM/项目/采购/库存/财务优先，OA/风险/BI 随后治理。
5. 定期检查操作审计、风险中心、低库存、逾期应收和合同续约。
6. 生产使用 MinIO/S3 存储附件，并配置数据库与对象存储备份。

## 23. 测试报告核对说明

`SYSTEM_TEST_REPORT.md` 中有一部分失败来自旧接口路径、聚合页误解或权限名称口径不一致，并不代表后端接口缺失。后续 API 测试建议优先以 `apps/admin/src/api/*.ts` 和 Controller 映射为准。

### 23.1 明确误报或测试口径需调整

| 报告项 | 测试报告口径 | 当前正确口径 | 结论 |
| --- | --- | --- | --- |
| 采购申请接口不存在 | `/api/procurement/purchase-requests` | `/api/procurement/requests`、`/api/procurement/requests/{id}/approval` | 误报，接口存在但路径不同 |
| 库存移动接口不存在 | `/api/inventory/movements` | `/api/inventory/parts/{partId}/movements` | 误报，库存流水按物料查询和写入 |
| 库存分析接口不存在 | `/api/inventory/analytics` | 页面聚合 `/api/inventory/parts` 和 `/api/inventory/replenishment-suggestions` | 误报，当前没有单独 analytics API |
| 员工中心接口不存在 | `/api/hr/employees` | 员工基础档案在 `/api/qualifications/employees`，HR 扩展资料在 `/api/hr/employees/{employeeId}/...` | 误报，员工中心复用资质员工档案 |
| 入转调离接口不存在 | `/api/hr/lifecycle` | `/api/hr/lifecycles`、`/api/hr/employees/{employeeId}/lifecycles` | 误报，路径为复数并支持员工维度 |
| 资质总览接口不存在 | `/api/qualifications` | `/api/qualifications/dashboard` | 误报，根路径不提供资源 |
| 总账报表接口不存在 | `/api/finance/ledger` | `/api/finance/ledger/overview`、`/vouchers`、`/statements` | 误报，根路径不提供资源 |
| 合同创建 API 异常 | 独立创建合同 | 当前业务从报价执行 `POST /api/crm/quotes/{id}/convert` 转合同 | 大概率为测试口径问题；若 convert 返回 500 才是后端缺陷 |
| 报价审批失败：需先成本审批 | 直接审批报价 | 报价需先完成成本请求/提交/审批，再提交报价审批 | 业务规则，不是缺陷 |

### 23.2 确实存在或需要治理的问题

| 问题 | 判断 | 解决方案 |
| --- | --- | --- |
| 成本提交/审批账号无权限 | 真实权限配置问题 | 给项目交付、售前或总经办相关角色授予 `crm:quote:cost`；报价审批人还需 `crm:quote:approve` 并满足审批流 `QUOTE` 的审批人配置 |
| 报告中的成本权限名不匹配 | 文档/测试口径问题 | 当前代码使用单一权限 `crm:quote:cost`，不是 `crm:quote:cost:submit` 和 `crm:quote:cost:approve`；如要区分提交/审批，需要新增权限点并改 Controller |
| 跟进回访、费用报销、外包、人事、资质、系统设置多处 403 | 多数是角色权限配置问题 | 按岗位补齐 `crm:followup:view/create`、`office:expense:view/create`、`office:outsource:view/create/complete`、`qualification:*`、`system:*` 等权限 |
| 总经办账号不能管理用户/角色/权限 | 真实角色设计问题 | 若总经办应承担系统管理职责，授予 `system:user:view/create/update/delete/reset-password`、`system:role:view/create/update/delete`、`system:permission:view` 等权限；否则报告应改为“预期无权限” |
| 错误提示“系统繁忙”定位困难 | 真实体验问题 | 后端对可预期业务异常继续抛 `BusinessException`；未知异常保留通用文案但在审计日志/应用日志记录 requestId、异常类型和堆栈 |
| 参数校验提示不够明确 | 真实体验问题 | 对枚举参数转换失败、阶段流转非法、金额/日期字段校验失败补充明确错误消息 |
| 测试数据不足 | 真实测试覆盖问题 | 增加供应商、采购订单、库存物料、员工、证书、费用、外包、付款申请等种子数据或测试前置脚本 |

### 23.3 建议测试脚本修正

1. API 路径直接复用 `apps/admin/src/api/*.ts` 中的 URL，不要按页面路径推断接口路径。
2. 用不同角色测试时先列出预期权限，403 只有在“该角色应该能做”时才记为缺陷。
3. 合同流程测试应走 `报价客户结果接受 -> POST /api/crm/quotes/{id}/convert -> 合同审批 -> 签署文件审批`。
4. 库存移动测试需先创建或查询一个物料，再对该物料调用 `/api/inventory/parts/{partId}/movements`。
5. 员工中心测试应先调用 `/api/qualifications/employees` 获取员工，再测试 `/api/hr/employees/{employeeId}/educations`、`/lifecycles`、`/leaves` 等 HR 扩展接口。
6. 财务总账测试不要请求 `/api/finance/ledger` 根路径，应分别请求 `/overview`、`/vouchers`、`/statements`。
