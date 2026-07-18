<template>
  <div class="page-stack permission-page">
    <header class="page-heading">
      <div>
        <h2>权限目录</h2>
        <p>权限点定义系统中的查看、编辑、审批和执行能力，并由角色统一授权。</p>
      </div>
      <a-space>
        <a-button @click="loadData"
          ><template #icon><ReloadOutlined /></template>刷新</a-button
        >
        <a-button
          v-if="auth.can('system:permission:create')"
          type="primary"
          @click="openCreateModal"
        >
          <template #icon><PlusOutlined /></template>新增权限
        </a-button>
      </a-space>
    </header>

    <section class="metric-band">
      <div>
        <span>权限总数</span><strong>{{ permissions.length }}</strong>
      </div>
      <div>
        <span>业务模块</span><strong>{{ moduleOptions.length - 1 }}</strong>
      </div>
      <div>
        <span>已被角色使用</span><strong>{{ usedPermissionCount }}</strong>
      </div>
      <div>
        <span>自定义权限</span><strong>{{ customPermissionCount }}</strong>
      </div>
    </section>

    <section class="permission-content">
      <div class="table-toolbar">
        <a-input
          v-model:value="keyword"
          allow-clear
          placeholder="搜索权限名称或代码"
        />
        <a-select v-model:value="moduleFilter" :options="moduleOptions" />
      </div>

      <a-table
        :columns="columns"
        :data-source="filteredPermissions"
        :loading="loading"
        row-key="id"
        :pagination="{
          pageSize: 20,
          showSizeChanger: true,
          showTotal: (total: number) => `共 ${total} 条`,
        }"
        :scroll="{ x: 920 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'permission'">
            <div class="permission-name-line">
              <strong>{{ record.name }}</strong>
              <a-tag v-if="record.builtIn">内置</a-tag>
            </div>
            <span class="table-subtitle">{{ record.code }}</span>
          </template>
          <template v-else-if="column.key === 'module'">
            <a-tag color="blue">{{ moduleLabel(record.module) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'actionType'">
            <span>{{ actionLabel(record.code) }}</span>
          </template>
          <template v-else-if="column.key === 'roleCount'">
            <strong>{{ record.roleCount }}</strong> 个角色
          </template>
          <template v-else-if="column.key === 'createdAt'">{{
            formatDateTime(record.createdAt)
          }}</template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button
                v-if="auth.can('system:permission:update')"
                size="small"
                type="link"
                @click="openEditModal(record)"
                >编辑</a-button
              >
              <a-popconfirm
                v-if="auth.can('system:permission:delete') && !record.builtIn"
                :title="
                  record.roleCount
                    ? `该权限仍被 ${record.roleCount} 个角色使用`
                    : '确定删除该权限？'
                "
                ok-text="确定"
                cancel-text="取消"
                :disabled="record.roleCount > 0"
                @confirm="handleDelete(record.id)"
              >
                <a-button
                  size="small"
                  type="link"
                  danger
                  :disabled="record.roleCount > 0"
                  >删除</a-button
                >
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </section>

    <a-modal
      v-model:open="modalVisible"
      :title="isEdit ? '编辑权限' : '新增权限'"
      width="620px"
      :confirm-loading="modalLoading"
      @ok="handleModalOk"
      @cancel="closeModal"
    >
      <a-alert
        v-if="editingPermission?.builtIn"
        class="section-alert"
        type="info"
        show-icon
        message="系统内置权限只允许调整显示名称和所属模块，权限代码不会改变。"
      />
      <a-form
        ref="formRef"
        :model="formState"
        :rules="formRules"
        layout="vertical"
      >
        <a-form-item label="权限代码" name="code">
          <a-input
            v-model:value="formState.code"
            :disabled="isEdit"
            placeholder="例如：project:budget:approve"
          />
        </a-form-item>
        <a-form-item label="权限名称" name="name"
          ><a-input
            v-model:value="formState.name"
            placeholder="例如：项目预算审批"
        /></a-form-item>
        <a-form-item label="所属模块" name="module"
          ><a-input
            v-model:value="formState.module"
            placeholder="例如：project"
        /></a-form-item>
        <a-alert
          type="warning"
          show-icon
          message="新权限创建后不会自动授予任何角色，需要在角色管理中完成授权。"
        />
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message, type FormInstance } from "ant-design-vue";
import { PlusOutlined, ReloadOutlined } from "@ant-design/icons-vue";
import {
  createPermissionApi,
  deletePermissionApi,
  listPermissionsApi,
  updatePermissionApi,
  type PermissionResponse,
} from "@/api/system";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
const permissions = ref<PermissionResponse[]>([]);
const loading = ref(false);
const keyword = ref("");
const moduleFilter = ref("ALL");
const modalVisible = ref(false);
const modalLoading = ref(false);
const isEdit = ref(false);
const editId = ref("");
const editingPermission = ref<PermissionResponse>();
const formRef = ref<FormInstance>();
const formState = reactive({ code: "", name: "", module: "" });
const formRules = {
  code: [{ required: true, message: "请输入权限代码" }],
  name: [{ required: true, message: "请输入权限名称" }],
  module: [{ required: true, message: "请输入所属模块" }],
};
const columns = [
  { title: "权限", key: "permission", width: 350 },
  { title: "业务模块", key: "module", width: 160 },
  { title: "动作类型", key: "actionType", width: 130 },
  { title: "使用情况", key: "roleCount", width: 140 },
  { title: "创建时间", key: "createdAt", width: 180 },
  { title: "操作", key: "action", width: 140, fixed: "right" },
];
const moduleLabels: Record<string, string> = {
  system: "系统设置",
  dashboard: "经营驾驶舱",
  crm: "CRM",
  procurement: "供应链采购",
  project: "项目管理",
  inventory: "库存管理",
  workforce: "人事管理",
  qualification: "资质管理",
  office: "OA协同",
  finance: "财务资金",
  bi: "经营分析",
};
const actionLabels: Record<string, string> = {
  view: "查看",
  create: "新增",
  update: "编辑",
  delete: "删除",
  approve: "审批",
  submit: "提交",
  execute: "执行",
  settle: "核销",
  invoice: "开票",
  receive: "收货",
  issue: "领用",
  return: "归还",
  close: "关闭",
  assign: "派工",
  complete: "验收",
  publish: "发布",
  reset: "重置",
  apply: "申请",
  collect: "回款",
  generate: "生成",
  accept: "验收",
  manage: "维护",
  process: "处理",
  upload: "上传",
  convert: "转合同",
  "customer-result": "客户结果",
  "reset-password": "重置密码",
};

const moduleOptions = computed(() => [
  { label: "全部模块", value: "ALL" },
  ...Array.from(new Set(permissions.value.map((item) => item.module)))
    .sort((a, b) => moduleLabel(a).localeCompare(moduleLabel(b), "zh-CN"))
    .map((module) => ({ label: moduleLabel(module), value: module })),
]);
const filteredPermissions = computed(() => {
  const normalized = keyword.value.trim().toLowerCase();
  return permissions.value.filter((permission) => {
    const moduleMatches =
      moduleFilter.value === "ALL" || permission.module === moduleFilter.value;
    const keywordMatches =
      !normalized ||
      permission.name.toLowerCase().includes(normalized) ||
      permission.code.toLowerCase().includes(normalized);
    return moduleMatches && keywordMatches;
  });
});
const usedPermissionCount = computed(
  () => permissions.value.filter((item) => item.roleCount > 0).length,
);
const customPermissionCount = computed(
  () => permissions.value.filter((item) => !item.builtIn).length,
);

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    permissions.value = await listPermissionsApi();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "加载权限目录失败");
  } finally {
    loading.value = false;
  }
}

function openCreateModal() {
  isEdit.value = false;
  editId.value = "";
  editingPermission.value = undefined;
  Object.assign(formState, { code: "", name: "", module: "" });
  modalVisible.value = true;
}

function openEditModal(record: PermissionResponse) {
  isEdit.value = true;
  editId.value = record.id;
  editingPermission.value = record;
  Object.assign(formState, {
    code: record.code,
    name: record.name,
    module: record.module,
  });
  modalVisible.value = true;
}

function closeModal() {
  modalVisible.value = false;
  formRef.value?.resetFields();
}

async function handleModalOk() {
  try {
    await formRef.value?.validate();
    modalLoading.value = true;
    if (isEdit.value) {
      await updatePermissionApi(editId.value, {
        name: formState.name,
        module: formState.module,
      });
      message.success("权限信息已更新");
    } else {
      await createPermissionApi({ ...formState });
      message.success("权限已创建，请继续配置角色授权");
    }
    closeModal();
    await loadData();
  } catch (error) {
    if (error instanceof Error) message.error(error.message);
  } finally {
    modalLoading.value = false;
  }
}

async function handleDelete(id: string) {
  try {
    await deletePermissionApi(id);
    message.success("权限已删除");
    await loadData();
  } catch (error) {
    if (error instanceof Error) message.error(error.message);
  }
}

function moduleLabel(module: string) {
  return moduleLabels[module] || module;
}

function actionLabel(code: string) {
  const action = code.split(":").pop() || "";
  return actionLabels[action] || action;
}

function formatDateTime(value?: string) {
  return value ? new Date(value).toLocaleString("zh-CN") : "-";
}
</script>

<style scoped>
.page-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.page-heading h2 {
  margin: 0;
  color: #182230;
}

.page-heading p {
  margin: 5px 0 0;
  color: #667085;
}

.metric-band {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  border: 1px solid #e4e7ec;
  background: #fff;
}

.metric-band > div {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 18px 20px;
  border-right: 1px solid #e4e7ec;
}

.metric-band > div:last-child {
  border-right: 0;
}

.metric-band span {
  color: #667085;
  font-size: 13px;
}

.metric-band strong {
  color: #101828;
  font-size: 24px;
}

.permission-content {
  padding: 20px;
  border: 1px solid #e4e7ec;
  background: #fff;
}

.table-toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.table-toolbar .ant-input-affix-wrapper {
  width: min(360px, 100%);
}

.table-toolbar .ant-select {
  width: 190px;
}

.permission-name-line {
  display: flex;
  align-items: center;
  gap: 7px;
}

@media (max-width: 760px) {
  .page-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .metric-band {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .metric-band > div:nth-child(2) {
    border-right: 0;
  }
}

@media (max-width: 560px) {
  .table-toolbar {
    flex-direction: column;
  }

  .table-toolbar .ant-input-affix-wrapper,
  .table-toolbar .ant-select {
    width: 100%;
  }
}
</style>
