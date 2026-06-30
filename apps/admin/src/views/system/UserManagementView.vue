<template>
  <div class="page-container" :class="{ embedded: props.embedded }">
    <a-card title="登录账号" class="table-card">
      <template #extra>
        <a-button v-if="auth.can('system:user:create')" type="primary" @click="openCreateModal">
          新增账号
        </a-button>
      </template>

      <a-table
        :columns="columns"
        :data-source="users"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'orgId'">
            <a-tag color="green">{{ getOrgName(record.orgId) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'roles'">
            <a-tag v-for="role in record.roles" :key="role.id" color="blue">
              {{ role.name }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'enabled'">
            <a-tag :color="record.enabled ? 'green' : 'red'">
              {{ record.enabled ? '启用' : '禁用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button v-if="auth.can('system:user:update')" size="small" @click="openEditModal(record)">编辑</a-button>
              <a-button v-if="auth.can('system:user:reset-password')" size="small" @click="openResetPasswordModal(record)">重置密码</a-button>
              <a-popconfirm
                v-if="auth.can('system:user:delete')"
                title="确定删除该登录账号？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="handleDelete(record.id)"
              >
                <a-button size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- Create/Edit Modal -->
    <a-modal
      :open="modalVisible"
      :title="isEdit ? '编辑账号' : '新增账号'"
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
        <a-form-item label="用户名" name="username" v-if="!isEdit">
          <a-input v-model:value="formState.username" />
        </a-form-item>
        <a-form-item label="姓名" name="displayName">
          <a-input v-model:value="formState.displayName" />
        </a-form-item>
        <a-form-item label="密码" name="password" v-if="!isEdit">
          <a-input-password v-model:value="formState.password" />
        </a-form-item>
        <a-form-item label="手机号" name="phone">
          <a-input v-model:value="formState.phone" />
        </a-form-item>
        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="formState.email" />
        </a-form-item>
        <a-form-item label="所属组织" name="orgId">
          <a-select v-model:value="formState.orgId" :options="orgOptions" allow-clear placeholder="选择组织" />
        </a-form-item>
        <a-form-item label="状态" name="enabled" v-if="isEdit">
          <a-switch v-model:checked="formState.enabled" />
        </a-form-item>
        <a-form-item label="角色" name="roleIds">
          <a-select
            v-model:value="formState.roleIds"
            mode="multiple"
            placeholder="选择角色"
            :options="roleOptions"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- Reset Password Modal -->
    <a-modal
      :open="resetPasswordVisible"
      title="重置密码"
      :confirm-loading="resetPasswordLoading"
      @ok="handleResetPasswordOk"
      @cancel="resetPasswordVisible = false"
    >
      <a-form layout="vertical">
        <a-form-item label="新密码">
          <a-input-password v-model:value="newPassword" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import type { TableProps, FormInstance } from 'ant-design-vue';
import {
  listUsersApi,
  createUserApi,
  updateUserApi,
  deleteUserApi,
  resetPasswordApi,
  listRolesApi,
  listOrganizationsFlatApi,
  type UserResponse,
  type RoleResponse,
  type OrganizationResponse,
} from '@/api/system';
import { useAuthStore } from '@/stores/auth';

const props = withDefaults(defineProps<{ embedded?: boolean }>(), { embedded: false });
const auth = useAuthStore();

const columns = [
  { title: '用户名', dataIndex: 'username', key: 'username' },
  { title: '姓名', dataIndex: 'displayName', key: 'displayName' },
  { title: '组织', dataIndex: 'orgId', key: 'orgId' },
  { title: '手机号', dataIndex: 'phone', key: 'phone' },
  { title: '邮箱', dataIndex: 'email', key: 'email' },
  { title: '角色', key: 'roles' },
  { title: '状态', key: 'enabled' },
  { title: '操作', key: 'action', width: 180 },
];

const users = ref<UserResponse[]>([]);
const roles = ref<RoleResponse[]>([]);
const organizations = ref<OrganizationResponse[]>([]);
const loading = ref(false);

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
});

const modalVisible = ref(false);
const modalLoading = ref(false);
const isEdit = ref(false);
const editId = ref<string>('');
const formRef = ref<FormInstance>();

const formState = reactive({
  username: '',
  displayName: '',
  password: '',
  phone: '',
  email: '',
  enabled: true,
  roleIds: [] as string[],
  orgId: '',
});

const formRules = {
  username: [{ required: true, message: '请输入用户名' }],
  displayName: [{ required: true, message: '请输入姓名' }],
  password: [{ required: true, message: '请输入密码' }, { min: 6, message: '密码至少6位' }],
};

const roleOptions = ref<{ label: string; value: string }[]>([]);
const orgOptions = ref<{ label: string; value: string }[]>([]);

const resetPasswordVisible = ref(false);
const resetPasswordLoading = ref(false);
const resetPasswordUserId = ref<string>('');
const newPassword = ref('');

async function loadData() {
  loading.value = true;
  try {
    const res = await listUsersApi(pagination.current - 1, pagination.pageSize);
    users.value = res.content;
    pagination.total = res.totalElements;
  } catch (e) {
    message.error('加载员工列表失败');
  } finally {
    loading.value = false;
  }
}

async function loadRoles() {
  try {
    const res = await listRolesApi(0, 100);
    roles.value = res.content;
    roleOptions.value = res.content.map(r => ({ label: r.name, value: r.id }));
  } catch (e) {
    message.error('加载角色列表失败');
  }
}

async function loadOrganizations() {
  try {
    organizations.value = await listOrganizationsFlatApi();
    orgOptions.value = organizations.value.map(o => ({ label: o.fullPath || o.name, value: o.id }));
  } catch (e) {
    message.error('加载组织列表失败');
  }
}

function getOrgName(orgId?: string) {
  if (!orgId) return '-';
  const org = organizations.value.find(o => o.id === orgId);
  return org ? org.name : '-';
}

onMounted(() => {
  loadData();
  if (auth.can('system:role:view')) loadRoles();
  if (auth.can('system:organization:view')) loadOrganizations();
});

const handleTableChange: TableProps['onChange'] = (pag) => {
  pagination.current = pag.current || 1;
  pagination.pageSize = pag.pageSize || 20;
  loadData();
};

function openCreateModal() {
  isEdit.value = false;
  editId.value = '';
  Object.assign(formState, {
    username: '',
    displayName: '',
    password: '',
    phone: '',
    email: '',
    enabled: true,
    roleIds: [],
    orgId: '',
  });
  modalVisible.value = true;
}

function openEditModal(record: UserResponse) {
  isEdit.value = true;
  editId.value = record.id;
  Object.assign(formState, {
    username: record.username,
    displayName: record.displayName,
    password: '',
    phone: record.phone || '',
    email: record.email || '',
    enabled: record.enabled,
    roleIds: record.roles.map(r => r.id),
    orgId: record.orgId || '',
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
      await updateUserApi(editId.value, {
        orgId: formState.orgId || undefined,
        displayName: formState.displayName,
        phone: formState.phone,
        email: formState.email,
        enabled: formState.enabled,
        roleIds: formState.roleIds,
      });
      message.success('账号信息已更新');
    } else {
      await createUserApi({
        orgId: formState.orgId || undefined,
        username: formState.username,
        displayName: formState.displayName,
        password: formState.password,
        phone: formState.phone,
        email: formState.email,
        roleIds: formState.roleIds,
      });
      message.success('账号已创建');
    }

    closeModal();
    loadData();
  } catch (e) {
    if (e instanceof Error) message.error(e.message);
  } finally {
    modalLoading.value = false;
  }
}

async function handleDelete(id: string) {
  try {
    await deleteUserApi(id);
    message.success('账号已删除');
    loadData();
  } catch (e) {
    if (e instanceof Error) message.error(e.message);
  }
}

function openResetPasswordModal(record: UserResponse) {
  resetPasswordUserId.value = record.id;
  newPassword.value = '';
  resetPasswordVisible.value = true;
}

async function handleResetPasswordOk() {
  if (!newPassword.value || newPassword.value.length < 6) {
    message.error('密码至少6位');
    return;
  }
  resetPasswordLoading.value = true;
  try {
    await resetPasswordApi(resetPasswordUserId.value, newPassword.value);
    message.success('密码已重置');
    resetPasswordVisible.value = false;
  } catch (e) {
    if (e instanceof Error) message.error(e.message);
  } finally {
    resetPasswordLoading.value = false;
  }
}
</script>

<style scoped>
.page-container {
  padding: 24px;
}
.page-container.embedded {
  padding: 0;
}
.table-card {
  margin-bottom: 16px;
}
</style>
