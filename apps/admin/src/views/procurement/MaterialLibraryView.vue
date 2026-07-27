<template>
  <div class="page-stack">
    <a-card title="物料库">
      <template #extra>
        <a-space>
          <a-button :loading="loading" @click="loadData">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
          <a-button v-if="canManage" @click="openCategoryCreate">
            <template #icon><PlusOutlined /></template>
            新增分类
          </a-button>
          <a-button v-if="canManage" type="primary" @click="openCreate">
            <template #icon><PlusOutlined /></template>
            新增物料
          </a-button>
        </a-space>
      </template>

      <a-alert
        v-if="errorMessage"
        class="section-alert"
        type="warning"
        show-icon
        :message="errorMessage"
      />

      <a-row :gutter="[16, 16]" class="metric-row">
        <a-col :xs="12" :lg="6">
          <a-statistic title="物料品种" :value="materials.length" suffix="种" />
        </a-col>
        <a-col :xs="12" :lg="6">
          <a-statistic title="低库存物料" :value="lowStockCount" suffix="种" />
        </a-col>
        <a-col :xs="12" :lg="6">
          <a-statistic title="库存总量" :value="totalStock" :precision="2" />
        </a-col>
        <a-col :xs="12" :lg="6">
          <a-statistic
            title="库存金额"
            :value="inventoryValue"
            :formatter="moneyFormatter"
          />
        </a-col>
      </a-row>

      <a-space wrap class="table-toolbar">
        <a-input
          v-model:value="keyword"
          allow-clear
          class="material-search"
          placeholder="搜索编码、名称、规格或分类"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-select
          v-model:value="stockFilter"
          class="stock-filter"
          :options="stockFilterOptions"
        />
        <a-select
          v-model:value="categoryFilter"
          class="category-filter"
          :options="categoryFilterOptions"
        />
        <span class="result-count">{{ filteredMaterials.length }} 条物料</span>
      </a-space>

      <a-table
        :columns="columns"
        :data-source="filteredMaterials"
        :loading="loading"
        :pagination="{ pageSize: 15, showSizeChanger: true }"
        :row-key="(record: ProcurementMaterial) => record.id"
        :scroll="{ x: 1040 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">
            <strong>{{ record.name }}</strong>
            <span class="table-subtitle">{{ record.code || "-" }}</span>
          </template>
          <template v-else-if="column.key === 'model'">
            {{ record.model || "-" }}
          </template>
          <template v-else-if="column.key === 'stock'">
            <strong>{{ formatQuantity(record.stockQty) }}</strong>
            <span class="table-subtitle">
              安全库存 {{ formatQuantity(record.safetyQty) }}
            </span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.lowStock ? 'red' : 'green'">
              {{ record.lowStock ? "低库存" : "正常" }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'category'">
            <a-tag>{{ record.category }}</a-tag>
          </template>
          <template v-else-if="column.key === 'cost'">
            {{ formatMoney(record.unitCost) }}
          </template>
          <template v-else-if="column.key === 'value'">
            {{ formatMoney(Number(record.stockQty) * Number(record.unitCost)) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space v-if="canManage" size="small">
              <a-button type="link" size="small" @click="openEdit(record)">
                编辑
              </a-button>
              <a-tooltip
                v-if="Number(record.stockQty) > 0"
                title="有库存的物料不能删除"
              >
                <span
                  ><a-button type="link" danger size="small" disabled
                    >删除</a-button
                  ></span
                >
              </a-tooltip>
              <a-popconfirm
                v-else
                :title="
                  isAdmin ? '确认直接删除该物料？' : '确认提交物料删除申请？'
                "
                :description="
                  isAdmin ? '删除后无法恢复。' : '申请提交后由管理员审批。'
                "
                ok-text="确认"
                cancel-text="取消"
                @confirm="handleDelete(record)"
              >
                <a-button type="link" danger size="small">
                  {{ isAdmin ? "删除" : "申请删除" }}
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
        <template #emptyText>暂无符合条件的物料</template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="formOpen"
      :title="editingMaterial ? '编辑物料' : '新增物料'"
      width="720px"
      :confirm-loading="saving"
      @ok="saveMaterial"
    >
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="8">
            <a-form-item label="物料编码" name="code">
              <a-input
                v-model:value="form.code"
                :disabled="Boolean(editingMaterial)"
                :placeholder="editingMaterial ? '' : '留空自动生成'"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="16">
            <a-form-item label="物料名称" name="name">
              <a-input v-model:value="form.name" placeholder="请输入物料名称" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="规格型号">
              <a-input
                v-model:value="form.model"
                placeholder="请输入规格型号"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="物料分类" name="category">
              <a-select
                v-model:value="form.category"
                show-search
                placeholder="请选择物料分类"
                :options="categoryOptions"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="安全库存">
              <a-input-number
                v-model:value="form.safetyQty"
                :min="0"
                :precision="2"
                class="full-input"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="单位成本">
              <a-input-number
                v-model:value="form.unitCost"
                :min="0"
                :precision="2"
                class="full-input"
              />
            </a-form-item>
          </a-col>
          <a-col v-if="editingMaterial" :span="24">
            <a-descriptions bordered size="small" :column="2">
              <a-descriptions-item label="当前库存">
                {{ formatQuantity(editingMaterial.stockQty) }}
              </a-descriptions-item>
              <a-descriptions-item label="库存状态">
                {{ editingMaterial.lowStock ? "低库存" : "正常" }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="categoryOpen"
      title="新增物料分类"
      width="480px"
      :confirm-loading="categorySaving"
      @ok="saveCategory"
    >
      <a-form
        ref="categoryFormRef"
        :model="categoryForm"
        :rules="categoryRules"
        layout="vertical"
      >
        <a-form-item label="分类名称" name="name">
          <a-input
            v-model:value="categoryForm.name"
            :maxlength="64"
            placeholder="例如：设备类"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import PlusOutlined from "@ant-design/icons-vue/PlusOutlined";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import SearchOutlined from "@ant-design/icons-vue/SearchOutlined";
import {
  createMaterialCategory,
  createProcurementMaterial,
  deleteProcurementMaterial,
  listMaterialCategories,
  listProcurementMaterials,
  updateProcurementMaterial,
  type CreateProcurementMaterialPayload,
  type MaterialCategory,
  type ProcurementMaterial,
} from "@/api/procurement";
import { useAuthStore } from "@/stores/auth";

type StockFilter = "ALL" | "LOW" | "NORMAL";

const auth = useAuthStore();
const materials = ref<ProcurementMaterial[]>([]);
const categories = ref<MaterialCategory[]>([]);
const keyword = ref("");
const stockFilter = ref<StockFilter>("ALL");
const categoryFilter = ref("ALL");
const loading = ref(false);
const saving = ref(false);
const categorySaving = ref(false);
const errorMessage = ref("");
const formOpen = ref(false);
const categoryOpen = ref(false);
const formRef = ref();
const categoryFormRef = ref();
const editingMaterial = ref<ProcurementMaterial | null>(null);
const form = reactive<CreateProcurementMaterialPayload>(initialForm());
const categoryForm = reactive({ name: "" });

const canManage = computed(() => auth.can("procurement:material:manage"));
const isAdmin = computed(
  () => auth.user?.roleCodes?.includes("ADMIN") ?? false,
);
const lowStockCount = computed(
  () => materials.value.filter((item) => item.lowStock).length,
);
const totalStock = computed(() =>
  materials.value.reduce((sum, item) => sum + Number(item.stockQty || 0), 0),
);
const inventoryValue = computed(() =>
  materials.value.reduce(
    (sum, item) =>
      sum + Number(item.stockQty || 0) * Number(item.unitCost || 0),
    0,
  ),
);
const filteredMaterials = computed(() => {
  const query = keyword.value.trim().toLocaleLowerCase();
  return materials.value.filter((item) => {
    const matchesKeyword =
      !query ||
      [item.code, item.name, item.model, item.category].some((value) =>
        String(value || "")
          .toLocaleLowerCase()
          .includes(query),
      );
    const matchesStock =
      stockFilter.value === "ALL" ||
      (stockFilter.value === "LOW" && item.lowStock) ||
      (stockFilter.value === "NORMAL" && !item.lowStock);
    const matchesCategory =
      categoryFilter.value === "ALL" || item.category === categoryFilter.value;
    return matchesKeyword && matchesStock && matchesCategory;
  });
});
const categoryOptions = computed(() =>
  categories.value.map((item) => ({ label: item.name, value: item.name })),
);
const categoryFilterOptions = computed(() => [
  { label: "全部物料分类", value: "ALL" },
  ...categoryOptions.value,
]);

const columns = [
  { title: "物料", key: "material", width: 220 },
  { title: "规格型号", key: "model", width: 170 },
  { title: "库存 / 安全库存", key: "stock", width: 150 },
  { title: "状态", key: "status", width: 100 },
  { title: "分类", key: "category", width: 120 },
  { title: "单位成本", key: "cost", width: 130 },
  { title: "库存金额", key: "value", width: 140 },
  { title: "操作", key: "action", width: 150, fixed: "right" },
];
const stockFilterOptions = [
  { label: "全部库存状态", value: "ALL" },
  { label: "仅看低库存", value: "LOW" },
  { label: "仅看正常", value: "NORMAL" },
];
const rules = {
  name: [{ required: true, message: "请输入物料名称" }],
  category: [{ required: true, message: "请选择物料分类" }],
};
const categoryRules = {
  name: [{ required: true, message: "请输入分类名称" }],
};

onMounted(loadData);

async function loadData() {
  loading.value = true;
  errorMessage.value = "";
  try {
    [materials.value, categories.value] = await Promise.all([
      listProcurementMaterials(),
      listMaterialCategories(),
    ]);
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : "物料库加载失败";
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  editingMaterial.value = null;
  Object.assign(form, initialForm());
  formOpen.value = true;
}

function openEdit(material: ProcurementMaterial) {
  editingMaterial.value = material;
  Object.assign(form, {
    code: material.code || "",
    name: material.name,
    model: material.model || "",
    category: material.category,
    safetyQty: Number(material.safetyQty || 0),
    unitCost: Number(material.unitCost || 0),
  });
  formOpen.value = true;
}

async function saveMaterial() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    const payload = {
      name: form.name.trim(),
      model: form.model?.trim() || undefined,
      category: form.category,
      safetyQty: Number(form.safetyQty || 0),
      unitCost: Number(form.unitCost || 0),
    };
    if (editingMaterial.value) {
      await updateProcurementMaterial(editingMaterial.value.id, payload);
      message.success("物料资料已更新");
    } else {
      await createProcurementMaterial({
        ...payload,
        code: form.code?.trim() || undefined,
      });
      message.success("物料已新增");
    }
    formOpen.value = false;
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "物料保存失败");
  } finally {
    saving.value = false;
  }
}

function openCategoryCreate() {
  categoryForm.name = "";
  categoryOpen.value = true;
}

async function saveCategory() {
  await categoryFormRef.value?.validate();
  categorySaving.value = true;
  try {
    const created = await createMaterialCategory(categoryForm.name.trim());
    categoryOpen.value = false;
    message.success("物料分类已新增");
    await loadData();
    if (formOpen.value) form.category = created.name;
  } catch (error) {
    message.error(error instanceof Error ? error.message : "分类新增失败");
  } finally {
    categorySaving.value = false;
  }
}

async function handleDelete(material: ProcurementMaterial) {
  try {
    const result = await deleteProcurementMaterial(material.id);
    message.success(result.message);
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "物料删除失败");
  }
}

function initialForm(): CreateProcurementMaterialPayload {
  return {
    code: "",
    name: "",
    model: "",
    category: "材料类",
    safetyQty: 0,
    unitCost: 0,
  };
}

function formatMoney(value: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number(value || 0));
}

function moneyFormatter({ value }: { value: number | string }) {
  return formatMoney(Number(value));
}

function formatQuantity(value: number) {
  return new Intl.NumberFormat("zh-CN", { maximumFractionDigits: 2 }).format(
    Number(value || 0),
  );
}
</script>

<style scoped>
.material-search {
  width: min(360px, 100%);
}

.stock-filter {
  width: 160px;
}

.category-filter {
  width: 160px;
}

.result-count {
  color: #667085;
  font-size: 13px;
}

@media (max-width: 576px) {
  .material-search,
  .stock-filter,
  .category-filter {
    width: 100%;
  }
}
</style>
