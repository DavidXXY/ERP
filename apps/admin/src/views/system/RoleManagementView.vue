<template>
  <div class="page-container">
    <a-card title="角色管理" class="table-card">
      <template #extra>
        <a-button v-if="auth.can('system:role:create')" type="primary" @click="openCreateModal">
          新增角色
        </a-button>
      </template>

      <a-table
        :columns="columns"
        :data-source="roles"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'permissions'">
            <a-tag v-for="perm in record.permissions.slice(0, 3)" :key="perm.id" color="purple">
              {{ perm.name }}
            </a-tag>
            <a-tag v-if="record.permissions.length > 3" color="default">
              +{{ record.permissions.length - 3 }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'dataScope'">
            <a-tag :color="getDataScopeColor(record.dataScope)">
              {{ getDataScopeName(record.dataScope) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button v-if="auth.can('system:role:update')" size="small" @click="openEditModal(record)">编辑</a-button>
              <a-popconfirm
                v-if="auth.can('system:role:delete')"
                title="确定删除该角色？"
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
      :title="isEdit ? '编辑角色' : '新增角色'"
      :confirm-loading="modalLoading"
      width="600px"
      @ok="handleModalOk"
      @cancel="closeModal"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="formRules"
        layout="vertical"
      >
        <a-form-item label="角色代码" name="code" v-if="!isEdit">
          <a-input v-model:value="formState.code" placeholder="如: ROLE_MANAGER" />
        </a-form-item>
        <a-form-item label="角色名称" name="name">
          <a-input v-model:value="formState.name" placeholder="如: 部门经理" />
        </a-form-item>
        <a-form-item label="数据范围" name="dataScope">
          <a-select v-model:value="formState.dataScope" :options="dataScopeOptions" />
        </a-form-item>
        <a-form-item label="权限" name="permissionIds">
          <a-tree-select
            v-model:value="formState.permissionIds"
            :tree-data="permissionTree"
            tree-checkable
            allow-clear
            placeholder="选择权限"
            :field-names="{ label: 'name', value: 'id', key: 'id' }"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue';
import { message } from 'ant-design-vue';
import type { TableProps, FormInstance } from 'ant-design-vue';
import {
  listRolesApi,
  createRoleApi,
  updateRoleApi,
  deleteRoleApi,
  listPermissionsApi,
  type RoleResponse,
  type PermissionResponse,
} from '@/api/system';
import { useAuthStore } from '@/stores/auth';

const auth = useAuthStore();

const columns = [
  { title: '角色代码', dataIndex: 'code', key: 'code' },
  { title: '角色名称', dataIndex: 'name', key: 'name' },
  { title: '数据范围', key: 'dataScope' },
  { title: '权限', key: 'permissions' },
  { title: '操作', key: 'action', width: 120 },
];

const roles = ref<RoleResponse[]>([]);
const permissions = ref<PermissionResponse[]>([]);
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
  code: '',
  name: '',
  dataScope: 'SELF',
  permissionIds: [] as string[],
});

const formRules = {
  code: [{ required: true, message: '请输入角色代码' }],
  name: [{ required: true, message: '请输入角色名称' }],
  dataScope: [{ required: true, message: '请选择数据范围' }],
};

const dataScopeOptions = [
  { label: '仅本人', value: 'SELF' },
  { label: '本部门', value: 'DEPT' },
  { label: '本部门及下级', value: 'DEPT_AND_SUB' },
  { label: '全公司', value: 'ALL' },
];

const dataScopeMap: Record<string, string> = {
  SELF: '仅本人',
  DEPT: '本部门',
  DEPT_AND_SUB: '本部门及下级',
  ALL: '全公司',
};

const dataScopeColorMap: Record<string, string> = {
  SELF: 'green',
  DEPT: 'blue',
  DEPT_AND_SUB: 'orange',
  ALL: 'red',
};

function getDataScopeName(scope: string) {
  return dataScopeMap[scope] || scope;
}

function getDataScopeColor(scope: string) {
  return dataScopeColorMap[scope] || 'default';
}

const permissionTree = computed(() => {
  const modules: Record<string, { id: string; name: string; children: PermissionResponse[] }> = {};
  permissions.value.forEach(p => {
    if (!modules[p.module]) {
      modules[p.module] = {
        id: `module-${p.module}`,
        name: p.module,
        children: [],
      };
    }
    modules[p.module].children.push(p);
  });
  return Object.values(modules);
});

async function loadData() {
  loading.value = true;
  try {
    const res = await listRolesApi(pagination.current - 1, pagination.pageSize);
    roles.value = res.content;
    pagination.total = res.totalElements;
  } catch (e) {
    message.error('加载角色列表失败');
  } finally {
    loading.value = false;
  }
}

async function loadPermissions() {
  try {
    permissions.value = await listPermissionsApi();
  } catch (e) {
    message.error('加载权限列表失败');
  }
}

onMounted(() => {
  loadData();
  if (auth.can('system:permission:view')) loadPermissions();
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
    code: '',
    name: '',
    dataScope: 'SELF',
    permissionIds: [],
  });
  modalVisible.value = true;
}

function openEditModal(record: RoleResponse) {
  isEdit.value = true;
  editId.value = record.id;
  Object.assign(formState, {
    code: record.code,
    name: record.name,
    dataScope: record.dataScope,
    permissionIds: record.permissions.map(p => p.id),
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
      await updateRoleApi(editId.value, {
        name: formState.name,
        dataScope: formState.dataScope,
        permissionIds: formState.permissionIds,
      });
      message.success('角色已更新');
    } else {
      await createRoleApi({
        code: formState.code,
        name: formState.name,
        dataScope: formState.dataScope,
        permissionIds: formState.permissionIds,
      });
      message.success('角色已创建');
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
    await deleteRoleApi(id);
    message.success('角色已删除');
    loadData();
  } catch (e) {
    if (e instanceof Error) message.error(e.message);
  }
}
</script>

<style scoped>
.page-container {
  padding: 24px;
}
.table-card {
  margin-bottom: 16px;
}
</style>
