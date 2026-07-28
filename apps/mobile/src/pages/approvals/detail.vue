<script setup lang="ts">
import { computed, ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import StateView from "@/components/StateView.vue";
import { getApproval, processApproval } from "@/api/office";
import { useAuthStore } from "@/stores/auth";
import type { Approval } from "@/types/domain";
import {
  approvalTypeLabels,
  dateText,
  money,
  statusClass,
  statusLabels,
} from "@/utils/format";

const auth = useAuthStore();
const id = ref("");
const record = ref<Approval | null>(null);
const loading = ref(true);
const error = ref("");
const comment = ref("");
const submitting = ref(false);
const canProcess = computed(
  () => record.value?.status === "PENDING" && record.value.canApprove !== false,
);

async function load() {
  loading.value = true;
  error.value = "";
  try {
    record.value = await getApproval(id.value);
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    loading.value = false;
  }
}

async function process(decision: "APPROVED" | "REJECTED") {
  if (decision === "REJECTED" && !comment.value.trim()) {
    uni.showToast({ title: "驳回时请填写意见", icon: "none" });
    return;
  }
  const modal = await uni.showModal({
    title: decision === "APPROVED" ? "确认通过" : "确认驳回",
    content: `将以 ${auth.user?.displayName || "当前账号"} 的身份提交处理结果。`,
  });
  if (!modal.confirm) return;
  submitting.value = true;
  try {
    record.value = await processApproval(
      id.value,
      decision,
      comment.value.trim() || "同意",
      auth.user?.displayName || auth.user?.username || "移动端用户",
    );
    uni.showToast({ title: "处理成功", icon: "success" });
  } catch (e) {
    uni.showToast({ title: (e as Error).message, icon: "none" });
  } finally {
    submitting.value = false;
  }
}

onLoad((query) => {
  id.value = String(query?.id || "");
  void load();
});
</script>

<template>
  <view class="page-shell detail-page">
    <StateView :loading="loading" :error="error" :empty="!record" @retry="load">
      <template v-if="record">
        <view class="hero surface">
          <view class="row-between"
            ><text class="type">{{
              approvalTypeLabels[record.approvalType] || record.approvalType
            }}</text
            ><text class="status-pill" :class="statusClass(record.status)">{{
              statusLabels[record.status] || record.status
            }}</text></view
          >
          <text class="title">{{ record.title }}</text
          ><text class="code">{{ record.code }}</text>
          <view v-if="record.amount != null" class="amount"
            >金额（元，税价随来源单据）{{ money(record.amount) }}</view
          >
        </view>
        <view class="surface info-block">
          <text class="section-title">申请信息</text>
          <view class="info-row"
            ><text>申请人</text><text>{{ record.applicantName }}</text></view
          >
          <view class="info-row"
            ><text>所属部门</text
            ><text>{{ record.departmentName || "-" }}</text></view
          >
          <view class="info-row"
            ><text>提交时间</text
            ><text>{{ dateText(record.createdAt) }}</text></view
          >
          <view class="reason"
            ><text>申请说明</text
            ><text>{{
              record.reason || record.content || "未填写补充说明"
            }}</text></view
          >
        </view>
        <view class="surface info-block">
          <text class="section-title">审批流程</text>
          <view v-if="record.nodes?.length" class="timeline">
            <view
              v-for="node in record.nodes"
              :key="node.id"
              class="timeline-row"
              ><view
                class="dot"
                :class="{
                  done:
                    node.nodeStatus === 'APPROVED' ||
                    node.nodeStatus === 'COMPLETED',
                }"
              /><view
                ><text class="node-name">{{ node.nodeName || "审批节点" }}</text
                ><text class="node-meta"
                  >{{ node.assigneeName || "待分配" }} ·
                  {{
                    statusLabels[node.nodeStatus || ""] ||
                    node.nodeStatus ||
                    "待处理"
                  }}</text
                ></view
              ></view
            >
          </view>
          <text v-else class="muted flow-empty">暂无流程节点记录</text>
        </view>
        <view v-if="canProcess" class="surface action-panel">
          <text class="field-label">审批意见</text
          ><textarea
            v-model="comment"
            class="field-textarea"
            maxlength="500"
            placeholder="填写处理意见，驳回时必填"
          />
          <view class="action-grid"
            ><button
              class="danger-btn"
              :disabled="submitting"
              @click="process('REJECTED')"
            >
              驳回</button
            ><button
              class="primary-btn"
              :disabled="submitting"
              @click="process('APPROVED')"
            >
              通过
            </button></view
          >
        </view>
      </template>
    </StateView>
  </view>
</template>

<style scoped>
.detail-page {
  padding-top: 24rpx;
}
.hero {
  padding: 30rpx;
}
.type {
  color: #176b5b;
  font-size: 24rpx;
  font-weight: 700;
}
.title {
  display: block;
  margin: 25rpx 0 12rpx;
  font-size: 38rpx;
  font-weight: 780;
  line-height: 1.35;
}
.code {
  color: #7b848d;
  font-size: 23rpx;
}
.amount {
  margin-top: 25rpx;
  padding-top: 22rpx;
  border-top: 1rpx solid #e8ecee;
  font-size: 40rpx;
  font-weight: 800;
}
.info-block {
  margin-top: 20rpx;
  padding: 28rpx;
}
.info-row {
  display: flex;
  justify-content: space-between;
  gap: 30rpx;
  padding: 22rpx 0;
  border-bottom: 1rpx solid #edf0f1;
  color: #69737e;
  font-size: 25rpx;
}
.info-row text:last-child {
  color: #28323c;
  text-align: right;
}
.reason {
  padding-top: 22rpx;
}
.reason text {
  display: block;
  color: #69737e;
  font-size: 25rpx;
}
.reason text:last-child {
  margin-top: 12rpx;
  color: #28323c;
  line-height: 1.65;
}
.timeline {
  margin-top: 24rpx;
}
.timeline-row {
  min-height: 95rpx;
  display: flex;
  gap: 20rpx;
  position: relative;
}
.timeline-row:not(:last-child)::after {
  content: "";
  position: absolute;
  left: 10rpx;
  top: 24rpx;
  bottom: 0;
  width: 2rpx;
  background: #dce4e2;
}
.dot {
  z-index: 1;
  width: 22rpx;
  height: 22rpx;
  margin-top: 5rpx;
  border-radius: 50%;
  border: 5rpx solid #d7dedc;
  background: #fff;
}
.dot.done {
  border-color: #176b5b;
}
.node-name,
.node-meta {
  display: block;
}
.node-name {
  font-weight: 650;
}
.node-meta {
  margin-top: 8rpx;
  color: #7b848d;
  font-size: 23rpx;
}
.flow-empty {
  display: block;
  padding: 30rpx 0 10rpx;
}
.action-panel {
  margin-top: 20rpx;
  padding: 28rpx;
}
.action-grid {
  margin-top: 22rpx;
  display: grid;
  grid-template-columns: 1fr 1.6fr;
  gap: 16rpx;
}
</style>
