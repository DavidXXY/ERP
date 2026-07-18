<template>
  <div class="page-stack system-health-page">
    <header class="page-heading">
      <div>
        <h2>系统运行情况</h2>
        <p>实时查看服务器的运行状态、资源使用情况及 Java 虚拟机信息。</p>
      </div>
      <a-space>
        <Badge
          :status="loading ? 'processing' : 'success'"
          :text="loading ? '刷新中' : '运行中'"
        />
        <a-button :loading="loading" @click="loadData">
          <template #icon><ReloadOutlined /></template>刷新
        </a-button>
      </a-space>
    </header>

    <!-- 关键指标 -->
    <section class="metric-band">
      <div>
        <span>CPU 逻辑核数</span>
        <strong>{{ health?.cpu.availableProcessors ?? "-" }}</strong>
      </div>
      <div>
        <span>系统负载</span>
        <strong :class="{ 'text-danger': systemLoadPercent > 80 }">
          {{
            health?.cpu.systemLoadAverage >= 0 ? systemLoadPercent + "%" : "N/A"
          }}
        </strong>
      </div>
      <div>
        <span>JVM 堆内存</span>
        <strong>{{ formatBytes(health?.memory.heap.max ?? 0) }}</strong>
      </div>
      <div>
        <span>已运行</span>
        <strong>{{ uptimeText }}</strong>
      </div>
    </section>

    <!-- 应用与依赖 -->
    <a-card title="应用与依赖">
      <a-descriptions bordered :column="{ xs: 1, sm: 2, md: 3 }" size="small">
        <a-descriptions-item label="应用名称">{{
          health?.application?.appName ?? "-"
        }}</a-descriptions-item>
        <a-descriptions-item label="版本号">{{
          health?.application?.version ?? "-"
        }}</a-descriptions-item>
        <a-descriptions-item label="构建时间">{{
          health?.application?.buildTime || "-"
        }}</a-descriptions-item>
        <a-descriptions-item label="运行环境">
          <a-tag color="blue">{{
            health?.application?.activeProfiles || "default"
          }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="存储类型">{{
          health?.application?.storageType ?? "-"
        }}</a-descriptions-item>
        <a-descriptions-item label="Redis">{{
          health?.dependencies?.redisEndpoint || "未配置"
        }}</a-descriptions-item>
        <a-descriptions-item label="数据库" :span="3">{{
          health?.dependencies?.databaseUrl || "-"
        }}</a-descriptions-item>
        <a-descriptions-item label="数据库驱动" :span="3">{{
          health?.dependencies?.databaseDriver || "-"
        }}</a-descriptions-item>
        <a-descriptions-item label="本地存储路径" :span="3">{{
          health?.dependencies?.localStoragePath || "-"
        }}</a-descriptions-item>
        <a-descriptions-item label="工作目录" :span="3">{{
          health?.dependencies?.workingDir || "-"
        }}</a-descriptions-item>
        <a-descriptions-item label="临时目录" :span="3">{{
          health?.dependencies?.tempDir || "-"
        }}</a-descriptions-item>
      </a-descriptions>
    </a-card>

    <!-- 系统概览 -->
    <a-card title="系统概览">
      <a-descriptions
        bordered
        :column="{ xs: 1, sm: 2, md: 3, lg: 4 }"
        size="small"
      >
        <a-descriptions-item label="操作系统">{{
          osInfo.name
        }}</a-descriptions-item>
        <a-descriptions-item label="系统版本">{{
          osInfo.version
        }}</a-descriptions-item>
        <a-descriptions-item label="系统架构">{{
          osInfo.architecture
        }}</a-descriptions-item>
        <a-descriptions-item label="Java 版本">{{
          health?.jvm.javaVersion ?? "-"
        }}</a-descriptions-item>
        <a-descriptions-item label="JVM 名称">{{
          health?.jvm.jvmName ?? "-"
        }}</a-descriptions-item>
        <a-descriptions-item label="JVM 版本">{{
          health?.jvm.jvmVersion ?? "-"
        }}</a-descriptions-item>
        <a-descriptions-item label="JVM 供应商">{{
          health?.jvm.jvmVendor ?? "-"
        }}</a-descriptions-item>
        <a-descriptions-item label="Java 供应商">{{
          health?.jvm.javaVendor ?? "-"
        }}</a-descriptions-item>
      </a-descriptions>
    </a-card>

    <!-- CPU 与内存 -->
    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :lg="12">
        <a-card title="CPU 使用率">
          <a-spin :spinning="loading">
            <div class="health-metrics">
              <div class="metric-item">
                <div class="metric-label">
                  <span>进程 CPU</span>
                  <strong>{{
                    formatPercent(health?.cpu.processCpuLoad)
                  }}</strong>
                </div>
                <a-progress
                  :percent="normalizedPercent(health?.cpu.processCpuLoad)"
                  :stroke-color="cpuColor(health?.cpu.processCpuLoad)"
                  :format="() => ''"
                />
              </div>
              <div class="metric-item">
                <div class="metric-label">
                  <span>系统 CPU</span>
                  <strong>{{
                    formatPercent(health?.cpu.systemCpuLoad)
                  }}</strong>
                </div>
                <a-progress
                  :percent="normalizedPercent(health?.cpu.systemCpuLoad)"
                  :stroke-color="cpuColor(health?.cpu.systemCpuLoad)"
                  :format="() => ''"
                />
              </div>
              <div class="metric-item">
                <div class="metric-label">
                  <span>系统平均负载</span>
                  <strong>{{
                    health?.cpu.systemLoadAverage >= 0
                      ? systemLoadPercent + "%"
                      : "N/A"
                  }}</strong>
                </div>
                <a-progress
                  :percent="systemLoadPercent"
                  :stroke-color="cpuColor(systemPseudoLoad)"
                  :format="() => ''"
                />
              </div>
            </div>
          </a-spin>
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="12">
        <a-card title="物理内存">
          <a-spin :spinning="loading">
            <div class="health-metrics">
              <div class="metric-item">
                <div class="metric-label">
                  <span>总计</span>
                  <strong>{{
                    formatBytes(health?.memory.totalPhysicalMemory ?? 0)
                  }}</strong>
                </div>
              </div>
              <div
                v-if="health?.memory.totalPhysicalMemory"
                class="metric-item"
              >
                <div class="metric-label">
                  <span>已使用</span>
                  <strong>{{ formatBytes(physUsed) }}</strong>
                </div>
                <a-progress
                  :percent="physPercent"
                  :stroke-color="physColor"
                  :format="() => ''"
                />
                <div class="metric-hint">
                  <span
                    >空闲
                    {{
                      formatBytes(health?.memory.freePhysicalMemory ?? 0)
                    }}</span
                  >
                </div>
              </div>
            </div>
          </a-spin>
        </a-card>
      </a-col>
    </a-row>

    <!-- JVM 堆内存 -->
    <a-card title="JVM 堆内存">
      <a-spin :spinning="loading">
        <a-row :gutter="[16, 16]">
          <a-col :xs="24" :lg="12">
            <div class="metric-item">
              <div class="metric-label">
                <span>堆内存使用</span>
                <strong
                  >{{ formatBytes(health?.memory.heap.used ?? 0) }} /
                  {{ formatBytes(health?.memory.heap.max ?? 0) }}</strong
                >
              </div>
              <a-progress
                :percent="heapPercent"
                :stroke-color="memColor(heapPercent)"
                :format="() => heapPercent + '%'"
              />
            </div>
          </a-col>
          <a-col :xs="24" :lg="12">
            <div class="metric-item">
              <div class="metric-label">
                <span>非堆内存使用</span>
                <strong
                  >{{ formatBytes(health?.memory.nonHeap.used ?? 0) }} /
                  {{ formatBytes(health?.memory.nonHeap.max ?? 0) }}</strong
                >
              </div>
              <a-progress
                :percent="nonHeapPercent"
                :stroke-color="memColor(nonHeapPercent)"
                :format="() => nonHeapPercent + '%'"
              />
            </div>
          </a-col>
        </a-row>
        <a-descriptions
          bordered
          :column="3"
          size="small"
          class="health-descriptions"
        >
          <a-descriptions-item label="堆初始容量">{{
            formatBytes(health?.memory.heap.init ?? 0)
          }}</a-descriptions-item>
          <a-descriptions-item label="堆已提交">{{
            formatBytes(health?.memory.heap.committed ?? 0)
          }}</a-descriptions-item>
          <a-descriptions-item label="堆最大值">{{
            formatBytes(health?.memory.heap.max ?? 0)
          }}</a-descriptions-item>
          <a-descriptions-item label="非堆初始容量">{{
            formatBytes(health?.memory.nonHeap.init ?? 0)
          }}</a-descriptions-item>
          <a-descriptions-item label="非堆已提交">{{
            formatBytes(health?.memory.nonHeap.committed ?? 0)
          }}</a-descriptions-item>
          <a-descriptions-item label="非堆最大值">{{
            formatBytes(health?.memory.nonHeap.max ?? 0)
          }}</a-descriptions-item>
        </a-descriptions>
      </a-spin>
    </a-card>

    <!-- 磁盘 -->
    <a-card title="磁盘使用情况">
      <a-spin :spinning="loading">
        <div v-for="disk in disks" :key="disk.path" class="disk-card">
          <div class="metric-label">
            <span><FolderOutlined /> {{ disk.path }}</span>
            <strong
              >{{ formatBytes(disk.used) }} /
              {{ formatBytes(disk.totalSpace) }}</strong
            >
          </div>
          <a-progress
            :percent="diskPercent(disk)"
            :stroke-color="diskColor(diskPercent(disk))"
            :format="() => diskPercent(disk) + '%'"
          />
          <div class="metric-hint">
            <span>已用 {{ formatBytes(disk.used) }}</span>
            <span>可用 {{ formatBytes(disk.freeSpace) }}</span>
          </div>
        </div>
        <a-empty
          v-if="!disks.length"
          :image="simpleImage"
          description="无磁盘信息"
        />
      </a-spin>
    </a-card>

    <!-- JVM 详细信息 -->
    <a-card title="JVM 启动参数">
      <a-spin :spinning="loading">
        <pre class="jvm-args">{{ formattedArgs }}</pre>
      </a-spin>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { Badge, Empty, message } from "ant-design-vue";
import { ReloadOutlined, FolderOutlined } from "@ant-design/icons-vue";
import { getSystemHealthApi, type SystemHealthResponse } from "@/api/system";

const loading = ref(false);
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE;
const health = ref<SystemHealthResponse>(
  null as unknown as SystemHealthResponse,
);

onMounted(loadData);

const osInfo = computed(() => {
  const os = health.value?.operatingSystem;
  return {
    name: os?.name ?? "-",
    version: os?.version ?? "-",
    architecture: os?.architecture ?? "-",
  };
});

const systemLoadPercent = computed(() => {
  const avg = health.value?.cpu.systemLoadAverage;
  const cores = health.value?.cpu.availableProcessors ?? 1;
  if (avg == null || avg < 0) return 0;
  return Math.min(Math.round((avg / cores) * 100), 100);
});

const systemPseudoLoad = computed(() => {
  return systemLoadPercent.value / 100;
});

const uptimeText = computed(() => {
  const ms = health.value?.jvm.uptime ?? 0;
  const sec = Math.floor(ms / 1000);
  const days = Math.floor(sec / 86400);
  const hours = Math.floor((sec % 86400) / 3600);
  const mins = Math.floor((sec % 3600) / 60);
  const secs = sec % 60;
  const parts: string[] = [];
  if (days > 0) parts.push(`${days} 天`);
  if (hours > 0) parts.push(`${hours} 小时`);
  if (mins > 0) parts.push(`${mins} 分钟`);
  parts.push(`${secs} 秒`);
  return parts.join(" ");
});

const physUsed = computed(() => {
  const total = health.value?.memory.totalPhysicalMemory ?? 0;
  const free = health.value?.memory.freePhysicalMemory ?? 0;
  return total > 0 ? total - free : 0;
});

const physPercent = computed(() => {
  const total = health.value?.memory.totalPhysicalMemory;
  if (!total || total <= 0) return 0;
  return Math.min(
    Math.round(
      ((total - (health.value?.memory.freePhysicalMemory ?? 0)) / total) * 100,
    ),
    100,
  );
});

const physColor = computed(() => memColor(physPercent.value));

const heapPercent = computed(() => {
  const used = health.value?.memory.heap.used ?? 0;
  const max = health.value?.memory.heap.max ?? 0;
  if (max <= 0) return 0;
  return Math.min(Math.round((used / max) * 100), 100);
});

const nonHeapPercent = computed(() => {
  const used = health.value?.memory.nonHeap.used ?? 0;
  const max = health.value?.memory.nonHeap.max ?? 0;
  if (max <= 0) return 0;
  return Math.min(Math.round((used / max) * 100), 100);
});

const disks = computed(() => {
  return (health.value?.disk ?? []).map((d) => ({
    ...d,
    used: (d.totalSpace ?? 0) - (d.freeSpace ?? 0),
  }));
});

const formattedArgs = computed(() => {
  const args = health.value?.jvm.inputArguments;
  if (!args || args.length === 0) return "无启动参数";
  return args.join("\n");
});

async function loadData() {
  loading.value = true;
  try {
    health.value = await getSystemHealthApi();
  } catch (error) {
    message.error(
      error instanceof Error ? error.message : "系统运行信息加载失败",
    );
  } finally {
    loading.value = false;
  }
}

function formatBytes(value: number | undefined | null): string {
  if (value == null || value <= 0) return "0 B";
  const units = ["B", "KB", "MB", "GB", "TB"];
  const base = 1024;
  let unitIndex = 0;
  let size = value;
  while (size >= base && unitIndex < units.length - 1) {
    size /= base;
    unitIndex++;
  }
  return size.toFixed(1) + " " + units[unitIndex];
}

function formatPercent(value: number | undefined | null): string {
  if (value == null || value < 0) return "N/A";
  return (value * 100).toFixed(1) + "%";
}

function normalizedPercent(value: number | undefined | null): number {
  if (value == null || value < 0) return 0;
  return Math.min(Math.round(value * 100), 100);
}

function cpuColor(value: number | undefined | null): string {
  if (value == null || value < 0) return "#bfbfbf";
  if (value < 0.5) return "#52c41a";
  if (value < 0.8) return "#faad14";
  return "#f5222d";
}

function memColor(percent: number): string {
  if (percent < 60) return "#52c41a";
  if (percent < 85) return "#faad14";
  return "#f5222d";
}

function diskPercent(disk: { used: number; totalSpace: number }): number {
  if (!disk.totalSpace) return 0;
  return Math.min(Math.round((disk.used / disk.totalSpace) * 100), 100);
}

function diskColor(percent: number): string {
  if (percent < 75) return "#52c41a";
  if (percent < 90) return "#faad14";
  return "#f5222d";
}
</script>

<style scoped>
.system-health-page {
  min-width: 0;
}

.page-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.page-heading h2 {
  margin: 0;
  color: #182230;
}

.page-heading p {
  margin: 5px 0 0;
  color: #667085;
}

.metric-band {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  border: 1px solid #e4e7ec;
  background: #fff;
}

.metric-band > div {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 18px 20px;
  border-right: 1px solid #e4e7ec;
}

.metric-band > div:last-child {
  border-right: 0;
}

.metric-band span {
  color: #667085;
  font-size: 13px;
}

.metric-band strong {
  color: #101828;
  font-size: 24px;
}

.health-metrics {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.metric-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.metric-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.metric-label span {
  color: #667085;
  font-size: 13px;
}

.metric-label strong {
  color: #182230;
  font-size: 14px;
  white-space: nowrap;
}

.metric-hint {
  display: flex;
  justify-content: space-between;
  color: #98a2b3;
  font-size: 12px;
}

.health-descriptions {
  margin-top: 20px;
}

.disk-card {
  margin-bottom: 20px;
  padding: 16px;
  border: 1px solid #eaecf0;
  border-radius: 6px;
}

.disk-card:last-child {
  margin-bottom: 0;
}

.disk-card .metric-label {
  margin-bottom: 8px;
}

.jvm-args {
  margin: 0;
  padding: 16px;
  border: 1px solid #eaecf0;
  border-radius: 6px;
  background: #fafbfc;
  color: #475467;
  font-size: 12px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-all;
}

.text-danger {
  color: #cf1322;
}

@media (max-width: 900px) {
  .page-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .metric-band {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .metric-band > div:nth-child(2) {
    border-right: 0;
  }
}

@media (max-width: 560px) {
  .metric-band {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
