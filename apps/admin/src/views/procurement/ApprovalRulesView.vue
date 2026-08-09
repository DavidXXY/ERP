<template>
  <div class="page-stack">
    <a-card>
      <template #title>分级审批规则</template>
      <template #extra>
        <a-space>
          <a-button @click="router.push('/procurement')">返回采购管理</a-button>
          <a-button :loading="loading" @click="loadData">
            <template #icon><ReloadOutlined /></template>刷新
          </a-button>
        </a-space>
      </template>

      <a-space class="table-toolbar">
        <a-button
          v-if="auth.can('procurement:request:approve')"
          type="primary"
          @click="openCreate"
        >
          <template #icon><PlusOutlined /></template>新增规则
        </a-button>
      </a-space>

      <a-alert
        class="section-alert"
        type="info"
        show-icon
        message="按采购申请金额区间自动路由审批级别：提交申请时自动匹配金额区间并记录审批级别，审批时校验当前操作人角色是否符合该级别要求。金额区间为左闭右开，不配置的金额不强制级别。"
      />

      <a-table
        :columns="columns"
        :data-source="rules"
        :loading="loading"
        :pagination="false"
        row-key="id"
        :scroll="{ x: 1100 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amount'">
            {{ formatAmount(record.minAmount) }} ~
            {{ record.maxAmount != null ? formatAmount(record.maxAmount) : "不限" }}
          </template>
          <template v-else-if="column.key === 'level'">
            <a-tag>{{ levelLabel(record.approvalLevel) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'role'">
            {{ record.requiredRoleCode || "不限角色" }}
          </template>
          <template v-else-if="column.key === 'enabled'">
            <a-tag :color="record.enabled ? 'green' : 'default'">{{
              record.enabled ? "启用" : "停用"
            }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a-button
                v-if="auth.can('procurement:request:approve')"
                type="link"
                size="small"
                @click="openEdit(record)"
                >编辑</a-button
              >
              <a-popconfirm
                v-if="auth.can('procurement:request:approve')"
                title="确认删除该规则？"
                @confirm="handleDelete(record)"
              >
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="formOpen"
      :title="editingId ? '编辑审批规则' : '新增审批规则'"
      width="640px"
      :confirm-loading="saving"
      @ok="handleSave"
    >
      <a-form layout="vertical">
        <a-form-item label="规则名称" required>
          <a-input v-model:value="form.ruleName" placeholder="如：大额采购-总经理审批" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :xs="24" :md="12">
            <a-form-item label="金额下限（元）">
              <a-input-number v-model:value="form.minAmount" :min="0" :precision="2" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="金额上限（元，不含）">
              <a-input-number v-model:value="form.maxAmount" :min="0" :precision="2" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="审批级别" required>
              <a-select
                v-model:value="form.approvalLevel"
                :options="[
                  { label: '部门级', value: 'DEPARTMENT' },
                  { label: '经理级', value: 'MANAGER' },
                  { label: '总经理级', value: 'EXECUTIVE' },
                  { label: '自定义', value: 'CUSTOM' },
                ]"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="审批角色">
              <a-select
                v-model:value="form.requiredRoleCode"
                :options="roleOptions"
                allow-clear
                placeholder="不限角色"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="排序">
              <a-input-number v-model:value="form.sortOrder" :min="1" :precision="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="启用">
              <a-switch v-model:checked="form.enabled" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { message } from "ant-design-vue";
import PlusOutlined from "@ant-design/icons-vue/PlusOutlined";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { useAuthStore } from "@/stores/auth";
import {
  createApprovalRule,
  deleteApprovalRule,
  listApprovalRules,
  updateApprovalRule,
  type ApprovalRule,
} from "@/api/procurement";

const auth = useAuthStore();
const router = useRouter();
const loading = ref(false);
const saving = ref(false);
const rules = ref<ApprovalRule[]>([]);
const formOpen = ref(false);
const editingId = ref<string | null>(null);
const roleOptions = [
  { label: "采购专员", value: "PROCUREMENT_SPECIALIST" },
  { label: "采购经理", value: "PROCUREMENT_MANAGER" },
  { label: "总经理", value: "EXECUTIVE_MANAGER" },
  { label: "财务经理", value: "FINANCE_MANAGER" },
  { label: "系统管理员", value: "ADMIN" },
];

const columns = [
  { title: "排序", dataIndex: "sortOrder", width: 70 },
  { title: "规则名称", dataIndex: "ruleName", width: 240 },
  { title: "金额区间（元）", key: "amount", width: 200 },
  { title: "审批级别", key: "level", width: 110 },
  { title: "审批角色", key: "role", width: 170 },
  { title: "状态", key: "enabled", width: 80 },
  { title: "操作", key: "action", width: 140 },
];

const form = reactive<{
  ruleName: string;
  minAmount?: number;
  maxAmount?: number;
  approvalLevel: string;
  requiredRoleCode?: string;
  enabled: boolean;
  sortOrder: number;
}>({
  ruleName: "",
  minAmount: 0,
  maxAmount: undefined,
  approvalLevel: "DEPARTMENT",
  requiredRoleCode: undefined,
  enabled: true,
  sortOrder: 1,
});

function formatAmount(v?: number) {
  return v == null ? "0" : new Intl.NumberFormat("zh-CN").format(v);
}
function levelLabel(level: string) {
  return (
    {
      DEPARTMENT: "部门级",
      MANAGER: "经理级",
      EXECUTIVE: "总经理级",
      CUSTOM: "自定义",
    } as Record<string, string>
  )[level] || level;
}
function openCreate() {
  editingId.value = null;
  Object.assign(form, {
    ruleName: "",
    minAmount: 0,
    maxAmount: undefined,
    approvalLevel: "DEPARTMENT",
    requiredRoleCode: undefined,
    enabled: true,
    sortOrder: (rules.value.length || 0) + 1,
  });
  formOpen.value = true;
}
function openEdit(record: ApprovalRule) {
  editingId.value = record.id;
  Object.assign(form, {
    ruleName: record.ruleName,
    minAmount: record.minAmount,
    maxAmount: record.maxAmount,
    approvalLevel: record.approvalLevel,
    requiredRoleCode: record.requiredRoleCode,
    enabled: record.enabled,
    sortOrder: record.sortOrder,
  });
  formOpen.value = true;
}
async function handleSave() {
  if (!form.ruleName.trim()) {
    message.warning("请填写规则名称");
    return;
  }
  if (!form.approvalLevel) {
    message.warning("请选择审批级别");
    return;
  }
  if (
    form.minAmount != null &&
    form.maxAmount != null &&
    form.maxAmount <= form.minAmount
  ) {
    message.warning("金额上限必须大于下限");
    return;
  }
  saving.value = true;
  try {
    const payload = {
      ruleName: form.ruleName.trim(),
      minAmount: form.minAmount ?? undefined,
      maxAmount: form.maxAmount ?? undefined,
      approvalLevel: form.approvalLevel,
      requiredRoleCode: form.requiredRoleCode || undefined,
      enabled: form.enabled,
      sortOrder: form.sortOrder,
    };
    if (editingId.value) {
      await updateApprovalRule(editingId.value, payload);
    } else {
      await createApprovalRule(payload);
    }
    formOpen.value = false;
    message.success(editingId.value ? "规则已更新" : "规则已创建");
    await loadData();
  } catch (e) {
    message.error(e instanceof Error ? e.message : "保存失败");
  } finally {
    saving.value = false;
  }
}
async function handleDelete(record: ApprovalRule) {
  try {
    await deleteApprovalRule(record.id);
    message.success("规则已删除");
    await loadData();
  } catch (e) {
    message.error(e instanceof Error ? e.message : "删除失败");
  }
}
async function loadData() {
  loading.value = true;
  try {
    rules.value = await listApprovalRules();
  } catch (e) {
    message.error(e instanceof Error ? e.message : "审批规则加载失败");
  } finally {
    loading.value = false;
  }
}
onMounted(loadData);
</script>
