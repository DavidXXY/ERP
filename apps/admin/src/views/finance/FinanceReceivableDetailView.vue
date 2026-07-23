<template>
  <BusinessDetailPage
    :title="detail?.receivable.contractName || detail?.receivable.customerName"
    :code="detail?.receivable.code"
    :subtitle="
      detail
        ? `${detail.receivable.customerName} · ${detail.receivable.contractCode}`
        : ''
    "
    :loading="loading"
    back-to="/finance/receivables"
    :status-label="statusLabel(detail?.receivable.status)"
    :status-color="
      detail?.receivable.status === 'SETTLED'
        ? 'green'
        : detail?.receivable.status === 'OVERDUE'
          ? 'red'
          : 'blue'
    "
    :risk-label="detail?.receivable.status === 'OVERDUE' ? '逾期风险' : ''"
    risk-color="red"
    :stats="metrics"
    @refresh="loadData"
  >
    <template #actions>
      <a-button
        v-if="detail?.receivable.customerId"
        @click="router.push(`/crm/customers/${detail.receivable.customerId}`)"
        >查看客户</a-button
      >
      <a-button
        v-if="detail?.contract?.id"
        @click="router.push(`/crm/contracts/${detail.contract.id}`)"
        >查看合同</a-button
      >
    </template>
    <template #relations>
      <a-steps size="small" :current="currentStep" responsive>
        <a-step title="合同应收" :description="detail?.receivable.sourceNo" />
        <a-step title="开票申请" :description="invoiceRequestLabel" />
        <a-step
          title="发票登记"
          :description="detail?.receivable.invoiceNo || '待登记'"
        />
        <a-step
          title="客户回款"
          :description="money(detail?.receivable.settledAmount)"
        />
        <a-step
          title="核销结清"
          :description="
            detail?.receivable.status === 'SETTLED' ? '已完成' : '进行中'
          "
        />
      </a-steps>
    </template>
    <a-card v-if="detail" :bordered="false">
      <a-tabs>
        <a-tab-pane key="overview" tab="应收单据">
          <a-row :gutter="[16, 16]">
            <a-col :xs="24" :xl="12">
              <a-descriptions
                bordered
                :column="1"
                size="small"
                title="应收信息"
              >
                <a-descriptions-item label="来源单号">{{
                  detail.receivable.sourceNo
                }}</a-descriptions-item>
                <a-descriptions-item label="合同"
                  >{{ detail.receivable.contractCode }} ·
                  {{ detail.receivable.contractName }}</a-descriptions-item
                >
                <a-descriptions-item label="客户">{{
                  detail.receivable.customerName
                }}</a-descriptions-item>
                <a-descriptions-item label="应收金额">{{
                  money(detail.receivable.amount)
                }}</a-descriptions-item>
                <a-descriptions-item label="到期日">{{
                  detail.receivable.dueDate
                }}</a-descriptions-item>
                <a-descriptions-item label="负责人">{{
                  detail.customerInvoice.ownerName || "-"
                }}</a-descriptions-item>
              </a-descriptions>
            </a-col>
            <a-col :xs="24" :xl="12">
              <a-descriptions
                bordered
                :column="1"
                size="small"
                title="开票信息"
              >
                <a-descriptions-item label="开票抬头">{{
                  detail.customerInvoice.invoiceTitle || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="税号">{{
                  detail.customerInvoice.taxNo || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="开户行/账号">{{
                  [
                    detail.customerInvoice.bankName,
                    detail.customerInvoice.bankAccount,
                  ]
                    .filter(Boolean)
                    .join(" / ") || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="发票号码">{{
                  detail.receivable.invoiceNo || "未登记"
                }}</a-descriptions-item>
                <a-descriptions-item label="开票日期">{{
                  detail.receivable.invoiceDate || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="付款习惯">{{
                  detail.customerInvoice.paymentHabit || "-"
                }}</a-descriptions-item>
              </a-descriptions>
            </a-col>
          </a-row>
        </a-tab-pane>
        <a-tab-pane key="process" tab="开票与回款进度">
          <a-timeline>
            <a-timeline-item color="green"
              ><strong>应收节点生成</strong>
              <p>
                {{ detail.receivable.sourceNo }} ·
                {{ money(detail.receivable.amount) }}
              </p></a-timeline-item
            >
            <a-timeline-item
              :color="detail.receivable.invoiceRequested ? 'green' : 'gray'"
              ><strong>开票申请</strong>
              <p>
                {{ invoiceRequestLabel
                }}<template v-if="detail.receivable.invoiceRequestedBy">
                  · {{ detail.receivable.invoiceRequestedBy }}</template
                >
              </p></a-timeline-item
            >
            <a-timeline-item
              :color="detail.receivable.invoiceNo ? 'green' : 'gray'"
              ><strong>正式开票</strong>
              <p>
                {{ detail.receivable.invoiceNo || "等待财务登记"
                }}<template v-if="detail.receivable.invoiceDate">
                  · {{ detail.receivable.invoiceDate }}</template
                >
              </p></a-timeline-item
            >
            <a-timeline-item
              :color="detail.receivable.settledAmount > 0 ? 'green' : 'gray'"
              ><strong>回款登记</strong>
              <p>
                已回款 {{ money(detail.receivable.settledAmount) }}，待回款
                {{ money(detail.receivable.outstandingAmount) }}
              </p></a-timeline-item
            >
            <a-timeline-item
              :color="detail.receivable.status === 'SETTLED' ? 'green' : 'gray'"
              ><strong>核销结清</strong>
              <p>
                {{
                  detail.receivable.status === "SETTLED"
                    ? "该应收已完成闭环"
                    : "等待剩余款项到账"
                }}
              </p></a-timeline-item
            >
          </a-timeline>
        </a-tab-pane>
        <a-tab-pane key="audit" tab="审批与审计">
          <a-descriptions bordered :column="{ xs: 1, md: 2 }" size="small">
            <a-descriptions-item label="开票申请人">{{
              detail.receivable.invoiceRequestedBy || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="申请时间">{{
              dateTime(detail.receivable.invoiceRequestedAt)
            }}</a-descriptions-item>
            <a-descriptions-item label="审核人">{{
              detail.receivable.invoiceReviewedBy || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="审核时间">{{
              dateTime(detail.receivable.invoiceReviewedAt)
            }}</a-descriptions-item>
            <a-descriptions-item label="审核意见" :span="2">{{
              detail.receivable.invoiceReviewComment || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="申请说明" :span="2">{{
              detail.receivable.invoiceRequestRemark || "-"
            }}</a-descriptions-item>
          </a-descriptions>
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
  getFinanceReceivableDetail,
  type FinanceReceivableDetail,
} from "@/api/finance";
const route = useRoute(),
  router = useRouter();
const loading = ref(false);
const detail = ref<FinanceReceivableDetail | null>(null);
const metrics = computed<DetailMetric[]>(() =>
  detail.value
    ? [
        { label: "应收金额", value: money(detail.value.receivable.amount) },
        {
          label: "已收金额",
          value: money(detail.value.receivable.settledAmount),
        },
        {
          label: "待收金额",
          value: money(detail.value.receivable.outstandingAmount),
          danger: detail.value.receivable.status === "OVERDUE",
        },
        {
          label: "到期日",
          value: detail.value.receivable.dueDate,
          hint:
            detail.value.receivable.status === "OVERDUE"
              ? "已逾期"
              : "按期跟进",
        },
      ]
    : [],
);
const currentStep = computed(() =>
  detail.value?.receivable.status === "SETTLED"
    ? 4
    : detail.value?.receivable.settledAmount
      ? 3
      : detail.value?.receivable.invoiceNo
        ? 2
        : detail.value?.receivable.invoiceRequested
          ? 1
          : 0,
);
const invoiceRequestLabel = computed(
  () =>
    (
      ({
        NOT_REQUESTED: "未申请",
        PENDING_APPROVAL: "审批中",
        APPROVED: "审批通过",
        REJECTED: "已驳回",
        INVOICED: "已开票",
      }) as Record<string, string>
    )[detail.value?.receivable.invoiceRequestStatus || ""] || "未申请",
);
onMounted(loadData);
async function loadData() {
  loading.value = true;
  try {
    detail.value = await getFinanceReceivableDetail(String(route.params.id));
  } catch (e) {
    message.error(e instanceof Error ? e.message : "应收详情加载失败");
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
function dateTime(v?: string) {
  return v ? v.slice(0, 16).replace("T", " ") : "-";
}
function statusLabel(v?: string) {
  return (
    (
      {
        INVOICE_PENDING: "待开票",
        PAYMENT_PENDING: "待回款",
        SETTLED: "已结清",
        OVERDUE: "已逾期",
      } as Record<string, string>
    )[v || ""] || ""
  );
}
</script>
<style scoped>
.ant-timeline p {
  margin: 5px 0;
  color: #657186;
}
</style>
