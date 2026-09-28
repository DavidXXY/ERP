<template>
  <div class="page-stack">
    <a-card>
      <template #title>薪酬核算</template>
      <template #extra>
        <a-button :loading="loading" @click="loadData">
          <template #icon><ReloadOutlined /></template>刷新</a-button
        >
      </template>
      <a-table
        :columns="columns"
        :data-source="runs"
        :loading="loading"
        :pagination="{ pageSize: 10 }"
        :row-key="(r: any) => r.id"
        size="small"
      />
    </a-card>
  </div>
</template>
<script setup lang="ts">
import { onMounted, ref } from "vue";
import { message } from "ant-design-vue";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { listPayrollRuns, type PayrollRun } from "@/api/extensions";

const loading = ref(false);
const runs = ref<PayrollRun[]>([]);
const columns = [
  { title: "单号", dataIndex: "code", width: 150 },
  { title: "期间", dataIndex: "period", width: 120 },
  { title: "应发", dataIndex: "grossAmount", width: 130 },
  { title: "实发", dataIndex: "netAmount", width: 130 },
  { title: "社保", dataIndex: "socialAmount", width: 130 },
  { title: "个税", dataIndex: "taxAmount", width: 130 },
  { title: "状态", dataIndex: "status", width: 110 },
];

onMounted(loadData);
async function loadData() {
  loading.value = true;
  try {
    runs.value = await listPayrollRuns();
  } catch (e: any) {
    message.error(e.message || "加载失败");
  } finally {
    loading.value = false;
  }
}
</script>
