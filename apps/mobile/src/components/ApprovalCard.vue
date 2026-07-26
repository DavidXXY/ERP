<script setup lang="ts">
import type { Approval } from "@/types/domain";
import { approvalTypeLabels, dateText, money, statusClass, statusLabels } from "@/utils/format";
defineProps<{ item: Approval }>();
</script>

<template>
  <view class="approval-card surface" @click="uni.navigateTo({ url: `/pages/approvals/detail?id=${item.id}` })">
    <view class="row-between">
      <view class="row"><text class="type-mark">{{ approvalTypeLabels[item.approvalType]?.slice(0, 1) || "审" }}</text><text class="code">{{ approvalTypeLabels[item.approvalType] || item.approvalType }} · {{ item.code }}</text></view>
      <text class="status-pill" :class="statusClass(item.status)">{{ statusLabels[item.status] || item.status }}</text>
    </view>
    <text class="card-title">{{ item.title }}</text>
    <view class="meta row-between"><text>{{ item.applicantName }}<text v-if="item.departmentName"> · {{ item.departmentName }}</text></text><text v-if="item.amount != null" class="amount">{{ money(item.amount) }}</text></view>
    <view class="footer row-between"><text>{{ dateText(item.createdAt) }}</text><text class="node">{{ item.currentNodeName || item.currentApproverName || item.currentApprover || "查看流程" }} ›</text></view>
  </view>
</template>

<style scoped>
.approval-card { padding: 26rpx; margin-bottom: 18rpx; }
.type-mark { width: 48rpx; height: 48rpx; display: flex; align-items: center; justify-content: center; margin-right: 14rpx; border-radius: 8rpx; background: #e4f0ed; color: #176b5b; font-weight: 800; }
.code { color: #69737e; font-size: 23rpx; }
.card-title { display: block; margin: 23rpx 0 18rpx; font-size: 31rpx; font-weight: 700; line-height: 1.4; }
.meta { color: #4c5661; font-size: 25rpx; }
.amount { color: #17202a; font-size: 28rpx; font-weight: 700; }
.footer { margin-top: 22rpx; padding-top: 18rpx; border-top: 1rpx solid #edf0f1; color: #8a929a; font-size: 22rpx; }
.node { color: #176b5b; }
</style>
