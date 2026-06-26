<template>
  <div class="page-stack">
    <a-card>
      <template #title>备件仓储</template>
      <template #extra>
        <a-space>
          <a-button @click="loadParts">刷新</a-button>
          <a-button v-if="auth.can('inventory:part:create')" type="primary" @click="partOpen = true">
            新增备件
          </a-button>
        </a-space>
      </template>

      <a-alert
        v-if="errorMessage"
        class="section-alert"
        type="warning"
        show-icon
        :message="errorMessage"
        description="库存接口需要后端服务和数据库迁移正常启动。"
      />

      <a-table
        :columns="partColumns"
        :data-source="parts"
        :loading="loading"
        :pagination="{ pageSize: 8 }"
        :row-key="(record: InventoryPart) => record.id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <strong>{{ record.name }}</strong>
            <span class="table-subtitle">{{ record.code }} · {{ record.model || "未填型号" }}</span>
          </template>
          <template v-else-if="column.key === 'stock'">
            <strong>{{ record.stockQty }}</strong>
            <span class="table-subtitle">安全库存 {{ record.safetyQty }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.lowStock ? 'red' : 'green'">
              {{ record.lowStock ? "低库存" : "正常" }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'cost'">
            {{ formatMoney(record.unitCost) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button size="small" @click="openMovement(record)">出入库</a-button>
              <a-button size="small" @click="selectPart(record)">流水</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-card v-if="selectedPart">
      <template #title>{{ selectedPart.name }} 库存流水</template>
      <template #extra>
        <a-tag :color="selectedPart.lowStock ? 'red' : 'green'">
          当前库存 {{ selectedPart.stockQty }}
        </a-tag>
      </template>
      <a-table
        :columns="movementColumns"
        :data-source="movements"
        :loading="movementLoading"
        :pagination="{ pageSize: 6 }"
        :row-key="(record: StockMovement) => record.id"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <a-tag :color="movementColor(record.movementType)">
              {{ movementLabel(record.movementType) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ formatDateTime(record.createdAt) }}
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="partOpen"
      title="新增备件"
      width="720px"
      :confirm-loading="savingPart"
      @ok="handleCreatePart"
    >
      <a-form ref="partFormRef" :model="partForm" :rules="partRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="备件编码" name="code">
              <a-input v-model:value="partForm.code" placeholder="BJ-DL-002" />
            </a-form-item>
          </a-col>
          <a-col :span="16">
            <a-form-item label="备件名称" name="name">
              <a-input v-model:value="partForm.name" placeholder="备件名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="规格型号">
              <a-input v-model:value="partForm.model" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="库位">
              <a-input v-model:value="partForm.location" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="初始库存">
              <a-input-number v-model:value="partForm.stockQty" :min="0" :precision="2" class="full-input" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="安全库存">
              <a-input-number v-model:value="partForm.safetyQty" :min="0" :precision="2" class="full-input" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="单位成本">
              <a-input-number v-model:value="partForm.unitCost" :min="0" :precision="2" class="full-input" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="movementOpen"
      :title="movementPart ? `${movementPart.name} 出入库` : '出入库'"
      width="640px"
      :confirm-loading="savingMovement"
      @ok="handleCreateMovement"
    >
      <a-form ref="movementFormRef" :model="movementForm" :rules="movementRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="流水类型" name="movementType">
              <a-select v-model:value="movementForm.movementType" :options="movementOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="数量" name="quantity">
              <a-input-number v-model:value="movementForm.quantity" :min="0.01" :precision="2" class="full-input" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="来源单号">
              <a-input v-model:value="movementForm.sourceNo" placeholder="工单 / 采购单 / 盘点单" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="备注">
              <a-input v-model:value="movementForm.remark" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import {
  createInventoryPart,
  createStockMovement,
  listInventoryParts,
  listStockMovements,
  type CreateInventoryPartPayload,
  type CreateStockMovementPayload,
  type InventoryPart,
  type StockMovement,
  type StockMovementType,
} from "@/api/core-business";
import { useAuthStore } from "@/stores/auth";

type PartFormState = CreateInventoryPartPayload;
type MovementFormState = CreateStockMovementPayload;

const auth = useAuthStore();
const parts = ref<InventoryPart[]>([]);
const selectedPart = ref<InventoryPart | null>(null);
const movementPart = ref<InventoryPart | null>(null);
const movements = ref<StockMovement[]>([]);
const loading = ref(false);
const movementLoading = ref(false);
const savingPart = ref(false);
const savingMovement = ref(false);
const partOpen = ref(false);
const movementOpen = ref(false);
const errorMessage = ref("");
const partFormRef = ref();
const movementFormRef = ref();
const partForm = reactive<PartFormState>(initialPartForm());
const movementForm = reactive<MovementFormState>(initialMovementForm());

const partColumns = [
  { title: "备件", key: "name", width: 280 },
  { title: "库存", key: "stock", width: 130 },
  { title: "状态", key: "status", width: 110 },
  { title: "库位", dataIndex: "location", width: 120 },
  { title: "单位成本", key: "cost", dataIndex: "unitCost", width: 120 },
  { title: "操作", key: "action", width: 150 },
];

const movementColumns = [
  { title: "类型", key: "type", dataIndex: "movementType", width: 110 },
  { title: "数量", dataIndex: "quantity", width: 100 },
  { title: "来源单号", dataIndex: "sourceNo", width: 160 },
  { title: "备注", dataIndex: "remark" },
  { title: "时间", key: "createdAt", dataIndex: "createdAt", width: 180 },
];

const movementOptions = [
  { label: "入库", value: "INBOUND" },
  { label: "领用出库", value: "OUTBOUND" },
  { label: "归还", value: "RETURN" },
  { label: "报废", value: "SCRAP" },
  { label: "盘盈调整", value: "ADJUSTMENT" },
];

const partRules = {
  code: [{ required: true, message: "请输入备件编码" }],
  name: [{ required: true, message: "请输入备件名称" }],
};

const movementRules = {
  movementType: [{ required: true, message: "请选择流水类型" }],
  quantity: [{ required: true, message: "请输入数量" }],
};

onMounted(loadParts);

async function loadParts() {
  loading.value = true;
  errorMessage.value = "";
  try {
    parts.value = await listInventoryParts();
    if (selectedPart.value) {
      selectedPart.value = parts.value.find((part) => part.id === selectedPart.value?.id) || null;
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "备件数据加载失败";
  } finally {
    loading.value = false;
  }
}

async function selectPart(part: InventoryPart) {
  selectedPart.value = part;
  movementLoading.value = true;
  try {
    movements.value = await listStockMovements(part.id);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "库存流水加载失败");
  } finally {
    movementLoading.value = false;
  }
}

function openMovement(part: InventoryPart) {
  movementPart.value = part;
  Object.assign(movementForm, initialMovementForm());
  movementOpen.value = true;
}

async function handleCreatePart() {
  await partFormRef.value?.validate();
  savingPart.value = true;
  try {
    await createInventoryPart(partForm);
    Object.assign(partForm, initialPartForm());
    partOpen.value = false;
    message.success("备件已新增");
    await loadParts();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "备件新增失败");
  } finally {
    savingPart.value = false;
  }
}

async function handleCreateMovement() {
  await movementFormRef.value?.validate();
  if (!movementPart.value) {
    return;
  }
  savingMovement.value = true;
  try {
    await createStockMovement(movementPart.value.id, movementForm);
    movementOpen.value = false;
    message.success("库存流水已写入");
    await loadParts();
    if (selectedPart.value?.id === movementPart.value.id) {
      const updated = parts.value.find((part) => part.id === movementPart.value?.id);
      if (updated) {
        await selectPart(updated);
      }
    }
  } catch (error) {
    message.error(error instanceof Error ? error.message : "库存流水写入失败");
  } finally {
    savingMovement.value = false;
  }
}

function initialPartForm(): PartFormState {
  return {
    code: "",
    name: "",
    model: "",
    stockQty: 0,
    safetyQty: 0,
    location: "",
    unitCost: 0,
  };
}

function initialMovementForm(): MovementFormState {
  return {
    movementType: "INBOUND",
    quantity: 1,
    sourceNo: "",
    remark: "",
  };
}

function movementLabel(type: StockMovementType) {
  return movementOptions.find((option) => option.value === type)?.label || type;
}

function movementColor(type: StockMovementType) {
  const colors: Record<StockMovementType, string> = {
    INBOUND: "green",
    OUTBOUND: "orange",
    RETURN: "blue",
    SCRAP: "red",
    ADJUSTMENT: "purple",
  };
  return colors[type];
}

function formatMoney(value: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    maximumFractionDigits: 0,
  }).format(value || 0);
}

function formatDateTime(value: string) {
  return value ? new Date(value).toLocaleString("zh-CN") : "-";
}
</script>
