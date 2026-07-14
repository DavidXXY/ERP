<template>
  <div class="page-stack approval-config-page">
    <a-card>
      <template #title>审批流配置</template>
      <template #extra><a-button type="primary" @click="openAdd()">新增规则</a-button></template>
      <a-alert type="info" show-icon message="按审批流集中维护规则；每条规则可指定人员或角色作为审批对象。" style="margin-bottom: 16px" />
      <a-collapse v-model:active-key="activeFlowKeys" class="approval-flow-collapse">
        <a-collapse-panel v-for="group in groupedFlows" :key="group.flowCode">
          <template #header>
            <div class="flow-panel-title">
              <strong>{{ group.flowName }}</strong>
              <span>{{ group.flowCode }} · {{ group.items.length }} 条规则 · {{ group.maxStep }} 步</span>
            </div>
          </template>
          <template #extra>
            <a-button size="small" type="link" @click.stop="openAdd(group.flowCode)">新增规则</a-button>
          </template>
          <a-table
            :data-source="group.items"
            :columns="columns"
            :loading="loading"
            row-key="id"
            size="small"
            :pagination="false"
            :custom-row="(record: ApprovalConfigResponse) => ({ onClick: () => openEdit(record) })"
            :row-class-name="() => 'clickable-row'"
          >
            <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'assignee'">
            <a-tag :color="record.assigneeType === 'ROLE' ? 'purple' : 'blue'">{{ record.assigneeType === 'ROLE' ? '角色' : '人员' }}</a-tag>
            {{ record.assigneeName }}
          </template>
          <template v-else-if="column.key === 'mode'"><a-tag :color="record.approvalMode === 'SEQUENTIAL' ? 'blue' : 'green'">{{ record.approvalMode === 'SEQUENTIAL' ? `依次 · 第${record.sequenceNo}步` : '同步' }}</a-tag></template>
          <template v-else-if="column.key === 'condition'">
            <a-tag :color="conditionColor(record.conditionType)">{{ conditionLabel(record.conditionType) }}</a-tag>
            <span class="table-subtitle">{{ conditionText(record) }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" @click.stop="openEdit(record)">编辑</a-button>
              <a-popconfirm title="确定移除该审批人配置？" @confirm="removeConfig(record.id)">
                <a-button type="link" danger @click.stop>移除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
            </template>
          </a-table>
        </a-collapse-panel>
      </a-collapse>
    </a-card>

    <a-modal v-model:open="addOpen" :title="editingId ? '编辑审批规则' : '新增审批规则'" :confirm-loading="saving" @ok="saveConfig">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="审批流程" name="flowCode"><a-select v-model:value="form.flowCode" :options="flowOptions" /></a-form-item>
        <a-form-item label="审批对象类型" name="assigneeType">
          <a-radio-group v-model:value="form.assigneeType">
            <a-radio-button value="ROLE">角色</a-radio-button>
            <a-radio-button value="USER">人员</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="form.assigneeType === 'ROLE' ? '审批角色' : '审批人员'" name="targetIds">
          <a-select
            v-model:value="form.targetIds"
            mode="multiple"
            show-search
            option-filter-prop="label"
            :max-tag-count="4"
            :placeholder="editingId ? '请选择 1 个审批对象' : '可选择多个审批对象'"
            :options="targetOptions"
          />
        </a-form-item>
        <a-form-item label="审批模式" name="approvalMode"><a-radio-group v-model:value="form.approvalMode"><a-radio value="PARALLEL">同步审批（任一人通过）</a-radio><a-radio value="SEQUENTIAL">依次审批</a-radio></a-radio-group></a-form-item>
        <a-form-item v-if="form.approvalMode === 'SEQUENTIAL'" label="起始审批顺序" name="sequenceNo">
          <a-input-number v-model:value="form.sequenceNo" :min="1" :precision="0" />
          <span class="form-hint">{{ editingId ? "当前审批配置所在步骤。" : "多人依次审批时，会按选择顺序从该数字开始递增。" }}</span>
        </a-form-item>
        <a-form-item label="适用条件" name="conditionType"><a-select v-model:value="form.conditionType" :options="conditionOptions" /></a-form-item>
        <a-row v-if="form.conditionType === 'AMOUNT' || form.conditionType === 'AMOUNT_AND_DEPARTMENT'" :gutter="12">
          <a-col :span="12"><a-form-item label="最小金额"><a-input-number v-model:value="form.minAmount" :min="0" :precision="2" style="width:100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="最大金额"><a-input-number v-model:value="form.maxAmount" :min="0" :precision="2" style="width:100%" /></a-form-item></a-col>
        </a-row>
        <a-form-item v-if="form.conditionType === 'DEPARTMENT' || form.conditionType === 'AMOUNT_AND_DEPARTMENT'" label="适用部门/组织">
          <a-input v-model:value="form.departmentName" placeholder="例如：工程部、财务部、华东区域" />
        </a-form-item>
        <a-row v-if="form.conditionType === 'BUSINESS_TYPE' || form.conditionType === 'PROJECT' || form.conditionType === 'SUPPLIER_RISK' || form.conditionType === 'CUSTOMER_LEVEL' || form.conditionType === 'COMPOSITE'" :gutter="12">
          <a-col :span="12"><a-form-item label="业务类型"><a-input v-model:value="form.businessType" placeholder="例如：TOOL、咨询服务" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="项目编码"><a-input v-model:value="form.projectCode" placeholder="例如：PRJ-001" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="供应商风险"><a-select v-model:value="form.supplierRisk" allow-clear :options="supplierRiskOptions" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="客户等级"><a-input v-model:value="form.customerLevel" placeholder="例如：A、VIP、NORMAL" /></a-form-item></a-col>
        </a-row>
        <a-form-item label="规则优先级"><a-input-number v-model:value="form.priority" :min="1" :precision="0" style="width:100%" /></a-form-item>
        <a-form-item label="规则备注"><a-textarea v-model:value="form.remark" :rows="2" placeholder="例如：金额超过 5 万需财务经理审批" /></a-form-item>
        <a-form-item v-if="editingId" label="启用状态"><a-switch v-model:checked="form.enabled" checked-children="启用" un-checked-children="停用" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message, type FormInstance } from "ant-design-vue";
import { createApprovalConfig, deleteApprovalConfig, listApprovalConfigs, listRolesApi, listUsersApi, updateApprovalConfig, type ApprovalConfigResponse, type RoleResponse, type UserResponse } from "@/api/system";

const flowOptions = [
  { label: "报价审批", value: "QUOTE", flowName: "报价审批" },
  { label: "合同审批", value: "CONTRACT", flowName: "合同审批" },
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
const configs = ref<ApprovalConfigResponse[]>([]); const users = ref<UserResponse[]>([]); const roles = ref<RoleResponse[]>([]); const loading = ref(false); const saving = ref(false); const addOpen = ref(false); const editingId = ref<string | null>(null); const activeFlowKeys = ref<string[]>([]); const formRef = ref<FormInstance>();
const form = reactive({ flowCode: "QUOTE", assigneeType: "ROLE" as "USER" | "ROLE", targetIds: [] as string[], approvalMode: "SEQUENTIAL" as "PARALLEL" | "SEQUENTIAL", sequenceNo: 1, conditionType: "ANY" as ApprovalConfigResponse["conditionType"], minAmount: undefined as number | undefined, maxAmount: undefined as number | undefined, departmentName: "", businessType: "", projectCode: "", supplierRisk: undefined as string | undefined, customerLevel: "", priority: 100, remark: "", enabled: true });
const userOptions = computed(() => users.value.filter(item => item.enabled).map(item => ({ label: `${item.displayName} · ${item.username}`, value: item.id })));
const roleOptions = computed(() => roles.value.map(item => ({ label: `${item.name} · ${item.code}`, value: item.id })));
const targetOptions = computed(() => form.assigneeType === "ROLE" ? roleOptions.value : userOptions.value);
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
const supplierRiskOptions = [{ label: "正常", value: "NORMAL" }, { label: "关注", value: "WATCHLIST" }, { label: "冻结", value: "BLOCKED" }];
const columns = [{ title: "审批对象", key: "assignee", width: 180 }, { title: "模式", key: "mode", width: 140 }, { title: "适用条件", key: "condition", width: 360 }, { title: "操作", key: "action", width: 120 }];
const rules = { flowCode: [{ required: true, message: "请选择审批流程" }], assigneeType: [{ required: true, message: "请选择审批对象类型" }], targetIds: [{ required: true, type: "array", min: 1, message: "请选择审批对象" }], approvalMode: [{ required: true }], sequenceNo: [{ required: true, message: "请输入审批顺序" }] };
const groupedFlows = computed(() => flowOptions.map(flow => {
  const items = configs.value.filter(item => item.flowCode === flow.value).sort((a, b) => a.sequenceNo - b.sequenceNo || a.priority - b.priority);
  return { flowCode: flow.value, flowName: flow.flowName, items, maxStep: items.reduce((max, item) => Math.max(max, item.sequenceNo), 0) };
}));

async function loadData() { loading.value = true; try { const [configData, userData, roleData] = await Promise.all([listApprovalConfigs(), listUsersApi(0, 200), listRolesApi(0, 200)]); configs.value = configData; users.value = userData.content; roles.value = roleData.content; if (!activeFlowKeys.value.length) activeFlowKeys.value = flowOptions.map(item => item.value); } catch (error) { message.error(error instanceof Error ? error.message : "审批配置加载失败"); } finally { loading.value = false; } }
function openAdd(flowCode = "QUOTE") { editingId.value = null; Object.assign(form, { flowCode, assigneeType: "ROLE", targetIds: [], approvalMode: "SEQUENTIAL", sequenceNo: 1, conditionType: "ANY", minAmount: undefined, maxAmount: undefined, departmentName: "", businessType: "", projectCode: "", supplierRisk: undefined, customerLevel: "", priority: 100, remark: "", enabled: true }); addOpen.value = true; }
function openEdit(record: ApprovalConfigResponse) {
  editingId.value = record.id;
  Object.assign(form, {
    flowCode: record.flowCode,
    assigneeType: record.assigneeType,
    targetIds: [record.assigneeType === "ROLE" ? record.roleId : record.userId].filter(Boolean) as string[],
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
    remark: record.remark || "",
    enabled: record.enabled,
  });
  addOpen.value = true;
}
async function saveConfig() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    const flow = flowOptions.find(item => item.value === form.flowCode)!;
    const uniqueTargetIds = [...new Set(form.targetIds)];
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
          approvalMode: form.approvalMode,
          sequenceNo: form.approvalMode === "SEQUENTIAL" ? form.sequenceNo + index : form.sequenceNo,
          conditionType: form.conditionType,
          minAmount: form.minAmount,
          maxAmount: form.maxAmount,
          departmentName: form.departmentName,
          businessType: form.businessType,
          projectCode: form.projectCode,
          supplierRisk: form.supplierRisk,
          customerLevel: form.customerLevel,
          priority: form.priority,
          remark: form.remark,
        });
      }
      message.success(`已配置 ${uniqueTargetIds.length} 个审批对象`);
    }
    addOpen.value = false;
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "配置保存失败");
  } finally {
    saving.value = false;
  }
}
async function removeConfig(id: string) { try { await deleteApprovalConfig(id); message.success("审批人员配置已移除"); await loadData(); } catch (error) { message.error(error instanceof Error ? error.message : "移除失败"); } }
function conditionLabel(value: ApprovalConfigResponse["conditionType"]) { return ({ ANY: "全部", AMOUNT: "金额", DEPARTMENT: "部门", AMOUNT_AND_DEPARTMENT: "金额+部门", BUSINESS_TYPE: "业务", PROJECT: "项目", SUPPLIER_RISK: "供应商", CUSTOMER_LEVEL: "客户", COMPOSITE: "复合" } as Record<string, string>)[value] || value; }
function conditionColor(value: ApprovalConfigResponse["conditionType"]) { return ({ ANY: "default", AMOUNT: "orange", DEPARTMENT: "blue", AMOUNT_AND_DEPARTMENT: "purple" } as Record<string, string>)[value] || "default"; }
function conditionText(record: ApprovalConfigResponse) {
  const parts = [];
  if (record.minAmount != null || record.maxAmount != null) parts.push(`${record.minAmount ?? 0} - ${record.maxAmount ?? "不限"}`);
  if (record.departmentName) parts.push(record.departmentName);
  if (record.businessType) parts.push(`业务 ${record.businessType}`);
  if (record.projectCode) parts.push(`项目 ${record.projectCode}`);
  if (record.supplierRisk) parts.push(`供应商 ${record.supplierRisk}`);
  if (record.customerLevel) parts.push(`客户 ${record.customerLevel}`);
  if (record.priority) parts.push(`优先级 ${record.priority}`);
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
:deep(.clickable-row) {
  cursor: pointer;
}
:deep(.clickable-row:hover td) {
  background: #f8fafc !important;
}
</style>
