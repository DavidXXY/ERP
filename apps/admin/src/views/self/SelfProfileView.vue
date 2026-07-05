<template>
  <div class="self-page">
    <a-page-header title="我的档案" @back="$router.back()" />

    <div v-if="loading"><a-skeleton active :paragraph="{ rows: 10 }" /></div>

    <template v-else-if="detail">
      <a-card title="基本信息" class="profile-section">
        <a-descriptions bordered :column="2" size="small">
          <a-descriptions-item label="姓名">{{ detail.employee.name }}</a-descriptions-item>
          <a-descriptions-item label="工号">{{ detail.employee.workNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="部门">{{ detail.employee.organizationName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="岗位">{{ detail.employee.position || '-' }}</a-descriptions-item>
          <a-descriptions-item label="手机号">{{ detail.employee.phone || '-' }}</a-descriptions-item>
          <a-descriptions-item label="身份证号">{{ detail.employee.idCard || '-' }}</a-descriptions-item>
          <a-descriptions-item label="入职日期">{{ detail.employee.entryDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="合同状态">
            <a-tag v-if="detail.employee.contractEnd" :color="contractTagColor(detail.employee.contractEnd)">{{ contractTagLabel(detail.employee.contractEnd) }}</a-tag>
            <span v-else>-</span>
          </a-descriptions-item>
        </a-descriptions>
      </a-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { message } from "ant-design-vue";
import { getSelfProfile, type LeaveRecord } from "@/api/hr";
import type { EmployeeDetail } from "@/api/qualification";

const loading = ref(false);
const detail = ref<EmployeeDetail>();

function contractTagColor(endDate?: string): string {
  if (!endDate) return "default";
  const diff = (new Date(endDate).getTime() - Date.now()) / (86400000);
  if (diff <= 0) return "red"; if (diff <= 30) return "red"; if (diff <= 90) return "orange"; return "green";
}
function contractTagLabel(endDate?: string): string {
  if (!endDate) return "";
  const diff = (new Date(endDate).getTime() - Date.now()) / (86400000);
  if (diff <= 0) return "已到期"; if (diff <= 30) return "即将到期"; if (diff <= 90) return "即将到期"; return "有效";
}

async function loadData() {
  loading.value = true;
  try { detail.value = await getSelfProfile(); }
  catch (error: any) { message.error(error.message || "加载失败"); }
  finally { loading.value = false; }
}
onMounted(loadData);
</script>

<style scoped>
.self-page { max-width: 900px; }
.profile-section { margin-bottom: 12px; }
</style>
