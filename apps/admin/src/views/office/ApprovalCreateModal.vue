<template>
  <a-modal
    v-model:open="openProxy"
    title="发起通用审批"
    width="680px"
    :confirm-loading="saving"
    @ok="handleOk"
  >
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-form-item label="标题" name="title"
        ><a-input v-model:value="form.title"
      /></a-form-item>
      <a-row :gutter="16"
        ><a-col :span="12"
          ><a-form-item label="来源单号"
            ><a-input v-model:value="form.sourceNo" /></a-form-item></a-col
        ><a-col :span="12"
          ><a-form-item label="金额（元，税价随来源单据）"
            ><a-input-number
              v-model:value="form.amount"
              :min="0"
              :precision="2"
              class="full-input" /></a-form-item></a-col
      ></a-row>
      <a-form-item label="申请人" name="applicantName"
        ><a-input v-model:value="form.applicantName"
      /></a-form-item>
      <a-row :gutter="16">
        <a-col :span="12"
          ><a-form-item label="部门/组织"
            ><a-input v-model:value="form.departmentName" /></a-form-item
        ></a-col>
        <a-col :span="12"
          ><a-form-item label="业务类型"
            ><a-input v-model:value="form.businessType" /></a-form-item
        ></a-col>
        <a-col :span="12"
          ><a-form-item label="项目编码"
            ><a-input v-model:value="form.projectCode" /></a-form-item
        ></a-col>
        <a-col :span="12"
          ><a-form-item label="客户等级"
            ><a-input v-model:value="form.customerLevel" /></a-form-item
        ></a-col>
      </a-row>
      <a-form-item label="申请内容" name="content"
        ><a-textarea v-model:value="form.content" :rows="3"
      /></a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import { useAuthStore } from "@/stores/auth";
import type { ApprovalType } from "@/api/office";
import type { ApprovalCreatePayload } from "./composables/useApprovalActions";

const props = defineProps<{ open: boolean; saving: boolean }>();
const emit = defineEmits<{
  (e: "update:open", v: boolean): void;
  (e: "submit", v: ApprovalCreatePayload): void;
}>();

const auth = useAuthStore();
const formRef = ref();

const openProxy = computed({
  get: () => props.open,
  set: (v: boolean) => emit("update:open", v),
});

const form = reactive<ApprovalCreatePayload>({
  code: "",
  approvalType: "OTHER" as ApprovalType,
  title: "",
  sourceNo: "",
  amount: 0,
  applicantName: "",
  content: "",
  departmentName: "",
  businessType: "",
  projectCode: "",
  supplierRisk: "",
  customerLevel: "",
});

const rules = {
  title: [{ required: true }],
  applicantName: [{ required: true }],
  content: [{ required: true }],
};

function generateCode(prefix: string) {
  const d = new Date();
  const ds = `${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, "0")}${String(d.getDate()).padStart(2, "0")}`;
  return `${prefix}-${ds}-${String(d.getHours()).padStart(2, "0")}${String(d.getMinutes()).padStart(2, "0")}`;
}

watch(
  () => props.open,
  (isOpen) => {
    if (isOpen) {
      Object.assign(form, {
        code: generateCode("SP"),
        approvalType: "OTHER" as ApprovalType,
        title: "",
        sourceNo: "",
        amount: 0,
        applicantName: auth.user?.displayName || "",
        content: "",
        departmentName: "",
        businessType: "",
        projectCode: "",
        supplierRisk: "",
        customerLevel: "",
      });
    }
  },
);

async function handleOk() {
  await formRef.value?.validate();
  emit("submit", { ...form });
}
</script>
