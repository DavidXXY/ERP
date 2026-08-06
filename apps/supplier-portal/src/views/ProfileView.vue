<template>
  <div class="page-shell narrow">
    <header class="page-heading"><div><p class="eyebrow">企业档案</p><h1>企业资料</h1><p>采购方将依据这些信息进行供应商准入与合作评估。</p></div><a-tag :color="statusColor">{{ statusText }}</a-tag></header>
    <a-alert v-if="profile.admissionReviewComment" :type="profile.admissionStatus === 'REJECTED' ? 'error' : 'info'" show-icon :message="profile.admissionReviewComment" />
    <a-spin :spinning="loading">
      <a-form :model="profile" layout="vertical" class="profile-form" @finish="save">
        <section class="form-section">
          <div class="section-title"><div><h2>工商信息</h2><p>已准入后，企业名称与信用代码变更需联系采购方审批。</p></div></div>
          <div class="form-grid-2">
            <a-form-item label="企业名称" required><a-input v-model:value="profile.name" /></a-form-item>
            <a-form-item label="统一社会信用代码" required><a-input v-model:value="profile.unifiedSocialCreditCode" /></a-form-item>
            <a-form-item label="供应商类别"><a-input v-model:value="profile.category" /></a-form-item>
            <a-form-item label="法定代表人"><a-input v-model:value="profile.legalRepresentative" /></a-form-item>
            <a-form-item label="注册资本"><a-input v-model:value="profile.registeredCapital" /></a-form-item>
            <a-form-item label="纳税人类型"><a-input v-model:value="profile.taxpayerType" /></a-form-item>
          </div>
          <a-form-item label="注册地址"><a-input v-model:value="profile.registeredAddress" /></a-form-item>
          <a-form-item label="经营范围"><a-textarea v-model:value="profile.businessScope" :rows="3" /></a-form-item>
        </section>
        <section class="form-section">
          <div class="section-title"><div><h2>联系人与结算</h2><p>银行信息不在门户中明文回显。</p></div></div>
          <div class="form-grid-2">
            <a-form-item label="联系人"><a-input v-model:value="profile.contactName" /></a-form-item>
            <a-form-item label="联系电话"><a-input v-model:value="profile.phone" /></a-form-item>
            <a-form-item label="开户银行"><a-input v-model:value="profile.bankName" :placeholder="profile.admissionStatus === 'APPROVED' ? '已准入后变更请联系采购方' : ''" /></a-form-item>
            <a-form-item label="银行账号"><a-input v-model:value="bankAccount" :placeholder="profile.maskedBankAccount || '请输入银行账号'" /></a-form-item>
          </div>
          <a-form-item label="结算条款"><a-input v-model:value="profile.settlementTerms" /></a-form-item>
        </section>
        <section class="form-section">
          <div class="section-title"><div><h2>有效期</h2><p>到期前请更新对应资质文件。</p></div></div>
          <div class="form-grid-2">
            <a-form-item label="营业执照有效期"><a-input v-model:value="profile.licenseValidTo" type="date" /></a-form-item>
            <a-form-item label="资质有效期"><a-input v-model:value="profile.qualificationValidTo" type="date" /></a-form-item>
          </div>
        </section>
        <div class="sticky-actions"><a-button type="primary" html-type="submit" :loading="saving"><SaveOutlined /> 保存资料</a-button></div>
      </a-form>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import { SaveOutlined } from "@ant-design/icons-vue";
import * as api from "../api";
import { usePortalStore } from "../store";

const store = usePortalStore(); const loading = ref(false); const saving = ref(false); const bankAccount = ref("");
const profile = reactive<api.SupplierProfile>({ id: "", name: "", unifiedSocialCreditCode: "", admissionStatus: "PENDING" });
const statusText = computed(() => ({ APPROVED: "已准入", REJECTED: "已退回", PENDING: "审核中" })[profile.admissionStatus]);
const statusColor = computed(() => ({ APPROVED: "green", REJECTED: "red", PENDING: "orange" })[profile.admissionStatus]);
onMounted(load);
async function load() { loading.value = true; try { const session = store.session || await api.getSession(); store.setSession(session); Object.assign(profile, session.supplier); } catch (e) { message.error(e instanceof Error ? e.message : "加载失败"); } finally { loading.value = false; } }
async function save() {
  if (!profile.name.trim() || !profile.unifiedSocialCreditCode.trim()) { message.warning("请填写企业名称与统一社会信用代码"); return; }
  saving.value = true;
  try {
    const updated = await api.updateProfile({ ...profile, bankAccount: bankAccount.value || undefined, maskedBankAccount: undefined });
    Object.assign(profile, updated); bankAccount.value = "";
    if (store.session) store.session.supplier = updated;
    message.success("企业资料已保存");
  } catch (e) { message.error(e instanceof Error ? e.message : "保存失败"); } finally { saving.value = false; }
}
</script>
