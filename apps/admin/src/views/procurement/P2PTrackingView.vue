<template>
  <div class="page-stack">
    <a-card title="采购到付款全流程追踪">
      <template #extra
        ><a-button @click="router.push('/procurement')">返回采购管理</a-button
        ><a-button :loading="loading" @click="loadData"
          ><template #icon><ReloadOutlined /></template>刷新</a-button
        ></template
      >
      <a-spin :spinning="loading">
        <a-row :gutter="[16, 16]" class="metric-row">
          <a-col :xs="12" :xl="6"
            ><a-card
              ><a-statistic
                title="采购申请"
                :value="requests.length"
                suffix="单" /></a-card
          ></a-col>
          <a-col :xs="12" :xl="6"
            ><a-card
              ><a-statistic
                title="待转订单"
                :value="pendingOrder"
                suffix="单"
                :value-style="{
                  color: pendingOrder > 0 ? '#faad14' : '#52c41a',
                }" /></a-card
          ></a-col>
          <a-col :xs="12" :xl="6"
            ><a-card
              ><a-statistic
                title="待入库"
                :value="pendingReceipt"
                suffix="单"
                :value-style="{
                  color: pendingReceipt > 0 ? '#faad14' : '#52c41a',
                }" /></a-card
          ></a-col>
          <a-col :xs="12" :xl="6"
            ><a-card
              ><a-statistic
                title="匹配异常"
                :value="matchingRiskCount"
                suffix="单"
                :value-style="{
                  color: matchingRiskCount > 0 ? '#ff4d4f' : '#52c41a',
                }" /></a-card
          ></a-col>
        </a-row>
      </a-spin>
    </a-card>

    <a-card title="订单-质检入库-应付-发票四单匹配" style="margin-top: 16px">
      <section class="p2p-action-panel">
        <div class="p2p-action-head">
          <div>
            <h3>四单匹配处理动作</h3>
            <p>按异常类型给出下一步处理建议，避免只看见差异但不知道谁处理。</p>
          </div>
          <a-tag :color="matchingRiskCount > 0 ? 'red' : 'green'"
            >{{ matchingRiskCount }} 个异常</a-tag
          >
        </div>
        <div class="p2p-action-grid">
          <button
            v-for="item in matchingActions"
            :key="item.key"
            class="p2p-action-card"
            type="button"
            @click="router.push(item.link)"
          >
            <span>{{ item.label }}</span>
            <strong>{{ item.count }}</strong>
            <em>{{ item.action }}</em>
          </button>
        </div>
      </section>
      <a-table
        :data-source="matchingItems"
        :columns="matchingColumns"
        :loading="loading"
        :pagination="{ pageSize: 8 }"
        :row-key="(r: any) => r.orderId"
        size="middle"
        :scroll="{ x: 1120 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'order'"
            ><strong>{{ record.orderCode || "-" }}</strong
            ><span class="table-subtitle"
              >{{ record.supplierName || "未知供应商" }} ·
              {{ record.partName }}</span
            ></template
          >
          <template v-else-if="column.key === 'qty'"
            >{{ record.receivedQty }} / {{ record.orderedQty }}</template
          >
          <template v-else-if="column.key === 'orderAmount'">{{
            formatMoney(record.orderAmount)
          }}</template>
          <template v-else-if="column.key === 'receiptAmount'">{{
            formatMoney(record.receiptAmount)
          }}</template>
          <template v-else-if="column.key === 'payableAmount'">{{
            formatMoney(record.payableAmount)
          }}</template>
          <template v-else-if="column.key === 'invoiceAmount'">
            {{ formatMoney(record.invoiceAmount) }}
            <span class="table-subtitle"
              >已审核匹配 {{ formatMoney(record.matchedInvoiceAmount) }}</span
            >
          </template>
          <template v-else-if="column.key === 'status'"
            ><a-tag :color="matchStatusColor(record.matchStatus)">{{
              matchStatusLabel(record.matchStatus)
            }}</a-tag></template
          >
          <template v-else-if="column.key === 'action'">
            <a-button
              type="link"
              size="small"
              @click="router.push(matchAction(record).link)"
              >{{ matchAction(record).label }}</a-button
            >
          </template>
        </template>
        <template #emptyText>暂无采购订单匹配数据</template>
      </a-table>
    </a-card>

    <a-card title="采购申请 P2P 状态" style="margin-top: 16px">
      <a-table
        :data-source="p2pItems"
        :columns="p2pColumns"
        :loading="loading"
        :pagination="{ pageSize: 10 }"
        :row-key="(r: any) => r.id"
        size="middle"
        :scroll="{ x: 980 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'code'"
            ><strong>{{ record.code }}</strong
            ><span class="table-subtitle">{{
              record.materialName || ""
            }}</span></template
          >
          <template v-else-if="column.key === 'amount'">{{
            formatMoney(record.amount)
          }}</template>
          <template v-else-if="column.key === 'p2p'" class="p2p-steps">
            <div class="p2p-row">
              <div v-for="(s, si) in record._steps" :key="si" class="p2p-dot">
                <div class="p2p-dot-circle" :class="'p2p-' + s.status"></div>
                <span class="p2p-dot-label">{{ s.label }}</span>
              </div>
            </div>
          </template>
        </template>
        <template #expandedRowRender="{ record }">
          <div style="padding: 12px 0">
            <a-timeline>
              <a-timeline-item color="green"
                ><strong>采购申请</strong>
                <p style="margin: 2px 0; color: #8c8c8c; font-size: 12px">
                  {{ record.code }} · {{ formatMoney(record.amount) }}
                </p></a-timeline-item
              >
              <a-timeline-item v-if="record._order" color="green">
                <strong>采购订单</strong>
                <p style="margin: 2px 0; font-size: 12px">
                  {{ record._order.code }} ·
                  {{
                    formatMoney(
                      record._order.totalAmount || record._order.amount,
                    )
                  }}
                </p>
              </a-timeline-item>
              <a-timeline-item v-else color="orange"
                ><strong>待生成采购订单</strong></a-timeline-item
              >
              <a-timeline-item v-if="record._receipt" color="green">
                <strong>到货入库</strong>
                <p style="margin: 2px 0; font-size: 12px">
                  {{
                    record._receipt.receiptCode ||
                    record._receipt.code ||
                    "已入库"
                  }}
                  · {{ record._receipt.receivedDate?.slice(0, 10) }}
                </p>
              </a-timeline-item>
              <a-timeline-item v-else-if="record._order" color="orange"
                ><strong>等待入库</strong></a-timeline-item
              >
              <a-timeline-item v-if="record._payable" color="green">
                <strong>采购应付</strong>
                <p style="margin: 2px 0; font-size: 12px">
                  {{ record._payable.code }} ·
                  {{ formatMoney(record._payable.amount) }}
                </p>
              </a-timeline-item>
              <a-timeline-item v-else-if="record._receipt" color="orange"
                ><strong>等待生成应付</strong></a-timeline-item
              >
            </a-timeline>
          </div>
        </template>
      </a-table>
    </a-card>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { message } from "ant-design-vue";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import {
  listPurchaseRequests,
  listPurchaseOrders,
  listGoodsReceipts,
  listProcurementPayables,
  listProcurementMatching,
  type PurchaseRequest,
  type PurchaseOrder,
  type GoodsReceipt,
  type ProcurementPayable,
  type ProcurementMatching,
} from "@/api/procurement";
const router = useRouter();
const loading = ref(false);
const requests = ref<PurchaseRequest[]>([]);
const orders = ref<PurchaseOrder[]>([]);
const receipts = ref<GoodsReceipt[]>([]);
const payables = ref<ProcurementPayable[]>([]);
const matchingItems = ref<ProcurementMatching[]>([]);

const p2pItems = computed(() =>
  requests.value.map((r) => {
    const order = orders.value.find((o) => o.requestId === r.id);
    const receipt = order
      ? receipts.value.find((rc) => rc.orderId === order.id)
      : undefined;
    const payable = order
      ? payables.value.find((p) => p.orderId === order.id)
      : undefined;

    const stepStatus = (done: boolean, inProgress: boolean) =>
      done ? "finish" : inProgress ? "process" : "wait";
    const steps = [
      { label: "申请", status: stepStatus(true, false) },
      { label: "订单", status: stepStatus(!!order, !order) },
      { label: "入库", status: stepStatus(!!receipt, !receipt && !!order) },
      { label: "应付", status: stepStatus(!!payable, !payable && !!receipt) },
    ];
    const amount = r.totalAmount || (r.unitPrice || 0) * (r.quantity || 0);
    return {
      ...r,
      amount,
      _order: order,
      _receipt: receipt,
      _payable: payable,
      _steps: steps,
    };
  }),
);

const pendingOrder = computed(
  () => p2pItems.value.filter((i) => !i._order).length,
);
const pendingReceipt = computed(
  () => p2pItems.value.filter((i) => i._order && !i._receipt).length,
);
const matchingRiskCount = computed(
  () => matchingItems.value.filter((i) => i.matchStatus !== "MATCHED").length,
);
const matchingActions = computed(() => [
  {
    key: "receiving",
    label: "待入库",
    count: matchingItems.value.filter((i) => i.matchStatus === "RECEIVING")
      .length,
    action: "跟进仓库到货或催供应商交付",
    link: "/procurement/receipts",
  },
  {
    key: "payable",
    label: "缺应付",
    count: matchingItems.value.filter(
      (i) => i.matchStatus === "PAYABLE_MISSING",
    ).length,
    action: "补齐采购应付，进入付款计划",
    link: "/procurement/payables",
  },
  {
    key: "amount",
    label: "金额不一致",
    count: matchingItems.value.filter(
      (i) => i.matchStatus === "AMOUNT_MISMATCH",
    ).length,
    action: "复核订单、入库金额和发票应付",
    link: "/procurement/p2p",
  },
  {
    key: "invoice",
    label: "发票待处理",
    count: matchingItems.value.filter((i) =>
      ["INVOICE_PENDING", "INVOICE_MISMATCH", "INVOICE_REVIEW"].includes(
        i.matchStatus,
      ),
    ).length,
    action: "登记、复核并审核供应商发票",
    link: "/procurement/invoices",
  },
]);

const p2pColumns = [
  { title: "申请单", key: "code", width: 200 },
  { title: "金额", key: "amount", width: 130 },
  { title: "P2P 状态", key: "p2p", width: 320 },
  { title: "申请状态", dataIndex: "status", width: 110 },
];
const matchingColumns = [
  { title: "采购订单", key: "order", width: 240 },
  { title: "入库/订购数量", key: "qty", width: 130 },
  { title: "订单金额", key: "orderAmount", width: 130 },
  { title: "入库金额", key: "receiptAmount", width: 130 },
  { title: "应付金额", key: "payableAmount", width: 130 },
  { title: "发票/审核匹配", key: "invoiceAmount", width: 170 },
  { title: "匹配状态", key: "status", width: 120 },
  { title: "风险说明", dataIndex: "riskMessage", width: 260 },
  { title: "处理动作", key: "action", width: 120 },
];

onMounted(loadData);
async function loadData() {
  loading.value = true;
  try {
    const [reqResult, ordResult, rcs, pay, matching] = await Promise.all([
      listPurchaseRequests(),
      listPurchaseOrders(),
      listGoodsReceipts(),
      listProcurementPayables(),
      listProcurementMatching(),
    ]);
    requests.value = (reqResult as any).content || reqResult;
    orders.value = (ordResult as any).content || ordResult;
    receipts.value = rcs;
    payables.value = pay;
    matchingItems.value = matching;
  } catch (e: any) {
    message.error(e.message || "加载失败");
  } finally {
    loading.value = false;
  }
}
function formatMoney(v: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
  }).format(v || 0);
}
function matchStatusLabel(v: string) {
  return (
    (
      {
        MATCHED: "已匹配",
        RECEIVING: "待入库",
        PAYABLE_MISSING: "缺应付",
        AMOUNT_MISMATCH: "金额不一致",
        INVOICE_PENDING: "发票未收齐",
        INVOICE_MISMATCH: "发票金额异常",
        INVOICE_REVIEW: "发票待审核",
        CANCELLED: "已取消",
      } as Record<string, string>
    )[v] || v
  );
}
function matchStatusColor(v: string) {
  return (
    (
      {
        MATCHED: "green",
        RECEIVING: "blue",
        PAYABLE_MISSING: "orange",
        AMOUNT_MISMATCH: "red",
        INVOICE_PENDING: "orange",
        INVOICE_MISMATCH: "red",
        INVOICE_REVIEW: "blue",
        CANCELLED: "default",
      } as Record<string, string>
    )[v] || "default"
  );
}
function matchAction(record: ProcurementMatching) {
  return (
    (
      {
        RECEIVING: { label: "去入库", link: "/procurement/receipts" },
        PAYABLE_MISSING: { label: "补应付", link: "/procurement/payables" },
        AMOUNT_MISMATCH: { label: "查差异", link: "/procurement/p2p" },
        INVOICE_PENDING: { label: "登记发票", link: "/procurement/invoices" },
        INVOICE_MISMATCH: { label: "查发票", link: "/procurement/invoices" },
        INVOICE_REVIEW: { label: "审核发票", link: "/procurement/invoices" },
        CANCELLED: { label: "看订单", link: "/procurement/orders" },
        MATCHED: { label: "已完成", link: "/procurement/p2p" },
      } as Record<string, { label: string; link: string }>
    )[record.matchStatus] || { label: "处理", link: "/procurement/p2p" }
  );
}
</script>
<style scoped>
.p2p-action-panel {
  margin-bottom: 14px;
  padding: 14px;
  border: 1px solid #e5e7eb;
  background: #fbfcfe;
}
.p2p-action-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 12px;
}
.p2p-action-head h3 {
  margin: 0;
  color: #111827;
  font-size: 15px;
  font-weight: 600;
}
.p2p-action-head p {
  margin: 4px 0 0;
  color: #6b7280;
  font-size: 12px;
}
.p2p-action-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}
.p2p-action-card {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 12px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  text-align: left;
}
.p2p-action-card:hover {
  border-color: #91caff;
  background: #f6faff;
}
.p2p-action-card span {
  color: #667085;
  font-size: 12px;
}
.p2p-action-card strong {
  color: #101828;
  font-size: 20px;
}
.p2p-action-card em {
  overflow: hidden;
  color: #98a2b3;
  font-size: 12px;
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.p2p-row {
  display: flex;
  align-items: center;
  gap: 6px;
}
.p2p-dot {
  display: flex;
  align-items: center;
  gap: 4px;
}
.p2p-dot-circle {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  flex-shrink: 0;
}
.p2p-dot-circle.p2p-finish {
  background: #52c41a;
}
.p2p-dot-circle.p2p-process {
  background: #1890ff;
  animation: pulse 1.5s infinite;
}
.p2p-dot-circle.p2p-wait {
  background: #d9d9d9;
}
.p2p-dot-circle.p2p-error {
  background: #ff4d4f;
}
.p2p-dot-label {
  font-size: 11px;
  color: #595959;
  white-space: nowrap;
}
@keyframes pulse {
  0% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
  100% {
    opacity: 1;
  }
}
@media (max-width: 900px) {
  .p2p-action-head {
    flex-direction: column;
  }
  .p2p-action-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
