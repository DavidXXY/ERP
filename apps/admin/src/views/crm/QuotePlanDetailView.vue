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
          <a-tag>V{{ record.versionNo }}</a-tag>
          <a-tag :color="quoteStatusColor(record.status)">{{ quoteStatusLabel(record.status) }}</a-tag>
        </div>

        <a-descriptions bordered :column="{ xs: 1, sm: 2, md: 3 }" style="margin-top: 20px">
          <a-descriptions-item label="客户">{{ record.customerName }}</a-descriptions-item>
          <a-descriptions-item label="关联商机">{{ record.opportunityCode || "未关联" }}</a-descriptions-item>
          <a-descriptions-item label="报价金额">
            <strong>{{ formatMoney(record.amount) }}</strong>
          </a-descriptions-item>
          <a-descriptions-item label="服务频次">{{ record.inspectCycle || "未设置" }}</a-descriptions-item>
          <a-descriptions-item label="付款节点">{{ record.paymentNodes || "未设置" }}</a-descriptions-item>
          <a-descriptions-item label="更新时间">{{ formatDateTime(record.updatedAt) }}</a-descriptions-item>
        </a-descriptions>

        <a-card title="服务范围" style="margin-top: 16px">
          <p style="margin: 0; white-space: pre-wrap">{{ record.serviceScope }}</p>
        </a-card>

        <a-card v-if="record.status === 'PENDING_APPROVAL' || record.status === 'APPROVED' || record.status === 'REJECTED'" title="内部审批记录" style="margin-top: 16px">
          <a-descriptions bordered :column="2">
            <a-descriptions-item label="审批人">{{ record.lastApproverName || "-" }}</a-descriptions-item>
            <a-descriptions-item label="审批时间">{{ formatDateTime(record.lastApprovalAt) }}</a-descriptions-item>
            <a-descriptions-item label="审批意见" :span="2">{{ record.lastApprovalComment || "-" }}</a-descriptions-item>
          </a-descriptions>
        </a-card>

        <a-card v-if="record.customerDecision" title="客户反馈" style="margin-top: 16px">
          <a-descriptions bordered :column="2">
            <a-descriptions-item label="客户结果">
              <a-tag :color="record.customerDecision === 'ACCEPTED' ? 'green' : 'red'">
                {{ record.customerDecision === 'ACCEPTED' ? '客户接受' : '客户拒绝' }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="登记人">{{ record.customerDecisionBy || "-" }}</a-descriptions-item>
            <a-descriptions-item label="结果说明" :span="2">{{ record.customerComment || "-" }}</a-descriptions-item>
            <a-descriptions-item label="登记时间" :span="2">{{ formatDateTime(record.customerDecidedAt) }}</a-descriptions-item>
          </a-descriptions>
        </a-card>

        <a-card v-if="record.convertedContractId" title="转合同" style="margin-top: 16px">
          <a-alert type="success" show-icon message="本报价已转为合同" />
        </a-card>
      </template>

      <a-empty v-else description="未找到报价方案" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { message } from "ant-design-vue";
import { useRoute, useRouter } from "vue-router";
import { getQuote, listAttachments, uploadAttachment, deleteAttachment, type QuotePlan, type CrmAttachment } from "@/api/crm";
import { formatMoney, quoteStatusColor, quoteStatusLabel } from "./crm-options";

const route = useRoute();
const router = useRouter();
const record = ref<QuotePlan | null>(null);
const loading = ref(true);
const id = route.params.id as string;

const attachments = ref<CrmAttachment[]>([]);

async function loadAttachments() {
  try {
    attachments.value = await listAttachments("QUOTE", id);
  } catch {}
}

async function handleUpload(file: File) {
  try {
    await uploadAttachment("QUOTE", id, undefined, file);
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

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    record.value = await getQuote(id);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "报价加载失败");
  } finally {
    loading.value = false;
  }
}

function goBack() {
  router.push("/crm/quotes");
}

function formatDateTime(value?: string) {
  if (!value) return "";
  return new Date(value).toLocaleString("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}
</script>
