<template>
  <div class="page-stack">
    <a-card title="到货入库">
      <template #extra>
        <a-button
          v-if="auth.can('procurement:order:receive')"
          type="primary"
          @click="openArrival"
        >
          <template #icon><PlusOutlined /></template>
          登记到货
        </a-button>
        <a-button :loading="loading" @click="loadData">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </template>

      <a-alert
        type="info"
        show-icon
        class="section-alert"
        message="采购到货后先完成质量检验，合格数量入库并生成应付；不合格数量进入退换货和索赔处理。"
      />

      <a-table
        :columns="receiptColumns"
        :data-source="receipts"
        :loading="loading"
        :pagination="{ pageSize: 10 }"
        row-key="id"
        :scroll="{ x: 1180 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'receipt'">
            <strong>{{ record.code || record.id.slice(0, 8) }}</strong>
            <span class="table-subtitle">
              订单 {{ record.orderCode || record.orderId.slice(0, 8) }}
            </span>
          </template>
          <template v-else-if="column.key === 'part'">
            {{ record.partName }}
            <span class="table-subtitle">{{ record.deliveryNo }}</span>
          </template>
          <template v-else-if="column.key === 'quantity'">
            <strong>{{ record.quantity }}</strong>
            <span
              v-if="record.inspectionStatus !== 'PENDING'"
              class="table-subtitle"
            >
              合格 {{ record.qualifiedQty || 0 }} / 不合格
              {{ record.rejectedQty || 0 }}
            </span>
          </template>
          <template v-else-if="column.key === 'amount'">
            {{ money(record.amount) }}
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag
              :color="
                record.inspectionStatus === 'PENDING'
                  ? 'orange'
                  : record.inspectionStatus === 'REJECTED'
                    ? 'red'
                    : record.inspectionStatus === 'PARTIAL'
                      ? 'blue'
                      : 'green'
              "
            >
              {{ inspectionLabel(record.inspectionStatus) }}
            </a-tag>
            <a-tag
              v-if="record.appealStatus && record.appealStatus !== 'NONE'"
              :color="appealColor(record.appealStatus)"
              class="appeal-tag"
            >
              {{ appealLabel(record.appealStatus) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button
              v-if="
                record.inspectionStatus === 'PENDING' &&
                auth.can('procurement:receipt:inspect')
              "
              type="link"
              @click="openInspect(record)"
            >
              质量检验
            </a-button>
            <a-button
              v-if="
                record.appealStatus === 'PENDING' &&
                auth.can('procurement:receipt:inspect')
              "
              type="link"
              danger
              @click="openAppeal(record)"
            >
              处理申诉
            </a-button>
          </template>
        </template>
      </a-table>

      <a-divider orientation="left">退换货与索赔</a-divider>
      <a-table
        size="small"
        :data-source="returns"
        :columns="returnColumns"
        row-key="id"
        :pagination="{ pageSize: 8 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amount'">
            {{ money(record.amount) }}
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 'COMPLETED' ? 'green' : 'orange'">
              {{ record.status === "COMPLETED" ? "已结案" : "处理中" }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button
              v-if="
                record.status !== 'COMPLETED' &&
                auth.can('procurement:order:receive')
              "
              type="link"
              @click="openResolve(record)"
            >
              处理
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="inspectOpen"
      title="到货质量检验"
      @ok="saveInspection"
    >
      <a-form layout="vertical">
        <a-alert
          v-if="selectedReceipt"
          :message="`${selectedReceipt.partName} · 到货数量 ${selectedReceipt.quantity}`"
          style="margin-bottom: 16px"
        />
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="合格数量">
              <a-input-number
                v-model:value="inspectForm.qualifiedQty"
                :min="0"
                class="full-input"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="不合格数量">
              <a-input-number
                v-model:value="inspectForm.rejectedQty"
                :min="0"
                class="full-input"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="质检人">
          <a-input v-model:value="inspectForm.inspectorName" />
        </a-form-item>
        <a-form-item label="检验意见 / 不合格原因">
          <a-textarea v-model:value="inspectForm.comment" :rows="3" />
        </a-form-item>
        <a-form-item label="应付到期日">
          <a-input v-model:value="inspectForm.payableDueDate" type="date" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="resolveOpen"
      title="退换货 / 索赔结案"
      @ok="saveResolution"
    >
      <a-form layout="vertical">
        <a-row :gutter="12">
          <a-col :span="8">
            <a-form-item label="补货数量">
              <a-input-number
                v-model:value="resolveForm.replacementQty"
                :min="0"
                class="full-input"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="红字/抵扣金额（含税，元）">
              <a-input-number
                v-model:value="resolveForm.creditAmount"
                :min="0"
                class="full-input"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="索赔金额（含税，元）">
              <a-input-number
                v-model:value="resolveForm.claimAmount"
                :min="0"
                class="full-input"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="供应商反馈">
          <a-textarea v-model:value="resolveForm.supplierResponse" />
        </a-form-item>
        <a-form-item label="纠正预防措施（CAPA）">
          <a-textarea v-model:value="resolveForm.correctiveAction" />
        </a-form-item>
      </a-form>
    </a-modal>
    <a-modal
      v-model:open="arrivalOpen"
      title="登记到货"
      width="640px"
      :confirm-loading="arrivalSaving"
      @ok="saveArrival"
    >
      <a-form layout="vertical">
        <a-form-item label="采购订单" required>
          <a-select
            v-model:value="arrivalForm.orderId"
            placeholder="选择已审批且未收齐的订单"
            show-search
            option-filter-prop="label"
            :options="arrivalOrderOptions"
            @change="onArrivalOrderChange"
          />
        </a-form-item>
        <a-alert
          v-if="selectedArrivalOrder"
          type="info"
          show-icon
          :message="`${selectedArrivalOrder.code} · ${selectedArrivalOrder.partName} · 剩余可收 ${arrivalRemaining}${selectedArrivalOrder.unitPrice ? ' × ' + money(selectedArrivalOrder.unitPrice) : ''}`"
          style="margin-bottom: 14px"
        />
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="本次到货数量" required>
              <a-input-number
                v-model:value="arrivalForm.quantity"
                :min="0.01"
                :max="arrivalRemaining"
                :precision="2"
                class="full-input"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="到货日期" required>
              <a-input v-model:value="arrivalForm.receivedDate" type="date" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="送货单号" required>
              <a-input v-model:value="arrivalForm.deliveryNo" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="收货人" required>
              <a-input v-model:value="arrivalForm.receiverName" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="应付到期日">
          <a-input v-model:value="arrivalForm.payableDueDate" type="date" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="appealOpen"
      title="质检申诉处理"
      :confirm-loading="savingAppeal"
      @ok="saveAppeal"
    >
      <a-alert
        v-if="selectedAppeal"
        type="warning"
        show-icon
        :message="`${selectedAppeal.partName} · 到货单 ${selectedAppeal.code || ''} · 质检${inspectionLabel(selectedAppeal.inspectionStatus)}（合格 ${selectedAppeal.qualifiedQty || 0} / 不合格 ${selectedAppeal.rejectedQty || 0}）`"
        :description="`供应商申诉：${selectedAppeal.appealReason || '（未填写原因）'}`"
        style="margin-bottom: 14px"
      />
      <a-form layout="vertical">
        <a-form-item label="处理结果" required>
          <a-radio-group v-model:value="appealForm.action">
            <a-radio value="DISMISSED">维持原质检结果（申诉不成立）</a-radio>
            <a-radio value="REOPEN">受理申诉，打回重新质检</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item
          label="处理意见"
          :required="appealForm.action === 'DISMISSED'"
        >
          <a-textarea
            v-model:value="appealForm.comment"
            :rows="3"
            maxlength="500"
            placeholder="处理意见将通知供应商"
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
import { useAuthStore } from "@/stores/auth";
import { todayLocal } from "@/utils/date";
import {
  inspectGoodsReceipt,
  listGoodsReceipts,
  listProcurementReturns,
  listPurchaseOrders,
  registerPurchaseArrival,
  resolveAppeal,
  resolveProcurementReturn,
  type GoodsReceipt,
  type ProcurementReturnOrder,
  type PurchaseOrder,
} from "@/api/procurement";

const auth = useAuthStore();
const loading = ref(false);
const inspectOpen = ref(false);
const resolveOpen = ref(false);
const appealOpen = ref(false);
const savingAppeal = ref(false);
const arrivalOpen = ref(false);
const arrivalSaving = ref(false);
const arrivalOrders = ref<PurchaseOrder[]>([]);
const receipts = ref<GoodsReceipt[]>([]);
const returns = ref<ProcurementReturnOrder[]>([]);
const selectedReceipt = ref<GoodsReceipt | null>(null);
const selectedReturn = ref<ProcurementReturnOrder | null>(null);
const selectedAppeal = ref<GoodsReceipt | null>(null);
const appealForm = reactive({
  action: "DISMISSED" as "DISMISSED" | "REOPEN",
  comment: "",
});
const today = () => todayLocal();
const inspectForm = reactive({
  qualifiedQty: 0,
  rejectedQty: 0,
  inspectorName: "",
  comment: "",
  payableDueDate: today(),
});
const resolveForm = reactive({
  replacementQty: 0,
  creditAmount: 0,
  claimAmount: 0,
  correctiveAction: "",
  supplierResponse: "",
  handlerName: "",
});
const arrivalForm = reactive({
  orderId: "",
  quantity: 0,
  receivedDate: today(),
  deliveryNo: "",
  receiverName: "",
  payableDueDate: today(),
});
const arrivalOrderOptions = computed(() =>
  arrivalOrders.value
    .filter(
      (order) =>
        order.approvalStatus === "APPROVED" &&
        (order.status === "ORDERED" || order.status === "PARTIAL_RECEIVED") &&
        Number(order.orderedQty) > Number(order.receivedQty),
    )
    .map((order) => ({
      value: order.id,
      label: `${order.code || order.id.slice(0, 8)} · ${order.partName}（剩余 ${(
        Number(order.orderedQty) - Number(order.receivedQty)
      ).toFixed(2)}）`,
    })),
);
const selectedArrivalOrder = computed(
  () =>
    arrivalOrders.value.find((order) => order.id === arrivalForm.orderId) ||
    null,
);
const arrivalRemaining = computed(() =>
  Math.max(
    0,
    Number(selectedArrivalOrder.value?.orderedQty || 0) -
      Number(selectedArrivalOrder.value?.receivedQty || 0),
  ),
);
const receiptColumns = [
  { title: "到货单", key: "receipt", width: 210 },
  { title: "物料 / 送货单", key: "part", width: 250 },
  { title: "到货数量", key: "quantity", width: 170 },
  { title: "入库金额（含税，元）", key: "amount", width: 190 },
  { title: "到货日期", dataIndex: "receivedDate", width: 120 },
  { title: "收货人", dataIndex: "receiverName", width: 120 },
  { title: "验收人", dataIndex: "inspectorName", width: 120 },
  { title: "质检状态", key: "status", width: 150 },
  { title: "操作", key: "action", width: 170, fixed: "right" as const },
];
const returnColumns = [
  { title: "退货单", dataIndex: "code" },
  { title: "数量", dataIndex: "quantity" },
  { title: "退货金额（含税，元）", key: "amount", width: 190 },
  { title: "原因", dataIndex: "reason" },
  { title: "日期", dataIndex: "returnDate" },
  { title: "状态", key: "status" },
  { title: "操作", key: "action" },
];

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    const [receiptResult, returnResult] = await Promise.all([
      listGoodsReceipts(),
      listProcurementReturns(),
    ]);
    receipts.value = receiptResult;
    returns.value = returnResult;
  } catch (error) {
    message.error(error instanceof Error ? error.message : "加载失败");
  } finally {
    loading.value = false;
  }
}

function openInspect(receipt: GoodsReceipt) {
  selectedReceipt.value = receipt;
  Object.assign(inspectForm, {
    qualifiedQty: Number(receipt.quantity),
    rejectedQty: 0,
    inspectorName: auth.user?.displayName || "",
    comment: "",
    payableDueDate: receipt.payableDueDate || today(),
  });
  inspectOpen.value = true;
}

async function openArrival() {
  Object.assign(arrivalForm, {
    orderId: "",
    quantity: 0,
    receivedDate: today(),
    deliveryNo: "",
    receiverName: auth.user?.displayName || "",
    payableDueDate: today(),
  });
  arrivalOpen.value = true;
  try {
    const result = await listPurchaseOrders({ page: 0, size: 999 });
    arrivalOrders.value = result.content || [];
  } catch {
    arrivalOrders.value = [];
  }
}

function onArrivalOrderChange() {
  arrivalForm.quantity = arrivalRemaining.value;
}

async function saveArrival() {
  const order = selectedArrivalOrder.value;
  if (!order) {
    message.warning("请选择采购订单");
    return;
  }
  if (!arrivalForm.deliveryNo.trim()) {
    message.warning("请填写送货单号");
    return;
  }
  if (!arrivalForm.receiverName.trim()) {
    message.warning("请填写收货人");
    return;
  }
  if (Number(arrivalForm.quantity) <= 0) {
    message.warning("请填写到货数量");
    return;
  }
  if (Number(arrivalForm.quantity) > arrivalRemaining.value) {
    message.warning("到货数量不能超过剩余可收数量");
    return;
  }
  arrivalSaving.value = true;
  try {
    await registerPurchaseArrival(order.id, {
      quantity: Number(arrivalForm.quantity),
      receivedDate: arrivalForm.receivedDate,
      deliveryNo: arrivalForm.deliveryNo.trim(),
      receiverName: arrivalForm.receiverName.trim(),
      payableDueDate: arrivalForm.payableDueDate,
      clientRequestId: `arrival-${order.id}-${Date.now()}`,
    });
    arrivalOpen.value = false;
    message.success("到货已登记，请完成质检；合格后才入库并生成应付");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "登记到货失败");
  } finally {
    arrivalSaving.value = false;
  }
}

function openResolve(returnOrder: ProcurementReturnOrder) {
  selectedReturn.value = returnOrder;
  Object.assign(resolveForm, {
    replacementQty: returnOrder.quantity,
    creditAmount: 0,
    claimAmount: 0,
    correctiveAction: "",
    supplierResponse: "",
    handlerName: auth.user?.displayName || "",
  });
  resolveOpen.value = true;
}

async function saveInspection() {
  if (!selectedReceipt.value) return;
  if (
    Number(inspectForm.qualifiedQty) + Number(inspectForm.rejectedQty) !==
    Number(selectedReceipt.value.quantity)
  ) {
    message.warning("合格数量与不合格数量之和必须等于到货数量");
    return;
  }
  try {
    await inspectGoodsReceipt(selectedReceipt.value.id, { ...inspectForm });
    inspectOpen.value = false;
    message.success("质检完成，合格数量已入库并生成应付");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "质检登记失败");
  }
}

async function saveResolution() {
  if (!selectedReturn.value) return;
  try {
    await resolveProcurementReturn(selectedReturn.value.id, { ...resolveForm });
    resolveOpen.value = false;
    message.success("退换货 / 索赔已结案");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "退换货处理失败");
  }
}

function openAppeal(receipt: GoodsReceipt) {
  selectedAppeal.value = receipt;
  appealForm.action = "DISMISSED";
  appealForm.comment = "";
  appealOpen.value = true;
}
async function saveAppeal() {
  if (!selectedAppeal.value) return;
  if (appealForm.action === "DISMISSED" && !appealForm.comment.trim()) {
    message.warning("驳回申诉时请填写处理意见");
    return;
  }
  savingAppeal.value = true;
  try {
    await resolveAppeal(selectedAppeal.value.id, {
      action: appealForm.action,
      comment: appealForm.comment.trim() || undefined,
    });
    appealOpen.value = false;
    message.success(
      appealForm.action === "REOPEN"
        ? "已受理并打回重新质检，供应商将收到通知"
        : "已驳回申诉，供应商将收到通知",
    );
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "处理失败");
  } finally {
    savingAppeal.value = false;
  }
}
function appealLabel(status?: string) {
  return (
    {
      PENDING: "申诉待处理",
      DISMISSED: "申诉未成立",
      REOPENED: "已受理重检",
    }[status || "NONE"] ||
    status ||
    ""
  );
}
function appealColor(status?: string) {
  return (
    { PENDING: "red", DISMISSED: "default", REOPENED: "blue" }[
      status || "NONE"
    ] || "default"
  );
}
function inspectionLabel(status?: string) {
  return (
    {
      PENDING: "待质检",
      PASSED: "全部合格",
      PARTIAL: "部分合格",
      REJECTED: "全部不合格",
    }[status || "PENDING"] || status
  );
}

function money(value: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
  }).format(Number(value || 0));
}
</script>
