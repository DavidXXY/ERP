<template>
  <div class="page-stack personal-settings-page">
    <header class="personal-heading">
      <div>
        <h2>个人设置</h2>
        <p>{{ overview?.account.username || auth.user?.username }}</p>
      </div>
      <a-button @click="loadOverview"><template #icon><ReloadOutlined /></template>刷新</a-button>
    </header>

    <a-spin :spinning="loading">
      <template v-if="overview">
        <section class="personal-summary">
          <div class="personal-identity">
            <span class="personal-avatar">{{ initial }}</span>
            <div>
              <strong>{{ overview.account.displayName }}</strong>
              <span>{{ overview.employee?.workNo || overview.account.username }}</span>
            </div>
          </div>
          <div><span>所属组织</span><strong>{{ overview.employee?.organizationName || overview.account.organizationName || "未分配" }}</strong></div>
          <div><span>我的证书</span><strong>{{ overview.certificates.length }}</strong></div>
          <div><span>劳动合同</span><strong>{{ overview.contracts.length }}</strong></div>
        </section>

        <a-alert
          v-if="!overview.employee"
          type="warning"
          show-icon
          message="当前账号尚未关联员工档案"
          description="账号资料和密码仍可维护；员工档案关联后即可使用证书与合同功能。"
        />

        <a-tabs v-model:active-key="activeTab" class="personal-tabs">
          <a-tab-pane key="profile" tab="个人资料">
            <section class="settings-surface profile-layout">
              <div class="settings-block">
                <div class="section-heading"><strong>账号资料</strong></div>
                <a-form ref="profileFormRef" :model="profileForm" layout="vertical" class="compact-form">
                  <a-form-item label="登录账号"><a-input :value="overview.account.username" disabled /></a-form-item>
                  <a-form-item label="显示姓名" name="displayName" :rules="requiredRules"><a-input v-model:value="profileForm.displayName" /></a-form-item>
                  <a-form-item label="手机号"><a-input v-model:value="profileForm.phone" /></a-form-item>
                  <a-form-item label="邮箱" name="email" :rules="emailRules"><a-input v-model:value="profileForm.email" /></a-form-item>
                  <a-button type="primary" :loading="saving" @click="saveProfile">保存资料</a-button>
                </a-form>
              </div>

              <div class="settings-block employee-profile-block">
                <div class="section-heading"><strong>员工档案</strong></div>
                <a-descriptions v-if="overview.employee" bordered :column="1" size="small">
                  <a-descriptions-item label="姓名">{{ overview.employee.name }}</a-descriptions-item>
                  <a-descriptions-item label="工号">{{ overview.employee.workNo || "-" }}</a-descriptions-item>
                  <a-descriptions-item label="组织">{{ overview.employee.organizationPath || "未分配" }}</a-descriptions-item>
                  <a-descriptions-item label="岗位">{{ overview.employee.position || "-" }}</a-descriptions-item>
                  <a-descriptions-item label="入职日期">{{ overview.employee.entryDate || "-" }}</a-descriptions-item>
                  <a-descriptions-item label="人员状态">{{ employmentLabel(overview.employee.employmentStatus) }}</a-descriptions-item>
                </a-descriptions>
                <a-empty v-else description="未关联员工档案" />
              </div>
            </section>
          </a-tab-pane>

          <a-tab-pane key="certificates" tab="我的证书">
            <section class="settings-surface">
              <div class="section-heading">
                <div><strong>证书档案</strong><span>{{ overview.certificates.length }} 项</span></div>
                <a-button v-if="overview.employee" type="primary" @click="openCertificate()">
                  <template #icon><PlusOutlined /></template>新增证书
                </a-button>
              </div>

              <template v-if="overview.employee">
                <div class="certificate-desktop-table">
                  <a-table :columns="certificateColumns" :data-source="overview.certificates" row-key="id" :pagination="false" :scroll="{ x: 940 }">
                    <template #bodyCell="{ column, record }">
                      <template v-if="column.key === 'certificate'"><strong>{{ record.name }}</strong><span class="table-subtitle">{{ record.certificateNo || '未登记编号' }}</span></template>
                      <template v-else-if="column.key === 'status'"><a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag></template>
                      <template v-else-if="column.key === 'attachments'"><a-button v-if="record.attachments.length" type="link" size="small" @click="preview(record.attachments)">{{ record.attachments.length }}份</a-button><span v-else>-</span></template>
                      <template v-else-if="column.key === 'actions'">
                        <a-space :size="4">
                          <a-button type="text" size="small" :disabled="record.locked" title="编辑证书" @click="openCertificate(record)"><template #icon><EditOutlined /></template></a-button>
                          <a-popconfirm title="确认删除该证书？" @confirm="removeCertificate(record)"><a-button danger type="text" size="small" :disabled="record.locked" title="删除证书"><template #icon><DeleteOutlined /></template></a-button></a-popconfirm>
                        </a-space>
                      </template>
                    </template>
                  </a-table>
                </div>
                <div class="certificate-mobile-list">
                  <article v-for="record in overview.certificates" :key="record.id" class="record-item">
                    <div class="record-heading"><strong>{{ record.name }}</strong><a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag></div>
                    <span>{{ record.certificateNo || "未登记编号" }}</span>
                    <div class="record-meta"><span>{{ record.specialty || "未设置专业" }}</span><span>有效期 {{ record.validTo || "-" }}</span></div>
                    <div class="record-actions"><a-button v-if="record.attachments.length" size="small" @click="preview(record.attachments)">附件 {{ record.attachments.length }}</a-button><a-button size="small" :disabled="record.locked" @click="openCertificate(record)">编辑</a-button></div>
                  </article>
                </div>
                <a-empty v-if="!overview.certificates.length" description="暂无证书" />
              </template>
              <a-empty v-else description="关联员工档案后可维护个人证书" />
            </section>
          </a-tab-pane>

          <a-tab-pane key="contracts" tab="劳动合同">
            <section class="settings-surface">
              <div class="section-heading"><div><strong>劳动合同</strong><span>{{ overview.contracts.length }} 份</span></div></div>
              <a-table v-if="overview.contracts.length" :columns="contractColumns" :data-source="overview.contracts" row-key="id" :pagination="false" :scroll="{ x: 800 }">
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'contract'"><strong>{{ record.contractNo }}</strong><span class="table-subtitle">{{ contractTypeLabel(record.contractType) }}</span></template>
                  <template v-else-if="column.key === 'period'">{{ record.startDate }} 至 {{ record.endDate || '长期' }}</template>
                  <template v-else-if="column.key === 'status'"><a-tag :color="contractStatusColor(record.status)">{{ contractStatusLabel(record.status) }}</a-tag></template>
                  <template v-else-if="column.key === 'attachments'"><a-button v-if="record.attachments.length" type="link" size="small" @click="preview(record.attachments)">查看 {{ record.attachments.length }} 份</a-button><span v-else>-</span></template>
                </template>
              </a-table>
              <a-empty v-else description="暂无劳动合同" />
            </section>
          </a-tab-pane>

          <a-tab-pane key="security" tab="账号安全">
            <section class="settings-surface security-surface">
              <div class="section-heading"><strong>修改密码</strong></div>
              <a-form ref="passwordFormRef" :model="passwordForm" layout="vertical" class="compact-form">
                <a-form-item label="当前密码" name="currentPassword" :rules="requiredRules"><a-input-password v-model:value="passwordForm.currentPassword" autocomplete="current-password" /></a-form-item>
                <a-form-item label="新密码" name="newPassword" :rules="passwordRules"><a-input-password v-model:value="passwordForm.newPassword" autocomplete="new-password" /></a-form-item>
                <a-form-item label="确认新密码" name="confirmPassword" :rules="requiredRules"><a-input-password v-model:value="passwordForm.confirmPassword" autocomplete="new-password" /></a-form-item>
                <a-button type="primary" :loading="saving" @click="changePassword">修改密码</a-button>
              </a-form>
            </section>
          </a-tab-pane>
        </a-tabs>
      </template>
    </a-spin>

    <a-modal v-model:open="certificateOpen" :title="certificateEditingId ? '编辑证书' : '新增证书'" width="760px" :confirm-loading="saving" @ok="saveCertificate">
      <a-form ref="certificateFormRef" :model="certificateForm" layout="vertical">
        <a-row :gutter="14"><a-col :xs="24" :md="12"><a-form-item label="证书名称" name="name" :rules="requiredRules"><a-input v-model:value="certificateForm.name" /></a-form-item></a-col><a-col :xs="24" :md="12"><a-form-item label="证书编号"><a-input v-model:value="certificateForm.certificateNo" /></a-form-item></a-col></a-row>
        <a-row :gutter="14"><a-col :xs="24" :md="12"><a-form-item label="证书类型"><a-input v-model:value="certificateForm.type" /></a-form-item></a-col><a-col :xs="24" :md="12"><a-form-item label="专业"><a-input v-model:value="certificateForm.specialty" /></a-form-item></a-col></a-row>
        <a-row :gutter="14"><a-col :xs="24" :md="8"><a-form-item label="发证日期"><a-input v-model:value="certificateForm.issueDate" type="date" /></a-form-item></a-col><a-col :xs="24" :md="8"><a-form-item label="有效期至"><a-input v-model:value="certificateForm.validTo" type="date" /></a-form-item></a-col><a-col :xs="24" :md="8"><a-form-item label="复审日期"><a-input v-model:value="certificateForm.reviewDate" type="date" /></a-form-item></a-col></a-row>
        <a-form-item label="证书附件"><a-space wrap><a-upload :show-upload-list="false" :before-upload="uploadCertificateFile"><a-button :loading="uploading"><template #icon><UploadOutlined /></template>上传图片或 PDF</a-button></a-upload><a-tag v-for="(item, index) in certificateForm.attachments" :key="item.dataUrl" closable @close="certificateForm.attachments.splice(index, 1)">{{ item.name }}</a-tag></a-space></a-form-item>
        <a-form-item label="备注"><a-textarea v-model:value="certificateForm.remark" :rows="3" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="previewOpen" title="附件" width="900px" :footer="null">
      <div class="attachment-grid">
        <figure v-for="item in previewAttachments" :key="item.dataUrl">
          <a v-if="item.type === 'application/pdf' || item.dataUrl.toLowerCase().endsWith('.pdf')" :href="item.dataUrl" target="_blank" rel="noopener">打开 PDF</a>
          <img v-else :src="item.dataUrl" :alt="item.name" />
          <figcaption>{{ item.name }}</figcaption>
        </figure>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message, type FormInstance } from "ant-design-vue";
import DeleteOutlined from "@ant-design/icons-vue/DeleteOutlined";
import EditOutlined from "@ant-design/icons-vue/EditOutlined";
import PlusOutlined from "@ant-design/icons-vue/PlusOutlined";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import UploadOutlined from "@ant-design/icons-vue/UploadOutlined";
import type { Attachment } from "@/api/qualification";
import {
  changeMyPasswordApi,
  createMyCertificateApi,
  deleteMyCertificateApi,
  getPersonalOverviewApi,
  updateMyCertificateApi,
  updateMyProfileApi,
  uploadMyAttachmentApi,
  type MyCertificate,
  type MyCertificatePayload,
  type PersonalOverview,
} from "@/api/personal";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
const loading = ref(false);
const saving = ref(false);
const uploading = ref(false);
const activeTab = ref("profile");
const overview = ref<PersonalOverview>();
const profileFormRef = ref<FormInstance>();
const passwordFormRef = ref<FormInstance>();
const certificateFormRef = ref<FormInstance>();
const certificateOpen = ref(false);
const certificateEditingId = ref("");
const previewOpen = ref(false);
const previewAttachments = ref<Attachment[]>([]);

const profileForm = reactive({ displayName: "", phone: "", email: "" });
const passwordForm = reactive({ currentPassword: "", newPassword: "", confirmPassword: "" });
const certificateForm = reactive<MyCertificatePayload>(emptyCertificate());
const requiredRules = [{ required: true, message: "请填写必填项" }];
const emailRules = [{ type: "email" as const, message: "请输入正确的邮箱地址" }];
const passwordRules = [{ required: true, message: "请输入新密码" }, { min: 8, message: "密码至少 8 位" }];
const initial = computed(() => (overview.value?.account.displayName || "员").trim().slice(0, 1));
const certificateColumns = [
  { title: "证书", key: "certificate", width: 220 },
  { title: "类型", dataIndex: "type", width: 130 },
  { title: "专业", dataIndex: "specialty", width: 150 },
  { title: "有效期至", dataIndex: "validTo", width: 120 },
  { title: "状态", key: "status", width: 100 },
  { title: "附件", key: "attachments", width: 80 },
  { title: "操作", key: "actions", width: 90, fixed: "right" },
];
const contractColumns = [
  { title: "合同", key: "contract", width: 200 },
  { title: "合同期限", key: "period", width: 240 },
  { title: "签订日期", dataIndex: "signDate", width: 120 },
  { title: "状态", key: "status", width: 100 },
  { title: "附件", key: "attachments", width: 110 },
];

onMounted(loadOverview);

async function loadOverview() {
  loading.value = true;
  try {
    overview.value = await getPersonalOverviewApi();
    Object.assign(profileForm, {
      displayName: overview.value.account.displayName,
      phone: overview.value.account.phone || "",
      email: overview.value.account.email || "",
    });
  } catch (error) {
    message.error(error instanceof Error ? error.message : "加载个人设置失败");
  } finally {
    loading.value = false;
  }
}

async function saveProfile() {
  await profileFormRef.value?.validate();
  saving.value = true;
  try {
    await updateMyProfileApi(profileForm);
    await Promise.all([loadOverview(), auth.loadCurrentUser()]);
    message.success("个人资料已保存");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "保存失败");
  } finally {
    saving.value = false;
  }
}

async function changePassword() {
  await passwordFormRef.value?.validate();
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    message.error("两次输入的新密码不一致");
    return;
  }
  saving.value = true;
  try {
    await changeMyPasswordApi({ currentPassword: passwordForm.currentPassword, newPassword: passwordForm.newPassword });
    Object.assign(passwordForm, { currentPassword: "", newPassword: "", confirmPassword: "" });
    passwordFormRef.value?.resetFields();
    message.success("密码已修改");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "密码修改失败");
  } finally {
    saving.value = false;
  }
}

function openCertificate(record?: MyCertificate) {
  certificateEditingId.value = record?.id || "";
  Object.assign(certificateForm, record ? {
    name: record.name,
    type: record.type || "",
    certificateNo: record.certificateNo || "",
    specialty: record.specialty || "",
    issueDate: record.issueDate,
    validTo: record.validTo,
    reviewDate: record.reviewDate,
    attachments: [...record.attachments],
    remark: record.remark || "",
  } : emptyCertificate());
  certificateOpen.value = true;
}

async function saveCertificate() {
  await certificateFormRef.value?.validate();
  saving.value = true;
  try {
    if (certificateEditingId.value) await updateMyCertificateApi(certificateEditingId.value, certificateForm);
    else await createMyCertificateApi(certificateForm);
    certificateOpen.value = false;
    await loadOverview();
    message.success("证书已提交并进入待核验状态");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "证书保存失败");
  } finally {
    saving.value = false;
  }
}

async function removeCertificate(record: MyCertificate) {
  try {
    await deleteMyCertificateApi(record.id);
    await loadOverview();
    message.success("证书已删除");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "证书删除失败");
  }
}

async function uploadCertificateFile(file: File) {
  uploading.value = true;
  try {
    certificateForm.attachments.push(await uploadMyAttachmentApi(file));
    message.success("附件已上传");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "附件上传失败");
  } finally {
    uploading.value = false;
  }
  return false;
}

function preview(items: Attachment[]) {
  previewAttachments.value = items;
  previewOpen.value = true;
}

function emptyCertificate(): MyCertificatePayload {
  return { name: "", type: "", certificateNo: "", specialty: "", issueDate: undefined, validTo: undefined, reviewDate: undefined, attachments: [], remark: "" };
}

function statusLabel(value: string) {
  return ({ VALID: "有效", EXPIRING: "临期", EXPIRED: "已过期", UNVERIFIED: "待核验", LOCKED: "已锁定", VOIDED: "已作废" } as Record<string, string>)[value] || value;
}

function statusColor(value: string) {
  return ({ VALID: "green", EXPIRING: "orange", EXPIRED: "red", UNVERIFIED: "blue", LOCKED: "purple", VOIDED: "default" } as Record<string, string>)[value] || "default";
}

function employmentLabel(value: string) {
  return ({ ACTIVE: "在职", LEFT: "离职", DISABLED: "停用" } as Record<string, string>)[value] || value;
}

function contractTypeLabel(value: string) {
  return ({ FIXED_TERM: "固定期限", OPEN_ENDED: "无固定期限", PART_TIME: "非全日制", INTERNSHIP: "实习协议", OTHER: "其他" } as Record<string, string>)[value] || value;
}

function contractStatusLabel(value: string) {
  return ({ ACTIVE: "履行中", DRAFT: "待生效", TERMINATED: "已终止", EXPIRED: "已到期" } as Record<string, string>)[value] || value;
}

function contractStatusColor(value: string) {
  return ({ ACTIVE: "green", DRAFT: "blue", TERMINATED: "default", EXPIRED: "red" } as Record<string, string>)[value] || "default";
}
</script>

<style scoped>
.personal-settings-page { min-width: 0; }
.personal-heading, .section-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.personal-heading h2 { margin: 0; color: #182230; }
.personal-heading p { margin: 5px 0 0; color: #667085; }
.personal-summary { display: grid; grid-template-columns: minmax(220px, 1.5fr) repeat(3, minmax(140px, 1fr)); border: 1px solid #e4e7ec; background: #fff; }
.personal-summary > div { display: flex; min-width: 0; flex-direction: column; justify-content: center; gap: 5px; padding: 18px 20px; border-right: 1px solid #e4e7ec; }
.personal-summary > div:last-child { border-right: 0; }
.personal-summary span { overflow: hidden; color: #667085; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.personal-summary strong { overflow: hidden; color: #101828; font-size: 20px; text-overflow: ellipsis; white-space: nowrap; }
.personal-identity { display: grid !important; grid-template-columns: 44px minmax(0, 1fr); align-items: center; }
.personal-identity > div { display: grid; min-width: 0; gap: 4px; }
.personal-avatar { display: grid; width: 44px; height: 44px; place-items: center; border-radius: 50%; background: #dceee9; color: #155f52 !important; font-size: 18px !important; font-weight: 700; }
.personal-tabs :deep(.ant-tabs-nav) { margin-bottom: 0; padding: 0 4px; background: #fff; }
.settings-surface { min-height: 300px; padding: 24px; border: 1px solid #e4e7ec; border-top: 0; background: #fff; }
.profile-layout { display: grid; grid-template-columns: minmax(300px, 0.8fr) minmax(360px, 1.2fr); gap: 36px; }
.settings-block { min-width: 0; }
.section-heading { min-height: 34px; margin-bottom: 18px; }
.section-heading > div { display: flex; flex-direction: column; gap: 3px; }
.section-heading span { color: #667085; font-size: 12px; }
.compact-form { max-width: 520px; }
.certificate-mobile-list { display: none; }
.record-item { padding: 14px 0; border-bottom: 1px solid #eaecf0; }
.record-heading, .record-meta, .record-actions { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.record-item > span, .record-meta { margin-top: 6px; color: #667085; font-size: 12px; }
.record-actions { justify-content: flex-end; margin-top: 12px; }
.security-surface { min-height: 360px; }
.attachment-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 14px; }
.attachment-grid figure { overflow: hidden; margin: 0; border: 1px solid #e4e7ec; border-radius: 6px; background: #f8fafc; }
.attachment-grid img { display: block; width: 100%; max-height: 540px; object-fit: contain; }
.attachment-grid figure > a { display: grid; min-height: 160px; place-items: center; font-weight: 600; }
.attachment-grid figcaption { padding: 10px 12px; color: #475467; font-size: 12px; }
@media (max-width: 900px) {
  .personal-summary { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .personal-summary > div:nth-child(2) { border-right: 0; }
  .personal-summary > div:nth-child(-n + 2) { border-bottom: 1px solid #e4e7ec; }
  .profile-layout { grid-template-columns: 1fr; gap: 28px; }
  .certificate-desktop-table { display: none; }
  .certificate-mobile-list { display: block; }
}
@media (max-width: 560px) {
  .personal-heading { align-items: flex-start; }
  .settings-surface { padding: 18px; }
  .personal-summary { grid-template-columns: 1fr; }
  .personal-summary > div { padding: 14px; }
  .personal-summary > div,
  .personal-summary > div:nth-child(2) { border-right: 0; border-bottom: 1px solid #e4e7ec; }
  .personal-summary > div:last-child { border-bottom: 0; }
  .personal-summary strong { font-size: 16px; }
  .personal-identity { grid-column: auto; }
}
</style>
