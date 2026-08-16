<template>
  <div class="page-stack approval-config-page">
    <a-card>
      <template #title>审批流配置</template>
      <template #extra>
        <a-space wrap>
          <a-select
            :value="selectedFlow"
            :options="flowOptions"
            style="width: 220px"
            @change="onFlowChange"
          />
          <a-radio-group
            :value="flowMode"
            button-style="solid"
            @change="onModeChange"
          >
            <a-radio-button value="SEQUENTIAL">依次审批</a-radio-button>
            <a-radio-button value="PARALLEL">同步审批</a-radio-button>
          </a-radio-group>
          <a-button
            type="primary"
            :loading="saving"
            :disabled="!dirty"
            @click="saveDesign"
            >保存设计</a-button
          >
          <a-button @click="openTemplate">套用模板</a-button>
          <a-button @click="publishFlow(selectedFlow)">发布</a-button>
          <a-button @click="openVersions(selectedFlow)">版本</a-button>
          <a-button @click="openCopyFlow">复制审批流</a-button>
          <a-button @click="openBatchPreview">批量模拟</a-button>
        </a-space>
      </template>

      <a-alert
        type="info"
        show-icon
        message="拖拽节点调整审批顺序，点击节点可编辑；修改后点击「保存设计」一次性落库。"
        style="margin-bottom: 16px"
      />

      <div v-if="warnings.length" class="flow-warnings">
        <a-tag v-for="w in warnings" :key="w" color="orange">{{ w }}</a-tag>
        <a-tag v-if="dirty" color="red">未保存</a-tag>
      </div>

      <a-spin :spinning="loading">
        <ApprovalFlowDesigner
          :steps="flowSteps"
          @reorder-step="reorderStep"
          @move-branch="moveBranch"
          @add-step="addStep"
          @add-branch="addBranch"
          @edit-branch="openEdit"
          @delete-branch="deleteBranch"
          @delete-step="deleteStep"
        />
      </a-spin>

      <a-collapse v-model:active-key="previewKeys" ghost>
        <a-collapse-panel key="preview" header="审批路径预览">
          <a-row :gutter="12">
            <a-col :xs="24" :md="6"
              ><a-input-number
                v-model:value="previewForm.amount"
                placeholder="金额/天数"
                :min="0"
                :precision="2"
                style="width: 100%"
            /></a-col>
            <a-col :xs="24" :md="6"
              ><a-input
                v-model:value="previewForm.departmentName"
                placeholder="部门/组织"
            /></a-col>
            <a-col :xs="24" :md="6"
              ><a-input
                v-model:value="previewForm.businessType"
                placeholder="业务类型"
            /></a-col>
            <a-col :xs="24" :md="6"
              ><a-button block @click="runPreview">预览路径</a-button></a-col
            >
          </a-row>
          <a-row :gutter="12" style="margin-top: 10px">
            <a-col :xs="24" :md="6"
              ><a-input
                v-model:value="previewForm.projectCode"
                placeholder="项目编码"
            /></a-col>
            <a-col :xs="24" :md="6"
              ><a-select
                v-model:value="previewForm.supplierRisk"
                allow-clear
                placeholder="供应商风险"
                :options="supplierRiskOptions"
                style="width: 100%"
            /></a-col>
            <a-col :xs="24" :md="6"
              ><a-input
                v-model:value="previewForm.customerLevel"
                placeholder="客户等级"
            /></a-col>
          </a-row>
          <div v-if="previewResult" class="preview-result">
            <a-alert
              :message="`${previewResult.ruleText} · V${previewResult.versionNo}`"
              type="success"
              show-icon
            />
            <a-steps :current="-1" size="small" class="preview-steps">
              <a-step
                v-for="step in previewResult.steps"
                :key="step.stepNo"
                :title="`第 ${step.stepNo} 步`"
                :description="stepDescription(step)"
              />
            </a-steps>
          </div>
        </a-collapse-panel>
      </a-collapse>
    </a-card>

    <a-modal
      v-model:open="addOpen"
      :title="editingId ? '编辑审批节点' : '新增审批节点'"
      :confirm-loading="saving"
      @ok="saveConfig"
    >
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="审批流程">
          <a-input :value="selectedFlowName" disabled />
        </a-form-item>
        <a-form-item label="审批对象类型" name="assigneeType">
          <a-radio-group v-model:value="form.assigneeType">
            <a-radio-button value="ROLE">角色</a-radio-button>
            <a-radio-button value="USER">人员</a-radio-button>
            <a-radio-button value="DYNAMIC">动态</a-radio-button>
            <a-radio-button value="AUTO">自动</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item
          v-if="form.assigneeType === 'ROLE' || form.assigneeType === 'USER'"
          :label="form.assigneeType === 'ROLE' ? '审批角色' : '审批人员'"
          name="targetIds"
        >
          <a-select
            v-model:value="form.targetIds"
            mode="multiple"
            show-search
            option-filter-prop="label"
            :max-tag-count="4"
            :placeholder="
              editingId
                ? '请选择 1 个审批对象'
                : '可选择多个审批对象（每个对象生成一个分支）'
            "
            :options="targetOptions"
            @change="handleTargetChange"
          />
        </a-form-item>
        <a-form-item
          v-else-if="form.assigneeType === 'DYNAMIC'"
          label="动态审批对象"
          name="dynamicAssignee"
        >
          <a-select
            v-model:value="form.dynamicAssignee"
            :options="dynamicAssigneeOptions"
          />
        </a-form-item>
        <a-alert
          v-else
          type="warning"
          show-icon
          message="命中该规则时将自动通过，请确认条件足够明确。"
          style="margin-bottom: 16px"
        />
        <a-form-item label="适用条件" name="conditionType"
          ><a-select
            v-model:value="form.conditionType"
            :options="conditionOptions"
        /></a-form-item>
        <a-row
          v-if="
            form.conditionType === 'AMOUNT' ||
            form.conditionType === 'AMOUNT_AND_DEPARTMENT'
          "
          :gutter="12"
        >
          <a-col :span="12"
            ><a-form-item label="最小金额（含税，元）"
              ><a-input-number
                v-model:value="form.minAmount"
                :min="0"
                :precision="2"
                style="width: 100%" /></a-form-item
          ></a-col>
          <a-col :span="12"
            ><a-form-item label="最大金额（含税，元）"
              ><a-input-number
                v-model:value="form.maxAmount"
                :min="0"
                :precision="2"
                style="width: 100%" /></a-form-item
          ></a-col>
        </a-row>
        <a-form-item
          v-if="
            form.conditionType === 'DEPARTMENT' ||
            form.conditionType === 'AMOUNT_AND_DEPARTMENT'
          "
          label="适用部门/组织"
        >
          <a-input
            v-model:value="form.departmentName"
            placeholder="例如：工程部、财务部、华东区域"
          />
        </a-form-item>
        <a-row
          v-if="
            form.conditionType === 'BUSINESS_TYPE' ||
            form.conditionType === 'PROJECT' ||
            form.conditionType === 'SUPPLIER_RISK' ||
            form.conditionType === 'CUSTOMER_LEVEL' ||
            form.conditionType === 'COMPOSITE'
          "
          :gutter="12"
        >
          <a-col :span="12"
            ><a-form-item label="业务类型"
              ><a-input
                v-model:value="form.businessType"
                placeholder="例如：TOOL、咨询服务" /></a-form-item
          ></a-col>
          <a-col :span="12"
            ><a-form-item label="项目编码"
              ><a-input
                v-model:value="form.projectCode"
                placeholder="例如：PRJ-001" /></a-form-item
          ></a-col>
          <a-col :span="12"
            ><a-form-item label="供应商风险"
              ><a-select
                v-model:value="form.supplierRisk"
                allow-clear
                :options="supplierRiskOptions" /></a-form-item
          ></a-col>
          <a-col :span="12"
            ><a-form-item label="客户等级"
              ><a-input
                v-model:value="form.customerLevel"
                placeholder="例如：A、VIP、NORMAL" /></a-form-item
          ></a-col>
        </a-row>
        <a-collapse
          v-model:active-key="advancedKeys"
          ghost
          class="advanced-settings-collapse"
        >
          <a-collapse-panel key="advanced">
            <template #header>
              <span>高级设置</span>
              <span v-if="advancedSettingCount" class="advanced-count"
                >已配置 {{ advancedSettingCount }} 项</span
              >
            </template>
            <a-form-item label="节点通过策略">
              <a-select
                v-model:value="form.stepPolicy"
                :options="stepPolicyOptions"
              />
            </a-form-item>
            <a-row :gutter="12">
              <a-col :span="12"
                ><a-form-item label="SLA小时数"
                  ><a-input-number
                    v-model:value="form.slaHours"
                    :min="1"
                    :precision="0"
                    style="width: 100%" /></a-form-item
              ></a-col>
              <a-col :span="12"
                ><a-form-item label="超时升级角色"
                  ><a-select
                    v-model:value="form.escalationRoleId"
                    allow-clear
                    show-search
                    option-filter-prop="label"
                    :options="roleOptions" /></a-form-item
              ></a-col>
            </a-row>
            <a-form-item label="规则备注"
              ><a-textarea
                v-model:value="form.remark"
                :rows="2"
                placeholder="例如：金额超过 5 万需财务经理审批"
            /></a-form-item>
            <a-form-item v-if="editingId" label="启用状态"
              ><a-switch
                v-model:checked="form.enabled"
                checked-children="启用"
                un-checked-children="停用"
            /></a-form-item>
          </a-collapse-panel>
        </a-collapse>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="copyOpen"
      title="复制审批流"
      :confirm-loading="saving"
      @ok="handleCopyFlow"
    >
      <a-form :model="copyForm" layout="vertical">
        <a-form-item label="源审批流"
          ><a-select
            v-model:value="copyForm.sourceFlowCode"
            :options="flowOptions"
        /></a-form-item>
        <a-form-item label="目标审批流"
          ><a-select
            v-model:value="copyForm.targetFlowCode"
            :options="flowOptions"
        /></a-form-item>
        <a-form-item
          ><a-checkbox v-model:checked="copyForm.overwrite"
            >覆盖目标审批流当前规则</a-checkbox
          ></a-form-item
        >
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="templateOpen"
      title="套用审批流模板"
      :footer="null"
      width="640px"
    >
      <a-alert
        type="warning"
        show-icon
        message="套用模板会覆盖当前流程的节点，套用后仍需点击「保存设计」落库。"
        style="margin-bottom: 12px"
      />
      <a-radio-group v-model:value="selectedTemplate" style="width: 100%">
        <div v-for="t in approvalTemplates" :key="t.key" class="template-item">
          <a-radio :value="t.key">
            <div class="template-head">
              <strong>{{ t.name }}</strong>
              <a-tag :color="t.mode === 'SEQUENTIAL' ? 'blue' : 'green'">{{
                t.mode === "SEQUENTIAL" ? "依次" : "同步"
              }}</a-tag>
            </div>
            <div class="template-desc">{{ t.description }}</div>
          </a-radio>
        </div>
      </a-radio-group>
      <div style="margin-top: 16px; text-align: right">
        <a-button @click="templateOpen = false">取消</a-button>
        <a-button type="primary" style="margin-left: 8px" @click="applyTemplate"
          >套用</a-button
        >
      </div>
    </a-modal>

    <a-modal
      v-model:open="batchOpen"
      title="批量模拟审批路径"
      width="760px"
      :confirm-loading="saving"
      @ok="runBatchPreview"
    >
      <a-textarea v-model:value="batchText" :rows="8" />
      <div v-if="batchResults.length" class="batch-results">
        <a-alert
          v-for="(item, index) in batchResults"
          :key="index"
          type="success"
          show-icon
          :message="`${item.flowCode} · V${item.versionNo} · ${item.ruleText}`"
          :description="item.steps.map(stepDescription).join(' / ')"
        />
      </div>
    </a-modal>

    <a-modal v-model:open="versionOpen" title="审批流版本" :footer="null">
      <a-table
        size="small"
        :data-source="versions"
        :columns="versionColumns"
        :pagination="false"
        row-key="versionNo"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-popconfirm
              title="确认回滚到该版本？"
              @confirm="rollbackFlow(record.versionNo)"
            >
              <a-button type="link" size="small">回滚</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message, type FormInstance } from "ant-design-vue";
import ApprovalFlowDesigner from "@/components/ApprovalFlowDesigner.vue";
import {
  batchPreviewApprovalFlows,
  copyApprovalFlow,
  listApprovalConfigs,
  listApprovalFlowVersions,
  listRolesApi,
  listUsersApi,
  previewApprovalFlow,
  publishApprovalFlow,
  replaceApprovalFlow,
  rollbackApprovalFlow,
  type ApprovalConfigResponse,
  type ApprovalFlowPreview,
  type ApprovalFlowVersion,
  type ApprovalRulePayload,
  type RoleResponse,
  type UserResponse,
} from "@/api/system";
import {
  approvalTemplates,
  type ApprovalFlowTemplate,
} from "./approvalTemplates";

type FlowStep = { stepNo: number; rules: ApprovalConfigResponse[] };

const flowOptions = [
  { label: "报价审批", value: "QUOTE", flowName: "报价审批" },
  { label: "合同审批", value: "CONTRACT", flowName: "合同审批" },
  {
    label: "合同变更/盖章件审批",
    value: "CONTRACT_CHANGE",
    flowName: "合同变更/盖章件审批",
  },
  { label: "采购申请审批", value: "PURCHASE", flowName: "采购申请审批" },
  { label: "项目立项审批", value: "PROJECT", flowName: "项目立项审批" },
  { label: "付款申请审批", value: "PAYMENT", flowName: "付款申请审批" },
  { label: "费用报销审批", value: "EXPENSE", flowName: "费用报销审批" },
  { label: "外包服务审批", value: "OUTSOURCE", flowName: "外包服务审批" },
  { label: "请假审批", value: "LEAVE", flowName: "请假审批" },
  { label: "出差审批", value: "TRAVEL", flowName: "出差审批" },
  { label: "用印审批", value: "SEAL", flowName: "用印审批" },
  { label: "通用审批", value: "OTHER", flowName: "通用审批" },
];

const configs = ref<ApprovalConfigResponse[]>([]);
const users = ref<UserResponse[]>([]);
const roles = ref<RoleResponse[]>([]);
const versions = ref<ApprovalFlowVersion[]>([]);
const selectedFlow = ref("QUOTE");
const flowMode = ref<"PARALLEL" | "SEQUENTIAL">("SEQUENTIAL");
const flowSteps = ref<FlowStep[]>([]);
const dirty = ref(false);
const loading = ref(false);
const saving = ref(false);
const addOpen = ref(false);
const copyOpen = ref(false);
const batchOpen = ref(false);
const versionOpen = ref(false);
const editingId = ref<string | null>(null);
const previewKeys = ref<string[]>([]);
const advancedKeys = ref<string[]>([]);
const formRef = ref<FormInstance>();
const previewResult = ref<ApprovalFlowPreview | undefined>();
const batchResults = ref<ApprovalFlowPreview[]>([]);
const selectedVersionFlow = ref("QUOTE");
const templateOpen = ref(false);
const selectedTemplate = ref(approvalTemplates[0].key);

const previewForm = reactive({
  amount: undefined as number | undefined,
  departmentName: "",
  businessType: "",
  projectCode: "",
  supplierRisk: undefined as string | undefined,
  customerLevel: "",
});

const form = reactive({
  flowCode: "QUOTE",
  assigneeType: "ROLE" as ApprovalConfigResponse["assigneeType"],
  targetIds: [] as string[],
  dynamicAssignee: "DEPARTMENT_LEADER",
  conditionType: "ANY" as ApprovalConfigResponse["conditionType"],
  minAmount: undefined as number | undefined,
  maxAmount: undefined as number | undefined,
  departmentName: "",
  businessType: "",
  projectCode: "",
  supplierRisk: undefined as string | undefined,
  customerLevel: "",
  stepPolicy: "ANY_APPROVE" as ApprovalConfigResponse["stepPolicy"],
  slaHours: undefined as number | undefined,
  escalationRoleId: undefined as string | undefined,
  remark: "",
  enabled: true,
  sequenceNo: 1,
});

const copyForm = reactive({
  sourceFlowCode: "QUOTE",
  targetFlowCode: "CONTRACT",
  overwrite: false,
});
const batchText = ref(
  JSON.stringify(
    [
      { flowCode: "QUOTE", amount: 60000, customerLevel: "VIP" },
      { flowCode: "EXPENSE", amount: 1200, businessType: "TRAVEL" },
    ],
    null,
    2,
  ),
);

const userOptions = computed(() =>
  users.value
    .filter((item) => item.enabled)
    .map((item) => ({
      label: `${item.displayName} · ${item.username}`,
      value: item.id,
    })),
);
const roleOptions = computed(() =>
  roles.value.map((item) => ({
    label: `${item.name} · ${item.code}`,
    value: item.id,
  })),
);
const targetOptions = computed(() =>
  form.assigneeType === "ROLE" ? roleOptions.value : userOptions.value,
);
const conditionOptions = [
  { label: "全部单据", value: "ANY" },
  { label: "按金额区间", value: "AMOUNT" },
  { label: "按部门/组织", value: "DEPARTMENT" },
  { label: "金额 + 部门", value: "AMOUNT_AND_DEPARTMENT" },
  { label: "按业务类型", value: "BUSINESS_TYPE" },
  { label: "按项目", value: "PROJECT" },
  { label: "按供应商风险", value: "SUPPLIER_RISK" },
  { label: "按客户等级", value: "CUSTOMER_LEVEL" },
  { label: "复合条件", value: "COMPOSITE" },
];
const supplierRiskOptions = [
  { label: "正常", value: "NORMAL" },
  { label: "关注", value: "WATCHLIST" },
  { label: "冻结", value: "BLOCKED" },
];
const stepPolicyOptions = [
  { label: "任一通过", value: "ANY_APPROVE" },
  { label: "全部通过", value: "ALL_APPROVE" },
  { label: "多数通过", value: "MAJORITY_APPROVE" },
];
const dynamicAssigneeOptions = [
  { label: "部门负责人", value: "DEPARTMENT_LEADER" },
  { label: "直属上级", value: "DIRECT_MANAGER" },
  { label: "项目经理", value: "PROJECT_MANAGER" },
  { label: "客户负责人", value: "CUSTOMER_OWNER" },
  { label: "财务经理", value: "FINANCE_MANAGER" },
  { label: "采购经理", value: "PROCUREMENT_MANAGER" },
  { label: "人事经理", value: "HR_MANAGER" },
];
const dynamicLabelMap: Record<string, string> = {
  DEPARTMENT_LEADER: "部门负责人",
  DIRECT_MANAGER: "直属上级",
  PROJECT_MANAGER: "项目经理",
  CUSTOMER_OWNER: "客户负责人",
  FINANCE_MANAGER: "财务经理",
  PROCUREMENT_MANAGER: "采购经理",
  HR_MANAGER: "人事经理",
};
const rules = {
  assigneeType: [{ required: true, message: "请选择审批对象类型" }],
  targetIds: [
    { required: true, type: "array", min: 1, message: "请选择审批对象" },
  ],
};
const versionColumns = [
  { title: "版本", dataIndex: "versionNo" },
  { title: "规则数", dataIndex: "ruleCount" },
  { title: "状态", dataIndex: "publishStatus" },
  { title: "操作", key: "action", width: 100 },
];
const selectedFlowName = computed(
  () =>
    flowOptions.find((item) => item.value === selectedFlow.value)?.flowName ||
    selectedFlow.value,
);
const advancedSettingCount = computed(
  () =>
    [
      form.stepPolicy !== "ANY_APPROVE",
      form.slaHours != null,
      form.escalationRoleId != null,
      Boolean(form.remark.trim()),
      editingId.value != null && !form.enabled,
    ].filter(Boolean).length,
);
const warnings = computed(() => {
  const all = flowSteps.value.flatMap((s) => s.rules);
  const w: string[] = [];
  if (!all.length) w.push("尚未配置任何审批节点");
  if (all.length && !all.some((r) => r.enabled)) w.push("无启用节点");
  if (
    all.length &&
    !all.some(
      (r) => r.enabled && r.conditionType === "ANY" && r.sequenceNo === 1,
    )
  )
    w.push("缺少第1步默认规则（全部单据）");
  if (
    all.some((r) => r.assigneeType === "ROLE" && roleUserCount(r.roleId) === 0)
  )
    w.push("存在空角色");
  if (all.some((r) => r.assigneeType === "AUTO" && r.conditionType === "ANY"))
    w.push("自动通过条件过宽");
  if (hasAmountOverlap()) w.push("同一步金额区间重叠");
  return w;
});

function tempId() {
  return typeof crypto !== "undefined" && "randomUUID" in crypto
    ? crypto.randomUUID()
    : "draft-" + Math.random().toString(36).slice(2) + Date.now().toString(36);
}

async function loadData() {
  loading.value = true;
  try {
    const [configData, userData, roleData] = await Promise.all([
      listApprovalConfigs(),
      listUsersApi(0, 200),
      listRolesApi(0, 200),
    ]);
    configs.value = configData;
    users.value = userData.content;
    roles.value = roleData.content;
    const configured = new Set(configData.map((c) => c.flowCode));
    if (!configured.has(selectedFlow.value)) {
      selectedFlow.value = configured.size ? [...configured][0] : "QUOTE";
    }
    reinitDraft(selectedFlow.value);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "审批配置加载失败");
  } finally {
    loading.value = false;
  }
}

function reinitDraft(flowCode: string) {
  const items = configs.value
    .filter((c) => c.flowCode === flowCode)
    .sort((a, b) => a.sequenceNo - b.sequenceNo || a.priority - b.priority);
  const byStep = new Map<number, ApprovalConfigResponse[]>();
  for (const r of items) {
    if (!byStep.has(r.sequenceNo)) byStep.set(r.sequenceNo, []);
    byStep.get(r.sequenceNo)!.push({ ...r });
  }
  flowSteps.value = [...byStep.entries()]
    .sort((a, b) => a[0] - b[0])
    .map(([stepNo, stepRules]) => ({
      stepNo,
      rules: stepRules.sort((a, b) => a.priority - b.priority),
    }));
  flowMode.value =
    items.find((c) => c.approvalMode)?.approvalMode || "SEQUENTIAL";
  renumber();
  dirty.value = false;
}

function renumber() {
  flowSteps.value = flowSteps.value.map((s, i) => ({
    stepNo: i + 1,
    rules: s.rules.map((r, j) => ({
      ...r,
      sequenceNo: i + 1,
      priority: j + 1,
    })),
  }));
}

function onFlowChange(value: string) {
  if (value === selectedFlow.value) return;
  if (dirty.value) {
    const ok = window.confirm("当前流程有未保存的修改，切换将丢弃。是否继续？");
    if (!ok) return;
  }
  selectedFlow.value = value;
  reinitDraft(value);
}

function onModeChange(e: unknown) {
  const mode = (e as { target?: { value?: string } })?.target?.value as
    | "PARALLEL"
    | "SEQUENTIAL"
    | undefined;
  if (!mode || mode === flowMode.value) return;
  flowMode.value = mode;
  flowSteps.value = flowSteps.value.map((s) => ({
    stepNo: s.stepNo,
    rules: s.rules.map((r) => ({ ...r, approvalMode: mode })),
  }));
  dirty.value = true;
}

function reorderStep(from: number, to: number) {
  const arr = [...flowSteps.value];
  const [moved] = arr.splice(from, 1);
  arr.splice(to, 0, moved);
  flowSteps.value = arr;
  renumber();
  dirty.value = true;
}

function moveBranch(
  fromStep: number,
  fromIndex: number,
  toStep: number,
  toIndex: number,
) {
  const arr = flowSteps.value.map((s) => ({
    stepNo: s.stepNo,
    rules: [...s.rules],
  }));
  const src = arr[fromStep];
  if (!src) return;
  const [moved] = src.rules.splice(fromIndex, 1);
  if (!moved) return;
  if (fromStep === toStep) {
    const insertAt = toIndex > fromIndex ? toIndex - 1 : toIndex;
    src.rules.splice(
      Math.min(Math.max(insertAt, 0), src.rules.length),
      0,
      moved,
    );
  } else {
    const dst = arr[toStep];
    if (!dst) src.rules.push(moved);
    else dst.rules.splice(Math.min(toIndex, dst.rules.length), 0, moved);
  }
  flowSteps.value = arr.filter((s) => s.rules.length > 0);
  renumber();
  dirty.value = true;
}

function addStep() {
  const maxStep = flowSteps.value.length
    ? Math.max(...flowSteps.value.map((s) => s.stepNo))
    : 0;
  openAdd(selectedFlow.value, maxStep + 1);
}

function addBranch(stepIndex: number) {
  const step = flowSteps.value[stepIndex];
  openAdd(selectedFlow.value, step ? step.stepNo : 1);
}

function openAdd(flowCode: string, sequenceNo: number) {
  editingId.value = null;
  advancedKeys.value = [];
  Object.assign(form, {
    flowCode,
    assigneeType: "ROLE",
    targetIds: [],
    dynamicAssignee: "DEPARTMENT_LEADER",
    conditionType: "ANY",
    minAmount: undefined,
    maxAmount: undefined,
    departmentName: "",
    businessType: "",
    projectCode: "",
    supplierRisk: undefined,
    customerLevel: "",
    stepPolicy: "ANY_APPROVE",
    slaHours: undefined,
    escalationRoleId: undefined,
    remark: "",
    enabled: true,
    sequenceNo,
  });
  addOpen.value = true;
}

function openEdit(record: ApprovalConfigResponse) {
  editingId.value = record.id;
  Object.assign(form, {
    flowCode: record.flowCode,
    assigneeType: record.assigneeType,
    targetIds: [
      record.assigneeType === "ROLE"
        ? record.roleId
        : record.assigneeType === "USER"
          ? record.userId
          : undefined,
    ].filter(Boolean) as string[],
    dynamicAssignee: record.dynamicAssignee || "DEPARTMENT_LEADER",
    conditionType: record.conditionType,
    minAmount: record.minAmount,
    maxAmount: record.maxAmount,
    departmentName: record.departmentName || "",
    businessType: record.businessType || "",
    projectCode: record.projectCode || "",
    supplierRisk: record.supplierRisk,
    customerLevel: record.customerLevel || "",
    stepPolicy: record.stepPolicy || "ANY_APPROVE",
    slaHours: record.slaHours,
    escalationRoleId: record.escalationRoleId,
    remark: record.remark || "",
    enabled: record.enabled,
    sequenceNo: record.sequenceNo,
  });
  advancedKeys.value = advancedSettingCount.value ? ["advanced"] : [];
  addOpen.value = true;
}

function handleTargetChange(values: string[]) {
  if (editingId.value && values.length > 1) {
    form.targetIds = [values[values.length - 1]];
  }
}

function buildRule(
  assigneeType: ApprovalConfigResponse["assigneeType"],
  userId?: string,
  roleId?: string,
): ApprovalConfigResponse {
  const flowName = selectedFlowName.value;
  const assigneeName =
    assigneeType === "ROLE"
      ? roles.value.find((r) => r.id === roleId)?.name || "角色"
      : assigneeType === "USER"
        ? users.value.find((u) => u.id === userId)?.displayName || "用户"
        : assigneeType === "DYNAMIC"
          ? dynamicLabelMap[form.dynamicAssignee] || "动态审批人"
          : "自动通过";
  return {
    id: editingId.value ?? tempId(),
    flowCode: selectedFlow.value,
    flowName,
    assigneeType,
    userId: assigneeType === "USER" ? userId : undefined,
    roleId: assigneeType === "ROLE" ? roleId : undefined,
    assigneeName,
    versionNo: 1,
    dynamicAssignee:
      assigneeType === "DYNAMIC" ? form.dynamicAssignee : undefined,
    autoAction: assigneeType === "AUTO" ? "APPROVE" : undefined,
    slaHours: form.slaHours,
    escalationRoleId: form.escalationRoleId,
    escalationRoleName: roles.value.find((r) => r.id === form.escalationRoleId)
      ?.name,
    stepPolicy: form.stepPolicy,
    approvalMode: flowMode.value,
    sequenceNo: form.sequenceNo,
    conditionType: form.conditionType,
    minAmount: form.minAmount,
    maxAmount: form.maxAmount,
    departmentName: form.departmentName || undefined,
    businessType: form.businessType || undefined,
    projectCode: form.projectCode || undefined,
    supplierRisk: form.supplierRisk,
    customerLevel: form.customerLevel || undefined,
    priority: 1,
    remark: form.remark || undefined,
    enabled: form.enabled,
  };
}

async function saveConfig() {
  await formRef.value?.validate();
  const targets =
    form.assigneeType === "USER" || form.assigneeType === "ROLE"
      ? [...new Set(form.targetIds)]
      : ["__virtual__"];
  if (editingId.value && targets.length !== 1) {
    message.warning("编辑已有配置时请选择 1 个审批对象");
    return;
  }
  const newRules = targets.map((t) =>
    buildRule(
      form.assigneeType,
      form.assigneeType === "USER" ? t : undefined,
      form.assigneeType === "ROLE" ? t : undefined,
    ),
  );
  if (editingId.value) {
    const rule = newRules[0];
    flowSteps.value = flowSteps.value.map((s) => ({
      stepNo: s.stepNo,
      rules: s.rules.map((r) => (r.id === editingId.value ? rule : r)),
    }));
  } else {
    const stepNo = form.sequenceNo;
    const existing = flowSteps.value.find((s) => s.stepNo === stepNo);
    if (existing) existing.rules.push(...newRules);
    else flowSteps.value.push({ stepNo, rules: newRules });
  }
  renumber();
  dirty.value = true;
  addOpen.value = false;
}

async function saveDesign() {
  const payloadRules: ApprovalRulePayload[] = [];
  for (const step of flowSteps.value) {
    for (const r of step.rules) {
      payloadRules.push({
        assigneeType: r.assigneeType,
        userId: r.userId,
        roleId: r.roleId,
        dynamicAssignee: r.dynamicAssignee,
        autoAction: r.autoAction,
        slaHours: r.slaHours,
        escalationRoleId: r.escalationRoleId,
        stepPolicy: r.stepPolicy,
        approvalMode: flowMode.value,
        sequenceNo: r.sequenceNo,
        conditionType: r.conditionType,
        minAmount: r.minAmount,
        maxAmount: r.maxAmount,
        departmentName: r.departmentName,
        businessType: r.businessType,
        projectCode: r.projectCode,
        supplierRisk: r.supplierRisk,
        customerLevel: r.customerLevel,
        priority: r.priority,
        remark: r.remark,
        enabled: r.enabled,
      });
    }
  }
  if (!payloadRules.length) {
    message.warning("审批流至少需要一个审批节点");
    return;
  }
  saving.value = true;
  try {
    await replaceApprovalFlow({
      flowCode: selectedFlow.value,
      flowName: selectedFlowName.value,
      rules: payloadRules,
    });
    message.success("审批流已保存");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "保存失败");
  } finally {
    saving.value = false;
  }
}

function deleteBranch(rule: ApprovalConfigResponse) {
  flowSteps.value = flowSteps.value
    .map((s) => ({
      stepNo: s.stepNo,
      rules: s.rules.filter((r) => r.id !== rule.id),
    }))
    .filter((s) => s.rules.length > 0);
  renumber();
  dirty.value = true;
}

function deleteStep(stepIndex: number) {
  flowSteps.value = flowSteps.value
    .filter((_, i) => i !== stepIndex)
    .map((s, i) => ({ stepNo: i + 1, rules: s.rules }));
  renumber();
  dirty.value = true;
}

async function publishFlow(flowCode: string) {
  if (dirty.value) {
    message.warning("有未保存的修改，请先点击「保存设计」再发布");
    return;
  }
  try {
    await publishApprovalFlow(flowCode);
    message.success("审批流已发布");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "发布失败");
  }
}

async function openVersions(flowCode: string) {
  selectedVersionFlow.value = flowCode;
  versions.value = await listApprovalFlowVersions(flowCode);
  versionOpen.value = true;
}

async function rollbackFlow(versionNo: number) {
  try {
    await rollbackApprovalFlow(selectedVersionFlow.value, versionNo);
    message.success("已回滚并生成新版本");
    versionOpen.value = false;
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "回滚失败");
  }
}

function openCopyFlow() {
  Object.assign(copyForm, {
    sourceFlowCode: "QUOTE",
    targetFlowCode: "CONTRACT",
    overwrite: false,
  });
  copyOpen.value = true;
}

async function handleCopyFlow() {
  const target = flowOptions.find(
    (item) => item.value === copyForm.targetFlowCode,
  )!;
  saving.value = true;
  try {
    await copyApprovalFlow({
      sourceFlowCode: copyForm.sourceFlowCode,
      targetFlowCode: copyForm.targetFlowCode,
      targetFlowName: target.flowName,
      overwrite: copyForm.overwrite,
    });
    copyOpen.value = false;
    message.success("审批流已复制");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "复制失败");
  } finally {
    saving.value = false;
  }
}

function openBatchPreview() {
  batchResults.value = [];
  batchOpen.value = true;
}

async function runBatchPreview() {
  saving.value = true;
  try {
    const items = JSON.parse(batchText.value);
    if (!Array.isArray(items)) throw new Error("请输入 JSON 数组");
    batchResults.value = await batchPreviewApprovalFlows(items);
    message.success("批量模拟完成");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "批量模拟失败");
  } finally {
    saving.value = false;
  }
}

async function runPreview() {
  try {
    previewResult.value = await previewApprovalFlow({
      flowCode: selectedFlow.value,
      ...previewForm,
    });
  } catch (error) {
    message.error(error instanceof Error ? error.message : "审批路径预览失败");
  }
}

function roleUserCount(roleId?: string) {
  if (!roleId) return 0;
  return roles.value.find((role) => role.id === roleId)?.userCount ?? 0;
}

function hasAmountOverlap() {
  return flowSteps.value.some((step) => {
    const ranges = step.rules
      .filter(
        (item) =>
          item.enabled &&
          (item.conditionType === "AMOUNT" ||
            item.conditionType === "AMOUNT_AND_DEPARTMENT"),
      )
      .map((item) => ({
        start: item.minAmount ?? 0,
        end: item.maxAmount ?? Number.POSITIVE_INFINITY,
      }))
      .sort((a, b) => a.start - b.start);
    return ranges.some(
      (range, index) => index > 0 && range.start <= ranges[index - 1].end,
    );
  });
}

function stepDescription(step: ApprovalFlowPreview["steps"][number]) {
  const extras = [step.assignees.join("、") || "无审批人"];
  if (step.autoApproved) extras.push("自动通过");
  if (step.slaHours) extras.push(`SLA ${step.slaHours}小时`);
  if (step.escalationRoleName) extras.push(`升级 ${step.escalationRoleName}`);
  return extras.join(" · ");
}

function openTemplate() {
  selectedTemplate.value = approvalTemplates[0].key;
  templateOpen.value = true;
}

function buildTemplateRules(t: ApprovalFlowTemplate): ApprovalConfigResponse[] {
  return t.rules.map((r, index) => ({
    id: tempId(),
    flowCode: selectedFlow.value,
    flowName: selectedFlowName.value,
    assigneeType: r.assigneeType,
    assigneeName:
      r.assigneeType === "DYNAMIC"
        ? dynamicLabelMap[r.dynamicAssignee || ""] || "动态审批人"
        : "自动通过",
    versionNo: 1,
    dynamicAssignee: r.dynamicAssignee,
    autoAction: r.autoAction,
    slaHours: undefined,
    escalationRoleId: undefined,
    escalationRoleName: undefined,
    stepPolicy: "ANY_APPROVE",
    approvalMode: t.mode,
    sequenceNo: r.sequenceNo,
    conditionType: r.conditionType,
    minAmount: r.minAmount,
    maxAmount: r.maxAmount,
    departmentName: undefined,
    businessType: undefined,
    projectCode: undefined,
    supplierRisk: undefined,
    customerLevel: undefined,
    priority: index + 1,
    remark: r.remark,
    enabled: true,
  }));
}

function applyTemplate() {
  const t = approvalTemplates.find(
    (item) => item.key === selectedTemplate.value,
  );
  if (!t) return;
  const rules = buildTemplateRules(t);
  const byStep = new Map<number, ApprovalConfigResponse[]>();
  for (const r of rules) {
    if (!byStep.has(r.sequenceNo)) byStep.set(r.sequenceNo, []);
    byStep.get(r.sequenceNo)!.push(r);
  }
  flowSteps.value = [...byStep.entries()]
    .sort((a, b) => a[0] - b[0])
    .map(([stepNo, stepRules]) => ({ stepNo, rules: stepRules }));
  flowMode.value = t.mode;
  renumber();
  dirty.value = true;
  templateOpen.value = false;
  message.success(`已套用模板「${t.name}」，请检查后点击「保存设计」`);
}

onMounted(loadData);
</script>

<style scoped>
.flow-warnings {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}
.advanced-settings-collapse {
  margin-top: 4px;
  border-top: 1px solid #f0f0f0;
}
.advanced-count {
  margin-left: 8px;
  color: #64748b;
  font-size: 12px;
}
.preview-result {
  display: grid;
  gap: 12px;
  margin-top: 14px;
}
.preview-steps {
  padding: 4px 6px;
}
.batch-results {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}
.template-item {
  padding: 10px 12px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  margin-bottom: 8px;
  transition:
    border-color 0.15s ease,
    background 0.15s ease;
}
.template-item:hover {
  border-color: #1677ff;
  background: #f6f9fd;
}
.template-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.template-desc {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}
</style>
