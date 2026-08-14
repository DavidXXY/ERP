<template>
  <div class="page-stack">
    <a-card>
      <template #title>框架协议</template>
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
          <template #icon><PlusOutlined /></template>新建框架协议
        </a-button>
      </a-space>

      <a-alert
        class="section-alert"
        type="info"
        show-icon
        message="与供应商签订长期供货协议并约定物料价格，创建采购订单时可引用协议价格，减少重复询价。"
      />

      <a-table
        :columns="columns"
        :data-source="agreements"
        :loading="loading"
        :pagination="{ pageSize: 10 }"
        row-key="id"
        :scroll="{ x: 1200 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'title'">
            <strong>{{ record.title }}</strong>
            <span class="table-subtitle">{{ record.code }}</span>
          </template>
          <template v-else-if="column.key === 'supplier'">
            {{ record.supplierName || "-" }}
          </template>
          <template v-else-if="column.key === 'validity'">
            {{ record.validFrom }} ~ {{ record.validTo }}
          </template>
          <template v-else-if="column.key === 'items'">
            {{ record.items.length }} 项物料
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 'ACTIVE' ? 'green' : 'default'">{{
              record.status === "ACTIVE" ? "生效中" : "已关闭"
            }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a-button type="link" size="small" @click="openDetail(record)"
                >查看</a-button
              >
              <a-button
                v-if="
                  record.status === 'ACTIVE' &&
                  auth.can('procurement:purchase:create')
                "
                type="link"
                size="small"
                @click="openEdit(record)"
                >编辑</a-button
              >
              <a-popconfirm
                v-if="
                  record.status === 'ACTIVE' &&
                  auth.can('procurement:purchase:create')
                "
                title="确认关闭该框架协议？关闭后不能据此下单。"
                @confirm="handleClose(record)"
              >
                <a-button type="link" size="small" danger>关闭</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="formOpen"
      :title="editingId ? '编辑框架协议' : '新建框架协议'"
      width="960px"
      :confirm-loading="saving"
      @ok="handleSave"
    >
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="12">
            <a-form-item label="协议名称" required>
              <a-input
                v-model:value="form.title"
                placeholder="如：2026年度办公用品框架协议"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="供应商" required>
              <a-select
                v-model:value="form.supplierId"
                :options="supplierOptions"
                show-search
                option-filter-prop="label"
                placeholder="选择已准入供应商"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="生效日期" required>
              <a-input v-model:value="form.validFrom" type="date" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="失效日期" required>
              <a-input v-model:value="form.validTo" type="date" />
            </a-form-item>
          </a-col>
          <a-col :xs="24">
            <a-form-item label="备注">
              <a-textarea v-model:value="form.remark" :rows="2" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-divider orientation="left">协议物料与价格</a-divider>
        <a-table
          size="small"
          :data-source="form.items"
          :columns="itemColumns"
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
                style="width: 260px"
                @change="(id: string) => syncPart(record, id)"
              />
            </template>
            <template v-else-if="column.key === 'price'">
              <a-input-number
                v-model:value="record.unitPrice"
                :min="0.01"
                :precision="2"
                style="width: 140px"
              />
            </template>
            <template v-else-if="column.key === 'tax'">
              <a-input-number
                v-model:value="record.taxRate"
                :min="0"
                :max="100"
                :precision="2"
                style="width: 100px"
              />
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
      v-model:open="detailOpen"
      :title="detail?.title || '框架协议'"
      width="820px"
      :footer="null"
    >
      <a-descriptions v-if="detail" bordered size="small" :column="2">
        <a-descriptions-item label="协议编码">{{
          detail.code
        }}</a-descriptions-item>
        <a-descriptions-item label="供应商">{{
          detail.supplierName
        }}</a-descriptions-item>
        <a-descriptions-item label="有效期"
          >{{ detail.validFrom }} ~ {{ detail.validTo }}</a-descriptions-item
        >
        <a-descriptions-item label="状态">
          <a-tag :color="detail.status === 'ACTIVE' ? 'green' : 'default'">{{
            detail.status === "ACTIVE" ? "生效中" : "已关闭"
          }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="创建人">{{
          detail.createdByName
        }}</a-descriptions-item>
        <a-descriptions-item label="备注">{{
          detail.remark || "-"
        }}</a-descriptions-item>
      </a-descriptions>
      <a-table
        v-if="detail"
        size="small"
        style="margin-top: 16px"
        :data-source="detail.items"
        :columns="detailItemColumns"
        :pagination="false"
        row-key="id"
      />
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
  closeFrameworkAgreement,
  getFrameworkAgreement,
  listFrameworkAgreements,
  listProcurementMaterials,
  listSuppliers,
  saveFrameworkAgreement,
  type FrameworkAgreement,
  type ProcurementMaterial,
  type Supplier,
} from "@/api/procurement";

const auth = useAuthStore();
const router = useRouter();
const loading = ref(false);
const saving = ref(false);
const agreements = ref<FrameworkAgreement[]>([]);
const suppliers = ref<Supplier[]>([]);
const parts = ref<ProcurementMaterial[]>([]);
const formOpen = ref(false);
const detailOpen = ref(false);
const editingId = ref<string | null>(null);
const detail = ref<FrameworkAgreement | null>(null);

const columns = [
  { title: "协议", key: "title", width: 300 },
  { title: "供应商", key: "supplier", width: 200 },
  { title: "有效期", key: "validity", width: 220 },
  { title: "物料", key: "items", width: 100 },
  { title: "状态", key: "status", width: 100 },
  { title: "负责人", dataIndex: "createdByName", width: 120 },
  { title: "操作", key: "action", width: 190 },
];
const itemColumns = [
  { title: "物料", key: "part", width: 300 },
  { title: "协议单价（含税，元）", key: "price", width: 170 },
  { title: "税率%", key: "tax", width: 130 },
  { title: "操作", key: "action", width: 80 },
];
const detailItemColumns = [
  { title: "物料", dataIndex: "partName", width: 260 },
  { title: "协议单价（含税，元）", dataIndex: "unitPrice", width: 180 },
  { title: "税率%", dataIndex: "taxRate", width: 120 },
];

const supplierOptions = ref<{ label: string; value: string }[]>([]);
const partOptions = ref<{ label: string; value: string }[]>([]);

type FormItem = {
  rowKey: number;
  partId?: string;
  partName: string;
  unitPrice?: number;
  taxRate: number;
};
const form = reactive<{
  title: string;
  supplierId?: string;
  validFrom: string;
  validTo: string;
  remark: string;
  items: FormItem[];
}>({
  title: "",
  supplierId: undefined,
  validFrom: "",
  validTo: "",
  remark: "",
  items: [],
});

function syncPart(item: FormItem, id: string) {
  const part = parts.value.find((p) => p.id === id);
  item.partName = part?.name || "";
  if (!item.taxRate) item.taxRate = 13;
}
function addItem() {
  form.items.push({
    rowKey: Date.now(),
    partId: undefined,
    partName: "",
    unitPrice: undefined,
    taxRate: 13,
  });
}
function removeItem(rowKey: number) {
  form.items = form.items.filter((i) => i.rowKey !== rowKey);
}
function openCreate() {
  editingId.value = null;
  Object.assign(form, {
    title: "",
    supplierId: undefined,
    validFrom: "",
    validTo: "",
    remark: "",
    items: [],
  });
  addItem();
  formOpen.value = true;
}
function openEdit(record: FrameworkAgreement) {
  editingId.value = record.id;
  Object.assign(form, {
    title: record.title,
    supplierId: record.supplierId,
    validFrom: record.validFrom,
    validTo: record.validTo,
    remark: record.remark || "",
    items: record.items.map((i, index) => ({
      rowKey: index + 1,
      partId: i.partId,
      partName: i.partName,
      unitPrice: i.unitPrice,
      taxRate: i.taxRate,
    })),
  });
  formOpen.value = true;
}
async function openDetail(record: FrameworkAgreement) {
  try {
    detail.value = await getFrameworkAgreement(record.id);
    detailOpen.value = true;
  } catch (e) {
    message.error(e instanceof Error ? e.message : "协议加载失败");
  }
}
async function handleSave() {
  if (!form.title.trim()) {
    message.warning("请填写协议名称");
    return;
  }
  if (!form.supplierId) {
    message.warning("请选择供应商");
    return;
  }
  if (!form.validFrom || !form.validTo) {
    message.warning("请填写协议有效期");
    return;
  }
  if (!form.items.length || form.items.some((i) => !i.partId || !i.unitPrice)) {
    message.warning("请至少添加一条含物料和单价的协议明细");
    return;
  }
  saving.value = true;
  try {
    await saveFrameworkAgreement(editingId.value, {
      title: form.title.trim(),
      supplierId: form.supplierId,
      validFrom: form.validFrom,
      validTo: form.validTo,
      remark: form.remark,
      items: form.items.map((i) => ({
        partId: i.partId!,
        partName: i.partName,
        unitPrice: i.unitPrice!,
        taxRate: i.taxRate,
      })),
    });
    formOpen.value = false;
    message.success(editingId.value ? "框架协议已更新" : "框架协议已创建");
    await loadData();
  } catch (e) {
    message.error(e instanceof Error ? e.message : "保存失败");
  } finally {
    saving.value = false;
  }
}
async function handleClose(record: FrameworkAgreement) {
  try {
    await closeFrameworkAgreement(record.id);
    message.success("框架协议已关闭");
    await loadData();
  } catch (e) {
    message.error(e instanceof Error ? e.message : "关闭失败");
  }
}
async function loadData() {
  loading.value = true;
  try {
    const [list, supplierList, partList] = await Promise.all([
      listFrameworkAgreements(),
      listSuppliers(0, 999),
      listProcurementMaterials(),
    ]);
    agreements.value = list;
    suppliers.value = supplierList.content || supplierList;
    parts.value = partList;
    supplierOptions.value = suppliers.value
      .filter((s) => s.admissionStatus === "APPROVED")
      .map((s) => ({ label: `${s.name} (${s.code || ""})`, value: s.id }));
    partOptions.value = parts.value.map((p) => ({
      label: `${p.name} (${p.model || ""})`,
      value: p.id,
    }));
  } catch (e) {
    message.error(e instanceof Error ? e.message : "框架协议加载失败");
  } finally {
    loading.value = false;
  }
}
onMounted(loadData);
</script>
