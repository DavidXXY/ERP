<template>
  <div class="page-stack self-page">
    <a-card title="下属汇报">
      <template #extra>
        <a-space wrap class="self-card-toolbar">
          <a-button @click="$router.push('/self')">返回工作台</a-button>
        </a-space>
      </template>

      <a-alert
        v-if="!hasSubordinates"
        class="section-alert"
        type="info"
        show-icon
        message="当前展示直属及下级组织内员工的汇报，以及抄送给你的汇报。若未看到数据，可能当前账号组织下暂无下属员工。"
      />

      <a-row :gutter="[16, 16]" class="inbox-filters">
        <a-col :xs="24" :sm="8"
          ><a-select
            v-model:value="filters.type"
            :options="[
              { value: '', label: '全部类型' },
              { value: 'DAILY', label: '日报' },
              { value: 'WEEKLY', label: '周报' },
              { value: 'MONTHLY', label: '月报' },
            ]"
            style="width: 100%"
            @change="load"
        /></a-col>
        <a-col :xs="12" :sm="8"
          ><a-date-picker
            v-model:value="filters.fromDate"
            value-format="YYYY-MM-DD"
            placeholder="开始日期"
            style="width: 100%"
            @change="load"
        /></a-col>
        <a-col :xs="12" :sm="8"
          ><a-date-picker
            v-model:value="filters.toDate"
            value-format="YYYY-MM-DD"
            placeholder="结束日期"
            style="width: 100%"
            @change="load"
        /></a-col>
      </a-row>

      <a-table
        :data-source="data"
        :columns="columns"
        row-key="id"
        size="middle"
        :pagination="{ pageSize: 20 }"
        :loading="loading"
        :scroll="{ x: 1000 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'employeeName'">
            <strong>{{ record.employeeName }}</strong
            ><br /><small class="muted">{{
              record.departmentName || "-"
            }}</small>
          </template>
          <template v-else-if="column.key === 'reportType'"
            ><a-space :size="4" wrap>
              <a-tag :color="typeColor(record.reportType)">{{
                typeLabel(record.reportType)
              }}</a-tag>
              <a-tag v-if="record.reportCategory === 'ENGINEERING'" color="gold"
                >工程</a-tag
              >
            </a-space></template
          >
          <template v-else-if="column.key === 'projectName'">
            <template v-if="record.reportCategory === 'ENGINEERING'">
              <div>{{ record.projectName }}</div>
              <div v-if="record.hours" class="muted">
                工时 {{ hoursLabel(record.hours) }}
                <a-tag v-if="record.reportStatus === 'CONFIRMED'" color="green"
                  >已确认</a-tag
                >
                <a-tag v-else-if="record.reportStatus === 'REJECTED'" color="red"
                  >已驳回</a-tag
                >
                <a-tag v-else color="orange">待确认</a-tag>
              </div>
              <div v-if="record.additionalProjectName" class="muted">
                另:{{ record.additionalProjectName }}
              </div>
            </template>
            <span v-else-if="record.projectName">{{ record.projectName }}</span>
            <span v-else class="muted">-</span>
          </template>
          <template v-else-if="column.key === 'reportDate'">{{
            record.reportDate
          }}</template>
          <template v-else-if="column.key === 'content'">
            <a-typography-paragraph
              style="margin-bottom: 0"
              :ellipsis="{ rows: 1, expandable: false }"
              >{{ record.content }}</a-typography-paragraph
            >
          </template>
          <template v-else-if="column.key === 'ccNames'">
            <span v-if="record.ccNames.length">{{
              record.ccNames.join("、")
            }}</span>
            <span v-else class="muted">-</span>
          </template>
          <template v-else-if="column.key === 'createdAt'">{{
            dateTime(record.createdAt)
          }}</template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record)"
              >查看</a-button
            >
          </template>
        </template>
      </a-table>
    </a-card>

    <a-drawer
      :open="drawer.open"
      title="汇报详情"
      :width="Math.min(680, windowWidth - 24)"
      destroy-on-close
      @close="drawer.open = false"
    >
      <a-descriptions v-if="drawer.record" :column="1" bordered size="small">
        <a-descriptions-item label="汇报人"
          >{{ drawer.record.employeeName }}
          <small class="muted"
            >（{{ drawer.record.departmentName || "未分配组织" }}）</small
          ></a-descriptions-item
        >
        <a-descriptions-item label="汇报类别">
          <a-tag
            :color="
              drawer.record.reportCategory === 'ENGINEERING' ? 'gold' : 'cyan'
            "
            >{{
              drawer.record.reportCategory === "ENGINEERING"
                ? "工程汇报"
                : "日常汇报"
            }}</a-tag
          >
        </a-descriptions-item>
        <a-descriptions-item label="汇报类型">
          <a-tag :color="typeColor(drawer.record.reportType)">{{
            typeLabel(drawer.record.reportType)
          }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item
          v-if="drawer.record.projectName"
          label="所属项目"
          >{{ drawer.record.projectName }}</a-descriptions-item
        >
        <a-descriptions-item
          v-if="
            drawer.record.reportCategory === 'ENGINEERING' &&
            drawer.record.additionalProjectName
          "
          label="第二项目"
          >{{ drawer.record.additionalProjectName }}</a-descriptions-item
        >
        <a-descriptions-item
          v-if="drawer.record.reportCategory === 'ENGINEERING'"
          label="填报工时"
          >{{ hoursLabel(drawer.record.hours) }}</a-descriptions-item
        >
        <a-descriptions-item
          v-if="drawer.record.reportCategory === 'ENGINEERING'"
          label="确认状态"
        >
          <a-tag
            v-if="drawer.record.reportStatus === 'CONFIRMED'"
            color="green"
            >已确认</a-tag
          >
          <a-tag
            v-else-if="drawer.record.reportStatus === 'REJECTED'"
            color="red"
            >已驳回</a-tag
          >
          <a-tag v-else color="orange">待项目经理确认</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="汇报日期">{{
          drawer.record.reportDate
        }}</a-descriptions-item>
        <a-descriptions-item label="提交时间">{{
          dateTime(drawer.record.createdAt)
        }}</a-descriptions-item>
        <a-descriptions-item label="抄送">{{
          drawer.record.ccNames.length
            ? drawer.record.ccNames.join("、")
            : "无抄送"
        }}</a-descriptions-item>
        <a-descriptions-item label="工作内容" :span="3">{{
          drawer.record.content || "-"
        }}</a-descriptions-item>
        <a-descriptions-item
          v-if="drawer.record.progressSummary"
          label="进度/达成"
          :span="3"
          >{{ drawer.record.progressSummary }}</a-descriptions-item
        >
        <a-descriptions-item
          v-if="drawer.record.planNext"
          label="下一步计划"
          :span="3"
          >{{ drawer.record.planNext }}</a-descriptions-item
        >
        <a-descriptions-item
          v-if="drawer.record.issue"
          label="问题/求助"
          :span="3"
          >{{ drawer.record.issue }}</a-descriptions-item
        >
      </a-descriptions>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import { getReceivedReports, type ReportRecord } from "@/api/reporting";

const loading = ref(false);
const data = ref<ReportRecord[]>([]);
const windowWidth = ref(window.innerWidth);
window.addEventListener(
  "resize",
  () => (windowWidth.value = window.innerWidth),
);

const filters = reactive<{ type: string; fromDate?: string; toDate?: string }>({
  type: "",
  fromDate: undefined,
  toDate: undefined,
});

const drawer = reactive<{ open: boolean; record: ReportRecord | null }>({
  open: false,
  record: null,
});

const columns = [
  { title: "汇报人", key: "employeeName", width: 160 },
  { title: "类型", key: "reportType", width: 110 },
  { title: "项目", key: "projectName", width: 180 },
  { title: "汇报日期", key: "reportDate", width: 120 },
  { title: "工作内容", key: "content" },
  { title: "抄送", key: "ccNames", width: 200 },
  { title: "提交时间", key: "createdAt", width: 170 },
  { title: "操作", key: "action", width: 80 },
];

const hasSubordinates = ref(true);

function typeLabel(t: string) {
  return { DAILY: "日报", WEEKLY: "周报", MONTHLY: "月报" }[t] || t;
}
function hoursLabel(h?: number) {
  if (h == null) return "-";
  return h === 4 ? "半日（4小时）" : h === 8 ? "全日（8小时）" : `${h} 小时`;
}
function typeColor(t: string) {
  return { DAILY: "blue", WEEKLY: "purple", MONTHLY: "green" }[t] || "default";
}
function dateTime(v: string) {
  if (!v) return "-";
  return new Date(v).toLocaleString("zh-CN", { hour12: false });
}

function openDetail(record: ReportRecord) {
  drawer.record = record;
  drawer.open = true;
}

async function load() {
  loading.value = true;
  try {
    data.value = await getReceivedReports({
      type: filters.type,
      fromDate: filters.fromDate,
      toDate: filters.toDate,
    });
    hasSubordinates.value = data.value.length > 0;
  } catch (error: any) {
    message.error(error.message || "加载失败");
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
.muted {
  color: #999;
}
.inbox-filters {
  margin-bottom: 16px;
}
</style>
