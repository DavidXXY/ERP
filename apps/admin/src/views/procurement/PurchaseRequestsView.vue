<template>
  <div class="page-stack">
    <a-card>
      <template #title>采购申请</template>
      <template #extra><a-button @click="router.push('/procurement')">返回采购管理</a-button><a-button :loading="loading" @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button></template>
      <a-space wrap class="table-toolbar">
        <a-button v-if="auth.can('procurement:purchase:create')" type="primary" @click="openCreate"><template #icon><PlusOutlined /></template>新增申请</a-button>
      </a-space>
      <a-table :columns="requestColumns" :data-source="requests" :loading="loading" :pagination="{pageSize:10}" :row-key="(r:any)=>r.id" :scroll="{x:1350}">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'request'"><strong>{{ record.code }}</strong><span class="table-subtitle">{{ record.reason || record.description || '' }}</span></template>
          <template v-else-if="column.key === 'material'">{{ record.materialName || '-' }}<span class="table-subtitle">{{ record.materialSpec || '' }}</span></template>
          <template v-else-if="column.key === 'qty'">{{ record.quantity }}<span class="table-subtitle">{{ record.unit || '' }}</span></template>
          <template v-else-if="column.key === 'amount'"><strong>{{ formatMoney(record.totalAmount || record.unitPrice * record.quantity) }}</strong></template>
          <template v-else-if="column.key === 'date'">{{ record.requiredDate || '-' }}</template>
          <template v-else-if="column.key === 'status'"><a-tag :color="{DRAFT:'default',SUBMITTED:'blue',APPROVED:'green',ORDERED:'cyan',RECEIVED:'green',CANCELLED:'red'}[record.status]||'default'">{{ {DRAFT:'草稿',SUBMITTED:'待审批',APPROVED:'已通过',ORDERED:'已下单',RECEIVED:'已收货',CANCELLED:'已取消'}[record.status]||record.status }}</a-tag></template>
          <template v-else-if="column.key === 'action'">
            <span @click.stop><a-popconfirm v-if="record.status === 'DRAFT'" title="提交审批?" @confirm="handleSubmit(record)"><a-button type="link" size="small">提交审批</a-button></a-popconfirm></span>
          </template>
        </template>
      </a-table>
    </a-card>
    <a-modal v-model:open="createOpen" title="新增采购申请" width="760px" :confirm-loading="saving" @ok="handleCreate">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16"><a-col :span="12"><a-form-item label="物料名称" name="materialName"><a-input v-model:value="form.materialName" /></a-form-item></a-col><a-col :span="12"><a-form-item label="规格型号"><a-input v-model:value="form.materialSpec" /></a-form-item></a-col></a-row>
        <a-row :gutter="16"><a-col :span="8"><a-form-item label="数量" name="quantity"><a-input-number v-model:value="form.quantity" :min="1" class="full-input" /></a-form-item></a-col><a-col :span="8"><a-form-item label="单位"><a-input v-model:value="form.unit" /></a-form-item></a-col><a-col :span="8"><a-form-item label="需求日期"><a-input v-model:value="form.requiredDate" type="date" /></a-form-item></a-col></a-row>
        <a-row :gutter="16"><a-col :span="12"><a-form-item label="预计单价"><a-input-number v-model:value="form.unitPrice" :min="0" :precision="2" class="full-input" /></a-form-item></a-col><a-col :span="12"><a-form-item label="成本归属"><a-select v-model:value="form.costType" :options="costTypeOptions" /></a-form-item></a-col></a-row>
        <a-form-item label="申请原因" name="reason"><a-textarea v-model:value="form.reason" :rows="2" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref } from "vue"; import { useRouter } from "vue-router";
import { message } from "ant-design-vue"; import PlusOutlined from "@ant-design/icons-vue/PlusOutlined"; import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { createPurchaseRequest, listPurchaseRequests, processPurchaseRequestApproval, type PurchaseRequest } from "@/api/core-business";
import { useAuthStore } from "@/stores/auth";
const auth=useAuthStore(); const router=useRouter(); const loading=ref(false); const saving=ref(false); const requests=ref<PurchaseRequest[]>([]);
const createOpen=ref(false); const formRef=ref();
const form=reactive({materialName:'',materialSpec:'',quantity:1,unit:'个',unitPrice:0,requiredDate:'',reason:'',costType:'PROJECT'});
const rules={materialName:[{required:true}],quantity:[{required:true}],reason:[{required:true}]};
const costTypeOptions=[{label:'项目采购',value:'PROJECT'},{label:'部门采购',value:'DEPARTMENT'}];
const requestColumns=[
  {title:'申请单',key:'request',width:220},{title:'物料',key:'material',width:200},{title:'数量',key:'qty',width:110},
  {title:'金额',key:'amount',width:130},{title:'需求日期',key:'date',width:120},{title:'状态',key:'status',width:110},
  {title:'操作',key:'action',width:120,fixed:'right' as const},
];
onMounted(loadData);
async function loadData(){loading.value=true;try{requests.value=await listPurchaseRequests();}catch(e:any){message.error(e.message||'加载失败');}finally{loading.value=false;}}
function openCreate(){Object.assign(form,{materialName:'',materialSpec:'',quantity:1,unit:'个',unitPrice:0,requiredDate:'',reason:'',costType:'PROJECT'});createOpen.value=true;}
async function handleCreate(){await formRef.value?.validate();saving.value=true;try{await createPurchaseRequest({...form,applicantName:auth.user?.displayName||''});createOpen.value=false;message.success('申请已创建');await loadData();}catch(e:any){message.error(e.message||'创建失败');}finally{saving.value=false;}}
async function handleSubmit(record:PurchaseRequest){try{await processPurchaseRequestApproval(record.id);message.success('已提交审批');await loadData();}catch(e:any){message.error(e.message||'提交失败');}}
function formatMoney(v:number){return new Intl.NumberFormat('zh-CN',{style:'currency',currency:'CNY',minimumFractionDigits:2}).format(v||0);}
</script>
