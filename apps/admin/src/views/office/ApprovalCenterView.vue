<template>
  <div class="page-stack">
    <a-card>
      <template #title>审批中心</template>
      <template #extra>
        <a-space>
          <a-button @click="goBack">返回办公室</a-button>
          <a-button :loading="loading" @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
        </a-space>
      </template>

      <a-space wrap class="table-toolbar">
        <a-radio-group v-model:value="sourceFilter" button-style="solid" size="small">
          <a-radio-button value="all">全部 ({{ mergedList.length }})</a-radio-button>
          <a-radio-button value="office">办公室 ({{ officeCount }})</a-radio-button>
          <a-radio-button value="quote">报价审批 ({{ quoteCount }})</a-radio-button>
          <a-radio-button value="change">合同变更 ({{ changeCount }})</a-radio-button>
        </a-radio-group>
        <a-button v-if="auth.can('office:approval:create')" type="primary" @click="openApprovalCreate"><template #icon><PlusOutlined /></template>发起审批</a-button>
      </a-space>

      <a-table :columns="mergedColumns" :data-source="filteredList" :loading="loading" :pagination="{pageSize:10}" :row-key="(r:any)=>r._key" :scroll="{x:1350}">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'source'">
            <a-tag :color="record._source === 'office' ? 'blue' : record._source === 'quote' ? 'purple' : 'orange'">
              {{ record._source === 'office' ? '办公室' : record._source === 'quote' ? '报价审批' : '合同变更' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'approval'">
            <strong>{{ record.code }}</strong>
            <span class="table-subtitle">{{ record.title || record.customerName || record.desc }}</span>
          </template>
          <template v-else-if="column.key === 'type'">{{ record._type }}</template>
          <template v-else-if="column.key === 'applicant'">{{ record.applicantName || record.requestedBy || '-' }}</template>
          <template v-else-if="column.key === 'amount'">{{ formatMoney(record.amount) }}</template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record._statusColor">{{ record._statusLabel }}</a-tag>
          </template>
          <template v-else-if="column.key === 'date'">{{ record.date?.slice(0, 10) || '-' }}</template>
          <template v-else-if="column.key === 'action'">
            <template v-if="record._source === 'office' && record.status === 'PENDING' && auth.can('office:approval:process')">
              <a-button type="link" size="small" @click="openProcess(record)">处理审批</a-button>
            </template>
            <template v-else-if="record._source === 'quote'">
              <a-button type="link" size="small" @click="router.push('/crm/quotes/' + record._entityId)">查看报价</a-button>
              <a-button v-if="auth.can('crm:quote:approve')" type="link" size="small" @click="openCrmProcess(record)">审批处理</a-button>
            </template>
            <template v-else-if="record._source === 'change'">
              <a-button type="link" size="small" @click="router.push('/crm/contracts/' + record._contractId)">查看合同</a-button>
              <span @click.stop>
                <a-popconfirm title="确认通过?" @confirm="handleCrmChangeAction(record, 'APPROVED')">
                  <a-button type="link" size="small">通过</a-button>
                </a-popconfirm>
                <a-popconfirm title="确认驳回?" @confirm="handleCrmChangeAction(record, 'REJECTED')">
                  <a-button type="link" size="small" danger>驳回</a-button>
                </a-popconfirm>
              </span>
            </template>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- Office approval create modal -->
    <a-modal v-model:open="approvalCreateOpen" title="发起通用审批" width="680px" :confirm-loading="saving" @ok="handleApprovalCreate">
      <a-form ref="approvalCreateFormRef" :model="approvalCreateForm" :rules="approvalCreateRules" layout="vertical">
        <a-form-item label="标题" name="title"><a-input v-model:value="approvalCreateForm.title" /></a-form-item>
        <a-row :gutter="16"><a-col :span="12"><a-form-item label="来源单号"><a-input v-model:value="approvalCreateForm.sourceNo" /></a-form-item></a-col><a-col :span="12"><a-form-item label="金额"><a-input-number v-model:value="approvalCreateForm.amount" :min="0" :precision="2" class="full-input" /></a-form-item></a-col></a-row>
        <a-form-item label="申请人" name="applicantName"><a-input v-model:value="approvalCreateForm.applicantName" /></a-form-item>
        <a-form-item label="申请内容" name="content"><a-textarea v-model:value="approvalCreateForm.content" :rows="3" /></a-form-item>
      </a-form>
    </a-modal>

    <!-- Office approval process modal -->
    <a-modal v-model:open="processOpen" title="处理审批" :confirm-loading="saving" @ok="handleProcess">
      <a-alert v-if="selectedApproval" class="section-alert" type="info" :message="`${selectedApproval.code} · ${selectedApproval.title} · ${formatMoney(selectedApproval.amount)}`" />
      <a-form ref="processFormRef" :model="processForm" :rules="processRules" layout="vertical">
        <a-form-item label="审批结论" name="decision"><a-radio-group v-model:value="processForm.decision" button-style="solid"><a-radio-button value="APPROVED">通过</a-radio-button><a-radio-button value="REJECTED">驳回</a-radio-button></a-radio-group></a-form-item>
        <a-form-item label="审批意见" name="comment"><a-textarea v-model:value="processForm.comment" :rows="3" /></a-form-item>
        <a-form-item label="审批人" name="approverName"><a-input v-model:value="processForm.approverName" /></a-form-item>
      </a-form>
    </a-modal>

    <!-- CRM quote approval modal -->
    <a-modal v-model:open="crmProcessOpen" title="报价审批" :confirm-loading="saving" @ok="handleCrmProcess">
      <a-alert v-if="selectedCrmApproval" class="section-alert" type="info" :message="`${selectedCrmApproval.code} · ${selectedCrmApproval.customerName} · ${formatMoney(selectedCrmApproval.amount)}`" />
      <a-form ref="crmProcessFormRef" :model="crmProcessForm" :rules="crmProcessRules" layout="vertical">
        <a-form-item label="审批结论" name="decision"><a-radio-group v-model:value="crmProcessForm.decision" button-style="solid"><a-radio-button value="APPROVED">通过</a-radio-button><a-radio-button value="REJECTED">驳回</a-radio-button></a-radio-group></a-form-item>
        <a-form-item label="审批意见" name="comment"><a-textarea v-model:value="crmProcessForm.comment" :rows="3" /></a-form-item>
        <a-form-item label="审批人" name="approverName"><a-input v-model:value="crmProcessForm.approverName" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { message } from "ant-design-vue";
import PlusOutlined from "@ant-design/icons-vue/PlusOutlined"; import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { createApproval, getOfficeOverview, listApprovals, processApproval, type Approval, type ApprovalStatus, type ApprovalType } from "@/api/office";
import { listQuotes, listContracts, listContractChanges, processQuoteApproval, approveContractChange, rejectContractChange, type QuotePlan } from "@/api/crm";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore(); const router = useRouter(); const loading = ref(false); const saving = ref(false);
const sourceFilter = ref("all");

// Office approval data
const officeApprovals = ref<Approval[]>([]);
// CRM approval data
const pendingQuotes = ref<QuotePlan[]>([]);
const pendingChanges = ref<any[]>([]);

// Modals
const approvalCreateOpen = ref(false); const processOpen = ref(false); const crmProcessOpen = ref(false);
const approvalCreateFormRef = ref(); const processFormRef = ref(); const crmProcessFormRef = ref();
const selectedApproval = ref<Approval | null>(null);
const selectedCrmApproval = ref<any>(null);

const approvalCreateForm = reactive({ code: "", approvalType: "OTHER" as ApprovalType, title: "", sourceNo: "", amount: 0, applicantName: "", content: "" });
const processForm = reactive({ decision: "APPROVED" as "APPROVED" | "REJECTED", comment: "同意", approverName: "" });
const crmProcessForm = reactive({ decision: "APPROVED" as "APPROVED" | "REJECTED", comment: "同意", approverName: "" });
const crmProcessRules = { decision: [{ required: true }], comment: [{ required: true }], approverName: [{ required: true }] };
const approvalCreateRules = { title: [{ required: true }], applicantName: [{ required: true }], content: [{ required: true }] };
const processRules = { decision: [{ required: true }], comment: [{ required: true }], approverName: [{ required: true }] };

// Unified approval list
const mergedList = computed(() => {
  const items: any[] = [];
  // Office approvals
  officeApprovals.value.forEach((a) => {
    items.push({
      _key: "office-" + a.id, _source: "office", _entityId: a.id,
      _type: "通用审批", _statusLabel: approvalStatusLabel(a.status),
      _statusColor: approvalStatusColor(a.status),
      id: a.id, code: a.code, title: a.title, amount: a.amount,
      applicantName: a.applicantName, status: a.status, date: a.createdAt,
      approverName: a.approverName, approvalComment: a.approvalComment,
    });
  });
  // CRM quote approvals
  pendingQuotes.value.forEach((q) => {
    items.push({
      _key: "quote-" + q.id, _source: "quote", _entityId: q.id,
      _type: "报价审批", _statusLabel: "待审批",
      _statusColor: "orange",
      id: q.id, code: q.code, title: q.customerName, desc: q.serviceScope?.slice(0, 40),
      amount: q.amount, customerName: q.customerName, status: q.status, date: q.updatedAt,
      applicantName: (q as any).editorName, approverName: q.lastApproverName,
      approvalComment: q.lastApprovalComment,
    });
  });
  // Contract changes
  pendingChanges.value.forEach((c) => {
    items.push({
      _key: "change-" + c.id, _source: "change", _entityId: c.id,
      _contractId: c.contractId,
      _type: "合同变更", _statusLabel: "待审批",
      _statusColor: "orange",
      id: c.id, code: c.contractCode || "-", title: c.reason,
      desc: c.changeData, amount: 0, date: c.requestedAt,
      applicantName: c.requestedBy, status: c.status,
    });
  });
  items.sort((a, b) => ((a.date || "").localeCompare(b.date || "")) * -1);
  return items;
});

const filteredList = computed(() => {
  if (sourceFilter.value === "all") return mergedList.value;
  return mergedList.value.filter((i) => i._source === sourceFilter.value);
});
const officeCount = computed(() => mergedList.value.filter((i) => i._source === "office").length);
const quoteCount = computed(() => mergedList.value.filter((i) => i._source === "quote").length);
const changeCount = computed(() => mergedList.value.filter((i) => i._source === "change").length);

const mergedColumns = [
  { title: "来源", key: "source", width: 100 },
  { title: "编号 / 说明", key: "approval", width: 240 },
  { title: "类型", key: "type", width: 100 },
  { title: "申请人", key: "applicant", width: 120 },
  { title: "金额", key: "amount", width: 130 },
  { title: "状态", key: "status", width: 100 },
  { title: "时间", key: "date", width: 120 },
  { title: "操作", key: "action", width: 220, fixed: "right" as const },
];

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    const promises: Promise<any>[] = [
      listApprovals(),
      listQuotes(),
      listContracts(),
    ];
    const results = await Promise.all(promises);
    officeApprovals.value = results[0];
    const allQuotes = results[1];
    const allContracts = results[2];
    pendingQuotes.value = allQuotes.filter((q: QuotePlan) => q.status === "PENDING_APPROVAL");
    // Fetch contract changes
    try {
      const contracts = allContracts;
      const allChanges: any[] = [];
      for (const c of contracts) {
        try {
          const changes = await listContractChanges(c.id);
          allChanges.push(...changes.filter((ch: any) => ch.status === "PENDING").map((ch: any) => ({ ...ch, contractId: c.id, contractCode: c.code })));
        } catch { /* skip */ }
      }
      pendingChanges.value = allChanges;
    } catch { /* contract changes supplementary */ }
  } catch (error) { message.error(error instanceof Error ? error.message : "数据加载失败"); }
  finally { loading.value = false; }
}

function goBack() { router.push("/office"); }
function openApprovalCreate() {
  Object.assign(approvalCreateForm, { code: generateCode("SP"), approvalType: "OTHER" as ApprovalType, title: "", sourceNo: "", amount: 0, applicantName: auth.user?.displayName || "", content: "" });
  approvalCreateOpen.value = true;
}
async function handleApprovalCreate() {
  await approvalCreateFormRef.value?.validate(); saving.value = true;
  try { await createApproval({ ...approvalCreateForm }); approvalCreateOpen.value = false; message.success("审批已发起"); await loadData(); }
  catch (error) { message.error(error instanceof Error ? error.message : "审批发起失败"); }
  finally { saving.value = false; }
}
function openProcess(item: any) { selectedApproval.value = item; Object.assign(processForm, { decision: "APPROVED", comment: "同意", approverName: auth.user?.displayName || "" }); processOpen.value = true; }
async function handleProcess() {
  await processFormRef.value?.validate(); if (!selectedApproval.value) return; saving.value = true;
  try { await processApproval(selectedApproval.value.id, { ...processForm }); processOpen.value = false; message.success(processForm.decision === "APPROVED" ? "审批已通过" : "审批已驳回"); await loadData(); }
  catch (error) { message.error(error instanceof Error ? error.message : "审批处理失败"); }
  finally { saving.value = false; }
}
function openCrmProcess(item: any) { selectedCrmApproval.value = item; Object.assign(crmProcessForm, { decision: "APPROVED", comment: "同意", approverName: auth.user?.displayName || "" }); crmProcessOpen.value = true; }
async function handleCrmProcess() {
  await crmProcessFormRef.value?.validate(); if (!selectedCrmApproval.value) return; saving.value = true;
  try { await processQuoteApproval(selectedCrmApproval.value._entityId, { ...crmProcessForm }); crmProcessOpen.value = false; message.success(crmProcessForm.decision === "APPROVED" ? "报价审批已通过" : "报价已驳回"); await loadData(); }
  catch (error) { message.error(error instanceof Error ? error.message : "审批处理失败"); }
  finally { saving.value = false; }
}
async function handleCrmChangeAction(item: any, decision: string) {
  saving.value = true;
  try {
    if (decision === "APPROVED") await approveContractChange(item._entityId, { operatorName: auth.user?.displayName || "", comment: "" });
    else await rejectContractChange(item._entityId, { operatorName: auth.user?.displayName || "", comment: "" });
    message.success(decision === "APPROVED" ? "变更已通过" : "变更已驳回"); await loadData();
  } catch (error) { message.error(error instanceof Error ? error.message : "操作失败"); }
  finally { saving.value = false; }
}

function generateCode(prefix: string) {
  const d = new Date(); const ds = `${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, "0")}${String(d.getDate()).padStart(2, "0")}`;
  return `${prefix}-${ds}-${String(d.getHours()).padStart(2, "0")}${String(d.getMinutes()).padStart(2, "0")}`;
}
function approvalTypeLabel(v: ApprovalType) { return ({ QUOTE: "报价", CONTRACT: "合同", PURCHASE: "采购", OUTSOURCE: "外包", EXPENSE: "报销", PAYMENT: "付款", SEAL: "用章", LEAVE: "请假", TRAVEL: "出差", OTHER: "其他" } as Record<ApprovalType, string>)[v]; }
function approvalStatusLabel(v: ApprovalStatus) { return ({ PENDING: "待审批", APPROVED: "已通过", REJECTED: "已驳回" } as Record<ApprovalStatus, string>)[v]; }
function approvalStatusColor(v: ApprovalStatus) { return ({ PENDING: "orange", APPROVED: "green", REJECTED: "red" } as Record<ApprovalStatus, string>)[v]; }
function formatMoney(v: number) { return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY", minimumFractionDigits: 2 }).format(v || 0); }
</script>
