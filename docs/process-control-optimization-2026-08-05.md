# 流程控制闭环说明

本文记录截至 2026-08-05、数据库增量版本 V105 的关键业务闭环。目标是把原先依赖人工约定的流程边界下沉到后端状态校验、稳定用户 ID、权限和会计凭证，避免跳步、重复处理及同一人完成不相容操作。

## 控制矩阵

| 业务域 | 入口状态 | 允许动作 | 完成状态或结果 | 关键约束 |
| --- | --- | --- | --- | --- |
| 采购订单 | `ORDERED` | 取消 | `CANCELLED` | 已登记任何到货记录后禁止取消 |
| 采购订单 | `PARTIAL_RECEIVED` / `RECEIVED` | 关闭 | `CLOSED` | 不得存在待质检到货或未结案退换货 |
| 到货记录 | `PENDING` | 质检 | `PASSED` / `PARTIAL` / `REJECTED` | 订单必须已审批且未取消、未关闭 |
| 退换货 | `OPEN` | 登记换货、折让或索赔 | `COMPLETED` | 三种处理结果至少一项非零 |
| 维修工单 | `COMPLETED` | 客户验收 | `ACCEPTED` | 免费质保不能生成收费应收 |
| 合同 | `PENDING_APPROVAL` | 直接修订 | `PENDING_APPROVAL` | 进入后续状态后必须走合同变更 |
| 费用报销 | `APPROVED` | 付款 | `PAID` | 需要付款执行权限并生成凭证 |
| 付款申请 | 待申请/审批/付款 | 申请、审批、执行 | `PAID` | 申请人、审批人、执行人职责分离 |
| 会计期间 | `OPEN` | 正常关账 | `CLOSED` | 无关账阻断项时直接完成 |
| 会计期间 | `OPEN` | 强制关账 | `OPEN` 待复核，再到 `CLOSED` | 两名不同用户完成发起和复核 |
| 会计期间 | `CLOSED` | 反结账 | `CLOSED` 待复核，再到 `OPEN` | 两名不同用户完成发起和复核 |

## 采购到付款

采购订单的取消和关闭具有不同含义：未发生到货的已下单订单可以取消；一旦登记到货，业务事实已经发生，只能完成质检、处理退换货后关闭。主要接口为：

- `POST /api/procurement/orders/{id}/cancel`：取消尚未到货的订单。
- `POST /api/procurement/orders/{id}/close`：关闭部分收货或已收货订单。
- `POST /api/procurement/receipts/{id}/inspection`：登记合格与不合格数量；两者之和必须等于到货数量。
- `POST /api/procurement/returns/{id}/resolve`：登记换货数量、折让金额或索赔金额并结案。

合格数量完成库存和应付联动，不合格数量自动生成 `OPEN` 退换货记录。登记换货时，系统以 `RETURN:{退换货 ID}` 作为客户端请求标识，生成一条 `PENDING` 到货记录，避免重复请求产生多条换货记录；该记录仍需再次质检。存在任何 `PENDING` 到货或非 `COMPLETED` 退换货时，采购订单不能关闭。

## 维修验收到应收

维修工单按以下顺序推进：

```text
CREATED -> ASSIGNED -> IN_PROGRESS -> COMPLETED -> ACCEPTED
```

完工前必须填写服务结果。客户验收使用 `PUT /api/maintenance/work-orders/{id}/accept` 和 `maintenance:order:manage` 权限。非免费质保且计费金额大于零的工单在验收时自动生成应收，工单编码写入应收 `source_no`；系统按该来源编号检查幂等。收费工单必须关联有效客户，免费质保工单不得携带收费金额。

## 合同变更

合同仅在 `PENDING_APPROVAL` 状态允许通过常规更新接口直接修订。合同进入后续状态后，业务人员必须通过 `POST /api/crm/contracts/{id}/changes` 提交变更原因和变更内容，再由独立审批人员通过 `POST /api/crm/contract-changes/{id}/approve` 审批。该约束保留原合同与变更轨迹，避免直接覆盖已经生效或正在履约的数据。

## 费用付款与自动凭证

审批通过的费用报销通过 `POST /api/office/expenses/{id}/pay` 付款，接口要求 `finance:payment:execute` 权限。请求登记付款日期和银行流水号，系统保存付款用户 ID、姓名和时间，将状态更新为 `PAID`，并生成以下 `EXPENSE_PAYMENT` 凭证：

| 科目 | 借方 | 贷方 |
| --- | --- | --- |
| `2241 其他应付款` | 报销金额 | 0 |
| `1002 银行存款` | 0 | 报销金额 |

自动凭证仍受 `AccountingPeriodGuard` 控制，付款日期处于已关账或关账中的期间时不能入账。

## 付款职责分离

| 环节 | 权限 | 人员约束 |
| --- | --- | --- |
| 创建付款申请 | `finance:payment:apply` | 记录 `applicant_user_id` |
| 审批付款申请 | `finance:payment:approve` | 不得与申请人相同，记录 `approver_user_id` |
| 执行付款 | `finance:payment:execute` | 不得与申请人或审批人相同，记录 `payer_user_id` |

显示名称仅用于界面和审计展示，实际职责校验优先使用稳定用户 ID。生产角色不应把三项权限集中配置给同一账号。

## 会计期间双人复核

正常关账在检查项全部通过时直接完成。存在阻断项而使用强制关账时，第一次调用保存 `FORCE_CLOSE` 待复核动作、发起人、发起时间和原因；第二名用户再次调用后完成关账。反结账始终采用同样的两阶段流程，待复核动作为 `REOPEN`。发起人再次操作会被拒绝。

接口和权限：

- `POST /api/governance/periods/{year}/{month}/close`
- `POST /api/governance/periods/{year}/{month}/reopen`
- `governance:period:close`

## V105 数据变更

V105 新增以下字段：

- `oa_expense_claims`：`paid_date`、`payment_reference`、`paid_by_user_id`、`paid_by_name`、`paid_at`。
- `fin_payment_applications`：`applicant_user_id`、`approver_user_id`。
- `fin_payment_records`：`payer_user_id`。
- `biz_accounting_periods`：`pending_action`、`action_requested_by_id`、`action_requested_by`、`action_requested_at`、`action_request_reason`。

## 上线与回退

1. 上线前备份 PostgreSQL，并确认应用包中的最高版本为 V105。
2. 查询 `flyway_schema_history`，重点核对 V101 和 V105 的安装状态与 checksum。
3. 为付款申请、审批、执行分配不同用户，为期间例外操作准备至少两名具有权限的用户。
4. 在预发布环境验证采购拒收换货、维修验收应收、费用付款凭证和期间双人复核。
5. 发布后抽查付款三类用户 ID、期间待复核字段、业务状态和凭证借贷平衡。
6. 如需回退应用，先停止新业务写入并评估 V105 字段是否已产生数据；本次迁移为加列，不应在紧急回退中直接删列。

### V101 特别提示

本分支的 V101 包含旧版维修表结构兼容逻辑。若某环境已经执行过内容不同的 V101，Flyway 会报告 checksum mismatch。不要直接运行 `flyway repair`：必须先备份数据库，对比该环境的实际维修表结构和迁移差异，再选择经审核的新版本补偿迁移或确认结构等价后的 repair，并保留变更记录。后续兼容修复必须新增迁移版本，不得再次修改 V101。

## 验证基线

以下是流程实现提交时的既有交付验证记录，本次纯文档修改未重复执行全量代码测试，仅执行 Markdown、链接和差异校验：

- 后端：167 项测试通过；本地缺少 Docker 时 1 项 PostgreSQL Testcontainers 测试跳过，CI 必须确保该项 `skipped="0"`。
- 管理端：24 项测试、lint、format、typecheck 和生产构建通过。
- 移动端：4 项测试、typecheck、H5 与微信小程序构建通过。
- 浏览器：桌面视口和 `390x844` 移动视口关键流程检查通过。
