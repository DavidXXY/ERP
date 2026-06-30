<template>
  <a-layout class="app-layout">
    <a-layout-sider
      v-model:collapsed="app.sidebarCollapsed"
      class="app-sider"
      collapsible
      breakpoint="lg"
      :collapsed-width="72"
      :width="252"
    >
      <div class="brand">
        <div class="brand-mark">企</div>
        <div v-if="!app.sidebarCollapsed">
          <strong>企业管理系统</strong>
          <span>一体化经营平台</span>
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
          v-if="canAccessCrm"
          key="crm"
        >
          <template #icon><TeamOutlined /></template>
          <template #title>CRM</template>
          <a-menu-item v-if="auth.can('crm:customer:view')" key="/crm/customers">客户池</a-menu-item>
          <a-menu-item v-if="auth.can('crm:opportunity:view')" key="/crm/opportunities">线索商机</a-menu-item>
          <a-menu-item v-if="auth.can('crm:quote:view')" key="/crm/quotes">报价方案</a-menu-item>
          <a-menu-item v-if="auth.can('crm:contract:view')" key="/crm/contracts">客户合同</a-menu-item>
          <a-menu-item v-if="auth.can('crm:followup:view')" key="/crm/follow-ups">跟进回访</a-menu-item>
          <a-menu-item v-if="auth.can('crm:renewal:view')" key="/crm/renewals">续约管理</a-menu-item>
          <a-menu-item v-if="auth.can('crm:receivable:view')" key="/crm/receivables">合同应收</a-menu-item>
          <a-menu-item v-if="auth.can('crm:profile:view')" key="/crm/profiles">客户经营画像</a-menu-item>
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
          <span>库存管理</span>
        </a-menu-item>
        <a-sub-menu v-if="canAccessMaintenance" key="maintenance">
          <template #icon><ToolOutlined /></template>
          <template #title>服务管理</template>
          <a-menu-item v-if="auth.can('maintenance:workorder:view')" key="/maintenance/work-orders">服务工单</a-menu-item>
          <a-menu-item v-if="auth.can('maintenance:equipment:view')" key="/maintenance/equipment">资产设备</a-menu-item>
          <a-menu-item v-if="auth.can('maintenance:plan:view')" key="/maintenance/plans">服务计划</a-menu-item>
        </a-sub-menu>
        <a-menu-item v-if="canAccessHumanResources" key="/hr">
          <template #icon><CalendarOutlined /></template>
          <span>人事管理</span>
        </a-menu-item>
        <a-sub-menu v-if="canAccessQualification" key="qualification">
          <template #icon><SafetyCertificateOutlined /></template>
          <template #title>资质管理</template>
          <a-menu-item v-if="auth.can('qualification:view')" key="/qualification/dashboard">资质总览</a-menu-item>
          <a-menu-item v-if="auth.can('qualification:company:view')" key="/qualification/companies">公司资质</a-menu-item>
          <a-menu-item v-if="auth.can('qualification:performance:view')" key="/qualification/performances">项目业绩</a-menu-item>
          <a-menu-item v-if="auth.can('qualification:tender:view')" key="/qualification/tender">投标查询</a-menu-item>
          <a-menu-item v-if="auth.can('qualification:warning:view')" key="/qualification/warnings">预警中心</a-menu-item>
        </a-sub-menu>
        <a-sub-menu v-if="canAccessOffice" key="office">
          <template #icon><FileDoneOutlined /></template>
          <template #title>OA协同</template>
          <a-menu-item v-if="auth.can('office:approval:view')" key="/office/approvals">审批中心</a-menu-item>
          <a-menu-item v-if="auth.can('office:expense:view')" key="/office/expenses">费用报销</a-menu-item>
          <a-menu-item v-if="auth.can('office:outsource:view')" key="/office/outsourcing">外包服务</a-menu-item>
          <a-menu-item v-if="auth.can('office:document:view')" key="/office/documents">电子档案</a-menu-item>
          <a-menu-item v-if="auth.can('office:notification:view')" key="/office/notifications">消息中心</a-menu-item>
          <a-menu-item v-if="auth.can('office:audit:view')" key="/office/audits">操作审计</a-menu-item>
        </a-sub-menu>
        <a-sub-menu v-if="canAccessFinance" key="finance">
          <template #icon><WalletOutlined /></template>
          <template #title>财务资金</template>
          <a-menu-item v-if="auth.can('finance:view')" key="/finance/overview">资金概览</a-menu-item>
          <a-menu-item v-if="auth.can('finance:receivable:view')" key="/finance/receivables">应收管理</a-menu-item>
          <a-menu-item v-if="auth.can('finance:payable:view')" key="/finance/payables">应付管理</a-menu-item>
          <a-menu-item v-if="canAccessPaymentApplications" key="/finance/payment-applications">付款申请</a-menu-item>
          <a-menu-item v-if="auth.can('finance:ledger:view')" key="/finance/ledger">总账报表</a-menu-item>
        </a-sub-menu>

        <a-sub-menu
          v-if="auth.can('system:view') || auth.can('system:organization:view') || auth.can('system:role:view') || auth.can('system:permission:view')"
          key="system"
        >
          <template #icon><SettingOutlined /></template>
          <template #title>系统设置</template>
          <a-menu-item v-if="auth.can('system:organization:view')" key="/system/organizations">组织架构</a-menu-item>
          <a-menu-item v-if="auth.can('system:role:view')" key="/system/roles">角色管理</a-menu-item>
          <a-menu-item v-if="auth.can('system:permission:view')" key="/system/permissions">权限管理</a-menu-item>
        </a-sub-menu>
      </a-menu>
      </a-layout-sider>

    <a-layout>
      <a-layout-header class="app-header">
        <div class="app-header-title">
          <a-typography-title :level="3">{{ route.meta.title }}</a-typography-title>
          <span class="app-header-subtitle">统一业务与经营管理</span>
        </div>
        <a-space class="app-header-actions">
          <a-tag class="app-environment" color="green">开发环境</a-tag>
          <a-dropdown :trigger="['click']">
            <a-button type="text">
              {{ auth.user?.displayName || "当前用户" }}
            </a-button>
            <template #overlay>
              <a-menu>
                <a-menu-item key="profile" @click="router.push('/profile')">
                  <template #icon><UserOutlined /></template>个人设置
                </a-menu-item>
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
import CalendarOutlined from "@ant-design/icons-vue/CalendarOutlined";
import FileDoneOutlined from "@ant-design/icons-vue/FileDoneOutlined";
import InboxOutlined from "@ant-design/icons-vue/InboxOutlined";
import MenuFoldOutlined from "@ant-design/icons-vue/MenuFoldOutlined";
import ProjectOutlined from "@ant-design/icons-vue/ProjectOutlined";
import SafetyCertificateOutlined from "@ant-design/icons-vue/SafetyCertificateOutlined";
import SettingOutlined from "@ant-design/icons-vue/SettingOutlined";
import ShoppingCartOutlined from "@ant-design/icons-vue/ShoppingCartOutlined";
import TeamOutlined from "@ant-design/icons-vue/TeamOutlined";
import ToolOutlined from "@ant-design/icons-vue/ToolOutlined";
import WalletOutlined from "@ant-design/icons-vue/WalletOutlined";
import UserOutlined from "@ant-design/icons-vue/UserOutlined";
import { useAppStore } from "@/stores/app";
import { useAuthStore } from "@/stores/auth";

const app = useAppStore();
const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const openKeys = ref<string[]>(route.path.startsWith("/finance") ? ["finance"] : route.path.startsWith("/office") ? ["office"] : route.path.startsWith("/maintenance") ? ["maintenance"] : route.path.startsWith("/qualification") ? ["qualification"] : route.path.startsWith("/system") ? ["system"] : route.path.startsWith("/crm") ? ["crm"] : []);

const activeKey = computed(() => route.path);
const canAccessCrm = computed(() => [
  "crm:customer:view",
  "crm:opportunity:view",
  "crm:quote:view",
  "crm:contract:view",
  "crm:followup:view",
  "crm:renewal:view",
  "crm:receivable:view",
  "crm:profile:view",
].some((permission) => auth.can(permission)));
const canAccessPaymentApplications = computed(() => ["finance:payable:view", "finance:payment:approve", "finance:payment:execute"].some((permission) => auth.can(permission)));
const canAccessFinance = computed(() => [
  "finance:view",
  "finance:receivable:view",
  "finance:payable:view",
  "finance:payment:approve",
  "finance:payment:execute",
  "finance:ledger:view",
].some((permission) => auth.can(permission)));
const canAccessMaintenance = computed(() => ["maintenance:view", "maintenance:equipment:view", "maintenance:plan:view", "maintenance:workorder:view"].some((permission) => auth.can(permission)));
const canAccessHumanResources = computed(() => ["qualification:employee:view", "qualification:certificate:view", "workforce:view"].some((permission) => auth.can(permission)));
const canAccessQualification = computed(() => ["qualification:view", "qualification:company:view", "qualification:performance:view", "qualification:tender:view", "qualification:warning:view"].some((permission) => auth.can(permission)));
const canAccessOffice = computed(() => ["office:view", "office:approval:view", "office:expense:view", "office:outsource:view", "office:document:view", "office:notification:view", "office:audit:view"].some((permission) => auth.can(permission)));

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
