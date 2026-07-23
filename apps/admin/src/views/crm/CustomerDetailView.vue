<template>
  <BusinessDetailPage
    :title="detail?.name"
    :code="detail?.code"
    :subtitle="
      detail
        ? `${detail.industry || '未设置行业'} · 负责人 ${detail.ownerName}`
        : ''
    "
    :loading="loading"
    back-to="/crm/customers"
    :status-label="levelLabel(detail?.level)"
    :status-color="
      detail?.level === 'STRATEGIC'
        ? 'purple'
        : detail?.level === 'KEY'
          ? 'blue'
          : 'default'
    "
    :risk-label="riskLabel(detail?.riskStatus)"
    :risk-color="detail?.riskStatus === 'NORMAL' ? 'green' : 'red'"
    :stats="metrics"
    @refresh="loadData"
  >
    <template #actions>
      <a-button
        type="primary"
        @click="router.push(`/crm/opportunities?customerId=${customerId}`)"
        >新建商机</a-button
      >
    </template>
    <template #relations>
      <a-steps size="small" :current="relationCurrent" responsive>
        <a-step
          title="客户建档"
          :description="`${detail?.contactCount || 0} 位联系人`"
        />
        <a-step
          title="商机与报价"
          :description="`${detail?.opportunities.length || 0} 个商机`"
        />
        <a-step
          title="合同签约"
          :description="`${detail?.contracts.length || 0} 份合同`"
        />
        <a-step title="履约项目" description="合同生效后承接" />
        <a-step
          title="开票回款"
          :description="`已收 ${money(detail?.metrics.settledAmount)}`"
        />
      </a-steps>
    </template>

    <a-card v-if="detail" :bordered="false">
      <a-tabs>
        <a-tab-pane key="overview" tab="客户档案">
          <a-row :gutter="[16, 16]">
            <a-col :xs="24" :xl="14">
              <a-descriptions
                bordered
                :column="{ xs: 1, md: 2 }"
                size="small"
                title="基本资料"
              >
                <a-descriptions-item label="客户编码">{{
                  detail.code || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="负责人">{{
                  detail.ownerName || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="客户等级">{{
                  levelLabel(detail.level)
                }}</a-descriptions-item>
                <a-descriptions-item label="行业">{{
                  detail.industry || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="付款习惯">{{
                  detail.paymentHabit || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="风险状态">{{
                  riskLabel(detail.riskStatus)
                }}</a-descriptions-item>
                <a-descriptions-item label="风险说明" :span="2">{{
                  detail.riskNote || "暂无风险说明"
                }}</a-descriptions-item>
              </a-descriptions>
            </a-col>
            <a-col :xs="24" :xl="10">
              <a-descriptions
                bordered
                :column="1"
                size="small"
                title="开票信息"
              >
                <a-descriptions-item label="抬头">{{
                  detail.invoice.title || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="税号">{{
                  detail.invoice.taxNo || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="开户行">{{
                  detail.invoice.bankName || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="银行账号">{{
                  detail.invoice.bankAccount || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="注册地址/电话">{{
                  [
                    detail.invoice.registeredAddress,
                    detail.invoice.registeredPhone,
                  ]
                    .filter(Boolean)
                    .join(" / ") || "-"
                }}</a-descriptions-item>
              </a-descriptions>
            </a-col>
          </a-row>
        </a-tab-pane>

        <a-tab-pane
          key="contacts"
          :tab="`联系人与地址 (${detail.contacts.length}/${detail.sites.length})`"
        >
          <a-row :gutter="[16, 16]">
            <a-col :xs="24" :lg="12">
              <a-table
                :columns="contactColumns"
                :data-source="detail.contacts"
                row-key="id"
                size="small"
                :pagination="false"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'name'"
                    ><strong>{{ record.name }}</strong
                    ><a-tag v-if="record.primaryContact" color="green"
                      >主要</a-tag
                    ></template
                  >
                </template>
              </a-table>
            </a-col>
            <a-col :xs="24" :lg="12">
              <a-table
                :columns="siteColumns"
                :data-source="detail.sites"
                row-key="id"
                size="small"
                :pagination="false"
              />
            </a-col>
          </a-row>
        </a-tab-pane>

        <a-tab-pane key="sales" :tab="`商机 (${detail.opportunities.length})`">
          <a-table
            :columns="opportunityColumns"
            :data-source="detail.opportunities"
            row-key="id"
            :scroll="{ x: 900 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'code'"
                ><a @click="router.push(`/crm/opportunities/${record.id}`)">{{
                  record.code || "查看商机"
                }}</a
                ><span class="table-subtitle">{{
                  record.needSummary
                }}</span></template
              >
              <template v-else-if="column.key === 'amount'">{{
                money(record.expectedAmount)
              }}</template>
              <template v-else-if="column.key === 'probability'"
                >{{ record.probability }}%</template
              >
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="contracts" :tab="`合同 (${detail.contracts.length})`">
          <a-table
            :columns="contractColumns"
            :data-source="detail.contracts"
            row-key="id"
            :scroll="{ x: 920 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'contract'"
                ><a @click="router.push(`/crm/contracts/${record.id}`)">{{
                  record.code || record.projectName
                }}</a
                ><span class="table-subtitle">{{
                  record.projectName
                }}</span></template
              >
              <template v-else-if="column.key === 'amount'">{{
                money(record.amount)
              }}</template>
              <template v-else-if="column.key === 'status'"
                ><a-tag>{{
                  contractStatusLabel(record.status)
                }}</a-tag></template
              >
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane
          key="finance"
          :tab="`应收回款 (${detail.receivables.length})`"
        >
          <a-table
            :columns="receivableColumns"
            :data-source="detail.receivables"
            row-key="id"
            :scroll="{ x: 1000 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'code'"
                ><a @click="router.push(`/finance/receivables/${record.id}`)">{{
                  record.code || record.sourceNo
                }}</a></template
              >
              <template
                v-else-if="
                  ['amount', 'settled', 'outstanding'].includes(column.key)
                "
                >{{
                  money(
                    column.key === "amount"
                      ? record.amount
                      : column.key === "settled"
                        ? record.settledAmount
                        : record.outstandingAmount,
                  )
                }}</template
              >
              <template v-else-if="column.key === 'status'"
                ><a-tag
                  :color="
                    record.status === 'OVERDUE'
                      ? 'red'
                      : record.status === 'SETTLED'
                        ? 'green'
                        : 'blue'
                  "
                  >{{ receivableStatusLabel(record.status) }}</a-tag
                ></template
              >
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane
          key="timeline"
          :tab="`跟进时间线 (${detail.followUps.length})`"
        >
          <a-timeline v-if="detail.followUps.length">
            <a-timeline-item v-for="item in detail.followUps" :key="item.id">
              <strong>{{ item.subject }}</strong> · {{ item.ownerName }}
              <p>{{ item.content }}</p>
              <small
                >{{ dateTime(item.followedAt)
                }}<template v-if="item.nextAction">
                  · 下一步：{{ item.nextAction }}</template
                ></small
              >
            </a-timeline-item>
          </a-timeline>
          <a-empty v-else description="暂无跟进记录" />
        </a-tab-pane>
      </a-tabs>
    </a-card>
    <a-result
      v-else-if="!loading"
      status="404"
      title="客户不存在"
      sub-title="该客户可能已被删除或不在当前数据权限范围内"
    />
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
  getCustomer,
  type CustomerDetail,
  type CustomerLevel,
  type RiskStatus,
} from "@/api/crm";

const route = useRoute();
const router = useRouter();
const customerId = computed(() => String(route.params.id));
const loading = ref(false);
const detail = ref<CustomerDetail | null>(null);

const metrics = computed<DetailMetric[]>(() =>
  detail.value
    ? [
        {
          label: "已签订单金额",
          value: money(detail.value.signedOrderAmount),
          hint: `${detail.value.contracts.length} 份合同`,
        },
        {
          label: "累计支付金额",
          value: money(detail.value.paidAmount),
          hint: "已核销回款",
        },
        {
          label: "待付金额",
          value: money(detail.value.pendingAmount),
          danger: detail.value.pendingAmount > 0,
          hint: "客户待支付",
        },
        {
          label: "在跟商机",
          value: detail.value.opportunities.filter(
            (item) => !["WON", "LOST"].includes(item.stage),
          ).length,
          hint: `商机总数 ${detail.value.opportunities.length}`,
        },
      ]
    : [],
);
const relationCurrent = computed(() =>
  detail.value?.receivables.some((item) => item.settledAmount > 0)
    ? 4
    : detail.value?.contracts.length
      ? 3
      : detail.value?.opportunities.length
        ? 1
        : 0,
);
const contactColumns = [
  { title: "联系人", key: "name" },
  { title: "职务", dataIndex: "title" },
  { title: "电话", dataIndex: "phone" },
  { title: "邮箱", dataIndex: "email" },
];
const siteColumns = [
  { title: "项目地址", dataIndex: "name", width: 150 },
  { title: "详细地址", dataIndex: "address" },
];
const opportunityColumns = [
  { title: "商机", key: "code", width: 280 },
  { title: "阶段", dataIndex: "stage", width: 120 },
  { title: "预计金额", key: "amount", width: 140 },
  { title: "成功率", key: "probability", width: 90 },
  { title: "下一步", dataIndex: "nextAction", width: 200 },
  { title: "负责人", dataIndex: "ownerName", width: 120 },
];
const contractColumns = [
  { title: "合同", key: "contract", width: 260 },
  { title: "类型", dataIndex: "contractType", width: 130 },
  { title: "金额", key: "amount", width: 140 },
  { title: "开始", dataIndex: "startDate", width: 120 },
  { title: "结束", dataIndex: "endDate", width: 120 },
  { title: "状态", key: "status", width: 120 },
];
const receivableColumns = [
  { title: "应收单", key: "code", width: 180 },
  { title: "来源", dataIndex: "sourceNo", width: 180 },
  { title: "应收", key: "amount", width: 140 },
  { title: "已收", key: "settled", width: 140 },
  { title: "未收", key: "outstanding", width: 140 },
  { title: "到期日", dataIndex: "dueDate", width: 120 },
  { title: "状态", key: "status", width: 120 },
];

onMounted(loadData);
async function loadData() {
  loading.value = true;
  try {
    detail.value = await getCustomer(customerId.value);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "客户详情加载失败");
  } finally {
    loading.value = false;
  }
}
function money(value?: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    maximumFractionDigits: 0,
  }).format(Number(value || 0));
}
function dateTime(value?: string) {
  return value ? value.slice(0, 16).replace("T", " ") : "-";
}
function levelLabel(value?: CustomerLevel) {
  return (
    (
      { STRATEGIC: "战略客户", KEY: "重点客户", NORMAL: "普通客户" } as Record<
        string,
        string
      >
    )[value || ""] || ""
  );
}
function riskLabel(value?: RiskStatus) {
  return (
    (
      {
        NORMAL: "风险正常",
        OVERDUE: "存在逾期",
        RENEWAL_RISK: "续约风险",
      } as Record<string, string>
    )[value || ""] || ""
  );
}
function contractStatusLabel(value: string) {
  return (
    (
      {
        PENDING_APPROVAL: "待审批",
        PENDING_SEAL: "待盖章",
        SEAL_APPROVAL: "盖章审批",
        ACTIVE: "履约中",
        RENEWAL_PENDING: "待续约",
        OVERDUE_RISK: "逾期风险",
        CLOSED: "已关闭",
      } as Record<string, string>
    )[value] || value
  );
}
function receivableStatusLabel(value: string) {
  return (
    (
      {
        INVOICE_PENDING: "待开票",
        PAYMENT_PENDING: "待回款",
        SETTLED: "已结清",
        OVERDUE: "已逾期",
      } as Record<string, string>
    )[value] || value
  );
}
</script>

<style scoped>
.table-subtitle {
  display: block;
  color: #8c96a5;
  font-size: 12px;
}
.ant-timeline p {
  margin: 5px 0;
  color: #596579;
}
.ant-timeline small {
  color: #9aa4b2;
}
</style>
