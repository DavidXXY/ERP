<script setup lang="ts">
withDefaults(defineProps<{ loading?: boolean; empty?: boolean; error?: string; emptyText?: string }>(), { emptyText: "暂无数据" });
const emit = defineEmits<{ retry: [] }>();
</script>

<template>
  <view v-if="loading" class="state"><view class="spinner" /><text>正在加载</text></view>
  <view v-else-if="error" class="state"><uni-icons type="info" size="38" color="#c33b3b" /><text>{{ error }}</text><button class="retry" @click="emit('retry')">重新加载</button></view>
  <view v-else-if="empty" class="state"><uni-icons type="folder-add" size="42" color="#89929b" /><text>{{ emptyText }}</text></view>
  <slot v-else />
</template>

<style scoped>
.state { min-height: 320rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 18rpx; color: #69737e; }
.spinner { width: 38rpx; height: 38rpx; border: 5rpx solid #dbe4e2; border-top-color: #176b5b; border-radius: 50%; animation: spin .8s linear infinite; }
.retry { margin: 8rpx 0 0; padding: 0 26rpx; min-height: 64rpx; line-height: 64rpx; color: #176b5b; background: #fff; border: 1rpx solid #b9d1cc; border-radius: 10rpx; font-size: 25rpx; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
