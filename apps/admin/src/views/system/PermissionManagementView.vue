<template>
  <div class="page-container">
    <a-card title="权限管理" class="table-card">
      <template #extra>
        <a-button v-if="auth.can('system:permission:create')" type="primary" @click="openCreateModal()">
          新增权限
        </a-button>
      </template>

      <a-table
        :columns="columns"
        :data-source="permissions"
        :loading="loading"
        row-key="id"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'module'">
            <a-tag color="cyan">{{ record.module }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button v-if="auth.can('system:permission:update')" size="small" @click="openEditModal(record)">编辑</a-button>
              <a-popconfirm
                v-if="auth.can('system:permission:delete')"
                title="确定删除该权限？"
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
      :title="isEdit ? '编辑权限' : '新增权限'"
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
        <a-form-item label="权限代码" name="code" v-if="!isEdit">
          <a-input v-model:value="formState.code" placeholder="如: system:user:view" />
        </a-form-item>
        <a-form-item label="权限名称" name="name">
          <a-input v-model:value="formState.name" placeholder="如: 查看用户" />
        </a-form-item>
        <a-form-item label="所属模块" name="module">
          <a-input v-model:value="formState.module" placeholder="如: system" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import type { FormInstance } from 'ant-design-vue';
import {
  listPermissionsApi,
  createPermissionApi,
  updatePermissionApi,
  deletePermissionApi,
  type PermissionResponse,
  type CreatePermissionRequest,
  type UpdatePermissionRequest,
} from '@/api/system';
import { useAuthStore } from '@/stores/auth';

const auth = useAuthStore();

const columns = [
  { title: '权限代码', dataIndex: 'code', key: 'code' },
  { title: '权限名称', dataIndex: 'name', key: 'name' },
  { title: '所属模块', key: 'module' },
  { title: '操作', key: 'action', width: 180 },
];

const permissions = ref<PermissionResponse[]>([]);
const loading = ref(false);

const modalVisible = ref(false);
const modalLoading = ref(false);
const isEdit = ref(false);
const editId = ref<string>('');
const formRef = ref<FormInstance>();

const formState = reactive({
  code: '',
  name: '',
  module: '',
});

const formRules = {
  code: [{ required: true, message: '请输入权限代码' }],
  name: [{ required: true, message: '请输入权限名称' }],
  module: [{ required: true, message: '请输入所属模块' }],
};

async function loadData() {
  loading.value = true;
  try {
    permissions.value = await listPermissionsApi();
  } catch (e) {
    message.error('加载权限列表失败');
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadData();
});

function openCreateModal() {
  isEdit.value = false;
  editId.value = '';
  Object.assign(formState, {
    code: '',
    name: '',
    module: '',
  });
  modalVisible.value = true;
}

function openEditModal(record: PermissionResponse) {
  isEdit.value = true;
  editId.value = record.id;
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
      message.success('权限已更新');
    } else {
      await createPermissionApi({
        code: formState.code,
        name: formState.name,
        module: formState.module,
      });
      message.success('权限已创建');
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
    await deletePermissionApi(id);
    message.success('权限已删除');
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
