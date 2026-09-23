<script setup lang="ts">
import { getCurrentInstance, onMounted, ref } from "vue";

const emit = defineEmits<{ change: [path: string] }>();
const canvasId = `signature-${Math.random().toString(16).slice(2)}`;
const instance = getCurrentInstance();
const hasInk = ref(false);
const canvasSize = ref({ width: 0, height: 0 });
let context: UniApp.CanvasContext | null = null;
let drawing = false;

// The canvas is sized with rpx in CSS, so its real pixel size differs per
// device. Measure the rendered node and use those pixels for both clearing and
// exporting, instead of hardcoded values that crop or scale the signature.
function measure() {
  uni.createSelectorQuery()
    .in(instance)
    .select(`#${canvasId}`)
    .boundingClientRect((rect) => {
      const info = rect as UniApp.NodeInfo | null;
      if (info?.width && info?.height) canvasSize.value = { width: info.width, height: info.height };
    })
    .exec();
}
onMounted(measure);

function canvasWidth() { return canvasSize.value.width || 700; }
function canvasHeight() { return canvasSize.value.height || 280; }

function ensureContext() {
  if (!context) {
    context = uni.createCanvasContext(canvasId, instance);
    context.setStrokeStyle("#17202a");
    context.setLineWidth(3);
    context.setLineCap("round");
    context.setLineJoin("round");
  }
  return context;
}

function point(event: TouchEvent) {
  const touch = event.touches[0] as Touch & { x?: number; y?: number };
  return { x: touch.x ?? touch.clientX, y: touch.y ?? touch.clientY };
}

function start(event: TouchEvent) {
  const ctx = ensureContext();
  const p = point(event);
  drawing = true;
  ctx.beginPath();
  ctx.moveTo(p.x, p.y);
}

function move(event: TouchEvent) {
  if (!drawing) return;
  const ctx = ensureContext();
  const p = point(event);
  ctx.lineTo(p.x, p.y);
  ctx.stroke();
  ctx.draw(true);
  hasInk.value = true;
}

function end() { drawing = false; }

function clear() {
  const ctx = ensureContext();
  ctx.clearRect(0, 0, canvasWidth(), canvasHeight());
  ctx.draw();
  hasInk.value = false;
  emit("change", "");
}

function save() {
  if (!hasInk.value) { uni.showToast({ title: "请先完成签字", icon: "none" }); return; }
  const width = canvasWidth();
  const height = canvasHeight();
  uni.canvasToTempFilePath({
    canvasId,
    x: 0,
    y: 0,
    width,
    height,
    destWidth: width,
    destHeight: height,
    fileType: "png",
    quality: 1,
    success: (result) => emit("change", result.tempFilePath),
    fail: () => uni.showToast({ title: "签字保存失败", icon: "none" }),
  }, instance);
}
</script>

<template>
  <view class="signature-wrap">
    <canvas :canvas-id="canvasId" :id="canvasId" class="signature-canvas" disable-scroll @touchstart="start" @touchmove="move" @touchend="end" />
    <view class="signature-actions row-between"><text class="muted">请客户在上方签字</text><view><text class="text-btn" @click="clear">清除</text><text class="text-btn save" @click="save">确认签字</text></view></view>
  </view>
</template>

<style scoped>
.signature-wrap { border: 1rpx solid #dfe5e7; border-radius: 10rpx; overflow: hidden; background: #fff; }
.signature-canvas { width: 100%; height: 260rpx; background: repeating-linear-gradient(0deg, #fff, #fff 43rpx, #f1f3f4 44rpx); }
.signature-actions { min-height: 70rpx; padding: 0 18rpx; border-top: 1rpx solid #e6eaec; font-size: 22rpx; }
.text-btn { display: inline-block; padding: 16rpx 10rpx; color: #7c858d; }.text-btn.save { margin-left: 12rpx; color: #176b5b; font-weight: 700; }
</style>
