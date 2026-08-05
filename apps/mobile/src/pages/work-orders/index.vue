<script setup lang="ts">
import { computed, ref } from "vue";
import { onPullDownRefresh, onShow } from "@dcloudio/uni-app";
import StateView from "@/components/StateView.vue";
import WorkOrderCard from "@/components/WorkOrderCard.vue";
import { listMyWorkOrders } from "@/api/maintenance";
import type { WorkOrder } from "@/types/domain";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
const records = ref<WorkOrder[]>([]);
const loading = ref(true);
const error = ref("");
const active = ref("ACTIVE");
const keyword = ref("");
const filtered = computed(() => records.value.filter((item) => {
  const statusMatch = active.value === "ALL" || (active.value === "ACTIVE" ? !["ACCEPTED", "CANCELLED"].includes(item.status) : item.status === active.value);
  return statusMatch && (!keyword.value || `${item.code}${item.title}${item.customerName || ""}${item.siteAddress || ""}`.includes(keyword.value.trim()));
}));

async function load() {
  loading.value = !records.value.length; error.value = "";
  if (!auth.can("maintenance:view") && !auth.can("maintenance:order:manage")) {
    records.value = [];
    error.value = "当前账号未开通现场工单";
    loading.value = false;
    uni.stopPullDownRefresh();
    return;
  }
  try { records.value = await listMyWorkOrders(); }
  catch (e) { error.value = (e as Error).message; }
  finally { loading.value = false; uni.stopPullDownRefresh(); }
}
onShow(load); onPullDownRefresh(load);
</script>

<template>
  <view class="page-shell orders-page">
    <view class="search-box"><uni-icons type="search" size="18" color="#74808a" /><input v-model="keyword" placeholder="搜索工单、客户或地址" /></view>
    <view class="filter-row">
      <view v-for="tab in [{v:'ACTIVE',l:'执行中'},{v:'ASSIGNED',l:'待接单'},{v:'COMPLETED',l:'待验收'},{v:'ALL',l:'全部'}]" :key="tab.v" class="filter" :class="{ active: active === tab.v }" @click="active = tab.v">{{ tab.l }}</view>
    </view>
    <StateView :loading="loading" :error="error" :empty="!filtered.length" empty-text="当前没有匹配的工单" @retry="load">
      <WorkOrderCard v-for="item in filtered" :key="item.id" :item="item" />
    </StateView>
  </view>
</template>

<style scoped>
.orders-page { padding-top: 22rpx; }
.search-box { height: 78rpx; padding: 0 22rpx; display: flex; align-items: center; gap: 13rpx; border: 1rpx solid #dfe4e7; border-radius: 10rpx; background: #fff; }
.search-box input { flex: 1; font-size: 26rpx; }
.filter-row { margin: 20rpx 0 24rpx; display: grid; grid-template-columns: repeat(4, 1fr); gap: 10rpx; }
.filter { min-width: 0; height: 64rpx; display: flex; align-items: center; justify-content: center; border-radius: 9rpx; color: #66717b; background: #e9edef; font-size: 23rpx; }
.filter.active { color: #fff; background: #176b5b; }
</style>
