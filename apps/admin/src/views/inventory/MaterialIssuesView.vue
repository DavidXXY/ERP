<template>
  <div class="page-stack">
    <a-card>
      <template #title>领料管理</template>
      <template #extra><a-button @click="router.push('/inventory')">返回库存管理</a-button><a-button :loading="loading" @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button></template>
      <a-table :columns="issueColumns" :data-source="issues" :loading="loading" :pagination="{pageSize:10}" :row-key="(r:any)=>r.id" :scroll="{x:1250}" size="middle">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'code'"><strong>{{ record.code }}</strong><span class="table-subtitle">{{ record.issueDate || '' }}</span></template>
          <template v-if="column.key === 'project'">{{ record.projectName || record.projectId?.slice(0,8) || '-' }}</template>
          <template v-else-if="column.key === 'lines'"><span>{{ record.lines?.length || 0 }} 项</span></template>
          <template v-else-if="column.key === 'amount'">{{ formatMoney(record.totalCost || 0) }}</template>
          <template v-else-if="column.key === 'status'"><a-tag :color="statusColor(record.status, {POSTED:'blue',PARTIAL_RETURNED:'orange',FULLY_RETURNED:'green'}, 'default')">{{ statusLabel(record.status, {POSTED:'已领用',PARTIAL_RETURNED:'部分退回',FULLY_RETURNED:'全部退回'}) }}</a-tag></template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>
<script setup lang="ts">
import { onMounted, ref } from "vue"; import { useRouter } from "vue-router";
import { message } from "ant-design-vue"; import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { listMaterialIssues, type MaterialIssue } from "@/api/inventory";
import { statusLabel, statusColor } from "@/utils/status-mapper";
const router=useRouter(); const loading=ref(false); const issues=ref<MaterialIssue[]>([]);
const issueColumns=[{title:'领料单',key:'code',width:210},{title:'项目',key:'project',width:240},{title:'用途',dataIndex:'purpose',width:220},{title:'明细',key:'lines',width:100},{title:'材料成本',key:'amount',width:140},{title:'状态',key:'status',width:120}];
onMounted(loadData);
async function loadData(){loading.value=true;try{issues.value=await listMaterialIssues();}catch(e:any){message.error(e.message||'加载失败');}finally{loading.value=false;}}
function formatMoney(v:number){return new Intl.NumberFormat('zh-CN',{style:'currency',currency:'CNY'}).format(v||0);}
</script>
