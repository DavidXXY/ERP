<template>
  <div class="page-shell">
    <header class="page-heading">
      <div><p class="eyebrow">账号安全</p><h1>账号设置</h1><p>修改门户登录密码，临时密码首次登录后必须更新。</p></div>
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
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { message } from "ant-design-vue";
import * as api from "../api";
import { usePortalStore } from "../store";

const store = usePortalStore();
const saving = ref(false);
const form = reactive({ currentPassword: "", newPassword: "", confirmPassword: "" });
async function submit() {
  if (form.newPassword.length < 8) return message.warning("新密码至少 8 位");
  if (form.newPassword !== form.confirmPassword) return message.warning("两次输入的新密码不一致");
  saving.value = true;
  try { store.setSession(await api.changePassword({ currentPassword: form.currentPassword, newPassword: form.newPassword })); Object.assign(form, { currentPassword: "", newPassword: "", confirmPassword: "" }); message.success("密码已更新"); }
  catch (error) { message.error(error instanceof Error ? error.message : "密码更新失败"); }
  finally { saving.value = false; }
}
</script>
