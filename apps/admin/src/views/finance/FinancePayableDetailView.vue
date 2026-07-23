<template>
  <BusinessDetailPage
    :title="payable?.supplierName"
    :code="payable?.code"
    :subtitle="payable ? `采购订单 ${payable.orderCode}` : ''"
    :loading="loading"
    back-to="/finance/payables"
    :status-label="statusLabel(payable?.status)"
    :status-color="
      payable?.status === 'PAID' ? 'green' : payable?.overdue ? 'red' : 'blue'
    "
    :risk-label="
      payable?.overdue ? '逾期应付' : payable?.reservedAmount ? '审批占用' : ''
    "
    :risk-color="payable?.overdue ? 'red' : 'orange'"
    :stats="metrics"
    @refresh="loadData"
  >
    <template #actions>
      <a-button
        v-if="payable?.supplierId"
        @click="router.push(`/procurement/suppliers/${payable.supplierId}`)"
        >查看供应商</a-button
      >
      <a-button
        v-if="order"
        @click="router.push(`/procurement/orders/${order.id}`)"
        >查看采购订单</a-button
      >
      <a-button
        v-if="payable?.availableAmount"
        type="primary"
        @click="
          router.push(`/finance/payment-applications?payableId=${payable.id}`)
        "
        >申请付款</a-button
      >
    </template>
    <template #relations>
      <a-steps size="small" :current="currentStep" responsive>
        <a-step title="采购订单" :description="payable?.orderCode" />
        <a-step title="到货质检" :description="`${receipts.length} 笔收货`" />
        <a-step title="供应商发票" :description="`${invoices.length} 张`" />
        <a-step
          title="付款审批"
          :description="`${applications.length} 笔申请`"
        />
        <a-step title="银行付款" :description="money(payable?.paidAmount)" />
        <a-step
          title="应付核销"
          :description="payable?.status === 'PAID' ? '已结清' : '进行中'"
        />
      </a-steps>
    </template>
    <a-card v-if="payable" :bordered="false">
      <a-tabs>
        <a-tab-pane key="overview" tab="应付单据">
          <a-descriptions
            bordered
            :column="{ xs: 1, md: 2, xl: 3 }"
            size="small"
          >
            <a-descriptions-item label="供应商">{{
              payable.supplierName
            }}</a-descriptions-item>
            <a-descriptions-item label="采购订单"
              ><a
                @click="router.push(`/procurement/orders/${payable.orderId}`)"
                >{{ payable.orderCode }}</a
              ></a-descriptions-item
            >
            <a-descriptions-item label="到期日">{{
              payable.dueDate
            }}</a-descriptions-item>
            <a-descriptions-item label="应付金额">{{
              money(payable.amount)
            }}</a-descriptions-item>
            <a-descriptions-item label="已付金额">{{
              money(payable.paidAmount)
            }}</a-descriptions-item>
            <a-descriptions-item label="待付金额">{{
              money(payable.outstandingAmount)
            }}</a-descriptions-item>
            <a-descriptions-item label="审批占用">{{
              money(payable.reservedAmount)
            }}</a-descriptions-item>
            <a-descriptions-item label="可申请付款">{{
              money(payable.availableAmount)
            }}</a-descriptions-item>
            <a-descriptions-item label="付款状态">{{
              statusLabel(payable.status)
            }}</a-descriptions-item>
          </a-descriptions>
          <a-alert
            v-if="payable.overdue"
            class="section-gap"
            type="error"
            show-icon
            message="该应付已逾期"
            description="请核对发票、审批和资金排期，避免影响供应商履约及后续供货。"
          />
        </a-tab-pane>
        <a-tab-pane
          key="receipts"
          :tab="`收货与发票 (${receipts.length}/${invoices.length})`"
        >
          <a-row :gutter="[16, 16]">
            <a-col :xs="24" :xl="12"
              ><a-table
                :columns="receiptColumns"
                :data-source="receipts"
                row-key="id"
                size="small"
                :pagination="false"
                ><template #bodyCell="{ column, record }"
                  ><template v-if="column.key === 'amount'">{{
                    money(record.amount)
                  }}</template></template
                ></a-table
              ></a-col
            >
            <a-col :xs="24" :xl="12"
              ><a-table
                :columns="invoiceColumns"
                :data-source="invoices"
                row-key="id"
                size="small"
                :pagination="false"
                ><template #bodyCell="{ column, record }"
                  ><template v-if="column.key === 'amount'">{{
                    money(record.amount)
                  }}</template
                  ><template v-else-if="column.key === 'match'"
                    ><a-tag
                      :color="
                        record.matchStatus === 'MATCHED' ? 'green' : 'red'
                      "
                      >{{
                        record.matchStatus === "MATCHED"
                          ? "三单匹配"
                          : "存在差异"
                      }}</a-tag
                    ></template
                  ></template
                ></a-table
              ></a-col
            >
          </a-row>
        </a-tab-pane>
        <a-tab-pane
          key="applications"
          :tab="`付款审批 (${applications.length})`"
        >
          <a-table
            :columns="applicationColumns"
            :data-source="applications"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'code'"
                ><a
                  @click="
                    router.push(`/finance/payment-applications/${record.id}`)
                  "
                  >{{ record.code || "付款申请" }}</a
                ></template
              >
              <template v-else-if="column.key === 'amount'">{{
                money(record.requestedAmount)
              }}</template>
              <template v-else-if="column.key === 'status'"
                ><a-tag
                  :color="
                    record.status === 'PAID'
                      ? 'green'
                      : record.status === 'REJECTED'
                        ? 'red'
                        : 'blue'
                  "
                  >{{ applicationStatusLabel(record.status) }}</a-tag
                ></template
              >
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="payments" :tab="`付款流水 (${payments.length})`">
          <a-table
            :columns="paymentColumns"
            :data-source="payments"
            row-key="id"
          >
            <template #bodyCell="{ column, record }"
              ><template v-if="column.key === 'amount'"
                ><strong>{{ money(record.amount) }}</strong></template
              ></template
            >
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="reconcile" tab="三单匹配与核销">
          <a-result
            :status="reconcileStatus"
            :title="reconcileTitle"
            :sub-title="reconcileDescription"
          />
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </BusinessDetailPage>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { message } from "ant-design-vue";
import BusinessDetailPage, {
  type DetailMetric,
} from "@/components/BusinessDetailPage.vue";
import {
  listFinancePayables,
  listPaymentApplications,
  listPaymentRecords,
  type FinancePayable,
  type PaymentApplication,
  type PaymentRecord,
} from "@/api/finance";
import {
  listGoodsReceipts,
  listPurchaseOrders,
  listSupplierInvoices,
  type GoodsReceipt,
  type PurchaseOrder,
  type SupplierInvoice,
} from "@/api/procurement";
const route = useRoute(),
  router = useRouter();
const loading = ref(false),
  payable = ref<FinancePayable | null>(null),
  order = ref<PurchaseOrder | null>(null),
  receipts = ref<GoodsReceipt[]>([]),
  invoices = ref<SupplierInvoice[]>([]),
  applications = ref<PaymentApplication[]>([]),
  payments = ref<PaymentRecord[]>([]);
const metrics = computed<DetailMetric[]>(() =>
  payable.value
    ? [
        { label: "应付金额", value: money(payable.value.amount) },
        { label: "已付金额", value: money(payable.value.paidAmount) },
        {
          label: "审批占用",
          value: money(payable.value.reservedAmount),
          warning: payable.value.reservedAmount > 0,
        },
        { label: "可申请金额", value: money(payable.value.availableAmount) },
        {
          label: "待付金额",
          value: money(payable.value.outstandingAmount),
          danger: payable.value.overdue,
        },
      ]
    : [],
);
const currentStep = computed(() =>
  payable.value?.status === "PAID"
    ? 5
    : payments.value.length
      ? 4
      : applications.value.length
        ? 3
        : invoices.value.length
          ? 2
          : receipts.value.length
            ? 1
            : 0,
);
const reconcileStatus = computed(() => "success" as const);
const reconcileTitle = computed(() =>
  invoices.value.some((i) => i.matchStatus === "MISMATCH")
    ? "存在发票差异，需复核"
    : "采购订单、收货、发票与应付链路已建立",
);
const reconcileDescription = computed(
  () =>
    `订单 ${money(order.value?.orderAmount)}，收货 ${money(receipts.value.reduce((s, i) => s + i.amount, 0))}，发票 ${money(invoices.value.reduce((s, i) => s + i.amount, 0))}，应付 ${money(payable.value?.amount)}。`,
);
const receiptColumns = [
  { title: "收货单", dataIndex: "code" },
  { title: "数量", dataIndex: "quantity", width: 90 },
  { title: "金额", key: "amount", width: 130 },
  { title: "日期", dataIndex: "receivedDate", width: 110 },
  { title: "质检", dataIndex: "inspectionStatus", width: 110 },
];
const invoiceColumns = [
  { title: "发票号", dataIndex: "invoiceNo" },
  { title: "金额", key: "amount", width: 130 },
  { title: "日期", dataIndex: "invoiceDate", width: 110 },
  { title: "匹配", key: "match", width: 110 },
];
const applicationColumns = [
  { title: "付款申请", key: "code", width: 190 },
  { title: "申请金额", key: "amount", width: 140 },
  { title: "申请人", dataIndex: "applicantName", width: 120 },
  { title: "申请日期", dataIndex: "requestedDate", width: 120 },
  { title: "用途", dataIndex: "purpose" },
  { title: "状态", key: "status", width: 110 },
];
const paymentColumns = [
  { title: "付款单", dataIndex: "code", width: 180 },
  { title: "付款金额", key: "amount", width: 150 },
  { title: "付款日期", dataIndex: "paidDate", width: 120 },
  { title: "方式", dataIndex: "paymentMethod", width: 130 },
  { title: "银行流水", dataIndex: "bankReference" },
  { title: "付款人", dataIndex: "payerName", width: 120 },
];
onMounted(loadData);
async function loadData() {
  loading.value = true;
  try {
    const id = String(route.params.id);
    const [ps, os, rs, isx, as, pays] = await Promise.all([
      listFinancePayables(),
      listPurchaseOrders({ page: 0, size: 999 }),
      listGoodsReceipts(),
      listSupplierInvoices(),
      listPaymentApplications(),
      listPaymentRecords(),
    ]);
    payable.value = ps.find((i) => i.id === id) || null;
    if (payable.value) {
      order.value =
        os.content.find((i) => i.id === payable.value!.orderId) || null;
      receipts.value = rs.filter((i) => i.orderId === payable.value!.orderId);
      invoices.value = isx.filter((i) => i.orderId === payable.value!.orderId);
      applications.value = as.filter((i) => i.payableId === id);
      payments.value = pays.filter((i) => i.payableId === id);
    }
  } catch (e) {
    message.error(e instanceof Error ? e.message : "应付详情加载失败");
  } finally {
    loading.value = false;
  }
}
function money(v?: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    maximumFractionDigits: 0,
  }).format(Number(v || 0));
}
function statusLabel(v?: string) {
  return (
    (
      {
        PENDING: "待付款",
        PARTIAL_PAID: "部分已付",
        PAID: "已付清",
        CANCELLED: "已取消",
      } as Record<string, string>
    )[v || ""] || ""
  );
}
function applicationStatusLabel(v: string) {
  return (
    (
      {
        PENDING_APPROVAL: "待审批",
        APPROVED: "待付款",
        REJECTED: "已驳回",
        PAID: "已付款",
      } as Record<string, string>
    )[v] || v
  );
}
</script>
<style scoped>
.section-gap {
  margin-top: 16px;
}
</style>
