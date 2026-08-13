<template>
  <a-layout class="portal-layout">
    <a-layout-sider
      v-model:collapsed="collapsed"
      breakpoint="lg"
      :collapsed-width="72"
      :width="232"
      class="portal-sider"
    >
      <div class="portal-brand">
        <SafetyCertificateOutlined />
        <span v-if="!collapsed">供应商协作门户</span>
        <a-button
          v-if="!collapsed"
          type="text"
          class="sider-close"
          aria-label="关闭导航"
          @click="collapsed = true"
        >
          <CloseOutlined />
        </a-button>
      </div>
      <a-menu
        :selected-keys="[activeKey]"
        theme="dark"
        mode="inline"
        @click="navigate"
      >
        <a-menu-item key="/dashboard"
          ><DashboardOutlined /><span>工作台</span></a-menu-item
        >
        <a-menu-item key="/notifications"
          ><BellOutlined /><span>通知中心</span></a-menu-item
        >
        <a-menu-item key="/inquiries"
          ><FileSearchOutlined /><span>询价与报价</span></a-menu-item
        >
        <a-menu-item key="/orders"
          ><ProfileOutlined /><span>采购订单</span></a-menu-item
        >
        <a-menu-item key="/finance"
          ><AccountBookOutlined /><span>开票与对账</span></a-menu-item
        >
        <a-menu-item key="/profile"
          ><BankOutlined /><span>企业资料</span></a-menu-item
        >
        <a-menu-item key="/documents"
          ><FolderOpenOutlined /><span>资质文件</span></a-menu-item
        >
        <a-menu-item key="/account"
          ><LockOutlined /><span>账号安全</span></a-menu-item
        >
      </a-menu>
      <div v-if="!collapsed" class="sider-account">
        <span>{{ store.session?.account.contactName }}</span>
        <small>{{ store.session?.account.email }}</small>
      </div>
    </a-layout-sider>
    <button
      v-if="!collapsed"
      class="sider-backdrop"
      aria-label="关闭导航"
      @click="collapsed = true"
    />
    <a-layout>
      <a-layout-header class="portal-header">
        <a-button
          type="text"
          class="mobile-menu"
          :aria-label="collapsed ? '展开导航' : '收起导航'"
          @click="collapsed = !collapsed"
        >
          <MenuOutlined />
        </a-button>
        <div class="company-identity">
          <strong>{{ store.session?.supplier.name || "供应商" }}</strong>
          <a-tag :color="admission.color">{{ admission.text }}</a-tag>
        </div>
        <a-dropdown
          v-model:open="notifOpen"
          trigger="click"
          placement="bottomRight"
        >
          <a-badge :count="unread" :offset="[-2, 6]">
            <a-button type="text" aria-label="通知"><BellOutlined /></a-button>
          </a-badge>
          <template #overlay>
            <div class="notif-panel">
              <div class="notif-head">
                <strong>消息通知</strong>
                <a-button
                  v-if="unread > 0"
                  type="link"
                  size="small"
                  @click="readAll"
                  >全部已读</a-button
                >
              </div>
              <div class="notif-footer">
                <a-button type="link" size="small" @click="router.push('/notifications')"
                  >查看全部通知</a-button
                >
              </div>
              <div v-if="notifications.length === 0" class="notif-empty">
                暂无通知
              </div>
              <a-list
                v-else
                size="small"
                :data-source="notifications"
                class="notif-list"
              >
                <template #renderItem="{ item }">
                  <a-list-item
                    :class="{ 'notif-item': true, unread: !item.read }"
                    @click="openNotification(item)"
                  >
                    <a-list-item-meta>
                      <template #title>
                        <span>{{ item.title }}</span>
                        <a-tag
                          v-if="!item.read"
                          color="red"
                          size="small"
                          class="notif-new"
                          >新</a-tag
                        >
                      </template>
                      <template #description>
                        <div class="notif-content">{{ item.content }}</div>
                        <small>{{ formatTime(item.createdAt) }}</small>
                      </template>
                    </a-list-item-meta>
                  </a-list-item>
                </template>
              </a-list>
            </div>
          </template>
        </a-dropdown>
        <a-dropdown>
          <a-button type="text"
            ><UserOutlined /> {{ store.session?.account.contactName }}
            <DownOutlined
          /></a-button>
          <template #overlay
            ><a-menu
              ><a-menu-item @click="router.push('/account')"
                ><LockOutlined /> 账号安全</a-menu-item
              ><a-menu-item @click="logout"
                ><LogoutOutlined /> 退出登录</a-menu-item
              ></a-menu
            ></template
          >
        </a-dropdown>
      </a-layout-header>
      <a-layout-content class="portal-content">
        <a-alert
          v-if="store.session?.account.mustChangePassword"
          type="warning"
          show-icon
          class="review-alert"
          message="请修改临时密码"
          description="为保护账号安全，完成密码修改前不能继续报价。"
        />
        <a-alert
          v-if="store.session?.account.status !== 'ACTIVE'"
          type="warning"
          show-icon
          class="review-alert"
          message="账号正在审核"
          description="您可以继续完善企业资料和上传资质；账号及供应商准入均通过后即可提交报价。"
        />
        <RouterView />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  BankOutlined,
  BellOutlined,
  CloseOutlined,
  DashboardOutlined,
  DownOutlined,
  FileSearchOutlined,
  FolderOpenOutlined,
  AccountBookOutlined,
  LockOutlined,
  LogoutOutlined,
  MenuOutlined,
  ProfileOutlined,
  SafetyCertificateOutlined,
  UserOutlined,
} from "@ant-design/icons-vue";
import { message } from "ant-design-vue";
import { usePortalStore } from "../store";
import * as api from "../api";
import { notificationRoute } from "../notifyTarget";

const route = useRoute();
const router = useRouter();
const store = usePortalStore();
const collapsed = ref(false);
const notifications = ref<api.PortalNotification[]>([]);
const unread = ref(0);
const notifOpen = ref(false);
const activeKey = computed(() => route.path);
const admission = computed(() => {
  const value = store.session?.supplier.admissionStatus;
  if (value === "APPROVED") return { color: "green", text: "已准入" };
  if (value === "REJECTED") return { color: "red", text: "准入退回" };
  return { color: "orange", text: "准入审核中" };
});

let notifTimer: number | undefined;
let refreshTimer: number | undefined;
onMounted(async () => {
  if (!store.session) {
    try {
      await store.restore();
    } catch {
      store.logout();
      await router.replace("/login");
    }
  }
  await loadNotifications();
  notifTimer = window.setInterval(loadNotifications, 60000);
  // 会话续期：生产环境令牌 30 分钟过期，每 5 分钟静默换取新令牌避免使用中被踢下线
  refreshTimer = window.setInterval(refreshSession, 5 * 60 * 1000);
});
onUnmounted(() => {
  if (notifTimer) window.clearInterval(notifTimer);
  if (refreshTimer) window.clearInterval(refreshTimer);
});
async function loadNotifications() {
  try {
    const [page, count] = await Promise.all([
      api.listNotifications(),
      api.unreadNotificationCount(),
    ]);
    notifications.value = page.items;
    unread.value = count;
  } catch {
    /* 忽略通知加载失败 */
  }
}
async function refreshSession() {
  try {
    const session = await api.getSession();
    store.setSession(session);
  } catch {
    /* 令牌失效由响应拦截器统一跳转登录 */
  }
}
function formatTime(value?: string) {
  if (!value) return "";
  return new Date(value).toLocaleString("zh-CN", { hour12: false });
}
async function openNotification(item: api.PortalNotification) {
  if (!item.read) {
    await api.markNotificationRead(item.id);
    item.read = true;
    unread.value = Math.max(0, unread.value - 1);
  }
  notifOpen.value = false;
  await router.push(notificationRoute(item));
}
async function readAll() {
  try {
    await api.markAllNotificationsRead();
    notifications.value.forEach((item) => (item.read = true));
    unread.value = 0;
  } catch (e) {
    message.error(e instanceof Error ? e.message : "操作失败");
  }
}
function navigate({ key }: { key: string }) {
  router.push(key);
  if (window.matchMedia("(max-width: 760px)").matches) collapsed.value = true;
}
async function logout() {
  store.logout();
  message.success("已退出登录");
  await router.push("/login");
}
</script>
<style scoped>
.notif-panel {
  width: 340px;
  max-height: 460px;
  overflow: hidden;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
}
.notif-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border-bottom: 1px solid #f0f0f0;
}
.notif-list {
  overflow-y: auto;
}
.notif-item {
  cursor: pointer;
  padding: 8px 14px !important;
}
.notif-item.unread {
  background: #e6f7ff;
}
.notif-new {
  margin-left: 6px;
}
.notif-content {
  color: #595959;
}
.notif-empty {
  text-align: center;
  padding: 28px 0;
  color: #8c8c8c;
}
.notif-footer {
  border-top: 1px solid #f0f0f0;
  text-align: center;
  padding: 2px 0;
}
</style>
