<template>
  <BusinessDetailPage
    :title="approval?.title"
    :code="approval?.code"
    :subtitle="
      approval
        ? `${typeLabel(approval.approvalType)} · 申请人 ${approval.applicantName}`
        : ''
    "
    :loading="loading"
    back-to="/workbench/todos?tab=approvals"
    :status-label="statusLabel(approval?.status)"
    :status-color="
      approval?.status === 'APPROVED'
        ? 'green'
        : approval?.status === 'REJECTED'
          ? 'red'
          : 'blue'
    "
    :risk-label="slaRisk"
    :risk-color="slaRisk === '已超时' ? 'red' : 'orange'"
    :stats="metrics"
    @refresh="loadData"
  >
    <template #actions>
      <a-button v-if="sourceRoute" @click="router.push(sourceRoute)"
        >查看原业务单据</a-button
      >
      <a-button
        v-if="
          approval?.status === 'PENDING' && auth.can('office:approval:process')
        "
        type="primary"
        @click="processOpen = true"
        >审批处理</a-button
      >
    </template>
    <template #relations>
      <a-steps size="small" :current="currentStep" responsive>
        <a-step title="发起申请" :description="approval?.applicantName" />
        <a-step
          v-for="node in approval?.nodes || []"
          :key="node.id"
          :title="`第 ${node.stepNo} 级审批`"
          :description="node.assigneeName || node.approverName || '待认领'"
          :status="nodeStatus(node.nodeStatus)"
        />
        <a-step
          title="业务回写"
          :description="
            approval?.status === 'APPROVED' ? '已生效' : '等待审批完成'
          "
        />
      </a-steps>
    </template>
    <a-card v-if="approval" :bordered="false">
      <a-tabs>
        <a-tab-pane key="overview" tab="审批单据">
          <a-row :gutter="[16, 16]">
            <a-col :xs="24" :xl="13">
              <a-descriptions
                bordered
                :column="{ xs: 1, md: 2 }"
                size="small"
                title="申请信息"
              >
                <a-descriptions-item label="审批类型">{{
                  typeLabel(approval.approvalType)
                }}</a-descriptions-item
                ><a-descriptions-item label="来源单号">{{
                  approval.sourceNo || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="申请人">{{
                  approval.applicantName
                }}</a-descriptions-item
                ><a-descriptions-item label="金额">{{
                  money(approval.amount)
                }}</a-descriptions-item>
                <a-descriptions-item label="部门/业务"
                  >{{ approval.departmentName || "-" }} /
                  {{ approval.businessType || "-" }}</a-descriptions-item
                ><a-descriptions-item label="项目编码">{{
                  approval.projectCode || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="申请内容" :span="2">{{
                  approval.content || "-"
                }}</a-descriptions-item>
              </a-descriptions>
            </a-col>
            <a-col :xs="24" :xl="11">
              <a-descriptions
                bordered
                :column="1"
                size="small"
                title="流程与规则"
              >
                <a-descriptions-item label="审批模式">{{
                  approval.approvalMode || "-"
                }}</a-descriptions-item
                ><a-descriptions-item label="当前审批人">{{
                  approval.currentApproverName || approval.approverName || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="当前节点"
                  >{{ approval.currentStep || 0 }} /
                  {{
                    approval.totalSteps || approval.nodes?.length || 0
                  }}</a-descriptions-item
                ><a-descriptions-item label="命中规则">{{
                  approval.matchedRuleText || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="流程版本">{{
                  approval.approvalConfigVersion
                    ? `V${approval.approvalConfigVersion}`
                    : "-"
                }}</a-descriptions-item
                ><a-descriptions-item label="发起时间">{{
                  dateTime(approval.createdAt)
                }}</a-descriptions-item>
              </a-descriptions>
            </a-col>
          </a-row>
        </a-tab-pane>
        <a-tab-pane key="source" tab="业务单据快照">
          <a-descriptions
            v-if="sourceLines.length"
            bordered
            :column="{ xs: 1, md: 2 }"
            size="small"
          >
            <a-descriptions-item
              v-for="line in sourceLines"
              :key="line.label"
              :label="line.label"
              :span="line.span"
              >{{ line.value }}</a-descriptions-item
            >
          </a-descriptions>
          <a-empty v-else description="该审批未保存业务快照" />
        </a-tab-pane>
        <a-tab-pane
          key="nodes"
          :tab="`流程节点 (${approval.nodes?.length || 0})`"
        >
          <a-table
            :columns="nodeColumns"
            :data-source="approval.nodes || []"
            row-key="id"
            :pagination="false"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'step'"
                >第 {{ record.stepNo }} 级</template
              ><template v-else-if="column.key === 'assignee'">{{
                record.assigneeName || record.approverName || "-"
              }}</template>
              <template v-else-if="column.key === 'status'"
                ><a-tag
                  :color="
                    record.nodeStatus === 'APPROVED' ||
                    record.nodeStatus === 'COMPLETED'
                      ? 'green'
                      : record.nodeStatus === 'REJECTED'
                        ? 'red'
                        : 'blue'
                  "
                  >{{ nodeLabel(record.nodeStatus) }}</a-tag
                ></template
              >
              <template v-else-if="column.key === 'sla'"
                >{{ record.slaHours ? `${record.slaHours} 小时` : "-"
                }}<span class="sub">{{
                  record.dueAt ? `截止 ${dateTime(record.dueAt)}` : ""
                }}</span></template
              >
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane
          key="audit"
          :tab="`处理与审计 (${approval.actions.length})`"
        >
          <a-timeline v-if="approval.actions.length">
            <a-timeline-item
              v-for="action in approval.actions"
              :key="action.id"
              :color="
                action.decision === 'APPROVED'
                  ? 'green'
                  : action.decision === 'REJECTED'
                    ? 'red'
                    : 'blue'
              "
              ><strong>{{
                actionTypeLabel(action.actionType, action.decision)
              }}</strong>
              · {{ action.operatorName }}
              <p>{{ action.comment || "无处理意见" }}</p>
              <small
                >节点 {{ action.stepNo || "-" }} ·
                {{ dateTime(action.createdAt) }}</small
              ></a-timeline-item
            > </a-timeline
          ><a-empty v-else description="暂无处理记录" />
        </a-tab-pane>
      </a-tabs>
    </a-card>
    <a-modal
      v-model:open="processOpen"
      title="审批处理"
      :confirm-loading="saving"
      @ok="submitProcess"
    >
      <a-form layout="vertical">
        <a-form-item label="审批结论" required
          ><a-radio-group v-model:value="form.decision" button-style="solid"
            ><a-radio-button value="APPROVED">通过</a-radio-button
            ><a-radio-button value="REJECTED"
              >驳回</a-radio-button
            ></a-radio-group
          ></a-form-item
        >
        <a-form-item label="审批意见" required
          ><a-textarea v-model:value="form.comment" :rows="4"
        /></a-form-item>
        <a-form-item label="审批人" required
          ><a-input v-model:value="form.approverName"
        /></a-form-item>
      </a-form>
    </a-modal>
  </BusinessDetailPage>
</template>
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { message } from "ant-design-vue";
import BusinessDetailPage, {
  type DetailMetric,
} from "@/components/BusinessDetailPage.vue";
import {
  listApprovals,
  processApproval,
  type Approval,
  type ApprovalRuntimeNode,
} from "@/api/office";
import { useAuthStore } from "@/stores/auth";
const route = useRoute(),
  router = useRouter(),
  auth = useAuthStore(),
  loading = ref(false),
  saving = ref(false),
  processOpen = ref(false),
  approval = ref<Approval | null>(null),
  form = reactive<{
    decision: "APPROVED" | "REJECTED";
    comment: string;
    approverName: string;
  }>({
    decision: "APPROVED",
    comment: "同意",
    approverName: auth.user?.displayName || "",
  });
const metrics = computed<DetailMetric[]>(() =>
  approval.value
    ? [
        { label: "申请金额", value: money(approval.value.amount) },
        {
          label: "流程进度",
          value: `${approval.value.currentStep || 0}/${approval.value.totalSteps || approval.value.nodes?.length || 0}`,
        },
        {
          label: "当前审批人",
          value:
            approval.value.currentApproverName ||
            approval.value.approverName ||
            "待认领",
        },
        { label: "发起时间", value: dateTime(approval.value.createdAt) },
        { label: "处理状态", value: statusLabel(approval.value.status) },
      ]
    : [],
);
const currentStep = computed(() =>
  approval.value?.status === "APPROVED"
    ? (approval.value.nodes?.length || 0) + 1
    : Math.max(0, approval.value?.currentStep || 0),
);
const now = Date.now();
const slaRisk = computed(() => {
  const due = approval.value?.nodes?.find((n) =>
    ["PENDING", "ACTIVE"].includes(n.nodeStatus),
  )?.dueAt;
  if (!due) return "";
  return new Date(due).getTime() < now ? "已超时" : "临近时限";
});
const sourceRoute = computed(() => {
  const a = approval.value;
  if (!a) return "";
  const source = a.sourceDetail as any;
  if (a.approvalType === "PAYMENT" && source?.id)
    return `/finance/payment-applications/${source.id}`;
  if (a.approvalType === "PURCHASE" && source?.id)
    return `/procurement/orders/${source.id}`;
  if (a.approvalType === "CONTRACT" && source?.id)
    return `/crm/contracts/${source.id}`;
  if (a.approvalType === "QUOTE" && source?.id)
    return `/crm/quotes/${source.id}`;
  return "";
});
const sourceLines = computed(() => {
  const source = approval.value?.sourceDetail as any;
  if (!source) return [];
  const labels: Record<string, string> = {
    code: "单据编号",
    supplierName: "供应商",
    customerName: "客户",
    projectName: "项目",
    claimantName: "报销人",
    amount: "金额",
    description: "说明",
    serviceType: "服务类型",
    plannedDate: "计划日期",
    expenseDate: "发生日期",
    status: "业务状态",
    purpose: "用途",
    requestedAmount: "申请金额",
  };
  return Object.entries(source)
    .filter(
      ([key, value]) =>
        labels[key] && value != null && typeof value !== "object",
    )
    .map(([key, value]) => ({
      label: labels[key],
      value: key.toLowerCase().includes("amount")
        ? money(Number(value))
        : String(value),
      span: ["description", "purpose"].includes(key) ? 2 : 1,
    }));
});
const nodeColumns = [
  { title: "节点", key: "step", width: 100 },
  { title: "审批人", key: "assignee", width: 160 },
  { title: "审批策略", dataIndex: "stepPolicy", width: 130 },
  { title: "时效", key: "sla", width: 180 },
  { title: "状态", key: "status", width: 110 },
  { title: "审批意见", dataIndex: "approvalComment" },
];
onMounted(loadData);
async function loadData() {
  loading.value = true;
  try {
    const list = await listApprovals();
    approval.value = list.find((i) => i.id === String(route.params.id)) || null;
  } catch (e) {
    message.error(e instanceof Error ? e.message : "审批详情加载失败");
  } finally {
    loading.value = false;
  }
}
async function submitProcess() {
  if (!approval.value || !form.comment.trim() || !form.approverName.trim()) {
    message.warning("请填写审批意见和审批人");
    return;
  }
  saving.value = true;
  try {
    approval.value = await processApproval(approval.value.id, { ...form });
    processOpen.value = false;
    message.success(form.decision === "APPROVED" ? "审批已通过" : "审批已驳回");
  } catch (e) {
    message.error(e instanceof Error ? e.message : "审批处理失败");
  } finally {
    saving.value = false;
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
function typeLabel(v?: string) {
  return (
    (
      {
        QUOTE: "报价审批",
        CONTRACT: "合同审批",
        PURCHASE: "采购审批",
        OUTSOURCE: "外包审批",
        EXPENSE: "费用报销",
        PAYMENT: "付款审批",
        SEAL: "用印审批",
        LEAVE: "请假审批",
        TRAVEL: "出差审批",
        OTHER: "通用审批",
      } as Record<string, string>
    )[v || ""] || ""
  );
}
function statusLabel(v?: string) {
  return (
    (
      { PENDING: "审批中", APPROVED: "已通过", REJECTED: "已驳回" } as Record<
        string,
        string
      >
    )[v || ""] || ""
  );
}
function nodeLabel(v: string) {
  return (
    (
      {
        PENDING: "待处理",
        ACTIVE: "处理中",
        APPROVED: "已通过",
        COMPLETED: "已完成",
        REJECTED: "已驳回",
        SKIPPED: "已跳过",
      } as Record<string, string>
    )[v] || v
  );
}
function nodeStatus(v: string) {
  return (
    ["APPROVED", "COMPLETED"].includes(v)
      ? "finish"
      : v === "REJECTED"
        ? "error"
        : "process"
  ) as "finish" | "error" | "process";
}
function actionTypeLabel(t: string | undefined, d: string) {
  return (
    (
      {
        TRANSFER: "转交",
        ADD_SIGN: "加签",
        RETURN: "退回",
        WITHDRAW: "撤回",
        RESUBMIT: "重新提交",
      } as Record<string, string>
    )[t || ""] || statusLabel(d)
  );
}
</script>
<style scoped>
.sub {
  display: block;
  color: #8c96a5;
  font-size: 12px;
}
.ant-timeline p {
  margin: 5px 0;
  color: #657186;
}
.ant-timeline small {
  color: #9aa4b2;
}
</style>
