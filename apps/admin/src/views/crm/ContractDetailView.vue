<template>
  <div class="page-stack">
    <a-card>
      <template #extra>
        <a-space>
          <a-button @click="goBack">返回列表</a-button>
          <a-button @click="loadData">刷新</a-button>
          <a-button @click="printPage">打印</a-button>
          <a-button type="primary" @click="openEdit">合同变更</a-button>
          <a-button v-if="canApproveContract" type="primary" @click="handleApproveContract">合同审批通过</a-button>
          <a-button v-if="canSubmitSignedApproval" type="primary" @click="handleSubmitSignedApproval">提交盖章件审批</a-button>
          <a-button v-if="canApproveSignedDoc" type="primary" @click="handleApproveSignedDocument">盖章件审批通过</a-button>
          <a-button v-if="canApproveSignedDoc" danger @click="handleRejectSignedDocument">盖章件审批驳回</a-button>
        </a-space>
      </template>

      <div v-if="loading" style="text-align: center; padding: 48px">
        <a-spin size="large" />
      </div>

      <template v-else-if="record">
        <div class="print-area">
        <div class="detail-title">
          <h2>{{ record.code }}</h2>
          <a-tag :color="contractStatusColor(record.status)">{{ contractStatusLabel(record.status) }}</a-tag>
        </div>
        <a-descriptions bordered :column="{ xs: 1, sm: 2, md: 3 }" style="margin-top: 20px">
          <a-descriptions-item label="客户">{{ record.customerName }}</a-descriptions-item>
          <a-descriptions-item label="项目名称">{{ record.projectName }}</a-descriptions-item>
          <a-descriptions-item label="合同类型">{{ record.contractType }}</a-descriptions-item>
          <a-descriptions-item label="合同金额">
            <strong>{{ formatMoney(record.amount) }}</strong>
          </a-descriptions-item>
          <a-descriptions-item label="已开票金额">
            <strong>{{ formatMoney(invoicedTotal) }}</strong>
          </a-descriptions-item>
          <a-descriptions-item label="已回款金额">
            <strong>{{ formatMoney(receivedTotal) }}</strong>
          </a-descriptions-item>
          <a-descriptions-item label="未税金额">
            <strong>{{ formatMoney(record.netAmount ?? calcNetAmount(record.amount, record.taxRate)) }}</strong>
          </a-descriptions-item>
          <a-descriptions-item label="税率">{{ formatTaxRate(record.taxRate) }}</a-descriptions-item>
          <a-descriptions-item label="服务频次">{{ record.serviceCycle || "未设置" }}</a-descriptions-item>
          <a-descriptions-item label="关联报价">{{ record.quoteId || "未关联" }}</a-descriptions-item>
        </a-descriptions>

        <a-card title="合同周期" style="margin-top: 16px">
          <a-row :gutter="16">
            <a-col :xs="12">
              <a-statistic title="开始日期" :value="record.startDate" />
            </a-col>
            <a-col :xs="12">
              <a-statistic title="结束日期" :value="record.endDate" />
            </a-col>
          </a-row>
        </a-card>

        <a-card title="审批进展" style="margin-top: 16px">
          <ApprovalProgressFlow :steps="contractApprovalSteps(record)" />
        </a-card>

        <a-card title="合同闭环工作台" class="closure-card" style="margin-top: 16px">
          <template #extra>
            <a-space>
              <a-tag :color="closureRisk.color">{{ closureRisk.label }}</a-tag>
              <a-button size="small" @click="loadClosureData">刷新闭环</a-button>
            </a-space>
          </template>

          <a-row :gutter="[12, 12]" class="closure-stats">
            <a-col v-for="item in closureStats" :key="item.key" :xs="12" :lg="6">
              <div class="closure-stat">
                <span>{{ item.label }}</span>
                <strong :class="{ danger: item.danger }">{{ item.value }}</strong>
                <p>{{ item.hint }}</p>
              </div>
            </a-col>
          </a-row>

          <div class="closure-flow">
            <button
              v-for="item in closureSteps"
              :key="item.key"
              type="button"
              class="closure-step"
              :class="{ done: item.done, warn: item.warn }"
              @click="item.route && router.push(item.route)"
            >
              <span>{{ item.label }}</span>
              <strong>{{ item.title }}</strong>
              <p>{{ item.hint }}</p>
            </button>
          </div>

          <a-list class="closure-actions" size="small" :data-source="closureActions">
            <template #header>
              <strong>建议动作</strong>
            </template>
            <template #renderItem="{ item }">
              <a-list-item>
                <template #actions>
                  <a-button v-if="item.route" size="small" type="link" @click="router.push(item.route)">处理</a-button>
                </template>
                <a-list-item-meta>
                  <template #title>
                    <a-space>
                      <a-tag :color="item.level === 'high' ? 'red' : item.level === 'medium' ? 'orange' : 'blue'">
                        {{ item.level === "high" ? "高优先" : item.level === "medium" ? "关注" : "建议" }}
                      </a-tag>
                      <span>{{ item.title }}</span>
                    </a-space>
                  </template>
                  <template #description>{{ item.description }}</template>
                </a-list-item-meta>
              </a-list-item>
            </template>
            <template #empty>
              <a-empty description="当前合同闭环完整，无待处理建议" />
            </template>
          </a-list>
        </a-card>

        <a-alert
          v-if="record.status === 'ACTIVE'"
          class="section-alert"
          type="success"
          show-icon
          message="合同已生效，项目已进入自动承接流程"
          description="双方盖章件审批通过后，系统会自动创建项目并带入客户、合同金额、计划周期和报价成本预算。"
        />
        <a-alert
          v-else
          class="section-alert"
          type="warning"
          show-icon
          :message="record.status === 'PENDING_APPROVAL' ? '合同待审批' : record.status === 'PENDING_SEAL' ? '合同已审批，待上传双方盖章件' : record.status === 'SEAL_APPROVAL' ? '双方盖章件审批中' : '合同未生效'"
          description="项目会在双方盖章件审批通过后自动生成，未完成盖章审批前不会进入项目管理执行。"
        >
          <template #action>
            <a-space>
              <a-button v-if="canApproveContract" type="primary" size="small" @click="handleApproveContract">
                合同审批通过
              </a-button>
              <a-upload
                v-if="canUploadSignedDoc"
                :before-upload="(f: File) => handleUpload('SIGNED_DOC', f)"
                :show-upload-list="false"
                accept=".jpg,.jpeg,.png,.pdf"
              >
                <a-button type="primary" size="small">上传双方盖章件</a-button>
              </a-upload>
              <a-button v-if="record.status === 'SEAL_APPROVAL'" size="small" @click="contractTabKey = 'signed'">
                查看盖章件
              </a-button>
              <a-button v-if="canApproveSignedDoc" type="primary" size="small" @click="handleApproveSignedDocument">
                盖章件审批通过
              </a-button>
              <a-button v-if="canApproveSignedDoc" danger size="small" @click="handleRejectSignedDocument">
                驳回
              </a-button>
            </a-space>
          </template>
        </a-alert>

        <a-card v-if="relatedQuote" title="关联报价" style="margin-top: 16px">
          <a-descriptions bordered :column="{ xs: 1, sm: 2, md: 3 }">
            <a-descriptions-item label="报价编号">{{ relatedQuote.code }}</a-descriptions-item>
            <a-descriptions-item label="版本">V{{ relatedQuote.versionNo }}</a-descriptions-item>
            <a-descriptions-item label="报价金额">
              <strong>{{ formatMoney(relatedQuote.amount) }}</strong>
            </a-descriptions-item>
            <a-descriptions-item label="未税金额">
              <strong>{{ formatMoney(relatedQuote.netAmount ?? calcNetAmount(relatedQuote.amount, relatedQuote.taxRate)) }}</strong>
            </a-descriptions-item>
            <a-descriptions-item label="服务范围" :span="3">{{ relatedQuote.serviceScope }}</a-descriptions-item>
            <a-descriptions-item label="服务频次">{{ relatedQuote.inspectCycle || "未设置" }}</a-descriptions-item>
            <a-descriptions-item label="付款方式/节点">{{ relatedQuote.paymentNodes || "未设置" }}</a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-tag :color="quoteStatusColor(relatedQuote.status)">{{ quoteStatusLabel(relatedQuote.status) }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item v-if="relatedQuote.lastApproverName" label="审批人">{{ relatedQuote.lastApproverName }}</a-descriptions-item>
            <a-descriptions-item v-if="relatedQuote.lastApprovalComment" label="审批意见" :span="2">{{ relatedQuote.lastApprovalComment }}</a-descriptions-item>
          </a-descriptions>
        </a-card>

        <a-card v-if="relatedOpportunity" title="关联商机" style="margin-top: 16px">
          <a-descriptions bordered :column="{ xs: 1, sm: 2, md: 3 }">
            <a-descriptions-item label="商机编号">{{ relatedOpportunity.code }}</a-descriptions-item>
            <a-descriptions-item label="阶段">
              <a-tag :color="opportunityStageColor(relatedOpportunity.stage)">{{ opportunityStageLabel(relatedOpportunity.stage) }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="客户需求" :span="3">{{ relatedOpportunity.needSummary }}</a-descriptions-item>
            <a-descriptions-item label="预计金额">{{ formatMoney(relatedOpportunity.expectedAmount) }}</a-descriptions-item>
            <a-descriptions-item label="成功率">{{ relatedOpportunity.probability }}%</a-descriptions-item>
            <a-descriptions-item label="负责人">{{ relatedOpportunity.ownerName }}</a-descriptions-item>
          </a-descriptions>
        </a-card>

        <BusinessTraceTimeline :contract="record" />
        </div><!-- end print-area -->
      </template>

      <a-empty v-else description="未找到合同" />
    </a-card>

    <a-tabs v-model:activeKey="contractTabKey" style="margin-top: 16px">
      <a-tab-pane key="approval" tab="审批件">
        <template #extra>
          <a-upload :before-upload="(f: File) => handleUpload('APPROVAL_DOC', f)" :show-upload-list="false" accept=".jpg,.jpeg,.png,.pdf,.doc,.docx">
            <a-button size="small" type="primary">上传审批件</a-button>
          </a-upload>
        </template>
        <a-empty v-if="approvalAttachments.length === 0" description="暂无审批件" />
        <a-list v-else :data-source="approvalAttachments" size="small">
          <template #renderItem="{ item }">
            <a-list-item>
              <a-button type="link" size="small" @click="handleOpenAttachment(item)">{{ item.fileName }}</a-button>
              <span>{{ (item.fileSize / 1024).toFixed(1) + " KB" }}</span>
              <a-button type="link" size="small" @click="handleDownloadAttachment(item)">下载</a-button>
              <a-button type="link" danger size="small" @click="handleDeleteAttachment(item.id)">删除</a-button>
            </a-list-item>
          </template>
        </a-list>
      </a-tab-pane>
      <a-tab-pane key="signed" tab="双方盖章件">
        <template #extra>
          <a-upload v-if="canUploadSignedDoc" :before-upload="(f: File) => handleUpload('SIGNED_DOC', f)" :show-upload-list="false" accept=".jpg,.jpeg,.png,.pdf">
            <a-button size="small" type="primary">上传双方盖章件</a-button>
          </a-upload>
          <a-tag v-else color="default">{{ record?.status === "PENDING_APPROVAL" ? "合同审批通过后开放" : "当前状态不可上传" }}</a-tag>
        </template>
        <a-empty v-if="signedAttachments.length === 0" description="暂无盖章件" />
        <a-list v-else :data-source="signedAttachments" size="small">
          <template #renderItem="{ item }">
            <a-list-item>
              <a-button type="link" size="small" @click="handleOpenAttachment(item)">{{ item.fileName }}</a-button>
              <span>{{ (item.fileSize / 1024).toFixed(1) + " KB" }}</span>
              <a-button type="link" size="small" @click="handleDownloadAttachment(item)">下载</a-button>
              <a-button type="link" danger size="small" @click="handleDeleteAttachment(item.id)">删除</a-button>
            </a-list-item>
          </template>
        </a-list>
      </a-tab-pane>
      <a-tab-pane key="changes" tab="变更记录">
        <a-empty v-if="changeList.length === 0" description="暂无变更记录" />
        <div v-else class="change-timeline">
          <div v-for="item in changeList" :key="item.id" class="change-card" :class="'change-' + (item.status?.toLowerCase() || 'pending')">
            <div class="change-header">
              <strong>{{ item.reason }}</strong>
              <a-tag :color="item.status === 'APPROVED' ? 'green' : item.status === 'REJECTED' ? 'red' : 'orange'">
                {{ item.status === 'APPROVED' ? '已通过' : item.status === 'REJECTED' ? '已驳回' : '待审批' }}
              </a-tag>
            </div>
            <ApprovalProgressFlow :steps="contractChangeApprovalSteps(item)" />
            <div v-if="item.status === 'PENDING'" style="margin-top:8px">
              <a-space>
                <a-button size="small" type="primary" @click="handleApproveChange(item.id)">通过</a-button>
                <a-button size="small" danger @click="handleRejectChange(item.id)">驳回</a-button>
              </a-space>
            </div>
          </div>
        </div>
      </a-tab-pane>
    </a-tabs>
    <a-modal v-model:open="editOpen" title="合同变更" :confirm-loading="saving" @ok="handleEdit">
      <a-form ref="editFormRef" :model="editForm" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="12"><a-form-item label="项目名称" name="projectName"><a-input v-model:value="editForm.projectName" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="合同类型" name="contractType"><a-input v-model:value="editForm.contractType" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="合同金额" name="amount"><a-input-number v-model:value="editForm.amount" :min="0" class="full-input" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="税率(%)"><a-input-number v-model:value="editForm.taxRate" :min="0" :max="100" :precision="2" class="full-input" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="服务频次"><a-input v-model:value="editForm.serviceCycle" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="开始日期"><a-input v-model:value="editForm.startDate" type="date" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="结束日期"><a-input v-model:value="editForm.endDate" type="date" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="变更原因" name="reason"><a-textarea v-model:value="editForm.reason" :rows="2" placeholder="请说明变更原因" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";
const auth = useAuthStore();
import { listProjects, type Project } from "@/api/project";
import { getContract, getOpportunity, getQuote, uploadAttachment, deleteAttachment, listAttachments, openAttachment, downloadAttachment, createContractChange, approveContractChange, rejectContractChange, listContractChanges, listReceivables, approveContract, submitSignedDocumentApproval, type Opportunity, type QuotePlan, type ServiceContract, type CrmAttachment, type ContractChangeResponse, type Receivable } from "@/api/crm";
import { contractStatusColor, contractStatusLabel, formatMoney, opportunityStageColor, opportunityStageLabel, quoteStatusColor, quoteStatusLabel } from "./crm-options";
import BusinessTraceTimeline from "@/components/business/BusinessTraceTimeline.vue";
import ApprovalProgressFlow, { type ApprovalProgressStep } from "@/components/ApprovalProgressFlow.vue";

const route = useRoute();
const router = useRouter();
const record = ref<ServiceContract | null>(null);
const relatedQuote = ref<QuotePlan | null>(null);
const relatedOpportunity = ref<Opportunity | null>(null);
const relatedProject = ref<Project | null>(null);
const contractReceivables = ref<Receivable[]>([]);
const loading = ref(true);
const saving = ref(false);
const editOpen = ref(false);
const contractTabKey = ref(route.query.tab === "signed" ? "signed" : "approval");
const editFormRef = ref();
const editForm = reactive({ projectName: "", contractType: "", amount: 0, taxRate: 13, serviceCycle: "", startDate: "", endDate: "", reason: "" });
const changeColumns = [
  { title: "变更原因", key: "reason", width: 220 },
  { title: "状态", key: "status", width: 80 },
  { title: "申请人", key: "requested", width: 140 },
  { title: "处理", key: "action", width: 160 },
];

const editRules = { projectName: [{ required: true, message: "请输入项目名称" }], amount: [{ required: true, message: "请输入合同金额" }], };
const id = route.params.id as string;

onMounted(loadData);
onMounted(loadAttachments);
onMounted(loadChanges);

async function loadData() {
  loading.value = true;
  try {
    record.value = await getContract(id);
    // Load related quote
    if (record.value?.quoteId) {
      try {
        relatedQuote.value = await getQuote(record.value.quoteId);
        // Load related opportunity from quote
        if (relatedQuote.value?.opportunityId) {
          try {
            relatedOpportunity.value = await getOpportunity(relatedQuote.value.opportunityId);
          } catch { /* opportunity fetch is supplementary */ }
        }
      } catch { /* quote fetch is supplementary */ }
    }
    await loadClosureData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "合同加载失败");
  } finally {
    loading.value = false;
  }
}

async function loadClosureData() {
  if (!record.value) return;
  const current = record.value;
  const [projectsResult, receivablesResult] = await Promise.allSettled([
    listProjects(0, 500),
    listReceivables(),
  ]);
  if (projectsResult.status === "fulfilled") {
    relatedProject.value = projectsResult.value.content.find((item) => item.contractId === current.id || item.contractCode === current.code) || null;
  }
  if (receivablesResult.status === "fulfilled") {
    contractReceivables.value = receivablesResult.value.filter((item) =>
      item.contractId === current.id || item.contractCode === current.code || item.sourceNo === current.code
    );
  }
}

const receivableTotal = computed(() => contractReceivables.value.reduce((sum, item) => sum + Number(item.amount || 0), 0));
const invoicedTotal = computed(() => contractReceivables.value.reduce((sum, item) => sum + (item.invoiceNo ? Number(item.amount || 0) : 0), 0));
const receivedTotal = computed(() => contractReceivables.value.reduce((sum, item) => sum + Number(item.settledAmount || 0), 0));
const outstandingTotal = computed(() => contractReceivables.value.reduce((sum, item) => sum + Number(item.outstandingAmount || 0), 0));
const overdueReceivables = computed(() => contractReceivables.value.filter((item) => item.status === "OVERDUE"));
const collectionRate = computed(() => receivableTotal.value > 0 ? Math.round(receivedTotal.value / receivableTotal.value * 1000) / 10 : 0);
const contractProgress = computed(() => {
  if (!record.value?.startDate || !record.value.endDate) return 0;
  const start = new Date(record.value.startDate).getTime();
  const end = new Date(record.value.endDate).getTime();
  const now = Date.now();
  if (!Number.isFinite(start) || !Number.isFinite(end) || end <= start) return 0;
  return Math.max(0, Math.min(100, Math.round((now - start) / (end - start) * 100)));
});
const daysToEnd = computed(() => {
  if (!record.value?.endDate) return 0;
  return Math.ceil((new Date(record.value.endDate).getTime() - Date.now()) / 86400000);
});
const closureRisk = computed(() => {
  if (overdueReceivables.value.length > 0 || outstandingTotal.value > Number(record.value?.amount || 0) * 0.3) return { label: "高风险", color: "red" };
  if (!relatedProject.value || contractReceivables.value.length === 0 || daysToEnd.value <= 30) return { label: "需关注", color: "orange" };
  return { label: "闭环正常", color: "green" };
});
const closureStats = computed(() => [
  { key: "progress", label: "合同进度", value: `${contractProgress.value}%`, hint: daysToEnd.value >= 0 ? `距到期 ${daysToEnd.value} 天` : `已到期 ${Math.abs(daysToEnd.value)} 天`, danger: daysToEnd.value <= 30 },
  { key: "project", label: "项目承接", value: relatedProject.value ? "已承接" : "未承接", hint: relatedProject.value?.code || "合同生效后建议生成项目", danger: !relatedProject.value && record.value?.status === "ACTIVE" },
  { key: "collection", label: "回款率", value: `${collectionRate.value}%`, hint: `${formatMoney(receivedTotal.value)} / ${formatMoney(receivableTotal.value)}`, danger: overdueReceivables.value.length > 0 },
  { key: "outstanding", label: "未回款", value: formatMoney(outstandingTotal.value), hint: overdueReceivables.value.length ? `${overdueReceivables.value.length} 笔逾期` : "应收状态正常", danger: outstandingTotal.value > 0 },
]);
const closureSteps = computed(() => [
  { key: "customer", label: "客户", title: record.value?.customerName || "-", hint: "客户档案与开票信息", done: Boolean(record.value?.customerId), warn: false, route: record.value?.customerId ? `/crm/customers?customer=${record.value.customerId}` : "" },
  { key: "opportunity", label: "商机", title: relatedOpportunity.value?.code || "未关联", hint: relatedOpportunity.value?.stage || "建议从商机穿透来源", done: Boolean(relatedOpportunity.value), warn: !relatedOpportunity.value, route: relatedOpportunity.value ? `/crm/opportunities/${relatedOpportunity.value.id}` : "/crm/opportunities" },
  { key: "quote", label: "报价", title: relatedQuote.value?.code || "未关联", hint: relatedQuote.value ? `V${relatedQuote.value.versionNo}` : "报价利润校验不可追溯", done: Boolean(relatedQuote.value), warn: !relatedQuote.value, route: relatedQuote.value ? `/crm/quotes/${relatedQuote.value.id}` : "/crm/quotes" },
  { key: "contract", label: "合同", title: record.value?.code || "-", hint: contractStatusLabel(record.value?.status || "ACTIVE"), done: Boolean(record.value), warn: false, route: "" },
  { key: "project", label: "项目", title: relatedProject.value?.code || "未承接", hint: relatedProject.value?.stage || "项目预算、采购、成本入口", done: Boolean(relatedProject.value), warn: !relatedProject.value && record.value?.status === "ACTIVE", route: relatedProject.value ? "/projects/list" : "" },
  { key: "receivable", label: "应收回款", title: `${contractReceivables.value.length} 笔`, hint: `${collectionRate.value}% 已回款`, done: contractReceivables.value.length > 0, warn: contractReceivables.value.length === 0 || overdueReceivables.value.length > 0, route: "/finance/receivables" },
]);
const closureActions = computed(() => {
  const rows: Array<{ key: string; level: "high" | "medium" | "low"; title: string; description: string; route?: string }> = [];
  if (record.value?.status === "ACTIVE" && !relatedProject.value) {
    rows.push({ key: "project", level: "high", title: "合同已生效但未承接项目", description: "建议立即生成项目草稿，带入合同额、周期和默认预算，避免采购和成本无法归集。" });
  }
  if (contractReceivables.value.length === 0) {
    rows.push({ key: "receivable", level: "high", title: "缺少应收计划", description: "合同没有应收节点，财务无法跟踪开票、到期、回款和逾期。", route: "/crm/receivables" });
  }
  if (overdueReceivables.value.length > 0) {
    rows.push({ key: "overdue", level: "high", title: "存在逾期应收", description: `当前 ${overdueReceivables.value.length} 笔逾期，未回款 ${formatMoney(outstandingTotal.value)}，建议生成催收跟进。`, route: "/finance/receivables" });
  }
  if (!relatedQuote.value) {
    rows.push({ key: "quote", level: "medium", title: "报价链路缺失", description: "缺少报价会影响毛利、折扣权限、成本项校验的追溯。", route: "/crm/quotes" });
  }
  if (daysToEnd.value <= 30) {
    rows.push({ key: "renewal", level: "medium", title: "合同临近到期", description: daysToEnd.value >= 0 ? `距到期 ${daysToEnd.value} 天，建议提前进入续约评估。` : `已到期 ${Math.abs(daysToEnd.value)} 天，建议关闭或续约。`, route: "/crm/renewals" });
  }
  return rows;
});

const attachments = ref<CrmAttachment[]>([]);
const approvalAttachments = computed(() => attachments.value.filter((a) => a.attachmentType === "APPROVAL_DOC" || !a.attachmentType));
const signedAttachments = computed(() => attachments.value.filter((a) => a.attachmentType === "SIGNED_DOC"));
const pendingSignedDocApproval = computed(() => changeList.value.find((item) =>
  item.status === "PENDING" && (item.changeData || "").includes("SIGNED_DOC_APPROVAL")
) || null);
const canApproveContract = computed(() => record.value?.status === "PENDING_APPROVAL" && approvalAttachments.value.length > 0 && auth.can("crm:contract:update"));
const canUploadSignedDoc = computed(() => record.value?.status === "PENDING_SEAL" && auth.can("crm:contract:update"));
const canSubmitSignedApproval = computed(() => record.value?.status === "PENDING_SEAL" && signedAttachments.value.length > 0 && auth.can("crm:contract:update"));
const canApproveSignedDoc = computed(() => record.value?.status === "SEAL_APPROVAL" && Boolean(pendingSignedDocApproval.value) && auth.can("crm:contract:update"));

function contractApprovalSteps(item: ServiceContract): ApprovalProgressStep[] {
  const contractApproved = ["PENDING_SEAL", "SEAL_APPROVAL", "ACTIVE", "RENEWAL_PENDING", "OVERDUE_RISK", "CLOSED"].includes(item.status);
  const sealRejected = pendingSignedDocApproval.value?.status === "REJECTED";
  return [
    { key: "start", personName: item.salesOwnerName || item.customerName || "发起人", title: "发起合同", note: `${item.contractType || "-"} · ${formatMoney(item.amount)}`, state: "done" },
    {
      key: "contract",
      personName: contractApproved ? "合同审批人" : "当前审批人",
      title: item.status === "PENDING_APPROVAL" ? "待审批" : "已同意",
      note: item.status === "PENDING_APPROVAL" ? "等待合同审批" : "合同已进入盖章阶段",
      state: item.status === "PENDING_APPROVAL" ? "pending" : "done",
    },
    {
      key: "seal",
      personName: pendingSignedDocApproval.value?.approvedBy || pendingSignedDocApproval.value?.requestedBy || (item.status === "ACTIVE" ? "盖章审批人" : "当前审批人"),
      title: item.status === "SEAL_APPROVAL" ? "待审批" : sealRejected ? "已驳回" : item.status === "ACTIVE" ? "已同意" : "待提交",
      time: pendingSignedDocApproval.value?.approvedAt || pendingSignedDocApproval.value?.createdAt,
      note: item.status === "SEAL_APPROVAL" ? "等待盖章件审批" : pendingSignedDocApproval.value?.approvalComment || (item.status === "ACTIVE" ? "双方盖章件已审批" : "上传盖章件后审批"),
      state: item.status === "SEAL_APPROVAL" ? "pending" : sealRejected ? "rejected" : item.status === "ACTIVE" ? "done" : "waiting",
    },
    { key: "project", personName: relatedProject.value?.code || "项目承接", title: item.status === "ACTIVE" ? "已承接" : "待承接", note: relatedProject.value?.name || "盖章通过后自动承接项目", state: item.status === "ACTIVE" ? "done" : "waiting" },
  ];
}

function contractChangeApprovalSteps(item: ContractChangeResponse): ApprovalProgressStep[] {
  return [
    { key: "start", personName: item.requestedBy || "发起人", title: "发起申请", time: item.requestedAt || item.createdAt, note: item.reason, state: "done" },
    {
      key: "approval",
      personName: item.approvedBy || "当前审批人",
      title: item.status === "PENDING" ? "待审批" : item.status === "REJECTED" ? "已驳回" : "已同意",
      time: item.approvedAt,
      note: item.status === "PENDING" ? "等待合同变更审批" : item.approvalComment || item.reason,
      state: item.status === "PENDING" ? "pending" : item.status === "REJECTED" ? "rejected" : "done",
    },
  ];
}

async function loadAttachments() {
  try {
    attachments.value = await listAttachments("CONTRACT", id);
  } catch {}
}

async function handleUpload(attachmentType: string | undefined, file: File) {
  try {
    await uploadAttachment("CONTRACT", id, attachmentType, file);
    message.success("附件上传成功");
    await loadAttachments();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "上传失败");
  }
  return false;
}

async function handleDeleteAttachment(attId: string) {
  try {
    await deleteAttachment(attId);
    message.success("附件已删除");
    await loadAttachments();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "删除失败");
  }
}

async function handleOpenAttachment(item: CrmAttachment) {
  try {
    await openAttachment(item);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "附件打开失败");
  }
}

async function handleDownloadAttachment(item: CrmAttachment) {
  try {
    await downloadAttachment(item);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "附件下载失败");
  }
}

async function handleApproveContract() {
  try {
    await approveContract(id, { operatorName: auth.user?.displayName || "", comment: "" });
    message.success("合同审批已通过，请上传双方盖章件");
    await Promise.all([loadData(), loadAttachments(), loadChanges()]);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "合同审批失败");
  }
}

async function handleSubmitSignedApproval() {
  try {
    await submitSignedDocumentApproval(id, { operatorName: auth.user?.displayName || "", comment: "双方盖章件审批" });
    message.success("双方盖章件已提交审批");
    await Promise.all([loadData(), loadChanges()]);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "提交盖章件审批失败");
  }
}

async function handleApproveSignedDocument() {
  if (!pendingSignedDocApproval.value) return;
  try {
    await approveContractChange(pendingSignedDocApproval.value.id, { operatorName: auth.user?.displayName || "", comment: "双方盖章件审批通过" });
    message.success("盖章件审批已通过，合同已生效并自动转项目");
    await Promise.all([loadData(), loadAttachments(), loadChanges()]);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "盖章件审批失败");
  }
}

async function handleRejectSignedDocument() {
  if (!pendingSignedDocApproval.value) return;
  try {
    await rejectContractChange(pendingSignedDocApproval.value.id, { operatorName: auth.user?.displayName || "", comment: "双方盖章件审批驳回" });
    message.success("盖章件审批已驳回，请重新上传或提交");
    await Promise.all([loadData(), loadAttachments(), loadChanges()]);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "盖章件驳回失败");
  }
}

function openEdit() {
  if (!record.value) return;
  Object.assign(editForm, { projectName: record.value.projectName, contractType: record.value.contractType, amount: record.value.amount, taxRate: record.value.taxRate ?? 13, serviceCycle: record.value.serviceCycle || "", startDate: record.value.startDate || "", endDate: record.value.endDate || "" });
  editOpen.value = true;
}

async function handleEdit() {
  await editFormRef.value?.validate();
  if (!record.value) return;
  if (!editForm.reason.trim()) { message.error("请填写变更原因"); return; }
  saving.value = true;
  try {
    const payload = {
      changeData: JSON.stringify({ projectName: editForm.projectName, contractType: editForm.contractType, amount: editForm.amount, taxRate: editForm.taxRate, serviceCycle: editForm.serviceCycle, startDate: editForm.startDate, endDate: editForm.endDate }),
      reason: editForm.reason,
      requestedBy: auth.user?.displayName || "",
    };
    await createContractChange(id, payload);
    editOpen.value = false;
    message.success("变更请求已提交，等待审批");
    await loadChanges();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "提交失败");
  } finally {
    saving.value = false;
  }
}

const changeList = ref<ContractChangeResponse[]>([]);
const changesLoading = ref(false);

async function loadChanges() {
  changesLoading.value = true;
  try { changeList.value = await listContractChanges(id); }
  catch {}
  finally { changesLoading.value = false; }
}

async function handleApproveChange(changeId: string) {
  try {
    await approveContractChange(changeId, { operatorName: auth.user?.displayName || "", comment: "" });
    message.success("变更已审批通过，合同已更新");
    await loadChanges();
    await loadData();
  } catch (error) { message.error(error instanceof Error ? error.message : "审批失败"); }
}

async function handleRejectChange(changeId: string) {
  try {
    await rejectContractChange(changeId, { operatorName: auth.user?.displayName || "", comment: "" });
    message.success("变更已驳回");
    await loadChanges();
  } catch (error) { message.error(error instanceof Error ? error.message : "驳回失败"); }
}

function printPage() { window.print(); }

function goBack() {
  router.push("/crm/contracts");
}

function formatTaxRate(value?: number) {
  return `${Number(value ?? 13).toFixed(2).replace(/\.?0+$/, "")}%`;
}

function calcNetAmount(amount?: number, taxRate?: number) {
  const rate = Number(taxRate ?? 13);
  const divisor = 1 + rate / 100;
  return divisor > 0 ? Number(amount || 0) / divisor : Number(amount || 0);
}
</script>
<style scoped>
.closure-card :deep(.ant-card-body) {
  padding-top: 14px;
}

.closure-stats {
  margin-bottom: 14px;
}

.closure-stat {
  min-height: 104px;
  padding: 12px;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  background: #fbfcfe;
}

.closure-stat span {
  color: #667085;
  font-size: 12px;
}

.closure-stat strong {
  display: block;
  overflow: hidden;
  margin: 8px 0 6px;
  color: #172033;
  font-size: 22px;
  line-height: 1.1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.closure-stat strong.danger {
  color: #cf1322;
}

.closure-stat p {
  overflow: hidden;
  margin: 0;
  color: #7a8699;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.closure-flow {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.closure-step {
  min-height: 112px;
  padding: 12px;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
  background: #f8fafc;
  cursor: pointer;
  text-align: left;
}

.closure-step.done {
  border-color: #b7d7ff;
  background: #f4f9ff;
}

.closure-step.warn {
  border-color: #ffd6d6;
  background: #fff7f7;
}

.closure-step span {
  color: #667085;
  font-size: 12px;
}

.closure-step strong {
  display: block;
  overflow: hidden;
  margin-top: 8px;
  color: #172033;
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.closure-step p {
  display: -webkit-box;
  overflow: hidden;
  margin: 6px 0 0;
  color: #7a8699;
  font-size: 12px;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.closure-actions {
  border-top: 1px solid #edf0f5;
  padding-top: 4px;
}

@media (max-width: 1200px) {
  .closure-flow {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .closure-flow {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>

<style>
@media print {
  .app-sider, .ant-layout-sider { display: none !important; }
  .app-header, .ant-layout-header { display: none !important; }
  .ant-card-extra { display: none !important; }
  .ant-card-actions { display: none !important; }
  .ant-btn { display: none !important; }
  button { display: none !important; }
  .app-content, .ant-layout-content { 
    margin-left: 0 !important;
    padding: 20px !important;
    width: 100% !important;
  }
  .page-stack { width: 100% !important; }
  .ant-card { box-shadow: none !important; border: 1px solid #ddd !important; break-inside: avoid; }
  .ant-card-body { padding: 20px !important; }
  table { font-size: 11pt !important; }
  .ant-descriptions-item-label { font-size: 10pt !important; }
  .ant-descriptions-item-content { font-size: 10pt !important; }
  body { background: white !important; }
}
</style>
