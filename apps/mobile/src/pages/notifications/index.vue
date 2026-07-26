<script setup lang="ts">
import { computed, ref } from "vue";
import { onPullDownRefresh, onShow } from "@dcloudio/uni-app";
import StateView from "@/components/StateView.vue";
import { listNotifications, markNotificationRead, refreshNotifications } from "@/api/office";
import type { NotificationRecord } from "@/types/domain";
import { dateText } from "@/utils/format";

const records=ref<NotificationRecord[]>([]);const loading=ref(true);const error=ref("");const onlyUnread=ref(false);
const filtered=computed(()=>onlyUnread.value?records.value.filter(i=>!i.read):records.value);
async function load(refresh=false){loading.value=!records.value.length;error.value="";try{if(refresh)await refreshNotifications();records.value=await listNotifications();}catch(e){error.value=(e as Error).message;}finally{loading.value=false;uni.stopPullDownRefresh();}}
async function open(item:NotificationRecord){if(!item.read){try{await markNotificationRead(item.id);item.read=true;}catch{}}if(item.relatedType==="WORK_ORDER"&&item.relatedId)uni.navigateTo({url:`/pages/work-orders/detail?id=${item.relatedId}`});else if(item.relatedId&&item.relatedType?.includes("APPROVAL"))uni.navigateTo({url:`/pages/approvals/detail?id=${item.relatedId}`});}
function toggleUnread(event:Event){onlyUnread.value=Boolean((event as Event & {detail:{value:boolean}}).detail.value);}
onShow(()=>load(false));onPullDownRefresh(()=>load(true));
</script>

<template><view class="page-shell notifications-page"><view class="toolbar row-between"><text>{{ records.filter(i=>!i.read).length }} 条未读</text><switch :checked="onlyUnread" color="#176b5b" style="transform:scale(.75)" @change="toggleUnread" /></view><StateView :loading="loading" :error="error" :empty="!filtered.length" empty-text="暂无消息通知" @retry="load(false)"><view v-for="item in filtered" :key="item.id" class="notice surface" :class="{unread:!item.read}" @click="open(item)"><view class="notice-dot" /><view><text class="title">{{ item.title }}</text><text class="content">{{ item.content }}</text><text class="time">{{ dateText(item.createdAt) }}</text></view><uni-icons type="right" size="17" color="#9aa2a9" /></view></StateView></view></template>

<style scoped>.notifications-page{padding-top:20rpx}.toolbar{height:64rpx;margin-bottom:14rpx;color:#69737e;font-size:23rpx}.notice{min-height:145rpx;margin-bottom:14rpx;padding:23rpx;display:grid;grid-template-columns:18rpx 1fr 28rpx;gap:14rpx;align-items:start}.notice.unread{border-left:6rpx solid #176b5b}.notice-dot{width:13rpx;height:13rpx;margin-top:10rpx;border-radius:50%;background:#cbd2d5}.unread .notice-dot{background:#176b5b}.title,.content,.time{display:block}.title{font-size:28rpx;font-weight:700}.content{margin-top:10rpx;color:#66717b;font-size:24rpx;line-height:1.5}.time{margin-top:12rpx;color:#929aa1;font-size:21rpx}</style>
