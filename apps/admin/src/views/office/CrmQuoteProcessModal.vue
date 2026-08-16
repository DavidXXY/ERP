<template>
  <a-modal
    v-model:open="openProxy"
    title="报价审批"
    width="760px"
    :confirm-loading="saving"
    @ok="handleOk"
  >
    <a-alert
      v-if="approval"
      class="section-alert"
      type="info"
      :message="`${approval.code} · ${approval.customerName} · 报价金额（含税，元）${formatMoney(approval.amount ?? 0)}`"
      :description="approval.desc || '未填写服务范围'"
    />
    <a-descriptions
      v-if="approval"
      size="small"
      bordered
      :column="2"
      class="approval-context"
    >
      <a-descriptions-item label="客户">{{
        approval.customerName || "-"
      }}</a-descriptions-item>
      <a-descriptions-item label="报价金额（含税，元）">{{
        formatMoney(approval.amount ?? 0)
      }}</a-descriptions-item>
      <a-descriptions-item label="付款方式/节点" :span="2">{{
        approval.paymentNodes || "-"
      }}</a-descriptions-item>
      <a-descriptions-item label="成本预算（含税，元）">{{
        approval.budgetAmount != null ? formatMoney(approval.budgetAmount) : "-"
      }}</a-descriptions-item>
      <a-descriptions-item label="毛利率">{{
        approval.grossMarginRate != null
          ? `${Number(approval.grossMarginRate).toFixed(1)}%`
          : "-"
      }}</a-descriptions-item>
    </a-descriptions>
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-form-item label="审批结论" name="decision"
        ><a-radio-group v-model:value="form.decision" button-style="solid"
          ><a-radio-button value="APPROVED">通过</a-radio-button
          ><a-radio-button value="REJECTED">驳回</a-radio-button></a-radio-group
        ></a-form-item
      >
      <a-form-item label="审批意见" name="comment"
        ><a-textarea v-model:value="form.comment" :rows="3"
      /></a-form-item>
      <a-form-item label="审批人" name="approverName"
        ><a-input v-model:value="form.approverName"
      /></a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import { useAuthStore } from "@/stores/auth";
import { formatMoney } from "./approvalFormat";
import type { MergedQuoteItem } from "./approvalItemTypes";
import type { ApprovalProcessPayload } from "./composables/useApprovalActions";

const props = defineProps<{
  open: boolean;
  saving: boolean;
  approval: MergedQuoteItem | null;
}>();
const emit = defineEmits<{
  (e: "update:open", v: boolean): void;
  (e: "submit", v: ApprovalProcessPayload): void;
}>();

const auth = useAuthStore();
const formRef = ref();

const openProxy = computed({
  get: () => props.open,
  set: (v: boolean) => emit("update:open", v),
});

const form = reactive<ApprovalProcessPayload>({
  decision: "APPROVED",
  comment: "同意",
  approverName: "",
});

const rules = {
  decision: [{ required: true }],
  comment: [{ required: true }],
  approverName: [{ required: true }],
};

watch(
  () => props.open,
  (isOpen) => {
    if (isOpen) {
      Object.assign(form, {
        decision: "APPROVED",
        comment: "同意",
        approverName: auth.user?.displayName || "",
      });
    }
  },
);

async function handleOk() {
  try {
    await formRef.value?.validate();
  } catch {
    return;
  }
  emit("submit", { ...form });
}
</script>

<style scoped>
.approval-context {
  margin: 12px 0 16px;
}
</style>
