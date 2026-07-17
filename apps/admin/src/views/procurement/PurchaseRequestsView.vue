<template>
  <div class="page-stack">
    <a-card>
      <template #title>采购申请</template>
      <template #extra><a-button @click="router.push('/procurement')">返回采购管理</a-button><a-button :loading="loading" @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button></template>
      <a-space wrap class="table-toolbar">
        <a-button v-if="auth.can('procurement:purchase:create')" type="primary" @click="openCreate"><template #icon><PlusOutlined /></template>新增申请</a-button>
      </a-space>
      <a-alert class="section-alert" type="info" show-icon message="项目采购会进入项目成本闭环，建议先确认预算余额。" />
      <a-table :columns="requestColumns" :data-source="requests" :loading="loading" :pagination="{pageSize:10}" :row-key="(r:any)=>r.id" :scroll="{x:1450}">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'request'"><strong>{{ record.code }}</strong><span class="table-subtitle">{{ record.reason || record.description || '' }}</span></template>
          <template v-else-if="column.key === 'material'">{{ record.partName || record.materialName || '-' }}<span class="table-subtitle">{{ record.materialSpec || '' }}</span></template>
          <template v-else-if="column.key === 'qty'">{{ record.quantity }}<span class="table-subtitle">{{ record.unit || '' }}</span></template>
          <template v-else-if="column.key === 'amount'"><strong>{{ record.totalAmount || record.unitPrice ? formatMoney(record.totalAmount || record.unitPrice * record.quantity) : '-' }}</strong><span class="table-subtitle">税率 {{ formatTaxRate(record.taxRate) }}</span></template>
          <template v-else-if="column.key === 'target'">{{ record.costTargetName || '-' }}<span class="table-subtitle">{{ record.costType === 'PROJECT' ? '项目预算' : '部门费用' }}</span></template>
          <template v-else-if="column.key === 'date'">{{ record.requiredDate || record.expectedDate || '-' }}</template>
          <template v-else-if="column.key === 'status'"><a-tag :color="({DRAFT:'default',SUBMITTED:'blue',APPROVED:'green',ORDERED:'cyan',RECEIVED:'green',CANCELLED:'red'} as any)[record.status]||'default'">{{ ({DRAFT:'草稿',SUBMITTED:'待审批',APPROVED:'已通过',ORDERED:'已下单',RECEIVED:'已收货',CANCELLED:'已取消'} as any)[record.status]||record.status }}</a-tag></template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <span @click.stop><a-popconfirm v-if="record.status === 'DRAFT'" title="提交审批?" @confirm="handleSubmit(record)"><a-button type="link" size="small">提交审批</a-button></a-popconfirm></span>
              <a-button type="link" size="small" @click="openApproval(record)">{{ record.approvalStatus === 'PENDING' ? '审批' : '流程' }}</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
    <a-modal v-model:open="createOpen" title="新增采购申请" width="760px" :confirm-loading="saving" @ok="handleCreate">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16"><a-col :span="12"><a-form-item label="物料名称" name="materialName"><a-input v-model:value="form.materialName" /></a-form-item></a-col><a-col :span="12"><a-form-item label="规格型号"><a-input v-model:value="form.materialSpec" /></a-form-item></a-col></a-row>
        <a-row :gutter="16"><a-col :span="8"><a-form-item label="数量" name="quantity"><a-input-number v-model:value="form.quantity" :min="1" class="full-input" /></a-form-item></a-col><a-col :span="8"><a-form-item label="单位"><a-input v-model:value="form.unit" /></a-form-item></a-col><a-col :span="8"><a-form-item label="需求日期"><a-input v-model:value="form.requiredDate" type="date" /></a-form-item></a-col></a-row>
        <a-row :gutter="16"><a-col :span="8"><a-form-item label="预计单价"><a-input-number v-model:value="form.unitPrice" :min="0" :precision="2" class="full-input" /></a-form-item></a-col><a-col :span="8"><a-form-item label="税率(%)"><a-input-number v-model:value="form.taxRate" :min="0" :max="100" :precision="2" class="full-input" /></a-form-item></a-col><a-col :span="8"><a-form-item label="成本归属"><a-select v-model:value="form.costType" :options="costTypeOptions" @change="form.costTargetId=''" /></a-form-item></a-col></a-row>
        <a-form-item :label="form.costType === 'PROJECT' ? '关联项目' : '关联部门'" name="costTargetId">
          <a-select v-model:value="form.costTargetId" show-search option-filter-prop="label" :options="targetOptions" placeholder="请选择成本归属对象" />
        </a-form-item>
        <a-alert v-if="selectedTarget" class="section-alert" type="warning" show-icon :message="targetHint" />
        <a-form-item label="申请原因" name="reason"><a-textarea v-model:value="form.reason" :rows="2" /></a-form-item>
        <a-alert type="info" show-icon :message="`预计采购金额 ${formatMoney(estimatedAmount)}，审批通过后进入请购到下单、到货、应付、付款闭环。`" />
      </a-form>
    </a-modal>
    <a-modal v-model:open="approvalOpen" title="采购申请审批进展" :footer="canApproveSelected ? undefined : null" :confirm-loading="saving" @ok="handleApproval">
      <a-alert v-if="selectedRequest" class="section-alert" type="info" :message="`${selectedRequest.code || '-'} · ${selectedRequest.partName || selectedRequest.materialName || '-'} · ${formatMoney(selectedRequest.totalAmount || Number(selectedRequest.unitPrice || 0) * Number(selectedRequest.quantity || 0))}`" />
      <a-card v-if="selectedRequest" size="small" title="流程进展" class="section-alert">
        <a-timeline>
          <a-timeline-item color="green">
            发起申请 · {{ selectedRequest.requesterName || selectedRequest.applicantName || "-" }}
            <span class="table-subtitle">{{ selectedRequest.reason || selectedRequest.description || "-" }}</span>
          </a-timeline-item>
          <a-timeline-item :color="selectedRequest.approvalStatus === 'PENDING' ? 'orange' : selectedRequest.approvalStatus === 'REJECTED' ? 'red' : 'green'">
            审批阶段 · {{ selectedRequest.lastApproverName || "待审批人处理" }}
            <span class="table-subtitle">{{ selectedRequest.approvalStatus === 'PENDING' ? '未审批' : `${approvalStatusLabel(selectedRequest.approvalStatus)} · ${selectedRequest.lastApprovalComment || '-'}` }}</span>
          </a-timeline-item>
          <a-timeline-item :color="selectedRequest.status === 'ORDERED' || selectedRequest.status === 'RECEIVED' ? 'green' : 'gray'">
            采购执行 · {{ selectedRequest.status === 'ORDERED' || selectedRequest.status === 'RECEIVED' ? statusLabel(selectedRequest.status) : "未执行" }}
          </a-timeline-item>
        </a-timeline>
      </a-card>
      <a-form v-if="canApproveSelected" ref="approvalFormRef" :model="approvalForm" layout="vertical">
        <a-form-item label="审批结论"><a-radio-group v-model:value="approvalForm.decision" button-style="solid"><a-radio-button value="APPROVED">通过</a-radio-button><a-radio-button value="REJECTED">驳回</a-radio-button></a-radio-group></a-form-item>
        <a-form-item label="审批意见"><a-textarea v-model:value="approvalForm.comment" :rows="3" /></a-form-item>
        <a-form-item label="审批人"><a-input v-model:value="approvalForm.approverName" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue"; import { useRouter } from "vue-router";
import { message } from "ant-design-vue"; import PlusOutlined from "@ant-design/icons-vue/PlusOutlined"; import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { createPurchaseRequest, listProcurementCostTargets, listPurchaseRequests, processPurchaseRequestApproval, type ApprovalStatus, type ProcurementCostTargetOption, type PurchaseRequest, type PurchaseRequestStatus } from "@/api/procurement";
import { useAuthStore } from "@/stores/auth";
const auth=useAuthStore(); const router=useRouter(); const loading=ref(false); const saving=ref(false); const requests=ref<PurchaseRequest[]>([]);
const projects=ref<ProcurementCostTargetOption[]>([]); const departments=ref<ProcurementCostTargetOption[]>([]);
const createOpen=ref(false); const approvalOpen=ref(false); const formRef=ref(); const approvalFormRef=ref(); const selectedRequest=ref<PurchaseRequest|null>(null);
const form=reactive({materialName:'',materialSpec:'',quantity:1,unit:'个',unitPrice:0,taxRate:13,requiredDate:'',reason:'',costType:'PROJECT',costTargetId:''});
const approvalForm=reactive<{decision:ApprovalStatus;comment:string;approverName:string}>({decision:'APPROVED',comment:'同意采购',approverName:''});
const rules={materialName:[{required:true}],quantity:[{required:true}],reason:[{required:true}],costTargetId:[{required:true,message:'请选择成本归属对象'}]};
const costTypeOptions=[{label:'项目采购',value:'PROJECT'},{label:'部门采购',value:'DEPARTMENT'}];
const requestColumns=[
  {title:'申请单',key:'request',width:220},{title:'物料',key:'material',width:200},{title:'数量',key:'qty',width:110},
  {title:'金额',key:'amount',width:130},{title:'成本归属',key:'target',width:190},{title:'需求日期',key:'date',width:120},{title:'状态',key:'status',width:110},
  {title:'操作',key:'action',width:120,fixed:'right' as const},
];
onMounted(loadData);
const targetOptions=computed(()=> (form.costType==='PROJECT'?projects.value:departments.value).map((item)=>({label:`${item.name}${item.code?' · '+item.code:''}`,value:item.id})));
const selectedTarget=computed(()=> (form.costType==='PROJECT'?projects.value:departments.value).find((item)=>item.id===form.costTargetId));
const estimatedAmount=computed(()=>Number(form.quantity||0)*Number(form.unitPrice||0));
const canApproveSelected=computed(()=>!!selectedRequest.value&&selectedRequest.value.approvalStatus==='PENDING'&&auth.can('procurement:request:approve'));
const targetHint=computed(()=> form.costType==='PROJECT'
  ? `将归集到项目：${selectedTarget.value?.name || '-'}。如预算不足，后续项目成本会进入风险中心。`
  : `将归集到部门：${selectedTarget.value?.name || '-'}。请确认费用归口和审批人。`);
async function loadData(){loading.value=true;try{const [result,targets]=await Promise.all([listPurchaseRequests({page:0,size:999}),listProcurementCostTargets()]);requests.value=result.content;projects.value=targets.projects;departments.value=targets.departments;}catch(e:any){message.error(e.message||'加载失败');}finally{loading.value=false;}}
function openCreate(){Object.assign(form,{materialName:'',materialSpec:'',quantity:1,unit:'个',unitPrice:0,requiredDate:'',reason:'',costType:'PROJECT',costTargetId:''});createOpen.value=true;}
async function handleCreate(){await formRef.value?.validate();saving.value=true;try{await createPurchaseRequest({
  requesterName:auth.user?.displayName||'',partName:form.materialName,quantity:form.quantity,unitPrice:form.unitPrice,taxRate:form.taxRate,expectedDate:form.requiredDate||undefined,
  reason:`${form.reason}${form.materialSpec?'；规格：'+form.materialSpec:''}${form.unit?'；单位：'+form.unit:''}${form.unitPrice?`；预计单价：${form.unitPrice}`:''}`,
  costType:form.costType as any,projectId:form.costType==='PROJECT'?form.costTargetId:undefined,departmentId:form.costType==='DEPARTMENT'?form.costTargetId:undefined,
} as any);createOpen.value=false;message.success('申请已创建，并进入采购审批闭环');await loadData();}catch(e:any){message.error(e.message||'创建失败');}finally{saving.value=false;}}
async function handleSubmit(record:PurchaseRequest){try{await processPurchaseRequestApproval(record.id,{decision:'APPROVED' as any,comment:'提交审批',approverName:auth.user?.displayName||''});message.success('已提交审批');await loadData();}catch(e:any){message.error(e.message||'提交失败');}}
function openApproval(record:PurchaseRequest){selectedRequest.value=record;Object.assign(approvalForm,{decision:'APPROVED',comment:'同意采购',approverName:auth.user?.displayName||''});approvalOpen.value=true;}
async function handleApproval(){if(!selectedRequest.value)return;saving.value=true;try{await processPurchaseRequestApproval(selectedRequest.value.id,{...approvalForm});approvalOpen.value=false;message.success(approvalForm.decision==='APPROVED'?'采购申请已通过':'采购申请已驳回');await loadData();}catch(e:any){message.error(e.message||'审批失败');}finally{saving.value=false;}}
function formatMoney(v:number){return new Intl.NumberFormat('zh-CN',{style:'currency',currency:'CNY',minimumFractionDigits:2}).format(v||0);}
function formatTaxRate(v?:number){return `${Number(v??13).toFixed(2).replace(/\.?0+$/,'')}%`;}
function statusLabel(status:PurchaseRequestStatus){return ({DRAFT:'草稿',SUBMITTED:'待审批',APPROVED:'已通过',ORDERED:'已下单',RECEIVED:'已收货',CANCELLED:'已取消'} as Record<PurchaseRequestStatus,string>)[status]||status;}
function approvalStatusLabel(status:ApprovalStatus){return ({PENDING:'待审批',APPROVED:'已通过',REJECTED:'已驳回'} as Record<ApprovalStatus,string>)[status]||status;}
</script>
