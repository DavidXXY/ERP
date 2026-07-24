<template>
  <div class="page-stack">
    <a-card title="操作审计日志">
      <template #extra>
        <a-space>
          <a-range-picker v-model:value="dateRange" @change="loadData" />
          <a-input-search
            v-model:value="searchUser"
            placeholder="搜索操作人、路径或业务对象"
            style="width: 200px"
            allow-clear
            @search="loadData"
          />
          <a-button @click="exportAudits">导出</a-button>
          <a-button :loading="loading" @click="loadData"
            ><template #icon><ReloadOutlined /></template>刷新</a-button
          >
        </a-space>
      </template>
      <a-table
        :columns="columns"
        :data-source="auditLogs"
        :loading="loading"
        :pagination="{
          current: page + 1,
          pageSize: size,
          total,
          showSizeChanger: true,
          showTotal: (t: any) => '共 ' + t + ' 条',
        }"
        :row-key="(r: any) => r.id"
        :scroll="{ x: 1200 }"
        size="small"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'method'">
            <a-tag :color="methodColor(record.httpMethod)">{{
              record.httpMethod
            }}</a-tag>
          </template>
          <template v-else-if="column.key === 'path'">
            <span
              :title="record.requestPath"
              style="
                max-width: 400px;
                display: inline-block;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
              "
              >{{ record.requestPath }}</span
            >
          </template>
          <template v-else-if="column.key === 'module'"
            ><a-tag>{{
              record.bizModule || auditObjectLabel(record.requestPath)
            }}</a-tag></template
          >
          <template v-else-if="column.key === 'status'">
            <a-tag
              :color="
                record.responseStatus < 300
                  ? 'green'
                  : record.responseStatus < 400
                    ? 'orange'
                    : 'red'
              "
              >{{ record.responseStatus }}</a-tag
            >
          </template>
          <template v-else-if="column.key === 'duration'"
            >{{ record.durationMs }}ms</template
          >
          <template v-else-if="column.key === 'time'">{{
            record.createdAt?.slice(0, 19)?.replace("T", " ") || "-"
          }}</template>
          <template v-else-if="column.key === 'action'"
            ><a-button type="link" size="small" @click="openDetail(record)"
              >详情</a-button
            ></template
          >
        </template>
      </a-table>
    </a-card>

    <a-drawer v-model:open="detailOpen" title="审计详情" :width="560">
      <a-descriptions v-if="selected" bordered :column="1" size="small">
        <a-descriptions-item label="操作人">{{
          selected.username || "-"
        }}</a-descriptions-item>
        <a-descriptions-item label="请求方法"
          ><a-tag :color="methodColor(selected.httpMethod)">{{
            selected.httpMethod
          }}</a-tag></a-descriptions-item
        >
        <a-descriptions-item label="业务对象">{{
          auditObjectLabel(selected.requestPath)
        }}</a-descriptions-item>
        <a-descriptions-item label="业务模块">{{
          selected.bizModule || "-"
        }}</a-descriptions-item>
        <a-descriptions-item label="对象ID">{{
          selected.bizObject || "-"
        }}</a-descriptions-item>
        <a-descriptions-item label="操作类型">{{
          selected.operationType || "-"
        }}</a-descriptions-item>
        <a-descriptions-item label="请求路径">{{
          selected.requestPath
        }}</a-descriptions-item>
        <a-descriptions-item label="查询参数">{{
          selected.queryString || "-"
        }}</a-descriptions-item>
        <a-descriptions-item label="响应状态"
          ><a-tag
            :color="
              selected.responseStatus < 300
                ? 'green'
                : selected.responseStatus < 400
                  ? 'orange'
                  : 'red'
            "
            >{{ selected.responseStatus }}</a-tag
          ></a-descriptions-item
        >
        <a-descriptions-item label="客户端 IP">{{
          selected.clientIp || "-"
        }}</a-descriptions-item>
        <a-descriptions-item label="耗时"
          >{{ selected.durationMs }}ms</a-descriptions-item
        >
        <a-descriptions-item label="操作时间">{{
          selected.createdAt?.slice(0, 19)?.replace("T", " ") || "-"
        }}</a-descriptions-item>
      </a-descriptions>
    </a-drawer>
  </div>
</template>
<script setup lang="ts">
import { onMounted, ref } from "vue";
import type { Dayjs } from "dayjs";
import { message } from "ant-design-vue";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { listAuditLogs, type AuditLogRecord } from "@/api/office";
import { downloadCsv } from "@/utils/csv";

const loading = ref(false);
const auditLogs = ref<AuditLogRecord[]>([]);
const page = ref(0);
const size = ref(20);
const total = ref(0);
const searchUser = ref("");
const dateRange = ref<[Dayjs, Dayjs] | null>(null);
const detailOpen = ref(false);
const selected = ref<AuditLogRecord | null>(null);

const columns = [
  { title: "操作人", dataIndex: "username", key: "username", width: 120 },
  { title: "请求方法", key: "method", width: 100 },
  { title: "模块", key: "module", width: 120 },
  {
    title: "操作类型",
    dataIndex: "operationType",
    key: "operationType",
    width: 140,
  },
  { title: "请求路径", key: "path", width: 420, ellipsis: true },
  { title: "状态码", key: "status", width: 90 },
  { title: "客户端IP", dataIndex: "clientIp", key: "ip", width: 140 },
  { title: "耗时", key: "duration", width: 90 },
  { title: "操作时间", key: "time", width: 180 },
  { title: "操作", key: "action", width: 90, fixed: "right" },
];

function methodColor(method: string) {
  return (
    { GET: "green", POST: "blue", PUT: "orange", PATCH: "cyan", DELETE: "red" }[
      method
    ] || "default"
  );
}

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    const result = await listAuditLogs(page.value, size.value, {
      keyword: searchUser.value.trim(),
      startDate: dateRange.value?.[0]?.format("YYYY-MM-DD"),
      endDate: dateRange.value?.[1]?.format("YYYY-MM-DD"),
    });
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

function openDetail(record: AuditLogRecord) {
  selected.value = record;
  detailOpen.value = true;
}

function auditObjectLabel(path: string) {
  if (path.includes("/crm/")) return "CRM";
  if (path.includes("/procurement/")) return "采购";
  if (path.includes("/inventory/")) return "库存";
  if (path.includes("/projects")) return "项目";
  if (path.includes("/finance/")) return "财务";
  if (path.includes("/office/")) return "OA";
  if (path.includes("/qualifications")) return "资质";
  if (path.includes("/system/")) return "系统";
  return "通用接口";
}

function exportAudits() {
  downloadCsv(
    "操作审计日志.csv",
    [
      "操作人",
      "方法",
      "模块",
      "操作类型",
      "业务对象",
      "路径",
      "查询参数",
      "状态",
      "IP",
      "耗时(ms)",
      "时间",
    ],
    auditLogs.value.map((item) => [
      item.username || "",
      item.httpMethod,
      item.bizModule || "",
      item.operationType || "",
      auditObjectLabel(item.requestPath),
      item.requestPath,
      item.queryString || "",
      item.responseStatus,
      item.clientIp || "",
      item.durationMs,
      item.createdAt?.slice(0, 19)?.replace("T", " ") || "",
    ]),
  );
}
</script>
