<template>
  <BusinessDetailPage
    :title="supplier?.name"
    :code="supplier?.code"
    :subtitle="
      supplier
        ? `${supplier.category || '未分类'} · ${supplier.contactName || '未设置联系人'}`
        : ''
    "
    :loading="loading"
    back-to="/procurement/suppliers"
    :status-label="admissionLabel(supplier?.admissionStatus)"
    :status-color="
      supplier?.admissionStatus === 'APPROVED' ? 'green' : 'orange'
    "
    :risk-label="riskLabel(supplier?.riskStatus)"
    :risk-color="
      supplier?.riskStatus === 'NORMAL'
        ? 'green'
        : supplier?.riskStatus === 'BLOCKED'
          ? 'red'
          : 'orange'
    "
    :stats="metrics"
    @refresh="loadData"
  >
    <template #actions>
      <a-button
        type="primary"
        @click="router.push(`/procurement/requests?supplierId=${supplierId}`)"
        >发起采购</a-button
      >
    </template>
    <template #relations>
      <a-steps size="small" :current="relationCurrent" responsive>
        <a-step
          title="准入建档"
          :description="`${profileCompleteness}% 完整`"
        />
        <a-step title="询价比价" :description="`${quotations.length} 次报价`" />
        <a-step title="采购签约" :description="`${orders.length} 笔订单`" />
        <a-step title="到货质检" :description="`${receipts.length} 笔到货`" />
        <a-step title="发票结算" :description="`${invoices.length} 张发票`" />
        <a-step title="付款闭环" :description="money(supplier?.paidAmount)" />
      </a-steps>
    </template>

    <a-card v-if="supplier" :bordered="false">
      <a-tabs>
        <a-tab-pane key="overview" tab="供应商档案">
          <a-row :gutter="[16, 16]">
            <a-col :xs="24" :xl="14">
              <a-descriptions
                bordered
                :column="{ xs: 1, md: 2 }"
                size="small"
                title="工商与联系信息"
              >
                <a-descriptions-item label="统一社会信用代码">{{
                  supplier.unifiedSocialCreditCode || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="法定代表人">{{
                  supplier.legalRepresentative || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="注册资本">{{
                  supplier.registeredCapital || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="供应商类别">{{
                  supplier.category || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="联系人">{{
                  supplier.contactName || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="联系电话">{{
                  supplier.phone || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="注册地址" :span="2">{{
                  supplier.registeredAddress || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="经营范围" :span="2">{{
                  supplier.businessScope || "-"
                }}</a-descriptions-item>
              </a-descriptions>
            </a-col>
            <a-col :xs="24" :xl="10">
              <a-descriptions
                bordered
                :column="1"
                size="small"
                title="资质与结算"
              >
                <a-descriptions-item label="营业执照有效期">{{
                  supplier.licenseValidTo || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="资质有效期">{{
                  supplier.qualificationValidTo || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="纳税人类型">{{
                  supplier.taxpayerType || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="开户银行">{{
                  supplier.bankName || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="银行账号">{{
                  supplier.bankAccount || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="结算条款">{{
                  supplier.settlementTerms || "-"
                }}</a-descriptions-item>
              </a-descriptions>
            </a-col>
          </a-row>
          <a-alert
            v-if="profileCompleteness < 80"
            class="section-gap"
            type="warning"
            show-icon
            message="供应商档案不完整"
            :description="`当前完整度 ${profileCompleteness}%，建议完成工商、资质、银行及结算资料后再扩大采购。`"
          />
        </a-tab-pane>

        <a-tab-pane key="quotes" :tab="`询价报价 (${quotations.length})`">
          <a-table
            :columns="quoteColumns"
            :data-source="quotations"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'price'">{{
                money(record.unitPrice)
              }}</template>
              <template v-else-if="column.key === 'selected'"
                ><a-tag :color="record.selected ? 'green' : 'default'">{{
                  record.selected ? "已中选" : "未中选"
                }}</a-tag></template
              >
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="orders" :tab="`采购订单 (${orders.length})`">
          <a-table
            :columns="orderColumns"
            :data-source="orders"
            row-key="id"
            :scroll="{ x: 1050 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'order'"
                ><a @click="router.push(`/procurement/orders/${record.id}`)">{{
                  record.code || "采购订单"
                }}</a
                ><span class="sub">{{ record.partName }}</span></template
              >
              <template v-else-if="column.key === 'amount'">{{
                money(record.orderAmount)
              }}</template>
              <template v-else-if="column.key === 'receipt'"
                >{{ record.receivedQty }} / {{ record.orderedQty }}</template
              >
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="delivery" :tab="`到货质检与退货 (${receipts.length})`">
          <a-table
            :columns="receiptColumns"
            :data-source="receipts"
            row-key="id"
            :scroll="{ x: 1000 }"
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
                ><span v-if="record.rejectedQty" class="sub"
                  >不合格 {{ record.rejectedQty }}</span
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
          >
            <template #bodyCell="{ column, record }"
              ><template v-if="column.key === 'amount'">{{
                money(record.amount)
              }}</template></template
            >
          </a-table>
        </a-tab-pane>

        <a-tab-pane
          key="finance"
          :tab="`发票与付款 (${invoices.length}/${payables.length})`"
        >
          <a-row :gutter="[16, 16]">
            <a-col :xs="24" :xl="12">
              <a-table
                :columns="invoiceColumns"
                :data-source="invoices"
                row-key="id"
                size="small"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'amount'">{{
                    money(record.amount)
                  }}</template>
                  <template v-else-if="column.key === 'match'"
                    ><a-tag
                      :color="
                        record.matchStatus === 'MATCHED' ? 'green' : 'red'
                      "
                      >{{
                        record.matchStatus === "MATCHED" ? "匹配" : "差异"
                      }}</a-tag
                    ></template
                  >
                </template>
              </a-table>
            </a-col>
            <a-col :xs="24" :xl="12">
              <a-table
                :columns="payableColumns"
                :data-source="payables"
                row-key="id"
                size="small"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'code'"
                    ><a
                      @click="router.push(`/finance/payables/${record.id}`)"
                      >{{ record.code || record.orderCode }}</a
                    ></template
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
            </a-col>
          </a-row>
        </a-tab-pane>

        <a-tab-pane key="performance" tab="履约评价">
          <a-row :gutter="[16, 16]">
            <a-col
              v-for="score in performanceScores"
              :key="score.label"
              :xs="12"
              :lg="6"
            >
              <a-card size="small"
                ><a-statistic
                  :title="score.label"
                  :value="score.value"
                  suffix="分" /><a-progress
                  :percent="score.value"
                  :show-info="false"
                  size="small"
              /></a-card>
            </a-col>
          </a-row>
          <a-alert
            class="section-gap"
            :type="
              overallScore >= 80
                ? 'success'
                : overallScore >= 60
                  ? 'warning'
                  : 'error'
            "
            show-icon
            :message="`综合履约评分 ${overallScore} 分`"
            description="评分根据准时交付、质检通过、发票匹配和资料完整度自动计算。"
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
  listGoodsReceipts,
  listProcurementInquiries,
  listProcurementPayables,
  listProcurementReturns,
  listPurchaseOrders,
  listSupplierInvoices,
  listSuppliers,
  type GoodsReceipt,
  type ProcurementPayable,
  type ProcurementReturnOrder,
  type PurchaseOrder,
  type Supplier,
  type SupplierInvoice,
  type SupplierQuotation,
} from "@/api/procurement";

const route = useRoute();
const router = useRouter();
const supplierId = computed(() => String(route.params.id));
const loading = ref(false);
const supplier = ref<Supplier | null>(null);
const orders = ref<PurchaseOrder[]>([]);
const receipts = ref<GoodsReceipt[]>([]);
const payables = ref<ProcurementPayable[]>([]);
const invoices = ref<SupplierInvoice[]>([]);
const returns = ref<ProcurementReturnOrder[]>([]);
const quotations = ref<SupplierQuotation[]>([]);
const profileFields = computed(() =>
  supplier.value
    ? [
        supplier.value.unifiedSocialCreditCode,
        supplier.value.legalRepresentative,
        supplier.value.registeredAddress,
        supplier.value.contactName,
        supplier.value.phone,
        supplier.value.licenseValidTo,
        supplier.value.qualificationValidTo,
        supplier.value.bankName,
        supplier.value.bankAccount,
        supplier.value.settlementTerms,
      ]
    : [],
);
const profileCompleteness = computed(() =>
  Math.round(
    (profileFields.value.filter(Boolean).length /
      Math.max(1, profileFields.value.length)) *
      100,
  ),
);
const metrics = computed<DetailMetric[]>(() =>
  supplier.value
    ? [
        {
          label: "签约金额",
          value: money(supplier.value.contractedAmount),
          hint: `${orders.value.length} 笔采购订单`,
        },
        { label: "应付金额", value: money(supplier.value.payableAmount) },
        { label: "已付金额", value: money(supplier.value.paidAmount) },
        {
          label: "待付金额",
          value: money(supplier.value.outstandingAmount),
          danger: supplier.value.outstandingAmount > 0,
        },
        {
          label: "质检不合格",
          value: receipts.value.reduce(
            (sum, item) => sum + Number(item.rejectedQty || 0),
            0,
          ),
          warning: receipts.value.some(
            (item) => Number(item.rejectedQty || 0) > 0,
          ),
        },
      ]
    : [],
);
const relationCurrent = computed(() =>
  supplier.value?.paidAmount
    ? 5
    : invoices.value.length
      ? 4
      : receipts.value.length
        ? 3
        : orders.value.length
          ? 2
          : quotations.value.length
            ? 1
            : 0,
);
const onTimeRate = computed(() => {
  const evaluated = orders.value.filter((item) => item.expectedDeliveryDate);
  if (!evaluated.length) return 100;
  const onTime = evaluated.filter((order) => {
    const related = receipts.value
      .filter((receipt) => receipt.orderId === order.id && receipt.inspectionStatus !== "PENDING")
      .sort((a, b) => String(b.receivedDate).localeCompare(String(a.receivedDate)));
    return (
      Number(order.receivedQty) >= Number(order.orderedQty) &&
      !!related[0]?.receivedDate &&
      related[0].receivedDate <= String(order.expectedDeliveryDate)
    );
  }).length;
  return Math.round((onTime / evaluated.length) * 100);
});
const qualityRate = computed(() => {
  const inspected = receipts.value.filter((item) => item.inspectionStatus !== "PENDING");
  const received = inspected.reduce((sum, item) => sum + Number(item.quantity || 0), 0);
  const qualified = inspected.reduce((sum, item) => sum + Number(item.qualifiedQty || 0), 0);
  return received ? Math.round((qualified / received) * 100) : 100;
});
const invoiceRate = computed(() =>
  invoices.value.length
    ? Math.round(
        (invoices.value.filter((item) => item.matchStatus === "MATCHED")
          .length /
          invoices.value.length) *
          100,
      )
    : 100,
);
const performanceScores = computed(() => [
  { label: "准时交付率", value: onTimeRate.value },
  { label: "质量合格率", value: qualityRate.value },
  { label: "发票匹配", value: invoiceRate.value },
  { label: "资料完整", value: profileCompleteness.value },
]);
const overallScore = computed(() =>
  Math.round(
    performanceScores.value.reduce((sum, item) => sum + item.value, 0) /
      performanceScores.value.length,
  ),
);
const quoteColumns = [
  { title: "询价单", dataIndex: "inquiryCode" },
  { title: "单价", key: "price", width: 140 },
  { title: "税率", dataIndex: "taxRate", width: 90 },
  { title: "交付日", dataIndex: "deliveryDate", width: 120 },
  { title: "付款条款", dataIndex: "paymentTerms" },
  { title: "结果", key: "selected", width: 100 },
];
const orderColumns = [
  { title: "采购订单", key: "order", width: 230 },
  { title: "成本归属", dataIndex: "costTargetName", width: 180 },
  { title: "金额", key: "amount", width: 140 },
  { title: "收货", key: "receipt", width: 100 },
  { title: "预计交付", dataIndex: "expectedDeliveryDate", width: 120 },
  { title: "审批", dataIndex: "approvalStatus", width: 100 },
  { title: "状态", dataIndex: "status", width: 120 },
];
const receiptColumns = [
  { title: "收货单", dataIndex: "code", width: 180 },
  { title: "订单", dataIndex: "orderCode", width: 180 },
  { title: "数量", dataIndex: "quantity", width: 90 },
  { title: "金额", key: "amount", width: 140 },
  { title: "收货日期", dataIndex: "receivedDate", width: 120 },
  { title: "质检", key: "inspection", width: 150 },
  { title: "收货人", dataIndex: "receiverName", width: 120 },
];
const returnColumns = [
  { title: "退货单", dataIndex: "code" },
  { title: "数量", dataIndex: "quantity", width: 90 },
  { title: "金额", key: "amount", width: 140 },
  { title: "原因", dataIndex: "reason" },
  { title: "日期", dataIndex: "returnDate", width: 120 },
];
const invoiceColumns = [
  { title: "发票号", dataIndex: "invoiceNo" },
  { title: "金额", key: "amount", width: 130 },
  { title: "日期", dataIndex: "invoiceDate", width: 110 },
  { title: "四单匹配", key: "match", width: 100 },
];
const payableColumns = [
  { title: "应付单", key: "code" },
  { title: "应付", key: "amount", width: 120 },
  { title: "已付", key: "paid", width: 120 },
  { title: "待付", key: "outstanding", width: 120 },
  { title: "到期日", dataIndex: "dueDate", width: 110 },
];
onMounted(loadData);
async function loadData() {
  loading.value = true;
  try {
    const [
      supplierPage,
      orderPage,
      allReceipts,
      allPayables,
      allInvoices,
      allReturns,
      inquiries,
    ] = await Promise.all([
      listSuppliers(0, 999),
      listPurchaseOrders({ page: 0, size: 999 }),
      listGoodsReceipts(),
      listProcurementPayables(),
      listSupplierInvoices(),
      listProcurementReturns(),
      listProcurementInquiries(),
    ]);
    supplier.value =
      supplierPage.content.find((item) => item.id === supplierId.value) || null;
    orders.value = orderPage.content.filter(
      (item) => item.supplierId === supplierId.value,
    );
    const orderIds = new Set(orders.value.map((item) => item.id));
    receipts.value = allReceipts.filter((item) => orderIds.has(item.orderId));
    payables.value = allPayables.filter(
      (item) => item.supplierId === supplierId.value,
    );
    invoices.value = allInvoices.filter(
      (item) => item.supplierId === supplierId.value,
    );
    returns.value = allReturns.filter(
      (item) => item.supplierId === supplierId.value,
    );
    quotations.value = inquiries
      .flatMap((inquiry) =>
        inquiry.quotes.map((quote) =>
          Object.assign({}, quote, { inquiryCode: inquiry.code }),
        ),
      )
      .filter((item) => item.supplierId === supplierId.value);
  } catch (error) {
    message.error(
      error instanceof Error ? error.message : "供应商详情加载失败",
    );
  } finally {
    loading.value = false;
  }
}
function money(value?: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    maximumFractionDigits: 0,
  }).format(Number(value || 0));
}
function riskLabel(value?: string) {
  return (
    (
      {
        NORMAL: "风险正常",
        WATCHLIST: "重点观察",
        BLOCKED: "已停用",
      } as Record<string, string>
    )[value || ""] || ""
  );
}
function admissionLabel(value?: string) {
  return (
    (
      { APPROVED: "已准入", PENDING: "待准入", REJECTED: "未准入" } as Record<
        string,
        string
      >
    )[value || ""] ||
    value ||
    "待准入"
  );
}
function inspectionLabel(value?: string) {
  return (
    (
      {
        PENDING: "待质检",
        PASSED: "质检通过",
        PARTIAL: "部分合格",
        REJECTED: "不合格",
      } as Record<string, string>
    )[value || ""] || "待质检"
  );
}
</script>
<style scoped>
.sub {
  display: block;
  color: #8c96a5;
  font-size: 12px;
}
.section-gap {
  margin-top: 16px;
}
</style>
