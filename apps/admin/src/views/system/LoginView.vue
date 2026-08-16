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
        <a-form-item>
          <a-checkbox v-model:checked="rememberMe">记住账号</a-checkbox>
        </a-form-item>
        <a-form-item
          v-if="mfaRequired"
          label="动态验证码或恢复码"
          name="mfaCode"
          :rules="[{ required: true, message: '请输入动态验证码或恢复码' }]"
        >
          <a-input
            v-model:value="formState.mfaCode"
            size="large"
            autocomplete="one-time-code"
            autofocus
          />
        </a-form-item>
        <a-button
          block
          size="large"
          type="primary"
          html-type="submit"
          :loading="loading"
        >
          {{ mfaRequired ? "验证并登录" : "登录系统" }}
        </a-button>
        <a-button
          v-if="mfaRequired"
          block
          type="link"
          @click="resetMfaChallenge"
        >
          返回账号登录
        </a-button>
      </a-form>
    </a-card>
  </main>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const loading = ref(false);
const errorMessage = ref("");
const mfaRequired = ref(false);
const rememberMe = ref(false);

const REMEMBER_KEY = "ops_erp_remember_credentials";

function encodeRemembered(value: string) {
  return btoa(encodeURIComponent(value));
}

function decodeRemembered(value: string) {
  return decodeURIComponent(atob(value));
}

function loadRememberedCredentials() {
  try {
    const raw = localStorage.getItem(REMEMBER_KEY);
    if (!raw) return;
    const saved = JSON.parse(raw) as { username?: string };
    if (saved.username) formState.username = decodeRemembered(saved.username);
    rememberMe.value = Boolean(saved.username);
  } catch {
    localStorage.removeItem(REMEMBER_KEY);
  }
}

function saveRememberedCredentials() {
  if (rememberMe.value) {
    localStorage.setItem(
      REMEMBER_KEY,
      JSON.stringify({
        username: encodeRemembered(formState.username.trim()),
      }),
    );
  } else {
    localStorage.removeItem(REMEMBER_KEY);
  }
}

const formState = reactive({
  username: "",
  password: "",
  mfaCode: "",
});

async function handleLogin() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const authenticated = await auth.login(
      formState.username,
      formState.password,
      formState.mfaCode || undefined,
    );
    if (!authenticated) {
      mfaRequired.value = true;
      errorMessage.value = "请输入验证器中的动态验证码，或使用一个恢复码";
      return;
    }
    saveRememberedCredentials();
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

function resetMfaChallenge() {
  mfaRequired.value = false;
  formState.mfaCode = "";
  errorMessage.value = "";
}

onMounted(loadRememberedCredentials);
</script>
