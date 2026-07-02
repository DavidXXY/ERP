<template>
  <div class="page-stack">
    <a-card>
      <template #extra>
        <a-space>
          <a-button @click="goBack">返回列表</a-button>
          <a-button @click="loadData">刷新</a-button>
        </a-space>
      </template>

      <div v-if="loading" style="text-align: center; padding: 48px">
        <a-spin size="large" />
      </div>

      <template v-else-if="record">
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
    </a-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { message } from "ant-design-vue";
import { useRoute, useRouter } from "vue-router";
import { getContract, getOpportunity, getQuote, uploadAttachment, deleteAttachment, listAttachments, getAttachmentDownloadUrl, type Opportunity, type QuotePlan, type ServiceContract, type CrmAttachment } from "@/api/crm";
import { contractStatusColor, contractStatusLabel, formatMoney, opportunityStageColor, opportunityStageLabel, quoteStatusColor, quoteStatusLabel } from "./crm-options";

const route = useRoute();
const router = useRouter();
const record = ref<ServiceContract | null>(null);
const relatedQuote = ref<QuotePlan | null>(null);
const relatedOpportunity = ref<Opportunity | null>(null);
const loading = ref(true);
const id = route.params.id as string;

onMounted(() => { loadData(); loadAttachments(); });

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

function goBack() {
  router.push("/crm/contracts");
}
</script>
