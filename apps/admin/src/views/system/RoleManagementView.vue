<template>
  <div class="page-stack role-page">
    <header class="page-heading">
      <div>
        <h2>角色与授权</h2>
        <p>角色同时决定可执行的操作和可查看的数据范围。</p>
      </div>
      <a-space>
        <a-button @click="reloadAll"><template #icon><ReloadOutlined /></template>刷新</a-button>
        <a-button v-if="auth.can('system:role:create')" type="primary" @click="openCreateModal">
          <template #icon><PlusOutlined /></template>新增角色
        </a-button>
      </a-space>
    </header>

    <section class="metric-band">
      <div><span>角色总数</span><strong>{{ pagination.total }}</strong></div>
      <div><span>已分配账号</span><strong>{{ assignedUserCount }}</strong></div>
      <div><span>全公司权限</span><strong>{{ allScopeCount }}</strong></div>
      <div><span>权限点</span><strong>{{ permissions.length }}</strong></div>
    </section>

    <section class="role-content">
      <div class="table-toolbar">
        <a-input v-model:value="keyword" allow-clear placeholder="搜索角色名称或代码" />
        <a-select v-model:value="scopeFilter" :options="scopeFilterOptions" />
      </div>

      <a-table
        :columns="columns"
        :data-source="filteredRoles"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :scroll="{ x: 1080 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'role'">
            <div class="role-name-line">
              <strong>{{ record.name }}</strong>
              <a-tag v-if="record.builtIn">内置</a-tag>
            </div>
            <span class="table-subtitle">{{ record.code }}</span>
          </template>
          <template v-else-if="column.key === 'dataScope'">
            <a-tag :color="getDataScopeColor(record.dataScope)">{{ getDataScopeName(record.dataScope) }}</a-tag>
            <span v-if="record.dataScope === 'CUSTOM'" class="table-subtitle">
              {{ record.dataOrganizations.map((item: RoleResponse['dataOrganizations'][number]) => item.name).join('、') }}
            </span>
          </template>
          <template v-else-if="column.key === 'userCount'"><strong>{{ record.userCount }}</strong> 个账号</template>
          <template v-else-if="column.key === 'permissions'">
            <strong>{{ record.permissions.length }}</strong> 项权限
            <span class="table-subtitle">{{ permissionModuleSummary(record) }}</span>
          </template>
          <template v-else-if="column.key === 'updatedAt'">{{ formatDateTime(record.updatedAt) }}</template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button v-if="auth.can('system:role:update')" size="small" type="link" @click="openEditModal(record)">编辑授权</a-button>
              <a-popconfirm
                v-if="auth.can('system:role:delete') && !record.builtIn"
                :title="record.userCount ? `该角色仍分配给 ${record.userCount} 个账号` : '确定删除该角色？'"
                ok-text="确定"
                cancel-text="取消"
                :disabled="record.userCount > 0"
                @confirm="handleDelete(record.id)"
              >
                <a-button size="small" type="link" danger :disabled="record.userCount > 0">删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </section>

    <a-modal
      v-model:open="modalVisible"
      :title="isEdit ? `编辑角色 · ${formState.name}` : '新增角色'"
      width="920px"
      :confirm-loading="modalLoading"
      @ok="handleModalOk"
      @cancel="closeModal"
    >
      <a-alert
        v-if="editingRole?.builtIn"
        class="section-alert"
        type="warning"
        show-icon
        message="正在修改系统内置角色，保存后该角色下所有账号的权限会立即变化。"
      />
      <a-form ref="formRef" :model="formState" :rules="formRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="10">
            <a-form-item label="角色代码" name="code"><a-input v-model:value="formState.code" :disabled="isEdit" placeholder="例如：PROJECT_MANAGER" /></a-form-item>
          </a-col>
          <a-col :xs="24" :md="14">
            <a-form-item label="角色名称" name="name"><a-input v-model:value="formState.name" placeholder="例如：项目经理" /></a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="数据范围" name="dataScope">
          <a-radio-group v-model:value="formState.dataScope" button-style="solid">
            <a-radio-button v-for="option in dataScopeOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <p class="scope-help">{{ dataScopeDescription }}</p>

        <a-form-item v-if="formState.dataScope === 'CUSTOM'" label="指定组织" required>
          <a-tree-select
            v-model:value="formState.dataOrganizationIds"
            :tree-data="organizations"
            multiple
            tree-default-expand-all
            placeholder="选择可以查看的组织"
            :field-names="{ label: 'name', value: 'id', children: 'children' }"
          />
        </a-form-item>

        <div class="permission-heading">
          <div>
            <strong>操作权限</strong>
            <span>已选择 {{ formState.permissionIds.length }} / {{ permissions.length }} 项</span>
          </div>
          <a-space>
            <a-button size="small" type="link" @click="selectAllPermissions">全部选择</a-button>
            <a-button size="small" type="link" @click="formState.permissionIds = []">全部清空</a-button>
          </a-space>
        </div>

        <div class="permission-matrix">
          <section v-for="group in permissionGroups" :key="group.module" class="permission-group">
            <div class="permission-group-heading">
              <div>
                <strong>{{ moduleLabel(group.module) }}</strong>
                <span>{{ selectedModuleCount(group.module) }} / {{ group.permissions.length }}</span>
              </div>
              <a-button size="small" type="link" @click="toggleModule(group.module)">
                {{ selectedModuleCount(group.module) === group.permissions.length ? "清空" : "全选" }}
              </a-button>
            </div>
            <a-checkbox-group v-model:value="formState.permissionIds" class="permission-options">
              <a-checkbox v-for="permission in group.permissions" :key="permission.id" :value="permission.id">
                <span>{{ permission.name }}</span>
                <small>{{ actionLabel(permission.code) }}</small>
              </a-checkbox>
            </a-checkbox-group>
          </section>
        </div>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message, type FormInstance, type TableProps } from "ant-design-vue";
import { PlusOutlined, ReloadOutlined } from "@ant-design/icons-vue";
import {
  createRoleApi,
  deleteRoleApi,
  listOrganizationsApi,
  listPermissionsApi,
  listRolesApi,
  updateRoleApi,
  type OrganizationResponse,
  type PermissionResponse,
  type RoleResponse,
} from "@/api/system";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
const roles = ref<RoleResponse[]>([]);
const permissions = ref<PermissionResponse[]>([]);
const organizations = ref<OrganizationResponse[]>([]);
const loading = ref(false);
const modalVisible = ref(false);
const modalLoading = ref(false);
const isEdit = ref(false);
const editingRole = ref<RoleResponse>();
const editId = ref("");
const formRef = ref<FormInstance>();
const keyword = ref("");
const scopeFilter = ref("ALL_SCOPES");
const formState = reactive(initialForm());
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
});
const columns = [
  { title: "角色", key: "role", width: 220 },
  { title: "数据范围", key: "dataScope", width: 240 },
  { title: "使用账号", key: "userCount", width: 130 },
  { title: "操作权限", key: "permissions", width: 260 },
  { title: "更新时间", key: "updatedAt", width: 170 },
  { title: "操作", key: "action", width: 160, fixed: "right" },
];
const dataScopeOptions = [
  { label: "仅本人", value: "SELF" },
  { label: "本部门", value: "DEPT" },
  { label: "本部门及下级", value: "DEPT_AND_SUB" },
  { label: "指定组织", value: "CUSTOM" },
  { label: "全公司", value: "ALL" },
];
const scopeFilterOptions = [{ label: "全部数据范围", value: "ALL_SCOPES" }, ...dataScopeOptions];
const formRules = {
  code: [{ required: true, message: "请输入角色代码" }],
  name: [{ required: true, message: "请输入角色名称" }],
  dataScope: [{ required: true, message: "请选择数据范围" }],
};
const dataScopeMap: Record<string, string> = {
  SELF: "仅本人",
  DEPT: "本部门",
  DEPARTMENT: "本部门",
  DEPT_AND_SUB: "本部门及下级",
  CUSTOM: "指定组织",
  ALL: "全公司",
};
const dataScopeColorMap: Record<string, string> = { SELF: "green", DEPT: "blue", DEPARTMENT: "blue", DEPT_AND_SUB: "orange", CUSTOM: "purple", ALL: "red" };
const dataScopeDescriptions: Record<string, string> = {
  SELF: "只能查看本人负责或分配给本人的业务数据。",
  DEPT: "可以查看本人所在组织的成员数据，不包含下级组织。",
  DEPT_AND_SUB: "可以查看本人所在组织及所有下级组织的成员数据。",
  CUSTOM: "可以查看手工指定组织的成员数据，适用于跨部门协同角色。",
  ALL: "可以查看全公司的业务数据，请谨慎授予。",
};
const moduleLabels: Record<string, string> = {
  system: "系统设置", dashboard: "经营驾驶舱", crm: "CRM", procurement: "供应链采购",
  project: "项目管理", inventory: "库存管理", maintenance: "服务管理", workforce: "人事管理",
  qualification: "资质管理", office: "OA协同", finance: "财务资金", bi: "经营分析",
};

const filteredRoles = computed(() => {
  const normalized = keyword.value.trim().toLowerCase();
  return roles.value.filter((role) => {
    const scopeMatches = scopeFilter.value === "ALL_SCOPES" || role.dataScope === scopeFilter.value;
    const keywordMatches = !normalized || role.name.toLowerCase().includes(normalized) || role.code.toLowerCase().includes(normalized);
    return scopeMatches && keywordMatches;
  });
});
const permissionGroups = computed(() => {
  const grouped = new Map<string, PermissionResponse[]>();
  permissions.value.forEach((permission) => {
    if (!grouped.has(permission.module)) grouped.set(permission.module, []);
    grouped.get(permission.module)?.push(permission);
  });
  return Array.from(grouped.entries())
    .map(([module, items]) => ({ module, permissions: items.sort((a, b) => a.code.localeCompare(b.code)) }))
    .sort((a, b) => moduleLabel(a.module).localeCompare(moduleLabel(b.module), "zh-CN"));
});
const assignedUserCount = computed(() => roles.value.reduce((sum, role) => sum + Number(role.userCount || 0), 0));
const allScopeCount = computed(() => roles.value.filter((role) => role.dataScope === "ALL").length);
const dataScopeDescription = computed(() => dataScopeDescriptions[formState.dataScope] || "");

onMounted(reloadAll);

async function reloadAll() {
  await Promise.all([loadData(), loadPermissions(), loadOrganizations()]);
}

async function loadData() {
  loading.value = true;
  try {
    const result = await listRolesApi(pagination.current - 1, pagination.pageSize);
    roles.value = result.content;
    pagination.total = result.totalElements;
  } catch (error) {
    message.error(error instanceof Error ? error.message : "加载角色列表失败");
  } finally {
    loading.value = false;
  }
}

async function loadPermissions() {
  if (!auth.can("system:permission:view")) return;
  try {
    permissions.value = await listPermissionsApi();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "加载权限列表失败");
  }
}

async function loadOrganizations() {
  if (!auth.can("system:organization:view")) return;
  try {
    organizations.value = await listOrganizationsApi();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "加载组织列表失败");
  }
}

const handleTableChange: TableProps["onChange"] = (page) => {
  pagination.current = page.current || 1;
  pagination.pageSize = page.pageSize || 20;
  loadData();
};

function openCreateModal() {
  isEdit.value = false;
  editingRole.value = undefined;
  editId.value = "";
  Object.assign(formState, initialForm());
  modalVisible.value = true;
}

function openEditModal(record: RoleResponse) {
  isEdit.value = true;
  editingRole.value = record;
  editId.value = record.id;
  Object.assign(formState, {
    code: record.code,
    name: record.name,
    dataScope: record.dataScope === "DEPARTMENT" ? "DEPT" : record.dataScope,
    permissionIds: record.permissions.map((item) => item.id),
    dataOrganizationIds: record.dataOrganizations.map((item) => item.id),
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
    if (formState.dataScope === "CUSTOM" && !formState.dataOrganizationIds.length) {
      message.warning("自定义数据范围至少选择一个组织");
      return;
    }
    modalLoading.value = true;
    const payload = {
      name: formState.name,
      dataScope: formState.dataScope,
      permissionIds: formState.permissionIds,
      dataOrganizationIds: formState.dataScope === "CUSTOM" ? formState.dataOrganizationIds : [],
    };
    if (isEdit.value) {
      await updateRoleApi(editId.value, payload);
      message.success("角色授权已更新，相关账号下次请求即生效");
    } else {
      await createRoleApi({ ...payload, code: formState.code });
      message.success("角色已创建");
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
    await deleteRoleApi(id);
    message.success("角色已删除");
    await loadData();
  } catch (error) {
    if (error instanceof Error) message.error(error.message);
  }
}

function initialForm() {
  return { code: "", name: "", dataScope: "SELF", permissionIds: [] as string[], dataOrganizationIds: [] as string[] };
}

function selectAllPermissions() {
  formState.permissionIds = permissions.value.map((permission) => permission.id);
}

function selectedModuleCount(module: string) {
  const selected = new Set(formState.permissionIds);
  return permissions.value.filter((permission) => permission.module === module && selected.has(permission.id)).length;
}

function toggleModule(module: string) {
  const moduleIds = permissions.value.filter((permission) => permission.module === module).map((permission) => permission.id);
  const selected = new Set(formState.permissionIds);
  const allSelected = moduleIds.every((id) => selected.has(id));
  moduleIds.forEach((id) => allSelected ? selected.delete(id) : selected.add(id));
  formState.permissionIds = Array.from(selected);
}

function permissionModuleSummary(role: RoleResponse) {
  const modules = Array.from(new Set(role.permissions.map((permission) => moduleLabel(permission.module))));
  return modules.length ? modules.slice(0, 3).join("、") + (modules.length > 3 ? ` 等 ${modules.length} 个模块` : "") : "未配置权限";
}

function actionLabel(code: string) {
  const action = code.split(":").pop() || "";
  const labels: Record<string, string> = {
    view: "查看", create: "新增", update: "编辑", delete: "删除", approve: "审批", submit: "提交",
    execute: "执行", settle: "核销", invoice: "开票", apply: "申请", collect: "回款", generate: "生成",
    accept: "验收", assign: "派工", receive: "收货", issue: "领用", return: "归还", manage: "维护",
    process: "处理", upload: "上传", complete: "验收", convert: "转合同", "customer-result": "客户结果",
    "reset-password": "重置密码",
  };
  return labels[action] || action;
}

function moduleLabel(module: string) {
  return moduleLabels[module] || module;
}

function getDataScopeName(scope: string) {
  return dataScopeMap[scope] || scope;
}

function getDataScopeColor(scope: string) {
  return dataScopeColorMap[scope] || "default";
}

function formatDateTime(value?: string) {
  return value ? new Date(value).toLocaleString("zh-CN") : "-";
}
</script>

<style scoped>
.page-heading,
.permission-heading,
.permission-group-heading {
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

.metric-band span,
.permission-heading span,
.permission-group-heading span {
  color: #667085;
  font-size: 13px;
}

.metric-band strong {
  color: #101828;
  font-size: 24px;
}

.role-content {
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
  width: min(340px, 100%);
}

.table-toolbar .ant-select {
  width: 190px;
}

.role-name-line {
  display: flex;
  align-items: center;
  gap: 7px;
}

.scope-help {
  margin: -14px 0 18px;
  color: #667085;
  font-size: 13px;
}

.permission-heading {
  margin: 8px 0 12px;
  padding-top: 18px;
  border-top: 1px solid #eaecf0;
}

.permission-heading > div,
.permission-group-heading > div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.permission-matrix {
  max-height: 430px;
  overflow: auto;
  border: 1px solid #e4e7ec;
}

.permission-group {
  display: grid;
  grid-template-columns: 170px minmax(0, 1fr);
  border-bottom: 1px solid #eaecf0;
}

.permission-group:last-child {
  border-bottom: 0;
}

.permission-group-heading {
  align-items: flex-start;
  padding: 14px;
  background: #f9fafb;
}

.permission-options {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px 14px;
  padding: 16px;
}

.permission-options :deep(.ant-checkbox-wrapper) {
  display: flex;
  align-items: flex-start;
  min-width: 0;
  margin-inline-start: 0;
}

.permission-options span,
.permission-options small {
  display: block;
}

.permission-options small {
  color: #98a2b3;
  font-size: 11px;
}

@media (max-width: 820px) {
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

  .permission-group {
    grid-template-columns: 1fr;
  }

  .permission-options {
    grid-template-columns: repeat(2, minmax(0, 1fr));
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

  .permission-options {
    grid-template-columns: 1fr;
  }
}
</style>
