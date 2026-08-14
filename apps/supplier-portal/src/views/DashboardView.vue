<template>
  <div class="page-shell">
    <header class="page-heading">
      <div><p class="eyebrow">协作概览</p><h1>上午好，{{ store.session?.account.contactName }}</h1><p>这里汇总当前准入进度与待处理询价。</p></div>
      <a-button :loading="loading" @click="load"><ReloadOutlined /> 刷新</a-button>
    </header>

    <section class="metrics-grid" aria-label="关键状态">
      <article class="metric"><span class="metric-icon green"><SafetyCertificateOutlined /></span><div><small>供应商准入</small><strong>{{ admissionText }}</strong></div></article>
      <article class="metric"><span class="metric-icon amber"><ClockCircleOutlined /></span><div><small>待处理询价</small><strong>{{ pendingCount }}</strong></div></article>
      <article class="metric"><span class="metric-icon blue"><SendOutlined /></span><div><small>已提交报价</small><strong>{{ submittedCount }}</strong></div></article>
      <article class="metric"><span class="metric-icon green"><ProfileOutlined /></span><div><small>采购订单</small><strong>{{ orderCount }}</strong></div></article>
    </section>

    <section class="dashboard-grid">
      <div class="section-block">
        <div class="section-title"><div><h2>待处理询价</h2><p>优先处理临近截止且尚未提交的询价。</p></div><RouterLink to="/inquiries">查看全部</RouterLink></div>
        <a-skeleton v-if="loading" active />
        <a-empty v-else-if="pending.length === 0" description="暂无待处理询价" />
        <div v-else class="inquiry-list compact">
          <button v-for="item in pending.slice(0, 4)" :key="item.id" class="inquiry-row" @click="router.push('/inquiries')">
            <span><b>{{ item.title }}</b><small>{{ item.code }} · {{ item.lines.length }} 项物料</small></span>
            <span class="deadline" :class="{ urgent: daysLeft(item.deadline) <= 2 }"><ClockCircleOutlined /> {{ deadlineText(item.deadline) }}</span>
            <RightOutlined />
          </button>
        </div>
      </div>
      <aside class="section-block readiness">
        <div class="section-title"><div><h2>准入准备度</h2><p>完成资料有助于采购方快速审核。</p></div></div>
        <a-progress type="circle" :percent="readiness" :size="128" :stroke-color="readiness === 100 ? '#27845f' : '#b26a16'" />
        <div class="readiness-items">
          <span><CheckCircleOutlined :class="{ done: Boolean(store.session?.supplier.registeredAddress) }" /> 企业基础资料</span>
          <span><CheckCircleOutlined :class="{ done: documents.length > 0 }" /> 至少一份资质文件</span>
          <span><CheckCircleOutlined :class="{ done: store.session?.account.status === 'ACTIVE' }" /> 门户账号审核</span>
          <span><CheckCircleOutlined :class="{ done: store.session?.supplier.admissionStatus === 'APPROVED' }" /> 供应商准入审核</span>
        </div>
      </aside>
    </section>
    <section class="section-block">
      <div class="section-title">
        <div>
          <h2>中标与采购订单</h2>
          <p>确认中标项目，并跟进采购订单与交货进度。</p>
        </div>
        <RouterLink to="/orders">查看全部</RouterLink>
      </div>
      <a-skeleton v-if="loading" active />
      <a-empty v-else-if="orders.length === 0" description="暂无中标项目或采购订单" />
      <div v-else class="inquiry-list compact">
        <button
          v-for="item in orders.slice(0, 5)"
          :key="itemKey(item)"
          class="inquiry-row"
          @click="router.push('/orders')"
        >
          <span>
            <b>{{ item.order ? item.order.code + " · " + item.order.partName : "中标项目 · " + (item.inquiry?.code || "") }}</b>
            <small>
              {{ item.inquiry?.title || "待采购方下单" }}
              <template v-if="item.order"> · {{ money(item.order.orderAmount, item.order.currency) }} · {{ orderStatusText(item.order.status) }}</template>
              <template v-else-if="item.contract"> · {{ money(item.contract.amount, item.contract.currency) }} · {{ contractStatusText(item.contract.status) }}</template>
            </small>
          </span>
          <span v-if="item.contract && !item.contract.acknowledged" class="deadline urgent"><CheckCircleOutlined /> 待确认中标</span>
          <span v-else-if="item.contract?.acknowledged" class="deadline"><CheckCircleOutlined /> 已确认中标</span>
          <RightOutlined />
        </button>
      </div>
    </section>
    <section v-if="risks.length > 0" class="section-block risks">
      <div class="section-title">
        <div>
          <h2>待办与风险提醒</h2>
          <p>需要您及时处理或关注的事项。</p>
        </div>
      </div>
      <div class="risk-list">
        <button
          v-for="risk in risks"
          :key="risk.key"
          class="risk-row"
          type="button"
          @click="router.push(risk.to)"
        >
          <span class="risk-icon" :class="risk.level"
            ><WarningOutlined v-if="risk.level === 'high'" /><BellOutlined
              v-else
          /></span>
          <span class="risk-main"
            ><b>{{ risk.title }}</b
            ><small>{{ risk.detail }}</small></span
          >
          <RightOutlined />
        </button>
      </div>
    </section>
    <section v-if="performanceReviews.length > 0" class="section-block performance">
      <div class="section-title">
        <div>
          <h2>合作绩效</h2>
          <p>采购方按周期对交付、质量与配合度进行的评价，供贵司改进参考。</p>
        </div>
        <button class="link-btn" type="button" @click="performanceOpen = true">查看全部</button>
      </div>
      <div class="performance-grid">
        <article
          v-for="item in performanceReviews.slice(0, 3)"
          :key="item.id"
          class="performance-card"
          role="button"
          tabindex="0"
          @click="showPerformance(item)"
          @keydown.enter="showPerformance(item)"
        >
          <div class="performance-head">
            <strong>{{ item.reviewPeriod }}</strong>
            <a-space :size="4">
              <a-tag :color="item.totalScore >= 90 ? 'green' : item.totalScore >= 75 ? 'blue' : 'orange'"
                >{{ item.grade }}</a-tag
              >
              <a-tag v-if="appealInfo(item)" :color="appealInfo(item)!.color">{{
                appealInfo(item)!.text
              }}</a-tag>
            </a-space>
          </div>
          <div class="performance-score">
            <strong>{{ item.totalScore }}</strong><small>综合得分</small>
          </div>
          <div class="performance-metrics">
            <span>准时率 {{ item.onTimeRate }}%</span>
            <span>质量合格 {{ item.qualityRate }}%</span>
            <span>单据匹配 {{ item.invoiceMatchRate }}%</span>
            <span>响应评分 {{ item.responseScore }}</span>
          </div>
          <p v-if="item.improvementAction" class="table-subtitle">
            改进建议：{{ item.improvementAction }}
          </p>
          <small class="table-subtitle">
            {{ item.reviewerName }} · {{ formatDate(item.createdAt) }}
          </small>
        </article>
      </div>
    </section>
  </div>

  <a-modal
    v-model:open="performanceOpen"
    title="绩效明细"
    :footer="null"
    width="min(680px, 100vw)"
  >
    <template v-if="selectedReview">
      <div class="perf-head">
        <div>
          <strong>{{ selectedReview.reviewPeriod }}</strong>
          <small>{{ selectedReview.reviewerName }} · 评审于 {{ formatDate(selectedReview.createdAt) }}</small>
        </div>
        <a-tag :color="selectedReview.totalScore >= 90 ? 'green' : selectedReview.totalScore >= 75 ? 'blue' : 'orange'"
          >{{ selectedReview.grade }} · {{ selectedReview.totalScore }} 分</a-tag
        >
      </div>
      <a-progress
        v-for="dim in performanceDims(selectedReview)"
        :key="dim.label"
        :percent="dim.value"
        :status="dim.value >= 90 ? 'success' : dim.value >= 75 ? 'active' : 'exception'"
        class="perf-dim"
      >
        <template #format>{{ dim.label }} {{ dim.value }}%</template>
      </a-progress>
      <a-alert
        v-if="selectedReview.improvementAction"
        type="info"
        show-icon
        class="perf-tip"
        :message="'改进建议：' + selectedReview.improvementAction"
      />
      <a-alert
        v-if="appealInfo(selectedReview)"
        :type="appealInfo(selectedReview)!.type"
        show-icon
        class="perf-tip"
        :message="appealInfo(selectedReview)!.text"
        :description="appealDescription(selectedReview)"
      />
      <a-button
        v-if="canAppeal(selectedReview)"
        type="primary"
        ghost
        block
        class="perf-tip"
        @click="openAppeal"
        >对本期评价发起申诉</a-button
      >
    </template>
    <a-divider style="margin: 16px 0 8px" />
    <h4 style="margin-bottom: 8px">历史评分</h4>
    <a-table
      size="small"
      row-key="id"
      :data-source="performanceReviews"
      :columns="performanceColumns"
      :pagination="false"
    />
  </a-modal>

  <a-modal
    v-model:open="appealOpen"
    title="绩效评价申诉"
    :confirm-loading="appealing"
    @ok="submitAppeal"
  >
    <a-alert
      type="info"
      show-icon
      class="perf-tip"
      message="提交后采购方将复核本期绩效，处理结果会以消息通知贵司。"
    />
    <a-textarea
      v-model:value="appealReason"
      :rows="4"
      placeholder="请填写申诉理由与依据，例如某批订单数据缺失、评分口径异议等"
      :maxlength="1000"
      show-count
    />
  </a-modal>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { message } from "ant-design-vue";
import { BellOutlined, CheckCircleOutlined, ClockCircleOutlined, ProfileOutlined, ReloadOutlined, RightOutlined, SafetyCertificateOutlined, SendOutlined, WarningOutlined } from "@ant-design/icons-vue";
import * as api from "../api";
import { usePortalStore } from "../store";
import { contractStatusText, daysLeft, deadlineText, docExpiryDays, formatDate, money } from "../utils/quote";

const store = usePortalStore(); const router = useRouter(); const loading = ref(false);
const inquiries = ref<api.PortalInquiry[]>([]); const documents = ref<api.PortalDocument[]>([]);
const performanceReviews = ref<api.PerformanceReview[]>([]);
const orders = ref<api.PortalOrderEntry[]>([]);
const finance = ref<api.FinanceSummary>({
  invoiceCount: 0,
  invoiceAmount: 0,
  invoiceApprovedAmount: 0,
  invoiceDifferenceAmount: 0,
  pendingInvoiceApprovals: 0,
  matchedInvoiceCount: 0,
  payableCount: 0,
  payableAmount: 0,
  paidAmount: 0,
  outstandingAmount: 0,
  overdueAmount: 0,
});
const risks = computed(() => {
  const list: {
    key: string;
    level: "high" | "normal";
    title: string;
    detail: string;
    to: string;
  }[] = [];
  if (Number(finance.value.overdueAmount) > 0) {
    list.push({
      key: "overdue",
      level: "high",
      title: "存在逾期未付款项",
      detail: `逾期未付 ${money(finance.value.overdueAmount)}，请及时与采购方对账。`,
      to: "/finance",
    });
  }
  const unacknowledged = orders.value.filter(
    (item) => item.contract && !item.contract.acknowledged,
  );
  if (unacknowledged.length > 0) {
    list.push({
      key: "ack",
      level: "normal",
      title: `${unacknowledged.length} 个中标项目待确认`,
      detail: "确认后采购方将正式下单。",
      to: "/orders",
    });
  }
  const pendingChanges = orders.value.filter((item) =>
    (item.changes || []).some(
      (change) =>
        !change.supplierResponse &&
        (change.status === "PENDING" || change.status === "APPROVED"),
    ),
  );
  if (pendingChanges.length > 0) {
    list.push({
      key: "change",
      level: "normal",
      title: `${pendingChanges.length} 个订单有变更待处理`,
      detail: "数量、单价或交期有变更，请及时同意或提出异议。",
      to: "/orders",
    });
  }
  const pendingAppeals = orders.value.filter((item) =>
    (item.receipts || []).some((receipt) => receipt.appealStatus === "PENDING"),
  );
  if (pendingAppeals.length > 0) {
    list.push({
      key: "appeal",
      level: "normal",
      title: `${pendingAppeals.length} 笔质检申诉处理中`,
      detail: "申诉已提交采购方，请耐心等待处理结果。",
      to: "/orders",
    });
  }
  const expiring = documents.value.filter((doc) => {
    const days = docExpiryDays(doc.validTo);
    return days !== null && days <= 90;
  });
  if (expiring.length > 0) {
    list.push({
      key: "doc",
      level: "normal",
      title: `${expiring.length} 份资质文件临期`,
      detail: "请在 90 天内更新并重新上传。",
      to: "/documents",
    });
  }
  return list.slice(0, 6);
});
const performanceOpen = ref(false);
const selectedReview = ref<api.PerformanceReview>();
const appealOpen = ref(false);
const appealReason = ref("");
const appealing = ref(false);
const pending = computed(() => inquiries.value.filter((i) => i.status === "OPEN" && i.quote?.status !== "SUBMITTED"));
const pendingCount = computed(() => pending.value.length);
const submittedCount = computed(() => inquiries.value.filter((i) => i.quote?.status === "SUBMITTED").length);
const orderCount = computed(() => orders.value.filter((item) => item.order).length);
const orderStatusText = (value?: string) => ({
  DRAFT: "待提交", ORDERED: "已下单", PARTIAL_RECEIVED: "部分收货",
  RECEIVED: "已收货", CLOSED: "已关闭", CANCELLED: "已取消",
} as Record<string, string>)[value || ""] || value || "未知";
const itemKey = (item: api.PortalOrderEntry) =>
  item.order?.id || item.contract?.id || item.inquiry?.id || "";
const admissionText = computed(() => ({ APPROVED: "已通过", REJECTED: "已退回", PENDING: "审核中" })[store.session?.supplier.admissionStatus || "PENDING"]);
const readiness = computed(() => [Boolean(store.session?.supplier.registeredAddress), documents.value.length > 0, store.session?.account.status === "ACTIVE", store.session?.supplier.admissionStatus === "APPROVED"].filter(Boolean).length * 25);
onMounted(load);
async function load() {
  loading.value = true;
  try {
    [inquiries.value, documents.value, performanceReviews.value, orders.value, finance.value] =
      await Promise.all([
        api.listInquiries(),
        api.listDocuments(),
        api.listPerformanceReviews(),
        api.listOrders(),
        api.getFinanceSummary(),
      ]);
  } catch (e) {
    message.error(e instanceof Error ? e.message : "加载失败");
  } finally {
    loading.value = false;
  }
}

const performanceColumns = [
  { title: "评审周期", dataIndex: "reviewPeriod" },
  { title: "综合得分", dataIndex: "totalScore", width: 90 },
  { title: "等级", dataIndex: "grade", width: 80 },
  { title: "评审人", dataIndex: "reviewerName", width: 110 },
];

function showPerformance(item: api.PerformanceReview) {
  selectedReview.value = item;
  performanceOpen.value = true;
}

function performanceDims(item: api.PerformanceReview) {
  return [
    { label: "准时交付", value: Number(item.onTimeRate) },
    { label: "质量合格", value: Number(item.qualityRate) },
    { label: "单据匹配", value: Number(item.invoiceMatchRate) },
    { label: "响应评分", value: Number(item.responseScore) },
  ];
}

function canAppeal(item?: api.PerformanceReview) {
  return (
    !!item &&
    (!item.appealStatus || item.appealStatus === "NONE") &&
    item.appealResolution !== "REOPENED"
  );
}

function appealInfo(item?: api.PerformanceReview) {
  if (!item) return undefined;
  if (item.appealStatus === "PENDING")
    return { text: "申诉审核中", color: "orange", type: "warning" };
  if (item.appealStatus === "DISMISSED")
    return { text: "申诉未成立", color: "red", type: "error" };
  if (item.appealStatus === "REOPENED")
    return { text: "已受理 · 重新核定中", color: "blue", type: "info" };
  if (!item.appealStatus || item.appealStatus === "NONE") {
    if (item.appealResolution === "REOPENED")
      return { text: "已重新核定", color: "green", type: "success" };
    if (item.appealResolution === "DISMISSED")
      return { text: "申诉已处理", color: "default", type: "info" };
  }
  return undefined;
}

function appealDescription(item?: api.PerformanceReview) {
  if (!item) return undefined;
  const lines: string[] = [];
  if (item.appealReason) lines.push("申诉理由：" + item.appealReason);
  if (item.appealReviewComment)
    lines.push("采购方意见：" + item.appealReviewComment);
  if (item.appealReviewedBy && item.appealReviewedAt)
    lines.push(
      "处理人：" + item.appealReviewedBy + " · " + formatDate(item.appealReviewedAt),
    );
  return lines.length ? lines.join("\n") : undefined;
}

function openAppeal() {
  appealReason.value = "";
  appealOpen.value = true;
}

async function submitAppeal() {
  if (!selectedReview.value) return;
  const reason = appealReason.value.trim();
  if (!reason) {
    message.warning("请填写申诉理由");
    return;
  }
  appealing.value = true;
  try {
    const updated = await api.appealPerformanceReview(
      selectedReview.value.id,
      reason,
    );
    const index = performanceReviews.value.findIndex(
      (item) => item.id === updated.id,
    );
    if (index >= 0) performanceReviews.value[index] = updated;
    selectedReview.value = updated;
    appealOpen.value = false;
    message.success("申诉已提交，采购方将尽快处理");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "申诉提交失败");
  } finally {
    appealing.value = false;
  }
}
</script>

<style scoped>
.risk-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 10px;
}
.risk-row {
  display: flex;
  align-items: center;
  gap: 12px;
  text-align: left;
  padding: 12px 14px;
  border: 1px solid #e7ece9;
  border-radius: 8px;
  background: #fafbfa;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}
.risk-row:hover {
  border-color: #c9d6d0;
  background: #f3f7f4;
}
.risk-icon {
  font-size: 18px;
  color: #68756f;
  flex: none;
}
.risk-icon.high {
  color: #cf1322;
}
.risk-main {
  flex: 1;
  min-width: 0;
}
.risk-main b {
  display: block;
  font-size: 14px;
}
.risk-main small {
  color: #75817c;
  display: block;
  overflow-wrap: anywhere;
}
</style>
