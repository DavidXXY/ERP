<template>
  <div class="page-stack">
    <a-card>
      <template #title>采购订单</template>
      <template #extra
        ><a-button @click="router.push('/procurement')">返回采购管理</a-button
        ><a-button :loading="loading" @click="loadData"
          ><template #icon><ReloadOutlined /></template>刷新</a-button
        ></template
      >
      <a-space class="table-toolbar"
        ><a-button
          v-if="auth.can('procurement:purchase:create')"
          type="primary"
          @click="openCreate"
          ><template #icon><PlusOutlined /></template>新增订单</a-button
        ></a-space
      >
      <a-table
        :columns="orderColumns"
        :data-source="orders"
        :loading="loading"
        :pagination="{ pageSize: 10 }"
        :row-key="(r: any) => r.id"
        :scroll="{ x: 1250 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'order'"
            ><strong>{{ record.code }}</strong
            ><span class="table-subtitle">{{
              record.supplierName || record.supplierId?.slice(0, 8)
            }}</span></template
          >
          <template v-else-if="column.key === 'items'"
            ><span>{{ record.orderItems?.length || 0 }} 项</span></template
          >
          <template v-else-if="column.key === 'amount'"
            ><strong>{{
              formatMoney(
                record.totalAmount || record.amount || record.orderAmount,
              )
            }}</strong
            ><span class="table-subtitle"
              >税率 {{ formatTaxRate(record.taxRate) }}</span
            ></template
          >
          <template v-else-if="column.key === 'status'"
            ><a-tag
              :color="
                (
                  {
                    DRAFT: 'default',
                    ORDERED: 'blue',
                    PARTIAL_RECEIVED: 'orange',
                    RECEIVED: 'green',
                    CLOSED: 'cyan',
                    CANCELLED: 'red',
                  } as any
                )[record.status] || 'default'
              "
              >{{
                (
                  {
                    DRAFT: "草稿",
                    ORDERED: "已下单",
                    PARTIAL_RECEIVED: "部分收货",
                    RECEIVED: "已收货",
                    CLOSED: "已关闭",
                    CANCELLED: "已取消",
                  } as any
                )[record.status] || record.status
              }}</a-tag
            ></template
          >
          <template v-else-if="column.key === 'date'">{{
            record.orderedAt?.slice(0, 10) ||
            record.createdAt?.slice(0, 10) ||
            "-"
          }}</template>
          <template v-else-if="column.key === 'action'">
            <a-space
              ><a-button
                type="link"
                size="small"
                @click="router.push(`/procurement/orders/${record.id}`)"
                >详情</a-button
              >
              <a-button
                v-if="
                  record.status === 'DRAFT' &&
                  record.approvalStatus === 'PENDING'
                "
                type="link"
                size="small"
                @click="handleSubmit(record)"
                >提交审批</a-button
              >
              <a-button
                v-if="
                  record.status === 'DRAFT' &&
                  record.approvalStatus === 'PENDING' &&
                  auth.can('procurement:request:approve')
                "
                type="link"
                size="small"
                @click="handleApproval(record, 'APPROVED')"
                >审批通过</a-button
              >
              <a-button
                v-if="
                  record.status === 'DRAFT' &&
                  record.approvalStatus === 'PENDING' &&
                  auth.can('procurement:request:approve')
                "
                danger
                type="link"
                size="small"
                @click="handleApproval(record, 'REJECTED')"
                >驳回</a-button
              ></a-space
            >
            <a-button
              v-if="['PARTIAL_RECEIVED', 'RECEIVED'].includes(record.status)"
              type="link"
              size="small"
              @click="handleClose(record)"
              >关闭订单</a-button
            >
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>
<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { message } from "ant-design-vue";
import PlusOutlined from "@ant-design/icons-vue/PlusOutlined";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import {
  listPurchaseOrders,
  submitPurchaseOrder,
  approvePurchaseOrder,
  closePurchaseOrder,
  type PurchaseOrder,
} from "@/api/procurement";
import { useAuthStore } from "@/stores/auth";
const auth = useAuthStore();
const router = useRouter();
const loading = ref(false);
const orders = ref<PurchaseOrder[]>([]);
function openCreate() {
  router.push("/procurement?tab=orders");
}
const orderColumns = [
  { title: "订单编号", key: "order", width: 220 },
  { title: "明细", key: "items", width: 80 },
  { title: "金额", key: "amount", width: 140 },
  { title: "状态", key: "status", width: 130 },
  { title: "下单日期", key: "date", width: 120 },
  { title: "操作", key: "action", width: 280, fixed: "right" },
];
async function handleSubmit(record: PurchaseOrder) {
  try {
    await submitPurchaseOrder(record.id);
    message.success("订单已提交审批");
    await loadData();
  } catch (e) {
    message.error(e instanceof Error ? e.message : "提交失败");
  }
}
async function handleApproval(
  record: PurchaseOrder,
  decision: "APPROVED" | "REJECTED",
) {
  try {
    await approvePurchaseOrder(record.id, {
      decision,
      approverName: auth.user?.displayName || "审批人",
      comment: decision === "APPROVED" ? "同意采购订单" : "采购订单驳回",
    });
    message.success(
      decision === "APPROVED" ? "订单审批通过，可以登记到货" : "订单已驳回",
    );
    await loadData();
  } catch (e) {
    message.error(e instanceof Error ? e.message : "审批失败");
  }
}
async function handleClose(record: PurchaseOrder) {
  try {
    await closePurchaseOrder(record.id);
    message.success("订单已关闭，未交数量不再接收");
    await loadData();
  } catch (e) {
    message.error(e instanceof Error ? e.message : "关闭失败");
  }
}
onMounted(loadData);
async function loadData() {
  loading.value = true;
  try {
    const result = await listPurchaseOrders({ page: 0, size: 999 });
    orders.value = result.content;
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
    minimumFractionDigits: 2,
  }).format(v || 0);
}
function formatTaxRate(v?: number) {
  return `${Number(v ?? 13)
    .toFixed(2)
    .replace(/\.?0+$/, "")}%`;
}
</script>
