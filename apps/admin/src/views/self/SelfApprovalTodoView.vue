<template>
  <div class="page-stack self-approval-page">
    <a-card title="我的待办审批" :bordered="false" class="approval-hero-card">
      <template #extra>
        <a-space wrap class="approval-toolbar">
          <a-select
            v-model:value="riskFilter"
            allow-clear
            placeholder="风险筛选"
            :options="riskOptions"
            class="risk-filter"
          />
          <a-button :loading="loading" @click="loadData"
            ><template #icon><ReloadOutlined /></template>刷新</a-button
          >
        </a-space>
      </template>

      <a-row :gutter="[16, 16]" class="self-metric-grid">
        <a-col :xs="12" :lg="6"
          ><div class="self-metric-card">
            <span>待处理</span><strong>{{ pendingApprovals.length }}</strong>
          </div></a-col
        >
        <a-col :xs="12" :lg="6"
          ><div class="self-metric-card">
            <span>待审金额</span
            ><strong>{{ formatCompactMoney(pendingAmount) }}</strong>
          </div></a-col
        >
        <a-col :xs="12" :lg="6"
          ><div class="self-metric-card danger">
            <span>超时</span><strong>{{ overdueCount }}</strong>
          </div></a-col
        >
        <a-col :xs="12" :lg="6"
          ><div class="self-metric-card danger">
            <span>高风险</span><strong>{{ highRiskCount }}</strong>
          </div></a-col
        >
      </a-row>
    </a-card>

    <a-card class="approval-table-card">
      <template #title>待办队列</template>
      <a-table
        :columns="columns"
        :data-source="filteredApprovals"
        :loading="loading"
        :pagination="{ pageSize: 10 }"
        row-key="id"
        :scroll="{ x: 980 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'approval'">
            <strong>{{ record.code }}</strong>
            <span class="table-subtitle">{{ record.title }}</span>
          </template>
          <template v-else-if="column.key === 'type'">{{
            approvalTypeLabel(record.approvalType)
          }}</template>
          <template v-else-if="column.key === 'amount'">{{
            formatMoney(record.amount)
          }}</template>
          <template v-else-if="column.key === 'status'">
            <a-tag color="orange">待审批</a-tag>
            <a-tag :color="riskColor(riskLevel(record))">{{
              riskLabel(riskLevel(record))
            }}</a-tag>
            <span v-if="record.currentApproverName" class="table-subtitle"
              >当前：{{ record.currentApproverName }}</span
            >
          </template>
          <template v-else-if="column.key === 'date'">
            {{ record.createdAt?.slice(0, 10) || "-" }}
            <span class="table-subtitle">{{
              approvalAgeLabel(record.createdAt)
            }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button
              type="link"
              size="small"
              @click="openProcess(record, 'APPROVED')"
              >通过</a-button
            >
            <a-button
              type="link"
              size="small"
              danger
              @click="openProcess(record, 'REJECTED')"
              >驳回</a-button
            >
          </template>
        </template>
        <template #emptyText>
          <a-empty description="暂无待办审批" />
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="processOpen"
      :title="processForm.decision === 'APPROVED' ? '审批通过' : '审批驳回'"
      :confirm-loading="saving"
      @ok="handleProcess"
    >
      <a-alert
        v-if="selectedApproval"
        class="section-alert"
        type="info"
        :message="`${selectedApproval.code} · ${selectedApproval.title} · ${formatMoney(selectedApproval.amount)}`"
      />
      <a-form
        ref="processFormRef"
        :model="processForm"
        :rules="processRules"
        layout="vertical"
      >
        <a-form-item label="审批结论" name="decision">
          <a-radio-group
            v-model:value="processForm.decision"
            button-style="solid"
          >
            <a-radio-button value="APPROVED">通过</a-radio-button>
            <a-radio-button value="REJECTED">驳回</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="审批意见" name="comment"
          ><a-textarea v-model:value="processForm.comment" :rows="3"
        /></a-form-item>
        <a-form-item label="审批人" name="approverName"
          ><a-input v-model:value="processForm.approverName"
        /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { getSelfApprovals, processSelfApproval } from "@/api/hr";
import type { Approval, ApprovalType } from "@/api/office";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
const loading = ref(false);
const saving = ref(false);
const approvals = ref<Approval[]>([]);
const riskFilter = ref<string>();
const processOpen = ref(false);
const processFormRef = ref();
const selectedApproval = ref<Approval | null>(null);
const processForm = reactive({
  decision: "APPROVED" as "APPROVED" | "REJECTED",
  comment: "同意",
  approverName: "",
});
const processRules = {
  decision: [{ required: true }],
  comment: [{ required: true, message: "请填写审批意见" }],
  approverName: [{ required: true, message: "请输入审批人" }],
};
const riskOptions = [
  { label: "高风险", value: "HIGH" },
  { label: "中风险", value: "MEDIUM" },
  { label: "正常", value: "NORMAL" },
];
const columns = [
  { title: "编号 / 说明", key: "approval", width: 260 },
  { title: "类型", key: "type", width: 100 },
  { title: "申请人", dataIndex: "applicantName", width: 120 },
  { title: "金额", key: "amount", width: 130 },
  { title: "状态", key: "status", width: 190 },
  { title: "提交时间", key: "date", width: 130 },
  { title: "操作", key: "action", width: 130, fixed: "right" as const },
];

const pendingApprovals = computed(() =>
  approvals.value.filter((item) => item.status === "PENDING"),
);
const filteredApprovals = computed(() =>
  pendingApprovals.value.filter(
    (item) => !riskFilter.value || riskLevel(item) === riskFilter.value,
  ),
);
const pendingAmount = computed(() =>
  pendingApprovals.value.reduce(
    (sum, item) => sum + Number(item.amount || 0),
    0,
  ),
);
const overdueCount = computed(
  () =>
    pendingApprovals.value.filter(
      (item) => approvalAgeHours(item.createdAt) >= 48,
    ).length,
);
const highRiskCount = computed(
  () =>
    pendingApprovals.value.filter((item) => riskLevel(item) === "HIGH").length,
);

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    approvals.value = await getSelfApprovals();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "待办审批加载失败");
  } finally {
    loading.value = false;
  }
}

function openProcess(record: Approval, decision: "APPROVED" | "REJECTED") {
  selectedApproval.value = record;
  Object.assign(processForm, {
    decision,
    comment: decision === "APPROVED" ? "同意" : "",
    approverName: auth.user?.displayName || "",
  });
  processOpen.value = true;
}

async function handleProcess() {
  await processFormRef.value?.validate();
  if (!selectedApproval.value) return;
  saving.value = true;
  try {
    await processSelfApproval(selectedApproval.value.id, { ...processForm });
    processOpen.value = false;
    message.success(
      processForm.decision === "APPROVED" ? "审批已通过" : "审批已驳回",
    );
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "审批处理失败");
  } finally {
    saving.value = false;
  }
}

function approvalAgeHours(value?: string) {
  if (!value) return 0;
  return Math.max(
    0,
    Math.floor((Date.now() - new Date(value).getTime()) / 3600000),
  );
}

function approvalAgeLabel(value?: string) {
  const hours = approvalAgeHours(value);
  if (hours < 1) return "刚提交";
  if (hours < 24) return `已等待 ${hours} 小时`;
  return `已等待 ${Math.floor(hours / 24)} 天`;
}

function riskLevel(item: Approval) {
  const hours = approvalAgeHours(item.createdAt);
  if (Number(item.amount || 0) >= 100000 || hours >= 48) return "HIGH";
  if (Number(item.amount || 0) >= 30000 || hours >= 24) return "MEDIUM";
  return "NORMAL";
}

function riskLabel(value: string) {
  return (
    (
      { HIGH: "高风险", MEDIUM: "中风险", NORMAL: "正常" } as Record<
        string,
        string
      >
    )[value] || value
  );
}
function riskColor(value: string) {
  return (
    (
      { HIGH: "red", MEDIUM: "orange", NORMAL: "green" } as Record<
        string,
        string
      >
    )[value] || "default"
  );
}
function approvalTypeLabel(value: ApprovalType) {
  return (
    (
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
    )[value] || value
  );
}
function formatMoney(value: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    minimumFractionDigits: 2,
  }).format(value || 0);
}
function formatCompactMoney(value: number) {
  if (Math.abs(value) >= 10000) return `${(value / 10000).toFixed(1)}万`;
  return new Intl.NumberFormat("zh-CN", { maximumFractionDigits: 0 }).format(
    value || 0,
  );
}
</script>

<style scoped>
.self-approval-page {
  max-width: 1200px;
}

.approval-hero-card {
  background: #f8fafc;
}
.approval-table-card {
  margin-top: 0;
}

.risk-filter {
  width: 130px;
}

@media (max-width: 640px) {
  .approval-toolbar,
  .approval-toolbar :deep(.ant-space-item),
  .approval-toolbar :deep(.ant-btn),
  .risk-filter {
    width: 100%;
  }
}
</style>
