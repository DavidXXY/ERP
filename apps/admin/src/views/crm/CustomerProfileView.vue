<template>
  <div class="page-stack">
    <a-card title="客户经营画像">
      <template #extra><a-button @click="loadData">刷新</a-button></template>

      <a-row :gutter="[16, 16]" class="metric-row">
        <a-col :xs="12" :lg="6"><a-statistic title="客户数" :value="profiles.length" suffix="家" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="商机池金额" :value="opportunityAmount" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="合同总额" :value="contractAmount" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="逾期金额" :value="overdueAmount" :formatter="moneyFormatter" /></a-col>
      </a-row>

      <a-space wrap class="table-toolbar">
        <a-input v-model:value="keyword" allow-clear placeholder="搜索客户、行业或负责人" style="width: 280px" />
        <a-checkbox v-model:checked="riskOnly">仅看风险客户</a-checkbox>
      </a-space>

      <a-table :columns="columns" :data-source="filteredProfiles" :loading="loading" :row-key="(record: CustomerProfile) => record.customerId" :scroll="{ x: 1320 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'customer'">
            <a-button type="link" class="table-link" @click="openCustomer(record.customerId)">{{ record.customerName }}</a-button>
            <span class="table-subtitle">{{ record.customerCode }} · {{ record.industry }}</span>
          </template>
          <template v-else-if="column.key === 'level'"><a-tag :color="levelColor(record.level)">{{ levelLabel(record.level) }}</a-tag></template>
          <template v-else-if="column.key === 'risk'"><a-tag :color="riskColor(record.riskStatus)">{{ riskLabel(record.riskStatus) }}</a-tag></template>
          <template v-else-if="column.key === 'opportunity'">
            <strong>{{ formatMoney(record.opportunityAmount) }}</strong>
            <span class="table-subtitle">{{ record.opportunityCount }} 个商机</span>
          </template>
          <template v-else-if="column.key === 'contract'">
            <strong>{{ formatMoney(record.contractAmount) }}</strong>
            <span class="table-subtitle">{{ record.contractCount }} 份合同</span>
          </template>
          <template v-else-if="column.key === 'receivable'">
            <span :class="{ 'text-danger': record.overdueAmount > 0 }">{{ formatMoney(record.outstandingAmount) }}</span>
            <span class="table-subtitle">逾期 {{ formatMoney(record.overdueAmount) }}</span>
          </template>
          <template v-else-if="column.key === 'endDate'">{{ record.nearestContractEndDate || "-" }}</template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { message } from "ant-design-vue";
import { useRouter } from "vue-router";
import { listCustomerProfiles, type CustomerProfile } from "@/api/crm";
import { formatMoney, levelColor, levelLabel, riskColor, riskLabel } from "./crm-options";

const router = useRouter();
const profiles = ref<CustomerProfile[]>([]);
const loading = ref(false);
const keyword = ref("");
const riskOnly = ref(false);
const columns = [
  { title: "客户", key: "customer", width: 230 },
  { title: "等级", key: "level", width: 100 },
  { title: "负责人", dataIndex: "ownerName", width: 110 },
  { title: "风险", key: "risk", width: 110 },
  { title: "商机", key: "opportunity", width: 160 },
  { title: "合同", key: "contract", width: 160 },
  { title: "待收 / 逾期", key: "receivable", width: 170 },
  { title: "最近合同到期", key: "endDate", width: 140 },
];
const filteredProfiles = computed(() => {
  const term = keyword.value.trim().toLowerCase();
  return profiles.value.filter((item) => {
    const text = `${item.customerName} ${item.customerCode} ${item.industry} ${item.ownerName}`.toLowerCase();
    return (!riskOnly.value || item.riskStatus !== "NORMAL") && (!term || text.includes(term));
  });
});
const opportunityAmount = computed(() => profiles.value.reduce((sum, item) => sum + Number(item.opportunityAmount || 0), 0));
const contractAmount = computed(() => profiles.value.reduce((sum, item) => sum + Number(item.contractAmount || 0), 0));
const overdueAmount = computed(() => profiles.value.reduce((sum, item) => sum + Number(item.overdueAmount || 0), 0));

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    profiles.value = await listCustomerProfiles();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "客户经营画像加载失败");
  } finally {
    loading.value = false;
  }
}

function openCustomer(customerId: string) {
  router.push({ path: "/crm/customers", query: { customer: customerId } });
}

function moneyFormatter({ value }: { value: number }) {
  return formatMoney(value);
}
</script>
