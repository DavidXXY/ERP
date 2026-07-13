<template>
  <div class="page-stack">
    <a-card>
      <template #extra>
        <a-space>
          <a-button @click="goBack">返回列表</a-button>
          <a-button @click="loadData">刷新</a-button>
          <a-button @click="printPage">打印</a-button>
          <a-button type="primary" @click="openEdit">合同变更</a-button>
          <a-button v-if="record && auth.can('project:create')" type="primary" @click="convertToProject">转项目</a-button>
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

        <a-alert
          v-if="record.status === 'ACTIVE'"
          class="section-alert"
          type="success"
          show-icon
          message="合同已生效，可承接为项目"
          description="转项目后会带入客户、合同金额、计划周期，并按默认成本结构拆分预算，后续采购、领料和成本会进入项目利润闭环。"
        />
        <a-alert
          v-else
          class="section-alert"
          type="warning"
          show-icon
          message="建议合同生效后再转项目"
          description="未生效合同仍可创建项目草稿，但项目审批和预算执行应以最终合同为准。"
        />

        <a-card v-if="relatedQuote" title="关联报价" style="margin-top: 16px">
          <a-descriptions bordered :column="{ xs: 1, sm: 2, md: 3 }">
            <a-descriptions-item label="报价编号">{{ relatedQuote.code }}</a-descriptions-item>
            <a-descriptions-item label="版本">V{{ relatedQuote.versionNo }}</a-descriptions-item>
            <a-descriptions-item label="报价金额">
              <strong>{{ formatMoney(relatedQuote.amount) }}</strong>
            </a-descriptions-item>
            <a-descriptions-item label="服务范围" :span="3">{{ relatedQuote.serviceScope }}</a-descriptions-item>
            <a-descriptions-item label="服务频次">{{ relatedQuote.inspectCycle || "未设置" }}</a-descriptions-item>
            <a-descriptions-item label="付款节点">{{ relatedQuote.paymentNodes || "未设置" }}</a-descriptions-item>
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

    <a-tabs default-active-key="approval" style="margin-top: 16px">
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
              <a :href="getAttachmentDownloadUrl(item.id)" target="_blank">{{ item.fileName }}</a>
              <span>{{ (item.fileSize / 1024).toFixed(1) + " KB" }}</span>
              <a-button type="link" danger size="small" @click="handleDeleteAttachment(item.id)">删除</a-button>
            </a-list-item>
          </template>
        </a-list>
      </a-tab-pane>
      <a-tab-pane key="signed" tab="双方盖章件">
        <template #extra>
          <a-upload :before-upload="(f: File) => handleUpload('SIGNED_DOC', f)" :show-upload-list="false" accept=".jpg,.jpeg,.png,.pdf">
            <a-button size="small" type="primary">上传盖章件</a-button>
          </a-upload>
        </template>
        <a-empty v-if="signedAttachments.length === 0" description="暂无盖章件" />
        <a-list v-else :data-source="signedAttachments" size="small">
          <template #renderItem="{ item }">
            <a-list-item>
              <a :href="getAttachmentDownloadUrl(item.id)" target="_blank">{{ item.fileName }}</a>
              <span>{{ (item.fileSize / 1024).toFixed(1) + " KB" }}</span>
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
            <a-timeline>
              <a-timeline-item color="blue">
                <template #label>{{ item.requestedAt?.slice(0,10) || "-" }}</template>
                <strong>提交变更申请</strong>
                <p style="margin:2px 0 0;color:#8c8c8c;font-size:12px">申请人：{{ item.requestedBy || "-" }}</p>
              </a-timeline-item>
              <a-timeline-item :color="item.status === 'APPROVED' ? 'green' : item.status === 'REJECTED' ? 'red' : 'orange'">
                <template #label>{{ item.approvedAt?.slice(0,10) || "待处理" }}</template>
                <strong v-if="item.status === 'PENDING'">等待审批</strong>
                <strong v-else>{{ item.status === 'APPROVED' ? '审批通过' : '审批驳回' }}</strong>
                <p v-if="item.approvedBy" style="margin:2px 0 0;color:#8c8c8c;font-size:12px">审批人：{{ item.approvedBy }}</p>
                <div v-if="item.status === 'PENDING'" style="margin-top:8px">
                  <a-space>
                    <a-button size="small" type="primary" @click="handleApproveChange(item.id)">通过</a-button>
                    <a-button size="small" danger @click="handleRejectChange(item.id)">驳回</a-button>
                  </a-space>
                </div>
              </a-timeline-item>
            </a-timeline>
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
import { Modal, message } from "ant-design-vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";
const auth = useAuthStore();
import { createProject } from "@/api/project";
import { getContract, getOpportunity, getQuote, uploadAttachment, deleteAttachment, listAttachments, getAttachmentDownloadUrl, createContractChange, approveContractChange, rejectContractChange, listContractChanges, type Opportunity, type QuotePlan, type ServiceContract, type CrmAttachment, type ContractChangeResponse } from "@/api/crm";
import { contractStatusColor, contractStatusLabel, formatMoney, opportunityStageColor, opportunityStageLabel, quoteStatusColor, quoteStatusLabel } from "./crm-options";
import BusinessTraceTimeline from "@/components/business/BusinessTraceTimeline.vue";

const route = useRoute();
const router = useRouter();
const record = ref<ServiceContract | null>(null);
const relatedQuote = ref<QuotePlan | null>(null);
const relatedOpportunity = ref<Opportunity | null>(null);
const loading = ref(true);
const saving = ref(false);
const editOpen = ref(false);
const editFormRef = ref();
const editForm = reactive({ projectName: "", contractType: "", amount: 0, serviceCycle: "", startDate: "", endDate: "", reason: "" });
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
  } catch (error) {
    message.error(error instanceof Error ? error.message : "合同加载失败");
  } finally {
    loading.value = false;
  }
}

const attachments = ref<CrmAttachment[]>([]);
const approvalAttachments = computed(() => attachments.value.filter((a) => a.attachmentType === "APPROVAL_DOC" || !a.attachmentType));
const signedAttachments = computed(() => attachments.value.filter((a) => a.attachmentType === "SIGNED_DOC"));

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

function openEdit() {
  if (!record.value) return;
  Object.assign(editForm, { projectName: record.value.projectName, contractType: record.value.contractType, amount: record.value.amount, serviceCycle: record.value.serviceCycle || "", startDate: record.value.startDate || "", endDate: record.value.endDate || "" });
  editOpen.value = true;
}

async function handleEdit() {
  await editFormRef.value?.validate();
  if (!record.value) return;
  if (!editForm.reason.trim()) { message.error("请填写变更原因"); return; }
  saving.value = true;
  try {
    const payload = {
      changeData: JSON.stringify({ projectName: editForm.projectName, contractType: editForm.contractType, amount: editForm.amount, serviceCycle: editForm.serviceCycle, startDate: editForm.startDate, endDate: editForm.endDate }),
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
async function convertToProject() {
  if (!record.value) return;
  Modal.confirm({
    title: "确认将合同承接为项目？",
    content: "系统会按合同金额生成默认预算：人工20%、材料45%、外包20%、差旅5%、其他10%。后续可在项目详情中调整成本归集。",
    okText: "生成项目",
    cancelText: "取消",
    async onOk() {
      if (!record.value) return;
      try {
        const amount = Number(record.value.amount || 0);
        await createProject({
      customerId: record.value.customerId,
      name: record.value.projectName + "项目",
      projectType: "RENOVATION",
      managerName: auth.user?.displayName || "",
      siteAddress: `${record.value.customerName || record.value.projectName}项目现场`,
      contractAmount: record.value.amount,
      plannedStartDate: record.value.startDate,
      plannedEndDate: record.value.endDate,
          budgetItems: [
            { category: "LABOR", plannedAmount: Math.round(amount * 0.2 * 100) / 100, remark: "合同转项目默认人工预算" },
            { category: "MATERIAL", plannedAmount: Math.round(amount * 0.45 * 100) / 100, remark: "合同转项目默认材料预算" },
            { category: "SUBCONTRACT", plannedAmount: Math.round(amount * 0.2 * 100) / 100, remark: "合同转项目默认外包预算" },
            { category: "TRAVEL", plannedAmount: Math.round(amount * 0.05 * 100) / 100, remark: "合同转项目默认差旅预算" },
            { category: "OTHER", plannedAmount: Math.round(amount * 0.1 * 100) / 100, remark: "合同转项目默认其他预算" },
          ],
      contractId: record.value.id,
    });
        message.success("项目已创建，并进入项目预算与成本闭环");
        router.push("/projects/list");
      } catch (error) { message.error(error instanceof Error ? error.message : "项目创建失败"); }
    },
  });
}

function goBack() {
  router.push("/crm/contracts");
}
</script>
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
