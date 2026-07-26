<script setup lang="ts">
defineProps<{ title: string; subtitle?: string; badge?: number }>();
</script>

<template>
  <view class="app-header safe-top">
    <view>
      <text class="eyebrow">工程运维 ERP</text>
      <text class="title">{{ title }}</text>
      <text v-if="subtitle" class="subtitle">{{ subtitle }}</text>
    </view>
    <view class="header-mark" aria-hidden="true"><text>OPS</text><text v-if="badge" class="badge">{{ badge > 99 ? "99+" : badge }}</text></view>
  </view>
</template>

<style scoped lang="scss">
.app-header { padding: 28rpx 32rpx 30rpx; background: #fff; display: flex; justify-content: space-between; align-items: flex-end; border-bottom: 1rpx solid #e2e7ea; }
.eyebrow { display: block; color: #176b5b; font-size: 21rpx; font-weight: 700; }
.title { display: block; margin-top: 8rpx; font-size: 45rpx; line-height: 1.15; font-weight: 780; letter-spacing: 0; color: #17202a; }
.subtitle { display: block; margin-top: 10rpx; color: #69737e; font-size: 24rpx; }
.header-mark { width: 82rpx; height: 82rpx; border-radius: 12rpx; display: flex; align-items: center; justify-content: center; background: #17202a; color: #fff; font-size: 21rpx; font-weight: 800; position: relative; }
.badge { position: absolute; right: -10rpx; top: -12rpx; min-width: 32rpx; height: 32rpx; padding: 0 7rpx; border-radius: 16rpx; display: flex; align-items: center; justify-content: center; background: #c33b3b; font-size: 18rpx; }
</style>
