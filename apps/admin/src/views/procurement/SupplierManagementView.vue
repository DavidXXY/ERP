<template>
  <div class="page-stack">
    <a-card>
      <template #title>供应商</template>
      <template #extra><a-button @click="router.push('/procurement')">返回采购管理</a-button><a-button :loading="loading" @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button></template>
      <a-space class="table-toolbar"><a-button v-if="auth.can('procurement:supplier:create')" type="primary" @click="openCreate"><template #icon><PlusOutlined /></template>新增供应商</a-button></a-space>
      <a-table :columns="supplierColumns" :data-source="suppliers" :loading="loading" :pagination="{pageSize:10}" :row-key="(r:any)=>r.id" :scroll="{x:1100}">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'supplier'"><strong>{{ record.code }}</strong><span class="table-subtitle">{{ record.name }}</span></template>
          <template v-else-if="column.key === 'contact'">{{ record.contactName || '-' }}<br><span class="table-subtitle">{{ record.contactPhone || '' }}</span></template>
          <template v-else-if="column.key === 'status'"><a-tag :color="record.riskStatus === 'NORMAL' ? 'green' : record.riskStatus === 'WATCHLIST' ? 'orange' : 'red'">{{ {NORMAL:'正常',WATCHLIST:'关注',BLOCKED:'冻结'}[record.riskStatus] || record.riskStatus }}</a-tag></template>
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
import { onMounted, reactive, ref } from "vue"; import { useRouter } from "vue-router";
import { message } from "ant-design-vue"; import PlusOutlined from "@ant-design/icons-vue/PlusOutlined"; import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { listSuppliers, createSupplier, type Supplier } from "@/api/core-business"; import { useAuthStore } from "@/stores/auth";
const auth=useAuthStore(); const router=useRouter(); const loading=ref(false); const saving=ref(false); const suppliers=ref<Supplier[]>([]);
const createOpen=ref(false); const formRef=ref();
const form=reactive({code:'',name:'',contactName:'',contactPhone:'',remark:''});
const rules={code:[{required:true}],name:[{required:true}]};
const supplierColumns=[{title:'供应商',key:'supplier',width:240},{title:'联系人',key:'contact',width:200},{title:'状态',key:'status',width:100}];
onMounted(loadData);
async function loadData(){loading.value=true;try{suppliers.value=await listSuppliers();}catch(e:any){message.error(e.message||'加载失败');}finally{loading.value=false;}}
function openCreate(){Object.assign(form,{code:generateCode('GYS'),name:'',contactName:'',contactPhone:'',remark:''});createOpen.value=true;}
async function handleCreate(){await formRef.value?.validate();saving.value=true;try{await createSupplier({...form});createOpen.value=false;message.success('供应商已创建');await loadData();}catch(e:any){message.error(e.message||'创建失败');}finally{saving.value=false;}}
function generateCode(p:string){const d=new Date();return p+'-'+d.getFullYear()+String(d.getMonth()+1).padStart(2,'0')+String(d.getDate()).padStart(2,'0')+'-'+String(d.getHours()).padStart(2,'0')+String(d.getMinutes()).padStart(2,'0');}
</script>
