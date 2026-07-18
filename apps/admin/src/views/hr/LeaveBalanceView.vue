<template>
  <div class="page-stack">
    <a-card title="请假额度管理">
      <template #extra>
        <a-space>
          <a-button @click="loadData"
            ><template #icon><ReloadOutlined /></template>刷新</a-button
          >
          <a-button type="primary" @click="openBalanceForm">
            <template #icon><PlusOutlined /></template>设置额度
          </a-button>
        </a-space>
      </template>

      <a-table
        :loading="loading"
        :data-source="data"
        :columns="columns"
        row-key="id"
        :pagination="{ pageSize: 20 }"
        :expand-row-keys="expandedRows"
        @expand="onExpand"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'leaveType'">
            <a-tag :color="typeColor(record.leaveType)">{{
              typeLabel(record.leaveType)
            }}</a-tag>
          </template>
          <template v-else-if="column.key === 'remainingDays'">
            <a-progress
              :percent="calcPercent(record)"
              :format="() => `${record.remainingDays.toFixed(1)}天`"
              :size="'small'"
              :stroke-color="
                record.remainingDays < 2
                  ? '#ff4d4f'
                  : record.remainingDays < 5
                    ? '#faad14'
                    : '#52c41a'
              "
            />
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button type="text" size="small" @click="editBalance(record)"
                ><EditOutlined
              /></a-button>
              <a-popconfirm
                title="确认初始化该员工年度额度？"
                @confirm="initBalancesItem(record.employeeId)"
              >
                <a-button type="text" size="small" title="初始化额度"
                  ><SyncOutlined
                /></a-button>
              </a-popconfirm>
            </a-space>
          </template>
          <template v-else></template>
        </template>
        <template #expandedRowRender="{ record }">
          <div
            v-if="employeeBalances[record.employeeId]"
            class="expanded-balances"
          >
            <a-table
              size="small"
              :data-source="employeeBalances[record.employeeId]"
              :columns="innerColumns"
              row-key="id"
              :pagination="false"
            >
              <template #bodyCell="{ column: col, record: rec }">
                <template v-if="col.key === 'leaveType'">
                  <a-tag :color="typeColor(rec.leaveType)">{{
                    typeLabel(rec.leaveType)
                  }}</a-tag>
                </template>
                <template v-else-if="col.key === 'remainingDays'">
                  {{ rec.remainingDays.toFixed(1) }}天
                </template>
                <template v-else></template>
              </template>
            </a-table>
          </div>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="formModal"
      :title="formId ? '编辑额度' : '设置额度'"
      width="600px"
      :confirm-loading="saving"
      @ok="saveBalance"
    >
      <a-form ref="formRef" :model="formData" layout="vertical">
        <a-row :gutter="14">
          <a-col :span="12">
            <a-form-item label="员工" name="employeeId" :rules="requiredRule">
              <a-select
                v-model:value="formData.employeeId"
                show-search
                option-filter-prop="label"
                placeholder="搜索选择员工"
                :options="employeeOptions"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="假期类型"
              name="leaveType"
              :rules="requiredRule"
            >
              <a-select
                v-model:value="formData.leaveType"
                :options="leaveTypeOptions"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="14">
          <a-col :span="8">
            <a-form-item label="年度" name="year" :rules="requiredRule">
              <a-input-number
                v-model:value="formData.year"
                :min="2020"
                :max="2099"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item
              label="总额度(天)"
              name="totalDays"
              :rules="requiredRule"
            >
              <a-input-number
                v-model:value="formData.totalDays"
                :min="0"
                :step="0.5"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="已使用(天)">
              <a-input-number
                v-model:value="formData.usedDays"
                :min="0"
                :step="0.5"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="batchModal"
      title="批量初始化年度额度"
      width="500px"
      :confirm-loading="saving"
      @ok="batchInit"
    >
      <a-form layout="vertical">
        <a-form-item label="选择在职员工">
          <a-table
            size="small"
            :data-source="activeEmployees"
            :columns="selectColumns"
            row-key="id"
            :pagination="false"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'selected'">
                <a-checkbox
                  :checked="batchSelected.includes(record.id)"
                  @change="toggleBatchSelect(record.id)"
                />
              </template>
            </template>
          </a-table>
        </a-form-item>
        <p style="color: #65717e; font-size: 12px">
          将按照默认额度（年假15天、病假12天）为所选员工创建今年额度。
        </p>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="importModal"
      title="导入员工数据"
      width="600px"
      :confirm-loading="importing"
      @ok="doImport"
    >
      <a-form layout="vertical">
        <a-form-item label="选择Excel文件">
          <a-upload-dragger
            :before-upload="handleFileSelect"
            accept=".xlsx,.xls"
            :show-upload-list="false"
            :multiple="false"
          >
            <template #icon><InboxOutlined /></template>
            <p v-if="!importFile">点击或拖拽文件到此区域</p>
            <p v-else>{{ importFile.name }}</p>
          </a-upload-dragger>
        </a-form-item>
        <a-space>
          <a-button type="link" size="small" @click="downloadTemplate"
            ><DownloadOutlined />下载导入模板</a-button
          >
        </a-space>
      </a-form>
    </a-modal>

    <a-result
      v-if="importResult"
      status="success"
      :title="`导入完成：成功 ${importResult.success} 条，失败 ${importResult.fail} 条`"
    >
      <template #extra>
        <a-button
          @click="
            importResult = undefined;
            importModal = false;
          "
          >关闭</a-button
        >
        <a-button
          type="primary"
          @click="
            importResult = undefined;
            importModal = false;
            loadData();
          "
          >刷新数据</a-button
        >
      </template>
    </a-result>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import {
  PlusOutlined,
  EditOutlined,
  ReloadOutlined,
  SyncOutlined,
  DownloadOutlined,
  InboxOutlined,
} from "@ant-design/icons-vue";
import { useAuthStore } from "@/stores/auth";
import {
  listQualificationEmployees,
  type QualificationEmployee,
} from "@/api/qualification";
import {
  listAllLeaveBalances,
  setLeaveBalance,
  initLeaveBalances,
  downloadLeaveBalanceTemplate,
  importLeaveBalancesExcel,
  type LeaveBalanceRecord,
  type LeaveBalancePayload,
} from "@/api/hr";

const auth = useAuthStore();

const loading = ref(false);
const saving = ref(false);
const importing = ref(false);
const data = ref<LeaveBalanceRecord[]>([]);
const employees = ref<QualificationEmployee[]>([]);
const expandedRows = ref<string[]>([]);
const employeeBalances = ref<Record<string, LeaveBalanceRecord[]>>({});
const importFile = ref<File>();
const importResult = ref<{ success: number; fail: number; errors: string[] }>();
const importModal = ref(false);

const activeEmployees = computed(() =>
  employees.value.filter((e) => e.employmentStatus === "ACTIVE"),
);
const employeeOptions = computed(() =>
  activeEmployees.value.map((e) => ({
    label: `${e.name} (${e.workNo || "无工号"})`,
    value: e.id,
  })),
);

const typeColor = (t: string) => {
  const colors: Record<string, string> = {
    ANNUAL: "blue",
    SICK: "red",
    PERSONAL: "default",
    MARRIAGE: "pink",
    MATERNITY: "purple",
    COMPENSATORY: "cyan",
  };
  return colors[t] || "default";
};
const typeLabel = (t: string) => {
  const labels: Record<string, string> = {
    ANNUAL: "年假",
    SICK: "病假",
    PERSONAL: "事假",
    MARRIAGE: "婚假",
    MATERNITY: "产假",
    COMPENSATORY: "调休",
  };
  return labels[t] || t;
};

const leaveTypeOptions = [
  { value: "ANNUAL", label: "年假" },
  { value: "SICK", label: "病假" },
  { value: "PERSONAL", label: "事假" },
  { value: "COMPENSATORY", label: "调休" },
  { value: "MARRIAGE", label: "婚假" },
  { value: "MATERNITY", label: "产假" },
];

const calcPercent = (r: LeaveBalanceRecord) =>
  Math.round((r.remainingDays / Math.max(r.totalDays, 1)) * 100);

const columns = [
  { title: "员工", dataIndex: "employeeName", width: 140 },
  { title: "假期类型", key: "leaveType", width: 100 },
  { title: "年度", dataIndex: "year", width: 80 },
  { title: "总天数", dataIndex: "totalDays", width: 80 },
  { title: "已用", dataIndex: "usedDays", width: 80 },
  { title: "剩余", key: "remainingDays", width: 180 },
  { title: "操作", key: "actions", width: 100, fixed: "right" },
];

const innerColumns = [
  { title: "假期类型", key: "leaveType", width: 100 },
  { title: "年度", dataIndex: "year", width: 70 },
  { title: "总天数", dataIndex: "totalDays", width: 70 },
  { title: "已用", dataIndex: "usedDays", width: 70 },
  { title: "剩余", key: "remainingDays", width: 80 },
];

const selectColumns = [
  { title: "", key: "selected", width: 50 },
  { title: "姓名", dataIndex: "name", width: 120 },
  { title: "工号", dataIndex: "workNo", width: 100 },
  { title: "部门", dataIndex: "department", width: 120 },
];

const requiredRule = [{ required: true, message: "请填写" }];

const batchSelected = ref<string[]>([]);
const batchModal = ref(false);

// Form
const formModal = ref(false);
const formId = ref("");
const formRef = ref();
const formData = reactive<LeaveBalancePayload & { employeeId: string }>({
  employeeId: "",
  leaveType: "ANNUAL",
  year: new Date().getFullYear(),
  totalDays: 15,
  usedDays: 0,
});

function openBalanceForm() {
  formId.value = "";
  Object.assign(formData, {
    employeeId: "",
    leaveType: "ANNUAL",
    year: new Date().getFullYear(),
    totalDays: 15,
    usedDays: 0,
  });
  formModal.value = true;
}

function editBalance(record: LeaveBalanceRecord) {
  formId.value = record.id;
  Object.assign(formData, {
    employeeId: record.employeeId,
    leaveType: record.leaveType,
    year: record.year,
    totalDays: record.totalDays,
    usedDays: record.usedDays,
  });
  formModal.value = true;
}

async function saveBalance() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    await setLeaveBalance(formData.employeeId, {
      leaveType: formData.leaveType,
      year: formData.year,
      totalDays: formData.totalDays,
      usedDays: formData.usedDays,
    });
    formModal.value = false;
    message.success("额度已保存");
    await loadData();
  } catch (error: any) {
    message.error(error.message || "保存失败");
  } finally {
    saving.value = false;
  }
}

async function initBalancesItem(employeeId: string) {
  saving.value = true;
  try {
    await initLeaveBalances(employeeId);
    message.success("额度已初始化");
    await loadData();
  } catch (error: any) {
    message.error(error.message || "初始化失败");
  } finally {
    saving.value = false;
  }
}

function toggleBatchSelect(id: string) {
  const idx = batchSelected.value.indexOf(id);
  if (idx >= 0) batchSelected.value.splice(idx, 1);
  else batchSelected.value.push(id);
}

async function batchInit() {
  saving.value = true;
  try {
    for (const id of batchSelected.value) {
      await initLeaveBalances(id);
    }
    batchModal.value = false;
    message.success(`已为 ${batchSelected.value.length} 名员工初始化额度`);
    await loadData();
  } catch (error: any) {
    message.error(error.message || "批量初始化失败");
  } finally {
    saving.value = false;
  }
}

function onExpand(expanded: boolean, record: LeaveBalanceRecord) {
  if (expanded) {
    expandedRows.value = [record.employeeId];
    if (!employeeBalances.value[record.employeeId]) {
      loadAllBalancesForEmployee(record.employeeId);
    }
  } else {
    expandedRows.value = [];
  }
}

async function loadAllBalancesForEmployee(employeeId: string) {
  try {
    // We'll just re-filter from the existing data
    employeeBalances.value[employeeId] = data.value.filter(
      (d) => d.employeeId === employeeId,
    );
  } catch {}
}

// Import
function handleFileSelect(file: File) {
  importFile.value = file;
  return false;
}

async function doImport() {
  if (!importFile.value) {
    message.warning("请选择文件");
    return;
  }
  importing.value = true;
  try {
    const result = await importLeaveBalancesExcel(
      importFile.value,
      auth.user?.displayName || "系统用户",
    );
    importResult.value = result;
  } catch (error: any) {
    message.error(error.message || "导入失败");
  } finally {
    importing.value = false;
  }
}

async function downloadTemplate() {
  try {
    await downloadLeaveBalanceTemplate();
  } catch (error: any) {
    message.error(error.message || "下载失败");
  }
}

async function loadData() {
  loading.value = true;
  try {
    const [balances, emps] = await Promise.all([
      listAllLeaveBalances(),
      listQualificationEmployees({}),
    ]);
    data.value = balances;
    employees.value = emps;
  } catch (error: any) {
    message.error(error.message || "加载失败");
  } finally {
    loading.value = false;
  }
}

onMounted(loadData);
</script>

<style scoped>
.expanded-balances {
  padding: 8px 0;
}
</style>
