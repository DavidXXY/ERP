<template>
  <a-modal
    v-model:open="openProxy"
    title="处理审批"
    width="720px"
    :confirm-loading="saving"
    @ok="handleOk"
  >
    <a-alert
      v-if="approval"
      class="section-alert"
      type="info"
      :message="`${approval.code} · ${approval.title} · 金额（元，税价随来源单据）${formatMoney(approval.amount)}`"
      :description="approval.content || '未填写申请内容'"
    />
    <a-descriptions
      v-if="approval"
      size="small"
      bordered
      :column="2"
      class="approval-context"
    >
      <a-descriptions-item label="审批类型">{{
        approvalTypeLabel(approval.approvalType)
      }}</a-descriptions-item>
      <a-descriptions-item label="来源单号">{{
        approval.sourceNo || "-"
      }}</a-descriptions-item>
      <a-descriptions-item label="申请人">{{
        approval.applicantName
      }}</a-descriptions-item>
      <a-descriptions-item label="部门/业务"
        >{{ approval.departmentName || "-" }} /
        {{ approval.businessType || "-" }}</a-descriptions-item
      >
      <a-descriptions-item label="项目编码">{{
        approval.projectCode || "-"
      }}</a-descriptions-item>
      <a-descriptions-item label="客户等级">{{
        approval.customerLevel || "-"
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
import { approvalTypeLabel, formatMoney } from "./approvalFormat";
import type { Approval } from "@/api/office";
import type { ApprovalProcessPayload } from "./composables/useApprovalActions";

const props = defineProps<{
  open: boolean;
  saving: boolean;
  approval: Approval | null;
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
