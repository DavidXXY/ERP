<script setup lang="ts">
import { ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import StateView from "@/components/StateView.vue";
import { flushQueue, getQueue, removeQueued, type OfflineOperation } from "@/utils/offline";
import { dateText } from "@/utils/format";

const queue=ref<OfflineOperation[]>([]);const syncing=ref(false);
function load(){queue.value=getQueue();}
async function retry(){syncing.value=true;try{await flushQueue();load();uni.showToast({title:queue.value.length?"部分任务仍未同步":"同步完成",icon:queue.value.length?"none":"success"});}finally{syncing.value=false;}}
async function remove(item:OfflineOperation){const result=await uni.showModal({title:"删除离线任务",content:"删除后该条现场记录不会再自动上传。"});if(result.confirm){removeQueued(item.id);load();}}
onShow(load);
</script>

<template><view class="page-shell offline-page"><view class="offline-info"><uni-icons type="cloud-upload" size="26" color="#176b5b" /><view><text>弱网任务队列</text><text>联网后会自动按保存顺序同步</text></view></view><StateView :empty="!queue.length" empty-text="没有等待同步的任务"><view v-for="item in queue" :key="item.id" class="queue-item surface"><view class="row-between"><text class="kind">{{ item.kind==='UPLOAD'?'文件上传':'业务提交' }}</text><text v-if="item.retries" class="retry-count">已重试 {{ item.retries }} 次</text></view><text class="label">{{ item.label }}</text><text class="time">{{ dateText(item.createdAt) }}</text><view v-if="item.lastError" class="error">{{ item.lastError }}</view><view class="item-actions"><button @click="remove(item)">删除</button></view></view></StateView><button v-if="queue.length" class="primary-btn sync-btn" :disabled="syncing" @click="retry"><uni-icons type="refresh" size="20" color="#fff" />{{ syncing?'正在同步':'立即重试全部' }}</button></view></template>

<style scoped>.offline-page{padding-top:24rpx}.offline-info{margin-bottom:22rpx;padding:24rpx;display:flex;gap:18rpx;align-items:center;border-radius:11rpx;background:#e9f3f0;color:#176b5b}.offline-info text{display:block}.offline-info text:first-child{font-weight:700}.offline-info text:last-child{margin-top:6rpx;color:#657873;font-size:22rpx}.queue-item{margin-bottom:16rpx;padding:24rpx}.kind{color:#176b5b;font-size:22rpx;font-weight:700}.retry-count{color:#a56915;font-size:21rpx}.label{display:block;margin-top:17rpx;font-size:29rpx;font-weight:700}.time{display:block;margin-top:10rpx;color:#89929a;font-size:21rpx}.error{margin-top:14rpx;padding:12rpx;border-radius:7rpx;background:#fbe9e9;color:#a73131;font-size:22rpx}.item-actions{margin-top:12rpx;text-align:right}.item-actions button{display:inline-block;margin:0;padding:8rpx 12rpx;color:#b53232;background:transparent;font-size:22rpx}.sync-btn{margin-top:24rpx}</style>
