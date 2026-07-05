<template>
  <div class="self-page">
    <a-page-header title="我的请假额度" @back="$router.back()" />

    <a-row :gutter="16">
      <a-col :xs="24" :sm="12" v-for="b in balances" :key="b.id" style="margin-bottom:16px">
        <a-card hoverable>
          <a-statistic :title="typeLabel(b.leaveType)" :value="b.remainingDays.toFixed(1)" suffix="天" :value-style="{ color: b.remainingDays < 2 ? '#ff4d4f' : b.remainingDays < 5 ? '#faad14' : '#52c41a' }" />
          <a-progress :percent="calcPct(b)" :stroke-color="b.remainingDays < 2 ? '#ff4d4f' : b.remainingDays < 5 ? '#faad14' : '#52c41a'" style="margin-top:12px" />
          <div class="balance-detail">
            <span>总额度：{{ b.totalDays }} 天</span>
            <span>已使用：{{ b.usedDays }} 天</span>
          </div>
        </a-card>
      </a-col>
    </a-row>
    <a-empty v-if="!loading && !balances.length" description="暂无额度数据" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { getSelfLeaveBalances, type LeaveBalanceRecord } from "@/api/hr";
import { message } from "ant-design-vue";

const loading = ref(false);
const balances = ref<LeaveBalanceRecord[]>([]);

function calcPct(b: LeaveBalanceRecord) { return Math.round((b.remainingDays / Math.max(b.totalDays, 1)) * 100); }
function typeLabel(t: string) { const l: Record<string,string> = {ANNUAL:"年假",SICK:"病假",PERSONAL:"事假",MARRIAGE:"婚假",MATERNITY:"产假",COMPENSATORY:"调休",OTHER:"其他"}; return l[t]||t; }

onMounted(async () => {
  loading.value = true;
  try { balances.value = await getSelfLeaveBalances(); }
  catch (error: any) { message.error(error.message || "加载失败"); }
  finally { loading.value = false; }
});
</script>

<style scoped>
.self-page { max-width: 800px; }
.balance-detail { display: flex; justify-content: space-between; margin-top: 8px; font-size: 12px; color: #65717e; }
</style>
