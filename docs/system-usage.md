# Engineering Ops ERP 系统使用手册

> 综合业务管理系统 · 功能全景文档
> 最后更新：2026-07-02

---

## 一、系统概览

Engineering Ops ERP 是一套面向普通企业的一体化管理系统，覆盖**客户关系管理（CRM）、项目管理、供应链采购、仓储库存、服务维护、人力资源管理、资质管理、财务管理、OA 协同办公**九大核心业务模块，并提供**经营驾驶舱（BI）**与**系统管理**支撑。

### 技术架构

| 层级 | 技术栈 |
|------|--------|
| 前端 | Vue 3 + TypeScript + Vite + Ant Design Vue |
| 后端 | Spring Boot 3 + Spring Data JPA + Spring Security |
| 数据库 | PostgreSQL 16（开发可用 H2 替代） |
| 缓存 | Redis 7 |
| 文件存储 | MinIO |
| 数据库迁移 | Flyway |

---

## 二、模块功能详解

### 2.1 经营驾驶舱 Dashboard

路径：`/dashboard`
权限：`dashboard:view`

作为系统的首页看板，集中呈现企业经营的核心指标与趋势图表。

**关键指标区域：**

- **合同收入规模** — 全部已签合同的累计金额
- **累计回款** — 已实际收到的款项总额
- **待收金额** — 到期尚未收回的应收账款
- **资金净流量** — 回款总额 - 付款总额，正数为净流入
- **在建项目** — 状态为"进行中"的项目数量（个）
- **进行中工单** — 状态为"已派工"或"进行中"的服务工单数
- **库存资产** — 库存物料按单价计算的资产总值
- **待续约合同** — 30 天内到期、需要续约的合同数量
- **已闭环工单** — 已完成验收的工单累计数
- **项目累计成本** — 各项目已归集的实际成本之和
- **低库存物料** — 当前库存低于安全库存线的物料数量（红色预警）
- **客户投诉** — 关联投诉的工单或跟进记录数

**趋势与排行：**

- **月度经营趋势** — 按月展示营收、回款、成本曲线的折线图
- **客户利润排行** — 按客户维度展示：合同金额、回款金额、工单收入与服务成本、综合毛利
- **设备故障排行** — 统计各设备关联的工单数、故障数、服务成本
- **人员产值排行** — 按工程师维度：闭环工单数、总工时、产值、服务成本、人时产值

---

### 2.2 客户管理 CRM

#### 2.2.1 客户池

路径：`/crm/customers`
权限：`crm:customer:view`

统一管理企业所有客户档案，提供战略客户 / 风险客户标识。

- 客户列表搜索（按名称、编码、行业、负责人过滤）
- 客户等级筛选（战略/重要/普通）
- **新增客户** — 录入名称、统一社会信用代码、行业、等级、联系人、地址等
- **编辑客户** — 修改客户档案全部字段
- **删除客户** — 联级删除关联商机、合同、报价、跟进
- **客户详情/画像** — 全景视图：基本信息、关联商机列表、已签合同（份数与总额）、跟进历史、服务工单

#### 2.2.2 商机管理

路径：`/crm/opportunities`
权限：`crm:opportunity:view`

跟进销售线索与机会。

- 商机列表：客户名称、商机名称、预计金额、阶段（跟进/方案/谈判/成交/流失）
- **新增商机** — 关联客户、填写商机名称、预计金额、来源、跟进人
- **商机推进** — 按阶段逐步推进（跟进→方案→谈判→成交/流失）
- **商机详情** — 完整商机信息与推进历史
- **删除商机**

#### 2.2.3 报价方案

路径：`/crm/quotes`
权限：`crm:quote:view`

对商机输出正式报价。

- 报价列表：客户、商机、报价编号、版本号、金额、状态（草稿/待审批/已通过/已驳回/已转换）
- **新增报价** — 关联商机与客户，填入报价明细
- **编辑报价**
- **提交审批** — 发起报价审批流程
- **处理审批** — 通过或驳回
- **报价修订** — 查看修订历史
- **客户结果** — 记录客户最终决策（接受/拒绝/部分接受）
- **转为合同** — 一键转换为正式合同
- **删除报价**

#### 2.2.4 合同管理

路径：`/crm/contracts`
权限：`crm:contract:view`

- 合同列表：编号、客户、名称、金额、签订日期、到期日期、状态（执行中/即将到期/已到期）
- **合同详情** — 查看完整条款、付款计划、关联应收记录

#### 2.2.5 跟进回访

路径：`/crm/follow-ups`
权限：`crm:followup:view`

- 跟进列表：客户、主题、方式（电话/上门/线上/其他）、跟进人、时间
- **新增跟进** — 关联客户、填写跟进内容与结果

#### 2.2.6 续约管理

路径：`/crm/renewals`
权限：`crm:renewal:view`

- 续约列表：合同信息、到期日期、预警状态、续约建议
- 到期合同自动标记预警

#### 2.2.7 合同应收

路径：`/crm/receivables`
权限：`crm:receivable:view`

- 应收列表：合同、应收金额、已开票、已回款、逾期天数、状态
- **开发票** — 记录发票信息
- **记录回款** — 登记实际回款流水

---

### 2.3 项目管理

路径：`/projects`
权限：`project:view`

覆盖项目从立项到验收的全生命周期。

**立项阶段：**
- **新增项目** — 填写名称、类型、客户、负责人、预算金额
- **立项审批** — 提交审批，通过后进入"进行中"

**执行阶段：**
- **阶段推进** — 启动→执行→验收→质保
- **成本归集** — 人工/材料/差旅/外包，按类别记录实际成本
- **预算执行看板** — 各科目预算 vs 实际对比
- **阶段记录** — 所有阶段推进的历史日志

**项目列表关键列：** 项目/客户名称、类型/负责人、阶段/进度、立项审批状态、合同金额、预算/实际成本对比、成本分类明细

---

### 2.4 供应链采购

路径：`/procurement`
权限：`procurement:view`

覆盖采购申请→审批→下单→收货→应付的全链路。

**采购申请：**
- **新增采购申请** — 物料/服务说明、数量、预估金额、需求日期、采购类别
- **申请审批** — 提交进入审批流程
- 待审批数统计

**采购订单：**
- **下单** — 从已审批申请生成正式订单，关联供应商

**收货入库：**
- **分批收货** — 分批到货登记，自动触发库存入库

**采购应付：**
- 收货后自动创建应付单
- 应付金额、已付金额、余额

**供应商管理：**
- **新增供应商** — 名称、联系人、电话、地址、账期

**成本归集：**
- 按项目/部门归集采购成本

---

### 2.5 库存管理

路径：`/inventory`
权限：`inventory:view`

**物料档案：**
- **新增物料** — 名称、编码、规格、单位、单价、安全库存量

**库存流水：**
- 出入库流水明细
- **手动出入库** — 调整库存（入库/出库/盘盈/盘亏）

**项目领料与退料：**
- **新增领料单** — 关联项目，领用物料
- **领料归还** — 未使用物料退回库存

**预警：** 低库存物料自动标记

---

### 2.6 服务维护

#### 2.6.1 工单管理

路径：`/maintenance/work-orders`
权限：`maintenance:workorder:view`

- 工单列表：编号、设备、客户、故障描述、紧急程度、状态（待派工/已派工/进行中/待验收/已完成/已关闭）
- **创建工单** — 关联设备与客户
- **派工** — 选择可派工人员
- **现场签到** — 签到记录（含位置）
- **完工提交** — 处理结果、耗时、物料消耗
- **完工验收** — 确认完成

#### 2.6.2 资产设备

路径：`/maintenance/equipment`
权限：`maintenance:equipment:view`

- 设备列表：名称、型号、所属客户、安装位置、状态
- **新增设备**

#### 2.6.3 服务计划

路径：`/maintenance/plans`
权限：`maintenance:plan:view`

- 计划列表：名称、关联设备、周期（日/周/月/季度/年）、状态
- **新增计划**
- **自动生成工单** — 到期计划一键生成工单

#### 2.6.4 人员排班与考勤

权限：`workforce:view`

- 排班计划、签到记录、工时统计

---

### 2.7 人力资源管理

路径：`/hr`
权限：`qualification:employee:view`, `qualification:certificate:view`, `workforce:view`

**员工管理：**
- 员工列表：姓名、手机、邮箱、部门、岗位、入职日期
- **新增/编辑/删除员工**

**人员证书：**
- 证书列表：名称、编号、颁发机构、有效期
- **新增证书** — 支持上传图片或 PDF
- **到期预警**

**员工合同：**
- 合同列表：类型、签订日期、到期日期
- **新增合同**

**排班与考勤：**
- 排班计划、签到记录

---

### 2.8 资质管理

路径：`/qualification`
权限：`qualification:view`

面向工程/服务型企业的投标资质管理体系。

**公司资质（/qualification/companies，权限 qualification:company:view）：**
- 资质名称、等级、颁发机构、有效期、附件上传
- 新增/编辑/删除，到期预警

**人员证书（/qualification/certificates，权限 qualification:certificate:view）：**
- 证书名称、持证人员、编号、有效期
- 新增/编辑/删除，到期预警

**人员档案（/qualification/employees，权限 qualification:employee:view）：**
- 与 HR 模块共享的员工档案，包括劳动合同管理

**项目业绩（/qualification/performances，权限 qualification:performance:view）：**
- 项目名称、合同金额、完成日期、客户、证明文件
- 新增/编辑/删除

**投标组合查询（/qualification/tender，权限 qualification:tender:view）：**
- 根据投标要求智能组合查询满足条件的公司资质 + 人员证书 + 项目业绩

**资质预警（/qualification/warnings，权限 qualification:warning:view）：**
- 集中展示即将过期或已过期的公司资质与人员证书，红色标记预警

---

### 2.9 财务管理

**资金概览（/finance/overview，权限 finance:view）：**
- 应收总额、逾期应收、应付总额、逾期应付、资金净流入、回款率/付款率

**应收管理（/finance/receivables，权限 finance:receivable:view）：**
- 来源合同、客户、应收金额、已开票、已回款、余额、到期日、逾期状态
- 开票、回款登记

**应付管理（/finance/payables，权限 finance:payable:view）：**
- 来源（采购/外包/报销）、供应商/收款方、应付金额、已付金额、余额

**付款申请（/finance/payment-applications，权限 finance:payable:view / finance:payment:approve / finance:payment:execute）：**
- 新增付款申请 → 审批 → 执行付款

**总账报表（/finance/ledger，权限 finance:ledger:view）：**
- 科目概览（资产/负债/收入/成本/费用）
- 自动会计凭证（业务驱动，系统自动生成）
- 财务报表（资产负债表、利润表）

---

### 2.10 OA 协同办公

**审批中心（/office/approvals，权限 office:approval:view）：**
- 统一审批入口，汇集各模块待审批事项
- 发起审批 / 处理审批

**费用报销（/office/expenses，权限 office:expense:view）：**
- 报销编号、类型（差旅/办公/招待/其他）、金额、状态
- 新增报销（可关联项目/工单）→ 自动进入审批，审批后生成应付

**外包服务（/office/outsourcing，权限 office:outsource:view）：**
- 外包编号、供应商、服务类型、金额、计划日期、状态
- 新增外包 / 验收完成（成本自动归集）

**电子档案（/office/documents，权限 office:document:view）：**
- 文件名、类型（合同/资质/工单/报销/通用）、大小、上传人、时间
- 上传/预览/下载/编辑/删除，支持按业务类型关联

**消息中心（/office/notifications，权限 office:notification:view）：**
- 消息列表、标记已读、未读消息统计

**操作审计（/office/audits，权限 office:audit:view）：**
- 审计日志：操作人、时间、IP、操作类型、对象、详情

---

### 2.11 系统管理

路径：`/system`
权限：`system:view`

**组织架构（/system/organizations，权限 system:organization:view）：**
- 组织树：部门/公司层级树形展示
- 新增/编辑/删除组织结构

**角色管理（/system/roles，权限 system:role:view）：**
- 角色列表：名称、编码、数据权限范围（全部/本部门及下属/仅本人）
- 新增/编辑/删除（分配权限集）

**权限管理（/system/permissions，权限 system:permission:view）：**
- 权限清单：编码、名称、所属模块
- 新增/编辑/删除

**用户管理（重定向至 HR）：**
- 用户列表、关联员工、分配角色、重置密码

---

### 2.12 个人设置

路径：`/profile`

- **个人信息** — 查看/修改姓名、手机、邮箱
- **修改密码** — 原密码验证后设置新密码
- **个人证书** — 管理个人名下执业/资格证书（CRUD + 到期预警）
- **个人附件** — 上传/管理个人文件

---

## 三、业务流转关系

```
客户管理 CRM -> 报价方案 -> 合同管理 -> 应收管理
    |                          |
    +-- 商机 -> 报价 -> 合同 -> 续约管理
    +-- 客户画像（整合合同、工单、利润）

项目管理 <-- 客户合同（关联项目）
    |-- 预算审批 -> 阶段推进 -> 成本归集
    |-- 领料出库（调用库存模块）
    +-- 合同回款（影响应收管理）

供应链采购 <-- 项目采购需求
    |-- 采购申请 -> 审批 -> 订单 -> 收货入库
    |-- 收货 -> 自动生成库存入库 + 应付
    +-- 应付联动 -> 应付管理 -> 付款申请

库存管理 <-- 采购收货入库
    |-- 项目领料 -> 出库
    |-- 项目退料 -> 入库
    +-- 工单物料消耗 -> 出库

服务维护 <-- 资产管理 -> 服务计划 -> 自动工单
    |-- 派工 -> 签到 -> 完工 -> 验收
    |-- 工单物料消耗（调用库存）
    +-- 工单成本归集

人力资源 <-- 员工档案（共享资质模块）
    |-- 证书管理（共享资质模块）
    |-- 排班与考勤
    +-- 员工合同

资质管理 <-- 公司资质 + 人员证书 + 项目业绩
    |-- 投标组合查询
    +-- 到期预警

财务管理
    |-- 应收管理（来源：合同 + 开票 + 回款）
    |-- 应付管理（来源：采购 + 报销 + 外包）
    |-- 付款申请 -> 审批 -> 付款执行
    +-- 总账报表（自动凭证）

OA 协同办公
    |-- 审批中心 <-- 报价/立项/付款/报销/外包审批
    |-- 费用报销 -> 自动生成应付
    |-- 外包服务 -> 验收后归集成本
    |-- 电子档案 <-- 各模块附件统一管理
    |-- 消息中心 <-- 审批通知、到期预警
    +-- 操作审计 <-- 记录所有关键操作
```

---

## 四、权限体系

### 数据权限模型

- **角色** 绑定权限编码 + 数据范围（全部/本部门及下属/仅本人）
- **组织架构** 作为数据隔离的基础
- **Spring Security** + `@PreAuthorize` 注解控制 API 级权限

### 主要权限编码一览

| 模块 | 关键权限 |
|------|---------|
| 驾驶舱 | `dashboard:view`, `bi:view` |
| CRM | `crm:customer:*`, `crm:opportunity:*`, `crm:quote:*`, `crm:contract:*`, `crm:followup:*`, `crm:renewal:*`, `crm:receivable:*` |
| 项目 | `project:view / create / approve / stage / cost` |
| 采购 | `procurement:view`, `procurement:payable:view` |
| 库存 | `inventory:view / create / edit` |
| 维护 | `maintenance:equipment:*`, `maintenance:workorder:*`, `maintenance:plan:*`, `workforce:*` |
| 人力 | `qualification:employee:*`, `qualification:certificate:*` |
| 资质 | `qualification:company:*`, `qualification:employee:*`, `qualification:certificate:*`, `qualification:performance:*`, `qualification:tender:view`, `qualification:warning:view` |
| 财务 | `finance:view / receivable:view / payable:view / payment:approve / payment:execute / ledger:view` |
| OA | `office:*`（含 approval / expense / outsource / document / notification / audit 子权限）|
| 系统 | `system:view / user:* / role:* / organization:* / permission:*` |

---

## 五、开发与测试

### 默认管理员账号

- 用户名：`admin`
- 密码：`Admin@123`

### 本地启动（完整基础设施 - PostgreSQL）

```bash
npm install
docker compose -f infra/docker-compose.yml up -d
npm run api:dev
npm run admin:dev
```

### 本地启动（轻量模式 - H2 文件数据库）

```bash
npm install
npm run tools:install
npm run api:dev:local
npm run admin:dev
```

### 访问地址

- 管理后台：http://localhost:5174
- 后端 API：http://localhost:8080/api
- API 健康检查：http://localhost:8080/actuator/health
- H2 控制台（仅 local 模式）：http://localhost:8080/h2-console

---

## 六、数据初始化

系统预设了 Flyway 迁移脚本自动导入的演示数据：

- **基础**：组织架构、管理员角色、权限矩阵、种子用户
- **CRM**：客户、商机、报价、合同、跟进记录
- **项目**：项目、预算科目、成本条目
- **库存**：物料档案
- **资质**：10 名员工、18 张人员证书、35 项公司资质、252 条项目业绩
- **财务**：应收、应付、付款账户参考
- **维护**：设备、工单、服务计划
- **OA**：审批、报销、外包示例
- **人力**：排班、考勤记录

---

## 📌 v0.2 新增功能（2026-07）

### 操作审计日志

后端自动记录所有 API 请求（路径、方法、状态码、耗时、客户端 IP、操作人）。

- **位置**：OA协同 → 操作审计
- **功能**：按时间范围/操作人筛选，分页浏览，实时刷新
- **后端**：`SystemAuditLog` 实体 + `AuditInterceptor` 拦截器
- **接口**：`GET /api/office/audits?page=&size=`

### 消息通知系统

- **未读角标**：侧边栏"消息中心"项实时显示未读数（红色 badge）
- **自动轮询**：每 30 秒自动检查 `/api/office/notifications/count`
- **事件触发**：审批创建/完成、费用报销提交、合同到期预警等自动生成通知
- **接口**：`GET /api/office/notifications` · `POST /api/office/notifications/{id}/read` · `GET /api/office/notifications/count`

### HR 模块（员工档案扩展）

**新增 REST 端点**（`/api/hr/`）覆盖员工子档案：

| 端点 | 功能 |
|------|------|
| `employees/{id}/educations` | 教育经历 CRUD |
| `employees/{id}/work-experiences` | 工作经历 CRUD |
| `employees/{id}/emergency-contacts` | 紧急联系人 CRUD |
| `employees/{id}/lifecycles` | 入职/调岗/离职生命周期 |
| `employees/{id}/leaves` | 请假申请与审批 |
| `employees/{id}/leave-balances` | 年假/病假额度管理 |
| `/hr/analytics` | 人力分析（在职/离职/学历/组织分布） |
| `/hr/export/employees` | Excel 导出（员工花名册） |
| `/hr/import/employees` | Excel 导入（批量新增员工） |
| `/hr/export/template` | 导入模板下载 |

权限使用 `qualification:employee:view`（读）和 `qualification:employee:manage`（写）。

### CRM Excel 导出

客户和合同列表支持一键导出 `.xlsx`：

- **客户导出**：编码 · 名称 · 行业 · 等级 · 负责人 · 付款习惯 · 风险状态
- **合同导出**：编号 · 客户 · 项目名称 · 金额 · 类型 · 起止日期 · 状态
- **接口**：`GET /api/crm/export/customers` · `GET /api/crm/export/contracts`

### 系统架构调整

- **`SystemService`** 拆分为 `UserService`/`RoleService`/`PermissionService`/`OrganizationService`
- **`BiService`** 数据库级聚合（按月营收趋势、现金流汇总、工作量统计由 JPQL GROUP BY 替代全表扫描）
- **前端**：`unplugin-vue-components` 自动按需注册 Ant Design Vue 组件，`main.ts` 从 91 行精简至 13 行
- **安全**：CRM 删除权限从手动 ADMIN 角色检查改为声明式 `@PreAuthorize("hasAuthority('crm:*:delete')")`
- **构建**：`vite build` 已修复 4 个模板语法错误，可正常产出生产包

---
> 财务模块功能更新：2026-07-05
> 

### 2.9 财务管理（增强版）

#### 2.9.1 功能全景

| 子模块 | 主要功能 | 交互方式 |
|--------|----------|----------|
| **指标仪表盘** | 应收/应付余额、逾期笔数、可用资金、凭证数、净利润、现金余额、预算节余、待批付款 | 动态指标卡（最高 12 个） |
| **应收管理** | 应收列表、开票、回款登记、核销、日期筛选、CSV 导出 | 操作按钮 + 日期输入 + 下载 |
| **应付管理** | 应付列表、申请付款（自动生成付款申请） | 操作按钮 + API 联动 |
| **付款审批** | 付款申请列表、批准/拒绝、执行付款 | 三步骤操作（申请→审批→执行） |
| **回款预测** | 按周预测未来 8 周回款金额 | 柱状图 + 到期笔数标注 |
| **应收账龄** | 0-30 / 31-60 / 61-90 / 90+ 天分布 | 金额 + 占比标签 |
| **总账凭证** | 会计凭证列表、分录详情展开 | 点击凭证行展开分录明细表 |
| **财务报表** | 资产总计、负债总计、总收入、总费用、净利润、净现金流 | 指标卡片式展示 |
| **凭证生成** | 从业务线索一键生成草稿凭证 | "生成凭证"操作按钮 |
| **审计日志** | 操作时间轴（开票/回款/核销/付款/审批/凭证/关账） | 最近 30 条滚动展示 |
| **会计期间** | 当前期间显示、关账按钮、关账状态指示 | 顶部面板 |
| **付款统计** | 按付款方式分布（银行转账/支票/现金/微信/支付宝） | 标签统计 |

#### 2.9.2 指标卡说明

财务页面顶部展示动态指标卡，当后端可用时最多展示 12 个：

| 指标 | 计算公式 / 数据源 | 颜色规则 |
|------|-------------------|----------|
| 应收余额 | `GET /finance/overview → receivableOutstanding` | 黄色预警 |
| 应付余额 | `GET /finance/payables → payables.filter(status!=已付款)` | 蓝色信息 |
| 本月核销 | 硬编码模拟 | 绿色（82% 回款率） |
| 应收逾期 | `receivables.filter(status=逾期).length` | 红色危险 |
| 应付逾期 | `payables.filter(status=逾期).length` | 红色危险 |
| 可用资金 | 应收余额 - 应付余额 + 本月核销 | 蓝色信息 |
| 凭证数（后端可用时） | `GET /finance/ledger/overview → voucherCount` | 蓝色信息 |
| 净利润（后端可用时） | `GET /finance/ledger/overview → profit` | 盈利绿/亏损红 |
| 现金余额（后端可用时） | `GET /finance/ledger/overview → cashBalance` | 蓝色信息 |
| 预算节余 | 项目预算汇总 - 项目成本汇总 | 结余绿/超支红 |
| 待批付款（有数据时） | `paymentApps.filter(status=PENDING).length` | 黄色预警 |

#### 2.9.3 应收操作流程

```
应收列表 → 待开票 → 点击"开票" → POST /api/finance/receivables/{id}/invoice
          → 待回款 → 点击"回款登记" → POST /api/finance/receivables/{id}/receipts
          → 待回款/逾期 → 点击"核销" → 本地状态更新
          → 日期筛选 → filterDateStart / filterDateEnd 输入框
          → CSV导出 → 生成 UTF-8 BOM CSV 文件下载
```

**日期筛选：** 应收/应付面板顶部嵌入两个 date 输入框 + 清除按钮，联动 `filterDateStart` / `filterDateEnd` 状态，筛选结果实时响应。

**CSV 导出：** 点击"导出CSV"按钮，当前 `receivables` 数组 → 拼装 BOM + header + rows → Blob → URL.createObjectURL → 自动下载 `应收款.csv`。

#### 2.9.4 付款全流程

```
应付列表 → 点击"申请付款" → createPaymentApplication API
  → 付款管理面板（待审批状态）
    ├─ 点击"批准" → POST /api/finance/payment-applications/{id}/approval (decision=APPROVED)
    │  → 状态变为"已批准"
    │  → 点击"执行付款" → POST /api/finance/payment-applications/{id}/payment
    │  → 状态变为"已付款"，数据刷新
    └─ 点击"拒绝" → POST /api/finance/payment-applications/{id}/approval (decision=REJECTED)
      → 状态变为"已拒绝"
```

#### 2.9.5 总账面板

总账面板为右侧三栏切换面板（mini-action 按钮组）：

| 标签 | 组件 | 数据源 | 说明 |
|------|------|--------|------|
| 会计凭证 | `VoucherPanel` | `GET /finance/ledger/vouchers` | 凭证列表 + 点击展开分录明细 |
| 财务报表 | `FinancialStatementsPanel` | `GET /finance/ledger/statements` | 资产/负债/收入/费用/利润/现金流 |
| 付款管理 | `PaymentApplicationsPanel` | `GET /finance/payment-applications` | 待批/已批/已付列表 + 操作按钮 |

**凭证详情展开：** 点击凭证行 → 展开分录明细表格（科目编码、科目名称、借方、贷方、摘要），再次点击收起。

**付款方式统计：** 付款管理面板顶部统计各付款方式笔数（银行转账/支票/现金/微信/支付宝），从 `paymentRecords` 中实时计算。

#### 2.9.6 凭证生成

业务线索面板（LedgerTable）新增"生成凭证"按钮：

```
业务线索 → 点击"生成凭证" → genVoucher(type, source, amount)
  → 创建草稿凭证（DRAFT 状态）
  → 追加到 vouchers 数组开头
  → 推送到会计凭证面板
```

生成的凭证包含：自动编号 PZ-{timestamp}、业务类型、来源单号、当前日期、摘要、借贷金额。

#### 2.9.7 回款预测

模块：`ReceivableForecastPanel`

基于应收款到期日自动预测未来 8 周回款：

- 按周分组（起始日到 7 天后）
- 统计每周到期笔数与金额
- 柱状图可视化（宽度比例 = 金额 / 最大金额）
- 顶部汇总：未来 8 周预计可回款总额、总笔数
- 数据源：`receivables.filter(status !== "已核销")`

#### 2.9.8 审计日志

模块：`AuditLogPanel`

自动记录所有财务操作的审计追踪：

| 操作 | 触发源 | 日志内容 |
|------|--------|----------|
| 开票 | `handleInvoice` | "XXX 已开票" |
| 回款 | `handleCollect` | "XXX 已回款登记" |
| 核销 | `handleWriteOff` | "XXX 已核销" |
| 付款申请 | `PayableList handlePay` | "付款申请已提交" |
| 付款离线 | `PayableList handlePay` fallback | "XXX 标记付款" |
| 凭证生成 | `LedgerTable genVoucher` | "PZ-XXX 服务收入" |
| 付款批准 | `PaymentApplicationsPanel` | "XXX 已批准" |
| 付款拒绝 | `PaymentApplicationsPanel` | "XXX 已拒绝" |
| 付款执行 | `PaymentApplicationsPanel` | "XXX 已付款" |
| 关账 | FinanceModule 关账按钮 | "会计期间 YYYY-MM 已关闭" |

日志存储：`auditLog` 状态数组，上限 200 条自动裁剪，面板展示最近 30 条。

#### 2.9.9 会计期间

- 自动计算当前会计期间（YYYY-MM，从 `new Date()` 获取）
- 关账状态指示（绿色"已关账" / 黄色"未关账"）
- 关账按钮 → `setPeriodClosed(true)` + `logAudit("关账", ...)`
- 关账后显示"数据已锁定"提示
- 当前为前端状态管理，未联动后端期间表

#### 2.9.10 侧边栏待批计数

财务模块侧边栏按钮右上角显示红色圆形徽章，值为 `paymentApps.filter(status === "PENDING").length`。徽章在 `paymentApps` 变化时自动更新，0 时不显示。

#### 2.9.11 概览页集成

概览页面（OverviewModule）新增"待批付款"指标卡，数据源为 `paymentApps.filter(status === "PENDING").length`，与财务模块的待批付款卡一致。

#### 2.9.12 预算节余计算

预算节余 = 所有项目预算汇总 - 所有项目成本汇总。结余率为百分比，显示在指标卡 meta 区。

#### 2.9.13 离线降级

所有 API 调用均使用 `.catch(() => [])` 或 `.catch(() => null)` 保护。后端不可用时：
- 财务数据降级到 `initialReceivables` / `initialPayables` mock 数据
- ledger / payment 数据为空数组
- 操作按钮使用本地状态更新
- 不白屏、不报错

#### 2.9.14 后端 API 清单

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/finance/overview` | 财务总览（应收/应付/逾期/现金流） |
| GET | `/api/finance/receivables` | 应收列表 |
| POST | `/api/finance/receivables/{id}/invoice` | 开票 |
| POST | `/api/finance/receivables/{id}/receipts` | 回款登记 |
| GET | `/api/finance/payables` | 应付列表 |
| GET | `/api/finance/payment-applications` | 付款申请列表 |
| POST | `/api/finance/payment-applications` | 新建付款申请 |
| POST | `/api/finance/payment-applications/{id}/approval` | 审批付款申请 |
| POST | `/api/finance/payment-applications/{id}/payment` | 执行付款 |
| GET | `/api/finance/payments` | 付款记录列表 |
| GET | `/api/finance/ledger/overview` | 总账概览 |
| GET | `/api/finance/ledger/vouchers` | 会计凭证列表 |
| GET | `/api/finance/ledger/statements` | 财务报表 |
| POST | `/api/auth/login` | JWT 登录 |


---

## 🛠 本地开发环境

### 前提

| 工具 | 版本 | 说明 |
|------|------|------|
| JDK | **17**（必须） | Spring Boot 3.4 + JDK 26 会导致 springdoc 无法启动 |
| Maven | 3.9+ | 使用 `mvn wrapper` 也可 |
| Node.js | 20+ | 前端构建 |
| Docker | 可选 | 仅生产部署需要（PostgreSQL + Redis + MinIO） |

JDK 17 安装位置：`/opt/homebrew/opt/openjdk@17`

### 启动方式

**后端**（无需 Docker，使用 H2 内存数据库）：

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
cd services/api && SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

**前端**（自动代理 `/api` → 8080）：

```bash
npm run admin:dev
# → http://localhost:5174
```

### 初始化机制

Flyway 默认扫描 `classpath:db/migration-h2`（H2 兼容版 Migration），但由于两套 Migration 版本号重叠（`V1~V32`），本地开发时 **Flyway 已禁用**。

改用 **`DataInitializer`**（`@Component implements CommandLineRunner`）在首次启动时自动：
1. 创建 90+ 项系统权限（`sys_permissions`）
2. 创建 `ADMIN` 角色并赋予全部权限
3. 创建 admin 用户（admin/Admin@123）

Hibernate `ddl-auto: update` 从 JPA 实体自动建表，无需手动执行 SQL。

如需重置数据库：

```bash
rm -f ~/.ops-erp-data/ops_erp.mv.db
```

### 生产部署（Docker）

```bash
# 1. 恢复主 Migration 文件
mv db-main-backup/* src/main/resources/db/migration/

# 2. 构建后端 JAR
mvn package -DskipTests

# 3. 启动基础设施（PostgreSQL + Redis + MinIO）
docker compose -f infra/docker-compose.yml up -d

# 4. 部署后端（使用 prod profile 连接 PostgreSQL + Flyway）
java -jar target/ops-erp-api-0.1.0.jar --spring.profiles.active=prod
```

### 验收

| 检查项 | 方法 |
|--------|------|
| 后端健康 | `curl http://localhost:8080/actuator/health` → `{"status":"UP"}` |
| 登录 | `POST /api/auth/login {"username":"admin","password":"Admin@123"}` → 返回 JWT |
| 权限 | JWT payload 中 `permissions` 应包含 90+ 项 |
| 前端访问 | http://localhost:5174 → 显示登录页面 |
