<script setup lang="ts">
const entries = [
  { title: "请假申请", subtitle: "年假、病假、调休等", icon: "calendar", color: "green", url: "/pages/applications/leave" },
  { title: "费用报销", subtitle: "差旅、交通、工具等", icon: "wallet", color: "amber", url: "/pages/applications/expense" },
  { title: "出差申请", subtitle: "行程及费用预算", icon: "map", color: "dark", url: "/pages/applications/travel" },
];
</script>

<template><view class="page-shell app-page"><view class="intro"><text>选择申请类型</text><text>提交后可在“我的申请”查看审批进度</text></view><view v-for="entry in entries" :key="entry.url" class="entry surface" @click="uni.navigateTo({ url: entry.url })"><view class="icon" :class="entry.color"><uni-icons :type="entry.icon" size="26" :color="entry.color === 'dark' ? '#fff' : entry.color === 'amber' ? '#9a5c0d' : '#176b5b'" /></view><view><text>{{ entry.title }}</text><text>{{ entry.subtitle }}</text></view><uni-icons type="right" size="18" color="#8b949c" /></view><button class="history-btn" @click="uni.navigateTo({ url: '/pages/applications/history' })"><uni-icons type="list" size="19" color="#176b5b" />查看我的申请</button></view></template>

<style scoped>.app-page{padding-top:30rpx}.intro{margin:10rpx 4rpx 30rpx}.intro text{display:block}.intro text:first-child{font-size:38rpx;font-weight:780}.intro text:last-child{margin-top:10rpx;color:#69737e;font-size:24rpx}.entry{min-height:150rpx;margin-bottom:18rpx;padding:24rpx;display:grid;grid-template-columns:90rpx 1fr 35rpx;align-items:center;gap:20rpx}.icon{width:82rpx;height:82rpx;display:flex;align-items:center;justify-content:center;border-radius:12rpx}.green{background:#e2f0ed}.amber{background:#fff0d7}.dark{background:#25323a}.entry text{display:block}.entry text:first-child{font-size:30rpx;font-weight:700}.entry text:last-child{margin-top:9rpx;color:#78818a;font-size:23rpx}.history-btn{margin-top:32rpx;min-height:82rpx;display:flex;align-items:center;justify-content:center;gap:10rpx;color:#176b5b;background:#fff;border:1rpx solid #b9d1cc;border-radius:11rpx;font-size:27rpx}</style>
