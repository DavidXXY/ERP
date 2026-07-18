<template>
  <div class="page-stack user-page">
    <header class="page-heading">
      <div>
        <h2>账号管理</h2>
        <p>维护系统登录账号、所属组织、启停状态和角色授权。</p>
      </div>
      <a-space>
        <a-button @click="loadData">
          <template #icon><ReloadOutlined /></template>刷新
        </a-button>
        <a-button
          v-if="auth.can('system:user:create')"
          type="primary"
          @click="openCreateModal"
        >
          <template #icon><PlusOutlined /></template>新增账号
        </a-button>
      </a-space>
    </header>

    <section class="metric-band">
      <div>
        <span>账号总数</span><strong>{{ pagination.total }}</strong>
      </div>
      <div>
        <span>启用账号</span><strong>{{ enabledUserCount }}</strong>
      </div>
      <div>
        <span>角色模板</span><strong>{{ roles.length }}</strong>
      </div>
      <div>
        <span>组织节点</span><strong>{{ organizations.length }}</strong>
      </div>
    </section>

    <section class="user-content">
      <a-space class="table-toolbar" wrap>
        <a-input-search
          v-model:value="keyword"
          allow-clear
          placeholder="搜索姓名、账号、手机号或邮箱"
          style="width: 320px"
        />
        <a-select
          v-model:value="statusFilter"
          :options="statusOptions"
          style="width: 140px"
        />
        <a-select
          v-model:value="roleFilter"
          :options="roleFilterOptions"
          style="width: 180px"
          show-search
          option-filter-prop="label"
        />
      </a-space>

      <a-table
        :columns="columns"
        :data-source="filteredUsers"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :scroll="{ x: 1120 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'account'">
            <strong>{{ record.displayName }}</strong>
            <span class="table-subtitle">{{ record.username }}</span>
          </template>
          <template v-else-if="column.key === 'contact'">
            <span>{{ record.phone || "-" }}</span>
            <span class="table-subtitle">{{ record.email || "-" }}</span>
          </template>
          <template v-else-if="column.key === 'organization'">
            {{ organizationName(record.orgId) }}
          </template>
          <template v-else-if="column.key === 'roles'">
            <a-space wrap size="small">
              <a-tag v-for="role in record.roles" :key="role.id" color="blue">{{
                role.name
              }}</a-tag>
              <span v-if="!record.roles.length" class="table-subtitle"
                >未分配</span
              >
            </a-space>
          </template>
          <template v-else-if="column.key === 'enabled'">
            <a-tag :color="record.enabled ? 'green' : 'default'">{{
              record.enabled ? "启用" : "停用"
            }}</a-tag>
          </template>
          <template v-else-if="column.key === 'updatedAt'">{{
            formatDateTime(record.updatedAt)
          }}</template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button
                v-if="auth.can('system:user:update')"
                size="small"
                type="link"
                @click="openEditModal(record)"
                >编辑</a-button
              >
              <a-button
                v-if="auth.can('system:user:update')"
                size="small"
                type="link"
                @click="toggleEnabled(record)"
              >
                {{ record.enabled ? "停用" : "启用" }}
              </a-button>
              <a-popconfirm
                v-if="auth.can('system:user:reset-password')"
                title="确认将该账号密码重置为 Admin@123？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="handleResetPassword(record)"
              >
                <a-button size="small" type="link">重置密码</a-button>
              </a-popconfirm>
              <a-popconfirm
                v-if="auth.can('system:user:delete')"
                title="确定删除该账号？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="handleDelete(record.id)"
              >
                <a-button size="small" type="link" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </section>

    <a-modal
      v-model:open="modalVisible"
      :title="isEdit ? `编辑账号 · ${formState.username}` : '新增账号'"
      width="760px"
      :confirm-loading="modalLoading"
      @ok="handleModalOk"
      @cancel="closeModal"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="formRules"
        layout="vertical"
      >
        <a-row :gutter="16">
          <a-col :xs="24" :md="12">
            <a-form-item label="登录账号" name="username">
              <a-input
                v-model:value="formState.username"
                :disabled="isEdit"
                placeholder="例如：zhangsan"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item v-if="!isEdit" label="初始密码" name="password">
              <a-input-password
                v-model:value="formState.password"
                placeholder="至少 6 位"
              />
            </a-form-item>
            <a-form-item v-else label="账号状态" name="enabled">
              <a-switch
                v-model:checked="formState.enabled"
                checked-children="启用"
                un-checked-children="停用"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :xs="24" :md="12">
            <a-form-item label="姓名" name="displayName"
              ><a-input v-model:value="formState.displayName"
            /></a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="所属组织">
              <a-select
                v-model:value="formState.orgId"
                allow-clear
                show-search
                option-filter-prop="label"
                :options="organizationOptions"
                placeholder="选择组织"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :xs="24" :md="12">
            <a-form-item label="手机号"
              ><a-input v-model:value="formState.phone"
            /></a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="邮箱"
              ><a-input v-model:value="formState.email"
            /></a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="角色">
          <a-select
            v-model:value="formState.roleIds"
            mode="multiple"
            allow-clear
            show-search
            option-filter-prop="label"
            :options="roleOptions"
            placeholder="选择角色"
          />
        </a-form-item>
        <a-alert
          type="info"
          show-icon
          message="账号权限由角色统一授权；角色的数据范围会影响该账号可见的数据。"
        />
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message, type FormInstance, type TableProps } from "ant-design-vue";
import { PlusOutlined, ReloadOutlined } from "@ant-design/icons-vue";
import {
  createUserApi,
  deleteUserApi,
  listOrganizationsFlatApi,
  listRolesApi,
  listUsersApi,
  resetPasswordApi,
  updateUserApi,
  type OrganizationResponse,
  type RoleResponse,
  type UserResponse,
} from "@/api/system";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
const users = ref<UserResponse[]>([]);
const roles = ref<RoleResponse[]>([]);
const organizations = ref<OrganizationResponse[]>([]);
const loading = ref(false);
const modalVisible = ref(false);
const modalLoading = ref(false);
const isEdit = ref(false);
const editId = ref("");
const keyword = ref("");
const statusFilter = ref("ALL");
const roleFilter = ref("ALL");
const formRef = ref<FormInstance>();
const formState = reactive(initialForm());
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
});
const columns = [
  { title: "账号", key: "account", width: 210 },
  { title: "联系方式", key: "contact", width: 210 },
  { title: "所属组织", key: "organization", width: 210 },
  { title: "角色", key: "roles", width: 260 },
  { title: "状态", key: "enabled", width: 90 },
  { title: "更新时间", key: "updatedAt", width: 170 },
  { title: "操作", key: "action", width: 250, fixed: "right" },
];
const formRules = {
  username: [{ required: true, message: "请输入登录账号" }],
  displayName: [{ required: true, message: "请输入姓名" }],
  password: [{ required: true, min: 6, message: "初始密码至少 6 位" }],
};
const statusOptions = [
  { label: "全部状态", value: "ALL" },
  { label: "启用", value: "ENABLED" },
  { label: "停用", value: "DISABLED" },
];

const enabledUserCount = computed(
  () => users.value.filter((item) => item.enabled).length,
);
const organizationOptions = computed(() =>
  organizations.value.map((item) => ({
    label: item.fullPath || item.name,
    value: item.id,
  })),
);
const roleOptions = computed(() =>
  roles.value.map((item) => ({
    label: `${item.name} · ${item.code}`,
    value: item.id,
  })),
);
const roleFilterOptions = computed(() => [
  { label: "全部角色", value: "ALL" },
  ...roleOptions.value,
]);
const filteredUsers = computed(() => {
  const normalized = keyword.value.trim().toLowerCase();
  return users.value.filter((user) => {
    const keywordMatches =
      !normalized ||
      user.username.toLowerCase().includes(normalized) ||
      user.displayName.toLowerCase().includes(normalized) ||
      (user.phone || "").toLowerCase().includes(normalized) ||
      (user.email || "").toLowerCase().includes(normalized);
    const statusMatches =
      statusFilter.value === "ALL" ||
      (statusFilter.value === "ENABLED" && user.enabled) ||
      (statusFilter.value === "DISABLED" && !user.enabled);
    const roleMatches =
      roleFilter.value === "ALL" ||
      user.roles.some((role) => role.id === roleFilter.value);
    return keywordMatches && statusMatches && roleMatches;
  });
});

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    const [userData, roleData, organizationData] = await Promise.all([
      listUsersApi(pagination.current - 1, pagination.pageSize),
      auth.can("system:role:view")
        ? listRolesApi(0, 200)
        : Promise.resolve({ content: [] as RoleResponse[] }),
      auth.can("system:organization:view")
        ? listOrganizationsFlatApi()
        : Promise.resolve([] as OrganizationResponse[]),
    ]);
    users.value = userData.content;
    pagination.total = userData.totalElements;
    roles.value = roleData.content;
    organizations.value = organizationData;
  } catch (error) {
    message.error(error instanceof Error ? error.message : "加载账号列表失败");
  } finally {
    loading.value = false;
  }
}

function initialForm() {
  return {
    username: "",
    password: "Admin@123",
    displayName: "",
    phone: "",
    email: "",
    enabled: true,
    orgId: undefined as string | undefined,
    roleIds: [] as string[],
  };
}

function openCreateModal() {
  isEdit.value = false;
  editId.value = "";
  Object.assign(formState, initialForm());
  modalVisible.value = true;
}

function openEditModal(record: UserResponse) {
  isEdit.value = true;
  editId.value = record.id;
  Object.assign(formState, {
    username: record.username,
    password: "",
    displayName: record.displayName,
    phone: record.phone || "",
    email: record.email || "",
    enabled: record.enabled,
    orgId: record.orgId,
    roleIds: record.roles.map((role) => role.id),
  });
  modalVisible.value = true;
}

function closeModal() {
  modalVisible.value = false;
  formRef.value?.clearValidate();
}

async function handleModalOk() {
  try {
    await formRef.value?.validate();
    modalLoading.value = true;
    if (isEdit.value) {
      await updateUserApi(editId.value, {
        orgId: formState.orgId,
        displayName: formState.displayName,
        phone: formState.phone || undefined,
        email: formState.email || undefined,
        enabled: formState.enabled,
        roleIds: formState.roleIds,
      });
      message.success("账号信息已更新");
    } else {
      await createUserApi({
        orgId: formState.orgId,
        username: formState.username,
        displayName: formState.displayName,
        password: formState.password,
        phone: formState.phone || undefined,
        email: formState.email || undefined,
        roleIds: formState.roleIds,
      });
      message.success("账号已创建");
    }
    closeModal();
    await loadData();
  } catch (error) {
    if (error instanceof Error) message.error(error.message);
  } finally {
    modalLoading.value = false;
  }
}

async function toggleEnabled(record: UserResponse) {
  try {
    await updateUserApi(record.id, {
      orgId: record.orgId,
      displayName: record.displayName,
      phone: record.phone,
      email: record.email,
      enabled: !record.enabled,
      roleIds: record.roles.map((role) => role.id),
    });
    message.success(record.enabled ? "账号已停用" : "账号已启用");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "账号状态更新失败");
  }
}

async function handleResetPassword(record: UserResponse) {
  try {
    await resetPasswordApi(record.id, "Admin@123");
    message.success("密码已重置为 Admin@123");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "重置密码失败");
  }
}

async function handleDelete(id: string) {
  try {
    await deleteUserApi(id);
    message.success("账号已删除");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "删除账号失败");
  }
}

function handleTableChange(
  nextPagination: Parameters<NonNullable<TableProps["onChange"]>>[0],
) {
  pagination.current = Number(nextPagination.current || 1);
  pagination.pageSize = Number(nextPagination.pageSize || 20);
  loadData();
}

function organizationName(orgId?: string) {
  if (!orgId) return "未绑定";
  return (
    organizations.value.find((item) => item.id === orgId)?.name || "未绑定"
  );
}

function formatDateTime(value?: string) {
  if (!value) return "-";
  return value.replace("T", " ").slice(0, 16);
}
</script>

<style scoped>
.page-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.page-heading h2 {
  margin: 0 0 4px;
}

.page-heading p {
  margin: 0;
  color: #64748b;
}

.metric-band {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  border: 1px solid #e8edf3;
  border-radius: 8px;
  background: #fff;
}

.metric-band > div {
  display: grid;
  gap: 4px;
  padding: 14px 16px;
  border-right: 1px solid #eef2f6;
}

.metric-band > div:last-child {
  border-right: 0;
}

.metric-band span {
  color: #64748b;
  font-size: 12px;
}

.metric-band strong {
  color: #17212b;
  font-size: 24px;
}

.user-content {
  padding: 16px;
  border: 1px solid #e8edf3;
  border-radius: 8px;
  background: #fff;
}

@media (max-width: 760px) {
  .page-heading {
    flex-direction: column;
  }

  .metric-band {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .metric-band > div:nth-child(2) {
    border-right: 0;
  }
}
</style>
