<template>
  <header class="page-heading">
    <div>
      <h1>通知中心</h1>
      <p>询价、定标、订单、财务与账号相关消息统一在这里查看。</p>
    </div>
    <a-button type="primary" ghost :disabled="unreadCount === 0" :loading="marking" @click="readAll"
      ><CheckOutlined /> 全部已读</a-button
    >
  </header>

  <div class="filter-row">
    <a-segmented v-model:value="filter" :options="filterOptions" /><span
      >{{ filtered.length }} 条</span
    >
  </div>

  <a-card :loading="loading" :bordered="false" class="notif-card">
    <a-empty v-if="!loading && filtered.length === 0" description="暂无相关通知" />
    <a-list v-else :data-source="filtered" size="small">
      <template #renderItem="{ item }">
        <a-list-item class="notif-row" :class="{ unread: !item.read }" @click="open(item)">
          <a-list-item-meta>
            <template #avatar>
              <a-avatar :style="{ backgroundColor: avatarColor(item) }" shape="square">
                <component :is="avatarIcon(item)" />
              </a-avatar>
            </template>
            <template #title>
              <span class="notif-title">{{ item.title }}</span>
              <a-tag v-if="!item.read" color="red" size="small">新</a-tag>
              <a-tag v-if="!item.read" color="blue" size="small">未读</a-tag>
              <a-tag size="small" class="type-tag">{{ typeText(item.type) }}</a-tag>
            </template>
            <template #description>
              <div class="notif-content">{{ item.content }}</div>
              <small>{{ formatTime(item.createdAt) }}</small>
            </template>
          </a-list-item-meta>
        </a-list-item>
      </template>
    </a-list>
    <div v-if="hasMore" class="load-more-row">
      <a-button :loading="loadingMore" @click="loadMore">加载更多</a-button>
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import {
  BellOutlined,
  CheckOutlined,
  FileSearchOutlined,
  ProfileOutlined,
  AccountBookOutlined,
  BankOutlined,
  FolderOpenOutlined,
  SafetyOutlined,
  DashboardOutlined,
} from "@ant-design/icons-vue";
import { message } from "ant-design-vue";
import * as api from "../api";
import { notificationRoute } from "../notifyTarget";

const router = useRouter();
const loading = ref(false);
const marking = ref(false);
const notifications = ref<api.PortalNotification[]>([]);
const filter = ref("ALL");
const hasMore = ref(false);
const loadingMore = ref(false);

const filterOptions = [
  { label: "全部", value: "ALL" },
  { label: "未读", value: "UNREAD" },
  { label: "询价", value: "INQUIRY" },
  { label: "订单", value: "ORDER" },
  { label: "财务", value: "FINANCE" },
  { label: "其他", value: "OTHER" },
];

const unreadCount = computed(
  () => notifications.value.filter((item) => !item.read).length,
);

const inquiryTypes = new Set([
  "INQUIRY",
  "INQUIRY_INVITATION",
  "CLARIFICATION_ANSWER",
  "AWARD",
  "NOT_AWARDED",
]);
const orderTypes = new Set([
  "ORDER",
  "ORDER_DOCUMENT",
  "ORDER_CHANGE",
  "SHIPMENT",
  "RECEIPT",
  "INSPECTION",
  "CONTRACT",
]);
const financeTypes = new Set(["INVOICE", "PAYABLE", "RECONCILIATION"]);

const filtered = computed(() => {
  const value = filter.value;
  if (value === "ALL") return notifications.value;
  if (value === "UNREAD") return notifications.value.filter((item) => !item.read);
  if (value === "INQUIRY")
    return notifications.value.filter((item) => inquiryTypes.has(item.relatedType || ""));
  if (value === "ORDER")
    return notifications.value.filter((item) => orderTypes.has(item.relatedType || ""));
  if (value === "FINANCE")
    return notifications.value.filter((item) => financeTypes.has(item.relatedType || ""));
  return notifications.value.filter(
    (item) =>
      !inquiryTypes.has(item.relatedType || "") &&
      !orderTypes.has(item.relatedType || "") &&
      !financeTypes.has(item.relatedType || ""),
  );
});

function typeText(type: string) {
  const map: Record<string, string> = {
    INQUIRY_INVITATION: "询价邀请",
    AWARD: "定标",
    NOT_AWARDED: "未中标",
    CLARIFICATION_ANSWER: "澄清答复",
    ORDER_DOCUMENT: "订单文件",
    ORDER: "订单",
    ORDER_CHANGE: "订单变更",
    INSPECTION: "质检结果",
    CONTRACT: "合同",
    INVOICE: "发票",
    PAYABLE: "付款",
    PERFORMANCE: "绩效评价",
    CHANGE_REQUEST: "资料变更",
  };
  return map[type] || type || "通知";
}

function avatarColor(item: api.PortalNotification) {
  if (inquiryTypes.has(item.relatedType || "")) return "#1677ff";
  if (orderTypes.has(item.relatedType || "")) return "#52c41a";
  if (financeTypes.has(item.relatedType || "")) return "#fa8c16";
  return "#8c8c8c";
}

function avatarIcon(item: api.PortalNotification) {
  if (inquiryTypes.has(item.relatedType || "")) return FileSearchOutlined;
  if (orderTypes.has(item.relatedType || "")) return ProfileOutlined;
  if (financeTypes.has(item.relatedType || "")) return AccountBookOutlined;
  return BellOutlined;
}

function formatTime(value?: string) {
  if (!value) return "";
  return new Date(value).toLocaleString("zh-CN", { hour12: false });
}

async function load() {
  loading.value = true;
  try {
    const page = await api.listNotifications();
    notifications.value = page.items;
    hasMore.value = page.hasMore;
  } catch (e) {
    message.error(e instanceof Error ? e.message : "加载通知失败");
  } finally {
    loading.value = false;
  }
}

async function loadMore() {
  const last = notifications.value[notifications.value.length - 1];
  if (!last) return;
  loadingMore.value = true;
  try {
    const page = await api.listNotifications(last.createdAt);
    notifications.value = [...notifications.value, ...page.items];
    hasMore.value = page.hasMore;
  } catch (e) {
    message.error(e instanceof Error ? e.message : "加载更多失败");
  } finally {
    loadingMore.value = false;
  }
}

async function open(item: api.PortalNotification) {
  if (!item.read) {
    try {
      await api.markNotificationRead(item.id);
      item.read = true;
    } catch {
      /* 忽略已读标记失败 */
    }
  }
  const target = notificationRoute(item);
  await router.push(target);
}

async function readAll() {
  marking.value = true;
  try {
    await api.markAllNotificationsRead();
    notifications.value.forEach((item) => (item.read = true));
    message.success("已全部标记为已读");
  } catch (e) {
    message.error(e instanceof Error ? e.message : "操作失败");
  } finally {
    marking.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}
.filter-row span {
  color: #8c8c8c;
  font-size: 13px;
}
.notif-card {
  border-radius: 10px;
}
.notif-row {
  cursor: pointer;
  padding: 10px 14px !important;
}
.notif-row.unread {
  background: #f0f7ff;
}
.notif-title {
  font-weight: 500;
}
.type-tag {
  margin-left: 6px;
  color: #595959;
}
.notif-content {
  color: #595959;
  white-space: pre-wrap;
}
.load-more-row {
  text-align: center;
  padding: 12px 0 4px;
}
@media (max-width: 760px) {
  .notif-row .ant-list-item-meta-title {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 4px;
  }
  .type-tag {
    margin-left: 0;
  }
  .notif-content {
    margin-top: 4px;
  }
}
</style>
