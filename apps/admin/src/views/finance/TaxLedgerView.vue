<template>
  <div class="page-stack tax-ledger-page">
    <header class="tax-header">
      <div>
        <span class="eyebrow">VAT CONTROL LEDGER</span>
        <h2>税务台账</h2>
        <p>销项、进项与作废红冲凭证统一追溯</p>
      </div>
      <a-space wrap>
        <a-button @click="exportLedger">导出台账</a-button>
        <a-button :loading="loading" @click="loadData">
          <template #icon><ReloadOutlined /></template>刷新
        </a-button>
      </a-space>
    </header>

    <section class="tax-metrics">
      <div>
        <span>销项税额</span
        ><strong>{{ formatMoney(summary.outputTax) }}</strong
        ><small>价税合计 {{ formatMoney(summary.outputGross) }}</small>
      </div>
      <div>
        <span>可抵扣进项税额</span
        ><strong>{{ formatMoney(summary.inputTax) }}</strong
        ><small>价税合计 {{ formatMoney(summary.inputGross) }}</small>
      </div>
      <div>
        <span>应纳增值税</span
        ><strong :class="{ danger: summary.payable > 0 }">{{
          signedMoney(summary.payable)
        }}</strong
        ><small>销项税额减进项税额</small>
      </div>
      <div>
        <span>已调整发票</span><strong>{{ summary.adjusted }}</strong
        ><small>作废或红冲记录</small>
      </div>
    </section>

    <section class="ledger-panel">
      <div class="filter-bar">
        <a-input v-model:value="filters.from" type="date" class="date-input" />
        <span>至</span>
        <a-input v-model:value="filters.to" type="date" class="date-input" />
        <a-segmented v-model:value="filters.side" :options="sideOptions" />
        <a-select
          v-model:value="filters.status"
          allow-clear
          placeholder="税务状态"
          class="status-select"
          :options="statusOptions"
        />
        <a-input-search
          v-model:value="keyword"
          allow-clear
          placeholder="发票号、业务单号、往来单位"
          class="keyword-input"
        />
        <a-button type="primary" @click="loadData">查询</a-button>
      </div>

      <a-alert
        v-if="errorMessage"
        class="section-alert"
        type="warning"
        show-icon
        :message="errorMessage"
      />

      <a-table
        :columns="columns"
        :data-source="filteredRows"
        :loading="loading"
        :pagination="{ pageSize: 12 }"
        :row-key="(item: TaxInvoiceLine) => `${item.side}-${item.id}`"
        :scroll="{ x: 1250 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'invoice'">
            <strong>{{ record.invoiceNo || "-" }}</strong>
            <span class="table-subtitle">{{ record.businessNo }}</span>
          </template>
          <template v-else-if="column.key === 'side'">
            <a-tag :color="record.side === 'OUTPUT' ? 'blue' : 'green'">{{
              sideLabel(record.side)
            }}</a-tag>
          </template>
          <template v-else-if="column.key === 'amount'">
            <strong>{{ formatMoney(record.grossAmount) }}</strong>
            <span class="table-subtitle"
              >不含税 {{ formatMoney(record.netAmount) }}</span
            >
          </template>
          <template v-else-if="column.key === 'tax'">
            <strong>{{ formatMoney(record.taxAmount) }}</strong>
            <span class="table-subtitle"
              >税率 {{ formatRate(record.taxRate) }}</span
            >
          </template>
          <template v-else-if="column.key === 'verification'">
            {{ verificationLabel(record.verificationStatus) }}
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="taxStatusColor(record.status)">{{
              taxStatusLabel(record.status)
            }}</a-tag>
            <span v-if="record.adjustmentReason" class="table-subtitle">{{
              record.adjustmentReason
            }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button
              v-if="
                record.status === 'NORMAL' && auth.can('finance:tax:manage')
              "
              danger
              type="link"
              size="small"
              @click="openAdjust(record)"
              >作废/红冲</a-button
            >
            <span v-else class="muted">-</span>
          </template>
        </template>
      </a-table>
    </section>

    <a-modal
      v-model:open="adjustModalOpen"
      title="发票作废或红冲"
      :confirm-loading="submitting"
      @ok="submitAdjustment"
    >
      <a-alert
        class="section-alert"
        type="warning"
        show-icon
        message="提交后将自动生成原业务凭证的反向冲销凭证。"
      />
      <a-form layout="vertical">
        <a-form-item label="发票"
          ><a-input :value="adjustTarget?.invoiceNo" disabled
        /></a-form-item>
        <a-form-item label="处理方式" required
          ><a-radio-group v-model:value="adjustForm.status"
            ><a-radio-button value="VOIDED">作废</a-radio-button
            ><a-radio-button value="RED_FLUSHED"
              >红冲</a-radio-button
            ></a-radio-group
          ></a-form-item
        >
        <a-form-item label="调整日期" required
          ><a-input v-model:value="adjustForm.adjustmentDate" type="date"
        /></a-form-item>
        <a-form-item label="调整原因" required
          ><a-textarea
            v-model:value="adjustForm.reason"
            :rows="4"
            :maxlength="500"
        /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import {
  adjustTaxInvoice,
  listTaxLedger,
  type TaxInvoiceLine,
} from "@/api/finance";
import { useAuthStore } from "@/stores/auth";
import { downloadCsv } from "@/utils/csv";

const auth = useAuthStore();
const loading = ref(false);
const submitting = ref(false);
const errorMessage = ref("");
const rows = ref<TaxInvoiceLine[]>([]);
const keyword = ref("");
const adjustModalOpen = ref(false);
const adjustTarget = ref<TaxInvoiceLine>();
const filters = reactive({
  from: `${new Date().getFullYear()}-01-01`,
  to: today(),
  side: "ALL",
  status: undefined as string | undefined,
});
const adjustForm = reactive({
  status: "RED_FLUSHED" as "VOIDED" | "RED_FLUSHED",
  adjustmentDate: today(),
  reason: "",
});
const sideOptions = [
  { label: "全部", value: "ALL" },
  { label: "销项", value: "OUTPUT" },
  { label: "进项", value: "INPUT" },
];
const statusOptions = [
  { label: "正常", value: "NORMAL" },
  { label: "已作废", value: "VOIDED" },
  { label: "已红冲", value: "RED_FLUSHED" },
];
const columns = [
  { title: "发票 / 业务单", key: "invoice", width: 210 },
  { title: "方向", key: "side", width: 90 },
  { title: "开票日期", dataIndex: "invoiceDate", width: 115 },
  { title: "往来单位", dataIndex: "partnerName", width: 190 },
  { title: "价税金额（含税，元）", key: "amount", width: 190 },
  { title: "税额", key: "tax", width: 150 },
  { title: "认证状态", key: "verification", width: 110 },
  { title: "税务状态", key: "status", width: 180 },
  { title: "操作", key: "action", fixed: "right" as const, width: 110 },
];
const filteredRows = computed(() => {
  const term = keyword.value.trim().toLowerCase();
  return !term
    ? rows.value
    : rows.value.filter((item) =>
        `${item.invoiceNo} ${item.businessNo} ${item.partnerName}`
          .toLowerCase()
          .includes(term),
      );
});
const summary = computed(() =>
  rows.value.reduce(
    (value, item) => {
      if (item.status !== "NORMAL") value.adjusted += 1;
      if (item.status === "NORMAL" && item.side === "OUTPUT") {
        value.outputTax += Number(item.taxAmount);
        value.outputGross += Number(item.grossAmount);
      }
      if (
        item.status === "NORMAL" &&
        item.side === "INPUT" &&
        item.verificationStatus === "VERIFIED"
      ) {
        value.inputTax += Number(item.taxAmount);
        value.inputGross += Number(item.grossAmount);
      }
      value.payable = value.outputTax - value.inputTax;
      return value;
    },
    {
      outputTax: 0,
      outputGross: 0,
      inputTax: 0,
      inputGross: 0,
      payable: 0,
      adjusted: 0,
    },
  ),
);

onMounted(loadData);
async function loadData() {
  loading.value = true;
  errorMessage.value = "";
  try {
    rows.value = await listTaxLedger({
      from: filters.from,
      to: filters.to,
      side: filters.side === "ALL" ? undefined : filters.side,
      status: filters.status,
    });
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : "税务台账加载失败";
  } finally {
    loading.value = false;
  }
}
function openAdjust(item: TaxInvoiceLine) {
  adjustTarget.value = item;
  Object.assign(adjustForm, {
    status: "RED_FLUSHED",
    adjustmentDate: today(),
    reason: "",
  });
  adjustModalOpen.value = true;
}
async function submitAdjustment() {
  if (!adjustTarget.value || adjustForm.reason.trim().length < 5) {
    message.warning("请填写至少 5 个字的调整原因");
    return;
  }
  submitting.value = true;
  try {
    await adjustTaxInvoice(adjustTarget.value.side, adjustTarget.value.id, {
      ...adjustForm,
      reason: adjustForm.reason.trim(),
    });
    message.success(
      adjustForm.status === "VOIDED"
        ? "发票已作废并生成冲销凭证"
        : "发票已红冲并生成冲销凭证",
    );
    adjustModalOpen.value = false;
    await loadData();
  } catch (error) {
    message.error((error as Error).message);
  } finally {
    submitting.value = false;
  }
}
function exportLedger() {
  downloadCsv(
    `tax-ledger-${filters.from}-${filters.to}.csv`,
    [
      "方向",
      "发票号",
      "业务单号",
      "往来单位",
      "开票日期",
      "价税合计",
      "不含税金额",
      "税额",
      "税率",
      "状态",
    ],
    filteredRows.value.map((item) => [
      sideLabel(item.side),
      item.invoiceNo,
      item.businessNo,
      item.partnerName,
      item.invoiceDate,
      item.grossAmount,
      item.netAmount,
      item.taxAmount,
      item.taxRate,
      taxStatusLabel(item.status),
    ]),
  );
}
function sideLabel(value: string) {
  return value === "OUTPUT" ? "销项" : "进项";
}
function taxStatusLabel(value: string) {
  return (
    (
      { NORMAL: "正常", VOIDED: "已作废", RED_FLUSHED: "已红冲" } as Record<
        string,
        string
      >
    )[value] ?? value
  );
}
function taxStatusColor(value: string) {
  return value === "NORMAL" ? "green" : value === "VOIDED" ? "default" : "red";
}
function verificationLabel(value?: string) {
  return value === "VERIFIED"
    ? "已认证"
    : value === "PENDING"
      ? "待认证"
      : value || "不适用";
}
function formatRate(value: number) {
  return `${Number(value || 0).toFixed(2)}%`;
}
function formatMoney(value: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    minimumFractionDigits: 2,
  }).format(Number(value || 0));
}
function signedMoney(value: number) {
  return `${Number(value) > 0 ? "+" : ""}${formatMoney(value)}`;
}
function today() {
  return new Date().toISOString().slice(0, 10);
}
</script>

<style scoped>
.tax-ledger-page {
  color: #1f2937;
}
.tax-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  padding: 4px 2px;
}
.tax-header h2 {
  margin: 3px 0 2px;
  font-size: 24px;
  line-height: 1.25;
}
.tax-header p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}
.eyebrow {
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
}
.tax-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  background: #172033;
  color: #fff;
}
.tax-metrics > div {
  display: grid;
  gap: 7px;
  padding: 18px;
  border-right: 1px solid #334155;
}
.tax-metrics > div:last-child {
  border-right: 0;
}
.tax-metrics span,
.tax-metrics small {
  color: #94a3b8;
  font-size: 12px;
}
.tax-metrics strong {
  font-size: 23px;
  overflow-wrap: anywhere;
}
.tax-metrics .danger {
  color: #ff7875;
}
.ledger-panel {
  padding: 18px;
  border: 1px solid #d9dee7;
  background: #fff;
}
.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.date-input {
  width: 150px;
}
.status-select {
  width: 130px;
}
.keyword-input {
  width: min(300px, 100%);
}
.muted {
  color: #94a3b8;
}
@media (max-width: 800px) {
  .tax-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .tax-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
@media (max-width: 520px) {
  .tax-metrics {
    grid-template-columns: 1fr;
  }
  .tax-metrics > div {
    border-right: 0;
    border-bottom: 1px solid #334155;
  }
  .date-input,
  .status-select,
  .keyword-input {
    width: 100%;
  }
}
</style>
