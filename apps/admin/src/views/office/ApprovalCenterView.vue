<template>
  <div class="page-stack">
    <a-card v-if="!drawerOnly">
      <template #title>审批处理</template>
      <template #extra>
        <a-space>
          <a-button v-if="!embedded" @click="goBack">返回办公室</a-button>
          <a-button :loading="loading" @click="loadData"
            ><template #icon><ReloadOutlined /></template>刷新</a-button
          >
        </a-space>
      </template>

      <a-space wrap class="table-toolbar">
        <a-radio-group
          v-model:value="sourceFilter"
          button-style="solid"
          size="small"
        >
          <a-radio-button value="all"
            >全部 ({{ mergedList.length }})</a-radio-button
          >
          <a-radio-button value="office"
            >办公室 ({{ officeCount }})</a-radio-button
          >
          <a-radio-button value="quote"
            >报价审批 ({{ quoteCount }})</a-radio-button
          >
          <a-radio-button value="contract"
            >合同审批 ({{ contractCount }})</a-radio-button
          >
          <a-radio-button value="change"
            >合同变更 ({{ changeCount }})</a-radio-button
          >
        </a-radio-group>
        <a-select
          v-model:value="slaFilter"
          allow-clear
          placeholder="处理时效"
          :options="slaOptions"
          style="width: 150px"
        />
        <a-select
          v-model:value="riskFilter"
          allow-clear
          placeholder="审批风险"
          :options="riskOptions"
          style="width: 150px"
        />
        <a-button
          v-if="auth.can('office:approval:create')"
          type="primary"
          @click="openApprovalCreate"
          ><template #icon><PlusOutlined /></template>发起审批</a-button
        >
      </a-space>

      <section class="approval-health-panel">
        <button
          v-for="card in healthCards"
          :key="card.key"
          class="health-card"
          type="button"
          @click="card.action"
        >
          <span>{{ card.label }}</span>
          <strong :class="{ 'text-danger': card.danger }">{{
            card.value
          }}</strong>
          <small>{{ card.hint }}</small>
        </button>
      </section>

      <a-table
        :columns="mergedColumns"
        :data-source="filteredList"
        :loading="loading"
        :pagination="{ pageSize: 10 }"
        :row-key="(r: any) => r._key"
        :scroll="{ x: 1350 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'source'">
            <a-tag
              :color="
                record._source === 'office'
                  ? 'blue'
                  : record._source === 'quote'
                    ? 'purple'
                    : 'orange'
              "
            >
              {{
                record._source === "office"
                  ? "办公室"
                  : record._source === "quote"
                    ? "报价审批"
                    : "合同变更"
              }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'approval'">
            <strong>{{ record.code }}</strong>
            <span class="table-subtitle">{{
              record.title || record.customerName || record.desc
            }}</span>
          </template>
          <template v-else-if="column.key === 'detail'">
            <div class="approval-detail-list">
              <span v-for="line in approvalDetailLines(record)" :key="line">{{
                line
              }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'type'">{{
            record._type
          }}</template>
          <template v-else-if="column.key === 'applicant'">{{
            record.applicantName || record.requestedBy || "-"
          }}</template>
          <template v-else-if="column.key === 'amount'">{{
            formatMoney(record.amount)
          }}</template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record._statusColor">{{
              record._statusLabel
            }}</a-tag>
            <a-tag v-if="record._slaLevel === 'OVERDUE'" color="red"
              >超时</a-tag
            >
            <a-tag v-else-if="record._slaLevel === 'DUE_SOON'" color="orange"
              >临近</a-tag
            >
          </template>
          <template v-else-if="column.key === 'rule'">
            <span v-if="record.currentApproverName" class="table-subtitle"
              >当前：{{ record.currentApproverName }}</span
            >
            <span v-if="record.matchedRuleText" class="table-subtitle">{{
              record.matchedRuleText
            }}</span>
            <a-tag v-if="record.approvalConfigVersion"
              >V{{ record.approvalConfigVersion }}</a-tag
            >
            <span v-if="record.nodes?.length" class="table-subtitle">{{
              runtimeNodeSummary(record.nodes)
            }}</span>
            <span v-if="!record.currentApproverName && !record.matchedRuleText"
              >-</span
            >
          </template>
          <template v-else-if="column.key === 'date'"
            >{{ record.date?.slice(0, 10) || "-"
            }}<span class="table-subtitle">{{
              approvalAgeLabel(record)
            }}</span></template
          >
          <template v-else-if="column.key === 'action'">
            <template
              v-if="
                record._source === 'office' &&
                record.status === 'PENDING' &&
                auth.can('office:approval:process')
              "
            >
              <a-button
                type="link"
                size="small"
                @click="router.push(`/office/approvals/${record.id}`)"
                >查看</a-button
              >
              <a-button type="link" size="small" @click="openProcess(record)"
                >处理审批</a-button
              >
              <a-button type="link" size="small" @click="handleReturn(record)"
                >退回</a-button
              >
              <a-button
                type="link"
                size="small"
                @click="openRuntime(record, 'transfer')"
                >转交</a-button
              >
              <a-button
                type="link"
                size="small"
                @click="openRuntime(record, 'addSign')"
                >加签</a-button
              >
              <a-popconfirm
                title="确认撤回该审批？"
                @confirm="handleWithdraw(record)"
              >
                <a-button
                  v-if="auth.can('office:approval:create')"
                  type="link"
                  size="small"
                  danger
                  >撤回</a-button
                >
              </a-popconfirm>
            </template>
            <template v-else-if="record._source === 'office'">
              <a-button
                type="link"
                size="small"
                @click="router.push(`/office/approvals/${record.id}`)"
                >查看</a-button
              >
            </template>
            <template v-else-if="record._source === 'quote'">
              <a-button
                type="link"
                size="small"
                @click="router.push('/crm/quotes/' + record._entityId)"
                >查看报价</a-button
              >
              <a-button
                v-if="auth.can('crm:quote:approve')"
                type="link"
                size="small"
                @click="openCrmProcess(record)"
                >审批处理</a-button
              >
            </template>
            <template v-else-if="record._source === 'contract'">
              <a-button
                type="link"
                size="small"
                @click="router.push('/crm/contracts/' + record._contractId)"
                >查看合同</a-button
              >
              <a-popconfirm
                title="确认合同审批通过？"
                @confirm="handleContractApproval(record)"
              >
                <a-button
                  v-if="auth.can('crm:contract:update')"
                  type="link"
                  size="small"
                  >通过</a-button
                >
              </a-popconfirm>
            </template>
            <template v-else-if="record._source === 'change'">
              <a-button
                type="link"
                size="small"
                @click="router.push('/crm/contracts/' + record._contractId)"
                >查看合同</a-button
              >
              <span @click.stop>
                <a-popconfirm
                  title="确认通过?"
                  @confirm="handleCrmChangeAction(record, 'APPROVED')"
                >
                  <a-button type="link" size="small">通过</a-button>
                </a-popconfirm>
                <a-popconfirm
                  title="确认驳回?"
                  @confirm="handleCrmChangeAction(record, 'REJECTED')"
                >
                  <a-button type="link" size="small" danger>驳回</a-button>
                </a-popconfirm>
              </span>
            </template>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-drawer v-model:open="detailOpen" title="审批详情" width="760px">
      <template v-if="detailApproval">
        <a-space direction="vertical" class="detail-stack" size="middle">
          <a-descriptions bordered size="small" :column="2" title="审批单信息">
            <a-descriptions-item label="审批编号">{{
              detailApproval.code
            }}</a-descriptions-item>
            <a-descriptions-item label="审批类型">{{
              approvalTypeLabel(detailApproval.approvalType)
            }}</a-descriptions-item>
            <a-descriptions-item label="标题" :span="2">{{
              detailApproval.title
            }}</a-descriptions-item>
            <a-descriptions-item label="来源单号">{{
              detailApproval.sourceNo || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="金额">{{
              formatMoney(detailApproval.amount)
            }}</a-descriptions-item>
            <a-descriptions-item label="申请人">{{
              detailApproval.applicantName || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="状态"
              ><a-tag :color="approvalStatusColor(detailApproval.status)">{{
                approvalStatusLabel(detailApproval.status)
              }}</a-tag></a-descriptions-item
            >
            <a-descriptions-item label="部门/业务"
              >{{ detailApproval.departmentName || "-" }} /
              {{ detailApproval.businessType || "-" }}</a-descriptions-item
            >
            <a-descriptions-item label="项目编码">{{
              detailApproval.projectCode || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="申请内容" :span="2">{{
              detailApproval.content || "-"
            }}</a-descriptions-item>
          </a-descriptions>

          <a-descriptions
            v-if="isExpenseApproval(detailApproval)"
            bordered
            size="small"
            :column="2"
            title="报销单完整内容"
          >
            <a-descriptions-item label="报销单号">{{
              expenseDetail(detailApproval)?.code ||
              detailApproval.sourceNo ||
              "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="报销人">{{
              expenseDetail(detailApproval)?.claimantName ||
              detailApproval.applicantName ||
              "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="费用类型">{{
              expenseDetail(detailApproval)
                ? expenseTypeLabel(
                    expenseDetail(detailApproval)!.expenseType as ExpenseType,
                  )
                : approvalBusinessTypeLabel(detailApproval.businessType)
            }}</a-descriptions-item>
            <a-descriptions-item label="发生日期">{{
              expenseDetail(detailApproval)?.expenseDate || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="绑定项目">{{
              expenseDetail(detailApproval)?.projectCode || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="绑定工单">{{
              expenseDetail(detailApproval)?.workOrderCode || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="报销金额">{{
              formatMoney(
                Number(
                  expenseDetail(detailApproval)?.amount ??
                    detailApproval.amount,
                ),
              )
            }}</a-descriptions-item>
            <a-descriptions-item label="报销状态">{{
              expenseDetail(detailApproval)?.status
                ? expenseStatusLabel(
                    expenseDetail(detailApproval)!.status as ExpenseStatus,
                  )
                : "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="费用说明" :span="2">{{
              expenseDetail(detailApproval)?.description ||
              detailApproval.content ||
              "-"
            }}</a-descriptions-item>
          </a-descriptions>
          <a-table
            v-if="
              isExpenseApproval(detailApproval) &&
              expenseDetail(detailApproval)?.lines?.length
            "
            size="small"
            :columns="expenseLineColumns"
            :data-source="expenseDetail(detailApproval)?.lines"
            :pagination="false"
            :row-key="(line: any) => line.id || line.lineNo"
          >
            <template #bodyCell="{ column, record: line }">
              <template v-if="column.key === 'expenseType'">{{
                expenseTypeLabel(line.expenseType as ExpenseType)
              }}</template>
              <template v-else-if="column.key === 'amount'">{{
                formatMoney(Number(line.amount || 0))
              }}</template>
              <template v-else-if="column.key === 'invoice'">{{
                line.invoiceFileName || "-"
              }}</template>
            </template>
          </a-table>

          <a-descriptions
            v-else-if="detailApproval.sourceDetail"
            bordered
            size="small"
            :column="2"
            title="业务单据内容"
          >
            <a-descriptions-item
              v-for="line in sourceDetailLines(detailApproval)"
              :key="line.label"
              :label="line.label"
              :span="line.span || 1"
              >{{ line.value }}</a-descriptions-item
            >
          </a-descriptions>

          <a-card size="small" title="审批流节点">
            <ApprovalProgressFlow
              v-if="detailApproval.nodes?.length"
              :steps="officeApprovalProgressSteps(detailApproval)"
            />
            <a-empty v-else description="暂无审批节点" />
          </a-card>

          <a-card size="small" title="处理记录">
            <a-timeline v-if="detailApproval.actions?.length">
              <a-timeline-item
                v-for="action in detailApproval.actions"
                :key="action.id"
                :color="approvalActionColor(action.decision)"
              >
                {{ approvalStatusLabel(action.decision) }} ·
                {{ action.operatorName || "-" }}
                <span class="table-subtitle"
                  >{{ action.comment || "-" }} ·
                  {{ action.createdAt?.slice(0, 16).replace("T", " ") }}</span
                >
              </a-timeline-item>
            </a-timeline>
            <a-empty v-else description="暂无处理记录" />
          </a-card>

          <a-card
            v-if="
              auth.can('office:approval:create') &&
              isCurrentApplicant(detailApproval) &&
              ['PENDING', 'REJECTED'].includes(detailApproval.status)
            "
            size="small"
            title="申请人操作"
          >
            <a-popconfirm
              v-if="detailApproval.status === 'PENDING'"
              title="确认撤回该审批？"
              @confirm="handleDetailWithdraw"
            >
              <a-button danger :loading="saving">撤回审批</a-button>
            </a-popconfirm>
            <a-button
              v-else
              type="primary"
              :loading="saving"
              @click="handleDetailResubmit"
              >按最新规则重新提交</a-button
            >
          </a-card>

          <a-card
            v-if="
              detailApproval.status === 'PENDING' &&
              auth.can('office:approval:process')
            "
            size="small"
            title="审批处理"
          >
            <a-form
              ref="detailProcessFormRef"
              :model="detailProcessForm"
              :rules="processRules"
              layout="vertical"
            >
              <a-form-item label="审批结论" name="decision"
                ><a-radio-group
                  v-model:value="detailProcessForm.decision"
                  button-style="solid"
                  ><a-radio-button value="APPROVED">通过</a-radio-button
                  ><a-radio-button value="REJECTED"
                    >驳回</a-radio-button
                  ></a-radio-group
                ></a-form-item
              >
              <a-form-item label="审批意见" name="comment"
                ><a-textarea
                  v-model:value="detailProcessForm.comment"
                  :rows="3"
              /></a-form-item>
              <a-form-item label="审批人" name="approverName"
                ><a-input v-model:value="detailProcessForm.approverName"
              /></a-form-item>
              <a-space>
                <a-button
                  type="primary"
                  :loading="saving"
                  @click="handleDetailProcess"
                  >提交审批</a-button
                >
                <a-button @click="openProcess(detailApproval)"
                  >弹窗处理</a-button
                >
              </a-space>
            </a-form>
          </a-card>
        </a-space>
      </template>
    </a-drawer>

    <!-- Office approval create modal -->
    <a-modal
      v-model:open="approvalCreateOpen"
      title="发起通用审批"
      width="680px"
      :confirm-loading="saving"
      @ok="handleApprovalCreate"
    >
      <a-form
        ref="approvalCreateFormRef"
        :model="approvalCreateForm"
        :rules="approvalCreateRules"
        layout="vertical"
      >
        <a-form-item label="标题" name="title"
          ><a-input v-model:value="approvalCreateForm.title"
        /></a-form-item>
        <a-row :gutter="16"
          ><a-col :span="12"
            ><a-form-item label="来源单号"
              ><a-input
                v-model:value="
                  approvalCreateForm.sourceNo
                " /></a-form-item></a-col
          ><a-col :span="12"
            ><a-form-item label="金额"
              ><a-input-number
                v-model:value="approvalCreateForm.amount"
                :min="0"
                :precision="2"
                class="full-input" /></a-form-item></a-col
        ></a-row>
        <a-form-item label="申请人" name="applicantName"
          ><a-input v-model:value="approvalCreateForm.applicantName"
        /></a-form-item>
        <a-row :gutter="16">
          <a-col :span="12"
            ><a-form-item label="部门/组织"
              ><a-input
                v-model:value="
                  approvalCreateForm.departmentName
                " /></a-form-item
          ></a-col>
          <a-col :span="12"
            ><a-form-item label="业务类型"
              ><a-input
                v-model:value="approvalCreateForm.businessType" /></a-form-item
          ></a-col>
          <a-col :span="12"
            ><a-form-item label="项目编码"
              ><a-input
                v-model:value="approvalCreateForm.projectCode" /></a-form-item
          ></a-col>
          <a-col :span="12"
            ><a-form-item label="客户等级"
              ><a-input
                v-model:value="approvalCreateForm.customerLevel" /></a-form-item
          ></a-col>
        </a-row>
        <a-form-item label="申请内容" name="content"
          ><a-textarea v-model:value="approvalCreateForm.content" :rows="3"
        /></a-form-item>
      </a-form>
    </a-modal>

    <!-- Office approval process modal -->
    <a-modal
      v-model:open="processOpen"
      title="处理审批"
      width="720px"
      :confirm-loading="saving"
      @ok="handleProcess"
    >
      <a-alert
        v-if="selectedApproval"
        class="section-alert"
        type="info"
        :message="`${selectedApproval.code} · ${selectedApproval.title} · ${formatMoney(selectedApproval.amount)}`"
        :description="selectedApproval.content || '未填写申请内容'"
      />
      <a-descriptions
        v-if="selectedApproval"
        size="small"
        bordered
        :column="2"
        class="approval-context"
      >
        <a-descriptions-item label="审批类型">{{
          approvalTypeLabel(selectedApproval.approvalType)
        }}</a-descriptions-item>
        <a-descriptions-item label="来源单号">{{
          selectedApproval.sourceNo || "-"
        }}</a-descriptions-item>
        <a-descriptions-item label="申请人">{{
          selectedApproval.applicantName
        }}</a-descriptions-item>
        <a-descriptions-item label="部门/业务"
          >{{ selectedApproval.departmentName || "-" }} /
          {{ selectedApproval.businessType || "-" }}</a-descriptions-item
        >
        <a-descriptions-item label="项目编码">{{
          selectedApproval.projectCode || "-"
        }}</a-descriptions-item>
        <a-descriptions-item label="客户等级">{{
          selectedApproval.customerLevel || "-"
        }}</a-descriptions-item>
      </a-descriptions>
      <a-form
        ref="processFormRef"
        :model="processForm"
        :rules="processRules"
        layout="vertical"
      >
        <a-form-item label="审批结论" name="decision"
          ><a-radio-group
            v-model:value="processForm.decision"
            button-style="solid"
            ><a-radio-button value="APPROVED">通过</a-radio-button
            ><a-radio-button value="REJECTED"
              >驳回</a-radio-button
            ></a-radio-group
          ></a-form-item
        >
        <a-form-item label="审批意见" name="comment"
          ><a-textarea v-model:value="processForm.comment" :rows="3"
        /></a-form-item>
        <a-form-item label="审批人" name="approverName"
          ><a-input v-model:value="processForm.approverName"
        /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="runtimeOpen"
      :title="runtimeAction === 'transfer' ? '审批转交' : '审批加签'"
      :confirm-loading="saving"
      @ok="handleRuntimeAction"
    >
      <a-form
        ref="runtimeFormRef"
        :model="runtimeForm"
        :rules="runtimeRules"
        layout="vertical"
      >
        <a-form-item label="处理人" name="targetUserId"
          ><a-select
            v-model:value="runtimeForm.targetUserId"
            show-search
            option-filter-prop="label"
            :options="userOptions"
        /></a-form-item>
        <a-form-item label="说明" name="comment"
          ><a-textarea v-model:value="runtimeForm.comment" :rows="3"
        /></a-form-item>
      </a-form>
    </a-modal>

    <!-- CRM quote approval modal -->
    <a-modal
      v-model:open="crmProcessOpen"
      title="报价审批"
      width="760px"
      :confirm-loading="saving"
      @ok="handleCrmProcess"
    >
      <a-alert
        v-if="selectedCrmApproval"
        class="section-alert"
        type="info"
        :message="`${selectedCrmApproval.code} · ${selectedCrmApproval.customerName} · ${formatMoney(selectedCrmApproval.amount)}`"
        :description="selectedCrmApproval.desc || '未填写服务范围'"
      />
      <a-descriptions
        v-if="selectedCrmApproval"
        size="small"
        bordered
        :column="2"
        class="approval-context"
      >
        <a-descriptions-item label="客户">{{
          selectedCrmApproval.customerName || "-"
        }}</a-descriptions-item>
        <a-descriptions-item label="报价金额">{{
          formatMoney(selectedCrmApproval.amount)
        }}</a-descriptions-item>
        <a-descriptions-item label="付款方式/节点" :span="2">{{
          selectedCrmApproval.paymentNodes || "-"
        }}</a-descriptions-item>
        <a-descriptions-item label="成本预算">{{
          selectedCrmApproval.budgetAmount != null
            ? formatMoney(selectedCrmApproval.budgetAmount)
            : "-"
        }}</a-descriptions-item>
        <a-descriptions-item label="毛利率">{{
          selectedCrmApproval.grossMarginRate != null
            ? `${(Number(selectedCrmApproval.grossMarginRate) * 100).toFixed(1)}%`
            : "-"
        }}</a-descriptions-item>
      </a-descriptions>
      <a-form
        ref="crmProcessFormRef"
        :model="crmProcessForm"
        :rules="crmProcessRules"
        layout="vertical"
      >
        <a-form-item label="审批结论" name="decision"
          ><a-radio-group
            v-model:value="crmProcessForm.decision"
            button-style="solid"
            ><a-radio-button value="APPROVED">通过</a-radio-button
            ><a-radio-button value="REJECTED"
              >驳回</a-radio-button
            ></a-radio-group
          ></a-form-item
        >
        <a-form-item label="审批意见" name="comment"
          ><a-textarea v-model:value="crmProcessForm.comment" :rows="3"
        /></a-form-item>
        <a-form-item label="审批人" name="approverName"
          ><a-input v-model:value="crmProcessForm.approverName"
        /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { message } from "ant-design-vue";
import PlusOutlined from "@ant-design/icons-vue/PlusOutlined";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import {
  addSignApproval,
  createApproval,
  getOfficeReferences,
  listApprovals,
  processApproval,
  resubmitApproval,
  returnApproval,
  transferApproval,
  withdrawApproval,
  type Approval,
  type ApprovalRuntimeNode,
  type ApprovalStatus,
  type ApprovalType,
  type Expense,
  type ExpenseStatus,
  type ExpenseType,
} from "@/api/office";
import {
  listQuotes,
  listContracts,
  listContractChanges,
  processQuoteApproval,
  approveContract,
  approveContractChange,
  rejectContractChange,
  type QuotePlan,
  type ServiceContract,
} from "@/api/crm";
import { useAuthStore } from "@/stores/auth";
import ApprovalProgressFlow, {
  type ApprovalProgressStep,
} from "@/components/ApprovalProgressFlow.vue";

const props = defineProps<{ embedded?: boolean; drawerOnly?: boolean }>();
const { embedded, drawerOnly } = props;
const emit = defineEmits<{ changed: [] }>();

const auth = useAuthStore();
const router = useRouter();
const loading = ref(false);
const saving = ref(false);
const sourceFilter = ref("all");
const slaFilter = ref<string>();
const riskFilter = ref<string>();

// Office approval data
const officeApprovals = ref<Approval[]>([]);
const users = ref<Array<{ id: string; displayName: string; enabled: boolean }>>(
  [],
);
// CRM approval data
const pendingQuotes = ref<QuotePlan[]>([]);
const pendingContracts = ref<ServiceContract[]>([]);
const pendingChanges = ref<any[]>([]);

// Modals
const approvalCreateOpen = ref(false);
const processOpen = ref(false);
const crmProcessOpen = ref(false);
const runtimeOpen = ref(false);
const detailOpen = ref(false);
const approvalCreateFormRef = ref();
const processFormRef = ref();
const crmProcessFormRef = ref();
const runtimeFormRef = ref();
const detailProcessFormRef = ref();
const selectedApproval = ref<Approval | null>(null);
const selectedCrmApproval = ref<any>(null);
const detailApproval = ref<any | null>(null);
const runtimeAction = ref<"transfer" | "addSign">("transfer");

const approvalCreateForm = reactive({
  code: "",
  approvalType: "OTHER" as ApprovalType,
  title: "",
  sourceNo: "",
  amount: 0,
  applicantName: "",
  content: "",
  departmentName: "",
  businessType: "",
  projectCode: "",
  supplierRisk: "",
  customerLevel: "",
});
const processForm = reactive({
  decision: "APPROVED" as "APPROVED" | "REJECTED",
  comment: "同意",
  approverName: "",
});
const detailProcessForm = reactive({
  decision: "APPROVED" as "APPROVED" | "REJECTED",
  comment: "同意",
  approverName: "",
});
const runtimeForm = reactive({
  targetUserId: "",
  comment: "",
  operatorName: "",
});
const crmProcessForm = reactive({
  decision: "APPROVED" as "APPROVED" | "REJECTED",
  comment: "同意",
  approverName: "",
});
const crmProcessRules = {
  decision: [{ required: true }],
  comment: [{ required: true }],
  approverName: [{ required: true }],
};
const approvalCreateRules = {
  title: [{ required: true }],
  applicantName: [{ required: true }],
  content: [{ required: true }],
};
const processRules = {
  decision: [{ required: true }],
  comment: [{ required: true }],
  approverName: [{ required: true }],
};
const runtimeRules = {
  targetUserId: [{ required: true, message: "请选择处理人" }],
  comment: [{ required: true, message: "请输入说明" }],
};
const userOptions = computed(() =>
  users.value
    .filter((item) => item.enabled)
    .map((item) => ({ label: item.displayName, value: item.id })),
);
const expenseLineColumns = [
  { title: "序号", dataIndex: "lineNo", width: 70 },
  { title: "费用类型", key: "expenseType", width: 100 },
  { title: "发生日期", dataIndex: "expenseDate", width: 110 },
  { title: "金额", key: "amount", width: 120 },
  { title: "说明", dataIndex: "description" },
  { title: "发票", key: "invoice", width: 180 },
];

// Unified approval list
const mergedList = computed(() => {
  const items: any[] = [];
  // Office approvals
  officeApprovals.value.forEach((a) => {
    items.push({
      _key: "office-" + a.id,
      _source: "office",
      _entityId: a.id,
      _type: "通用审批",
      _statusLabel: approvalStatusLabel(a.status),
      _statusColor: approvalStatusColor(a.status),
      id: a.id,
      code: a.code,
      title: a.title,
      amount: a.amount,
      content: a.content,
      approvalType: a.approvalType,
      sourceNo: a.sourceNo,
      applicantName: a.applicantName,
      status: a.status,
      date: a.createdAt,
      departmentName: a.departmentName,
      businessType: a.businessType,
      projectCode: a.projectCode,
      customerLevel: a.customerLevel,
      approverName: a.approverName,
      approvalComment: a.approvalComment,
      sourceDetail: a.sourceDetail,
      currentApproverName: a.currentApproverName,
      matchedRuleText: a.matchedRuleText,
      approvalConfigVersion: a.approvalConfigVersion,
      nodes: a.nodes || [],
      _slaLevel: slaLevel(a.createdAt, a.status === "PENDING"),
      _riskLevel: approvalRiskLevel(
        a.amount,
        a.createdAt,
        a.status === "PENDING",
      ),
    });
  });
  // CRM quote approvals
  pendingQuotes.value.forEach((q) => {
    items.push({
      _key: "quote-" + q.id,
      _source: "quote",
      _entityId: q.id,
      _type: "报价审批",
      _statusLabel: "待审批",
      _statusColor: "orange",
      id: q.id,
      code: q.code,
      title: q.customerName,
      desc: q.serviceScope,
      amount: q.amount,
      customerName: q.customerName,
      status: q.status,
      date: q.updatedAt,
      paymentNodes: q.paymentNodes,
      budgetAmount: q.budgetAmount,
      grossMarginRate: q.grossMarginRate,
      applicantName: (q as any).editorName,
      approverName: q.lastApproverName,
      approvalComment: q.lastApprovalComment,
      _slaLevel: slaLevel(q.updatedAt, true),
      _riskLevel: approvalRiskLevel(q.amount, q.updatedAt, true),
    });
  });
  // Contract approvals
  pendingContracts.value.forEach((c) => {
    items.push({
      _key: "contract-" + c.id,
      _source: "contract",
      _entityId: c.id,
      _contractId: c.id,
      _type: "合同审批",
      _statusLabel: "待审批",
      _statusColor: "orange",
      id: c.id,
      code: c.code || "-",
      title: c.projectName,
      desc: `${c.customerName || "-"} · ${c.contractType || "-"}`,
      amount: c.amount,
      customerName: c.customerName,
      status: c.status,
      date: c.startDate,
      contractType: c.contractType,
      startDate: c.startDate,
      endDate: c.endDate,
      serviceCycle: c.serviceCycle,
      _slaLevel: slaLevel(c.startDate, true),
      _riskLevel: approvalRiskLevel(c.amount, c.startDate, true),
    });
  });
  // Contract changes
  pendingChanges.value.forEach((c) => {
    items.push({
      _key: "change-" + c.id,
      _source: "change",
      _entityId: c.id,
      _contractId: c.contractId,
      _type: "合同变更",
      _statusLabel: "待审批",
      _statusColor: "orange",
      id: c.id,
      code: c.contractCode || "-",
      title: c.reason,
      desc: changeSummary(c),
      amount: changeAmount(c),
      changeData: c.changeData,
      date: c.requestedAt,
      applicantName: c.requestedBy,
      status: c.status,
      _slaLevel: slaLevel(c.requestedAt, true),
      _riskLevel: approvalRiskLevel(0, c.requestedAt, true),
    });
  });
  items.sort((a, b) => (a.date || "").localeCompare(b.date || "") * -1);
  return items;
});

const filteredList = computed(() => {
  return mergedList.value.filter((item) => {
    return (
      (sourceFilter.value === "all" || item._source === sourceFilter.value) &&
      (!slaFilter.value || item._slaLevel === slaFilter.value) &&
      (!riskFilter.value || item._riskLevel === riskFilter.value)
    );
  });
});
const officeCount = computed(
  () => mergedList.value.filter((i) => i._source === "office").length,
);
const quoteCount = computed(
  () => mergedList.value.filter((i) => i._source === "quote").length,
);
const contractCount = computed(
  () => mergedList.value.filter((i) => i._source === "contract").length,
);
const changeCount = computed(
  () => mergedList.value.filter((i) => i._source === "change").length,
);
const pendingCount = computed(
  () => mergedList.value.filter((item) => isPendingApproval(item)).length,
);
const overdueCount = computed(
  () => mergedList.value.filter((item) => item._slaLevel === "OVERDUE").length,
);
const dueSoonCount = computed(
  () => mergedList.value.filter((item) => item._slaLevel === "DUE_SOON").length,
);
const highRiskCount = computed(
  () => mergedList.value.filter((item) => item._riskLevel === "HIGH").length,
);
const largeAmount = computed(() =>
  mergedList.value
    .filter((item) => isPendingApproval(item))
    .reduce((sum, item) => sum + Number(item.amount || 0), 0),
);
const healthCards = computed(() => [
  {
    key: "pending",
    label: "待处理审批",
    value: `${pendingCount.value} 项`,
    hint: `待审金额 ${formatMoney(largeAmount.value)}`,
    danger: pendingCount.value > 0,
    action: () => {
      slaFilter.value = undefined;
      riskFilter.value = undefined;
      sourceFilter.value = "all";
    },
  },
  {
    key: "overdue",
    label: "SLA超时",
    value: `${overdueCount.value} 项`,
    hint: "超过48小时未处理",
    danger: overdueCount.value > 0,
    action: () => {
      slaFilter.value = "OVERDUE";
      riskFilter.value = undefined;
    },
  },
  {
    key: "dueSoon",
    label: "临近超时",
    value: `${dueSoonCount.value} 项`,
    hint: "24-48小时待处理",
    danger: dueSoonCount.value > 0,
    action: () => {
      slaFilter.value = "DUE_SOON";
      riskFilter.value = undefined;
    },
  },
  {
    key: "highRisk",
    label: "高风险审批",
    value: `${highRiskCount.value} 项`,
    hint: "大额或超时事项",
    danger: highRiskCount.value > 0,
    action: () => {
      riskFilter.value = "HIGH";
      slaFilter.value = undefined;
    },
  },
]);
const slaOptions = [
  { label: "SLA超时", value: "OVERDUE" },
  { label: "临近超时", value: "DUE_SOON" },
  { label: "正常", value: "NORMAL" },
];
const riskOptions = [
  { label: "高风险", value: "HIGH" },
  { label: "中风险", value: "MEDIUM" },
  { label: "正常", value: "NORMAL" },
];

const mergedColumns = [
  { title: "来源", key: "source", width: 100 },
  { title: "编号 / 说明", key: "approval", width: 240 },
  { title: "审批信息", key: "detail", width: 360 },
  { title: "类型", key: "type", width: 100 },
  { title: "申请人", key: "applicant", width: 120 },
  { title: "金额", key: "amount", width: 130 },
  { title: "状态", key: "status", width: 120 },
  { title: "规则来源", key: "rule", width: 260 },
  { title: "时间", key: "date", width: 120 },
  { title: "操作", key: "action", width: 220, fixed: "right" as const },
];
onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    const canLoadReferences =
      auth.can("office:view") ||
      auth.can("office:expense:create") ||
      auth.can("office:outsource:create");
    const [referenceData, approvalData, quoteData, contractData] =
      await Promise.all([
        canLoadReferences ? getOfficeReferences() : Promise.resolve(null),
        auth.can("office:approval:view")
          ? listApprovals()
          : Promise.resolve([]),
        auth.can("crm:quote:view") ? listQuotes() : Promise.resolve([]),
        auth.can("crm:contract:view") ? listContracts() : Promise.resolve([]),
      ]);
    users.value = referenceData?.users || [];
    officeApprovals.value = approvalData;
    const allQuotes = quoteData;
    const allContracts = contractData;
    pendingQuotes.value = allQuotes.filter(
      (q: QuotePlan) => q.status === "PENDING_APPROVAL",
    );
    pendingContracts.value = allContracts.filter(
      (c: ServiceContract) => c.status === "PENDING_APPROVAL",
    );
    // Fetch contract changes
    try {
      if (!auth.can("crm:contract:view")) {
        pendingChanges.value = [];
        return;
      }
      const contracts = allContracts;
      const allChanges: any[] = [];
      for (const c of contracts) {
        try {
          const changes = await listContractChanges(c.id);
          allChanges.push(
            ...changes
              .filter((ch: any) => ch.status === "PENDING")
              .map((ch: any) => ({
                ...ch,
                contractId: c.id,
                contractCode: c.code,
              })),
          );
        } catch {
          /* skip */
        }
      }
      pendingChanges.value = allChanges;
    } catch {
      /* contract changes supplementary */
    }
  } catch (error) {
    message.error(error instanceof Error ? error.message : "数据加载失败");
  } finally {
    loading.value = false;
  }
}

function goBack() {
  router.push("/office");
}
function openApprovalCreate() {
  Object.assign(approvalCreateForm, {
    code: generateCode("SP"),
    approvalType: "OTHER" as ApprovalType,
    title: "",
    sourceNo: "",
    amount: 0,
    applicantName: auth.user?.displayName || "",
    content: "",
    departmentName: "",
    businessType: "",
    projectCode: "",
    supplierRisk: "",
    customerLevel: "",
  });
  approvalCreateOpen.value = true;
}
async function handleApprovalCreate() {
  await approvalCreateFormRef.value?.validate();
  saving.value = true;
  try {
    await createApproval({ ...approvalCreateForm });
    approvalCreateOpen.value = false;
    message.success("审批已发起");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "审批发起失败");
  } finally {
    saving.value = false;
  }
}
function openProcess(item: any) {
  selectedApproval.value = item;
  Object.assign(processForm, {
    decision: "APPROVED",
    comment: "同意",
    approverName: auth.user?.displayName || "",
  });
  processOpen.value = true;
}
function openDetail(item: any) {
  detailApproval.value = item;
  Object.assign(detailProcessForm, {
    decision: "APPROVED",
    comment: "同意",
    approverName: auth.user?.displayName || "",
  });
  detailOpen.value = true;
}

async function openApprovalById(id: string) {
  if (!officeApprovals.value.length) await loadData();
  const target = mergedList.value.find(
    (item) => item._source === "office" && item.id === id,
  );
  if (!target) {
    message.warning("未找到对应审批事项，可能已被处理或当前账号无权限查看");
    return;
  }
  openDetail(target);
}

defineExpose({ openApprovalById, loadData });
async function handleProcess() {
  await processFormRef.value?.validate();
  if (!selectedApproval.value) return;
  saving.value = true;
  try {
    await processApproval(selectedApproval.value.id, { ...processForm });
    processOpen.value = false;
    message.success(
      processForm.decision === "APPROVED" ? "审批已通过" : "审批已驳回",
    );
    await loadData();
    emit("changed");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "审批处理失败");
  } finally {
    saving.value = false;
  }
}
async function handleDetailProcess() {
  await detailProcessFormRef.value?.validate();
  if (!detailApproval.value) return;
  saving.value = true;
  try {
    await processApproval(detailApproval.value.id, { ...detailProcessForm });
    detailOpen.value = false;
    message.success(
      detailProcessForm.decision === "APPROVED" ? "审批已通过" : "审批已驳回",
    );
    await loadData();
    emit("changed");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "审批处理失败");
  } finally {
    saving.value = false;
  }
}
function openRuntime(item: any, action: "transfer" | "addSign") {
  selectedApproval.value = item;
  runtimeAction.value = action;
  Object.assign(runtimeForm, {
    targetUserId: "",
    comment: action === "transfer" ? "审批转交" : "审批加签",
    operatorName: auth.user?.displayName || "",
  });
  runtimeOpen.value = true;
}
async function handleRuntimeAction() {
  await runtimeFormRef.value?.validate();
  if (!selectedApproval.value) return;
  saving.value = true;
  try {
    if (runtimeAction.value === "transfer")
      await transferApproval(selectedApproval.value.id, { ...runtimeForm });
    else await addSignApproval(selectedApproval.value.id, { ...runtimeForm });
    runtimeOpen.value = false;
    message.success(
      runtimeAction.value === "transfer" ? "审批已转交" : "审批已加签",
    );
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "操作失败");
  } finally {
    saving.value = false;
  }
}
async function handleWithdraw(item: any) {
  saving.value = true;
  try {
    await withdrawApproval(item.id, {
      comment: "申请人撤回",
      operatorName: auth.user?.displayName || "",
    });
    message.success("审批已撤回");
    await loadData();
    emit("changed");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "撤回失败");
  } finally {
    saving.value = false;
  }
}
function isCurrentApplicant(item: any) {
  return Boolean(
    item?.applicantName && item.applicantName === auth.user?.displayName,
  );
}
async function handleDetailWithdraw() {
  if (!detailApproval.value) return;
  await handleWithdraw(detailApproval.value);
  detailOpen.value = false;
}
async function handleDetailResubmit() {
  if (!detailApproval.value) return;
  saving.value = true;
  try {
    await resubmitApproval(detailApproval.value.id, {
      applicantName: auth.user?.displayName || "",
      comment: "审批配置已更新，按最新规则重新提交",
    });
    message.success("审批已按最新规则重新提交");
    detailOpen.value = false;
    await loadData();
    emit("changed");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "重新提交失败");
  } finally {
    saving.value = false;
  }
}
async function handleReturn(item: any) {
  saving.value = true;
  try {
    await returnApproval(item.id, {
      comment: "退回上一节点",
      operatorName: auth.user?.displayName || "",
    });
    message.success("审批已退回");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "退回失败");
  } finally {
    saving.value = false;
  }
}
function openCrmProcess(item: any) {
  selectedCrmApproval.value = item;
  Object.assign(crmProcessForm, {
    decision: "APPROVED",
    comment: "同意",
    approverName: auth.user?.displayName || "",
  });
  crmProcessOpen.value = true;
}
async function handleCrmProcess() {
  await crmProcessFormRef.value?.validate();
  if (!selectedCrmApproval.value) return;
  saving.value = true;
  try {
    await processQuoteApproval(selectedCrmApproval.value._entityId, {
      ...crmProcessForm,
    });
    crmProcessOpen.value = false;
    message.success(
      crmProcessForm.decision === "APPROVED" ? "报价审批已通过" : "报价已驳回",
    );
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "审批处理失败");
  } finally {
    saving.value = false;
  }
}
async function handleContractApproval(item: any) {
  saving.value = true;
  try {
    await approveContract(item._contractId, {
      operatorName: auth.user?.displayName || "",
      comment: "合同审批通过",
    });
    message.success("合同审批已通过");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "合同审批失败");
  } finally {
    saving.value = false;
  }
}
async function handleCrmChangeAction(item: any, decision: string) {
  saving.value = true;
  try {
    if (decision === "APPROVED")
      await approveContractChange(item._entityId, {
        operatorName: auth.user?.displayName || "",
        comment: "",
      });
    else
      await rejectContractChange(item._entityId, {
        operatorName: auth.user?.displayName || "",
        comment: "",
      });
    message.success(decision === "APPROVED" ? "变更已通过" : "变更已驳回");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "操作失败");
  } finally {
    saving.value = false;
  }
}

function approvalDetailLines(record: any) {
  if (record._source === "office") {
    return [
      `类型：${approvalTypeLabel(record.approvalType) || record._type}`,
      `内容：${record.content || "未填写"}`,
      record.sourceNo ? `来源单号：${record.sourceNo}` : "",
      record.departmentName || record.businessType
        ? `组织/业务：${record.departmentName || "-"} / ${record.businessType || "-"}`
        : "",
    ].filter(Boolean);
  }
  if (record._source === "quote") {
    return [
      `客户：${record.customerName || "-"}`,
      `服务范围：${record.desc || "-"}`,
      `付款方式：${record.paymentNodes || "-"}`,
      `预算/毛利率：${record.budgetAmount != null ? formatMoney(record.budgetAmount) : "-"} / ${record.grossMarginRate != null ? `${(Number(record.grossMarginRate) * 100).toFixed(1)}%` : "-"}`,
    ];
  }
  if (record._source === "contract") {
    return [
      `客户：${record.customerName || "-"}`,
      `合同类型：${record.contractType || "-"}`,
      `周期：${record.startDate || "-"} 至 ${record.endDate || "-"}`,
      `服务周期：${record.serviceCycle || "-"}`,
    ];
  }
  if (record._source === "change") {
    return [
      `合同：${record.code || "-"}`,
      `变更类型：${changeTypeLabel(record)}`,
      `变更内容：${changeSummary(record)}`,
      `申请原因：${record.title || "-"}`,
    ];
  }
  return ["-"];
}

function isExpenseApproval(record: any) {
  return record.approvalType === "EXPENSE";
}

function expenseDetail(record: any) {
  return record.sourceDetail as Expense | null | undefined;
}

function approvalBusinessTypeLabel(value?: string) {
  if (!value) return "-";
  return (
    (
      {
        TRAVEL: "差旅",
        TRANSPORT: "交通",
        ACCOMMODATION: "住宿",
        TOOL: "工具采购",
        OTHER: "其他",
      } as Record<string, string>
    )[value] || value
  );
}

function sourceDetailLines(record: any) {
  const detail = record.sourceDetail || {};
  return Object.entries(detail)
    .filter(
      ([, value]) => value !== null && value !== undefined && value !== "",
    )
    .map(([key, value]) => ({
      label: sourceDetailLabel(key),
      value: String(value),
      span: key === "description" || key === "acceptanceNote" ? 2 : 1,
    }));
}

function sourceDetailLabel(key: string) {
  return (
    (
      {
        code: "单号",
        claimantName: "申请人",
        supplierName: "服务商",
        projectCode: "项目",
        workOrderCode: "工单",
        serviceType: "服务类型",
        amount: "金额",
        plannedDate: "计划日期",
        status: "状态",
        description: "说明",
        acceptanceNote: "验收说明",
      } as Record<string, string>
    )[key] || key
  );
}

function parseChangeData(record: any) {
  try {
    return record.changeData ? JSON.parse(record.changeData) : {};
  } catch {
    return {};
  }
}

function changeTypeLabel(record: any) {
  const data = parseChangeData(record);
  if (data.type === "SIGNED_DOC_APPROVAL") return "双方盖章件审批";
  if (data.type === "RECEIVABLE_UPDATE") return "应收计划变更";
  return "合同信息变更";
}

function changeSummary(record: any) {
  const data = parseChangeData(record);
  if (data.type === "SIGNED_DOC_APPROVAL")
    return "审批双方盖章件，通过后合同生效并自动创建项目";
  if (data.type === "RECEIVABLE_UPDATE") {
    return (
      [
        data.amount != null
          ? `金额改为 ${formatMoney(Number(data.amount))}`
          : "",
        data.dueDate ? `到期日改为 ${data.dueDate}` : "",
        data.sourceNo ? `来源单号改为 ${data.sourceNo}` : "",
      ]
        .filter(Boolean)
        .join("；") || "应收计划信息变更"
    );
  }
  const labels: Record<string, string> = {
    projectName: "项目名称",
    contractType: "合同类型",
    amount: "合同金额",
    taxRate: "税率",
    startDate: "开始日期",
    endDate: "结束日期",
    serviceCycle: "服务周期",
  };
  return (
    Object.entries(data)
      .filter(([key]) => key !== "type")
      .map(
        ([key, value]) =>
          `${labels[key] || key}：${key === "amount" ? formatMoney(Number(value)) : value}`,
      )
      .join("；") ||
    record.changeData ||
    "-"
  );
}

function changeAmount(record: any) {
  const data = parseChangeData(record);
  return data.amount != null ? Number(data.amount) : 0;
}

function generateCode(prefix: string) {
  const d = new Date();
  const ds = `${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, "0")}${String(d.getDate()).padStart(2, "0")}`;
  return `${prefix}-${ds}-${String(d.getHours()).padStart(2, "0")}${String(d.getMinutes()).padStart(2, "0")}`;
}
function isPendingApproval(item: any) {
  return item.status === "PENDING" || item.status === "PENDING_APPROVAL";
}
function approvalAgeHours(value?: string) {
  if (!value) return 0;
  return Math.max(
    0,
    Math.floor((Date.now() - new Date(value).getTime()) / 3600000),
  );
}
function slaLevel(value?: string, pending = true) {
  if (!pending) return "NORMAL";
  const hours = approvalAgeHours(value);
  if (hours >= 48) return "OVERDUE";
  if (hours >= 24) return "DUE_SOON";
  return "NORMAL";
}
function approvalRiskLevel(amount: number, value?: string, pending = true) {
  if (!pending) return "NORMAL";
  const hours = approvalAgeHours(value);
  if (Number(amount || 0) >= 100000 || hours >= 48) return "HIGH";
  if (Number(amount || 0) >= 30000 || hours >= 24) return "MEDIUM";
  return "NORMAL";
}
function approvalAgeLabel(record: any) {
  if (!isPendingApproval(record)) return "已处理";
  const hours = approvalAgeHours(record.date);
  if (hours < 1) return "刚提交";
  if (hours < 24) return `已等待 ${hours} 小时`;
  return `已等待 ${Math.floor(hours / 24)} 天`;
}
function runtimeNodeSummary(nodes: ApprovalRuntimeNode[]) {
  return nodes
    .map(
      (node) =>
        `第${node.stepNo}步 ${node.assigneeName || "-"} ${node.nodeStatus}${node.dueAt ? " 截止" + node.dueAt.slice(0, 16).replace("T", " ") : ""}`,
    )
    .join(" / ");
}
function officeApprovalProgressSteps(item: Approval): ApprovalProgressStep[] {
  const steps: ApprovalProgressStep[] = [
    {
      key: "start",
      personName: item.applicantName || "发起人",
      title: "发起申请",
      time: item.createdAt,
      note: item.title || item.content,
      state: "done",
    },
  ];
  (item.nodes || []).forEach((node) => {
    steps.push({
      key: node.id,
      personName: node.approverName || node.assigneeName || "当前审批人",
      title:
        node.nodeStatus === "APPROVED"
          ? "已同意"
          : node.nodeStatus === "REJECTED"
            ? "已驳回"
            : node.nodeStatus === "SKIPPED"
              ? "已跳过"
              : "待审批",
      time: node.completedAt || node.dueAt,
      note: node.approvalComment || node.conditionText || node.sourceValue,
      state:
        node.nodeStatus === "APPROVED"
          ? "done"
          : node.nodeStatus === "REJECTED"
            ? "rejected"
            : node.nodeStatus === "SKIPPED"
              ? "skipped"
              : "pending",
    });
  });
  return steps;
}
function nodeStatusLabel(v: string) {
  return (
    (
      {
        PENDING: "待处理",
        APPROVED: "已通过",
        REJECTED: "已驳回",
        SKIPPED: "已跳过",
      } as Record<string, string>
    )[v] || v
  );
}
function nodeColor(v: string) {
  return (
    (
      {
        PENDING: "blue",
        APPROVED: "green",
        REJECTED: "red",
        SKIPPED: "gray",
      } as Record<string, string>
    )[v] || "blue"
  );
}
function approvalActionColor(v: ApprovalStatus) {
  return v === "APPROVED" ? "green" : v === "REJECTED" ? "red" : "blue";
}
function approvalTypeLabel(v: ApprovalType) {
  return (
    {
      QUOTE: "报价",
      CONTRACT: "合同",
      PURCHASE: "采购",
      OUTSOURCE: "外包",
      EXPENSE: "报销",
      PAYMENT: "付款",
      SEAL: "用章",
      LEAVE: "请假",
      TRAVEL: "出差",
      OTHER: "其他",
    } as Record<ApprovalType, string>
  )[v];
}
function approvalStatusLabel(v: ApprovalStatus) {
  return (
    { PENDING: "待审批", APPROVED: "已通过", REJECTED: "已驳回" } as Record<
      ApprovalStatus,
      string
    >
  )[v];
}
function approvalStatusColor(v: ApprovalStatus) {
  return (
    { PENDING: "orange", APPROVED: "green", REJECTED: "red" } as Record<
      ApprovalStatus,
      string
    >
  )[v];
}
function expenseTypeLabel(v: ExpenseType) {
  return (
    {
      TRAVEL: "差旅",
      TRANSPORT: "交通",
      ACCOMMODATION: "住宿",
      TOOL: "工具采购",
      OTHER: "其他",
    } as Record<ExpenseType, string>
  )[v];
}
function expenseStatusLabel(v: ExpenseStatus) {
  return (
    {
      PENDING_APPROVAL: "待审批",
      APPROVED: "已通过",
      REJECTED: "已驳回",
      PAID: "已付款",
    } as Record<ExpenseStatus, string>
  )[v];
}
function formatMoney(v: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    minimumFractionDigits: 2,
  }).format(v || 0);
}
</script>

<style scoped>
.approval-health-panel {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 16px;
}

.health-card {
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  padding: 12px;
  border: 1px solid #eef2f7;
  border-radius: 6px;
  background: #f8fafc;
  cursor: pointer;
  text-align: left;
}

.health-card span,
.health-card small {
  color: #667085;
  font-size: 12px;
}

.health-card strong {
  color: #101828;
  font-size: 20px;
}

.approval-detail-list {
  display: grid;
  gap: 3px;
  color: #475467;
  font-size: 12px;
  line-height: 1.45;
}

.approval-detail-list span {
  overflow-wrap: anywhere;
}

.approval-context {
  margin: 12px 0 16px;
}

.detail-stack {
  width: 100%;
}

.text-danger {
  color: #cf1322;
}

@media (max-width: 900px) {
  .approval-health-panel {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
