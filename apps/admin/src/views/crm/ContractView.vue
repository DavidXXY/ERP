<template>
  <div class="page-stack">
    <a-card title="客户合同">
      <template #extra><a-button @click="loadData">刷新</a-button></template>

      <a-row :gutter="[16, 16]" class="metric-row">
        <a-col :xs="12" :lg="6"><a-statistic title="合同总数" :value="contracts.length" suffix="份" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="合同总额" :value="totalAmount" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="履约中" :value="activeCount" suffix="份" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="需关注" :value="riskCount" suffix="份" /></a-col>
      </a-row>

      <a-space wrap class="table-toolbar">
        <a-select v-model:value="statusFilter" allow-clear placeholder="全部状态" :options="statusOptions" style="width: 150px" />
        <a-input v-model:value="keyword" allow-clear placeholder="搜索合同编号、项目或客户" style="width: 280px" />
      </a-space>

      <a-table :columns="columns" :data-source="filteredContracts" :loading="loading" :row-key="(record: ServiceContract) => record.id" :scroll="{ x: 1120 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'contract'">
            <strong>{{ record.code }}</strong>
            <span class="table-subtitle">{{ record.customerName }}</span>
          </template>
          <template v-else-if="column.key === 'project'">
            <span>{{ record.projectName }}</span>
            <span class="table-subtitle">{{ record.contractType }}</span>
          </template>
          <template v-else-if="column.key === 'period'">
            <span>{{ record.startDate }} 至 {{ record.endDate }}</span>
            <span class="table-subtitle">{{ record.serviceCycle || "未设置服务频次" }}</span>
          </template>
          <template v-else-if="column.key === 'amount'"><strong>{{ formatMoney(record.amount) }}</strong></template>
          <template v-else-if="column.key === 'status'"><a-tag :color="contractStatusColor(record.status)">{{ contractStatusLabel(record.status) }}</a-tag></template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { message } from "ant-design-vue";
import { listContracts, type ContractStatus, type ServiceContract } from "@/api/crm";
import { contractStatusColor, contractStatusLabel, formatMoney } from "./crm-options";

const contracts = ref<ServiceContract[]>([]);
const loading = ref(false);
const keyword = ref("");
const statusFilter = ref<ContractStatus>();
const statusOptions = [
  { label: "履约中", value: "ACTIVE" },
  { label: "待续约", value: "RENEWAL_PENDING" },
  { label: "履约风险", value: "OVERDUE_RISK" },
  { label: "已关闭", value: "CLOSED" },
];
const columns = [
  { title: "合同 / 客户", key: "contract", width: 230 },
  { title: "项目", key: "project", width: 280 },
  { title: "合同周期", key: "period", width: 270 },
  { title: "金额", key: "amount", width: 140 },
  { title: "状态", key: "status", width: 110 },
];
const filteredContracts = computed(() => {
  const term = keyword.value.trim().toLowerCase();
  return contracts.value.filter((item) => {
    const text = `${item.code} ${item.projectName} ${item.customerName}`.toLowerCase();
    return (!statusFilter.value || item.status === statusFilter.value) && (!term || text.includes(term));
  });
});
const totalAmount = computed(() => contracts.value.reduce((sum, item) => sum + Number(item.amount || 0), 0));
const activeCount = computed(() => contracts.value.filter((item) => item.status === "ACTIVE").length);
const riskCount = computed(() => contracts.value.filter((item) => item.status === "RENEWAL_PENDING" || item.status === "OVERDUE_RISK").length);

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    contracts.value = await listContracts();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "合同加载失败");
  } finally {
    loading.value = false;
  }
}

function moneyFormatter({ value }: { value: number }) {
  return formatMoney(value);
}
</script>
