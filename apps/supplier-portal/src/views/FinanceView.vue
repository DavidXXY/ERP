<template>
  <div class="page-shell">
    <header class="page-heading">
      <div>
        <p class="eyebrow">财务协作</p>
        <h1>开票与对账</h1>
        <p>查看采购方登记的发票、应付与付款进度，便于对账与回款跟进。</p>
      </div>
      <div class="page-actions">
        <a-button type="primary" ghost :loading="loading" @click="load"
          ><ReloadOutlined /> 刷新</a-button
        >
        <a-button ghost @click="exportExcel"
          ><FileExcelOutlined /> 导出对账 Excel</a-button
        >
      </div>
    </header>

    <section class="finance-summary">
      <article class="finance-card">
        <small>累计开票</small>
        <strong>{{ money(summary.invoiceAmount) }}</strong>
        <span>{{ summary.invoiceCount }} 张发票</span>
      </article>
      <article class="finance-card">
        <small>应付总额</small>
        <strong>{{ money(summary.payableAmount) }}</strong>
        <span>{{ summary.payableCount }} 笔应付</span>
      </article>
      <article class="finance-card">
        <small>已付金额</small>
        <strong class="paid">{{ money(summary.paidAmount) }}</strong>
        <span>采购方已付款</span>
      </article>
      <article class="finance-card">
        <small>待付金额</small>
        <strong>{{ money(summary.outstandingAmount) }}</strong>
        <span>尚未到账</span>
      </article>
      <article class="finance-card">
        <small>逾期未付</small>
        <strong class="overdue">{{ money(summary.overdueAmount) }}</strong>
        <span>超过到期日</span>
      </article>
    </section>

    <a-card :bordered="false" class="finance-block" :loading="loading">
      <div class="section-title">
        <div>
          <h2>发票记录</h2>
          <p>采购方登记并核验的供应商发票。</p>
        </div>
      </div>
      <a-table
        v-if="invoices.length > 0"
        size="small"
        row-key="id"
        :data-source="invoices"
        :columns="invoiceColumns"
        :pagination="{ pageSize: 10 }"
        :row-class-name="rowClassName"
      />
      <a-empty
        v-else
        :image="Empty.PRESENTED_IMAGE_SIMPLE"
        description="暂无发票记录"
      />
    </a-card>

    <a-card :bordered="false" class="finance-block" :loading="loading">
      <div class="section-title">
        <div>
          <h2>应付与付款</h2>
          <p>采购方按收货生成的应付及付款进度。</p>
        </div>
      </div>
      <a-table
        v-if="payables.length > 0"
        size="small"
        row-key="id"
        :data-source="payables"
        :columns="payableColumns"
        :pagination="{ pageSize: 10 }"
        :row-class-name="rowClassName"
      />
      <a-empty
        v-else
        :image="Empty.PRESENTED_IMAGE_SIMPLE"
        description="暂无应付记录"
      />
    </a-card>
    <a-modal
      v-model:open="uploadOpen"
      title="上传开票资料"
      :confirm-loading="uploading"
      @ok="submitUpload"
    >
      <a-form layout="vertical">
        <a-form-item label="采购订单" required>
          <a-select
            v-model:value="uploadForm.orderId"
            placeholder="选择要开票的订单"
            :options="orderOptions"
            show-search
            option-filter-prop="label"
          />
        </a-form-item>
        <a-form-item label="发票号码" required>
          <a-input
            v-model:value="uploadForm.invoiceNo"
            maxlength="100"
            placeholder="如 FP20260812001"
          />
        </a-form-item>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="含税金额" required>
              <a-input-number
                v-model:value="uploadForm.amount"
                :min="0.01"
                :precision="2"
                style="width: 100%"
                placeholder="发票含税金额"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="税率(%)" required>
              <a-input-number
                v-model:value="uploadForm.taxRate"
                :min="0"
                :max="100"
                :precision="2"
                style="width: 100%"
                placeholder="如 13"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="开票日期" required>
          <a-input v-model:value="uploadForm.invoiceDate" type="date" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="uploadForm.remark" :rows="2" maxlength="500" />
        </a-form-item>
        <a-form-item label="发票文件" required>
          <a-upload
            :before-upload="selectFile"
            :file-list="fileList"
            :max-count="1"
          >
            <a-button><PaperClipOutlined /> 选择发票文件</a-button>
          </a-upload>
          <small class="upload-hint"
            >支持图片、PDF、Word、Excel，不超过 20MB</small
          >
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, h, nextTick, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { message } from "ant-design-vue";
import type { UploadFile } from "ant-design-vue";
import {
  DeleteOutlined,
  DownloadOutlined,
  FileExcelOutlined,
  PaperClipOutlined,
  PlusOutlined,
  ReloadOutlined,
  UploadOutlined,
} from "@ant-design/icons-vue";
import { Empty, Popconfirm as APopconfirm, Tag as ATag } from "ant-design-vue";
import * as api from "../api";
import {
  fileSize,
  formatDate,
  money,
  validateUploadFile,
} from "../utils/quote";

const loading = ref(false);
const invoices = ref<api.PortalInvoice[]>([]);
const payables = ref<api.PortalPayable[]>([]);
const submissions = ref<api.InvoiceSubmission[]>([]);
const orders = ref<api.PortalOrder[]>([]);
const uploadOpen = ref(false);
const uploading = ref(false);
const selectedFile = ref<File>();
const fileList = ref<UploadFile[]>([]);
const uploadForm = reactive({
  orderId: undefined as string | undefined,
  invoiceNo: "",
  amount: undefined as number | undefined,
  taxRate: undefined as number | undefined,
  invoiceDate: "",
  remark: "",
});
const orderOptions = computed(() =>
  orders.value.map((order) => ({
    label: `${order.code} · ${order.partName} · ${money(order.orderAmount)}`,
    value: order.id,
  })),
);
const route = useRoute();
const router = useRouter();
const highlightId = ref("");
const summary = ref<api.FinanceSummary>({
  invoiceCount: 0,
  invoiceAmount: 0,
  payableCount: 0,
  payableAmount: 0,
  paidAmount: 0,
  outstandingAmount: 0,
  overdueAmount: 0,
});

function invoiceStatusText(value: string) {
  return (
    {
      REGISTERED: "已登记",
      VERIFIED: "已核验",
      APPROVED: "已审批",
      REJECTED: "已驳回",
    } as Record<string, string>
  )[value] || value || "—";
}
function approvalText(value: string) {
  return { PENDING: "待审批", APPROVED: "已通过", REJECTED: "已驳回" }[
    value
  ] || value || "—";
}
function matchText(value?: string) {
  return value === "MATCHED"
    ? "已匹配"
    : value === "MISMATCH"
      ? "存在差异"
      : value || "—";
}
function payableStatusText(value: string) {
  return (
    {
      PENDING: "待付款",
      PARTIAL_PAID: "部分付款",
      PAID: "已付款",
      CANCELLED: "已取消",
    } as Record<string, string>
  )[value] || value || "—";
}
function submissionStatusText(value: string) {
  return (
    { PENDING: "待审核", APPROVED: "已登记", REJECTED: "已退回" } as Record<
      string,
      string
    >
  )[value] || value || "—";
}
function submissionStatusColor(value: string) {
  return { PENDING: "orange", APPROVED: "green", REJECTED: "red" }[value] || "default";
}

const submissionColumns = [
  { title: "发票号", dataIndex: "invoiceNo" },
  { title: "订单", dataIndex: "orderCode" },
  {
    title: "开票日期",
    dataIndex: "invoiceDate",
    customRender: ({ text }: { text?: string }) => formatDate(text),
  },
  {
    title: "金额",
    dataIndex: "amount",
    customRender: ({ text }: { text: number }) => money(text),
  },
  {
    title: "税率",
    dataIndex: "taxRate",
    customRender: ({ text }: { text?: number }) =>
      text == null ? "—" : `${text}%`,
  },
  {
    title: "状态",
    dataIndex: "status",
    customRender: ({ record }: { record: api.InvoiceSubmission }) =>
      h(ATag, { color: submissionStatusColor(record.status) }, () =>
        submissionStatusText(record.status),
      ),
  },
  {
    title: "审核意见",
    dataIndex: "reviewComment",
    customRender: ({ text }: { text?: string }) => text || "—",
  },
  {
    title: "附件",
    dataIndex: "fileName",
    customRender: ({ record }: { record: api.InvoiceSubmission }) =>
      h(
        "a",
        {
          class: "download-link",
          onClick: () => downloadSubmission(record),
        },
        () => record.fileName,
      ),
  },
  {
    title: "提交时间",
    dataIndex: "createdAt",
    customRender: ({ text }: { text?: string }) => formatDate(text),
  },
  {
    title: "操作",
    key: "actions",
    customRender: ({ record }: { record: api.InvoiceSubmission }) =>
      record.status === "PENDING"
        ? h(
            APopconfirm,
            {
              title: "确认删除该开票资料？",
              onConfirm: () => removeSubmission(record.id),
            },
            {
              default: () => h("a", { class: "danger-link" }, "删除"),
            },
          )
        : "—",
  },
];

const invoiceColumns = [

  { title: "发票号", dataIndex: "invoiceNo" },
  { title: "订单", dataIndex: "orderCode" },
  { title: "开票日期", dataIndex: "invoiceDate", customRender: ({ text }: { text?: string }) => formatDate(text) },
  { title: "金额", dataIndex: "amount", customRender: ({ text }: { text: number }) => money(text) },
  {
    title: "状态",
    dataIndex: "status",
    customRender: ({ record }: { record: api.PortalInvoice }) =>
      `${invoiceStatusText(record.status)} · ${approvalText(record.approvalStatus)} · ${matchText(record.matchStatus)}`,
  },
];

const payableColumns = [
  { title: "应付单号", dataIndex: "code" },
  { title: "订单", dataIndex: "orderCode" },
  { title: "应付金额", dataIndex: "amount", customRender: ({ text }: { text: number }) => money(text) },
  { title: "已付金额", dataIndex: "paidAmount", customRender: ({ text }: { text: number }) => money(text) },
  { title: "待付金额", dataIndex: "outstandingAmount", customRender: ({ text }: { text: number }) => money(text) },
  { title: "到期日", dataIndex: "dueDate", customRender: ({ text }: { text?: string }) => formatDate(text) },
  {
    title: "付款回单",
    key: "receipt",
    customRender: ({ record }: { record: api.PortalPayable }) =>
      record.paymentReceiptFileName
        ? h(
            "a",
            {
              class: "download-link",
              onClick: () => downloadPaymentReceipt(record),
            },
            () => record.paymentReceiptFileName,
          )
        : record.paidAmount > 0
          ? h("span", { class: "table-subtitle" }, () => "未上传")
          : h("span", { class: "table-subtitle" }, () => "—"),
  },
  {
    title: "状态",
    dataIndex: "status",
    customRender: ({ text }: { text: string }) => payableStatusText(text),
  },
];

onMounted(load);
async function load() {
  loading.value = true;
  try {
    const [invoiceList, payableList, sum, submissionList, orderList] =
      await Promise.all([
        api.listInvoices(),
        api.listPayables(),
        api.getFinanceSummary(),
        api.listInvoiceSubmissions(),
        api.listOrders(),
      ]);
    invoices.value = invoiceList;
    payables.value = payableList;
    summary.value = sum;
    submissions.value = submissionList;
    orders.value = orderList
      .map((entry) => entry.order)
      .filter((order): order is api.PortalOrder => Boolean(order));
    const invoiceId = typeof route.query.invoice === "string" ? route.query.invoice : undefined;
    const payableId = typeof route.query.payable === "string" ? route.query.payable : undefined;
    const target = invoiceId
      ? invoiceList.find((item) => item.id === invoiceId)
      : payableId
        ? payableList.find((item) => item.id === payableId)
        : undefined;
    if (target) {
      highlightId.value = target.id;
      await nextTick();
      document
        .querySelector(`tr.ops-row-highlight`)
        ?.scrollIntoView({ block: "center", behavior: "smooth" });
      setTimeout(() => (highlightId.value = ""), 3200);
    }
    if (invoiceId || payableId) await router.replace({ path: "/finance" });
  } catch (e) {
    message.error(e instanceof Error ? e.message : "加载失败");
  } finally {
    loading.value = false;
  }
}

function rowClassName(record: { id: string }) {
  return record.id === highlightId.value ? "ops-row-highlight" : "";
}

function openUpload() {
  uploadForm.orderId = undefined;
  uploadForm.invoiceNo = "";
  uploadForm.amount = undefined;
  uploadForm.taxRate = undefined;
  uploadForm.invoiceDate = "";
  uploadForm.remark = "";
  selectedFile.value = undefined;
  fileList.value = [];
  uploadOpen.value = true;
}
function selectFile(file: File) {
  const invalid = validateUploadFile(file);
  if (invalid) {
    message.warning(invalid);
    return false;
  }
  selectedFile.value = file;
  fileList.value = [
    { uid: file.name, name: file.name, status: "done", originFileObj: file } as UploadFile,
  ];
  return false;
}
async function submitUpload() {
  if (!uploadForm.orderId) {
    message.warning("请选择采购订单");
    return;
  }
  if (!uploadForm.invoiceNo.trim()) {
    message.warning("请填写发票号码");
    return;
  }
  if (uploadForm.amount == null || uploadForm.amount <= 0) {
    message.warning("请填写正确的含税金额");
    return;
  }
  if (uploadForm.taxRate == null) {
    message.warning("请填写税率");
    return;
  }
  if (!uploadForm.invoiceDate) {
    message.warning("请选择开票日期");
    return;
  }
  if (!selectedFile.value) {
    message.warning("请选择发票文件");
    return;
  }
  uploading.value = true;
  try {
    const data = new FormData();
    data.append(
      "metadata",
      new Blob(
        [
          JSON.stringify({
            orderId: uploadForm.orderId,
            invoiceNo: uploadForm.invoiceNo.trim(),
            amount: uploadForm.amount,
            taxRate: uploadForm.taxRate,
            invoiceDate: uploadForm.invoiceDate,
            remark: uploadForm.remark.trim() || undefined,
          }),
        ],
        { type: "application/json" },
      ),
    );
    data.append("file", selectedFile.value);
    await api.uploadInvoiceSubmission(data);
    uploadOpen.value = false;
    selectedFile.value = undefined;
    fileList.value = [];
    await load();
    message.success("开票资料已提交，等待采购方审核");
  } catch (e) {
    message.error(e instanceof Error ? e.message : "上传失败");
  } finally {
    uploading.value = false;
  }
}
function downloadSubmission(item: api.InvoiceSubmission) {
  window.location.href = api.invoiceSubmissionDownloadUrl(item.id);
}
function downloadPaymentReceipt(item: api.PortalPayable) {
  window.location.href = api.paymentReceiptDownloadUrl(item.id);
}
async function removeSubmission(id: string) {
  try {
    await api.deleteInvoiceSubmission(id);
    await load();
    message.success("开票资料已删除");
  } catch (e) {
    message.error(e instanceof Error ? e.message : "删除失败");
  }
}
function exportExcel() {
  window.location.href = api.financeExcelUrl();
}
</script>

<style scoped>
.page-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.finance-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}
.finance-card {
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  padding: 14px 16px;
  background: #fafafa;
}
.finance-card small {
  display: block;
  color: #8c8c8c;
}
.finance-card strong {
  display: block;
  font-size: 22px;
  margin: 4px 0;
}
.finance-card strong.paid {
  color: #1f5c46;
}
.finance-card strong.overdue {
  color: #cf1322;
}
.finance-card span {
  color: #595959;
  font-size: 12px;
}
.finance-block {
  border-radius: 10px;
  margin-bottom: 16px;
}
.upload-hint {
  color: #8c8c8c;
  font-size: 12px;
}
.download-link {
  color: #1677ff;
  cursor: pointer;
}
.danger-link {
  color: #ff4d4f;
  cursor: pointer;
}
:deep(.ops-row-highlight td) {
  background: #fffbe6 !important;
  transition: background 0.4s;
}
@media (max-width: 760px) {
  .finance-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }
  .finance-card {
    padding: 12px;
  }
  .finance-card strong {
    font-size: 18px;
  }
}
@media (max-width: 420px) {
  .finance-summary {
    grid-template-columns: 1fr;
  }
}
</style>
