<script setup lang="ts">
import { reactive, ref } from "vue";
import { createTravel } from "@/api/office";
import { getPersonalProfile } from "@/api/personal";
import { useAuthStore } from "@/stores/auth";
import { createOperationId } from "@/utils/offline";

const auth = useAuthStore();
const saving = ref(false);
const department = ref("未设置部门");
const form = reactive({
  destination: "",
  purpose: "",
  transportType: "高铁",
  startDate: "",
  endDate: "",
  estimatedAmount: 0,
  companionNames: "",
});
async function submit() {
  if (
    !form.destination.trim() ||
    !form.purpose.trim() ||
    !form.startDate ||
    !form.endDate
  ) {
    uni.showToast({ title: "请完整填写出差信息", icon: "none" });
    return;
  }
  saving.value = true;
  try {
    await createTravel({
      code: createOperationId("MBTRV").slice(0, 40),
      applicantId: auth.user?.id,
      applicantName: auth.user?.displayName || auth.user?.username,
      departmentName: department.value,
      destination: form.destination.trim(),
      purpose: form.purpose.trim(),
      transportType: form.transportType,
      startDate: form.startDate,
      endDate: form.endDate,
      estimatedAmount: Number(form.estimatedAmount || 0),
      companionNames: form.companionNames.trim() || undefined,
    });
    uni.showToast({ title: "出差申请已提交", icon: "success" });
    setTimeout(() => uni.navigateBack(), 700);
  } catch (e) {
    uni.showToast({ title: (e as Error).message, icon: "none" });
  } finally {
    saving.value = false;
  }
}
getPersonalProfile()
  .then((p) => {
    department.value =
      p.account.organizationName ||
      p.employee?.organizationName ||
      department.value;
  })
  .catch(() => {});
</script>

<template>
  <view class="page-shell form-page"
    ><view class="surface form-card"
      ><view class="field"
        ><text class="field-label">目的地 *</text
        ><input
          v-model="form.destination"
          class="field-input"
          placeholder="例如：上海市浦东新区" /></view
      ><view class="field"
        ><text class="field-label">出差事由 *</text
        ><textarea
          v-model="form.purpose"
          class="field-textarea"
          maxlength="500"
          placeholder="说明拜访客户、现场服务或项目事项"
        /></view
      ><view class="date-grid"
        ><view class="field"
          ><text class="field-label">开始日期 *</text
          ><picker
            mode="date"
            :value="form.startDate"
            @change="form.startDate = String(($event.detail as any).value)"
            ><view class="field-picker">{{
              form.startDate || "请选择"
            }}</view></picker
          ></view
        ><view class="field"
          ><text class="field-label">结束日期 *</text
          ><picker
            mode="date"
            :value="form.endDate"
            @change="form.endDate = String(($event.detail as any).value)"
            ><view class="field-picker">{{
              form.endDate || "请选择"
            }}</view></picker
          ></view
        ></view
      ><view class="field"
        ><text class="field-label">交通方式</text
        ><input v-model="form.transportType" class="field-input" /></view
      ><view class="field"
        ><text class="field-label">预计费用（含税，元）</text
        ><input
          v-model.number="form.estimatedAmount"
          class="field-input"
          type="digit"
          placeholder="0.00" /></view
      ><view class="field"
        ><text class="field-label">同行人员</text
        ><input
          v-model="form.companionNames"
          class="field-input"
          placeholder="选填，多人用顿号分隔" /></view></view
    ><button class="primary-btn" :disabled="saving" @click="submit">
      {{ saving ? "正在提交" : "提交出差申请" }}
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
.date-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
}
</style>
