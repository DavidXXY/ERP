<script setup lang="ts">
import { computed, ref } from "vue";
import { onPullDownRefresh, onShow } from "@dcloudio/uni-app";
import StateView from "@/components/StateView.vue";
import { listParts, type InventoryPart } from "@/api/inventory";

const records=ref<InventoryPart[]>([]);const loading=ref(true);const error=ref("");const keyword=ref("");
const filtered=computed(()=>records.value.filter(i=>!keyword.value||`${i.code}${i.name}${i.model||""}${i.location||""}`.includes(keyword.value.trim())));
async function load(){loading.value=!records.value.length;error.value="";try{records.value=await listParts();}catch(e){error.value=(e as Error).message;}finally{loading.value=false;uni.stopPullDownRefresh();}}
onShow(load);onPullDownRefresh(load);
</script>

<template><view class="page-shell spares-page"><view class="search-box"><uni-icons type="search" size="18" color="#74808a" /><input v-model="keyword" placeholder="搜索备件名称、编码或库位" /></view><view class="actions"><button @click="uni.navigateTo({url:'/pages/spares/issue'})">领用</button><button @click="uni.navigateTo({url:'/pages/spares/return'})">归还</button><button class="scrap" @click="uni.navigateTo({url:'/pages/spares/scrap'})">报废</button></view><StateView :loading="loading" :error="error" :empty="!filtered.length" empty-text="没有匹配的备件" @retry="load"><view v-for="item in filtered" :key="item.id" class="part surface"><view class="row-between"><text class="code">{{ item.code }}</text><text v-if="item.lowStock" class="low">库存不足</text></view><text class="name">{{ item.name }}</text><text class="model">{{ item.model||"无规格" }} · {{ item.location||"库位未设置" }}</text><view class="stock row-between"><text>可用库存</text><text>{{ item.stockQty }}</text></view></view></StateView></view></template>

<style scoped>.spares-page{padding-top:22rpx}.search-box{height:78rpx;padding:0 22rpx;display:flex;align-items:center;gap:13rpx;border:1rpx solid #dfe4e7;border-radius:10rpx;background:#fff}.search-box input{flex:1;font-size:26rpx}.actions{margin:18rpx 0 24rpx;display:grid;grid-template-columns:repeat(3,1fr);gap:10rpx}.actions button{height:68rpx;line-height:68rpx;color:#176b5b;background:#e4f0ed;border-radius:9rpx;font-size:24rpx}.actions .scrap{color:#a73131;background:#f9e5e5}.part{margin-bottom:16rpx;padding:24rpx}.code{color:#176b5b;font-size:22rpx;font-weight:700}.low{padding:6rpx 12rpx;border-radius:20rpx;color:#a73131;background:#fae2e2;font-size:20rpx}.name{display:block;margin-top:15rpx;font-size:29rpx;font-weight:700}.model{display:block;margin-top:8rpx;color:#7b848d;font-size:22rpx}.stock{margin-top:18rpx;padding-top:16rpx;border-top:1rpx solid #edf0f1;color:#69737e;font-size:23rpx}.stock text:last-child{color:#17202a;font-size:30rpx;font-weight:800}</style>
