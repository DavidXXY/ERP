<template>
  <div class="collaboration-page">
    <a-card :bordered="false" class="hero">
      <div class="hero-row">
        <div>
          <a-typography-title :level="3">跨部门协同中心</a-typography-title>
          <a-typography-text type="secondary"
            >责任到人、限时闭环、预算受控、成本可追溯，贯通销售、项目、采购、仓库、财务、人事与
            OA。</a-typography-text
          >
        </div>
        <a-space wrap>
          <a-select
            v-model:value="filters.year"
            :options="yearOptions"
            style="width: 110px"
          />
          <a-select
            v-model:value="filters.month"
            allow-clear
            placeholder="全年"
            :options="monthOptions"
            style="width: 100px"
          />
          <a-select
            v-model:value="filters.departmentId"
            allow-clear
            placeholder="全部部门"
            :options="departmentOptions"
            style="width: 150px"
          />
          <a-button @click="downloadReport">导出管理报表</a-button>
          <a-button @click="router.push('/office/approvals')"
            >统一审批中心</a-button
          >
          <a-button type="primary" :loading="loading" @click="loadData"
            >刷新</a-button
          >
        </a-space>
      </div>
      <a-row :gutter="[12, 12]" class="cards">
        <a-col v-for="card in cards" :key="card.key" :xs="12" :md="8" :xl="4">
          <div class="metric" :class="card.tone">
            <span>{{ card.title }}</span
            ><strong>{{ card.value }}</strong
            ><small>{{ card.hint }}</small>
          </div>
        </a-col>
      </a-row>
    </a-card>

    <a-card :bordered="false" title="跨部门管理驾驶舱">
      <a-row :gutter="[12, 12]">
        <a-col
          v-for="item in dashboardCards"
          :key="item.key"
          :xs="12"
          :md="6"
          :xl="3"
        >
          <a-statistic
            :title="item.title"
            :value="item.value"
            :suffix="item.suffix"
            :precision="item.precision || 0"
          />
        </a-col>
      </a-row>
    </a-card>

    <a-card :bordered="false">
      <a-tabs v-model:active-key="tab">
        <a-tab-pane key="todos" tab="统一待办">
          <a-alert
            class="section-tip"
            type="info"
            show-icon
            message="支持办理、转办、抄送、催办和期限管理；逾期任务由后台定时升级并发送站内消息。"
          />
          <a-table
            :columns="todoColumns"
            :data-source="data.todos"
            row-key="id"
            size="middle"
            :pagination="{ pageSize: 10 }"
            :scroll="{ x: 1100 }"
          >
            <template #bodyCell="{ column, record }">
              <a-tag
                v-if="column.key === 'priority'"
                :color="priorityColor(record.priority)"
                >{{ priorityLabel(record.priority) }}</a-tag
              >
              <a-tag
                v-else-if="column.key === 'status'"
                :color="record.status === 'DONE' ? 'green' : 'orange'"
                >{{ record.status === "DONE" ? "已完成" : "待处理" }}</a-tag
              >
              <span
                v-else-if="column.key === 'overdue'"
                :class="{ danger: record.overdueDays }"
                >{{
                  record.overdueDays ? `${record.overdueDays} 天` : "正常"
                }}</span
              >
              <span v-else-if="column.key === 'assignee'">{{
                userName(record.assigneeUserId)
              }}</span>
              <a-space v-else-if="column.key === 'action'">
                <a-button type="link" @click="router.push(record.link)"
                  >业务办理</a-button
                >
                <a-dropdown>
                  <a-button type="link">协同操作</a-button>
                  <template #overlay
                    ><a-menu
                      @click="({ key }: any) => openTodoAction(record, key)"
                    >
                      <a-menu-item key="COMPLETE">完成</a-menu-item
                      ><a-menu-item key="REOPEN">重新打开</a-menu-item>
                      <a-menu-item key="TRANSFER">转办</a-menu-item
                      ><a-menu-item key="CC">抄送</a-menu-item>
                      <a-menu-item key="REMIND">催办</a-menu-item
                      ><a-menu-item key="SET_DUE">设置期限</a-menu-item>
                    </a-menu></template
                  >
                </a-dropdown>
              </a-space>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="handover" tab="合同转项目交接">
          <a-alert
            class="section-tip"
            type="warning"
            show-icon
            message="范围、付款条款、验收标准、联系人、技术方案、报价摘要和至少一份附件全部齐备后，项目部门才能接收。"
          />
          <a-table
            :columns="handoverColumns"
            :data-source="data.handovers"
            row-key="id"
            size="middle"
            :scroll="{ x: 1200 }"
          >
            <template #bodyCell="{ column, record }">
              <a-tag
                v-if="column.key === 'materials'"
                :color="record.materialsComplete ? 'green' : 'red'"
                >{{
                  record.materialsComplete
                    ? "材料齐全"
                    : `缺资料（附件${record.materialCount || 0}）`
                }}</a-tag
              >
              <a-tag
                v-else-if="column.key === 'status'"
                :color="record.status === 'ACCEPTED' ? 'green' : 'orange'"
                >{{
                  record.status === "ACCEPTED"
                    ? "已接收"
                    : record.status === "SUBMITTED"
                      ? "已提交"
                      : "待完善"
                }}</a-tag
              >
              <a-space v-else-if="column.key === 'action'">
                <a-button type="link" @click="editHandover(record)"
                  >完善资料</a-button
                >
                <a-button
                  v-if="record.status !== 'ACCEPTED'"
                  type="link"
                  :disabled="!record.materialsComplete"
                  @click="confirmHandover(record)"
                  >确认接收</a-button
                >
              </a-space>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="budget" tab="预算版本与控制">
          <div class="section-actions">
            <a-button type="primary" @click="budgetOpen = true"
              >申请预算变更</a-button
            >
          </div>
          <a-alert
            class="section-tip"
            type="warning"
            show-icon
            message="采购占用与实际成本持续校验；预算调整保留版本、申请原因、审批人和生效时间。"
          />
          <a-tabs>
            <a-tab-pane key="current" tab="当前预算">
              <a-table
                :columns="budgetColumns"
                :data-source="data.budgets"
                row-key="projectId"
                size="middle"
              >
                <template #bodyCell="{ column, record }">
                  <strong v-if="moneyKeys.includes(column.key)">{{
                    money(record[column.key])
                  }}</strong>
                  <a-progress
                    v-else-if="column.key === 'usageRate'"
                    :percent="record.usageRate"
                    size="small"
                    :status="record.warning ? 'exception' : 'normal'"
                  />
                </template>
              </a-table>
            </a-tab-pane>
            <a-tab-pane key="versions" tab="变更版本">
              <a-table
                :columns="budgetVersionColumns"
                :data-source="data.budgetVersions"
                row-key="id"
                size="middle"
              >
                <template #bodyCell="{ column, record }">
                  <strong v-if="moneyKeys.includes(column.key)">{{
                    money(record[column.key])
                  }}</strong>
                  <a-tag
                    v-else-if="column.key === 'status'"
                    :color="statusColor(record.status)"
                    >{{ statusText(record.status) }}</a-tag
                  >
                  <a-space
                    v-else-if="
                      column.key === 'action' && record.status === 'PENDING'
                    "
                  >
                    <a-button
                      type="link"
                      @click="reviewBudget(record, 'APPROVED')"
                      >通过</a-button
                    >
                    <a-button
                      type="link"
                      danger
                      @click="reviewBudget(record, 'REJECTED')"
                      >驳回</a-button
                    >
                  </a-space>
                </template>
              </a-table>
            </a-tab-pane>
          </a-tabs>
        </a-tab-pane>

        <a-tab-pane key="finance" tab="合同·开票·回款">
          <a-table
            :columns="financeColumns"
            :data-source="data.financialMilestones"
            row-key="contractId"
            size="middle"
          >
            <template #bodyCell="{ column, record }">
              <strong v-if="moneyKeys.includes(column.key)">{{
                money(record[column.key])
              }}</strong>
              <a-tag
                v-else-if="column.key === 'overdue'"
                :color="record.overdue ? 'red' : 'green'"
                >{{ record.overdue ? "已逾期" : "正常" }}</a-tag
              >
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="procurement" tab="采购精细对账">
          <a-alert
            class="section-tip"
            type="info"
            show-icon
            message="按数量、金额、税率、重复发票和差异容忍值执行订单—收货—发票—应付—付款匹配。"
          />
          <a-table
            :columns="procurementColumns"
            :data-source="data.procurementReconciliation"
            row-key="orderId"
            size="middle"
            :scroll="{ x: 1600 }"
          >
            <template #bodyCell="{ column, record }">
              <strong v-if="moneyKeys.includes(column.key)">{{
                money(record[column.key])
              }}</strong>
              <a-tag
                v-else-if="column.key === 'matchStatus'"
                :color="
                  record.matchStatus === 'MATCHED'
                    ? 'green'
                    : record.matchStatus === 'PARTIAL'
                      ? 'orange'
                      : 'red'
                "
                >{{ matchText(record.matchStatus) }}</a-tag
              >
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="staff" tab="派工·工时·负荷">
          <a-tabs>
            <a-tab-pane key="assignments" tab="项目派工">
              <div class="section-actions">
                <a-button type="primary" @click="staffOpen = true"
                  >新增派工</a-button
                >
              </div>
              <a-table
                :columns="staffColumns"
                :data-source="data.staffAssignments"
                row-key="id"
                size="middle"
              >
                <template #bodyCell="{ column, record }">
                  <strong v-if="column.key === 'laborCost'">{{
                    money(record.laborCost)
                  }}</strong>
                  <a-tag
                    v-else-if="column.key === 'certificateStatus'"
                    :color="
                      record.certificateStatus === 'VALID' ? 'green' : 'orange'
                    "
                    >{{
                      record.certificateStatus === "VALID"
                        ? "证书有效"
                        : "需复核"
                    }}</a-tag
                  >
                  <a-button
                    v-else-if="column.key === 'action'"
                    type="link"
                    @click="openTimesheet(record)"
                    >填报工时</a-button
                  >
                </template>
              </a-table>
            </a-tab-pane>
            <a-tab-pane key="timesheets" tab="工时审批">
              <div class="section-actions">
                <a-button @click="periodOpen = true">月结锁定</a-button>
              </div>
              <a-table
                :columns="timesheetColumns"
                :data-source="data.timesheets"
                row-key="id"
                size="middle"
              >
                <template #bodyCell="{ column, record }">
                  <a-tag
                    v-if="column.key === 'status'"
                    :color="statusColor(record.status)"
                    >{{ statusText(record.status) }}</a-tag
                  >
                  <strong v-else-if="column.key === 'cost'">{{
                    money(record.cost)
                  }}</strong>
                  <a-space
                    v-else-if="
                      column.key === 'action' && record.status === 'SUBMITTED'
                    "
                  >
                    <a-button
                      type="link"
                      @click="reviewSheet(record, 'APPROVED')"
                      >通过</a-button
                    >
                    <a-button
                      type="link"
                      danger
                      @click="reviewSheet(record, 'REJECTED')"
                      >驳回</a-button
                    >
                  </a-space>
                </template>
              </a-table>
              <div class="lock-list">
                <a-tag
                  v-for="item in data.periodLocks"
                  :key="item.id"
                  color="red"
                  >{{ item.yearMonth }} 已锁定 · {{ item.lockedByName }}</a-tag
                >
              </div>
            </a-tab-pane>
            <a-tab-pane key="loads" tab="人员负荷">
              <a-table
                :columns="resourceColumns"
                :data-source="data.resourceLoads"
                row-key="userId"
                size="middle"
              >
                <template #bodyCell="{ column, record }">
                  <a-progress
                    v-if="column.key === 'utilization'"
                    :percent="record.utilizationRate"
                    :status="record.overloaded ? 'exception' : 'normal'"
                    size="small"
                  />
                  <a-tag
                    v-else-if="column.key === 'allocation'"
                    :color="record.overloaded ? 'red' : 'green'"
                    >{{ record.allocationPercent }}%</a-tag
                  >
                </template>
              </a-table>
            </a-tab-pane>
          </a-tabs>
        </a-tab-pane>

        <a-tab-pane key="responsibility" tab="部门责任矩阵">
          <div class="section-actions">
            <a-button type="primary" @click="responsibilityOpen = true"
              >设置项目责任</a-button
            >
          </div>
          <a-table
            :columns="responsibilityColumns"
            :data-source="data.responsibilities"
            row-key="id"
            size="middle"
          >
            <template #bodyCell="{ column, record }"
              ><span v-if="column.key === 'collaborators'">{{
                (record.collaboratorDepartmentNames || []).join("、") || "-"
              }}</span></template
            >
          </a-table>
        </a-tab-pane>

        <a-tab-pane
          v-if="data.actionLogs.length"
          key="audit"
          tab="业务操作轨迹"
        >
          <a-table
            :columns="auditColumns"
            :data-source="data.actionLogs"
            row-key="id"
            size="middle"
            :pagination="{ pageSize: 15 }"
          />
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-modal
      v-model:open="todoOpen"
      title="协同待办操作"
      ok-text="确认"
      @ok="submitTodoAction"
    >
      <a-form layout="vertical">
        <a-form-item label="操作"
          ><a-tag>{{ actionText(todoForm.action) }}</a-tag></a-form-item
        >
        <a-form-item
          v-if="todoForm.action === 'TRANSFER'"
          label="转办给"
          required
          ><a-select
            v-model:value="todoForm.targetUserId"
            :options="userOptions"
        /></a-form-item>
        <a-form-item v-if="todoForm.action === 'CC'" label="抄送人" required
          ><a-select
            v-model:value="todoForm.ccUserIds"
            mode="multiple"
            :options="userOptions"
        /></a-form-item>
        <a-form-item
          v-if="todoForm.action === 'SET_DUE'"
          label="完成期限"
          required
          ><a-date-picker
            v-model:value="todoForm.dueDate"
            value-format="YYYY-MM-DD"
        /></a-form-item>
        <a-form-item label="处理意见"
          ><a-textarea v-model:value="todoForm.comment" :rows="3"
        /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="handoverOpen"
      title="完善合同转项目交接材料"
      width="760px"
      ok-text="保存并提交"
      @ok="submitHandover"
    >
      <a-form layout="vertical">
        <a-row :gutter="12"
          ><a-col :span="12"
            ><a-form-item label="项目"
              ><a-input
                :value="handoverForm.projectName"
                disabled /></a-form-item></a-col
          ><a-col :span="12"
            ><a-form-item label="合同"
              ><a-input
                :value="handoverForm.contractCode"
                disabled /></a-form-item></a-col
        ></a-row>
        <a-form-item label="交付范围" required
          ><a-textarea v-model:value="handoverForm.scopeSummary"
        /></a-form-item>
        <a-row :gutter="12"
          ><a-col :span="12"
            ><a-form-item label="付款条款" required
              ><a-textarea
                v-model:value="
                  handoverForm.paymentTerms
                " /></a-form-item></a-col
          ><a-col :span="12"
            ><a-form-item label="验收标准" required
              ><a-textarea
                v-model:value="
                  handoverForm.acceptanceCriteria
                " /></a-form-item></a-col
        ></a-row>
        <a-row :gutter="12"
          ><a-col :span="12"
            ><a-form-item label="客户联系人" required
              ><a-textarea
                v-model:value="
                  handoverForm.customerContact
                " /></a-form-item></a-col
          ><a-col :span="12"
            ><a-form-item label="风险说明"
              ><a-textarea
                v-model:value="handoverForm.riskNotes" /></a-form-item></a-col
        ></a-row>
        <a-form-item label="技术方案摘要" required
          ><a-textarea v-model:value="handoverForm.technicalSolution"
        /></a-form-item>
        <a-form-item label="报价清单摘要" required
          ><a-textarea v-model:value="handoverForm.quotationSummary"
        /></a-form-item>
        <a-form-item label="合同/方案附件" required>
          <a-upload :show-upload-list="false" :before-upload="uploadMaterial"
            ><a-button>上传附件</a-button></a-upload
          >
          <span class="upload-hint">
            已上传 {{ handoverForm.materialCount || 0 }} 份</span
          >
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="staffOpen"
      title="项目人员派工"
      ok-text="提交派工"
      @ok="submitStaff"
    >
      <a-form layout="vertical">
        <a-form-item label="项目" required
          ><a-select
            v-model:value="staffForm.projectId"
            show-search
            option-filter-prop="label"
            :options="projectOptions"
        /></a-form-item>
        <a-form-item label="人员" required
          ><a-select
            v-model:value="staffForm.userId"
            show-search
            option-filter-prop="label"
            :options="userOptions"
        /></a-form-item>
        <a-row :gutter="12"
          ><a-col :span="12"
            ><a-form-item label="项目角色" required
              ><a-input
                v-model:value="staffForm.roleName" /></a-form-item></a-col
          ><a-col :span="12"
            ><a-form-item label="投入比例" required
              ><a-input-number
                v-model:value="staffForm.allocationPercent"
                :min="1"
                :max="100"
                style="width: 100%"
                addon-after="%" /></a-form-item></a-col
        ></a-row>
        <a-row :gutter="12"
          ><a-col :span="12"
            ><a-form-item label="计划工时" required
              ><a-input-number
                v-model:value="staffForm.plannedHours"
                :min="1"
                style="width: 100%" /></a-form-item></a-col
          ><a-col :span="12"
            ><a-form-item label="小时成本"
              ><a-input-number
                v-model:value="staffForm.hourlyCost"
                :min="0"
                style="width: 100%"
                addon-after="元" /></a-form-item></a-col
        ></a-row>
        <a-row :gutter="12"
          ><a-col :span="12"
            ><a-form-item label="开始日期" required
              ><a-date-picker
                v-model:value="staffForm.startDate"
                value-format="YYYY-MM-DD"
                style="width: 100%" /></a-form-item></a-col
          ><a-col :span="12"
            ><a-form-item label="结束日期" required
              ><a-date-picker
                v-model:value="staffForm.endDate"
                value-format="YYYY-MM-DD"
                style="width: 100%" /></a-form-item></a-col
        ></a-row>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="timesheetOpen"
      title="填报项目工时"
      ok-text="提交审批"
      @ok="submitSheet"
    >
      <a-form layout="vertical">
        <a-form-item label="派工任务"
          ><a-input :value="timesheetForm.label" disabled
        /></a-form-item>
        <a-row :gutter="12"
          ><a-col :span="12"
            ><a-form-item label="工作日期" required
              ><a-date-picker
                v-model:value="timesheetForm.workDate"
                value-format="YYYY-MM-DD"
                style="width: 100%" /></a-form-item></a-col
          ><a-col :span="12"
            ><a-form-item label="工时" required
              ><a-input-number
                v-model:value="timesheetForm.hours"
                :min="0.25"
                :max="8"
                :step="0.25"
                style="width: 100%" /></a-form-item></a-col
        ></a-row>
        <a-form-item label="工作内容" required
          ><a-textarea v-model:value="timesheetForm.description"
        /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="budgetOpen"
      title="申请预算变更"
      ok-text="提交审批"
      @ok="submitBudget"
    >
      <a-form layout="vertical"
        ><a-form-item label="项目" required
          ><a-select
            v-model:value="budgetForm.projectId"
            :options="projectOptions"
        /></a-form-item>
        <a-form-item label="调整后预算" required
          ><a-input-number
            v-model:value="budgetForm.requestedAmount"
            :min="1"
            style="width: 100%"
            addon-after="元"
        /></a-form-item>
        <a-form-item label="变更原因" required
          ><a-textarea v-model:value="budgetForm.reason" /></a-form-item
      ></a-form>
    </a-modal>

    <a-modal
      v-model:open="periodOpen"
      title="工时月结锁定"
      ok-text="确认锁定"
      @ok="submitPeriodLock"
    >
      <a-form layout="vertical"
        ><a-form-item label="月份" required
          ><a-month-picker
            v-model:value="periodForm.yearMonth"
            value-format="YYYY-MM"
            style="width: 100%" /></a-form-item
        ><a-form-item label="锁定原因" required
          ><a-textarea v-model:value="periodForm.reason" /></a-form-item
      ></a-form>
    </a-modal>

    <a-modal
      v-model:open="responsibilityOpen"
      title="设置项目责任矩阵"
      ok-text="保存"
      @ok="submitResponsibility"
    >
      <a-form layout="vertical"
        ><a-form-item label="项目" required
          ><a-select
            v-model:value="responsibilityForm.sourceId"
            :options="projectOptions"
        /></a-form-item>
        <a-form-item label="主责人"
          ><a-select
            v-model:value="responsibilityForm.ownerUserId"
            allow-clear
            :options="userOptions"
        /></a-form-item>
        <a-form-item label="主责部门"
          ><a-select
            v-model:value="responsibilityForm.departmentId"
            allow-clear
            :options="departmentOptions"
        /></a-form-item>
        <a-form-item label="协作部门"
          ><a-select
            v-model:value="responsibilityForm.collaboratorDepartmentIds"
            mode="multiple"
            :options="departmentOptions" /></a-form-item
      ></a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { message, Modal } from "ant-design-vue";
import { useRouter } from "vue-router";
import {
  acceptProjectHandover,
  actOnCollaborationTodo,
  assignProjectStaff,
  exportCollaboration,
  getCollaborationOverview,
  getCollaborationReferences,
  lockTimesheetPeriod,
  requestBudgetChange,
  reviewBudgetChange,
  reviewTimesheet,
  saveProjectHandover,
  saveResponsibility,
  submitTimesheet,
  uploadHandoverMaterial,
  type CollaborationOverview,
  type CollaborationReferences,
} from "@/api/collaboration";

const router = useRouter(),
  loading = ref(false),
  tab = ref("todos");
const now = new Date(),
  filters = reactive({
    year: now.getFullYear(),
    month: undefined as number | undefined,
    departmentId: undefined as string | undefined,
  });
const blank: CollaborationOverview = {
  cards: {},
  todos: [],
  handovers: [],
  budgets: [],
  financialMilestones: [],
  procurementReconciliation: [],
  staffAssignments: [],
  responsibilities: [],
  timesheets: [],
  resourceLoads: [],
  budgetVersions: [],
  periodLocks: [],
  actionLogs: [],
  managementDashboard: {},
};
const data = reactive<CollaborationOverview>({ ...blank }),
  references = reactive<CollaborationReferences>({
    users: [],
    departments: [],
    projects: [],
  });
const todoOpen = ref(false),
  handoverOpen = ref(false),
  staffOpen = ref(false),
  timesheetOpen = ref(false),
  budgetOpen = ref(false),
  periodOpen = ref(false),
  responsibilityOpen = ref(false);
const todoRecord = ref<any>(),
  todoForm = reactive<any>({
    action: "",
    targetUserId: undefined,
    ccUserIds: [],
    dueDate: "",
    comment: "",
  });
const handoverForm = reactive<any>({}),
  staffForm = reactive<any>({
    projectId: undefined,
    userId: undefined,
    roleName: "项目成员",
    plannedHours: 8,
    hourlyCost: 0,
    allocationPercent: 100,
    startDate: "",
    endDate: "",
  });
const timesheetForm = reactive<any>({
    assignmentId: undefined,
    label: "",
    workDate: "",
    hours: 8,
    description: "",
  }),
  budgetForm = reactive<any>({
    projectId: undefined,
    requestedAmount: 0,
    reason: "",
  });
const periodForm = reactive<any>({ yearMonth: "", reason: "月度人工成本结账" }),
  responsibilityForm = reactive<any>({
    sourceId: undefined,
    ownerUserId: undefined,
    departmentId: undefined,
    collaboratorDepartmentIds: [],
  });

const yearOptions = Array.from({ length: 7 }, (_, i) => ({
    label: `${now.getFullYear() - 3 + i}年`,
    value: now.getFullYear() - 3 + i,
  })),
  monthOptions = Array.from({ length: 12 }, (_, i) => ({
    label: `${i + 1}月`,
    value: i + 1,
  }));
const projectOptions = computed(() =>
    references.projects.map((x) => ({
      label: `${x.code} · ${x.name}`,
      value: x.id,
    })),
  ),
  userOptions = computed(() =>
    references.users.map((x) => ({ label: x.name, value: x.id })),
  ),
  departmentOptions = computed(() =>
    references.departments.map((x) => ({ label: x.name, value: x.id })),
  );
const cards = computed(() => [
  {
    key: "pendingTodos",
    title: "跨部门待办",
    value: data.cards.pendingTodos || 0,
    hint: "统一处理队列",
    tone: "blue",
  },
  {
    key: "pendingHandovers",
    title: "待交接项目",
    value: data.cards.pendingHandovers || 0,
    hint: "销售 → 项目",
    tone: "orange",
  },
  {
    key: "budgetWarnings",
    title: "预算预警",
    value: data.cards.budgetWarnings || 0,
    hint: "使用率 ≥ 90%",
    tone: "red",
  },
  {
    key: "reconciliationExceptions",
    title: "采购差异",
    value: data.cards.reconciliationExceptions || 0,
    hint: "数量·金额·税率",
    tone: "purple",
  },
  {
    key: "overdueReceivables",
    title: "逾期应收",
    value: data.cards.overdueReceivables || 0,
    hint: "财务联动销售",
    tone: "red",
  },
  {
    key: "staffAssignments",
    title: "项目派工",
    value: data.cards.staffAssignments || 0,
    hint: "人员与人工成本",
    tone: "green",
  },
]);
const dashboardCards = computed(() => {
  const d = data.managementDashboard || {};
  return [
    {
      key: "todo",
      title: "待办完成率",
      value: d.todoCompletionRate || 0,
      suffix: "%",
    },
    {
      key: "overdue",
      title: "待办逾期率",
      value: d.todoOverdueRate || 0,
      suffix: "%",
    },
    {
      key: "approval",
      title: "平均审批耗时",
      value: d.averageApprovalHours || 0,
      suffix: "小时",
      precision: 1,
    },
    {
      key: "handover",
      title: "平均交接耗时",
      value: d.averageHandoverHours || 0,
      suffix: "小时",
      precision: 1,
    },
    {
      key: "budget",
      title: "预算使用率",
      value: d.budgetUsageRate || 0,
      suffix: "%",
    },
    {
      key: "collection",
      title: "回款达成率",
      value: d.collectionRate || 0,
      suffix: "%",
    },
    {
      key: "match",
      title: "采购匹配率",
      value: d.procurementMatchRate || 0,
      suffix: "%",
    },
    {
      key: "ontime",
      title: "供应商准时率",
      value: d.supplierOnTimeRate || 0,
      suffix: "%",
    },
  ];
});
const moneyKeys = [
  "budgetAmount",
  "committedAmount",
  "actualAmount",
  "laborCost",
  "remainingAmount",
  "contractAmount",
  "billedAmount",
  "receivedAmount",
  "outstandingAmount",
  "orderAmount",
  "invoiceAmount",
  "payableAmount",
  "paidAmount",
  "previousAmount",
  "requestedAmount",
  "differenceAmount",
];
const todoColumns = [
  { title: "事项", dataIndex: "title", width: 230 },
  { title: "说明", dataIndex: "detail", width: 210 },
  { title: "处理人", key: "assignee", width: 110 },
  { title: "状态", key: "status", width: 90 },
  { title: "优先级", key: "priority", width: 90 },
  { title: "超时", key: "overdue", width: 85 },
  { title: "催办", dataIndex: "reminderCount", width: 70 },
  { title: "操作", key: "action", width: 190 },
];
const handoverColumns = [
  { title: "项目", dataIndex: "projectName", width: 220 },
  { title: "合同", dataIndex: "contractCode", width: 160 },
  { title: "交付范围", dataIndex: "scopeSummary", width: 280, ellipsis: true },
  { title: "材料", key: "materials", width: 150 },
  { title: "状态", key: "status", width: 90 },
  { title: "操作", key: "action", width: 180 },
];
const budgetColumns = [
  { title: "项目", dataIndex: "projectName" },
  { title: "预算", key: "budgetAmount" },
  { title: "已占用", key: "committedAmount" },
  { title: "已发生", key: "actualAmount" },
  { title: "人工成本", key: "laborCost" },
  { title: "剩余", key: "remainingAmount" },
  { title: "使用率", key: "usageRate", width: 160 },
];
const budgetVersionColumns = [
  { title: "项目", dataIndex: "projectName" },
  { title: "版本", dataIndex: "versionNo", width: 70 },
  { title: "原预算", key: "previousAmount" },
  { title: "新预算", key: "requestedAmount" },
  { title: "差额", key: "differenceAmount" },
  { title: "原因", dataIndex: "reason" },
  { title: "申请人", dataIndex: "requestedByName" },
  { title: "状态", key: "status" },
  { title: "操作", key: "action" },
];
const financeColumns = [
  { title: "合同", dataIndex: "contractCode" },
  { title: "客户", dataIndex: "customerName" },
  { title: "合同额", key: "contractAmount" },
  { title: "已开票", key: "billedAmount" },
  { title: "已回款", key: "receivedAmount" },
  { title: "待收", key: "outstandingAmount" },
  { title: "下次到期", dataIndex: "nextDueDate" },
  { title: "状态", key: "overdue" },
];
const procurementColumns = [
  { title: "订单", dataIndex: "orderCode", width: 180 },
  { title: "供应商", dataIndex: "supplierName", width: 160 },
  {
    title: "订单/收货数量",
    customRender: ({ record }: any) =>
      `${record.orderedQty}/${record.receivedQty}`,
    width: 130,
  },
  { title: "订单额", key: "orderAmount" },
  { title: "收货额", key: "receivedAmount" },
  { title: "发票", key: "invoiceAmount" },
  { title: "应付", key: "payableAmount" },
  { title: "已付", key: "paidAmount" },
  { title: "待付", key: "outstandingAmount" },
  { title: "容差", key: "toleranceAmount" },
  { title: "差异原因", dataIndex: "differenceReason", width: 220 },
  { title: "匹配", key: "matchStatus", width: 100 },
];
const staffColumns = [
  { title: "项目", dataIndex: "projectName" },
  { title: "员工", dataIndex: "userName" },
  { title: "部门", dataIndex: "department" },
  { title: "角色", dataIndex: "roleName" },
  {
    title: "投入",
    customRender: ({ record }: any) => `${record.allocationPercent}%`,
  },
  {
    title: "计划/实际工时",
    customRender: ({ record }: any) =>
      `${record.plannedHours}/${record.actualHours}`,
  },
  { title: "人工成本", key: "laborCost" },
  { title: "证书", key: "certificateStatus" },
  { title: "操作", key: "action" },
];
const timesheetColumns = [
  { title: "日期", dataIndex: "workDate" },
  { title: "项目", dataIndex: "projectName" },
  { title: "员工", dataIndex: "userName" },
  { title: "工时", dataIndex: "hours" },
  { title: "工作内容", dataIndex: "description" },
  { title: "成本", key: "cost" },
  { title: "状态", key: "status" },
  { title: "审批人", dataIndex: "reviewedByName" },
  { title: "操作", key: "action" },
];
const resourceColumns = [
  { title: "员工", dataIndex: "userName" },
  { title: "部门", dataIndex: "department" },
  { title: "计划工时", dataIndex: "plannedHours" },
  { title: "已确认工时", dataIndex: "actualHours" },
  { title: "标准产能", dataIndex: "capacityHours" },
  { title: "负荷率", key: "utilization", width: 180 },
  { title: "项目投入合计", key: "allocation" },
];
const responsibilityColumns = [
  { title: "对象类型", dataIndex: "sourceType" },
  { title: "主责人", dataIndex: "ownerName" },
  { title: "主责部门", dataIndex: "departmentName" },
  { title: "协作部门", key: "collaborators" },
];
const auditColumns = [
  { title: "时间", dataIndex: "createdAt" },
  { title: "对象", dataIndex: "sourceType" },
  { title: "动作", dataIndex: "actionType" },
  { title: "操作人", dataIndex: "operatorName" },
  { title: "意见", dataIndex: "comment" },
];

function money(v: any) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    maximumFractionDigits: 0,
  }).format(Number(v || 0));
}
function userName(id?: string) {
  return references.users.find((x) => x.id === id)?.name || "待认领";
}
function priorityColor(v: string) {
  return v === "URGENT" ? "red" : v === "HIGH" ? "orange" : "blue";
}
function priorityLabel(v: string) {
  return v === "URGENT" ? "紧急" : v === "HIGH" ? "高" : "普通";
}
function statusColor(v: string) {
  return v === "APPROVED" || v === "COMPLETED"
    ? "green"
    : v === "REJECTED"
      ? "red"
      : "orange";
}
function statusText(v: string) {
  return (
    (
      {
        PENDING: "待审批",
        SUBMITTED: "待审批",
        APPROVED: "已通过",
        REJECTED: "已驳回",
        COMPLETED: "已完成",
      } as any
    )[v] || v
  );
}
function matchText(v: string) {
  return (
    (
      {
        MATCHED: "完全匹配",
        PARTIAL: "部分匹配",
        WAITING_RECEIPT: "待收货",
        MISMATCH: "有差异",
      } as any
    )[v] || v
  );
}
function actionText(v: string) {
  return (
    (
      {
        COMPLETE: "完成",
        REOPEN: "重新打开",
        TRANSFER: "转办",
        CC: "抄送",
        REMIND: "催办",
        SET_DUE: "设置期限",
      } as any
    )[v] || v
  );
}
async function loadData() {
  loading.value = true;
  try {
    Object.assign(data, await getCollaborationOverview(filters));
    Object.assign(references, await getCollaborationReferences());
  } catch (e: any) {
    message.error(e.message || "加载失败");
  } finally {
    loading.value = false;
  }
}
async function downloadReport() {
  try {
    await exportCollaboration(filters);
  } catch (e: any) {
    message.error(e.message);
  }
}
function openTodoAction(record: any, action: string) {
  todoRecord.value = record;
  Object.assign(todoForm, {
    action,
    targetUserId: undefined,
    ccUserIds: [],
    dueDate: "",
    comment: "",
  });
  todoOpen.value = true;
}
async function submitTodoAction() {
  try {
    await actOnCollaborationTodo(
      todoRecord.value.type,
      todoRecord.value.id,
      todoForm,
    );
    todoOpen.value = false;
    message.success("待办状态已更新");
    await loadData();
  } catch (e: any) {
    message.error(e.message);
  }
}
function editHandover(record: any) {
  Object.keys(handoverForm).forEach((k) => delete handoverForm[k]);
  Object.assign(handoverForm, record);
  handoverOpen.value = true;
}
async function uploadMaterial(file: File) {
  try {
    await uploadHandoverMaterial(handoverForm.id, file);
    handoverForm.materialCount = (handoverForm.materialCount || 0) + 1;
    message.success("附件上传成功");
  } catch (e: any) {
    message.error(e.message);
  }
  return false;
}
async function submitHandover() {
  const required = [
    "scopeSummary",
    "paymentTerms",
    "acceptanceCriteria",
    "customerContact",
    "technicalSolution",
    "quotationSummary",
  ];
  if (required.some((k) => !handoverForm[k]))
    return message.warning("请补齐所有必填交接内容");
  try {
    await saveProjectHandover(handoverForm);
    handoverOpen.value = false;
    message.success("交接材料已提交");
    await loadData();
  } catch (e: any) {
    message.error(e.message);
  }
}
function confirmHandover(record: any) {
  Modal.confirm({
    title: "确认接收项目",
    content: "确认后将记录当前登录账号、接收时间及业务审计轨迹。",
    onOk: async () => {
      await acceptProjectHandover(record.id, {
        comment: "项目部门确认材料齐全并接收",
      });
      message.success("项目交接完成");
      await loadData();
    },
  });
}
async function submitStaff() {
  if (
    !staffForm.projectId ||
    !staffForm.userId ||
    !staffForm.startDate ||
    !staffForm.endDate
  )
    return message.warning("请完整填写派工信息");
  try {
    await assignProjectStaff(staffForm);
    staffOpen.value = false;
    message.success("派工成功");
    await loadData();
  } catch (e: any) {
    message.error(e.message);
  }
}
function openTimesheet(record: any) {
  Object.assign(timesheetForm, {
    assignmentId: record.id,
    label: `${record.projectName} · ${record.userName}`,
    workDate: "",
    hours: Math.min(8, Number(record.plannedHours || 8)),
    description: "",
  });
  timesheetOpen.value = true;
}
async function submitSheet() {
  if (!timesheetForm.workDate || !timesheetForm.description)
    return message.warning("请填写日期和工作内容");
  try {
    await submitTimesheet(timesheetForm);
    timesheetOpen.value = false;
    message.success("工时已提交审批");
    await loadData();
  } catch (e: any) {
    message.error(e.message);
  }
}
function reviewSheet(record: any, decision: string) {
  Modal.confirm({
    title: decision === "APPROVED" ? "通过工时" : "驳回工时",
    content: "审批结果将影响项目人工成本。",
    onOk: async () => {
      await reviewTimesheet(
        record.id,
        decision,
        decision === "APPROVED" ? "工时确认" : "请补充工作内容",
      );
      await loadData();
    },
  });
}
async function submitBudget() {
  if (
    !budgetForm.projectId ||
    !budgetForm.requestedAmount ||
    !budgetForm.reason
  )
    return message.warning("请完整填写预算变更");
  try {
    await requestBudgetChange(budgetForm);
    budgetOpen.value = false;
    message.success("预算变更已提交");
    await loadData();
  } catch (e: any) {
    message.error(e.message);
  }
}
function reviewBudget(record: any, decision: string) {
  Modal.confirm({
    title: decision === "APPROVED" ? "批准预算变更" : "驳回预算变更",
    content: `${record.projectName}：${money(record.previousAmount)} → ${money(record.requestedAmount)}`,
    onOk: async () => {
      await reviewBudgetChange(
        record.id,
        decision,
        decision === "APPROVED" ? "同意调整" : "不同意调整",
      );
      await loadData();
    },
  });
}
async function submitPeriodLock() {
  if (!periodForm.yearMonth || !periodForm.reason)
    return message.warning("请选择月份并填写原因");
  try {
    await lockTimesheetPeriod(periodForm.yearMonth, periodForm.reason);
    periodOpen.value = false;
    message.success("该月工时已锁定");
    await loadData();
  } catch (e: any) {
    message.error(e.message);
  }
}
async function submitResponsibility() {
  if (!responsibilityForm.sourceId) return message.warning("请选择项目");
  try {
    await saveResponsibility({ sourceType: "PROJECT", ...responsibilityForm });
    responsibilityOpen.value = false;
    message.success("责任矩阵已保存");
    await loadData();
  } catch (e: any) {
    message.error(e.message);
  }
}
watch(
  () => [filters.year, filters.month, filters.departmentId],
  () => loadData(),
);
onMounted(loadData);
</script>

<style scoped>
.collaboration-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.hero {
  background: linear-gradient(135deg, #f7fbff, #f8fbf8);
}
.hero-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}
.hero-row h3 {
  margin: 0 0 6px;
}
.cards {
  margin-top: 20px;
}
.metric {
  display: flex;
  min-height: 110px;
  flex-direction: column;
  padding: 16px;
  border: 1px solid #e6ebf0;
  border-radius: 10px;
  background: #fff;
}
.metric span {
  color: #687582;
}
.metric strong {
  margin: 5px 0;
  font-size: 26px;
}
.metric small {
  color: #94a0ab;
}
.metric.red strong,
.danger {
  color: #cf3f3f;
}
.metric.orange strong {
  color: #d97706;
}
.metric.green strong {
  color: #16835d;
}
.metric.purple strong {
  color: #6d5bd0;
}
.metric.blue strong {
  color: #2563a6;
}
.section-tip {
  margin-bottom: 14px;
}
.section-actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}
.lock-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
}
.upload-hint {
  margin-left: 10px;
  color: #788491;
}
@media (max-width: 900px) {
  .hero-row {
    flex-direction: column;
  }
}
</style>
