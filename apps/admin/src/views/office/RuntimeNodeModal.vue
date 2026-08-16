<template>
  <a-modal
    v-model:open="openProxy"
    :title="action === 'transfer' ? '审批转交' : '审批加签'"
    :confirm-loading="saving"
    @ok="handleOk"
  >
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-form-item label="处理人" name="targetUserId"
        ><a-select
          v-model:value="form.targetUserId"
          show-search
          option-filter-prop="label"
          :options="userOptions"
      /></a-form-item>
      <a-form-item label="说明" name="comment"
        ><a-textarea v-model:value="form.comment" :rows="3"
      /></a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import { useAuthStore } from "@/stores/auth";
import type { RuntimeActionPayload } from "./composables/useApprovalActions";

const props = defineProps<{
  open: boolean;
  saving: boolean;
  action: "transfer" | "addSign";
  userOptions: Array<{ label: string; value: string }>;
}>();
const emit = defineEmits<{
  (e: "update:open", v: boolean): void;
  (e: "submit", v: RuntimeActionPayload): void;
}>();

const auth = useAuthStore();
const formRef = ref();

const openProxy = computed({
  get: () => props.open,
  set: (v: boolean) => emit("update:open", v),
});

const form = reactive<RuntimeActionPayload>({
  targetUserId: "",
  comment: "",
  operatorName: "",
});

const rules = {
  targetUserId: [{ required: true, message: "请选择处理人" }],
  comment: [{ required: true, message: "请输入说明" }],
};

watch(
  () => props.open,
  (isOpen) => {
    if (isOpen) {
      Object.assign(form, {
        targetUserId: "",
        comment: props.action === "transfer" ? "审批转交" : "审批加签",
        operatorName: auth.user?.displayName || "",
      });
    }
  },
);

async function handleOk() {
  await formRef.value?.validate();
  emit("submit", { ...form });
}
</script>
