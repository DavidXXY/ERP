<template>
  <div class="page-stack organization-page">
    <header class="page-heading">
      <div>
        <h2>组织架构</h2>
        <p>维护公司、部门和团队层级，组织归属将直接影响账号的数据访问范围。</p>
      </div>
      <a-space>
        <a-button @click="loadData"
          ><template #icon><ReloadOutlined /></template>刷新</a-button
        >
        <a-button
          v-if="auth.can('system:organization:create')"
          type="primary"
          @click="openCreateModal()"
        >
          <template #icon><PlusOutlined /></template>新增组织
        </a-button>
      </a-space>
    </header>

    <section class="metric-band">
      <div>
        <span>组织总数</span><strong>{{ flatOrganizations.length }}</strong>
      </div>
      <div>
        <span>部门与团队</span><strong>{{ operatingUnitCount }}</strong>
      </div>
      <div>
        <span>启用组织</span><strong>{{ activeOrganizationCount }}</strong>
      </div>
      <div>
        <span>已归属员工</span><strong>{{ assignedEmployeeCount }}</strong>
      </div>
    </section>

    <div class="organization-workspace">
      <aside class="tree-panel">
        <div class="panel-heading">
          <div>
            <strong>组织目录</strong>
            <span>按层级查看</span>
          </div>
        </div>
        <a-spin :spinning="loading">
          <a-tree
            v-if="organizations.length"
            block-node
            default-expand-all
            :selected-keys="selectedId ? [selectedId] : []"
            :tree-data="organizations"
            :field-names="{ title: 'name', key: 'id', children: 'children' }"
            @select="handleSelect"
          >
            <template #title="{ dataRef }">
              <div class="tree-node" :class="{ disabled: !dataRef.enabled }">
                <span class="tree-node-name">{{ dataRef.name }}</span>
                <span class="tree-node-count">{{
                  dataRef.totalEmployeeCount
                }}</span>
              </div>
            </template>
          </a-tree>
          <a-empty v-else-if="!loading" description="暂无组织" />
        </a-spin>
      </aside>

      <section v-if="selectedOrganization" class="detail-panel">
        <div class="detail-navigation">
          <a-button class="directory-trigger" @click="directoryVisible = true">
            <template #icon><ApartmentOutlined /></template>组织目录
          </a-button>
          <a-button v-if="parentOrganization" @click="goToParent">
            <template #icon><ArrowLeftOutlined /></template>返回上级
          </a-button>
        </div>

        <div class="detail-heading">
          <div>
            <div class="title-line">
              <h3>{{ selectedOrganization.name }}</h3>
              <a-tag :color="getTypeColor(selectedOrganization.type)">{{
                getTypeName(selectedOrganization.type)
              }}</a-tag>
              <a-tag
                :color="selectedOrganization.enabled ? 'green' : 'default'"
              >
                {{ selectedOrganization.enabled ? "启用" : "停用" }}
              </a-tag>
            </div>
            <p>{{ selectedOrganization.fullPath }}</p>
          </div>
          <a-space wrap>
            <a-button
              v-if="auth.can('system:organization:create')"
              @click="openCreateModal(selectedOrganization.id)"
            >
              <template #icon><PlusOutlined /></template>新增下级
            </a-button>
            <a-button
              v-if="auth.can('system:organization:update')"
              @click="openEditModal(selectedOrganization)"
            >
              <template #icon><EditOutlined /></template>编辑
            </a-button>
            <a-popconfirm
              v-if="
                auth.can('system:organization:delete') &&
                selectedOrganization.code !== 'ROOT'
              "
              title="删除前必须先移出下级组织和成员，确认删除？"
              ok-text="确定"
              cancel-text="取消"
              @confirm="handleDelete(selectedOrganization.id)"
            >
              <a-button danger
                ><template #icon><DeleteOutlined /></template>删除</a-button
              >
            </a-popconfirm>
          </a-space>
        </div>

        <div class="detail-metrics">
          <div>
            <span>直属员工</span
            ><strong>{{ selectedOrganization.directEmployeeCount }}</strong>
          </div>
          <div>
            <span>含下级员工</span
            ><strong>{{ selectedOrganization.totalEmployeeCount }}</strong>
          </div>
          <div>
            <span>登录账号</span
            ><strong>{{ selectedOrganization.totalUserCount }}</strong>
          </div>
          <div>
            <span>直属下级</span
            ><strong>{{ selectedOrganization.childCount }}</strong>
          </div>
        </div>

        <a-descriptions
          class="org-descriptions"
          bordered
          :column="2"
          size="small"
        >
          <a-descriptions-item label="组织代码">{{
            selectedOrganization.code
          }}</a-descriptions-item>
          <a-descriptions-item label="排序">{{
            selectedOrganization.sortOrder
          }}</a-descriptions-item>
          <a-descriptions-item label="负责人">{{
            selectedOrganization.leaderName || "未设置"
          }}</a-descriptions-item>
          <a-descriptions-item label="联系电话">{{
            selectedOrganization.phone || "未设置"
          }}</a-descriptions-item>
          <a-descriptions-item label="上级组织">{{
            selectedOrganization.parentName || "无"
          }}</a-descriptions-item>
          <a-descriptions-item label="更新时间">{{
            formatDateTime(selectedOrganization.updatedAt)
          }}</a-descriptions-item>
          <a-descriptions-item label="说明" :span="2">{{
            selectedOrganization.description || "暂无说明"
          }}</a-descriptions-item>
        </a-descriptions>

        <div class="subsection-heading">
          <div>
            <strong>直属下级</strong>
            <span>{{ selectedOrganization.children?.length || 0 }} 个组织</span>
          </div>
        </div>
        <div v-if="selectedOrganization.children?.length" class="child-list">
          <button
            v-for="child in selectedOrganization.children"
            :key="child.id"
            type="button"
            class="child-row"
            @click="selectOrganization(child.id)"
          >
            <span>
              <strong>{{ child.name }}</strong>
              <small>{{ getTypeName(child.type) }} · {{ child.code }}</small>
            </span>
            <span>{{ child.totalEmployeeCount }} 人</span>
          </button>
        </div>
        <a-empty v-else description="暂无直属下级" :image="simpleImage" />

        <template v-if="auth.can('qualification:employee:view')">
          <div class="subsection-heading employee-subsection-heading">
            <div>
              <strong>直属员工</strong>
              <span>{{ organizationEmployees.length }} 人</span>
            </div>
            <a-button @click="openHumanResources">人事管理</a-button>
          </div>
          <a-spin :spinning="organizationEmployeesLoading">
            <div
              v-if="organizationEmployees.length"
              class="organization-employee-list"
            >
              <button
                v-for="employee in organizationEmployees"
                :key="employee.id"
                type="button"
                class="organization-employee-row"
                @click="openHumanResources"
              >
                <span>
                  <strong>{{ employee.name }}</strong>
                  <small
                    >{{ employee.workNo || "未登记工号" }} ·
                    {{ employee.position || "未设置岗位" }}</small
                  >
                </span>
                <a-tag
                  :color="
                    employee.employmentStatus === 'ACTIVE' ? 'green' : 'default'
                  "
                >
                  {{
                    employee.employmentStatus === "ACTIVE" ? "在职" : "非在职"
                  }}
                </a-tag>
              </button>
            </div>
            <a-empty
              v-else-if="!organizationEmployeesLoading"
              description="该组织暂无直属员工"
              :image="simpleImage"
            />
          </a-spin>
        </template>
      </section>

      <section v-else class="empty-detail">
        <a-empty description="请选择组织查看详情" />
      </section>
    </div>

    <a-drawer
      v-model:open="directoryVisible"
      title="组织目录"
      placement="left"
      width="min(360px, 88vw)"
      :body-style="{ padding: '12px' }"
    >
      <a-spin :spinning="loading">
        <a-tree
          v-if="organizations.length"
          block-node
          default-expand-all
          :selected-keys="selectedId ? [selectedId] : []"
          :tree-data="organizations"
          :field-names="{ title: 'name', key: 'id', children: 'children' }"
          @select="handleSelect"
        >
          <template #title="{ dataRef }">
            <div class="tree-node" :class="{ disabled: !dataRef.enabled }">
              <span class="tree-node-name">{{ dataRef.name }}</span>
              <span class="tree-node-count">{{
                dataRef.totalEmployeeCount
              }}</span>
            </div>
          </template>
        </a-tree>
        <a-empty v-else-if="!loading" description="暂无组织" />
      </a-spin>
    </a-drawer>

    <a-modal
      v-model:open="modalVisible"
      :title="isEdit ? '编辑组织' : '新增组织'"
      width="720px"
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
            <a-form-item label="组织代码" name="code"
              ><a-input
                v-model:value="formState.code"
                :disabled="isEdit"
                placeholder="例如：DEPT_SALES"
            /></a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="组织名称" name="name"
              ><a-input
                v-model:value="formState.name"
                placeholder="例如：销售部"
            /></a-form-item>
          </a-col>
          <a-col :xs="24" :md="8">
            <a-form-item label="组织类型" name="type"
              ><a-select v-model:value="formState.type" :options="typeOptions"
            /></a-form-item>
          </a-col>
          <a-col :xs="24" :md="10">
            <a-form-item label="上级组织">
              <a-tree-select
                v-model:value="formState.parentId"
                :tree-data="parentTreeOptions"
                allow-clear
                tree-default-expand-all
                placeholder="作为根组织"
                :field-names="{
                  label: 'name',
                  value: 'id',
                  children: 'children',
                }"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="6">
            <a-form-item label="排序"
              ><a-input-number
                v-model:value="formState.sortOrder"
                :min="0"
                class="full-input"
            /></a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="负责人"
              ><a-input
                v-model:value="formState.leaderName"
                placeholder="负责人姓名"
            /></a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="联系电话"
              ><a-input
                v-model:value="formState.phone"
                placeholder="办公电话或手机号"
            /></a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="组织说明"
              ><a-textarea
                v-model:value="formState.description"
                :rows="3"
                placeholder="职责、业务范围或管理说明"
            /></a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="启用状态"
              ><a-switch
                v-model:checked="formState.enabled"
                checked-children="启用"
                un-checked-children="停用"
            /></a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { Empty, message, type FormInstance } from "ant-design-vue";
import {
  ApartmentOutlined,
  ArrowLeftOutlined,
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
} from "@ant-design/icons-vue";
import {
  createOrganizationApi,
  deleteOrganizationApi,
  listOrganizationsApi,
  listOrganizationsFlatApi,
  updateOrganizationApi,
  type OrganizationResponse,
} from "@/api/system";
import { useAuthStore } from "@/stores/auth";
import {
  listQualificationEmployees,
  type QualificationEmployee,
} from "@/api/qualification";

const auth = useAuthStore();
const router = useRouter();
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE;
const typeOptions = [
  { label: "公司", value: "COMPANY" },
  { label: "部门", value: "DEPARTMENT" },
  { label: "团队", value: "TEAM" },
];
const typeMap: Record<string, string> = {
  COMPANY: "公司",
  DEPARTMENT: "部门",
  TEAM: "团队",
};
const typeColorMap: Record<string, string> = {
  COMPANY: "red",
  DEPARTMENT: "blue",
  TEAM: "green",
};
const organizations = ref<OrganizationResponse[]>([]);
const flatOrganizations = ref<OrganizationResponse[]>([]);
const loading = ref(false);
const selectedId = ref("");
const organizationEmployees = ref<QualificationEmployee[]>([]);
const organizationEmployeesLoading = ref(false);
const directoryVisible = ref(false);
const modalVisible = ref(false);
const modalLoading = ref(false);
const isEdit = ref(false);
const editId = ref("");
const formRef = ref<FormInstance>();
const formState = reactive(initialForm());
const formRules = {
  code: [{ required: true, message: "请输入组织代码" }],
  name: [{ required: true, message: "请输入组织名称" }],
  type: [{ required: true, message: "请选择组织类型" }],
};

const selectedOrganization = computed(() =>
  findOrganization(organizations.value, selectedId.value),
);
const parentOrganization = computed(() => {
  const parentId = selectedOrganization.value?.parentId;
  return parentId ? findOrganization(organizations.value, parentId) : undefined;
});
const operatingUnitCount = computed(
  () =>
    flatOrganizations.value.filter((item) => item.type !== "COMPANY").length,
);
const activeOrganizationCount = computed(
  () => flatOrganizations.value.filter((item) => item.enabled).length,
);
const assignedEmployeeCount = computed(() =>
  flatOrganizations.value.reduce(
    (sum, item) => sum + Number(item.directEmployeeCount || 0),
    0,
  ),
);
const parentTreeOptions = computed(() =>
  filterParentOptions(organizations.value, isEdit.value ? editId.value : ""),
);

onMounted(loadData);
watch(selectedId, loadOrganizationEmployees);

async function loadData() {
  loading.value = true;
  try {
    [organizations.value, flatOrganizations.value] = await Promise.all([
      listOrganizationsApi(),
      listOrganizationsFlatApi(),
    ]);
    if (
      !selectedId.value ||
      !flatOrganizations.value.some((item) => item.id === selectedId.value)
    ) {
      selectedId.value = organizations.value[0]?.id || "";
    }
  } catch (error) {
    message.error(error instanceof Error ? error.message : "加载组织架构失败");
  } finally {
    loading.value = false;
  }
}

function handleSelect(keys: (string | number)[]) {
  selectOrganization(String(keys[0] || selectedId.value));
}

function selectOrganization(id: string) {
  selectedId.value = id;
  directoryVisible.value = false;
}

function goToParent() {
  if (parentOrganization.value) selectOrganization(parentOrganization.value.id);
}

async function loadOrganizationEmployees(organizationId: string) {
  if (!organizationId || !auth.can("qualification:employee:view")) {
    organizationEmployees.value = [];
    return;
  }
  organizationEmployeesLoading.value = true;
  try {
    const result = await listQualificationEmployees({ organizationId });
    if (selectedId.value === organizationId)
      organizationEmployees.value = result;
  } catch (error) {
    organizationEmployees.value = [];
    message.error(error instanceof Error ? error.message : "加载组织员工失败");
  } finally {
    organizationEmployeesLoading.value = false;
  }
}

function openHumanResources() {
  router.push({
    path: "/hr",
    query: { tab: "employees", organizationId: selectedId.value },
  });
}

function openCreateModal(parentId?: string) {
  isEdit.value = false;
  editId.value = "";
  Object.assign(formState, initialForm(parentId));
  modalVisible.value = true;
}

function openEditModal(record: OrganizationResponse) {
  isEdit.value = true;
  editId.value = record.id;
  Object.assign(formState, {
    code: record.code,
    name: record.name,
    type: record.type,
    parentId: record.parentId,
    sortOrder: record.sortOrder,
    leaderName: record.leaderName || "",
    phone: record.phone || "",
    enabled: record.enabled,
    description: record.description || "",
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
    const payload = {
      name: formState.name,
      type: formState.type,
      parentId: formState.parentId,
      sortOrder: formState.sortOrder,
      leaderName: formState.leaderName,
      phone: formState.phone,
      enabled: formState.enabled,
      description: formState.description,
    };
    if (isEdit.value) {
      await updateOrganizationApi(editId.value, payload);
      message.success("组织信息已更新");
    } else {
      const created = await createOrganizationApi({
        ...payload,
        code: formState.code,
      });
      selectedId.value = created.id;
      message.success("组织已创建");
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
    await deleteOrganizationApi(id);
    selectedId.value = "";
    message.success("组织已删除");
    await loadData();
  } catch (error) {
    if (error instanceof Error) message.error(error.message);
  }
}

function initialForm(parentId?: string) {
  return {
    code: "",
    name: "",
    type: "DEPARTMENT",
    parentId: parentId as string | undefined,
    sortOrder: 0,
    leaderName: "",
    phone: "",
    enabled: true,
    description: "",
  };
}

function findOrganization(
  items: OrganizationResponse[],
  id: string,
): OrganizationResponse | undefined {
  for (const item of items) {
    if (item.id === id) return item;
    const child = findOrganization(item.children || [], id);
    if (child) return child;
  }
  return undefined;
}

function filterParentOptions(
  items: OrganizationResponse[],
  excludedId: string,
): OrganizationResponse[] {
  return items
    .filter((item) => item.id !== excludedId)
    .map((item) => ({
      ...item,
      children: filterParentOptions(item.children || [], excludedId),
    }));
}

function getTypeName(type: string) {
  return typeMap[type] || type;
}

function getTypeColor(type: string) {
  return typeColorMap[type] || "default";
}

function formatDateTime(value?: string) {
  return value ? new Date(value).toLocaleString("zh-CN") : "-";
}
</script>

<style scoped>
.organization-page {
  min-width: 0;
}

.page-heading,
.detail-heading,
.panel-heading,
.subsection-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.page-heading h2,
.detail-heading h3 {
  margin: 0;
  color: #182230;
}

.page-heading p,
.detail-heading p {
  margin: 5px 0 0;
  color: #667085;
}

.metric-band,
.detail-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  border: 1px solid #e4e7ec;
  background: #fff;
}

.metric-band > div,
.detail-metrics > div {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 18px 20px;
  border-right: 1px solid #e4e7ec;
}

.metric-band > div:last-child,
.detail-metrics > div:last-child {
  border-right: 0;
}

.metric-band span,
.detail-metrics span,
.panel-heading span,
.subsection-heading span {
  color: #667085;
  font-size: 13px;
}

.metric-band strong,
.detail-metrics strong {
  color: #101828;
  font-size: 24px;
}

.organization-workspace {
  display: grid;
  grid-template-columns: minmax(260px, 32%) minmax(0, 1fr);
  min-height: 560px;
  border: 1px solid #e4e7ec;
  background: #fff;
}

.tree-panel {
  padding: 20px 16px;
  border-right: 1px solid #e4e7ec;
}

.panel-heading div,
.subsection-heading div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.panel-heading {
  padding: 0 8px 16px;
}

.tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  min-width: 0;
}

.tree-node.disabled {
  color: #98a2b3;
}

.tree-node-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-node-count {
  min-width: 24px;
  color: #667085;
  font-size: 12px;
  text-align: right;
}

.detail-panel {
  min-width: 0;
  padding: 26px;
}

.detail-navigation {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.directory-trigger {
  display: none;
}

.title-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.detail-metrics {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin: 24px 0;
}

.org-descriptions {
  margin-bottom: 28px;
}

.subsection-heading {
  margin-bottom: 12px;
}

.child-list {
  border-top: 1px solid #eaecf0;
}

.child-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 14px 4px;
  border: 0;
  border-bottom: 1px solid #eaecf0;
  background: transparent;
  color: #344054;
  cursor: pointer;
  text-align: left;
}

.child-row:hover {
  background: #f9fafb;
}

.child-row > span:first-child {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.child-row small {
  color: #667085;
}

.employee-subsection-heading {
  margin-top: 28px;
}

.organization-employee-list {
  border-top: 1px solid #eaecf0;
}

.organization-employee-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
  padding: 13px 4px;
  border: 0;
  border-bottom: 1px solid #eaecf0;
  background: transparent;
  color: #344054;
  cursor: pointer;
  text-align: left;
}

.organization-employee-row:hover {
  background: #f9fafb;
}

.organization-employee-row > span {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.organization-employee-row small {
  overflow: hidden;
  color: #667085;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-detail {
  display: grid;
  place-items: center;
}

@media (max-width: 900px) {
  .page-heading,
  .detail-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .metric-band {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .metric-band > div:nth-child(2) {
    border-right: 0;
  }

  .organization-workspace {
    display: block;
  }

  .tree-panel {
    display: none;
  }

  .directory-trigger {
    display: inline-flex;
  }
}

@media (max-width: 560px) {
  .detail-panel {
    padding: 18px;
  }

  .detail-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .detail-metrics > div:nth-child(2n) {
    border-right: 0;
  }

  .detail-metrics > div:nth-child(-n + 2) {
    border-bottom: 1px solid #e4e7ec;
  }
}
</style>
