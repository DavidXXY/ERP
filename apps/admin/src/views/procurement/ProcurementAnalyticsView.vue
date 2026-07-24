<template>
  <div class="page-stack">
    <a-card title="采购支出分析" style="margin-bottom: 16px">
      <template #extra
        ><a-select
          v-model:value="selectedYear"
          :options="yearOptions"
          style="width: 110px"
        />
        <a-button @click="router.push('/procurement')">返回采购管理</a-button
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
        <a-row :gutter="12" style="margin-top: 12px">
          <a-col :xs="24" :lg="14">
            <a-card title="月度采购支出" size="small">
              <a-table
                :data-source="monthlySpend"
                :columns="monthlyColumns"
                row-key="month"
                size="small"
                :pagination="false"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'amount'">{{
                    formatMoney(record.amount)
                  }}</template>
                  <template v-else-if="column.key === 'share'"
                    >{{ record.share }}%</template
                  >
                </template>
              </a-table>
            </a-card>
          </a-col>
          <a-col :xs="24" :lg="10">
            <a-card title="供应商支出排名" size="small">
              <a-table
                :data-source="supplierSpend"
                :columns="supplierColumns"
                row-key="name"
                size="small"
                :pagination="{ pageSize: 6 }"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'amount'">{{
                    formatMoney(record.amount)
                  }}</template>
                </template>
              </a-table>
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
const selectedYear = ref(new Date().getFullYear());
const yearOptions = computed(() => {
  const years = new Set(
    costs.value
      .map((item) => Number(item.incurredDate?.slice(0, 4)))
      .filter(Boolean),
  );
  years.add(new Date().getFullYear());
  return Array.from(years)
    .sort((a, b) => b - a)
    .map((value) => ({ label: `${value}年`, value }));
});
const filteredCosts = computed(() =>
  costs.value.filter(
    (item) => Number(item.incurredDate?.slice(0, 4)) === selectedYear.value,
  ),
);

const totalSpend = computed(() =>
  filteredCosts.value.reduce((s, c) => s + Number(c.amount || 0), 0),
);
const projectSpend = computed(() =>
  filteredCosts.value
    .filter((c) => c.costType === "PROJECT")
    .reduce((s, c) => s + Number(c.amount || 0), 0),
);
const deptSpend = computed(() =>
  filteredCosts.value
    .filter((c) => c.costType === "DEPARTMENT")
    .reduce((s, c) => s + Number(c.amount || 0), 0),
);
const orderCount = computed(
  () => new Set(filteredCosts.value.map((item) => item.orderId)).size,
);

const costByType = computed(() => {
  const map: { name: string; value: number; color: string }[] = [];
  const project = filteredCosts.value
    .filter((c) => c.costType === "PROJECT")
    .reduce((s, c) => s + Number(c.amount || 0), 0);
  const dept = filteredCosts.value
    .filter((c) => c.costType === "DEPARTMENT")
    .reduce((s, c) => s + Number(c.amount || 0), 0);
  if (project) map.push({ name: "项目采购", value: project, color: "#1890ff" });
  if (dept) map.push({ name: "部门采购", value: dept, color: "#722ed1" });
  return map;
});

const costByTarget = computed(() => {
  const map = new Map<string, number>();
  filteredCosts.value.forEach((c) => {
    const name = c.costTargetName || c.costTargetId?.slice(0, 8) || "未知";
    map.set(name, (map.get(name) || 0) + Number(c.amount || 0));
  });
  return Array.from(map.entries())
    .map(([name, value]) => ({ name, value }))
    .sort((a, b) => b.value - a.value);
});

const monthlySpend = computed(() => {
  const total = totalSpend.value;
  return Array.from({ length: 12 }, (_, index) => {
    const month = String(index + 1).padStart(2, "0");
    const amount = filteredCosts.value
      .filter((item) => item.incurredDate?.slice(5, 7) === month)
      .reduce((sum, item) => sum + Number(item.amount || 0), 0);
    return {
      month: `${month}月`,
      amount,
      share: total ? ((amount / total) * 100).toFixed(1) : "0.0",
    };
  });
});
const supplierSpend = computed(() => {
  const orderMap = new Map(orders.value.map((item) => [item.id, item]));
  const map = new Map<string, number>();
  filteredCosts.value.forEach((item) => {
    const name = orderMap.get(item.orderId)?.supplierName || "未知供应商";
    map.set(name, (map.get(name) || 0) + Number(item.amount || 0));
  });
  return Array.from(map.entries())
    .map(([name, amount]) => ({ name, amount }))
    .sort((a, b) => b.amount - a.amount);
});
const monthlyColumns = [
  { title: "月份", dataIndex: "month" },
  { title: "支出", key: "amount" },
  { title: "年度占比", key: "share" },
];
const supplierColumns = [
  { title: "供应商", dataIndex: "name" },
  { title: "采购支出", key: "amount" },
];

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
