<template>
  <div class="page-stack">
    <a-card>
      <template #title>库存移动记录</template>
      <template #extra><a-button @click="router.push('/inventory')">返回库存管理</a-button><a-button :loading="loading" @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button></template>
      <a-table :columns="movementColumns" :data-source="movements" :loading="loading" :pagination="{pageSize:10}" :row-key="(r:any)=>r.id" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'"><a-tag :color="{INBOUND:'green',OUTBOUND:'red',RETURN:'blue',SCRAP:'orange',ADJUSTMENT:'purple'}[record.movementType]||'default'">{{ {INBOUND:'入库',OUTBOUND:'出库',RETURN:'退回',SCRAP:'报废',ADJUSTMENT:'调整'}[record.movementType]||record.movementType }}</a-tag></template>
          <template v-else-if="column.key === 'createdAt'">{{ record.createdAt?.slice(0,10) || '-' }}</template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>
<script setup lang="ts">
import { onMounted, ref } from "vue"; import { useRouter } from "vue-router";
import { message } from "ant-design-vue"; import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { listStockMovements, type StockMovement } from "@/api/inventory";
const router=useRouter(); const loading=ref(false); const movements=ref<StockMovement[]>([]);
const movementColumns=[{title:'类型',key:'type',width:100},{title:'数量',dataIndex:'quantity',width:100},{title:'来源单号',dataIndex:'sourceNo',width:150},{title:'备注',dataIndex:'remark'},{title:'时间',key:'createdAt',width:170}];
onMounted(loadData);
async function loadData(){loading.value=true;try{movements.value=await listStockMovements();}catch(e:any){message.error(e.message||'加载失败');}finally{loading.value=false;}}
</script>
