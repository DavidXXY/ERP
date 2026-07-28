<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { createExpense } from "@/api/office";
import { useAuthStore } from "@/stores/auth";
import { createExpenseCode } from "@/utils/offline";

const auth = useAuthStore();
const types = [
  { v: "TRAVEL", l: "差旅费" },
  { v: "TRANSPORT", l: "交通费" },
  { v: "ACCOMMODATION", l: "住宿费" },
  { v: "TOOL", l: "工具耗材" },
  { v: "OTHER", l: "其他" },
];
const form = reactive({
  expenseType: "TRAVEL",
  amount: 0,
  expenseDate: new Date().toISOString().slice(0, 10),
  description: "",
  projectId: "",
  workOrderId: "",
});
const saving = ref(false);
const typeIndex = computed(() =>
  Math.max(
    0,
    types.findIndex((t) => t.v === form.expenseType),
  ),
);
async function submit() {
  if (form.amount <= 0 || !form.expenseDate || !form.description.trim()) {
    uni.showToast({ title: "请完整填写费用信息", icon: "none" });
    return;
  }
  saving.value = true;
  try {
    await createExpense({
      code: createExpenseCode(),
      claimantId: auth.user?.id,
      claimantName: auth.user?.displayName || auth.user?.username,
      expenseType: form.expenseType,
      amount: Number(form.amount),
      expenseDate: form.expenseDate,
      description: form.description.trim(),
      projectId: form.projectId || undefined,
      workOrderId: form.workOrderId || undefined,
    });
    uni.showToast({ title: "报销已提交", icon: "success" });
    setTimeout(() => uni.navigateBack(), 700);
  } catch (e) {
    uni.showToast({ title: (e as Error).message, icon: "none" });
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <view class="page-shell form-page"
    ><view class="surface form-card"
      ><view class="field"
        ><text class="field-label">费用类型 *</text
        ><picker
          :range="types"
          range-key="l"
          :value="typeIndex"
          @change="
            form.expenseType = types[Number(($event.detail as any).value)].v
          "
          ><view class="field-picker"
            >{{ types[typeIndex].l
            }}<uni-icons
              type="down"
              size="16"
              color="#7b848d" /></view></picker></view
      ><view class="field"
        ><text class="field-label">报销金额（含税，元）*</text
        ><view class="money-input"
          ><text>¥</text
          ><input
            v-model.number="form.amount"
            type="digit"
            placeholder="0.00" /></view></view
      ><view class="field"
        ><text class="field-label">费用日期 *</text
        ><picker
          mode="date"
          :value="form.expenseDate"
          @change="form.expenseDate = String(($event.detail as any).value)"
          ><view class="field-picker">{{ form.expenseDate }}</view></picker
        ></view
      ><view class="field"
        ><text class="field-label">费用说明 *</text
        ><textarea
          v-model="form.description"
          class="field-textarea"
          maxlength="500"
          placeholder="说明费用用途、关联客户或事项"
        /></view
      ><view class="field"
        ><text class="field-label">关联工单ID</text
        ><input
          v-model="form.workOrderId"
          class="field-input"
          placeholder="选填，可从工单详情复制" /></view></view
    ><button class="primary-btn" :disabled="saving" @click="submit">
      {{ saving ? "正在提交" : "提交费用报销" }}
    </button></view
  >
</template>

<style scoped>
.form-page {
  padding-top: 24rpx;
}
.form-card {
  padding: 28rpx;
  margin-bottom: 24rpx;
}
.field-picker {
  justify-content: space-between;
}
.money-input {
  height: 100rpx;
  padding: 0 24rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
  border: 1rpx solid #dfe5e7;
  border-radius: 10rpx;
  background: #f8fafb;
}
.money-input text {
  color: #176b5b;
  font-size: 40rpx;
  font-weight: 700;
}
.money-input input {
  flex: 1;
  font-size: 38rpx;
  font-weight: 700;
}
</style>
