<template>
  <div class="page-stack">
    <a-card title="人力概况" style="margin-bottom: 16px">
      <a-row :gutter="[16, 16]" class="metric-row">
        <a-col :xs="12" :xl="4"><a-card><a-statistic title="总人数" :value="analytics.totalEmployees" suffix="人" /></a-card></a-col>
        <a-col :xs="12" :xl="4"><a-card><a-statistic title="在职人数" :value="analytics.activeEmployees" suffix="人" :value-style="{ color: '#3f8600' }" /></a-card></a-col>
        <a-col :xs="12" :xl="4"><a-card><a-statistic title="已离职" :value="analytics.leftEmployees" suffix="人" :value-style="{ color: analytics.leftEmployees > 0 ? '#ff4d4f' : undefined }" /></a-card></a-col>
        <a-col :xs="12" :xl="4"><a-card><a-statistic title="本月新增" :value="analytics.newThisMonth" suffix="人" :value-style="{ color: '#1890ff' }" /></a-card></a-col>
        <a-col :xs="12" :xl="4"><a-card><a-statistic title="待批请假" :value="analytics.leavePendingCount" suffix="单" :value-style="{ color: analytics.leavePendingCount > 0 ? '#faad14' : undefined }" /></a-card></a-col>
        <a-col :xs="12" :xl="4"><a-card><a-statistic title="离职率" :value="turnoverRate" suffix="%" :precision="1" :value-style="{ color: turnoverRate > 10 ? '#ff4d4f' : undefined }" /></a-card></a-col>
      </a-row>
    </a-card>

    <a-row :gutter="12">
      <a-col :xs="24" :lg="8">
        <a-card title="人员状态" class="analytics-card">
          <div v-if="analytics.statusDistribution.length === 0" class="chart-empty">暂无数据</div>
          <div v-else class="dist-rows">
            <div v-for="item in analytics.statusDistribution" :key="item.name" class="dist-row">
              <span class="dist-label">{{ item.name }}</span>
              <div class="dist-track">
                <div class="dist-fill" :style="{ width: distPercent(item.count, analytics.statusDistribution) + '%', background: statusColor(item.name) }"></div>
              </div>
              <span class="dist-val">{{ item.count }}人</span>
            </div>
          </div>
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="8">
        <a-card title="学历分布" class="analytics-card">
          <div v-if="analytics.educationDistribution.length === 0" class="chart-empty">暂无数据</div>
          <div v-else class="dist-rows">
            <div v-for="item in analytics.educationDistribution" :key="item.name" class="dist-row">
              <span class="dist-label">{{ item.name }}</span>
              <div class="dist-track">
                <div class="dist-fill" :style="{ width: distPercent(item.count, analytics.educationDistribution) + '%', background: '#1890ff' }"></div>
              </div>
              <span class="dist-val">{{ item.count }}人</span>
            </div>
          </div>
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="8">
        <a-card title="组织分布 TOP10" class="analytics-card">
          <div v-if="topOrganizations.length === 0" class="chart-empty">暂无数据</div>
          <div v-else class="dist-rows">
            <div v-for="item in topOrganizations" :key="item.name" class="dist-row">
              <span class="dist-label">{{ item.name }}</span>
              <div class="dist-track">
                <div class="dist-fill" :style="{ width: distPercent(item.count, topOrganizations) + '%', background: '#722ed1' }"></div>
              </div>
              <span class="dist-val">{{ item.count }}人</span>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <a-card title="近期人员变动" class="analytics-card">
      <div v-if="analytics.recentLifecycles.length === 0" style="text-align:center;padding:24px;color:#8c8c8c">暂无变动记录</div>
      <a-timeline v-else>
        <a-timeline-item v-for="item in analytics.recentLifecycles" :key="item.date + item.employeeName" :color="typeColor(item.type)">
          <template #label>{{ item.date }}</template>
          <strong>{{ item.employeeName }}</strong> &middot; {{ item.type }}
          <p style="margin:2px 0 0;color:#8c8c8c;font-size:12px">{{ item.detail }}</p>
        </a-timeline-item>
      </a-timeline>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { message } from "ant-design-vue";
import { ReloadOutlined } from "@ant-design/icons-vue";
import { getHrAnalytics, type HrAnalytics } from "@/api/hr";

const analytics = ref<HrAnalytics>({
  totalEmployees: 0, activeEmployees: 0, leftEmployees: 0,
  newThisMonth: 0, leavePendingCount: 0,
  educationDistribution: [], statusDistribution: [],
  organizationDistribution: [], recentLifecycles: [],
});

const topOrganizations = computed(() =>
  [...analytics.value.organizationDistribution].sort((a, b) => b.count - a.count).slice(0, 10)
);

const turnoverRate = computed(() => {
  const total = analytics.value.totalEmployees || 1;
  return Math.round((analytics.value.leftEmployees / total) * 1000) / 10;
});

function distPercent(count: number, data: Array<{ count: number }>) {
  const maxD = Math.max(...data.map((d) => d.count), 1);
  return Math.round((count / maxD) * 100);
}

function statusColor(name: string) {
  return { 在职: "#52c41a", 试用: "#1890ff", 离职: "#ff4d4f", 休假: "#faad14" }[name] || "#8c8c8c";
}

function typeColor(t: string) {
  return ({ 入职: "green", 调岗: "blue", 离职: "red" } as Record<string, string>)[t] || "gray";
}

async function loadData() {
  try { analytics.value = await getHrAnalytics(); }
  catch (error: any) { message.error(error.message || "加载失败"); }
}

onMounted(loadData);
</script>

<style scoped>
.analytics-card { margin-top: 12px; }
.chart-empty { text-align: center; padding: 32px 0; color: #8c8c8c; }
.dist-rows { display: flex; flex-direction: column; gap: 8px; }
.dist-row { display: flex; align-items: center; gap: 10px; }
.dist-label { width: 70px; font-size: 13px; color: #333; flex-shrink: 0; text-align: right; }
.dist-track { flex: 1; height: 22px; background: #f5f5f5; border-radius: 4px; overflow: hidden; }
.dist-fill { height: 100%; border-radius: 4px; transition: width 0.4s; min-width: 0; }
.dist-val { font-size: 12px; color: #595959; width: 36px; flex-shrink: 0; }
</style>
