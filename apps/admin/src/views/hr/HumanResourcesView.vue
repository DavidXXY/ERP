<template>
  <div class="human-resources-page">
    <a-tabs v-model:active-key="activeTab" class="human-resources-tabs">
      <a-tab-pane v-if="canViewEmployees" key="employees" tab="员工档案" />
      <a-tab-pane v-if="canViewCertificates" key="certificates" tab="人员证书" />
      <a-tab-pane v-if="canViewWorkforce" key="workforce" tab="排班考勤" />
    </a-tabs>
    <QualificationCenterView v-if="activeTab === 'employees'" display-mode="employees" />
    <QualificationCenterView v-else-if="activeTab === 'certificates'" display-mode="certificates" />
    <WorkforceView v-else-if="activeTab === 'workforce'" />
  </div>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";

type HumanResourcesTab = "employees" | "certificates" | "workforce";

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const QualificationCenterView = defineAsyncComponent(() => import("@/views/qualification/QualificationCenterView.vue"));
const WorkforceView = defineAsyncComponent(() => import("@/views/maintenance/WorkforceView.vue"));

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
</style>
