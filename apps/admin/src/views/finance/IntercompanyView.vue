<template>
  <div class="page-stack">
    <a-card>
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="entities" tab="法人 / 账套">
          <a-table
            :columns="entityColumns"
            :data-source="entities"
            :loading="loading"
            :pagination="{ pageSize: 10 }"
            :row-key="(r: any) => r.id"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'active'">
                <a-tag :color="record.active ? 'green' : 'red'">{{
                  record.active ? "启用" : "停用"
                }}</a-tag>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="transactions" tab="内部交易">
          <a-table
            :columns="txColumns"
            :data-source="transactions"
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
  listLegalEntities,
  listIntercompanyTransactions,
  type LegalEntity,
  type IntercompanyTransaction,
} from "@/api/extensions";

const activeTab = ref("entities");
const loading = ref(false);
const entities = ref<LegalEntity[]>([]);
const transactions = ref<IntercompanyTransaction[]>([]);

const entityColumns = [
  { title: "编码", dataIndex: "code", width: 120 },
  { title: "名称", dataIndex: "name", width: 200 },
  { title: "类型", dataIndex: "entityType", width: 140 },
  { title: "币种", dataIndex: "currency", width: 100 },
  { title: "状态", key: "active", width: 100 },
];
const txColumns = [
  { title: "单号", dataIndex: "code", width: 150 },
  { title: "发起法人", dataIndex: "fromEntityName", width: 160 },
  { title: "接收法人", dataIndex: "toEntityName", width: 160 },
  { title: "金额（税价不适用，元）", dataIndex: "amount", width: 120 },
  { title: "方向", dataIndex: "direction", width: 120 },
  { title: "日期", dataIndex: "transactionDate", width: 120 },
  { title: "事由", dataIndex: "reason" },
];

onMounted(loadData);
async function loadData() {
  loading.value = true;
  try {
    const [e, t] = await Promise.all([
      listLegalEntities(),
      listIntercompanyTransactions(),
    ]);
    entities.value = e;
    transactions.value = t;
  } catch (e: any) {
    message.error(e.message || "加载失败");
  } finally {
    loading.value = false;
  }
}
</script>
