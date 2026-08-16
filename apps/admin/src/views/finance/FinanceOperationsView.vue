<template>
  <div class="finance-ops page-stack">
    <header class="ops-header">
      <div>
        <span class="eyebrow">FINANCE CONTROL</span>
        <h2>财务运营工作台</h2>
        <p>月结、对账、预算、税务及合并证据统一闭环</p>
      </div>
      <a-space wrap>
        <a-date-picker
          v-model:value="workDate"
          picker="month"
          :allow-clear="false"
        />
        <a-button :loading="loading" @click="loadAll">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-space>
    </header>

    <section class="control-strip" aria-label="财务控制异常">
      <button
        v-for="item in controlMetrics"
        :key="item.key"
        type="button"
        @click="activeTab = item.tab"
      >
        <span>{{ item.label }}</span>
        <strong :class="item.tone">{{ item.value }}</strong>
        <small>{{ item.note }}</small>
      </button>
    </section>

    <section class="ops-surface">
      <a-tabs v-model:active-key="activeTab">
        <a-tab-pane key="close" tab="月结与期末">
          <div class="command-row">
            <div>
              <h3>{{ periodLabel }} 期末控制</h3>
              <p>期初平衡、期末凭证、自动转回和生成请求轨迹</p>
            </div>
            <a-space wrap>
              <a-button @click="runOpeningValidation">期初校验</a-button>
              <a-button v-if="canManage" @click="reverseDue"
                >处理到期转回</a-button
              >
              <a-button
                v-if="canManage"
                type="primary"
                @click="periodModal = true"
              >
                <template #icon><PlusOutlined /></template>期末任务
              </a-button>
            </a-space>
          </div>
          <a-alert
            v-if="openingValidation"
            :type="openingValidation.valid ? 'success' : 'error'"
            show-icon
            :message="
              openingValidation.valid
                ? '期初数据校验通过'
                : '期初数据存在阻断项'
            "
            :description="openingDescription"
            class="inline-alert"
          />
          <a-table
            :columns="periodColumns"
            :data-source="periodJobs"
            row-key="id"
            :pagination="false"
            :scroll="{ x: 1120 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'type'"
                ><a-tag>{{ processLabel(record.processType) }}</a-tag></template
              >
              <template v-else-if="column.key === 'amount'">{{
                money(record.amount)
              }}</template>
              <template v-else-if="column.key === 'status'"
                ><a-tag :color="statusColor(record.status)">{{
                  statusLabel(record.status)
                }}</a-tag></template
              >
              <template v-else-if="column.key === 'reverse'">{{
                record.autoReverse ? record.reversalDate : "不转回"
              }}</template>
              <template v-else-if="column.key === 'action'">
                <a-button
                  v-if="
                    canManage && ['PENDING', 'FAILED'].includes(record.status)
                  "
                  type="link"
                  size="small"
                  @click="executeJob(record)"
                  >执行/重试</a-button
                >
              </template>
            </template>
          </a-table>
          <div class="subsection-heading">
            <h3>凭证生成轨迹</h3>
            <span>幂等键、尝试次数和失败原因</span>
          </div>
          <a-table
            :columns="voucherColumns"
            :data-source="voucherRequests"
            row-key="id"
            size="small"
            :pagination="{ pageSize: 5 }"
            :scroll="{ x: 920 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'"
                ><a-tag :color="statusColor(record.status)">{{
                  statusLabel(record.status)
                }}</a-tag></template
              >
              <template v-else-if="column.key === 'error'"
                ><span class="error-text">{{
                  record.lastError || "-"
                }}</span></template
              >
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="budget" tab="预算差异">
          <div class="command-row">
            <div>
              <h3>预算执行与预测</h3>
              <p>实际或最新预测与批准基准比较</p>
            </div>
            <a-button @click="router.push('/governance')"
              >维护预算控制</a-button
            >
          </div>
          <a-table
            :columns="budgetColumns"
            :data-source="budgetRows"
            row-key="controlId"
            :scroll="{ x: 1100 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'"
                ><strong>{{ record.name }}</strong
                ><small class="cell-note"
                  >{{ record.controlCode }} · {{ record.owner }}</small
                ></template
              >
              <template
                v-else-if="
                  [
                    'budget',
                    'committed',
                    'actual',
                    'forecast',
                    'variance',
                  ].includes(column.key)
                "
                ><span
                  :class="{
                    danger: column.key === 'variance' && record.variance > 0,
                  }"
                  >{{ money(record[column.key]) }}</span
                ></template
              >
              <template v-else-if="column.key === 'usage'"
                ><a-progress
                  :percent="Math.min(record.usageRate, 999)"
                  size="small"
                  :status="record.usageRate > 100 ? 'exception' : 'normal'"
              /></template>
              <template v-else-if="column.key === 'status'"
                ><a-tag :color="statusColor(record.status)">{{
                  statusLabel(record.status)
                }}</a-tag></template
              >
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="partner" tab="往来对账">
          <div class="command-row">
            <div>
              <h3>客户 / 供应商对账单</h3>
              <p>业务余额、对方确认余额和差异留痕</p>
            </div>
            <a-segmented
              v-model:value="partnerType"
              :options="[
                { label: '客户', value: 'CUSTOMER' },
                { label: '供应商', value: 'SUPPLIER' },
              ]"
              @change="loadPartners"
            />
          </div>
          <a-table
            :columns="partnerColumns"
            :data-source="partnerRows"
            row-key="partnerId"
            :scroll="{ x: 1000 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'partner'"
                ><strong>{{ record.partnerName }}</strong
                ><small class="cell-note">{{
                  record.partnerCode
                }}</small></template
              >
              <template
                v-else-if="
                  ['ledger', 'confirmed', 'difference'].includes(column.key)
                "
                ><span
                  :class="{
                    danger:
                      column.key === 'difference' && record.difference !== 0,
                  }"
                  >{{
                    money(
                      record[
                        column.key === "ledger"
                          ? "ledgerBalance"
                          : column.key === "confirmed"
                            ? "confirmedBalance"
                            : "difference"
                      ],
                    )
                  }}</span
                ></template
              >
              <template v-else-if="column.key === 'status'"
                ><a-tag :color="statusColor(record.status)">{{
                  statusLabel(record.status)
                }}</a-tag></template
              >
              <template v-else-if="column.key === 'action'"
                ><a-button
                  v-if="canManage"
                  type="link"
                  size="small"
                  @click="openPartnerConfirm(record)"
                  >确认</a-button
                ></template
              >
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="cash" tab="现金情景">
          <div class="command-row">
            <div>
              <h3>现金预测情景</h3>
              <p>按到期应收应付叠加管理层调整</p>
            </div>
            <a-button v-if="canManage" type="primary" @click="cashModal = true"
              ><template #icon><PlusOutlined /></template>新建情景</a-button
            >
          </div>
          <a-table
            :columns="cashColumns"
            :data-source="cashRows"
            row-key="id"
            :scroll="{ x: 980 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'"
                ><strong>{{ record.name }}</strong
                ><small class="cell-note"
                  >{{ record.asOfDate }} · {{ record.horizonDays }} 天</small
                ></template
              >
              <template
                v-else-if="
                  ['opening', 'receipt', 'payment', 'forecast'].includes(
                    column.key,
                  )
                "
                ><span
                  :class="{
                    danger:
                      column.key === 'forecast' && record.forecastCash < 0,
                  }"
                  >{{
                    money(
                      record[
                        column.key === "opening"
                          ? "openingCash"
                          : column.key === "receipt"
                            ? "expectedReceipts"
                            : column.key === "payment"
                              ? "expectedPayments"
                              : "forecastCash"
                      ],
                    )
                  }}</span
                ></template
              >
              <template v-else-if="column.key === 'status'"
                ><a-tag>{{ statusLabel(record.status) }}</a-tag></template
              >
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="project" tab="项目盈利">
          <div class="command-row">
            <div>
              <h3>项目盈利与预算消耗</h3>
              <p>合同额、实际成本和毛利风险联查</p>
            </div>
            <a-button @click="router.push('/projects')">项目工作台</a-button>
          </div>
          <a-table
            :columns="projectColumns"
            :data-source="projectRows"
            row-key="projectId"
            :scroll="{ x: 1120 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'project'"
                ><strong>{{ record.projectName }}</strong
                ><small class="cell-note"
                  >{{ record.projectCode }} · {{ record.customerName }}</small
                ></template
              >
              <template
                v-else-if="
                  ['contract', 'budget', 'cost', 'margin'].includes(column.key)
                "
                >{{
                  money(
                    record[
                      column.key === "contract"
                        ? "contractAmount"
                        : column.key === "budget"
                          ? "budgetAmount"
                          : column.key === "cost"
                            ? "actualCost"
                            : "grossMargin"
                    ],
                  )
                }}</template
              >
              <template v-else-if="column.key === 'rate'"
                >{{ Number(record.grossMarginRate).toFixed(2) }}%</template
              >
              <template v-else-if="column.key === 'risk'"
                ><a-tag :color="statusColor(record.riskLevel)">{{
                  record.riskMessage
                }}</a-tag></template
              >
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="tax" tab="税务申报">
          <div class="command-row">
            <div>
              <h3>{{ periodLabel }} 税务勾稽</h3>
              <p>发票税额与总账税额一致后锁定申报</p>
            </div>
            <a-space
              ><a-button @click="router.push('/finance/tax-ledger')"
                >税务台账</a-button
              ><a-button v-if="canManage" type="primary" @click="reconcileTax"
                >执行勾稽</a-button
              ></a-space
            >
          </div>
          <a-table
            :columns="taxColumns"
            :data-source="taxRows"
            row-key="id"
            :scroll="{ x: 980 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'period'"
                >{{ record.fiscalYear }}-{{ pad(record.periodNo) }}</template
              >
              <template
                v-else-if="
                  [
                    'output',
                    'input',
                    'payable',
                    'ledger',
                    'difference',
                  ].includes(column.key)
                "
                ><span
                  :class="{
                    danger:
                      column.key === 'difference' && record.difference !== 0,
                  }"
                  >{{
                    money(
                      record[
                        column.key === "output"
                          ? "outputTax"
                          : column.key === "input"
                            ? "inputTax"
                            : column.key === "payable"
                              ? "taxPayable"
                              : column.key === "ledger"
                                ? "ledgerTax"
                                : "difference"
                      ],
                    )
                  }}</span
                ></template
              >
              <template v-else-if="column.key === 'status'"
                ><a-tag :color="statusColor(record.status)">{{
                  statusLabel(record.status)
                }}</a-tag></template
              >
              <template v-else-if="column.key === 'action'"
                ><a-button
                  v-if="canManage && record.status !== 'LOCKED'"
                  type="link"
                  size="small"
                  :disabled="record.difference !== 0"
                  @click="openTaxLock(record)"
                  >锁定申报</a-button
                ></template
              >
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="consolidation" tab="合并抵销">
          <div class="command-row">
            <div>
              <h3>多主体合并与内部交易抵销</h3>
              <p>本位币口径录入主体汇总，完成后固化底稿</p>
            </div>
            <a-button
              v-if="canManage"
              type="primary"
              @click="consolidationModal = true"
              ><template #icon><PlusOutlined /></template>合并批次</a-button
            >
          </div>
          <a-table
            :columns="consolidationColumns"
            :data-source="consolidationRows"
            row-key="id"
            :scroll="{ x: 1040 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'period'"
                >{{ record.fiscalYear }}-{{ pad(record.periodNo) }}</template
              >
              <template
                v-else-if="
                  ['revenue', 'expense', 'elimination', 'profit'].includes(
                    column.key,
                  )
                "
                >{{
                  money(
                    record[
                      column.key === "revenue"
                        ? "combinedRevenue"
                        : column.key === "expense"
                          ? "combinedExpense"
                          : column.key === "elimination"
                            ? "intercompanyRevenue"
                            : "consolidatedProfit"
                    ],
                  )
                }}</template
              >
              <template v-else-if="column.key === 'status'"
                ><a-tag :color="statusColor(record.status)">{{
                  statusLabel(record.status)
                }}</a-tag></template
              >
              <template v-else-if="column.key === 'action'"
                ><a-button
                  v-if="canManage && record.status === 'DRAFT'"
                  type="link"
                  size="small"
                  @click="completeRun(record)"
                  >完成并留痕</a-button
                ></template
              >
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="evidence" tab="审计证据">
          <div class="command-row">
            <div>
              <h3>不可变报表快照</h3>
              <p>内容摘要去重，锁定申报、合并和关账自动留痕</p>
            </div>
            <a-button v-if="canManage" @click="captureCurrentSnapshot"
              ><template #icon><SafetyCertificateOutlined /></template
              >固化当前概览</a-button
            >
          </div>
          <a-table
            :columns="snapshotColumns"
            :data-source="snapshotRows"
            row-key="id"
            :scroll="{ x: 960 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'type'"
                ><a-tag>{{ reportLabel(record.reportType) }}</a-tag></template
              >
              <template v-else-if="column.key === 'hash'"
                ><code>{{ record.contentHash.slice(0, 16) }}…</code></template
              >
              <template v-else-if="column.key === 'time'">{{
                dateTime(record.capturedAt)
              }}</template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </section>

    <a-modal
      v-model:open="periodModal"
      title="新增期末处理任务"
      :confirm-loading="saving"
      @ok="savePeriodJob"
    >
      <a-form layout="vertical">
        <a-form-item label="处理类型" required
          ><a-select
            v-model:value="periodForm.processType"
            :options="processOptions"
        /></a-form-item>
        <a-form-item label="说明" required
          ><a-input v-model:value="periodForm.description"
        /></a-form-item>
        <a-row :gutter="12"
          ><a-col :span="12"
            ><a-form-item label="金额（税价不适用，元）" required
              ><a-input-number
                v-model:value="periodForm.amount"
                :min="0.01"
                :precision="2"
                style="width: 100%" /></a-form-item></a-col
          ><a-col :span="12"
            ><a-form-item label="幂等键" required
              ><a-input
                v-model:value="
                  periodForm.idempotencyKey
                " /></a-form-item></a-col
        ></a-row>
        <a-row :gutter="12"
          ><a-col :span="12"
            ><a-form-item label="借方科目" required
              ><a-input
                v-model:value="
                  periodForm.debitAccountCode
                " /></a-form-item></a-col
          ><a-col :span="12"
            ><a-form-item label="贷方科目" required
              ><a-input
                v-model:value="
                  periodForm.creditAccountCode
                " /></a-form-item></a-col
        ></a-row>
        <a-form-item
          ><a-checkbox v-model:checked="periodForm.autoReverse"
            >下期自动转回</a-checkbox
          ></a-form-item
        >
        <a-form-item v-if="periodForm.autoReverse" label="转回日期" required
          ><a-date-picker
            v-model:value="periodReversalDate"
            style="width: 100%"
        /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="partnerModal"
      title="往来余额确认"
      :confirm-loading="saving"
      @ok="confirmPartner"
    >
      <a-form layout="vertical"
        ><a-form-item label="对方确认余额（税价随来源单据，元）" required
          ><a-input-number
            v-model:value="partnerForm.statementBalance"
            :precision="2"
            style="width: 100%" /></a-form-item
        ><a-form-item label="确认状态" required
          ><a-select
            v-model:value="partnerForm.status"
            :options="partnerStatusOptions" /></a-form-item
        ><a-form-item label="确认说明" required
          ><a-textarea
            v-model:value="partnerForm.note"
            :rows="3" /></a-form-item
      ></a-form>
    </a-modal>

    <a-modal
      v-model:open="cashModal"
      title="新建现金预测情景"
      :confirm-loading="saving"
      @ok="saveCashScenario"
    >
      <a-form layout="vertical"
        ><a-form-item label="情景名称" required
          ><a-input v-model:value="cashForm.name" /></a-form-item
        ><a-row :gutter="12"
          ><a-col :span="12"
            ><a-form-item label="预测天数" required
              ><a-input-number
                v-model:value="cashForm.horizonDays"
                :min="1"
                :max="3660"
                style="width: 100%" /></a-form-item></a-col
          ><a-col :span="12"
            ><a-form-item label="期初现金" required
              ><a-input-number
                v-model:value="cashForm.openingCash"
                :precision="2"
                style="width: 100%" /></a-form-item></a-col></a-row
        ><a-row :gutter="12"
          ><a-col :span="12"
            ><a-form-item label="额外收款"
              ><a-input-number
                v-model:value="cashForm.receiptAdjustment"
                :precision="2"
                style="width: 100%" /></a-form-item></a-col
          ><a-col :span="12"
            ><a-form-item label="额外付款"
              ><a-input-number
                v-model:value="cashForm.paymentAdjustment"
                :precision="2"
                style="width: 100%" /></a-form-item></a-col></a-row
        ><a-form-item label="情景假设"
          ><a-textarea
            v-model:value="cashForm.assumptions"
            :rows="3" /></a-form-item
      ></a-form>
    </a-modal>

    <a-modal
      v-model:open="consolidationModal"
      title="新建合并批次"
      :confirm-loading="saving"
      width="720px"
      @ok="saveConsolidation"
    >
      <a-form layout="vertical"
        ><a-form-item label="批次名称" required
          ><a-input v-model:value="consolidationForm.name"
        /></a-form-item>
        <div
          v-for="(entity, index) in consolidationForm.entities"
          :key="index"
          class="entity-row"
        >
          <a-input
            v-model:value="entity.entityCode"
            placeholder="主体编码"
          /><a-input
            v-model:value="entity.entityName"
            placeholder="主体名称"
          /><a-input-number
            v-model:value="entity.revenue"
            :precision="2"
            placeholder="收入"
          /><a-input-number
            v-model:value="entity.expense"
            :precision="2"
            placeholder="费用"
          />
        </div>
        <a-button type="dashed" block @click="addEntity">增加主体</a-button
        ><a-row :gutter="12" class="elimination-row"
          ><a-col :span="12"
            ><a-form-item label="内部收入抵销"
              ><a-input-number
                v-model:value="consolidationForm.intercompanyRevenue"
                :min="0"
                :precision="2"
                style="width: 100%" /></a-form-item></a-col
          ><a-col :span="12"
            ><a-form-item label="内部费用抵销"
              ><a-input-number
                v-model:value="consolidationForm.intercompanyExpense"
                :min="0"
                :precision="2"
                style="width: 100%" /></a-form-item></a-col></a-row
      ></a-form>
    </a-modal>
    <a-modal
      v-model:open="taxModal"
      title="锁定税务申报"
      :confirm-loading="saving"
      @ok="confirmTaxLock"
    >
      <a-form layout="vertical"
        ><a-form-item label="申报编号" required
          ><a-input
            v-model:value="taxReference"
            placeholder="请输入税务申报回执编号" /></a-form-item
      ></a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import dayjs, { type Dayjs } from "dayjs";
import { message } from "ant-design-vue";
import {
  PlusOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
} from "@ant-design/icons-vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import {
  listProjectProfitability,
  type ProjectProfitability,
} from "@/api/project";
import {
  captureReportSnapshot,
  completeConsolidation,
  confirmPartnerStatement,
  createCashScenario,
  createConsolidation,
  createPeriodJob,
  executePeriodJob,
  getOperationsOverview,
  listBudgetVariance,
  listCashScenarios,
  listConsolidations,
  listPartnerStatements,
  listPeriodJobs,
  listReportSnapshots,
  listTaxFilings,
  listVoucherRequests,
  lockTaxFiling,
  reconcileTaxFiling,
  reverseDuePeriodJobs,
  validateOpening,
  type BudgetVariance,
  type CashScenario,
  type Consolidation,
  type OpeningValidation,
  type OperationsOverview,
  type PartnerStatement,
  type PeriodJob,
  type ReportSnapshot,
  type TaxFiling,
  type VoucherRequest,
} from "@/api/finance-operations";

const router = useRouter();
const auth = useAuthStore();
const canManage = computed(() => auth.can("finance:operations:manage"));
const loading = ref(false);
const saving = ref(false);
const activeTab = ref("close");
const workDate = ref<Dayjs>(dayjs());
const year = computed(() => workDate.value.year());
const month = computed(() => workDate.value.month() + 1);
const periodEnd = computed(() =>
  workDate.value.endOf("month").format("YYYY-MM-DD"),
);
const periodLabel = computed(() => `${year.value}-${pad(month.value)}`);
const overview = reactive<OperationsOverview>({
  pendingPeriodJobs: 0,
  failedVoucherRequests: 0,
  unreconciledPartners: 0,
  unlockedTaxPeriods: 0,
  draftConsolidations: 0,
  snapshots: 0,
  budgetVariance: 0,
  forecastLiquidity: 0,
});
const periodJobs = ref<PeriodJob[]>([]),
  budgetRows = ref<BudgetVariance[]>([]),
  partnerRows = ref<PartnerStatement[]>([]),
  cashRows = ref<CashScenario[]>([]),
  projectRows = ref<ProjectProfitability[]>([]),
  taxRows = ref<TaxFiling[]>([]),
  consolidationRows = ref<Consolidation[]>([]),
  snapshotRows = ref<ReportSnapshot[]>([]),
  voucherRequests = ref<VoucherRequest[]>([]);
const openingValidation = ref<OpeningValidation>();
const partnerType = ref("CUSTOMER");
const periodModal = ref(false),
  partnerModal = ref(false),
  cashModal = ref(false),
  consolidationModal = ref(false),
  taxModal = ref(false);
const selectedPartner = ref<PartnerStatement>();
const selectedTax = ref<TaxFiling>();
const taxReference = ref("");
const periodReversalDate = ref<Dayjs>();
const periodForm = reactive({
  processType: "ACCRUAL",
  description: "",
  amount: 0,
  debitAccountCode: "6602",
  creditAccountCode: "2201",
  autoReverse: true,
  idempotencyKey: "",
});
const partnerForm = reactive({
  statementBalance: 0,
  status: "CONFIRMED",
  note: "",
});
const cashForm = reactive({
  name: "基准情景",
  horizonDays: 30,
  openingCash: 0,
  receiptAdjustment: 0,
  paymentAdjustment: 0,
  assumptions: "",
});
const consolidationForm = reactive({
  name: "",
  entities: [
    { entityCode: "HQ", entityName: "总部", revenue: 0, expense: 0 },
    { entityCode: "SUB", entityName: "业务主体", revenue: 0, expense: 0 },
  ],
  intercompanyRevenue: 0,
  intercompanyExpense: 0,
});
const controlMetrics = computed(() => [
  {
    key: "jobs",
    label: "待处理期末任务",
    value: overview.pendingPeriodJobs,
    tone: overview.pendingPeriodJobs ? "danger" : "",
    note: "含失败待重试",
    tab: "close",
  },
  {
    key: "voucher",
    label: "凭证失败请求",
    value: overview.failedVoucherRequests,
    tone: overview.failedVoucherRequests ? "danger" : "",
    note: "支持原键重试",
    tab: "close",
  },
  {
    key: "partner",
    label: "往来未确认",
    value: overview.unreconciledPartners,
    tone: overview.unreconciledPartners ? "warning" : "",
    note: "客户及供应商",
    tab: "partner",
  },
  {
    key: "budget",
    label: "预算超支暴露",
    value: money(overview.budgetVariance),
    tone: overview.budgetVariance > 0 ? "danger" : "",
    note: "预测与实际孰高",
    tab: "budget",
  },
  {
    key: "cash",
    label: "最新预测现金",
    value: money(overview.forecastLiquidity),
    tone: overview.forecastLiquidity < 0 ? "danger" : "",
    note: "单本位币口径",
    tab: "cash",
  },
  {
    key: "tax",
    label: "未锁税务期间",
    value: overview.unlockedTaxPeriods,
    tone: overview.unlockedTaxPeriods ? "warning" : "",
    note: "待勾稽或锁定",
    tab: "tax",
  },
]);
const openingDescription = computed(() =>
  openingValidation.value?.valid
    ? `借方 ${money(openingValidation.value.totalDebit)}，贷方 ${money(openingValidation.value.totalCredit)}`
    : openingValidation.value?.issues.map((i) => i.message).join("；"),
);
const processOptions = [
  { label: "费用计提", value: "ACCRUAL" },
  { label: "费用摊销", value: "AMORTIZATION" },
  { label: "固定资产折旧", value: "DEPRECIATION" },
  { label: "损益结转", value: "PROFIT_CARRY_FORWARD" },
];
const partnerStatusOptions = [
  { label: "已确认", value: "CONFIRMED" },
  { label: "金额一致", value: "MATCHED" },
  { label: "存在争议", value: "DISPUTED" },
];
const periodColumns = [
  { title: "类型", key: "type", width: 120 },
  { title: "说明", dataIndex: "description", width: 240 },
  { title: "金额（税价不适用，元）", key: "amount", width: 170 },
  {
    title: "借 / 贷",
    customRender: ({ record }: any) =>
      `${record.debitAccountCode} / ${record.creditAccountCode}`,
    width: 150,
  },
  { title: "自动转回", key: "reverse", width: 130 },
  { title: "状态", key: "status", width: 100 },
  { title: "操作", key: "action", width: 100, fixed: "right" as const },
];
const voucherColumns = [
  { title: "幂等键", dataIndex: "idempotencyKey", width: 220 },
  { title: "业务号", dataIndex: "businessNo", width: 220 },
  { title: "状态", key: "status", width: 110 },
  { title: "尝试", dataIndex: "attemptCount", width: 80 },
  { title: "失败原因", key: "error", width: 300 },
];
const budgetColumns = [
  { title: "控制项", key: "name", width: 250 },
  { title: "预算", key: "budget", width: 130 },
  { title: "承诺", key: "committed", width: 130 },
  { title: "实际", key: "actual", width: 130 },
  { title: "预测", key: "forecast", width: 130 },
  { title: "偏差", key: "variance", width: 130 },
  { title: "使用率", key: "usage", width: 180 },
  { title: "状态", key: "status", width: 110 },
];
const partnerColumns = [
  { title: "往来单位", key: "partner", width: 250 },
  { title: "账面余额（税价随来源单据，元）", key: "ledger", width: 220 },
  { title: "确认余额（税价随来源单据，元）", key: "confirmed", width: 220 },
  { title: "差异", key: "difference", width: 150 },
  { title: "状态", key: "status", width: 110 },
  { title: "确认人", dataIndex: "confirmedBy", width: 120 },
  { title: "操作", key: "action", width: 90, fixed: "right" as const },
];
const cashColumns = [
  { title: "情景", key: "name", width: 250 },
  { title: "期初现金", key: "opening", width: 150 },
  { title: "计划收款", key: "receipt", width: 150 },
  { title: "计划付款", key: "payment", width: 150 },
  { title: "预测期末", key: "forecast", width: 160 },
  { title: "状态", key: "status", width: 100 },
  { title: "假设", dataIndex: "assumptions", width: 240 },
];
const projectColumns = [
  { title: "项目", key: "project", width: 280 },
  { title: "合同额（含税，元）", key: "contract", width: 170 },
  { title: "预算", key: "budget", width: 140 },
  { title: "实际成本（税价随来源单据，元）", key: "cost", width: 220 },
  { title: "毛利（税价随来源单据，元）", key: "margin", width: 200 },
  { title: "毛利率", key: "rate", width: 100 },
  { title: "风险", key: "risk", width: 240 },
];
const taxColumns = [
  { title: "申报期", key: "period", width: 110 },
  { title: "销项税", key: "output", width: 140 },
  { title: "进项税", key: "input", width: 140 },
  { title: "应纳税", key: "payable", width: 140 },
  { title: "总账税额", key: "ledger", width: 140 },
  { title: "差异", key: "difference", width: 140 },
  { title: "状态", key: "status", width: 110 },
  { title: "申报编号", dataIndex: "filingReference", width: 160 },
  { title: "操作", key: "action", width: 110, fixed: "right" as const },
];
const consolidationColumns = [
  { title: "批次", dataIndex: "name", width: 220 },
  { title: "期间", key: "period", width: 100 },
  { title: "主体", dataIndex: "entityCount", width: 80 },
  { title: "合计收入", key: "revenue", width: 140 },
  { title: "合计费用", key: "expense", width: 140 },
  { title: "内部收入抵销", key: "elimination", width: 150 },
  { title: "合并利润（税价随来源单据，元）", key: "profit", width: 220 },
  { title: "状态", key: "status", width: 100 },
  { title: "操作", key: "action", width: 120, fixed: "right" as const },
];
const snapshotColumns = [
  { title: "类型", key: "type", width: 160 },
  { title: "范围", dataIndex: "scopeKey", width: 180 },
  { title: "内容摘要", key: "hash", width: 210 },
  { title: "证据说明", dataIndex: "evidenceNote", width: 260 },
  { title: "固化人", dataIndex: "capturedBy", width: 120 },
  { title: "固化时间", key: "time", width: 170 },
];

async function loadAll() {
  loading.value = true;
  try {
    const [o, j, b, c, p, t, co, s, v, projects] = await Promise.all([
      getOperationsOverview(),
      listPeriodJobs({ year: year.value, month: month.value }),
      listBudgetVariance(),
      listCashScenarios(),
      listPartnerStatements(partnerType.value, periodEnd.value),
      listTaxFilings(),
      listConsolidations(),
      listReportSnapshots(),
      listVoucherRequests(),
      listProjectProfitability(),
    ]);
    Object.assign(overview, o);
    periodJobs.value = j;
    budgetRows.value = b;
    cashRows.value = c;
    partnerRows.value = p;
    taxRows.value = t;
    consolidationRows.value = co;
    snapshotRows.value = s;
    voucherRequests.value = v;
    projectRows.value = projects;
  } catch (e: any) {
    message.error(e?.message || "财务运营数据加载失败");
  } finally {
    loading.value = false;
  }
}
async function loadPartners() {
  partnerRows.value = await listPartnerStatements(
    partnerType.value,
    periodEnd.value,
  );
}
async function runOpeningValidation() {
  openingValidation.value = await validateOpening(year.value);
}
async function savePeriodJob() {
  if (
    !periodForm.description ||
    !periodForm.amount ||
    !periodForm.idempotencyKey
  )
    return message.warning("请完整填写任务信息");
  saving.value = true;
  try {
    await createPeriodJob({
      fiscalYear: year.value,
      periodNo: month.value,
      ...periodForm,
      reversalDate: periodForm.autoReverse
        ? periodReversalDate.value?.format("YYYY-MM-DD")
        : undefined,
    });
    periodModal.value = false;
    message.success("期末任务已建立");
    await loadAll();
  } catch (e: any) {
    message.error(e?.message || "期末任务建立失败");
  } finally {
    saving.value = false;
  }
}
async function executeJob(row: PeriodJob) {
  try {
    await executePeriodJob(row.id);
    message.success("期末凭证已完成");
    await loadAll();
  } catch (e: any) {
    message.error(e?.message || "期末凭证执行失败");
  }
}
async function reverseDue() {
  try {
    await reverseDuePeriodJobs(dayjs().format("YYYY-MM-DD"));
    message.success("到期转回处理完成");
    await loadAll();
  } catch (e: any) {
    message.error(e?.message || "到期转回处理失败");
  }
}
function openPartnerConfirm(row: PartnerStatement) {
  selectedPartner.value = row;
  partnerForm.statementBalance = row.confirmedBalance || row.ledgerBalance;
  partnerForm.status = row.status === "PENDING" ? "CONFIRMED" : row.status;
  partnerForm.note = row.confirmationNote || "余额已核对";
  partnerModal.value = true;
}
async function confirmPartner() {
  if (!selectedPartner.value || !partnerForm.note)
    return message.warning("请填写确认说明");
  saving.value = true;
  try {
    await confirmPartnerStatement(
      selectedPartner.value.partnerType,
      selectedPartner.value.partnerId,
      periodEnd.value,
      partnerForm,
    );
    partnerModal.value = false;
    message.success("往来确认已留痕");
    await loadAll();
  } catch (e: any) {
    message.error(e?.message || "往来确认失败");
  } finally {
    saving.value = false;
  }
}
async function saveCashScenario() {
  if (!cashForm.name) return message.warning("请填写情景名称");
  saving.value = true;
  try {
    await createCashScenario({
      ...cashForm,
      asOfDate: dayjs().format("YYYY-MM-DD"),
    });
    cashModal.value = false;
    message.success("现金情景已生成");
    await loadAll();
  } catch (e: any) {
    message.error(e?.message || "现金情景生成失败");
  } finally {
    saving.value = false;
  }
}
async function reconcileTax() {
  try {
    await reconcileTaxFiling(year.value, month.value);
    message.success("税务勾稽完成");
    await loadAll();
  } catch (e: any) {
    message.error(e?.message || "税务勾稽失败");
  }
}
function openTaxLock(row: TaxFiling) {
  selectedTax.value = row;
  taxReference.value = "";
  taxModal.value = true;
}
async function confirmTaxLock() {
  if (!selectedTax.value || !taxReference.value.trim())
    return message.warning("请输入申报编号");
  saving.value = true;
  try {
    await lockTaxFiling(
      selectedTax.value.fiscalYear,
      selectedTax.value.periodNo,
      taxReference.value.trim(),
    );
    taxModal.value = false;
    message.success("申报已锁定并固化快照");
    await loadAll();
  } catch (e: any) {
    message.error(e?.message || "申报锁定失败");
  } finally {
    saving.value = false;
  }
}
function addEntity() {
  consolidationForm.entities.push({
    entityCode: "",
    entityName: "",
    revenue: 0,
    expense: 0,
  });
}
async function saveConsolidation() {
  if (
    !consolidationForm.name ||
    consolidationForm.entities.some((e) => !e.entityCode || !e.entityName)
  )
    return message.warning("请完整填写主体信息");
  saving.value = true;
  try {
    await createConsolidation({
      fiscalYear: year.value,
      periodNo: month.value,
      ...consolidationForm,
    });
    consolidationModal.value = false;
    message.success("合并批次已建立");
    await loadAll();
  } catch (e: any) {
    message.error(e?.message || "合并批次建立失败");
  } finally {
    saving.value = false;
  }
}
async function completeRun(row: Consolidation) {
  try {
    await completeConsolidation(row.id);
    message.success("合并底稿已固化");
    await loadAll();
  } catch (e: any) {
    message.error(e?.message || "合并底稿固化失败");
  }
}
async function captureCurrentSnapshot() {
  try {
    await captureReportSnapshot({
      reportType: "FINANCE_OVERVIEW",
      scopeKey: periodLabel.value,
      fiscalYear: year.value,
      periodNo: month.value,
      payload: JSON.stringify({
        overview,
        budgetRows: budgetRows.value,
        periodJobs: periodJobs.value,
      }),
      evidenceNote: "财务运营工作台手工固化",
    });
    message.success("当前概览已固化");
    await loadAll();
  } catch (e: any) {
    message.error(e?.message || "当前概览固化失败");
  }
}
function money(v: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    minimumFractionDigits: 2,
  }).format(Number(v || 0));
}
function pad(v: number) {
  return String(v).padStart(2, "0");
}
function dateTime(v?: string) {
  return v ? dayjs(v).format("YYYY-MM-DD HH:mm") : "-";
}
function processLabel(v: string) {
  return processOptions.find((i) => i.value === v)?.label || v;
}
function reportLabel(v: string) {
  return (
    (
      {
        TAX_FILING: "税务申报",
        CONSOLIDATION: "合并底稿",
        PERIOD_CLOSE: "关账证据",
        FINANCE_OVERVIEW: "运营概览",
      } as Record<string, string>
    )[v] || v
  );
}
function statusLabel(v: string) {
  return (
    (
      {
        PENDING: "待处理",
        COMPLETED: "已完成",
        REVERSED: "已转回",
        FAILED: "失败",
        PROCESSING: "处理中",
        SUCCEEDED: "成功",
        COMPENSATED: "已补偿",
        OVERRUN: "超预算",
        WARNING: "预警",
        NORMAL: "正常",
        NO_BASELINE: "无基准",
        MATCHED: "一致",
        DISPUTED: "争议",
        CONFIRMED: "已确认",
        DRAFT: "草稿",
        RECONCILED: "已勾稽",
        FILED: "已申报",
        LOCKED: "已锁定",
        APPROVED: "已批准",
        ARCHIVED: "已归档",
        HIGH: "高风险",
        MEDIUM: "中风险",
        LOW: "低风险",
      } as Record<string, string>
    )[v] || v
  );
}
function statusColor(v: string) {
  if (["FAILED", "OVERRUN", "DISPUTED", "HIGH"].includes(v)) return "red";
  if (["WARNING", "PENDING", "DRAFT", "PROCESSING", "MEDIUM"].includes(v))
    return "orange";
  if (
    [
      "COMPLETED",
      "SUCCEEDED",
      "MATCHED",
      "CONFIRMED",
      "RECONCILED",
      "LOCKED",
      "APPROVED",
      "NORMAL",
      "LOW",
    ].includes(v)
  )
    return "green";
  return "default";
}
function refreshPeriodContext() {
  periodForm.idempotencyKey = `PE-${periodLabel.value}-${Date.now()}`;
  periodReversalDate.value = workDate.value.add(1, "month").startOf("month");
}
watch(workDate, () => {
  refreshPeriodContext();
  loadAll();
});
onMounted(() => {
  refreshPeriodContext();
  loadAll();
  runOpeningValidation();
});
</script>

<style scoped>
.finance-ops {
  gap: 16px;
}
.ops-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  padding: 4px 2px;
}
.ops-header h2 {
  margin: 3px 0 2px;
  font-size: 24px;
  line-height: 1.25;
  letter-spacing: 0;
}
.ops-header p,
.command-row p {
  margin: 0;
  color: #667085;
}
.eyebrow {
  font-size: 11px;
  font-weight: 700;
  color: #b42318;
  letter-spacing: 0;
}
.control-strip {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  border: 1px solid #e4e7ec;
  border-radius: 6px;
  background: #fff;
  overflow: hidden;
}
.control-strip button {
  min-width: 0;
  text-align: left;
  border: 0;
  border-right: 1px solid #e4e7ec;
  background: #fff;
  padding: 13px 14px;
  cursor: pointer;
}
.control-strip button:last-child {
  border-right: 0;
}
.control-strip span,
.control-strip small {
  display: block;
  color: #667085;
  font-size: 12px;
}
.control-strip strong {
  display: block;
  margin: 3px 0;
  font-size: 19px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.danger,
.control-strip strong.danger {
  color: #b42318;
}
.control-strip strong.warning {
  color: #b54708;
}
.ops-surface {
  padding: 0 18px 18px;
  border: 1px solid #e4e7ec;
  border-radius: 6px;
  background: #fff;
}
.command-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin: 2px 0 16px;
}
.command-row h3,
.subsection-heading h3 {
  margin: 0 0 3px;
  font-size: 16px;
}
.inline-alert {
  margin-bottom: 14px;
}
.subsection-heading {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin: 24px 0 10px;
}
.subsection-heading span {
  color: #667085;
  font-size: 12px;
}
.cell-note {
  display: block;
  color: #667085;
  font-size: 12px;
  margin-top: 3px;
}
.error-text {
  display: block;
  max-width: 300px;
  color: #b42318;
  white-space: normal;
}
.entity-row {
  display: grid;
  grid-template-columns: 120px 1fr 140px 140px;
  gap: 8px;
  margin-bottom: 8px;
}
.elimination-row {
  margin-top: 16px;
}
code {
  font-size: 12px;
  color: #344054;
}
.finance-ops :deep(.ant-tabs-nav) {
  margin-bottom: 16px;
}
.finance-ops :deep(.ant-table-cell) {
  vertical-align: middle;
}
@media (max-width: 1100px) {
  .control-strip {
    grid-template-columns: repeat(3, 1fr);
  }
  .control-strip button:nth-child(3) {
    border-right: 0;
  }
  .control-strip button:nth-child(-n + 3) {
    border-bottom: 1px solid #e4e7ec;
  }
}
@media (max-width: 640px) {
  .finance-ops {
    gap: 12px;
  }
  .ops-header {
    align-items: flex-start;
    flex-direction: column;
  }
  .ops-header h2 {
    font-size: 21px;
  }
  .control-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .control-strip button {
    padding: 11px 10px;
    border-bottom: 1px solid #e4e7ec;
  }
  .control-strip button:nth-child(2n) {
    border-right: 0;
  }
  .control-strip button:nth-last-child(-n + 2) {
    border-bottom: 0;
  }
  .ops-surface {
    padding: 0 10px 12px;
  }
  .command-row {
    align-items: flex-start;
    flex-direction: column;
  }
  .entity-row {
    grid-template-columns: 1fr 1fr;
  }
  .entity-row :deep(.ant-input-number) {
    width: 100%;
  }
  .finance-ops :deep(.ant-tabs-tab) {
    padding: 10px 0;
  }
  .finance-ops :deep(.ant-tabs-nav-list) {
    min-width: max-content;
  }
  .finance-ops :deep(.ant-tabs-nav-wrap) {
    overflow: auto;
  }
}
</style>
