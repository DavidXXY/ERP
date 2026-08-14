<template>
  <div class="page-stack">
    <a-card title="库存价值分析" style="margin-bottom: 16px">
      <template #extra
        ><a-button @click="router.push('/inventory')">返回库存管理</a-button
        ><a-button
          type="primary"
          :disabled="replenishmentSuggestions.length === 0"
          @click="openConvert"
          >生成采购申请</a-button
        ><a-button :loading="loading" @click="loadData"
          ><template #icon><ReloadOutlined /></template>刷新</a-button
        ></template
      >
      <a-spin :spinning="loading">
        <a-row :gutter="[16, 16]" class="metric-row">
          <a-col :xs="12" :xl="6"
            ><a-card
              ><a-statistic
                title="库存总价值（含税，元）"
                :value="totalValue"
                :formatter="moneyFormatter" /></a-card
          ></a-col>
          <a-col :xs="12" :xl="6"
            ><a-card
              ><a-statistic
                title="物料种类"
                :value="parts.length"
                suffix="种" /></a-card
          ></a-col>
          <a-col :xs="12" :xl="6"
            ><a-card
              ><a-statistic
                title="低库存物料"
                :value="lowStockCount"
                suffix="种"
                :value-style="{
                  color: lowStockCount > 0 ? '#ff4d4f' : '#52c41a',
                }" /></a-card
          ></a-col>
          <a-col :xs="12" :xl="6"
            ><a-card
              ><a-statistic
                title="需补货建议"
                :value="replenishmentSuggestions.length"
                suffix="项"
                :value-style="{
                  color:
                    replenishmentSuggestions.length > 0 ? '#fa8c16' : '#52c41a',
                }" /></a-card
          ></a-col>
        </a-row>

        <a-row :gutter="12" style="margin-top: 12px">
          <a-col :xs="24" :lg="12">
            <a-card title="库存按物料分类分布" size="small">
              <div v-if="byCategory.length === 0" class="chart-empty">
                暂无数据
              </div>
              <div v-else class="dist-rows">
                <div
                  v-for="item in byCategory"
                  :key="item.name"
                  class="dist-row"
                >
                  <span class="dist-label">{{ item.name }}</span>
                  <div class="dist-track">
                    <div
                      class="dist-fill"
                      :style="{
                        width: distPercent(item.value, byCategory) + '%',
                        background: '#52c41a',
                      }"
                    ></div>
                  </div>
                  <span class="dist-money">{{ formatMoney(item.value) }}</span>
                </div>
              </div>
            </a-card>
          </a-col>
          <a-col :xs="24" :lg="12">
            <a-card title="低库存预警" size="small">
              <div
                v-if="lowStockItems.length === 0"
                style="text-align: center; padding: 24px; color: #52c41a"
              >
                ✅ 暂无低库存物料
              </div>
              <a-table
                v-else
                size="small"
                :data-source="lowStockItems"
                :columns="lowStockColumns"
                :pagination="false"
                :row-key="(r: any) => r.partCode"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'name'"
                    ><strong>{{ record.partName }}</strong
                    ><span class="table-subtitle">{{
                      record.spec || ""
                    }}</span></template
                  >
                  <template v-else-if="column.key === 'stock'"
                    >{{ record.stockQty }}{{ record.unit }}</template
                  >
                  <template v-else-if="column.key === 'value'">{{
                    formatMoney(record.stockQty * (record.unitCost || 0))
                  }}</template>
                </template>
              </a-table>
            </a-card>
          </a-col>
        </a-row>

        <a-card title="智能补货建议" size="small" style="margin-top: 12px">
          <a-table
            size="small"
            :data-source="replenishmentSuggestions"
            :columns="replenishmentColumns"
            :pagination="{ pageSize: 6 }"
            :row-key="(r: any) => r.partId"
            :scroll="{ x: 920 }"
            :row-selection="{
              selectedRowKeys,
              onChange: (keys: (string | number)[]) => {
                selectedRowKeys = keys.map(String);
              },
            }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'part'"
                ><strong>{{ record.partName }}</strong
                ><span class="table-subtitle"
                  >{{ record.partCode || "-" }} ·
                  {{ record.model || "无规格" }}</span
                ></template
              >
              <template v-else-if="column.key === 'stock'"
                >{{ record.stockQty }} / {{ record.safetyQty }}</template
              >
              <template v-else-if="column.key === 'outbound'">{{
                record.recentOutboundQty
              }}</template>
              <template v-else-if="column.key === 'suggested'"
                ><strong>{{ record.suggestedQty }}</strong></template
              >
              <template v-else-if="column.key === 'priority'"
                ><a-tag :color="priorityColor(record.priority)">{{
                  priorityLabel(record.priority)
                }}</a-tag></template
              >
            </template>
            <template #emptyText>暂无补货建议</template>
          </a-table>
        </a-card>
      </a-spin>
    </a-card>

    <a-modal
      v-model:open="convertOpen"
      title="生成采购申请"
      width="760px"
      :confirm-loading="converting"
      @ok="handleConvert"
    >
      <a-form layout="vertical">
        <a-row :gutter="12">
          <a-col :span="24"
            ><a-form-item label="成本归属" required
              ><a-segmented
                v-model:value="convertForm.costType"
                :options="costTypeOptions"
                style="width: 100%" /></a-form-item
          ></a-col>
          <a-col v-if="convertForm.costType === 'PROJECT'" :span="24"
            ><a-form-item label="关联项目" required
              ><a-select
                v-model:value="convertForm.projectId"
                :options="projectOptions"
                show-search
                option-filter-prop="label"
                placeholder="选择已审批且未关闭的项目" /></a-form-item
          ></a-col>
          <a-col v-else :span="24"
            ><a-form-item label="成本部门" required
              ><a-select
                v-model:value="convertForm.departmentId"
                :options="departmentOptions"
                show-search
                option-filter-prop="label"
                placeholder="选择承担采购成本的部门" /></a-form-item
          ></a-col>
          <a-col :xs="24" :md="12"
            ><a-form-item label="期望到货日期"
              ><a-date-picker
                v-model:value="convertForm.expectedDate"
                style="width: 100%" /></a-form-item
          ></a-col>
          <a-col :xs="24" :md="12"
            ><a-form-item label="申请说明"
              ><a-input
                v-model:value="convertForm.reason"
                placeholder="补货原因（可选，默认沿用建议原因）" /></a-form-item
          ></a-col>
        </a-row>
        <a-table
          size="small"
          :data-source="convertLines"
          :columns="convertLineColumns"
          :pagination="false"
          :row-key="(r: any) => r.partId"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'part'"
              ><strong>{{ record.partName }}</strong
              ><span class="table-subtitle"
                >{{ record.partCode || "-" }} ·
                {{ record.model || "无规格" }}</span
              ></template
            >
            <template v-else-if="column.key === 'quantity'"
              ><a-input-number
                v-model:value="record.quantity"
                :min="0.01"
                :precision="2"
                size="small"
            /></template>
            <template v-else-if="column.key === 'unitPrice'"
              ><a-input-number
                v-model:value="record.unitPrice"
                :min="0"
                :precision="2"
                size="small"
                style="width: 120px"
            /></template>
          </template>
        </a-table>
      </a-form>
    </a-modal>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import dayjs from "dayjs";
import { message } from "ant-design-vue";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import {
  listInventoryParts,
  listReplenishmentSuggestions,
  type InventoryPart,
  type ReplenishmentSuggestion,
} from "@/api/inventory";
import {
  createReplenishmentPurchaseRequests,
  listProcurementCostTargets,
  type ProcurementCostTargetOptions,
} from "@/api/procurement";
const router = useRouter();
const loading = ref(false);
const parts = ref<InventoryPart[]>([]);
const replenishmentSuggestions = ref<ReplenishmentSuggestion[]>([]);
const selectedRowKeys = ref<string[]>([]);
const convertOpen = ref(false);
const converting = ref(false);
const convertLines = ref<ConvertLine[]>([]);
const costTargets = ref<ProcurementCostTargetOptions>({
  projects: [],
  departments: [],
});
const convertForm = reactive<{
  costType: "PROJECT" | "DEPARTMENT";
  projectId?: string;
  departmentId?: string;
  reason: string;
  expectedDate?: dayjs.Dayjs;
}>({
  costType: "DEPARTMENT",
  projectId: undefined,
  departmentId: undefined,
  reason: "",
  expectedDate: undefined,
});

type ConvertLine = {
  partId: string;
  partCode?: string;
  partName: string;
  model?: string;
  quantity: number;
  unitPrice: number;
  reason?: string;
};

const costTypeOptions = [
  { label: "项目采购", value: "PROJECT" },
  { label: "部门采购", value: "DEPARTMENT" },
];
const projectOptions = computed(() =>
  costTargets.value.projects.map((item) => ({
    label: `${item.name} (${item.code})`,
    value: item.id,
  })),
);
const departmentOptions = computed(() =>
  costTargets.value.departments.map((item) => ({
    label: `${item.name} (${item.code})`,
    value: item.id,
  })),
);
const convertLineColumns = [
  { title: "物料", key: "part" },
  { title: "补货数量", key: "quantity", width: 130 },
  { title: "单价（含税，元）", key: "unitPrice", width: 150 },
];

const totalValue = computed(() =>
  parts.value.reduce(
    (s, p) => s + Number(p.stockQty || 0) * Number(p.unitCost || 0),
    0,
  ),
);
const lowStockCount = computed(
  () =>
    parts.value.filter(
      (p) => Number(p.stockQty || 0) <= Number(p.safetyQty || 0),
    ).length,
);
const lowStockItems = computed(() =>
  parts.value
    .filter((p) => Number(p.stockQty || 0) <= Number(p.safetyQty || 0))
    .sort((a, b) => Number(a.stockQty || 0) - Number(b.stockQty || 0)),
);
const lowStockColumns = [
  { title: "物料", key: "name", width: 260 },
  { title: "当前库存", key: "stock", width: 120 },
  { title: "安全库存", dataIndex: "safetyQty", width: 100 },
  { title: "价值", key: "value", width: 130 },
];
const replenishmentColumns = [
  { title: "物料", key: "part", width: 260 },
  { title: "库存/安全", key: "stock", width: 120 },
  { title: "近30天消耗", key: "outbound", width: 120 },
  { title: "建议补货", key: "suggested", width: 120 },
  { title: "优先级", key: "priority", width: 100 },
  { title: "原因", dataIndex: "reason", width: 260 },
];

const byCategory = computed(() => {
  const map = new Map<string, number>();
  parts.value.forEach((p) => {
    const wh = p.category || "未分类";
    map.set(
      wh,
      (map.get(wh) || 0) + Number(p.stockQty || 0) * Number(p.unitCost || 0),
    );
  });
  return Array.from(map.entries())
    .map(([name, value]) => ({ name, value }))
    .sort((a, b) => b.value - a.value);
});

function distPercent(value: number, data: { value: number }[]) {
  const max = Math.max(...data.map((d) => d.value), 1);
  return Math.round((value / max) * 100);
}

onMounted(loadData);
async function loadData() {
  loading.value = true;
  try {
    const [partRows, suggestions] = await Promise.all([
      listInventoryParts(),
      listReplenishmentSuggestions(),
    ]);
    parts.value = partRows;
    replenishmentSuggestions.value = suggestions;
    listProcurementCostTargets()
      .then((targets) => {
        costTargets.value = targets;
      })
      .catch(() => undefined);
  } catch (e: any) {
    message.error(e.message || "加载失败");
  } finally {
    loading.value = false;
  }
}
function openConvert() {
  const selected = replenishmentSuggestions.value.filter((s) =>
    selectedRowKeys.value.includes(s.partId),
  );
  if (selected.length === 0) {
    message.warning("请先勾选需要生成采购申请的物料");
    return;
  }
  convertLines.value = selected.map((s) => ({
    partId: s.partId,
    partCode: s.partCode,
    partName: s.partName,
    model: s.model,
    quantity: Number(s.suggestedQty),
    unitPrice: Number(
      parts.value.find((p) => p.id === s.partId)?.unitCost || 0,
    ),
    reason: s.reason,
  }));
  convertForm.costType = "DEPARTMENT";
  convertForm.projectId = undefined;
  convertForm.departmentId = undefined;
  convertForm.reason = "";
  convertForm.expectedDate = undefined;
  convertOpen.value = true;
}
async function handleConvert() {
  const costType = convertForm.costType;
  if (costType === "PROJECT" && !convertForm.projectId) {
    message.warning("请选择成本项目");
    return;
  }
  if (costType === "DEPARTMENT" && !convertForm.departmentId) {
    message.warning("请选择成本部门");
    return;
  }
  converting.value = true;
  try {
    const result = await createReplenishmentPurchaseRequests({
      costType,
      projectId: convertForm.projectId,
      departmentId: convertForm.departmentId,
      reason: convertForm.reason || undefined,
      expectedDate: convertForm.expectedDate
        ? convertForm.expectedDate.format("YYYY-MM-DD")
        : undefined,
      lines: convertLines.value.map((l) => ({
        partId: l.partId,
        quantity: Number(l.quantity),
        unitPrice: Number(l.unitPrice),
        reason: l.reason,
      })),
    });
    message.success(
      `已生成 ${result.totalLines} 条采购申请，批次号 ${result.batchCode}`,
    );
    convertOpen.value = false;
    selectedRowKeys.value = [];
    await loadData();
  } catch (e: any) {
    message.error(e.message || "生成采购申请失败");
  } finally {
    converting.value = false;
  }
}
function formatMoney(v: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
  }).format(v || 0);
}
function moneyFormatter({ value }: { value: number | string }) {
  return formatMoney(Number(value));
}
function priorityLabel(v: string) {
  return (
    ({ HIGH: "高", MEDIUM: "中", LOW: "低" } as Record<string, string>)[v] || v
  );
}
function priorityColor(v: string) {
  return (
    ({ HIGH: "red", MEDIUM: "orange", LOW: "green" } as Record<string, string>)[
      v
    ] || "default"
  );
}
</script>
<style scoped>
.chart-empty {
  text-align: center;
  padding: 32px 0;
  color: #8c8c8c;
}
.dist-rows {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.dist-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.dist-label {
  width: 80px;
  font-size: 13px;
  color: #333;
  flex-shrink: 0;
  text-align: right;
}
.dist-track {
  flex: 1;
  height: 22px;
  background: #f5f5f5;
  border-radius: 4px;
  overflow: hidden;
}
.dist-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.4s;
  min-width: 0;
}
.dist-money {
  font-size: 12px;
  color: #595959;
  width: 80px;
  text-align: right;
  flex-shrink: 0;
}
</style>
