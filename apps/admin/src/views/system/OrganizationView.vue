<template>
  <div class="page-container">
    <a-card title="组织架构" class="table-card">
      <template #extra>
        <a-button v-if="auth.can('system:organization:create')" type="primary" @click="openCreateModal()">
          新增组织
        </a-button>
      </template>

      <a-tree
        :tree-data="treeData"
        :default-expand-all="true"
        :field-names="{ title: 'name', key: 'id', children: 'children' }"
      >
        <template #title="{ dataRef }">
          <div class="org-node">
            <a-space>
              <span>{{ dataRef.name }}</span>
              <a-tag color="cyan" :style="{ fontSize: '10px' }">{{ dataRef.code }}</a-tag>
              <a-tag :color="getTypeColor(dataRef.type)" :style="{ fontSize: '10px' }">
                {{ getTypeName(dataRef.type) }}
              </a-tag>
            </a-space>
            <a-space>
              <a-button
                v-if="auth.can('system:organization:update')"
                size="small"
                @click.stop="openEditModal(dataRef.id)"
              >
                编辑
              </a-button>
              <a-popconfirm
                v-if="auth.can('system:organization:delete')"
                title="确定删除该组织？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="handleDelete(dataRef.id)"
              >
                <a-button size="small" danger @click.stop>删除</a-button>
              </a-popconfirm>
            </a-space>
          </div>
        </template>
      </a-tree>
    </a-card>

    <!-- Create/Edit Modal -->
    <a-modal
      :open="modalVisible"
      :title="isEdit ? '编辑组织' : '新增组织'"
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
        <a-form-item label="组织代码" name="code" v-if="!isEdit">
          <a-input v-model:value="formState.code" placeholder="如: DEPT_SALES" />
        </a-form-item>
        <a-form-item label="组织名称" name="name">
          <a-input v-model:value="formState.name" placeholder="如: 销售部" />
        </a-form-item>
        <a-form-item label="组织类型" name="type">
          <a-select v-model:value="formState.type" :options="typeOptions" />
        </a-form-item>
        <a-form-item label="上级组织" name="parentId">
          <a-tree-select
            v-model:value="formState.parentId"
            :tree-data="flatTreeData"
            allow-clear
            placeholder="选择上级组织"
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
import type { FormInstance } from 'ant-design-vue';
import {
  listOrganizationsApi,
  listOrganizationsFlatApi,
  createOrganizationApi,
  updateOrganizationApi,
  deleteOrganizationApi,
  type OrganizationResponse,
} from '@/api/system';
import { useAuthStore } from '@/stores/auth';

const auth = useAuthStore();

const typeOptions = [
  { label: '公司', value: 'COMPANY' },
  { label: '部门', value: 'DEPARTMENT' },
  { label: '团队', value: 'TEAM' },
];

const typeMap: Record<string, string> = {
  COMPANY: '公司',
  DEPARTMENT: '部门',
  TEAM: '团队',
};

const typeColorMap: Record<string, string> = {
  COMPANY: 'red',
  DEPARTMENT: 'blue',
  TEAM: 'green',
};

function getTypeName(type: string) {
  return typeMap[type] || type;
}

function getTypeColor(type: string) {
  return typeColorMap[type] || 'default';
}

const organizations = ref<OrganizationResponse[]>([]);
const treeData = computed(() => organizations.value);

const flatOrganizations = ref<OrganizationResponse[]>([]);
const flatTreeData = computed(() => {
  return flatOrganizations.value.map(o => ({
    ...o,
    title: o.name,
    key: o.id,
    value: o.id,
  }));
});

const modalVisible = ref(false);
const modalLoading = ref(false);
const isEdit = ref(false);
const editId = ref<string>('');
const formRef = ref<FormInstance>();

const formState = reactive({
  code: '',
  name: '',
  type: 'DEPARTMENT',
  parentId: '',
});

const formRules = {
  code: [{ required: true, message: '请输入组织代码' }],
  name: [{ required: true, message: '请输入组织名称' }],
};

async function loadData() {
  try {
    organizations.value = await listOrganizationsApi();
    flatOrganizations.value = await listOrganizationsFlatApi();
  } catch (e) {
    message.error('加载组织架构失败');
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
    type: 'DEPARTMENT',
    parentId: '',
  });
  modalVisible.value = true;
}

function openEditModal(id: string) {
  const org = flatOrganizations.value.find(o => o.id === id);
  if (!org) return;
  isEdit.value = true;
  editId.value = id;
  Object.assign(formState, {
    code: org.code,
    name: org.name,
    type: org.type,
    parentId: org.parentId || '',
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
      await updateOrganizationApi(editId.value, {
        name: formState.name,
        type: formState.type,
        parentId: formState.parentId || undefined,
      });
      message.success('组织已更新');
    } else {
      await createOrganizationApi({
        code: formState.code,
        name: formState.name,
        type: formState.type,
        parentId: formState.parentId || undefined,
      });
      message.success('组织已创建');
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
    await deleteOrganizationApi(id);
    message.success('组织已删除');
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
.org-node {
  display: inline-flex;
  align-items: center;
  gap: 16px;
  min-width: 420px;
  justify-content: space-between;
}
</style>
