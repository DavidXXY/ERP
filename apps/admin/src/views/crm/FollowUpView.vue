<template>
  <div class="page-stack">
    <a-card>
      <template #title>
        <a-radio-group v-model:value="activeTab" button-style="solid" size="small">
          <a-radio-button value="pending">待办 <template v-if="pendingCount">{{ pendingCount }}</template></a-radio-button>
          <a-radio-button value="history">历史</a-radio-button>
        </a-radio-group>
      </template>
      <template #extra>
        <a-space>
          <a-button @click="loadData">刷新</a-button>
          <a-button v-if="auth.can('crm:followup:create')" type="primary" @click="openCreate()">新增记录</a-button>
        </a-space>
      </template>

      <a-space wrap class="table-toolbar">
        <a-select v-model:value="typeFilter" allow-clear placeholder="全部类型" :options="typeOptions" style="width: 140px" />
        <a-input v-model:value="keyword" allow-clear placeholder="搜索客户、主题或内容" style="width: 280px" />
      </a-space>

      <template v-if="activeTab === 'pending'">
        <a-empty v-if="pendingItems.length === 0" description="暂无待办事项" />
        <div v-for="group in pendingGroups" :key="group.key" class="fp-group">
          <div class="fp-group-header">
            <strong>{{ group.customerName }}</strong>
            <span v-if="group.opportunityCode" class="table-subtitle">{{ group.opportunityCode }}</span>
          </div>
          <div v-for="item in group.items" :key="item.id" class="fp-item" :class="{ 'fp-overdue': isOverdue(item.nextActionAt) }">
            <div class="fp-item-head">
              <a-tag :color="followUpTypeColor(item.type)" size="small">{{ followUpTypeLabel(item.type) }}</a-tag>
              <strong>{{ item.subject }}</strong>
              <a-tag v-if="isOverdue(item.nextActionAt)" color="red" size="small">已逾期</a-tag>
            </div>
            <p class="fp-item-content">{{ item.content }}</p>
            <div class="fp-item-meta">
              <span>跟进人 {{ item.ownerName }} · {{ formatDateTime(item.followedAt) }}</span>
              <span v-if="item.nextAction" class="fp-next-action">
                下一步：{{ item.nextAction }}
                <span v-if="item.nextActionAt" class="table-subtitle">（{{ item.nextActionAt }}）</span>
              </span>
            </div>
          </div>
        </div>
      </template>

      <template v-else>
        <a-table :columns="columns" :data-source="filteredItems" :loading="loading" :row-key="(record: FollowUp) => record.id" :scroll="{ x: 1180 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'customer'">
              <strong>{{ record.customerName }}</strong>
              <span class="table-subtitle">{{ record.opportunityCode || "客户日常跟进" }}</span>
            </template>
            <template v-else-if="column.key === 'type'"><a-tag :color="followUpTypeColor(record.type)">{{ followUpTypeLabel(record.type) }}</a-tag></template>
            <template v-else-if="column.key === 'content'">
              <strong>{{ record.subject }}</strong>
              <span class="table-subtitle line-clamp-2">{{ record.content }}</span>
            </template>
            <template v-else-if="column.key === 'followedAt'">{{ formatDateTime(record.followedAt) }}</template>
            <template v-else-if="column.key === 'nextAction'">
              <span :class="{ 'text-danger': isOverdue(record.nextActionAt) }">{{ record.nextAction || "-" }}</span>
              <span class="table-subtitle">{{ record.nextActionAt || "" }}</span>
            </template>
            <template v-else-if="column.key === 'action'">
              <span @click.stop>
                <a-popconfirm v-if="auth.can('crm:followup:delete')" title="确实删除此跟进记录？" @confirm="handleDeleteFollowUp(record)">
                  <a-button size="small" type="link" danger>删除</a-button>
                </a-popconfirm>
              </span>
            </template>
          </template>
        </a-table>
      </template>
    </a-card>

    <a-modal v-model:open="createOpen" title="新增跟进回访" width="760px" :confirm-loading="saving" @ok="handleCreate">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="12"><a-form-item label="客户" name="customerId"><a-select v-model:value="form.customerId" show-search option-filter-prop="label" :options="customerOptions" @change="syncOpportunity" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="关联商机"><a-select v-model:value="form.opportunityId" allow-clear show-search option-filter-prop="label" :options="availableOpportunityOptions" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="跟进类型" name="type"><a-select v-model:value="form.type" :options="typeOptions" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="负责人" name="ownerName"><a-input v-model:value="form.ownerName" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="发生时间" name="followedAt"><a-input v-model:value="form.followedAt" type="datetime-local" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="主题" name="subject"><a-input v-model:value="form.subject" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="沟通内容" name="content"><a-textarea v-model:value="form.content" :rows="4" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="下一步动作"><a-input v-model:value="form.nextAction" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import { createFollowUp, deleteFollowUp, listCustomers, listFollowUps, listOpportunities, type FollowUp } from "@/api/crm";
import { useAuthStore } from "@/stores/auth";
import { followUpTypeColor, followUpTypeLabel, followUpTypeOptions } from "./crm-options";

const auth = useAuthStore();
const items = ref<FollowUp[]>([]);
const customers = ref<any[]>([]);
const opportunities = ref<any[]>([]);
const loading = ref(false);
const saving = ref(false);
const createOpen = ref(false);
const formRef = ref();
const activeTab = ref("pending");
const typeFilter = ref<string>();
const keyword = ref("");
const typeOptions = [...followUpTypeOptions];

const columns = [
  { title: "客户 / 商机", key: "customer", width: 200 },
  { title: "类型", key: "type", width: 80 },
  { title: "主题 / 内容", key: "content", width: 320 },
  { title: "跟进时间", key: "followedAt", width: 150 },
  { title: "下一步动作", key: "nextAction", width: 240 },
  { title: "负责人", dataIndex: "ownerName", width: 100 },
  { title: "操作", key: "action", width: 70 },
];
const form = reactive(initialForm());
const rules = {
  customerId: [{ required: true, message: "请选择客户" }],
  type: [{ required: true, message: "请选择跟进类型" }],
  subject: [{ required: true, message: "请输入主题" }],
  content: [{ required: true, message: "请输入沟通内容" }],
  followedAt: [{ required: true, message: "请选择发生时间" }],
  ownerName: [{ required: true, message: "请输入负责人" }],
};

const customerOptions = computed(() =>
  customers.value.map((c) => ({ label: `${c.name}（${c.code || ""}）`, value: c.id }))
);
const availableOpportunityOptions = computed(() =>
  opportunities.value
    .filter((o) => o.customerId === form.customerId && o.stage !== "WON" && o.stage !== "LOST")
    .map((o) => ({ label: `${o.code} ${o.needSummary?.slice(0, 20)}`, value: o.id }))
);
const filteredItems = computed(() => {
  const term = keyword.value.trim().toLowerCase();
  return items.value.filter((item) => {
    const t = typeFilter.value;
    if (t && item.type !== t) return false;
    const text = `${item.customerName} ${item.subject} ${item.content}`.toLowerCase();
    return !term || text.includes(term);
  });
});
const pendingItems = computed(() =>
  items.value.filter((i) => i.nextAction && i.nextActionAt)
);
const pendingGroups = computed(() => {
  const map = new Map<string, { customerName: string; opportunityCode: string; items: FollowUp[] }>();
  pendingItems.value.forEach((item) => {
    const key = item.customerId || item.customerName || "other";
    if (!map.has(key)) {
      map.set(key, {
        customerName: item.customerName,
        opportunityCode: item.opportunityCode || "",
        items: [],
      });
    }
    map.get(key)!.items.push(item);
  });
  // Sort by overdue first
  map.forEach((g) => {
    g.items.sort((a, b) => {
      const aOverdue = isOverdue(a.nextActionAt) ? 0 : 1;
      const bOverdue = isOverdue(b.nextActionAt) ? 0 : 1;
      return aOverdue - bOverdue;
    });
  });
  return Array.from(map.values());
});
const pendingCount = computed(() => pendingItems.value.length);

function isOverdue(dateStr?: string) {
  if (!dateStr) return false;
  return dateStr < new Date().toISOString().slice(0, 10);
}

function syncOpportunity(customerId: string) {
  form.opportunityId = undefined;
}

function initialForm() {
  return {
    customerId: undefined as string | undefined,
    opportunityId: undefined as string | undefined,
    type: "VISIT" as string,
    subject: "",
    content: "",
    followedAt: new Date().toISOString().slice(0, 16),
    ownerName: auth.user?.displayName || "",
    nextAction: "",
  };
}

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    [items.value, customers.value, opportunities.value] = await Promise.all([
      listFollowUps(), listCustomers(), listOpportunities(),
    ]);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "数据加载失败");
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  Object.assign(form, initialForm());
  createOpen.value = true;
}

async function handleCreate() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    await createFollowUp({ ...form, followedAt: form.followedAt || new Date().toISOString() });
    createOpen.value = false;
    message.success("跟进记录已创建");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "创建失败");
  } finally {
    saving.value = false;
  }
}

async function handleDeleteFollowUp(record: FollowUp) {
  try {
    await deleteFollowUp(record.id);
    message.success("跟进记录已删除");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "删除失败");
  }
}

function formatDateTime(val?: string) {
  if (!val) return "";
  return new Date(val).toLocaleString("zh-CN", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" });
}
</script>

<style scoped>
.fp-group { margin-bottom: 16px; }
.fp-group-header { padding: 6px 0 4px; border-bottom: 1px solid #f0f0f0; margin-bottom: 8px; display: flex; gap: 8px; align-items: baseline; }
.fp-item { padding: 10px 12px; border: 1px solid #f0f0f0; border-radius: 6px; margin-bottom: 8px; transition: border-color 0.2s; }
.fp-item:hover { border-color: #91d5ff; }
.fp-item.fp-overdue { border-color: #ffa39e; background: #fff1f0; }
.fp-item-head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.fp-item-content { margin: 6px 0 0; font-size: 13px; color: #595959; white-space: pre-wrap; }
.fp-item-meta { margin-top: 6px; font-size: 12px; color: #8c8c8c; display: flex; gap: 16px; flex-wrap: wrap; }
.fp-next-action { color: #1890ff; }
</style>
