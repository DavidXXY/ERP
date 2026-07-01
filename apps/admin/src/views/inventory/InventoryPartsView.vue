<template>
  <div class="page-stack">
    <a-card title="库存管理">
      <template #extra>
        <a-button :loading="loading" @click="loadData">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </template>

      <a-alert v-if="errorMessage" class="section-alert" type="warning" show-icon :message="errorMessage" />

      <a-row :gutter="[16, 16]" class="metric-row">
        <a-col :xs="12" :lg="6"><a-statistic title="物料品种" :value="parts.length" suffix="种" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="库存金额" :value="inventoryValue" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="低库存" :value="lowStockCount" suffix="种" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="项目材料净耗用" :value="netIssueAmount" :formatter="moneyFormatter" /></a-col>
      </a-row>

      <a-tabs v-model:active-key="activeTab">
        <a-tab-pane key="parts" tab="库存台账">
          <div class="table-toolbar">
            <a-button v-if="auth.can('inventory:part:create')" type="primary" @click="openPart">
              <template #icon><PlusOutlined /></template>
              新增物料
            </a-button>
          </div>
          <a-table :columns="partColumns" :data-source="parts" :loading="loading" :pagination="{ pageSize: 8 }" :row-key="(record: InventoryPart) => record.id" :scroll="{ x: 980 }" size="middle">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                <strong>{{ record.name }}</strong>
                <span class="table-subtitle">{{ record.code }} · {{ record.model || '未填型号' }}</span>
              </template>
              <template v-else-if="column.key === 'stock'">
                <strong>{{ formatQuantity(record.stockQty) }}</strong>
                <span class="table-subtitle">安全库存 {{ formatQuantity(record.safetyQty) }}</span>
              </template>
              <template v-else-if="column.key === 'status'"><a-tag :color="record.lowStock ? 'red' : 'green'">{{ record.lowStock ? '低库存' : '正常' }}</a-tag></template>
              <template v-else-if="column.key === 'cost'">{{ formatMoney(record.unitCost) }}</template>
              <template v-else-if="column.key === 'value'"><strong>{{ formatMoney(record.stockQty * record.unitCost) }}</strong></template>
              <template v-else-if="column.key === 'action'">
                <a-space size="small">
                  <a-button v-if="auth.can('inventory:movement:create')" type="link" size="small" @click="openMovement(record)">库存调整</a-button>
                  <a-button type="link" size="small" @click="openMovements(record)">流水</a-button>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="issues" tab="项目领料">
          <div class="table-toolbar">
            <a-button v-if="auth.can('inventory:issue:create')" type="primary" @click="openIssue">
              <template #icon><ExportOutlined /></template>
              新增领料单
            </a-button>
          </div>
          <a-table :columns="issueColumns" :data-source="issues" :loading="loading" :pagination="{ pageSize: 8 }" :row-key="(record: MaterialIssue) => record.id" :scroll="{ x: 1250 }" size="middle">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'code'"><strong>{{ record.code }}</strong><span class="table-subtitle">{{ record.issueDate }} · {{ record.receiverName }}</span></template>
              <template v-else-if="column.key === 'project'">{{ record.projectName }}<span class="table-subtitle">{{ record.projectCode }}</span></template>
              <template v-else-if="column.key === 'lines'">{{ issueLineSummary(record) }}<span class="table-subtitle">共 {{ record.lines.length }} 项</span></template>
              <template v-else-if="column.key === 'amount'"><strong>{{ formatMoney(record.totalAmount) }}</strong></template>
              <template v-else-if="column.key === 'status'"><a-tag :color="issueStatusColor(record.status)">{{ issueStatusLabel(record.status) }}</a-tag></template>
              <template v-else-if="column.key === 'action'">
                <a-button v-if="auth.can('inventory:return:create') && returnableLines(record).length" type="link" size="small" @click="openReturn(record)">办理退料</a-button>
                <span v-else class="muted">已无可退数量</span>
              </template>
            </template>
            <template #emptyText>暂无项目领料单</template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="returns" tab="项目退料">
          <a-table :columns="returnColumns" :data-source="returns" :loading="loading" :pagination="{ pageSize: 8 }" :row-key="(record: MaterialReturn) => record.id" :scroll="{ x: 1050 }" size="middle">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'code'"><strong>{{ record.code }}</strong><span class="table-subtitle">原领料单 {{ record.issueCode }}</span></template>
              <template v-else-if="column.key === 'project'">{{ record.projectName }}<span class="table-subtitle">{{ record.projectCode }}</span></template>
              <template v-else-if="column.key === 'lines'">{{ returnLineSummary(record) }}</template>
              <template v-else-if="column.key === 'amount'"><strong>{{ formatMoney(record.totalAmount) }}</strong></template>
            </template>
            <template #emptyText>暂无项目退料单</template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-drawer v-model:open="movementDrawerOpen" :width="760" :title="selectedPart ? `${selectedPart.name} 库存流水` : '库存流水'">
      <template #extra><a-tag v-if="selectedPart" :color="selectedPart.lowStock ? 'red' : 'green'">当前库存 {{ formatQuantity(selectedPart.stockQty) }}</a-tag></template>
      <a-table :columns="movementColumns" :data-source="movements" :loading="movementLoading" :pagination="{ pageSize: 10 }" :row-key="(record: StockMovement) => record.id" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'"><a-tag :color="movementColor(record.movementType)">{{ movementLabel(record.movementType) }}</a-tag></template>
          <template v-else-if="column.key === 'createdAt'">{{ formatDateTime(record.createdAt) }}</template>
        </template>
      </a-table>
    </a-drawer>

    <a-modal v-model:open="partOpen" title="新增物料" width="720px" :confirm-loading="saving" @ok="handleCreatePart">
      <a-form ref="partFormRef" :model="partForm" :rules="partRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="16"><a-form-item label="物料名称" name="name"><a-input v-model:value="partForm.name" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="规格型号"><a-input v-model:value="partForm.model" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="库位"><a-input v-model:value="partForm.location" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="初始库存"><a-input-number v-model:value="partForm.stockQty" :min="0" :precision="2" class="full-input" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="安全库存"><a-input-number v-model:value="partForm.safetyQty" :min="0" :precision="2" class="full-input" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="单位成本"><a-input-number v-model:value="partForm.unitCost" :min="0" :precision="2" class="full-input" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal v-model:open="movementOpen" :title="movementPart ? `${movementPart.name} 库存调整` : '库存调整'" width="640px" :confirm-loading="saving" @ok="handleCreateMovement">
      <a-form ref="movementFormRef" :model="movementForm" :rules="movementRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="12"><a-form-item label="调整类型" name="movementType"><a-select v-model:value="movementForm.movementType" :options="manualMovementOptions" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="数量" name="quantity"><a-input-number v-model:value="movementForm.quantity" :min="0.01" :precision="2" class="full-input" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="来源单号"><a-input v-model:value="movementForm.sourceNo" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="备注"><a-input v-model:value="movementForm.remark" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal v-model:open="issueOpen" title="新增项目领料单" width="880px" :confirm-loading="saving" @ok="handleCreateIssue">
      <a-form ref="issueFormRef" :model="issueForm" :rules="issueRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="10"><a-form-item label="关联项目" name="projectId"><a-select v-model:value="issueForm.projectId" :options="projectOptions" show-search option-filter-prop="label" /></a-form-item></a-col>
          <a-col :xs="24" :md="6"><a-form-item label="领料日期" name="issueDate"><a-input v-model:value="issueForm.issueDate" type="date" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="领用人" name="receiverName"><a-input v-model:value="issueForm.receiverName" /></a-form-item></a-col>
          <a-col :xs="24" :md="16"><a-form-item label="领料用途" name="purpose"><a-input v-model:value="issueForm.purpose" /></a-form-item></a-col>
        </a-row>
        <a-divider>领料明细 · {{ formatMoney(issueTotal) }}</a-divider>
        <div v-for="(line, index) in issueForm.lines" :key="line.key" class="issue-line">
          <a-row :gutter="12" align="middle">
            <a-col :span="11"><a-select v-model:value="line.partId" :options="partOptions" show-search option-filter-prop="label" class="full-input" placeholder="选择物料" /></a-col>
            <a-col :span="5"><a-input-number v-model:value="line.quantity" :min="0.01" :precision="2" class="full-input" /></a-col>
            <a-col :span="5"><span>{{ formatMoney(lineAmount(line)) }}</span></a-col>
            <a-col :span="3"><a-button danger type="text" :disabled="issueForm.lines.length === 1" @click="removeIssueLine(index)">移除</a-button></a-col>
          </a-row>
        </div>
        <a-button type="dashed" block @click="addIssueLine"><template #icon><PlusOutlined /></template>增加物料明细</a-button>
      </a-form>
    </a-modal>

    <a-modal v-model:open="returnOpen" title="办理项目退料" width="820px" :confirm-loading="saving" @ok="handleCreateReturn">
      <a-alert v-if="activeIssue" class="section-alert" type="info" :message="`${activeIssue.code} · ${activeIssue.projectName} · 可退 ${returnableLines(activeIssue).length} 项`" />
      <a-form ref="returnFormRef" :model="returnForm" :rules="returnRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="8"><a-form-item label="退料日期" name="returnDate"><a-input v-model:value="returnForm.returnDate" type="date" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="经办人" name="handlerName"><a-input v-model:value="returnForm.handlerName" /></a-form-item></a-col>
        </a-row>
        <a-table :columns="returnLineColumns" :data-source="returnForm.lines" :pagination="false" :row-key="(line: ReturnFormLine) => line.issueLineId" size="small">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'part'">{{ record.partName }}<span class="table-subtitle">单价 {{ formatMoney(record.unitCost) }}</span></template>
            <template v-else-if="column.key === 'returnable'">{{ formatQuantity(record.returnableQty) }}</template>
            <template v-else-if="column.key === 'quantity'"><a-input-number v-model:value="record.quantity" :min="0" :max="record.returnableQty" :precision="2" class="full-input" /></template>
            <template v-else-if="column.key === 'amount'">{{ formatMoney(record.quantity * record.unitCost) }}</template>
          </template>
        </a-table>
        <a-divider>本次冲回项目成本 {{ formatMoney(returnTotal) }}</a-divider>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import ExportOutlined from "@ant-design/icons-vue/ExportOutlined";
import PlusOutlined from "@ant-design/icons-vue/PlusOutlined";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import {
  createInventoryPart,
  createMaterialIssue,
  createMaterialReturn,
  createStockMovement,
  listInventoryParts,
  listInventoryProjectOptions,
  listMaterialIssues,
  listMaterialReturns,
  listStockMovements,
  type CreateInventoryPartPayload,
  type CreateStockMovementPayload,
  type InventoryIssueStatus,
  type InventoryPart,
  type InventoryProjectOption,
  type MaterialIssue,
  type MaterialReturn,
  type StockMovement,
  type StockMovementType,
} from "@/api/core-business";
import { useAuthStore } from "@/stores/auth";

type IssueFormLine = { key: number; partId: string; quantity: number };
type IssueForm = { code: string; projectId: string; issueDate: string; receiverName: string; purpose: string; lines: IssueFormLine[] };
type ReturnFormLine = { issueLineId: string; partName: string; returnableQty: number; unitCost: number; quantity: number };
type ReturnForm = { code: string; returnDate: string; handlerName: string; lines: ReturnFormLine[] };

const auth = useAuthStore();
const activeTab = ref("parts");
const parts = ref<InventoryPart[]>([]);
const issues = ref<MaterialIssue[]>([]);
const returns = ref<MaterialReturn[]>([]);
const eligibleProjects = ref<InventoryProjectOption[]>([]);
const movements = ref<StockMovement[]>([]);
const selectedPart = ref<InventoryPart | null>(null);
const movementPart = ref<InventoryPart | null>(null);
const activeIssue = ref<MaterialIssue | null>(null);
const loading = ref(false);
const movementLoading = ref(false);
const saving = ref(false);
const errorMessage = ref("");
const partOpen = ref(false);
const movementOpen = ref(false);
const movementDrawerOpen = ref(false);
const issueOpen = ref(false);
const returnOpen = ref(false);
const partFormRef = ref();
const movementFormRef = ref();
const issueFormRef = ref();
const returnFormRef = ref();
let lineKey = 1;
const partForm = reactive<CreateInventoryPartPayload>(initialPartForm());
const movementForm = reactive<CreateStockMovementPayload>(initialMovementForm());
const issueForm = reactive<IssueForm>(initialIssueForm());
const returnForm = reactive<ReturnForm>(initialReturnForm());

const partColumns = [
  { title: "物料", key: "name", width: 260 }, { title: "库存", key: "stock", width: 130 },
  { title: "状态", key: "status", width: 100 }, { title: "库位", dataIndex: "location", width: 110 },
  { title: "单位成本", key: "cost", width: 130 }, { title: "库存金额", key: "value", width: 140 },
  { title: "操作", key: "action", width: 170, fixed: "right" },
];
const issueColumns = [
  { title: "领料单", key: "code", width: 210 }, { title: "项目", key: "project", width: 240 },
  { title: "用途", dataIndex: "purpose", width: 220 }, { title: "领料明细", key: "lines", width: 260 },
  { title: "材料成本", key: "amount", width: 140 }, { title: "状态", key: "status", width: 120 },
  { title: "操作", key: "action", width: 120, fixed: "right" },
];
const returnColumns = [
  { title: "退料单", key: "code", width: 240 }, { title: "项目", key: "project", width: 240 },
  { title: "退料明细", key: "lines", width: 280 }, { title: "退料日期", dataIndex: "returnDate", width: 120 },
  { title: "经办人", dataIndex: "handlerName", width: 120 }, { title: "冲回成本", key: "amount", width: 140 },
];
const movementColumns = [
  { title: "类型", key: "type", width: 100 }, { title: "数量", dataIndex: "quantity", width: 100 },
  { title: "来源单号", dataIndex: "sourceNo", width: 150 }, { title: "备注", dataIndex: "remark" },
  { title: "时间", key: "createdAt", width: 170 },
];
const returnLineColumns = [
  { title: "物料", key: "part", width: 260 }, { title: "可退数量", key: "returnable", width: 110 },
  { title: "本次退料", key: "quantity", width: 150 }, { title: "冲回成本", key: "amount", width: 140 },
];
const manualMovementOptions = [
  { label: "其他入库", value: "INBOUND" }, { label: "报废出库", value: "SCRAP" }, { label: "盘盈调整", value: "ADJUSTMENT" },
];
const partRules = { code: [], name: [{ required: true, message: "请输入物料名称" }] };
const movementRules = { movementType: [{ required: true }], quantity: [{ required: true, message: "请输入数量" }] };
const issueRules = {
  code: [], projectId: [{ required: true, message: "请选择项目" }],
  issueDate: [{ required: true }], receiverName: [{ required: true, message: "请输入领用人" }], purpose: [{ required: true, message: "请输入领料用途" }],
};
const returnRules = { code: [], returnDate: [{ required: true }], handlerName: [{ required: true, message: "请输入经办人" }] };

const inventoryValue = computed(() => parts.value.reduce((sum, item) => sum + Number(item.stockQty) * Number(item.unitCost), 0));
const lowStockCount = computed(() => parts.value.filter((item) => item.lowStock).length);
const netIssueAmount = computed(() => issues.value.reduce((sum, item) => sum + Number(item.totalAmount), 0) - returns.value.reduce((sum, item) => sum + Number(item.totalAmount), 0));
const partOptions = computed(() => parts.value.map((item) => ({ label: `${item.name} (${item.code}) · 库存 ${formatQuantity(item.stockQty)}`, value: item.id })));
const projectOptions = computed(() => eligibleProjects.value.map((item) => ({ label: `${item.code} · ${item.name} · ${item.managerName}`, value: item.id })));
const issueTotal = computed(() => issueForm.lines.reduce((sum, item) => sum + lineAmount(item), 0));
const returnTotal = computed(() => returnForm.lines.reduce((sum, item) => sum + Number(item.quantity || 0) * Number(item.unitCost || 0), 0));

onMounted(loadData);

async function loadData() {
  loading.value = true; errorMessage.value = "";
  try {
    [parts.value, issues.value, returns.value, eligibleProjects.value] = await Promise.all([
      listInventoryParts(), listMaterialIssues(), listMaterialReturns(), listInventoryProjectOptions(),
    ]);
    if (selectedPart.value) selectedPart.value = parts.value.find((item) => item.id === selectedPart.value?.id) || null;
  } catch (error) { errorMessage.value = error instanceof Error ? error.message : "仓储数据加载失败"; }
  finally { loading.value = false; }
}
function openPart() { Object.assign(partForm, initialPartForm()); partOpen.value = true; }
function openMovement(part: InventoryPart) { movementPart.value = part; Object.assign(movementForm, initialMovementForm()); movementOpen.value = true; }
async function openMovements(part: InventoryPart) {
  selectedPart.value = part; movementDrawerOpen.value = true; movementLoading.value = true;
  try { movements.value = await listStockMovements(part.id); }
  catch (error) { message.error(error instanceof Error ? error.message : "库存流水加载失败"); }
  finally { movementLoading.value = false; }
}
function openIssue() {
  if (!eligibleProjects.value.length) { message.warning("暂无审批通过且未关闭的项目"); return; }
  Object.assign(issueForm, initialIssueForm()); issueOpen.value = true;
}
function openReturn(issue: MaterialIssue) {
  activeIssue.value = issue;
  Object.assign(returnForm, initialReturnForm(), {
    lines: returnableLines(issue).map((line) => ({ issueLineId: line.id, partName: line.partName, returnableQty: Number(line.returnableQty), unitCost: Number(line.unitCost), quantity: 0 })),
  });
  returnOpen.value = true;
}
function addIssueLine() { issueForm.lines.push({ key: ++lineKey, partId: "", quantity: 1 }); }
function removeIssueLine(index: number) { if (issueForm.lines.length > 1) issueForm.lines.splice(index, 1); }

async function handleCreatePart() {
  await partFormRef.value?.validate(); saving.value = true;
  try { await createInventoryPart({ ...partForm }); partOpen.value = false; message.success("物料已新增"); await loadData(); }
  catch (error) { message.error(error instanceof Error ? error.message : "物料新增失败"); }
  finally { saving.value = false; }
}
async function handleCreateMovement() {
  if (!movementPart.value) return;
  await movementFormRef.value?.validate(); saving.value = true;
  try { await createStockMovement(movementPart.value.id, { ...movementForm }); movementOpen.value = false; message.success("库存调整已完成"); await loadData(); }
  catch (error) { message.error(error instanceof Error ? error.message : "库存调整失败"); }
  finally { saving.value = false; }
}
async function handleCreateIssue() {
  await issueFormRef.value?.validate();
  if (issueForm.lines.some((line) => !line.partId || line.quantity <= 0)) { message.warning("请完整填写领料明细"); return; }
  if (new Set(issueForm.lines.map((line) => line.partId)).size !== issueForm.lines.length) { message.warning("同一物料不能重复添加"); return; }
  saving.value = true;
  try {
    await createMaterialIssue({ code: issueForm.code, projectId: issueForm.projectId, issueDate: issueForm.issueDate, receiverName: issueForm.receiverName, purpose: issueForm.purpose, lines: issueForm.lines.map((line) => ({ partId: line.partId, quantity: line.quantity })) });
    issueOpen.value = false; activeTab.value = "issues"; message.success("领料完成，库存和项目材料成本已同步"); await loadData();
  } catch (error) { message.error(error instanceof Error ? error.message : "项目领料失败"); }
  finally { saving.value = false; }
}
async function handleCreateReturn() {
  if (!activeIssue.value) return;
  await returnFormRef.value?.validate();
  const lines = returnForm.lines.filter((line) => Number(line.quantity) > 0);
  if (!lines.length) { message.warning("请填写至少一项退料数量"); return; }
  saving.value = true;
  try {
    await createMaterialReturn(activeIssue.value.id, { code: returnForm.code, returnDate: returnForm.returnDate, handlerName: returnForm.handlerName, lines: lines.map((line) => ({ issueLineId: line.issueLineId, quantity: line.quantity })) });
    returnOpen.value = false; activeTab.value = "returns"; message.success("退料完成，库存已恢复并冲回项目材料成本"); await loadData();
  } catch (error) { message.error(error instanceof Error ? error.message : "项目退料失败"); }
  finally { saving.value = false; }
}

function initialPartForm(): CreateInventoryPartPayload { return { code: "", name: "", model: "", stockQty: 0, safetyQty: 0, location: "", unitCost: 0 }; }
function initialMovementForm(): CreateStockMovementPayload { return { movementType: "INBOUND", quantity: 1, sourceNo: "", remark: "" }; }
function initialIssueForm(): IssueForm { return { code: "", projectId: "", issueDate: today(), receiverName: auth.user?.displayName || "", purpose: "", lines: [{ key: ++lineKey, partId: "", quantity: 1 }] }; }
function initialReturnForm(): ReturnForm { return { code: "", returnDate: today(), handlerName: auth.user?.displayName || "", lines: [] }; }
function today() { const value = new Date(); return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, "0")}-${String(value.getDate()).padStart(2, "0")}`; }
function lineAmount(line: IssueFormLine) { const part = parts.value.find((item) => item.id === line.partId); return Number(line.quantity || 0) * Number(part?.unitCost || 0); }
function returnableLines(issue: MaterialIssue) { return issue.lines.filter((line) => Number(line.returnableQty) > 0); }
function issueLineSummary(issue: MaterialIssue) { return issue.lines.map((line) => `${line.partName} × ${formatQuantity(line.quantity)}`).join("，"); }
function returnLineSummary(item: MaterialReturn) { return item.lines.map((line) => `${line.partName} × ${formatQuantity(line.quantity)}`).join("，"); }
function issueStatusLabel(status: InventoryIssueStatus) { return ({ POSTED: "已领料", PARTIAL_RETURNED: "部分退料", FULLY_RETURNED: "全部退料" } as Record<InventoryIssueStatus, string>)[status]; }
function issueStatusColor(status: InventoryIssueStatus) { return ({ POSTED: "blue", PARTIAL_RETURNED: "orange", FULLY_RETURNED: "green" } as Record<InventoryIssueStatus, string>)[status]; }
function movementLabel(type: StockMovementType) { return ({ INBOUND: "入库", OUTBOUND: "领用", RETURN: "退料", SCRAP: "报废", ADJUSTMENT: "调整" } as Record<StockMovementType, string>)[type]; }
function movementColor(type: StockMovementType) { return ({ INBOUND: "green", OUTBOUND: "blue", RETURN: "cyan", SCRAP: "red", ADJUSTMENT: "orange" } as Record<StockMovementType, string>)[type]; }
function formatMoney(value: number) { return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY", minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value || 0); }
function moneyFormatter(value: number | string) { return formatMoney(Number(value)); }
function formatQuantity(value: number) { return new Intl.NumberFormat("zh-CN", { maximumFractionDigits: 2 }).format(value || 0); }
function formatDateTime(value: string) { return new Intl.DateTimeFormat("zh-CN", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)); }
</script>

<style scoped>
.issue-line {
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.issue-line:last-of-type {
  margin-bottom: 12px;
}
</style>
