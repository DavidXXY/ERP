<template>
  <a-config-provider :locale="zhCN" :theme="theme">
    <RouterView />
  </a-config-provider>
</template>

<script setup lang="ts">
import zhCN from "ant-design-vue/es/locale/zh_CN";

const theme = {
  token: {
    colorPrimary: "#176b4d",
    colorInfo: "#176b4d",
    colorSuccess: "#27845f",
    colorWarning: "#b26a16",
    colorError: "#b43e3e",
    colorText: "#1e2d28",
    colorTextSecondary: "#66736e",
    colorBorder: "#d9e0dc",
    borderRadius: 6,
    fontFamily: "Inter, PingFang SC, Microsoft YaHei, sans-serif",
  },
};
</script>
