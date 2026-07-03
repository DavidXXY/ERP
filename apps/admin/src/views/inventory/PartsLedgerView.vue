<template>
  <div class="page-stack">
    <a-card>
      <template #title>库存台账</template>
      <template #extra><a-button @click="router.push('/inventory')">返回库存管理</a-button><a-button :loading="loading" @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button></template>
      <a-table :columns="partColumns" :data-source="parts" :loading="loading" :pagination="{pageSize:10}" :row-key="(r:any)=>r.id" :scroll="{x:1000}" size="middle">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'"><strong>{{ record.partName }}</strong><span class="table-subtitle">{{ record.partCode }} · {{ record.spec || '' }}</span></template>
          <template v-else-if="column.key === 'stock'">{{ record.quantity }}<span class="table-subtitle">{{ record.unit || '' }}</span></template>
          <template v-else-if="column.key === 'status'"><a-tag :color="record.quantity <= (record.safetyStock || 0) ? 'red' : 'green'">{{ record.quantity <= (record.safetyStock || 0) ? '低库存' : '正常' }}</a-tag></template>
          <template v-else-if="column.key === 'cost'">{{ formatMoney(record.unitCost) }}</template>
          <template v-else-if="column.key === 'value'">{{ formatMoney((record.unitCost || 0) * (record.quantity || 0)) }}</template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>
<script setup lang="ts">
import { onMounted, ref } from "vue"; import { useRouter } from "vue-router";
import { message } from "ant-design-vue"; import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { listInventoryParts, type InventoryPart } from "@/api/core-business";
const router=useRouter(); const loading=ref(false); const parts=ref<InventoryPart[]>([]);
const partColumns=[{title:'物料',key:'name',width:260},{title:'库存',key:'stock',width:130},{title:'状态',key:'status',width:100},{title:'库位',dataIndex:'location',width:110},{title:'单位成本',key:'cost',width:130},{title:'库存金额',key:'value',width:140}];
onMounted(loadData);
async function loadData(){loading.value=true;try{parts.value=await listInventoryParts();}catch(e:any){message.error(e.message||'加载失败');}finally{loading.value=false;}}
function formatMoney(v:number){return new Intl.NumberFormat('zh-CN',{style:'currency',currency:'CNY'}).format(v||0);}
</script>
