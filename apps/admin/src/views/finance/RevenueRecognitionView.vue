<template>
  <div class="page-stack">
    <a-card>
      <template #title>收入确认</template>
      <template #extra>
        <a-button :loading="loading" @click="loadData">
          <template #icon><ReloadOutlined /></template>刷新</a-button
        >
      </template>
      <a-table
        :columns="columns"
        :data-source="recognitions"
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
import {
  listRevenueRecognitions,
  type RevenueRecognition,
} from "@/api/extensions";

const loading = ref(false);
const recognitions = ref<RevenueRecognition[]>([]);
const columns = [
  { title: "单号", dataIndex: "code", width: 150 },
  { title: "合同", dataIndex: "contractId", width: 260 },
  { title: "金额", dataIndex: "amount", width: 120 },
  { title: "确认日", dataIndex: "recognizeDate", width: 120 },
  { title: "确认人", dataIndex: "recognizedBy", width: 120 },
  { title: "备注", dataIndex: "remark" },
];

onMounted(loadData);
async function loadData() {
  loading.value = true;
  try {
    recognitions.value = await listRevenueRecognitions();
  } catch (e: any) {
    message.error(e.message || "加载失败");
  } finally {
    loading.value = false;
  }
}
</script>
