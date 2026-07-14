<template>
  <div class="page-stack self-page">
    <a-card title="我的档案">
      <template #extra>
        <a-button :loading="loading" @click="loadData">刷新</a-button>
      </template>

      <div v-if="loading"><a-skeleton active :paragraph="{ rows: 10 }" /></div>

      <template v-else-if="detail">
        <section class="profile-summary">
          <div class="profile-avatar">{{ detail.employee.name?.slice(0, 1) || "我" }}</div>
          <div>
            <strong>{{ detail.employee.name }}</strong>
            <span>{{ detail.employee.workNo || "未登记工号" }} · {{ detail.employee.position || "未设置岗位" }}</span>
          </div>
          <a-tag v-if="detail.employee.contractEnd" :color="contractTagColor(detail.employee.contractEnd)">{{ contractTagLabel(detail.employee.contractEnd) }}</a-tag>
        </section>

        <a-descriptions bordered :column="{ xs: 1, sm: 1, md: 2 }" size="small">
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
      </template>
      <a-empty v-else description="暂无员工档案" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { message } from "ant-design-vue";
import { getSelfProfile } from "@/api/hr";
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
.profile-summary {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
  padding: 14px;
  border: 1px solid #e8edf3;
  border-radius: 8px;
  background: #f8fafc;
}

.profile-summary strong,
.profile-summary span {
  display: block;
}

.profile-summary strong {
  color: #17212b;
  font-size: 18px;
}

.profile-summary span {
  color: #64748b;
  font-size: 13px;
}

.profile-avatar {
  display: grid;
  width: 44px;
  height: 44px;
  flex: none;
  place-items: center;
  border-radius: 8px;
  background: #1f6a5b;
  color: #fff;
  font-weight: 700;
}

@media (max-width: 640px) {
  .profile-summary {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .self-page :deep(.ant-descriptions-item-label),
  .self-page :deep(.ant-descriptions-item-content) {
    padding: 10px 12px;
  }

  .self-page :deep(.ant-descriptions-item-label) {
    width: 92px;
    white-space: nowrap;
  }
}
</style>
