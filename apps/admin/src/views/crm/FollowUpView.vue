<template>
  <div class="page-stack">
    <a-card title="跟进回访">
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
          <template v-else-if="column.key === 'nextAction'">{{ record.nextAction || "-" }}</template>
          <template v-else-if="column.key === 'action'">
            <span @click.stop>
              <a-popconfirm
                v-if="auth.user?.roles.includes('ADMIN')"
                title="确实删除此跟进记录？"
                @confirm="handleDeleteFollowUp(record)"
              >
                <a-button size="small" type="link" danger>删除</a-button>
              </a-popconfirm>
            </span>
          </template>
        </template>
      </a-table>
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
import { useRoute, useRouter } from "vue-router";
import { createFollowUp, deleteFollowUp, listCustomers, listFollowUps, listOpportunities, type CustomerSummary, type FollowUp, type FollowUpType, type Opportunity } from "@/api/crm";
import { useAuthStore } from "@/stores/auth";
import { followUpTypeColor, followUpTypeLabel, followUpTypeOptions } from "./crm-options";

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const items = ref<FollowUp[]>([]);
const customers = ref<CustomerSummary[]>([]);
const opportunities = ref<Opportunity[]>([]);
const loading = ref(false);
const saving = ref(false);
const createOpen = ref(false);
const formRef = ref();
const keyword = ref("");
const typeFilter = ref<FollowUpType>();
const typeOptions = [...followUpTypeOptions];
const form = reactive(initialForm());
const rules = {
  customerId: [{ required: true, message: "请选择客户" }],
  type: [{ required: true, message: "请选择跟进类型" }],
  subject: [{ required: true, message: "请输入主题" }],
  content: [{ required: true, message: "请输入沟通内容" }],
  followedAt: [{ required: true, message: "请选择发生时间" }],
  ownerName: [{ required: true, message: "请输入负责人" }],
};
const columns = [
  { title: "客户 / 商机", key: "customer", width: 220 },
  { title: "类型", key: "type", width: 90 },
  { title: "主题 / 内容", key: "content", width: 360 },
  { title: "发生时间", key: "followedAt", width: 170 },
  { title: "下一步动作", key: "nextAction", width: 260 },
  { title: "负责人", dataIndex: "ownerName", width: 110 },
  { title: "操作", key: "action", width: 100 }, 
];
const customerOptions = computed(() => customers.value.map((item) => ({ label: `${item.name}（${item.code}）`, value: item.id })));
const availableOpportunityOptions = computed(() => opportunities.value
  .filter((item) => !form.customerId || item.customerId === form.customerId)
  .map((item) => ({ label: `${item.code} · ${item.needSummary}`, value: item.id })));
const filteredItems = computed(() => {
  const term = keyword.value.trim().toLowerCase();
  return items.value.filter((item) => {
    const text = `${item.customerName} ${item.subject} ${item.content}`.toLowerCase();
    return (!typeFilter.value || item.type === typeFilter.value) && (!term || text.includes(term));
  });
});

onMounted(async () => {
  await loadData();
  const customerId = typeof route.query.customer === "string" ? route.query.customer : undefined;
  if (route.query.create === "1" && customerId && customers.value.some(customer => customer.id === customerId)) {
    openCreate(customerId);
    const { customer: _customer, create: _create, ...query } = route.query;
    await router.replace({ path: route.path, query });
  }
});

async function loadData() {
  loading.value = true;
  try {
    [items.value, customers.value, opportunities.value] = await Promise.all([listFollowUps(), listCustomers(), listOpportunities()]);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "跟进记录加载失败");
  } finally {
    loading.value = false;
  }
}

function syncOpportunity() {
  if (!opportunities.value.some((item) => item.id === form.opportunityId && item.customerId === form.customerId)) {
    form.opportunityId = undefined;
  }
}

function openCreate(customerId?: string) {
  Object.assign(form, initialForm(), { customerId });
  syncOpportunity();
  createOpen.value = true;
}

async function handleDeleteFollowUp(record: any) {
  try {
    await deleteFollowUp(record.id);
    message.success("跟进记录已删除");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "删除失败");
  }
}

async function handleCreate() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    await createFollowUp({
      ...form,
      customerId: form.customerId!,
      followedAt: new Date(form.followedAt).toISOString(),
    });
    Object.assign(form, initialForm());
    createOpen.value = false;
    message.success("跟进记录已保存");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "跟进记录保存失败");
  } finally {
    saving.value = false;
  }
}

function initialForm() {
  const now = new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 16);
  return { customerId: undefined as string | undefined, opportunityId: undefined as string | undefined, type: "PHONE" as FollowUpType, subject: "", content: "", followedAt: now, nextAction: "", ownerName: auth.user?.displayName || "" };
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString("zh-CN", { hour12: false });
}
</script>
