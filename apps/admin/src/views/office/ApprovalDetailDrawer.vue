<template>
  <a-drawer
    v-model:open="openProxy"
    title="审批详情"
    width="min(760px, 100vw)"
  >
    <template v-if="approval">
      <a-space direction="vertical" class="detail-stack" size="middle">
        <a-descriptions bordered size="small" :column="2" title="审批单信息">
          <a-descriptions-item label="审批编号">{{
            approval.code
          }}</a-descriptions-item>
          <a-descriptions-item label="审批类型">{{
            approvalTypeLabel(approval.approvalType)
          }}</a-descriptions-item>
          <a-descriptions-item label="标题" :span="2">{{
            approval.title
          }}</a-descriptions-item>
          <a-descriptions-item label="来源单号">{{
            approval.sourceNo || "-"
          }}</a-descriptions-item>
          <a-descriptions-item label="金额（元，税价随来源单据）">{{
            formatMoney(approval.amount)
          }}</a-descriptions-item>
          <a-descriptions-item label="申请人">{{
            approval.applicantName || "-"
          }}</a-descriptions-item>
          <a-descriptions-item label="状态"
            ><a-tag :color="approvalStatusColor(approval.status)">{{
              approvalStatusLabel(approval.status)
            }}</a-tag></a-descriptions-item
          >
          <a-descriptions-item label="部门/业务"
            >{{ approval.departmentName || "-" }} /
            {{ approval.businessType || "-" }}</a-descriptions-item
          >
          <a-descriptions-item label="项目编码">{{
            approval.projectCode || "-"
          }}</a-descriptions-item>
          <a-descriptions-item label="申请内容" :span="2">{{
            approval.content || "-"
          }}</a-descriptions-item>
        </a-descriptions>

        <a-descriptions
          v-if="isExpenseApproval(approval)"
          bordered
          size="small"
          :column="2"
          title="报销单完整内容"
        >
          <a-descriptions-item label="报销单号">{{
            expenseDetail(approval)?.code || approval.sourceNo || "-"
          }}</a-descriptions-item>
          <a-descriptions-item label="报销人">{{
            expenseDetail(approval)?.claimantName ||
            approval.applicantName ||
            "-"
          }}</a-descriptions-item>
          <a-descriptions-item label="费用类型">{{
            expenseDetail(approval)
              ? expenseTypeLabel(
                  expenseDetail(approval)!.expenseType as ExpenseType,
                )
              : approvalBusinessTypeLabel(approval.businessType)
          }}</a-descriptions-item>
          <a-descriptions-item label="发生日期">{{
            expenseDetail(approval)?.expenseDate || "-"
          }}</a-descriptions-item>
          <a-descriptions-item label="绑定项目">{{
            expenseDetail(approval)?.projectCode || "-"
          }}</a-descriptions-item>
          <a-descriptions-item label="绑定工单">{{
            expenseDetail(approval)?.workOrderCode || "-"
          }}</a-descriptions-item>
          <a-descriptions-item label="报销金额（含税，元）">{{
            formatMoney(
              Number(expenseDetail(approval)?.amount ?? approval.amount),
            )
          }}</a-descriptions-item>
          <a-descriptions-item label="报销状态">{{
            expenseDetail(approval)?.status
              ? expenseStatusLabel(
                  expenseDetail(approval)!.status as ExpenseStatus,
                )
              : "-"
          }}</a-descriptions-item>
          <a-descriptions-item label="费用说明" :span="2">{{
            expenseDetail(approval)?.description ||
            approval.content ||
            "-"
          }}</a-descriptions-item>
        </a-descriptions>
        <a-table
          v-if="
            isExpenseApproval(approval) &&
            expenseDetail(approval)?.lines?.length
          "
          size="small"
          :columns="expenseLineColumns"
          :data-source="expenseDetail(approval)?.lines"
          :pagination="false"
          :row-key="(line: any) => line.id || line.lineNo"
        >
          <template #bodyCell="{ column, record: line }">
            <template v-if="column.key === 'expenseType'">{{
              expenseTypeLabel(line.expenseType as ExpenseType)
            }}</template>
            <template v-else-if="column.key === 'amount'">{{
              formatMoney(Number(line.amount || 0))
            }}</template>
            <template v-else-if="column.key === 'invoice'">{{
              line.invoiceFileName || "-"
            }}</template>
          </template>
        </a-table>

        <a-descriptions
          v-else-if="approval.sourceDetail"
          bordered
          size="small"
          :column="2"
          title="业务单据内容"
        >
          <a-descriptions-item
            v-for="line in sourceDetailLines(approval)"
            :key="line.label"
            :label="line.label"
            :span="line.span || 1"
            >{{ line.value }}</a-descriptions-item
          >
        </a-descriptions>

        <section
          v-if="sealAttachments(approval).length"
          class="approval-attachments"
        >
          <h4>用印附件</h4>
          <a-list
            size="small"
            bordered
            :data-source="sealAttachments(approval)"
          >
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta
                  :title="item.fileName"
                  :description="formatFileSize(item.sizeBytes)"
                />
                <template #actions>
                  <a-button
                    type="link"
                    size="small"
                    @click="previewDocument(item)"
                    >预览</a-button
                  >
                  <a-button
                    type="link"
                    size="small"
                    @click="downloadDocument(item)"
                    >下载</a-button
                  >
                </template>
              </a-list-item>
            </template>
          </a-list>
        </section>

        <a-card size="small" title="审批流节点">
          <ApprovalProgressFlow
            v-if="approval.nodes?.length"
            :steps="officeApprovalProgressSteps(approval)"
          />
          <a-empty v-else description="暂无审批节点" />
        </a-card>

        <a-card size="small" title="处理记录">
          <a-timeline v-if="approval.actions?.length">
            <a-timeline-item
              v-for="action in approval.actions"
              :key="action.id"
              :color="approvalActionColor(action.decision)"
            >
              {{ approvalStatusLabel(action.decision) }} ·
              {{ action.operatorName || "-" }}
              <span class="table-subtitle"
                >{{ action.comment || "-" }} ·
                {{ action.createdAt?.slice(0, 16).replace("T", " ") }}</span
              >
            </a-timeline-item>
          </a-timeline>
          <a-empty v-else description="暂无处理记录" />
        </a-card>

        <a-card
          v-if="
            auth.can('office:approval:create') &&
            isCurrentApplicant(approval) &&
            ['PENDING', 'REJECTED'].includes(approval.status)
          "
          size="small"
          title="申请人操作"
        >
          <a-popconfirm
            v-if="approval.status === 'PENDING'"
            title="确认撤回该审批？"
            @confirm="emit('withdraw')"
          >
            <a-button danger :loading="saving">撤回审批</a-button>
          </a-popconfirm>
          <a-button
            v-else
            type="primary"
            :loading="saving"
            @click="emit('resubmit')"
            >按最新规则重新提交</a-button
          >
        </a-card>

        <a-card
          v-if="
            approval.status === 'PENDING' && auth.can('office:approval:process')
          "
          size="small"
          title="审批处理"
        >
          <a-form
            ref="detailProcessFormRef"
            :model="detailProcessForm"
            :rules="processRules"
            layout="vertical"
          >
            <a-form-item label="审批结论" name="decision"
              ><a-radio-group
                v-model:value="detailProcessForm.decision"
                button-style="solid"
                ><a-radio-button value="APPROVED">通过</a-radio-button
                ><a-radio-button value="REJECTED"
                  >驳回</a-radio-button
                ></a-radio-group
              ></a-form-item
            >
            <a-form-item label="审批意见" name="comment"
              ><a-textarea
                v-model:value="detailProcessForm.comment"
                :rows="3"
            /></a-form-item>
            <a-form-item label="审批人" name="approverName"
              ><a-input v-model:value="detailProcessForm.approverName"
            /></a-form-item>
            <a-space>
              <a-button
                type="primary"
                :loading="saving"
                @click="handleDetailProcess"
                >提交审批</a-button
              >
              <a-button @click="emit('open-process', approval)"
                >弹窗处理</a-button
              >
            </a-space>
          </a-form>
        </a-card>
      </a-space>
    </template>
  </a-drawer>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import { useAuthStore } from "@/stores/auth";
import {
  downloadDocument,
  previewDocument,
  type Approval,
  type ApprovalRuntimeNode,
  type DocumentRecord,
  type Expense,
  type ExpenseStatus,
  type ExpenseType,
} from "@/api/office";
import ApprovalProgressFlow, {
  type ApprovalProgressStep,
} from "@/components/ApprovalProgressFlow.vue";
import { approvalStatusColor, approvalStatusLabel } from "./approvalStatusMeta";
import {
  approvalActionColor,
  approvalBusinessTypeLabel,
  approvalTypeLabel,
  expenseStatusLabel,
  expenseTypeLabel,
  formatMoney,
} from "./approvalFormat";
import type { ApprovalProcessPayload } from "./composables/useApprovalActions";

const props = defineProps<{
  open: boolean;
  saving: boolean;
  approval: any;
}>();
const emit = defineEmits<{
  (e: "update:open", v: boolean): void;
  (e: "process", v: ApprovalProcessPayload): void;
  (e: "withdraw"): void;
  (e: "resubmit"): void;
  (e: "open-process", v: any): void;
}>();

const auth = useAuthStore();
const detailProcessFormRef = ref();

const openProxy = computed({
  get: () => props.open,
  set: (v: boolean) => emit("update:open", v),
});

const detailProcessForm = reactive<ApprovalProcessPayload>({
  decision: "APPROVED",
  comment: "同意",
  approverName: "",
});

const processRules = {
  decision: [{ required: true }],
  comment: [{ required: true }],
  approverName: [{ required: true }],
};

const expenseLineColumns = [
  { title: "序号", dataIndex: "lineNo", width: 70 },
  { title: "费用类型", key: "expenseType", width: 100 },
  { title: "发生日期", dataIndex: "expenseDate", width: 110 },
  { title: "金额（元，税价随来源单据）", key: "amount", width: 230 },
  { title: "说明", dataIndex: "description" },
  { title: "发票", key: "invoice", width: 180 },
];

watch(
  () => props.open,
  (isOpen) => {
    if (isOpen) {
      Object.assign(detailProcessForm, {
        decision: "APPROVED",
        comment: "同意",
        approverName: auth.user?.displayName || "",
      });
    }
  },
);

async function handleDetailProcess() {
  await detailProcessFormRef.value?.validate();
  emit("process", { ...detailProcessForm });
}

function isExpenseApproval(record: any) {
  return record.approvalType === "EXPENSE";
}

function expenseDetail(record: any) {
  return record.sourceDetail as Expense | null | undefined;
}

function sealAttachments(record: any): DocumentRecord[] {
  return record.approvalType === "SEAL"
    ? record.sourceDetail?.attachments || []
    : [];
}

function formatFileSize(value?: number) {
  const bytes = Number(value || 0);
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

function sourceDetailLines(record: any) {
  const detail = record.sourceDetail || {};
  const lines = Object.entries(detail)
    .filter(
      ([key, value]) =>
        value !== null &&
        value !== undefined &&
        value !== "" &&
        ![
          "id",
          "applicantId",
          "projectId",
          "approvalRequestId",
          "createdAt",
        ].includes(key) &&
        typeof value !== "object",
    )
    .map(([key, value]) => ({
      label: sourceDetailLabel(key),
      value: sourceDetailValue(key, value),
      span: [
        "description",
        "purpose",
        "documentPurpose",
        "acceptanceNote",
      ].includes(key)
        ? 2
        : 1,
    }));

  return normalizeDescriptionSpans(lines);
}

function normalizeDescriptionSpans<T extends { span: number }>(lines: T[]) {
  let pendingSingleIndex = -1;

  lines.forEach((line, index) => {
    if (line.span === 2) {
      if (pendingSingleIndex >= 0) lines[pendingSingleIndex].span = 2;
      pendingSingleIndex = -1;
      return;
    }

    pendingSingleIndex = pendingSingleIndex >= 0 ? -1 : index;
  });

  if (pendingSingleIndex >= 0) lines[pendingSingleIndex].span = 2;
  return lines;
}

function sourceDetailLabel(key: string) {
  return (
    (
      {
        code: "单号",
        claimantName: "申请人",
        applicantName: "申请人",
        departmentName: "部门",
        supplierName: "服务商",
        projectCode: "项目",
        workOrderCode: "工单",
        serviceType: "服务类型",
        amount: "金额（元，税价随来源单据）",
        plannedDate: "计划日期",
        status: "状态",
        description: "说明",
        acceptanceNote: "验收说明",
        destination: "目的地",
        purpose: "出差事由",
        transportType: "交通方式",
        startDate: "开始日期",
        endDate: "结束日期",
        travelDays: "出差天数",
        estimatedAmount: "预算金额（含税，元）",
        companionNames: "同行人员",
        sealType: "印章类型",
        documentName: "文件名称",
        documentPurpose: "用印用途",
        counterparty: "对方单位",
        copyCount: "文件份数",
        useDate: "用印日期",
        takeOut: "是否外带",
        expectedReturnDate: "预计归还日期",
        returnedAt: "实际归还时间",
      } as Record<string, string>
    )[key] || key
  );
}

function sourceDetailValue(key: string, value: unknown) {
  if (typeof value === "boolean") return value ? "是" : "否";
  if (key === "returnedAt") {
    return new Intl.DateTimeFormat("zh-CN", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
    }).format(new Date(String(value)));
  }
  if (key === "status") {
    return (
      (
        {
          COMPLETED: "已完成",
          CANCELLED: "已取消",
        } as Record<string, string>
      )[String(value)] || approvalStatusLabel(String(value))
    );
  }
  return String(value);
}

function officeApprovalProgressSteps(item: Approval): ApprovalProgressStep[] {
  const steps: ApprovalProgressStep[] = [
    {
      key: "start",
      personName: item.applicantName || "发起人",
      title: "发起申请",
      time: item.createdAt,
      note: item.title || item.content,
      state: "done",
    },
  ];
  (item.nodes || []).forEach((node: ApprovalRuntimeNode) => {
    steps.push({
      key: node.id,
      personName: node.approverName || node.assigneeName || "当前审批人",
      title:
        node.nodeStatus === "APPROVED"
          ? "已同意"
          : node.nodeStatus === "REJECTED"
            ? approvalStatusLabel("REJECTED")
            : node.nodeStatus === "SKIPPED"
              ? "已跳过"
              : approvalStatusLabel("PENDING"),
      time: node.completedAt || node.dueAt,
      note: node.approvalComment || node.conditionText || node.sourceValue,
      state:
        node.nodeStatus === "APPROVED"
          ? "done"
          : node.nodeStatus === "REJECTED"
            ? "rejected"
            : node.nodeStatus === "SKIPPED"
              ? "skipped"
              : "pending",
    });
  });
  return steps;
}

function isCurrentApplicant(item: any) {
  return Boolean(
    item?.applicantName && item.applicantName === auth.user?.displayName,
  );
}
</script>

<style scoped>
.detail-stack {
  width: 100%;
}
</style>
