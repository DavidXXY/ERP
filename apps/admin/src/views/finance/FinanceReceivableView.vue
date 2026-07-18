<template>
  <div class="page-stack">
    <a-card title="应收管理">
      <template #extra>
        <a-space>
          <a-button @click="exportReceivables">导出</a-button>
          <a-button :loading="loading" @click="loadData"
            ><template #icon><ReloadOutlined /></template>刷新</a-button
          >
        </a-space>
      </template>
      <a-row :gutter="[16, 16]" class="metric-row">
        <a-col :xs="12" :lg="6"
          ><a-statistic
            title="应收余额"
            :value="outstandingAmount"
            :formatter="moneyFormatter"
        /></a-col>
        <a-col :xs="12" :lg="6"
          ><a-statistic
            title="待开票"
            :value="amountByStatus('INVOICE_PENDING')"
            :formatter="moneyFormatter"
        /></a-col>
        <a-col :xs="12" :lg="6"
          ><a-statistic
            title="待回款"
            :value="amountByStatus('PAYMENT_PENDING')"
            :formatter="moneyFormatter"
        /></a-col>
        <a-col :xs="12" :lg="6"
          ><a-statistic
            title="逾期应收"
            :value="amountByStatus('OVERDUE')"
            :formatter="moneyFormatter"
        /></a-col>
      </a-row>

      <section class="finance-action-panel">
        <div class="action-heading">
          <div>
            <h3>催收优先级</h3>
            <p>按逾期天数和未收金额识别今天最需要推进的回款事项。</p>
          </div>
          <a-space>
            <a-button size="small" @click="showOverdue">只看逾期</a-button>
            <a-button size="small" @click="showHighPriority">高优先级</a-button>
          </a-space>
        </div>
        <div class="action-grid">
          <div
            v-for="bucket in agingBuckets"
            :key="bucket.key"
            class="action-card"
          >
            <span>{{ bucket.label }}</span>
            <strong>{{ formatMoney(bucket.amount) }}</strong>
            <small>{{ bucket.count }} 笔</small>
          </div>
          <button
            v-for="item in collectionFocus"
            :key="item.id"
            class="focus-row"
            type="button"
            @click="openReceipt(item)"
          >
            <span>{{ item.customerName }}</span>
            <strong>{{ formatMoney(item.outstandingAmount) }}</strong>
            <small>{{ agingLabel(item) }}</small>
          </button>
        </div>
      </section>

      <a-space wrap class="table-toolbar">
        <a-input-search
          v-model:value="keyword"
          allow-clear
          placeholder="搜索应收单、客户、合同"
          style="width: 260px"
        />
        <a-select
          v-model:value="statusFilter"
          allow-clear
          placeholder="全部状态"
          :options="statusOptions"
          style="width: 140px"
        />
        <a-select
          v-model:value="priorityFilter"
          allow-clear
          placeholder="催收优先级"
          :options="priorityOptions"
          style="width: 150px"
        />
      </a-space>
      <a-table
        :columns="columns"
        :data-source="filteredItems"
        :loading="loading"
        :pagination="{ pageSize: 8 }"
        :row-key="(item: Receivable) => item.id"
        :scroll="{ x: 1320 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'receivable'">
            <a-button
              type="link"
              class="cell-link"
              @click="openDetail(record)"
              >{{ record.code }}</a-button
            >
            <span class="table-subtitle">{{ record.customerName }}</span>
          </template>
          <template v-else-if="column.key === 'contract'">{{
            record.contractCode || "-"
          }}</template>
          <template v-else-if="column.key === 'amount'"
            ><strong>{{ formatMoney(record.amount) }}</strong
            ><span class="table-subtitle"
              >已收 {{ formatMoney(record.settledAmount) }} · 待收
              {{ formatMoney(record.outstandingAmount) }}</span
            ></template
          >
          <template v-else-if="column.key === 'invoice'">
            {{
              record.invoiceNo ||
              (record.invoiceRequested ? "待财务开票" : "未申请开票")
            }}
            <span v-if="record.invoiceDate" class="table-subtitle">{{
              record.invoiceDate
            }}</span>
            <span v-else-if="record.invoiceRequested" class="table-subtitle">
              {{ record.invoiceRequestedBy || "业务侧" }} ·
              {{ formatDateTime(record.invoiceRequestedAt) }}
            </span>
            <span v-if="record.invoiceRequestRemark" class="table-subtitle">{{
              record.invoiceRequestRemark
            }}</span>
          </template>
          <template v-else-if="column.key === 'aging'">
            <a-tag :color="priorityColor(record)">{{
              priorityLabel(record)
            }}</a-tag>
            <span class="table-subtitle">{{ agingLabel(record) }}</span>
          </template>
          <template v-else-if="column.key === 'status'"
            ><a-tag :color="statusColor(record.status)">{{
              statusLabel(record.status)
            }}</a-tag></template
          >
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a-button type="link" size="small" @click="openDetail(record)"
                >详情</a-button
              >
              <a-button
                v-if="
                  auth.can('finance:receivable:invoice') &&
                  record.invoiceRequested &&
                  !record.invoiceNo &&
                  record.status !== 'SETTLED'
                "
                type="link"
                size="small"
                @click="openInvoice(record)"
                >处理开票</a-button
              >
              <a-tag
                v-else-if="!record.invoiceNo && record.status !== 'SETTLED'"
                color="default"
                >待申请</a-tag
              >
              <a-button
                v-if="
                  auth.can('finance:receivable:collect') &&
                  record.invoiceNo &&
                  record.status !== 'SETTLED'
                "
                type="link"
                size="small"
                @click="openReceipt(record)"
                >登记回款</a-button
              >
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="detailOpen"
      title="应收详情"
      width="860px"
      :footer="null"
    >
      <a-spin :spinning="detailLoading">
        <template v-if="detail">
          <div class="detail-header">
            <div>
              <strong>{{ detail.receivable.code }}</strong>
              <span>{{ detail.receivable.customerName }}</span>
            </div>
            <a-space>
              <a-button
                v-if="
                  auth.can('finance:receivable:invoice') &&
                  detail.receivable.invoiceRequested &&
                  !detail.receivable.invoiceNo &&
                  detail.receivable.status !== 'SETTLED'
                "
                type="primary"
                @click="openInvoice(detail.receivable)"
              >
                处理开票
              </a-button>
              <a-button
                v-if="
                  auth.can('finance:receivable:collect') &&
                  detail.receivable.invoiceNo &&
                  detail.receivable.status !== 'SETTLED'
                "
                type="primary"
                @click="openReceipt(detail.receivable)"
              >
                登记回款
              </a-button>
            </a-space>
          </div>

          <a-descriptions
            bordered
            size="small"
            title="应收信息"
            :column="2"
            class="detail-section"
          >
            <a-descriptions-item label="应收金额">{{
              formatMoney(detail.receivable.amount)
            }}</a-descriptions-item>
            <a-descriptions-item label="待收金额">{{
              formatMoney(detail.receivable.outstandingAmount)
            }}</a-descriptions-item>
            <a-descriptions-item label="已收金额">{{
              formatMoney(detail.receivable.settledAmount)
            }}</a-descriptions-item>
            <a-descriptions-item label="到期日">{{
              detail.receivable.dueDate
            }}</a-descriptions-item>
            <a-descriptions-item label="状态"
              ><a-tag :color="statusColor(detail.receivable.status)">{{
                statusLabel(detail.receivable.status)
              }}</a-tag></a-descriptions-item
            >
            <a-descriptions-item label="开票状态">{{
              detail.receivable.invoiceNo ||
              (detail.receivable.invoiceRequested ? "待财务开票" : "未申请开票")
            }}</a-descriptions-item>
            <a-descriptions-item
              v-if="detail.receivable.invoiceRequested"
              label="申请信息"
              :span="2"
            >
              {{ detail.receivable.invoiceRequestedBy || "业务侧" }} ·
              {{ formatDateTime(detail.receivable.invoiceRequestedAt) }}
              <span v-if="detail.receivable.invoiceRequestRemark">
                · {{ detail.receivable.invoiceRequestRemark }}</span
              >
            </a-descriptions-item>
          </a-descriptions>

          <a-descriptions
            bordered
            size="small"
            title="企业开票信息"
            :column="2"
            class="detail-section"
          >
            <a-descriptions-item label="开票抬头">{{
              valueOrDash(
                detail.customerInvoice.invoiceTitle ||
                  detail.customerInvoice.customerName,
              )
            }}</a-descriptions-item>
            <a-descriptions-item label="税号">{{
              valueOrDash(detail.customerInvoice.taxNo)
            }}</a-descriptions-item>
            <a-descriptions-item label="开户行">{{
              valueOrDash(detail.customerInvoice.bankName)
            }}</a-descriptions-item>
            <a-descriptions-item label="银行账号">{{
              valueOrDash(detail.customerInvoice.bankAccount)
            }}</a-descriptions-item>
            <a-descriptions-item label="注册地址">{{
              valueOrDash(detail.customerInvoice.registeredAddress)
            }}</a-descriptions-item>
            <a-descriptions-item label="注册电话">{{
              valueOrDash(detail.customerInvoice.registeredPhone)
            }}</a-descriptions-item>
            <a-descriptions-item label="客户负责人">{{
              valueOrDash(detail.customerInvoice.ownerName)
            }}</a-descriptions-item>
            <a-descriptions-item label="付款习惯">{{
              valueOrDash(detail.customerInvoice.paymentHabit)
            }}</a-descriptions-item>
          </a-descriptions>

          <a-descriptions
            v-if="detail.contract"
            bordered
            size="small"
            title="合同信息"
            :column="2"
            class="detail-section"
          >
            <a-descriptions-item label="合同编号">
              <a-button
                type="link"
                class="cell-link"
                @click="goContractDetail(detail.contract.id)"
              >
                {{ valueOrDash(detail.contract.code) }}
              </a-button>
            </a-descriptions-item>
            <a-descriptions-item label="项目名称">{{
              detail.contract.projectName
            }}</a-descriptions-item>
            <a-descriptions-item label="合同类型">{{
              detail.contract.contractType
            }}</a-descriptions-item>
            <a-descriptions-item label="合同状态">{{
              contractStatusLabel(detail.contract.status)
            }}</a-descriptions-item>
            <a-descriptions-item label="合同金额">{{
              formatMoney(detail.contract.amount)
            }}</a-descriptions-item>
            <a-descriptions-item label="不含税金额"
              >{{ formatMoney(detail.contract.netAmount) }} · 税率
              {{ detail.contract.taxRate }}%</a-descriptions-item
            >
            <a-descriptions-item label="合同周期"
              >{{ detail.contract.startDate }} 至
              {{ detail.contract.endDate }}</a-descriptions-item
            >
            <a-descriptions-item label="服务频次">{{
              valueOrDash(detail.contract.serviceCycle)
            }}</a-descriptions-item>
          </a-descriptions>
          <a-empty v-else description="未关联合同" />
        </template>
      </a-spin>
    </a-modal>

    <a-modal
      v-model:open="invoiceOpen"
      title="登记开票"
      :confirm-loading="saving"
      @ok="handleInvoice"
    >
      <a-alert
        v-if="selectedItem"
        class="section-alert"
        type="info"
        :message="`${selectedItem.code} · ${selectedItem.customerName} · ${formatMoney(selectedItem.amount)}`"
      />
      <a-form
        ref="invoiceFormRef"
        :model="invoiceForm"
        :rules="invoiceRules"
        layout="vertical"
      >
        <a-form-item label="发票号码" name="invoiceNo"
          ><a-input v-model:value="invoiceForm.invoiceNo"
        /></a-form-item>
        <a-form-item label="开票日期" name="invoiceDate"
          ><a-input v-model:value="invoiceForm.invoiceDate" type="date"
        /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="receiptOpen"
      title="登记回款"
      :confirm-loading="saving"
      @ok="handleReceipt"
    >
      <a-alert
        v-if="selectedItem"
        class="section-alert"
        type="info"
        :message="`${selectedItem.code} · 待收 ${formatMoney(selectedItem.outstandingAmount)}`"
      />
      <a-form
        ref="receiptFormRef"
        :model="receiptForm"
        :rules="receiptRules"
        layout="vertical"
      >
        <div class="quick-amounts">
          <a-button size="small" @click="setReceiptRatio(0.3)">30%</a-button>
          <a-button size="small" @click="setReceiptRatio(0.5)">50%</a-button>
          <a-button size="small" @click="setReceiptRatio(1)">全额</a-button>
        </div>
        <a-row :gutter="16">
          <a-col :span="12"
            ><a-form-item label="本次回款" name="amount"
              ><a-input-number
                v-model:value="receiptForm.amount"
                :min="0.01"
                :max="selectedItem?.outstandingAmount"
                class="full-input" /></a-form-item
          ></a-col>
          <a-col :span="12"
            ><a-form-item label="回款日期" name="receivedDate"
              ><a-input
                v-model:value="receiptForm.receivedDate"
                type="date" /></a-form-item
          ></a-col>
          <a-col :span="24"
            ><a-form-item label="银行流水号" name="referenceNo"
              ><a-input v-model:value="receiptForm.referenceNo" /></a-form-item
          ></a-col>
          <a-col :span="24"
            ><a-form-item label="登记人" name="recorderName"
              ><a-input v-model:value="receiptForm.recorderName" /></a-form-item
          ></a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { useRouter } from "vue-router";
import type { Receivable, ReceivableStatus } from "@/api/crm";
import {
  getFinanceReceivableDetail,
  listFinanceReceivables,
  recordFinanceReceipt,
  registerFinanceInvoice,
  type FinanceReceivableDetail,
} from "@/api/finance";
import { useAuthStore } from "@/stores/auth";
import { downloadCsv } from "@/utils/csv";

const auth = useAuthStore();
const router = useRouter();
const items = ref<Receivable[]>([]);
const loading = ref(false);
const saving = ref(false);
const detailOpen = ref(false);
const detailLoading = ref(false);
const invoiceOpen = ref(false);
const receiptOpen = ref(false);
const selectedItem = ref<Receivable | null>(null);
const detail = ref<FinanceReceivableDetail | null>(null);
const keyword = ref("");
const statusFilter = ref<ReceivableStatus>();
const priorityFilter = ref<string>();
const invoiceFormRef = ref();
const receiptFormRef = ref();
const invoiceForm = reactive({ invoiceNo: "", invoiceDate: today() });
const receiptForm = reactive({
  amount: 0,
  receivedDate: today(),
  referenceNo: "",
  recorderName: "",
});
const statusOptions = [
  { label: "待开票", value: "INVOICE_PENDING" },
  { label: "待回款", value: "PAYMENT_PENDING" },
  { label: "已核销", value: "SETTLED" },
  { label: "逾期", value: "OVERDUE" },
];
const priorityOptions = [
  { label: "高优先级", value: "HIGH" },
  { label: "中优先级", value: "MEDIUM" },
  { label: "正常", value: "NORMAL" },
];
const columns = [
  { title: "应收单 / 客户", key: "receivable", width: 240 },
  { title: "合同编号", key: "contract", width: 170 },
  { title: "来源单号", dataIndex: "sourceNo", width: 170 },
  { title: "应收 / 回款", key: "amount", width: 260 },
  { title: "开票信息", key: "invoice", width: 180 },
  { title: "账龄 / 优先级", key: "aging", width: 150 },
  { title: "到期日", dataIndex: "dueDate", width: 120 },
  { title: "状态", key: "status", width: 110 },
  { title: "操作", key: "action", width: 220, fixed: "right" },
];
const invoiceRules = {
  invoiceNo: [{ required: true, message: "请输入发票号码" }],
  invoiceDate: [{ required: true }],
};
const receiptRules = {
  amount: [{ required: true, message: "请输入回款金额" }],
  receivedDate: [{ required: true }],
  referenceNo: [{ required: true, message: "请输入银行流水号" }],
  recorderName: [{ required: true, message: "请输入登记人" }],
};
const filteredItems = computed(() =>
  items.value.filter((item) => {
    const term = keyword.value.trim().toLowerCase();
    const text =
      `${item.code} ${item.contractCode || ""} ${item.customerName}`.toLowerCase();
    return (
      (!statusFilter.value || item.status === statusFilter.value) &&
      (!priorityFilter.value || priorityLevel(item) === priorityFilter.value) &&
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
const agingBuckets = computed(() => [
  buildAgingBucket("current", "未到期", 0, 0),
  buildAgingBucket("d1", "逾期1-30天", 1, 30),
  buildAgingBucket("d31", "逾期31-60天", 31, 60),
  buildAgingBucket("d61", "逾期60天以上", 61, Number.POSITIVE_INFINITY),
]);
const collectionFocus = computed(() =>
  items.value
    .filter(
      (item) =>
        item.status !== "SETTLED" &&
        item.invoiceNo &&
        Number(item.outstandingAmount || 0) > 0,
    )
    .sort((a, b) => priorityScore(b) - priorityScore(a))
    .slice(0, 3),
);

onMounted(loadData);
async function loadData() {
  loading.value = true;
  try {
    items.value = await listFinanceReceivables();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "应收加载失败");
  } finally {
    loading.value = false;
  }
}
function amountByStatus(status: ReceivableStatus) {
  return items.value
    .filter((item) => item.status === status)
    .reduce((sum, item) => sum + Number(item.outstandingAmount || 0), 0);
}
function buildAgingBucket(
  key: string,
  label: string,
  min: number,
  max: number,
) {
  const rows = items.value.filter(
    (item) => item.status !== "SETTLED" && inAgingRange(item, min, max),
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
function showOverdue() {
  statusFilter.value = "OVERDUE";
  priorityFilter.value = undefined;
}
function showHighPriority() {
  priorityFilter.value = "HIGH";
  statusFilter.value = undefined;
}
function exportReceivables() {
  const headers = [
    "应收单",
    "客户",
    "合同",
    "来源单号",
    "应收金额",
    "已收",
    "待收",
    "发票号",
    "到期日",
    "状态",
    "账龄",
    "优先级",
  ];
  const rows = filteredItems.value.map((item) => [
    item.code || "",
    item.customerName,
    item.contractCode || "",
    item.sourceNo,
    item.amount,
    item.settledAmount,
    item.outstandingAmount,
    item.invoiceNo || (item.invoiceRequested ? "待财务开票" : ""),
    item.dueDate,
    statusLabel(item.status),
    agingLabel(item),
    priorityLabel(item),
  ]);
  downloadCsv(`finance-receivables-${today()}.csv`, headers, rows);
}
async function openDetail(item: Receivable) {
  selectedItem.value = item;
  detailOpen.value = true;
  detailLoading.value = true;
  try {
    detail.value = await getFinanceReceivableDetail(item.id);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "应收详情加载失败");
  } finally {
    detailLoading.value = false;
  }
}
function openInvoice(item: Receivable) {
  selectedItem.value = item;
  Object.assign(invoiceForm, { invoiceNo: "", invoiceDate: today() });
  invoiceOpen.value = true;
}
function openReceipt(item: Receivable) {
  selectedItem.value = item;
  Object.assign(receiptForm, {
    amount: item.outstandingAmount,
    receivedDate: today(),
    referenceNo: "",
    recorderName: auth.user?.displayName || "",
  });
  receiptOpen.value = true;
}
function goContractDetail(contractId: string) {
  detailOpen.value = false;
  router.push("/crm/contracts/" + contractId);
}
function setReceiptRatio(ratio: number) {
  const amount = Number(selectedItem.value?.outstandingAmount || 0);
  receiptForm.amount = Math.max(0.01, Math.round(amount * ratio * 100) / 100);
}
async function handleInvoice() {
  await invoiceFormRef.value?.validate();
  if (!selectedItem.value) return;
  saving.value = true;
  try {
    const result = await registerFinanceInvoice(selectedItem.value.id, {
      ...invoiceForm,
    });
    invoiceOpen.value = false;
    message.success("开票信息已登记");
    await loadData();
    await refreshDetail(result.id);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "开票登记失败");
  } finally {
    saving.value = false;
  }
}
async function handleReceipt() {
  await receiptFormRef.value?.validate();
  if (!selectedItem.value) return;
  saving.value = true;
  try {
    const result = await recordFinanceReceipt(selectedItem.value.id, {
      ...receiptForm,
    });
    receiptOpen.value = false;
    message.success(
      result.status === "SETTLED" ? "应收已全部核销" : "部分回款已登记",
    );
    await loadData();
    await refreshDetail(result.id);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "回款登记失败");
  } finally {
    saving.value = false;
  }
}
async function refreshDetail(id: string) {
  if (!detailOpen.value) return;
  detail.value = await getFinanceReceivableDetail(id);
  selectedItem.value = detail.value.receivable;
}
function today() {
  const value = new Date();
  return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, "0")}-${String(value.getDate()).padStart(2, "0")}`;
}
function formatDateTime(value?: string) {
  return value
    ? new Date(value).toLocaleString("zh-CN", { hour12: false })
    : "-";
}
function valueOrDash(value?: string) {
  return value && value.trim() ? value : "-";
}
function inAgingRange(item: Receivable, min: number, max: number) {
  const overdue = overdueDays(item);
  if (min === 0 && max === 0) return overdue === 0;
  return overdue >= min && overdue <= max;
}
function overdueDays(item: Receivable) {
  const todayDate = new Date(today());
  const due = new Date(item.dueDate);
  return Math.max(
    0,
    Math.floor((todayDate.getTime() - due.getTime()) / 86400000),
  );
}
function priorityScore(item: Receivable) {
  return overdueDays(item) * 1000000 + Number(item.outstandingAmount || 0);
}
function priorityLevel(item: Receivable) {
  const days = overdueDays(item);
  const amount = Number(item.outstandingAmount || 0);
  if (days > 30 || amount >= 100000) return "HIGH";
  if (days > 0 || amount >= 30000) return "MEDIUM";
  return "NORMAL";
}
function priorityLabel(item: Receivable) {
  return (
    { HIGH: "高", MEDIUM: "中", NORMAL: "正常" } as Record<string, string>
  )[priorityLevel(item)];
}
function priorityColor(item: Receivable) {
  return (
    { HIGH: "red", MEDIUM: "orange", NORMAL: "green" } as Record<string, string>
  )[priorityLevel(item)];
}
function agingLabel(item: Receivable) {
  const days = overdueDays(item);
  if (days <= 0) return "未到期";
  return `逾期 ${days} 天`;
}
function statusLabel(status: ReceivableStatus) {
  return (
    {
      INVOICE_PENDING: "待开票",
      PAYMENT_PENDING: "待回款",
      SETTLED: "已核销",
      OVERDUE: "逾期",
    } as Record<ReceivableStatus, string>
  )[status];
}
function statusColor(status: ReceivableStatus) {
  return (
    {
      INVOICE_PENDING: "orange",
      PAYMENT_PENDING: "blue",
      SETTLED: "green",
      OVERDUE: "red",
    } as Record<ReceivableStatus, string>
  )[status];
}
function contractStatusLabel(status: string) {
  return (
    (
      {
        PENDING_APPROVAL: "待合同审批",
        PENDING_SEAL: "待双方盖章",
        SEAL_APPROVAL: "盖章件审批中",
        ACTIVE: "履约中",
        RENEWAL_PENDING: "续约待跟进",
        OVERDUE_RISK: "逾期风险",
        CLOSED: "已关闭",
      } as Record<string, string>
    )[status] || status
  );
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

.action-heading p {
  margin: 4px 0 0;
  color: #667085;
  font-size: 12px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.action-card,
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

.action-card span,
.focus-row span,
.action-card small,
.focus-row small {
  color: #667085;
  font-size: 12px;
}

.action-card strong,
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

.cell-link {
  height: auto;
  padding: 0;
  font-weight: 600;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.detail-header strong,
.detail-header span {
  display: block;
}

.detail-header strong {
  color: #101828;
  font-size: 16px;
}

.detail-header span {
  margin-top: 4px;
  color: #667085;
}

.detail-section {
  margin-bottom: 16px;
}

@media (max-width: 1100px) {
  .action-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
