<template>
  <div class="page-stack">
    <a-card>
      <template #title>库存盘点</template>
      <template #extra>
        <a-button :loading="loading" @click="loadData">
          <template #icon><ReloadOutlined /></template>刷新</a-button
        >
      </template>
      <a-table
        :columns="columns"
        :data-source="stocktakes"
        :loading="loading"
        :pagination="{ pageSize: 10 }"
        :row-key="(r: any) => r.id"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'POSTED' ? 'green' : 'orange'">
              {{ record.status === "POSTED" ? "已过账" : "草稿" }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button
              v-if="record.status !== 'POSTED'"
              size="small"
              type="primary"
              :loading="actingId === record.id"
              @click="post(record)"
              >过账</a-button
            >
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>
<script setup lang="ts">
import { onMounted, ref } from "vue";
import { message } from "ant-design-vue";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import {
  listStocktakes,
  postStocktake,
  type Stocktake,
} from "@/api/extensions";

const loading = ref(false);
const actingId = ref("");
const stocktakes = ref<Stocktake[]>([]);
const columns = [
  { title: "单号", dataIndex: "code", width: 150 },
  { title: "仓库", dataIndex: "warehouse", width: 140 },
  { title: "状态", key: "status", width: 110 },
  { title: "差异", dataIndex: "totalDifference", width: 120 },
  { title: "备注", dataIndex: "remark" },
  { title: "操作", key: "actions", width: 110 },
];

onMounted(loadData);
async function loadData() {
  loading.value = true;
  try {
    stocktakes.value = await listStocktakes();
  } catch (e: any) {
    message.error(e.message || "加载失败");
  } finally {
    loading.value = false;
  }
}
async function post(record: Stocktake) {
  actingId.value = record.id;
  try {
    await postStocktake(record.id);
    message.success("盘点已过账");
    await loadData();
  } catch (e: any) {
    message.error(e.message || "过账失败");
  } finally {
    actingId.value = "";
  }
}
</script>
