<template>
  <div class="page-stack">
    <a-card>
      <template #title>固定资产</template>
      <template #extra>
        <a-button :loading="loading" @click="loadData">
          <template #icon><ReloadOutlined /></template>刷新</a-button
        >
      </template>
      <a-table
        :columns="columns"
        :data-source="assets"
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
import { listFixedAssets, type FixedAsset } from "@/api/extensions";

const loading = ref(false);
const assets = ref<FixedAsset[]>([]);
const columns = [
  { title: "编码", dataIndex: "code", width: 150 },
  { title: "名称", dataIndex: "name", width: 180 },
  { title: "类别", dataIndex: "category", width: 120 },
  { title: "原值", dataIndex: "originalValue", width: 130 },
  { title: "累计折旧", dataIndex: "accumulatedDepreciation", width: 130 },
  { title: "净值", dataIndex: "netValue", width: 130 },
  { title: "状态", dataIndex: "status", width: 110 },
  { title: "取得日", dataIndex: "acquisitionDate", width: 120 },
];

onMounted(loadData);
async function loadData() {
  loading.value = true;
  try {
    assets.value = await listFixedAssets();
  } catch (e: any) {
    message.error(e.message || "加载失败");
  } finally {
    loading.value = false;
  }
}
</script>
