<template>
  <div class="page-stack approval-config-page">
    <a-card>
      <template #title>审批流配置</template>
      <template #extra>
        <a-space>
          <a-button @click="openBatchPreview">批量模拟</a-button>
          <a-button @click="openCopyFlow">复制审批流</a-button>
          <a-button type="primary" @click="openAdd()">新增规则</a-button>
        </a-space>
      </template>
      <a-alert
        type="info"
        show-icon
        message="按审批流集中维护规则；每条规则可指定人员或角色作为审批对象。"
        style="margin-bottom: 16px"
      />
      <a-collapse
        v-model:active-key="activeFlowKeys"
        class="approval-flow-collapse"
      >
        <a-collapse-panel v-for="group in groupedFlows" :key="group.flowCode">
          <template #header>
            <div class="flow-panel-title">
              <strong>{{ group.flowName }}</strong>
              <span
                >{{ group.flowCode }} · {{ group.items.length }} 条规则 ·
                {{ group.maxStep }} 步</span
              >
              <span v-if="group.versionNo"
                >当前版本 V{{ group.versionNo }}</span
              >
              <div v-if="flowWarnings(group).length" class="flow-warnings">
                <a-tag
                  v-for="warning in flowWarnings(group)"
                  :key="warning"
                  color="orange"
                  >{{ warning }}</a-tag
                >
              </div>
            </div>
          </template>
          <template #extra>
            <a-space @click.stop>
              <a-button
                size="small"
                type="link"
                @click="publishFlow(group.flowCode)"
                >发布</a-button
              >
              <a-button
                size="small"
                type="link"
                @click="openVersions(group.flowCode)"
                >版本</a-button
              >
              <a-button
                size="small"
                type="link"
                @click="openAdd(group.flowCode)"
                >新增规则</a-button
              >
            </a-space>
          </template>
          <div class="step-stack">
            <section
              v-for="step in group.steps"
              :key="step.stepNo"
              class="step-section"
            >
              <div class="step-title">
                <a-tag color="blue">第 {{ step.stepNo }} 步</a-tag>
                <span
                  >{{ step.items.length }} 条规则，{{
                    step.items.filter(
                      (item: ApprovalConfigResponse) => item.enabled,
                    ).length
                  }}
                  条启用</span
                >
              </div>
              <a-table
                :data-source="step.items"
                :columns="columns"
                :loading="loading"
                row-key="id"
                size="small"
                :pagination="false"
                :custom-row="
                  (record: ApprovalConfigResponse) => ({
                    onClick: () => openEdit(record),
                  })
                "
                :row-class-name="() => 'clickable-row'"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'assignee'">
                    <a-tag :color="assigneeTypeColor(record.assigneeType)">{{
                      assigneeTypeLabel(record.assigneeType)
                    }}</a-tag>
                    {{ record.assigneeName }}
                    <a-tag
                      v-if="
                        record.assigneeType === 'ROLE' &&
                        roleUserCount(record.roleId) === 0
                      "
                      color="red"
                      >无成员</a-tag
                    >
                    <a-tag v-if="!record.enabled" color="default">停用</a-tag>
                    <a-tag>V{{ record.versionNo }}</a-tag>
                  </template>
                  <template v-else-if="column.key === 'mode'"
                    ><a-tag
                      :color="
                        record.approvalMode === 'SEQUENTIAL' ? 'blue' : 'green'
                      "
                      >{{
                        record.approvalMode === "SEQUENTIAL" ? "依次" : "同步"
                      }}</a-tag
                    ></template
                  >
                  <template v-else-if="column.key === 'condition'">
                    <a-tag :color="conditionColor(record.conditionType)">{{
                      conditionLabel(record.conditionType)
                    }}</a-tag>
                    <span class="table-subtitle">{{
                      conditionText(record)
                    }}</span>
                    <span v-if="record.slaHours" class="table-subtitle"
                      >SLA {{ record.slaHours }} 小时</span
                    >
                  </template>
                  <template v-else-if="column.key === 'action'">
                    <a-space>
                      <a-button type="link" @click.stop="openEdit(record)"
                        >编辑</a-button
                      >
                      <a-popconfirm
                        title="确定移除该审批规则？"
                        @confirm="removeConfig(record.id)"
                      >
                        <a-button type="link" danger @click.stop>移除</a-button>
                      </a-popconfirm>
                    </a-space>
                  </template>
                </template>
              </a-table>
            </section>
          </div>
          <div class="preview-panel">
            <div class="preview-title">审批路径预览</div>
            <a-row :gutter="12">
              <a-col :xs="24" :md="6"
                ><a-input-number
                  v-model:value="previewForms[group.flowCode].amount"
                  placeholder="金额/天数"
                  :min="0"
                  :precision="2"
                  style="width: 100%"
              /></a-col>
              <a-col :xs="24" :md="6"
                ><a-input
                  v-model:value="previewForms[group.flowCode].departmentName"
                  placeholder="部门/组织"
              /></a-col>
              <a-col :xs="24" :md="6"
                ><a-input
                  v-model:value="previewForms[group.flowCode].businessType"
                  placeholder="业务类型"
              /></a-col>
              <a-col :xs="24" :md="6"
                ><a-button block @click="runPreview(group.flowCode)"
                  >预览路径</a-button
                ></a-col
              >
            </a-row>
            <a-row :gutter="12" style="margin-top: 10px">
              <a-col :xs="24" :md="6"
                ><a-input
                  v-model:value="previewForms[group.flowCode].projectCode"
                  placeholder="项目编码"
              /></a-col>
              <a-col :xs="24" :md="6"
                ><a-select
                  v-model:value="previewForms[group.flowCode].supplierRisk"
                  allow-clear
                  placeholder="供应商风险"
                  :options="supplierRiskOptions"
                  style="width: 100%"
              /></a-col>
              <a-col :xs="24" :md="6"
                ><a-input
                  v-model:value="previewForms[group.flowCode].customerLevel"
                  placeholder="客户等级"
              /></a-col>
            </a-row>
            <div v-if="previewResults[group.flowCode]" class="preview-result">
              <a-alert
                :message="`${previewResults[group.flowCode]?.ruleText} · V${previewResults[group.flowCode]?.versionNo}`"
                type="success"
                show-icon
              />
              <a-steps :current="-1" size="small" class="preview-steps">
                <a-step
                  v-for="step in previewResults[group.flowCode]?.steps"
                  :key="step.stepNo"
                  :title="`第 ${step.stepNo} 步`"
                  :description="stepDescription(step)"
                />
              </a-steps>
            </div>
          </div>
        </a-collapse-panel>
      </a-collapse>
    </a-card>

    <a-modal
      v-model:open="addOpen"
      :title="editingId ? '编辑审批规则' : '新增审批规则'"
      :confirm-loading="saving"
      @ok="saveConfig"
    >
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="审批流程" name="flowCode"
          ><a-select v-model:value="form.flowCode" :options="flowOptions"
        /></a-form-item>
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
              editingId ? '请选择 1 个审批对象' : '可选择多个审批对象'
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
        <a-form-item label="审批模式" name="approvalMode"
          ><a-radio-group v-model:value="form.approvalMode"
            ><a-radio value="PARALLEL">同步审批（任一人通过）</a-radio
            ><a-radio value="SEQUENTIAL">依次审批</a-radio></a-radio-group
          ></a-form-item
        >
        <a-form-item
          v-if="form.approvalMode === 'SEQUENTIAL'"
          label="起始审批顺序"
          name="sequenceNo"
        >
          <a-input-number
            v-model:value="form.sequenceNo"
            :min="1"
            :precision="0"
          />
          <span class="form-hint">{{
            editingId
              ? "当前审批配置所在步骤。"
              : "多人依次审批时，会按选择顺序从该数字开始递增。"
          }}</span>
        </a-form-item>
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
            ><a-form-item label="最小金额"
              ><a-input-number
                v-model:value="form.minAmount"
                :min="0"
                :precision="2"
                style="width: 100%" /></a-form-item
          ></a-col>
          <a-col :span="12"
            ><a-form-item label="最大金额"
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
        <a-form-item label="规则优先级"
          ><a-input-number
            v-model:value="form.priority"
            :min="1"
            :precision="0"
            style="width: 100%"
        /></a-form-item>
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
import {
  batchPreviewApprovalFlows,
  copyApprovalFlow,
  createApprovalConfig,
  deleteApprovalConfig,
  listApprovalConfigs,
  listApprovalDiagnostics,
  listApprovalFlowVersions,
  listRolesApi,
  listUsersApi,
  previewApprovalFlow,
  publishApprovalFlow,
  rollbackApprovalFlow,
  updateApprovalConfig,
  type ApprovalConfigResponse,
  type ApprovalFlowPreview,
  type ApprovalFlowVersion,
  type RoleResponse,
  type UserResponse,
} from "@/api/system";

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
const selectedVersionFlow = ref("QUOTE");
const loading = ref(false);
const saving = ref(false);
const addOpen = ref(false);
const copyOpen = ref(false);
const batchOpen = ref(false);
const versionOpen = ref(false);
const editingId = ref<string | null>(null);
const activeFlowKeys = ref<string[]>([]);
const formRef = ref<FormInstance>();
const previewResults = ref<Record<string, ApprovalFlowPreview | undefined>>({});
const batchResults = ref<ApprovalFlowPreview[]>([]);
const previewForms = reactive<
  Record<
    string,
    {
      amount?: number;
      departmentName: string;
      businessType: string;
      projectCode: string;
      supplierRisk?: string;
      customerLevel: string;
    }
  >
>({});
const form = reactive({
  flowCode: "QUOTE",
  assigneeType: "ROLE" as ApprovalConfigResponse["assigneeType"],
  targetIds: [] as string[],
  dynamicAssignee: "DEPARTMENT_LEADER",
  approvalMode: "SEQUENTIAL" as "PARALLEL" | "SEQUENTIAL",
  sequenceNo: 1,
  conditionType: "ANY" as ApprovalConfigResponse["conditionType"],
  minAmount: undefined as number | undefined,
  maxAmount: undefined as number | undefined,
  departmentName: "",
  businessType: "",
  projectCode: "",
  supplierRisk: undefined as string | undefined,
  customerLevel: "",
  priority: 100,
  stepPolicy: "ANY_APPROVE" as ApprovalConfigResponse["stepPolicy"],
  slaHours: undefined as number | undefined,
  escalationRoleId: undefined as string | undefined,
  remark: "",
  enabled: true,
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
const columns = [
  { title: "审批对象", key: "assignee", width: 180 },
  { title: "模式", key: "mode", width: 140 },
  { title: "适用条件", key: "condition", width: 360 },
  { title: "操作", key: "action", width: 120 },
];
const versionColumns = [
  { title: "版本", dataIndex: "versionNo" },
  { title: "规则数", dataIndex: "ruleCount" },
  { title: "状态", dataIndex: "publishStatus" },
  { title: "操作", key: "action", width: 100 },
];
const rules = {
  flowCode: [{ required: true, message: "请选择审批流程" }],
  assigneeType: [{ required: true, message: "请选择审批对象类型" }],
  targetIds: [
    { required: true, type: "array", min: 1, message: "请选择审批对象" },
  ],
  approvalMode: [{ required: true }],
  sequenceNo: [{ required: true, message: "请输入审批顺序" }],
};
const groupedFlows = computed(() =>
  flowOptions.map((flow) => {
    const items = configs.value
      .filter((item) => item.flowCode === flow.value)
      .sort((a, b) => a.sequenceNo - b.sequenceNo || a.priority - b.priority);
    const stepNos = [...new Set(items.map((item) => item.sequenceNo))].sort(
      (a, b) => a - b,
    );
    return {
      flowCode: flow.value,
      flowName: flow.flowName,
      items,
      versionNo: items.reduce(
        (max, item) => Math.max(max, item.versionNo || 1),
        0,
      ),
      maxStep: items.reduce((max, item) => Math.max(max, item.sequenceNo), 0),
      steps: stepNos.map((stepNo) => ({
        stepNo,
        items: items.filter((item) => item.sequenceNo === stepNo),
      })),
    };
  }),
);

flowOptions.forEach((item) => {
  previewForms[item.value] = {
    departmentName: "",
    businessType: "",
    projectCode: "",
    customerLevel: "",
  };
});

async function loadData() {
  loading.value = true;
  try {
    const [configData, userData, roleData, diagnostics] = await Promise.all([
      listApprovalConfigs(),
      listUsersApi(0, 200),
      listRolesApi(0, 200),
      listApprovalDiagnostics(),
    ]);
    configs.value = configData;
    users.value = userData.content;
    roles.value = roleData.content;
    if (diagnostics.some((item) => item.severity === "HIGH"))
      message.warning("审批流存在高风险配置，请查看流程面板提示");
    if (!activeFlowKeys.value.length)
      activeFlowKeys.value = flowOptions.map((item) => item.value);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "审批配置加载失败");
  } finally {
    loading.value = false;
  }
}
function openAdd(flowCode = "QUOTE") {
  editingId.value = null;
  Object.assign(form, {
    flowCode,
    assigneeType: "ROLE",
    targetIds: [],
    dynamicAssignee: "DEPARTMENT_LEADER",
    approvalMode: "SEQUENTIAL",
    sequenceNo: 1,
    conditionType: "ANY",
    minAmount: undefined,
    maxAmount: undefined,
    departmentName: "",
    businessType: "",
    projectCode: "",
    supplierRisk: undefined,
    customerLevel: "",
    priority: 100,
    stepPolicy: "ANY_APPROVE",
    slaHours: undefined,
    escalationRoleId: undefined,
    remark: "",
    enabled: true,
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
    approvalMode: record.approvalMode,
    sequenceNo: record.sequenceNo,
    conditionType: record.conditionType,
    minAmount: record.minAmount,
    maxAmount: record.maxAmount,
    departmentName: record.departmentName || "",
    businessType: record.businessType || "",
    projectCode: record.projectCode || "",
    supplierRisk: record.supplierRisk,
    customerLevel: record.customerLevel || "",
    priority: record.priority,
    stepPolicy: record.stepPolicy || "ANY_APPROVE",
    slaHours: record.slaHours,
    escalationRoleId: record.escalationRoleId,
    remark: record.remark || "",
    enabled: record.enabled,
  });
  addOpen.value = true;
}
function handleTargetChange(values: string[]) {
  if (editingId.value && values.length > 1) {
    form.targetIds = [values[values.length - 1]];
  }
}
async function saveConfig() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    const flow = flowOptions.find((item) => item.value === form.flowCode)!;
    const uniqueTargetIds =
      form.assigneeType === "USER" || form.assigneeType === "ROLE"
        ? [...new Set(form.targetIds)]
        : ["__virtual__"];
    if (editingId.value) {
      if (uniqueTargetIds.length !== 1) {
        message.warning("编辑已有配置时请选择 1 个审批对象");
        return;
      }
      await updateApprovalConfig(editingId.value, {
        flowCode: flow.value,
        flowName: flow.flowName,
        assigneeType: form.assigneeType,
        userId: form.assigneeType === "USER" ? uniqueTargetIds[0] : undefined,
        roleId: form.assigneeType === "ROLE" ? uniqueTargetIds[0] : undefined,
        dynamicAssignee:
          form.assigneeType === "DYNAMIC" ? form.dynamicAssignee : undefined,
        autoAction: form.assigneeType === "AUTO" ? "APPROVE" : undefined,
        slaHours: form.slaHours,
        escalationRoleId: form.escalationRoleId,
        approvalMode: form.approvalMode,
        sequenceNo: form.sequenceNo,
        conditionType: form.conditionType,
        minAmount: form.minAmount,
        maxAmount: form.maxAmount,
        departmentName: form.departmentName,
        businessType: form.businessType,
        projectCode: form.projectCode,
        supplierRisk: form.supplierRisk,
        customerLevel: form.customerLevel,
        priority: form.priority,
        stepPolicy: form.stepPolicy,
        remark: form.remark,
        enabled: form.enabled,
      });
      message.success("审批配置已更新");
    } else {
      for (const [index, targetId] of uniqueTargetIds.entries()) {
        await createApprovalConfig({
          flowCode: flow.value,
          flowName: flow.flowName,
          assigneeType: form.assigneeType,
          userId: form.assigneeType === "USER" ? targetId : undefined,
          roleId: form.assigneeType === "ROLE" ? targetId : undefined,
          dynamicAssignee:
            form.assigneeType === "DYNAMIC" ? form.dynamicAssignee : undefined,
          autoAction: form.assigneeType === "AUTO" ? "APPROVE" : undefined,
          slaHours: form.slaHours,
          escalationRoleId: form.escalationRoleId,
          approvalMode: form.approvalMode,
          sequenceNo:
            form.approvalMode === "SEQUENTIAL"
              ? form.sequenceNo + index
              : form.sequenceNo,
          conditionType: form.conditionType,
          minAmount: form.minAmount,
          maxAmount: form.maxAmount,
          departmentName: form.departmentName,
          businessType: form.businessType,
          projectCode: form.projectCode,
          supplierRisk: form.supplierRisk,
          customerLevel: form.customerLevel,
          priority: form.priority,
          stepPolicy: form.stepPolicy,
          remark: form.remark,
        });
      }
      message.success(
        `已配置 ${form.assigneeType === "USER" || form.assigneeType === "ROLE" ? uniqueTargetIds.length : 1} 个审批对象`,
      );
    }
    addOpen.value = false;
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "配置保存失败");
  } finally {
    saving.value = false;
  }
}
async function publishFlow(flowCode: string) {
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
async function removeConfig(id: string) {
  try {
    await deleteApprovalConfig(id);
    message.success("审批人员配置已移除");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "移除失败");
  }
}
async function runPreview(flowCode: string) {
  try {
    previewResults.value[flowCode] = await previewApprovalFlow({
      flowCode,
      ...previewForms[flowCode],
    });
  } catch (error) {
    message.error(error instanceof Error ? error.message : "审批路径预览失败");
  }
}
function roleUserCount(roleId?: string) {
  if (!roleId) return 0;
  return roles.value.find((role) => role.id === roleId)?.userCount ?? 0;
}
function flowWarnings(group: {
  items: ApprovalConfigResponse[];
  steps: Array<{ stepNo: number; items: ApprovalConfigResponse[] }>;
}) {
  const warnings: string[] = [];
  const enabledItems = group.items.filter((item) => item.enabled);
  if (!enabledItems.length) warnings.push("无启用规则");
  if (
    !enabledItems.some(
      (item) => item.sequenceNo === 1 && item.conditionType === "ANY",
    )
  )
    warnings.push("缺少默认第1步");
  if (
    enabledItems.some(
      (item) =>
        item.assigneeType === "ROLE" && roleUserCount(item.roleId) === 0,
    )
  )
    warnings.push("存在空角色");
  if (
    enabledItems.some(
      (item) => item.assigneeType === "AUTO" && item.conditionType === "ANY",
    )
  )
    warnings.push("自动通过过宽");
  if (hasAmountOverlap(group.steps)) warnings.push("金额区间重叠");
  return warnings;
}
function hasAmountOverlap(
  steps: Array<{ stepNo: number; items: ApprovalConfigResponse[] }>,
) {
  return steps.some((step) => {
    const ranges = step.items
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
function conditionLabel(value: ApprovalConfigResponse["conditionType"]) {
  return (
    (
      {
        ANY: "全部",
        AMOUNT: "金额",
        DEPARTMENT: "部门",
        AMOUNT_AND_DEPARTMENT: "金额+部门",
        BUSINESS_TYPE: "业务",
        PROJECT: "项目",
        SUPPLIER_RISK: "供应商",
        CUSTOMER_LEVEL: "客户",
        COMPOSITE: "复合",
      } as Record<string, string>
    )[value] || value
  );
}
function conditionColor(value: ApprovalConfigResponse["conditionType"]) {
  return (
    (
      {
        ANY: "default",
        AMOUNT: "orange",
        DEPARTMENT: "blue",
        AMOUNT_AND_DEPARTMENT: "purple",
      } as Record<string, string>
    )[value] || "default"
  );
}
function assigneeTypeLabel(value: ApprovalConfigResponse["assigneeType"]) {
  return (
    (
      { ROLE: "角色", USER: "人员", DYNAMIC: "动态", AUTO: "自动" } as Record<
        string,
        string
      >
    )[value] || value
  );
}
function assigneeTypeColor(value: ApprovalConfigResponse["assigneeType"]) {
  return (
    (
      {
        ROLE: "purple",
        USER: "blue",
        DYNAMIC: "cyan",
        AUTO: "green",
      } as Record<string, string>
    )[value] || "default"
  );
}
function stepDescription(step: ApprovalFlowPreview["steps"][number]) {
  const extras = [step.assignees.join("、") || "无审批人"];
  if (step.autoApproved) extras.push("自动通过");
  if (step.slaHours) extras.push(`SLA ${step.slaHours}小时`);
  if (step.escalationRoleName) extras.push(`升级 ${step.escalationRoleName}`);
  return extras.join(" · ");
}
function conditionText(record: ApprovalConfigResponse) {
  const parts = [];
  if (record.minAmount != null || record.maxAmount != null)
    parts.push(`${record.minAmount ?? 0} - ${record.maxAmount ?? "不限"}`);
  if (record.departmentName) parts.push(record.departmentName);
  if (record.businessType) parts.push(`业务 ${record.businessType}`);
  if (record.projectCode) parts.push(`项目 ${record.projectCode}`);
  if (record.supplierRisk) parts.push(`供应商 ${record.supplierRisk}`);
  if (record.customerLevel) parts.push(`客户 ${record.customerLevel}`);
  if (record.priority) parts.push(`优先级 ${record.priority}`);
  if (record.escalationRoleName)
    parts.push(`升级 ${record.escalationRoleName}`);
  if (record.remark) parts.push(record.remark);
  return parts.join(" · ") || "所有单据适用";
}
onMounted(loadData);
</script>

<style scoped>
.form-hint {
  display: block;
  margin-top: 6px;
  color: #8c8c8c;
  font-size: 12px;
}
.approval-flow-collapse {
  background: transparent;
}
.flow-panel-title {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.flow-panel-title span {
  color: #64748b;
  font-size: 12px;
}
.flow-warnings {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 4px;
}
.step-stack {
  display: grid;
  gap: 14px;
}
.step-section {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 10px;
  background: #fff;
}
.step-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  color: #64748b;
}
.preview-panel {
  margin-top: 14px;
  padding: 12px;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
  background: #f8fafc;
}
.preview-title {
  margin-bottom: 10px;
  font-weight: 600;
}
.preview-result {
  display: grid;
  gap: 12px;
  margin-top: 12px;
}
.preview-steps {
  padding: 4px 6px;
}
:deep(.clickable-row) {
  cursor: pointer;
}
:deep(.clickable-row:hover td) {
  background: #f8fafc !important;
}
</style>
