<template>
  <main class="auth-shell">
    <section class="auth-context">
      <div class="brand-mark"><SafetyCertificateOutlined /> 供应商协作门户</div>
      <div>
        <p class="eyebrow">采购协作</p>
        <h1>资料与报价，<br />在一个可信入口完成。</h1>
        <p class="auth-lead">维护企业档案、接收询价邀请、提交和追踪每一版报价。</p>
      </div>
      <div class="trust-row">
        <span><CheckCircleOutlined /> 身份独立</span>
        <span><FileProtectOutlined /> 版本留痕</span>
        <span><LockOutlined /> 权限隔离</span>
      </div>
    </section>

    <section class="auth-form-panel">
      <div class="auth-form-wrap">
        <a-segmented v-model:value="mode" block :options="modeOptions" />
        <div class="form-heading">
          <h2>{{ mode === "login" ? "登录供应商门户" : "注册供应商账号" }}</h2>
          <p>{{ mode === "login" ? "继续处理询价与企业资料" : "提交后可先完善资料，审核通过后参与报价" }}</p>
        </div>

        <a-form :model="form" :rules="rules" layout="vertical" @finish="submit">
          <template v-if="mode === 'register'">
            <div class="hp-field" aria-hidden="true">
              <a-form-item label="网站">
                <a-input
                  v-model:value="form.website"
                  name="website"
                  tabindex="-1"
                  autocomplete="off"
                />
              </a-form-item>
            </div>
            <a-form-item label="企业名称" name="companyName" required>
              <a-input v-model:value="form.companyName" autocomplete="organization" />
            </a-form-item>
            <a-form-item label="统一社会信用代码" name="unifiedSocialCreditCode" required>
              <a-input v-model:value="form.unifiedSocialCreditCode" :maxlength="80" />
            </a-form-item>
            <div class="form-grid-2">
              <a-form-item label="联系人" name="contactName" required>
                <a-input v-model:value="form.contactName" autocomplete="name" />
              </a-form-item>
              <a-form-item label="联系电话" name="phone" required>
                <a-input v-model:value="form.phone" autocomplete="tel" />
              </a-form-item>
            </div>
            <a-form-item label="采购邀请注册码" name="registrationCode">
              <a-input v-model:value="form.registrationCode" placeholder="已有供应商请填写采购方提供的注册码" />
            </a-form-item>
          </template>
          <a-form-item label="邮箱" name="email" required>
            <a-input v-model:value="form.email" type="email" autocomplete="email" />
          </a-form-item>
          <a-form-item label="密码" name="password" required>
            <a-input-password v-model:value="form.password" :autocomplete="mode === 'login' ? 'current-password' : 'new-password'" />
          </a-form-item>
          <a-form-item v-if="mode === 'login'" label="动态验证码">
            <a-input v-model:value="form.mfaCode" :maxlength="6" placeholder="已开启双重验证时填写" />
          </a-form-item>
          <a-button type="primary" html-type="submit" size="large" block :loading="submitting">
            {{ mode === "login" ? "登录" : "注册并进入" }}
          </a-button>
          <div v-if="mode === 'login'" class="auth-link-row">
            <a-button type="link" size="small" @click="resetOpen = true"
              >忘记密码？</a-button
            >
          </div>
        </a-form>
      </div>
    </section>
  </main>

  <a-modal
    v-model:open="resetOpen"
    title="重置密码"
    :footer="null"
    width="min(420px, 100vw)"
  >
    <template v-if="resetStep === 'request'">
      <a-form layout="vertical">
        <a-form-item label="注册邮箱" required>
          <a-input v-model:value="resetForm.email" type="email" placeholder="请输入门户注册邮箱" />
        </a-form-item>
        <a-button
          type="primary"
          block
          :loading="resetSending"
          @click="sendResetCode"
          >发送验证码</a-button
        >
      </a-form>
    </template>
    <template v-else>
      <a-alert
        v-if="devResetCode"
        type="info"
        show-icon
        :message="`开发模式验证码：${devResetCode}`"
        description="生产环境验证码将发送至邮箱。"
        style="margin-bottom: 14px"
      />
      <a-form layout="vertical">
        <a-form-item label="验证码" required>
          <a-input v-model:value="resetForm.code" :maxlength="6" placeholder="6 位验证码" />
        </a-form-item>
        <a-form-item label="新密码" required>
          <a-input-password v-model:value="resetForm.newPassword" placeholder="至少 8 位" />
        </a-form-item>
        <a-form-item label="确认新密码" required>
          <a-input-password v-model:value="resetForm.confirm" placeholder="再次输入新密码" />
        </a-form-item>
        <a-button
          type="primary"
          block
          :loading="resetSending"
          @click="submitReset"
          >重置密码</a-button
        >
      </a-form>
    </template>
  </a-modal>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { message } from "ant-design-vue";
import { CheckCircleOutlined, FileProtectOutlined, LockOutlined, SafetyCertificateOutlined } from "@ant-design/icons-vue";
import * as api from "../api";
import { usePortalStore } from "../store";

const router = useRouter();
const store = usePortalStore();
const mode = ref<"login" | "register">("login");
const modeOptions = [{ label: "登录", value: "login" }, { label: "注册", value: "register" }];
const submitting = ref(false);
const form = reactive({ companyName: "", unifiedSocialCreditCode: "", contactName: "", phone: "", email: "", password: "", mfaCode: "", registrationCode: "", website: "" });
const resetOpen = ref(false);
const resetStep = ref<"request" | "reset">("request");
const resetSending = ref(false);
const devResetCode = ref("");
const resetForm = reactive({
  email: "",
  code: "",
  newPassword: "",
  confirm: "",
});
const rules = {
  companyName: [{ required: true, message: "请输入企业名称" }],
  unifiedSocialCreditCode: [{ required: true, message: "请输入统一社会信用代码" }],
  contactName: [{ required: true, message: "请输入联系人" }],
  phone: [{ required: true, message: "请输入联系电话" }],
  email: [
    { required: true, message: "请输入邮箱" },
    { type: "email" as const, message: "请输入有效邮箱" },
  ],
  password: [
    { required: true, message: "请输入密码" },
    { min: 8, message: "密码至少 8 位" },
  ],
};

async function submit() {
  if (!form.email || form.password.length < 8) {
    message.warning("请填写有效邮箱，密码至少 8 位");
    return;
  }
  if (mode.value === "register" && (!form.companyName || !form.unifiedSocialCreditCode || !form.contactName || !form.phone)) {
    message.warning("请完整填写企业及联系人信息");
    return;
  }
  submitting.value = true;
  try {
    const session = mode.value === "login"
      ? await api.login({
          email: form.email,
          password: form.password,
          mfaCode: form.mfaCode || undefined,
        })
      : await api.register({ ...form });
    store.setSession(session);
    if (mode.value === "login" && session.lastLoginAt) {
      const time = new Date(session.lastLoginAt).toLocaleString("zh-CN", { hour12: false });
      const ip = session.lastLoginIp ? `，IP ${session.lastLoginIp}` : "";
      message.info(`上次登录：${time}${ip}，如非本人操作请及时修改密码`);
    }
    await router.push(session.account.mustChangePassword ? "/account" : "/dashboard");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "操作失败");
  } finally {
    submitting.value = false;
  }
}

async function sendResetCode() {
  if (!resetForm.email) {
    message.warning("请填写注册邮箱");
    return;
  }
  resetSending.value = true;
  try {
    const code = await api.forgotPassword(resetForm.email);
    devResetCode.value = code || "";
    resetStep.value = "reset";
    message.success(code ? "已生成验证码，请在下方输入" : "验证码已发送至邮箱");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "发送失败");
  } finally {
    resetSending.value = false;
  }
}

async function submitReset() {
  if (!resetForm.code.trim() || resetForm.newPassword.length < 8) {
    message.warning("请填写验证码，新密码至少 8 位");
    return;
  }
  if (resetForm.newPassword !== resetForm.confirm) {
    message.warning("两次输入的新密码不一致");
    return;
  }
  resetSending.value = true;
  try {
    await api.resetPassword(resetForm.email, resetForm.code.trim(), resetForm.newPassword);
    message.success("密码已重置，请使用新密码登录");
    resetOpen.value = false;
    resetStep.value = "request";
    resetForm.code = "";
    resetForm.newPassword = "";
    resetForm.confirm = "";
    devResetCode.value = "";
    mode.value = "login";
  } catch (error) {
    message.error(error instanceof Error ? error.message : "重置失败");
  } finally {
    resetSending.value = false;
  }
}
</script>
