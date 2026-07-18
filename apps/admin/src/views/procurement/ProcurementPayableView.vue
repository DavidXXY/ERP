<template>
  <div class="page-stack">
    <a-card>
      <template #title>采购应付</template>
      <template #extra
        ><a-button @click="router.push('/procurement')">返回采购管理</a-button
        ><a-button :loading="loading" @click="loadData"
          ><template #icon><ReloadOutlined /></template>刷新</a-button
        ></template
      >
      <a-table
        :columns="payableColumns"
        :data-source="payables"
        :loading="loading"
        :pagination="{ pageSize: 10 }"
        :row-key="(r: any) => r.id"
        :scroll="{ x: 1100 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'payable'"
            ><strong>{{ record.code }}</strong
            ><span class="table-subtitle">{{
              record.supplierName || ""
            }}</span></template
          >
          <template v-else-if="column.key === 'amount'"
            ><strong>{{ formatMoney(record.amount) }}</strong
            ><span class="table-subtitle"
              >税率 {{ formatTaxRate(record.taxRate) }}</span
            ></template
          >
          <template v-else-if="column.key === 'paid'"
            >{{ formatMoney(record.paidAmount || 0) }}<br /><span
              class="table-subtitle"
              >待付 {{ formatMoney(record.outstandingAmount || 0) }}</span
            ></template
          >
          <template v-else-if="column.key === 'dueDate'">{{
            record.dueDate || "-"
          }}</template>
          <template v-else-if="column.key === 'status'"
            ><a-tag
              :color="
                statusColor(
                  record.status,
                  {
                    PENDING: 'orange',
                    PARTIAL_PAID: 'blue',
                    PAID: 'green',
                    CANCELLED: 'red',
                  },
                  'default',
                )
              "
              >{{
                statusLabel(record.status, {
                  PENDING: "待付款",
                  PARTIAL_PAID: "部分已付",
                  PAID: "已付清",
                  CANCELLED: "已取消",
                })
              }}</a-tag
            ></template
          >
        </template>
      </a-table>
    </a-card>
  </div>
</template>
<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { message } from "ant-design-vue";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import {
  listProcurementPayables,
  type ProcurementPayable,
} from "@/api/procurement";
import { statusLabel, statusColor } from "@/utils/status-mapper";
const router = useRouter();
const loading = ref(false);
const payables = ref<ProcurementPayable[]>([]);
const payableColumns = [
  { title: "应付单", key: "payable", width: 240 },
  { title: "金额", key: "amount", width: 140 },
  { title: "已付/待付", key: "paid", width: 200 },
  { title: "到期日", key: "dueDate", width: 120 },
  { title: "状态", key: "status", width: 110 },
];
onMounted(loadData);
async function loadData() {
  loading.value = true;
  try {
    payables.value = await listProcurementPayables();
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
