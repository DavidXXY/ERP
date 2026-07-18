<template>
  <div class="page-stack">
    <a-card title="统一风险中心">
      <template #extra>
        <a-space>
          <a-button @click="showHighRisks">只看高风险</a-button>
          <a-button @click="showOverdueRisks">只看逾期</a-button>
          <a-button @click="exportRisks">导出</a-button>
          <a-button @click="handleSnapshot">今日快照</a-button>
          <a-button @click="handleEscalationScan">升级扫描</a-button>
          <a-button :loading="loading" @click="loadData">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </a-space>
      </template>

      <a-row :gutter="[16, 16]" class="metric-row">
        <a-col :xs="12" :lg="6"
          ><a-statistic
            title="风险总数"
            :value="filteredItems.length"
            suffix="项"
        /></a-col>
        <a-col :xs="12" :lg="6"
          ><a-statistic
            title="高风险"
            :value="severityCount('HIGH')"
            suffix="项"
            :value-style="{
              color: severityCount('HIGH') > 0 ? '#cf1322' : '#237804',
            }"
        /></a-col>
        <a-col :xs="12" :lg="6"
          ><a-statistic
            title="SLA超时"
            :value="slaOverdueCount"
            suffix="项"
            :value-style="{
              color: slaOverdueCount > 0 ? '#cf1322' : '#237804',
            }"
        /></a-col>
        <a-col :xs="12" :lg="6"
          ><a-statistic title="已关闭/忽略" :value="resolvedCount" suffix="项"
        /></a-col>
      </a-row>

      <section class="task-closure-panel">
        <div class="task-closure-title">
          <div>
            <h3>风险任务收口</h3>
            <p>
              按当前筛选条件汇总待认领、处理中、超时和金额敞口，便于当天清单化推进。
            </p>
          </div>
          <a-space>
            <a-button size="small" @click="workflowFilter = 'UNCLAIMED'"
              >未认领</a-button
            >
            <a-button size="small" @click="workflowFilter = 'PROCESSING'"
              >处理中</a-button
            >
            <a-button size="small" @click="showOverdueRisks">SLA超时</a-button>
          </a-space>
        </div>
        <div class="task-closure-grid">
          <button
            v-for="card in taskCards"
            :key="card.key"
            class="task-closure-card"
            type="button"
            @click="card.action"
          >
            <span>{{ card.label }}</span>
            <strong>{{ card.value }}</strong>
            <em>{{ card.hint }}</em>
          </button>
        </div>
      </section>

      <section class="closure-command-panel">
        <div class="closure-command-head">
          <div>
            <h3>闭环指挥台</h3>
            <p>把风险按责任、时限和下一步动作拆开，便于当天推进和复盘。</p>
          </div>
          <a-space>
            <a-button size="small" @click="claimTopRisk">认领首项</a-button>
            <a-button
              size="small"
              @click="
                workflowFilter = undefined;
                statusFilter = undefined;
              "
              >看全部</a-button
            >
          </a-space>
        </div>
        <div class="closure-command-grid">
          <div class="closure-command-block">
            <div class="block-title">
              <span>责任人队列</span>
              <strong>{{ ownerQueues.length }} 组</strong>
            </div>
            <button
              v-for="owner in ownerQueues"
              :key="owner.owner"
              class="owner-row"
              type="button"
              @click="filterOwner(owner.owner)"
            >
              <span>
                <strong>{{ owner.owner }}</strong>
                <small>{{ owner.modules }}</small>
              </span>
              <em>{{ owner.count }} 项</em>
            </button>
            <a-empty
              v-if="ownerQueues.length === 0"
              description="暂无待推进责任队列"
            />
          </div>
          <div class="closure-command-block">
            <div class="block-title">
              <span>SLA 临期</span>
              <strong>{{ dueSoonItems.length }} 项</strong>
            </div>
            <button
              v-for="item in dueSoonItems"
              :key="item.key"
              class="due-row"
              type="button"
              @click="openWorkflow(item)"
            >
              <span>
                <strong>{{ item.title }}</strong>
                <small>{{ item.subject }}</small>
              </span>
              <em :class="{ danger: item.slaOverdue }">{{ slaText(item) }}</em>
            </button>
            <a-empty
              v-if="dueSoonItems.length === 0"
              description="暂无临期或超时事项"
            />
          </div>
          <div class="closure-command-block">
            <div class="block-title">
              <span>推荐动作</span>
              <strong>{{ recommendedActions.length }} 条</strong>
            </div>
            <button
              v-for="item in recommendedActions"
              :key="item.key"
              class="action-suggestion-row"
              type="button"
              @click="openWorkflowWithSuggestion(item)"
            >
              <a-tag :color="item.color">{{ item.moduleName }}</a-tag>
              <span>
                <strong>{{ item.action }}</strong>
                <small>{{ item.subject }}</small>
              </span>
            </button>
            <a-empty
              v-if="recommendedActions.length === 0"
              description="暂无推荐动作"
            />
          </div>
        </div>
        <div class="closure-review-strip">
          <button
            v-for="item in reviewCards"
            :key="item.key"
            class="review-card"
            type="button"
            @click="item.action"
          >
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <em>{{ item.hint }}</em>
          </button>
        </div>
      </section>

      <a-row :gutter="[16, 16]" class="metric-row" v-if="summary">
        <a-col :xs="24" :lg="12">
          <section class="risk-summary-panel">
            <h3>模块风险分布</h3>
            <a-space wrap>
              <a-tag
                v-for="module in summary.modules"
                :key="module.module"
                :color="
                  module.highCount
                    ? 'red'
                    : module.slaOverdueCount
                      ? 'orange'
                      : 'blue'
                "
              >
                {{ module.moduleName }} {{ module.totalCount }} / 高
                {{ module.highCount }} / SLA {{ module.slaOverdueCount }}
              </a-tag>
            </a-space>
          </section>
        </a-col>
        <a-col :xs="24" :lg="12">
          <section class="risk-summary-panel">
            <h3>近14日快照趋势</h3>
            <a-space wrap>
              <a-tag
                v-for="point in summary.trends.slice(-7)"
                :key="point.date"
              >
                {{ point.date.slice(5) }} 总 {{ point.totalCount }} 高
                {{ point.highCount }} 关 {{ point.closedCount }}
              </a-tag>
            </a-space>
          </section>
        </a-col>
      </a-row>

      <a-space wrap class="table-toolbar">
        <a-input-search
          v-model:value="keyword"
          allow-clear
          placeholder="搜索风险、对象、说明"
          style="width: 280px"
        />
        <a-select
          v-model:value="moduleFilter"
          allow-clear
          placeholder="全部模块"
          :options="moduleOptions"
          style="width: 150px"
        />
        <a-select
          v-model:value="severityFilter"
          allow-clear
          placeholder="全部等级"
          :options="severityOptions"
          style="width: 140px"
        />
        <a-select
          v-model:value="statusFilter"
          allow-clear
          placeholder="全部状态"
          :options="statusOptions"
          style="width: 140px"
        />
        <a-select
          v-model:value="workflowFilter"
          allow-clear
          placeholder="处理状态"
          :options="workflowOptions"
          style="width: 150px"
        />
        <a-select
          v-model:value="ownerFilter"
          allow-clear
          placeholder="责任人"
          :options="ownerOptions"
          style="width: 150px"
        />
        <a-select
          v-model:value="batchStatus"
          placeholder="批量处理"
          :options="workflowOptions"
          style="width: 150px"
        />
        <a-button
          :disabled="!selectedRowKeys.length"
          @click="handleBatchWorkflow"
          >批量更新</a-button
        >
      </a-space>

      <a-alert
        v-if="loadNotes.length"
        class="section-alert"
        type="info"
        show-icon
        :message="`已按当前权限加载风险数据，跳过 ${loadNotes.length} 个不可用来源`"
      />

      <a-table
        :columns="columns"
        :data-source="filteredItems"
        :loading="loading"
        :pagination="{ pageSize: 12 }"
        :row-key="(record: RiskItem) => record.key"
        :row-selection="{ selectedRowKeys, onChange: onSelectionChange }"
        :scroll="{ x: 1180 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'risk'">
            <a-button
              type="link"
              class="table-link"
              @click="goRoute(record.route)"
              >{{ record.title }}</a-button
            >
            <span class="table-subtitle">{{ record.subject }}</span>
          </template>
          <template v-else-if="column.key === 'module'"
            ><a-tag>{{ record.moduleName }}</a-tag></template
          >
          <template v-else-if="column.key === 'severity'"
            ><a-tag :color="severityColor(record.severity)">{{
              severityLabel(record.severity)
            }}</a-tag></template
          >
          <template v-else-if="column.key === 'status'"
            ><a-tag :color="statusColor(record.status)">{{
              statusLabel(record.status)
            }}</a-tag></template
          >
          <template v-else-if="column.key === 'workflow'">
            <a-tag :color="workflowColor(record.workflow.status)">{{
              workflowLabel(record.workflow.status)
            }}</a-tag>
            <span v-if="record.workflow.owner" class="table-subtitle">{{
              record.workflow.owner
            }}</span>
            <span v-if="record.workflow.updatedAt" class="table-subtitle">{{
              dateOnly(record.workflow.updatedAt)
            }}</span>
          </template>
          <template v-else-if="column.key === 'sla'">
            <a-tag
              :color="
                record.slaOverdue ? 'red' : record.dueAt ? 'blue' : 'default'
              "
              >{{
                record.slaOverdue ? "已超时" : record.dueAt ? "SLA" : "-"
              }}</a-tag
            >
            <span v-if="record.dueAt" class="table-subtitle">{{
              record.dueAt.slice(0, 16).replace("T", " ")
            }}</span>
          </template>
          <template v-else-if="column.key === 'amount'">{{
            record.amount ? formatMoney(record.amount) : "-"
          }}</template>
          <template v-else-if="column.key === 'date'">{{
            record.date || "-"
          }}</template>
          <template v-else-if="column.key === 'action'">
            <a-space :size="4">
              <a-button type="link" size="small" @click="openWorkflow(record)"
                >流转</a-button
              >
              <a-button type="link" size="small" @click="goRoute(record.route)"
                >业务</a-button
              >
            </a-space>
          </template>
        </template>
        <template #emptyText>当前没有需要处理的业务风险</template>
      </a-table>
    </a-card>

    <a-drawer v-model:open="workflowOpen" title="风险处理" :width="520">
      <template v-if="activeRisk">
        <a-descriptions bordered size="small" :column="1">
          <a-descriptions-item label="风险事项">{{
            activeRisk.title
          }}</a-descriptions-item>
          <a-descriptions-item label="对象">{{
            activeRisk.subject
          }}</a-descriptions-item>
          <a-descriptions-item label="说明">{{
            activeRisk.description
          }}</a-descriptions-item>
          <a-descriptions-item label="等级"
            ><a-tag :color="severityColor(activeRisk.severity)">{{
              severityLabel(activeRisk.severity)
            }}</a-tag></a-descriptions-item
          >
        </a-descriptions>

        <a-form layout="vertical" style="margin-top: 16px">
          <a-form-item label="处理状态">
            <a-radio-group
              v-model:value="workflowForm.status"
              button-style="solid"
            >
              <a-radio-button value="UNCLAIMED">未认领</a-radio-button>
              <a-radio-button value="CLAIMED">已认领</a-radio-button>
              <a-radio-button value="PROCESSING">处理中</a-radio-button>
              <a-radio-button value="IGNORED">忽略</a-radio-button>
              <a-radio-button value="CLOSED">关闭</a-radio-button>
            </a-radio-group>
          </a-form-item>
          <a-form-item label="处理人"
            ><a-input
              v-model:value="workflowForm.owner"
              placeholder="默认当前用户"
          /></a-form-item>
          <a-form-item label="处理备注"
            ><a-textarea v-model:value="workflowForm.note" :rows="3"
          /></a-form-item>
          <a-form-item
            v-if="
              workflowForm.status === 'CLOSED' ||
              workflowForm.status === 'IGNORED'
            "
            label="关闭/忽略原因"
          >
            <a-textarea v-model:value="workflowForm.reason" :rows="2" />
          </a-form-item>
          <template v-if="workflowForm.status === 'CLOSED'">
            <a-divider orientation="left">异常闭环复盘</a-divider>
            <a-form-item label="根因">
              <a-textarea
                v-model:value="workflowForm.rootCause"
                :rows="2"
                placeholder="例如：采购到货延期、客户验收资料不完整、预算变更未同步"
              />
            </a-form-item>
            <a-form-item label="责任部门">
              <a-input
                v-model:value="workflowForm.responsibleDepartment"
                placeholder="例如：项目部、采购部、财务部"
              />
            </a-form-item>
            <a-form-item label="是否复发">
              <a-radio-group v-model:value="workflowForm.recurrence">
                <a-radio :value="false">首次/偶发</a-radio>
                <a-radio :value="true">复发</a-radio>
              </a-radio-group>
            </a-form-item>
            <a-form-item label="下次预防动作">
              <a-textarea
                v-model:value="workflowForm.preventionAction"
                :rows="2"
                placeholder="例如：阶段闸口增加资料校验；供应商评分低于阈值自动升级审批"
              />
            </a-form-item>
          </template>
        </a-form>
        <a-space>
          <a-button type="primary" @click="saveWorkflow">保存处理状态</a-button>
          <a-button @click="goRoute(activeRisk.route)">打开业务页面</a-button>
        </a-space>

        <a-divider>处理轨迹</a-divider>
        <a-spin :spinning="actionLoading">
          <a-empty
            v-if="workflowActions.length === 0"
            description="暂无处理记录"
          />
          <a-timeline v-else>
            <a-timeline-item
              v-for="action in workflowActions"
              :key="`${action.createdAt}-${action.toStatus}`"
              :color="workflowColor(action.toStatus)"
            >
              <strong
                >{{ workflowLabel(action.fromStatus || "UNCLAIMED") }} →
                {{ workflowLabel(action.toStatus) }}</strong
              >
              <p style="margin: 2px 0; color: #8c8c8c; font-size: 12px">
                {{ action.operatorName || "-" }} ·
                {{ action.createdAt?.slice(0, 19)?.replace("T", " ") || "-" }}
              </p>
              <p v-if="action.owner" style="margin: 2px 0; font-size: 12px">
                处理人：{{ action.owner }}
              </p>
              <p v-if="action.note" style="margin: 2px 0; font-size: 12px">
                备注：{{ action.note }}
              </p>
              <p v-if="action.reason" style="margin: 2px 0; font-size: 12px">
                原因：{{ action.reason }}
              </p>
              <div
                v-if="
                  action.rootCause ||
                  action.responsibleDepartment ||
                  action.preventionAction
                "
                class="action-review-card"
              >
                <p v-if="action.rootCause">根因：{{ action.rootCause }}</p>
                <p v-if="action.responsibleDepartment">
                  责任部门：{{ action.responsibleDepartment }}
                </p>
                <p v-if="action.handlingHours != null">
                  处理耗时：{{ action.handlingHours }}h
                </p>
                <p v-if="action.recurrence != null">
                  是否复发：{{ action.recurrence ? "是" : "否" }}
                </p>
                <p v-if="action.preventionAction">
                  预防动作：{{ action.preventionAction }}
                </p>
              </div>
            </a-timeline-item>
          </a-timeline>
        </a-spin>
      </template>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { message } from "ant-design-vue";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { useRouter } from "vue-router";
import { listRenewals } from "@/api/crm";
import { listFinancePayables, listFinanceReceivables } from "@/api/finance";
import { listReplenishmentSuggestions } from "@/api/inventory";
import { getOfficeWorkbench } from "@/api/office";
import { listProcurementMatching } from "@/api/procurement";
import { listProjectProfitability } from "@/api/project";
import { listQualificationWarnings } from "@/api/qualification";
import {
  batchUpdateRiskWorkflow,
  getRiskSummary,
  listRiskItems,
  listRiskWorkflowActions,
  listRiskWorkflows,
  scanRiskEscalations,
  snapshotRiskToday,
  updateRiskWorkflow,
  type RiskItemResponse,
  type RiskSummaryResponse,
  type RiskWorkflowActionResponse,
  type RiskWorkflowStatus,
} from "@/api/risk";
import { useAuthStore } from "@/stores/auth";
import { downloadCsv } from "@/utils/csv";

type RiskSeverity = "HIGH" | "MEDIUM" | "LOW";
type RiskStatus = "OPEN" | "PENDING" | "OVERDUE";
type WorkflowStatus = RiskWorkflowStatus;
type RiskWorkflow = {
  status: WorkflowStatus;
  owner?: string;
  note?: string;
  reason?: string;
  updatedAt?: string;
  rootCause?: string;
  responsibleDepartment?: string;
  handlingHours?: number;
  recurrence?: boolean;
  preventionAction?: string;
};
type RiskItem = {
  key: string;
  module: string;
  moduleName: string;
  title: string;
  subject: string;
  description: string;
  severity: RiskSeverity;
  status: RiskStatus;
  amount?: number;
  date?: string;
  route: string;
  ruleCode?: string;
  slaHours?: number;
  dueAt?: string;
  slaOverdue: boolean;
  workflow: RiskWorkflow;
};

const FILTER_STORAGE_KEY = "ops.riskCenter.filters";
const WORKFLOW_STORAGE_KEY = "ops.riskCenter.workflow";
const router = useRouter();
const auth = useAuthStore();
const loading = ref(false);
const items = ref<RiskItem[]>([]);
const summary = ref<RiskSummaryResponse>();
const loadNotes = ref<string[]>([]);
const keyword = ref("");
const moduleFilter = ref<string>();
const severityFilter = ref<RiskSeverity>();
const statusFilter = ref<RiskStatus>();
const workflowFilter = ref<WorkflowStatus>();
const ownerFilter = ref<string>();
const workflowOpen = ref(false);
const activeRisk = ref<RiskItem | null>(null);
const workflowStore = ref<Record<string, RiskWorkflow>>({});
const workflowActions = ref<RiskWorkflowActionResponse[]>([]);
const actionLoading = ref(false);
const selectedRowKeys = ref<string[]>([]);
const batchStatus = ref<WorkflowStatus>("CLAIMED");
const workflowForm = reactive({
  status: "UNCLAIMED" as WorkflowStatus,
  owner: "",
  note: "",
  reason: "",
  rootCause: "",
  responsibleDepartment: "",
  recurrence: false,
  preventionAction: "",
});

const columns = [
  { title: "风险事项 / 对象", key: "risk", width: 320 },
  { title: "模块", key: "module", width: 110 },
  { title: "等级", key: "severity", width: 100 },
  { title: "状态", key: "status", width: 100 },
  { title: "处理", key: "workflow", width: 140 },
  { title: "SLA", key: "sla", width: 140 },
  { title: "金额", key: "amount", width: 140 },
  { title: "日期", key: "date", width: 120 },
  { title: "说明", dataIndex: "description", width: 300 },
  { title: "操作", key: "action", width: 90, fixed: "right" as const },
];

const severityOptions = [
  { label: "高风险", value: "HIGH" },
  { label: "中风险", value: "MEDIUM" },
  { label: "低风险", value: "LOW" },
];
const statusOptions = [
  { label: "待处理", value: "OPEN" },
  { label: "处理中", value: "PENDING" },
  { label: "已逾期", value: "OVERDUE" },
];
const workflowOptions = [
  { label: "未认领", value: "UNCLAIMED" },
  { label: "已认领", value: "CLAIMED" },
  { label: "处理中", value: "PROCESSING" },
  { label: "已忽略", value: "IGNORED" },
  { label: "已关闭", value: "CLOSED" },
];

const moduleOptions = computed(() =>
  Array.from(
    new Map(
      items.value.map((item) => [
        item.module,
        { label: item.moduleName, value: item.module },
      ]),
    ).values(),
  ),
);
const ownerOptions = computed(() =>
  Array.from(
    new Set(
      items.value.map((item) => item.workflow.owner || defaultOwnerLabel(item)),
    ),
  ).map((owner) => ({ label: owner, value: owner })),
);
const filteredItems = computed(() => {
  const term = keyword.value.trim().toLowerCase();
  return items.value.filter((item) => {
    const text =
      `${item.title} ${item.subject} ${item.description}`.toLowerCase();
    const owner = item.workflow.owner || defaultOwnerLabel(item);
    return (
      (!moduleFilter.value || item.module === moduleFilter.value) &&
      (!severityFilter.value || item.severity === severityFilter.value) &&
      (!statusFilter.value || item.status === statusFilter.value) &&
      (!workflowFilter.value ||
        item.workflow.status === workflowFilter.value) &&
      (!ownerFilter.value || owner === ownerFilter.value) &&
      (!term || text.includes(term))
    );
  });
});
const riskAmount = computed(() =>
  filteredItems.value.reduce((sum, item) => sum + Number(item.amount || 0), 0),
);
const resolvedCount = computed(
  () =>
    items.value.filter(
      (item) =>
        item.workflow.status === "CLOSED" || item.workflow.status === "IGNORED",
    ).length,
);
const slaOverdueCount = computed(
  () => filteredItems.value.filter((item) => item.slaOverdue).length,
);
const activeItems = computed(() =>
  filteredItems.value.filter(
    (item) =>
      item.workflow.status !== "CLOSED" && item.workflow.status !== "IGNORED",
  ),
);
const closureRate = computed(() =>
  items.value.length
    ? Math.round((resolvedCount.value / items.value.length) * 100)
    : 100,
);
const dueSoonItems = computed(() =>
  activeItems.value
    .filter((item) => item.slaOverdue || hoursToDue(item) <= 24)
    .sort((a, b) => hoursToDue(a) - hoursToDue(b))
    .slice(0, 5),
);
const ownerQueues = computed(() => {
  const bucket = new Map<
    string,
    { owner: string; count: number; modules: Set<string> }
  >();
  activeItems.value.forEach((item) => {
    const owner = item.workflow.owner || defaultOwnerLabel(item);
    const current = bucket.get(owner) || {
      owner,
      count: 0,
      modules: new Set<string>(),
    };
    current.count++;
    current.modules.add(item.moduleName);
    bucket.set(owner, current);
  });
  return Array.from(bucket.values())
    .map((item) => ({
      owner: item.owner,
      count: item.count,
      modules: Array.from(item.modules).slice(0, 3).join(" / "),
    }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 5);
});
const recommendedActions = computed(() =>
  activeItems.value
    .map((item) => ({
      ...item,
      action: recommendAction(item),
      color: item.slaOverdue ? "red" : severityColor(item.severity),
    }))
    .sort(
      (a, b) =>
        Number(b.slaOverdue) - Number(a.slaOverdue) ||
        severityRank(b.severity) - severityRank(a.severity),
    )
    .slice(0, 5),
);
const repeatModule = computed(() => {
  const module = [...(summary.value?.modules || [])].sort(
    (a, b) => b.totalCount - a.totalCount,
  )[0];
  return module ? `${module.moduleName} ${module.totalCount} 项` : "暂无";
});
const reviewCards = computed(() => [
  {
    key: "closure-rate",
    label: "闭环率",
    value: `${closureRate.value}%`,
    hint: `${resolvedCount.value}/${items.value.length || 0} 已收口`,
    action: () => {
      workflowFilter.value = "CLOSED";
    },
  },
  {
    key: "repeat-module",
    label: "复发模块",
    value: repeatModule.value,
    hint: "按快照总量排序",
    action: () => {
      const module = [...(summary.value?.modules || [])].sort(
        (a, b) => b.totalCount - a.totalCount,
      )[0];
      moduleFilter.value = module?.module;
    },
  },
  {
    key: "due-soon",
    label: "24小时内",
    value: `${dueSoonItems.value.length} 项`,
    hint: "临期与超时合计",
    action: () => {
      statusFilter.value = "OVERDUE";
    },
  },
  {
    key: "unowned",
    label: "未明确责任",
    value: `${activeItems.value.filter((item) => !item.workflow.owner).length} 项`,
    hint: "建议先认领分派",
    action: () => {
      workflowFilter.value = "UNCLAIMED";
    },
  },
]);
const taskCards = computed(() => {
  const unclaimed = filteredItems.value.filter(
    (item) => item.workflow.status === "UNCLAIMED",
  ).length;
  const processing = filteredItems.value.filter(
    (item) => item.workflow.status === "PROCESSING",
  ).length;
  return [
    {
      key: "unclaimed",
      label: "待认领",
      value: `${unclaimed} 项`,
      hint: unclaimed ? "需要明确责任人" : "暂无待认领",
      action: () => {
        workflowFilter.value = "UNCLAIMED";
      },
    },
    {
      key: "processing",
      label: "处理中",
      value: `${processing} 项`,
      hint: processing ? "跟进处理进度" : "暂无处理中",
      action: () => {
        workflowFilter.value = "PROCESSING";
      },
    },
    {
      key: "sla",
      label: "SLA超时",
      value: `${slaOverdueCount.value} 项`,
      hint: slaOverdueCount.value ? "建议立即升级" : "SLA正常",
      action: () => {
        statusFilter.value = "OVERDUE";
      },
    },
    {
      key: "amount",
      label: "金额敞口",
      value: formatMoney(riskAmount.value),
      hint: "当前筛选范围",
      action: () => {
        severityFilter.value = "HIGH";
      },
    },
  ];
});

restoreWorkflow();
restoreFilters();
onMounted(loadData);
watch(
  [
    keyword,
    moduleFilter,
    severityFilter,
    statusFilter,
    workflowFilter,
    ownerFilter,
  ],
  persistFilters,
);

async function loadData() {
  loading.value = true;
  loadNotes.value = [];
  const rows: Array<Omit<RiskItem, "workflow">> = [];
  try {
    await loadSummary();
    if (await loadBackendRiskItems()) return;
    await Promise.all([
      loadWorkflowStore(),
      loadOffice(rows),
      loadInventory(rows),
      loadProcurement(rows),
      loadProjects(rows),
      loadFinance(rows),
      loadQualification(rows),
      loadRenewals(rows),
    ]);
    items.value = rows
      .map(applyWorkflow)
      .sort(
        (a, b) =>
          severityRank(b.severity) - severityRank(a.severity) ||
          (b.date || "").localeCompare(a.date || ""),
      );
  } catch (error) {
    message.error(error instanceof Error ? error.message : "风险数据加载失败");
  } finally {
    loading.value = false;
  }
}

async function loadBackendRiskItems() {
  try {
    const rows = await listRiskItems();
    const workflowRows = rows
      .filter((item) => item.workflow)
      .map(
        (item) =>
          [
            item.key,
            toWorkflow(
              item.workflow as NonNullable<RiskItemResponse["workflow"]>,
            ),
          ] as const,
      );
    workflowStore.value = {
      ...workflowStore.value,
      ...Object.fromEntries(workflowRows),
    };
    localStorage.setItem(
      WORKFLOW_STORAGE_KEY,
      JSON.stringify(workflowStore.value),
    );
    items.value = rows
      .map((item) =>
        applyWorkflow({
          key: item.key,
          module: item.module,
          moduleName: item.moduleName,
          title: item.title,
          subject: item.subject,
          description: item.description,
          severity: item.severity,
          status: item.status,
          amount: Number(item.amount || 0),
          date: item.date,
          route: item.route,
          ruleCode: item.ruleCode,
          slaHours: item.slaHours,
          dueAt: item.dueAt,
          slaOverdue: item.slaOverdue,
        }),
      )
      .sort(
        (a, b) =>
          severityRank(b.severity) - severityRank(a.severity) ||
          (b.date || "").localeCompare(a.date || ""),
      );
    return true;
  } catch {
    loadNotes.value.push("后端风险聚合");
    return false;
  }
}

async function loadOffice(rows: Array<Omit<RiskItem, "workflow">>) {
  if (
    !canAny([
      "office:approval:view",
      "office:notification:view",
      "office:expense:view",
      "office:outsource:view",
    ])
  )
    return;
  await safeLoad("OA协同", async () => {
    const workbench = await getOfficeWorkbench();
    workbench.warnings.forEach((item) =>
      rows.push({
        key: `office-warning-${item.type}-${item.id}`,
        module: "office",
        moduleName: "OA协同",
        title: item.title,
        subject: item.content,
        description: item.content,
        severity: normalizeSeverity(item.severity),
        status: item.type.includes("OVERDUE") ? "OVERDUE" : "OPEN",
        date: dateOnly(item.createdAt),
        route: item.route,
        slaOverdue: false,
      }),
    );
    workbench.todos.slice(0, 20).forEach((item) =>
      rows.push({
        key: `office-todo-${item.type}-${item.id}`,
        module: "office",
        moduleName: "OA协同",
        title: item.title,
        subject: item.subtitle,
        description: "待办事项未处理",
        severity: normalizeSeverity(item.priority),
        status: "PENDING",
        amount: Number(item.amount || 0),
        date: dateOnly(item.createdAt),
        route: item.route,
        slaOverdue: false,
      }),
    );
  });
}

async function loadInventory(rows: Array<Omit<RiskItem, "workflow">>) {
  if (!auth.can("inventory:view")) return;
  await safeLoad("库存管理", async () => {
    const suggestions = await listReplenishmentSuggestions();
    suggestions.forEach((item) =>
      rows.push({
        key: `inventory-${item.partId}`,
        module: "inventory",
        moduleName: "库存管理",
        title: "补货建议",
        subject: `${item.partName} · ${item.partCode || "-"}`,
        description: item.reason,
        severity: normalizeSeverity(item.priority),
        status: "OPEN",
        amount: Number(item.suggestedQty || 0),
        route: "/inventory/analytics",
        slaOverdue: false,
      }),
    );
  });
}

async function loadProcurement(rows: Array<Omit<RiskItem, "workflow">>) {
  if (!auth.can("procurement:view")) return;
  await safeLoad("供应链采购", async () => {
    const matching = await listProcurementMatching();
    matching
      .filter((item) => item.matchStatus !== "MATCHED")
      .forEach((item) =>
        rows.push({
          key: `procurement-${item.orderId}`,
          module: "procurement",
          moduleName: "供应链采购",
          title: matchStatusLabel(item.matchStatus),
          subject: `${item.orderCode || "-"} · ${item.supplierName || "未知供应商"}`,
          description: item.riskMessage,
          severity: item.matchStatus === "AMOUNT_MISMATCH" ? "HIGH" : "MEDIUM",
          status: item.matchStatus === "RECEIVING" ? "PENDING" : "OPEN",
          amount: Number(
            item.payableAmount || item.receiptAmount || item.orderAmount || 0,
          ),
          route: "/procurement/p2p",
          slaOverdue: false,
        }),
      );
  });
}

async function loadProjects(rows: Array<Omit<RiskItem, "workflow">>) {
  if (!auth.can("project:view")) return;
  await safeLoad("项目管理", async () => {
    const profitability = await listProjectProfitability();
    profitability
      .filter((item) => item.riskLevel !== "LOW")
      .forEach((item) =>
        rows.push({
          key: `project-${item.projectId}`,
          module: "project",
          moduleName: "项目管理",
          title: item.riskMessage,
          subject: `${item.projectName} · ${item.customerName || "未关联客户"}`,
          description: `毛利率 ${formatPercent(item.grossMarginRate)}，预算使用 ${formatPercent(item.budgetUsageRate)}`,
          severity: normalizeSeverity(item.riskLevel),
          status: "OPEN",
          amount: Number(item.grossMargin || 0),
          route: "/projects/list",
          slaOverdue: false,
        }),
      );
  });
}

async function loadFinance(rows: Array<Omit<RiskItem, "workflow">>) {
  await safeLoad("财务资金", async () => {
    if (auth.can("finance:receivable:view")) {
      const receivables = await listFinanceReceivables();
      receivables
        .filter(
          (item) =>
            item.status === "OVERDUE" ||
            (Number(item.outstandingAmount || 0) > 0 && isPast(item.dueDate)),
        )
        .forEach((item) =>
          rows.push({
            key: `finance-receivable-${item.id}`,
            module: "finance",
            moduleName: "财务资金",
            title: "应收逾期",
            subject: `${item.customerName} · ${item.contractCode || item.sourceNo}`,
            description: `到期日 ${item.dueDate}`,
            severity: "HIGH",
            status: "OVERDUE",
            amount: Number(item.outstandingAmount || 0),
            date: item.dueDate,
            route: "/finance/receivables",
            slaOverdue: false,
          }),
        );
    }
    if (auth.can("finance:payable:view")) {
      const payables = await listFinancePayables();
      payables
        .filter((item) => item.overdue)
        .forEach((item) =>
          rows.push({
            key: `finance-payable-${item.id}`,
            module: "finance",
            moduleName: "财务资金",
            title: "应付逾期",
            subject: `${item.supplierName} · ${item.orderCode}`,
            description: `到期日 ${item.dueDate}`,
            severity: "MEDIUM",
            status: "OVERDUE",
            amount: Number(item.outstandingAmount || 0),
            date: item.dueDate,
            route: "/finance/payables",
            slaOverdue: false,
          }),
        );
    }
  });
}

async function loadQualification(rows: Array<Omit<RiskItem, "workflow">>) {
  if (!auth.can("qualification:warning:view")) return;
  await safeLoad("资质管理", async () => {
    const warnings = await listQualificationWarnings();
    warnings.forEach((item) =>
      rows.push({
        key: `qualification-${item.sourceType}-${item.sourceId}-${item.warningType}`,
        module: "qualification",
        moduleName: "资质管理",
        title: item.title,
        subject: item.sourceName,
        description:
          item.status === "OVERDUE"
            ? `已逾期 ${Math.abs(item.daysLeft)} 天`
            : `剩余 ${item.daysLeft} 天`,
        severity:
          item.level === "DANGER"
            ? "HIGH"
            : item.level === "WARNING"
              ? "MEDIUM"
              : "LOW",
        status: item.status === "OVERDUE" ? "OVERDUE" : "OPEN",
        date: item.dueDate,
        route: "/qualification/warnings",
        slaOverdue: false,
      }),
    );
  });
}

async function loadRenewals(rows: Array<Omit<RiskItem, "workflow">>) {
  if (!auth.can("crm:renewal:view")) return;
  await safeLoad("CRM", async () => {
    const renewals = await listRenewals();
    renewals
      .filter((item) => item.renewalRisk !== "LOW")
      .forEach((item) =>
        rows.push({
          key: `crm-renewal-${item.contractId}`,
          module: "crm",
          moduleName: "CRM",
          title: item.renewalRisk === "EXPIRED" ? "合同已到期" : "合同续约风险",
          subject: `${item.customerName} · ${item.contractCode}`,
          description:
            item.daysRemaining < 0
              ? `已到期 ${Math.abs(item.daysRemaining)} 天`
              : `剩余 ${item.daysRemaining} 天`,
          severity:
            item.renewalRisk === "EXPIRED" || item.renewalRisk === "HIGH"
              ? "HIGH"
              : "MEDIUM",
          status: item.daysRemaining < 0 ? "OVERDUE" : "OPEN",
          amount: Number(item.outstandingAmount || item.amount || 0),
          date: item.endDate,
          route: "/crm/renewals",
          slaOverdue: false,
        }),
      );
  });
}

async function safeLoad(source: string, loader: () => Promise<void>) {
  try {
    await loader();
  } catch {
    loadNotes.value.push(source);
  }
}

function canAny(permissions: string[]) {
  return permissions.some((permission) => auth.can(permission));
}
function goRoute(route: string) {
  if (route) router.push(route);
}
function onSelectionChange(keys: string[]) {
  selectedRowKeys.value = keys;
}
function showHighRisks() {
  severityFilter.value = "HIGH";
  statusFilter.value = undefined;
}
function showOverdueRisks() {
  statusFilter.value = "OVERDUE";
  severityFilter.value = undefined;
}
function exportRisks() {
  const headers = [
    "模块",
    "风险事项",
    "对象",
    "等级",
    "业务状态",
    "处理状态",
    "金额",
    "日期",
    "说明",
  ];
  const rows = filteredItems.value.map((item) => [
    item.moduleName,
    item.title,
    item.subject,
    severityLabel(item.severity),
    statusLabel(item.status),
    workflowLabel(item.workflow.status),
    item.amount ? String(item.amount) : "",
    item.date || "",
    item.description,
  ]);
  downloadCsv(
    `risk-center-${new Date().toISOString().slice(0, 10)}.csv`,
    headers,
    rows,
  );
}

async function loadSummary() {
  try {
    summary.value = await getRiskSummary(14);
  } catch {
    summary.value = undefined;
  }
}

async function handleBatchWorkflow() {
  if (!selectedRowKeys.value.length) return;
  try {
    const owner = auth.user?.displayName || "";
    const rows = await batchUpdateRiskWorkflow({
      riskKeys: selectedRowKeys.value,
      status: batchStatus.value,
      owner,
      note: "批量处理",
    });
    workflowStore.value = {
      ...workflowStore.value,
      ...Object.fromEntries(
        rows.map((item) => [item.riskKey, toWorkflow(item)]),
      ),
    };
    localStorage.setItem(
      WORKFLOW_STORAGE_KEY,
      JSON.stringify(workflowStore.value),
    );
    selectedRowKeys.value = [];
    message.success(`已批量更新 ${rows.length} 项风险`);
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "批量处理失败");
  }
}

async function handleSnapshot() {
  try {
    const count = await snapshotRiskToday();
    message.success(
      count ? `已归档 ${count} 条今日风险快照` : "今日风险快照已是最新",
    );
    await loadSummary();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "快照归档失败");
  }
}

async function handleEscalationScan() {
  try {
    const count = await scanRiskEscalations();
    message.success(
      count ? `已生成 ${count} 条升级通知` : "暂无需要升级的超时风险",
    );
  } catch (error) {
    message.error(error instanceof Error ? error.message : "升级扫描失败");
  }
}
function severityCount(severity: RiskSeverity) {
  return filteredItems.value.filter((item) => item.severity === severity)
    .length;
}
function severityRank(severity: string) {
  return (
    ({ HIGH: 3, MEDIUM: 2, LOW: 1 } as Record<string, number>)[severity] || 0
  );
}
function normalizeSeverity(value: string): RiskSeverity {
  return value === "HIGH" ? "HIGH" : value === "MEDIUM" ? "MEDIUM" : "LOW";
}
function severityLabel(value: RiskSeverity) {
  return (
    { HIGH: "高风险", MEDIUM: "中风险", LOW: "低风险" } as Record<
      RiskSeverity,
      string
    >
  )[value];
}
function severityColor(value: RiskSeverity) {
  return (
    { HIGH: "red", MEDIUM: "orange", LOW: "green" } as Record<
      RiskSeverity,
      string
    >
  )[value];
}
function statusLabel(value: RiskStatus) {
  return (
    { OPEN: "待处理", PENDING: "处理中", OVERDUE: "已逾期" } as Record<
      RiskStatus,
      string
    >
  )[value];
}
function statusColor(value: RiskStatus) {
  return (
    { OPEN: "blue", PENDING: "orange", OVERDUE: "red" } as Record<
      RiskStatus,
      string
    >
  )[value];
}
function workflowLabel(value: WorkflowStatus) {
  return (
    {
      UNCLAIMED: "未认领",
      CLAIMED: "已认领",
      PROCESSING: "处理中",
      IGNORED: "已忽略",
      CLOSED: "已关闭",
    } as Record<WorkflowStatus, string>
  )[value];
}
function workflowColor(value: WorkflowStatus) {
  return (
    {
      UNCLAIMED: "default",
      CLAIMED: "blue",
      PROCESSING: "orange",
      IGNORED: "purple",
      CLOSED: "green",
    } as Record<WorkflowStatus, string>
  )[value];
}
function matchStatusLabel(value: string) {
  return (
    (
      {
        RECEIVING: "待入库",
        PAYABLE_MISSING: "缺应付",
        AMOUNT_MISMATCH: "金额不一致",
        CANCELLED: "已取消",
      } as Record<string, string>
    )[value] || value
  );
}
function isPast(value?: string) {
  return Boolean(value) && new Date(value as string).getTime() < Date.now();
}
function dateOnly(value?: string) {
  return value ? value.slice(0, 10) : undefined;
}
function formatPercent(value: number) {
  return `${Number(value || 0).toFixed(1)}%`;
}
function formatMoney(value: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value || 0);
}
function moneyFormatter(value: number | string) {
  return formatMoney(Number(value));
}
function hoursToDue(item: RiskItem) {
  if (!item.dueAt) return Number.POSITIVE_INFINITY;
  return Math.ceil((new Date(item.dueAt).getTime() - Date.now()) / 3600000);
}
function slaText(item: RiskItem) {
  const hours = hoursToDue(item);
  if (!Number.isFinite(hours)) return "-";
  if (hours < 0) return `超时 ${Math.abs(hours)}h`;
  if (hours === 0) return "1h内";
  return `${hours}h`;
}
function defaultOwnerLabel(item: RiskItem) {
  return (
    (
      {
        finance: "财务经理",
        procurement: "采购负责人",
        inventory: "仓库管理员",
        project: "项目经理",
        office: "综合管理员",
        qualification: "资质管理员",
        crm: "客户经理",
        maintenance: "维保主管",
      } as Record<string, string>
    )[item.module] || "待分派"
  );
}
function recommendAction(item: RiskItem) {
  if (item.slaOverdue) return "立即升级并补充处理备注";
  if (item.workflow.status === "UNCLAIMED") return "先认领并明确责任人";
  if (item.module === "finance" && item.title.includes("应收"))
    return "登记催收承诺或回款计划";
  if (item.module === "procurement") return "复核订单、入库、应付三单差异";
  if (item.module === "project") return "复盘预算、采购成本和未结算变更";
  if (item.module === "inventory") return "确认采购申请或跨仓调拨";
  if (item.module === "qualification") return "补齐证照续期材料和责任节点";
  if (item.module === "crm") return "安排客户续约跟进和报价动作";
  return "补充处理计划并推进到处理中";
}
function filterOwner(owner: string) {
  ownerFilter.value = owner;
  workflowFilter.value = undefined;
}
async function claimTopRisk() {
  const target =
    activeItems.value.find((item) => item.workflow.status === "UNCLAIMED") ||
    activeItems.value[0];
  if (!target) {
    message.info("暂无需要认领的风险");
    return;
  }
  activeRisk.value = target;
  Object.assign(workflowForm, {
    status: "CLAIMED" as WorkflowStatus,
    owner:
      target.workflow.owner ||
      auth.user?.displayName ||
      defaultOwnerLabel(target),
    note: "已认领，准备推进闭环处理",
    reason: "",
    rootCause: "",
    responsibleDepartment: "",
    recurrence: false,
    preventionAction: "",
  });
  await saveWorkflow();
}
function openWorkflowWithSuggestion(record: RiskItem & { action: string }) {
  openWorkflow(record);
  workflowForm.status =
    record.workflow.status === "UNCLAIMED" ? "CLAIMED" : "PROCESSING";
  workflowForm.note = record.action;
  workflowForm.owner =
    record.workflow.owner ||
    auth.user?.displayName ||
    defaultOwnerLabel(record);
}

function applyWorkflow(item: Omit<RiskItem, "workflow"> | RiskItem): RiskItem {
  return {
    ...item,
    workflow: workflowStore.value[item.key] || { status: "UNCLAIMED" },
  };
}

function openWorkflow(record: RiskItem) {
  activeRisk.value = record;
  Object.assign(workflowForm, {
    status: record.workflow.status,
    owner: record.workflow.owner || auth.user?.displayName || "",
    note: record.workflow.note || "",
    reason: record.workflow.reason || "",
    rootCause: record.workflow.rootCause || "",
    responsibleDepartment: record.workflow.responsibleDepartment || "",
    recurrence: Boolean(record.workflow.recurrence),
    preventionAction: record.workflow.preventionAction || "",
  });
  workflowOpen.value = true;
  loadWorkflowActions(record.key);
}

async function saveWorkflow() {
  if (!activeRisk.value) return;
  if (
    workflowForm.status === "CLOSED" &&
    (!workflowForm.rootCause.trim() ||
      !workflowForm.responsibleDepartment.trim() ||
      !workflowForm.preventionAction.trim())
  ) {
    message.error("关闭风险前请补齐根因、责任部门和下次预防动作");
    return;
  }
  let next: RiskWorkflow;
  try {
    const saved = await updateRiskWorkflow({
      riskKey: activeRisk.value.key,
      status: workflowForm.status,
      owner: workflowForm.owner || auth.user?.displayName || "",
      note: workflowForm.note,
      reason: workflowForm.reason,
      rootCause:
        workflowForm.status === "CLOSED" ? workflowForm.rootCause : undefined,
      responsibleDepartment:
        workflowForm.status === "CLOSED"
          ? workflowForm.responsibleDepartment
          : undefined,
      recurrence:
        workflowForm.status === "CLOSED" ? workflowForm.recurrence : undefined,
      preventionAction:
        workflowForm.status === "CLOSED"
          ? workflowForm.preventionAction
          : undefined,
    });
    next = {
      status: saved.status,
      owner: saved.owner,
      note: saved.note,
      reason: saved.reason,
      updatedAt: saved.updatedAt || saved.processedAt,
      rootCause: saved.rootCause,
      responsibleDepartment: saved.responsibleDepartment,
      handlingHours: saved.handlingHours,
      recurrence: saved.recurrence,
      preventionAction: saved.preventionAction,
    };
  } catch {
    next = {
      status: workflowForm.status,
      owner: workflowForm.owner || auth.user?.displayName || "",
      note: workflowForm.note,
      reason: workflowForm.reason,
      updatedAt: new Date().toISOString(),
      rootCause: workflowForm.rootCause,
      responsibleDepartment: workflowForm.responsibleDepartment,
      recurrence: workflowForm.recurrence,
      preventionAction: workflowForm.preventionAction,
    };
    message.warning("后端风险状态保存失败，已暂存到当前浏览器");
  }
  workflowStore.value = {
    ...workflowStore.value,
    [activeRisk.value.key]: next,
  };
  localStorage.setItem(
    WORKFLOW_STORAGE_KEY,
    JSON.stringify(workflowStore.value),
  );
  items.value = items.value.map((item) =>
    item.key === activeRisk.value?.key ? { ...item, workflow: next } : item,
  );
  await loadWorkflowActions(activeRisk.value.key);
  workflowOpen.value = false;
  message.success("风险处理状态已更新");
}

async function loadWorkflowActions(riskKey: string) {
  actionLoading.value = true;
  try {
    workflowActions.value = await listRiskWorkflowActions(riskKey);
  } catch {
    workflowActions.value = [];
  } finally {
    actionLoading.value = false;
  }
}

async function loadWorkflowStore() {
  try {
    const rows = await listRiskWorkflows();
    workflowStore.value = {
      ...workflowStore.value,
      ...Object.fromEntries(
        rows.map((item) => [item.riskKey, toWorkflow(item)]),
      ),
    };
  } catch {
    loadNotes.value.push("风险处理状态");
  }
}

function toWorkflow(
  item: NonNullable<RiskItemResponse["workflow"]>,
): RiskWorkflow {
  return {
    status: item.status,
    owner: item.owner,
    note: item.note,
    reason: item.reason,
    updatedAt: item.updatedAt || item.processedAt,
    rootCause: item.rootCause,
    responsibleDepartment: item.responsibleDepartment,
    handlingHours: item.handlingHours,
    recurrence: item.recurrence,
    preventionAction: item.preventionAction,
  };
}

function restoreWorkflow() {
  try {
    workflowStore.value = JSON.parse(
      localStorage.getItem(WORKFLOW_STORAGE_KEY) || "{}",
    );
  } catch {
    workflowStore.value = {};
  }
}

function restoreFilters() {
  try {
    const filters = JSON.parse(
      localStorage.getItem(FILTER_STORAGE_KEY) || "{}",
    );
    keyword.value = filters.keyword || "";
    moduleFilter.value = filters.moduleFilter;
    severityFilter.value = filters.severityFilter;
    statusFilter.value = filters.statusFilter;
    workflowFilter.value = filters.workflowFilter;
    ownerFilter.value = filters.ownerFilter;
  } catch {
    // Ignore invalid local preference payloads.
  }
}

function persistFilters() {
  localStorage.setItem(
    FILTER_STORAGE_KEY,
    JSON.stringify({
      keyword: keyword.value,
      moduleFilter: moduleFilter.value,
      severityFilter: severityFilter.value,
      statusFilter: statusFilter.value,
      workflowFilter: workflowFilter.value,
      ownerFilter: ownerFilter.value,
    }),
  );
}
</script>

<style scoped>
.closure-command-panel {
  margin: 16px 0;
  padding: 14px;
  border: 1px solid #e5e7eb;
  background: #fbfcfe;
}

.closure-command-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.closure-command-head h3,
.block-title span {
  margin: 0;
  color: #111827;
  font-size: 15px;
  font-weight: 600;
}

.closure-command-head p {
  margin: 4px 0 0;
  color: #6b7280;
  font-size: 12px;
}

.closure-command-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.closure-command-block {
  min-width: 0;
  padding: 12px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fff;
}

.block-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.block-title strong {
  color: #1677ff;
  font-size: 13px;
}

.owner-row,
.due-row,
.action-suggestion-row {
  display: grid;
  width: 100%;
  align-items: center;
  gap: 10px;
  padding: 9px 0;
  border: 0;
  border-bottom: 1px solid #f2f4f7;
  background: transparent;
  cursor: pointer;
  text-align: left;
}

.owner-row,
.due-row {
  grid-template-columns: minmax(0, 1fr) auto;
}

.action-suggestion-row {
  grid-template-columns: auto minmax(0, 1fr);
}

.owner-row:last-child,
.due-row:last-child,
.action-suggestion-row:last-child {
  border-bottom: 0;
}

.owner-row:hover,
.due-row:hover,
.action-suggestion-row:hover {
  background: #f6faff;
}

.owner-row span,
.due-row span,
.action-suggestion-row span {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.owner-row strong,
.due-row strong,
.action-suggestion-row strong {
  overflow: hidden;
  color: #1f2937;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.owner-row small,
.due-row small,
.action-suggestion-row small {
  overflow: hidden;
  color: #8c8c8c;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.owner-row em,
.due-row em {
  color: #595959;
  font-size: 12px;
  font-style: normal;
  font-weight: 600;
  white-space: nowrap;
}

.due-row em.danger {
  color: #cf1322;
}

.closure-review-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.review-card {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 12px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  text-align: left;
}

.review-card:hover {
  border-color: #91caff;
  background: #f6faff;
}

.review-card span {
  color: #667085;
  font-size: 12px;
}

.review-card strong {
  overflow: hidden;
  color: #101828;
  font-size: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.review-card em {
  color: #98a2b3;
  font-size: 12px;
  font-style: normal;
}

.task-closure-panel {
  margin: 16px 0;
  padding: 14px;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.task-closure-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.task-closure-title h3 {
  margin: 0;
  color: #111827;
  font-size: 15px;
  font-weight: 600;
}

.task-closure-title p {
  margin: 4px 0 0;
  color: #6b7280;
  font-size: 12px;
}

.task-closure-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.task-closure-card {
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: flex-start;
  gap: 5px;
  padding: 12px;
  border: 1px solid #eef2f7;
  border-radius: 6px;
  background: #f8fafc;
  cursor: pointer;
  text-align: left;
}

.task-closure-card:hover {
  border-color: #91caff;
  background: #f0f7ff;
}

.task-closure-card span {
  color: #667085;
  font-size: 12px;
}

.task-closure-card strong {
  max-width: 100%;
  color: #101828;
  font-size: 20px;
  overflow-wrap: anywhere;
}

.task-closure-card em {
  color: #98a2b3;
  font-size: 12px;
  font-style: normal;
}

.risk-summary-panel {
  min-height: 76px;
  padding: 12px 14px;
  border: 1px solid #e5e7eb;
  background: #fafafa;
}
.risk-summary-panel h3 {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 600;
}

.action-review-card {
  margin-top: 8px;
  padding: 8px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fbfcfe;
}

.action-review-card p {
  margin: 2px 0;
  color: #344054;
  font-size: 12px;
}

@media (max-width: 900px) {
  .closure-command-head {
    flex-direction: column;
  }

  .closure-command-grid,
  .closure-review-strip {
    grid-template-columns: 1fr;
  }

  .task-closure-title {
    align-items: flex-start;
    flex-direction: column;
  }

  .task-closure-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
