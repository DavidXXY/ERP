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

        <a-menu-item v-if="canAccessRiskCenter" key="/risk-center">
          <template #icon><WarningOutlined /></template>
          <span>统一风险中心</span>
        </a-menu-item>

        <a-menu-item v-if="canAccessBusinessTodos" key="/workbench/todos">
          <template #icon><FileDoneOutlined /></template>
          <span>业务待办中心</span>
        </a-menu-item>

        <a-sub-menu
          v-if="canAccessCrm"
          key="crm"
        >
          <template #icon><TeamOutlined /></template>
          <template #title>CRM</template>
          <a-menu-item v-if="auth.can('crm:dashboard:view')" key="/crm/dashboard">CRM仪表盘</a-menu-item>
          <a-menu-item v-if="auth.can('crm:customer:view')" key="/crm/customers">客户池</a-menu-item>
          <a-menu-item v-if="auth.can('crm:opportunity:view')" key="/crm/opportunities">线索商机</a-menu-item>
          <a-menu-item v-if="auth.can('crm:quote:view')" key="/crm/quotes">报价方案</a-menu-item>
          <a-menu-item v-if="auth.can('crm:contract:view')" key="/crm/contracts">客户合同</a-menu-item>
          <a-menu-item v-if="auth.can('crm:followup:view')" key="/crm/follow-ups">跟进回访</a-menu-item>
          <a-menu-item v-if="auth.can('crm:renewal:view')" key="/crm/renewals">续约管理</a-menu-item>
          <a-menu-item v-if="auth.can('crm:receivable:view')" key="/crm/receivables">合同应收</a-menu-item>
        </a-sub-menu>

        <a-sub-menu v-if="auth.can('procurement:view')" key="procurement">
          <template #icon><ShoppingCartOutlined /></template>
          <template #title>供应链采购</template>
          <a-menu-item key="/procurement/requests">采购申请</a-menu-item>
          <a-menu-item key="/procurement/orders">采购订单</a-menu-item>
          <a-menu-item key="/procurement/receipts">到货入库</a-menu-item>
          <a-menu-item key="/procurement/costs">成本归集</a-menu-item>
          <a-menu-item key="/procurement/payables">采购应付</a-menu-item>
          <a-menu-item key="/procurement/suppliers">供应商</a-menu-item>
          <a-menu-item key="/procurement/analytics">采购分析</a-menu-item>
          <a-menu-item key="/procurement/p2p">P2P流程</a-menu-item>
        </a-sub-menu>
        <a-sub-menu v-if="auth.can('project:view')" key="projects">
          <template #icon><ProjectOutlined /></template>
          <template #title>项目管理</template>
          <a-menu-item key="/projects/list">项目列表</a-menu-item>
          <a-menu-item key="/projects/budget">预算执行</a-menu-item>
          <a-menu-item key="/projects/costs">成本明细</a-menu-item>
          <a-menu-item key="/projects/stages">阶段履历</a-menu-item>
        </a-sub-menu>
        <a-sub-menu v-if="auth.can('inventory:view')" key="inventory">
          <template #icon><InboxOutlined /></template>
          <template #title>库存管理</template>
          <a-menu-item key="/inventory/parts">库存台账</a-menu-item>
          <a-menu-item key="/inventory/issues">领料管理</a-menu-item>
          <a-menu-item key="/inventory/movements">库存移动</a-menu-item>
          <a-menu-item key="/inventory/analytics">库存分析</a-menu-item>
        </a-sub-menu>
        <a-sub-menu v-if="canAccessHumanResources" key="hr">
          <template #icon><CalendarOutlined /></template>
          <template #title>人事管理</template>
          <a-menu-item key="/hr">员工中心</a-menu-item>
          <a-menu-item v-if="auth.can('qualification:employee:view')" key="/hr/lifecycle">入转调离</a-menu-item>
          <a-menu-item v-if="auth.can('workforce:view')" key="/hr/leaves">请假管理</a-menu-item>
          <a-menu-item v-if="auth.can('qualification:employee:view')" key="/hr/analytics">人力分析</a-menu-item>
          <a-menu-item v-if="auth.can('qualification:employee:manage')" key="/hr/leave-balances">请假额度</a-menu-item>
        </a-sub-menu>
        <a-sub-menu v-if="canAccessQualification" key="qualification">
          <template #icon><SafetyCertificateOutlined /></template>
          <template #title>资质管理</template>
          <a-menu-item v-if="auth.can('qualification:view')" key="/qualification/dashboard">资质总览</a-menu-item>
          <a-menu-item v-if="auth.can('qualification:company:view')" key="/qualification/companies">公司资质</a-menu-item>
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
          v-if="auth.can('system:view') || auth.can('system:organization:view') || auth.can('system:role:view') || auth.can('system:permission:view') || auth.can('risk:update')"
          key="system"
        >
          <template #icon><SettingOutlined /></template>
          <template #title>系统设置</template>
          <a-menu-item v-if="auth.can('system:view')" key="/system/health">系统运行情况</a-menu-item>
          <a-menu-item v-if="auth.can('system:organization:view')" key="/system/organizations">组织架构</a-menu-item>
          <a-menu-item v-if="auth.can('system:role:view')" key="/system/roles">角色管理</a-menu-item>
          <a-menu-item v-if="auth.can('system:permission:view')" key="/system/permissions">权限管理</a-menu-item>
          <a-menu-item v-if="auth.can('system:role:view')" key="/system/approval-configs">审批人员配置</a-menu-item>
          <a-menu-item v-if="auth.can('system:role:view') || auth.can('risk:update')" key="/system/process-rules">流程规则配置</a-menu-item>
        </a-sub-menu>
        <a-sub-menu key="self">
          <template #icon><UserOutlined /></template>
          <template #title>员工自助</template>
          <a-menu-item key="/self">我的工作台</a-menu-item>
          <a-menu-item key="/self/approvals">我的待办审批</a-menu-item>
          <a-menu-item key="/self/profile">我的档案</a-menu-item>
          <a-menu-item key="/self/leaves">我的请假</a-menu-item>
          <a-menu-item key="/self/balances">我的额度</a-menu-item>
        </a-sub-menu>
      </a-menu>
      <div class="sidebar-version" @click="router.push('/system/health')">v{{ sysVersion }}</div>
      </a-layout-sider>

    <a-layout>
      <a-layout-header class="app-header">
        <div class="app-header-title">
          <a-typography-title :level="3">{{ route.meta.title }}</a-typography-title>
          <span class="app-header-subtitle">统一业务与经营管理</span>
        </div>
        <GlobalSearch />
        <a-space class="app-header-actions">
          <a-tag class="app-environment" color="green">开发环境</a-tag>
          <a-badge :count="unreadCount" :overflow-count="99">
            <a-button type="text" @click="router.push('/office/notifications')">
              消息
            </a-button>
          </a-badge>
          <a-button type="text" @click="router.push('/risk-center')">风险</a-button>
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
                  {{ auth.user?.roleCodes.join(" / ") || "未加载角色" }}
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
import { computed, onMounted, ref } from "vue";
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
import WalletOutlined from "@ant-design/icons-vue/WalletOutlined";
import WarningOutlined from "@ant-design/icons-vue/WarningOutlined";
import UserOutlined from "@ant-design/icons-vue/UserOutlined";
import SearchOutlined from "@ant-design/icons-vue/SearchOutlined";
import GlobalSearch from "./GlobalSearch.vue";
import { getUnreadNotificationCount } from "@/api/office";
import { useAppStore } from "@/stores/app";
import { request } from "@/api/http";
import { useAuthStore } from "@/stores/auth";

const app = useAppStore();
const auth = useAuthStore();
const unreadCount = ref(0);

onMounted(async () => {
  try { unreadCount.value = await getUnreadNotificationCount(); } catch {}
  setInterval(async () => {
    try { unreadCount.value = await getUnreadNotificationCount(); } catch {}
  }, 30000);
});
const route = useRoute();
const router = useRouter();
const openKeys = ref<string[]>(route.path.startsWith("/self") ? ["self"] : route.path.startsWith("/finance") ? ["finance"] : route.path.startsWith("/office") ? ["office"] : route.path.startsWith("/qualification") ? ["qualification"] : route.path.startsWith("/system") ? ["system"] : route.path.startsWith("/crm") ? ["crm"] : route.path.startsWith("/procurement") ? ["procurement"] : route.path.startsWith("/inventory") ? ["inventory"] : route.path.startsWith("/projects") ? ["projects"] : []);

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
const canAccessRiskCenter = computed(() => [
  "dashboard:view",
  "risk:view",
  "office:approval:view",
  "office:notification:view",
  "office:expense:view",
  "office:outsource:view",
  "inventory:view",
  "procurement:view",
  "project:view",
  "finance:receivable:view",
  "finance:payable:view",
  "qualification:warning:view",
  "crm:renewal:view",
].some((permission) => auth.can(permission)));
const canAccessBusinessTodos = computed(() => [
  "dashboard:view",
  "risk:view",
  "office:approval:view",
  "office:notification:view",
  "crm:opportunity:view",
  "crm:quote:view",
  "crm:contract:view",
  "crm:receivable:view",
  "procurement:view",
  "inventory:view",
  "project:view",
  "finance:receivable:view",
  "finance:payable:view",
].some((permission) => auth.can(permission)));
const canAccessHumanResources = computed(() => ["qualification:employee:view", "qualification:certificate:view", "workforce:view"].some((permission) => auth.can(permission)));
const canAccessQualification = computed(() => ["qualification:view", "qualification:company:view", "qualification:tender:view", "qualification:warning:view"].some((permission) => auth.can(permission)));
const canAccessOffice = computed(() => ["office:view", "office:approval:view", "office:expense:view", "office:outsource:view", "office:document:view", "office:notification:view", "office:audit:view"].some((permission) => auth.can(permission)));

function onOpenChange(keys: string[]) {
  openKeys.value = keys;
}

const sysVersion = ref("0.1.0");
onMounted(async () => {
  try {
    const versionData = await request<{ version: string }>({ method: "GET", url: "/system/version" });
    sysVersion.value = versionData.version;
  } catch {}
});
function onMenuClick(event: { key: string }) {
  router.push(event.key);
}

function handleLogout() {
  auth.logout();
  router.replace("/login");
}
</script>

<style>
.sidebar-version {
  padding: 8px 24px;
  color: #8c8c8c;
  font-size: 11px;
  cursor: pointer;
  text-align: center;
  border-top: 1px solid #f0f0f0;
  margin-top: auto;
}
.sidebar-version:hover { color: #1890ff; }
</style>
