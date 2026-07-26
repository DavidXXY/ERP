<script setup lang="ts">
import { computed, ref } from "vue";
import { onPullDownRefresh, onShow } from "@dcloudio/uni-app";
import AppHeader from "@/components/AppHeader.vue";
import StateView from "@/components/StateView.vue";
import WorkOrderCard from "@/components/WorkOrderCard.vue";
import { getMobileWorkbench } from "@/api/mobile";
import { useAuthStore } from "@/stores/auth";
import type { MobileWorkbench } from "@/types/domain";
import { getQueue } from "@/utils/offline";

const auth = useAuthStore();
const data = ref<MobileWorkbench | null>(null);
const loading = ref(true);
const error = ref("");
const offlineCount = ref(0);
const greeting = computed(() => {
  const hour = new Date().getHours();
  return `${hour < 11 ? "早上好" : hour < 14 ? "中午好" : hour < 18 ? "下午好" : "晚上好"}，${auth.user?.displayName || "同事"}`;
});

async function load() {
  if (!auth.token) { auth.restore(); }
  loading.value = !data.value;
  error.value = "";
  offlineCount.value = getQueue().length;
  try { data.value = await getMobileWorkbench(); }
  catch (e) { error.value = (e as Error).message; }
  finally { loading.value = false; uni.stopPullDownRefresh(); }
}

onShow(load);
onPullDownRefresh(load);
</script>

<template>
  <view>
    <AppHeader :title="greeting" subtitle="把今天重要的事情先做完" :badge="data?.unreadNotifications" />
    <view class="page-shell home-shell">
      <StateView :loading="loading" :error="error" @retry="load">
        <view class="metric-strip surface">
          <view class="metric" @click="uni.switchTab({ url: '/pages/approvals/index' })"><text class="metric-value">{{ data?.pendingApprovals || 0 }}</text><text>待审批</text></view>
          <view class="metric" @click="uni.switchTab({ url: '/pages/work-orders/index' })"><text class="metric-value">{{ data?.activeWorkOrders || 0 }}</text><text>进行中</text></view>
          <view class="metric" @click="uni.navigateTo({ url: '/pages/notifications/index' })"><text class="metric-value danger-value">{{ data?.unreadNotifications || 0 }}</text><text>未读消息</text></view>
        </view>

        <view v-if="offlineCount" class="offline-banner" @click="uni.navigateTo({ url: '/pages/offline/index' })">
          <uni-icons type="cloud-upload" size="20" color="#8b570f" /><text>{{ offlineCount }} 条现场记录等待同步</text><text>查看 ›</text>
        </view>

        <view class="section-head row-between"><text class="section-title">快捷操作</text><text class="section-note">高频入口</text></view>
        <view class="quick-grid">
          <view class="quick-item" @click="uni.switchTab({ url: '/pages/approvals/index' })"><view class="quick-icon green"><uni-icons type="checkbox" size="24" color="#176b5b" /></view><text>审批处理</text></view>
          <view class="quick-item" @click="uni.switchTab({ url: '/pages/work-orders/index' })"><view class="quick-icon dark"><uni-icons type="gear" size="24" color="#fff" /></view><text>现场工单</text></view>
          <view class="quick-item" @click="uni.navigateTo({ url: '/pages/applications/index' })"><view class="quick-icon amber"><uni-icons type="compose" size="24" color="#9a5c0d" /></view><text>发起申请</text></view>
          <view class="quick-item" @click="uni.navigateTo({ url: '/pages/notifications/index' })"><view class="quick-icon red"><uni-icons type="notification" size="24" color="#b53232" /></view><text>消息通知</text></view>
        </view>

        <view class="section-head row-between"><text class="section-title">今日任务轨道</text><text class="section-link" @click="uni.switchTab({ url: '/pages/work-orders/index' })">全部工单</text></view>
        <view v-if="data?.workOrders?.length" class="task-rail">
          <view v-for="(item, index) in data.workOrders.slice(0, 3)" :key="item.id" class="rail-row">
            <view class="rail-index">{{ String(index + 1).padStart(2, "0") }}</view>
            <view class="rail-content"><WorkOrderCard :item="item" compact /></view>
          </view>
        </view>
        <view v-else class="surface empty-state">今天没有待执行的现场工单</view>
      </StateView>
    </view>
  </view>
</template>

<style scoped>
.home-shell { padding-top: 24rpx; }
.metric-strip { display: grid; grid-template-columns: repeat(3, 1fr); padding: 26rpx 0; }
.metric { min-width: 0; display: flex; flex-direction: column; align-items: center; gap: 8rpx; color: #69737e; font-size: 22rpx; border-right: 1rpx solid #e7ebed; }
.metric:last-child { border-right: 0; }
.metric-value { color: #17202a; font-size: 42rpx; font-weight: 800; line-height: 1; }
.danger-value { color: #b53232; }
.offline-banner { margin-top: 18rpx; padding: 20rpx 22rpx; display: flex; align-items: center; gap: 12rpx; background: #fff4de; border: 1rpx solid #efd8a7; border-radius: 10rpx; color: #805312; font-size: 24rpx; }
.offline-banner text:nth-child(2) { flex: 1; }
.section-head { margin: 38rpx 2rpx 19rpx; }
.section-note { color: #8b949c; font-size: 22rpx; }
.quick-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12rpx; }
.quick-item { min-width: 0; display: flex; flex-direction: column; align-items: center; gap: 12rpx; color: #39434d; font-size: 23rpx; }
.quick-icon { width: 92rpx; height: 82rpx; border-radius: 12rpx; display: flex; align-items: center; justify-content: center; }
.green { background: #e2f0ed; }.dark { background: #25323a; }.amber { background: #fff0d7; }.red { background: #fbe5e5; }
.rail-row { display: flex; align-items: stretch; gap: 13rpx; }
.rail-index { width: 50rpx; padding-top: 24rpx; color: #9ca5ac; font-size: 21rpx; font-weight: 700; border-right: 2rpx solid #c6d7d3; }
.rail-content { flex: 1; min-width: 0; }
</style>
