<template>
  <div class="page-shell">
    <header class="page-heading"><div><p class="eyebrow">资质中心</p><h1>资质文件</h1><p>上传营业执照、行业资质、银行及税务资料，单个文件不超过 20MB。</p></div><a-button type="primary" @click="uploadOpen = true"><UploadOutlined /> 上传文件</a-button></header>
    <div class="filter-row"><a-segmented v-model:value="filter" :options="filterOptions" /><span>{{ filtered.length }} 份文件</span></div>
    <a-alert
      v-for="warning in expiryWarnings"
      :key="warning"
      type="warning"
      show-icon
      :message="warning"
      style="margin-bottom: 12px"
    />
    <a-alert
      v-if="expiringDocuments.length > 0"
      type="info"
      show-icon
      message="资质到期提醒"
      :description="`${expiringDocuments.length} 份文件将在 90 天内到期或已到期，请及时更新并重新上传。`"
      style="margin-bottom: 12px"
    />
    <a-skeleton v-if="loading" active />
    <a-empty v-else-if="filtered.length === 0" description="暂无符合条件的资质文件" />
    <div v-else class="document-grid">
      <article v-for="doc in filtered" :key="doc.id" class="document-card">
        <div class="file-icon"><FilePdfOutlined v-if="doc.contentType?.includes('pdf')" /><FileImageOutlined v-else-if="doc.contentType?.startsWith('image')" /><FileOutlined v-else /></div>
        <div class="file-info"><strong>{{ doc.documentName }}</strong><span>{{ typeLabel(doc.documentType) }} · {{ fileSize(doc.sizeBytes) }}</span><small>上传于 {{ formatDate(doc.createdAt) }}<template v-if="doc.validTo"> · 有效至 {{ doc.validTo }}</template></small></div>
        <a-tag :color="reviewColor(doc.reviewStatus)">{{ reviewText(doc.reviewStatus) }}</a-tag>
        <p v-if="doc.reviewComment" class="review-comment">{{ doc.reviewComment }}</p>
        <div class="file-actions"><a-button type="link" @click="download(doc)"><DownloadOutlined /> 下载</a-button><a-popconfirm v-if="doc.reviewStatus !== 'APPROVED'" title="确认删除该文件？" @confirm="remove(doc.id)"><a-button type="link" danger><DeleteOutlined /> 删除</a-button></a-popconfirm></div>
      </article>
    </div>
    <a-modal v-model:open="uploadOpen" title="上传资质文件" :confirm-loading="uploading" @ok="upload">
      <a-form layout="vertical">
        <a-form-item label="资料类型" required><a-select v-model:value="uploadForm.documentType" :options="typeOptions" /></a-form-item>
        <a-form-item label="有效期"><a-input v-model:value="uploadForm.validTo" type="date" /></a-form-item>
        <a-form-item label="文件" required><a-upload :before-upload="selectFile" :file-list="fileList" :max-count="1"><a-button><PaperClipOutlined /> 选择文件</a-button></a-upload></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import type { UploadFile } from "ant-design-vue";
import { DeleteOutlined, DownloadOutlined, FileImageOutlined, FileOutlined, FilePdfOutlined, PaperClipOutlined, UploadOutlined } from "@ant-design/icons-vue";
import * as api from "../api";
import { docExpiryDays, expiryMessage, fileSize, formatDate, validateUploadFile } from "../utils/quote";

const documents = ref<api.PortalDocument[]>([]); const loading = ref(false); const uploading = ref(false); const uploadOpen = ref(false); const filter = ref("ALL");
const selectedFile = ref<File>(); const fileList = ref<UploadFile[]>([]); const uploadForm = reactive({ documentType: "BUSINESS_LICENSE", validTo: "" });
const filterOptions = [{ label: "全部", value: "ALL" }, { label: "待审核", value: "PENDING" }, { label: "已通过", value: "APPROVED" }, { label: "已退回", value: "REJECTED" }];
const typeOptions = [{ label: "营业执照", value: "BUSINESS_LICENSE" }, { label: "行业资质", value: "QUALIFICATION" }, { label: "银行证明", value: "BANK_PROOF" }, { label: "税务资料", value: "TAX_DOCUMENT" }, { label: "其他", value: "OTHER" }];
const filtered = computed(() => filter.value === "ALL" ? documents.value : documents.value.filter((d) => d.reviewStatus === filter.value));
const expiringDocuments = computed(() => {
  return documents.value.filter((doc) => {
    const days = docExpiryDays(doc.validTo);
    return days !== null && days <= 90;
  });
});
const expiryWarnings = computed(() => {
  return expiringDocuments.value
    .map((doc) =>
      expiryMessage(doc.documentName || typeLabel(doc.documentType), doc.validTo),
    )
    .filter((text): text is string => Boolean(text))
    .slice(0, 5);
});
onMounted(load);
async function load() { loading.value = true; try { documents.value = await api.listDocuments(); } catch (e) { message.error(e instanceof Error ? e.message : "加载失败"); } finally { loading.value = false; } }
function selectFile(file: File) {
  const invalid = validateUploadFile(file);
  if (invalid) {
    message.warning(invalid);
    return false;
  }
  selectedFile.value = file;
  fileList.value = [{ uid: file.name, name: file.name, status: "done", originFileObj: file } as UploadFile];
  return false;
}
async function upload() { if (!selectedFile.value) { message.warning("请选择文件"); return; } uploading.value = true; try { const data = new FormData(); data.append("documentType", uploadForm.documentType); if (uploadForm.validTo) data.append("validTo", uploadForm.validTo); data.append("file", selectedFile.value); await api.uploadDocument(data); uploadOpen.value = false; selectedFile.value = undefined; fileList.value = []; await load(); message.success("文件已上传并进入审核"); } catch (e) { message.error(e instanceof Error ? e.message : "上传失败"); } finally { uploading.value = false; } }
async function remove(id: string) { try { await api.deleteDocument(id); await load(); message.success("文件已删除"); } catch (e) { message.error(e instanceof Error ? e.message : "删除失败"); } }
function download(doc: api.PortalDocument) { window.location.href = api.documentDownloadUrl(doc.id); }
const typeLabel = (v: string) => ({ BUSINESS_LICENSE: "营业执照", QUALIFICATION: "行业资质", BANK_PROOF: "银行证明", TAX_DOCUMENT: "税务资料", OTHER: "其他" })[v] || v;
const reviewText = (v: string) => ({ PENDING: "待审核", APPROVED: "已通过", REJECTED: "已退回" })[v] || v;
const reviewColor = (v: string) => ({ PENDING: "orange", APPROVED: "green", REJECTED: "red" })[v] || "default";
</script>
