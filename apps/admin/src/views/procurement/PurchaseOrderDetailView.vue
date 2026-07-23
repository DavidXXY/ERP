<template>
  <BusinessDetailPage
    :title="
      order ? `${order.supplierName || '供应商'}采购订单` : '采购订单详情'
    "
    :code="order?.code"
    :subtitle="order ? `${order.partName} · ${order.costTargetName}` : ''"
    :loading="loading"
    back-to="/procurement/orders"
    :status-label="statusLabel(order?.status)"
    :status-color="
      order?.status === 'CLOSED' || order?.status === 'RECEIVED'
        ? 'green'
        : order?.status === 'CANCELLED'
          ? 'red'
          : 'blue'
    "
    :risk-label="riskLabel"
    :risk-color="riskLabel ? 'orange' : 'green'"
    :stats="metrics"
    @refresh="loadData"
  >
    <template #actions>
      <a-button
        v-if="order?.supplierId"
        @click="router.push(`/procurement/suppliers/${order.supplierId}`)"
        >查看供应商</a-button
      >
      <a-button
        v-if="
          order?.status === 'DRAFT' && auth.can('procurement:purchase:create')
        "
        type="primary"
        :loading="saving"
        @click="handleSubmit"
        >提交订单审批</a-button
      >
      <a-button
        v-if="
          order?.approvalStatus === 'PENDING' &&
          auth.can('procurement:purchase:approve')
        "
        type="primary"
        @click="approvalOpen = true"
        >审批订单</a-button
      >
    </template>
    <template #relations>
      <a-steps size="small" :current="currentStep" responsive>
        <a-step
          title="采购申请"
          :description="order?.requestCode || '未关联'"
        />
        <a-step title="询价定标" :description="inquiry?.code || '直接采购'" />
        <a-step
          title="订单审批"
          :description="approvalLabel(order?.approvalStatus)"
        />
        <a-step title="到货质检" :description="`${receipts.length} 笔到货`" />
        <a-step title="发票应付" :description="`${invoices.length} 张发票`" />
        <a-step title="付款核销" :description="money(paidAmount)" />
      </a-steps>
    </template>

    <a-card v-if="order" :bordered="false">
      <a-tabs>
        <a-tab-pane key="overview" tab="订单总览">
          <a-row :gutter="[16, 16]">
            <a-col :xs="24" :xl="14">
              <a-descriptions
                bordered
                :column="{ xs: 1, md: 2 }"
                size="small"
                title="订单信息"
              >
                <a-descriptions-item label="供应商"
                  ><a
                    @click="
                      router.push(`/procurement/suppliers/${order.supplierId}`)
                    "
                    >{{ order.supplierName || "-" }}</a
                  ></a-descriptions-item
                >
                <a-descriptions-item label="采购申请">{{
                  order.requestCode || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="物料">{{
                  order.partName
                }}</a-descriptions-item>
                <a-descriptions-item label="成本归属"
                  >{{ order.costTargetName }}（{{
                    order.costTargetCode
                  }}）</a-descriptions-item
                >
                <a-descriptions-item label="订购/已收"
                  >{{ order.orderedQty }} /
                  {{ order.receivedQty }}</a-descriptions-item
                >
                <a-descriptions-item label="预计交付">{{
                  order.expectedDeliveryDate || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="单价">{{
                  money(order.unitPrice)
                }}</a-descriptions-item>
                <a-descriptions-item label="税率"
                  >{{ Number(order.taxRate || 0) }}%</a-descriptions-item
                >
                <a-descriptions-item label="订单金额" :span="2"
                  ><strong>{{
                    money(order.orderAmount)
                  }}</strong></a-descriptions-item
                >
                <a-descriptions-item label="订单版本">V{{ order.orderVersion || 1 }}</a-descriptions-item>
                <a-descriptions-item label="币种">{{ order.currency || "CNY" }}</a-descriptions-item>
                <a-descriptions-item label="运费">{{ money(order.freightAmount || 0) }}</a-descriptions-item>
                <a-descriptions-item label="询价单">{{ order.inquiryId || "未关联" }}</a-descriptions-item>
                <a-descriptions-item label="采购合同">{{ order.contractId || "未关联" }}</a-descriptions-item>
                <a-descriptions-item label="寻源/直接采购依据" :span="2">{{ order.sourceReason || "-" }}</a-descriptions-item>
              </a-descriptions>
            </a-col>
            <a-col :xs="24" :xl="10">
              <a-descriptions
                bordered
                :column="1"
                size="small"
                title="审批与履约"
              >
                <a-descriptions-item label="审批状态">{{
                  approvalLabel(order.approvalStatus)
                }}</a-descriptions-item>
                <a-descriptions-item label="审批人">{{
                  order.approverName || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="审批时间">{{
                  dateTime(order.approvedAt)
                }}</a-descriptions-item>
                <a-descriptions-item label="审批意见">{{
                  order.approvalComment || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="订单状态">{{
                  statusLabel(order.status)
                }}</a-descriptions-item>
              </a-descriptions>
            </a-col>
          </a-row>
          <a-alert
            v-if="riskLabel"
            class="section-gap"
            type="warning"
            show-icon
            :message="riskLabel"
            :description="riskDescription"
          />
        </a-tab-pane>

        <a-tab-pane key="source" tab="申请与询价定标">
          <a-descriptions
            v-if="request"
            bordered
            :column="{ xs: 1, md: 2 }"
            size="small"
            title="采购申请"
          >
            <a-descriptions-item label="申请编号">{{
              request.code
            }}</a-descriptions-item
            ><a-descriptions-item label="申请人">{{
              request.applicantName || request.requesterName
            }}</a-descriptions-item>
            <a-descriptions-item label="物料"
              >{{ request.materialName || request.partName }}
              {{ request.materialSpec }}</a-descriptions-item
            ><a-descriptions-item label="数量"
              >{{ request.quantity }}
              {{ request.unit || "" }}</a-descriptions-item
            >
            <a-descriptions-item label="需求日期">{{
              request.requiredDate || request.expectedDate || "-"
            }}</a-descriptions-item
            ><a-descriptions-item label="成本归属">{{
              request.costTargetName
            }}</a-descriptions-item>
            <a-descriptions-item label="申请原因" :span="2">{{
              request.reason || request.description || "-"
            }}</a-descriptions-item>
          </a-descriptions>
          <a-empty v-else description="未找到关联采购申请" />
          <a-divider>询价与供应商报价</a-divider>
          <a-table
            :columns="quotationColumns"
            :data-source="inquiry?.quotes || []"
            row-key="id"
            :pagination="false"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'supplier'"
                ><strong>{{ record.supplierName }}</strong
                ><a-tag v-if="record.selected" color="green"
                  >已中选</a-tag
                ></template
              >
              <template v-else-if="column.key === 'price'">{{
                money(record.unitPrice)
              }}</template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="delivery" :tab="`到货质检 (${receipts.length})`">
          <a-table
            :columns="receiptColumns"
            :data-source="receipts"
            row-key="id"
            :scroll="{ x: 1050 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'amount'">{{
                money(record.amount)
              }}</template>
              <template v-else-if="column.key === 'inspection'"
                ><a-tag
                  :color="
                    record.inspectionStatus === 'PASSED'
                      ? 'green'
                      : record.inspectionStatus === 'REJECTED'
                        ? 'red'
                        : 'orange'
                  "
                  >{{ inspectionLabel(record.inspectionStatus) }}</a-tag
                ><span class="sub"
                  >合格 {{ record.qualifiedQty || 0 }} · 不合格
                  {{ record.rejectedQty || 0 }}</span
                ></template
              >
            </template>
          </a-table>
          <a-divider>退货记录</a-divider>
          <a-table
            :columns="returnColumns"
            :data-source="returns"
            row-key="id"
            size="small"
            :pagination="false"
            ><template #bodyCell="{ column, record }"
              ><template v-if="column.key === 'amount'">{{
                money(record.amount)
              }}</template></template
            ></a-table
          >
        </a-tab-pane>

        <a-tab-pane key="invoice" :tab="`发票与三单匹配 (${invoices.length})`">
          <a-table
            :columns="invoiceColumns"
            :data-source="invoices"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'amount'">{{
                money(record.amount)
              }}</template>
              <template v-else-if="column.key === 'difference'"
                ><span :class="{ danger: record.differenceAmount }">{{
                  money(record.differenceAmount)
                }}</span></template
              >
              <template v-else-if="column.key === 'match'"
                ><a-tag
                  :color="record.matchStatus === 'MATCHED' ? 'green' : 'red'"
                  >{{
                    record.matchStatus === "MATCHED" ? "匹配" : "差异"
                  }}</a-tag
                ></template
              >
            </template>
          </a-table>
          <a-result
            :status="
              matching?.matchStatus === 'MATCHED' ? 'success' : 'warning'
            "
            :title="
              matching?.matchStatus === 'MATCHED'
                ? '订单、收货、应付三单匹配'
                : '存在数量或金额差异'
            "
            :sub-title="matching?.riskMessage || '等待业务数据形成完整匹配结果'"
          />
        </a-tab-pane>

        <a-tab-pane
          key="payment"
          :tab="`应付付款 (${payables.length}/${applications.length})`"
        >
          <a-table
            :columns="payableColumns"
            :data-source="payables"
            row-key="id"
            :pagination="false"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'code'"
                ><a @click="router.push(`/finance/payables/${record.id}`)">{{
                  record.code || record.orderCode
                }}</a></template
              >
              <template
                v-else-if="
                  ['amount', 'paid', 'outstanding'].includes(column.key)
                "
                >{{
                  money(
                    column.key === "amount"
                      ? record.amount
                      : column.key === "paid"
                        ? record.paidAmount
                        : record.outstandingAmount,
                  )
                }}</template
              >
            </template>
          </a-table>
          <a-divider>付款申请</a-divider>
          <a-table
            :columns="applicationColumns"
            :data-source="applications"
            row-key="id"
            size="small"
          >
            <template #bodyCell="{ column, record }"
              ><template v-if="column.key === 'code'"
                ><a
                  @click="
                    router.push(`/finance/payment-applications/${record.id}`)
                  "
                  >{{ record.code }}</a
                ></template
              ><template v-else-if="column.key === 'amount'">{{
                money(record.requestedAmount)
              }}</template></template
            >
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="cost" :tab="`成本归集 (${costs.length})`">
          <a-table
            :columns="costColumns"
            :data-source="costs"
            row-key="id"
            :pagination="false"
          >
            <template #bodyCell="{ column, record }"
              ><template v-if="column.key === 'amount'"
                ><strong>{{ money(record.amount) }}</strong></template
              ></template
            >
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="audit" tab="审批与审计">
          <a-timeline>
            <a-timeline-item color="green"
              ><strong>采购订单创建</strong>
              <p>
                {{ dateTime(order.createdAt || order.orderedAt) }} ·
                {{ order.code }}
              </p></a-timeline-item
            >
            <a-timeline-item
              :color="
                order.approvalStatus === 'APPROVED'
                  ? 'green'
                  : order.approvalStatus === 'REJECTED'
                    ? 'red'
                    : 'blue'
              "
              ><strong
                >订单审批：{{ approvalLabel(order.approvalStatus) }}</strong
              >
              <p>
                {{ order.approverName || "等待审批" }} ·
                {{ order.approvalComment || "暂无意见" }}
              </p></a-timeline-item
            >
            <a-timeline-item
              v-for="receipt in receipts"
              :key="receipt.id"
              :color="
                receipt.inspectionStatus === 'PASSED' ? 'green' : 'orange'
              "
              ><strong
                >{{ receipt.code }} 到货并{{
                  inspectionLabel(receipt.inspectionStatus)
                }}</strong
              >
              <p>
                {{ receipt.receiverName }} · {{ receipt.receivedDate }} · 数量
                {{ receipt.quantity }}
              </p></a-timeline-item
            >
          </a-timeline>
        </a-tab-pane>
      </a-tabs>
    </a-card>
    <a-result v-else-if="!loading" status="404" title="采购订单不存在" />
    <a-modal
      v-model:open="approvalOpen"
      title="采购订单审批"
      :confirm-loading="saving"
      @ok="handleApproval"
    >
      <a-form layout="vertical">
        <a-form-item label="审批结论"
          ><a-radio-group
            v-model:value="approvalForm.decision"
            button-style="solid"
            ><a-radio-button value="APPROVED">通过</a-radio-button
            ><a-radio-button value="REJECTED"
              >驳回</a-radio-button
            ></a-radio-group
          ></a-form-item
        >
        <a-form-item label="审批意见"
          ><a-textarea v-model:value="approvalForm.comment" :rows="3"
        /></a-form-item>
        <a-form-item label="审批人"
          ><a-input v-model:value="approvalForm.approverName"
        /></a-form-item>
      </a-form>
    </a-modal>
  </BusinessDetailPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { message } from "ant-design-vue";
import BusinessDetailPage, {
  type DetailMetric,
} from "@/components/BusinessDetailPage.vue";
import { useAuthStore } from "@/stores/auth";
import {
  approvePurchaseOrder,
  listGoodsReceipts,
  listProcurementCostAllocations,
  listProcurementInquiries,
  listProcurementMatching,
  listProcurementPayables,
  listProcurementReturns,
  listPurchaseOrders,
  listPurchaseRequests,
  listSupplierInvoices,
  submitPurchaseOrder,
  type GoodsReceipt,
  type ProcurementCostAllocation,
  type ProcurementInquiry,
  type ProcurementMatching,
  type ProcurementPayable,
  type ProcurementReturnOrder,
  type PurchaseOrder,
  type PurchaseRequest,
  type SupplierInvoice,
} from "@/api/procurement";
import {
  listPaymentApplications,
  type PaymentApplication,
} from "@/api/finance";
const route = useRoute(),
  router = useRouter(),
  auth = useAuthStore(),
  loading = ref(false),
  saving = ref(false),
  approvalOpen = ref(false),
  order = ref<PurchaseOrder | null>(null),
  request = ref<PurchaseRequest | null>(null),
  inquiry = ref<ProcurementInquiry | null>(null),
  receipts = ref<GoodsReceipt[]>([]),
  payables = ref<ProcurementPayable[]>([]),
  costs = ref<ProcurementCostAllocation[]>([]),
  returns = ref<ProcurementReturnOrder[]>([]),
  invoices = ref<SupplierInvoice[]>([]),
  applications = ref<PaymentApplication[]>([]),
  matching = ref<ProcurementMatching | null>(null);
const approvalForm = reactive<{
  decision: "APPROVED" | "REJECTED";
  comment: string;
  approverName: string;
}>({
  decision: "APPROVED",
  comment: "同意采购",
  approverName: auth.user?.displayName || "",
});
const paidAmount = computed(() =>
    payables.value.reduce((s, i) => s + Number(i.paidAmount || 0), 0),
  ),
  outstanding = computed(() =>
    payables.value.reduce((s, i) => s + Number(i.outstandingAmount || 0), 0),
  );
const metrics = computed<DetailMetric[]>(() =>
  order.value
    ? [
        { label: "订单金额", value: money(order.value.orderAmount) },
        {
          label: "到货进度",
          value: `${order.value.receivedQty}/${order.value.orderedQty}`,
          hint: `${Math.round((order.value.receivedQty / order.value.orderedQty) * 100) || 0}%`,
        },
        { label: "已付金额", value: money(paidAmount.value) },
        {
          label: "待付金额",
          value: money(outstanding.value),
          warning: outstanding.value > 0,
        },
        {
          label: "成本归集",
          value: money(costs.value.reduce((s, i) => s + i.amount, 0)),
        },
      ]
    : [],
);
const currentStep = computed(() =>
  outstanding.value === 0 && paidAmount.value > 0
    ? 5
    : invoices.value.length || payables.value.length
      ? 4
      : receipts.value.length
        ? 3
        : order.value?.approvalStatus === "APPROVED"
          ? 2
          : inquiry.value
            ? 1
            : 0,
);
const riskLabel = computed(() =>
  matching.value && matching.value.matchStatus !== "MATCHED"
    ? "三单匹配存在差异"
    : receipts.value.some((i) => Number(i.rejectedQty || 0) > 0)
      ? "到货存在不合格品"
      : order.value?.expectedDeliveryDate &&
          new Date(order.value.expectedDeliveryDate) < new Date() &&
          Number(order.value.receivedQty) < Number(order.value.orderedQty)
        ? "采购交付已逾期"
        : "",
);
const riskDescription = computed(
  () =>
    matching.value?.riskMessage ||
    "请处理退货、补货或供应商质量改进，确认后再进入发票和付款环节。",
);
const quotationColumns = [
  { title: "供应商", key: "supplier", width: 240 },
  { title: "单价", key: "price", width: 140 },
  { title: "税率", dataIndex: "taxRate", width: 90 },
  { title: "交付日期", dataIndex: "deliveryDate", width: 120 },
  { title: "付款条件", dataIndex: "paymentTerms" },
  { title: "备注", dataIndex: "remark" },
];
const receiptColumns = [
  { title: "收货单", dataIndex: "code", width: 180 },
  { title: "数量", dataIndex: "quantity", width: 90 },
  { title: "金额", key: "amount", width: 140 },
  { title: "日期", dataIndex: "receivedDate", width: 120 },
  { title: "送货单", dataIndex: "deliveryNo", width: 160 },
  { title: "质检结果", key: "inspection", width: 180 },
  { title: "质检人", dataIndex: "inspectorName", width: 120 },
];
const returnColumns = [
  { title: "退货单", dataIndex: "code" },
  { title: "数量", dataIndex: "quantity", width: 90 },
  { title: "金额", key: "amount", width: 140 },
  { title: "原因", dataIndex: "reason" },
  { title: "日期", dataIndex: "returnDate", width: 120 },
  { title: "处理人", dataIndex: "handlerName", width: 120 },
];
const invoiceColumns = [
  { title: "发票号", dataIndex: "invoiceNo" },
  { title: "金额", key: "amount", width: 140 },
  { title: "税率", dataIndex: "taxRate", width: 90 },
  { title: "日期", dataIndex: "invoiceDate", width: 120 },
  { title: "差异", key: "difference", width: 140 },
  { title: "匹配", key: "match", width: 100 },
];
const payableColumns = [
  { title: "应付单", key: "code" },
  { title: "应付", key: "amount", width: 140 },
  { title: "已付", key: "paid", width: 140 },
  { title: "待付", key: "outstanding", width: 140 },
  { title: "到期日", dataIndex: "dueDate", width: 120 },
  { title: "状态", dataIndex: "status", width: 110 },
];
const applicationColumns = [
  { title: "申请单", key: "code" },
  { title: "金额", key: "amount", width: 140 },
  { title: "申请人", dataIndex: "applicantName", width: 120 },
  { title: "申请日期", dataIndex: "requestedDate", width: 120 },
  { title: "状态", dataIndex: "status", width: 120 },
];
const costColumns = [
  { title: "成本归属", dataIndex: "costTargetName" },
  { title: "物料", dataIndex: "partName" },
  { title: "金额", key: "amount", width: 150 },
  { title: "归集日期", dataIndex: "incurredDate", width: 120 },
  { title: "收货单", dataIndex: "receiptCode", width: 180 },
];
onMounted(loadData);
async function loadData() {
  loading.value = true;
  try {
    const id = String(route.params.id);
    const [os, rs, ps, cs, qs, rts, isx, ms, prs, apps] = await Promise.all([
      listPurchaseOrders({ page: 0, size: 999 }),
      listGoodsReceipts(),
      listProcurementPayables(),
      listProcurementCostAllocations(),
      listProcurementInquiries(),
      listProcurementReturns(),
      listSupplierInvoices(),
      listProcurementMatching(),
      listPurchaseRequests({ page: 0, size: 999 }),
      listPaymentApplications(),
    ]);
    order.value = os.content.find((i) => i.id === id) || null;
    if (order.value) {
      request.value =
        prs.content.find((i) => i.id === order.value!.requestId) || null;
      inquiry.value =
        qs.find((i) => i.requestId === order.value!.requestId) || null;
      receipts.value = rs.filter((i) => i.orderId === id);
      payables.value = ps.filter((i) => i.orderId === id);
      costs.value = cs.filter((i) => i.orderId === id);
      returns.value = rts.filter((i) => i.orderId === id);
      invoices.value = isx.filter((i) => i.orderId === id);
      matching.value = ms.find((i) => i.orderId === id) || null;
      const payableIds = new Set(payables.value.map((i) => i.id));
      applications.value = apps.filter((i) => payableIds.has(i.payableId));
    }
  } catch (e) {
    message.error(e instanceof Error ? e.message : "采购订单详情加载失败");
  } finally {
    loading.value = false;
  }
}
async function handleSubmit() {
  if (!order.value) return;
  saving.value = true;
  try {
    order.value = await submitPurchaseOrder(order.value.id);
    message.success("采购订单已提交审批");
  } catch (e) {
    message.error(e instanceof Error ? e.message : "提交失败");
  } finally {
    saving.value = false;
  }
}
async function handleApproval() {
  if (!order.value || !approvalForm.comment || !approvalForm.approverName) {
    message.warning("请填写审批意见和审批人");
    return;
  }
  saving.value = true;
  try {
    order.value = await approvePurchaseOrder(order.value.id, {
      ...approvalForm,
    });
    approvalOpen.value = false;
    message.success(
      approvalForm.decision === "APPROVED" ? "订单审批通过" : "订单已驳回",
    );
    await loadData();
  } catch (e) {
    message.error(e instanceof Error ? e.message : "审批失败");
  } finally {
    saving.value = false;
  }
}
function money(v?: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    maximumFractionDigits: 0,
  }).format(Number(v || 0));
}
function dateTime(v?: string) {
  return v ? v.slice(0, 16).replace("T", " ") : "-";
}
function statusLabel(v?: string) {
  return (
    (
      {
        DRAFT: "草稿",
        ORDERED: "已下单",
        PARTIAL_RECEIVED: "部分收货",
        RECEIVED: "已收货",
        CLOSED: "已关闭",
        CANCELLED: "已取消",
      } as Record<string, string>
    )[v || ""] || ""
  );
}
function approvalLabel(v?: string) {
  return (
    (
      { PENDING: "待审批", APPROVED: "已通过", REJECTED: "已驳回" } as Record<
        string,
        string
      >
    )[v || ""] || "-"
  );
}
function inspectionLabel(v?: string) {
  return (
    (
      {
        PENDING: "待质检",
        PASSED: "质检通过",
        PARTIAL: "部分合格",
        REJECTED: "不合格",
      } as Record<string, string>
    )[v || ""] || "待质检"
  );
}
</script>
<style scoped>
.section-gap {
  margin-top: 16px;
}
.sub {
  display: block;
  color: #8c96a5;
  font-size: 12px;
}
.danger {
  color: #cf1322;
  font-weight: 600;
}
.ant-timeline p {
  margin: 5px 0;
  color: #657186;
}
</style>
