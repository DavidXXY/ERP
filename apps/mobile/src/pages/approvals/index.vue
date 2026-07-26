<script setup lang="ts">
import { computed, ref } from "vue";
import { onPullDownRefresh, onShow } from "@dcloudio/uni-app";
import ApprovalCard from "@/components/ApprovalCard.vue";
import StateView from "@/components/StateView.vue";
import { listApprovals } from "@/api/office";
import type { Approval } from "@/types/domain";

const records = ref<Approval[]>([]);
const loading = ref(true);
const error = ref("");
const active = ref("PENDING");
const keyword = ref("");
const tabs = [{ value: "PENDING", label: "待处理" }, { value: "APPROVED", label: "已通过" }, { value: "REJECTED", label: "已驳回" }, { value: "ALL", label: "全部" }];
const filtered = computed(() => records.value.filter((item) => (active.value === "ALL" || item.status === active.value) && (!keyword.value || `${item.title}${item.code}${item.applicantName}`.includes(keyword.value.trim()))));

async function load() {
  loading.value = !records.value.length; error.value = "";
  try { records.value = await listApprovals(); }
  catch (e) { error.value = (e as Error).message; }
  finally { loading.value = false; uni.stopPullDownRefresh(); }
}
onShow(load); onPullDownRefresh(load);
</script>

<template>
  <view class="page-shell approvals-page">
    <view class="search-box"><uni-icons type="search" size="18" color="#74808a" /><input v-model="keyword" placeholder="搜索标题、单号或申请人" /></view>
    <scroll-view scroll-x class="tabs"><view class="tabs-inner"><view v-for="tab in tabs" :key="tab.value" class="tab" :class="{ active: active === tab.value }" @click="active = tab.value">{{ tab.label }}<text v-if="tab.value === 'PENDING'" class="count">{{ records.filter((r) => r.status === 'PENDING').length }}</text></view></view></scroll-view>
    <StateView :loading="loading" :error="error" :empty="!filtered.length" empty-text="当前筛选下没有审批" @retry="load">
      <ApprovalCard v-for="item in filtered" :key="item.id" :item="item" />
    </StateView>
  </view>
</template>

<style scoped>
.approvals-page { padding-top: 22rpx; }
.search-box { height: 78rpx; padding: 0 22rpx; display: flex; align-items: center; gap: 13rpx; border: 1rpx solid #dfe4e7; border-radius: 10rpx; background: #fff; }
.search-box input { flex: 1; font-size: 26rpx; }
.tabs { margin: 20rpx 0 24rpx; white-space: nowrap; }
.tabs-inner { display: flex; gap: 10rpx; }
.tab { height: 64rpx; padding: 0 25rpx; display: inline-flex; align-items: center; gap: 8rpx; border-radius: 9rpx; color: #66717b; background: #e9edef; font-size: 24rpx; }
.tab.active { color: #fff; background: #176b5b; }
.count { min-width: 29rpx; height: 29rpx; padding: 0 5rpx; display: flex; align-items: center; justify-content: center; border-radius: 15rpx; background: rgba(255,255,255,.22); font-size: 18rpx; }
</style>
