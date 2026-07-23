<template>
  <div class="page-stack process-rule-page">
    <a-card :bordered="false">
      <template #title>流程规则配置</template>
      <template #extra>
        <a-space>
          <a-button @click="loadData">刷新</a-button>
          <a-button type="primary" :loading="savingAll" @click="saveAll"
            >保存全部模板</a-button
          >
        </a-space>
      </template>
      <a-alert
        type="info"
        show-icon
        message="阶段闸口、SLA、低毛利阈值、供应商评分规则集中维护。保存后进入风险中心、业务待办和相关流程判断，无需改代码。"
      />
    </a-card>

    <a-row :gutter="[16, 16]">
      <a-col v-for="item in ruleForms" :key="item.ruleCode" :xs="24" :xl="12">
        <a-card :bordered="false" class="rule-card">
          <template #title>
            <a-space>
              <span>{{ item.name }}</span>
              <a-tag>{{ moduleLabel(item.module) }}</a-tag>
            </a-space>
          </template>
          <template #extra>
            <a-switch
              v-model:checked="item.enabled"
              checked-children="启用"
              un-checked-children="停用"
            />
          </template>

          <a-form layout="vertical">
            <a-row :gutter="12">
              <a-col :xs="24" :md="12">
                <a-form-item :label="thresholdLabel(item, 'high')">
                  <a-input-number
                    v-model:value="item.highThreshold"
                    :min="0"
                    :precision="2"
                    class="full-input"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="12">
                <a-form-item :label="thresholdLabel(item, 'medium')">
                  <a-input-number
                    v-model:value="item.mediumThreshold"
                    :min="0"
                    :precision="2"
                    class="full-input"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="12">
                <a-form-item label="预警提前天数">
                  <a-input-number
                    v-model:value="item.warningDays"
                    :min="0"
                    :precision="0"
                    class="full-input"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="12">
                <a-form-item label="SLA处理时限(小时)">
                  <a-input-number
                    v-model:value="item.slaHours"
                    :min="1"
                    :precision="0"
                    class="full-input"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="12">
                <a-form-item label="默认责任人">
                  <a-input
                    v-model:value="item.defaultOwner"
                    placeholder="例如：项目经理"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="12">
                <a-form-item label="升级责任人">
                  <a-input
                    v-model:value="item.escalationOwner"
                    placeholder="例如：运营负责人"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="24">
                <a-form-item label="规则说明">
                  <a-textarea v-model:value="item.remark" :rows="2" />
                </a-form-item>
              </a-col>
            </a-row>
          </a-form>

          <div class="rule-footer">
            <span>{{ previewText(item) }}</span>
            <a-button
              type="primary"
              size="small"
              :loading="savingCode === item.ruleCode"
              @click="saveOne(item)"
              >保存规则</a-button
            >
          </div>
        </a-card>
      </a-col>
    </a-row>

    <a-card :bordered="false">
      <template #title>已配置规则</template>
      <a-table
        :columns="columns"
        :data-source="rules"
        :loading="loading"
        row-key="ruleCode"
        :pagination="{ pageSize: 10 }"
        :scroll="{ x: 1280 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <strong>{{ record.name }}</strong>
            <span class="table-subtitle">{{ record.ruleCode }}</span>
          </template>
          <template v-else-if="column.key === 'module'">
            <a-tag>{{ moduleLabel(record.module) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'enabled'">
            <a-tag :color="record.enabled ? 'green' : 'default'">{{
              record.enabled ? "启用" : "停用"
            }}</a-tag>
          </template>
          <template v-else-if="column.key === 'thresholds'">
            <span>{{ thresholdText(record) }}</span>
          </template>
          <template v-else-if="column.key === 'owner'">
            <span>{{ record.defaultOwner || "-" }}</span>
            <span class="table-subtitle"
              >升级：{{ record.escalationOwner || "-" }}</span
            >
          </template>
          <template v-else-if="column.key === 'source'">
            <a-tag :color="record.id ? 'blue' : 'default'">
              {{ record.id ? "已配置" : "系统默认" }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button type="link" @click="openEdit(record)">修改</a-button>
              <a-popconfirm
                v-if="record.id"
                :title="deleteConfirmText(record)"
                ok-text="确定"
                cancel-text="取消"
                @confirm="removeRule(record)"
              >
                <a-button type="link" danger :loading="deletingId === record.id"
                  >删除</a-button
                >
              </a-popconfirm>
              <a-tooltip v-else title="系统默认规则没有可删除的自定义配置">
                <a-button type="link" danger disabled>删除</a-button>
              </a-tooltip>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="editOpen"
      title="修改流程规则"
      width="720px"
      :confirm-loading="editing"
      ok-text="保存修改"
      cancel-text="取消"
      @ok="submitEdit"
    >
      <a-form
        ref="editFormRef"
        :model="editForm"
        :rules="formRules"
        layout="vertical"
      >
        <a-row :gutter="12">
          <a-col :xs="24" :md="12">
            <a-form-item label="规则编码" name="ruleCode">
              <a-input v-model:value="editForm.ruleCode" disabled />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="规则名称" name="name">
              <a-input
                v-model:value="editForm.name"
                :maxlength="120"
                show-count
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="所属模块" name="module">
              <a-select
                v-model:value="editForm.module"
                :options="moduleOptions"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="规则状态" name="enabled">
              <a-switch
                v-model:checked="editForm.enabled"
                checked-children="启用"
                un-checked-children="停用"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="高危阈值" name="highThreshold">
              <a-input-number
                v-model:value="editForm.highThreshold"
                :min="0"
                :precision="2"
                class="full-input"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="关注阈值" name="mediumThreshold">
              <a-input-number
                v-model:value="editForm.mediumThreshold"
                :min="0"
                :precision="2"
                class="full-input"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="预警提前天数" name="warningDays">
              <a-input-number
                v-model:value="editForm.warningDays"
                :min="0"
                :precision="0"
                class="full-input"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="SLA处理时限(小时)" name="slaHours">
              <a-input-number
                v-model:value="editForm.slaHours"
                :min="1"
                :precision="0"
                class="full-input"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="默认责任人" name="defaultOwner">
              <a-input v-model:value="editForm.defaultOwner" :maxlength="80" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="升级责任人" name="escalationOwner">
              <a-input
                v-model:value="editForm.escalationOwner"
                :maxlength="80"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="规则说明" name="remark">
              <a-textarea
                v-model:value="editForm.remark"
                :rows="3"
                :maxlength="500"
                show-count
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <a-alert
        v-if="editingSystemDefault"
        type="info"
        show-icon
        message="保存后将生成该系统规则的自定义配置；删除自定义配置时会恢复系统默认值。"
      />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { message, type FormInstance } from "ant-design-vue";
import {
  deleteRiskRule,
  listRiskRules,
  saveRiskRule,
  updateRiskRule,
  type RiskRuleConfigResponse,
} from "@/api/risk";

type RuleForm = Omit<RiskRuleConfigResponse, "id">;

const templates: RuleForm[] = [
  {
    ruleCode: "project_stage_gate",
    name: "项目阶段闸口",
    module: "project",
    enabled: true,
    highThreshold: 100,
    mediumThreshold: 80,
    warningDays: 0,
    slaHours: 48,
    defaultOwner: "项目经理",
    escalationOwner: "项目总监",
    remark:
      "阶段推进前校验审批、预算、成本、资料、验收记录。高阈值表示必须满足项比例。",
  },
  {
    ruleCode: "crm_low_margin_quote",
    name: "报价低毛利阈值",
    module: "crm",
    enabled: true,
    highThreshold: 10,
    mediumThreshold: 15,
    warningDays: 0,
    slaHours: 24,
    defaultOwner: "销售经理",
    escalationOwner: "财务负责人",
    remark: "报价毛利率低于高危阈值时阻断或升级审批，低于关注阈值时提示复核。",
  },
  {
    ruleCode: "supplier_score_rule",
    name: "供应商评分规则",
    module: "procurement",
    enabled: true,
    highThreshold: 60,
    mediumThreshold: 80,
    warningDays: 0,
    slaHours: 72,
    defaultOwner: "采购负责人",
    escalationOwner: "运营负责人",
    remark: "供应商综合评分低于高危阈值列为高风险，低于关注阈值进入观察名单。",
  },
  {
    ruleCode: "business_todo_sla",
    name: "统一待办SLA",
    module: "office",
    enabled: true,
    highThreshold: 24,
    mediumThreshold: 72,
    warningDays: 1,
    slaHours: 48,
    defaultOwner: "业务负责人",
    escalationOwner: "总经理",
    remark: "跨模块待办超过SLA自动升级，高/中阈值用于区分超时严重程度。",
  },
];

const rules = ref<RiskRuleConfigResponse[]>([]);
const ruleForms = ref<RuleForm[]>(templates.map(cloneRule));
const loading = ref(false);
const savingCode = ref("");
const savingAll = ref(false);
const editOpen = ref(false);
const editing = ref(false);
const deletingId = ref("");
const editingId = ref("");
const editFormRef = ref<FormInstance>();
const editForm = ref<RuleForm>(cloneRule(templates[0]));
const editingSystemDefault = computed(() => !editingId.value);

const moduleOptions = [
  { label: "项目管理", value: "project" },
  { label: "CRM", value: "crm" },
  { label: "供应链采购", value: "procurement" },
  { label: "OA协同", value: "office" },
  { label: "财务资金", value: "finance" },
  { label: "库存管理", value: "inventory" },
  { label: "资质管理", value: "qualification" },
  { label: "维保", value: "maintenance" },
];

const formRules = {
  name: [{ required: true, whitespace: true, message: "请输入规则名称" }],
  module: [{ required: true, message: "请选择所属模块" }],
  slaHours: [
    {
      type: "number",
      min: 1,
      message: "SLA处理时限必须大于0",
      trigger: "change",
    },
  ],
};

const columns = [
  { title: "规则", key: "name", width: 220 },
  { title: "模块", key: "module", width: 110 },
  { title: "状态", key: "enabled", width: 90 },
  { title: "来源", key: "source", width: 100 },
  { title: "阈值 / SLA", key: "thresholds", width: 260 },
  { title: "责任人", key: "owner", width: 180 },
  { title: "说明", dataIndex: "remark", key: "remark" },
  { title: "操作", key: "actions", width: 140, fixed: "right" as const },
];

async function loadData() {
  loading.value = true;
  try {
    rules.value = await listRiskRules();
    ruleForms.value = templates.map((template) => ({
      ...cloneRule(template),
      ...stripId(
        rules.value.find((item) => item.ruleCode === template.ruleCode),
      ),
    }));
  } catch (error) {
    message.error(error instanceof Error ? error.message : "规则配置加载失败");
  } finally {
    loading.value = false;
  }
}

async function saveOne(item: RuleForm) {
  savingCode.value = item.ruleCode;
  try {
    await saveRiskRule(normalizeRule(item));
    message.success(`${item.name}已保存`);
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "规则保存失败");
  } finally {
    savingCode.value = "";
  }
}

async function saveAll() {
  savingAll.value = true;
  try {
    await Promise.all(
      ruleForms.value.map((item) => saveRiskRule(normalizeRule(item))),
    );
    message.success("流程规则模板已全部保存");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "批量保存失败");
  } finally {
    savingAll.value = false;
  }
}

function openEdit(item: RiskRuleConfigResponse) {
  editingId.value = item.id || "";
  editForm.value = normalizeRule(stripId(item) as RuleForm);
  editOpen.value = true;
}

async function submitEdit() {
  try {
    await editFormRef.value?.validate();
  } catch {
    return;
  }

  editing.value = true;
  try {
    const payload = normalizeRule(editForm.value);
    if (editingId.value) {
      await updateRiskRule(editingId.value, payload);
    } else {
      await saveRiskRule(payload);
    }
    message.success(`${payload.name}已修改`);
    editOpen.value = false;
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "规则修改失败");
  } finally {
    editing.value = false;
  }
}

async function removeRule(item: RiskRuleConfigResponse) {
  if (!item.id) return;
  deletingId.value = item.id;
  try {
    await deleteRiskRule(item.id);
    const restoresDefault = systemDefaultCodes.has(item.ruleCode);
    message.success(
      restoresDefault
        ? `${item.name}的自定义配置已删除，已恢复系统默认值`
        : `${item.name}已删除`,
    );
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "规则删除失败");
  } finally {
    deletingId.value = "";
  }
}

const systemDefaultCodes = new Set([
  "office_warning",
  "office_todo",
  "inventory_replenishment",
  "procurement_matching",
  "project_profitability",
  "finance_receivable_overdue",
  "finance_payable_overdue",
  "qualification_expiry",
  "crm_renewal",
  "maintenance_work_order_overdue",
]);

function deleteConfirmText(item: RiskRuleConfigResponse) {
  return systemDefaultCodes.has(item.ruleCode)
    ? `确定删除“${item.name}”的自定义配置并恢复系统默认值？`
    : `确定删除规则“${item.name}”？删除后将不再参与流程判断。`;
}

function normalizeRule(item: RuleForm): RuleForm {
  return {
    ruleCode: item.ruleCode,
    name: item.name,
    module: item.module,
    enabled: item.enabled,
    highThreshold: numberOrUndefined(item.highThreshold),
    mediumThreshold: numberOrUndefined(item.mediumThreshold),
    warningDays: numberOrUndefined(item.warningDays),
    slaHours: numberOrUndefined(item.slaHours),
    defaultOwner: item.defaultOwner || undefined,
    escalationOwner: item.escalationOwner || undefined,
    remark: item.remark || undefined,
  };
}

function stripId(item?: RiskRuleConfigResponse): Partial<RuleForm> {
  if (!item) return {};
  const { id: _id, ...rest } = item;
  return rest;
}

function cloneRule(item: RuleForm): RuleForm {
  return { ...item };
}

function numberOrUndefined(value?: number) {
  return value == null ? undefined : Number(value);
}

function moduleLabel(module: string) {
  return (
    (
      {
        project: "项目管理",
        crm: "CRM",
        procurement: "供应链采购",
        office: "OA协同",
        finance: "财务资金",
        inventory: "库存管理",
        qualification: "资质管理",
        maintenance: "维保",
      } as Record<string, string>
    )[module] || module
  );
}

function thresholdLabel(item: RuleForm, level: "high" | "medium") {
  if (item.ruleCode === "project_stage_gate")
    return level === "high" ? "强制闸口满足率(%)" : "预警闸口满足率(%)";
  if (item.ruleCode === "crm_low_margin_quote")
    return level === "high" ? "高危毛利率下限(%)" : "关注毛利率下限(%)";
  if (item.ruleCode === "supplier_score_rule")
    return level === "high" ? "高风险评分线" : "观察评分线";
  return level === "high" ? "高危阈值" : "关注阈值";
}

function previewText(item: RuleForm) {
  if (!item.enabled) return "规则停用后不会参与流程判断和风险升级。";
  return `高危 ${item.highThreshold ?? "-"} · 关注 ${item.mediumThreshold ?? "-"} · SLA ${item.slaHours ?? "-"} 小时`;
}

function thresholdText(item: RiskRuleConfigResponse) {
  const parts = [];
  if (item.highThreshold != null) parts.push(`高危 ${item.highThreshold}`);
  if (item.mediumThreshold != null) parts.push(`关注 ${item.mediumThreshold}`);
  if (item.warningDays != null) parts.push(`提前 ${item.warningDays} 天`);
  if (item.slaHours != null) parts.push(`SLA ${item.slaHours} 小时`);
  return parts.join(" · ") || "-";
}

onMounted(loadData);
</script>

<style scoped>
.process-rule-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.rule-card {
  height: 100%;
  border-radius: 8px;
}

.full-input {
  width: 100%;
}

.rule-footer {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #eef2f7;
  color: #667085;
  font-size: 12px;
}

@media (max-width: 768px) {
  .rule-footer {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
