<template>
  <div class="page-stack">
    <a-card>
      <template #extra>
        <a-space>
          <a-button @click="goBack">返回列表</a-button>
          <a-button @click="loadData">刷新</a-button>
        </a-space>
      </template>

      <div v-if="loading" style="text-align: center; padding: 48px">
        <a-spin size="large" />
      </div>

      <template v-else-if="record">
        <div class="detail-title">
          <h2>{{ record.code }}</h2>
          <a-tag :color="opportunityStageColor(record.stage)">{{ opportunityStageLabel(record.stage) }}</a-tag>
          <a-progress v-if="record.stage !== 'WON' && record.stage !== 'LOST'" :percent="record.probability" size="small" style="width: 120px" />
        </div>

        <a-descriptions bordered :column="{ xs: 1, sm: 2, md: 3 }" style="margin-top: 20px">
          <a-descriptions-item label="客户">{{ record.customerName }}</a-descriptions-item>
          <a-descriptions-item label="来源">{{ record.source || "未填" }}</a-descriptions-item>
          <a-descriptions-item label="负责人">{{ record.ownerName }}</a-descriptions-item>
          <a-descriptions-item label="预计金额">
            <strong>{{ formatMoney(record.expectedAmount) }}</strong>
          </a-descriptions-item>
          <a-descriptions-item label="成功率">{{ record.probability }}%</a-descriptions-item>
          <a-descriptions-item label="更新时间">{{ formatDateTime(record.updatedAt) }}</a-descriptions-item>
        </a-descriptions>

        <a-card title="客户需求" style="margin-top: 16px">
          <p style="margin: 0; white-space: pre-wrap">{{ record.needSummary || "未填写客户需求" }}</p>
        </a-card>

        <a-card title="下一步动作" style="margin-top: 16px">
          <a-row :gutter="16">
            <a-col :span="16">
              <p style="margin: 0">{{ record.nextAction || "未安排" }}</p>
            </a-col>
            <a-col :span="8">
              <span class="table-subtitle">计划日期：{{ record.nextActionAt || "未设置" }}</span>
              <span v-if="actionOverdue" class="table-subtitle text-danger">已逾期</span>
            </a-col>
          </a-row>
        </a-card>

        <a-card v-if="record?.stage === 'LOST' && lostFollowUp" title="丢单信息" style="margin-top: 16px">
          <a-descriptions bordered :column="1">
            <a-descriptions-item label="丢单时间">{{ formatDateTime(lostFollowUp.followedAt) }}</a-descriptions-item>
            <a-descriptions-item label="丢单原因">{{ lostFollowUp.content.replace("丢单原因：", "") }}</a-descriptions-item>
          </a-descriptions>
        </a-card>
        <a-card v-if="relatedQuote" title="关联报价" style="margin-top: 16px">
          <a-button type="link" @click="router.push('/crm/quotes/' + relatedQuote.id)">
            {{ relatedQuote.code }} V{{ relatedQuote.versionNo }} · {{ formatMoney(relatedQuote.amount) }}
          </a-button>
          <p style="margin:4px 0 0;color:#8c8c8c;font-size:12px">
            状态：<a-tag :color="quoteStatusColor(relatedQuote.status)">{{ quoteStatusLabel(relatedQuote.status) }}</a-tag>
            <template v-if="relatedQuote.convertedContractId">
              · 已转 <a @click="router.push('/crm/contracts/' + relatedQuote.convertedContractId)">合同</a>
            </template>
          </p>
        </a-card>

        <a-card title="推进与事件时间线" style="margin-top: 16px">
          <template #extra>
            <a-radio-group v-model:value="timelineMode" size="small" button-style="solid">
              <a-radio-button value="advance">推进</a-radio-button>
              <a-radio-button value="all">全部</a-radio-button>
            </a-radio-group>
          </template>
          <a-empty v-if="timelineEvents.length === 0" description="暂无记录" />
          <a-timeline v-else>
            <a-timeline-item v-for="evt in timelineEvents" :key="evt.id" :color="evt.color">
              <template #label>{{ evt.time }}</template>
              <strong>{{ evt.title }}</strong>
              <p style="margin: 4px 0 0">{{ evt.desc }}</p>
            </a-timeline-item>
          </a-timeline>
        </a-card>

        <a-card title="跟进记录" style="margin-top: 16px">
          <template #extra>
            <a-button v-if="auth.can('crm:followup:create')" size="small" type="primary" @click="openCreateFollowUp">
              新增跟进
            </a-button>
          </template>

          <a-empty v-if="!loading && followUps.length === 0" description="暂无跟进记录" style="margin: 24px 0" />

          <a-comment v-for="item in followUps" :key="item.id" :author="item.ownerName" :datetime="formatDateTime(item.followedAt)">
            <template #avatar>
              <a-avatar :style="{ backgroundColor: followUpTypeColor(item.type) === 'red' ? '#f5222d' : followUpTypeColor(item.type) === 'blue' ? '#1890ff' : '#8c8c8c' }" size="small">
                {{ item.type.slice(0, 1) }}
              </a-avatar>
            </template>
            <template #content>
              <a-space>
                <a-tag :color="followUpTypeColor(item.type)">{{ followUpTypeLabel(item.type) }}</a-tag>
                <strong>{{ item.subject }}</strong>
              </a-space>
              <p style="margin: 8px 0 0; white-space: pre-wrap">{{ item.content }}</p>
              <p v-if="item.nextAction" class="table-subtitle" style="margin: 6px 0 0">
                下一步：{{ item.nextAction }}
              </p>
            </template>
          </a-comment>
        </a-card>
      </template>

      <a-empty v-else description="未找到商机" />
    </a-card>
    <a-modal v-model:open="createFollowUpOpen" title="新增跟进记录" width="640px" :confirm-loading="savingFollowUp" @ok="handleCreateFollowUp">
      <a-form ref="followUpFormRef" :model="followUpForm" :rules="followUpRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="12">
            <a-form-item label="跟进类型" name="type">
              <a-select v-model:value="followUpForm.type" :options="typeOptions" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="负责人" name="ownerName">
              <a-select v-model:value="followUpForm.ownerName" :options="userOptions" show-search option-filter-prop="label" placeholder="选择负责人" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="发生时间" name="followedAt">
              <a-input v-model:value="followUpForm.followedAt" type="datetime-local" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="主题" name="subject">
              <a-input v-model:value="followUpForm.subject" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="沟通内容" name="content">
              <a-textarea v-model:value="followUpForm.content" :rows="4" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="下一步动作">
              <a-input v-model:value="followUpForm.nextAction" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import { useRoute, useRouter } from "vue-router";
import { createFollowUp, getOpportunity, listFollowUps, listQuotes, type FollowUp, type FollowUpType, type Opportunity } from "@/api/crm";
import { listUsersApi, type UserResponse } from "@/api/system";
import { useAuthStore } from "@/stores/auth";
import { followUpTypeColor, followUpTypeLabel, followUpTypeOptions, formatMoney, opportunityStageColor, opportunityStageLabel, quoteStatusColor, quoteStatusLabel } from "./crm-options";

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const record = ref<Opportunity | null>(null);
const relatedQuote = ref<any>(null);
const users = ref<UserResponse[]>([]);
const loading = ref(true);
const timelineMode = ref("advance");
const id = route.params.id as string;

// Follow-up state
const followUps = ref<FollowUp[]>([]);
const createFollowUpOpen = ref(false);
const savingFollowUp = ref(false);
const followUpFormRef = ref();
const typeOptions = [...followUpTypeOptions];
const followUpForm = reactive(initialFollowUpForm());
const followUpRules = {
  type: [{ required: true, message: "请选择跟进类型" }],
  subject: [{ required: true, message: "请输入主题" }],
  content: [{ required: true, message: "请输入沟通内容" }],
  followedAt: [{ required: true, message: "请选择发生时间" }],
  ownerName: [{ required: true, message: "请选择负责人" }],
};

const userOptions = computed(() => {
  const options = users.value
    .filter((item) => item.enabled)
    .map((item) => ({ label: `${item.displayName} · ${item.username}`, value: item.displayName }));
  const selected = followUpForm.ownerName;
  return selected && !options.some((item) => item.value === selected)
    ? [{ label: selected, value: selected }, ...options]
    : options;
});

const lostFollowUp = computed(() => followUps.value.find((item) => item.subject.startsWith("商机推进至「丢单」")));

const stageAdvancements = computed(() => followUps.value.filter((item) => item.subject.startsWith("商机推进至")));

const timelineEvents = computed(() => {
  const events: Array<{ id: string; time: string; title: string; desc: string; color: string; sortKey: string; isAdvance: boolean }> = [];

  // Stage advancements
  stageAdvancements.value.forEach((item) => {
    events.push({
      id: "adv-" + item.id,
      time: formatDateTime(item.followedAt) || "",
      title: item.subject,
      desc: item.content,
      color: "green",
      sortKey: item.followedAt || "",
      isAdvance: true,
    });
  });

  // Related quote event
  if (relatedQuote.value) {
    events.push({
      id: "quote-" + relatedQuote.value.id,
      time: formatDateTime(relatedQuote.value.createdAt || relatedQuote.value.updatedAt),
      title: "报价创建：" + relatedQuote.value.code + " V" + (relatedQuote.value.versionNo || 1),
      desc: "金额 " + formatMoney(relatedQuote.value.amount) + " · " + (relatedQuote.value.serviceScope?.slice(0, 50) || ""),
      color: "purple",
      sortKey: relatedQuote.value.createdAt || relatedQuote.value.updatedAt || "",
      isAdvance: false,
    });
  }

  // Sort by time ascending (if possible)
  events.sort((a, b) => (a.sortKey || "").localeCompare(b.sortKey || ""));

  if (timelineMode.value === "advance") {
    return events.filter((e) => e.isAdvance);
  }
  return events;
});

const actionOverdue = computed(() =>
  Boolean(record.value?.nextActionAt && record.value.nextActionAt < new Date().toISOString().slice(0, 10))
);

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    const [opp, allFollowUps, userPage] = await Promise.all([
      getOpportunity(id),
      listFollowUps(),
      listUsersApi(0, 999).catch(() => ({ content: [] as UserResponse[] })),
    ]);
    record.value = opp;
    followUps.value = allFollowUps.filter((item) => item.opportunityId === id);
    users.value = userPage.content;
    // Load related quote
    try {
      const allQuotes = await listQuotes();
      const foundQuote = allQuotes.find((q) => q.opportunityId === id);
      if (foundQuote) relatedQuote.value = foundQuote;
    } catch { /* quote fetch is supplementary */ }
  } catch (error) {
    message.error(error instanceof Error ? error.message : "商机加载失败");
  } finally {
    loading.value = false;
  }
}

function goBack() {
  router.push("/crm/opportunities");
}

// Follow-up functions
function openCreateFollowUp() {
  Object.assign(followUpForm, initialFollowUpForm());
  createFollowUpOpen.value = true;
}

async function handleCreateFollowUp() {
  await followUpFormRef.value?.validate();
  savingFollowUp.value = true;
  try {
    await createFollowUp({
      customerId: record.value!.customerId!,
      opportunityId: id,
      type: followUpForm.type,
      subject: followUpForm.subject,
      content: followUpForm.content,
      followedAt: new Date(followUpForm.followedAt).toISOString(),
      nextAction: followUpForm.nextAction || undefined,
      ownerName: followUpForm.ownerName,
    });
    Object.assign(followUpForm, initialFollowUpForm());
    createFollowUpOpen.value = false;
    message.success("跟进记录已保存");
    await loadFollowUps();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "跟进记录保存失败");
  } finally {
    savingFollowUp.value = false;
  }
}

async function loadFollowUps() {
  try {
    const all = await listFollowUps();
    followUps.value = all.filter((item) => item.opportunityId === id);
  } catch {
    // Follow-ups are supplementary; don't show error
  }
}

function initialFollowUpForm() {
  const now = new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 16);
  return {
    type: "PHONE" as FollowUpType,
    subject: "",
    content: "",
    followedAt: now,
    nextAction: "",
    ownerName: auth.user?.displayName || "",
  };
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
</script>
