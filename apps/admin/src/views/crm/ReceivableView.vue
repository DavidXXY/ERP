<template>
  <div class="page-stack">
    <a-card title="合同应收">
      <template #extra
        ><a-button @click="loadData">刷新</a-button
        ><a-button @click="handleExportCsv"
          ><template #icon><DownloadOutlined /></template>导出</a-button
        ></template
      >

      <a-alert
        class="section-alert"
        type="info"
        show-icon
        message="开票流程：业务提交申请 → 财务审核 → 审核通过后财务登记发票"
      />

      <a-row :gutter="[16, 16]" class="metric-row">
        <a-col :xs="12" :lg="6"
          ><a-statistic
            title="应收总额（含税，元）"
            :value="totalAmount"
            :formatter="moneyFormatter"
        /></a-col>
        <a-col :xs="12" :lg="6"
          ><a-statistic
            title="待开票（含税，元）"
            :value="invoicePendingAmount"
            :formatter="moneyFormatter"
        /></a-col>
        <a-col :xs="12" :lg="6"
          ><a-statistic
            title="待回款（含税，元）"
            :value="paymentPendingAmount"
            :formatter="moneyFormatter"
        /></a-col>
        <a-col :xs="12" :lg="6"
          ><a-statistic
            title="逾期金额（含税，元）"
            :value="overdueAmount"
            :formatter="moneyFormatter"
        /></a-col>
      </a-row>

      <a-space wrap class="table-toolbar">
        <a-select
          v-model:value="statusFilter"
          allow-clear
          placeholder="全部状态"
          :options="statusOptions"
          style="width: 140px"
        />
        <a-select
          v-model:value="salesFilter"
          allow-clear
          show-search
          option-filter-prop="label"
          placeholder="全部销售人员"
          :options="salesOptions"
          style="width: 170px"
        />
        <a-select
          v-model:value="departmentFilter"
          allow-clear
          show-search
          option-filter-prop="label"
          placeholder="全部部门"
          :options="departmentOptions"
          style="width: 150px"
        />
        <a-input
          v-model:value="keyword"
          allow-clear
          placeholder="搜索应收单、合同或客户"
          style="width: 280px"
        />
      </a-space>

      <a-table
        :columns="receivableColumns"
        :data-source="filteredItems"
        :loading="loading"
        :row-key="(record: Receivable) => record.id"
        :scroll="{ x: 1060 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'receivable'">
            <strong>{{ record.code }}</strong>
            <span class="table-subtitle">{{ record.customerName }}</span>
          </template>
          <template v-else-if="column.key === 'contract'">
            <strong>{{ record.contractName || "未命名合同" }}</strong>
            <span class="table-subtitle"
              >合同编号：{{ record.contractCode || "-" }}</span
            >
          </template>
          <template v-else-if="column.key === 'amount'">
            <strong>{{ formatMoney(record.amount) }}</strong>
            <span class="table-subtitle"
              >已收 {{ formatMoney(record.settledAmount) }} · 待收
              {{ formatMoney(record.outstandingAmount) }}</span
            >
          </template>
          <template v-else-if="column.key === 'invoice'">
            <a-tag :color="invoiceStatusColor(record)">{{
              invoiceStatusLabel(record)
            }}</a-tag>
            <span v-if="record.invoiceDate" class="table-subtitle"
              >{{ record.invoiceNo }} · {{ record.invoiceDate }}</span
            >
            <span
              v-else-if="record.invoiceRequestStatus === 'REJECTED'"
              class="table-subtitle text-danger"
            >
              {{
                record.invoiceReviewComment || "财务已驳回，请修改后重新提交"
              }}
            </span>
            <span v-else-if="record.invoiceRequestedAt" class="table-subtitle">
              {{ record.invoiceRequestedBy || "业务侧" }} ·
              {{ formatDateTime(record.invoiceRequestedAt) }}
            </span>
          </template>
          <template v-else-if="column.key === 'dueDate'"
            ><span :class="{ 'text-danger': record.status === 'OVERDUE' }">{{
              record.dueDate
            }}</span></template
          >
          <template v-else-if="column.key === 'status'"
            ><a-tag :color="receivableStatusColor(record.status)">{{
              receivableStatusLabel(record.status)
            }}</a-tag></template
          >
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a-button
                v-if="
                  auth.can('crm:receivable:view') &&
                  !record.invoiceNo &&
                  (!record.invoiceRequestStatus ||
                    record.invoiceRequestStatus === 'NOT_REQUESTED' ||
                    record.invoiceRequestStatus === 'REJECTED') &&
                  record.status !== 'SETTLED'
                "
                size="small"
                type="link"
                @click="openInvoiceRequest(record)"
              >
                {{
                  record.invoiceRequestStatus === "REJECTED"
                    ? "重新申请"
                    : "申请开票"
                }}
              </a-button>
              <a-tag
                v-else-if="
                  !record.invoiceNo &&
                  record.invoiceRequestStatus === 'PENDING_APPROVAL'
                "
                color="blue"
                >待财务审核</a-tag
              >
              <a-tag
                v-else-if="record.invoiceRequestStatus === 'APPROVED'"
                color="cyan"
                >审核通过，待开票</a-tag
              >
              <a-button
                v-if="record.status !== 'SETTLED'"
                size="small"
                type="link"
                @click="openEdit(record)"
              >
                变更审批
              </a-button>
              <a-button
                v-if="
                  auth.can('crm:receivable:settle') &&
                  record.invoiceNo &&
                  record.outstandingAmount > 0
                "
                size="small"
                @click="openReceiptRequest(record)"
              >
                登记回款
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="invoiceOpen"
      title="申请开票"
      width="480px"
      :confirm-loading="saving"
      @ok="handleInvoiceRequest"
    >
      <p style="margin: 16px 0; color: #595959">
        提交后进入“待财务审核”；审核通过后由财务登记发票号码和开票日期。
      </p>
      <a-descriptions
        v-if="selectedItem"
        bordered
        size="small"
        :column="1"
        class="section-alert"
      >
        <a-descriptions-item label="应收单">{{
          selectedItem.code
        }}</a-descriptions-item>
        <a-descriptions-item label="客户">{{
          selectedItem.customerName
        }}</a-descriptions-item>
        <a-descriptions-item label="合同/项目">{{
          selectedItem.contractName
        }}</a-descriptions-item>
        <a-descriptions-item label="合同编号">{{
          selectedItem.contractCode
        }}</a-descriptions-item>
        <a-descriptions-item label="申请金额（含税，元）">{{
          formatMoney(selectedItem.amount)
        }}</a-descriptions-item>
      </a-descriptions>
      <a-form ref="invoiceFormRef" :model="invoiceForm" layout="vertical">
        <a-form-item label="申请说明（可选）"
          ><a-textarea
            v-model:value="invoiceForm.remark"
            :rows="2"
            placeholder="请简要说明开票需求"
        /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="receiptOpen"
      title="登记回款"
      width="480px"
      :confirm-loading="saving"
      @ok="handleReceiptRequest"
    >
      <a-form ref="receiptFormRef" :model="receiptForm" layout="vertical">
        <a-form-item label="回款金额（元）">
          <a-input-number
            v-model:value="receiptForm.amount"
            :min="0.01"
            class="full-input"
          />
        </a-form-item>
        <a-form-item label="银行流水号">
          <a-input
            v-model:value="receiptForm.referenceNo"
            placeholder="请输入银行流水号"
          />
        </a-form-item>
        <a-form-item label="回款日期">
          <a-input v-model:value="receiptForm.receivedDate" type="date" />
        </a-form-item>
        <a-form-item label="备注（可选）">
          <a-textarea
            v-model:value="receiptForm.remark"
            :rows="2"
            placeholder="请简要说明"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="editOpen"
      title="应收变更审批"
      :confirm-loading="saving"
      @ok="handleEdit"
    >
      <a-alert
        v-if="selectedItem"
        class="section-alert"
        type="info"
        :message="selectedItem.code + ' ' + selectedItem.customerName"
      />
      <a-form ref="editFormRef" :model="editForm" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="12"
            ><a-form-item label="来源单号"
              ><a-input v-model:value="editForm.sourceNo" /></a-form-item
          ></a-col>
          <a-col :xs="24" :md="12"
            ><a-form-item label="应收金额（含税，元）" name="amount"
              ><a-input-number
                v-model:value="editForm.amount"
                :min="0"
                class="full-input" /></a-form-item
          ></a-col>
          <a-col :xs="24" :md="12"
            ><a-form-item label="到期日"
              ><a-input
                v-model:value="editForm.dueDate"
                type="date" /></a-form-item
          ></a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import DownloadOutlined from "@ant-design/icons-vue/DownloadOutlined";
import { message } from "ant-design-vue";
import {
  listReceivables,
  applyReceivableInvoice,
  recordReceivableReceipt,
  updateReceivable,
  type Receivable,
  type ReceivableStatus,
} from "@/api/crm";
import { useAuthStore } from "@/stores/auth";
import { loadOwnerDepartmentMap, ownerDepartment } from "./crm-department";
import {
  formatMoney,
  receivableStatusColor,
  receivableStatusLabel,
} from "./crm-options";
import { downloadCsv, receivableRowToCsv } from "./crm-export";
import { todayLocal } from "@/utils/date";

const auth = useAuthStore();
const items = ref<Receivable[]>([]);
const departmentMap = ref<Map<string, string>>(new Map());
const loading = ref(false);
const saving = ref(false);
const invoiceOpen = ref(false);
const receiptOpen = ref(false);
const selectedItem = ref<Receivable | null>(null);
const invoiceFormRef = ref();
const receiptFormRef = ref();
const keyword = ref("");
const statusFilter = ref<ReceivableStatus>();
const salesFilter = ref<string>();
const departmentFilter = ref<string>();
const invoiceForm = reactive({ remark: "" });

const editOpen = ref(false);
const editFormRef = ref();
const editForm = reactive({ sourceNo: "", amount: 0, dueDate: "" });
const receiptForm = reactive({
  amount: 0,
  receivedDate: todayLocal(),
  referenceNo: "",
  remark: "",
});

const statusOptions = [
  { label: "待开票", value: "INVOICE_PENDING" },
  { label: "待回款", value: "PAYMENT_PENDING" },
  { label: "已核销", value: "SETTLED" },
  { label: "逾期", value: "OVERDUE" },
];
const salesOptions = computed(() => {
  const names = new Set<string>();
  items.value.forEach((item) => {
    if (item.salesOwnerName) names.add(item.salesOwnerName);
  });
  return Array.from(names)
    .sort((a, b) => a.localeCompare(b, "zh-CN"))
    .map((name) => ({ label: name, value: name }));
});
const departmentOptions = computed(() =>
  Array.from(new Set(departmentMap.value.values()))
    .sort((a, b) => a.localeCompare(b, "zh-CN"))
    .map((name) => ({ label: name, value: name })),
);
const receivableColumns = [
  { title: "应收单 / 客户", key: "receivable", width: 220 },
  { title: "合同名称 / 编号", key: "contract", width: 260 },
  { title: "销售负责人", dataIndex: "salesOwnerName", width: 120 },
  { title: "应收 / 回款（含税，元）", key: "amount", width: 280 },
  { title: "开票信息", key: "invoice", width: 180 },
  { title: "到期日", key: "dueDate", width: 130 },
  { title: "状态", key: "status", width: 110 },
  { title: "操作", key: "action", width: 170, fixed: "right" },
];
const filteredItems = computed(() => {
  const term = keyword.value.trim().toLowerCase();
  return items.value.filter((item) => {
    const text =
      `${item.code} ${item.contractCode} ${item.contractName} ${item.customerName}`.toLowerCase();
    return (
      (!statusFilter.value || item.status === statusFilter.value) &&
      (!salesFilter.value || item.salesOwnerName === salesFilter.value) &&
      (!departmentFilter.value ||
        ownerDepartment(item.salesOwnerName, departmentMap.value) ===
          departmentFilter.value) &&
      (!term || text.includes(term))
    );
  });
});
const totalAmount = computed(() =>
  items.value.reduce((sum, item) => sum + Number(item.amount || 0), 0),
);
const invoicePendingAmount = computed(() => sumByStatus("INVOICE_PENDING"));
const paymentPendingAmount = computed(() => sumByStatus("PAYMENT_PENDING"));
const overdueAmount = computed(() => sumByStatus("OVERDUE"));

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    const [rows, deptMap] = await Promise.all([
      listReceivables(),
      loadOwnerDepartmentMap(),
    ]);
    items.value = rows;
    departmentMap.value = deptMap;
  } catch (error) {
    message.error(error instanceof Error ? error.message : "合同应收加载失败");
  } finally {
    loading.value = false;
  }
}

function sumByStatus(status?: ReceivableStatus) {
  return items.value
    .filter((item) => !status || item.status === status)
    .reduce((sum, item) => sum + Number(item.outstandingAmount || 0), 0);
}

function invoiceStatusLabel(item: Receivable) {
  if (item.invoiceNo || item.invoiceRequestStatus === "INVOICED")
    return "已开票";
  return {
    PENDING_APPROVAL: "待财务审核",
    APPROVED: "审核通过，待开票",
    REJECTED: "审核驳回",
    NOT_REQUESTED: "未申请",
  }[item.invoiceRequestStatus || "NOT_REQUESTED"];
}

function invoiceStatusColor(item: Receivable) {
  return {
    INVOICED: "green",
    PENDING_APPROVAL: "blue",
    APPROVED: "cyan",
    REJECTED: "red",
    NOT_REQUESTED: "default",
  }[item.invoiceNo ? "INVOICED" : item.invoiceRequestStatus || "NOT_REQUESTED"];
}

function formatDateTime(value?: string) {
  return value
    ? new Date(value).toLocaleString("zh-CN", { hour12: false })
    : "-";
}

function openInvoiceRequest(record: Receivable) {
  selectedItem.value = record;
  invoiceForm.remark = "";
  invoiceOpen.value = true;
}

async function handleInvoiceRequest() {
  if (!selectedItem.value) return;
  saving.value = true;
  try {
    await applyReceivableInvoice(selectedItem.value.id, {
      applicantName: auth.user?.displayName || "当前用户",
      remark: invoiceForm.remark || undefined,
    });
    invoiceOpen.value = false;
    message.success("开票申请已提交，请等待财务处理");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "提交开票申请失败");
  } finally {
    saving.value = false;
  }
}

function openReceiptRequest(record: Receivable) {
  selectedItem.value = record;
  receiptForm.amount = Number(record.outstandingAmount || 0);
  receiptForm.receivedDate = todayLocal();
  receiptForm.referenceNo = "";
  receiptForm.remark = "";
  receiptOpen.value = true;
}

async function handleReceiptRequest() {
  if (!selectedItem.value) return;
  if (receiptForm.amount <= 0) {
    message.warning("回款金额必须大于 0");
    return;
  }
  if (!receiptForm.referenceNo.trim()) {
    message.warning("请输入银行流水号");
    return;
  }
  saving.value = true;
  try {
    await recordReceivableReceipt(selectedItem.value.id, {
      amount: receiptForm.amount,
      receivedDate: receiptForm.receivedDate,
      referenceNo: receiptForm.referenceNo.trim(),
      recorderName: auth.user?.displayName || auth.user?.username || "",
    });
    receiptOpen.value = false;
    message.success("回款已登记");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "登记回款失败");
  } finally {
    saving.value = false;
  }
}

function openEdit(record: Receivable) {
  selectedItem.value = record;
  Object.assign(editForm, {
    sourceNo: record.sourceNo || "",
    amount: record.amount,
    dueDate: record.dueDate || "",
  });
  editOpen.value = true;
}

async function handleEdit() {
  try {
    await editFormRef.value?.validate();
  } catch {
    return;
  }
  if (!selectedItem.value) return;
  saving.value = true;
  try {
    await updateReceivable(selectedItem.value.id, { ...editForm });
    editOpen.value = false;
    message.success("应收变更已提交审批，通过后自动更新");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "提交变更审批失败");
  } finally {
    saving.value = false;
  }
}

function handleExportCsv() {
  const headers = [
    "应收编号",
    "客户名称",
    "来源单号",
    "应收金额（含税，元）",
    "未收金额（含税，元）",
    "到期日",
    "状态",
    "发票号",
  ];
  const rows = items.value.map((r: any) => receivableRowToCsv(r));
  downloadCsv("合同应收.csv", headers, rows);
}

function today() {
  const value = new Date();
  return new Date(value.getTime() - value.getTimezoneOffset() * 60000)
    .toISOString()
    .slice(0, 10);
}

function moneyFormatter({ value }: { value: number }) {
  return formatMoney(value);
}
</script>
