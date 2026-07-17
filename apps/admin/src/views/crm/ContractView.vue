<template>
  <div class="page-stack">
    <a-card title="客户合同">
      <template #extra><a-button @click="loadData">刷新</a-button><a-button @click="handleExportCsv"><template #icon><DownloadOutlined /></template>导出</a-button></template>

      <a-row :gutter="[16, 16]" class="metric-row">
        <a-col :xs="12" :lg="4"><a-statistic title="合同总数" :value="contracts.length" suffix="份" /></a-col>
        <a-col :xs="12" :lg="4"><a-statistic title="合同总额" :value="totalAmount" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="4"><a-statistic title="已开票金额" :value="totalInvoicedAmount" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="4"><a-statistic title="已回款金额" :value="totalReceivedAmount" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="4"><a-statistic title="履约中" :value="activeCount" suffix="份" /></a-col>
        <a-col :xs="12" :lg="4"><a-statistic title="需关注" :value="riskCount" suffix="份" /></a-col>
      </a-row>

      <a-space wrap class="table-toolbar">
        <a-select v-model:value="statusFilter" allow-clear placeholder="全部状态" :options="statusOptions" style="width: 150px" />
        <a-select v-model:value="salesFilter" allow-clear show-search option-filter-prop="label" placeholder="全部销售人员" :options="salesOptions" style="width: 170px" />
        <a-select v-model:value="dateFilterMode" :options="dateFilterModeOptions" style="width: 110px" />
        <template v-if="dateFilterMode === 'RANGE'">
          <a-input v-model:value="dateRangeStart" type="date" style="width: 150px" />
          <a-input v-model:value="dateRangeEnd" type="date" style="width: 150px" />
        </template>
        <a-input v-else-if="dateFilterMode === 'MONTH'" v-model:value="dateMonth" type="month" style="width: 150px" />
        <a-input v-else v-model:value="dateYear" type="number" min="2000" max="2100" placeholder="年份" style="width: 120px" />
        <a-button @click="clearDateFilter">清空日期</a-button>
        <a-input v-model:value="keyword" allow-clear placeholder="搜索合同编号、项目或客户" style="width: 280px" />
      </a-space>

      <!-- desktop-table --><div class="desktop-table">
<a-table :columns="columns" :data-source="filteredContracts" :loading="loading" row-key="id" :scroll="{ x: 1360 }" :customRow="customRow">
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
            <span>签订/开始 {{ record.startDate || "-" }}</span>
            <span class="table-subtitle">结束 {{ record.endDate || "-" }} · {{ record.serviceCycle || "未设置服务频次" }}</span>
          </template>
          <template v-else-if="column.key === 'sales'">
            <span>{{ record.salesOwnerName || "未关联销售" }}</span>
          </template>
          <template v-else-if="column.key === 'amount'"><strong>{{ formatMoney(record.amount) }}</strong><span class="table-subtitle">未税 {{ formatMoney(record.netAmount ?? calcNetAmount(record.amount, record.taxRate)) }} · 税率 {{ formatTaxRate(record.taxRate) }}</span></template>
          <template v-else-if="column.key === 'invoiced'"><strong>{{ formatMoney(contractFinancial(record).invoicedAmount) }}</strong></template>
          <template v-else-if="column.key === 'received'"><strong>{{ formatMoney(contractFinancial(record).receivedAmount) }}</strong></template>
          <template v-else-if="column.key === 'status'"><a-tag :color="contractStatusColor(record.status)">{{ contractStatusLabel(record.status) }}</a-tag></template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small" @click.stop>
              <a-button size="small" type="link" @click="openDetail(record)">详情</a-button>
              <a-button
                v-if="record.status === 'PENDING_SEAL'"
                size="small"
                type="link"
                @click="openSignedUpload(record)"
              >
                上传盖章件
              </a-button>
              <a-popconfirm
                v-if="auth.can('crm:contract:delete')"
                title="确实删除此合同？"
                @confirm="handleDeleteContract(record)"
              >
                <a-button size="small" type="link" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
</div><!-- end desktop-table -->
    <div class="mobile-only">
      <div v-for="record in filteredContracts" :key="record.id" class="mobile-card-item" @click="router.push('/crm/contracts/' + record.id)">
        <div class="mobile-card-header"><strong>{{ record.code }}</strong><a-tag :color="contractStatusColor(record.status)">{{ contractStatusLabel(record.status) }}</a-tag></div>
        <div class="mobile-card-body"><span>{{ record.projectName || record.customerName }}</span><strong>{{ formatMoney(record.amount) }}</strong></div>
        <div class="mobile-card-tags">已开票 {{ formatMoney(contractFinancial(record).invoicedAmount) }} · 已回款 {{ formatMoney(contractFinancial(record).receivedAmount) }}</div>
        <div class="mobile-card-tags">未税 {{ formatMoney(record.netAmount ?? calcNetAmount(record.amount, record.taxRate)) }} · 税率 {{ formatTaxRate(record.taxRate) }}</div>
        <div class="mobile-card-footer"><span>{{ record.startDate || "" }} {{ record.endDate ? "~ " + record.endDate : "" }} · {{ record.salesOwnerName || "未关联销售" }}</span></div>
      </div>
    </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import DownloadOutlined from "@ant-design/icons-vue/DownloadOutlined";
import { useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import { exportContractsExcel } from "@/api/crm";
import { message } from "ant-design-vue";
import { deleteContract } from "@/api/crm";
import { listContracts, listReceivables, type ContractStatus, type Receivable, type ServiceContract } from "@/api/crm";
import { listUsersApi, type UserResponse } from "@/api/system";
import { contractStatusColor, contractStatusLabel, formatMoney } from "./crm-options";
import { downloadCsv, contractRowToCsv } from "./crm-export";

const router = useRouter();
const auth = useAuthStore();
const contracts = ref<ServiceContract[]>([]);
const receivables = ref<Receivable[]>([]);
const loading = ref(false);
const keyword = ref("");
const statusFilter = ref<ContractStatus>();
const salesFilter = ref<string>();
const users = ref<UserResponse[]>([]);
const dateFilterMode = ref<"RANGE" | "MONTH" | "YEAR">("RANGE");
const dateRangeStart = ref("");
const dateRangeEnd = ref("");
const dateMonth = ref("");
const dateYear = ref("");
const dateFilterModeOptions = [
  { label: "日期范围", value: "RANGE" },
  { label: "按月", value: "MONTH" },
  { label: "按年", value: "YEAR" },
];
const statusOptions = [
  { label: "合同审批中", value: "PENDING_APPROVAL" },
  { label: "待双方盖章", value: "PENDING_SEAL" },
  { label: "盖章件审批中", value: "SEAL_APPROVAL" },
  { label: "履约中", value: "ACTIVE" },
  { label: "待续约", value: "RENEWAL_PENDING" },
  { label: "履约风险", value: "OVERDUE_RISK" },
  { label: "已关闭", value: "CLOSED" },
];
const columns = [
  { title: "合同 / 客户", key: "contract", width: 230 },
  { title: "项目", key: "project", width: 280 },
  { title: "合同日期", key: "period", width: 270 },
  { title: "销售人员", key: "sales", width: 120 },
  { title: "金额", key: "amount", width: 180 },
  { title: "已开票", key: "invoiced", width: 140 },
  { title: "已回款", key: "received", width: 140 },
  { title: "状态", key: "status", width: 110 },
  { title: "操作", key: "action", width: 210 }, 
];
const filteredContracts = computed(() => {
  const term = keyword.value.trim().toLowerCase();
  return contracts.value.filter((item) => {
    const text = `${item.code} ${item.projectName} ${item.customerName} ${item.salesOwnerName || ""}`.toLowerCase();
    return (!statusFilter.value || item.status === statusFilter.value)
      && (!salesFilter.value || item.salesOwnerName === salesFilter.value)
      && matchesContractDate(item)
      && (!term || text.includes(term));
  });
});
const salesOptions = computed(() => {
  const names = new Set<string>();
  users.value.forEach((item) => {
    const name = item.displayName || item.username;
    if (name) names.add(name);
  });
  contracts.value.forEach((item) => {
    if (item.salesOwnerName) names.add(item.salesOwnerName);
  });
  return Array.from(names)
    .sort((a, b) => a.localeCompare(b, "zh-CN"))
    .map((name) => ({ label: name, value: name }));
});
const totalAmount = computed(() => contracts.value.reduce((sum, item) => sum + Number(item.amount || 0), 0));
const totalInvoicedAmount = computed(() => contracts.value.reduce((sum, item) => sum + contractFinancial(item).invoicedAmount, 0));
const totalReceivedAmount = computed(() => contracts.value.reduce((sum, item) => sum + contractFinancial(item).receivedAmount, 0));
const activeCount = computed(() => contracts.value.filter((item) => item.status === "ACTIVE").length);
const riskCount = computed(() => contracts.value.filter((item) => item.status === "RENEWAL_PENDING" || item.status === "OVERDUE_RISK").length);

async function handleDeleteContract(record: any) {
  try {
    await deleteContract(record.id);
    message.success("合同已删除");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "删除失败");
  }
}

function handleExportCsv() {
  const headers = ["合同编号", "客户名称", "项目名称", "合同类型", "合同金额", "未税金额", "已开票金额", "已回款金额", "开始日期", "结束日期", "销售人员", "状态"];
  const rows = filteredContracts.value.map((r) => {
    const financial = contractFinancial(r);
    const base = contractRowToCsv(r);
    return [...base.slice(0, 6), String(financial.invoicedAmount), String(financial.receivedAmount), ...base.slice(6)];
  });
  downloadCsv("客户合同.csv", headers, rows);
}

function openDetail(record: ServiceContract) {
  router.push("/crm/contracts/" + record.id);
}

function openSignedUpload(record: ServiceContract) {
  router.push({ path: "/crm/contracts/" + record.id, query: { tab: "signed" } });
}

function clearDateFilter() {
  dateRangeStart.value = "";
  dateRangeEnd.value = "";
  dateMonth.value = "";
  dateYear.value = "";
}

function matchesContractDate(record: ServiceContract) {
  if (dateFilterMode.value === "MONTH") {
    if (!dateMonth.value) return true;
    const start = dateMonth.value + "-01";
    const end = monthEnd(dateMonth.value);
    return dateRangesOverlap(record.startDate, record.endDate, start, end);
  }
  if (dateFilterMode.value === "YEAR") {
    if (!dateYear.value) return true;
    return dateRangesOverlap(record.startDate, record.endDate, `${dateYear.value}-01-01`, `${dateYear.value}-12-31`);
  }
  return dateRangesOverlap(record.startDate, record.endDate, dateRangeStart.value, dateRangeEnd.value);
}

function dateRangesOverlap(contractStart?: string, contractEnd?: string, filterStart?: string, filterEnd?: string) {
  if (!filterStart && !filterEnd) return true;
  const start = contractStart || contractEnd;
  const end = contractEnd || contractStart;
  if (!start && !end) return false;
  const actualStart = start || "0000-01-01";
  const actualEnd = end || "9999-12-31";
  const targetStart = filterStart || "0000-01-01";
  const targetEnd = filterEnd || "9999-12-31";
  return actualStart <= targetEnd && actualEnd >= targetStart;
}

function monthEnd(month: string) {
  const [year, monthNo] = month.split("-").map(Number);
  const lastDay = new Date(year, monthNo, 0).getDate();
  return `${year}-${String(monthNo).padStart(2, "0")}-${String(lastDay).padStart(2, "0")}`;
}

const customRow = (record: ServiceContract) => ({
  onClick: (e: MouseEvent) => {
    if ((e.target as HTMLElement)?.closest?.('button,a,.ant-btn,.ant-tag,.ant-popconfirm,.ant-dropdown-trigger,input,select,[role=button]')) return;
    openDetail(record);
  },
});

onMounted(loadData);

async function doExport() { try { await exportContractsExcel(); } catch(e: any) { message.error(e.message || "导出失败"); } }
async function loadData() {
  loading.value = true;
  try {
    const [contractRows, receivableRows, userPage] = await Promise.all([
      listContracts(),
      listReceivables().catch(() => [] as Receivable[]),
      listUsersApi(0, 999).catch(() => ({ content: [] as UserResponse[] })),
    ]);
    contracts.value = contractRows;
    receivables.value = receivableRows;
    users.value = userPage.content;
  } catch (error) {
    message.error(error instanceof Error ? error.message : "合同加载失败");
  } finally {
    loading.value = false;
  }
}

function moneyFormatter({ value }: { value: number }) {
  return formatMoney(value);
}

function formatTaxRate(value?: number) {
  return `${Number(value ?? 13).toFixed(2).replace(/\.?0+$/, "")}%`;
}

function calcNetAmount(amount?: number, taxRate?: number) {
  const rate = Number(taxRate ?? 13);
  const divisor = 1 + rate / 100;
  return divisor > 0 ? Number(amount || 0) / divisor : Number(amount || 0);
}

function contractFinancial(record: ServiceContract) {
  return receivables.value
    .filter((item) => item.contractId === record.id || item.contractCode === record.code || item.sourceNo === record.code)
    .reduce((summary, item) => ({
      invoicedAmount: summary.invoicedAmount + (item.invoiceNo ? Number(item.amount || 0) : 0),
      receivedAmount: summary.receivedAmount + Number(item.settledAmount || 0),
    }), { invoicedAmount: 0, receivedAmount: 0 });
}
</script>
