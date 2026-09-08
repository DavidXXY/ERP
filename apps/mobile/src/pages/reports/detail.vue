<script setup lang="ts">
import { ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { listMyReports, type ReportRecord } from "@/api/reporting";
import StateView from "@/components/StateView.vue";
import { reportTypeLabel, dateText } from "@/utils/reportFormat";

function hoursLabel(h?: number) {
  if (h == null) return "-";
  return h === 4 ? "半日（4小时）" : h === 8 ? "全日（8小时）" : `${h} 小时`;
}

const record = ref<ReportRecord | null>(null);
const loading = ref(true);
const error = ref("");

onLoad(async (query) => {
  try {
    const list = await listMyReports();
    record.value = list.find((r) => r.id === query?.id) || null;
  } catch (e: any) {
    error.value = e.message || "加载失败";
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <view class="page-shell app-page">
    <StateView v-if="loading" state="loading" />
    <StateView v-else-if="error" state="error" :text="error" />
    <view v-else-if="record" class="detail surface">
      <view class="head">
        <text class="tag">{{
          record.reportCategory === "ENGINEERING" ? "工程汇报" : "日常汇报"
        }}</text>
        <text class="tag">{{ reportTypeLabel(record.reportType) }}</text>
        <text class="date">{{ record.reportDate }}</text>
      </view>
      <view v-if="record.projectName" class="block">
        <text class="label">所属项目</text>
        <text class="value">{{ record.projectName }}</text>
      </view>
      <view v-if="record.reportCategory === 'ENGINEERING'" class="block">
        <text class="label">填报工时</text>
        <text class="value">{{ hoursLabel(record.hours) }}</text>
      </view>
      <view
        v-if="record.reportCategory === 'ENGINEERING' && record.additionalProjectName"
        class="block"
      >
        <text class="label">第二项目</text>
        <text class="value">{{ record.additionalProjectName }}</text>
      </view>
      <view v-if="record.reportCategory === 'ENGINEERING'" class="block">
        <text class="label">确认状态</text>
        <text class="value">{{
          record.reportStatus === "CONFIRMED"
            ? "已确认，工时已计入项目实际工时"
            : record.reportStatus === "REJECTED"
              ? "已驳回"
              : "待项目经理确认"
        }}</text>
      </view>
      <view class="block">
        <text class="label">工作内容</text>
        <text class="value">{{ record.content }}</text>
      </view>
      <view v-if="record.progressSummary" class="block">
        <text class="label">进度与达成</text>
        <text class="value">{{ record.progressSummary }}</text>
      </view>
      <view v-if="record.planNext" class="block">
        <text class="label">下一步计划</text>
        <text class="value">{{ record.planNext }}</text>
      </view>
      <view v-if="record.issue" class="block">
        <text class="label">问题与求助</text>
        <text class="value">{{ record.issue }}</text>
      </view>
      <view class="block">
        <text class="label">抄送</text>
        <text class="value">{{
          record.ccNames.length ? record.ccNames.join("、") : "无抄送"
        }}</text>
      </view>
      <view class="meta">提交时间 {{ dateText(record.createdAt) }}</view>
    </view>
    <StateView v-else state="empty" text="未找到该汇报" />
  </view>
</template>

<style scoped>
.app-page {
  padding-top: 30rpx;
}
.detail {
  padding: 28rpx 24rpx;
}
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}
.tag {
  background: #e2f0ed;
  color: #176b5b;
  font-size: 24rpx;
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
}
.date {
  color: #78818a;
  font-size: 24rpx;
}
.block {
  margin-bottom: 24rpx;
}
.label {
  display: block;
  font-size: 24rpx;
  color: #69737e;
  margin-bottom: 10rpx;
}
.value {
  display: block;
  font-size: 28rpx;
  color: #25323a;
  line-height: 1.7;
  white-space: pre-wrap;
}
.meta {
  color: #8b949c;
  font-size: 22rpx;
  margin-top: 8rpx;
}
</style>
