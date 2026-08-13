<template>
  <div class="page-shell narrow">
    <header class="page-heading">
      <div>
        <p class="eyebrow">企业档案</p>
        <h1>企业资料</h1>
        <p>采购方将依据这些信息进行供应商准入与合作评估。</p>
      </div>
      <a-tag :color="statusColor">{{ statusText }}</a-tag>
    </header>
    <a-alert
      v-if="profile.admissionReviewComment"
      :type="profile.admissionStatus === 'REJECTED' ? 'error' : 'info'"
      show-icon
      :message="profile.admissionReviewComment"
    />
    <section class="form-section admission-progress">
      <div class="section-title">
        <div>
          <h2>准入进度</h2>
          <p>完成资料与资质上传后，由采购方审核并准入。</p>
        </div>
      </div>
      <a-steps :current="admissionStep" size="small" responsive>
        <a-step title="提交注册" :status="admissionStep > 0 ? 'finish' : 'process'" />
        <a-step title="完善资料" :status="profile.admissionStatus === 'REJECTED' ? 'error' : admissionStep > 1 ? 'finish' : 'wait'" />
        <a-step title="采购方审核" :status="admissionStep > 2 ? 'finish' : profile.admissionStatus === 'PENDING' && admissionStep >= 2 ? 'process' : 'wait'" />
        <a-step
          title="准入通过"
          :status="profile.admissionStatus === 'APPROVED' ? 'finish' : profile.admissionStatus === 'REJECTED' ? 'error' : 'wait'"
        />
      </a-steps>
    </section>
    <a-spin :spinning="loading">
      <a-form
        :model="profile"
        layout="vertical"
        class="profile-form"
        @finish="save"
      >
        <section class="form-section">
          <div class="section-title">
            <div>
              <h2>工商信息</h2>
              <p>已准入后，企业名称与信用代码变更需联系采购方审批。</p>
            </div>
          </div>
          <div class="form-grid-2">
            <a-form-item label="企业名称" required
              ><a-input v-model:value="profile.name"
            /></a-form-item>
            <a-form-item label="统一社会信用代码" required
              ><a-input v-model:value="profile.unifiedSocialCreditCode"
            /></a-form-item>
            <a-form-item label="供应商分类">
              <a-input :value="profile.category || '由采购方待分配'" disabled />
            </a-form-item>
            <a-form-item label="法定代表人"
              ><a-input v-model:value="profile.legalRepresentative"
            /></a-form-item>
            <a-form-item label="注册资本"
              ><a-input v-model:value="profile.registeredCapital"
            /></a-form-item>
            <a-form-item label="纳税人类型"
              ><a-input v-model:value="profile.taxpayerType"
            /></a-form-item>
          </div>
          <a-form-item label="注册地址"
            ><a-input v-model:value="profile.registeredAddress"
          /></a-form-item>
          <a-form-item label="经营范围"
            ><a-textarea v-model:value="profile.businessScope" :rows="3"
          /></a-form-item>
        </section>
        <section class="form-section">
          <div class="section-title">
            <div>
              <h2>联系人与结算</h2>
              <p>银行信息不在门户中明文回显。</p>
            </div>
          </div>
          <div class="form-grid-2">
            <a-form-item label="联系人" required
              ><a-input v-model:value="profile.contactName"
            /></a-form-item>
            <a-form-item label="联系电话" required
              ><a-input v-model:value="profile.phone"
            /></a-form-item>
            <a-form-item label="开户银行"
              ><a-input
                v-model:value="profile.bankName"
                :placeholder="
                  profile.admissionStatus === 'APPROVED'
                    ? '已准入后变更请联系采购方'
                    : ''
                "
            /></a-form-item>
            <a-form-item label="银行账号"
              ><a-input
                v-model:value="bankAccount"
                :placeholder="profile.maskedBankAccount || '请输入银行账号'"
            /></a-form-item>
          </div>
          <a-form-item label="结算条款"
            ><a-input v-model:value="profile.settlementTerms"
          /></a-form-item>
        </section>
        <section class="form-section">
          <div class="section-title">
            <div>
              <h2>有效期</h2>
              <p>到期前请更新对应资质文件。</p>
            </div>
          </div>
          <a-alert
            v-for="warning in expiryWarnings"
            :key="warning"
            type="warning"
            show-icon
            :message="warning"
            style="margin-bottom: 12px"
          />
          <div class="form-grid-2">
            <a-form-item label="营业执照有效期"
              ><a-input v-model:value="profile.licenseValidTo" type="date"
            /></a-form-item>
            <a-form-item label="资质有效期"
              ><a-input
                v-model:value="profile.qualificationValidTo"
                type="date"
            /></a-form-item>
          </div>
        </section>
        <div class="sticky-actions">
          <a-button type="primary" html-type="submit" :loading="saving"
            ><SaveOutlined /> 保存资料</a-button
          >
        </div>
      </a-form>
    </a-spin>
    <a-card class="section-block" :bordered="false">
      <div class="section-title">
        <div>
          <h2>信息变更申请</h2>
          <p>
            已准入后，企业名称、信用代码、银行信息与结算条款变更需提交申请，由采购方审批后生效。
          </p>
        </div>
      </div>
      <a-alert
        type="info"
        show-icon
        message="提交后进入采购方审批，处理结果会显示在下方列表中。"
        style="margin-bottom: 16px"
      />
      <a-form layout="vertical" :model="changeForm" @finish="submitChange">
        <div class="form-grid-2">
          <a-form-item label="变更类型" required>
            <a-select v-model:value="changeForm.changeType" :options="changeTypeOptions" />
          </a-form-item>
          <a-form-item v-if="changeForm.changeType === 'NAME'" label="新的企业名称" required>
            <a-input v-model:value="changeForm.proposedName" :maxlength="160" />
          </a-form-item>
          <a-form-item
            v-else-if="changeForm.changeType === 'CREDIT_CODE'"
            label="新的统一社会信用代码"
            required
          >
            <a-input v-model:value="changeForm.proposedCreditCode" :maxlength="80" />
          </a-form-item>
          <template v-else-if="changeForm.changeType === 'BANK_INFO'">
            <a-form-item label="新的开户银行">
              <a-input v-model:value="changeForm.proposedBankName" :maxlength="120" />
            </a-form-item>
            <a-form-item label="新的银行账号">
              <a-input v-model:value="changeForm.proposedBankAccount" :maxlength="120" />
            </a-form-item>
          </template>
          <a-form-item
            v-else-if="changeForm.changeType === 'SETTLEMENT_TERMS'"
            label="新的结算条款"
            required
          >
            <a-input v-model:value="changeForm.proposedSettlementTerms" :maxlength="160" />
          </a-form-item>
        </div>
        <a-form-item label="变更原因" required>
          <a-textarea
            v-model:value="changeForm.reason"
            :rows="3"
            :maxlength="1000"
            placeholder="请说明变更原因，便于采购方审核"
          />
        </a-form-item>
        <a-button type="primary" html-type="submit" :loading="changeSaving">
          <SendOutlined /> 提交变更申请
        </a-button>
      </a-form>
      <a-divider />
      <h3 style="margin-bottom: 12px">申请记录</h3>
      <a-skeleton v-if="changeLoading" active />
      <a-empty
        v-else-if="changeRequests.length === 0"
        :image="Empty.PRESENTED_IMAGE_SIMPLE"
        description="暂无变更申请"
      />
      <a-list
        v-else
        size="small"
        :data-source="changeRequests"
        :pagination="{ pageSize: 5, showSizeChanger: false }"
      >
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <template #title>
                {{ changeTypeLabel(item.changeType) }}
                <a-tag :color="changeStatusColor(item.status)">{{
                  changeStatusText(item.status)
                }}</a-tag>
              </template>
              <template #description>
                <div>{{ changeSummary(item) }}</div>
                <div class="table-subtitle">
                  提交于 {{ formatDate(item.createdAt) }} · {{ item.requestedByName }}
                  <template v-if="item.reviewComment">
                    · 审批意见：{{ item.reviewComment }}
                  </template>
                </div>
              </template>
            </a-list-item-meta>
          </a-list-item>
        </template>
      </a-list>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import { Empty } from "ant-design-vue";
import { SaveOutlined, SendOutlined } from "@ant-design/icons-vue";
import * as api from "../api";
import { usePortalStore } from "../store";
import { expiryMessage, formatDate } from "../utils/quote";

const store = usePortalStore();
const loading = ref(false);
const saving = ref(false);
const bankAccount = ref("");
const changeRequests = ref<api.PortalChangeRequest[]>([]);
const changeLoading = ref(false);
const changeSaving = ref(false);
const changeForm = reactive({
  changeType: "NAME",
  proposedName: "",
  proposedCreditCode: "",
  proposedBankName: "",
  proposedBankAccount: "",
  proposedSettlementTerms: "",
  reason: "",
});
const changeTypeOptions = [
  { label: "企业名称", value: "NAME" },
  { label: "统一社会信用代码", value: "CREDIT_CODE" },
  { label: "银行信息", value: "BANK_INFO" },
  { label: "结算条款", value: "SETTLEMENT_TERMS" },
];
const profile = reactive<api.SupplierProfile>({
  id: "",
  name: "",
  unifiedSocialCreditCode: "",
  admissionStatus: "PENDING",
});
const statusText = computed(
  () =>
    ({ APPROVED: "已准入", REJECTED: "已退回", PENDING: "审核中" })[
      profile.admissionStatus
    ],
);
const statusColor = computed(
  () =>
    ({ APPROVED: "green", REJECTED: "red", PENDING: "orange" })[
      profile.admissionStatus
    ],
);
const admissionStep = computed(() => {
  const status = profile.admissionStatus;
  if (status === "APPROVED") return 3;
  if (status === "REJECTED") return 1;
  const complete =
    Boolean(profile.name) &&
    Boolean(profile.unifiedSocialCreditCode) &&
    Boolean(profile.registeredAddress);
  return complete ? 2 : 1;
});
const expiryWarnings = computed(() => {
  return [
    expiryMessage("营业执照", profile.licenseValidTo),
    expiryMessage("资质", profile.qualificationValidTo),
  ].filter((text): text is string => Boolean(text));
});
onMounted(load);
async function load() {
  loading.value = true;
  try {
    const [session, changes] = await Promise.all([
      store.session ? Promise.resolve(store.session) : api.getSession(),
      api.listChangeRequests(),
    ]);
    store.setSession(session);
    Object.assign(profile, session.supplier);
    changeRequests.value = changes;
  } catch (e) {
    message.error(e instanceof Error ? e.message : "加载失败");
  } finally {
    loading.value = false;
  }
}
async function loadChanges() {
  changeLoading.value = true;
  try {
    changeRequests.value = await api.listChangeRequests();
  } catch (e) {
    message.error(e instanceof Error ? e.message : "加载申请记录失败");
  } finally {
    changeLoading.value = false;
  }
}
async function submitChange() {
  if (!changeForm.reason.trim()) {
    message.warning("请填写变更原因");
    return;
  }
  if (
    changeForm.changeType === "NAME" &&
    !changeForm.proposedName.trim()
  ) {
    message.warning("请填写新的企业名称");
    return;
  }
  if (
    changeForm.changeType === "CREDIT_CODE" &&
    !changeForm.proposedCreditCode.trim()
  ) {
    message.warning("请填写新的统一社会信用代码");
    return;
  }
  if (
    changeForm.changeType === "BANK_INFO" &&
    !changeForm.proposedBankName.trim() &&
    !changeForm.proposedBankAccount.trim()
  ) {
    message.warning("请填写开户银行或银行账号");
    return;
  }
  if (
    changeForm.changeType === "SETTLEMENT_TERMS" &&
    !changeForm.proposedSettlementTerms.trim()
  ) {
    message.warning("请填写新的结算条款");
    return;
  }
  changeSaving.value = true;
  try {
    await api.createChangeRequest({
      ...changeForm,
      proposedName: changeForm.proposedName.trim() || undefined,
      proposedCreditCode: changeForm.proposedCreditCode.trim() || undefined,
      proposedBankName: changeForm.proposedBankName.trim() || undefined,
      proposedBankAccount: changeForm.proposedBankAccount.trim() || undefined,
      proposedSettlementTerms:
        changeForm.proposedSettlementTerms.trim() || undefined,
      reason: changeForm.reason.trim(),
    });
    Object.assign(changeForm, {
      proposedName: "",
      proposedCreditCode: "",
      proposedBankName: "",
      proposedBankAccount: "",
      proposedSettlementTerms: "",
      reason: "",
    });
    await loadChanges();
    message.success("变更申请已提交，等待采购方审批");
  } catch (e) {
    message.error(e instanceof Error ? e.message : "提交失败");
  } finally {
    changeSaving.value = false;
  }
}
const changeTypeLabel = (v: string) =>
  ({ NAME: "企业名称", CREDIT_CODE: "统一社会信用代码", BANK_INFO: "银行信息", SETTLEMENT_TERMS: "结算条款" })[v] || v;
const changeStatusText = (v: string) =>
  ({ PENDING: "待审批", APPROVED: "已通过", REJECTED: "已退回" })[v] || v;
const changeStatusColor = (v: string) =>
  ({ PENDING: "orange", APPROVED: "green", REJECTED: "red" })[v] || "default";
const changeSummary = (item: api.PortalChangeRequest) => {
  const value =
    item.proposedName ||
    item.proposedCreditCode ||
    item.proposedBankName ||
    item.proposedBankAccount ||
    item.proposedSettlementTerms ||
    "-";
  return `${changeTypeLabel(item.changeType)}：${value} · ${item.reason}`;
};
async function save() {
  if (
    !profile.name.trim() ||
    !profile.unifiedSocialCreditCode.trim() ||
    !profile.contactName?.trim() ||
    !profile.phone?.trim()
  ) {
    message.warning("请填写企业名称、统一社会信用代码、联系人和联系电话");
    return;
  }
  saving.value = true;
  try {
    const updated = await api.updateProfile({
      ...profile,
      bankAccount: bankAccount.value || undefined,
      maskedBankAccount: undefined,
    });
    Object.assign(profile, updated);
    bankAccount.value = "";
    if (store.session) store.session.supplier = updated;
    message.success("企业资料已保存");
  } catch (e) {
    message.error(e instanceof Error ? e.message : "保存失败");
  } finally {
    saving.value = false;
  }
}
</script>
