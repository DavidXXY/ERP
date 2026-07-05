<template>
  <div class="self-page">
    <a-page-header title="提交请假" @back="$router.back()" />

    <a-card>
      <a-form ref="formRef" :model="form" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="请假类型" name="leaveType" :rules="[{ required: true, message: '请选择请假类型' }]">
              <a-select v-model:value="form.leaveType" :options="leaveTypeOptions" @change="onTypeChange" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="共计天数">
              <a-input-number v-model:value="form.totalDays" :min="0.5" :step="0.5" style="width:100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="开始日期" name="startDate" :rules="[{ required: true, message: '请选择开始日期' }]">
              <a-date-picker v-model:value="form.startDate" value-format="YYYY-MM-DD" style="width:100%" @change="calcDays" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="结束日期" name="endDate" :rules="[{ required: true, message: '请选择结束日期' }]">
              <a-date-picker v-model:value="form.endDate" value-format="YYYY-MM-DD" style="width:100%" @change="calcDays" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="请假原因">
          <a-textarea v-model:value="form.reason" :rows="3" placeholder="请简要说明请假原因" />
        </a-form-item>
        <div v-if="selectedTypeBalance" class="balance-hint">
          <a-alert :message="`${typeLabel(form.leaveType)}剩余额度：${selectedTypeBalance.remainingDays.toFixed(1)} 天`" type="info" show-icon />
        </div>
        <a-form-item>
          <a-button type="primary" :loading="saving" @click="submitLeave">提交申请</a-button>
          <a-button style="margin-left:12px" @click="$router.back()">取消</a-button>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { message, DatePicker } from "ant-design-vue";
import { createSelfLeave, getSelfLeaveBalances, type LeaveBalanceRecord, type LeavePayload } from "@/api/hr";

const router = useRouter();
const saving = ref(false);
const formRef = ref();
const balances = ref<LeaveBalanceRecord[]>([]);
const form = reactive<LeavePayload>({ leaveType: "ANNUAL", startDate: "", endDate: "", totalDays: 1, reason: "" });

const selectedTypeBalance = computed(() => balances.value.find(b => b.leaveType === form.leaveType));

const leaveTypeOptions = [
  { value: "ANNUAL", label: "年假" }, { value: "SICK", label: "病假" },
  { value: "PERSONAL", label: "事假" }, { value: "MARRIAGE", label: "婚假" },
  { value: "MATERNITY", label: "产假" }, { value: "COMPENSATORY", label: "调休" },
  { value: "OTHER", label: "其他" },
];

function typeLabel(t: string) { const l: Record<string,string> = {ANNUAL:"年假",SICK:"病假",PERSONAL:"事假",MARRIAGE:"婚假",MATERNITY:"产假",COMPENSATORY:"调休",OTHER:"其他"}; return l[t]||t; }

function onTypeChange() {
  if (selectedTypeBalance.value && form.totalDays > selectedTypeBalance.value.remainingDays) {
    message.warning(`当前${typeLabel(form.leaveType)}余额不足（剩余 ${selectedTypeBalance.value.remainingDays.toFixed(1)} 天）`);
  }
}

function calcDays() {
  if (form.startDate && form.endDate) {
    const diff = (new Date(form.endDate).getTime() - new Date(form.startDate).getTime()) / 86400000 + 1;
    form.totalDays = diff > 0 ? diff : 1;
  }
}

async function submitLeave() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    await createSelfLeave(form);
    message.success("请假申请已提交");
    router.push("/self/leaves");
  } catch (error: any) { message.error(error.message || "提交失败"); }
  finally { saving.value = false; }
}

onMounted(async () => {
  try { balances.value = await getSelfLeaveBalances(); } catch {}
});
</script>

<style scoped>
.self-page { max-width: 700px; }
.balance-hint { margin-bottom: 16px; }
</style>
