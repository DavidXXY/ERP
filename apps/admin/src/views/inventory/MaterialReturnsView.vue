<template>
  <div class="page-stack">
    <a-card>
      <template #title>退料管理</template>
      <template #extra><a-button @click="router.push('/inventory')">返回库存管理</a-button><a-button :loading="loading" @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button></template>
      <a-table :columns="returnColumns" :data-source="returns" :loading="loading" :pagination="{pageSize:10}" :row-key="(r:any)=>r.id" :scroll="{x:1100}" size="middle">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'code'"><strong>{{ record.code }}</strong><span class="table-subtitle">{{ record.returnDate || '' }}</span></template>
          <template v-if="column.key === 'project'">{{ record.projectName || record.projectId?.slice(0,8) || '-' }}</template>
          <template v-else-if="column.key === 'lines'"><span>{{ record.lines?.length || 0 }} 项</span></template>
          <template v-else-if="column.key === 'amount'">{{ formatMoney(record.totalCost || 0) }}</template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>
<script setup lang="ts">
import { onMounted, ref } from "vue"; import { useRouter } from "vue-router";
import { message } from "ant-design-vue"; import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { listMaterialReturns, type MaterialReturn } from "@/api/core-business";
const router=useRouter(); const loading=ref(false); const returns=ref<MaterialReturn[]>([]);
const returnColumns=[{title:'退料单',key:'code',width:240},{title:'项目',key:'project',width:240},{title:'明细',key:'lines',width:100},{title:'退料日期',dataIndex:'returnDate',width:120},{title:'经办人',dataIndex:'handlerName',width:120},{title:'冲回成本',key:'amount',width:140}];
onMounted(loadData);
async function loadData(){loading.value=true;try{returns.value=await listMaterialReturns();}catch(e:any){message.error(e.message||'加载失败');}finally{loading.value=false;}}
function formatMoney(v:number){return new Intl.NumberFormat('zh-CN',{style:'currency',currency:'CNY'}).format(v||0);}
</script>
