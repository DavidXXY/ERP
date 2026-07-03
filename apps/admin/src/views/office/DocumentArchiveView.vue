<template>
  <div class="page-stack">
    <a-card>
      <template #title>电子档案</template>
      <template #extra><a-space><a-button @click="goBack">返回办公室</a-button><a-button :loading="loading" @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button></a-space></template>
      <a-space wrap class="table-toolbar"><a-button v-if="auth.can('office:document:upload')" type="primary" @click="openUpload"><template #icon><UploadOutlined /></template>上传档案</a-button></a-space>
      <a-table :columns="documentColumns" :data-source="documents" :loading="loading" :pagination="{pageSize:10}" :row-key="(r:any)=>r.id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'file'"><strong>{{ record.fileName }}</strong><span class="table-subtitle">{{ record.contentType || '未知格式' }}</span></template>
          <template v-else-if="column.key === 'size'">{{ formatFileSize(record.sizeBytes) }}</template>
          <template v-else-if="column.key === 'created'">{{ formatDateTime(record.createdAt) }}</template>
          <template v-else-if="column.key === 'action'"><a-button type="link" size="small" @click="handleDownload(record)"><template #icon><DownloadOutlined /></template>下载</a-button></template>
        </template>
      </a-table>
    </a-card>
    <a-modal v-model:open="uploadOpen" title="上传电子档案" :confirm-loading="saving" @ok="handleUpload">
      <a-form ref="uploadFormRef" :model="uploadForm" :rules="uploadRules" layout="vertical">
        <a-form-item label="档案类别" name="bizType"><a-select v-model:value="uploadForm.bizType" :options="documentTypeOptions" /></a-form-item>
        <a-form-item label="业务记录编号"><a-input v-model:value="uploadForm.bizId" placeholder="可留空，或填写关联记录UUID" /></a-form-item>
        <a-form-item label="文件" name="file"><a-upload :before-upload="captureFile" :max-count="1"><a-button><template #icon><UploadOutlined /></template>选择文件</a-button></a-upload></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref } from "vue"; import { useRouter } from "vue-router";
import { message } from "ant-design-vue"; import DownloadOutlined from "@ant-design/icons-vue/DownloadOutlined"; import PlusOutlined from "@ant-design/icons-vue/PlusOutlined"; import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined"; import UploadOutlined from "@ant-design/icons-vue/UploadOutlined";
import { downloadDocument, listDocuments, uploadDocument, type DocumentRecord } from "@/api/office"; import { useAuthStore } from "@/stores/auth";
const auth=useAuthStore(); const router=useRouter(); const loading=ref(false); const saving=ref(false); const documents=ref<DocumentRecord[]>([]);
const uploadOpen=ref(false); const uploadFormRef=ref();
const uploadForm=reactive<{bizType:string;bizId:string;file?:File}>({bizType:'CONTRACT',bizId:'',file:undefined});
const documentColumns=[{title:'文件',key:'file'},{title:'类别',dataIndex:'bizType',width:180},{title:'大小',key:'size',width:120},{title:'上传时间',key:'created',width:190},{title:'操作',key:'action',width:110}];
const documentTypeOptions=[{label:'客户合同',value:'CONTRACT'},{label:'设备图纸',value:'EQUIPMENT_DRAWING'},{label:'检测报告',value:'INSPECTION_REPORT'},{label:'项目资料',value:'COMPLETION_DOCUMENT'},{label:'人员证书',value:'EMPLOYEE_CERTIFICATE'},{label:'验收单',value:'ACCEPTANCE'},{label:'招投标文件',value:'BID'}];
const uploadRules={bizType:[{required:true}],file:[{required:true,message:'请选择文件'}]};
onMounted(loadData);
async function loadData(){loading.value=true;try{documents.value=await listDocuments();}catch(error){message.error(error instanceof Error?error.message:'数据加载失败');}finally{loading.value=false;}}
function goBack(){router.push('/office');}
function openUpload(){Object.assign(uploadForm,{bizType:'CONTRACT',bizId:'',file:undefined});uploadOpen.value=true;}
function captureFile(file:File){uploadForm.file=file;return false;}
async function handleUpload(){if(!uploadForm.file){message.error('请选择文件');return;}saving.value=true;try{await uploadDocument({bizType:uploadForm.bizType,bizId:uploadForm.bizId||undefined,file:uploadForm.file});uploadOpen.value=false;message.success('档案已上传');await loadData();}catch(error){message.error(error instanceof Error?error.message:'档案上传失败');}finally{saving.value=false;}}
async function handleDownload(item:DocumentRecord){try{await downloadDocument(item);}catch(error){message.error(error instanceof Error?error.message:'档案下载失败');}}
function formatFileSize(v:number){if(!v)return'0 B';if(v<1024)return v+' B';if(v<1024*1024)return(v/1024).toFixed(1)+' KB';return(v/1024/1024).toFixed(1)+' MB';}
function formatDateTime(v?:string){return v?new Date(v).toLocaleString('zh-CN',{hour12:false}):'-';}
</script>
