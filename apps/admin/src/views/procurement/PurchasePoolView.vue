<template>
  <div class="page-stack">
    <a-card>
      <template #title>待采购清单</template>
      <template #extra>
        <a-space>
          <a-button @click="router.push('/procurement/requests')"
            >查看采购申请</a-button
          >
          <a-button :loading="loading" @click="loadData">
            <template #icon><ReloadOutlined /></template>刷新
          </a-button>
        </a-space>
      </template>

      <a-row :gutter="[16, 16]" class="summary-row">
        <a-col :xs="12" :lg="6">
          <a-statistic title="待采购申请" :value="pool.totalRequests" suffix="条" />
        </a-col>
        <a-col :xs="12" :lg="6">
          <a-statistic title="可集采物料" :value="pool.totalGroups" suffix="类" />
        </a-col>
        <a-col :xs="12" :lg="6">
          <a-statistic
            title="待采购数量"
            :value="pool.totalRemainingQuantity"
            :precision="2"
          />
        </a-col>
        <a-col :xs="12" :lg="6">
          <a-statistic
            title="预计采购金额"
            :value="pool.totalEstimatedAmount"
            :precision="2"
            prefix="¥"
          />
        </a-col>
      </a-row>

      <a-alert
        class="section-alert"
        type="info"
        show-icon
        message="这里仅显示审批通过、尚有未采购数量且未进入询价的申请。系统按物料编码归类，可跨项目、跨申请集中询价，订单和成本仍能追溯到原始申请。"
      />

      <a-space wrap class="table-toolbar">
        <a-input-search
          v-model:value="keyword"
          allow-clear
          placeholder="搜索物料、申请号、批次或成本归属"
          style="width: 360px"
        />
        <a-select
          v-model:value="urgencyFilter"
          style="width: 150px"
          :options="[
            { label: '全部交期', value: 'ALL' },
            { label: '7天内到货', value: 'URGENT' },
            { label: '已逾期', value: 'OVERDUE' },
          ]"
        />
      </a-space>

      <a-table
        :columns="groupColumns"
        :data-source="filteredGroups"
        :loading="loading"
        row-key="groupKey"
        :pagination="{ pageSize: 12 }"
        :scroll="{ x: 1180 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">
            <strong>{{ record.partName }}</strong>
            <span class="table-subtitle">{{
              record.partCode || "未建物料编码，按名称归类"
            }}</span>
          </template>
          <template v-else-if="column.key === 'requests'">
            <strong>{{ record.requestCount }} 条申请</strong>
            <span class="table-subtitle"
              >涉及 {{ record.costTargetCount }} 个成本归属</span
            >
          </template>
          <template v-else-if="column.key === 'quantity'">
            <strong>{{ formatQuantity(record.totalRemainingQuantity) }}</strong>
            <span class="table-subtitle">待采购数量</span>
          </template>
          <template v-else-if="column.key === 'amount'">
            <strong>{{ formatMoney(record.totalEstimatedAmount) }}</strong>
            <span class="table-subtitle">按申请预计价</span>
          </template>
          <template v-else-if="column.key === 'date'">
            {{ record.earliestExpectedDate || "-" }}
            <a-tag
              v-if="urgencyStatus(record.earliestExpectedDate)"
              :color="
                urgencyStatus(record.earliestExpectedDate) === 'OVERDUE'
                  ? 'red'
                  : 'orange'
              "
            >
              {{
                urgencyStatus(record.earliestExpectedDate) === "OVERDUE"
                  ? "已逾期"
                  : "临近交期"
              }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button
              v-if="auth.can('procurement:purchase:create')"
              type="primary"
              ghost
              size="small"
              @click="openConsolidate(record)"
            >
              <template #icon><MergeCellsOutlined /></template>集中询价
            </a-button>
          </template>
        </template>

        <template #expandedRowRender="{ record }">
          <div class="expanded-lines">
            <a-table
              size="small"
              :columns="itemColumns"
              :data-source="record.items"
              :pagination="false"
              row-key="requestId"
              :scroll="{ x: 1180 }"
            >
              <template #bodyCell="{ column, record: item }">
                <template v-if="column.key === 'request'">
                  <strong>{{ item.requestCode }}</strong>
                  <span class="table-subtitle"
                    >{{ item.batchCode }} · 第{{ item.lineNo }}行</span
                  >
                </template>
                <template v-else-if="column.key === 'quantity'">
                  <strong>{{ formatQuantity(item.remainingQuantity) }}</strong>
                  <span class="table-subtitle"
                    >申请 {{ formatQuantity(item.requestedQuantity) }} / 已占用
                    {{ formatQuantity(item.orderedQuantity) }}</span
                  >
                </template>
                <template v-else-if="column.key === 'price'">
                  {{ formatMoney(item.estimatedUnitPrice) }}
                </template>
                <template v-else-if="column.key === 'amount'">
                  {{ formatMoney(item.estimatedAmount) }}
                </template>
                <template v-else-if="column.key === 'target'">
                  {{ item.costTargetName }}
                  <span class="table-subtitle">{{
                    item.costType === "PROJECT" ? "项目成本" : "部门费用"
                  }}</span>
                </template>
                <template v-else-if="column.key === 'reason'">
                  <a-tooltip :title="item.reason">
                    <span class="ellipsis-text">{{ item.reason || "-" }}</span>
                  </a-tooltip>
                </template>
              </template>
            </a-table>
          </div>
        </template>

        <template #emptyText>
          <a-empty description="暂无待采购申请">
            <a-button type="primary" @click="router.push('/procurement/requests')"
              >查看采购申请</a-button
            >
          </a-empty>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="consolidateOpen"
      title="组成集中采购询价"
      width="900px"
      :confirm-loading="saving"
      ok-text="创建集中询价"
      @ok="handleCreateInquiry"
    >
      <a-alert
        v-if="selectedGroup"
        class="section-alert"
        type="success"
        show-icon
        :message="`${selectedGroup.partName} · 已选择 ${selectedItems.length}/${selectedGroup.requestCount} 条申请 · 合计 ${formatQuantity(selectedQuantity)} · 预计 ${formatMoney(selectedAmount)}`"
      />
      <a-table
        v-if="selectedGroup"
        size="small"
        :columns="selectionColumns"
        :data-source="selectedGroup.items"
        :row-selection="inquiryRowSelection"
        :pagination="{ pageSize: 6 }"
        row-key="requestId"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'request'">
            <strong>{{ record.requestCode }}</strong>
            <span class="table-subtitle">{{ record.batchName }}</span>
          </template>
          <template v-else-if="column.key === 'quantity'">
            {{ formatQuantity(record.remainingQuantity) }}
          </template>
          <template v-else-if="column.key === 'amount'">
            {{ formatMoney(record.estimatedAmount) }}
          </template>
        </template>
      </a-table>

      <a-divider />
      <a-form
        ref="inquiryFormRef"
        :model="inquiryForm"
        :rules="inquiryRules"
        layout="vertical"
      >
        <a-form-item label="询价主题" name="title">
          <a-input v-model:value="inquiryForm.title" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="寻源方式">
              <a-select
                v-model:value="inquiryForm.sourcingMethod"
                :options="[
                  { label: '竞争性询价', value: 'COMPETITIVE' },
                  { label: '单一来源', value: 'SINGLE_SOURCE' },
                  { label: '框架协议', value: 'FRAMEWORK' },
                ]"
                @change="
                  inquiryForm.minQuoteCount =
                    inquiryForm.sourcingMethod === 'SINGLE_SOURCE' ? 1 : 3
                "
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="最低有效报价数">
              <a-input-number
                v-model:value="inquiryForm.minQuoteCount"
                :min="1"
                class="full-input"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="报价截止日期">
              <a-input v-model:value="inquiryForm.deadline" type="date" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item
          v-if="inquiryForm.sourcingMethod === 'SINGLE_SOURCE'"
          label="单一来源原因"
          name="exceptionReason"
        >
          <a-textarea v-model:value="inquiryForm.exceptionReason" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { message } from "ant-design-vue";
import MergeCellsOutlined from "@ant-design/icons-vue/MergeCellsOutlined";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import {
  createConsolidatedProcurementInquiry,
  listProcurementPurchasePool,
  type ProcurementPurchasePool,
  type ProcurementPurchasePoolGroup,
  type ProcurementPurchasePoolItem,
} from "@/api/procurement";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
const router = useRouter();
const loading = ref(false);
const saving = ref(false);
const keyword = ref("");
const urgencyFilter = ref("ALL");
const consolidateOpen = ref(false);
const selectedGroup = ref<ProcurementPurchasePoolGroup | null>(null);
const selectedRequestIds = ref<string[]>([]);
const inquiryFormRef = ref();
const pool = reactive<ProcurementPurchasePool>({
  totalGroups: 0,
  totalRequests: 0,
  totalRemainingQuantity: 0,
  totalEstimatedAmount: 0,
  groups: [],
});
const inquiryForm = reactive({
  title: "",
  deadline: "",
  sourcingMethod: "COMPETITIVE",
  minQuoteCount: 3,
  exceptionReason: "",
});

const groupColumns = [
  { title: "物料分类", key: "material", width: 260 },
  { title: "申请来源", key: "requests", width: 150 },
  { title: "汇总数量", key: "quantity", width: 130 },
  { title: "预计金额", key: "amount", width: 150 },
  { title: "最早需求日", key: "date", width: 170 },
  { title: "操作", key: "action", width: 130, fixed: "right" as const },
];
const itemColumns = [
  { title: "采购申请", key: "request", width: 190 },
  { title: "待采购数量", key: "quantity", width: 190 },
  { title: "预计单价", key: "price", width: 120 },
  { title: "预计金额", key: "amount", width: 130 },
  { title: "成本归属", key: "target", width: 220 },
  { title: "需求日期", dataIndex: "expectedDate", width: 120 },
  { title: "采购说明", key: "reason", width: 220 },
];
const selectionColumns = [
  { title: "申请/批次", key: "request" },
  { title: "成本归属", dataIndex: "costTargetName", width: 210 },
  { title: "到货日期", dataIndex: "expectedDate", width: 120 },
  { title: "数量", key: "quantity", width: 90 },
  { title: "预计金额", key: "amount", width: 130 },
];
const inquiryRules = computed(() => ({
  title: [{ required: true, message: "请输入询价主题" }],
  exceptionReason:
    inquiryForm.sourcingMethod === "SINGLE_SOURCE"
      ? [{ required: true, message: "请填写单一来源原因" }]
      : [],
}));

const filteredGroups = computed(() => {
  const key = keyword.value.trim().toLowerCase();
  return pool.groups.filter((group) => {
    const matchesKeyword =
      !key ||
      group.partName.toLowerCase().includes(key) ||
      (group.partCode || "").toLowerCase().includes(key) ||
      group.items.some((item) =>
        [
          item.requestCode,
          item.batchCode,
          item.batchName,
          item.costTargetName,
        ]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(key)),
      );
    const urgency = urgencyStatus(group.earliestExpectedDate);
    const matchesUrgency =
      urgencyFilter.value === "ALL" || urgencyFilter.value === urgency;
    return matchesKeyword && matchesUrgency;
  });
});
const selectedItems = computed<ProcurementPurchasePoolItem[]>(() =>
  (selectedGroup.value?.items || []).filter((item) =>
    selectedRequestIds.value.includes(item.requestId),
  ),
);
const selectedQuantity = computed(() =>
  selectedItems.value.reduce(
    (sum, item) => sum + Number(item.remainingQuantity || 0),
    0,
  ),
);
const selectedAmount = computed(() =>
  selectedItems.value.reduce(
    (sum, item) => sum + Number(item.estimatedAmount || 0),
    0,
  ),
);
const inquiryRowSelection = computed(() => ({
  selectedRowKeys: selectedRequestIds.value,
  onChange: (keys: (string | number)[]) => {
    selectedRequestIds.value = keys.map(String);
  },
}));

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    Object.assign(pool, await listProcurementPurchasePool());
  } catch (error: any) {
    message.error(error.message || "待采购清单加载失败");
  } finally {
    loading.value = false;
  }
}

function openConsolidate(group: ProcurementPurchasePoolGroup) {
  selectedGroup.value = group;
  selectedRequestIds.value = group.items.map((item) => item.requestId);
  Object.assign(inquiryForm, {
    title: `${group.partName}集中采购询价（${group.requestCount}条申请）`,
    deadline: defaultDeadline(group.earliestExpectedDate),
    sourcingMethod: "COMPETITIVE",
    minQuoteCount: 3,
    exceptionReason: "",
  });
  consolidateOpen.value = true;
}

async function handleCreateInquiry() {
  await inquiryFormRef.value?.validate();
  if (!selectedRequestIds.value.length) {
    message.warning("请至少选择一条采购申请");
    return;
  }
  saving.value = true;
  try {
    const inquiry = await createConsolidatedProcurementInquiry({
      requestIds: selectedRequestIds.value,
      title: inquiryForm.title,
      deadline: inquiryForm.deadline || undefined,
      sourcingMethod: inquiryForm.sourcingMethod,
      minQuoteCount: inquiryForm.minQuoteCount,
      exceptionReason: inquiryForm.exceptionReason || undefined,
    });
    consolidateOpen.value = false;
    message.success(
      `${inquiry.code} 已创建，${inquiry.requestCount || selectedRequestIds.value.length}条申请已进入集中询价`,
    );
    await loadData();
  } catch (error: any) {
    message.error(error.message || "集中询价创建失败");
  } finally {
    saving.value = false;
  }
}

function urgencyStatus(date?: string) {
  if (!date) return "";
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const target = new Date(`${date}T00:00:00`);
  const days = Math.ceil((target.getTime() - today.getTime()) / 86_400_000);
  if (days < 0) return "OVERDUE";
  if (days <= 7) return "URGENT";
  return "";
}

function defaultDeadline(expected?: string) {
  const result = new Date();
  const expectedDate = expected ? new Date(`${expected}T00:00:00`) : null;
  result.setDate(result.getDate() + 7);
  if (expectedDate && expectedDate < result) {
    result.setTime(expectedDate.getTime());
    result.setDate(result.getDate() - 1);
  }
  return result.toISOString().slice(0, 10);
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
</script>

<style scoped>
.summary-row {
  margin-bottom: 18px;
}

.summary-row :deep(.ant-col) {
  padding-top: 6px;
  padding-bottom: 6px;
}

.expanded-lines {
  padding: 8px 18px 16px 44px;
  background: #fafcff;
}

.ellipsis-text {
  display: inline-block;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}
</style>
