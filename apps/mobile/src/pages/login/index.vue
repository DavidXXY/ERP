<script setup lang="ts">
import { ref } from "vue";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
const username = ref("");
const password = ref("");
const mfaCode = ref("");
const mfaRequired = ref(false);
const loading = ref(false);
const error = ref("");

async function submit() {
  if (!username.value.trim() || !password.value) {
    error.value = "请输入账号和密码";
    return;
  }
  loading.value = true;
  error.value = "";
  try {
    const authenticated = await auth.login(
      username.value.trim(),
      password.value,
      mfaCode.value || undefined,
    );
    if (!authenticated) {
      mfaRequired.value = true;
      error.value = "请输入验证器动态码或恢复码";
      return;
    }
    uni.switchTab({ url: "/pages/home/index" });
  } catch (e) {
    error.value = (e as Error).message;
  } finally { loading.value = false; }
}

async function wechatLogin() {
  loading.value = true;
  error.value = "";
  try {
    await auth.loginWithWechat();
    uni.switchTab({ url: "/pages/home/index" });
  } catch (e) {
    const message = (e as Error).message;
    error.value = message.includes("绑定") ? "该微信尚未绑定ERP账号，请先使用账号密码登录后完成绑定" : message;
  } finally { loading.value = false; }
}
</script>

<template>
  <view class="login-page safe-top">
    <view class="brand-block">
      <view class="brand-mark">OPS</view>
      <text class="brand-name">工程运维 ERP</text>
      <text class="brand-copy">移动外勤与协同工作台</text>
    </view>

    <view class="login-panel">
      <text class="panel-title">登录工作台</text>
      <text class="panel-caption">使用现有 ERP 账号继续</text>
      <view class="field">
        <text class="field-label">账号</text>
        <input v-model="username" class="field-input" placeholder="请输入用户名" confirm-type="next" />
      </view>
      <view class="field">
        <text class="field-label">密码</text>
        <input v-model="password" class="field-input" password placeholder="请输入密码" confirm-type="done" @confirm="submit" />
      </view>
      <view v-if="mfaRequired" class="field">
        <text class="field-label">动态验证码或恢复码</text>
        <input v-model="mfaCode" class="field-input" placeholder="6 位动态码或恢复码" confirm-type="done" @confirm="submit" />
      </view>
      <view v-if="error" class="error-line"><uni-icons type="info" size="16" color="#c33b3b" /><text>{{ error }}</text></view>
      <button class="primary-btn" :disabled="loading" @click="submit">
        <uni-icons v-if="!loading" type="locked" size="19" color="#fff" />{{ loading ? "正在登录" : mfaRequired ? "验证并登录" : "登录" }}
      </button>
      <button v-if="mfaRequired" class="challenge-reset" :disabled="loading" @click="mfaRequired = false; mfaCode = ''; error = ''">返回账号登录</button>
      <!-- #ifdef MP-WEIXIN -->
      <button class="wechat-btn" :disabled="loading" @click="wechatLogin"><uni-icons type="weixin" size="22" color="#176b5b" />微信快捷登录</button>
      <!-- #endif -->
    </view>

    <text class="security-note">账号权限与 PC 管理端保持一致</text>
  </view>
</template>

<style scoped lang="scss">
.login-page { min-height: 100vh; padding: 80rpx 42rpx 50rpx; background: #eef2f1; position: relative; overflow: hidden; }
.login-page::before { content: ""; position: absolute; left: 0; top: 0; width: 18rpx; height: 100%; background: #176b5b; }
.brand-block { margin: 70rpx 0 80rpx; }
.brand-mark { width: 96rpx; height: 96rpx; display: flex; align-items: center; justify-content: center; border-radius: 14rpx; background: #17202a; color: #fff; font-size: 25rpx; font-weight: 850; }
.brand-name { display: block; margin-top: 28rpx; color: #17202a; font-size: 50rpx; font-weight: 800; letter-spacing: 0; }
.brand-copy { display: block; margin-top: 12rpx; color: #64706c; font-size: 26rpx; }
.login-panel { padding: 38rpx 32rpx 34rpx; border: 1rpx solid #d9e0de; border-radius: 14rpx; background: #fff; box-shadow: 0 16rpx 45rpx rgba(33, 53, 48, .08); }
.panel-title { display: block; font-size: 35rpx; font-weight: 750; }
.panel-caption { display: block; margin: 10rpx 0 38rpx; color: #77808b; font-size: 24rpx; }
.error-line { display: flex; align-items: center; gap: 9rpx; margin: -4rpx 0 22rpx; color: #c33b3b; font-size: 24rpx; }
.wechat-btn { margin-top: 20rpx; min-height: 84rpx; display: flex; align-items: center; justify-content: center; gap: 12rpx; border: 1rpx solid #bad0cb; border-radius: 12rpx; color: #176b5b; background: #f7fbfa; font-size: 28rpx; }
.challenge-reset { margin-top: 10rpx; border: 0; color: #176b5b; background: transparent; font-size: 25rpx; }
.security-note { display: block; margin-top: 36rpx; text-align: center; color: #85908d; font-size: 22rpx; }
</style>
