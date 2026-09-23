# 员工汇报：日报 / 周报 / 月报与工程工时确认

> 适用对象：普通员工、项目经理、业务管理员  
> 对应后端模块：`services/api/src/main/java/com/company/ops/api/modules/reporting`  
> 对应迁移：`V144__employee_reports.sql`、`V145`（日常/工程分类）、`V146`（工程工时确认）

## 1. 功能概述

员工汇报模块把日报、周报、月报沉淀到统一表 `emp_reports`，并在普通汇报之外提供“工程汇报”这一与项目强关联的类别。员工在管理端“员工自助”中填写并查看自己的汇报，管理者在“下属汇报”收件箱中查看下属和被抄送的汇报。

| 入口 | 路由 | 说明 |
| --- | --- | --- |
| 我的汇报 | `self/reports` | 员工填写并查看本人汇报 |
| 下属汇报 | `self/reports/inbox` | 查看直属/下级部门员工汇报，以及抄送给本人的汇报 |

汇报类型（`report_type`）为 `DAILY`（日报）、`WEEKLY`（周报）、`MONTHLY`（月报）；汇报类别（`report_category`）为 `DAILY`（日常汇报）或 `ENGINEERING`（工程汇报）。

## 2. 日常汇报

- 当前登录账号必须已关联员工档案（`qual_employees.system_user`），否则拒绝填写并提示联系管理员关联账号。
- 汇报日期不能晚于当天。
- 正文 `content` 必填（数据库约束 `length(content) > 0`）；可另填进展摘要 `progress_summary`、下步计划 `plan_next` 和问题 `issue`。
- 可抄送其他系统用户（`cc_user_ids` 保存用户 ID JSON 数组，`cc_names` 保存姓名快照）。提交后给每位抄送人写入一条 `REPORT` 类型通知；抄送人解析不到有效用户时不产生通知。
- 日常汇报保存后即完成，不进入审批流程（`report_status` 的状态流转只对工程汇报有意义）。

## 3. 工程汇报与工时确认

工程汇报用于把现场工作按项目沉淀为可核算的工时，规则如下：

1. **必须选择项目**：只能选择本人已加入的项目。“已加入”以项目成员派工记录（`ProjectStaffAssignment`）中是否存在当前账号为准；已关闭或已取消的项目不出现在可选列表中（`GET /api/reports/my-projects`）。
2. **必须填写工时**：最小半日（4 小时），最大全日（8 小时）。
3. **第二个项目**：仅当工时为半日（4 小时）时可另选一个不同的项目；两个项目不能相同。
4. **当日容量校验**：当日已提交/已通过工时加上本次工时不得超过 8 小时，超限时拒绝提交并提示已累计工时。
5. **提交即生成确认审批**：提交后自动创建业务类型为 `ENGINEERING_REPORT_CONFIRM` 的审批单（并行、单节点、`ANY_APPROVE`），汇报状态置为 `PENDING_CONFIRM`。
6. **审批人解析**：优先使用项目负责人对应的启用用户；无法解析时回退到 `PROJECT_MANAGER`、再回退 `PROJECT_DIRECTOR` 角色；两者都不存在时提交失败并提示检查项目负责人或项目经理角色配置。
7. **审批结果回写**：审批通过后汇报状态置为 `CONFIRMED`，并按派工记录把工时均摊写入项目工时表 `biz_project_timesheets`（`status=APPROVED`），同时回填 `source_report_id`/`source_approval_id` 防止重复计入，并触发派工记录重算。审批驳回时状态置为 `REJECTED`，不计入工时。

## 4. 下属汇报收件箱

`GET /api/reports/received` 合并两类数据并按汇报日期、创建时间倒序返回：

- 当前用户所在组织及其所有下级组织（递归）中，已关联系统账号的员工汇报（不含本人）。
- 明确抄送给当前用户的汇报。

支持按汇报类型（`type`）和日期区间（`fromDate`/`toDate`）过滤。同一汇报同时满足两类条件时只返回一次。

## 5. 权限

| 权限 | 说明 | 默认授予角色 |
| --- | --- | --- |
| `report:view` | 查看汇报 | 全部内置业务角色 |
| `report:create` | 填写汇报 | 全部内置业务角色 |
| `report:approve` | 工程汇报工时确认 | `ADMIN`、`PROJECT_MANAGER`、`PROJECT_DIRECTOR` |

## 6. 接口速查

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/reports/my` | 本人汇报列表 |
| `POST` | `/api/reports` | 新建汇报（日常或工程） |
| `GET` | `/api/reports/my-projects` | 本人已加入且未关闭/取消的项目 |
| `GET` | `/api/reports/cc-candidates` | 可抄送用户候选（未关联档案的用户带提示） |
| `GET` | `/api/reports/received` | 下属与抄送汇报收件箱 |

前端 API 客户端位于 `apps/admin/src/api/reporting.ts`。
