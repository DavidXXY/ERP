<template>
  <div class="page-stack">
    <a-card class="ledger-overview-card" title="总账与财务报表">
      <template #extra>
        <a-space wrap class="ledger-actions">
          <a-button
            v-if="auth.can('finance:voucher:create')"
            type="primary"
            @click="openDraftModal"
          >
            <template #icon><PlusOutlined /></template>
            新建凭证
          </a-button>
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
            title="累计借方（元，税价不适用）"
            :value="overview.totalDebit"
            :formatter="moneyFormatter"
        /></a-col>
        <a-col :xs="12" :xl="4"
          ><a-statistic
            title="营业收入（元，税价不适用）"
            :value="overview.revenue"
            :formatter="moneyFormatter"
        /></a-col>
        <a-col :xs="12" :xl="4"
          ><a-statistic
            title="成本费用（元，税价不适用）"
            :value="overview.expense"
            :formatter="moneyFormatter"
        /></a-col>
        <a-col :xs="12" :xl="4"
          ><a-statistic
            title="账面利润（元，税价不适用）"
            :value="overview.profit"
            :formatter="moneyFormatter"
            :value-style="profitStyle"
        /></a-col>
        <a-col :xs="12" :xl="4"
          ><a-statistic
            title="银行净流量（元，税价不适用）"
            :value="overview.cashBalance"
            :formatter="moneyFormatter"
        /></a-col>
      </a-row>

      <section class="ledger-check-panel">
        <div class="check-card">
          <span>试算平衡差额（元，税价不适用）</span>
          <strong :class="{ 'text-danger': trialBalanceDiff !== 0 }">{{
            formatMoney(Math.abs(trialBalanceDiff))
          }}</strong>
          <small>{{ trialBalanceDiff === 0 ? "借贷相等" : "借贷不平" }}</small>
        </div>
        <div class="check-card">
          <span>资产负债差额（元，税价不适用）</span>
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
          <a-tag color="blue">按张数/金额（元，税价不适用）</a-tag>
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
                <a-tag :color="voucherStatusColor(record.status)">{{
                  voucherStatusLabel(record.status)
                }}</a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-space size="small">
                  <a-button
                    type="link"
                    size="small"
                    @click="openVoucher(record)"
                    >查看</a-button
                  >
                  <a-button
                    v-if="
                      record.status === 'DRAFT' &&
                      auth.can('finance:voucher:review')
                    "
                    type="link"
                    size="small"
                    @click="handleReview(record)"
                    >复核</a-button
                  >
                  <a-button
                    v-if="
                      record.status === 'REVIEWED' &&
                      auth.can('finance:voucher:post')
                    "
                    type="link"
                    size="small"
                    @click="handlePost(record)"
                    >记账</a-button
                  >
                  <a-button
                    v-if="
                      record.status === 'POSTED' &&
                      auth.can('finance:voucher:reverse')
                    "
                    type="link"
                    size="small"
                    danger
                    @click="openReverseModal(record)"
                    >冲销</a-button
                  >
                </a-space>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="statements" tab="财务报表">
          <a-space wrap class="table-toolbar statement-toolbar">
            <span>报表期间</span>
            <a-input
              v-model:value="statementRange.from"
              type="date"
              class="date-input"
            />
            <span>至</span>
            <a-input
              v-model:value="statementRange.to"
              type="date"
              class="date-input"
            />
            <a-button type="primary" @click="loadStatements">生成报表</a-button>
          </a-space>
          <a-descriptions
            bordered
            :column="{ xs: 1, md: 3 }"
            class="statement-summary"
          >
            <a-descriptions-item label="资产合计（元，税价不适用）">{{
              formatMoney(statements.totalAssets)
            }}</a-descriptions-item>
            <a-descriptions-item label="负债合计（元，税价不适用）">{{
              formatMoney(statements.totalLiabilities)
            }}</a-descriptions-item>
            <a-descriptions-item label="所有者权益合计（元，税价不适用）">{{
              formatMoney(statements.totalEquity)
            }}</a-descriptions-item>
            <a-descriptions-item label="本期利润（元，税价不适用）">
              <strong :class="{ 'text-danger': statements.profit < 0 }">{{
                formatMoney(statements.profit)
              }}</strong>
            </a-descriptions-item>
            <a-descriptions-item label="营业收入（元，税价不适用）">{{
              formatMoney(statements.totalRevenue)
            }}</a-descriptions-item>
            <a-descriptions-item label="成本费用（元，税价不适用）">{{
              formatMoney(statements.totalExpense)
            }}</a-descriptions-item>
            <a-descriptions-item label="现金净流量（元，税价不适用）">{{
              formatMoney(statements.netCashFlow)
            }}</a-descriptions-item>
          </a-descriptions>
          <a-row :gutter="[16, 16]">
            <a-col :xs="24" :xl="12">
              <h3>资产负债表</h3>
              <a-table
                size="small"
                :columns="statementColumns"
                :data-source="[
                  ...statements.assets,
                  ...statements.liabilities,
                  ...statements.equity,
                ]"
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

        <a-tab-pane key="accounts" tab="会计科目">
          <div class="table-toolbar toolbar-between">
            <a-space wrap>
              <a-input-search
                v-model:value="accountKeyword"
                allow-clear
                placeholder="搜索科目编码或名称"
                style="width: 260px"
              />
              <a-select
                v-model:value="accountCategoryFilter"
                allow-clear
                placeholder="科目类别"
                :options="accountCategoryOptions"
                style="width: 140px"
              />
            </a-space>
            <a-button
              v-if="auth.can('finance:account:manage')"
              type="primary"
              @click="openAccountModal()"
            >
              <template #icon><PlusOutlined /></template>新增科目
            </a-button>
          </div>
          <a-table
            :columns="accountColumns"
            :data-source="filteredAccounts"
            :pagination="{ pageSize: 12 }"
            :row-key="(item: AccountingAccount) => item.id"
            :scroll="{ x: 900 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'category'">{{
                accountCategoryLabel(record.category)
              }}</template>
              <template v-else-if="column.key === 'direction'">{{
                record.normalDirection === "DEBIT" ? "借" : "贷"
              }}</template>
              <template v-else-if="column.key === 'properties'">
                <a-space
                  ><a-tag v-if="record.cashAccount" color="blue">现金类</a-tag
                  ><a-tag v-if="record.systemAccount">系统</a-tag
                  ><a-tag :color="record.active ? 'green' : 'default'">{{
                    record.active ? "启用" : "停用"
                  }}</a-tag></a-space
                >
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button
                  v-if="auth.can('finance:account:manage')"
                  type="link"
                  size="small"
                  @click="openAccountModal(record)"
                  >编辑</a-button
                >
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="opening" tab="期初余额">
          <div class="table-toolbar toolbar-between">
            <a-space
              ><span>会计年度</span
              ><a-input-number
                v-model:value="openingYear"
                :min="2000"
                :max="2200"
                @change="loadOpeningBalances"
            /></a-space>
            <a-button
              v-if="auth.can('finance:account:manage')"
              type="primary"
              @click="openOpeningModal"
            >
              <template #icon><PlusOutlined /></template>录入期初余额
            </a-button>
          </div>
          <a-alert
            class="section-alert"
            type="info"
            show-icon
            message="期初余额按年度和科目唯一保存；同一科目只能填写借方或贷方。"
          />
          <a-table
            :columns="openingColumns"
            :data-source="openingBalances"
            :pagination="{ pageSize: 12 }"
            :row-key="(item: OpeningBalance) => item.id"
            :scroll="{ x: 900 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'debit'">{{
                record.debitBalance ? formatMoney(record.debitBalance) : "-"
              }}</template>
              <template v-else-if="column.key === 'credit'">{{
                record.creditBalance ? formatMoney(record.creditBalance) : "-"
              }}</template>
              <template v-else-if="column.key === 'action'">
                <a-button
                  v-if="auth.can('finance:account:manage')"
                  type="link"
                  size="small"
                  @click="openOpeningModal(record)"
                  >编辑</a-button
                >
              </template>
            </template>
          </a-table>
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
          <a-descriptions-item label="借贷合计（元，税价不适用）">{{
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

    <a-modal
      v-model:open="draftModalOpen"
      title="新建凭证草稿"
      width="860px"
      :confirm-loading="submitting"
      @ok="saveDraft"
    >
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="8"
            ><a-form-item label="业务类型" required
              ><a-input
                v-model:value="draftForm.bizType"
                disabled /></a-form-item
          ></a-col>
          <a-col :xs="24" :md="8"
            ><a-form-item label="业务单号" required
              ><a-input
                v-model:value="draftForm.bizNo"
                :maxlength="80" /></a-form-item
          ></a-col>
          <a-col :xs="24" :md="8"
            ><a-form-item label="凭证日期" required
              ><a-input
                v-model:value="draftForm.voucherDate"
                type="date" /></a-form-item
          ></a-col>
          <a-col :span="24"
            ><a-form-item label="凭证摘要" required
              ><a-input
                v-model:value="draftForm.description"
                :maxlength="500" /></a-form-item
          ></a-col>
        </a-row>
        <div class="entry-editor-head">
          <strong>会计分录</strong
          ><a-button type="dashed" size="small" @click="addDraftLine"
            ><template #icon><PlusOutlined /></template>增加分录</a-button
          >
        </div>
        <div
          v-for="(line, index) in draftForm.lines"
          :key="index"
          class="entry-editor-row"
        >
          <a-select
            v-model:value="line.accountCode"
            show-search
            placeholder="选择会计科目"
            :filter-option="filterAccountOption"
            :options="activeAccountOptions"
            @change="selectDraftAccount(line)"
          />
          <a-input-number
            v-model:value="line.debit"
            :min="0"
            :precision="2"
            placeholder="借方"
          />
          <a-input-number
            v-model:value="line.credit"
            :min="0"
            :precision="2"
            placeholder="贷方"
          />
          <a-input v-model:value="line.summary" placeholder="分录摘要" />
          <a-button
            danger
            type="text"
            :disabled="draftForm.lines.length <= 2"
            @click="draftForm.lines.splice(index, 1)"
            >删除</a-button
          >
        </div>
        <a-alert
          :type="draftBalance === 0 && draftDebit > 0 ? 'success' : 'warning'"
          show-icon
          :message="`借方（元，税价不适用）${formatMoney(draftDebit)} · 贷方（元，税价不适用）${formatMoney(draftCredit)} · 差额（元，税价不适用）${formatMoney(Math.abs(draftBalance))}`"
        />
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="accountModalOpen"
      :title="accountForm.id ? '编辑会计科目' : '新增会计科目'"
      :confirm-loading="submitting"
      @ok="saveAccount"
    >
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12"
            ><a-form-item label="科目编码" required
              ><a-input
                v-model:value="accountForm.code"
                :disabled="accountForm.systemAccount"
                :maxlength="32" /></a-form-item
          ></a-col>
          <a-col :span="12"
            ><a-form-item label="科目名称" required
              ><a-input
                v-model:value="accountForm.name"
                :maxlength="120" /></a-form-item
          ></a-col>
          <a-col :span="12"
            ><a-form-item label="科目类别" required
              ><a-select
                v-model:value="accountForm.category"
                :disabled="accountForm.systemAccount"
                :options="accountCategoryOptions" /></a-form-item
          ></a-col>
          <a-col :span="12"
            ><a-form-item label="余额方向" required
              ><a-radio-group
                v-model:value="accountForm.normalDirection"
                :disabled="accountForm.systemAccount"
                ><a-radio-button value="DEBIT">借方</a-radio-button
                ><a-radio-button value="CREDIT"
                  >贷方</a-radio-button
                ></a-radio-group
              ></a-form-item
            ></a-col
          >
        </a-row>
        <a-space
          ><a-checkbox v-model:checked="accountForm.cashAccount"
            >现金类科目</a-checkbox
          ><a-checkbox
            v-model:checked="accountForm.active"
            :disabled="accountForm.systemAccount"
            >启用</a-checkbox
          ></a-space
        >
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="openingModalOpen"
      title="录入期初余额（元，税价不适用）"
      :confirm-loading="submitting"
      @ok="saveOpening"
    >
      <a-form layout="vertical">
        <a-form-item label="会计年度" required
          ><a-input-number
            v-model:value="openingForm.fiscalYear"
            :min="2000"
            :max="2200"
            class="full-input"
        /></a-form-item>
        <a-form-item label="会计科目" required
          ><a-select
            v-model:value="openingForm.accountCode"
            show-search
            :disabled="Boolean(openingForm.id)"
            :filter-option="filterAccountOption"
            :options="activeAccountOptions"
        /></a-form-item>
        <a-row :gutter="16">
          <a-col :span="12"
            ><a-form-item label="借方余额（元，税价不适用）"
              ><a-input-number
                v-model:value="openingForm.debitBalance"
                :min="0"
                :precision="2"
                class="full-input" /></a-form-item
          ></a-col>
          <a-col :span="12"
            ><a-form-item label="贷方余额（元，税价不适用）"
              ><a-input-number
                v-model:value="openingForm.creditBalance"
                :min="0"
                :precision="2"
                class="full-input" /></a-form-item
          ></a-col>
        </a-row>
        <a-form-item label="备注"
          ><a-textarea
            v-model:value="openingForm.note"
            :rows="3"
            :maxlength="500"
        /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="reverseModalOpen"
      title="冲销会计凭证"
      :confirm-loading="submitting"
      @ok="submitReverse"
    >
      <a-form layout="vertical">
        <a-form-item label="原凭证"
          ><a-input :value="reverseTarget?.code" disabled
        /></a-form-item>
        <a-form-item label="冲销日期" required
          ><a-input v-model:value="reverseForm.reversalDate" type="date"
        /></a-form-item>
        <a-form-item label="冲销原因" required
          ><a-textarea
            v-model:value="reverseForm.reason"
            :rows="4"
            :maxlength="500"
        /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { Empty, message } from "ant-design-vue";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import PlusOutlined from "@ant-design/icons-vue/PlusOutlined";
import {
  getFinancialStatements,
  getLedgerOverview,
  listAccountingAccounts,
  listOpeningBalances,
  listVouchers,
  createAccountingAccount,
  updateAccountingAccount,
  saveOpeningBalance,
  createVoucherDraft,
  postVoucher,
  reverseVoucher,
  reviewVoucher,
  type AccountingVoucher,
  type AccountingAccount,
  type FinancialStatements,
  type LedgerOverview,
  type OpeningBalance,
  type StatementLine,
  type VoucherEntry,
} from "@/api/ledger";
import { downloadCsv } from "@/utils/csv";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();

const loading = ref(false);
const activeTab = ref("vouchers");
const drawerOpen = ref(false);
const vouchers = ref<AccountingVoucher[]>([]);
const selectedVoucher = ref<AccountingVoucher | null>(null);
const keyword = ref("");
const bizTypeFilter = ref<string>();
const statusFilter = ref<string>();
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE;
const submitting = ref(false);
const draftModalOpen = ref(false);
const reverseModalOpen = ref(false);
const reverseTarget = ref<AccountingVoucher>();
const accounts = ref<AccountingAccount[]>([]);
const openingBalances = ref<OpeningBalance[]>([]);
const accountKeyword = ref("");
const accountCategoryFilter = ref<string>();
const openingYear = ref(new Date().getFullYear());
const accountModalOpen = ref(false);
const openingModalOpen = ref(false);
const statementRange = reactive({
  from: `${new Date().getFullYear()}-01-01`,
  to: today(),
});
const accountForm = reactive({
  id: "",
  code: "",
  name: "",
  category: "ASSET" as AccountingAccount["category"],
  normalDirection: "DEBIT" as AccountingAccount["normalDirection"],
  cashAccount: false,
  active: true,
  systemAccount: false,
});
const openingForm = reactive({
  id: "",
  fiscalYear: new Date().getFullYear(),
  accountCode: "",
  debitBalance: 0,
  creditBalance: 0,
  note: "",
});
const emptyDraftLine = () => ({
  accountCode: "",
  accountName: "",
  debit: undefined as number | undefined,
  credit: undefined as number | undefined,
  summary: "",
});
const draftForm = reactive({
  bizType: "MANUAL",
  bizNo: "",
  voucherDate: today(),
  description: "",
  lines: [emptyDraftLine(), emptyDraftLine()],
});
const reverseForm = reactive({ reversalDate: today(), reason: "" });
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
  from: statementRange.from,
  to: statementRange.to,
  assets: [],
  liabilities: [],
  equity: [],
  revenue: [],
  expenses: [],
  totalAssets: 0,
  totalLiabilities: 0,
  totalEquity: 0,
  totalLiabilitiesAndEquity: 0,
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
      Number(statements.totalLiabilitiesAndEquity || 0),
  ),
);
const reversedCount = computed(
  () =>
    vouchers.value.filter(
      (item) => item.status !== "POSTED" || !isVoucherBalanced(item),
    ).length,
);
const draftDebit = computed(() =>
  draftForm.lines.reduce((sum, item) => sum + Number(item.debit || 0), 0),
);
const draftCredit = computed(() =>
  draftForm.lines.reduce((sum, item) => sum + Number(item.credit || 0), 0),
);
const draftBalance = computed(() =>
  roundMoney(draftDebit.value - draftCredit.value),
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
  { label: "草稿", value: "DRAFT" },
  { label: "已复核", value: "REVIEWED" },
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
const accountCategoryOptions = [
  { label: "资产", value: "ASSET" },
  { label: "负债", value: "LIABILITY" },
  { label: "所有者权益", value: "EQUITY" },
  { label: "收入", value: "REVENUE" },
  { label: "成本费用", value: "EXPENSE" },
];
const activeAccountOptions = computed(() =>
  accounts.value
    .filter((item) => item.active)
    .map((item) => ({ label: `${item.code} ${item.name}`, value: item.code })),
);
const filteredAccounts = computed(() => {
  const term = accountKeyword.value.trim().toLowerCase();
  return accounts.value.filter(
    (item) =>
      (!accountCategoryFilter.value ||
        item.category === accountCategoryFilter.value) &&
      (!term || `${item.code} ${item.name}`.toLowerCase().includes(term)),
  );
});

const voucherColumns = [
  { title: "凭证", key: "voucher", width: 300 },
  { title: "日期", dataIndex: "voucherDate", width: 120 },
  { title: "业务类型", key: "bizType", width: 150 },
  { title: "业务单号", dataIndex: "bizNo", width: 190 },
  { title: "借贷金额（元，税价不适用）", key: "amount", width: 220 },
  { title: "平衡", key: "balance", width: 90 },
  { title: "状态", key: "status", width: 100 },
  { title: "操作", key: "action", width: 240, fixed: "right" as const },
];
const statementColumns = [
  { title: "科目编码", dataIndex: "accountCode", width: 100 },
  { title: "科目名称", dataIndex: "accountName" },
  { title: "借方（元，税价不适用）", key: "debit", width: 190 },
  { title: "贷方（元，税价不适用）", key: "credit", width: 190 },
  { title: "余额（元，税价不适用）", key: "balance", width: 200 },
];
const entryColumns = [
  { title: "科目", dataIndex: "accountName" },
  { title: "摘要", dataIndex: "summary" },
  { title: "借方（元，税价不适用）", key: "debit", width: 190 },
  { title: "贷方（元，税价不适用）", key: "credit", width: 190 },
];
const accountColumns = [
  { title: "科目编码", dataIndex: "code", width: 130 },
  { title: "科目名称", dataIndex: "name", width: 220 },
  { title: "类别", key: "category", width: 120 },
  { title: "余额方向", key: "direction", width: 100 },
  { title: "属性", key: "properties" },
  { title: "操作", key: "action", width: 90 },
];
const openingColumns = [
  { title: "年度", dataIndex: "fiscalYear", width: 90 },
  { title: "科目编码", dataIndex: "accountCode", width: 120 },
  { title: "科目名称", dataIndex: "accountName" },
  { title: "借方余额（元，税价不适用）", key: "debit", width: 220 },
  { title: "贷方余额（元，税价不适用）", key: "credit", width: 220 },
  { title: "备注", dataIndex: "note" },
  { title: "操作", key: "action", width: 90 },
];

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    const [overviewData, voucherData, statementData, accountData, openingData] =
      await Promise.all([
        getLedgerOverview(),
        listVouchers(),
        getFinancialStatements(statementRange),
        listAccountingAccounts(),
        listOpeningBalances(openingYear.value),
      ]);
    Object.assign(overview, overviewData);
    vouchers.value = voucherData;
    Object.assign(statements, statementData);
    accounts.value = accountData;
    openingBalances.value = openingData;
  } catch (error) {
    message.error(error instanceof Error ? error.message : "总账数据加载失败");
  } finally {
    loading.value = false;
  }
}

async function loadStatements() {
  try {
    Object.assign(statements, await getFinancialStatements(statementRange));
  } catch (error) {
    message.error((error as Error).message);
  }
}

async function loadOpeningBalances() {
  try {
    openingBalances.value = await listOpeningBalances(
      Number(openingYear.value),
    );
  } catch (error) {
    message.error((error as Error).message);
  }
}

function openVoucher(item: AccountingVoucher) {
  selectedVoucher.value = item;
  drawerOpen.value = true;
}

function openDraftModal() {
  Object.assign(draftForm, {
    bizType: "MANUAL",
    bizNo: `MANUAL-${Date.now()}`,
    voucherDate: today(),
    description: "",
    lines: [emptyDraftLine(), emptyDraftLine()],
  });
  draftModalOpen.value = true;
}
function addDraftLine() {
  draftForm.lines.push(emptyDraftLine());
}
function selectDraftAccount(line: {
  accountCode: string;
  accountName: string;
}) {
  line.accountName =
    accounts.value.find((item) => item.code === line.accountCode)?.name ?? "";
}
function filterAccountOption(input: string, option?: { label?: string }) {
  return String(option?.label ?? "")
    .toLowerCase()
    .includes(input.toLowerCase());
}

function openAccountModal(item?: AccountingAccount) {
  Object.assign(
    accountForm,
    item
      ? { ...item }
      : {
          id: "",
          code: "",
          name: "",
          category: "ASSET",
          normalDirection: "DEBIT",
          cashAccount: false,
          active: true,
          systemAccount: false,
        },
  );
  accountModalOpen.value = true;
}

async function saveAccount() {
  if (!accountForm.code.trim() || !accountForm.name.trim()) {
    message.warning("请填写科目编码和名称");
    return;
  }
  submitting.value = true;
  try {
    const payload = {
      code: accountForm.code.trim(),
      name: accountForm.name.trim(),
      category: accountForm.category,
      normalDirection: accountForm.normalDirection,
      cashAccount: accountForm.cashAccount,
      active: accountForm.active,
    };
    if (accountForm.id) await updateAccountingAccount(accountForm.id, payload);
    else await createAccountingAccount(payload);
    message.success(accountForm.id ? "会计科目已更新" : "会计科目已新增");
    accountModalOpen.value = false;
    accounts.value = await listAccountingAccounts();
  } catch (error) {
    message.error((error as Error).message);
  } finally {
    submitting.value = false;
  }
}

function openOpeningModal(item?: OpeningBalance) {
  Object.assign(
    openingForm,
    item
      ? { ...item, note: item.note ?? "" }
      : {
          id: "",
          fiscalYear: openingYear.value,
          accountCode: "",
          debitBalance: 0,
          creditBalance: 0,
          note: "",
        },
  );
  openingModalOpen.value = true;
}

async function saveOpening() {
  if (!openingForm.accountCode) {
    message.warning("请选择会计科目");
    return;
  }
  if (
    Number(openingForm.debitBalance) > 0 &&
    Number(openingForm.creditBalance) > 0
  ) {
    message.warning("期初余额不能同时填写借方和贷方");
    return;
  }
  submitting.value = true;
  try {
    await saveOpeningBalance({
      fiscalYear: Number(openingForm.fiscalYear),
      accountCode: openingForm.accountCode,
      debitBalance: Number(openingForm.debitBalance || 0),
      creditBalance: Number(openingForm.creditBalance || 0),
      note: openingForm.note.trim() || undefined,
    });
    message.success("期初余额已保存");
    openingModalOpen.value = false;
    openingYear.value = Number(openingForm.fiscalYear);
    await loadOpeningBalances();
  } catch (error) {
    message.error((error as Error).message);
  } finally {
    submitting.value = false;
  }
}
async function saveDraft() {
  if (
    !draftForm.bizType.trim() ||
    !draftForm.bizNo.trim() ||
    !draftForm.description.trim()
  ) {
    message.warning("请完整填写凭证来源、单号和摘要");
    return;
  }
  if (draftBalance.value !== 0 || draftDebit.value <= 0) {
    message.warning("会计分录必须借贷平衡且金额大于零");
    return;
  }
  if (
    draftForm.lines.some(
      (line) =>
        !line.accountCode.trim() ||
        !line.accountName.trim() ||
        Number(line.debit || 0) > 0 === Number(line.credit || 0) > 0,
    )
  ) {
    message.warning("每条分录必须填写科目，并且只能录入借方或贷方金额");
    return;
  }
  submitting.value = true;
  try {
    await createVoucherDraft(draftForm);
    message.success("凭证草稿已创建");
    draftModalOpen.value = false;
    await loadData();
  } catch (error) {
    message.error((error as Error).message);
  } finally {
    submitting.value = false;
  }
}
async function handleReview(item: AccountingVoucher) {
  try {
    await reviewVoucher(item.id);
    message.success("凭证已复核");
    await loadData();
  } catch (error) {
    message.error((error as Error).message);
  }
}
async function handlePost(item: AccountingVoucher) {
  try {
    await postVoucher(item.id);
    message.success("凭证已记账");
    await loadData();
  } catch (error) {
    message.error((error as Error).message);
  }
}
function openReverseModal(item: AccountingVoucher) {
  reverseTarget.value = item;
  Object.assign(reverseForm, { reversalDate: today(), reason: "" });
  reverseModalOpen.value = true;
}
async function submitReverse() {
  if (!reverseTarget.value || reverseForm.reason.trim().length < 5) {
    message.warning("请填写至少 5 个字的冲销原因");
    return;
  }
  submitting.value = true;
  try {
    await reverseVoucher(
      reverseTarget.value.id,
      reverseForm.reversalDate,
      reverseForm.reason,
    );
    message.success("冲销凭证已生成");
    reverseModalOpen.value = false;
    await loadData();
  } catch (error) {
    message.error((error as Error).message);
  } finally {
    submitting.value = false;
  }
}

function exportVouchers() {
  const headers = [
    "凭证号",
    "日期",
    "业务类型",
    "业务单号",
    "摘要",
    "借方合计（元，税价不适用）",
    "贷方合计（元，税价不适用）",
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
    voucherStatusLabel(item.status),
    isVoucherBalanced(item) ? "平衡" : "不平",
  ]);
  downloadCsv(`ledger-vouchers-${today()}.csv`, headers, rows);
}

function exportStatements() {
  const headers = [
    "报表",
    "科目编码",
    "科目名称",
    "借方（元，税价不适用）",
    "贷方（元，税价不适用）",
    "余额（元，税价不适用）",
  ];
  const rows = [
    ...statements.assets.map((item) => statementRow("资产", item)),
    ...statements.liabilities.map((item) => statementRow("负债", item)),
    ...statements.equity.map((item) => statementRow("所有者权益", item)),
    ...statements.revenue.map((item) => statementRow("收入", item)),
    ...statements.expenses.map((item) => statementRow("成本费用", item)),
  ];
  downloadCsv(`ledger-statements-${today()}.csv`, headers, rows);
}

function accountCategoryLabel(value: string) {
  return (
    (
      {
        ASSET: "资产",
        LIABILITY: "负债",
        EQUITY: "所有者权益",
        REVENUE: "收入",
        EXPENSE: "成本费用",
      } as Record<string, string>
    )[value] ?? value
  );
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

function voucherStatusLabel(status: AccountingVoucher["status"]) {
  return (
    {
      DRAFT: "草稿",
      REVIEWED: "已复核",
      POSTED: "已记账",
      REVERSED: "已冲销",
    } as const
  )[status];
}
function voucherStatusColor(status: AccountingVoucher["status"]) {
  return (
    {
      DRAFT: "default",
      REVIEWED: "blue",
      POSTED: "green",
      REVERSED: "orange",
    } as const
  )[status];
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

function moneyFormatter({ value }: { value: number | string }) {
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
.statement-toolbar {
  margin-bottom: 14px;
}
.date-input {
  width: 150px;
}
.toolbar-between {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

h3 {
  margin: 8px 0 12px;
}

.text-danger {
  color: #cf1322;
}

.entry-editor-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.entry-editor-row {
  display: grid;
  grid-template-columns: 260px 120px 120px minmax(140px, 1fr) 56px;
  gap: 8px;
  margin-bottom: 8px;
}
@media (max-width: 768px) {
  .panel-heading {
    align-items: flex-start;
    flex-direction: column;
  }
  .ledger-overview-card :deep(.ant-card-head-wrapper) {
    align-items: stretch;
    flex-direction: column;
    gap: 10px;
  }
  .ledger-overview-card :deep(.ant-card-head-title) {
    padding-bottom: 0;
  }
  .ledger-overview-card :deep(.ant-card-extra) {
    width: 100%;
    margin-inline-start: 0;
    padding-top: 0;
  }
  .ledger-actions {
    display: flex;
    width: 100%;
    flex-wrap: wrap;
  }
  .toolbar-between {
    align-items: stretch;
    flex-direction: column;
  }
  .entry-editor-row {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 1100px) {
  .ledger-check-panel,
  .source-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
