<template>
  <div class="page-stack">
    <a-card>
      <template #title>费用报销</template>
      <template #extra>
        <a-space>
          <a-button @click="goBack">返回办公室</a-button>
          <a-button :loading="loading" @click="loadData"
            ><template #icon><ReloadOutlined /></template>刷新</a-button
          >
        </a-space>
      </template>

      <a-space wrap class="table-toolbar">
        <a-button
          v-if="auth.can('office:expense:create')"
          type="primary"
          @click="openExpense"
        >
          <template #icon><PlusOutlined /></template>新增报销
        </a-button>
      </a-space>

      <a-table
        :columns="expenseColumns"
        :data-source="expenses"
        :loading="loading"
        :pagination="{ pageSize: 10 }"
        :row-key="(r: Expense) => r.id"
        :scroll="{ x: 1260 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'expense'">
            <strong>{{ record.code }}</strong>
            <span class="table-subtitle">{{ record.description }}</span>
          </template>
          <template v-else-if="column.key === 'binding'">{{
            record.projectCode || record.workOrderCode || "管理费用"
          }}</template>
          <template v-else-if="column.key === 'type'">{{
            expenseTypeLabel(record.expenseType)
          }}</template>
          <template v-else-if="column.key === 'lines'"
            >{{ record.lines?.length || 1 }} 行</template
          >
          <template v-else-if="column.key === 'amount'"
            ><strong>{{ formatMoney(record.amount) }}</strong></template
          >
          <template v-else-if="column.key === 'status'"
            ><a-tag :color="expenseStatusColor(record.status)">{{
              expenseStatusLabel(record.status)
            }}</a-tag></template
          >
          <template v-else-if="column.key === 'action'">
            <a-button
              v-if="record.approvalRequestId"
              type="link"
              size="small"
              @click="openApproval(record)"
              >查看/审批</a-button
            >
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
      v-model:open="expenseOpen"
      title="新增费用报销"
      width="980px"
      :confirm-loading="saving"
      @ok="handleExpense"
    >
      <a-form
        ref="expenseFormRef"
        :model="expenseForm"
        :rules="expenseRules"
        layout="vertical"
      >
        <a-row :gutter="16">
          <a-col :xs="24" :md="12">
            <a-form-item label="报销人" name="claimantId">
              <a-select
                v-model:value="expenseForm.claimantId"
                show-search
                option-filter-prop="label"
                :options="userOptions"
                @change="onClaimantChange"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="绑定项目">
              <a-select
                v-model:value="expenseForm.projectId"
                allow-clear
                show-search
                option-filter-prop="label"
                :options="projectOptions"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="绑定工单">
              <a-select
                v-model:value="expenseForm.workOrderId"
                allow-clear
                show-search
                option-filter-prop="label"
                :options="workOrderOptions"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="报销总额">
              <a-input-number
                :value="expenseTotal"
                disabled
                :precision="2"
                class="full-input"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="报销明细">
          <a-table
            :columns="lineColumns"
            :data-source="expenseForm.lines"
            :pagination="false"
            :row-key="(row: ExpenseDraftLine) => row.rowKey"
            size="small"
            :scroll="{ x: 980 }"
          >
            <template #bodyCell="{ column, record, index }">
              <template v-if="column.key === 'expenseType'">
                <a-select
                  v-model:value="record.expenseType"
                  :options="expenseTypeOptions"
                  style="width: 120px"
                />
              </template>
              <template v-else-if="column.key === 'expenseDate'">
                <a-input
                  v-model:value="record.expenseDate"
                  type="date"
                  style="width: 138px"
                />
              </template>
              <template v-else-if="column.key === 'amount'">
                <a-input-number
                  v-model:value="record.amount"
                  :min="0"
                  :precision="2"
                  style="width: 120px"
                />
              </template>
              <template v-else-if="column.key === 'description'">
                <a-input
                  v-model:value="record.description"
                  placeholder="费用说明"
                />
              </template>
              <template v-else-if="column.key === 'invoice'">
                <a-upload
                  :before-upload="(file: File) => bindInvoiceFile(record, file)"
                  :show-upload-list="false"
                  accept=".jpg,.jpeg,.png,.pdf,.ofd"
                >
                  <a-button size="small">上传发票</a-button>
                </a-upload>
                <span v-if="record.invoiceFileName" class="table-subtitle">{{
                  record.invoiceFileName
                }}</span>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button
                  type="link"
                  danger
                  size="small"
                  :disabled="expenseForm.lines.length === 1"
                  @click="removeLine(index)"
                  >删除</a-button
                >
              </template>
            </template>
          </a-table>
          <a-button type="dashed" block style="margin-top: 8px" @click="addLine"
            >新增一行</a-button
          >
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { message } from "ant-design-vue";
import PlusOutlined from "@ant-design/icons-vue/PlusOutlined";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import {
  createExpense,
  getOfficeReferences,
  listExpenses,
  uploadDocument,
  type Expense,
  type ExpenseLine,
  type ExpenseStatus,
  type ExpenseType,
} from "@/api/office";
import { useAuthStore } from "@/stores/auth";
import ApprovalCenterView from "@/views/office/ApprovalCenterView.vue";

type ExpenseDraftLine = ExpenseLine & { rowKey: string; invoiceFile?: File };

const auth = useAuthStore();
const router = useRouter();
const loading = ref(false);
const saving = ref(false);
const expenses = ref<Expense[]>([]);
const references = reactive({ users: [], projects: [], workOrders: [] } as any);
const approvalCenterRef = ref<InstanceType<typeof ApprovalCenterView>>();
const expenseOpen = ref(false);
const expenseFormRef = ref();
const expenseForm = reactive({
  code: "",
  claimantId: undefined as string | undefined,
  claimantName: "",
  projectId: undefined as string | undefined,
  workOrderId: undefined as string | undefined,
  lines: [] as ExpenseDraftLine[],
});

const expenseColumns = [
  { title: "报销单", key: "expense", width: 280 },
  { title: "报销人", dataIndex: "claimantName", width: 130 },
  { title: "主类型", key: "type", width: 110 },
  { title: "明细", key: "lines", width: 90 },
  { title: "绑定业务", key: "binding", width: 180 },
  { title: "发生日期", dataIndex: "expenseDate", width: 120 },
  { title: "金额", key: "amount", width: 140 },
  { title: "状态", key: "status", width: 130 },
  { title: "审批", key: "action", width: 120, fixed: "right" as const },
];
const lineColumns = [
  { title: "费用类型", key: "expenseType", width: 130 },
  { title: "发生日期", key: "expenseDate", width: 150 },
  { title: "金额", key: "amount", width: 130 },
  { title: "说明", key: "description", width: 260 },
  { title: "发票", key: "invoice", width: 220 },
  { title: "操作", key: "action", width: 80, fixed: "right" as const },
];
const expenseRules = {
  claimantId: [{ required: true, message: "请选择报销人" }],
};
const userOptions = computed(() =>
  references.users
    .filter((i: any) => i.enabled)
    .map((i: any) => ({ label: i.displayName, value: i.id })),
);
const projectOptions = computed(() =>
  references.projects.map((i: any) => ({
    label: i.code + " · " + i.name,
    value: i.id,
  })),
);
const workOrderOptions = computed(() =>
  references.workOrders.map((i: any) => ({
    label: i.code + " · " + i.title,
    value: i.id,
  })),
);
const expenseTypeOptions = [
  { label: "差旅", value: "TRAVEL" },
  { label: "交通", value: "TRANSPORT" },
  { label: "住宿", value: "ACCOMMODATION" },
  { label: "工具采购", value: "TOOL" },
  { label: "其他", value: "OTHER" },
];
const expenseTotal = computed(() =>
  expenseForm.lines.reduce((sum, line) => sum + Number(line.amount || 0), 0),
);

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    const [expenseData, referenceData] = await Promise.all([
      listExpenses(),
      getOfficeReferences(),
    ]);
    expenses.value = expenseData || [];
    references.users = referenceData.users || [];
    references.projects = referenceData.projects || [];
    references.workOrders = referenceData.workOrders || [];
  } catch (error) {
    message.error(error instanceof Error ? error.message : "数据加载失败");
  } finally {
    loading.value = false;
  }
}
function goBack() {
  router.push("/office");
}
function openExpense() {
  Object.assign(expenseForm, {
    code: generateCode("BX"),
    claimantId: auth.user?.id,
    claimantName: auth.user?.displayName || "",
    projectId: undefined,
    workOrderId: undefined,
    lines: [newLine()],
  });
  expenseOpen.value = true;
}
function onClaimantChange(id: string) {
  expenseForm.claimantName =
    references.users.find((i: any) => i.id === id)?.displayName || "";
}
function newLine(): ExpenseDraftLine {
  return {
    rowKey: `${Date.now()}-${Math.random()}`,
    expenseType: "TRAVEL",
    amount: 0,
    expenseDate: today(),
    description: "",
  };
}
function addLine() {
  expenseForm.lines.push(newLine());
}
function removeLine(index: number) {
  expenseForm.lines.splice(index, 1);
}
function bindInvoiceFile(record: ExpenseDraftLine, file: File) {
  record.invoiceFile = file;
  record.invoiceFileName = file.name;
  record.invoiceContentType = file.type || "application/octet-stream";
  record.invoiceSizeBytes = file.size;
  return false;
}
async function handleExpense() {
  await expenseFormRef.value?.validate();
  const draftLines = expenseForm.lines.filter(
    (line) => line.amount > 0 && line.expenseDate && line.description.trim(),
  );
  const lines = draftLines.map(({ rowKey, invoiceFile, ...line }) => line);
  if (!lines.length) {
    message.warning("请至少填写一条完整报销明细");
    return;
  }
  saving.value = true;
  try {
    const created = await createExpense({
      code: expenseForm.code,
      claimantId: expenseForm.claimantId,
      claimantName: expenseForm.claimantName,
      projectId: expenseForm.projectId,
      workOrderId: expenseForm.workOrderId,
      lines,
    });
    await uploadInvoiceFiles(created, draftLines);
    expenseOpen.value = false;
    message.success("报销单已提交审批");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "报销提交失败");
  } finally {
    saving.value = false;
  }
}
async function uploadInvoiceFiles(
  created: Expense,
  draftLines: ExpenseDraftLine[],
) {
  const tasks = draftLines
    .map((line, index) => ({
      file: line.invoiceFile,
      responseLine: created.lines?.[index],
    }))
    .filter((item): item is { file: File; responseLine: ExpenseLine } =>
      Boolean(item.file && item.responseLine?.id),
    );
  if (!tasks.length) return;
  const results = await Promise.allSettled(
    tasks.map((item) =>
      uploadDocument({
        bizType: "EXPENSE_INVOICE",
        bizId: item.responseLine.id,
        file: item.file,
      }),
    ),
  );
  const failed = results.filter((item) => item.status === "rejected").length;
  if (failed)
    message.warning(
      `${failed} 个发票文件上传失败，报销明细已保存，可稍后补传。`,
    );
}
function openApproval(record: Expense) {
  approvalCenterRef.value?.openApprovalById(record.approvalRequestId);
}
function generateCode(prefix: string) {
  const d = new Date();
  return (
    prefix +
    "-" +
    d.getFullYear() +
    String(d.getMonth() + 1).padStart(2, "0") +
    String(d.getDate()).padStart(2, "0") +
    "-" +
    String(d.getHours()).padStart(2, "0") +
    String(d.getMinutes()).padStart(2, "0")
  );
}
function today() {
  const d = new Date();
  return (
    d.getFullYear() +
    "-" +
    String(d.getMonth() + 1).padStart(2, "0") +
    "-" +
    String(d.getDate()).padStart(2, "0")
  );
}
function expenseTypeLabel(v: ExpenseType) {
  return (
    {
      TRAVEL: "差旅",
      TRANSPORT: "交通",
      ACCOMMODATION: "住宿",
      TOOL: "工具采购",
      OTHER: "其他",
    } as Record<ExpenseType, string>
  )[v];
}
function expenseStatusLabel(v: ExpenseStatus) {
  return (
    {
      PENDING_APPROVAL: "待审批",
      APPROVED: "已通过",
      REJECTED: "已驳回",
      PAID: "已付款",
    } as Record<ExpenseStatus, string>
  )[v];
}
function expenseStatusColor(v: ExpenseStatus) {
  return (
    {
      PENDING_APPROVAL: "orange",
      APPROVED: "blue",
      REJECTED: "red",
      PAID: "green",
    } as Record<ExpenseStatus, string>
  )[v];
}
function formatMoney(v: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    minimumFractionDigits: 2,
  }).format(v || 0);
}
</script>
