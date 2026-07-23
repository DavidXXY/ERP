<template>
  <BusinessDetailPage
    :title="partName"
    :code="part?.code || (part as any)?.partCode"
    :subtitle="
      part
        ? `${part.model || (part as any).spec || '无规格'} · 库位 ${part.location || '未设置'}`
        : ''
    "
    :loading="loading"
    back-to="/inventory/parts"
    :status-label="part?.lowStock ? '低库存' : '库存正常'"
    :status-color="part?.lowStock ? 'red' : 'green'"
    :risk-label="part?.lowStock ? '需要补货' : ''"
    risk-color="orange"
    :stats="metrics"
    @refresh="loadData"
  >
    <template #actions
      ><a-button
        type="primary"
        @click="router.push(`/procurement/requests?partId=${partId}`)"
        >发起补货申请</a-button
      ></template
    >
    <template #relations>
      <a-steps size="small" :current="part?.lowStock ? 1 : 4" responsive>
        <a-step title="采购入库" :description="`${inboundQty} 入库`" /><a-step
          title="库存可用"
          :description="`${stockQty} 当前库存`"
        />
        <a-step title="项目领料" :description="`${outboundQty} 出库`" /><a-step
          title="退料调整"
          :description="`${returnQty} 退回`"
        /><a-step title="库存盘存" description="流水可追溯" />
      </a-steps>
    </template>
    <a-card v-if="part" :bordered="false">
      <a-tabs>
        <a-tab-pane key="overview" tab="物料库存">
          <a-descriptions
            bordered
            :column="{ xs: 1, md: 2, xl: 3 }"
            size="small"
          >
            <a-descriptions-item label="物料编码">{{
              part.code || (part as any).partCode || "-"
            }}</a-descriptions-item
            ><a-descriptions-item label="物料名称">{{
              partName
            }}</a-descriptions-item
            ><a-descriptions-item label="规格型号">{{
              part.model || (part as any).spec || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="当前库存"
              >{{ stockQty }}
              {{ (part as any).unit || "" }}</a-descriptions-item
            ><a-descriptions-item label="安全库存"
              >{{ safetyQty }}
              {{ (part as any).unit || "" }}</a-descriptions-item
            ><a-descriptions-item label="库位">{{
              part.location || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="单位成本">{{
              money(part.unitCost)
            }}</a-descriptions-item
            ><a-descriptions-item label="库存价值">{{
              money(stockQty * Number(part.unitCost || 0))
            }}</a-descriptions-item
            ><a-descriptions-item label="库存状态">{{
              part.lowStock ? "低于安全库存" : "库存正常"
            }}</a-descriptions-item>
          </a-descriptions>
          <a-alert
            v-if="part.lowStock"
            class="section-gap"
            type="warning"
            show-icon
            message="库存低于安全线"
            :description="`建议补货 ${Math.max(0, safetyQty * 2 - stockQty)} ${(part as any).unit || ''}，可直接发起采购申请。`"
          />
        </a-tab-pane>
        <a-tab-pane key="movement" :tab="`出入库追踪 (${movements.length})`">
          <a-table
            :columns="movementColumns"
            :data-source="movements"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'type'"
                ><a-tag
                  :color="
                    ['INBOUND', 'RETURN'].includes(record.movementType)
                      ? 'green'
                      : record.movementType === 'OUTBOUND'
                        ? 'blue'
                        : 'orange'
                  "
                  >{{ movementLabel(record.movementType) }}</a-tag
                ></template
              >
              <template v-else-if="column.key === 'quantity'"
                ><strong>{{ record.quantity }}</strong></template
              >
              <template v-else-if="column.key === 'date'">{{
                dateTime(record.createdAt)
              }}</template>
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="trace" tab="批次与来源追溯">
          <a-timeline v-if="movements.length">
            <a-timeline-item
              v-for="item in movements"
              :key="item.id"
              :color="
                ['INBOUND', 'RETURN'].includes(item.movementType)
                  ? 'green'
                  : 'blue'
              "
              ><strong
                >{{ movementLabel(item.movementType) }}
                {{ item.quantity }}</strong
              >
              <p>
                来源单号：{{ item.sourceNo || "手工调整" }} ·
                {{ item.remark || "无备注" }}
              </p>
              <small>{{ dateTime(item.createdAt) }}</small></a-timeline-item
            > </a-timeline
          ><a-empty v-else description="暂无库存流水" />
        </a-tab-pane>
        <a-tab-pane key="aging" tab="库龄与补货">
          <a-result
            :status="part.lowStock ? 'warning' : 'success'"
            :title="part.lowStock ? '建议立即补货' : '当前库存健康'"
            :sub-title="`最近入库 ${inboundQty}，出库 ${outboundQty}，退回 ${returnQty}；安全库存 ${safetyQty}。`"
          />
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </BusinessDetailPage>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { message } from "ant-design-vue";
import BusinessDetailPage, {
  type DetailMetric,
} from "@/components/BusinessDetailPage.vue";
import {
  listInventoryParts,
  listStockMovements,
  type InventoryPart,
  type StockMovement,
} from "@/api/inventory";
const route = useRoute(),
  router = useRouter(),
  partId = computed(() => String(route.params.id)),
  loading = ref(false),
  part = ref<InventoryPart | null>(null),
  movements = ref<StockMovement[]>([]);
const partName = computed(
    () => part.value?.name || (part.value as any)?.partName || "物料详情",
  ),
  stockQty = computed(() =>
    Number(part.value?.stockQty ?? (part.value as any)?.quantity ?? 0),
  ),
  safetyQty = computed(() =>
    Number(part.value?.safetyQty ?? (part.value as any)?.safetyStock ?? 0),
  );
const inboundQty = computed(() => sumBy(["INBOUND"])),
  outboundQty = computed(() => sumBy(["OUTBOUND", "SCRAP"])),
  returnQty = computed(() => sumBy(["RETURN"]));
const metrics = computed<DetailMetric[]>(() =>
  part.value
    ? [
        {
          label: "当前库存",
          value: stockQty.value,
          hint: `安全库存 ${safetyQty.value}`,
          danger: part.value.lowStock,
        },
        {
          label: "可用价值",
          value: money(stockQty.value * Number(part.value.unitCost || 0)),
        },
        { label: "累计入库", value: inboundQty.value },
        { label: "累计出库", value: outboundQty.value },
        {
          label: "建议补货",
          value: Math.max(0, safetyQty.value * 2 - stockQty.value),
          warning: part.value.lowStock,
        },
      ]
    : [],
);
const movementColumns = [
  { title: "类型", key: "type", width: 110 },
  { title: "数量", key: "quantity", width: 100 },
  { title: "来源单号", dataIndex: "sourceNo", width: 190 },
  { title: "备注", dataIndex: "remark" },
  { title: "发生时间", key: "date", width: 160 },
];
onMounted(loadData);
async function loadData() {
  loading.value = true;
  try {
    const [parts, flows] = await Promise.all([
      listInventoryParts(),
      listStockMovements(partId.value),
    ]);
    part.value = parts.find((i) => i.id === partId.value) || null;
    movements.value = flows;
  } catch (e) {
    message.error(e instanceof Error ? e.message : "库存详情加载失败");
  } finally {
    loading.value = false;
  }
}
function sumBy(types: string[]) {
  return movements.value
    .filter((i) => types.includes(i.movementType))
    .reduce((s, i) => s + Number(i.quantity || 0), 0);
}
function money(v?: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    maximumFractionDigits: 0,
  }).format(Number(v || 0));
}
function dateTime(v?: string) {
  return v ? v.slice(0, 16).replace("T", " ") : "-";
}
function movementLabel(v: string) {
  return (
    (
      {
        INBOUND: "采购入库",
        OUTBOUND: "领料出库",
        RETURN: "退料入库",
        SCRAP: "报废出库",
        ADJUSTMENT: "库存调整",
      } as Record<string, string>
    )[v] || v
  );
}
</script>
<style scoped>
.section-gap {
  margin-top: 16px;
}
.ant-timeline p {
  margin: 5px 0;
  color: #657186;
}
.ant-timeline small {
  color: #9aa4b2;
}
</style>
