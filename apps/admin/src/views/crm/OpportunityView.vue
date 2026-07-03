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

            <!-- desktop-table --><div class="desktop-table">
<a-table
        :row-selection="{selectedRowKeys:selectedBatchKeys,onChange:(keys:any)=>selectedBatchKeys.value=keys}"
        :columns="columns"
        :data-source="filteredOpportunities"
        :loading="loading"
        :row-key="(record: any) => record.id"
        :pagination="{ pageSize: 10 }"
        :scroll="{ x: 1180 }"
        :customRow="(record: any) => ({ onClick: (e: any) => { if (e.target?.closest?.('button,a,.ant-btn,.ant-tag,.ant-popconfirm,.ant-dropdown-trigger,input,select,[role=button]')) return; router.push('/crm/opportunities/' + record.id) } })"
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
              @click.stop="openAdvance(record)"
            >
              推进
            </a-button>
            <a-button
              v-if="auth.can('crm:opportunity:update') && record.stage !== 'WON' && record.stage !== 'LOST'"
              size="small"
              type="link"
              danger
              @click.stop="openMarkLost(record)"
            >
              丢单
            </a-button>
            <span @click.stop>
              <a-popconfirm
                v-if="auth.can('crm:opportunity:delete')"
                title="确实删除此商机？"
                @confirm="handleDeleteOpportunity(record)"
              >
                <a-button size="small" type="link" danger>删除</a-button>
              </a-popconfirm>
            </span>
          </template>
        </template>
      </a-table>
</div><!-- end desktop-table -->
    <div class="mobile-only">
      <div v-for="record in filteredOpportunities" :key="record.id" class="mobile-card-item" @click="router.push('/crm/opportunities/' + record.id)">
        <div class="mobile-card-header"><strong>{{ record.code }}</strong><a-tag :color="opportunityStageColor(record.stage)">{{ opportunityStageLabel(record.stage) }}</a-tag></div>
        <div class="mobile-card-body"><span>{{ record.customerName }}</span><strong>{{ formatMoney(record.expectedAmount) }}</strong></div>
        <div class="mobile-card-footer"><span v-if="record.nextAction">下一步：{{ record.nextAction }}</span><span v-if="record.nextActionAt">· {{ record.nextActionAt }}</span></div>
      </div>
    </div>
    </a-card>

    <a-modal v-model:open="createOpen" title="新增商机" width="760px" :confirm-loading="saving" @ok="handleCreate">
      <a-form ref="createFormRef" :model="createForm" :rules="createRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24"><a-form-item label="客户" name="customerId"><a-select v-model:value="createForm.customerId" show-search option-filter-prop="label" :options="customerOptions" /></a-form-item></a-col>

          <a-col :xs="24" :md="8"><a-form-item label="负责人" name="ownerName"><a-select v-model:value="createForm.ownerName" :options="userOptions" show-search option-filter-prop="label" placeholder="选择负责人" /></a-form-item></a-col>
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

    <a-modal v-model:open="markLostOpen" title="标记为丢单" width="480px" :confirm-loading="saving" @ok="handleConfirmMarkLost">
      <a-form layout="vertical">
        <a-form-item label="丢单原因" required>
          <a-textarea v-model:value="lostReason" :rows="3" placeholder="请说明丢单原因" />
        </a-form-item>
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
  createFollowUp,
  createOpportunity,
  deleteOpportunity,
  listCustomers,
  listOpportunities,
  type CustomerSummary,
  type Opportunity,
  type OpportunityStage,
} from "@/api/crm";
import { listUsersApi, type UserResponse } from "@/api/system";
import { useAuthStore } from "@/stores/auth";
import {
  formatMoney,
  generateCode,
  opportunityStageColor,
  opportunityStageLabel,
  opportunityStageOptions,
} from "./crm-options";

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const opportunities = ref<Opportunity[]>([]);
const customers = ref<CustomerSummary[]>([]);
const users = ref<UserResponse[]>([]);
const loading = ref(false);
const saving = ref(false);
const createOpen = ref(false);
const markLostOpen = ref(false);
const markLostRecord = ref<Opportunity | null>(null);
const lostReason = ref("");
const selectedBatchKeys = ref<string[]>([]);
async function handleBatchDeleteOpps() {
  const ids = [...selectedBatchKeys.value];
  selectedBatchKeys.value = [];
  for (const id of ids) try { await deleteOpportunity(id); } catch {}
  message.success('批量删除完成 (' + ids.length + ' 项)');
  await loadData();
}
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
  code: [],
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

const customerOptions = computed(() => customers.value.map((item) => ({ label: item.code ? `${item.name}（${item.code}）` : item.name, value: item.id })));
const userOptions = computed(() => users.value.map((u) => ({ label: u.displayName, value: u.displayName })));
const filteredOpportunities = computed(() => {
  const term = keyword.value.trim().toLowerCase();
  const results = opportunities.value.filter((item) => {
    const matchesStage = !stageFilter.value || item.stage === stageFilter.value;
    const text = `${item.code} ${item.customerName} ${item.needSummary}`.toLowerCase();
    return matchesStage && (!term || text.includes(term));
  });
  results.sort((a, b) => {
    const getOrder = (s: string) => s === "WON" ? 2 : s === "LOST" ? 3 : 1;
    return getOrder(a.stage) - getOrder(b.stage);
  });
  return results;
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
    [opportunities.value, customers.value, users.value] = await Promise.all([listOpportunities(), listCustomers(), listUsersApi(1, 9999).then(r => r.content)]);
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
    const payload = {
      ...createForm,
      stage: "LEAD" as const,
      probability: 10,
      nextActionAt: createForm.nextActionAt || undefined,
      nextAction: createForm.nextAction || undefined,
    };
    await createOpportunity(payload);
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
    code: generateCode("SJ"),
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
    nextAction: record.nextAction || "待确定",
    nextActionAt: record.nextActionAt || dateAfterDays(7),
  });
  advanceOpen.value = true;
}

async function handleDeleteOpportunity(record: Opportunity) {
  try {
    await deleteOpportunity(record.id);
    message.success("商机已删除");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "删除失败");
  }
}

function openMarkLost(record: Opportunity) {
  markLostRecord.value = record;
  lostReason.value = "";
  markLostOpen.value = true;
}

async function handleConfirmMarkLost() {
  if (!lostReason.value.trim()) {
    message.warning("请输入丢单原因");
    return;
  }
  const record = markLostRecord.value;
  if (!record) return;
  saving.value = true;
  try {
    await advanceOpportunity(record.id, { stage: "LOST", probability: 0, nextAction: "已标记为丢单", nextActionAt: new Date().toISOString().slice(0, 10) });
    try {
      await createFollowUp({
        customerId: record.customerId!,
        opportunityId: record.id,
        type: "VISIT" as const,
        subject: "商机推进至「丢单」",
        content: `丢单原因：${lostReason.value}`,
        followedAt: new Date().toISOString(),
        ownerName: auth.user?.displayName || "",
      });
    } catch {}
    markLostOpen.value = false;
    message.success("商机已标记为丢单");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "操作失败");
  } finally {
    saving.value = false;
  }
}

async function handleAdvance() {
  await advanceFormRef.value?.validate();
  if (!selectedOpportunity.value) return;
  saving.value = true;
  try {
    const result = await advanceOpportunity(selectedOpportunity.value.id, { ...advanceForm, nextActionAt: advanceForm.nextActionAt || dateAfterDays(7), nextAction: advanceForm.nextAction || "待确定" });
    try {
      await createFollowUp({
        customerId: selectedOpportunity.value.customerId!,
        opportunityId: selectedOpportunity.value.id,
        type: "VISIT" as const,
        subject: `商机推进至「${opportunityStageLabel(result.stage)}」`,
        content: `阶段推进：${opportunityStageLabel(selectedOpportunity.value.stage)} → ${opportunityStageLabel(result.stage)}，成功率 ${result.probability}%`,
        followedAt: new Date().toISOString(),
        nextAction: result.nextAction || undefined,
        ownerName: auth.user?.displayName || "",
      });
    } catch { /* auto-record is supplementary */ }
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
  return { customerId: undefined as string | undefined, code: "", needSummary: "", expectedAmount: 0, nextAction: "", nextActionAt: "", ownerName: "" };
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
