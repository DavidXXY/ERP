<template>
  <div class="page-stack">
    <a-card title="供应商开票资料" class="submission-card">
      <template #extra>
        <a-button :loading="loading" @click="load">刷新</a-button>
      </template>

      <a-space class="table-toolbar">
        <a-segmented
          v-model:value="submissionFilter"
          :options="submissionFilterOptions"
        />
        <span class="table-count"> {{ filteredSubmissions.length }} 条 </span>
      </a-space>

      <a-table
        :data-source="filteredSubmissions"
        :columns="submissionColumns"
        row-key="id"
        :loading="loading"
        :pagination="{ pageSize: 10 }"
        :scroll="{ x: 1050 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'submission'">
            <strong>{{ record.invoiceNo }}</strong>
            <span class="table-subtitle">{{ record.fileName }}</span>
          </template>
          <template v-else-if="column.key === 'supplier'">
            {{ record.supplierName || "—" }}
          </template>
          <template v-else-if="column.key === 'order'">
            {{ record.orderCode || record.orderId.slice(0, 8) }}
          </template>
          <template v-else-if="column.key === 'amount'">
            <strong>{{ money(record.amount) }}</strong>
            <span class="table-subtitle">{{ record.taxRate }}%</span>
          </template>
          <template v-else-if="column.key === 'file'">
            <a class="download-link" @click.prevent="downloadSubmission(record)"
              >下载附件</a
            >
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="submissionStatusColor(record.status)">
              {{ submissionStatusText(record.status) }}
            </a-tag>
            <span v-if="record.reviewComment" class="table-subtitle">
              {{ record.reviewComment }}
            </span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space v-if="record.status === 'PENDING'">
              <a-button
                v-if="auth.can('procurement:request:approve')"
                type="link"
                @click="openReview(record, 'APPROVED')"
              >
                审核通过
              </a-button>
              <a-button
                v-if="auth.can('procurement:request:approve')"
                type="link"
                danger
                @click="openReview(record, 'REJECTED')"
              >
                退回
              </a-button>
            </a-space>
            <span v-else>—</span>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-card title="采购发票">
      <template #extra>
        <a-button :loading="loading" @click="load">刷新</a-button>
      </template>

      <a-space class="table-toolbar">
        <a-button
          v-if="auth.can('procurement:payable:view')"
          type="primary"
          @click="invoiceOpen = true"
        >
          登记供应商发票
        </a-button>
      </a-space>

      <a-table
        :data-source="invoices"
        :columns="invoiceColumns"
        row-key="id"
        :loading="loading"
        :pagination="{ pageSize: 12 }"
        :scroll="{ x: 1270 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'invoice'">
            <strong>{{ record.invoiceNo }}</strong>
            <span class="table-subtitle">{{ record.code }}</span>
          </template>
          <template v-else-if="column.key === 'order'">
            {{ orderLabel(record.orderId) }}
          </template>
          <template v-else-if="column.key === 'amount'">
            <strong>{{ money(record.amount) }}</strong>
            <span class="table-subtitle">
              已匹配 {{ money(record.matchedAmount) }}
            </span>
          </template>
          <template v-else-if="column.key === 'match'">
            <a-tag :color="record.matchStatus === 'MATCHED' ? 'green' : 'red'">
              {{
                record.matchStatus === "MATCHED"
                  ? "订单·收货·应付·发票一致"
                  : "金额不符"
              }}
            </a-tag>
            <span class="table-subtitle">
              差额 {{ money(record.differenceAmount) }}
            </span>
          </template>
          <template v-else-if="column.key === 'approval'">
            <a-tag
              :color="
                record.approvalStatus === 'APPROVED'
                  ? 'green'
                  : record.approvalStatus === 'REJECTED'
                    ? 'red'
                    : 'orange'
              "
            >
              {{
                record.approvalStatus === "APPROVED"
                  ? "已审核"
                  : record.approvalStatus === "REJECTED"
                    ? "已驳回"
                    : "待审核"
              }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'verify'">
            <a-tag
              :color="
                record.verificationStatus === 'VERIFIED'
                  ? 'blue'
                  : record.verificationStatus === 'EXCEPTION'
                    ? 'volcano'
                    : 'default'
              "
            >
              {{
                record.verificationStatus === "VERIFIED"
                  ? "已验真"
                  : record.verificationStatus === "EXCEPTION"
                    ? "验真异常"
                    : "未验真"
              }}
            </a-tag>
            <span v-if="record.verificationComment" class="table-subtitle">{{
              record.verificationComment
            }}</span>
          </template>
          <template v-else-if="column.key === 'handler'">
            {{ record.handlerName || "-" }}
          </template>
          <template v-else-if="column.key === 'approvedBy'">
            {{ record.approvedByName || "-" }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button
                v-if="
                  record.approvalStatus === 'PENDING' &&
                  record.matchStatus === 'MATCHED' &&
                  auth.can('procurement:request:approve')
                "
                type="link"
                @click="reviewInvoice(record, 'APPROVED')"
              >
                审核通过
              </a-button>
              <a-button
                v-if="
                  record.approvalStatus === 'PENDING' &&
                  auth.can('procurement:request:approve')
                "
                type="link"
                danger
                @click="reviewInvoice(record, 'REJECTED')"
              >
                驳回
              </a-button>
              <a-button
                v-if="
                  record.approvalStatus === 'APPROVED' &&
                  record.verificationStatus !== 'VERIFIED' &&
                  auth.can('procurement:payable:view')
                "
                type="link"
                @click="openVerify(record)"
              >
                {{
                  record.verificationStatus === "EXCEPTION"
                    ? "重新验真"
                    : "验真"
                }}
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="invoiceOpen"
      title="登记供应商发票"
      @ok="saveInvoice"
    >
      <a-form layout="vertical">
        <a-form-item label="采购订单">
          <a-select
            v-model:value="invoiceForm.orderId"
            :options="orderOptions"
          />
        </a-form-item>
        <a-form-item label="关联应付（支持多选合并开票）">
          <a-select
            v-model:value="invoiceForm.payableIds"
            mode="multiple"
            allow-clear
            :options="payableOptions"
            placeholder="可多选同一订单下的多笔应付合并开票"
          />
        </a-form-item>
        <a-form-item label="发票号码">
          <a-input v-model:value="invoiceForm.invoiceNo" />
        </a-form-item>
        <a-form-item label="发票金额（含税，元）">
          <a-input-number
            v-model:value="invoiceForm.amount"
            :min="0.01"
            class="full-input"
          />
        </a-form-item>
        <a-form-item label="税率">
          <a-input-number
            v-model:value="invoiceForm.taxRate"
            :min="0"
            :max="100"
          />
        </a-form-item>
        <a-form-item label="发票日期">
          <a-input v-model:value="invoiceForm.invoiceDate" type="date" />
        </a-form-item>
      </a-form>
    </a-modal>
    <a-modal
      v-model:open="reviewOpen"
      :title="`${reviewTarget ? reviewTarget.invoiceNo : '—'} · ${reviewAction === 'APPROVED' ? '审核通过' : '退回'}`"
      :confirm-loading="reviewing"
      @ok="confirmReview"
    >
      <a-alert
        v-if="reviewAction === 'APPROVED'"
        type="info"
        show-icon
        message="审核通过后，将按订单应付情况完成四单匹配并登记为正式发票，进入采购发票审核流程。"
        style="margin-bottom: 14px"
      />
      <a-form layout="vertical">
        <a-form-item
          :label="reviewAction === 'REJECTED' ? '退回原因' : '审核备注'"
          :required="reviewAction === 'REJECTED'"
        >
          <a-textarea
            v-model:value="reviewComment"
            :rows="3"
            maxlength="500"
            :placeholder="
              reviewAction === 'REJECTED'
                ? '请填写退回原因，将通知供应商'
                : '可选'
            "
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>

  <a-modal
    v-model:open="verifyOpen"
    :title="`${verifyTarget ? verifyTarget.invoiceNo : '—'} · 发票验真`"
    :confirm-loading="verifying"
    @ok="confirmVerify"
  >
    <a-alert
      type="info"
      show-icon
      message="发票验真通常在发票审核通过后进行，可在税务平台核对发票号码、金额与销方信息。"
      style="margin-bottom: 14px"
    />
    <a-form layout="vertical">
      <a-form-item label="验真结果" required>
        <a-radio-group v-model:value="verifyDecision">
          <a-radio value="VERIFIED">验真通过</a-radio>
          <a-radio value="EXCEPTION">验真异常</a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item
        v-if="verifyDecision === 'EXCEPTION'"
        label="异常说明"
        required
      >
        <a-textarea
          v-model:value="verifyComment"
          :rows="2"
          maxlength="500"
          placeholder="说明发票验真异常的原因"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import { useAuthStore } from "@/stores/auth";
import * as api from "@/api/procurement";

const auth = useAuthStore();
const loading = ref(false);
const invoiceOpen = ref(false);
const invoices = ref<api.SupplierInvoice[]>([]);
const orders = ref<api.PurchaseOrder[]>([]);
const payables = ref<api.ProcurementPayable[]>([]);
const submissions = ref<api.InvoiceSubmission[]>([]);
const submissionFilter = ref("PENDING");
const submissionFilterOptions = [
  { label: "待审核", value: "PENDING" },
  { label: "已登记", value: "APPROVED" },
  { label: "已退回", value: "REJECTED" },
  { label: "全部", value: "ALL" },
];
const filteredSubmissions = computed(() =>
  submissionFilter.value === "ALL"
    ? submissions.value
    : submissions.value.filter(
        (item) => item.status === submissionFilter.value,
      ),
);
const reviewOpen = ref(false);
const reviewing = ref(false);
const reviewTarget = ref<api.InvoiceSubmission>();
const reviewAction = ref<"APPROVED" | "REJECTED">("APPROVED");
const reviewComment = ref("");
const verifyOpen = ref(false);
const verifying = ref(false);
const verifyTarget = ref<api.SupplierInvoice>();
const verifyDecision = ref<"VERIFIED" | "EXCEPTION">("VERIFIED");
const verifyComment = ref("");

const today = () => new Date().toISOString().slice(0, 10);
const invoiceForm = reactive({
  orderId: "",
  payableIds: [] as string[],
  invoiceNo: "",
  amount: 0,
  taxRate: 13,
  invoiceDate: today(),
  clientRequestId: "",
});
const submissionColumns = [
  { title: "发票", key: "submission", width: 200 },
  { title: "供应商", key: "supplier", width: 160 },
  { title: "采购订单", key: "order", width: 180 },
  { title: "金额（含税）", key: "amount", width: 140 },
  { title: "开票日期", dataIndex: "invoiceDate", width: 110 },
  { title: "附件", key: "file", width: 100 },
  { title: "状态", key: "status", width: 150 },
  { title: "提交时间", dataIndex: "createdAt", width: 110 },
  { title: "操作", key: "action", width: 170, fixed: "right" as const },
];

const invoiceColumns = [
  { title: "发票", key: "invoice", width: 190 },
  { title: "采购订单", key: "order", width: 210 },
  { title: "发票金额（含税，元）", key: "amount", width: 190 },
  { title: "开票日期", dataIndex: "invoiceDate", width: 120 },
  { title: "经办人", key: "handler", width: 110 },
  { title: "审核人", key: "approvedBy", width: 110 },
  { title: "四单匹配", key: "match", width: 220 },
  { title: "审核", key: "approval", width: 100 },
  { title: "验真", key: "verify", width: 120 },
  { title: "操作", key: "action", width: 230, fixed: "right" as const },
];
const orderOptions = computed(() =>
  orders.value
    .filter((item) => item.approvalStatus === "APPROVED")
    .map((item) => ({
      label: `${item.code} · ${item.supplierName}`,
      value: item.id,
    })),
);
const payableOptions = computed(() =>
  payables.value
    .filter(
      (item) => !invoiceForm.orderId || item.orderId === invoiceForm.orderId,
    )
    .map((item) => ({
      label: `${item.code} · 待付 ${money(item.outstandingAmount)}`,
      value: item.id,
    })),
);

onMounted(load);

async function load() {
  loading.value = true;
  try {
    const [invoiceResult, orderResult, payableResult, submissionResult] =
      await Promise.all([
        api.listSupplierInvoices(),
        api.listPurchaseOrders({ page: 0, size: 999 }),
        api.listProcurementPayables(),
        api.listInvoiceSubmissions(),
      ]);
    invoices.value = invoiceResult;
    orders.value = orderResult.content;
    payables.value = payableResult;
    submissions.value = submissionResult;
  } catch (error) {
    message.error(error instanceof Error ? error.message : "加载失败");
  } finally {
    loading.value = false;
  }
}

async function saveInvoice() {
  if (
    !invoiceForm.orderId ||
    !invoiceForm.invoiceNo.trim() ||
    Number(invoiceForm.amount) <= 0
  ) {
    message.warning("请完整填写采购订单、发票号码和发票金额");
    return;
  }
  invoiceForm.clientRequestId = `invoice-${Date.now()}`;
  await api.createSupplierInvoice({
    orderId: invoiceForm.orderId,
    invoiceNo: invoiceForm.invoiceNo,
    amount: invoiceForm.amount,
    taxRate: invoiceForm.taxRate,
    invoiceDate: invoiceForm.invoiceDate,
    payableIds:
      invoiceForm.payableIds.length > 0 ? invoiceForm.payableIds : undefined,
    clientRequestId: invoiceForm.clientRequestId,
  });
  invoiceForm.payableIds = [];
  invoiceOpen.value = false;
  message.success("发票已登记，等待审核");
  await load();
}

async function reviewInvoice(
  invoice: api.SupplierInvoice,
  decision: "APPROVED" | "REJECTED",
) {
  await api.reviewSupplierInvoice(invoice.id, {
    decision,
    reviewerName: auth.user?.displayName || "审核人",
    comment: decision === "APPROVED" ? "四单匹配审核通过" : "发票审核驳回",
  });
  message.success(decision === "APPROVED" ? "发票审核通过" : "发票已驳回");
  await load();
}

function submissionStatusText(value: string) {
  return (
    { PENDING: "待审核", APPROVED: "已登记", REJECTED: "已退回" }[value] ||
    value ||
    "—"
  );
}
function submissionStatusColor(value: string) {
  return (
    { PENDING: "orange", APPROVED: "green", REJECTED: "red" }[value] ||
    "default"
  );
}
function openReview(
  submission: api.InvoiceSubmission,
  action: "APPROVED" | "REJECTED",
) {
  reviewTarget.value = submission;
  reviewAction.value = action;
  reviewComment.value = "";
  reviewOpen.value = true;
}
async function confirmReview() {
  if (!reviewTarget.value) return;
  if (reviewAction.value === "REJECTED" && !reviewComment.value.trim()) {
    message.warning("请填写退回原因");
    return;
  }
  reviewing.value = true;
  try {
    await api.reviewInvoiceSubmission(reviewTarget.value.id, {
      action: reviewAction.value,
      comment: reviewComment.value.trim() || undefined,
    });
    reviewOpen.value = false;
    message.success(
      reviewAction.value === "APPROVED"
        ? "已通过并登记为正式发票"
        : "已退回给供应商",
    );
    await load();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "审核失败");
  } finally {
    reviewing.value = false;
  }
}
function downloadSubmission(submission: api.InvoiceSubmission) {
  window.location.href = api.invoiceSubmissionDownloadUrl(submission.id);
}
function orderLabel(orderId: string) {
  const order = orders.value.find((item) => item.id === orderId);
  return order ? `${order.code} · ${order.supplierName}` : orderId.slice(0, 8);
}

function openVerify(invoice: api.SupplierInvoice) {
  verifyTarget.value = invoice;
  verifyDecision.value = "VERIFIED";
  verifyComment.value = "";
  verifyOpen.value = true;
}
async function confirmVerify() {
  if (!verifyTarget.value) return;
  if (verifyDecision.value === "EXCEPTION" && !verifyComment.value.trim()) {
    message.warning("请填写验真异常说明");
    return;
  }
  verifying.value = true;
  try {
    await api.verifySupplierInvoice(verifyTarget.value.id, {
      decision: verifyDecision.value,
      comment:
        verifyDecision.value === "EXCEPTION"
          ? verifyComment.value.trim()
          : verifyComment.value.trim() || undefined,
    });
    verifyOpen.value = false;
    message.success(
      verifyDecision.value === "VERIFIED"
        ? "发票验真通过"
        : "发票验真异常已登记",
    );
    await load();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "验真失败");
  } finally {
    verifying.value = false;
  }
}

function money(value: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
  }).format(Number(value || 0));
}
</script>
