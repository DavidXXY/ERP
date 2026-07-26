<script setup lang="ts">
import { ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { checkInWorkOrder, uploadWorkOrderAttachment } from "@/api/maintenance";
import { createOperationId, persistOfflineFile, queueOperation } from "@/utils/offline";

const id = ref("");
const address = ref("");
const latitude = ref<number>();
const longitude = ref<number>();
const accuracy = ref<number>();
const photos = ref<string[]>([]);
const locating = ref(false);
const submitting = ref(false);

function locate() {
  locating.value = true;
  uni.chooseLocation({
    success(result) { address.value = result.address || result.name; latitude.value = result.latitude; longitude.value = result.longitude; accuracy.value = undefined; },
    fail() {
      uni.getLocation({ type: "gcj02", isHighAccuracy: true, success(result) { latitude.value = result.latitude; longitude.value = result.longitude; accuracy.value = result.accuracy; address.value = `坐标 ${result.latitude.toFixed(6)}, ${result.longitude.toFixed(6)}`; }, fail: () => uni.showToast({ title: "无法获取位置，请检查定位授权", icon: "none" }) });
    },
    complete() { locating.value = false; },
  });
}

function choosePhotos() {
  uni.chooseMedia({ count: Math.max(1, 6 - photos.value.length), mediaType: ["image"], sourceType: ["camera", "album"], success: (result) => { photos.value.push(...result.tempFiles.map((f) => f.tempFilePath)); } });
}

async function submit() {
  if (latitude.value == null || longitude.value == null || !address.value) { uni.showToast({ title: "请先获取现场位置", icon: "none" }); return; }
  submitting.value = true;
  const operationId = createOperationId("checkin");
  const payload = { operationId, checkInAt: new Date().toISOString(), checkInLocation: address.value, latitude: latitude.value, longitude: longitude.value, accuracy: accuracy.value };
  try {
    await checkInWorkOrder(id.value, payload);
    let queued = 0;
    for (const path of photos.value) {
      try { await uploadWorkOrderAttachment(id.value, "SITE_PHOTO", path); }
      catch { const savedPath = await persistOfflineFile(path); queueOperation({ label: "签到现场照片", kind: "UPLOAD", upload: { url: `/maintenance/mobile/work-orders/${id.value}/attachments`, filePath: savedPath, formData: { category: "SITE_PHOTO" }, savedFile: savedPath !== path } }); queued++; }
    }
    uni.showToast({ title: queued ? "签到成功，照片稍后同步" : "签到成功", icon: "success" });
    setTimeout(() => uni.navigateBack(), 700);
  } catch (e) {
    const message = (e as Error).message;
    if (message.includes("网络")) {
      for (const path of photos.value) { const savedPath = await persistOfflineFile(path); queueOperation({ label: "签到现场照片", kind: "UPLOAD", upload: { url: `/maintenance/mobile/work-orders/${id.value}/attachments`, filePath: savedPath, formData: { category: "SITE_PHOTO" }, savedFile: savedPath !== path } }); }
      queueOperation({ label: "工单现场签到", kind: "REQUEST", request: { url: `/maintenance/mobile/work-orders/${id.value}/check-in`, method: "PUT", data: payload } });
      uni.showToast({ title: "已保存，联网后自动同步", icon: "none" }); setTimeout(() => uni.navigateBack(), 900);
    } else uni.showToast({ title: message, icon: "none" });
  } finally { submitting.value = false; }
}
onLoad((query) => { id.value = String(query?.id || ""); setTimeout(locate, 300); });
</script>

<template>
  <view class="page-shell checkin-page">
    <view class="location-panel surface">
      <view class="radar"><view class="radar-core"><uni-icons type="location-filled" size="33" color="#fff" /></view></view>
      <text class="location-title">{{ locating ? "正在定位" : address || "等待获取现场位置" }}</text>
      <text v-if="latitude != null" class="coordinates">{{ latitude?.toFixed(6) }}, {{ longitude?.toFixed(6) }}<text v-if="accuracy"> · 精度 {{ accuracy.toFixed(0) }}m</text></text>
      <button class="secondary-btn locate-btn" @click="locate"><uni-icons type="refresh" size="18" color="#176b5b" />重新定位</button>
    </view>
    <view class="surface photo-panel"><view class="row-between"><text class="section-title">现场照片</text><text class="muted">最多 6 张</text></view><view class="photo-grid"><image v-for="(path,index) in photos" :key="path" class="photo" :src="path" mode="aspectFill" @click="uni.previewImage({ urls: photos, current: index })" /><view v-if="photos.length < 6" class="add-photo" @click="choosePhotos"><uni-icons type="camera-filled" size="29" color="#176b5b" /><text>拍照</text></view></view></view>
    <view class="privacy-note"><uni-icons type="locked" size="17" color="#69737e" /><text>定位仅用于本次工单的现场服务留痕</text></view>
    <button class="primary-btn submit" :disabled="submitting || locating" @click="submit">{{ submitting ? "正在提交" : "确认现场签到" }}</button>
  </view>
</template>

<style scoped>
.checkin-page { padding-top: 24rpx; }.location-panel { padding: 45rpx 28rpx 32rpx; text-align: center; }.radar { width: 180rpx; height: 180rpx; margin: 0 auto 28rpx; display: flex; align-items: center; justify-content: center; border-radius: 50%; background: #e0efeb; box-shadow: inset 0 0 0 25rpx #edf6f4; }.radar-core { width: 82rpx; height: 82rpx; display: flex; align-items: center; justify-content: center; border-radius: 50%; background: #176b5b; }.location-title { display: block; font-size: 29rpx; font-weight: 700; line-height: 1.5; }.coordinates { display: block; margin-top: 10rpx; color: #79838c; font-size: 22rpx; }.locate-btn { width: 280rpx; min-height: 70rpx; margin-top: 26rpx; font-size: 25rpx; }.photo-panel { margin-top: 20rpx; padding: 28rpx; }.photo-grid { margin-top: 24rpx; display: grid; grid-template-columns: repeat(3, 1fr); gap: 12rpx; }.photo,.add-photo { width: 100%; aspect-ratio: 1; border-radius: 9rpx; }.add-photo { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10rpx; border: 1rpx dashed #a9c5bf; background: #f3f8f7; color: #176b5b; font-size: 22rpx; }.privacy-note { margin: 25rpx 0; display: flex; justify-content: center; align-items: center; gap: 9rpx; color: #69737e; font-size: 22rpx; }.submit { margin-top: 10rpx; }
</style>
