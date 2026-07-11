<template>
  <div class="page-stack">
    <a-card>
      <template #title>费用报销</template>
      <template #extra><a-space><a-button @click="goBack">返回办公室</a-button><a-button :loading="loading" @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button></a-space></template>
      <a-space wrap class="table-toolbar"><a-button v-if="auth.can('office:expense:create')" type="primary" @click="openExpense"><template #icon><PlusOutlined /></template>新增报销</a-button></a-space>
      <a-table :columns="expenseColumns" :data-source="expenses" :loading="loading" :pagination="{pageSize:10}" :row-key="(r:any)=>r.id" :scroll="{x:1180}">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'expense'"><strong>{{ record.code }}</strong><span class="table-subtitle">{{ record.description }}</span></template>
          <template v-else-if="column.key === 'binding'">{{ record.projectCode || record.workOrderCode || '管理费用' }}</template>
          <template v-else-if="column.key === 'type'">{{ expenseTypeLabel(record.expenseType) }}</template>
          <template v-else-if="column.key === 'amount'"><strong>{{ formatMoney(record.amount) }}</strong></template>
          <template v-else-if="column.key === 'status'"><a-tag :color="expenseStatusColor(record.status)">{{ expenseStatusLabel(record.status) }}</a-tag></template>
        </template>
      </a-table>
    </a-card>
    <a-modal v-model:open="expenseOpen" title="新增费用报销" width="720px" :confirm-loading="saving" @ok="handleExpense">
      <a-form ref="expenseFormRef" :model="expenseForm" :rules="expenseRules" layout="vertical">
        <a-row :gutter="16"><a-col :span="12"><a-form-item label="报销人" name="claimantId"><a-select v-model:value="expenseForm.claimantId" show-search option-filter-prop="label" :options="userOptions" @change="onClaimantChange" /></a-form-item></a-col><a-col :span="12"><a-form-item label="费用类型" name="expenseType"><a-select v-model:value="expenseForm.expenseType" :options="expenseTypeOptions" /></a-form-item></a-col></a-row>
        <a-row :gutter="16"><a-col :span="12"><a-form-item label="绑定项目"><a-select v-model:value="expenseForm.projectId" allow-clear show-search option-filter-prop="label" :options="projectOptions" /></a-form-item></a-col><a-col :span="12"><a-form-item label="绑定工单"><a-select v-model:value="expenseForm.workOrderId" allow-clear show-search option-filter-prop="label" :options="workOrderOptions" /></a-form-item></a-col></a-row>
        <a-row :gutter="16"><a-col :span="12"><a-form-item label="金额" name="amount"><a-input-number v-model:value="expenseForm.amount" :min="0" :precision="2" class="full-input" /></a-form-item></a-col><a-col :span="12"><a-form-item label="发生日期" name="expenseDate"><a-input v-model:value="expenseForm.expenseDate" type="date" /></a-form-item></a-col></a-row>
        <a-form-item label="说明" name="description"><a-textarea v-model:value="expenseForm.description" :rows="2" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { message } from "ant-design-vue";
import PlusOutlined from "@ant-design/icons-vue/PlusOutlined"; import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { createExpense, getOfficeOverview, getOfficeReferences, listExpenses, type Expense, type ExpenseStatus, type ExpenseType } from "@/api/office";
import { useAuthStore } from "@/stores/auth";
const auth=useAuthStore(); const router=useRouter(); const loading=ref(false); const saving=ref(false);
const expenses=ref<Expense[]>([]); const references=reactive({users:[],projects:[],workOrders:[]} as any);
const expenseOpen=ref(false); const expenseFormRef=ref();
const expenseForm=reactive({code:'',claimantId:undefined as string|undefined,claimantName:'',projectId:undefined as string|undefined,workOrderId:undefined as string|undefined,expenseType:'TRAVEL' as ExpenseType,amount:0,expenseDate:today(),description:''});
const expenseColumns=[{title:'报销单',key:'expense',width:280},{title:'报销人',dataIndex:'claimantName',width:130},{title:'类型',key:'type',width:110},{title:'绑定业务',key:'binding',width:180},{title:'发生日期',dataIndex:'expenseDate',width:120},{title:'金额',key:'amount',width:140},{title:'状态',key:'status',width:130}];
const expenseRules={claimantId:[{required:true}],expenseType:[{required:true}],amount:[{required:true}],expenseDate:[{required:true}],description:[{required:true}]};
const userOptions=computed(()=>references.users.filter((i:any)=>i.enabled).map((i:any)=>({label:i.displayName,value:i.id})));
const projectOptions=computed(()=>references.projects.map((i:any)=>({label:i.code+' · '+i.name,value:i.id})));
const workOrderOptions=computed(()=>references.workOrders.map((i:any)=>({label:i.code+' · '+i.title,value:i.id})));
const expenseTypeOptions=[{label:'差旅',value:'TRAVEL'},{label:'交通',value:'TRANSPORT'},{label:'住宿',value:'ACCOMMODATION'},{label:'工具采购',value:'TOOL'},{label:'其他',value:'OTHER'}];
onMounted(loadData);
async function loadData(){loading.value=true;try{const [expenseData,referenceData]=await Promise.all([listExpenses(),getOfficeReferences()]);expenses.value=expenseData||[];references.users=referenceData.users||[];references.projects=referenceData.projects||[];references.workOrders=referenceData.workOrders||[];}catch(error){message.error(error instanceof Error?error.message:'数据加载失败');}finally{loading.value=false;}}
function goBack(){router.push('/office');}
function openExpense(){Object.assign(expenseForm,{code:generateCode('BX'),claimantId:auth.user?.id,claimantName:auth.user?.displayName||'',projectId:undefined,workOrderId:undefined,expenseType:'TRAVEL',amount:0,expenseDate:today(),description:''});expenseOpen.value=true;}
function onClaimantChange(id:string){expenseForm.claimantName=references.users.find((i:any)=>i.id===id)?.displayName||'';}
async function handleExpense(){await expenseFormRef.value?.validate();saving.value=true;try{await createExpense({...expenseForm});expenseOpen.value=false;message.success('报销单已提交审批');await loadData();}catch(error){message.error(error instanceof Error?error.message:'报销提交失败');}finally{saving.value=false;}}
function generateCode(prefix:string){const d=new Date();return prefix+'-'+d.getFullYear()+String(d.getMonth()+1).padStart(2,'0')+String(d.getDate()).padStart(2,'0')+'-'+String(d.getHours()).padStart(2,'0')+String(d.getMinutes()).padStart(2,'0');}
function today(){const d=new Date();return d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0')+'-'+String(d.getDate()).padStart(2,'0');}
function expenseTypeLabel(v:ExpenseType){return({TRAVEL:'差旅',TRANSPORT:'交通',ACCOMMODATION:'住宿',TOOL:'工具采购',OTHER:'其他'} as Record<ExpenseType,string>)[v];}
function expenseStatusLabel(v:ExpenseStatus){return({PENDING_APPROVAL:'待审批',APPROVED:'已通过',REJECTED:'已驳回',PAID:'已付款'} as Record<ExpenseStatus,string>)[v];}
function expenseStatusColor(v:ExpenseStatus){return({PENDING_APPROVAL:'orange',APPROVED:'blue',REJECTED:'red',PAID:'green'} as Record<ExpenseStatus,string>)[v];}
function formatMoney(v:number){return new Intl.NumberFormat('zh-CN',{style:'currency',currency:'CNY',minimumFractionDigits:2}).format(v||0);}
</script>
