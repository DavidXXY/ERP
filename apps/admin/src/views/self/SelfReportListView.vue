<template>
  <div class="page-stack self-page">
    <a-card title="我的汇报">
      <template #extra>
        <a-space wrap class="self-card-toolbar">
          <a-button @click="$router.push('/self')">返回工作台</a-button>
          <a-button type="primary" @click="openNew()"
            ><PlusOutlined />填写汇报</a-button
          >
        </a-space>
      </template>

      <a-row :gutter="[16, 16]" class="self-inline-metrics">
        <a-col :xs="12" :md="6"
          ><div class="self-mini-metric">
            <span>全部</span><strong>{{ data.length }}</strong>
          </div></a-col
        >
        <a-col :xs="12" :md="6"
          ><div class="self-mini-metric">
            <span>日报</span><strong>{{ countByType("DAILY") }}</strong>
          </div></a-col
        >
        <a-col :xs="12" :md="6"
          ><div class="self-mini-metric">
            <span>周报</span><strong>{{ countByType("WEEKLY") }}</strong>
          </div></a-col
        >
        <a-col :xs="12" :md="6"
          ><div class="self-mini-metric">
            <span>月报</span><strong>{{ countByType("MONTHLY") }}</strong>
          </div></a-col
        >
      </a-row>

      <a-tabs v-model:active-key="typeFilter" class="self-tabs">
        <a-tab-pane key="all" tab="全部" />
        <a-tab-pane key="DAILY" tab="日报" />
        <a-tab-pane key="WEEKLY" tab="周报" />
        <a-tab-pane key="MONTHLY" tab="月报" />
      </a-tabs>

      <a-table
        :data-source="filteredData"
        :columns="columns"
        row-key="id"
        size="middle"
        :pagination="{ pageSize: 10 }"
        :loading="loading"
        :scroll="{ x: 900 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'reportType'"
            ><a-space :size="4" wrap>
              <a-tag :color="typeColor(record.reportType)">{{
                typeLabel(record.reportType)
              }}</a-tag>
              <a-tag v-if="record.reportCategory === 'ENGINEERING'" color="gold"
                >工程汇报</a-tag
              >
              <a-tag v-else color="cyan">日常汇报</a-tag>
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
            <span v-else class="muted">无抄送</span>
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
      :title="drawer.isDetail ? '汇报详情' : '填写汇报'"
      :width="Math.min(680, windowWidth - 24)"
      destroy-on-close
      @close="drawer.open = false"
    >
      <template v-if="drawer.isDetail && drawer.record">
        <a-descriptions :column="1" bordered size="small">
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
      </template>
      <a-form
        v-else
        ref="formRef"
        :model="form"
        layout="vertical"
        class="report-form"
      >
        <a-form-item
          label="汇报类别"
          name="reportCategory"
          :rules="[{ required: true, message: '请选择汇报类别' }]"
        >
          <a-radio-group
            v-model:value="form.reportCategory"
            :options="categoryOptions"
            button-style="solid"
            @change="onCategoryChange"
          />
        </a-form-item>
        <a-form-item
          v-if="form.reportCategory === 'ENGINEERING'"
          label="所属项目"
          name="projectId"
          :rules="[{ required: true, message: '工程汇报必须选择项目' }]"
        >
          <a-select
            v-model:value="form.projectId"
            :options="projectOptions"
            placeholder="请选择你加入的项目"
            allow-clear
            show-search
            option-filter-prop="label"
          />
        </a-form-item>
        <a-form-item
          v-if="form.reportCategory === 'ENGINEERING'"
          label="填报工时"
          name="hours"
          :rules="[{ required: true, message: '工程汇报必须选择工时' }]"
        >
          <a-radio-group
            v-model:value="form.hours"
            button-style="solid"
            :options="hoursOptions"
            @change="onHoursChange"
          />
          <div class="muted form-hint"
            >最小为半日（4小时）；选半日可再添加一个项目（可不选）</div
          >
        </a-form-item>
        <a-form-item
          v-if="
            form.reportCategory === 'ENGINEERING' &&
            form.hours === 4 &&
            form.projectId
          "
          label="第二个项目（可选）"
          name="additionalProjectId"
        >
          <a-select
            v-model:value="form.additionalProjectId"
            :options="additionalProjectOptions"
            placeholder="选填：将另一半工时记到第二项目"
            allow-clear
            show-search
            option-filter-prop="label"
          />
        </a-form-item>
        <a-form-item
          label="汇报类型"
          name="reportType"
          :rules="[{ required: true, message: '请选择汇报类型' }]"
        >
          <a-radio-group
            v-model:value="form.reportType"
            :options="typeOptions"
            button-style="solid"
          />
        </a-form-item>
        <a-form-item
          label="汇报日期"
          name="reportDate"
          :rules="[{ required: true, message: '请选择汇报日期' }]"
        >
          <a-date-picker
            v-model:value="form.reportDate"
            value-format="YYYY-MM-DD"
            :allow-clear="false"
            :disabled-date="(d: any) => d && d.getTime() > Date.now()"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item
          label="工作内容"
          name="content"
          :rules="[
            { required: true, message: '请填写工作内容' },
            { min: 5, max: 20000, message: '内容需 5~20000 字' },
          ]"
        >
          <a-textarea
            v-model:value="form.content"
            :rows="8"
            show-count
            placeholder="请描述今天/本周/本月完成的工作内容"
          />
        </a-form-item>
        <a-form-item label="进度与达成">
          <a-textarea
            v-model:value="form.progressSummary"
            :rows="2"
            show-count
            :maxlength="1000"
            placeholder="关键节点、指标完成情况（可选）"
          />
        </a-form-item>
        <a-form-item label="下一步计划">
          <a-textarea
            v-model:value="form.planNext"
            :rows="2"
            show-count
            :maxlength="2000"
            placeholder="后续工作安排（可选）"
          />
        </a-form-item>
        <a-form-item label="问题与求助">
          <a-textarea
            v-model:value="form.issue"
            :rows="2"
            show-count
            :maxlength="1000"
            placeholder="需要上级协调的问题（可选）"
          />
        </a-form-item>
        <a-form-item label="抄送">
          <a-select
            v-model:value="form.ccUserIds"
            mode="multiple"
            :options="ccOptions"
            placeholder="抄送给上级或同事，对方将在消息中心收到通知"
            allow-clear
            :max-tag-count="4"
          />
        </a-form-item>
        <a-form-item class="form-actions">
          <a-button type="primary" :loading="saving" @click="submit"
            >提交汇报</a-button
          >
          <a-button @click="drawer.open = false">取消</a-button>
        </a-form-item>
      </a-form>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import { PlusOutlined } from "@ant-design/icons-vue";
import {
  createReport,
  getCcCandidates,
  getMyProjects,
  getMyReports,
  type ReportCategory,
  type ReportPayload,
  type ReportProjectOption,
  type ReportRecord,
  type ReportType,
  type ReportUserOption,
} from "@/api/reporting";

const loading = ref(false);
const saving = ref(false);
const data = ref<ReportRecord[]>([]);
const typeFilter = ref("all");
const formRef = ref();
const ccOptions = ref<ReportUserOption[]>([]);
const rawProjectOptions = ref<ReportProjectOption[]>([]);
const projectOptions = computed(() =>
  rawProjectOptions.value.map((p) => ({
    label: p.code + " · " + p.name,
    value: p.id,
  })),
);
const windowWidth = ref(window.innerWidth);
window.addEventListener(
  "resize",
  () => (windowWidth.value = window.innerWidth),
);

const categoryOptions = [
  { value: "DAILY", label: "日常汇报" },
  { value: "ENGINEERING", label: "工程汇报" },
];

const typeOptions = [
  { value: "DAILY", label: "日报" },
  { value: "WEEKLY", label: "周报" },
  { value: "MONTHLY", label: "月报" },
];

const hoursOptions = [
  { value: 4, label: "半日（4小时）" },
  { value: 8, label: "全日（8小时）" },
];

const form = reactive<ReportPayload>({
  reportCategory: "DAILY",
  reportType: "DAILY",
  projectId: undefined,
  additionalProjectId: undefined,
  hours: undefined,
  reportDate: "",
  content: "",
  progressSummary: "",
  planNext: "",
  issue: "",
  ccUserIds: [],
});

const additionalProjectOptions = computed(() =>
  rawProjectOptions.value
    .filter((p) => p.id !== form.projectId)
    .map((p) => ({ label: p.code + " · " + p.name, value: p.id })),
);

const drawer = reactive<{
  open: boolean;
  isDetail: boolean;
  record: ReportRecord | null;
}>({ open: false, isDetail: false, record: null });

const columns = [
  { title: "类型", key: "reportType", width: 110 },
  { title: "项目", key: "projectName", width: 180 },
  { title: "汇报日期", key: "reportDate", width: 120 },
  { title: "工作内容", key: "content" },
  { title: "抄送", key: "ccNames", width: 220 },
  { title: "提交时间", key: "createdAt", width: 170 },
  { title: "操作", key: "action", width: 80 },
];

const filteredData = computed(() =>
  typeFilter.value === "all"
    ? data.value
    : data.value.filter((d) => d.reportType === typeFilter.value),
);

function countByType(type: ReportType) {
  return data.value.filter((item) => item.reportType === type).length;
}

function typeLabel(t: string) {
  return { DAILY: "日报", WEEKLY: "周报", MONTHLY: "月报" }[t] || t;
}
function typeColor(t: string) {
  return { DAILY: "blue", WEEKLY: "purple", MONTHLY: "green" }[t] || "default";
}
function dateTime(v: string) {
  if (!v) return "-";
  return new Date(v).toLocaleString("zh-CN", { hour12: false });
}

function hoursLabel(h?: number) {
  if (h == null) return "-";
  return h === 4 ? "半日（4小时）" : h === 8 ? "全日（8小时）" : `${h} 小时`;
}

function onCategoryChange() {
  if (form.reportCategory !== "ENGINEERING") {
    form.projectId = undefined;
    form.additionalProjectId = undefined;
    form.hours = undefined;
  }
}

function onHoursChange() {
  if (form.hours !== 4) form.additionalProjectId = undefined;
}

function todayLocal() {
  const d = new Date();
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

function openNew() {
  drawer.record = null;
  drawer.isDetail = false;
  Object.assign(form, {
    reportCategory: "DAILY" as ReportCategory,
    reportType: "DAILY",
    projectId: undefined,
    additionalProjectId: undefined,
    hours: undefined,
    reportDate: todayLocal(),
    content: "",
    progressSummary: "",
    planNext: "",
    issue: "",
    ccUserIds: [],
  } satisfies ReportPayload);
  drawer.open = true;
}

function openDetail(record: ReportRecord) {
  drawer.record = record;
  drawer.isDetail = true;
  drawer.open = true;
}

async function submit() {
  try {
    await formRef.value?.validate();
  } catch {
    return;
  }
  saving.value = true;
  try {
    await createReport({
      ...form,
      projectId:
        form.reportCategory === "ENGINEERING" ? form.projectId : undefined,
      additionalProjectId:
        form.reportCategory === "ENGINEERING" && form.hours === 4
          ? form.additionalProjectId
          : undefined,
      hours:
        form.reportCategory === "ENGINEERING" ? form.hours : undefined,
    });
    message.success("汇报提交成功，已通知抄送人");
    drawer.open = false;
    await load();
  } catch (error: any) {
    message.error(error.message || "提交失败");
  } finally {
    saving.value = false;
  }
}

async function load() {
  loading.value = true;
  try {
    data.value = await getMyReports();
  } catch (error: any) {
    message.error(error.message || "加载失败");
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  await load();
  try {
    ccOptions.value = await getCcCandidates();
  } catch {}
  try {
    rawProjectOptions.value = await getMyProjects();
  } catch {}
});
</script>

<style scoped>
.muted {
  color: #999;
}
.form-hint {
  margin-top: 4px;
  font-size: 12px;
}
.form-actions :deep(.ant-form-item-control-input-content) {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
@media (max-width: 640px) {
  .form-actions :deep(.ant-btn) {
    flex: 1 1 140px;
  }
}
</style>
