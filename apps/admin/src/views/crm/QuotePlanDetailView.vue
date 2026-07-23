<template>
  <div class="page-stack">
    <a-card>
      <template #extra>
        <a-space>
          <a-button @click="goBack">返回列表</a-button>
          <a-button @click="loadData">刷新</a-button>
          <a-button v-if="record" @click="handlePrintQuote"
            >打印报价单</a-button
          >
          <a-button v-if="record" type="primary" @click="openApproval"
            >审批进展</a-button
          >
        </a-space>
      </template>

      <div v-if="loading" style="text-align: center; padding: 48px">
        <a-spin size="large" />
      </div>

      <template v-else-if="record">
        <div class="detail-title">
          <h2>{{ record.code }}</h2>
          <a-tag>V{{ record.versionNo }}</a-tag>
          <a-tag :color="quoteStatusColor(record.status)">{{
            quoteStatusLabel(record.status)
          }}</a-tag>
        </div>

        <!-- Hide stepper during print -->
        <a-steps
          :current="lifecycleStep"
          size="small"
          style="
            margin: 20px 0 8px;
            padding: 12px 0;
            background: #fafafa;
            border-radius: 6px;
          "
          class="print-hide"
        >
          <a-step
            title="草稿"
            :status="stepStatuses[0]"
            :description="stepDescs[0]"
          />
          <a-step
            title="内部审批"
            :status="stepStatuses[1]"
            :description="stepDescs[1]"
          />
          <a-step
            title="客户确认"
            :status="stepStatuses[2]"
            :description="stepDescs[2]"
          />
          <a-step
            title="合同生成"
            :status="stepStatuses[3]"
            :description="stepDescs[3]"
          />
          <a-step
            title="应收管理"
            :status="stepStatuses[4]"
            :description="stepDescs[4]"
          />
        </a-steps>
        <div class="print-area">
          <a-descriptions
            bordered
            :column="{ xs: 1, sm: 2, md: 3 }"
            style="margin-top: 8px"
          >
            <a-descriptions-item label="客户">{{
              record.customerName
            }}</a-descriptions-item>
            <a-descriptions-item label="关联商机">{{
              record.opportunityCode || "未关联"
            }}</a-descriptions-item>
            <a-descriptions-item label="报价金额">
              <strong>{{ formatMoney(record.amount) }}</strong>
              <span class="table-subtitle"
                >税率 {{ formatTaxRate(record.taxRate) }}</span
              >
            </a-descriptions-item>
            <a-descriptions-item label="未税金额">
              <strong>{{
                formatMoney(
                  record.netAmount ??
                    calcNetAmount(record.amount, record.taxRate),
                )
              }}</strong>
            </a-descriptions-item>
            <a-descriptions-item label="服务频次">{{
              record.inspectCycle || "未设置"
            }}</a-descriptions-item>
            <a-descriptions-item label="付款方式/节点">{{
              record.paymentNodes || "未设置"
            }}</a-descriptions-item>
            <a-descriptions-item label="更新">{{
              formatDateTime(record.updatedAt)
            }}</a-descriptions-item>
          </a-descriptions>

          <a-card title="服务范围" style="margin-top: 16px">
            <p style="margin: 0; white-space: pre-wrap">
              {{ record.serviceScope }}
            </p>
          </a-card>

          <a-card title="审批进展" style="margin-top: 16px">
            <ApprovalProgressFlow :steps="quoteApprovalSteps(record)" />
          </a-card>

          <a-card title="报价预算与毛利测算" style="margin-top: 16px">
            <a-row :gutter="[16, 16]" class="metric-row">
              <a-col :xs="12" :md="6"
                ><a-statistic
                  title="报价金额"
                  :value="record.amount"
                  :formatter="moneyFormatter"
              /></a-col>
              <a-col :xs="12" :md="6"
                ><a-statistic
                  title="未税金额"
                  :value="
                    record.netAmount ??
                    calcNetAmount(record.amount, record.taxRate)
                  "
                  :formatter="moneyFormatter"
              /></a-col>
              <a-col :xs="12" :md="6"
                ><a-statistic
                  title="税率"
                  :value="record.taxRate ?? 13"
                  suffix="%"
              /></a-col>
              <a-col :xs="12" :md="6"
                ><a-statistic
                  title="预算成本"
                  :value="quoteMargin.cost"
                  :formatter="moneyFormatter"
              /></a-col>
              <a-col :xs="12" :md="6"
                ><a-statistic
                  title="预计毛利"
                  :value="quoteMargin.gross"
                  :formatter="moneyFormatter"
                  :value-style="{
                    color: quoteMargin.gross < 0 ? '#ff4d4f' : '#52c41a',
                  }"
              /></a-col>
              <a-col :xs="12" :md="6"
                ><a-statistic
                  title="毛利率"
                  :value="quoteMargin.rate"
                  suffix="%"
                  :precision="1"
                  :value-style="{
                    color:
                      quoteMargin.rate < 15
                        ? '#ff4d4f'
                        : quoteMargin.rate < 25
                          ? '#faad14'
                          : '#52c41a',
                  }"
              /></a-col>
            </a-row>
            <a-table
              size="small"
              :data-source="budgetRows"
              :columns="budgetColumns"
              :pagination="false"
              row-key="key"
              style="margin-top: 12px"
            >
              <template #bodyCell="{ column, record: row }">
                <template v-if="column.key === 'amount'">{{
                  formatMoney(row.amount)
                }}</template>
                <template v-else-if="column.key === 'ratio'"
                  >{{ row.ratio.toFixed(1) }}%</template
                >
              </template>
            </a-table>
            <a-alert
              v-if="quoteMargin.rate < 15"
              style="margin-top: 12px"
              type="warning"
              show-icon
              message="毛利率低于审批阈值"
              description="请在报价修订中复核预算、折扣权限或服务范围后再提交内部审批。"
            />
          </a-card>

          <a-card title="价格治理与版本追溯" style="margin-top: 16px">
            <a-row :gutter="[16, 16]">
              <a-col :xs="24" :lg="9">
                <a-descriptions bordered :column="1" size="small">
                  <a-descriptions-item label="当前版本"
                    >V{{ record.versionNo }}</a-descriptions-item
                  >
                  <a-descriptions-item label="成本覆盖">{{
                    quoteMargin.cost > 0 ? "已完成成本测算" : "尚未形成成本依据"
                  }}</a-descriptions-item>
                  <a-descriptions-item label="毛利审批">{{
                    quoteMargin.rate < 15
                      ? "低于安全阈值，需重点审批"
                      : "在建议安全线内"
                  }}</a-descriptions-item>
                  <a-descriptions-item label="客户反馈">{{
                    record.customerComment ||
                    (record.customerDecision
                      ? "已登记客户结论"
                      : "等待客户确认")
                  }}</a-descriptions-item>
                </a-descriptions>
              </a-col>
              <a-col :xs="24" :lg="15">
                <a-table
                  :columns="revisionColumns"
                  :data-source="revisions"
                  row-key="id"
                  size="small"
                  :pagination="{ pageSize: 5 }"
                >
                  <template #bodyCell="{ column, record: revision }">
                    <template v-if="column.key === 'version'"
                      ><a-tag>V{{ revision.versionNo }}</a-tag></template
                    >
                    <template v-else-if="column.key === 'amount'">{{
                      formatMoney(revision.amount)
                    }}</template>
                    <template v-else-if="column.key === 'margin'"
                      ><span
                        :class="{
                          'text-danger':
                            Number(revision.grossMarginRate || 0) < 15,
                        }"
                        >{{
                          Number(revision.grossMarginRate || 0).toFixed(1)
                        }}%</span
                      ></template
                    >
                    <template v-else-if="column.key === 'time'">{{
                      formatDateTime(revision.revisedAt)
                    }}</template>
                  </template>
                </a-table>
              </a-col>
            </a-row>
          </a-card>

          <!-- Enhanced converted contract card -->
          <a-card
            v-if="record.convertedContractId"
            title="已转合同"
            style="margin-top: 16px"
          >
            <a-descriptions
              v-if="relatedContract"
              bordered
              :column="2"
              size="small"
            >
              <a-descriptions-item label="合同编号">
                <a
                  @click="router.push('/crm/contracts/' + relatedContract.id)"
                  >{{ relatedContract.code }}</a
                >
              </a-descriptions-item>
              <a-descriptions-item label="项目名称">{{
                relatedContract.projectName
              }}</a-descriptions-item>
              <a-descriptions-item label="合同金额"
                ><strong>{{
                  formatMoney(relatedContract.amount)
                }}</strong></a-descriptions-item
              >
              <a-descriptions-item label="未税金额"
                ><strong>{{
                  formatMoney(
                    relatedContract.netAmount ??
                      calcNetAmount(
                        relatedContract.amount,
                        relatedContract.taxRate,
                      ),
                  )
                }}</strong></a-descriptions-item
              >
              <a-descriptions-item label="税率">{{
                formatTaxRate(relatedContract.taxRate)
              }}</a-descriptions-item>
              <a-descriptions-item label="合同周期"
                >{{ relatedContract.startDate }} ~
                {{ relatedContract.endDate }}</a-descriptions-item
              >
              <a-descriptions-item label="状态">
                <a-tag :color="contractStatusColor(relatedContract.status)">{{
                  contractStatusLabel(relatedContract.status)
                }}</a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="操作">
                <a-button
                  type="link"
                  size="small"
                  @click="router.push('/crm/contracts/' + relatedContract.id)"
                  >查看合同详情</a-button
                >
              </a-descriptions-item>
            </a-descriptions>
            <a-empty
              v-else-if="loadingContract"
              description="加载合同信息..."
            />
          </a-card>

          <!-- Receivable status card -->
          <a-card
            v-if="relatedContract && relatedReceivables.length > 0"
            title="应收进度"
            style="margin-top: 16px"
          >
            <a-row :gutter="16" class="metric-row">
              <a-col :xs="12" :md="6"
                ><a-statistic
                  title="应收总额"
                  :value="receivableSummary.total"
                  :formatter="moneyFormatter"
              /></a-col>
              <a-col :xs="12" :md="6"
                ><a-statistic
                  title="已开票"
                  :value="receivableSummary.invoiced"
                  :formatter="moneyFormatter"
                  :value-style="{ color: '#1890ff' }"
              /></a-col>
              <a-col :xs="12" :md="6"
                ><a-statistic
                  title="已回款"
                  :value="receivableSummary.received"
                  :formatter="moneyFormatter"
                  :value-style="{ color: '#52c41a' }"
              /></a-col>
              <a-col :xs="12" :md="6"
                ><a-statistic
                  title="未收"
                  :value="receivableSummary.outstanding"
                  :formatter="moneyFormatter"
                  :value-style="{
                    color:
                      receivableSummary.outstanding > 0 ? '#ff4d4f' : '#52c41a',
                  }"
              /></a-col>
            </a-row>
            <a-table
              size="small"
              :data-source="relatedReceivables"
              :columns="receivableMiniColumns"
              :pagination="false"
              :row-key="(r: any) => r.id"
              style="margin-top: 12px"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'amount'">{{
                  formatMoney(record.amount)
                }}</template>
                <template v-else-if="column.key === 'outstanding'"
                  ><span
                    :class="{ 'text-danger': +record.outstandingAmount > 0 }"
                    >{{ formatMoney(record.outstandingAmount) }}</span
                  ></template
                >
                <template v-else-if="column.key === 'status'"
                  ><a-tag :color="receivableStatusColor(record.status)">{{
                    receivableStatusLabel(record.status)
                  }}</a-tag></template
                >
              </template>
            </a-table>
          </a-card>
        </div>
        <!-- end print-area -->
      </template>

      <a-empty v-else description="未找到报价方案" />
    </a-card>
    <a-modal
      v-model:open="approvalOpen"
      title="报价审批进展"
      :footer="canApproveQuote ? undefined : null"
      :confirm-loading="saving"
      @ok="handleApproval"
    >
      <a-card v-if="record" size="small" title="流程阶段" class="section-alert">
        <ApprovalProgressFlow :steps="quoteApprovalSteps(record)" />
      </a-card>
      <a-form
        v-if="canApproveQuote"
        ref="approvalFormRef"
        :model="approvalForm"
        layout="vertical"
      >
        <a-form-item label="审批结论"
          ><a-radio-group
            v-model:value="approvalForm.decision"
            button-style="solid"
            ><a-radio-button value="APPROVED">通过</a-radio-button
            ><a-radio-button value="REJECTED"
              >驳回</a-radio-button
            ></a-radio-group
          ></a-form-item
        >
        <a-form-item label="审批意见"
          ><a-textarea v-model:value="approvalForm.comment" :rows="3"
        /></a-form-item>
        <a-form-item label="审批人"
          ><a-input v-model:value="approvalForm.approverName"
        /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import { useRoute, useRouter } from "vue-router";
import {
  getQuote,
  listContracts,
  listReceivables,
  listQuoteRevisions,
  processQuoteApproval,
  type ApprovalDecision,
  type QuotePlan,
  type ServiceContract,
  type Receivable,
  type QuoteRevision,
} from "@/api/crm";
import {
  formatMoney,
  quoteStatusColor,
  quoteStatusLabel,
  contractStatusColor,
  contractStatusLabel,
  receivableStatusColor,
  receivableStatusLabel,
} from "./crm-options";
import { useAuthStore } from "@/stores/auth";
import ApprovalProgressFlow, {
  type ApprovalProgressStep,
} from "@/components/ApprovalProgressFlow.vue";

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const record = ref<QuotePlan | null>(null);
const relatedContract = ref<ServiceContract | null>(null);
const relatedReceivables = ref<Receivable[]>([]);
const revisions = ref<QuoteRevision[]>([]);
const loading = ref(true);
const loadingContract = ref(false);
const saving = ref(false);
const approvalOpen = ref(false);
const approvalFormRef = ref();
const approvalForm = reactive<{
  decision: ApprovalDecision;
  comment: string;
  approverName: string;
}>({ decision: "APPROVED", comment: "同意报价", approverName: "" });
const id = route.params.id as string;
const canApproveQuote = computed(
  () =>
    record.value?.status === "PENDING_APPROVAL" &&
    auth.can("crm:quote:approve"),
);

const receivableMiniColumns = [
  { title: "应收编号", dataIndex: "code", width: 180 },
  { title: "应收金额", key: "amount", width: 130 },
  { title: "未收金额", key: "outstanding", width: 130 },
  { title: "到期日", dataIndex: "dueDate", width: 120 },
  { title: "状态", key: "status", width: 110 },
];
const budgetColumns = [
  { title: "成本类型", dataIndex: "label", key: "label", width: 140 },
  { title: "预算金额", key: "amount", width: 140 },
  { title: "占报价比", key: "ratio", width: 120 },
];
const revisionColumns = [
  { title: "版本", key: "version", width: 80 },
  { title: "报价金额", key: "amount", width: 130 },
  { title: "毛利率", key: "margin", width: 100 },
  { title: "修订说明", dataIndex: "revisionNote" },
  { title: "编辑人", dataIndex: "editorName", width: 100 },
  { title: "修订时间", key: "time", width: 170 },
];

const quoteMargin = computed(() => {
  if (!record.value) return { cost: 0, gross: 0, rate: 0 };
  const amount = Number(
    record.value.netAmount ??
      calcNetAmount(record.value.amount, record.value.taxRate),
  );
  const cost = Number(
    record.value.budgetAmount ??
      budgetRows.value.reduce((sum, item) => sum + item.amount, 0),
  );
  const gross = Number(record.value.grossMargin ?? amount - cost);
  const rate = Number(
    record.value.grossMarginRate ?? (amount > 0 ? (gross / amount) * 100 : 0),
  );
  return { cost, gross, rate };
});

const budgetRows = computed(() => {
  const amount = Number(record.value?.amount || 0);
  const rows = [
    {
      key: "labor",
      label: "人工",
      amount: Number(record.value?.laborBudget || 0),
    },
    {
      key: "material",
      label: "材料",
      amount: Number(record.value?.materialBudget || 0),
    },
    {
      key: "subcontract",
      label: "外包",
      amount: Number(record.value?.subcontractBudget || 0),
    },
    {
      key: "travel",
      label: "差旅",
      amount: Number(record.value?.travelBudget || 0),
    },
    {
      key: "other",
      label: "其他",
      amount: Number(record.value?.otherBudget || 0),
    },
  ];
  return rows.map((item) => ({
    ...item,
    amount: Math.round(item.amount * 100) / 100,
    ratio: amount > 0 ? (item.amount / amount) * 100 : 0,
  }));
});

const receivableSummary = computed(() => {
  const items = relatedReceivables.value;
  return {
    total: items.reduce((s, r) => s + Number(r.amount || 0), 0),
    invoiced: items.reduce(
      (s, r) => s + (r.invoiceNo ? Number(r.amount || 0) : 0),
      0,
    ),
    received: items.reduce(
      (s, r) => s + (Number(r.amount || 0) - Number(r.outstandingAmount || 0)),
      0,
    ),
    outstanding: items.reduce(
      (s, r) => s + Number(r.outstandingAmount || 0),
      0,
    ),
  };
});

// Process stepper
const lifecycleStep = computed(() => {
  const s = record.value?.status;
  if (!s) return 0;
  if (s === "DRAFT") return 0;
  if (s === "PENDING_APPROVAL" || s === "REJECTED") return 1;
  if (s === "APPROVED" || s === "CUSTOMER_DECLINED") return 2;
  if (s === "CUSTOMER_ACCEPTED") return 3;
  if (s === "CONVERTED") return 4;
  return 0;
});

const stepStatuses = computed(() => {
  const s = record.value?.status;
  const steps = ["finish", "wait", "wait", "wait", "wait"];
  if (!s) {
    steps[0] = "process";
    return steps;
  }
  if (s === "DRAFT") {
    steps[0] = "process";
  } else if (s === "PENDING_APPROVAL") {
    steps[0] = "finish";
    steps[1] = "process";
  } else if (s === "REJECTED") {
    steps[0] = "finish";
    steps[1] = "error";
  } else if (s === "APPROVED") {
    steps[0] = "finish";
    steps[1] = "finish";
    steps[2] = "process";
  } else if (s === "CUSTOMER_DECLINED") {
    steps[0] = "finish";
    steps[1] = "finish";
    steps[2] = "error";
  } else if (s === "CUSTOMER_ACCEPTED") {
    steps[0] = "finish";
    steps[1] = "finish";
    steps[2] = "finish";
    steps[3] = "process";
  } else if (s === "CONVERTED") {
    steps[0] = "finish";
    steps[1] = "finish";
    steps[2] = "finish";
    steps[3] = "finish";
    steps[4] = "finish";
  }
  return steps;
});

const stepDescs = computed(() => {
  const r = record.value;
  const descs = [formatDateTime(r?.updatedAt), "-", "-", "-", "-"];
  if (!r) return descs;
  if (r.status === "PENDING_APPROVAL") descs[1] = "待审批";
  else if (r.lastApproverName) descs[1] = r.lastApproverName;
  if (r.customerDecision === "ACCEPTED") descs[2] = "客户已接受";
  else if (r.customerDecision === "DECLINED") descs[2] = "客户已拒绝";
  else if (r.status === "APPROVED") descs[2] = "等待客户确认";
  if (r.convertedContractId && relatedContract.value)
    descs[3] = relatedContract.value.code || "-";
  if (relatedReceivables.value.length > 0)
    descs[4] = receivableSummary.value.outstanding > 0 ? "待收款" : "已结清";
  return descs;
});

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    [record.value, revisions.value] = await Promise.all([
      getQuote(id),
      listQuoteRevisions(id).catch(() => []),
    ]);
    // Load related contract if converted
    if (record.value?.convertedContractId) {
      loadingContract.value = true;
      try {
        const contracts = await listContracts();
        const found = contracts.find(
          (c) => c.id === record.value!.convertedContractId,
        );
        if (found) {
          relatedContract.value = found;
          // Load related receivables
          const allReceivables = await listReceivables();
          relatedReceivables.value = allReceivables.filter(
            (r) => r.contractId === found.id || r.sourceNo === found.code,
          );
        }
      } catch {
        /* supplementary data */
      } finally {
        loadingContract.value = false;
      }
    }
  } catch (error) {
    message.error(error instanceof Error ? error.message : "报价加载失败");
  } finally {
    loading.value = false;
  }
}

function handlePrintQuote() {
  window.print();
}
function openApproval() {
  Object.assign(approvalForm, {
    decision: "APPROVED",
    comment: "同意报价",
    approverName: auth.user?.displayName || "",
  });
  approvalOpen.value = true;
}
async function handleApproval() {
  if (!record.value) return;
  saving.value = true;
  try {
    await processQuoteApproval(record.value.id, { ...approvalForm });
    approvalOpen.value = false;
    message.success(
      approvalForm.decision === "APPROVED" ? "报价审批已通过" : "报价已驳回",
    );
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "审批处理失败");
  } finally {
    saving.value = false;
  }
}
function goBack() {
  router.push("/crm/quotes");
}
function formatDateTime(value?: string) {
  if (!value) return "";
  return new Date(value).toLocaleString("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}
function moneyFormatter({ value }: { value: number | string }) {
  return formatMoney(Number(value));
}

function quoteApprovalSteps(item: QuotePlan): ApprovalProgressStep[] {
  const internalDone = [
    "APPROVED",
    "CUSTOMER_ACCEPTED",
    "CUSTOMER_DECLINED",
    "CONVERTED",
  ].includes(item.status);
  return [
    {
      key: "start",
      personName: item.customerName || "发起人",
      title: "发起报价",
      time: item.updatedAt,
      note: `${item.code || "-"} · ${formatMoney(item.amount)}`,
      state: "done",
    },
    {
      key: "approval",
      personName: item.lastApproverName || "当前审批人",
      title:
        item.status === "PENDING_APPROVAL"
          ? "待审批"
          : item.status === "REJECTED"
            ? "已驳回"
            : internalDone
              ? "已同意"
              : "待提交",
      time: item.lastApprovalAt,
      note:
        item.status === "PENDING_APPROVAL"
          ? "等待报价审批"
          : item.lastApprovalComment || quoteStatusLabel(item.status),
      state:
        item.status === "PENDING_APPROVAL"
          ? "pending"
          : item.status === "REJECTED"
            ? "rejected"
            : internalDone
              ? "done"
              : "waiting",
    },
    {
      key: "customer",
      personName: item.customerDecisionBy || item.customerName || "客户确认",
      title:
        item.customerDecision === "ACCEPTED"
          ? "已确认"
          : item.customerDecision === "DECLINED"
            ? "已拒绝"
            : "待确认",
      time: item.customerDecidedAt,
      note:
        item.customerComment ||
        (internalDone ? "等待客户确认" : "审批通过后确认"),
      state:
        item.customerDecision === "ACCEPTED"
          ? "done"
          : item.customerDecision === "DECLINED"
            ? "rejected"
            : "waiting",
    },
    {
      key: "contract",
      personName: relatedContract.value?.code || "合同生成",
      title: item.convertedContractId ? "已生成" : "待生成",
      note: relatedContract.value?.projectName || "客户确认后生成合同",
      state: item.convertedContractId ? "done" : "waiting",
    },
  ];
}

function formatTaxRate(value?: number) {
  return `${Number(value ?? 13)
    .toFixed(2)
    .replace(/\.?0+$/, "")}%`;
}

function calcNetAmount(amount?: number, taxRate?: number) {
  const rate = Number(taxRate ?? 13);
  const divisor = 1 + rate / 100;
  return divisor > 0 ? Number(amount || 0) / divisor : Number(amount || 0);
}
</script>

<style scoped>
.metric-row .ant-statistic .ant-statistic-title {
  font-size: 12px;
}
</style>
<style>
@media print {
  /* 隐藏边栏、顶栏、按钮 */
  .app-sider,
  .ant-layout-sider {
    display: none !important;
  }
  .app-header,
  .ant-layout-header {
    display: none !important;
  }
  .ant-card-extra {
    display: none !important;
  }
  .ant-card-actions {
    display: none !important;
  }
  .print-hide {
    display: none !important;
  }
  .ant-btn {
    display: none !important;
  }
  button {
    display: none !important;
  }

  /* 主体内容占满宽度 */
  .app-content,
  .ant-layout-content {
    margin-left: 0 !important;
    padding: 20px !important;
    width: 100% !important;
  }

  /* 让主要内容铺满 */
  .page-stack {
    width: 100% !important;
  }

  /* 卡片打印样式 */
  .ant-card {
    box-shadow: none !important;
    border: 1px solid #ddd !important;
    break-inside: avoid;
  }
  .ant-card-body {
    padding: 20px !important;
  }

  /* 表格样式 */
  table {
    font-size: 11pt !important;
  }
  .ant-descriptions-item-label {
    font-size: 10pt !important;
  }
  .ant-descriptions-item-content {
    font-size: 10pt !important;
  }

  /* 背景 */
  body {
    background: white !important;
  }
}
</style>
