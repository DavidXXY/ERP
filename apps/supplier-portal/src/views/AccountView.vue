<template>
  <div class="page-shell">
    <header class="page-heading">
      <div><p class="eyebrow">账号安全</p><h1>账号设置</h1><p>修改门户登录密码、开启双重验证，保护账号安全。</p></div>
    </header>
    <a-card class="section-block" :bordered="false">
      <a-alert v-if="store.session?.account.mustChangePassword" type="warning" show-icon message="请先修改临时密码" description="管理员重置了您的密码，完成修改后才能继续报价。" style="margin-bottom: 20px" />
      <a-form layout="vertical" :model="form" @finish="submit">
        <a-form-item label="当前密码" name="currentPassword" required><a-input-password v-model:value="form.currentPassword" autocomplete="current-password" /></a-form-item>
        <a-form-item label="新密码" name="newPassword" required><a-input-password v-model:value="form.newPassword" autocomplete="new-password" /></a-form-item>
        <a-form-item label="确认新密码" name="confirmPassword" required><a-input-password v-model:value="form.confirmPassword" autocomplete="new-password" /></a-form-item>
        <a-button type="primary" html-type="submit" :loading="saving">更新密码</a-button>
      </a-form>
    </a-card>

    <a-card class="section-block" :bordered="false">
      <div class="section-title">
        <div>
          <h2>双重验证（MFA）</h2>
          <p>登录时除密码外需输入动态验证码，显著提升账号安全性。</p>
        </div>
        <a-tag :color="mfaStatus.enabled ? 'green' : 'default'">{{
          mfaStatus.enabled ? "已开启" : "未开启"
        }}</a-tag>
      </div>
      <p v-if="mfaStatus.enabled" class="table-subtitle">
        已绑定验证器，另有 {{ mfaStatus.recoveryCodeCount }} 个备用恢复码。
      </p>
      <a-button
        v-if="!mfaStatus.enabled"
        type="primary"
        ghost
        @click="mfaOpen = true"
        >开启双重验证</a-button
      >
      <a-button
        v-else
        type="link"
        danger
        @click="disableOpen = true"
        >关闭双重验证</a-button
      >
    </a-card>

    <a-card class="section-block" :bordered="false">
      <div class="section-title">
        <div>
          <h2>最近操作记录</h2>
          <p>登录、密码与安全设置等关键操作的审计留痕。</p>
        </div>
      </div>
      <a-table
        size="small"
        row-key="id"
        :data-source="activities"
        :columns="activityColumns"
        :pagination="{ pageSize: 8 }"
        :loading="activityLoading"
      />
    </a-card>
  </div>

  <a-modal
    v-model:open="mfaOpen"
    title="开启双重验证"
    :footer="null"
    width="min(480px, 100vw)"
  >
    <template v-if="mfaStep === 'password'">
      <a-form layout="vertical">
        <a-form-item label="当前密码" required>
          <a-input-password v-model:value="mfaPassword" autocomplete="current-password" />
        </a-form-item>
        <a-button type="primary" block :loading="mfaLoading" @click="startSetup"
          >下一步</a-button
        >
      </a-form>
    </template>
    <template v-else-if="mfaStep === 'scan'">
      <a-alert
        type="info"
        show-icon
        message="使用身份验证器 App（如 Google Authenticator、Microsoft Authenticator）扫描或手动添加"
        style="margin-bottom: 14px"
      />
      <div class="mfa-qr">
        <img v-if="mfaQrDataUrl" :src="mfaQrDataUrl" alt="双重验证二维码" />
        <a-skeleton v-else active :paragraph="{ rows: 3 }" />
      </div>
      <a-form layout="vertical">
        <a-form-item label="密钥（Secret）">
          <a-input :value="mfaSetup?.secret" read-only />
        </a-form-item>
        <a-form-item label="动态验证码" required>
          <a-input v-model:value="mfaCode" :maxlength="6" placeholder="输入验证器中的 6 位动态码" />
        </a-form-item>
        <a-button type="primary" block :loading="mfaLoading" @click="confirmEnable"
          >验证并开启</a-button
        >
      </a-form>
    </template>
    <template v-else>
      <a-alert
        type="warning"
        show-icon
        message="请妥善保存以下恢复码，用于无法使用验证器时登录。每个恢复码仅可使用一次。"
        style="margin-bottom: 14px"
      />
      <a-list size="small" bordered :data-source="mfaRecoveryCodes">
        <template #renderItem="{ item }">
          <a-list-item><code>{{ item }}</code></a-list-item>
        </template>
      </a-list>
      <a-button type="primary" block style="margin-top: 14px" @click="closeMfa"
        >我已保存恢复码</a-button
      >
    </template>
  </a-modal>

  <a-modal
    v-model:open="disableOpen"
    title="关闭双重验证"
    ok-text="确认关闭"
    :confirm-loading="mfaLoading"
    @ok="confirmDisable"
  >
    <a-form layout="vertical">
      <a-form-item label="当前密码" required>
        <a-input-password v-model:value="disablePassword" autocomplete="current-password" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import QRCode from "qrcode";
import * as api from "../api";
import { usePortalStore } from "../store";

const store = usePortalStore();
const saving = ref(false);
const form = reactive({ currentPassword: "", newPassword: "", confirmPassword: "" });
const mfaStatus = ref<api.MfaStatus>({ enabled: false, recoveryCodeCount: 0 });
const mfaOpen = ref(false);
const mfaStep = ref<"password" | "scan" | "codes">("password");
const mfaLoading = ref(false);
const mfaPassword = ref("");
const mfaCode = ref("");
const mfaSetup = ref<api.MfaSetup>();
const mfaQrDataUrl = ref("");
const mfaRecoveryCodes = ref<string[]>([]);
const disableOpen = ref(false);
const disablePassword = ref("");
const activityLoading = ref(false);
const activities = ref<api.AccountActivity[]>([]);
const activityColumns = [
  { title: "时间", dataIndex: "createdAt", customRender: ({ text }: { text: string }) => new Date(text).toLocaleString("zh-CN", { hour12: false }) },
  { title: "操作", dataIndex: "action", customRender: ({ text }: { text: string }) => actionText(text) },
  { title: "详情", dataIndex: "detail" },
  { title: "IP", dataIndex: "ip" },
];
function actionText(action: string) {
  return (
    {
      LOGIN: "门户登录",
      CHANGE_PASSWORD: "修改密码",
      RESET_REQUEST: "申请重置密码",
      RESET_PASSWORD: "重置密码",
      MFA_ENABLE: "开启双重验证",
      MFA_DISABLE: "关闭双重验证",
      QUOTE_SUBMIT: "提交报价",
      SHIPMENT_CREATE: "回传发货",
      CONTRACT_ACK: "确认中标合同",
    } as Record<string, string>
  )[action] || action;
}

async function submit() {
  if (form.newPassword.length < 8) return message.warning("新密码至少 8 位");
  if (form.newPassword !== form.confirmPassword) return message.warning("两次输入的新密码不一致");
  saving.value = true;
  try {
    store.setSession(await api.changePassword({ currentPassword: form.currentPassword, newPassword: form.newPassword }));
    Object.assign(form, { currentPassword: "", newPassword: "", confirmPassword: "" });
    message.success("密码已更新");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "密码更新失败");
  } finally {
    saving.value = false;
  }
}

onMounted(() => { void loadMfa(); void loadActivities(); });
async function loadMfa() {
  try {
    mfaStatus.value = await api.getMfaStatus();
  } catch {
    /* 忽略 */
  }
}
async function loadActivities() {
  activityLoading.value = true;
  try {
    activities.value = await api.listAccountActivities();
  } catch {
    /* 忽略 */
  } finally {
    activityLoading.value = false;
  }
}

async function startSetup() {
  if (!mfaPassword.value) return message.warning("请输入当前密码");
  mfaLoading.value = true;
  try {
    mfaSetup.value = await api.beginMfaSetup(mfaPassword.value);
    mfaQrDataUrl.value = await QRCode.toDataURL(mfaSetup.value.provisioningUri, {
      width: 220,
      margin: 1,
    });
    mfaStep.value = "scan";
  } catch (error) {
    message.error(error instanceof Error ? error.message : "操作失败");
  } finally {
    mfaLoading.value = false;
  }
}

async function confirmEnable() {
  if (!mfaCode.value.trim()) return message.warning("请输入动态验证码");
  mfaLoading.value = true;
  try {
    mfaRecoveryCodes.value = await api.enableMfa(mfaCode.value.trim());
    mfaStep.value = "codes";
    mfaStatus.value = { enabled: true, recoveryCodeCount: mfaRecoveryCodes.value.length };
  } catch (error) {
    message.error(error instanceof Error ? error.message : "操作失败");
  } finally {
    mfaLoading.value = false;
  }
}

function closeMfa() {
  mfaOpen.value = false;
  mfaStep.value = "password";
  mfaPassword.value = "";
  mfaCode.value = "";
  mfaSetup.value = undefined;
  mfaQrDataUrl.value = "";
  mfaRecoveryCodes.value = [];
  void loadMfa();
}

async function confirmDisable() {
  if (!disablePassword.value) return message.warning("请输入当前密码");
  mfaLoading.value = true;
  try {
    await api.disableMfa(disablePassword.value);
    disableOpen.value = false;
    disablePassword.value = "";
    mfaStatus.value = { enabled: false, recoveryCodeCount: 0 };
    message.success("双重验证已关闭");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "操作失败");
  } finally {
    mfaLoading.value = false;
  }
}
</script>
<style scoped>
.mfa-qr {
  display: flex;
  justify-content: center;
  margin-bottom: 14px;
}
.mfa-qr img {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 8px;
  background: #fff;
}
</style>
