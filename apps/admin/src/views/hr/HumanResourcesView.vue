<template>
  <div class="human-resources-page">
    <a-row :gutter="12" class="hr-metrics">
      <a-col :xs="12" :sm="6">
        <div class="metric-card"><div class="metric-value">{{ analytics.activeEmployees }}</div><div class="metric-label">在职人数</div></div>
      </a-col>
      <a-col :xs="12" :sm="6">
        <div class="metric-card"><div class="metric-value" style="color:#1890ff">{{ analytics.newThisMonth }}</div><div class="metric-label">本月新增</div></div>
      </a-col>
      <a-col :xs="12" :sm="6">
        <div class="metric-card"><div class="metric-value" style="color:#faad14">{{ analytics.leavePendingCount }}</div><div class="metric-label">待审批请假</div></div>
      </a-col>
      <a-col :xs="12" :sm="6">
        <div class="metric-card"><div class="metric-value">{{ analytics.totalEmployees }}</div><div class="metric-label">员工总数</div></div>
      </a-col>
    </a-row>
    <a-space class="hr-action-bar">
      <a-dropdown>
        <a-button><template #icon><DownloadOutlined /></template>导出/导入</a-button>
        <template #overlay>
          <a-menu>
            <a-menu-item key="export" @click="exportData">导出员工数据(Excel)</a-menu-item>
            <a-menu-item key="import" @click="openImport">导入员工数据</a-menu-item>
            <a-menu-item key="template" @click="downloadTemplate">下载导入模板</a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
    </a-space>
    <a-tabs v-model:active-key="activeTab" class="human-resources-tabs">
      <a-tab-pane v-if="canViewEmployees" key="employees" tab="员工档案" />
      <a-tab-pane v-if="canViewCertificates" key="certificates" tab="人员证书" />
      <a-tab-pane v-if="canViewWorkforce" key="workforce" tab="排班考勤" />
    </a-tabs>
    <a-modal v-model:open="importModal" title="导入员工数据" width="600px" :confirm-loading="false" @ok="doImport" :ok-text="'开始导入'">
      <a-form layout="vertical">
        <a-form-item label="选择Excel文件">
          <a-upload-dragger :before-upload="handleFileSelect" accept=".xlsx,.xls" :show-upload-list="false" :multiple="false">
            <template #icon><InboxOutlined /></template>
            <p v-if="!importFile">点击或拖拽文件到此区域</p>
            <p v-else>{{ importFile.name }}</p>
          </a-upload-dragger>
        </a-form-item>
      </a-form>
    </a-modal>
    <QualificationCenterView v-if="activeTab === 'employees'" display-mode="employees" />
    <QualificationCenterView v-else-if="activeTab === 'certificates'" display-mode="certificates" />
    <QualificationCenterView v-else display-mode="dashboard" />
  </div>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, onMounted, ref } from "vue";
import { message } from "ant-design-vue";
import { DownloadOutlined, InboxOutlined } from "@ant-design/icons-vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import { getHrAnalytics, type HrAnalytics } from "@/api/hr";

type HumanResourcesTab = "employees" | "certificates" | "workforce";

const auth = useAuthStore();
const analytics = ref<HrAnalytics>({ totalEmployees: 0, activeEmployees: 0, leftEmployees: 0, newThisMonth: 0, leavePendingCount: 0, educationDistribution: [], statusDistribution: [], organizationDistribution: [], recentLifecycles: [] });

const importFile = ref<File>();
const importModal = ref(false);

function openImport() { importModal.value = true; }

async function exportData() {
  try {
    const { exportEmployeesExcel } = await import("@/api/hr");
    await exportEmployeesExcel();
  } catch (error: any) { message.error(error.message || "导出失败"); }
}

async function downloadTemplate() {
  try {
    const { downloadImportTemplate } = await import("@/api/hr");
    await downloadImportTemplate();
  } catch (error: any) { message.error(error.message || "下载失败"); }
}

async function doImport() {
  if (!importFile.value) { message.warning("请选择文件"); return; }
  try {
    const { importEmployeesExcel } = await import("@/api/hr");
    importResult.value = await importEmployeesExcel(importFile.value, auth.user?.displayName || "系统用户");
    importModal.value = false;
    message.success(`导入完成：成功 ${importResult.value.success} 条，失败 ${importResult.value.fail} 条`);
    await loadMetrics();
  } catch (error: any) { message.error(error.message || "导入失败"); }
}

function handleFileSelect(file: File) { importFile.value = file; return false; }

async function loadMetrics() {
  try { analytics.value = await getHrAnalytics(); } catch {}
}
onMounted(loadMetrics);
const route = useRoute();
const router = useRouter();
const QualificationCenterView = defineAsyncComponent(() => import("@/views/qualification/QualificationCenterView.vue"));

const canViewEmployees = computed(() => auth.can("qualification:employee:view"));
const canViewCertificates = computed(() => auth.can("qualification:certificate:view"));
const canViewWorkforce = computed(() => auth.can("workforce:view"));
const availableTabs = computed<HumanResourcesTab[]>(() => [
  canViewEmployees.value ? "employees" : undefined,
  canViewCertificates.value ? "certificates" : undefined,
  canViewWorkforce.value ? "workforce" : undefined,
].filter((item): item is HumanResourcesTab => Boolean(item)));

const activeTab = computed<HumanResourcesTab>({
  get() {
    const requested = route.query.tab as HumanResourcesTab | undefined;
    return requested && availableTabs.value.includes(requested) ? requested : availableTabs.value[0] || "employees";
  },
  set(value) {
    router.replace({ path: "/hr", query: { tab: value } });
  },
});
</script>

<style scoped>
.human-resources-page {
  min-width: 0;
}

.human-resources-tabs :deep(.ant-tabs-nav) {
  margin: 0 0 12px;
  padding: 0 4px;
  border-bottom: 1px solid #dfe5e8;
  background: transparent;
}

.human-resources-tabs :deep(.ant-tabs-nav::before) {
  border-bottom: 0;
}

.human-resources-tabs :deep(.ant-tabs-tab) {
  padding: 10px 4px 12px;
  color: #63707d;
  font-weight: 500;
}

.human-resources-tabs :deep(.ant-tabs-tab-active .ant-tabs-tab-btn) {
  color: #185f52;
  font-weight: 600;
}

.human-resources-tabs :deep(.ant-tabs-ink-bar) {
  height: 3px;
  background: #237867;
}

.hr-metrics {
  margin-bottom: 12px;
}

.metric-card {
  background: #fff;
  border: 1px solid #e8ecef;
  border-radius: 8px;
  padding: 14px 16px;
  text-align: center;
}

.metric-value {
  font-size: 26px;
  font-weight: 700;
  color: #17212b;
  line-height: 1.2;
}

.metric-label {
  font-size: 12px;
  color: #65717e;
  margin-top: 2px;
}
</style>
