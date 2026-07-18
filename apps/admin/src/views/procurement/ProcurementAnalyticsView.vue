<template>
  <div class="page-stack">
    <a-card title="采购支出分析" style="margin-bottom: 16px">
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
                title="总支出"
                :value="totalSpend"
                :formatter="moneyFormatter" /></a-card
          ></a-col>
          <a-col :xs="12" :xl="6"
            ><a-card
              ><a-statistic
                title="项目采购"
                :value="projectSpend"
                :formatter="moneyFormatter"
                :value-style="{ color: '#1890ff' }" /></a-card
          ></a-col>
          <a-col :xs="12" :xl="6"
            ><a-card
              ><a-statistic
                title="部门采购"
                :value="deptSpend"
                :formatter="moneyFormatter"
                :value-style="{ color: '#722ed1' }" /></a-card
          ></a-col>
          <a-col :xs="12" :xl="6"
            ><a-card
              ><a-statistic
                title="采购单数"
                :value="orderCount"
                suffix="单" /></a-card
          ></a-col>
        </a-row>

        <a-row :gutter="12" style="margin-top: 12px">
          <a-col :xs="24" :lg="12">
            <a-card title="支出按类型" size="small">
              <div v-if="costByType.length === 0" class="chart-empty">
                暂无数据
              </div>
              <div v-else class="dist-rows">
                <div
                  v-for="item in costByType"
                  :key="item.name"
                  class="dist-row"
                >
                  <span class="dist-label">{{ item.name }}</span>
                  <div class="dist-track">
                    <div
                      class="dist-fill"
                      :style="{
                        width: distPercent(item.value, costByType) + '%',
                        background: item.color,
                      }"
                    ></div>
                  </div>
                  <span class="dist-money">{{ formatMoney(item.value) }}</span>
                </div>
              </div>
            </a-card>
          </a-col>
          <a-col :xs="24" :lg="12">
            <a-card title="支出 TOP 项目/部门" size="small">
              <div v-if="costByTarget.length === 0" class="chart-empty">
                暂无数据
              </div>
              <div v-else class="dist-rows">
                <div
                  v-for="item in costByTarget.slice(0, 10)"
                  :key="item.name"
                  class="dist-row"
                >
                  <span class="dist-label" :title="item.name">{{
                    item.name
                  }}</span>
                  <div class="dist-track">
                    <div
                      class="dist-fill"
                      :style="{
                        width: distPercent(item.value, costByTarget) + '%',
                        background: '#13c2c2',
                      }"
                    ></div>
                  </div>
                  <span class="dist-money">{{ formatMoney(item.value) }}</span>
                </div>
              </div>
            </a-card>
          </a-col>
        </a-row>
      </a-spin>
    </a-card>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { message } from "ant-design-vue";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import {
  listProcurementCostAllocations,
  listPurchaseOrders,
  type ProcurementCostAllocation,
  type PurchaseOrder,
} from "@/api/procurement";
const router = useRouter();
const loading = ref(false);
const costs = ref<ProcurementCostAllocation[]>([]);
const orders = ref<PurchaseOrder[]>([]);

const totalSpend = computed(() =>
  costs.value.reduce((s, c) => s + Number(c.amount || 0), 0),
);
const projectSpend = computed(() =>
  costs.value
    .filter((c) => c.costType === "PROJECT")
    .reduce((s, c) => s + Number(c.amount || 0), 0),
);
const deptSpend = computed(() =>
  costs.value
    .filter((c) => c.costType === "DEPARTMENT")
    .reduce((s, c) => s + Number(c.amount || 0), 0),
);
const orderCount = computed(() => orders.value.length);

const costByType = computed(() => {
  const map: { name: string; value: number; color: string }[] = [];
  const project = costs.value
    .filter((c) => c.costType === "PROJECT")
    .reduce((s, c) => s + Number(c.amount || 0), 0);
  const dept = costs.value
    .filter((c) => c.costType === "DEPARTMENT")
    .reduce((s, c) => s + Number(c.amount || 0), 0);
  if (project) map.push({ name: "项目采购", value: project, color: "#1890ff" });
  if (dept) map.push({ name: "部门采购", value: dept, color: "#722ed1" });
  return map;
});

const costByTarget = computed(() => {
  const map = new Map<string, number>();
  costs.value.forEach((c) => {
    const name = c.costTargetName || c.costTargetId?.slice(0, 8) || "未知";
    map.set(name, (map.get(name) || 0) + Number(c.amount || 0));
  });
  return Array.from(map.entries())
    .map(([name, value]) => ({ name, value }))
    .sort((a, b) => b.value - a.value);
});

function distPercent(value: number, data: { value: number }[]) {
  const max = Math.max(...data.map((d) => d.value), 1);
  return Math.round((value / max) * 100);
}

onMounted(loadData);
async function loadData() {
  loading.value = true;
  try {
    const [costData, orderData] = await Promise.all([
      listProcurementCostAllocations(),
      listPurchaseOrders(),
    ]);
    costs.value = costData;
    orders.value = orderData.content;
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
function moneyFormatter({ value }: { value: number | string }) {
  return formatMoney(Number(value));
}
</script>
<style scoped>
.chart-empty {
  text-align: center;
  padding: 32px 0;
  color: #8c8c8c;
}
.dist-rows {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.dist-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.dist-label {
  width: 80px;
  font-size: 13px;
  color: #333;
  flex-shrink: 0;
  text-align: right;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.dist-track {
  flex: 1;
  height: 22px;
  background: #f5f5f5;
  border-radius: 4px;
  overflow: hidden;
}
.dist-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.4s;
  min-width: 0;
}
.dist-money {
  font-size: 12px;
  color: #595959;
  width: 80px;
  text-align: right;
  flex-shrink: 0;
}
</style>
