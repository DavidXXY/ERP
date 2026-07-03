<template>
  <div class="page-stack">
    <a-card>
      <template #title>操作审计</template>
      <template #extra><a-space><a-button @click="goBack">返回办公室</a-button><a-button :loading="loading" @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button></a-space></template>
      <a-table :columns="auditColumns" :data-source="audits" :loading="loading" :pagination="{pageSize:10}" :row-key="(r:any)=>r.id" :scroll="{x:1000}">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'"><a-tag :color="record.responseStatus < 400 ? 'green' : 'red'">{{ record.responseStatus }}</a-tag></template>
          <template v-else-if="column.key === 'created'">{{ formatDateTime(record.createdAt) }}</template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>
<script setup lang="ts">
import { onMounted, ref } from "vue"; import { useRouter } from "vue-router";
import { message } from "ant-design-vue"; import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { listAudits, type AuditRecord } from "@/api/office";
const router=useRouter(); const loading=ref(false); const audits=ref<AuditRecord[]>([]);
const auditColumns=[{title:'时间',key:'created',width:190},{title:'用户',dataIndex:'username',width:130},{title:'方法',dataIndex:'httpMethod',width:90},{title:'访问路径',dataIndex:'requestPath'},{title:'状态',key:'status',width:90},{title:'耗时(ms)',dataIndex:'durationMs',width:110},{title:'来源IP',dataIndex:'clientIp',width:150}];
onMounted(loadData);
async function loadData(){loading.value=true;try{audits.value=await listAudits();}catch(error){message.error(error instanceof Error?error.message:'数据加载失败');}finally{loading.value=false;}}
function goBack(){router.push('/office');}
function formatDateTime(v?:string){return v?new Date(v).toLocaleString('zh-CN',{hour12:false}):'-';}
</script>
