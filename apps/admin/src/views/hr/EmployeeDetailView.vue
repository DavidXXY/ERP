<template>
  <div class="page-stack">
    <a-page-header title="员工档案详情" @back="$router.back()" class="detail-header">
      <template #extra>
        <a-space>
          <a-button @click="refresh"><template #icon><ReloadOutlined /></template>刷新</a-button>
          <a-button type="primary" @click="openEdit">{{ canManage ? '编辑' : '查看' }}</a-button>
        </a-space>
      </template>
    </a-page-header>

    <template v-if="loading">
      <a-skeleton active :paragraph="{ rows: 8 }" />
    </template>

    <template v-else-if="employee">
      <!-- Employee Info Summary -->
      <a-card class="employee-summary-card" :bordered="false">
        <a-descriptions bordered :column="3" size="small">
          <a-descriptions-item label="姓名" :span="1">{{ employee.name }}</a-descriptions-item>
          <a-descriptions-item label="状态" :span="1">
            <a-tag :color="statusColor(employee.employmentStatus)">{{ statusLabel(employee.employmentStatus) }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="工号" :span="1">{{ employee.workNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="部门/组织" :span="1">{{ employee.organizationName || '未分配' }}</a-descriptions-item>
          <a-descriptions-item label="岗位" :span="1">{{ employee.position || '-' }}</a-descriptions-item>
          <a-descriptions-item label="入职日期">{{ employee.entryDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="身份证号">{{ employee.idCard || '-' }}</a-descriptions-item>
          <a-descriptions-item label="手机号">{{ employee.phone || '-' }}</a-descriptions-item>
          <a-descriptions-item label="社保单位">{{ employee.socialSecurityUnit || '-' }}</a-descriptions-item>
          <a-descriptions-item label="社保起始">{{ employee.socialSecurityStart || '-' }}</a-descriptions-item>
          <a-descriptions-item label="社保截止">{{ employee.socialSecurityEnd || '-' }}</a-descriptions-item>
          <a-descriptions-item label="合同起始" :span="1">{{ employee.contractStart || '-' }}</a-descriptions-item>
          <a-descriptions-item label="合同截止" :span="1">{{ employee.contractEnd || '-' }}</a-descriptions-item>
          <a-descriptions-item label="备注" :span="3">{{ employee.remark || '-' }}</a-descriptions-item>
        </a-descriptions>
      </a-card>

      <!-- Tabs: Education, Work Experience, Emergency Contacts, Certificates -->
      <a-card :bordered="false" class="detail-tabs-card">
        <a-tabs v-model:active-key="detailTab">
          <a-tab-pane key="educations" tab="教育经历">
            <a-space class="table-toolbar">
              <a-button v-if="canManage" type="primary" size="small" @click="openEducation()">
                <template #icon><PlusOutlined /></template>添加
              </a-button>
            </a-space>
            <a-table :data-source="educations" :columns="educationColumns" row-key="id" size="small" :pagination="{ pageSize: 5 }">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'highest'">
                  <a-tag v-if="record.highest" color="blue">最高学历</a-tag>
                </template>
                <template v-else-if="column.key === 'actions'">
                  <a-space>
                    <a-button type="text" size="small" @click="openEducation(record)"><EditOutlined /></a-button>
                    <a-popconfirm title="确认删除？" @confirm="removeEducation(record.id)">
                      <a-button danger type="text" size="small"><DeleteOutlined /></a-button>
                    </a-popconfirm>
                  </a-space>
                </template>
              </template>
            </a-table>
          </a-tab-pane>

          <a-tab-pane key="work" tab="工作经历">
            <a-space class="table-toolbar">
              <a-button v-if="canManage" type="primary" size="small" @click="openWorkExperience()">
                <template #icon><PlusOutlined /></template>添加
              </a-button>
            </a-space>
            <a-table :data-source="workExperiences" :columns="workColumns" row-key="id" size="small" :pagination="{ pageSize: 5 }">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'current'">
                  <a-tag v-if="record.current" color="green">当前</a-tag>
                </template>
                <template v-else-if="column.key === 'actions'">
                  <a-space>
                    <a-button type="text" size="small" @click="openWorkExperience(record)"><EditOutlined /></a-button>
                    <a-popconfirm title="确认删除？" @confirm="removeWorkExperience(record.id)">
                      <a-button danger type="text" size="small"><DeleteOutlined /></a-button>
                    </a-popconfirm>
                  </a-space>
                </template>
              </template>
            </a-table>
          </a-tab-pane>

          <a-tab-pane key="contacts" tab="紧急联系人">
            <a-space class="table-toolbar">
              <a-button v-if="canManage" type="primary" size="small" @click="openEmergencyContact()">
                <template #icon><PlusOutlined /></template>添加
              </a-button>
            </a-space>
            <a-table :data-source="emergencyContacts" :columns="contactColumns" row-key="id" size="small" :pagination="{ pageSize: 5 }">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'primary'">
                  <a-tag v-if="record.primary" color="orange">主要联系人</a-tag>
                </template>
                <template v-else-if="column.key === 'actions'">
                  <a-space>
                    <a-button type="text" size="small" @click="openEmergencyContact(record)"><EditOutlined /></a-button>
                    <a-popconfirm title="确认删除？" @confirm="removeEmergencyContact(record.id)">
                      <a-button danger type="text" size="small"><DeleteOutlined /></a-button>
                    </a-popconfirm>
                  </a-space>
                </template>
              </template>
            </a-table>
          </a-tab-pane>

          <a-tab-pane key="certificates" tab="人员证书">
            <a-table :data-source="certificates" :columns="certColumns" row-key="id" size="small" :pagination="{ pageSize: 5 }">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'">
                  <a-tag :color="certStatusColor(record.status)">{{ certStatusLabel(record.status) }}</a-tag>
                </template>
              </template>
            </a-table>
          </a-tab-pane>
        </a-tabs>
      </a-card>
    </template>

    <!-- Education Modal -->
    <a-modal v-model:open="educationModal" :title="educationEditingId ? '编辑教育经历' : '添加教育经历'" width="700px" :confirm-loading="saving" @ok="saveEducation">
      <a-form ref="eduFormRef" :model="eduForm" layout="vertical">
        <a-row :gutter="14">
          <a-col :span="16"><a-form-item label="学校名称" name="schoolName" :rules="requiredRule"><a-input v-model:value="eduForm.schoolName" /></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="学历/学位"><a-select v-model:value="eduForm.degree" :options="degreeOptions" /></a-form-item></a-col>
        </a-row>
        <a-row :gutter="14">
          <a-col :span="12"><a-form-item label="专业"><a-input v-model:value="eduForm.major" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="最高学历"><a-switch v-model:checked="eduForm.highest" /></a-form-item></a-col>
        </a-row>
        <a-row :gutter="14">
          <a-col :span="12"><a-form-item label="开始日期"><a-input v-model:value="eduForm.startDate" type="date" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="结束日期"><a-input v-model:value="eduForm.endDate" type="date" /></a-form-item></a-col>
        </a-row>
        <a-form-item label="备注"><a-textarea v-model:value="eduForm.remark" :rows="2" /></a-form-item>
      </a-form>
    </a-modal>

    <!-- Work Experience Modal -->
    <a-modal v-model:open="workModal" :title="workEditingId ? '编辑工作经历' : '添加工作经历'" width="700px" :confirm-loading="saving" @ok="saveWorkExperience">
      <a-form ref="workFormRef" :model="workForm" layout="vertical">
        <a-row :gutter="14">
          <a-col :span="16"><a-form-item label="公司名称" name="companyName" :rules="requiredRule"><a-input v-model:value="workForm.companyName" /></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="目前在职"><a-switch v-model:checked="workForm.current" /></a-form-item></a-col>
        </a-row>
        <a-form-item label="职位"><a-input v-model:value="workForm.position" /></a-form-item>
        <a-row :gutter="14">
          <a-col :span="12"><a-form-item label="开始日期"><a-input v-model:value="workForm.startDate" type="date" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="结束日期"><a-input v-model:value="workForm.endDate" type="date" /></a-form-item></a-col>
        </a-row>
        <a-form-item label="工作描述"><a-textarea v-model:value="workForm.description" :rows="3" /></a-form-item>
        <a-form-item label="备注"><a-textarea v-model:value="workForm.remark" :rows="2" /></a-form-item>
      </a-form>
    </a-modal>

    <!-- Emergency Contact Modal -->
    <a-modal v-model:open="contactModal" :title="contactEditingId ? '编辑紧急联系人' : '添加紧急联系人'" width="600px" :confirm-loading="saving" @ok="saveEmergencyContact">
      <a-form ref="contactFormRef" :model="contactForm" layout="vertical">
        <a-row :gutter="14">
          <a-col :span="12"><a-form-item label="姓名" name="name" :rules="requiredRule"><a-input v-model:value="contactForm.name" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="关系"><a-input v-model:value="contactForm.relationship" /></a-form-item></a-col>
        </a-row>
        <a-row :gutter="14">
          <a-col :span="12"><a-form-item label="联系电话"><a-input v-model:value="contactForm.phone" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="默认联系人"><a-switch v-model:checked="contactForm.primary" /></a-form-item></a-col>
        </a-row>
        <a-form-item label="地址"><a-input v-model:value="contactForm.address" /></a-form-item>
        <a-form-item label="备注"><a-textarea v-model:value="contactForm.remark" :rows="2" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute } from "vue-router";
import { message } from "ant-design-vue";
import { PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined } from "@ant-design/icons-vue";
import { useAuthStore } from "@/stores/auth";
import {
  getQualificationEmployee,
  listPersonnelCertificates,
  type EmployeeDetail,
  type PersonnelCertificate,
} from "@/api/qualification";
import {
  listEducations, createEducation, updateEducation, deleteEducation,
  listWorkExperiences, createWorkExperience, updateWorkExperience, deleteWorkExperience,
  listEmergencyContacts, createEmergencyContact, updateEmergencyContact, deleteEmergencyContact,
  type EducationRecord, type EducationPayload,
  type WorkExperienceRecord, type WorkExperiencePayload,
  type EmergencyContactRecord, type EmergencyContactPayload,
} from "@/api/hr";

const auth = useAuthStore();
const route = useRoute();
const employeeId = computed(() => route.params.employeeId as string);
const canManage = computed(() => auth.can("qualification:employee:manage"));

const loading = ref(false);
const saving = ref(false);
const employee = ref<EmployeeDetail>();
const detailTab = ref("educations");

// Sub data
const educations = ref<EducationRecord[]>([]);
const workExperiences = ref<WorkExperienceRecord[]>([]);
const emergencyContacts = ref<EmergencyContactRecord[]>([]);
const certificates = ref<PersonnelCertificate[]>([]);

// Education modal
const educationModal = ref(false);
const educationEditingId = ref("");
const eduFormRef = ref();
const eduForm = reactive<EducationPayload>({ schoolName: "", degree: undefined, major: "", highest: false, startDate: undefined, endDate: undefined, remark: "" });
const educationColumns = [
  { title: "学校", dataIndex: "schoolName", width: 200 },
  { title: "学历", dataIndex: "degree", width: 100 },
  { title: "专业", dataIndex: "major", width: 150 },
  { title: "开始日期", dataIndex: "startDate", width: 120 },
  { title: "结束日期", dataIndex: "endDate", width: 120 },
  { title: "最高", key: "highest", width: 100 },
  { title: "操作", key: "actions", width: 100, fixed: "right" },
];

// Work experience modal
const workModal = ref(false);
const workEditingId = ref("");
const workFormRef = ref();
const workForm = reactive<WorkExperiencePayload>({ companyName: "", position: "", current: false, startDate: undefined, endDate: undefined, description: "", remark: "" });
const workColumns = [
  { title: "公司", dataIndex: "companyName", width: 200 },
  { title: "职位", dataIndex: "position", width: 130 },
  { title: "开始日期", dataIndex: "startDate", width: 110 },
  { title: "结束日期", dataIndex: "endDate", width: 110 },
  { title: "当前", key: "current", width: 80 },
  { title: "描述", dataIndex: "description", ellipsis: true },
  { title: "操作", key: "actions", width: 100, fixed: "right" },
];

// Emergency contact modal
const contactModal = ref(false);
const contactEditingId = ref("");
const contactFormRef = ref();
const contactForm = reactive<EmergencyContactPayload>({ name: "", relationship: "", phone: "", address: "", primary: false, remark: "" });
const contactColumns = [
  { title: "姓名", dataIndex: "name", width: 130 },
  { title: "关系", dataIndex: "relationship", width: 100 },
  { title: "电话", dataIndex: "phone", width: 130 },
  { title: "地址", dataIndex: "address", ellipsis: true },
  { title: "默认", key: "primary", width: 100 },
  { title: "操作", key: "actions", width: 100, fixed: "right" },
];

// Certificate columns (from qualification module)
const certColumns = [
  { title: "证书名称", dataIndex: "name", width: 200 },
  { title: "证书编号", dataIndex: "certificateNo", width: 150 },
  { title: "专业", dataIndex: "specialty", width: 120 },
  { title: "有效期至", dataIndex: "validTo", width: 120 },
  { title: "状态", key: "status", width: 90 },
];

const degreeOptions = [
  { value: "博士", label: "博士" },
  { value: "硕士", label: "硕士" },
  { value: "本科", label: "本科" },
  { value: "大专", label: "大专" },
  { value: "中专", label: "中专" },
  { value: "高中", label: "高中" },
  { value: "其他", label: "其他" },
];

const requiredRule = [{ required: true, message: "请填写必填项" }];

function statusColor(status: string) {
  return ({ ACTIVE: "green", LEFT: "red", DISABLED: "default" } as Record<string, string>)[status] || "default";
}
function statusLabel(status: string) {
  return ({ ACTIVE: "在职", LEFT: "离职", DISABLED: "停用" } as Record<string, string>)[status] || status;
}
function certStatusColor(s: string) {
  return ({ VALID: "green", EXPIRING: "orange", EXPIRED: "red" } as Record<string, string>)[s] || "default";
}
function certStatusLabel(s: string) {
  return ({ VALID: "有效", EXPIRING: "临期", EXPIRED: "已过期" } as Record<string, string>)[s] || s;
}

async function loadData() {
  loading.value = true;
  try {
    const emp = await getQualificationEmployee(employeeId.value);
    employee.value = emp;

    // Load certificates and filter by employee
    const allCerts = await listPersonnelCertificates({});
    certificates.value = allCerts.filter(c => c.employeeId === employeeId.value);

    // Load sub entities in parallel
    const [eds, works, contacts] = await Promise.all([
      listEducations(employeeId.value),
      listWorkExperiences(employeeId.value),
      listEmergencyContacts(employeeId.value),
    ]);
    educations.value = eds;
    workExperiences.value = works;
    emergencyContacts.value = contacts;
  } catch (error: any) {
    message.error(error.message || "加载失败");
  } finally {
    loading.value = false;
  }
}

function refresh() { loadData(); }

// Education
function openEducation(record?: EducationRecord) {
  educationEditingId.value = record?.id || "";
  Object.assign(eduForm, record ? { schoolName: record.schoolName, degree: record.degree, major: record.major, highest: record.highest, startDate: record.startDate, endDate: record.endDate, remark: record.remark } : { schoolName: "", degree: undefined, major: "", highest: false, startDate: undefined, endDate: undefined, remark: "" });
  educationModal.value = true;
}
async function saveEducation() {
  await eduFormRef.value?.validate();
  saving.value = true;
  try {
    if (educationEditingId.value) {
      await updateEducation(educationEditingId.value, eduForm);
    } else {
      await createEducation(employeeId.value, eduForm);
    }
    educationModal.value = false;
    message.success("教育经历已保存");
    educations.value = await listEducations(employeeId.value);
  } catch (error: any) {
    message.error(error.message || "保存失败");
  } finally {
    saving.value = false;
  }
}
async function removeEducation(id: string) {
  await deleteEducation(id);
  message.success("已删除");
  educations.value = await listEducations(employeeId.value);
}

// Work Experience
function openWorkExperience(record?: WorkExperienceRecord) {
  workEditingId.value = record?.id || "";
  const defaults = { companyName: "", position: "", current: false, startDate: undefined, endDate: undefined, description: "", remark: "" };
  Object.assign(workForm, record ? { companyName: record.companyName, position: record.position, current: record.current, startDate: record.startDate, endDate: record.endDate, description: record.description, remark: record.remark } : defaults);
  workModal.value = true;
}
async function saveWorkExperience() {
  await workFormRef.value?.validate();
  saving.value = true;
  try {
    if (workEditingId.value) {
      await updateWorkExperience(workEditingId.value, workForm);
    } else {
      await createWorkExperience(employeeId.value, workForm);
    }
    workModal.value = false;
    message.success("工作经历已保存");
    workExperiences.value = await listWorkExperiences(employeeId.value);
  } catch (error: any) {
    message.error(error.message || "保存失败");
  } finally {
    saving.value = false;
  }
}
async function removeWorkExperience(id: string) {
  await deleteWorkExperience(id);
  message.success("已删除");
  workExperiences.value = await listWorkExperiences(employeeId.value);
}

// Emergency Contact
function openEmergencyContact(record?: EmergencyContactRecord) {
  contactEditingId.value = record?.id || "";
  const defaults = { name: "", relationship: "", phone: "", address: "", primary: false, remark: "" };
  Object.assign(contactForm, record ? { name: record.name, relationship: record.relationship, phone: record.phone, address: record.address, primary: record.primary, remark: record.remark } : defaults);
  contactModal.value = true;
}
async function saveEmergencyContact() {
  await contactFormRef.value?.validate();
  saving.value = true;
  try {
    if (contactEditingId.value) {
      await updateEmergencyContact(contactEditingId.value, contactForm);
    } else {
      await createEmergencyContact(employeeId.value, contactForm);
    }
    contactModal.value = false;
    message.success("紧急联系人已保存");
    emergencyContacts.value = await listEmergencyContacts(employeeId.value);
  } catch (error: any) {
    message.error(error.message || "保存失败");
  } finally {
    saving.value = false;
  }
}
async function removeEmergencyContact(id: string) {
  await deleteEmergencyContact(id);
  message.success("已删除");
  emergencyContacts.value = await listEmergencyContacts(employeeId.value);
}

onMounted(loadData);
</script>

<style scoped>
.detail-header { padding: 0 0 12px; }
.employee-summary-card { margin-bottom: 12px; }
.detail-tabs-card :deep(.ant-card-body) { padding-top: 0; }
.table-toolbar { margin-bottom: 8px; display: block; }
</style>
