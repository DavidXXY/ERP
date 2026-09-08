<script setup lang="ts">
import { ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { listMyReports, type ReportRecord } from "@/api/reporting";
import StateView from "@/components/StateView.vue";
import { reportTypeLabel, dateText } from "@/utils/reportFormat";

const items = ref<ReportRecord[]>([]);
const loading = ref(true);
const error = ref("");

function hoursLabel(h?: number) {
  if (h == null) return "-";
  return h === 4 ? "半日(4h)" : h === 8 ? "全日(8h)" : `${h}h`;
}

async function load() {
  loading.value = true;
  error.value = "";
  try {
    items.value = await listMyReports();
  } catch (e: any) {
    error.value = e.message || "加载失败";
  } finally {
    loading.value = false;
  }
}
onShow(load);
</script>

<template>
  <view class="page-shell app-page">
    <view class="intro">
      <text>我的汇报</text>
      <text>日报 / 周报 / 月报，抄送会通知上级</text>
    </view>
    <StateView v-if="loading" state="loading" />
    <StateView v-else-if="error" state="error" :text="error" @retry="load" />
    <template v-else>
      <view
        v-for="item in items"
        :key="item.id"
        class="report-card surface"
        @click="uni.navigateTo({ url: `/pages/reports/detail?id=${item.id}` })"
      >
        <view class="report-head">
          <text class="tag">{{ reportTypeLabel(item.reportType) }}</text>
          <text v-if="item.reportCategory === 'ENGINEERING'" class="tag gold">{{
            item.projectName ? "工程·" + item.projectName : "工程汇报"
          }}</text>
          <text v-else class="tag cyan">日常汇报</text>
          <text class="date">{{ item.reportDate }}</text>
        </view>
        <view v-if="item.reportCategory === 'ENGINEERING'" class="hours-line">
          <text class="hours">工时 {{ hoursLabel(item.hours) }}</text>
          <text
            v-if="item.additionalProjectName"
            class="hours-sub"
            >另：{{ item.additionalProjectName }}</text
          >
          <text
            :class="[
              'status',
              item.reportStatus === 'CONFIRMED'
                ? 'ok'
                : item.reportStatus === 'REJECTED'
                  ? 'no'
                  : '',
            ]"
            >{{
              item.reportStatus === "CONFIRMED"
                ? "已确认"
                : item.reportStatus === "REJECTED"
                  ? "已驳回"
                  : "待确认"
            }}</text
          >
        </view>
        <text class="content">{{ item.content }}</text>
        <view class="meta">
          <text v-if="item.ccNames.length"
            >抄送：{{ item.ccNames.join("、") }}</text
          >
          <text v-else>无抄送</text>
          <text>{{ dateText(item.createdAt) }}</text>
        </view>
      </view>
      <view v-if="!items.length" class="empty surface">
        <text>还没有汇报，点击下方按钮填写</text>
      </view>
    </template>
    <button
      class="primary-btn"
      @click="uni.navigateTo({ url: '/pages/reports/edit' })"
    >
      填写汇报
    </button>
  </view>
</template>

<style scoped>
.app-page {
  padding-top: 30rpx;
  padding-bottom: 140rpx;
}
.intro {
  margin: 10rpx 4rpx 30rpx;
}
.intro text {
  display: block;
}
.intro text:first-child {
  font-size: 38rpx;
  font-weight: 780;
}
.intro text:last-child {
  margin-top: 10rpx;
  color: #69737e;
  font-size: 24rpx;
}
.report-card {
  padding: 24rpx;
  margin-bottom: 18rpx;
}
.report-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14rpx;
}
.tag {
  background: #e2f0ed;
  color: #176b5b;
  font-size: 24rpx;
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
}
.tag.gold {
  background: #fff3d6;
  color: #9a5c0d;
}
.tag.cyan {
  background: #e6f7ff;
  color: #0c6f9e;
}
.date {
  color: #78818a;
  font-size: 24rpx;
}
.content {
  font-size: 28rpx;
  color: #25323a;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.hours-line {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin: 12rpx 0 4rpx;
  font-size: 24rpx;
}
.hours {
  color: #9a5c0d;
  background: #fff3d6;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
}
.hours-sub {
  color: #69737e;
}
.status {
  color: #b25a00;
}
.status.ok {
  color: #176b5b;
}
.status.no {
  color: #c0392b;
}
.meta {
  margin-top: 16rpx;
  color: #8b949c;
  font-size: 22rpx;
  display: flex;
  justify-content: space-between;
}
.empty {
  padding: 60rpx 24rpx;
  text-align: center;
  color: #8b949c;
  font-size: 26rpx;
}
.primary-btn {
  position: fixed;
  left: 24rpx;
  right: 24rpx;
  bottom: 24rpx;
  min-height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #176b5b;
  color: #fff;
  border-radius: 12rpx;
  font-size: 30rpx;
  font-weight: 700;
}
</style>
