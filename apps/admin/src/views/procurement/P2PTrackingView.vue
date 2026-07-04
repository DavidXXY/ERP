<template>
  <div class="page-stack">
    <a-card title="采购到付款全流程追踪">
      <template #extra><a-button @click="router.push('/procurement')">返回采购管理</a-button><a-button :loading="loading" @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button></template>
      <a-spin :spinning="loading">
        <a-row :gutter="[16, 16]" class="metric-row">
          <a-col :xs="12" :xl="6"><a-card><a-statistic title="采购申请" :value="requests.length" suffix="单" /></a-card></a-col>
          <a-col :xs="12" :xl="6"><a-card><a-statistic title="待转订单" :value="pendingOrder" suffix="单" :value-style="{color:pendingOrder>0?'#faad14':'#52c41a'}" /></a-card></a-col>
          <a-col :xs="12" :xl="6"><a-card><a-statistic title="待入库" :value="pendingReceipt" suffix="单" :value-style="{color:pendingReceipt>0?'#faad14':'#52c41a'}" /></a-card></a-col>
          <a-col :xs="12" :xl="6"><a-card><a-statistic title="全链路完成" :value="completed" suffix="单" :value-style="{color:'#52c41a'}" /></a-card></a-col>
        </a-row>
      </a-spin>
    </a-card>

    <a-card title="采购申请 P2P 状态" style="margin-top:16px">
      <a-table :data-source="p2pItems" :columns="p2pColumns" :loading="loading" :pagination="{pageSize:10}" :row-key="(r:any)=>r.id" size="middle" :scroll="{x:980}">
        <template #bodyCell="{column,record}">
          <template v-if="column.key==='code'"><strong>{{ record.code }}</strong><span class="table-subtitle">{{ record.materialName || '' }}</span></template>
          <template v-else-if="column.key==='amount'">{{ formatMoney(record.amount) }}</template>
          <template v-else-if="column.key==='p2p'" class="p2p-steps">
            <div class="p2p-row">
              <div v-for="(s,si) in record._steps" :key="si" class="p2p-dot">
                <div class="p2p-dot-circle" :class="'p2p-'+s.status"></div>
                <span class="p2p-dot-label">{{ s.label }}</span>
              </div>
            </div>
          </template>
        </template>
        <template #expandedRowRender="{ record }">
          <div style="padding:12px 0">
            <a-timeline>
              <a-timeline-item color="green"><strong>采购申请</strong><p style="margin:2px 0;color:#8c8c8c;font-size:12px">{{ record.code }} · {{ formatMoney(record.amount) }}</p></a-timeline-item>
              <a-timeline-item v-if="record._order" color="green">
                <strong>采购订单</strong>
                <p style="margin:2px 0;font-size:12px">{{ record._order.code }} · {{ formatMoney(record._order.totalAmount || record._order.amount) }}</p>
              </a-timeline-item>
              <a-timeline-item v-else color="orange"><strong>待生成采购订单</strong></a-timeline-item>
              <a-timeline-item v-if="record._receipt" color="green">
                <strong>到货入库</strong>
                <p style="margin:2px 0;font-size:12px">{{ record._receipt.receiptCode || record._receipt.code || '已入库' }} · {{ record._receipt.receivedDate?.slice(0,10) }}</p>
              </a-timeline-item>
              <a-timeline-item v-else-if="record._order" color="orange"><strong>等待入库</strong></a-timeline-item>
              <a-timeline-item v-if="record._payable" color="green">
                <strong>采购应付</strong>
                <p style="margin:2px 0;font-size:12px">{{ record._payable.code }} · {{ formatMoney(record._payable.amount) }}</p>
              </a-timeline-item>
              <a-timeline-item v-else-if="record._receipt" color="orange"><strong>等待生成应付</strong></a-timeline-item>
            </a-timeline>
          </div>
        </template>
      </a-table>
    </a-card>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from "vue"; import { useRouter } from "vue-router";
import { message } from "ant-design-vue"; import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { listPurchaseRequests, listPurchaseOrders, listGoodsReceipts, listProcurementPayables, type PurchaseRequest, type PurchaseOrder, type GoodsReceipt, type ProcurementPayable } from "@/api/procurement";
const router=useRouter(); const loading=ref(false);
const requests=ref<PurchaseRequest[]>([]); const orders=ref<PurchaseOrder[]>([]); const receipts=ref<GoodsReceipt[]>([]); const payables=ref<ProcurementPayable[]>([]);

const p2pItems=computed(()=>requests.value.map(r=>{
  const order=orders.value.find(o=>o.requestId===r.id);
  const receipt=order?receipts.value.find(rc=>rc.orderId===order.id):undefined;
  const payable=order?payables.value.find(p=>p.orderId===order.id):undefined;

  const stepStatus=(done:boolean,inProgress:boolean)=>(done?'finish':inProgress?'process':'wait');
  const steps=[
    {label:'申请',status:stepStatus(true,false)},
    {label:'订单',status:stepStatus(!!order,!order)},
    {label:'入库',status:stepStatus(!!receipt,!receipt&&!!order)},
    {label:'应付',status:stepStatus(!!payable,!payable&&!!receipt)},
  ];
  const amount=r.totalAmount||(r.unitPrice||0)*(r.quantity||0);
  return{...r,amount,_order:order,_receipt:receipt,_payable:payable,_steps:steps};
}));

const pendingOrder=computed(()=>p2pItems.value.filter(i=>!i._order).length);
const pendingReceipt=computed(()=>p2pItems.value.filter(i=>i._order&&!i._receipt).length);
const completed=computed(()=>p2pItems.value.filter(i=>i._payable).length);

const p2pColumns=[
  {title:'申请单',key:'code',width:200},{title:'金额',key:'amount',width:130},
  {title:'P2P 状态',key:'p2p',width:320},
  {title:'申请状态',dataIndex:'status',width:110},
];

onMounted(loadData);
async function loadData(){loading.value=true;try{[requests.value,orders.value,receipts.value,payables.value]=await Promise.all([listPurchaseRequests(),listPurchaseOrders(),listGoodsReceipts(),listProcurementPayables()]);}catch(e:any){message.error(e.message||'加载失败');}finally{loading.value=false;}}
function formatMoney(v:number){return new Intl.NumberFormat('zh-CN',{style:'currency',currency:'CNY'}).format(v||0);}
</script>
<style scoped>
.p2p-row{display:flex;align-items:center;gap:6px}.p2p-dot{display:flex;align-items:center;gap:4px}
.p2p-dot-circle{width:14px;height:14px;border-radius:50%;flex-shrink:0}
.p2p-dot-circle.p2p-finish{background:#52c41a}.p2p-dot-circle.p2p-process{background:#1890ff;animation:pulse 1.5s infinite}
.p2p-dot-circle.p2p-wait{background:#d9d9d9}.p2p-dot-circle.p2p-error{background:#ff4d4f}
.p2p-dot-label{font-size:11px;color:#595959;white-space:nowrap}
@keyframes pulse{0%{opacity:1}50%{opacity:.5}100%{opacity:1}}
</style>
