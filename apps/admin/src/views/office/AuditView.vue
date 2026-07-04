<template>
  <div class="page-stack">
    <a-card title="操作审计日志">
      <template #extra>
        <a-space>
          <a-range-picker v-model:value="dateRange" @change="loadData" />
          <a-input-search v-model:value="searchUser" placeholder="搜索操作人" style="width:200px" allow-clear @search="loadData" />
          <a-button :loading="loading" @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
        </a-space>
      </template>
      <a-table
        :columns="columns"
        :data-source="auditLogs"
        :loading="loading"
        :pagination="{ current: page + 1, pageSize: size, total, showSizeChanger: true, showTotal: (t) => '共 ' + t + ' 条' }"
        :row-key="(r: any) => r.id"
        :scroll="{ x: 1200 }"
        size="small"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'method'">
            <a-tag :color="methodColor(record.httpMethod)">{{ record.httpMethod }}</a-tag>
          </template>
          <template v-else-if="column.key === 'path'">
            <span :title="record.requestPath" style="max-width:400px;display:inline-block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ record.requestPath }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.responseStatus < 300 ? 'green' : record.responseStatus < 400 ? 'orange' : 'red'">{{ record.responseStatus }}</a-tag>
          </template>
          <template v-else-if="column.key === 'duration'">{{ record.durationMs }}ms</template>
          <template v-else-if="column.key === 'time'">{{ record.createdAt?.slice(0, 19)?.replace('T', ' ') || '-' }}</template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>
<script setup lang="ts">
import { onMounted, ref } from "vue";
import { message } from "ant-design-vue";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { listAuditLogs, type AuditLogRecord } from "@/api/office";

const loading = ref(false);
const auditLogs = ref<AuditLogRecord[]>([]);
const page = ref(0);
const size = ref(20);
const total = ref(0);
const searchUser = ref("");
const dateRange = ref<[string, string] | null>(null);

const columns = [
  { title: "操作人", dataIndex: "username", key: "username", width: 120 },
  { title: "请求方法", key: "method", width: 100 },
  { title: "请求路径", key: "path", width: 420, ellipsis: true },
  { title: "状态码", key: "status", width: 90 },
  { title: "客户端IP", dataIndex: "clientIp", key: "ip", width: 140 },
  { title: "耗时", key: "duration", width: 90 },
  { title: "操作时间", key: "time", width: 180 },
];

function methodColor(method: string) {
  return { GET: "green", POST: "blue", PUT: "orange", PATCH: "cyan", DELETE: "red" }[method] || "default";
}

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    const result = await listAuditLogs(page.value, size.value);
    auditLogs.value = result.content;
    total.value = result.totalElements;
  } catch (e: any) {
    message.error(e.message || "加载审计日志失败");
  } finally {
    loading.value = false;
  }
}

function handleTableChange(pagination: any) {
  page.value = pagination.current - 1;
  size.value = pagination.pageSize;
  loadData();
}
</script>
