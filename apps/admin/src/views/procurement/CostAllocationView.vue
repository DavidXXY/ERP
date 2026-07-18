<template>
  <div class="page-stack">
    <a-card>
      <template #title>成本归集</template>
      <template #extra
        ><a-button @click="router.push('/procurement')">返回采购管理</a-button
        ><a-button :loading="loading" @click="loadData"
          ><template #icon><ReloadOutlined /></template>刷新</a-button
        ></template
      >
      <a-table
        :columns="costColumns"
        :data-source="costs"
        :loading="loading"
        :pagination="{ pageSize: 10 }"
        :row-key="(r: any) => r.id"
        :scroll="{ x: 1100 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'cost'"
            ><strong>{{ record.code || "-" }}</strong
            ><span class="table-subtitle">{{
              record.description || ""
            }}</span></template
          >
          <template v-else-if="column.key === 'type'">{{
            statusLabel(record.costType, {
              PROJECT: "项目",
              DEPARTMENT: "部门",
              OTHER: "其他",
            })
          }}</template>
          <template v-else-if="column.key === 'amount'">{{
            formatMoney(record.amount)
          }}</template>
          <template v-else-if="column.key === 'date'">{{
            record.createdAt?.slice(0, 10) || "-"
          }}</template>
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
  listProcurementCostAllocations,
  type ProcurementCostAllocation,
} from "@/api/procurement";
import { statusLabel, statusColor } from "@/utils/status-mapper";
const router = useRouter();
const loading = ref(false);
const costs = ref<ProcurementCostAllocation[]>([]);
const costColumns = [
  { title: "成本单", key: "cost", width: 240 },
  { title: "类型", key: "type", width: 120 },
  { title: "金额", key: "amount", width: 140 },
  { title: "日期", key: "date", width: 120 },
];
onMounted(loadData);
async function loadData() {
  loading.value = true;
  try {
    costs.value = await listProcurementCostAllocations();
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
</script>
