<template>
  <div class="page-stack">
    <a-card>
      <template #title>供应链采购</template>
      <template #extra>
        <a-space>
          <a-button @click="loadData">刷新</a-button>
          <a-button v-if="auth.can('procurement:supplier:create')" @click="supplierOpen = true">
            新增供应商
          </a-button>
          <a-button v-if="auth.can('procurement:purchase:create')" type="primary" @click="requestOpen = true">
            新增采购申请
          </a-button>
          <a-button v-if="auth.can('procurement:purchase:create')" @click="orderOpen = true">
            新增采购订单
          </a-button>
        </a-space>
      </template>

      <a-alert
        v-if="errorMessage"
        class="section-alert"
        type="warning"
        show-icon
        :message="errorMessage"
        description="采购接口需要后端服务和数据库迁移正常启动。"
      />

      <a-table
        :columns="requestColumns"
        :data-source="purchaseRequests"
        :loading="loading"
        :pagination="{ pageSize: 6 }"
        :row-key="(record: PurchaseRequest) => record.id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'code'">
            <strong>{{ record.code }}</strong>
            <span class="table-subtitle">{{ record.requesterName }} · {{ record.reason || "无备注" }}</span>
          </template>
          <template v-else-if="column.key === 'part'">
            {{ record.partName }}
            <span class="table-subtitle">数量 {{ record.quantity }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="requestColor(record.status)">{{ requestLabel(record.status) }}</a-tag>
            <a-tag :color="approvalColor(record.approvalStatus)">{{ approvalLabel(record.approvalStatus) }}</a-tag>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-card>
      <template #title>采购订单</template>
      <a-table
        :columns="orderColumns"
        :data-source="purchaseOrders"
        :loading="loading"
        :pagination="{ pageSize: 5 }"
        :row-key="(record: PurchaseOrder) => record.id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'code'">
            <strong>{{ record.code }}</strong>
            <span class="table-subtitle">{{ record.requestCode || "无关联申请" }}</span>
          </template>
          <template v-else-if="column.key === 'amount'">
            {{ formatMoney(record.orderAmount) }}
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag color="blue">{{ orderLabel(record.status) }}</a-tag>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-card>
      <template #title>供应商档案</template>
      <a-table
        :columns="supplierColumns"
        :data-source="suppliers"
        :loading="loading"
        :pagination="{ pageSize: 5 }"
        :row-key="(record: Supplier) => record.id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <strong>{{ record.name }}</strong>
            <span class="table-subtitle">{{ record.code }} · {{ record.category || "未分类" }}</span>
          </template>
          <template v-else-if="column.key === 'risk'">
            <a-tag :color="supplierRiskColor(record.riskStatus)">{{ supplierRiskLabel(record.riskStatus) }}</a-tag>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="supplierOpen"
      title="新增供应商"
      width="720px"
      :confirm-loading="savingSupplier"
      @ok="handleCreateSupplier"
    >
      <a-form ref="supplierFormRef" :model="supplierForm" :rules="supplierRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="供应商编码" name="code">
              <a-input v-model:value="supplierForm.code" placeholder="GYS-003" />
            </a-form-item>
          </a-col>
          <a-col :span="16">
            <a-form-item label="供应商名称" name="name">
              <a-input v-model:value="supplierForm.name" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="品类">
              <a-input v-model:value="supplierForm.category" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="账期">
              <a-input v-model:value="supplierForm.settlementTerms" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="联系人">
              <a-input v-model:value="supplierForm.contactName" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="电话">
              <a-input v-model:value="supplierForm.phone" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="风险状态">
              <a-select v-model:value="supplierForm.riskStatus" :options="supplierRiskOptions" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="requestOpen"
      title="新增采购申请"
      width="760px"
      :confirm-loading="savingRequest"
      @ok="handleCreateRequest"
    >
      <a-form ref="requestFormRef" :model="requestForm" :rules="requestRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="申请编码" name="code">
              <a-input v-model:value="requestForm.code" placeholder="CGSQ-202606-003" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="申请人" name="requesterName">
              <a-input v-model:value="requestForm.requesterName" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="期望到货">
              <a-input v-model:value="requestForm.expectedDate" type="date" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="关联备件">
              <a-select
                v-model:value="requestForm.partId"
                :options="partOptions"
                allow-clear
                show-search
                option-filter-prop="label"
                placeholder="选择备件"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="采购物料名称">
              <a-input v-model:value="requestForm.partName" placeholder="未选择备件时填写" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="数量" name="quantity">
              <a-input-number v-model:value="requestForm.quantity" :min="0.01" :precision="2" class="full-input" />
            </a-form-item>
          </a-col>
          <a-col :span="16">
            <a-form-item label="采购原因">
              <a-input v-model:value="requestForm.reason" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="orderOpen"
      title="新增采购订单"
      width="720px"
      :confirm-loading="savingOrder"
      @ok="handleCreateOrder"
    >
      <a-form ref="orderFormRef" :model="orderForm" :rules="orderRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="订单编码" name="code">
              <a-input v-model:value="orderForm.code" placeholder="CGDD-202606-002" />
            </a-form-item>
          </a-col>
          <a-col :span="16">
            <a-form-item label="供应商" name="supplierId">
              <a-select
                v-model:value="orderForm.supplierId"
                :options="supplierOptions"
                show-search
                option-filter-prop="label"
                placeholder="选择供应商"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="关联采购申请">
              <a-select
                v-model:value="orderForm.requestId"
                :options="requestOptions"
                allow-clear
                show-search
                option-filter-prop="label"
                placeholder="选择已审批申请"
              />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="订单金额">
              <a-input-number v-model:value="orderForm.orderAmount" :min="0" :precision="2" class="full-input" />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="预计到货">
              <a-input v-model:value="orderForm.expectedDeliveryDate" type="date" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import {
  createPurchaseOrder,
  createPurchaseRequest,
  createSupplier,
  listInventoryParts,
  listPurchaseOrders,
  listPurchaseRequests,
  listSuppliers,
  type ApprovalStatus,
  type CreatePurchaseOrderPayload,
  type CreatePurchaseRequestPayload,
  type CreateSupplierPayload,
  type InventoryPart,
  type PurchaseOrder,
  type PurchaseOrderStatus,
  type PurchaseRequest,
  type PurchaseRequestStatus,
  type Supplier,
  type SupplierRiskStatus,
} from "@/api/core-business";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
const suppliers = ref<Supplier[]>([]);
const purchaseRequests = ref<PurchaseRequest[]>([]);
const purchaseOrders = ref<PurchaseOrder[]>([]);
const parts = ref<InventoryPart[]>([]);
const loading = ref(false);
const errorMessage = ref("");
const supplierOpen = ref(false);
const requestOpen = ref(false);
const orderOpen = ref(false);
const savingSupplier = ref(false);
const savingRequest = ref(false);
const savingOrder = ref(false);
const supplierFormRef = ref();
const requestFormRef = ref();
const orderFormRef = ref();
const supplierForm = reactive<CreateSupplierPayload>(initialSupplierForm());
const requestForm = reactive<CreatePurchaseRequestPayload>(initialRequestForm());
const orderForm = reactive<CreatePurchaseOrderPayload>(initialOrderForm());

const requestColumns = [
  { title: "申请", key: "code", width: 270 },
  { title: "物料", key: "part", width: 220 },
  { title: "期望到货", dataIndex: "expectedDate", width: 120 },
  { title: "状态", key: "status", width: 190 },
];

const orderColumns = [
  { title: "订单", key: "code", width: 250 },
  { title: "供应商", dataIndex: "supplierName" },
  { title: "金额", key: "amount", width: 140 },
  { title: "预计到货", dataIndex: "expectedDeliveryDate", width: 120 },
  { title: "状态", key: "status", width: 110 },
];

const supplierColumns = [
  { title: "供应商", key: "name", width: 300 },
  { title: "联系人", dataIndex: "contactName", width: 110 },
  { title: "电话", dataIndex: "phone", width: 130 },
  { title: "账期", dataIndex: "settlementTerms" },
  { title: "风险", key: "risk", dataIndex: "riskStatus", width: 110 },
];

const supplierRiskOptions = [
  { label: "正常", value: "NORMAL" },
  { label: "关注", value: "WATCHLIST" },
  { label: "停用", value: "BLOCKED" },
];

const supplierRules = {
  code: [{ required: true, message: "请输入供应商编码" }],
  name: [{ required: true, message: "请输入供应商名称" }],
};

const requestRules = {
  code: [{ required: true, message: "请输入采购申请编码" }],
  requesterName: [{ required: true, message: "请输入申请人" }],
  quantity: [{ required: true, message: "请输入数量" }],
};

const orderRules = {
  code: [{ required: true, message: "请输入采购订单编码" }],
  supplierId: [{ required: true, message: "请选择供应商" }],
};

const partOptions = computed(() =>
  parts.value.map((part) => ({
    label: `${part.name} (${part.code}) · 库存 ${part.stockQty}`,
    value: part.id,
  })),
);

const supplierOptions = computed(() =>
  suppliers.value.map((supplier) => ({
    label: `${supplier.name} (${supplier.code})`,
    value: supplier.id,
  })),
);

const requestOptions = computed(() =>
  purchaseRequests.value
    .filter((request) => request.approvalStatus === "APPROVED" || request.status === "SUBMITTED")
    .map((request) => ({
      label: `${request.code} · ${request.partName}`,
      value: request.id,
    })),
);

onMounted(loadData);

async function loadData() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const [supplierData, requestData, orderData, partData] = await Promise.all([
      listSuppliers(),
      listPurchaseRequests(),
      listPurchaseOrders(),
      listInventoryParts(),
    ]);
    suppliers.value = supplierData;
    purchaseRequests.value = requestData;
    purchaseOrders.value = orderData;
    parts.value = partData;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "采购数据加载失败";
  } finally {
    loading.value = false;
  }
}

async function handleCreateSupplier() {
  await supplierFormRef.value?.validate();
  savingSupplier.value = true;
  try {
    await createSupplier(supplierForm);
    Object.assign(supplierForm, initialSupplierForm());
    supplierOpen.value = false;
    message.success("供应商已新增");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "供应商新增失败");
  } finally {
    savingSupplier.value = false;
  }
}

async function handleCreateRequest() {
  await requestFormRef.value?.validate();
  savingRequest.value = true;
  try {
    await createPurchaseRequest(requestForm);
    Object.assign(requestForm, initialRequestForm());
    requestOpen.value = false;
    message.success("采购申请已新增");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "采购申请新增失败");
  } finally {
    savingRequest.value = false;
  }
}

async function handleCreateOrder() {
  await orderFormRef.value?.validate();
  savingOrder.value = true;
  try {
    await createPurchaseOrder(orderForm);
    Object.assign(orderForm, initialOrderForm());
    orderOpen.value = false;
    message.success("采购订单已新增");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "采购订单新增失败");
  } finally {
    savingOrder.value = false;
  }
}

function initialSupplierForm(): CreateSupplierPayload {
  return {
    code: "",
    name: "",
    category: "",
    contactName: "",
    phone: "",
    settlementTerms: "",
    riskStatus: "NORMAL",
  };
}

function initialRequestForm(): CreatePurchaseRequestPayload {
  return {
    code: "",
    requesterName: "",
    partId: undefined,
    partName: "",
    quantity: 1,
    expectedDate: undefined,
    reason: "",
  };
}

function initialOrderForm(): CreatePurchaseOrderPayload {
  return {
    code: "",
    supplierId: "",
    requestId: undefined,
    orderAmount: 0,
    expectedDeliveryDate: undefined,
  };
}

function supplierRiskLabel(status: SupplierRiskStatus) {
  return supplierRiskOptions.find((option) => option.value === status)?.label || status;
}

function supplierRiskColor(status: SupplierRiskStatus) {
  const colors: Record<SupplierRiskStatus, string> = {
    NORMAL: "green",
    WATCHLIST: "orange",
    BLOCKED: "red",
  };
  return colors[status];
}

function requestLabel(status: PurchaseRequestStatus) {
  const labels: Record<PurchaseRequestStatus, string> = {
    DRAFT: "草稿",
    SUBMITTED: "已提交",
    APPROVED: "已审批",
    ORDERED: "已下单",
    RECEIVED: "已到货",
    CANCELLED: "已取消",
  };
  return labels[status];
}

function requestColor(status: PurchaseRequestStatus) {
  const colors: Record<PurchaseRequestStatus, string> = {
    DRAFT: "default",
    SUBMITTED: "blue",
    APPROVED: "green",
    ORDERED: "purple",
    RECEIVED: "cyan",
    CANCELLED: "red",
  };
  return colors[status];
}

function approvalLabel(status: ApprovalStatus) {
  const labels: Record<ApprovalStatus, string> = {
    PENDING: "待审批",
    APPROVED: "审批通过",
    REJECTED: "已驳回",
  };
  return labels[status];
}

function approvalColor(status: ApprovalStatus) {
  const colors: Record<ApprovalStatus, string> = {
    PENDING: "orange",
    APPROVED: "green",
    REJECTED: "red",
  };
  return colors[status];
}

function orderLabel(status: PurchaseOrderStatus) {
  const labels: Record<PurchaseOrderStatus, string> = {
    DRAFT: "草稿",
    ORDERED: "已下单",
    PARTIAL_RECEIVED: "部分到货",
    RECEIVED: "已到货",
    CLOSED: "已关闭",
    CANCELLED: "已取消",
  };
  return labels[status];
}

function formatMoney(value: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    maximumFractionDigits: 0,
  }).format(value || 0);
}
</script>
