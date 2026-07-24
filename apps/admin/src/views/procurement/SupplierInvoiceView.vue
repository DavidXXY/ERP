<template>
  <div class="page-stack">
    <a-card title="采购发票">
      <template #extra>
        <a-button :loading="loading" @click="load">刷新</a-button>
      </template>

      <a-space class="table-toolbar">
        <a-button
          v-if="auth.can('procurement:payable:view')"
          type="primary"
          @click="invoiceOpen = true"
        >
          登记供应商发票
        </a-button>
      </a-space>

      <a-table
        :data-source="invoices"
        :columns="invoiceColumns"
        row-key="id"
        :loading="loading"
        :pagination="{ pageSize: 12 }"
        :scroll="{ x: 1050 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'invoice'">
            <strong>{{ record.invoiceNo }}</strong>
            <span class="table-subtitle">{{ record.code }}</span>
          </template>
          <template v-else-if="column.key === 'order'">
            {{ orderLabel(record.orderId) }}
          </template>
          <template v-else-if="column.key === 'amount'">
            <strong>{{ money(record.amount) }}</strong>
            <span class="table-subtitle">
              已匹配 {{ money(record.matchedAmount) }}
            </span>
          </template>
          <template v-else-if="column.key === 'match'">
            <a-tag :color="record.matchStatus === 'MATCHED' ? 'green' : 'red'">
              {{
                record.matchStatus === "MATCHED"
                  ? "订单·收货·应付·发票一致"
                  : "金额不符"
              }}
            </a-tag>
            <span class="table-subtitle">
              差额 {{ money(record.differenceAmount) }}
            </span>
          </template>
          <template v-else-if="column.key === 'approval'">
            <a-tag
              :color="
                record.approvalStatus === 'APPROVED'
                  ? 'green'
                  : record.approvalStatus === 'REJECTED'
                    ? 'red'
                    : 'orange'
              "
            >
              {{
                record.approvalStatus === "APPROVED"
                  ? "已审核"
                  : record.approvalStatus === "REJECTED"
                    ? "已驳回"
                    : "待审核"
              }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button
                v-if="
                  record.approvalStatus === 'PENDING' &&
                  record.matchStatus === 'MATCHED' &&
                  auth.can('procurement:request:approve')
                "
                type="link"
                @click="reviewInvoice(record, 'APPROVED')"
              >
                审核通过
              </a-button>
              <a-button
                v-if="
                  record.approvalStatus === 'PENDING' &&
                  auth.can('procurement:request:approve')
                "
                type="link"
                danger
                @click="reviewInvoice(record, 'REJECTED')"
              >
                驳回
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="invoiceOpen"
      title="登记供应商发票"
      @ok="saveInvoice"
    >
      <a-form layout="vertical">
        <a-form-item label="采购订单">
          <a-select
            v-model:value="invoiceForm.orderId"
            :options="orderOptions"
          />
        </a-form-item>
        <a-form-item label="关联应付（支持分批发票）">
          <a-select
            v-model:value="invoiceForm.payableId"
            allow-clear
            :options="payableOptions"
          />
        </a-form-item>
        <a-form-item label="发票号码">
          <a-input v-model:value="invoiceForm.invoiceNo" />
        </a-form-item>
        <a-form-item label="发票金额">
          <a-input-number
            v-model:value="invoiceForm.amount"
            :min="0.01"
            class="full-input"
          />
        </a-form-item>
        <a-form-item label="税率">
          <a-input-number
            v-model:value="invoiceForm.taxRate"
            :min="0"
            :max="100"
          />
        </a-form-item>
        <a-form-item label="发票日期">
          <a-input v-model:value="invoiceForm.invoiceDate" type="date" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import { useAuthStore } from "@/stores/auth";
import * as api from "@/api/procurement";

const auth = useAuthStore();
const loading = ref(false);
const invoiceOpen = ref(false);
const invoices = ref<api.SupplierInvoice[]>([]);
const orders = ref<api.PurchaseOrder[]>([]);
const payables = ref<api.ProcurementPayable[]>([]);

const today = () => new Date().toISOString().slice(0, 10);
const invoiceForm = reactive({
  orderId: "",
  payableId: undefined as string | undefined,
  invoiceNo: "",
  amount: 0,
  taxRate: 13,
  invoiceDate: today(),
  clientRequestId: "",
});
const invoiceColumns = [
  { title: "发票", key: "invoice", width: 190 },
  { title: "采购订单", key: "order", width: 210 },
  { title: "金额", key: "amount", width: 150 },
  { title: "开票日期", dataIndex: "invoiceDate", width: 120 },
  { title: "四单匹配", key: "match", width: 220 },
  { title: "审核", key: "approval", width: 100 },
  { title: "操作", key: "action", width: 180, fixed: "right" as const },
];
const orderOptions = computed(() =>
  orders.value
    .filter((item) => item.approvalStatus === "APPROVED")
    .map((item) => ({
      label: `${item.code} · ${item.supplierName}`,
      value: item.id,
    })),
);
const payableOptions = computed(() =>
  payables.value
    .filter(
      (item) => !invoiceForm.orderId || item.orderId === invoiceForm.orderId,
    )
    .map((item) => ({
      label: `${item.code} · 待付 ${money(item.outstandingAmount)}`,
      value: item.id,
    })),
);

onMounted(load);

async function load() {
  loading.value = true;
  try {
    const [invoiceResult, orderResult, payableResult] = await Promise.all([
      api.listSupplierInvoices(),
      api.listPurchaseOrders({ page: 0, size: 999 }),
      api.listProcurementPayables(),
    ]);
    invoices.value = invoiceResult;
    orders.value = orderResult.content;
    payables.value = payableResult;
  } catch (error) {
    message.error(error instanceof Error ? error.message : "加载失败");
  } finally {
    loading.value = false;
  }
}

async function saveInvoice() {
  if (
    !invoiceForm.orderId ||
    !invoiceForm.invoiceNo.trim() ||
    Number(invoiceForm.amount) <= 0
  ) {
    message.warning("请完整填写采购订单、发票号码和发票金额");
    return;
  }
  invoiceForm.clientRequestId = `invoice-${Date.now()}`;
  await api.createSupplierInvoice({ ...invoiceForm });
  invoiceOpen.value = false;
  message.success("发票已登记，等待审核");
  await load();
}

async function reviewInvoice(
  invoice: api.SupplierInvoice,
  decision: "APPROVED" | "REJECTED",
) {
  await api.reviewSupplierInvoice(invoice.id, {
    decision,
    reviewerName: auth.user?.displayName || "审核人",
    comment: decision === "APPROVED" ? "四单匹配审核通过" : "发票审核驳回",
  });
  message.success(decision === "APPROVED" ? "发票审核通过" : "发票已驳回");
  await load();
}

function orderLabel(orderId: string) {
  const order = orders.value.find((item) => item.id === orderId);
  return order ? `${order.code} · ${order.supplierName}` : orderId.slice(0, 8);
}

function money(value: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
  }).format(Number(value || 0));
}
</script>
