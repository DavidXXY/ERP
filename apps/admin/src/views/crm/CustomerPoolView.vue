<template>
  <div class="page-stack">
    <section class="customer-directory">
      <header class="customer-directory-header">
        <div>
          <div class="customer-title-row">
            <h2>客户池</h2>
            <span>共 {{ customers.length }} 家客户</span>
          </div>
          <div class="customer-summary">
            <span><strong>{{ strategicCount }}</strong> 家战略客户</span>
            <span><strong>{{ riskCount }}</strong> 家风险客户</span>
          </div>
        </div>
        <a-space>
          <a-button title="刷新客户" aria-label="刷新客户" :loading="loading" @click="loadCustomers">
            <template #icon><ReloadOutlined /></template>
          </a-button>
          <a-button v-if="auth.can('crm:customer:create')" type="primary" @click="openCreate">
            <template #icon><PlusOutlined /></template>新增客户
          </a-button>
        </a-space>
      </header>

      <div class="customer-filter-bar">
        <a-input v-model:value="filters.keyword" allow-clear placeholder="搜索客户名称、编码、行业或负责人">
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-select v-model:value="filters.level" allow-clear placeholder="全部客户等级" :options="levelOptions" />
        <a-select v-model:value="filters.riskStatus" allow-clear placeholder="全部风险状态" :options="riskOptions" />
        <span class="customer-filter-result">当前 {{ filteredCustomers.length }} 家</span>
      </div>

      <a-alert v-if="errorMessage" class="customer-error" type="warning" show-icon :message="errorMessage" />

      <div class="customer-desktop-table">
        <a-table
          size="middle"
          :columns="columns"
          :data-source="filteredCustomers"
          :loading="loading"
          :pagination="{ pageSize: 10, showSizeChanger: false }"
          :row-key="(record: CustomerSummary) => record.id"
          :custom-row="customerRow"
          :scroll="{ x: 930 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'">
              <div class="customer-name-cell">
                <span class="customer-avatar" aria-hidden="true">{{ customerInitial(record.name) }}</span>
                <div>
                  <a-button type="link" class="table-link" @click.stop="selectCustomer(record.id)">{{ record.name }}</a-button>
                  <span class="table-subtitle">{{ record.code }} · {{ record.ownerName }}</span>
                </div>
              </div>
            </template>
            <template v-else-if="column.key === 'level'"><a-tag :color="levelColor(record.level)">{{ levelLabel(record.level) }}</a-tag><span class="table-subtitle">{{ record.industry }}</span></template>
            <template v-else-if="column.key === 'contact'"><strong class="customer-cell-primary">{{ record.primaryContact || '未登记联系人' }}</strong><span class="table-subtitle">共 {{ record.contactCount }} 位联系人</span></template>
            <template v-else-if="column.key === 'site'"><strong>{{ record.siteCount }}</strong> 个</template>
            <template v-else-if="column.key === 'payment'"><span class="customer-payment-habit">{{ record.paymentHabit || '未登记付款习惯' }}</span></template>
            <template v-else-if="column.key === 'risk'"><a-tag :color="riskColor(record.riskStatus)">{{ riskLabel(record.riskStatus) }}</a-tag></template>
            <template v-else-if="column.key === 'action'">
              <a-space :size="2">
                <a-button type="text" size="small" title="查看客户" aria-label="查看客户" @click.stop="selectCustomer(record.id)"><template #icon><EyeOutlined /></template></a-button>
                <a-button v-if="auth.can('crm:customer:update')" type="text" size="small" title="编辑客户" aria-label="编辑客户" @click.stop="openEdit(record.id)"><template #icon><EditOutlined /></template></a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>

      <div class="customer-mobile-list">
        <a-spin :spinning="loading">
          <a-empty v-if="!loading && !filteredCustomers.length" description="暂无符合条件的客户" />
          <div v-else class="customer-mobile-items">
            <article v-for="record in filteredCustomers" :key="record.id" class="customer-mobile-item" role="button" tabindex="0" @click="selectCustomer(record.id)" @keydown.enter.prevent="selectCustomer(record.id)">
              <div class="customer-mobile-heading">
                <span class="customer-avatar" aria-hidden="true">{{ customerInitial(record.name) }}</span>
                <div><strong>{{ record.name }}</strong><span>{{ record.code }} · {{ record.ownerName }}</span></div>
                <RightOutlined aria-hidden="true" />
              </div>
              <div class="customer-mobile-tags"><a-tag :color="levelColor(record.level)">{{ levelLabel(record.level) }}</a-tag><a-tag :color="riskColor(record.riskStatus)">{{ riskLabel(record.riskStatus) }}</a-tag><span>{{ record.industry }}</span></div>
              <div class="customer-mobile-meta"><span>联系人 <strong>{{ record.contactCount }}</strong></span><span>项目地址 <strong>{{ record.siteCount }}</strong></span><span class="customer-mobile-payment">{{ record.paymentHabit || '未登记付款习惯' }}</span></div>
            </article>
          </div>
        </a-spin>
      </div>
    </section>

    <a-drawer v-model:open="detailOpen" width="min(1080px, calc(100vw - 24px))" class="customer-detail-drawer" @after-open-change="handleDrawerVisibility">
      <template #title>
        <div v-if="selectedDetail" class="customer-drawer-title">
          <span class="customer-avatar" aria-hidden="true">{{ customerInitial(selectedDetail.name) }}</span>
          <div><strong>{{ selectedDetail.name }}</strong><span>{{ selectedDetail.code }} · {{ selectedDetail.industry }}</span></div>
          <a-tag :color="riskColor(selectedDetail.riskStatus)">{{ riskLabel(selectedDetail.riskStatus) }}</a-tag>
        </div>
        <span v-else>客户详情</span>
      </template>
      <template #extra>
        <a-space v-if="selectedDetail">
          <a-button v-if="auth.can('crm:followup:create')" @click="createFollowUpForCustomer"><template #icon><PlusOutlined /></template>新增跟进</a-button>
          <a-button v-if="auth.can('crm:customer:update')" type="primary" @click="openEdit(selectedDetail.id)"><template #icon><EditOutlined /></template>编辑档案</a-button>
        </a-space>
      </template>

      <a-spin :spinning="detailLoading">
        <template v-if="selectedDetail">
          <div class="customer-metric-band">
            <div><span>已签合同</span><strong>{{ selectedDetail.metrics.contractCount }} 份</strong></div>
            <div><span>合同总额</span><strong>{{ formatMoney(selectedDetail.metrics.contractAmount) }}</strong></div>
            <div><span>待收款项</span><strong :class="{ 'customer-risk-value': selectedDetail.metrics.outstandingAmount > 0 }">{{ formatMoney(selectedDetail.metrics.outstandingAmount) }}</strong></div>
            <div><span>已核销</span><strong>{{ formatMoney(selectedDetail.metrics.settledAmount) }}</strong></div>
          </div>

          <a-tabs v-model:active-key="detailTab" class="customer-detail-tabs">
            <a-tab-pane key="profile" tab="客户档案">
              <div class="customer-detail-grid">
                <section class="customer-detail-section">
                  <h3>基础资料</h3>
                  <a-descriptions :column="2" size="small" bordered>
                    <a-descriptions-item label="客户编码">{{ selectedDetail.code }}</a-descriptions-item>
                    <a-descriptions-item label="客户等级">{{ levelLabel(selectedDetail.level) }}</a-descriptions-item>
                    <a-descriptions-item label="行业">{{ selectedDetail.industry }}</a-descriptions-item>
                    <a-descriptions-item label="负责人">{{ selectedDetail.ownerName }}</a-descriptions-item>
                    <a-descriptions-item label="付款习惯" :span="2">{{ selectedDetail.paymentHabit || '-' }}</a-descriptions-item>
                    <a-descriptions-item label="风险说明" :span="2">{{ selectedDetail.riskNote || '-' }}</a-descriptions-item>
                  </a-descriptions>
                </section>
                <section class="customer-detail-section">
                  <h3>开票资料</h3>
                  <a-descriptions :column="2" size="small" bordered>
                    <a-descriptions-item label="发票抬头" :span="2">{{ selectedDetail.invoice.title || '-' }}</a-descriptions-item>
                    <a-descriptions-item label="纳税人识别号" :span="2">{{ selectedDetail.invoice.taxNo || '-' }}</a-descriptions-item>
                    <a-descriptions-item label="开户银行" :span="2">{{ selectedDetail.invoice.bankName || '-' }}</a-descriptions-item>
                    <a-descriptions-item label="银行账号" :span="2">{{ selectedDetail.invoice.bankAccount || '-' }}</a-descriptions-item>
                    <a-descriptions-item label="注册地址" :span="2">{{ selectedDetail.invoice.registeredAddress || '-' }}</a-descriptions-item>
                    <a-descriptions-item label="注册电话" :span="2">{{ selectedDetail.invoice.registeredPhone || '-' }}</a-descriptions-item>
                  </a-descriptions>
                </section>
                <section class="customer-detail-section">
                  <h3>联系人（{{ selectedDetail.contacts.length }}）</h3>
                  <a-empty v-if="!selectedDetail.contacts.length" description="暂无联系人" />
                  <div v-else class="customer-record-list">
                    <div v-for="contact in selectedDetail.contacts" :key="contact.id" class="customer-record-row">
                      <div><strong>{{ contact.name }}</strong><a-tag v-if="contact.primaryContact" color="blue">主要联系人</a-tag><span>{{ contact.title || '未登记职务' }}</span></div>
                      <div><span>{{ contact.phone || '未登记电话' }}</span><span>{{ contact.email || '未登记邮箱' }}</span></div>
                    </div>
                  </div>
                </section>
                <section class="customer-detail-section">
                  <h3>项目地址（{{ selectedDetail.sites.length }}）</h3>
                  <a-empty v-if="!selectedDetail.sites.length" description="暂无项目地址" />
                  <div v-else class="customer-record-list">
                    <div v-for="site in selectedDetail.sites" :key="site.id" class="customer-record-row customer-site-row"><strong>{{ site.name }}</strong><span>{{ site.address }}</span></div>
                  </div>
                </section>
              </div>
            </a-tab-pane>

            <a-tab-pane key="opportunities" :tab="`跟踪项目 ${selectedDetail.opportunities.length}`">
              <a-table size="small" :columns="opportunityColumns" :data-source="selectedDetail.opportunities" :pagination="false" :row-key="(record: CustomerDetail['opportunities'][number]) => record.id" :scroll="{ x: 900 }">
                <template #bodyCell="{ column, record }"><template v-if="column.key === 'opportunity'"><strong>{{ record.code }}</strong><span class="table-subtitle">{{ record.source || '来源未填' }}</span></template><template v-else-if="column.key === 'stage'"><a-tag :color="opportunityStageColor(record.stage)">{{ opportunityStageLabel(record.stage) }}</a-tag><span class="table-subtitle">成功率 {{ record.probability }}%</span></template><template v-else-if="column.key === 'amount'">{{ formatMoney(record.expectedAmount) }}</template><template v-else-if="column.key === 'nextAction'">{{ record.nextAction || '-' }}<span class="table-subtitle">{{ record.nextActionAt || '未安排日期' }}</span></template></template>
              </a-table>
            </a-tab-pane>

            <a-tab-pane key="contracts" :tab="`已签合同 ${selectedDetail.contracts.length}`">
              <a-table size="small" :columns="contractColumns" :data-source="selectedDetail.contracts" :pagination="false" :row-key="(record: CustomerDetail['contracts'][number]) => record.id" :scroll="{ x: 820 }">
                <template #bodyCell="{ column, record }"><template v-if="column.key === 'contract'"><strong>{{ record.projectName }}</strong><span class="table-subtitle">{{ record.code }} · {{ record.contractType }}</span></template><template v-else-if="column.key === 'period'">{{ record.startDate }} 至 {{ record.endDate }}</template><template v-else-if="column.key === 'amount'">{{ formatMoney(record.amount) }}</template><template v-else-if="column.key === 'status'"><a-tag :color="contractStatusColor(record.status)">{{ contractStatusLabel(record.status) }}</a-tag></template></template>
              </a-table>
            </a-tab-pane>

            <a-tab-pane key="receivables" :tab="`合同应收 ${selectedDetail.receivables.length}`">
              <a-table size="small" :columns="receivableColumns" :data-source="selectedDetail.receivables" :pagination="false" :row-key="(record: CustomerDetail['receivables'][number]) => record.id" :scroll="{ x: 900 }">
                <template #bodyCell="{ column, record }"><template v-if="column.key === 'receivable'"><strong>{{ record.code }}</strong><span class="table-subtitle">{{ record.sourceNo }}</span></template><template v-else-if="column.key === 'amount'"><strong>{{ formatMoney(record.amount) }}</strong><span class="table-subtitle">已收 {{ formatMoney(record.settledAmount) }} · 待收 {{ formatMoney(record.outstandingAmount) }}</span></template><template v-else-if="column.key === 'invoice'">{{ record.invoiceNo || '未开票' }}<span v-if="record.invoiceDate" class="table-subtitle">{{ record.invoiceDate }}</span></template><template v-else-if="column.key === 'status'"><a-tag :color="receivableStatusColor(record.status)">{{ receivableStatusLabel(record.status) }}</a-tag></template></template>
              </a-table>
            </a-tab-pane>

            <a-tab-pane key="followups" :tab="`跟进记录 ${selectedDetail.followUps.length}`">
              <a-empty v-if="!selectedDetail.followUps.length" description="暂无跟进记录" />
              <div v-else class="customer-followup-list">
                <article v-for="item in selectedDetail.followUps" :key="item.id" class="customer-followup-item">
                  <header><div><a-tag :color="followUpTypeColor(item.type)">{{ followUpTypeLabel(item.type) }}</a-tag><strong>{{ item.subject }}</strong></div><time>{{ formatDateTime(item.followedAt) }}</time></header>
                  <p>{{ item.content }}</p>
                  <footer><span>{{ item.ownerName }}<template v-if="item.opportunityCode"> · {{ item.opportunityCode }}</template></span><span>下一步：{{ item.nextAction || '未安排' }}</span></footer>
                </article>
              </div>
            </a-tab-pane>
          </a-tabs>
        </template>
      </a-spin>
    </a-drawer>

    <a-modal v-model:open="formOpen" :title="editingCustomerId ? '编辑客户档案' : '新增客户'" width="960px" :confirm-loading="saving" @ok="saveCustomer">
      <a-form ref="formRef" :model="formState" :rules="rules" layout="vertical">
        <a-tabs v-model:active-key="formTab" class="customer-form-tabs">
          <a-tab-pane key="base" tab="基本资料">
            <a-row :gutter="16">
              <a-col :xs="24" :md="16"><a-form-item label="客户名称" name="name"><a-input v-model:value="formState.name" /></a-form-item></a-col>
              <a-col :xs="24" :md="8"><a-form-item label="行业" name="industry"><a-input v-model:value="formState.industry" /></a-form-item></a-col>
              <a-col :xs="24" :md="8"><a-form-item label="客户等级" name="level"><a-select v-model:value="formState.level" :options="levelOptions" /></a-form-item></a-col>
              <a-col :xs="24" :md="8"><a-form-item label="负责人" name="ownerName"><a-input v-model:value="formState.ownerName" /></a-form-item></a-col>
              <a-col :xs="24" :md="12"><a-form-item label="付款习惯"><a-input v-model:value="formState.paymentHabit" placeholder="例如：月结30天、验收后付款" /></a-form-item></a-col>
              <a-col :xs="24" :md="12"><a-form-item label="风险状态"><a-select v-model:value="formState.riskStatus" :options="riskOptions" /></a-form-item></a-col>
              <a-col :span="24"><a-form-item label="风险说明"><a-textarea v-model:value="formState.riskNote" :rows="3" /></a-form-item></a-col>
            </a-row>
          </a-tab-pane>
          <a-tab-pane key="invoice" tab="开票资料">
            <a-row :gutter="16">
              <a-col :span="24"><a-form-item label="发票抬头"><a-input v-model:value="formState.invoiceTitle" /></a-form-item></a-col>
              <a-col :xs="24" :md="12"><a-form-item label="纳税人识别号"><a-input v-model:value="formState.taxNo" /></a-form-item></a-col>
              <a-col :xs="24" :md="12"><a-form-item label="注册电话"><a-input v-model:value="formState.registeredPhone" /></a-form-item></a-col>
              <a-col :xs="24" :md="12"><a-form-item label="开户银行"><a-input v-model:value="formState.bankName" /></a-form-item></a-col>
              <a-col :xs="24" :md="12"><a-form-item label="银行账号"><a-input v-model:value="formState.bankAccount" /></a-form-item></a-col>
              <a-col :span="24"><a-form-item label="注册地址"><a-input v-model:value="formState.registeredAddress" /></a-form-item></a-col>
            </a-row>
          </a-tab-pane>
          <a-tab-pane key="contacts" :tab="`联系人 ${validContactCount}`">
            <div class="customer-form-list-header"><h3>联系人</h3><a-button @click="addContact"><template #icon><PlusOutlined /></template>添加联系人</a-button></div>
            <div class="customer-form-list">
              <div v-for="(contact, index) in formState.contacts" :key="contact.key" class="customer-contact-form-row">
                <label>姓名<a-input v-model:value="contact.name" placeholder="联系人姓名" /></label>
                <label>职务<a-input v-model:value="contact.title" placeholder="部门或职务" /></label>
                <label>电话<a-input v-model:value="contact.phone" /></label>
                <label>邮箱<a-input v-model:value="contact.email" /></label>
                <label class="customer-primary-switch">主要联系人<a-switch :checked="contact.primaryContact" @change="setPrimaryContact(index, Boolean($event))" /></label>
                <a-button danger type="text" title="删除联系人" aria-label="删除联系人" @click="removeContact(index)"><template #icon><DeleteOutlined /></template></a-button>
              </div>
            </div>
          </a-tab-pane>
          <a-tab-pane key="sites" :tab="`项目地址 ${validSiteCount}`">
            <div class="customer-form-list-header"><h3>项目地址</h3><a-button @click="addSite"><template #icon><PlusOutlined /></template>添加地址</a-button></div>
            <div class="customer-form-list">
              <div v-for="(site, index) in formState.sites" :key="site.key" class="customer-site-form-row">
                <label>地址名称<a-input v-model:value="site.name" placeholder="例如总部、项目现场" /></label>
                <label>详细地址<a-input v-model:value="site.address" /></label>
                <a-button danger type="text" title="删除项目地址" aria-label="删除项目地址" @click="removeSite(index)"><template #icon><DeleteOutlined /></template></a-button>
              </div>
            </div>
          </a-tab-pane>
        </a-tabs>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import { useRoute, useRouter } from "vue-router";
import DeleteOutlined from "@ant-design/icons-vue/DeleteOutlined";
import EditOutlined from "@ant-design/icons-vue/EditOutlined";
import EyeOutlined from "@ant-design/icons-vue/EyeOutlined";
import PlusOutlined from "@ant-design/icons-vue/PlusOutlined";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import RightOutlined from "@ant-design/icons-vue/RightOutlined";
import SearchOutlined from "@ant-design/icons-vue/SearchOutlined";
import {
  createCustomer,
  getCustomer,
  listCustomers,
  updateCustomer,
  type CustomerDetail,
  type CustomerLevel,
  type CustomerSummary,
  type RiskStatus,
  type UpdateCustomerPayload,
} from "@/api/crm";
import { useAuthStore } from "@/stores/auth";
import {
  contractStatusColor,
  contractStatusLabel,
  followUpTypeColor,
  followUpTypeLabel,
  formatMoney,
  levelColor,
  levelLabel,
  opportunityStageColor,
  opportunityStageLabel,
  receivableStatusColor,
  receivableStatusLabel,
  riskColor,
  riskLabel,
} from "./crm-options";

type ContactForm = { key: string; name: string; title: string; phone: string; email: string; primaryContact: boolean };
type SiteForm = { key: string; name: string; address: string };
type CustomerFormState = {
  code: string;
  name: string;
  industry: string;
  level: CustomerLevel;
  ownerName: string;
  paymentHabit: string;
  riskStatus: RiskStatus;
  riskNote: string;
  invoiceTitle: string;
  taxNo: string;
  bankName: string;
  bankAccount: string;
  registeredAddress: string;
  registeredPhone: string;
  contacts: ContactForm[];
  sites: SiteForm[];
};

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const customers = ref<CustomerSummary[]>([]);
const selectedDetail = ref<CustomerDetail | null>(null);
const loading = ref(false);
const detailLoading = ref(false);
const saving = ref(false);
const detailOpen = ref(false);
const formOpen = ref(false);
const editingCustomerId = ref("");
const detailTab = ref("profile");
const formTab = ref("base");
const errorMessage = ref("");
const formRef = ref();
const filters = reactive({ keyword: "", level: undefined as CustomerLevel | undefined, riskStatus: undefined as RiskStatus | undefined });
const formState = reactive<CustomerFormState>(initialForm());

const levelOptions = [{ label: "战略客户", value: "STRATEGIC" }, { label: "重点客户", value: "KEY" }, { label: "普通客户", value: "NORMAL" }];
const riskOptions = [{ label: "正常", value: "NORMAL" }, { label: "逾期", value: "OVERDUE" }, { label: "续约风险", value: "RENEWAL_RISK" }];
const rules = { code: [], name: [{ required: true, message: "请输入客户名称" }], industry: [{ required: true, message: "请输入行业" }], ownerName: [{ required: true, message: "请输入负责人" }] };
const columns = [{ title: "客户", key: "name", width: 210 }, { title: "等级 / 行业", key: "level", width: 120 }, { title: "主要联系人", key: "contact", width: 180 }, { title: "项目地址", key: "site", width: 90 }, { title: "付款习惯", key: "payment", width: 160 }, { title: "风险", key: "risk", width: 90 }, { title: "操作", key: "action", width: 80, fixed: "right" }];
const opportunityColumns = [{ title: "商机", key: "opportunity", width: 150 }, { title: "需求", dataIndex: "needSummary", width: 280 }, { title: "阶段", key: "stage", width: 130 }, { title: "预计金额", key: "amount", width: 130 }, { title: "下一步动作", key: "nextAction", width: 220 }, { title: "负责人", dataIndex: "ownerName", width: 110 }];
const contractColumns = [{ title: "合同", key: "contract", width: 260 }, { title: "期限", key: "period", width: 220 }, { title: "服务周期", dataIndex: "serviceCycle", width: 150 }, { title: "金额", key: "amount", width: 130 }, { title: "状态", key: "status", width: 100 }];
const receivableColumns = [{ title: "应收单", key: "receivable", width: 190 }, { title: "应收 / 已收", key: "amount", width: 250 }, { title: "到期日", dataIndex: "dueDate", width: 120 }, { title: "开票信息", key: "invoice", width: 170 }, { title: "状态", key: "status", width: 100 }];

const filteredCustomers = computed(() => {
  const term = filters.keyword.trim().toLowerCase();
  return customers.value.filter((customer) => {
    const text = `${customer.name} ${customer.code} ${customer.industry} ${customer.ownerName} ${customer.primaryContact || ""}`.toLowerCase();
    return (!filters.level || customer.level === filters.level)
      && (!filters.riskStatus || customer.riskStatus === filters.riskStatus)
      && (!term || text.includes(term));
  });
});
const strategicCount = computed(() => customers.value.filter(customer => customer.level === "STRATEGIC").length);
const riskCount = computed(() => customers.value.filter(customer => customer.riskStatus !== "NORMAL").length);
const validContactCount = computed(() => formState.contacts.filter(contact => contact.name.trim()).length);
const validSiteCount = computed(() => formState.sites.filter(site => site.name.trim() && site.address.trim()).length);

onMounted(loadCustomers);

async function loadCustomers() {
  loading.value = true;
  errorMessage.value = "";
  try {
    customers.value = await listCustomers();
    const queryCustomerId = typeof route.query.customer === "string" ? route.query.customer : undefined;
    if (queryCustomerId && customers.value.some(customer => customer.id === queryCustomerId)) await selectCustomer(queryCustomerId, false);
  } catch (error) { errorMessage.value = error instanceof Error ? error.message : "客户列表加载失败"; }
  finally { loading.value = false; }
}

async function selectCustomer(id: string, syncRoute = true) {
  detailOpen.value = true;
  detailLoading.value = true;
  errorMessage.value = "";
  try {
    selectedDetail.value = await getCustomer(id);
    detailTab.value = "profile";
    if (syncRoute && route.query.customer !== id) await router.replace({ path: route.path, query: { ...route.query, customer: id } });
  } catch (error) { message.error(error instanceof Error ? error.message : "客户详情加载失败"); }
  finally { detailLoading.value = false; }
}

function customerRow(record: CustomerSummary) { return { onClick: () => selectCustomer(record.id) }; }
function customerInitial(name: string) { return name.trim().slice(0, 1) || "客"; }

function openCreate() {
  editingCustomerId.value = "";
  resetForm(initialForm(auth.user?.displayName || ""));
  formTab.value = "base";
  formOpen.value = true;
}

async function openEdit(id: string) {
  detailLoading.value = true;
  try {
    const detail = selectedDetail.value?.id === id ? selectedDetail.value : await getCustomer(id);
    selectedDetail.value = detail;
    editingCustomerId.value = id;
    resetForm(formFromDetail(detail));
    formTab.value = "base";
    formOpen.value = true;
  } catch (error) { message.error(error instanceof Error ? error.message : "客户档案加载失败"); }
  finally { detailLoading.value = false; }
}

async function saveCustomer() {
  await formRef.value?.validate();
  const incompleteContact = formState.contacts.find(contact => (contact.title || contact.phone || contact.email) && !contact.name.trim());
  if (incompleteContact) { formTab.value = "contacts"; message.error("请补充联系人姓名或删除该行"); return; }
  const incompleteSite = formState.sites.find(site => Boolean(site.name.trim()) !== Boolean(site.address.trim()));
  if (incompleteSite) { formTab.value = "sites"; message.error("项目地址名称和详细地址需要同时填写"); return; }

  saving.value = true;
  try {
    const payload = customerPayload();
    const saved = editingCustomerId.value
      ? await updateCustomer(editingCustomerId.value, payload)
      : await createCustomer({ code: formState.code, ...payload });
    formOpen.value = false;
    message.success(editingCustomerId.value ? "客户档案已更新" : "客户已创建");
    await loadCustomers();
    selectedDetail.value = saved;
    await selectCustomer(saved.id);
  } catch (error) { message.error(error instanceof Error ? error.message : "客户档案保存失败"); }
  finally { saving.value = false; }
}

function customerPayload(): UpdateCustomerPayload {
  return {
    name: formState.name,
    industry: formState.industry,
    level: formState.level,
    ownerName: formState.ownerName,
    paymentHabit: formState.paymentHabit,
    riskStatus: formState.riskStatus,
    riskNote: formState.riskNote,
    invoice: { title: formState.invoiceTitle, taxNo: formState.taxNo, bankName: formState.bankName, bankAccount: formState.bankAccount, registeredAddress: formState.registeredAddress, registeredPhone: formState.registeredPhone },
    contacts: formState.contacts.filter(contact => contact.name.trim()).map(({ key: _key, ...contact }) => contact),
    sites: formState.sites.filter(site => site.name.trim() && site.address.trim()).map(({ key: _key, ...site }) => site),
  };
}

function resetForm(value: CustomerFormState) { Object.assign(formState, value); }
function addContact() { formState.contacts.push(emptyContact()); }
function removeContact(index: number) { formState.contacts.splice(index, 1); if (!formState.contacts.length) formState.contacts.push(emptyContact()); if (!formState.contacts.some(contact => contact.primaryContact)) formState.contacts[0].primaryContact = true; }
function setPrimaryContact(index: number, checked: boolean) { formState.contacts.forEach((contact, contactIndex) => { contact.primaryContact = checked && contactIndex === index; }); }
function addSite() { formState.sites.push(emptySite()); }
function removeSite(index: number) { formState.sites.splice(index, 1); if (!formState.sites.length) formState.sites.push(emptySite()); }

function createFollowUpForCustomer() { if (selectedDetail.value) router.push({ path: "/crm/follow-ups", query: { customer: selectedDetail.value.id, create: "1" } }); }
function handleDrawerVisibility(open: boolean) { if (!open && route.query.customer) { const { customer: _customer, ...query } = route.query; router.replace({ path: route.path, query }); } }
function formatDateTime(value: string) { return new Date(value).toLocaleString("zh-CN", { hour12: false }); }

function initialForm(ownerName = ""): CustomerFormState { return { code: "", name: "", industry: "", level: "NORMAL", ownerName, paymentHabit: "", riskStatus: "NORMAL", riskNote: "", invoiceTitle: "", taxNo: "", bankName: "", bankAccount: "", registeredAddress: "", registeredPhone: "", contacts: [emptyContact(true)], sites: [emptySite()] }; }
function formFromDetail(detail: CustomerDetail): CustomerFormState { return { code: detail.code || "", name: detail.name, industry: detail.industry, level: detail.level, ownerName: detail.ownerName, paymentHabit: detail.paymentHabit || "", riskStatus: detail.riskStatus, riskNote: detail.riskNote || "", invoiceTitle: detail.invoice.title || "", taxNo: detail.invoice.taxNo || "", bankName: detail.invoice.bankName || "", bankAccount: detail.invoice.bankAccount || "", registeredAddress: detail.invoice.registeredAddress || "", registeredPhone: detail.invoice.registeredPhone || "", contacts: detail.contacts.length ? detail.contacts.map(contact => ({ key: uniqueKey(), name: contact.name, title: contact.title || "", phone: contact.phone || "", email: contact.email || "", primaryContact: contact.primaryContact })) : [emptyContact(true)], sites: detail.sites.length ? detail.sites.map(site => ({ key: uniqueKey(), name: site.name, address: site.address })) : [emptySite()] }; }
function emptyContact(primaryContact = false): ContactForm { return { key: uniqueKey(), name: "", title: "", phone: "", email: "", primaryContact }; }
function emptySite(): SiteForm { return { key: uniqueKey(), name: "", address: "" }; }
function uniqueKey() { return `${Date.now()}-${Math.random().toString(16).slice(2)}`; }
</script>

<style scoped>
.customer-directory { container-name: customer-directory; container-type: inline-size; overflow: hidden; min-width: 0; border: 1px solid #dfe5e8; border-radius: 8px; background: #fff; }
.customer-directory-header { display: flex; align-items: center; justify-content: space-between; gap: 20px; min-height: 88px; padding: 18px 20px; border-bottom: 1px solid #e7ebee; }
.customer-title-row { display: flex; align-items: center; gap: 10px; }
.customer-title-row h2 { margin: 0; color: #17212b; font-size: 18px; }
.customer-title-row span, .customer-summary { color: #687582; font-size: 12px; }
.customer-summary { display: flex; gap: 18px; margin-top: 7px; }
.customer-summary strong { color: #263442; font-size: 13px; }
.customer-filter-bar { display: grid; grid-template-columns: minmax(260px, 1fr) 160px 160px auto; gap: 10px; align-items: center; padding: 14px 20px; border-bottom: 1px solid #e7ebee; background: #f7f9fa; }
.customer-filter-bar :deep(.ant-input-prefix) { color: #84909c; }
.customer-filter-result { color: #73808d; font-size: 12px; text-align: right; white-space: nowrap; }
.customer-error { margin: 14px 20px 0; }
.customer-desktop-table { padding: 0 20px 18px; }
.customer-desktop-table :deep(.ant-table-thead > tr > th) { height: 44px; background: #fff; color: #596675; font-size: 12px; font-weight: 600; }
.customer-desktop-table :deep(.ant-table-tbody > tr:hover > td) { background: #f5faf8; }
.customer-name-cell { display: flex; align-items: center; gap: 10px; min-width: 0; }
.customer-avatar { display: grid; flex: 0 0 36px; width: 36px; height: 36px; place-items: center; border-radius: 50%; background: #e5f0ec; color: #176353; font-size: 14px; font-weight: 700; }
.customer-cell-primary { display: block; overflow: hidden; color: #293743; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.customer-payment-habit { display: -webkit-box; overflow: hidden; -webkit-box-orient: vertical; -webkit-line-clamp: 2; color: #4b5966; line-height: 1.5; }
.customer-mobile-list { display: none; }
.customer-drawer-title { display: flex; align-items: center; gap: 10px; min-width: 0; }
.customer-drawer-title > div { display: grid; min-width: 0; }
.customer-drawer-title strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.customer-drawer-title span { color: #71808d; font-size: 12px; font-weight: 400; }
.customer-drawer-title :deep(.ant-tag) { margin-inline-end: 0; }
.customer-metric-band { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); margin-bottom: 18px; border: 1px solid #e3e8eb; border-radius: 6px; background: #f8fafb; }
.customer-metric-band div { min-width: 0; padding: 14px 16px; border-right: 1px solid #e3e8eb; }
.customer-metric-band div:last-child { border-right: 0; }
.customer-metric-band span, .customer-metric-band strong { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.customer-metric-band span { margin-bottom: 5px; color: #75828e; font-size: 12px; }
.customer-metric-band strong { color: #26333f; font-size: 16px; }
.customer-risk-value { color: #b42318 !important; }
.customer-detail-tabs :deep(.ant-tabs-nav) { margin-bottom: 18px; }
.customer-detail-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 22px; }
.customer-detail-section { min-width: 0; }
.customer-detail-section h3 { margin: 0 0 10px; color: #293642; font-size: 14px; }
.customer-record-list { border: 1px solid #e5e9ec; border-radius: 6px; }
.customer-record-row { display: grid; grid-template-columns: minmax(0, 1fr) minmax(180px, auto); gap: 12px; align-items: center; padding: 11px 12px; border-bottom: 1px solid #edf0f2; }
.customer-record-row:last-child { border-bottom: 0; }
.customer-record-row > div { display: flex; flex-wrap: wrap; gap: 7px; align-items: center; min-width: 0; }
.customer-record-row > div:last-child { justify-content: flex-end; color: #687582; font-size: 12px; }
.customer-record-row :deep(.ant-tag) { margin-inline-end: 0; }
.customer-record-row span { color: #687582; font-size: 12px; }
.customer-site-row { grid-template-columns: 150px minmax(0, 1fr); }
.customer-followup-list { display: grid; gap: 10px; }
.customer-followup-item { padding: 14px 16px; border: 1px solid #e3e8eb; border-radius: 6px; background: #fff; }
.customer-followup-item header, .customer-followup-item footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.customer-followup-item header > div { display: flex; align-items: center; gap: 6px; }
.customer-followup-item :deep(.ant-tag) { margin-inline-end: 0; }
.customer-followup-item time, .customer-followup-item footer { color: #74818d; font-size: 12px; }
.customer-followup-item p { margin: 10px 0; color: #384652; line-height: 1.65; }
.customer-form-list-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.customer-form-list-header h3 { margin: 0; font-size: 14px; }
.customer-form-list { display: grid; gap: 10px; }
.customer-contact-form-row, .customer-site-form-row { display: grid; gap: 10px; align-items: end; padding: 12px; border: 1px solid #e3e8eb; border-radius: 6px; background: #f8fafb; }
.customer-contact-form-row { grid-template-columns: 1.1fr 1fr 1fr 1.3fr 88px 32px; }
.customer-site-form-row { grid-template-columns: 180px minmax(0, 1fr) 32px; }
.customer-contact-form-row label, .customer-site-form-row label { display: grid; gap: 6px; color: #667481; font-size: 12px; }
.customer-primary-switch { align-content: end; justify-items: start; }
@container customer-directory (max-width: 820px) {
  .customer-desktop-table { display: none; }
  .customer-mobile-list { display: block; padding: 12px; }
  .customer-mobile-items { display: grid; gap: 10px; }
  .customer-mobile-item { padding: 14px; border: 1px solid #e2e7ea; border-radius: 6px; cursor: pointer; outline: none; }
  .customer-mobile-item:hover, .customer-mobile-item:focus-visible { border-color: #8cbcaf; box-shadow: 0 2px 8px rgba(25, 72, 61, 0.08); }
  .customer-mobile-heading { display: grid; grid-template-columns: 36px minmax(0, 1fr) auto; gap: 10px; align-items: center; }
  .customer-mobile-heading > div { display: grid; min-width: 0; }
  .customer-mobile-heading strong, .customer-mobile-heading span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .customer-mobile-heading span { margin-top: 3px; color: #6b7885; font-size: 12px; }
  .customer-mobile-heading > svg { color: #9aa5af; font-size: 12px; }
  .customer-mobile-tags { display: flex; flex-wrap: wrap; align-items: center; gap: 5px; margin-top: 11px; }
  .customer-mobile-tags :deep(.ant-tag) { margin-inline-end: 0; }
  .customer-mobile-tags > span { color: #697683; font-size: 12px; }
  .customer-mobile-meta { display: flex; flex-wrap: wrap; gap: 14px; margin-top: 11px; padding-top: 10px; border-top: 1px solid #edf0f2; color: #7a8793; font-size: 12px; }
  .customer-mobile-meta strong { color: #35424e; }
  .customer-mobile-payment { overflow: hidden; min-width: 140px; margin-left: auto; text-overflow: ellipsis; white-space: nowrap; }
}
@container customer-directory (max-width: 620px) {
  .customer-directory-header { align-items: flex-start; padding: 15px 14px; }
  .customer-filter-bar { grid-template-columns: repeat(2, minmax(0, 1fr)); padding: 12px 14px; }
  .customer-filter-bar > :first-child { grid-column: 1 / -1; }
  .customer-filter-result { grid-column: 1 / -1; text-align: left; }
}
@media (max-width: 760px) {
  .customer-metric-band { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .customer-metric-band div:nth-child(2) { border-right: 0; }
  .customer-metric-band div:nth-child(-n + 2) { border-bottom: 1px solid #e3e8eb; }
  .customer-detail-grid { grid-template-columns: 1fr; }
  .customer-contact-form-row { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .customer-contact-form-row > button { justify-self: end; }
  .customer-site-form-row { grid-template-columns: 1fr 32px; }
  .customer-site-form-row label:nth-child(2) { grid-column: 1 / -1; grid-row: 2; }
  .customer-site-form-row > button { grid-column: 2; grid-row: 1; }
}
@media (max-width: 480px) {
  .customer-directory-header { flex-direction: column; }
  .customer-directory-header > .ant-space { width: 100%; }
  .customer-directory-header > .ant-space :deep(.ant-space-item:last-child) { flex: 1; }
  .customer-directory-header > .ant-space :deep(.ant-space-item:last-child .ant-btn) { width: 100%; }
  .customer-mobile-payment { width: 100%; margin-left: 0; }
  .customer-record-row, .customer-site-row { grid-template-columns: 1fr; }
  .customer-record-row > div:last-child { justify-content: flex-start; }
  .customer-contact-form-row { grid-template-columns: 1fr; }
  .customer-followup-item header, .customer-followup-item footer { align-items: flex-start; flex-direction: column; }
}
</style>
