<script setup lang="ts">
import { computed, ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import StateView from "@/components/StateView.vue";
import { listSelfLeaves } from "@/api/hr";
import { listExpenses, listTravelApplications } from "@/api/office";
import { dateText, statusClass, statusLabels } from "@/utils/format";

type HistoryItem = {
  id: string;
  type: string;
  title: string;
  status: string;
  createdAt: string;
  meta?: string;
};
const items = ref<HistoryItem[]>([]);
const loading = ref(true);
const error = ref("");
const active = ref("ALL");
const filtered = computed(() =>
  items.value.filter((i) => active.value === "ALL" || i.type === active.value),
);
async function load() {
  loading.value = true;
  error.value = "";
  try {
    const [leaves, expenses, travels] = await Promise.all([
      listSelfLeaves(),
      listExpenses(),
      listTravelApplications(),
    ]);
    items.value = [
      ...leaves.map((i) => ({
        id: i.id,
        type: "LEAVE",
        title: `${i.leaveType}请假 · ${i.totalDays}天`,
        status: i.status,
        createdAt: i.approvedAt || i.startDate,
        meta: `${i.startDate} 至 ${i.endDate}`,
      })),
      ...expenses.map((i: any) => ({
        id: String(i.id),
        type: "EXPENSE",
        title: String(i.description || "费用报销"),
        status: String(i.status || "PENDING"),
        createdAt: String(i.createdAt || i.expenseDate || ""),
        meta: `报销金额（含税，元）¥${Number(i.amount || 0).toFixed(2)}`,
      })),
      ...travels.map((i: any) => ({
        id: String(i.id),
        type: "TRAVEL",
        title: `出差至 ${i.destination || "-"}`,
        status: String(i.status || "PENDING"),
        createdAt: String(i.createdAt || i.startDate || ""),
        meta: `${i.startDate || ""} 至 ${i.endDate || ""}`,
      })),
    ].sort((a, b) => b.createdAt.localeCompare(a.createdAt));
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    loading.value = false;
  }
}
onShow(load);
</script>

<template>
  <view class="page-shell history-page"
    ><view class="filters"
      ><view
        v-for="tab in [
          { v: 'ALL', l: '全部' },
          { v: 'LEAVE', l: '请假' },
          { v: 'EXPENSE', l: '报销' },
          { v: 'TRAVEL', l: '出差' },
        ]"
        :key="tab.v"
        :class="{ active: active === tab.v }"
        @click="active = tab.v"
        >{{ tab.l }}</view
      ></view
    ><StateView
      :loading="loading"
      :error="error"
      :empty="!filtered.length"
      empty-text="暂无申请记录"
      @retry="load"
      ><view
        v-for="item in filtered"
        :key="`${item.type}-${item.id}`"
        class="history-item surface"
        ><view class="row-between"
          ><text class="type">{{
            item.type === "LEAVE"
              ? "请假"
              : item.type === "EXPENSE"
                ? "报销"
                : "出差"
          }}</text
          ><text class="status-pill" :class="statusClass(item.status)">{{
            statusLabels[item.status] || item.status
          }}</text></view
        ><text class="title">{{ item.title }}</text
        ><view class="row-between meta"
          ><text>{{ item.meta }}</text
          ><text>{{ dateText(item.createdAt) }}</text></view
        ></view
      ></StateView
    ></view
  >
</template>

<style scoped>
.history-page {
  padding-top: 22rpx;
}
.filters {
  margin-bottom: 22rpx;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10rpx;
}
.filters view {
  height: 62rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 9rpx;
  background: #e8edef;
  color: #65717a;
  font-size: 23rpx;
}
.filters .active {
  background: #176b5b;
  color: #fff;
}
.history-item {
  margin-bottom: 16rpx;
  padding: 24rpx;
}
.type {
  color: #176b5b;
  font-size: 23rpx;
  font-weight: 700;
}
.title {
  display: block;
  margin: 20rpx 0;
  font-size: 29rpx;
  font-weight: 700;
}
.meta {
  padding-top: 17rpx;
  border-top: 1rpx solid #edf0f1;
  color: #7b848d;
  font-size: 22rpx;
}
</style>
