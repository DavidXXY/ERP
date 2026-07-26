<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { createSelfLeave, listLeaveBalances } from "@/api/hr";
import type { LeaveBalance } from "@/types/domain";

const types = [{v:"ANNUAL",l:"年假"},{v:"SICK",l:"病假"},{v:"PERSONAL",l:"事假"},{v:"MARRIAGE",l:"婚假"},{v:"MATERNITY",l:"产假"},{v:"COMPENSATORY",l:"调休"},{v:"OTHER",l:"其他"}];
const form = reactive({ leaveType: "ANNUAL", startDate: "", endDate: "", totalDays: 1, reason: "" });
const balances = ref<LeaveBalance[]>([]); const saving = ref(false);
const typeIndex = computed(() => Math.max(0, types.findIndex((t) => t.v === form.leaveType)));
const balance = computed(() => balances.value.find((b) => b.leaveType === form.leaveType));
function calcDays(){ if(form.startDate&&form.endDate){const days=(new Date(form.endDate).getTime()-new Date(form.startDate).getTime())/86400000+1;form.totalDays=days>0?days:1;} }
async function submit(){if(!form.startDate||!form.endDate||!form.reason.trim()){uni.showToast({title:"请完整填写申请信息",icon:"none"});return;}saving.value=true;try{await createSelfLeave({...form});uni.showToast({title:"申请已提交",icon:"success"});setTimeout(()=>uni.navigateBack(),700);}catch(e){uni.showToast({title:(e as Error).message,icon:"none"});}finally{saving.value=false;}}
onMounted(async()=>{try{balances.value=await listLeaveBalances();}catch{/* balance is optional */}});
</script>

<template><view class="page-shell form-page"><view v-if="balance" class="balance surface"><text>当前可用余额</text><text>{{ balance.remainingDays.toFixed(1) }} 天</text></view><view class="surface form-card"><view class="field"><text class="field-label">请假类型 *</text><picker :range="types" range-key="l" :value="typeIndex" @change="form.leaveType=types[Number(($event.detail as any).value)].v"><view class="field-picker">{{ types[typeIndex].l }}<uni-icons type="down" size="16" color="#7b848d" /></view></picker></view><view class="date-grid"><view class="field"><text class="field-label">开始日期 *</text><picker mode="date" :value="form.startDate" @change="form.startDate=String(($event.detail as any).value);calcDays()"><view class="field-picker">{{ form.startDate||"请选择" }}</view></picker></view><view class="field"><text class="field-label">结束日期 *</text><picker mode="date" :value="form.endDate" @change="form.endDate=String(($event.detail as any).value);calcDays()"><view class="field-picker">{{ form.endDate||"请选择" }}</view></picker></view></view><view class="field"><text class="field-label">请假天数 *</text><input v-model.number="form.totalDays" class="field-input" type="digit" /></view><view class="field"><text class="field-label">请假原因 *</text><textarea v-model="form.reason" class="field-textarea" maxlength="500" placeholder="说明请假原因及工作安排" /></view></view><button class="primary-btn" :disabled="saving" @click="submit">{{ saving?"正在提交":"提交请假申请" }}</button></view></template>

<style scoped>.form-page{padding-top:24rpx}.balance{margin-bottom:18rpx;padding:25rpx 28rpx;display:flex;justify-content:space-between;align-items:center;color:#176b5b}.balance text:last-child{font-size:34rpx;font-weight:800}.form-card{padding:28rpx;margin-bottom:24rpx}.field-picker{justify-content:space-between}.date-grid{display:grid;grid-template-columns:1fr 1fr;gap:16rpx}</style>
