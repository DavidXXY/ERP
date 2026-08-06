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

        <a-form :model="form" layout="vertical" @finish="submit">
          <template v-if="mode === 'register'">
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
              <a-form-item label="联系电话" name="phone">
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
          <a-button type="primary" html-type="submit" size="large" block :loading="submitting">
            {{ mode === "login" ? "登录" : "注册并进入" }}
          </a-button>
        </a-form>
      </div>
    </section>
  </main>
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
const form = reactive({ companyName: "", unifiedSocialCreditCode: "", contactName: "", phone: "", email: "", password: "", registrationCode: "" });

async function submit() {
  if (!form.email || form.password.length < 8) {
    message.warning("请填写有效邮箱，密码至少 8 位");
    return;
  }
  if (mode.value === "register" && (!form.companyName || !form.unifiedSocialCreditCode || !form.contactName)) {
    message.warning("请完整填写企业及联系人信息");
    return;
  }
  submitting.value = true;
  try {
    const session = mode.value === "login"
      ? await api.login({ email: form.email, password: form.password })
      : await api.register({ ...form });
    store.setSession(session);
    await router.push(session.account.mustChangePassword ? "/account" : "/dashboard");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "操作失败");
  } finally {
    submitting.value = false;
  }
}
</script>
