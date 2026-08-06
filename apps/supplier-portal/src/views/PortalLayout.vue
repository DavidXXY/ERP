<template>
  <a-layout class="portal-layout">
    <a-layout-sider v-model:collapsed="collapsed" breakpoint="lg" :collapsed-width="72" :width="232" class="portal-sider">
      <div class="portal-brand">
        <SafetyCertificateOutlined />
        <span v-if="!collapsed">供应商协作门户</span>
        <a-button v-if="!collapsed" type="text" class="sider-close" aria-label="关闭导航" @click="collapsed = true">
          <CloseOutlined />
        </a-button>
      </div>
      <a-menu :selected-keys="[activeKey]" theme="dark" mode="inline" @click="navigate">
        <a-menu-item key="/dashboard"><DashboardOutlined /><span>工作台</span></a-menu-item>
        <a-menu-item key="/inquiries"><FileSearchOutlined /><span>询价与报价</span></a-menu-item>
        <a-menu-item key="/profile"><BankOutlined /><span>企业资料</span></a-menu-item>
        <a-menu-item key="/documents"><FolderOpenOutlined /><span>资质文件</span></a-menu-item>
        <a-menu-item key="/account"><LockOutlined /><span>账号安全</span></a-menu-item>
      </a-menu>
      <div v-if="!collapsed" class="sider-account">
        <span>{{ store.session?.account.contactName }}</span>
        <small>{{ store.session?.account.email }}</small>
      </div>
    </a-layout-sider>
    <button v-if="!collapsed" class="sider-backdrop" aria-label="关闭导航" @click="collapsed = true" />
    <a-layout>
      <a-layout-header class="portal-header">
        <a-button type="text" class="mobile-menu" :aria-label="collapsed ? '展开导航' : '收起导航'" @click="collapsed = !collapsed">
          <MenuOutlined />
        </a-button>
        <div class="company-identity">
          <strong>{{ store.session?.supplier.name || "供应商" }}</strong>
          <a-tag :color="admission.color">{{ admission.text }}</a-tag>
        </div>
        <a-dropdown>
          <a-button type="text"><UserOutlined /> {{ store.session?.account.contactName }} <DownOutlined /></a-button>
          <template #overlay><a-menu><a-menu-item @click="router.push('/account')"><LockOutlined /> 账号安全</a-menu-item><a-menu-item @click="logout"><LogoutOutlined /> 退出登录</a-menu-item></a-menu></template>
        </a-dropdown>
      </a-layout-header>
      <a-layout-content class="portal-content">
        <a-alert v-if="store.session?.account.mustChangePassword" type="warning" show-icon class="review-alert" message="请修改临时密码" description="为保护账号安全，完成密码修改前不能继续报价。" />
        <a-alert v-if="store.session?.account.status !== 'ACTIVE'" type="warning" show-icon class="review-alert" message="账号正在审核" description="您可以继续完善企业资料和上传资质；账号及供应商准入均通过后即可提交报价。" />
        <RouterView />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { BankOutlined, CloseOutlined, DashboardOutlined, DownOutlined, FileSearchOutlined, FolderOpenOutlined, LockOutlined, LogoutOutlined, MenuOutlined, SafetyCertificateOutlined, UserOutlined } from "@ant-design/icons-vue";
import { message } from "ant-design-vue";
import { usePortalStore } from "../store";

const route = useRoute();
const router = useRouter();
const store = usePortalStore();
const collapsed = ref(false);
const activeKey = computed(() => route.path);
const admission = computed(() => {
  const value = store.session?.supplier.admissionStatus;
  if (value === "APPROVED") return { color: "green", text: "已准入" };
  if (value === "REJECTED") return { color: "red", text: "准入退回" };
  return { color: "orange", text: "准入审核中" };
});

onMounted(async () => {
  if (!store.session) {
    try { await store.restore(); } catch { store.logout(); await router.replace("/login"); }
  }
});
function navigate({ key }: { key: string }) {
  router.push(key);
  if (window.matchMedia("(max-width: 760px)").matches) collapsed.value = true;
}
async function logout() { store.logout(); message.success("已退出登录"); await router.push("/login"); }
</script>
