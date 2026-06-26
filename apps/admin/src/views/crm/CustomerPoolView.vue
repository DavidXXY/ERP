<template>
  <div class="page-stack">
    <a-card>
      <template #title>客户池</template>
      <template #extra>
        <a-space>
          <a-button @click="loadCustomers">刷新</a-button>
          <a-button v-if="auth.can('crm:customer:create')" type="primary" @click="createOpen = true">
            新增客户
          </a-button>
        </a-space>
      </template>

      <a-alert
        v-if="errorMessage"
        class="section-alert"
        type="warning"
        show-icon
        :message="errorMessage"
        description="请确认后端服务已启动，或检查 VITE_API_BASE_URL 配置。"
      />

      <a-table
        :columns="columns"
        :data-source="customers"
        :loading="loading"
        :pagination="{ pageSize: 8 }"
        :row-key="(record: CustomerSummary) => record.id"
        :custom-row="customerRow"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <strong>{{ record.name }}</strong>
            <span class="table-subtitle">{{ record.code }} · {{ record.ownerName }}</span>
          </template>
          <template v-else-if="column.key === 'level'">
            <a-tag :color="levelColor(record.level)">{{ levelLabel(record.level) }}</a-tag>
            <span class="table-subtitle">{{ record.industry }}</span>
          </template>
          <template v-else-if="column.key === 'risk'">
            <a-tag :color="riskColor(record.riskStatus)">{{ riskLabel(record.riskStatus) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button size="small" @click.stop="selectCustomer(record.id)">查看详情</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-card v-if="selectedDetail" :loading="detailLoading">
      <template #title>
        <div class="detail-title">
          <span>{{ selectedDetail.name }}</span>
          <a-tag :color="riskColor(selectedDetail.riskStatus)">
            {{ riskLabel(selectedDetail.riskStatus) }}
          </a-tag>
        </div>
      </template>

      <a-row :gutter="[16, 16]" class="metric-row">
        <a-col :xs="24" :md="12" :xl="6">
          <a-statistic title="已签合同" :value="selectedDetail.metrics.contractCount" suffix="份" />
        </a-col>
        <a-col :xs="24" :md="12" :xl="6">
          <a-statistic title="合同总额" :value="selectedDetail.metrics.contractAmount" :formatter="moneyFormatter" />
        </a-col>
        <a-col :xs="24" :md="12" :xl="6">
          <a-statistic title="待收款项" :value="selectedDetail.metrics.outstandingAmount" :formatter="moneyFormatter" />
        </a-col>
        <a-col :xs="24" :md="12" :xl="6">
          <a-statistic title="已核销" :value="selectedDetail.metrics.settledAmount" :formatter="moneyFormatter" />
        </a-col>
      </a-row>

      <a-row :gutter="[16, 16]">
        <a-col :xs="24" :xl="12">
          <a-card size="small" title="基础档案">
            <a-descriptions :column="2" size="small" bordered>
              <a-descriptions-item label="客户编码">{{ selectedDetail.code }}</a-descriptions-item>
              <a-descriptions-item label="行业">{{ selectedDetail.industry }}</a-descriptions-item>
              <a-descriptions-item label="客户等级">{{ levelLabel(selectedDetail.level) }}</a-descriptions-item>
              <a-descriptions-item label="负责人">{{ selectedDetail.ownerName }}</a-descriptions-item>
              <a-descriptions-item label="付款习惯" :span="2">{{ selectedDetail.paymentHabit || "-" }}</a-descriptions-item>
              <a-descriptions-item label="风险说明" :span="2">{{ selectedDetail.riskNote || "-" }}</a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-col>

        <a-col :xs="24" :xl="12">
          <a-card size="small" title="开票资料">
            <a-descriptions :column="2" size="small" bordered>
              <a-descriptions-item label="发票抬头" :span="2">{{ selectedDetail.invoice.title || "-" }}</a-descriptions-item>
              <a-descriptions-item label="税号" :span="2">{{ selectedDetail.invoice.taxNo || "-" }}</a-descriptions-item>
              <a-descriptions-item label="开户银行" :span="2">{{ selectedDetail.invoice.bankName || "-" }}</a-descriptions-item>
              <a-descriptions-item label="银行账号" :span="2">{{ selectedDetail.invoice.bankAccount || "-" }}</a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-col>

        <a-col :xs="24" :xl="12">
          <a-card size="small" title="联系人">
            <a-list :data-source="selectedDetail.contacts" size="small">
              <template #renderItem="{ item }">
                <a-list-item>
                  <a-list-item-meta :title="`${item.name}${item.primaryContact ? ' · 主要联系人' : ''}`">
                    <template #description>{{ item.title || "-" }} · {{ item.phone || "-" }} · {{ item.email || "-" }}</template>
                  </a-list-item-meta>
                </a-list-item>
              </template>
            </a-list>
          </a-card>
        </a-col>

        <a-col :xs="24" :xl="12">
          <a-card size="small" title="项目地址">
            <a-list :data-source="selectedDetail.sites" size="small">
              <template #renderItem="{ item }">
                <a-list-item>
                  <a-list-item-meta :title="item.name" :description="item.address" />
                </a-list-item>
              </template>
            </a-list>
          </a-card>
        </a-col>

        <a-col :xs="24" :xl="12">
          <a-card size="small" title="已签约合同">
            <a-list :data-source="selectedDetail.contracts" size="small">
              <template #renderItem="{ item }">
                <a-list-item>
                  <a-list-item-meta :title="item.projectName">
                    <template #description>{{ item.code }} · {{ item.contractType }} · {{ item.startDate }} 至 {{ item.endDate }}</template>
                  </a-list-item-meta>
                  <template #actions>
                    <a-tag :color="contractColor(item.status)">{{ contractLabel(item.status) }}</a-tag>
                    <strong>{{ formatMoney(item.amount) }}</strong>
                  </template>
                </a-list-item>
              </template>
            </a-list>
          </a-card>
        </a-col>

        <a-col :xs="24" :xl="12">
          <a-card size="small" title="合同款项支付情况">
            <a-list :data-source="selectedDetail.receivables" size="small">
              <template #renderItem="{ item }">
                <a-list-item>
                  <a-list-item-meta :title="item.code">
                    <template #description>{{ item.sourceNo }} · 到期 {{ item.dueDate }}</template>
                  </a-list-item-meta>
                  <template #actions>
                    <a-tag :color="receivableColor(item.status)">{{ receivableLabel(item.status) }}</a-tag>
                    <strong>{{ formatMoney(item.amount) }}</strong>
                  </template>
                </a-list-item>
              </template>
            </a-list>
          </a-card>
        </a-col>
      </a-row>
    </a-card>

    <a-empty v-else-if="!loading" description="请选择客户查看详情" />

    <a-modal
      v-model:open="createOpen"
      title="新增客户"
      width="780px"
      :confirm-loading="saving"
      @ok="handleCreate"
    >
      <a-form ref="formRef" :model="formState" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="客户编码" name="code">
              <a-input v-model:value="formState.code" placeholder="KH-005" />
            </a-form-item>
          </a-col>
          <a-col :span="16">
            <a-form-item label="客户名称" name="name">
              <a-input v-model:value="formState.name" placeholder="客户单位全称" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="行业" name="industry">
              <a-input v-model:value="formState.industry" placeholder="轨道交通 / 园区 / 水务" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="客户等级" name="level">
              <a-select v-model:value="formState.level" :options="levelOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="负责人" name="ownerName">
              <a-input v-model:value="formState.ownerName" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="付款习惯">
              <a-input v-model:value="formState.paymentHabit" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="风险状态">
              <a-select v-model:value="formState.riskStatus" :options="riskOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="主要联系人">
              <a-input v-model:value="formState.contactName" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="联系电话">
              <a-input v-model:value="formState.contactPhone" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="项目地址名称">
              <a-input v-model:value="formState.siteName" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="项目地址">
              <a-input v-model:value="formState.siteAddress" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="发票抬头">
              <a-input v-model:value="formState.invoiceTitle" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="纳税人识别号">
              <a-input v-model:value="formState.taxNo" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import {
  createCustomer,
  getCustomer,
  listCustomers,
  type ContractStatus,
  type CreateCustomerPayload,
  type CustomerDetail,
  type CustomerLevel,
  type CustomerSummary,
  type ReceivableStatus,
  type RiskStatus,
} from "@/api/crm";
import { useAuthStore } from "@/stores/auth";

type CreateFormState = {
  code: string;
  name: string;
  industry: string;
  level: CustomerLevel;
  ownerName: string;
  paymentHabit: string;
  riskStatus: RiskStatus;
  contactName: string;
  contactPhone: string;
  siteName: string;
  siteAddress: string;
  invoiceTitle: string;
  taxNo: string;
};

const customers = ref<CustomerSummary[]>([]);
const selectedDetail = ref<CustomerDetail | null>(null);
const loading = ref(false);
const detailLoading = ref(false);
const saving = ref(false);
const createOpen = ref(false);
const errorMessage = ref("");
const formRef = ref();
const auth = useAuthStore();

const columns = [
  { title: "客户", key: "name", dataIndex: "name", width: 260 },
  { title: "等级 / 行业", key: "level", dataIndex: "level", width: 160 },
  { title: "联系人", dataIndex: "primaryContact", width: 180 },
  { title: "项目地址数", dataIndex: "siteCount", width: 110 },
  { title: "付款习惯", dataIndex: "paymentHabit" },
  { title: "风险", key: "risk", dataIndex: "riskStatus", width: 120 },
  { title: "操作", key: "action", width: 110 },
];

const levelOptions = [
  { label: "战略客户", value: "STRATEGIC" },
  { label: "重点客户", value: "KEY" },
  { label: "普通客户", value: "NORMAL" },
];

const riskOptions = [
  { label: "正常", value: "NORMAL" },
  { label: "逾期", value: "OVERDUE" },
  { label: "续约风险", value: "RENEWAL_RISK" },
];

const rules = {
  code: [{ required: true, message: "请输入客户编码" }],
  name: [{ required: true, message: "请输入客户名称" }],
  industry: [{ required: true, message: "请输入行业" }],
  ownerName: [{ required: true, message: "请输入负责人" }],
};

const formState = reactive<CreateFormState>(initialForm());

onMounted(loadCustomers);

async function loadCustomers() {
  loading.value = true;
  errorMessage.value = "";
  try {
    customers.value = await listCustomers();
    if (customers.value.length) {
      await selectCustomer(selectedDetail.value?.id || customers.value[0].id);
    } else {
      selectedDetail.value = null;
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "客户列表加载失败";
  } finally {
    loading.value = false;
  }
}

async function selectCustomer(id: string) {
  detailLoading.value = true;
  errorMessage.value = "";
  try {
    selectedDetail.value = await getCustomer(id);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "客户详情加载失败";
  } finally {
    detailLoading.value = false;
  }
}

function customerRow(record: CustomerSummary) {
  return {
    onClick: () => selectCustomer(record.id),
  };
}

async function handleCreate() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    const payload: CreateCustomerPayload = {
      code: formState.code,
      name: formState.name,
      industry: formState.industry,
      level: formState.level,
      ownerName: formState.ownerName,
      paymentHabit: formState.paymentHabit,
      riskStatus: formState.riskStatus,
      invoice: {
        title: formState.invoiceTitle,
        taxNo: formState.taxNo,
      },
      contacts: formState.contactName
        ? [
            {
              name: formState.contactName,
              phone: formState.contactPhone,
              primaryContact: true,
            },
          ]
        : [],
      sites:
        formState.siteName && formState.siteAddress
          ? [{ name: formState.siteName, address: formState.siteAddress }]
          : [],
    };
    const created = await createCustomer(payload);
    Object.assign(formState, initialForm());
    createOpen.value = false;
    message.success("客户已创建");
    await loadCustomers();
    await selectCustomer(created.id);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "客户创建失败");
  } finally {
    saving.value = false;
  }
}

function initialForm(): CreateFormState {
  return {
    code: "",
    name: "",
    industry: "",
    level: "NORMAL",
    ownerName: "",
    paymentHabit: "",
    riskStatus: "NORMAL",
    contactName: "",
    contactPhone: "",
    siteName: "",
    siteAddress: "",
    invoiceTitle: "",
    taxNo: "",
  };
}

function levelLabel(level: CustomerLevel) {
  return {
    STRATEGIC: "战略客户",
    KEY: "重点客户",
    NORMAL: "普通客户",
  }[level];
}

function levelColor(level: CustomerLevel) {
  return {
    STRATEGIC: "green",
    KEY: "blue",
    NORMAL: "default",
  }[level];
}

function riskLabel(status: RiskStatus) {
  return {
    NORMAL: "正常",
    OVERDUE: "逾期",
    RENEWAL_RISK: "续约风险",
  }[status];
}

function riskColor(status: RiskStatus) {
  return {
    NORMAL: "green",
    OVERDUE: "red",
    RENEWAL_RISK: "orange",
  }[status];
}

function contractLabel(status: ContractStatus) {
  return {
    ACTIVE: "履约中",
    RENEWAL_PENDING: "待续约",
    OVERDUE_RISK: "逾期风险",
    CLOSED: "已关闭",
  }[status];
}

function contractColor(status: ContractStatus) {
  return {
    ACTIVE: "green",
    RENEWAL_PENDING: "orange",
    OVERDUE_RISK: "red",
    CLOSED: "default",
  }[status];
}

function receivableLabel(status: ReceivableStatus) {
  return {
    INVOICE_PENDING: "待开票",
    PAYMENT_PENDING: "待回款",
    SETTLED: "已核销",
    OVERDUE: "逾期",
  }[status];
}

function receivableColor(status: ReceivableStatus) {
  return {
    INVOICE_PENDING: "orange",
    PAYMENT_PENDING: "blue",
    SETTLED: "green",
    OVERDUE: "red",
  }[status];
}

function formatMoney(value: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    maximumFractionDigits: 0,
  }).format(value || 0);
}

function moneyFormatter({ value }: { value: number }) {
  return formatMoney(value);
}
</script>
