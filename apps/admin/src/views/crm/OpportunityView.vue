<template>
  <div class="page-stack">
    <a-card title="线索商机">
      <template #extra>
        <a-space>
          <a-button @click="loadData">刷新</a-button>
          <a-button v-if="auth.can('crm:opportunity:create')" type="primary" @click="openCreate()">
            新增商机
          </a-button>
        </a-space>
      </template>

      <a-row :gutter="[16, 16]" class="metric-row">
        <a-col :xs="12" :lg="6"><a-statistic title="进行中" :value="activeCount" suffix="项" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="预计金额" :value="activeAmount" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="加权金额" :value="weightedAmount" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="待办已到期" :value="overdueActionCount" suffix="项" /></a-col>
      </a-row>

      <a-space wrap class="table-toolbar">
        <a-select v-model:value="stageFilter" allow-clear placeholder="全部阶段" :options="stageOptions" style="width: 150px" />
        <a-input v-model:value="keyword" allow-clear placeholder="搜索客户、商机编号、需求" style="width: 280px" />
      </a-space>

      <a-table
        :columns="columns"
        :data-source="filteredOpportunities"
        :loading="loading"
        :row-key="(record: Opportunity) => record.id"
        :pagination="{ pageSize: 10 }"
        :scroll="{ x: 1180 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'opportunity'">
            <strong>{{ record.code }}</strong>
            <span class="table-subtitle">{{ record.customerName }} · {{ record.source || "来源未填" }}</span>
          </template>
          <template v-else-if="column.key === 'need'">
            <span class="line-clamp-2">{{ record.needSummary }}</span>
          </template>
          <template v-else-if="column.key === 'stage'">
            <a-tag :color="opportunityStageColor(record.stage)">{{ opportunityStageLabel(record.stage) }}</a-tag>
            <a-progress :percent="record.probability" size="small" :show-info="false" />
          </template>
          <template v-else-if="column.key === 'amount'">
            <strong>{{ formatMoney(record.expectedAmount) }}</strong>
          </template>
          <template v-else-if="column.key === 'nextAction'">
            <span>{{ record.nextAction || "-" }}</span>
            <span class="table-subtitle" :class="{ 'text-danger': actionOverdue(record) }">
              {{ record.nextActionAt || "未安排日期" }}
            </span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button
              v-if="auth.can('crm:opportunity:update')"
              size="small"
              type="link"
              :disabled="record.stage === 'WON' || record.stage === 'LOST'"
              @click="openAdvance(record)"
            >
              推进
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="createOpen" title="新增商机" width="760px" :confirm-loading="saving" @ok="handleCreate">
      <a-form ref="createFormRef" :model="createForm" :rules="createRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="8"><a-form-item label="商机编号" name="code"><a-input v-model:value="createForm.code" /></a-form-item></a-col>
          <a-col :xs="24" :md="16"><a-form-item label="客户" name="customerId"><a-select v-model:value="createForm.customerId" show-search option-filter-prop="label" :options="customerOptions" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="来源"><a-input v-model:value="createForm.source" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="负责人" name="ownerName"><a-input v-model:value="createForm.ownerName" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="预计金额"><a-input-number v-model:value="createForm.expectedAmount" :min="0" class="full-input" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="客户需求" name="needSummary"><a-textarea v-model:value="createForm.needSummary" :rows="3" /></a-form-item></a-col>
          <a-col :xs="24" :md="16"><a-form-item label="下一步动作"><a-input v-model:value="createForm.nextAction" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="计划日期"><a-input v-model:value="createForm.nextActionAt" type="date" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal v-model:open="advanceOpen" title="推进商机" width="620px" :confirm-loading="saving" @ok="handleAdvance">
      <a-alert v-if="selectedOpportunity" class="section-alert" type="info" :message="`${selectedOpportunity.code} · ${selectedOpportunity.customerName}`" />
      <a-form ref="advanceFormRef" :model="advanceForm" :rules="advanceRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="12"><a-form-item label="推进至" name="stage"><a-select v-model:value="advanceForm.stage" :options="stageOptions" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="成功率" name="probability"><a-input-number v-model:value="advanceForm.probability" :min="0" :max="100" addon-after="%" class="full-input" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="下一步动作" name="nextAction"><a-input v-model:value="advanceForm.nextAction" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="计划日期" name="nextActionAt"><a-input v-model:value="advanceForm.nextActionAt" type="date" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import { useRoute, useRouter } from "vue-router";
import {
  advanceOpportunity,
  createOpportunity,
  listCustomers,
  listOpportunities,
  type CustomerSummary,
  type Opportunity,
  type OpportunityStage,
} from "@/api/crm";
import { useAuthStore } from "@/stores/auth";
import {
  formatMoney,
  opportunityStageColor,
  opportunityStageLabel,
  opportunityStageOptions,
} from "./crm-options";

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const opportunities = ref<Opportunity[]>([]);
const customers = ref<CustomerSummary[]>([]);
const loading = ref(false);
const saving = ref(false);
const createOpen = ref(false);
const advanceOpen = ref(false);
const createFormRef = ref();
const advanceFormRef = ref();
const selectedOpportunity = ref<Opportunity | null>(null);
const stageFilter = ref<OpportunityStage>();
const keyword = ref("");
const stageOptions = [...opportunityStageOptions];

const columns = [
  { title: "商机 / 客户", key: "opportunity", width: 220 },
  { title: "需求", key: "need", width: 280 },
  { title: "阶段 / 成功率", key: "stage", width: 160 },
  { title: "预计金额", key: "amount", width: 140 },
  { title: "下一步动作", key: "nextAction", width: 260 },
  { title: "负责人", dataIndex: "ownerName", width: 110 },
  { title: "操作", key: "action", width: 80, fixed: "right" },
];

const createForm = reactive(initialCreateForm());
const advanceForm = reactive({ stage: "QUALIFIED" as OpportunityStage, probability: 30, nextAction: "", nextActionAt: "" });
const createRules = {
  code: [{ required: true, message: "请输入商机编号" }],
  customerId: [{ required: true, message: "请选择客户" }],
  needSummary: [{ required: true, message: "请输入客户需求" }],
  ownerName: [{ required: true, message: "请输入负责人" }],
};
const advanceRules = {
  stage: [{ required: true, message: "请选择目标阶段" }],
  probability: [{ required: true, message: "请输入成功率" }],
  nextAction: [{ required: true, message: "请输入下一步动作" }],
  nextActionAt: [{ required: true, message: "请选择计划日期" }],
};

const customerOptions = computed(() => customers.value.map((item) => ({ label: `${item.name}（${item.code}）`, value: item.id })));
const filteredOpportunities = computed(() => {
  const term = keyword.value.trim().toLowerCase();
  return opportunities.value.filter((item) => {
    const matchesStage = !stageFilter.value || item.stage === stageFilter.value;
    const text = `${item.code} ${item.customerName} ${item.needSummary}`.toLowerCase();
    return matchesStage && (!term || text.includes(term));
  });
});
const activeItems = computed(() => opportunities.value.filter((item) => item.stage !== "WON" && item.stage !== "LOST"));
const activeCount = computed(() => activeItems.value.length);
const activeAmount = computed(() => activeItems.value.reduce((sum, item) => sum + Number(item.expectedAmount || 0), 0));
const weightedAmount = computed(() => activeItems.value.reduce((sum, item) => sum + Number(item.expectedAmount || 0) * item.probability / 100, 0));
const overdueActionCount = computed(() => activeItems.value.filter(actionOverdue).length);

onMounted(async () => {
  await loadData();
  const customerId = typeof route.query.customer === "string" ? route.query.customer : undefined;
  if (route.query.create === "1" && customerId && customers.value.some(customer => customer.id === customerId)) {
    openCreate({
      customerId,
      source: "合同续约",
      needSummary: typeof route.query.need === "string" ? route.query.need : "原合同续约",
      expectedAmount: Number(route.query.amount || 0),
      nextAction: "确认续约服务范围、付款节点和合同周期",
      nextActionAt: dateAfterDays(7),
    });
    const { customer: _customer, create: _create, need: _need, amount: _amount, ...query } = route.query;
    await router.replace({ path: route.path, query });
  }
});

async function loadData() {
  loading.value = true;
  try {
    [opportunities.value, customers.value] = await Promise.all([listOpportunities(), listCustomers()]);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "商机加载失败");
  } finally {
    loading.value = false;
  }
}

async function handleCreate() {
  await createFormRef.value?.validate();
  saving.value = true;
  try {
    await createOpportunity({ ...createForm, stage: "LEAD", probability: 10 });
    Object.assign(createForm, initialCreateForm());
    createOpen.value = false;
    message.success("商机已创建");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "商机创建失败");
  } finally {
    saving.value = false;
  }
}

function openCreate(prefill: Partial<ReturnType<typeof initialCreateForm>> = {}) {
  const customer = prefill.customerId ? customers.value.find(item => item.id === prefill.customerId) : undefined;
  Object.assign(createForm, initialCreateForm(), {
    code: generateOpportunityCode(),
    ownerName: customer?.ownerName || auth.user?.displayName || "",
    ...prefill,
  });
  createOpen.value = true;
}

function openAdvance(record: Opportunity) {
  selectedOpportunity.value = record;
  Object.assign(advanceForm, {
    stage: nextStage(record.stage),
    probability: Math.max(record.probability, defaultProbability(nextStage(record.stage))),
    nextAction: record.nextAction || "",
    nextActionAt: record.nextActionAt || "",
  });
  advanceOpen.value = true;
}

async function handleAdvance() {
  await advanceFormRef.value?.validate();
  if (!selectedOpportunity.value) return;
  saving.value = true;
  try {
    await advanceOpportunity(selectedOpportunity.value.id, { ...advanceForm });
    advanceOpen.value = false;
    message.success("商机阶段已推进");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "商机推进失败");
  } finally {
    saving.value = false;
  }
}

function actionOverdue(record: Opportunity) {
  return Boolean(record.nextActionAt && record.nextActionAt < new Date().toISOString().slice(0, 10));
}

function nextStage(stage: OpportunityStage): OpportunityStage {
  const stages: OpportunityStage[] = ["LEAD", "QUALIFIED", "SOLUTION", "QUOTATION", "NEGOTIATION", "WON"];
  const index = stages.indexOf(stage);
  return index >= 0 && index < stages.length - 1 ? stages[index + 1] : stage;
}

function defaultProbability(stage: OpportunityStage) {
  return { LEAD: 10, QUALIFIED: 30, SOLUTION: 50, QUOTATION: 70, NEGOTIATION: 85, WON: 100, LOST: 0 }[stage];
}

function initialCreateForm() {
  return { customerId: undefined as string | undefined, code: "", source: "", needSummary: "", expectedAmount: 0, nextAction: "", nextActionAt: "", ownerName: "" };
}

function generateOpportunityCode() {
  const date = new Date();
  const stamp = `${date.getFullYear()}${String(date.getMonth() + 1).padStart(2, "0")}${String(date.getDate()).padStart(2, "0")}`;
  const time = `${String(date.getHours()).padStart(2, "0")}${String(date.getMinutes()).padStart(2, "0")}${String(date.getSeconds()).padStart(2, "0")}`;
  return `SJ-${stamp}-${time}`;
}

function dateAfterDays(days: number) {
  const date = new Date();
  date.setDate(date.getDate() + days);
  return date.toISOString().slice(0, 10);
}

function moneyFormatter({ value }: { value: number }) {
  return formatMoney(value);
}
</script>
