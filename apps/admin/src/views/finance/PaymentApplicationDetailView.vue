<template>
  <BusinessDetailPage
    :title="application?.supplierName"
    :code="application?.code"
    :subtitle="
      application
        ? `应付单 ${application.payableCode} · 申请人 ${application.applicantName}`
        : ''
    "
    :loading="loading"
    back-to="/finance/payment-applications"
    :status-label="statusLabel(application?.status)"
    :status-color="
      application?.status === 'PAID'
        ? 'green'
        : application?.status === 'REJECTED'
          ? 'red'
          : 'blue'
    "
    :stats="metrics"
    @refresh="loadData"
  >
    <template #actions>
      <a-button
        v-if="application"
        @click="router.push(`/finance/payables/${application.payableId}`)"
        >查看应付单</a-button
      >
      <a-button
        v-if="application?.supplierId"
        @click="router.push(`/procurement/suppliers/${application.supplierId}`)"
        >查看供应商</a-button
      >
    </template>
    <template #relations>
      <a-steps size="small" :current="currentStep" responsive>
        <a-step title="提交申请" :description="application?.requestedDate" />
        <a-step
          title="财务审批"
          :description="application?.approverName || '等待审批'"
        />
        <a-step
          title="出纳付款"
          :description="payment?.payerName || '等待付款'"
        />
        <a-step
          title="银行流水"
          :description="payment?.bankReference || '待登记'"
        />
        <a-step
          title="应付核销"
          :description="application?.status === 'PAID' ? '已完成' : '进行中'"
        />
      </a-steps>
    </template>
    <a-card v-if="application" :bordered="false">
      <a-tabs>
        <a-tab-pane key="application" tab="申请内容">
          <a-descriptions bordered :column="{ xs: 1, md: 2 }" size="small">
            <a-descriptions-item label="付款申请">{{
              application.code
            }}</a-descriptions-item
            ><a-descriptions-item label="供应商">{{
              application.supplierName
            }}</a-descriptions-item>
            <a-descriptions-item label="应付单"
              ><a
                @click="
                  router.push(`/finance/payables/${application.payableId}`)
                "
                >{{ application.payableCode }}</a
              ></a-descriptions-item
            ><a-descriptions-item label="申请金额">{{
              money(application.requestedAmount)
            }}</a-descriptions-item>
            <a-descriptions-item label="申请日期">{{
              application.requestedDate
            }}</a-descriptions-item
            ><a-descriptions-item label="申请人">{{
              application.applicantName
            }}</a-descriptions-item>
            <a-descriptions-item label="付款用途" :span="2">{{
              application.purpose
            }}</a-descriptions-item>
          </a-descriptions>
        </a-tab-pane>
        <a-tab-pane key="approval" tab="审批记录">
          <a-timeline>
            <a-timeline-item color="green"
              ><strong>付款申请已提交</strong>
              <p>
                {{ application.applicantName }} ·
                {{ application.requestedDate }}
              </p></a-timeline-item
            >
            <a-timeline-item
              :color="
                application.status === 'REJECTED'
                  ? 'red'
                  : application.approvedAt
                    ? 'green'
                    : 'gray'
              "
              ><strong>{{
                application.status === "REJECTED" ? "审批驳回" : "财务审批"
              }}</strong>
              <p>
                {{ application.approverName || "等待审批" }} ·
                {{ application.approvalComment || "暂无意见" }}
              </p>
              <small>{{
                dateTime(application.approvedAt)
              }}</small></a-timeline-item
            >
          </a-timeline>
        </a-tab-pane>
        <a-tab-pane key="payment" tab="付款与凭证">
          <a-descriptions
            v-if="payment"
            bordered
            :column="{ xs: 1, md: 2 }"
            size="small"
          >
            <a-descriptions-item label="付款单号">{{
              payment.code
            }}</a-descriptions-item
            ><a-descriptions-item label="付款金额">{{
              money(payment.amount)
            }}</a-descriptions-item>
            <a-descriptions-item label="付款日期">{{
              payment.paidDate
            }}</a-descriptions-item
            ><a-descriptions-item label="付款方式">{{
              methodLabel(payment.paymentMethod)
            }}</a-descriptions-item>
            <a-descriptions-item label="银行流水/凭证号">{{
              payment.bankReference
            }}</a-descriptions-item
            ><a-descriptions-item label="付款人">{{
              payment.payerName
            }}</a-descriptions-item>
          </a-descriptions>
          <a-empty v-else description="尚未执行付款" />
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </BusinessDetailPage>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { message } from "ant-design-vue";
import BusinessDetailPage, {
  type DetailMetric,
} from "@/components/BusinessDetailPage.vue";
import {
  listPaymentApplications,
  listPaymentRecords,
  type PaymentApplication,
  type PaymentRecord,
} from "@/api/finance";
const route = useRoute(),
  router = useRouter(),
  loading = ref(false),
  application = ref<PaymentApplication | null>(null),
  payment = ref<PaymentRecord | null>(null);
const metrics = computed<DetailMetric[]>(() =>
  application.value
    ? [
        { label: "申请金额", value: money(application.value.requestedAmount) },
        { label: "申请日期", value: application.value.requestedDate },
        { label: "审批人", value: application.value.approverName || "待审批" },
        {
          label: "实际付款",
          value: money(payment.value?.amount),
          hint: payment.value?.paidDate || "尚未付款",
        },
      ]
    : [],
);
const currentStep = computed(() =>
  application.value?.status === "PAID"
    ? 4
    : application.value?.status === "APPROVED"
      ? 1
      : 0,
);
onMounted(loadData);
async function loadData() {
  loading.value = true;
  try {
    const id = String(route.params.id);
    const [apps, pays] = await Promise.all([
      listPaymentApplications(),
      listPaymentRecords(),
    ]);
    application.value = apps.find((i) => i.id === id) || null;
    payment.value = pays.find((i) => i.applicationId === id) || null;
  } catch (e) {
    message.error(e instanceof Error ? e.message : "付款申请详情加载失败");
  } finally {
    loading.value = false;
  }
}
function money(v?: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    maximumFractionDigits: 0,
  }).format(Number(v || 0));
}
function statusLabel(v?: string) {
  return (
    (
      {
        PENDING_APPROVAL: "待审批",
        APPROVED: "待付款",
        REJECTED: "已驳回",
        PAID: "已付款",
      } as Record<string, string>
    )[v || ""] || ""
  );
}
function dateTime(v?: string) {
  return v ? v.slice(0, 16).replace("T", " ") : "-";
}
function methodLabel(v: string) {
  return (
    (
      {
        BANK_TRANSFER: "银行转账",
        CHECK: "支票",
        CASH: "现金",
        OTHER: "其他",
      } as Record<string, string>
    )[v] || v
  );
}
</script>
<style scoped>
.ant-timeline p {
  margin: 5px 0;
  color: #657186;
}
.ant-timeline small {
  color: #9aa4b2;
}
</style>
