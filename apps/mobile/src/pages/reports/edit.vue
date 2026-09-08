<script setup lang="ts">
import { ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import {
  createReport,
  listCcCandidates,
  listMyProjects,
  type ReportCcOption,
  type ReportProjectOption,
} from "@/api/reporting";
function toLocalDate(d: Date) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

const category = ref<"DAILY" | "ENGINEERING">("DAILY");
const type = ref("DAILY");
const projectId = ref("");
const additionalProjectId = ref("");
const hours = ref<4 | 8 | 0>(0);
const projectOptions = ref<ReportProjectOption[]>([]);
const date = ref(toLocalDate(new Date()));
const content = ref("");
const progressSummary = ref("");
const planNext = ref("");
const issue = ref("");
const ccIds = ref<string[]>([]);
const ccOptions = ref<ReportCcOption[]>([]);
const saving = ref(false);

const categoryOptions = [
  { label: "日常汇报", value: "DAILY" },
  { label: "工程汇报", value: "ENGINEERING" },
];

const hoursOptions = [
  { label: "半日（4小时）", value: 4 },
  { label: "全日（8小时）", value: 8 },
];

function otherProjects() {
  return projectOptions.value.filter((p) => p.id !== projectId.value);
}

const typeOptions = [
  { label: "日报", value: "DAILY" },
  { label: "周报", value: "WEEKLY" },
  { label: "月报", value: "MONTHLY" },
];

onLoad(async () => {
  try {
    ccOptions.value = await listCcCandidates();
  } catch {}
  try {
    projectOptions.value = await listMyProjects();
  } catch {}
});

function toggleCc(id: string) {
  const idx = ccIds.value.indexOf(id);
  if (idx >= 0) ccIds.value.splice(idx, 1);
  else ccIds.value.push(id);
}

async function submit() {
  if (!content.value.trim()) {
    uni.showToast({ title: "请填写工作内容", icon: "none" });
    return;
  }
  if (category.value === "ENGINEERING" && !projectId.value) {
    uni.showToast({ title: "工程汇报请选择项目", icon: "none" });
    return;
  }
  if (category.value === "ENGINEERING" && !hours.value) {
    uni.showToast({ title: "请选择工时（半日/全日）", icon: "none" });
    return;
  }
  if (
    category.value === "ENGINEERING" &&
    hours.value === 4 &&
    additionalProjectId.value === projectId.value
  ) {
    uni.showToast({ title: "第二个项目不能与主项目相同", icon: "none" });
    return;
  }
  saving.value = true;
  try {
    await createReport({
      reportCategory: category.value,
      projectId: category.value === "ENGINEERING" ? projectId.value : undefined,
      additionalProjectId:
        category.value === "ENGINEERING" && hours.value === 4
          ? additionalProjectId.value || undefined
          : undefined,
      hours: category.value === "ENGINEERING" ? hours.value : undefined,
      reportType: type.value,
      reportDate: date.value,
      content: content.value.trim(),
      progressSummary: progressSummary.value.trim() || undefined,
      planNext: planNext.value.trim() || undefined,
      issue: issue.value.trim() || undefined,
      ccUserIds: ccIds.value,
    });
    uni.showToast({ title: "提交成功", icon: "success" });
    setTimeout(() => uni.navigateBack(), 600);
  } catch (e: any) {
    uni.showToast({ title: e.message || "提交失败", icon: "none" });
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <view class="page-shell app-page">
    <view class="intro">
      <text>填写汇报</text>
      <text>日报 / 周报 / 月报</text>
    </view>

    <view class="form surface">
      <view class="form-row">
        <text class="label">汇报类别</text>
        <view class="seg">
          <text
            v-for="opt in categoryOptions"
            :key="opt.value"
            :class="['seg-item', category === opt.value ? 'active' : '']"
            @click="category = opt.value as 'DAILY' | 'ENGINEERING'"
            >{{ opt.label }}</text
          >
        </view>
      </view>
      <view v-if="category === 'ENGINEERING'" class="form-row">
        <text class="label">所属项目</text>
        <picker
          mode="selector"
          :range="projectOptions.map((p) => p.code + ' ' + p.name)"
          @change="
            (e: any) =>
              (projectId = projectOptions[Number(e.detail.value)]?.id || '')
          "
        >
          <view class="picker-value">{{
            projectId
              ? projectOptions.find((p) => p.id === projectId)?.code +
                " " +
                projectOptions.find((p) => p.id === projectId)?.name
              : "请选择你加入的项目"
          }}</view>
        </picker>
        <text v-if="!projectOptions.length" class="hint"
          >暂无你加入的项目，请联系项目经理加入项目组</text
        >
      </view>
      <view v-if="category === 'ENGINEERING'" class="form-row">
        <text class="label">填报工时</text>
        <view class="seg">
          <text
            v-for="opt in hoursOptions"
            :key="opt.value"
            :class="['seg-item', hours === opt.value ? 'active' : '']"
            @click="hours = opt.value as 4 | 8"
            >{{ opt.label }}</text
          >
        </view>
        <text class="hint">最小为半日（4小时）；半日可选填第二个项目</text>
      </view>
      <view
        v-if="category === 'ENGINEERING' && hours === 4 && projectId"
        class="form-row"
      >
        <text class="label">第二个项目（可选）</text>
        <picker
          mode="selector"
          :range="otherProjects().map((p) => p.code + ' ' + p.name)"
          @change="
            (e: any) =>
              (additionalProjectId = otherProjects()[Number(e.detail.value)]?.id || '')
          "
        >
          <view class="picker-value">{{
            additionalProjectId
              ? otherProjects().find((p) => p.id === additionalProjectId)?.code +
                " " +
                otherProjects().find((p) => p.id === additionalProjectId)?.name
              : "选填：将另一半工时记到第二项目"
          }}</view>
        </picker>
      </view>
      <view class="form-row">
        <text class="label">汇报类型</text>
        <view class="seg">
          <text
            v-for="opt in typeOptions"
            :key="opt.value"
            :class="['seg-item', type === opt.value ? 'active' : '']"
            @click="type = opt.value"
            >{{ opt.label }}</text
          >
        </view>
      </view>
      <view class="form-row">
        <text class="label">汇报日期</text>
        <picker
          mode="date"
          :value="date"
          @change="(e: any) => (date = e.detail.value)"
        >
          <view class="picker-value">{{ date }}</view>
        </picker>
      </view>
      <view class="form-row">
        <text class="label">工作内容</text>
        <textarea
          v-model="content"
          class="textarea"
          placeholder="请描述今天/本周/本月完成的工作内容（必填）"
          :maxlength="20000"
        />
      </view>
      <view class="form-row">
        <text class="label">进度与达成</text>
        <textarea
          v-model="progressSummary"
          class="textarea short"
          placeholder="关键节点、指标完成情况（可选）"
          :maxlength="1000"
        />
      </view>
      <view class="form-row">
        <text class="label">下一步计划</text>
        <textarea
          v-model="planNext"
          class="textarea short"
          placeholder="后续工作安排（可选）"
          :maxlength="2000"
        />
      </view>
      <view class="form-row">
        <text class="label">问题与求助</text>
        <textarea
          v-model="issue"
          class="textarea short"
          placeholder="需要上级协调的问题（可选）"
          :maxlength="1000"
        />
      </view>
      <view class="form-row">
        <text class="label">抄送</text>
        <scroll-view scroll-x class="cc-scroll">
          <view class="cc-wrap">
            <text
              v-for="opt in ccOptions"
              :key="opt.id"
              :class="['cc-item', ccIds.includes(opt.id) ? 'active' : '']"
              @click="toggleCc(opt.id)"
              >{{ opt.name }}</text
            >
          </view>
        </scroll-view>
        <text v-if="!ccOptions.length" class="hint">暂无可抄送人员</text>
      </view>
    </view>

    <button class="primary-btn" :disabled="saving" @click="submit">
      {{ saving ? "提交中…" : "提交汇报" }}
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
.form {
  padding: 8rpx 24rpx;
}
.form-row {
  padding: 24rpx 0;
  border-bottom: 1rpx solid #eef1f4;
}
.form-row:last-child {
  border-bottom: none;
}
.label {
  display: block;
  font-size: 26rpx;
  color: #69737e;
  margin-bottom: 14rpx;
}
.seg {
  display: flex;
  gap: 16rpx;
}
.seg-item {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  border: 1rpx solid #d7dee3;
  border-radius: 10rpx;
  font-size: 28rpx;
  color: #25323a;
}
.seg-item.active {
  background: #176b5b;
  color: #fff;
  border-color: #176b5b;
}
.picker-value {
  padding: 16rpx 20rpx;
  background: #f5f7f9;
  border-radius: 10rpx;
  font-size: 28rpx;
}
.textarea {
  width: 100%;
  min-height: 200rpx;
  background: #f5f7f9;
  border-radius: 10rpx;
  padding: 20rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}
.textarea.short {
  min-height: 120rpx;
}
.cc-scroll {
  width: 100%;
}
.cc-wrap {
  display: flex;
  gap: 14rpx;
  white-space: nowrap;
}
.cc-item {
  display: inline-block;
  padding: 12rpx 24rpx;
  background: #f5f7f9;
  border-radius: 20rpx;
  font-size: 26rpx;
  color: #25323a;
  border: 1rpx solid transparent;
}
.cc-item.active {
  background: #e2f0ed;
  color: #176b5b;
  border-color: #176b5b;
}
.hint {
  color: #8b949c;
  font-size: 24rpx;
}
.primary-btn {
  margin-top: 32rpx;
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
.primary-btn[disabled] {
  opacity: 0.5;
}
</style>
