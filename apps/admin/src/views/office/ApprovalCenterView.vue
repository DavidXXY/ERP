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
              <a-button type="link" size="small" @click="submitReturn(record)"
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
                @confirm="submitWithdraw(record)"
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
                @confirm="submitContractApproval(record)"
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
                  @confirm="submitCrmChange(record, 'APPROVED')"
                >
                  <a-button type="link" size="small">通过</a-button>
                </a-popconfirm>
                <a-popconfirm
                  title="确认驳回?"
                  @confirm="submitCrmChange(record, 'REJECTED')"
                >
                  <a-button type="link" size="small" danger>驳回</a-button>
                </a-popconfirm>
              </span>
            </template>
          </template>
        </template>
      </a-table>
    </a-card>

    <ApprovalDetailDrawer
      v-model:open="detailOpen"
      :saving="saving"
      :approval="detailApproval"
      @process="onDetailProcess"
      @withdraw="submitDetailWithdraw"
      @resubmit="submitDetailResubmit"
      @open-process="openProcess"
    />

    <ApprovalCreateModal
      v-model:open="approvalCreateOpen"
      :saving="saving"
      @submit="submitCreateApproval"
    />

    <ApprovalProcessModal
      v-model:open="processOpen"
      :saving="saving"
      :approval="selectedApproval"
      @submit="onProcessSubmit"
    />

    <RuntimeNodeModal
      v-model:open="runtimeOpen"
      :saving="saving"
      :action="runtimeAction"
      :user-options="userOptions"
      @submit="onRuntimeSubmit"
    />

    <CrmQuoteProcessModal
      v-model:open="crmProcessOpen"
      :saving="saving"
      :approval="selectedCrmApproval"
      @submit="onCrmProcessSubmit"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { message } from "ant-design-vue";
import PlusOutlined from "@ant-design/icons-vue/PlusOutlined";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { getApproval, type Approval } from "@/api/office";
import { useAuthStore } from "@/stores/auth";
import { formatMoney } from "./approvalFormat";
import type { MergedApprovalItem, MergedQuoteItem } from "./approvalItemTypes";
import { useApprovalCenter } from "./composables/useApprovalCenter";
import {
  useApprovalActions,
  type ApprovalProcessPayload,
  type RuntimeActionPayload,
} from "./composables/useApprovalActions";
import ApprovalCreateModal from "./ApprovalCreateModal.vue";
import ApprovalProcessModal from "./ApprovalProcessModal.vue";
import RuntimeNodeModal from "./RuntimeNodeModal.vue";
import CrmQuoteProcessModal from "./CrmQuoteProcessModal.vue";
import ApprovalDetailDrawer from "./ApprovalDetailDrawer.vue";

const props = defineProps<{ embedded?: boolean; drawerOnly?: boolean }>();
const { embedded, drawerOnly } = props;
const emit = defineEmits<{ changed: [] }>();

const auth = useAuthStore();
const router = useRouter();

const sourceFilter = ref("all");
const slaFilter = ref<string>();
const riskFilter = ref<string>();

// Modal / drawer open state
const approvalCreateOpen = ref(false);
const processOpen = ref(false);
const crmProcessOpen = ref(false);
const runtimeOpen = ref(false);
const detailOpen = ref(false);

// Selected data
const selectedApproval = ref<Approval | null>(null);
const selectedCrmApproval = ref<any>(null);
const detailApproval = ref<any | null>(null);
const runtimeAction = ref<"transfer" | "addSign">("transfer");

const center = useApprovalCenter({ sourceFilter, slaFilter, riskFilter });
const {
  loading,
  mergedList,
  filteredList,
  officeCount,
  quoteCount,
  contractCount,
  changeCount,
  healthCards,
  slaOptions,
  riskOptions,
  mergedColumns,
  userOptions,
  loadData,
  approvalDetailLines,
  runtimeNodeSummary,
  approvalAgeLabel,
  officeApprovals,
} = center;

const actions = useApprovalActions({
  loadData,
  onChanged: () => emit("changed"),
  getDetailApproval: () => detailApproval.value,
  closeApprovalCreate: () => {
    approvalCreateOpen.value = false;
  },
  closeProcess: () => {
    processOpen.value = false;
  },
  closeRuntime: () => {
    runtimeOpen.value = false;
  },
  closeCrmProcess: () => {
    crmProcessOpen.value = false;
  },
  closeDetail: () => {
    detailOpen.value = false;
  },
});
const {
  saving,
  submitCreateApproval,
  submitOfficeProcess,
  submitDetailProcess,
  submitRuntime,
  submitWithdraw,
  submitDetailWithdraw,
  submitDetailResubmit,
  submitReturn,
  submitQuoteProcess,
  submitContractApproval,
  submitCrmChange,
} = actions;

onMounted(loadData);

function goBack() {
  router.push("/office");
}

function openApprovalCreate() {
  approvalCreateOpen.value = true;
}

function openProcess(item: MergedApprovalItem) {
  selectedApproval.value = item as unknown as Approval;
  processOpen.value = true;
}

function openRuntime(item: MergedApprovalItem, action: "transfer" | "addSign") {
  selectedApproval.value = item as unknown as Approval;
  runtimeAction.value = action;
  runtimeOpen.value = true;
}

function openCrmProcess(item: MergedQuoteItem) {
  selectedCrmApproval.value = item;
  crmProcessOpen.value = true;
}

function onProcessSubmit(form: ApprovalProcessPayload) {
  if (!selectedApproval.value) return;
  submitOfficeProcess(selectedApproval.value, form);
}

function onRuntimeSubmit(form: RuntimeActionPayload) {
  if (!selectedApproval.value) return;
  submitRuntime(selectedApproval.value, runtimeAction.value, form);
}

function onCrmProcessSubmit(form: ApprovalProcessPayload) {
  if (!selectedCrmApproval.value) return;
  submitQuoteProcess(selectedCrmApproval.value, form);
}

function onDetailProcess(form: ApprovalProcessPayload) {
  if (!detailApproval.value) return;
  submitDetailProcess(detailApproval.value, form);
}

async function openDetail(item: MergedApprovalItem) {
  detailApproval.value = item;
  if (item._source === "office") {
    try {
      detailApproval.value = await getApproval(item.id);
    } catch (error) {
      message.error(
        error instanceof Error ? error.message : "审批详情加载失败",
      );
      return;
    }
  }
  detailOpen.value = true;
}

async function openApprovalById(id: string) {
  if (!officeApprovals.value.length) await loadData();
  let target = mergedList.value.find(
    (item) => item._source === "office" && item.id === id,
  );
  if (!target) {
    try {
      const approval = await getApproval(id);
      officeApprovals.value = [
        approval,
        ...officeApprovals.value.filter((item) => item.id !== id),
      ];
      target = mergedList.value.find(
        (item) => item._source === "office" && item.id === id,
      );
    } catch (error) {
      message.warning(
        error instanceof Error ? error.message : "当前账号无权限查看该审批事项",
      );
      return;
    }
  }
  if (!target) {
    message.warning("未找到对应审批事项，可能已被处理或当前账号无权限查看");
    return;
  }
  openDetail(target);
}

defineExpose({ openApprovalById, loadData });
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

.text-danger {
  color: #cf1322;
}

@media (max-width: 900px) {
  .approval-health-panel {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
