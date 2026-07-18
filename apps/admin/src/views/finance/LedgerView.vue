<template>
  <div class="page-stack">
    <a-card title="总账与财务报表">
      <template #extra>
        <a-space>
          <a-button @click="exportVouchers">导出凭证</a-button>
          <a-button @click="exportStatements">导出报表</a-button>
          <a-button :loading="loading" @click="loadData">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </a-space>
      </template>

      <a-row :gutter="[16, 16]" class="metric-row">
        <a-col :xs="12" :xl="4"
          ><a-statistic
            title="记账凭证"
            :value="overview.voucherCount"
            suffix="张"
        /></a-col>
        <a-col :xs="12" :xl="4"
          ><a-statistic
            title="累计借方"
            :value="overview.totalDebit"
            :formatter="moneyFormatter"
        /></a-col>
        <a-col :xs="12" :xl="4"
          ><a-statistic
            title="营业收入"
            :value="overview.revenue"
            :formatter="moneyFormatter"
        /></a-col>
        <a-col :xs="12" :xl="4"
          ><a-statistic
            title="成本费用"
            :value="overview.expense"
            :formatter="moneyFormatter"
        /></a-col>
        <a-col :xs="12" :xl="4"
          ><a-statistic
            title="账面利润"
            :value="overview.profit"
            :formatter="moneyFormatter"
            :value-style="profitStyle"
        /></a-col>
        <a-col :xs="12" :xl="4"
          ><a-statistic
            title="银行净流量"
            :value="overview.cashBalance"
            :formatter="moneyFormatter"
        /></a-col>
      </a-row>

      <section class="ledger-check-panel">
        <div class="check-card">
          <span>试算平衡</span>
          <strong :class="{ 'text-danger': trialBalanceDiff !== 0 }">{{
            formatMoney(Math.abs(trialBalanceDiff))
          }}</strong>
          <small>{{ trialBalanceDiff === 0 ? "借贷相等" : "借贷不平" }}</small>
        </div>
        <div class="check-card">
          <span>资产负债差额</span>
          <strong :class="{ 'text-danger': balanceSheetDiff !== 0 }">{{
            formatMoney(Math.abs(balanceSheetDiff))
          }}</strong>
          <small>{{
            balanceSheetDiff === 0 ? "勾稽正常" : "需复核权益/利润结转"
          }}</small>
        </div>
        <div class="check-card">
          <span>未记账/冲销</span>
          <strong>{{ reversedCount }}</strong>
          <small>凭证异常关注</small>
        </div>
        <div class="check-card">
          <span>业务来源</span>
          <strong>{{ voucherTypeStats.length }}</strong>
          <small>已接入凭证类型</small>
        </div>
      </section>

      <section class="ledger-source-panel">
        <div class="panel-heading">
          <h3>凭证来源分布</h3>
          <a-tag color="blue">按张数/金额</a-tag>
        </div>
        <div class="source-grid">
          <button
            v-for="item in voucherTypeStats"
            :key="item.type"
            class="source-card"
            type="button"
            @click="bizTypeFilter = item.type"
          >
            <span>{{ bizTypeLabel(item.type) }}</span>
            <strong>{{ item.count }} 张</strong>
            <small>{{ formatMoney(item.amount) }}</small>
          </button>
          <a-empty
            v-if="voucherTypeStats.length === 0"
            :image="simpleImage"
            description="暂无凭证来源"
          />
        </div>
      </section>
    </a-card>

    <a-card>
      <a-tabs v-model:active-key="activeTab">
        <a-tab-pane key="vouchers" tab="会计凭证">
          <a-space wrap class="table-toolbar">
            <a-input-search
              v-model:value="keyword"
              allow-clear
              placeholder="搜索凭证、业务单号、摘要"
              style="width: 280px"
            />
            <a-select
              v-model:value="bizTypeFilter"
              allow-clear
              placeholder="业务来源"
              :options="bizTypeOptions"
              style="width: 150px"
            />
            <a-select
              v-model:value="statusFilter"
              allow-clear
              placeholder="凭证状态"
              :options="statusOptions"
              style="width: 140px"
            />
          </a-space>
          <a-table
            :columns="voucherColumns"
            :data-source="filteredVouchers"
            :loading="loading"
            :pagination="{ pageSize: 8 }"
            :row-key="(item: AccountingVoucher) => item.id"
            :scroll="{ x: 1160 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'voucher'">
                <a-button
                  type="link"
                  class="table-link"
                  @click="openVoucher(record)"
                  >{{ record.code }}</a-button
                >
                <span class="table-subtitle">{{ record.description }}</span>
              </template>
              <template v-else-if="column.key === 'bizType'">
                <a-tag>{{ bizTypeLabel(record.bizType) }}</a-tag>
              </template>
              <template v-else-if="column.key === 'amount'">{{
                formatMoney(record.totalDebit)
              }}</template>
              <template v-else-if="column.key === 'balance'">
                <a-tag :color="isVoucherBalanced(record) ? 'green' : 'red'">{{
                  isVoucherBalanced(record) ? "平衡" : "不平"
                }}</a-tag>
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag
                  :color="record.status === 'POSTED' ? 'green' : 'default'"
                  >{{ record.status === "POSTED" ? "已记账" : "已冲销" }}</a-tag
                >
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button type="link" size="small" @click="openVoucher(record)"
                  >查看分录</a-button
                >
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="statements" tab="财务报表">
          <a-descriptions
            bordered
            :column="{ xs: 1, md: 3 }"
            class="statement-summary"
          >
            <a-descriptions-item label="资产合计">{{
              formatMoney(statements.totalAssets)
            }}</a-descriptions-item>
            <a-descriptions-item label="负债合计">{{
              formatMoney(statements.totalLiabilities)
            }}</a-descriptions-item>
            <a-descriptions-item label="本期利润">
              <strong :class="{ 'text-danger': statements.profit < 0 }">{{
                formatMoney(statements.profit)
              }}</strong>
            </a-descriptions-item>
            <a-descriptions-item label="营业收入">{{
              formatMoney(statements.totalRevenue)
            }}</a-descriptions-item>
            <a-descriptions-item label="成本费用">{{
              formatMoney(statements.totalExpense)
            }}</a-descriptions-item>
            <a-descriptions-item label="现金净流量">{{
              formatMoney(statements.netCashFlow)
            }}</a-descriptions-item>
          </a-descriptions>
          <a-row :gutter="[16, 16]">
            <a-col :xs="24" :xl="12">
              <h3>资产负债表</h3>
              <a-table
                size="small"
                :columns="statementColumns"
                :data-source="[...statements.assets, ...statements.liabilities]"
                :pagination="false"
                :row-key="(item: StatementLine) => item.accountCode"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'debit'">{{
                    formatMoney(record.debit)
                  }}</template>
                  <template v-else-if="column.key === 'credit'">{{
                    formatMoney(record.credit)
                  }}</template>
                  <template v-else-if="column.key === 'balance'">{{
                    formatMoney(record.balance)
                  }}</template>
                </template>
              </a-table>
            </a-col>
            <a-col :xs="24" :xl="12">
              <h3>利润表</h3>
              <a-table
                size="small"
                :columns="statementColumns"
                :data-source="[...statements.revenue, ...statements.expenses]"
                :pagination="false"
                :row-key="(item: StatementLine) => item.accountCode"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'debit'">{{
                    formatMoney(record.debit)
                  }}</template>
                  <template v-else-if="column.key === 'credit'">{{
                    formatMoney(record.credit)
                  }}</template>
                  <template v-else-if="column.key === 'balance'">{{
                    formatMoney(record.balance)
                  }}</template>
                </template>
              </a-table>
            </a-col>
          </a-row>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-drawer v-model:open="drawerOpen" title="会计凭证分录" width="680">
      <template v-if="selectedVoucher">
        <a-descriptions bordered :column="2" size="small">
          <a-descriptions-item label="凭证号">{{
            selectedVoucher.code
          }}</a-descriptions-item>
          <a-descriptions-item label="日期">{{
            selectedVoucher.voucherDate
          }}</a-descriptions-item>
          <a-descriptions-item label="业务来源"
            >{{ bizTypeLabel(selectedVoucher.bizType) }} ·
            {{ selectedVoucher.bizNo }}</a-descriptions-item
          >
          <a-descriptions-item label="借贷合计">{{
            formatMoney(selectedVoucher.totalDebit)
          }}</a-descriptions-item>
          <a-descriptions-item label="摘要" :span="2">{{
            selectedVoucher.description
          }}</a-descriptions-item>
        </a-descriptions>
        <a-divider />
        <a-table
          size="small"
          :columns="entryColumns"
          :data-source="selectedVoucher.entries"
          :pagination="false"
          :row-key="(item: VoucherEntry) => item.id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'debit'">{{
              record.debit ? formatMoney(record.debit) : "-"
            }}</template>
            <template v-else-if="column.key === 'credit'">{{
              record.credit ? formatMoney(record.credit) : "-"
            }}</template>
          </template>
        </a-table>
      </template>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { Empty, message } from "ant-design-vue";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import {
  getFinancialStatements,
  getLedgerOverview,
  listVouchers,
  type AccountingVoucher,
  type FinancialStatements,
  type LedgerOverview,
  type StatementLine,
  type VoucherEntry,
} from "@/api/ledger";
import { downloadCsv } from "@/utils/csv";

const loading = ref(false);
const activeTab = ref("vouchers");
const drawerOpen = ref(false);
const vouchers = ref<AccountingVoucher[]>([]);
const selectedVoucher = ref<AccountingVoucher | null>(null);
const keyword = ref("");
const bizTypeFilter = ref<string>();
const statusFilter = ref<string>();
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE;
const overview = reactive<LedgerOverview>({
  voucherCount: 0,
  totalDebit: 0,
  totalCredit: 0,
  revenue: 0,
  expense: 0,
  profit: 0,
  cashBalance: 0,
});
const statements = reactive<FinancialStatements>({
  assets: [],
  liabilities: [],
  revenue: [],
  expenses: [],
  totalAssets: 0,
  totalLiabilities: 0,
  totalRevenue: 0,
  totalExpense: 0,
  profit: 0,
  netCashFlow: 0,
});

const profitStyle = computed(() =>
  overview.profit < 0 ? { color: "#cf1322" } : { color: "#237804" },
);
const trialBalanceDiff = computed(() =>
  roundMoney(
    Number(overview.totalDebit || 0) - Number(overview.totalCredit || 0),
  ),
);
const balanceSheetDiff = computed(() =>
  roundMoney(
    Number(statements.totalAssets || 0) -
      Number(statements.totalLiabilities || 0) -
      Number(statements.profit || 0),
  ),
);
const reversedCount = computed(
  () =>
    vouchers.value.filter(
      (item) => item.status !== "POSTED" || !isVoucherBalanced(item),
    ).length,
);
const voucherTypeStats = computed(() =>
  Array.from(
    vouchers.value
      .reduce((map, item) => {
        const current = map.get(item.bizType) || {
          type: item.bizType,
          count: 0,
          amount: 0,
        };
        current.count += 1;
        current.amount += Number(item.totalDebit || 0);
        map.set(item.bizType, current);
        return map;
      }, new Map<string, { type: string; count: number; amount: number }>())
      .values(),
  ).sort((a, b) => b.amount - a.amount),
);
const bizTypeOptions = computed(() =>
  voucherTypeStats.value.map((item) => ({
    label: bizTypeLabel(item.type),
    value: item.type,
  })),
);
const statusOptions = [
  { label: "已记账", value: "POSTED" },
  { label: "已冲销", value: "REVERSED" },
];
const filteredVouchers = computed(() => {
  const term = keyword.value.trim().toLowerCase();
  return vouchers.value.filter((item) => {
    const text =
      `${item.code} ${item.bizNo} ${item.bizType} ${item.description}`.toLowerCase();
    return (
      (!bizTypeFilter.value || item.bizType === bizTypeFilter.value) &&
      (!statusFilter.value || item.status === statusFilter.value) &&
      (!term || text.includes(term))
    );
  });
});

const voucherColumns = [
  { title: "凭证", key: "voucher", width: 300 },
  { title: "日期", dataIndex: "voucherDate", width: 120 },
  { title: "业务类型", key: "bizType", width: 150 },
  { title: "业务单号", dataIndex: "bizNo", width: 190 },
  { title: "借贷金额", key: "amount", width: 160 },
  { title: "平衡", key: "balance", width: 90 },
  { title: "状态", key: "status", width: 100 },
  { title: "操作", key: "action", width: 120, fixed: "right" as const },
];
const statementColumns = [
  { title: "科目编码", dataIndex: "accountCode", width: 100 },
  { title: "科目名称", dataIndex: "accountName" },
  { title: "借方", key: "debit", width: 130 },
  { title: "贷方", key: "credit", width: 130 },
  { title: "余额", key: "balance", width: 150 },
];
const entryColumns = [
  { title: "科目", dataIndex: "accountName" },
  { title: "摘要", dataIndex: "summary" },
  { title: "借方", key: "debit", width: 140 },
  { title: "贷方", key: "credit", width: 140 },
];

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    const [overviewData, voucherData, statementData] = await Promise.all([
      getLedgerOverview(),
      listVouchers(),
      getFinancialStatements(),
    ]);
    Object.assign(overview, overviewData);
    vouchers.value = voucherData;
    Object.assign(statements, statementData);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "总账数据加载失败");
  } finally {
    loading.value = false;
  }
}

function openVoucher(item: AccountingVoucher) {
  selectedVoucher.value = item;
  drawerOpen.value = true;
}

function exportVouchers() {
  const headers = [
    "凭证号",
    "日期",
    "业务类型",
    "业务单号",
    "摘要",
    "借方合计",
    "贷方合计",
    "状态",
    "平衡",
  ];
  const rows = filteredVouchers.value.map((item) => [
    item.code,
    item.voucherDate,
    bizTypeLabel(item.bizType),
    item.bizNo,
    item.description,
    item.totalDebit,
    item.totalCredit,
    item.status === "POSTED" ? "已记账" : "已冲销",
    isVoucherBalanced(item) ? "平衡" : "不平",
  ]);
  downloadCsv(`ledger-vouchers-${today()}.csv`, headers, rows);
}

function exportStatements() {
  const headers = ["报表", "科目编码", "科目名称", "借方", "贷方", "余额"];
  const rows = [
    ...statements.assets.map((item) => statementRow("资产", item)),
    ...statements.liabilities.map((item) => statementRow("负债", item)),
    ...statements.revenue.map((item) => statementRow("收入", item)),
    ...statements.expenses.map((item) => statementRow("成本费用", item)),
  ];
  downloadCsv(`ledger-statements-${today()}.csv`, headers, rows);
}

function statementRow(type: string, item: StatementLine) {
  return [
    type,
    item.accountCode,
    item.accountName,
    item.debit,
    item.credit,
    item.balance,
  ];
}

function isVoucherBalanced(item: AccountingVoucher) {
  return (
    roundMoney(Number(item.totalDebit || 0) - Number(item.totalCredit || 0)) ===
    0
  );
}

function roundMoney(value: number) {
  return Math.round(value * 100) / 100;
}

function bizTypeLabel(type: string) {
  return (
    (
      {
        RECEIPT: "客户回款",
        PAYMENT: "供应商付款",
        MANUAL: "手工凭证",
      } as Record<string, string>
    )[type] ||
    type ||
    "-"
  );
}

function today() {
  const value = new Date();
  return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, "0")}-${String(value.getDate()).padStart(2, "0")}`;
}

function formatMoney(value: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value || 0);
}

function moneyFormatter(value: number | string) {
  return formatMoney(Number(value));
}
</script>

<style scoped>
.ledger-check-panel {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-top: 16px;
}

.check-card,
.source-card {
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  padding: 12px;
  border: 1px solid #eef2f7;
  border-radius: 6px;
  background: #f8fafc;
}

.check-card span,
.source-card span,
.check-card small,
.source-card small {
  color: #667085;
  font-size: 12px;
}

.check-card strong,
.source-card strong {
  max-width: 100%;
  color: #101828;
  font-size: 18px;
  overflow-wrap: anywhere;
}

.ledger-source-panel {
  margin-top: 16px;
  padding: 14px;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.panel-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.panel-heading h3,
h3 {
  margin: 0;
  color: #101828;
  font-size: 15px;
}

.source-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.source-card {
  cursor: pointer;
  text-align: left;
}

.statement-summary {
  margin-bottom: 20px;
}

h3 {
  margin: 8px 0 12px;
}

.text-danger {
  color: #cf1322;
}

@media (max-width: 1100px) {
  .ledger-check-panel,
  .source-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
