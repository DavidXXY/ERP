<template>
  <main class="login-page">
    <section class="login-hero">
      <div class="login-mark">企</div>
      <h1>企业管理系统</h1>
      <p>客户、项目、采购、库存、财务与办公流程统一管理。</p>
    </section>

    <a-card class="login-card" title="账号登录">
      <a-alert
        v-if="errorMessage"
        class="section-alert"
        type="warning"
        show-icon
        :message="errorMessage"
      />

      <a-form :model="formState" layout="vertical" @finish="handleLogin">
        <a-form-item
          label="账号"
          name="username"
          :rules="[{ required: true, message: '请输入账号' }]"
        >
          <a-input
            v-model:value="formState.username"
            size="large"
            autocomplete="username"
          />
        </a-form-item>
        <a-form-item
          label="密码"
          name="password"
          :rules="[{ required: true, message: '请输入密码' }]"
        >
          <a-input-password
            v-model:value="formState.password"
            size="large"
            autocomplete="current-password"
          />
        </a-form-item>
        <a-button
          block
          size="large"
          type="primary"
          html-type="submit"
          :loading="loading"
        >
          登录系统
        </a-button>
      </a-form>
    </a-card>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const loading = ref(false);
const errorMessage = ref("");

const formState = reactive({
  username: "",
  password: "",
});

async function handleLogin() {
  loading.value = true;
  errorMessage.value = "";
  try {
    await auth.login(formState.username, formState.password);
    const redirect =
      typeof route.query.redirect === "string"
        ? route.query.redirect
        : "/dashboard";
    await router.replace(redirect);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "登录失败";
  } finally {
    loading.value = false;
  }
}
</script>
