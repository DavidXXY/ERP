<script setup lang="ts">
import type { WorkOrder } from "@/types/domain";
import { shortDate, statusClass, statusLabels } from "@/utils/format";
defineProps<{ item: WorkOrder; compact?: boolean }>();
</script>

<template>
  <view class="order-card surface" @click="uni.navigateTo({ url: `/pages/work-orders/detail?id=${item.id}` })">
    <view class="row-between"><text class="code">{{ item.code }}</text><text class="status-pill" :class="statusClass(item.status)">{{ statusLabels[item.status] || item.status }}</text></view>
    <text class="title">{{ item.title }}</text>
    <view class="location"><uni-icons type="location" size="16" color="#69737e" /><text>{{ item.siteAddress || item.customerName || "现场地址待补充" }}</text></view>
    <view class="footer row-between"><text>{{ shortDate(item.plannedDate) }}</text><text :class="{ urgent: item.priority === 'URGENT' }">{{ item.priority === "URGENT" ? "紧急" : item.assigneeName || "待指派" }}</text></view>
  </view>
</template>

<style scoped>
.order-card { padding: 26rpx; margin-bottom: 18rpx; }
.code { color: #176b5b; font-size: 23rpx; font-weight: 700; }
.title { display: block; margin: 20rpx 0 15rpx; font-size: 31rpx; font-weight: 720; line-height: 1.4; }
.location { display: flex; align-items: flex-start; gap: 7rpx; color: #69737e; font-size: 24rpx; line-height: 1.5; }
.footer { margin-top: 21rpx; padding-top: 17rpx; border-top: 1rpx solid #edf0f1; color: #7b848d; font-size: 23rpx; }
.urgent { color: #c33b3b; font-weight: 700; }
</style>
