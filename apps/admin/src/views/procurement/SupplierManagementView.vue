<template>
  <div class="page-stack">
    <a-card>
      <template #title>供应商</template>
      <template #extra><a-button @click="router.push('/procurement')">返回采购管理</a-button><a-button :loading="loading" @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button></template>
      <a-space class="table-toolbar"><a-button v-if="auth.can('procurement:supplier:create')" type="primary" @click="openCreate"><template #icon><PlusOutlined /></template>新增供应商</a-button></a-space>
      <section class="supplier-score-panel">
        <div class="supplier-score-head">
          <div>
            <h3>供应商评分</h3>
            <p>按风险状态、联系人资料、结算条款做基础评分，用于采购准入和关注名单。</p>
          </div>
          <a-tag :color="watchSupplierCount ? 'orange' : 'green'">关注 {{ watchSupplierCount }} 家</a-tag>
        </div>
        <div class="supplier-score-grid">
          <button v-for="item in supplierScoreCards" :key="item.label" class="supplier-score-card" type="button">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <em>{{ item.hint }}</em>
          </button>
        </div>
      </section>
      <a-table :columns="supplierColumns" :data-source="suppliers" :loading="loading" :pagination="{pageSize:10}" :row-key="(r:any)=>r.id" :scroll="{x:1100}">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'supplier'"><strong>{{ record.code }}</strong><span class="table-subtitle">{{ record.name }}</span></template>
          <template v-else-if="column.key === 'contact'">{{ record.contactName || '-' }}<br><span class="table-subtitle">{{ record.contactPhone || '' }}</span></template>
          <template v-else-if="column.key === 'score'">
            <a-progress :percent="supplierScore(record)" size="small" :stroke-color="supplierScore(record) >= 80 ? '#52c41a' : supplierScore(record) >= 60 ? '#faad14' : '#ff4d4f'" />
            <span class="table-subtitle">{{ supplierGrade(record) }}</span>
          </template>
          <template v-else-if="column.key === 'status'"><a-tag :color="record.riskStatus === 'NORMAL' ? 'green' : record.riskStatus === 'WATCHLIST' ? 'orange' : 'red'">{{ ({NORMAL:'正常',WATCHLIST:'关注',BLOCKED:'冻结'} as any)[record.riskStatus] || record.riskStatus }}</a-tag></template>
        </template>
      </a-table>
    </a-card>
    <a-modal v-model:open="createOpen" title="新增供应商" :confirm-loading="saving" @ok="handleCreate">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16"><a-col :span="12"><a-form-item label="供应商编码" name="code"><a-input v-model:value="form.code" /></a-form-item></a-col><a-col :span="12"><a-form-item label="供应商名称" name="name"><a-input v-model:value="form.name" /></a-form-item></a-col></a-row>
        <a-row :gutter="16"><a-col :span="12"><a-form-item label="联系人"><a-input v-model:value="form.contactName" /></a-form-item></a-col><a-col :span="12"><a-form-item label="联系电话"><a-input v-model:value="form.contactPhone" /></a-form-item></a-col></a-row>
        <a-form-item label="备注"><a-textarea v-model:value="form.remark" :rows="2" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue"; import { useRouter } from "vue-router";
import { message } from "ant-design-vue"; import PlusOutlined from "@ant-design/icons-vue/PlusOutlined"; import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { listSuppliers, createSupplier, type Supplier } from "@/api/procurement"; import { useAuthStore } from "@/stores/auth";
const auth=useAuthStore(); const router=useRouter(); const loading=ref(false); const saving=ref(false); const suppliers=ref<Supplier[]>([]);
const createOpen=ref(false); const formRef=ref();
const form=reactive({code:'',name:'',contactName:'',contactPhone:'',remark:''});
const rules={code:[{required:true}],name:[{required:true}]};
const watchSupplierCount=computed(()=>suppliers.value.filter((item)=>item.riskStatus!=='NORMAL'||supplierScore(item)<70).length);
const supplierScoreCards=computed(()=>[
  {label:'供应商总数',value:`${suppliers.value.length} 家`,hint:'已建档供应商'},
  {label:'优选供应商',value:`${suppliers.value.filter((item)=>supplierScore(item)>=80).length} 家`,hint:'评分80以上'},
  {label:'关注名单',value:`${watchSupplierCount.value} 家`,hint:'风险或资料不完整'},
  {label:'冻结供应商',value:`${suppliers.value.filter((item)=>item.riskStatus==='BLOCKED').length} 家`,hint:'禁止采购下单'},
]);
const supplierColumns=[{title:'供应商',key:'supplier',width:240},{title:'联系人',key:'contact',width:200},{title:'评分',key:'score',width:180},{title:'状态',key:'status',width:100}];
onMounted(loadData);
async function loadData(){loading.value=true;try{const result=await listSuppliers(0,999);suppliers.value=result.content;}catch(e:any){message.error(e.message||'加载失败');}finally{loading.value=false;}}
function openCreate(){Object.assign(form,{code:generateCode('GYS'),name:'',contactName:'',contactPhone:'',remark:''});createOpen.value=true;}
async function handleCreate(){await formRef.value?.validate();saving.value=true;try{await createSupplier({...form});createOpen.value=false;message.success('供应商已创建');await loadData();}catch(e:any){message.error(e.message||'创建失败');}finally{saving.value=false;}}
function generateCode(p:string){const d=new Date();return p+'-'+d.getFullYear()+String(d.getMonth()+1).padStart(2,'0')+String(d.getDate()).padStart(2,'0')+'-'+String(d.getHours()).padStart(2,'0')+String(d.getMinutes()).padStart(2,'0');}
function supplierScore(record:Supplier){let score=100;if(record.riskStatus==='WATCHLIST')score-=25;if(record.riskStatus==='BLOCKED')score-=60;if(!record.contactName)score-=8;if(!record.phone&&!((record as any).contactPhone))score-=8;if(!record.settlementTerms)score-=6;return Math.max(0,score);}
function supplierGrade(record:Supplier){const score=supplierScore(record);return score>=80?'优选':score>=60?'观察':'高风险';}
</script>
<style scoped>
.supplier-score-panel{margin-bottom:14px;padding:14px;border:1px solid #e5e7eb;background:#fbfcfe}.supplier-score-head{display:flex;justify-content:space-between;gap:16px;align-items:flex-start;margin-bottom:12px}.supplier-score-head h3{margin:0;color:#111827;font-size:15px;font-weight:600}.supplier-score-head p{margin:4px 0 0;color:#6b7280;font-size:12px}
.supplier-score-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:10px}.supplier-score-card{display:grid;gap:4px;min-width:0;padding:12px;border:1px solid #eef2f7;border-radius:8px;background:#fff;text-align:left}.supplier-score-card span{color:#667085;font-size:12px}.supplier-score-card strong{color:#101828;font-size:20px}.supplier-score-card em{overflow:hidden;color:#98a2b3;font-size:12px;font-style:normal;text-overflow:ellipsis;white-space:nowrap}
@media(max-width:900px){.supplier-score-head{flex-direction:column}.supplier-score-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}
</style>
