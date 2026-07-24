<template>
  <div class="page-stack">
    <a-card title="到货入库">
      <template #extra>
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
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button
              v-if="
                record.inspectionStatus === 'PENDING' &&
                auth.can('procurement:order:receive')
              "
              type="link"
              @click="openInspect(record)"
            >
              质量检验
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
            <a-form-item label="红字/抵扣金额">
              <a-input-number
                v-model:value="resolveForm.creditAmount"
                :min="0"
                class="full-input"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="索赔金额">
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
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { useAuthStore } from "@/stores/auth";
import {
  inspectGoodsReceipt,
  listGoodsReceipts,
  listProcurementReturns,
  resolveProcurementReturn,
  type GoodsReceipt,
  type ProcurementReturnOrder,
} from "@/api/procurement";

const auth = useAuthStore();
const loading = ref(false);
const inspectOpen = ref(false);
const resolveOpen = ref(false);
const receipts = ref<GoodsReceipt[]>([]);
const returns = ref<ProcurementReturnOrder[]>([]);
const selectedReceipt = ref<GoodsReceipt | null>(null);
const selectedReturn = ref<ProcurementReturnOrder | null>(null);
const today = () => new Date().toISOString().slice(0, 10);
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
const receiptColumns = [
  { title: "到货单", key: "receipt", width: 210 },
  { title: "物料 / 送货单", key: "part", width: 250 },
  { title: "到货数量", key: "quantity", width: 170 },
  { title: "金额", key: "amount", width: 130 },
  { title: "到货日期", dataIndex: "receivedDate", width: 120 },
  { title: "收货人", dataIndex: "receiverName", width: 120 },
  { title: "质检状态", key: "status", width: 110 },
  { title: "操作", key: "action", width: 110, fixed: "right" as const },
];
const returnColumns = [
  { title: "退货单", dataIndex: "code" },
  { title: "数量", dataIndex: "quantity" },
  { title: "金额", key: "amount" },
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
    payableDueDate: today(),
  });
  inspectOpen.value = true;
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
  await inspectGoodsReceipt(selectedReceipt.value.id, { ...inspectForm });
  inspectOpen.value = false;
  message.success("质检完成，合格数量已入库并生成应付");
  await loadData();
}

async function saveResolution() {
  if (!selectedReturn.value) return;
  await resolveProcurementReturn(selectedReturn.value.id, { ...resolveForm });
  resolveOpen.value = false;
  message.success("退换货 / 索赔已结案");
  await loadData();
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
