<template>
  <div class="page-stack">
    <a-card>
      <template #title>到货入库</template>
      <template #extra><a-button @click="router.push('/procurement')">返回采购管理</a-button><a-button :loading="loading" @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button></template>
      <a-table :columns="receiptColumns" :data-source="receipts" :loading="loading" :pagination="{pageSize:10}" :row-key="(r:any)=>r.id" :scroll="{x:1100}">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'receipt'"><strong>{{ record.receiptCode || record.id?.slice(0,8) }}</strong><span class="table-subtitle">{{ record.orderCode || '' }}</span></template>
          <template v-else-if="column.key === 'supplier'">{{ record.supplierName || '-' }}</template>
          <template v-else-if="column.key === 'amount'">{{ formatMoney(record.amount) }}</template>
          <template v-else-if="column.key === 'date'">{{ record.receivedAt?.slice(0,10) || '-' }}</template>
          <template v-else-if="column.key === 'status'"><a-tag color="green">已入库</a-tag></template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>
<script setup lang="ts">
import { onMounted, ref } from "vue"; import { useRouter } from "vue-router";
import { message } from "ant-design-vue"; import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { listGoodsReceipts, type GoodsReceipt } from "@/api/procurement";
const router=useRouter(); const loading=ref(false); const receipts=ref<GoodsReceipt[]>([]);
const receiptColumns=[{title:'入库单',key:'receipt',width:220},{title:'供应商',key:'supplier',width:200},{title:'金额',key:'amount',width:140},{title:'入库日期',key:'date',width:120},{title:'状态',key:'status',width:90}];
onMounted(loadData);
async function loadData(){loading.value=true;try{receipts.value=await listGoodsReceipts();}catch(e:any){message.error(e.message||'加载失败');}finally{loading.value=false;}}
function formatMoney(v:number){return new Intl.NumberFormat('zh-CN',{style:'currency',currency:'CNY',minimumFractionDigits:2}).format(v||0);}
</script>
