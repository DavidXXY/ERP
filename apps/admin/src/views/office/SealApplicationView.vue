<template>
  <div class="page-stack">
    <a-card>
      <template #title>用印申请</template>
      <template #extra>
        <a-space>
          <a-button @click="router.push('/office')">返回办公室</a-button>
          <a-button :loading="loading" @click="loadData"
            ><template #icon><ReloadOutlined /></template>刷新</a-button
          >
        </a-space>
      </template>

      <a-space wrap class="table-toolbar">
        <a-button
          v-if="auth.can('office:seal:create')"
          type="primary"
          @click="openCreate"
        >
          <template #icon><PlusOutlined /></template>新增用印申请
        </a-button>
      </a-space>

      <a-table
        :columns="columns"
        :data-source="items"
        :loading="loading"
        :pagination="{ pageSize: 10 }"
        :row-key="(record: SealApplication) => record.id"
        :scroll="{ x: 1520 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'application'">
            <strong>{{ record.code }}</strong>
            <span class="table-subtitle">{{ record.documentPurpose }}</span>
          </template>
          <template v-else-if="column.key === 'document'">
            <strong>{{ record.documentName }}</strong>
            <span class="table-subtitle">{{
              record.counterparty || "无对方单位"
            }}</span>
            <a-button
              v-if="record.attachments?.length"
              type="link"
              size="small"
              class="attachment-link"
              @click="previewDocument(record.attachments[0])"
            >
              {{ record.attachments.length }} 份附件
            </a-button>
          </template>
          <template v-else-if="column.key === 'usage'">
            {{ record.useDate }} · {{ record.copyCount }} 份
            <span class="table-subtitle">{{
              record.takeOut
                ? `外带，预计 ${record.expectedReturnDate} 归还`
                : "现场用印"
            }}</span>
          </template>
          <template v-else-if="column.key === 'returnedAt'">
            {{ formatDateTime(record.returnedAt) }}
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{
              statusLabel(record.status)
            }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a-button type="link" size="small" @click="openApproval(record)"
                >查看/审批</a-button
              >
              <a-popconfirm
                v-if="
                  record.takeOut &&
                  record.status === 'APPROVED' &&
                  auth.can('office:seal:return')
                "
                title="确认外带印章已经归还？"
                ok-text="确认归还"
                cancel-text="取消"
                @confirm="confirmReturn(record)"
              >
                <a-button type="link" size="small"
                  ><template #icon><CheckOutlined /></template
                  >确认归还</a-button
                >
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <ApprovalCenterView
      ref="approvalCenterRef"
      embedded
      drawer-only
      @changed="loadData"
    />

    <a-modal
      v-model:open="createOpen"
      title="新增用印申请"
      width="820px"
      :confirm-loading="saving"
      @ok="submit"
    >
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="12">
            <a-form-item label="申请人" name="applicantId">
              <a-select
                v-model:value="form.applicantId"
                show-search
                option-filter-prop="label"
                :options="userOptions"
                @change="syncApplicant"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="部门" name="departmentName">
              <a-input
                v-model:value="form.departmentName"
                placeholder="填写所属部门"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="印章类型" name="sealType">
              <a-select
                v-model:value="form.sealType"
                :options="sealTypeOptions"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="用印日期" name="useDate">
              <a-input v-model:value="form.useDate" type="date" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="16">
            <a-form-item label="文件名称" name="documentName">
              <a-input
                v-model:value="form.documentName"
                placeholder="合同、授权书或其他文件名称"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="8">
            <a-form-item label="份数" name="copyCount">
              <a-input-number
                v-model:value="form.copyCount"
                :min="1"
                :precision="0"
                class="full-input"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="对方单位">
              <a-input
                v-model:value="form.counterparty"
                placeholder="没有对方单位可不填"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="用印用途" name="documentPurpose">
              <a-textarea
                v-model:value="form.documentPurpose"
                :rows="3"
                placeholder="说明文件用途、授权依据和交付对象"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item
              label="用印附件"
              required
              :validate-status="attachmentError ? 'error' : undefined"
              :help="
                attachmentError ||
                '支持图片、PDF、Word、Excel，单个文件不超过 20MB'
              "
            >
              <a-upload
                v-model:file-list="attachmentFiles"
                :before-upload="beforeAttachmentUpload"
                :multiple="true"
                accept=".jpg,.jpeg,.png,.webp,.pdf,.doc,.docx,.xls,.xlsx"
              >
                <a-button>
                  <template #icon><UploadOutlined /></template>选择附件
                </a-button>
              </a-upload>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="外带印章">
              <a-switch
                v-model:checked="form.takeOut"
                checked-children="外带"
                un-checked-children="现场"
                @change="handleTakeOutChange"
              />
            </a-form-item>
          </a-col>
          <a-col v-if="form.takeOut" :xs="24" :md="12">
            <a-form-item label="预计归还日期" name="expectedReturnDate">
              <a-input v-model:value="form.expectedReturnDate" type="date" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { generateCode } from "@/utils/code";
import { computed, onMounted, reactive, ref } from "vue";
import { message, Upload, type UploadFile } from "ant-design-vue";
import CheckOutlined from "@ant-design/icons-vue/CheckOutlined";
import PlusOutlined from "@ant-design/icons-vue/PlusOutlined";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import UploadOutlined from "@ant-design/icons-vue/UploadOutlined";
import { useRouter } from "vue-router";
import {
  createSealApplication,
  previewDocument,
  getOfficeReferences,
  listSealApplications,
  returnSealApplication,
  type OfficeApplicationStatus,
  type SealApplication,
} from "@/api/office";
import { useAuthStore } from "@/stores/auth";
import ApprovalCenterView from "@/views/office/ApprovalCenterView.vue";

const auth = useAuthStore();
const router = useRouter();
const loading = ref(false);
const saving = ref(false);
const createOpen = ref(false);
const attachmentFiles = ref<UploadFile[]>([]);
const attachmentError = ref("");
const items = ref<SealApplication[]>([]);
const references = reactive({ users: [] } as any);
const formRef = ref();
const approvalCenterRef = ref<InstanceType<typeof ApprovalCenterView>>();
const form = reactive({
  code: "",
  applicantId: "",
  applicantName: "",
  departmentName: "",
  sealType: "公章",
  documentName: "",
  documentPurpose: "",
  counterparty: "",
  copyCount: 1,
  useDate: today(),
  takeOut: false,
  expectedReturnDate: undefined as string | undefined,
});
const columns = [
  { title: "申请单", key: "application", width: 280 },
  { title: "申请人", dataIndex: "applicantName", width: 120 },
  { title: "部门", dataIndex: "departmentName", width: 150 },
  { title: "印章", dataIndex: "sealType", width: 110 },
  { title: "文件", key: "document", width: 270 },
  { title: "用印安排", key: "usage", width: 250 },
  { title: "归还时间", key: "returnedAt", width: 190 },
  { title: "状态", key: "status", width: 120 },
  { title: "操作", key: "action", width: 190, fixed: "right" as const },
];
const rules = {
  applicantId: [{ required: true, message: "请选择申请人" }],
  departmentName: [{ required: true, message: "请填写部门" }],
  sealType: [{ required: true, message: "请选择印章类型" }],
  documentName: [{ required: true, message: "请填写文件名称" }],
  documentPurpose: [{ required: true, message: "请填写用印用途" }],
  copyCount: [{ required: true, message: "请填写份数" }],
  useDate: [{ required: true, message: "请选择用印日期" }],
  expectedReturnDate: [
    {
      validator: (_rule: unknown, value?: string) =>
        form.takeOut && !value
          ? Promise.reject(new Error("请填写预计归还日期"))
          : Promise.resolve(),
    },
  ],
};
const sealTypeOptions = [
  "公章",
  "合同章",
  "财务章",
  "法人章",
  "项目章",
  "其他",
].map((value) => ({ label: value, value }));
const userOptions = computed(() =>
  references.users
    .filter((item: any) => item.enabled)
    .map((item: any) => ({ label: item.displayName, value: item.id })),
);

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    const [applications, referenceData] = await Promise.all([
      listSealApplications(),
      getOfficeReferences(),
    ]);
    items.value = applications || [];
    references.users = referenceData.users || [];
  } catch (error) {
    message.error(error instanceof Error ? error.message : "数据加载失败");
  } finally {
    loading.value = false;
  }
}
function openCreate() {
  Object.assign(form, {
    code: generateCode("YY"),
    applicantId: auth.user?.id || "",
    applicantName: auth.user?.displayName || "",
    departmentName: "",
    sealType: "公章",
    documentName: "",
    documentPurpose: "",
    counterparty: "",
    copyCount: 1,
    useDate: today(),
    takeOut: false,
    expectedReturnDate: undefined,
  });
  attachmentFiles.value = [];
  attachmentError.value = "";
  createOpen.value = true;
}
function beforeAttachmentUpload(file: File) {
  const extension = `.${file.name.split(".").pop()?.toLowerCase() || ""}`;
  const allowed = [
    ".jpg",
    ".jpeg",
    ".png",
    ".webp",
    ".pdf",
    ".doc",
    ".docx",
    ".xls",
    ".xlsx",
  ];
  if (!allowed.includes(extension)) {
    message.error("仅支持图片、PDF、Word 或 Excel 文件");
    return Upload.LIST_IGNORE;
  }
  if (file.size > 20 * 1024 * 1024) {
    message.error("单个附件不能超过 20MB");
    return Upload.LIST_IGNORE;
  }
  attachmentError.value = "";
  return false;
}
function syncApplicant(id: string) {
  form.applicantName =
    references.users.find((item: any) => item.id === id)?.displayName || "";
}
function handleTakeOutChange(checked: boolean) {
  if (!checked) form.expectedReturnDate = undefined;
  else form.expectedReturnDate = form.useDate;
}
function openApproval(record: SealApplication) {
  approvalCenterRef.value?.openApprovalById(record.approvalRequestId);
}
async function submit() {
  try {
    await formRef.value?.validate();
  } catch {
    return;
  }
  const files = attachmentFiles.value.flatMap((item) =>
    item.originFileObj ? [item.originFileObj as File] : [],
  );
  if (!files.length) {
    attachmentError.value = "请至少上传一份用印附件";
    return;
  }
  if (
    form.takeOut &&
    form.expectedReturnDate &&
    form.expectedReturnDate < form.useDate
  ) {
    message.error("预计归还日期不能早于用印日期");
    return;
  }
  saving.value = true;
  try {
    await createSealApplication(
      {
        ...form,
        counterparty: form.counterparty || undefined,
        expectedReturnDate: form.takeOut ? form.expectedReturnDate : undefined,
      },
      files,
    );
    createOpen.value = false;
    message.success("用印申请已提交审批");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "用印申请提交失败");
  } finally {
    saving.value = false;
  }
}
async function confirmReturn(record: SealApplication) {
  try {
    await returnSealApplication(record.id);
    message.success("已确认归还");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "归还确认失败");
  }
}
function statusLabel(value: OfficeApplicationStatus) {
  return (
    {
      PENDING_APPROVAL: "待审批",
      APPROVED: "待用印/归还",
      REJECTED: "已驳回",
      COMPLETED: "已归还",
      CANCELLED: "已取消",
    } as Record<OfficeApplicationStatus, string>
  )[value];
}
function statusColor(value: OfficeApplicationStatus) {
  return (
    {
      PENDING_APPROVAL: "orange",
      APPROVED: "blue",
      REJECTED: "red",
      COMPLETED: "green",
      CANCELLED: "default",
    } as Record<OfficeApplicationStatus, string>
  )[value];
}
function formatDateTime(value?: string) {
  if (!value) return "-";
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(new Date(value));
}
function today() {
  const date = new Date();
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}
</script>
