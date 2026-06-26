<template>
  <a-layout class="app-layout">
    <a-layout-sider
      v-model:collapsed="app.sidebarCollapsed"
      class="app-sider"
      collapsible
      :width="252"
    >
      <div class="brand">
        <div class="brand-mark">运</div>
        <div v-if="!app.sidebarCollapsed">
          <strong>工程运维ERP</strong>
          <span>生产系统骨架</span>
        </div>
      </div>
      <a-menu
        mode="inline"
        theme="dark"
        :selected-keys="[activeKey]"
        :open-keys="openKeys"
        @open-change="onOpenChange"
        @click="onMenuClick"
      >
        <a-menu-item v-if="auth.can('dashboard:view')" key="/dashboard">
          <template #icon><DashboardOutlined /></template>
          <span>经营驾驶舱</span>
        </a-menu-item>

        <a-sub-menu
          v-if="auth.can('crm:customer:view') || auth.can('crm:opportunity:view') || auth.can('crm:contract:view')"
          key="crm"
        >
          <template #icon><TeamOutlined /></template>
          <template #title>CRM</template>
          <a-menu-item v-if="auth.can('crm:customer:view')" key="/crm/customers">客户池</a-menu-item>
          <a-menu-item v-if="auth.can('crm:opportunity:view')" key="/crm/opportunities">线索商机</a-menu-item>
          <a-menu-item v-if="auth.can('crm:contract:view')" key="/crm/contracts">维保合同</a-menu-item>
        </a-sub-menu>

        <a-menu-item v-if="auth.can('procurement:view')" key="/procurement">
          <template #icon><ShoppingCartOutlined /></template>
          <span>供应链采购</span>
        </a-menu-item>
        <a-menu-item v-if="auth.can('project:view')" key="/projects">
          <template #icon><ProjectOutlined /></template>
          <span>项目管理</span>
        </a-menu-item>
        <a-menu-item v-if="auth.can('inventory:view')" key="/inventory">
          <template #icon><InboxOutlined /></template>
          <span>备件仓储</span>
        </a-menu-item>
        <a-menu-item v-if="auth.can('finance:view')" key="/finance">
          <template #icon><WalletOutlined /></template>
          <span>财务资金</span>
        </a-menu-item>
        <a-menu-item v-if="auth.can('system:view')" key="/system">
          <template #icon><SettingOutlined /></template>
          <span>系统设置</span>
        </a-menu-item>
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <a-layout-header class="app-header">
        <div>
          <a-typography-title :level="3">{{ route.meta.title }}</a-typography-title>
          <span>模块化单体 · PostgreSQL · Spring Boot · Vue</span>
        </div>
        <a-space>
          <a-tag color="green">开发环境</a-tag>
          <a-dropdown>
            <a-button type="text">
              {{ auth.user?.displayName || "当前用户" }}
            </a-button>
            <template #overlay>
              <a-menu>
                <a-menu-item key="roles" disabled>
                  {{ auth.user?.roles.join(" / ") || "未加载角色" }}
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout" @click="handleLogout">退出登录</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
          <a-button type="text" @click="app.toggleSidebar">
            <template #icon><MenuFoldOutlined /></template>
          </a-button>
        </a-space>
      </a-layout-header>

      <a-layout-content class="app-content">
        <RouterView />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import DashboardOutlined from "@ant-design/icons-vue/DashboardOutlined";
import InboxOutlined from "@ant-design/icons-vue/InboxOutlined";
import MenuFoldOutlined from "@ant-design/icons-vue/MenuFoldOutlined";
import ProjectOutlined from "@ant-design/icons-vue/ProjectOutlined";
import SettingOutlined from "@ant-design/icons-vue/SettingOutlined";
import ShoppingCartOutlined from "@ant-design/icons-vue/ShoppingCartOutlined";
import TeamOutlined from "@ant-design/icons-vue/TeamOutlined";
import WalletOutlined from "@ant-design/icons-vue/WalletOutlined";
import { useAppStore } from "@/stores/app";
import { useAuthStore } from "@/stores/auth";

const app = useAppStore();
const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const openKeys = ref<string[]>(["crm"]);

const activeKey = computed(() => route.path);

function onOpenChange(keys: string[]) {
  openKeys.value = keys;
}

function onMenuClick(event: { key: string }) {
  router.push(event.key);
}

function handleLogout() {
  auth.logout();
  router.replace("/login");
}
</script>
