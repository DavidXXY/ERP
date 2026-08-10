<template>
  <div class="page-stack">
    <a-card>
      <template #title>集采计划</template>
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
          v-if="auth.can('procurement:purchase:create')"
          type="primary"
          @click="openCreate"
        >
          <template #icon><PlusOutlined /></template>新建集采计划
        </a-button>
      </a-space>

      <a-alert
        class="section-alert"
        type="info"
        show-icon
        message="编制年度/周期集中采购计划，明细可按需一键转入采购申请，统一走询价与订单流程。"
      />

      <a-table
        :columns="columns"
        :data-source="plans"
        :loading="loading"
        :pagination="{ pageSize: 10 }"
        row-key="id"
        :scroll="{ x: 1100 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <strong>{{ record.name }}</strong>
            <span class="table-subtitle">{{ record.code }}</span>
          </template>
          <template v-else-if="column.key === 'year'">
            {{ record.periodYear }}
          </template>
          <template v-else-if="column.key === 'items'">
            {{ record.items.length }} 项
            <span class="table-subtitle"
              >{{
                (record.items as CentralPlanItem[]).filter(
                  (i: CentralPlanItem) => i.status === "REQUESTED",
                ).length
              }}
              项已转申请</span
            >
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag
              :color="
                record.status === 'ACTIVE'
                  ? 'green'
                  : record.status === 'CLOSED'
                    ? 'default'
                    : 'orange'
              "
              >{{
                record.status === "ACTIVE"
                  ? "执行中"
                  : record.status === "CLOSED"
                    ? "已关闭"
                    : "草稿"
              }}</a-tag
            >
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a-button type="link" size="small" @click="openEdit(record)"
                >编辑</a-button
              >
              <a-button
                v-if="record.status === 'DRAFT'"
                type="link"
                size="small"
                @click="handleStatus(record, 'ACTIVE')"
                >发布</a-button
              >
              <a-button
                v-if="record.status === 'ACTIVE'"
                type="link"
                size="small"
                danger
                @click="handleStatus(record, 'CLOSED')"
                >关闭</a-button
              >
            </a-space>
          </template>
        </template>
        <template #expandedRowRender="{ record }">
          <a-table
            size="small"
            :columns="itemColumns"
            :data-source="record.items"
            :pagination="false"
            row-key="id"
          >
            <template #bodyCell="{ column, record: item }">
              <template v-if="column.key === 'part'">
                <strong>{{ item.partName }}</strong>
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag
                  :color="item.status === 'REQUESTED' ? 'green' : 'blue'"
                  >{{
                    item.status === "REQUESTED" ? "已转申请" : "待执行"
                  }}</a-tag
                >
                <span v-if="item.requestCode" class="table-subtitle">{{
                  item.requestCode
                }}</span>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button
                  v-if="
                    item.status === 'PLANNED' &&
                    auth.can('procurement:purchase:create') &&
                    record.status === 'ACTIVE'
                  "
                  type="link"
                  size="small"
                  @click="openConvert(record, item)"
                  >转入采购申请</a-button
                >
              </template>
            </template>
          </a-table>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="formOpen"
      :title="editingId ? '编辑集采计划' : '新建集采计划'"
      width="920px"
      :confirm-loading="saving"
      @ok="handleSave"
    >
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="12">
            <a-form-item label="计划名称" required>
              <a-input
                v-model:value="form.name"
                placeholder="如：2026年度生产物料集采计划"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="计划年度" required>
              <a-input-number
                v-model:value="form.periodYear"
                :min="2000"
                :max="2100"
                :precision="0"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24">
            <a-form-item label="备注">
              <a-textarea v-model:value="form.remark" :rows="2" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-divider orientation="left">计划明细</a-divider>
        <a-table
          size="small"
          :data-source="form.items"
          :columns="inputColumns"
          row-key="rowKey"
          :pagination="false"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'part'">
              <a-select
                v-model:value="record.partId"
                :options="partOptions"
                show-search
                option-filter-prop="label"
                placeholder="选择物料"
                style="width: 240px"
                @change="(id: string) => syncPart(record, id)"
              />
            </template>
            <template v-else-if="column.key === 'qty'">
              <a-input-number
                v-model:value="record.plannedQty"
                :min="0.01"
                :precision="2"
                style="width: 120px"
              />
            </template>
            <template v-else-if="column.key === 'price'">
              <a-input-number
                v-model:value="record.unitPrice"
                :min="0"
                :precision="2"
                style="width: 130px"
              />
            </template>
            <template v-else-if="column.key === 'date'">
              <a-input v-model:value="record.expectedDate" type="date" />
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button
                type="link"
                size="small"
                danger
                @click="removeItem(record.rowKey)"
                >删除</a-button
              >
            </template>
          </template>
        </a-table>
        <a-button type="dashed" block style="margin-top: 8px" @click="addItem">
          <template #icon><PlusOutlined /></template>添加物料
        </a-button>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="convertOpen"
      title="转入采购申请"
      width="560px"
      :confirm-loading="converting"
      @ok="handleConvert"
    >
      <a-form layout="vertical">
        <a-alert
          class="section-alert"
          type="info"
          :message="
            convertItem
              ? convertItem.partName + ' · 数量 ' + convertItem.plannedQty
              : ''
          "
        />
        <a-form-item label="成本部门" required>
          <a-select
            v-model:value="convertDepartmentId"
            :options="departmentOptions"
            placeholder="选择申请的成本部门"
          />
        </a-form-item>
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
  convertCentralPlanItem,
  listCentralPlans,
  listProcurementCostTargets,
  listProcurementMaterials,
  saveCentralPlan,
  updateCentralPlanStatus,
  type CentralPlan,
  type CentralPlanItem,
  type ProcurementCostTargetOption,
  type ProcurementMaterial,
} from "@/api/procurement";

const auth = useAuthStore();
const router = useRouter();
const loading = ref(false);
const saving = ref(false);
const converting = ref(false);
const plans = ref<CentralPlan[]>([]);
const parts = ref<ProcurementMaterial[]>([]);
const departments = ref<ProcurementCostTargetOption[]>([]);
const formOpen = ref(false);
const convertOpen = ref(false);
const editingId = ref<string | null>(null);
const convertPlan = ref<CentralPlan | null>(null);
const convertItem = ref<CentralPlanItem | null>(null);
const convertDepartmentId = ref<string | undefined>(undefined);
const partOptions = ref<{ label: string; value: string }[]>([]);
const departmentOptions = ref<{ label: string; value: string }[]>([]);

const columns = [
  { title: "计划", key: "name", width: 320 },
  { title: "年度", key: "year", width: 90 },
  { title: "明细", key: "items", width: 160 },
  { title: "状态", key: "status", width: 100 },
  { title: "创建人", dataIndex: "createdByName", width: 120 },
  { title: "操作", key: "action", width: 170 },
];
const itemColumns = [
  { title: "物料", key: "part", width: 240 },
  { title: "计划数量", dataIndex: "plannedQty", width: 110 },
  { title: "预算单价（含税，元）", dataIndex: "unitPrice", width: 140 },
  { title: "期望日期", dataIndex: "expectedDate", width: 130 },
  { title: "状态", key: "status", width: 160 },
  { title: "操作", key: "action", width: 130 },
];
const inputColumns = [
  { title: "物料", key: "part", width: 280 },
  { title: "计划数量", key: "qty", width: 150 },
  { title: "预算单价（含税，元）", key: "price", width: 170 },
  { title: "期望日期", key: "date", width: 160 },
  { title: "操作", key: "action", width: 80 },
];

type FormItem = {
  rowKey: number;
  partId?: string;
  partName: string;
  plannedQty?: number;
  unitPrice?: number;
  expectedDate?: string;
};
const form = reactive<{
  name: string;
  periodYear: number;
  remark: string;
  items: FormItem[];
}>({
  name: "",
  periodYear: new Date().getFullYear(),
  remark: "",
  items: [],
});

function syncPart(item: FormItem, id: string) {
  const part = parts.value.find((p) => p.id === id);
  item.partName = part?.name || "";
}
function addItem() {
  form.items.push({
    rowKey: Date.now(),
    partId: undefined,
    partName: "",
    plannedQty: undefined,
    unitPrice: 0,
    expectedDate: undefined,
  });
}
function removeItem(rowKey: number) {
  form.items = form.items.filter((i) => i.rowKey !== rowKey);
}
function openCreate() {
  editingId.value = null;
  Object.assign(form, {
    name: "",
    periodYear: new Date().getFullYear(),
    remark: "",
    items: [],
  });
  addItem();
  formOpen.value = true;
}
function openEdit(record: CentralPlan) {
  editingId.value = record.id;
  Object.assign(form, {
    name: record.name,
    periodYear: record.periodYear,
    remark: record.remark || "",
    items: record.items.map((i, index) => ({
      rowKey: index + 1,
      partId: i.partId,
      partName: i.partName,
      plannedQty: i.plannedQty,
      unitPrice: i.unitPrice,
      expectedDate: i.expectedDate,
    })),
  });
  formOpen.value = true;
}
async function handleSave() {
  if (!form.name.trim()) {
    message.warning("请填写计划名称");
    return;
  }
  if (!form.periodYear) {
    message.warning("请填写计划年度");
    return;
  }
  if (
    !form.items.length ||
    form.items.some((i) => !i.partId || !i.plannedQty)
  ) {
    message.warning("请至少添加一条含物料和数量的计划明细");
    return;
  }
  saving.value = true;
  try {
    await saveCentralPlan(editingId.value, {
      name: form.name.trim(),
      periodYear: form.periodYear,
      remark: form.remark,
      items: form.items.map((i) => ({
        partId: i.partId!,
        partName: i.partName,
        plannedQty: i.plannedQty!,
        unitPrice: i.unitPrice || 0,
        expectedDate: i.expectedDate,
      })),
    });
    formOpen.value = false;
    message.success(editingId.value ? "集采计划已更新" : "集采计划已创建");
    await loadData();
  } catch (e) {
    message.error(e instanceof Error ? e.message : "保存失败");
  } finally {
    saving.value = false;
  }
}
async function handleStatus(record: CentralPlan, status: string) {
  try {
    await updateCentralPlanStatus(record.id, status);
    message.success(status === "ACTIVE" ? "计划已发布" : "计划已关闭");
    await loadData();
  } catch (e) {
    message.error(e instanceof Error ? e.message : "状态更新失败");
  }
}
function openConvert(plan: CentralPlan, item: CentralPlanItem) {
  convertPlan.value = plan;
  convertItem.value = item;
  convertDepartmentId.value = undefined;
  convertOpen.value = true;
}
async function handleConvert() {
  if (!convertPlan.value || !convertItem.value) return;
  if (!convertDepartmentId.value) {
    message.warning("请选择成本部门");
    return;
  }
  converting.value = true;
  try {
    const created = await convertCentralPlanItem(
      convertPlan.value.id,
      convertItem.value.id,
      convertDepartmentId.value,
    );
    convertOpen.value = false;
    message.success(`已转入采购申请 ${created.code || ""}`);
    await loadData();
  } catch (e) {
    message.error(e instanceof Error ? e.message : "转入采购申请失败");
  } finally {
    converting.value = false;
  }
}
async function loadData() {
  loading.value = true;
  try {
    const [list, targets, partList] = await Promise.all([
      listCentralPlans(),
      listProcurementCostTargets(),
      listProcurementMaterials(),
    ]);
    plans.value = list;
    departments.value = targets.departments;
    parts.value = partList;
    departmentOptions.value = targets.departments.map((d) => ({
      label: d.name,
      value: d.id,
    }));
    partOptions.value = partList.map((p) => ({
      label: `${p.name} (${p.model || ""})`,
      value: p.id,
    }));
  } catch (e) {
    message.error(e instanceof Error ? e.message : "集采计划加载失败");
  } finally {
    loading.value = false;
  }
}
onMounted(loadData);
</script>
