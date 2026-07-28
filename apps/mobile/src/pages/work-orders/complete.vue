<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import SignaturePad from "@/components/SignaturePad.vue";
import {
  completeWorkOrder,
  uploadWorkOrderAttachment,
} from "@/api/maintenance";
import {
  createOperationId,
  persistOfflineFile,
  queueOperation,
} from "@/utils/offline";

const id = ref("");
const submitting = ref(false);
const photos = ref<string[]>([]);
const signaturePath = ref("");
const materials = ref<
  Array<{ partName: string; quantity: number; unitCost: number }>
>([]);
const materialDraft = reactive({ partName: "", quantity: 1, unitCost: 0 });
const form = reactive({
  serviceResult: "",
  remarks: "",
  laborHours: 1,
  laborCost: 0,
  travelCost: 0,
  outsourcingCost: 0,
  billableAmount: 0,
  customerSigner: "",
});
const materialCost = computed(() =>
  materials.value.reduce(
    (sum, item) =>
      sum + Number(item.quantity || 0) * Number(item.unitCost || 0),
    0,
  ),
);
const totalCost = computed(
  () =>
    Number(form.laborCost || 0) +
    Number(form.travelCost || 0) +
    Number(form.outsourcingCost || 0) +
    materialCost.value,
);

function addMaterial() {
  if (!materialDraft.partName.trim() || materialDraft.quantity <= 0) {
    uni.showToast({ title: "请填写材料名称和数量", icon: "none" });
    return;
  }
  materials.value.push({
    partName: materialDraft.partName.trim(),
    quantity: Number(materialDraft.quantity),
    unitCost: Number(materialDraft.unitCost || 0),
  });
  Object.assign(materialDraft, { partName: "", quantity: 1, unitCost: 0 });
}

function choosePhotos() {
  uni.chooseMedia({
    count: Math.max(1, 9 - photos.value.length),
    mediaType: ["image"],
    sourceType: ["camera", "album"],
    success: (result) =>
      photos.value.push(...result.tempFiles.map((f) => f.tempFilePath)),
  });
}

async function submit() {
  if (!form.serviceResult.trim()) {
    uni.showToast({ title: "请填写服务结果", icon: "none" });
    return;
  }
  if (!form.customerSigner.trim()) {
    uni.showToast({ title: "请填写客户签字人", icon: "none" });
    return;
  }
  if (!signaturePath.value) {
    uni.showToast({ title: "请完成并确认客户签字", icon: "none" });
    return;
  }
  const modal = await uni.showModal({
    title: "确认提交完工",
    content: "提交后工单进入待验收状态，现场记录仍可追溯。",
  });
  if (!modal.confirm) return;
  submitting.value = true;
  const payload = {
    operationId: createOperationId("complete"),
    laborHours: Number(form.laborHours || 0),
    laborCost: Number(form.laborCost || 0),
    materialCost: materialCost.value,
    travelCost: Number(form.travelCost || 0),
    outsourcingCost: Number(form.outsourcingCost || 0),
    costAmount: totalCost.value,
    billableAmount: Number(form.billableAmount || 0),
    serviceResult: form.serviceResult.trim(),
    remarks: form.remarks.trim(),
    customerSigner: form.customerSigner.trim(),
    materials: materials.value.map((m) => ({
      ...m,
      amount: Number(m.quantity) * Number(m.unitCost),
    })),
  };
  try {
    for (const path of photos.value)
      await uploadWorkOrderAttachment(id.value, "RESULT_PHOTO", path);
    await uploadWorkOrderAttachment(
      id.value,
      "CUSTOMER_SIGNATURE",
      signaturePath.value,
    );
    await completeWorkOrder(id.value, payload);
    uni.showToast({ title: "完工已提交", icon: "success" });
    setTimeout(() => uni.navigateBack(), 800);
  } catch (e) {
    const message = (e as Error).message;
    if (message.includes("网络")) {
      for (const path of photos.value) {
        const savedPath = await persistOfflineFile(path);
        queueOperation({
          label: "完工现场照片",
          kind: "UPLOAD",
          upload: {
            url: `/maintenance/mobile/work-orders/${id.value}/attachments`,
            filePath: savedPath,
            formData: { category: "RESULT_PHOTO" },
            savedFile: savedPath !== path,
          },
        });
      }
      const savedSignature = await persistOfflineFile(signaturePath.value);
      queueOperation({
        label: "客户签字",
        kind: "UPLOAD",
        upload: {
          url: `/maintenance/mobile/work-orders/${id.value}/attachments`,
          filePath: savedSignature,
          formData: { category: "CUSTOMER_SIGNATURE" },
          savedFile: savedSignature !== signaturePath.value,
        },
      });
      queueOperation({
        label: "工单完工记录",
        kind: "REQUEST",
        request: {
          url: `/maintenance/mobile/work-orders/${id.value}/complete`,
          method: "PUT",
          data: payload,
        },
      });
      uni.showToast({ title: "已保存，联网后自动同步", icon: "none" });
      setTimeout(() => uni.navigateBack(), 900);
    } else uni.showToast({ title: message, icon: "none", duration: 2500 });
  } finally {
    submitting.value = false;
  }
}
onLoad((query) => {
  id.value = String(query?.id || "");
});
</script>

<template>
  <view class="page-shell complete-page">
    <view class="surface form-section"
      ><text class="section-title">服务结果</text
      ><view class="field top-field"
        ><text class="field-label">处理说明 *</text
        ><textarea
          v-model="form.serviceResult"
          class="field-textarea"
          maxlength="1500"
          placeholder="说明故障原因、处理过程和最终结果"
        /></view
      ><view class="field"
        ><text class="field-label">补充备注</text
        ><textarea
          v-model="form.remarks"
          class="field-textarea small"
          maxlength="500"
          placeholder="选填"
        /></view
    ></view>
    <view class="surface form-section"
      ><view class="row-between"
        ><text class="section-title">完工照片</text
        ><text class="muted">最多 9 张</text></view
      ><view class="photo-grid"
        ><view v-for="(path, index) in photos" :key="path" class="photo-wrap"
          ><image class="photo" :src="path" mode="aspectFill" /><text
            class="remove"
            @click="photos.splice(index, 1)"
            >×</text
          ></view
        ><view v-if="photos.length < 9" class="add-photo" @click="choosePhotos"
          ><uni-icons type="camera-filled" size="29" color="#176b5b" /><text
            >添加照片</text
          ></view
        ></view
      ></view
    >
    <view class="surface form-section"
      ><view class="row-between"
        ><text class="section-title">使用材料</text
        ><text class="amount-total"
          >材料金额（含税，元）{{ materialCost.toFixed(2) }}</text
        ></view
      ><view
        v-for="(item, index) in materials"
        :key="`${item.partName}-${index}`"
        class="material-row"
        ><text>{{ item.partName }}</text
        ><text
          >{{ item.quantity }} × {{ item.unitCost.toFixed(2) }} 元（含税）</text
        ><uni-icons
          type="trash"
          size="18"
          color="#b53232"
          @click="materials.splice(index, 1)" /></view
      ><view class="material-form"
        ><input
          v-model="materialDraft.partName"
          class="mini-input name"
          placeholder="材料名称"
        /><input
          v-model.number="materialDraft.quantity"
          class="mini-input"
          type="digit"
          placeholder="数量"
        /><input
          v-model.number="materialDraft.unitCost"
          class="mini-input"
          type="digit"
          placeholder="含税单价（元）"
        /><button class="add-btn" @click="addMaterial">添加</button></view
      ></view
    >
    <view class="surface form-section"
      ><text class="section-title">工时与费用</text
      ><view class="cost-grid"
        ><view class="field"
          ><text class="field-label">工时</text
          ><input
            v-model.number="form.laborHours"
            class="field-input"
            type="digit" /></view
        ><view class="field"
          ><text class="field-label">人工费（元，税价不适用）</text
          ><input
            v-model.number="form.laborCost"
            class="field-input"
            type="digit" /></view
        ><view class="field"
          ><text class="field-label">差旅费（含税，元）</text
          ><input
            v-model.number="form.travelCost"
            class="field-input"
            type="digit" /></view
        ><view class="field"
          ><text class="field-label">外包费（含税，元）</text
          ><input
            v-model.number="form.outsourcingCost"
            class="field-input"
            type="digit" /></view></view
      ><view class="row-between cost-total"
        ><text>成本合计（元，税价随来源单据）</text
        ><text>¥{{ totalCost.toFixed(2) }}</text></view
      ></view
    >
    <view class="surface form-section"
      ><text class="section-title">客户验收签字</text
      ><view class="field top-field"
        ><text class="field-label">签字人 *</text
        ><input
          v-model="form.customerSigner"
          class="field-input"
          maxlength="80"
          placeholder="请输入客户签字人姓名" /></view
      ><SignaturePad @change="signaturePath = $event" /><view
        v-if="signaturePath"
        class="signature-ok"
        ><uni-icons
          type="checkmarkempty"
          size="18"
          color="#176b5b"
        />签字已确认</view
      ></view
    >
    <button class="primary-btn submit" :disabled="submitting" @click="submit">
      {{ submitting ? "正在提交" : "提交完工记录" }}
    </button>
  </view>
</template>

<style scoped>
.complete-page {
  padding-top: 24rpx;
}
.form-section {
  padding: 28rpx;
  margin-bottom: 18rpx;
}
.top-field {
  margin-top: 26rpx;
}
.field-textarea.small {
  min-height: 120rpx;
}
.photo-grid {
  margin-top: 24rpx;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12rpx;
}
.photo-wrap,
.add-photo {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
}
.photo {
  width: 100%;
  height: 100%;
  border-radius: 9rpx;
}
.remove {
  position: absolute;
  right: -7rpx;
  top: -8rpx;
  width: 35rpx;
  height: 35rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: #fff;
  background: #b53232;
}
.add-photo {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  border: 1rpx dashed #a9c5bf;
  border-radius: 9rpx;
  background: #f3f8f7;
  color: #176b5b;
  font-size: 21rpx;
}
.amount-total {
  color: #176b5b;
  font-weight: 700;
}
.material-row {
  min-height: 70rpx;
  display: grid;
  grid-template-columns: 1fr auto 40rpx;
  align-items: center;
  gap: 10rpx;
  border-bottom: 1rpx solid #edf0f1;
  font-size: 24rpx;
}
.material-row text:nth-child(2) {
  color: #69737e;
}
.material-form {
  margin-top: 20rpx;
  display: grid;
  grid-template-columns: 1.5fr 0.7fr 0.8fr 0.65fr;
  gap: 8rpx;
}
.mini-input {
  width: 100%;
  height: 66rpx;
  padding: 0 12rpx;
  border: 1rpx solid #dfe5e7;
  border-radius: 8rpx;
  font-size: 22rpx;
}
.add-btn {
  height: 66rpx;
  line-height: 66rpx;
  padding: 0;
  border-radius: 8rpx;
  color: #176b5b;
  background: #e4f0ed;
  font-size: 22rpx;
}
.cost-grid {
  margin-top: 25rpx;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 18rpx;
}
.cost-total {
  padding-top: 20rpx;
  border-top: 1rpx solid #e4e9eb;
  font-weight: 700;
}
.cost-total text:last-child {
  color: #176b5b;
  font-size: 32rpx;
}
.signature-ok {
  margin-top: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  color: #176b5b;
  font-size: 23rpx;
}
.submit {
  margin-top: 26rpx;
}
</style>
