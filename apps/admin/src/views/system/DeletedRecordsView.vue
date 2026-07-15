<template>
  <div class="page-stack">
    <a-card title="删除回收站">
      <template #extra>
        <a-button :loading="loading" @click="loadData">刷新</a-button>
      </template>
      <a-table :columns="columns" :data-source="records" :loading="loading" row-key="id" :pagination="{ pageSize: 10 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'entity'">
            <strong>{{ entityLabel(record.entityType) }}</strong>
            <span class="table-subtitle">{{ record.title }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 'PENDING' ? 'orange' : 'blue'">
              {{ record.status === "PENDING" ? "待审批" : "已审批隐藏" }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'request'">
            {{ record.requestedBy || "-" }}
            <span class="table-subtitle">{{ formatDateTime(record.requestedAt) }}</span>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button v-if="record.status === 'PENDING'" type="link" size="small" @click="approve(record.id)">审批通过</a-button>
              <a-popconfirm title="恢复后该数据会重新在业务列表显示，确认恢复？" @confirm="restore(record.id)">
                <a-button type="link" size="small">恢复显示</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { message } from "ant-design-vue";
import { approveDeletedRecord, listDeletedRecords, restoreDeletedRecord, type DeletedRecord } from "@/api/system";

const loading = ref(false);
const records = ref<DeletedRecord[]>([]);
const columns = [
  { title: "数据", key: "entity" },
  { title: "状态", key: "status", width: 130 },
  { title: "申请人", key: "request", width: 220 },
  { title: "操作", key: "actions", width: 180 },
];

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    records.value = await listDeletedRecords();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "加载失败");
  } finally {
    loading.value = false;
  }
}

async function approve(id: string) {
  await approveDeletedRecord(id);
  message.success("已审批通过");
  await loadData();
}

async function restore(id: string) {
  await restoreDeletedRecord(id);
  message.success("已恢复显示");
  await loadData();
}

function entityLabel(type: string) {
  return ({ CUSTOMER: "客户", OPPORTUNITY: "商机", QUOTE: "报价", CONTRACT: "合同", FOLLOW_UP: "跟进", PROJECT: "项目" } as Record<string, string>)[type] || type;
}

function formatDateTime(value?: string) {
  return value ? new Intl.DateTimeFormat("zh-CN", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)) : "-";
}
</script>
