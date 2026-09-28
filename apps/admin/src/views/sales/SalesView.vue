<template>
  <div class="page-stack">
    <a-card>
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="orders" tab="销售订单">
          <a-table
            :columns="orderColumns"
            :data-source="orders"
            :loading="loading"
            :pagination="{ pageSize: 10 }"
            :row-key="(r: any) => r.id"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="orderStatusColor(record.status)">{{
                  orderStatusLabel(record.status)
                }}</a-tag>
              </template>
              <template v-else-if="column.key === 'actions'">
                <a-space>
                  <a-button
                    v-if="record.status === 'DRAFT'"
                    size="small"
                    type="primary"
                    :loading="actingId === record.id"
                    @click="approveOrder(record)"
                    >审批</a-button
                  >
                  <a-button
                    v-if="record.status === 'DRAFT'"
                    size="small"
                    danger
                    @click="cancelOrder(record)"
                    >取消</a-button
                  >
                </a-space>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="shipments" tab="发货记录">
          <a-table
            :columns="shipmentColumns"
            :data-source="shipments"
            :loading="loading"
            :pagination="{ pageSize: 10 }"
            :row-key="(r: any) => r.id"
            size="small"
          />
        </a-tab-pane>
        <a-tab-pane key="returns" tab="退货记录">
          <a-table
            :columns="returnColumns"
            :data-source="returns"
            :loading="loading"
            :pagination="{ pageSize: 10 }"
            :row-key="(r: any) => r.id"
            size="small"
          />
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>
<script setup lang="ts">
import { onMounted, ref } from "vue";
import { message } from "ant-design-vue";
import {
  listSalesOrders,
  listSalesShipments,
  listSalesReturns,
  approveSalesOrder,
  cancelSalesOrder,
  type SalesOrder,
  type SalesShipment,
  type SalesReturn,
} from "@/api/extensions";

const activeTab = ref("orders");
const loading = ref(false);
const actingId = ref("");
const orders = ref<SalesOrder[]>([]);
const shipments = ref<SalesShipment[]>([]);
const returns = ref<SalesReturn[]>([]);

const orderColumns = [
  { title: "单号", dataIndex: "code", width: 150 },
  { title: "客户", dataIndex: "customerName", width: 160 },
  { title: "金额", dataIndex: "totalAmount", width: 120 },
  { title: "状态", key: "status", width: 110 },
  { title: "日期", dataIndex: "orderDate", width: 120 },
  { title: "操作", key: "actions", width: 140 },
];
const shipmentColumns = [
  { title: "单号", dataIndex: "code", width: 150 },
  { title: "订单", dataIndex: "orderCode", width: 150 },
  { title: "客户", dataIndex: "customerName", width: 160 },
  { title: "成本", dataIndex: "totalCost", width: 120 },
  { title: "日期", dataIndex: "shipmentDate", width: 120 },
];
const returnColumns = [
  { title: "单号", dataIndex: "code", width: 150 },
  { title: "订单", dataIndex: "orderCode", width: 150 },
  { title: "客户", dataIndex: "customerName", width: 160 },
  { title: "金额", dataIndex: "totalAmount", width: 120 },
  { title: "日期", dataIndex: "returnDate", width: 120 },
];

function orderStatusLabel(s: string) {
  return (
    (
      {
        DRAFT: "草稿",
        APPROVED: "已审批",
        PARTIAL_SHIPPED: "部分发货",
        SHIPPED: "已发货",
        CANCELLED: "已取消",
      } as Record<string, string>
    )[s] || s
  );
}
function orderStatusColor(s: string) {
  return (
    (
      {
        DRAFT: "default",
        APPROVED: "blue",
        PARTIAL_SHIPPED: "cyan",
        SHIPPED: "green",
        CANCELLED: "red",
      } as Record<string, string>
    )[s] || "default"
  );
}

onMounted(loadData);
async function loadData() {
  loading.value = true;
  try {
    const [o, s, r] = await Promise.all([
      listSalesOrders(),
      listSalesShipments(),
      listSalesReturns(),
    ]);
    orders.value = o;
    shipments.value = s;
    returns.value = r;
  } catch (e: any) {
    message.error(e.message || "加载失败");
  } finally {
    loading.value = false;
  }
}
async function approveOrder(order: SalesOrder) {
  actingId.value = order.id;
  try {
    await approveSalesOrder(order.id);
    message.success("订单已审批");
    await loadData();
  } catch (e: any) {
    message.error(e.message || "审批失败");
  } finally {
    actingId.value = "";
  }
}
async function cancelOrder(order: SalesOrder) {
  try {
    await cancelSalesOrder(order.id);
    message.success("订单已取消");
    await loadData();
  } catch (e: any) {
    message.error(e.message || "取消失败");
  }
}
</script>
