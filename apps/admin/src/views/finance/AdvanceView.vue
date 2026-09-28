<template>
  <div class="page-stack">
    <a-card>
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="receipts" tab="预收款">
          <a-table
            :columns="receiptColumns"
            :data-source="receipts"
            :loading="loading"
            :pagination="{ pageSize: 10 }"
            :row-key="(r: any) => r.id"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag
                  :color="
                    record.status === 'SETTLED'
                      ? 'green'
                      : record.status === 'PARTIAL'
                        ? 'blue'
                        : 'orange'
                  "
                >
                  {{ record.status }}
                </a-tag>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="payments" tab="预付款">
          <a-table
            :columns="paymentColumns"
            :data-source="payments"
            :loading="loading"
            :pagination="{ pageSize: 10 }"
            :row-key="(r: any) => r.id"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag
                  :color="
                    record.status === 'SETTLED'
                      ? 'green'
                      : record.status === 'PARTIAL'
                        ? 'blue'
                        : 'orange'
                  "
                >
                  {{ record.status }}
                </a-tag>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>
<script setup lang="ts">
import { onMounted, ref } from "vue";
import { message } from "ant-design-vue";
import {
  listAdvanceReceipts,
  listAdvancePayments,
  type AdvanceReceipt,
  type AdvancePayment,
} from "@/api/extensions";

const activeTab = ref("receipts");
const loading = ref(false);
const receipts = ref<AdvanceReceipt[]>([]);
const payments = ref<AdvancePayment[]>([]);

const receiptColumns = [
  { title: "单号", dataIndex: "code", width: 150 },
  { title: "客户", dataIndex: "customerName", width: 160 },
  { title: "金额", dataIndex: "amount", width: 120 },
  { title: "已核销", dataIndex: "settledAmount", width: 120 },
  { title: "可用", dataIndex: "available", width: 120 },
  { title: "状态", key: "status", width: 100 },
  { title: "收款日", dataIndex: "receivedDate", width: 120 },
];
const paymentColumns = [
  { title: "单号", dataIndex: "code", width: 150 },
  { title: "供应商", dataIndex: "supplierName", width: 160 },
  { title: "金额", dataIndex: "amount", width: 120 },
  { title: "已核销", dataIndex: "settledAmount", width: 120 },
  { title: "可用", dataIndex: "available", width: 120 },
  { title: "状态", key: "status", width: 100 },
  { title: "付款日", dataIndex: "paidDate", width: 120 },
];

onMounted(loadData);
async function loadData() {
  loading.value = true;
  try {
    const [r, p] = await Promise.all([
      listAdvanceReceipts(),
      listAdvancePayments(),
    ]);
    receipts.value = r;
    payments.value = p;
  } catch (e: any) {
    message.error(e.message || "加载失败");
  } finally {
    loading.value = false;
  }
}
</script>
