<template>
  <div class="page-stack">
    <a-card>
      <template #title>采购申请</template>
      <template #extra>
        <a-space>
          <a-button @click="router.push('/procurement')">返回采购管理</a-button>
          <a-button :loading="loading" @click="loadData">
            <template #icon><ReloadOutlined /></template>刷新
          </a-button>
        </a-space>
      </template>

      <a-space wrap class="table-toolbar">
        <a-button
          v-if="auth.can('procurement:purchase:create')"
          type="primary"
          @click="openCreate"
        >
          <template #icon><PlusOutlined /></template>新增单项申请
        </a-button>
        <a-button
          v-if="auth.can('procurement:purchase:create')"
          type="primary"
          ghost
          @click="openImport"
        >
          <template #icon><UploadOutlined /></template>批量导入申请
        </a-button>
        <a-button href="/templates/采购申请批量导入模板.xlsx" download>
          <template #icon><DownloadOutlined /></template>下载导入模板
        </a-button>
        <a-button @click="router.push('/procurement/purchase-pool')">
          <template #icon><ShoppingCartOutlined /></template>待采购清单
        </a-button>
      </a-space>

      <a-alert
        class="section-alert"
        type="info"
        show-icon
        message="一张申请批次可包含多条采购物料。展开批次查看明细；图纸、照片、规格书可上传到整批或指定物料。"
      />

      <a-table
        :columns="batchColumns"
        :data-source="requestBatches"
        :loading="loading"
        :pagination="{ pageSize: 10 }"
        row-key="batchId"
        :scroll="{ x: 1320 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'batch'">
            <strong>{{ record.batchCode }}</strong>
            <span class="table-subtitle">{{ record.batchName }}</span>
          </template>
          <template v-else-if="column.key === 'summary'">
            <strong>{{ record.itemCount }} 项物料</strong>
            <span class="table-subtitle"
              >合计数量 {{ formatQuantity(record.totalQuantity) }}</span
            >
          </template>
          <template v-else-if="column.key === 'amount'">
            <strong>{{ formatMoney(record.totalAmount) }}</strong>
            <span class="table-subtitle"
              >{{ record.pendingCount }} 项待审批</span
            >
          </template>
          <template v-else-if="column.key === 'target'">
            {{ record.costTargetName || "-" }}
            <span class="table-subtitle">{{
              record.costType === "PROJECT" ? "项目预算" : "部门费用"
            }}</span>
          </template>
          <template v-else-if="column.key === 'date'">
            {{ record.dateRange }}
          </template>
          <template v-else-if="column.key === 'approval'">
            <a-tag :color="approvalColor(record.approvalStatus)">
              {{ approvalStatusLabel(record.approvalStatus) }}
            </a-tag>
            <span v-if="record.mixedApproval" class="table-subtitle"
              >部分明细已处理</span
            >
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a-button
                type="link"
                size="small"
                @click="openBatchDocuments(record)"
              >
                <template #icon><PaperClipOutlined /></template>整批附件
              </a-button>
              <a-button type="link" size="small" @click="openApproval(record)">
                {{ record.pendingCount > 0 ? "整批审批" : "流程" }}
              </a-button>
            </a-space>
          </template>
        </template>

        <template #expandedRowRender="{ record }">
          <div class="line-detail">
            <a-table
              size="small"
              :columns="lineColumns"
              :data-source="record.items"
              :pagination="false"
              row-key="id"
              :scroll="{ x: 1120 }"
            >
              <template #bodyCell="{ column, record: line }">
                <template v-if="column.key === 'line'">
                  <strong>#{{ line.lineNo || 1 }}</strong>
                  <span class="table-subtitle">{{ line.code }}</span>
                </template>
                <template v-else-if="column.key === 'material'">
                  {{ line.partName || line.materialName || "-" }}
                  <span class="table-subtitle">{{ line.reason || "-" }}</span>
                </template>
                <template v-else-if="column.key === 'qty'">
                  {{ formatQuantity(line.quantity) }}
                </template>
                <template v-else-if="column.key === 'price'">
                  {{ formatMoney(Number(line.unitPrice || 0)) }}
                </template>
                <template v-else-if="column.key === 'amount'">
                  <strong>{{
                    formatMoney(
                      Number(
                        line.totalAmount ||
                          Number(line.unitPrice || 0) *
                            Number(line.quantity || 0),
                      ),
                    )
                  }}</strong>
                  <span class="table-subtitle"
                    >税率 {{ formatTaxRate(line.taxRate) }}</span
                  >
                </template>
                <template v-else-if="column.key === 'date'">
                  {{ line.expectedDate || line.requiredDate || "-" }}
                </template>
                <template v-else-if="column.key === 'approval'">
                  <a-tag :color="approvalColor(line.approvalStatus)">
                    {{ approvalStatusLabel(line.approvalStatus) }}
                  </a-tag>
                </template>
                <template v-else-if="column.key === 'action'">
                  <a-button
                    type="link"
                    size="small"
                    @click="openLineDocuments(line)"
                  >
                    <template #icon><PaperClipOutlined /></template>明细附件
                  </a-button>
                </template>
              </template>
            </a-table>
          </div>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="createOpen"
      title="新增单项采购申请"
      width="760px"
      :confirm-loading="saving"
      @ok="handleCreate"
    >
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="物料名称" name="materialName">
              <a-input v-model:value="form.materialName" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="规格型号">
              <a-input v-model:value="form.materialSpec" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="数量" name="quantity">
              <a-input-number
                v-model:value="form.quantity"
                :min="0.01"
                class="full-input"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="单位">
              <a-input v-model:value="form.unit" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="需求日期">
              <a-input v-model:value="form.requiredDate" type="date" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="预计单价">
              <a-input-number
                v-model:value="form.unitPrice"
                :min="0"
                :precision="2"
                class="full-input"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="税率(%)">
              <a-input-number
                v-model:value="form.taxRate"
                :min="0"
                :max="100"
                :precision="2"
                class="full-input"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="成本归属">
              <a-select
                v-model:value="form.costType"
                :options="costTypeOptions"
                @change="form.costTargetId = ''"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item
          :label="form.costType === 'PROJECT' ? '关联项目' : '关联部门'"
          name="costTargetId"
        >
          <a-select
            v-model:value="form.costTargetId"
            show-search
            option-filter-prop="label"
            :options="targetOptions"
            placeholder="请选择成本归属对象"
          />
        </a-form-item>
        <a-form-item label="申请原因" name="reason">
          <a-textarea v-model:value="form.reason" :rows="2" />
        </a-form-item>
        <a-alert
          type="info"
          show-icon
          :message="`预计采购金额 ${formatMoney(estimatedAmount)}。创建后可立即上传图纸、照片和规格书。`"
        />
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="importOpen"
      title="批量导入采购申请"
      width="760px"
      :confirm-loading="importing"
      ok-text="校验并创建批次"
      @ok="handleImport"
    >
      <a-form
        ref="importFormRef"
        :model="importForm"
        :rules="importRules"
        layout="vertical"
      >
        <a-row :gutter="16">
          <a-col :span="14">
            <a-form-item label="申请批次名称" name="batchName">
              <a-input
                v-model:value="importForm.batchName"
                placeholder="例如：XX项目8月第一批现场材料"
              />
            </a-form-item>
          </a-col>
          <a-col :span="10">
            <a-form-item label="成本归属">
              <a-select
                v-model:value="importForm.costType"
                :options="costTypeOptions"
                @change="importForm.costTargetId = ''"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item
          :label="importForm.costType === 'PROJECT' ? '关联项目' : '关联部门'"
          name="costTargetId"
        >
          <a-select
            v-model:value="importForm.costTargetId"
            show-search
            option-filter-prop="label"
            :options="importTargetOptions"
            placeholder="整批明细统一归属到该成本对象"
          />
        </a-form-item>
        <a-form-item label="整批说明">
          <a-textarea
            v-model:value="importForm.sharedReason"
            :rows="2"
            placeholder="会追加到每一条明细的采购原因中"
          />
        </a-form-item>
        <a-form-item label="采购明细文件" required>
          <a-upload-dragger
            :show-upload-list="false"
            accept=".xlsx,.xls,.csv"
            :before-upload="selectImportFile"
          >
            <p class="ant-upload-drag-icon"><InboxOutlined /></p>
            <p class="ant-upload-text">点击或拖入 Excel / CSV 文件</p>
            <p class="ant-upload-hint">
              一次最多500条，系统先校验整表，错误时不会写入部分数据
            </p>
          </a-upload-dragger>
          <a-alert
            v-if="importFile"
            class="section-alert"
            type="success"
            show-icon
            :message="`已选择：${importFile.name}（${formatFileSize(importFile.size)}）`"
          />
        </a-form-item>
        <a-space>
          <a-button href="/templates/采购申请批量导入模板.xlsx" download>
            <template #icon><DownloadOutlined /></template>下载最新模板
          </a-button>
          <span class="muted-text">请保留模板第4行表头。</span>
        </a-space>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="approvalOpen"
      title="采购申请批次审批"
      width="760px"
      :footer="canApproveSelected ? undefined : null"
      :confirm-loading="saving"
      @ok="handleApproval"
    >
      <a-alert
        v-if="selectedBatch"
        class="section-alert"
        type="info"
        :message="`${selectedBatch.batchCode} · ${selectedBatch.itemCount}项 · ${formatMoney(selectedBatch.totalAmount)} · ${selectedBatch.costTargetName}`"
      />
      <a-card
        v-if="selectedBatch"
        size="small"
        title="流程进展"
        class="section-alert"
      >
        <ApprovalProgressFlow :steps="batchApprovalSteps(selectedBatch)" />
      </a-card>
      <a-table
        v-if="selectedBatch"
        size="small"
        :columns="approvalLineColumns"
        :data-source="selectedBatch.items"
        :pagination="{ pageSize: 5 }"
        row-key="id"
      />
      <a-form v-if="canApproveSelected" :model="approvalForm" layout="vertical">
        <a-form-item label="审批结论">
          <a-radio-group
            v-model:value="approvalForm.decision"
            button-style="solid"
          >
            <a-radio-button value="APPROVED">整批通过</a-radio-button>
            <a-radio-button value="REJECTED">整批驳回</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="审批意见">
          <a-textarea v-model:value="approvalForm.comment" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-drawer
      v-model:open="documentOpen"
      :title="attachmentTarget?.title || '采购申请附件'"
      width="min(720px, 100vw)"
    >
      <a-alert
        class="section-alert"
        type="info"
        show-icon
        message="支持照片、PDF、Word、Excel、TXT、ZIP、DWG、DXF；单个文件不超过20MB。图片和PDF可在线预览。"
      />
      <div class="attachment-toolbar">
        <a-upload
          v-if="auth.can('procurement:purchase:create')"
          multiple
          :show-upload-list="false"
          :before-upload="uploadAttachment"
          accept=".jpg,.jpeg,.png,.webp,.pdf,.doc,.docx,.xls,.xlsx,.txt,.zip,.dwg,.dxf"
        >
          <a-button type="primary">
            <template #icon><UploadOutlined /></template>上传附件
          </a-button>
        </a-upload>
        <span class="muted-text"
          >整批附件供所有明细共用；明细附件只绑定到指定物料。</span
        >
      </div>
      <a-table
        :columns="documentColumns"
        :data-source="documents"
        :loading="documentsLoading"
        :pagination="false"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'file'">
            <strong>{{ record.fileName }}</strong>
            <span class="table-subtitle"
              >{{ formatFileSize(record.sizeBytes) }} ·
              {{ formatDateTime(record.createdAt) }}</span
            >
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button
                type="link"
                size="small"
                @click="previewDocument(record)"
                >预览</a-button
              >
              <a-button
                type="link"
                size="small"
                @click="downloadDocument(record)"
                >下载</a-button
              >
            </a-space>
          </template>
        </template>
        <template #emptyText>暂无附件，可上传图纸、照片或规格文件</template>
      </a-table>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { message } from "ant-design-vue";
import DownloadOutlined from "@ant-design/icons-vue/DownloadOutlined";
import InboxOutlined from "@ant-design/icons-vue/InboxOutlined";
import PaperClipOutlined from "@ant-design/icons-vue/PaperClipOutlined";
import PlusOutlined from "@ant-design/icons-vue/PlusOutlined";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import ShoppingCartOutlined from "@ant-design/icons-vue/ShoppingCartOutlined";
import UploadOutlined from "@ant-design/icons-vue/UploadOutlined";
import {
  createPurchaseRequest,
  importPurchaseRequestBatch,
  listProcurementCostTargets,
  listPurchaseRequests,
  processPurchaseRequestBatchApproval,
  type ApprovalStatus,
  type ProcurementCostTargetOption,
  type ProcurementCostType,
  type PurchaseRequest,
} from "@/api/procurement";
import {
  downloadDocument,
  listDocumentsByBiz,
  previewDocument,
  uploadDocument,
  type DocumentRecord,
} from "@/api/office";
import { useAuthStore } from "@/stores/auth";
import ApprovalProgressFlow, {
  type ApprovalProgressStep,
} from "@/components/ApprovalProgressFlow.vue";

type RequestBatch = {
  batchId: string;
  batchCode: string;
  batchName: string;
  items: PurchaseRequest[];
  itemCount: number;
  pendingCount: number;
  totalQuantity: number;
  totalAmount: number;
  costType: ProcurementCostType;
  costTargetName: string;
  requesterName: string;
  dateRange: string;
  approvalStatus: ApprovalStatus;
  mixedApproval: boolean;
};

const auth = useAuthStore();
const router = useRouter();
const loading = ref(false);
const saving = ref(false);
const importing = ref(false);
const requests = ref<PurchaseRequest[]>([]);
const projects = ref<ProcurementCostTargetOption[]>([]);
const departments = ref<ProcurementCostTargetOption[]>([]);
const createOpen = ref(false);
const importOpen = ref(false);
const approvalOpen = ref(false);
const documentOpen = ref(false);
const formRef = ref();
const importFormRef = ref();
const selectedBatch = ref<RequestBatch | null>(null);
const importFile = ref<File | null>(null);
const documents = ref<DocumentRecord[]>([]);
const documentsLoading = ref(false);
const attachmentTarget = ref<{
  bizType: string;
  bizId: string;
  title: string;
} | null>(null);

const form = reactive({
  materialName: "",
  materialSpec: "",
  quantity: 1,
  unit: "个",
  unitPrice: 0,
  taxRate: 13,
  requiredDate: "",
  reason: "",
  costType: "PROJECT" as ProcurementCostType,
  costTargetId: "",
});
const importForm = reactive({
  batchName: "",
  costType: "PROJECT" as ProcurementCostType,
  costTargetId: "",
  sharedReason: "",
});
const approvalForm = reactive<{
  decision: ApprovalStatus;
  comment: string;
  approverName: string;
}>({ decision: "APPROVED", comment: "同意整批采购", approverName: "" });

const rules = {
  materialName: [{ required: true, message: "请填写物料名称" }],
  quantity: [{ required: true, message: "请填写数量" }],
  reason: [{ required: true, message: "请填写申请原因" }],
  costTargetId: [{ required: true, message: "请选择成本归属对象" }],
};
const importRules = {
  batchName: [{ required: true, message: "请填写申请批次名称" }],
  costTargetId: [{ required: true, message: "请选择成本归属对象" }],
};
const costTypeOptions = [
  { label: "项目采购", value: "PROJECT" },
  { label: "部门采购", value: "DEPARTMENT" },
];
const batchColumns = [
  { title: "申请批次", key: "batch", width: 240 },
  { title: "明细汇总", key: "summary", width: 150 },
  { title: "预计金额", key: "amount", width: 160 },
  { title: "成本归属", key: "target", width: 190 },
  { title: "到货日期", key: "date", width: 190 },
  { title: "审批状态", key: "approval", width: 140 },
  { title: "操作", key: "action", width: 220, fixed: "right" as const },
];
const lineColumns = [
  { title: "行号/申请号", key: "line", width: 160 },
  { title: "物料/说明", key: "material", width: 280 },
  { title: "数量", key: "qty", width: 90 },
  { title: "预计单价", key: "price", width: 120 },
  { title: "预计金额", key: "amount", width: 150 },
  { title: "到货日期", key: "date", width: 120 },
  { title: "审批", key: "approval", width: 100 },
  { title: "操作", key: "action", width: 120, fixed: "right" as const },
];
const approvalLineColumns = [
  { title: "行号", dataIndex: "lineNo", width: 70 },
  { title: "物料", dataIndex: "partName" },
  { title: "数量", dataIndex: "quantity", width: 90 },
  { title: "金额", dataIndex: "totalAmount", width: 120 },
];
const documentColumns = [
  { title: "文件", key: "file" },
  { title: "操作", key: "action", width: 130 },
];

const targetOptions = computed(() =>
  (form.costType === "PROJECT" ? projects.value : departments.value).map(
    (item) => ({
      label: `${item.name}${item.code ? " · " + item.code : ""}`,
      value: item.id,
    }),
  ),
);
const importTargetOptions = computed(() =>
  (importForm.costType === "PROJECT" ? projects.value : departments.value).map(
    (item) => ({
      label: `${item.name}${item.code ? " · " + item.code : ""}`,
      value: item.id,
    }),
  ),
);
const estimatedAmount = computed(
  () => Number(form.quantity || 0) * Number(form.unitPrice || 0),
);
const requestBatches = computed<RequestBatch[]>(() => {
  const groups = new Map<string, PurchaseRequest[]>();
  for (const item of requests.value) {
    const key = item.batchId || item.id;
    groups.set(key, [...(groups.get(key) || []), item]);
  }
  return Array.from(groups.entries()).map(([batchId, items]) => {
    const sorted = [...items].sort(
      (a, b) => Number(a.lineNo || 1) - Number(b.lineNo || 1),
    );
    const dates = sorted
      .map((item) => item.expectedDate || item.requiredDate)
      .filter(Boolean)
      .sort() as string[];
    const statuses = new Set(sorted.map((item) => item.approvalStatus));
    const pendingCount = sorted.filter(
      (item) => item.approvalStatus === "PENDING",
    ).length;
    const approvalStatus: ApprovalStatus =
      pendingCount > 0
        ? "PENDING"
        : sorted.every((item) => item.approvalStatus === "APPROVED")
          ? "APPROVED"
          : "REJECTED";
    return {
      batchId,
      batchCode: sorted[0].batchCode || sorted[0].code || "-",
      batchName: sorted[0].batchName || sorted[0].partName || "单项采购申请",
      items: sorted,
      itemCount: sorted.length,
      pendingCount,
      totalQuantity: sorted.reduce(
        (sum, item) => sum + Number(item.quantity || 0),
        0,
      ),
      totalAmount: sorted.reduce(
        (sum, item) =>
          sum +
          Number(
            item.totalAmount ||
              Number(item.unitPrice || 0) * Number(item.quantity || 0),
          ),
        0,
      ),
      costType: sorted[0].costType,
      costTargetName: sorted[0].costTargetName,
      requesterName: sorted[0].requesterName,
      dateRange:
        dates.length === 0
          ? "-"
          : dates[0] === dates[dates.length - 1]
            ? dates[0]
            : `${dates[0]} 至 ${dates[dates.length - 1]}`,
      approvalStatus,
      mixedApproval: statuses.size > 1,
    };
  });
});
const canApproveSelected = computed(
  () =>
    !!selectedBatch.value &&
    selectedBatch.value.pendingCount > 0 &&
    auth.can("procurement:request:approve"),
);

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    const [result, targets] = await Promise.all([
      listPurchaseRequests({ page: 0, size: 999 }),
      listProcurementCostTargets(),
    ]);
    requests.value = result.content;
    projects.value = targets.projects;
    departments.value = targets.departments;
  } catch (error: any) {
    message.error(error.message || "采购申请加载失败");
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  Object.assign(form, {
    materialName: "",
    materialSpec: "",
    quantity: 1,
    unit: "个",
    unitPrice: 0,
    taxRate: 13,
    requiredDate: "",
    reason: "",
    costType: "PROJECT",
    costTargetId: "",
  });
  createOpen.value = true;
}

function openImport() {
  Object.assign(importForm, {
    batchName: "",
    costType: "PROJECT",
    costTargetId: "",
    sharedReason: "",
  });
  importFile.value = null;
  importOpen.value = true;
}

async function handleCreate() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    const created = await createPurchaseRequest({
      requesterName: auth.user?.displayName || "",
      partName: form.materialName,
      quantity: form.quantity,
      unitPrice: form.unitPrice,
      taxRate: form.taxRate,
      expectedDate: form.requiredDate || undefined,
      reason: `${form.reason}${form.materialSpec ? "；规格：" + form.materialSpec : ""}${form.unit ? "；单位：" + form.unit : ""}`,
      costType: form.costType,
      projectId: form.costType === "PROJECT" ? form.costTargetId : undefined,
      departmentId:
        form.costType === "DEPARTMENT" ? form.costTargetId : undefined,
    });
    createOpen.value = false;
    message.success("申请已创建，可继续上传图纸或照片");
    await loadData();
    await openDocuments({
      bizType: "PURCHASE_REQUEST_BATCH",
      bizId: created.batchId || created.id,
      title: `整批附件 · ${created.batchCode || created.code}`,
    });
  } catch (error: any) {
    message.error(error.message || "创建失败");
  } finally {
    saving.value = false;
  }
}

function selectImportFile(file: File) {
  importFile.value = file;
  return false;
}

async function handleImport() {
  await importFormRef.value?.validate();
  if (!importFile.value) {
    message.warning("请选择要导入的 Excel 或 CSV 文件");
    return;
  }
  importing.value = true;
  try {
    const result = await importPurchaseRequestBatch({
      file: importFile.value,
      batchName: importForm.batchName,
      costType: importForm.costType,
      projectId:
        importForm.costType === "PROJECT" ? importForm.costTargetId : undefined,
      departmentId:
        importForm.costType === "DEPARTMENT"
          ? importForm.costTargetId
          : undefined,
      sharedReason: importForm.sharedReason || undefined,
    });
    importOpen.value = false;
    message.success(
      `${result.batchCode} 已创建：${result.totalLines}项，预计金额${formatMoney(result.totalAmount)}`,
    );
    await loadData();
    await openDocuments({
      bizType: "PURCHASE_REQUEST_BATCH",
      bizId: result.batchId,
      title: `整批附件 · ${result.batchCode}`,
    });
  } catch (error: any) {
    message.error(error.message || "批量导入失败");
  } finally {
    importing.value = false;
  }
}

function openApproval(batch: RequestBatch) {
  selectedBatch.value = batch;
  Object.assign(approvalForm, {
    decision: "APPROVED",
    comment: "同意整批采购",
    approverName: auth.user?.displayName || "",
  });
  approvalOpen.value = true;
}

async function handleApproval() {
  if (!selectedBatch.value) return;
  saving.value = true;
  try {
    await processPurchaseRequestBatchApproval(selectedBatch.value.batchId, {
      ...approvalForm,
    });
    approvalOpen.value = false;
    message.success(
      approvalForm.decision === "APPROVED"
        ? "采购申请批次已通过"
        : "采购申请批次已驳回",
    );
    await loadData();
  } catch (error: any) {
    message.error(error.message || "批次审批失败");
  } finally {
    saving.value = false;
  }
}

function openBatchDocuments(batch: RequestBatch) {
  return openDocuments({
    bizType: "PURCHASE_REQUEST_BATCH",
    bizId: batch.batchId,
    title: `整批附件 · ${batch.batchCode}`,
  });
}

function openLineDocuments(line: PurchaseRequest) {
  return openDocuments({
    bizType: "PURCHASE_REQUEST",
    bizId: line.id,
    title: `明细附件 · ${line.code} · ${line.partName}`,
  });
}

async function openDocuments(target: {
  bizType: string;
  bizId: string;
  title: string;
}) {
  attachmentTarget.value = target;
  documentOpen.value = true;
  await loadDocuments();
}

async function loadDocuments() {
  if (!attachmentTarget.value) return;
  documentsLoading.value = true;
  try {
    documents.value = await listDocumentsByBiz(
      attachmentTarget.value.bizType,
      attachmentTarget.value.bizId,
    );
  } catch (error: any) {
    message.error(error.message || "附件加载失败");
  } finally {
    documentsLoading.value = false;
  }
}

async function uploadAttachment(file: File) {
  if (!attachmentTarget.value) return false;
  try {
    await uploadDocument({
      bizType: attachmentTarget.value.bizType,
      bizId: attachmentTarget.value.bizId,
      file,
    });
    message.success(`${file.name} 已上传`);
    await loadDocuments();
  } catch (error: any) {
    message.error(error.message || "附件上传失败");
  }
  return false;
}

function formatMoney(value: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    minimumFractionDigits: 2,
  }).format(value || 0);
}

function formatQuantity(value: number) {
  return new Intl.NumberFormat("zh-CN", {
    maximumFractionDigits: 2,
  }).format(value || 0);
}

function formatTaxRate(value?: number) {
  return `${Number(value ?? 13)
    .toFixed(2)
    .replace(/\.?0+$/, "")}%`;
}

function formatFileSize(value: number) {
  if (value < 1024) return `${value} B`;
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`;
  return `${(value / 1024 / 1024).toFixed(1)} MB`;
}

function formatDateTime(value?: string) {
  return value ? new Date(value).toLocaleString("zh-CN") : "-";
}

function approvalColor(status: ApprovalStatus) {
  return { PENDING: "blue", APPROVED: "green", REJECTED: "red" }[status];
}

function approvalStatusLabel(status: ApprovalStatus) {
  return { PENDING: "待审批", APPROVED: "已通过", REJECTED: "已驳回" }[status];
}

function batchApprovalSteps(batch: RequestBatch): ApprovalProgressStep[] {
  const latest = batch.items.find((item) => item.lastApprovalAt);
  return [
    {
      key: "start",
      personName: batch.requesterName || "发起人",
      title: `发起批次（${batch.itemCount}项）`,
      note: `${batch.batchName}，预计金额${formatMoney(batch.totalAmount)}`,
      state: "done",
    },
    {
      key: "approval",
      personName: latest?.lastApproverName || "当前审批人",
      title:
        batch.approvalStatus === "PENDING"
          ? "待审批"
          : batch.approvalStatus === "APPROVED"
            ? "已同意"
            : "已驳回",
      time: latest?.lastApprovalAt,
      note:
        batch.approvalStatus === "PENDING"
          ? `等待审批，${batch.pendingCount}项待处理`
          : latest?.lastApprovalComment ||
            approvalStatusLabel(batch.approvalStatus),
      state:
        batch.approvalStatus === "PENDING"
          ? "pending"
          : batch.approvalStatus === "REJECTED"
            ? "rejected"
            : "done",
    },
    {
      key: "execute",
      personName: "采购执行",
      title: "待询价/下单",
      note: "审批通过的明细可分别进入询价、订单、到货、应付和付款闭环",
      state: batch.approvalStatus === "APPROVED" ? "pending" : "waiting",
    },
  ];
}
</script>

<style scoped>
.line-detail {
  padding: 8px 20px 16px 44px;
  background: #fafcff;
}

.attachment-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.muted-text {
  color: #8c8c8c;
}
</style>
