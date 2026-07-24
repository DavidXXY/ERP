<template>
  <div class="page-stack">
    <a-card>
      <template #title
        >消息中心
        <a-tag v-if="unreadCount > 0" color="red"
          >{{ unreadCount }} 条未读</a-tag
        ></template
      >
      <template #extra
        ><a-space
          ><a-button @click="goBack">返回办公室</a-button
          ><a-button :loading="loading" @click="loadData"
            ><template #icon><ReloadOutlined /></template>刷新</a-button
          ><a-button v-if="unreadCount > 0" @click="handleMarkAllRead"
            >全部标记已读</a-button
          ></a-space
        ></template
      >
      <a-space class="table-toolbar" wrap>
        <a-radio-group
          v-model:value="readFilter"
          button-style="solid"
          size="small"
        >
          <a-radio-button value="all">全部</a-radio-button>
          <a-radio-button value="unread">未读</a-radio-button>
          <a-radio-button value="read">已读</a-radio-button>
        </a-radio-group>
      </a-space>
      <a-list
        bordered
        :data-source="filteredNotifications"
        :loading="loading"
        class="notification-list"
      >
        <template #renderItem="{ item }">
          <a-list-item
            :class="{ 'notification-unread': !item.read }"
            class="notification-item"
          >
            <div class="notification-body">
              <div class="notification-head">
                <div class="notification-title-row">
                  <span
                    :class="{ 'notification-title-unread': !item.read }"
                    class="notification-title"
                    >{{ item.title }}</span
                  >
                  <a-tag :color="typeColor(item.type)">{{
                    typeLabel(item.type)
                  }}</a-tag>
                  <a-tag v-if="!item.read" color="blue">未读</a-tag>
                  <a-tag v-else color="default">已读</a-tag>
                </div>
                <span class="notification-time">{{
                  formatDateTime(item.createdAt)
                }}</span>
              </div>
              <div class="notification-content">
                {{ item.content || "无消息内容" }}
              </div>
              <div class="notification-meta">
                <span>关联类型：{{ item.relatedType || "-" }}</span>
                <span v-if="item.relatedId">关联ID：{{ item.relatedId }}</span>
                <span v-if="item.readAt"
                  >已读时间：{{ formatDateTime(item.readAt) }}</span
                >
              </div>
            </div>
            <template #actions>
              <a-button
                v-if="!item.read"
                type="link"
                size="small"
                @click.stop="handleRead(item)"
                >标记已读</a-button
              >
              <a-button
                v-if="relatedRoute(item)"
                type="link"
                size="small"
                @click.stop="goRelated(item)"
                >查看关联</a-button
              >
            </template>
          </a-list-item>
        </template>
        <template #emptyText>暂无消息</template>
      </a-list>
    </a-card>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { message } from "ant-design-vue";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import {
  listNotifications,
  readNotification,
  type NotificationRecord,
} from "@/api/office";
const router = useRouter();
const loading = ref(false);
const notifications = ref<NotificationRecord[]>([]);
const readFilter = ref<"all" | "unread" | "read">("all");
const unreadCount = computed(
  () => notifications.value.filter((n) => !n.read).length,
);
const filteredNotifications = computed(() =>
  notifications.value.filter(
    (item) =>
      readFilter.value === "all" ||
      (readFilter.value === "unread" ? !item.read : item.read),
  ),
);
onMounted(loadData);
async function loadData() {
  loading.value = true;
  try {
    notifications.value = await listNotifications();
    notifyLayout();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "数据加载失败");
  } finally {
    loading.value = false;
  }
}
function goBack() {
  router.push("/office");
}
async function handleRead(item: NotificationRecord) {
  try {
    await readNotification(item.id);
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "消息处理失败");
  }
}
async function handleMarkAllRead() {
  const unread = notifications.value.filter((n) => !n.read);
  if (!unread.length) return;
  for (const n of unread) {
    try {
      await readNotification(n.id);
    } catch {}
  }
  message.success("已标记 " + unread.length + " 条为已读");
  await loadData();
}
function notifyLayout() {
  window.dispatchEvent(
    new CustomEvent("notification-count-changed", {
      detail: notifications.value.filter((item) => !item.read).length,
    }),
  );
}
function relatedRoute(item: NotificationRecord) {
  const type = (item.relatedType || item.type || "").toUpperCase();
  if (type.includes("APPROVAL")) return "/workbench/todos?tab=approvals";
  if (type.includes("EXPENSE")) return "/office/expenses";
  if (type.includes("OUTSOURCE")) return "/office/outsourcing";
  if (type.includes("DOCUMENT")) return "/office/documents";
  if (type.includes("EQUIPMENT")) return "/maintenance/equipment";
  if (type.includes("CERTIFICATE")) return "/maintenance/certificates";
  if (type.includes("CONTRACT")) return "/crm/contracts";
  if (type.includes("RECEIVABLE")) return "/finance/receivables";
  if (type.includes("PART") || type.includes("INVENTORY"))
    return "/inventory/parts";
  return "";
}
async function goRelated(item: NotificationRecord) {
  if (!item.read) await handleRead(item);
  const route = relatedRoute(item);
  if (route) router.push(route);
}
function typeLabel(type?: string) {
  return (
    (
      {
        APPROVAL: "审批",
        APPROVAL_RESULT: "审批结果",
        APPROVAL_SLA: "审批SLA",
        APPROVAL_ESCALATED: "审批升级",
        EQUIPMENT: "设备",
        CERTIFICATE: "证书",
        CONTRACT: "合同",
        FINANCE: "财务",
        INVENTORY: "库存",
        OUTSOURCE: "外包",
      } as Record<string, string>
    )[type || ""] ||
    type ||
    "消息"
  );
}
function typeColor(type?: string) {
  if (!type) return "default";
  if (type.includes("APPROVAL")) return "blue";
  if (type.includes("SLA") || type.includes("ESCALATED")) return "red";
  if (type === "FINANCE") return "green";
  if (type === "INVENTORY") return "orange";
  return "default";
}

function formatDateTime(v?: string) {
  return v ? new Date(v).toLocaleString("zh-CN", { hour12: false }) : "-";
}
</script>
<style scoped>
.notification-list :deep(.ant-list-item-action) {
  margin-left: 16px;
}
.notification-item {
  align-items: flex-start;
}
.notification-unread {
  background: #f0f7ff;
}
.notification-title-unread {
  font-weight: 600;
}
.notification-body {
  width: 100%;
  min-width: 0;
}
.notification-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 8px;
}
.notification-title-row {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}
.notification-title {
  font-size: 15px;
  color: #101828;
  word-break: break-word;
}
.notification-time {
  flex: none;
  color: #8c8c8c;
  font-size: 12px;
}
.notification-content {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
  color: #344054;
}
.notification-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 10px;
  color: #8c8c8c;
  font-size: 12px;
}
@media (max-width: 720px) {
  .notification-head {
    display: block;
  }
  .notification-time {
    display: block;
    margin-top: 6px;
  }
}
</style>
