<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { listParts, scrapPart, type InventoryPart } from "@/api/inventory";
import { createOperationId } from "@/utils/offline";

const parts=ref<InventoryPart[]>([]);const form=reactive({partId:"",quantity:1,remark:""});const saving=ref(false);const partIndex=computed(()=>Math.max(0,parts.value.findIndex(i=>i.id===form.partId)));const selected=computed(()=>parts.value[partIndex.value]);
async function submit(){if(!form.partId||form.quantity<=0||!form.remark.trim()){uni.showToast({title:"请完整填写报废信息",icon:"none"});return;}if(form.quantity>Number(selected.value?.stockQty||0)){uni.showToast({title:"报废数量超过当前库存",icon:"none"});return;}const modal=await uni.showModal({title:"确认报废",content:`将报废 ${selected.value?.name} ${form.quantity} 件，此操作会扣减库存。`});if(!modal.confirm)return;saving.value=true;try{await scrapPart(form.partId,{movementType:"SCRAP",quantity:Number(form.quantity),sourceNo:createOperationId("MBSCR").slice(0,40),remark:form.remark.trim()});uni.showToast({title:"报废已登记",icon:"success"});setTimeout(()=>uni.navigateBack(),700);}catch(e){uni.showToast({title:(e as Error).message,icon:"none"});}finally{saving.value=false;}}
onMounted(async()=>{try{parts.value=await listParts();}catch(e){uni.showToast({title:(e as Error).message,icon:"none"});}});
</script>

<template><view class="page-shell form-page"><view class="warning"><uni-icons type="info" size="20" color="#a73131" /><text>报废会直接扣减可用库存，请确认实物状态和数量。</text></view><view class="surface form-card"><view class="field"><text class="field-label">备件 *</text><picker :range="parts" range-key="name" :value="partIndex" @change="form.partId=parts[Number(($event.detail as any).value)].id"><view class="field-picker">{{ form.partId?`${selected?.name} · 库存 ${selected?.stockQty}`:"请选择备件" }}</view></picker></view><view class="field"><text class="field-label">报废数量 *</text><input v-model.number="form.quantity" class="field-input" type="digit" /></view><view class="field"><text class="field-label">报废原因 *</text><textarea v-model="form.remark" class="field-textarea" maxlength="300" placeholder="说明损坏、过期或无法修复情况" /></view></view><button class="danger-btn" :disabled="saving" @click="submit">{{ saving?"正在提交":"确认报废并扣减库存" }}</button></view></template>

<style scoped>.form-page{padding-top:24rpx}.warning{margin-bottom:18rpx;padding:22rpx;display:flex;align-items:flex-start;gap:12rpx;border:1rpx solid #efcaca;border-radius:10rpx;background:#fcecec;color:#903030;font-size:23rpx;line-height:1.5}.form-card{padding:28rpx;margin-bottom:24rpx}</style>
