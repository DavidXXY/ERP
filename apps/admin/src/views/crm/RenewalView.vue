<template>
  <div class="page-stack">
    <a-card title="续约管理">
      <template #extra><a-button @click="loadData">刷新</a-button></template>

      <a-row :gutter="[16, 16]" class="metric-row">
        <a-col :xs="12" :lg="6"><a-statistic title="续约清单" :value="renewals.length" suffix="份" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="30天内到期" :value="dueSoonCount" suffix="份" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="已到期" :value="expiredCount" suffix="份" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="关联待收" :value="outstandingAmount" :formatter="moneyFormatter" /></a-col>
      </a-row>

      <a-space wrap class="table-toolbar">
        <a-input v-model:value="keyword" allow-clear placeholder="搜索合同、项目或客户" style="width: 280px" />
        <a-select v-model:value="riskFilter" allow-clear placeholder="全部续约风险" :options="riskOptions" style="width: 150px" />
        <a-tag color="blue">当前 {{ filteredRenewals.length }} 份</a-tag>
      </a-space>

      <a-table :columns="columns" :data-source="filteredRenewals" :loading="loading" :row-key="(record: Renewal) => record.contractId" :scroll="{ x: 1220 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'contract'">
            <strong>{{ record.contractCode }}</strong>
            <span class="table-subtitle">{{ record.customerName }}</span>
          </template>
          <template v-else-if="column.key === 'project'">{{ record.projectName }}</template>
          <template v-else-if="column.key === 'amount'"><strong>{{ formatMoney(record.amount) }}</strong></template>
          <template v-else-if="column.key === 'endDate'">
            <span>{{ record.endDate }}</span>
            <span class="table-subtitle" :class="{ 'text-danger': record.daysRemaining <= 30 }">{{ daysLabel(record.daysRemaining) }}</span>
          </template>
          <template v-else-if="column.key === 'risk'"><a-tag :color="riskColor(record.renewalRisk)">{{ riskLabel(record.renewalRisk) }}</a-tag></template>
          <template v-else-if="column.key === 'outstanding'"><span :class="{ 'text-danger': record.outstandingAmount > 0 }">{{ formatMoney(record.outstandingAmount) }}</span></template>
          <template v-else-if="column.key === 'action'">
            <a-space :size="4">
              <a-button type="link" size="small" @click="viewCustomer(record)">客户</a-button>
              <a-button v-if="auth.can('crm:opportunity:create')" type="primary" size="small" @click="startRenewal(record)">发起续约</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { message } from "ant-design-vue";
import { useRouter } from "vue-router";
import { listRenewals, type Renewal } from "@/api/crm";
import { useAuthStore } from "@/stores/auth";
import { formatMoney } from "./crm-options";

const renewals = ref<Renewal[]>([]);
const auth = useAuthStore();
const router = useRouter();
const loading = ref(false);
const keyword = ref("");
const riskFilter = ref<Renewal["renewalRisk"]>();
const riskOptions = [{ label: "已到期", value: "EXPIRED" }, { label: "高风险", value: "HIGH" }, { label: "需跟进", value: "MEDIUM" }, { label: "低风险", value: "LOW" }];
const columns = [
  { title: "合同 / 客户", key: "contract", width: 230 },
  { title: "合同项目", key: "project", width: 280 },
  { title: "合同金额", key: "amount", width: 140 },
  { title: "到期时间", key: "endDate", width: 170 },
  { title: "续约风险", key: "risk", width: 110 },
  { title: "合同待收", key: "outstanding", width: 140 },
  { title: "操作", key: "action", width: 170, fixed: "right" },
];
const filteredRenewals = computed(() => {
  const term = keyword.value.trim().toLowerCase();
  return renewals.value.filter(item => {
    const text = `${item.contractCode} ${item.projectName} ${item.customerName}`.toLowerCase();
    return (!riskFilter.value || item.renewalRisk === riskFilter.value) && (!term || text.includes(term));
  });
});
const dueSoonCount = computed(() => renewals.value.filter((item) => item.daysRemaining >= 0 && item.daysRemaining <= 30).length);
const expiredCount = computed(() => renewals.value.filter((item) => item.daysRemaining < 0).length);
const outstandingAmount = computed(() => renewals.value.reduce((sum, item) => sum + Number(item.outstandingAmount || 0), 0));

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    renewals.value = await listRenewals();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "续约清单加载失败");
  } finally {
    loading.value = false;
  }
}

function daysLabel(days: number) {
  return days < 0 ? `已到期 ${Math.abs(days)} 天` : `剩余 ${days} 天`;
}

function riskLabel(risk: Renewal["renewalRisk"]) {
  return { EXPIRED: "已到期", HIGH: "高风险", MEDIUM: "需跟进", LOW: "低风险" }[risk];
}

function riskColor(risk: Renewal["renewalRisk"]) {
  return { EXPIRED: "red", HIGH: "red", MEDIUM: "orange", LOW: "green" }[risk];
}

function viewCustomer(record: Renewal) {
  router.push({ path: "/crm/customers", query: { customer: record.customerId } });
}

function startRenewal(record: Renewal) {
  router.push({
    path: "/crm/opportunities",
    query: {
      customer: record.customerId,
      create: "1",
      need: `${record.contractCode} · ${record.projectName}续约`,
      amount: String(record.amount || 0),
    },
  });
}

function moneyFormatter({ value }: { value: number }) {
  return formatMoney(value);
}
</script>
