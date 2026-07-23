<template>
  <div class="page-stack">
    <a-card title="应付管理">
      <template #extra>
        <a-space>
          <a-button @click="exportPayables">导出</a-button>
          <a-button :loading="loading" @click="loadData"
            ><template #icon><ReloadOutlined /></template>刷新</a-button
          >
        </a-space>
      </template>
      <a-row :gutter="[16, 16]" class="metric-row">
        <a-col :xs="12" :lg="6"
          ><a-statistic
            title="应付余额"
            :value="outstandingAmount"
            :formatter="moneyFormatter"
        /></a-col>
        <a-col :xs="12" :lg="6"
          ><a-statistic
            title="审批占用"
            :value="reservedAmount"
            :formatter="moneyFormatter"
        /></a-col>
        <a-col :xs="12" :lg="6"
          ><a-statistic
            title="可申请付款"
            :value="availableAmount"
            :formatter="moneyFormatter"
        /></a-col>
        <a-col :xs="12" :lg="6"
          ><a-statistic
            title="逾期应付"
            :value="overdueAmount"
            :formatter="moneyFormatter"
        /></a-col>
      </a-row>

      <section class="finance-action-panel">
        <div class="action-heading">
          <div>
            <h3>付款排期</h3>
            <p>按到期日、占用金额和可申请金额判断付款安排优先级。</p>
          </div>
          <a-space>
            <a-button size="small" @click="showDueSoon">7天内到期</a-button>
            <a-button size="small" @click="showOverdue">只看逾期</a-button>
          </a-space>
        </div>
        <div class="schedule-grid">
          <div
            v-for="bucket in scheduleBuckets"
            :key="bucket.key"
            class="schedule-card"
          >
            <span>{{ bucket.label }}</span>
            <strong>{{ formatMoney(bucket.amount) }}</strong>
            <small>{{ bucket.count }} 笔</small>
          </div>
          <button
            v-for="item in paymentFocus"
            :key="item.id"
            class="focus-row"
            type="button"
            @click="openApplication(item)"
          >
            <span>{{ item.supplierName }}</span>
            <strong>{{
              formatMoney(item.availableAmount || item.outstandingAmount)
            }}</strong>
            <small>{{ dueLabel(item) }}</small>
          </button>
        </div>
      </section>

      <a-space wrap class="table-toolbar">
        <a-input-search
          v-model:value="keyword"
          allow-clear
          placeholder="搜索应付单、供应商、采购单"
          style="width: 280px"
        />
        <a-select
          v-model:value="statusFilter"
          allow-clear
          placeholder="全部状态"
          :options="statusOptions"
          style="width: 140px"
        />
        <a-select
          v-model:value="scheduleFilter"
          allow-clear
          placeholder="付款排期"
          :options="scheduleOptions"
          style="width: 150px"
        />
      </a-space>
      <a-table
        :columns="columns"
        :data-source="filteredItems"
        :loading="loading"
        :pagination="{ pageSize: 8 }"
        :row-key="(item: FinancePayable) => item.id"
        :scroll="{ x: 1280 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'payable'"
            ><a @click="router.push(`/finance/payables/${record.id}`)"
              ><strong>{{ record.code }}</strong></a
            ><span class="table-subtitle"
              >采购单 {{ record.orderCode }}</span
            ></template
          >
          <template v-else-if="column.key === 'supplier'"
            ><a
              @click="
                router.push(`/procurement/suppliers/${record.supplierId}`)
              "
              >{{ record.supplierName }}</a
            ></template
          >
          <template v-else-if="column.key === 'amount'"
            ><strong>{{ formatMoney(record.amount) }}</strong
            ><span class="table-subtitle"
              >已付 {{ formatMoney(record.paidAmount) }} · 待付
              {{ formatMoney(record.outstandingAmount) }}</span
            ></template
          >
          <template v-else-if="column.key === 'available'"
            >{{ formatMoney(record.availableAmount)
            }}<span class="table-subtitle"
              >审批占用 {{ formatMoney(record.reservedAmount) }}</span
            ></template
          >
          <template v-else-if="column.key === 'schedule'">
            <a-tag :color="scheduleColor(record)">{{
              scheduleLabel(record)
            }}</a-tag>
            <span class="table-subtitle">{{ dueLabel(record) }}</span>
          </template>
          <template v-else-if="column.key === 'status'"
            ><a-tag :color="statusColor(record.status)">{{
              statusLabel(record.status)
            }}</a-tag
            ><a-tag v-if="record.overdue" color="red">逾期</a-tag></template
          >
          <template v-else-if="column.key === 'action'"
            ><a-button
              v-if="
                auth.can('finance:payment:apply') && record.availableAmount > 0
              "
              type="link"
              size="small"
              @click="openApplication(record)"
              >申请付款</a-button
            ><span v-else class="muted">暂无可申请金额</span></template
          >
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="applicationOpen"
      title="新增付款申请"
      width="720px"
      :confirm-loading="saving"
      @ok="handleCreateApplication"
    >
      <a-alert
        v-if="selectedItem"
        class="section-alert"
        type="info"
        :message="`${selectedItem.code} · ${selectedItem.supplierName} · 可申请 ${formatMoney(selectedItem.availableAmount)}`"
      />
      <a-alert
        v-if="selectedItem?.overdue"
        class="section-alert"
        type="warning"
        show-icon
        :message="`该应付已${dueLabel(selectedItem)}，建议优先处理或说明延付原因。`"
      />
      <a-alert
        v-if="selectedItem && selectedItem.reservedAmount > 0"
        class="section-alert"
        type="info"
        show-icon
        :message="`已有审批占用 ${formatMoney(selectedItem.reservedAmount)}，本次申请不得超过剩余可申请金额。`"
      />
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <div class="quick-amounts">
          <a-button size="small" @click="setApplicationRatio(0.3)"
            >30%</a-button
          >
          <a-button size="small" @click="setApplicationRatio(0.5)"
            >50%</a-button
          >
          <a-button size="small" @click="setApplicationRatio(1)">全额</a-button>
        </div>
        <a-row :gutter="16">
          <a-col :xs="24" :md="8"
            ><a-form-item label="申请金额" name="requestedAmount"
              ><a-input-number
                v-model:value="form.requestedAmount"
                :min="0.01"
                :max="selectedItem?.availableAmount"
                :precision="2"
                class="full-input" /></a-form-item
          ></a-col>
          <a-col :xs="24" :md="8"
            ><a-form-item label="申请日期" name="requestedDate"
              ><a-input
                v-model:value="form.requestedDate"
                type="date" /></a-form-item
          ></a-col>
          <a-col :xs="24" :md="8"
            ><a-form-item label="申请人" name="applicantName"
              ><a-input v-model:value="form.applicantName" /></a-form-item
          ></a-col>
          <a-col :xs="24" :md="16"
            ><a-form-item label="付款用途" name="purpose"
              ><a-input v-model:value="form.purpose" /></a-form-item
          ></a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from "vue-router";
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import {
  createPaymentApplication,
  listFinancePayables,
  type FinancePayable,
  type FinancePayableStatus,
} from "@/api/finance";
import { useAuthStore } from "@/stores/auth";
import { downloadCsv } from "@/utils/csv";

const auth = useAuthStore();
const router = useRouter();
const items = ref<FinancePayable[]>([]);
const selectedItem = ref<FinancePayable | null>(null);
const loading = ref(false);
const saving = ref(false);
const applicationOpen = ref(false);
const keyword = ref("");
const statusFilter = ref<FinancePayableStatus>();
const scheduleFilter = ref<string>();
const formRef = ref();
const form = reactive({
  code: "",
  requestedAmount: 0,
  requestedDate: today(),
  applicantName: "",
  purpose: "",
});
const statusOptions = [
  { label: "待付款", value: "PENDING" },
  { label: "部分付款", value: "PARTIAL_PAID" },
  { label: "已付款", value: "PAID" },
  { label: "已取消", value: "CANCELLED" },
];
const scheduleOptions = [
  { label: "已逾期", value: "OVERDUE" },
  { label: "7天内到期", value: "DUE_SOON" },
  { label: "已占用", value: "RESERVED" },
];
const columns = [
  { title: "应付单", key: "payable", width: 230 },
  { title: "供应商", key: "supplier", width: 220 },
  { title: "应付 / 已付", key: "amount", width: 260 },
  { title: "可申请 / 占用", key: "available", width: 210 },
  { title: "付款排期", key: "schedule", width: 150 },
  { title: "到期日", dataIndex: "dueDate", width: 120 },
  { title: "状态", key: "status", width: 160 },
  { title: "操作", key: "action", width: 130, fixed: "right" },
];
const rules = {
  code: [],
  requestedAmount: [{ required: true, message: "请输入申请金额" }],
  requestedDate: [{ required: true }],
  applicantName: [{ required: true, message: "请输入申请人" }],
  purpose: [{ required: true, message: "请输入付款用途" }],
};
const filteredItems = computed(() =>
  items.value.filter((item) => {
    const term = keyword.value.trim().toLowerCase();
    const text =
      `${item.code} ${item.orderCode} ${item.supplierName}`.toLowerCase();
    return (
      (!statusFilter.value || item.status === statusFilter.value) &&
      (!scheduleFilter.value || scheduleLevel(item) === scheduleFilter.value) &&
      (!term || text.includes(term))
    );
  }),
);
const outstandingAmount = computed(() =>
  items.value.reduce(
    (sum, item) => sum + Number(item.outstandingAmount || 0),
    0,
  ),
);
const reservedAmount = computed(() =>
  items.value.reduce((sum, item) => sum + Number(item.reservedAmount || 0), 0),
);
const availableAmount = computed(() =>
  items.value.reduce((sum, item) => sum + Number(item.availableAmount || 0), 0),
);
const overdueAmount = computed(() =>
  items.value
    .filter((item) => item.overdue)
    .reduce((sum, item) => sum + Number(item.outstandingAmount || 0), 0),
);
const scheduleBuckets = computed(() => [
  buildScheduleBucket("overdue", "已逾期", (item) => item.overdue),
  buildScheduleBucket(
    "d7",
    "7天内到期",
    (item) => !item.overdue && dueDays(item) <= 7,
  ),
  buildScheduleBucket(
    "d30",
    "30天内到期",
    (item) => !item.overdue && dueDays(item) <= 30,
  ),
  buildScheduleBucket(
    "reserved",
    "审批占用",
    (item) => Number(item.reservedAmount || 0) > 0,
  ),
]);
const paymentFocus = computed(() =>
  items.value
    .filter(
      (item) =>
        item.status !== "PAID" &&
        item.status !== "CANCELLED" &&
        Number(item.availableAmount || item.outstandingAmount || 0) > 0,
    )
    .sort((a, b) => scheduleScore(b) - scheduleScore(a))
    .slice(0, 3),
);

onMounted(loadData);
async function loadData() {
  loading.value = true;
  try {
    items.value = await listFinancePayables();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "应付加载失败");
  } finally {
    loading.value = false;
  }
}
function buildScheduleBucket(
  key: string,
  label: string,
  predicate: (item: FinancePayable) => boolean,
) {
  const rows = items.value.filter(
    (item) =>
      item.status !== "PAID" && item.status !== "CANCELLED" && predicate(item),
  );
  return {
    key,
    label,
    count: rows.length,
    amount: rows.reduce(
      (sum, item) => sum + Number(item.outstandingAmount || 0),
      0,
    ),
  };
}
function showDueSoon() {
  scheduleFilter.value = "DUE_SOON";
  statusFilter.value = undefined;
}
function showOverdue() {
  scheduleFilter.value = "OVERDUE";
  statusFilter.value = undefined;
}
function exportPayables() {
  const headers = [
    "应付单",
    "供应商",
    "采购单",
    "应付金额",
    "已付",
    "待付",
    "审批占用",
    "可申请",
    "到期日",
    "状态",
    "付款排期",
  ];
  const rows = filteredItems.value.map((item) => [
    item.code || "",
    item.supplierName,
    item.orderCode,
    item.amount,
    item.paidAmount,
    item.outstandingAmount,
    item.reservedAmount,
    item.availableAmount,
    item.dueDate,
    statusLabel(item.status),
    dueLabel(item),
  ]);
  downloadCsv(`finance-payables-${today()}.csv`, headers, rows);
}
function openApplication(item: FinancePayable) {
  selectedItem.value = item;
  Object.assign(form, {
    code: generateCode(),
    requestedAmount: item.availableAmount,
    requestedDate: today(),
    applicantName: auth.user?.displayName || "",
    purpose: `支付${item.orderCode}采购货款`,
  });
  applicationOpen.value = true;
}
function setApplicationRatio(ratio: number) {
  const amount = Number(selectedItem.value?.availableAmount || 0);
  form.requestedAmount = Math.max(0.01, Math.round(amount * ratio * 100) / 100);
}
async function handleCreateApplication() {
  await formRef.value?.validate();
  if (!selectedItem.value) return;
  saving.value = true;
  try {
    await createPaymentApplication({
      ...form,
      payableId: selectedItem.value.id,
    });
    applicationOpen.value = false;
    message.success("付款申请已提交审批");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "付款申请失败");
  } finally {
    saving.value = false;
  }
}
function today() {
  const value = new Date();
  return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, "0")}-${String(value.getDate()).padStart(2, "0")}`;
}
function dueDays(item: FinancePayable) {
  const todayDate = new Date(today());
  const due = new Date(item.dueDate);
  return Math.ceil((due.getTime() - todayDate.getTime()) / 86400000);
}
function scheduleScore(item: FinancePayable) {
  const overdueWeight = item.overdue ? 100000000 : 0;
  const dueWeight = Math.max(0, 30 - dueDays(item)) * 1000000;
  const reservedWeight = Number(item.reservedAmount || 0) > 0 ? 500000 : 0;
  return (
    overdueWeight +
    dueWeight +
    reservedWeight +
    Number(item.availableAmount || item.outstandingAmount || 0)
  );
}
function scheduleLevel(item: FinancePayable) {
  if (item.overdue) return "OVERDUE";
  if (dueDays(item) <= 7) return "DUE_SOON";
  if (Number(item.reservedAmount || 0) > 0) return "RESERVED";
  return "NORMAL";
}
function scheduleLabel(item: FinancePayable) {
  return (
    {
      OVERDUE: "逾期",
      DUE_SOON: "即将到期",
      RESERVED: "已占用",
      NORMAL: "正常",
    } as Record<string, string>
  )[scheduleLevel(item)];
}
function scheduleColor(item: FinancePayable) {
  return (
    {
      OVERDUE: "red",
      DUE_SOON: "orange",
      RESERVED: "blue",
      NORMAL: "green",
    } as Record<string, string>
  )[scheduleLevel(item)];
}
function dueLabel(item: FinancePayable) {
  const days = dueDays(item);
  if (days < 0) return `逾期 ${Math.abs(days)} 天`;
  if (days === 0) return "今天到期";
  return `${days} 天后到期`;
}
function generateCode() {
  const value = new Date();
  return `FKSQ-${today().replaceAll("-", "")}-${String(value.getHours()).padStart(2, "0")}${String(value.getMinutes()).padStart(2, "0")}${String(value.getSeconds()).padStart(2, "0")}${String(value.getMilliseconds()).padStart(3, "0")}`;
}
function statusLabel(status: FinancePayableStatus) {
  return (
    {
      PENDING: "待付款",
      PARTIAL_PAID: "部分付款",
      PAID: "已付款",
      CANCELLED: "已取消",
    } as Record<FinancePayableStatus, string>
  )[status];
}
function statusColor(status: FinancePayableStatus) {
  return (
    {
      PENDING: "orange",
      PARTIAL_PAID: "blue",
      PAID: "green",
      CANCELLED: "default",
    } as Record<FinancePayableStatus, string>
  )[status];
}
function formatMoney(value: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value || 0);
}
function moneyFormatter(value: number | string) {
  return formatMoney(Number(value));
}
</script>

<style scoped>
.finance-action-panel {
  margin: 16px 0;
  padding: 14px;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.action-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.action-heading h3 {
  margin: 0;
  color: #101828;
  font-size: 15px;
}

.action-heading p,
.schedule-card span,
.focus-row span,
.schedule-card small,
.focus-row small {
  color: #667085;
  font-size: 12px;
}

.action-heading p {
  margin: 4px 0 0;
}

.schedule-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.schedule-card,
.focus-row {
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  padding: 10px 12px;
  border: 1px solid #eef2f7;
  border-radius: 6px;
  background: #f8fafc;
}

.focus-row {
  cursor: pointer;
}

.schedule-card strong,
.focus-row strong {
  max-width: 100%;
  color: #101828;
  font-size: 18px;
  overflow-wrap: anywhere;
}

.quick-amounts {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

@media (max-width: 1100px) {
  .schedule-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
