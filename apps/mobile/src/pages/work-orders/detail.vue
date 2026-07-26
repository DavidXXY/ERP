<script setup lang="ts">
import { computed, ref } from "vue";
import { onLoad, onShow } from "@dcloudio/uni-app";
import StateView from "@/components/StateView.vue";
import { acceptWorkOrder, downloadWorkOrderAttachment, getWorkOrder, listAssignees, reassignWorkOrder, uploadWorkOrderAttachment } from "@/api/maintenance";
import { useAuthStore } from "@/stores/auth";
import type { WorkOrder } from "@/types/domain";
import { createOperationId, persistOfflineFile, queueOperation } from "@/utils/offline";
import { dateText, shortDate, statusClass, statusLabels } from "@/utils/format";

const id = ref("");
const auth = useAuthStore();
const record = ref<WorkOrder | null>(null);
const loading = ref(true);
const error = ref("");
const acting = ref(false);
const canAccept = computed(() => record.value?.status === "ASSIGNED" && !record.value.assignmentAcceptedAt);
const canCheckIn = computed(() => record.value?.status === "ASSIGNED" && Boolean(record.value.assignmentAcceptedAt));
const canComplete = computed(() => record.value?.status === "IN_PROGRESS");

async function load() {
  if (!id.value) return;
  loading.value = !record.value; error.value = "";
  try {
    record.value = await getWorkOrder(id.value);
    if (record.value.attachments?.length) {
      await Promise.all(record.value.attachments.map(async (item) => {
        try { item.previewUrl = await downloadWorkOrderAttachment(item.id); } catch { item.previewUrl = ""; }
      }));
    }
  }
  catch (e) { error.value = (e as Error).message; }
  finally { loading.value = false; }
}

async function acceptAssignment() {
  const modal = await uni.showModal({ title: "确认接单", content: "接单后该工单会进入你的待执行列表。" });
  if (!modal.confirm) return;
  acting.value = true;
  try { record.value = await acceptWorkOrder(id.value, createOperationId("accept")); uni.showToast({ title: "接单成功", icon: "success" }); }
  catch (e) { uni.showToast({ title: (e as Error).message, icon: "none" }); }
  finally { acting.value = false; }
}

async function reassign() {
  try {
    const assignees = await listAssignees();
    const result = await uni.showActionSheet({ itemList: assignees.map((item) => item.displayName) });
    const target = assignees[result.tapIndex];
    if (!target) return;
    record.value = await reassignWorkOrder(id.value, target);
    uni.showToast({ title: "改派成功", icon: "success" });
  } catch (e) {
    const message = (e as Error).message;
    if (!message.includes("cancel")) uni.showToast({ title: message, icon: "none" });
  }
}

function addPhoto() {
  uni.chooseMedia({ count: 6, mediaType: ["image"], sourceType: ["camera", "album"], success: async (result) => {
    let queued = 0;
    for (const file of result.tempFiles) {
      try { await uploadWorkOrderAttachment(id.value, "SITE_PHOTO", file.tempFilePath); }
      catch {
        const savedPath = await persistOfflineFile(file.tempFilePath);
        queueOperation({ label: `${record.value?.code || "工单"}现场照片`, kind: "UPLOAD", upload: { url: `/maintenance/mobile/work-orders/${id.value}/attachments`, filePath: savedPath, formData: { category: "SITE_PHOTO" }, savedFile: savedPath !== file.tempFilePath } });
        queued++;
      }
    }
    uni.showToast({ title: queued ? `${queued}张照片等待同步` : "照片已上传", icon: queued ? "none" : "success" });
    await load();
  } });
}

onLoad((query) => { id.value = String(query?.id || ""); });
onShow(load);
</script>

<template>
  <view class="page-shell order-detail">
    <StateView :loading="loading" :error="error" :empty="!record" @retry="load">
      <template v-if="record">
        <view class="surface hero">
          <view class="row-between"><text class="code">{{ record.code }}</text><view class="row"><text v-if="auth.can('maintenance:order:manage') && !['ACCEPTED','CANCELLED'].includes(record.status)" class="reassign" @click.stop="reassign">改派</text><text class="status-pill" :class="statusClass(record.status)">{{ statusLabels[record.status] || record.status }}</text></view></view>
          <text class="title">{{ record.title }}</text>
          <view class="customer"><uni-icons type="person" size="18" color="#69737e" /><text>{{ record.customerName || "客户待补充" }}</text></view>
        </view>
        <view class="surface block">
          <text class="section-title">现场信息</text>
          <view class="address"><uni-icons type="location-filled" size="22" color="#176b5b" /><view><text>{{ record.siteAddress || record.checkInLocation || "现场地址待补充" }}</text><text v-if="record.checkInAt" class="muted">签到：{{ dateText(record.checkInAt) }}</text></view></view>
          <view class="info-grid">
            <view><text>计划日期</text><text>{{ shortDate(record.plannedDate) }}</text></view>
            <view><text>负责人</text><text>{{ record.assigneeName || "待指派" }}</text></view>
            <view><text>设备</text><text>{{ record.equipmentName || "-" }}</text></view>
            <view><text>优先级</text><text :class="{ urgent: record.priority === 'URGENT' }">{{ record.priority === "URGENT" ? "紧急" : record.priority }}</text></view>
          </view>
        </view>
        <view class="surface spare-entry" @click="uni.navigateTo({ url: '/pages/spares/index' })"><view><text>备件领用与归还</text><text>查看库存、办理领用、归还或报废</text></view><uni-icons type="right" size="18" color="#176b5b" /></view>
        <view class="surface block"><text class="section-title">问题描述</text><text class="description">{{ record.description || "未填写问题描述" }}</text></view>
        <view class="surface block">
          <view class="row-between"><text class="section-title">现场影像</text><text class="section-link" @click="addPhoto">+ 拍照上传</text></view>
          <scroll-view v-if="record.attachments?.length" scroll-x class="photo-strip"><view class="photo-row"><image v-for="file in record.attachments.filter((f) => f.category !== 'CUSTOMER_SIGNATURE')" :key="file.id" class="photo" :src="file.previewUrl || ''" mode="aspectFill" @click="uni.previewImage({ urls: record!.attachments!.filter((f) => f.previewUrl).map((f) => f.previewUrl!) })" /></view></scroll-view>
          <text v-else class="muted no-photo">尚未上传现场照片</text>
        </view>
        <view v-if="record.statusLogs?.length" class="surface block"><text class="section-title">处理轨迹</text><view v-for="log in record.statusLogs" :key="log.id" class="log-row"><view class="log-dot" /><view><text>{{ statusLabels[log.toStatus] || log.toStatus }} · {{ log.operatorName }}</text><text>{{ dateText(log.createdAt) }}<text v-if="log.comment"> · {{ log.comment }}</text></text></view></view></view>

        <view v-if="canAccept || canCheckIn || canComplete" class="fixed-actions">
          <button v-if="canAccept" class="primary-btn" :disabled="acting" @click="acceptAssignment"><uni-icons type="hand-up" size="20" color="#fff" />确认接单</button>
          <button v-else-if="canCheckIn" class="primary-btn" @click="uni.navigateTo({ url: `/pages/work-orders/check-in?id=${record.id}` })"><uni-icons type="location-filled" size="20" color="#fff" />到场签到</button>
          <button v-else-if="canComplete" class="primary-btn" @click="uni.navigateTo({ url: `/pages/work-orders/complete?id=${record.id}` })"><uni-icons type="checkmarkempty" size="20" color="#fff" />提交完工</button>
        </view>
      </template>
    </StateView>
  </view>
</template>

<style scoped>
.order-detail { padding-top: 24rpx; padding-bottom: 210rpx; }
.reassign { margin-right: 14rpx; color: #176b5b; font-size: 23rpx; font-weight: 700; }
.hero,.block { padding: 28rpx; margin-bottom: 18rpx; }.code { color: #176b5b; font-size: 23rpx; font-weight: 750; }.title { display: block; margin: 23rpx 0 17rpx; font-size: 38rpx; font-weight: 780; line-height: 1.35; }
.customer { display: flex; gap: 9rpx; align-items: center; color: #69737e; font-size: 25rpx; }.address { margin-top: 25rpx; padding: 22rpx; display: flex; gap: 15rpx; background: #eff6f4; border-radius: 10rpx; line-height: 1.5; }.address view { flex: 1; }.address text { display: block; }.address .muted { margin-top: 7rpx; font-size: 22rpx; }
.info-grid { margin-top: 18rpx; display: grid; grid-template-columns: 1fr 1fr; border-top: 1rpx solid #e7ebed; border-left: 1rpx solid #e7ebed; }.info-grid view { min-width: 0; padding: 20rpx; border-right: 1rpx solid #e7ebed; border-bottom: 1rpx solid #e7ebed; }.info-grid text { display: block; color: #7b848d; font-size: 22rpx; }.info-grid text:last-child { margin-top: 8rpx; color: #27323b; font-size: 25rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.info-grid .urgent { color: #c33b3b; font-weight: 700; }
.description { display: block; margin-top: 20rpx; color: #46515b; line-height: 1.7; }.no-photo { display: block; padding: 35rpx 0 10rpx; text-align: center; }.photo-strip { margin-top: 20rpx; white-space: nowrap; }.photo-row { display: flex; gap: 14rpx; }.photo { flex: 0 0 auto; width: 180rpx; height: 150rpx; border-radius: 9rpx; background: #e9edef; }
.log-row { min-height: 88rpx; margin-top: 20rpx; display: flex; gap: 16rpx; }.log-dot { width: 17rpx; height: 17rpx; margin-top: 7rpx; border-radius: 50%; background: #176b5b; }.log-row text { display: block; }.log-row text:last-child { margin-top: 7rpx; color: #7b848d; font-size: 22rpx; }
.fixed-actions { position: fixed; z-index: 10; left: 0; right: 0; bottom: 0; padding: 20rpx 28rpx calc(env(safe-area-inset-bottom) + 20rpx); background: rgba(255,255,255,.96); border-top: 1rpx solid #dfe5e7; }
.spare-entry { margin-bottom: 18rpx; padding: 24rpx 28rpx; display: flex; align-items: center; justify-content: space-between; }.spare-entry text { display: block; }.spare-entry text:first-child { font-weight: 700; }.spare-entry text:last-child { margin-top: 7rpx; color: #7b848d; font-size: 22rpx; }
</style>
