<template>
  <div class="page-stack">
    <a-card title="库存价值分析" style="margin-bottom:16px">
      <template #extra><a-button @click="router.push('/inventory')">返回库存管理</a-button><a-button :loading="loading" @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button></template>
      <a-spin :spinning="loading">
        <a-row :gutter="[16, 16]" class="metric-row">
          <a-col :xs="12" :xl="6"><a-card><a-statistic title="库存总价值" :value="totalValue" :formatter="moneyFormatter" /></a-card></a-col>
          <a-col :xs="12" :xl="6"><a-card><a-statistic title="物料种类" :value="parts.length" suffix="种" /></a-card></a-col>
          <a-col :xs="12" :xl="6"><a-card><a-statistic title="低库存物料" :value="lowStockCount" suffix="种" :value-style="{color:lowStockCount>0?'#ff4d4f':'#52c41a'}" /></a-card></a-col>
          <a-col :xs="12" :xl="6"><a-card><a-statistic title="需补货建议" :value="replenishmentSuggestions.length" suffix="项" :value-style="{color:replenishmentSuggestions.length>0?'#fa8c16':'#52c41a'}" /></a-card></a-col>
        </a-row>

        <a-row :gutter="12" style="margin-top:12px">
          <a-col :xs="24" :lg="12">
            <a-card title="库存按仓库分布" size="small">
              <div v-if="byWarehouse.length===0" class="chart-empty">暂无数据</div>
              <div v-else class="dist-rows">
                <div v-for="item in byWarehouse" :key="item.name" class="dist-row">
                  <span class="dist-label">{{ item.name }}</span>
                  <div class="dist-track"><div class="dist-fill" :style="{width:distPercent(item.value,byWarehouse)+'%',background:'#52c41a'}"></div></div>
                  <span class="dist-money">{{ formatMoney(item.value) }}</span>
                </div>
              </div>
            </a-card>
          </a-col>
          <a-col :xs="24" :lg="12">
            <a-card title="低库存预警" size="small">
              <div v-if="lowStockItems.length===0" style="text-align:center;padding:24px;color:#52c41a">✅ 暂无低库存物料</div>
              <a-table v-else size="small" :data-source="lowStockItems" :columns="lowStockColumns" :pagination="false" :row-key="(r:any)=>r.partCode">
                <template #bodyCell="{column,record}">
                  <template v-if="column.key==='name'"><strong>{{ record.partName }}</strong><span class="table-subtitle">{{ record.spec||'' }}</span></template>
                  <template v-else-if="column.key==='stock'">{{ record.stockQty }}{{ record.unit }}</template>
                  <template v-else-if="column.key==='value'">{{ formatMoney(record.stockQty*(record.unitCost||0)) }}</template>
                </template>
              </a-table>
            </a-card>
          </a-col>
        </a-row>

        <a-card title="智能补货建议" size="small" style="margin-top:12px">
          <a-table size="small" :data-source="replenishmentSuggestions" :columns="replenishmentColumns" :pagination="{pageSize:6}" :row-key="(r:any)=>r.partId" :scroll="{x:920}">
            <template #bodyCell="{column,record}">
              <template v-if="column.key==='part'"><strong>{{ record.partName }}</strong><span class="table-subtitle">{{ record.partCode || '-' }} · {{ record.model || '无规格' }}</span></template>
              <template v-else-if="column.key==='stock'">{{ record.stockQty }} / {{ record.safetyQty }}</template>
              <template v-else-if="column.key==='outbound'">{{ record.recentOutboundQty }}</template>
              <template v-else-if="column.key==='suggested'"><strong>{{ record.suggestedQty }}</strong></template>
              <template v-else-if="column.key==='priority'"><a-tag :color="priorityColor(record.priority)">{{ priorityLabel(record.priority) }}</a-tag></template>
            </template>
            <template #emptyText>暂无补货建议</template>
          </a-table>
        </a-card>
      </a-spin>
    </a-card>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from "vue"; import { useRouter } from "vue-router";
import { message } from "ant-design-vue"; import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { listInventoryParts, listReplenishmentSuggestions, type InventoryPart, type ReplenishmentSuggestion } from "@/api/inventory";
const router=useRouter(); const loading=ref(false); const parts=ref<InventoryPart[]>([]); const replenishmentSuggestions=ref<ReplenishmentSuggestion[]>([]);

const totalValue=computed(()=>parts.value.reduce((s,p)=>s+Number(p.stockQty||0)*Number(p.unitCost||0),0));
const lowStockCount=computed(()=>parts.value.filter(p=>Number(p.stockQty||0)<=Number(p.safetyQty||0)).length);
const lowStockItems=computed(()=>parts.value.filter(p=>Number(p.stockQty||0)<=Number(p.safetyQty||0)).sort((a,b)=>Number(a.stockQty||0)-Number(b.stockQty||0)));
const lowStockColumns=[{title:'物料',key:'name',width:260},{title:'当前库存',key:'stock',width:120},{title:'安全库存',dataIndex:'safetyQty',width:100},{title:'价值',key:'value',width:130}];
const replenishmentColumns=[
  {title:'物料',key:'part',width:260},{title:'库存/安全',key:'stock',width:120},{title:'近30天消耗',key:'outbound',width:120},
  {title:'建议补货',key:'suggested',width:120},{title:'优先级',key:'priority',width:100},{title:'原因',dataIndex:'reason',width:260},
];

const byWarehouse=computed(()=>{
  const map=new Map<string,number>();
  parts.value.forEach(p=>{
    const wh=p.location||'未分类';
    map.set(wh,(map.get(wh)||0)+Number(p.stockQty||0)*Number(p.unitCost||0));
  });
  return Array.from(map.entries()).map(([name,value])=>({name,value})).sort((a,b)=>b.value-a.value);
});

function distPercent(value:number,data:{value:number}[]){
  const max=Math.max(...data.map(d=>d.value),1);
  return Math.round((value/max)*100);
}

onMounted(loadData);
async function loadData(){loading.value=true;try{const [partRows,suggestions]=await Promise.all([listInventoryParts(),listReplenishmentSuggestions()]);parts.value=partRows;replenishmentSuggestions.value=suggestions;}catch(e:any){message.error(e.message||'加载失败');}finally{loading.value=false;}}
function formatMoney(v:number){return new Intl.NumberFormat('zh-CN',{style:'currency',currency:'CNY'}).format(v||0);}
function moneyFormatter({value}:{value:number|string}){return formatMoney(Number(value));}
function priorityLabel(v:string){return ({HIGH:'高',MEDIUM:'中',LOW:'低'} as Record<string,string>)[v]||v;}
function priorityColor(v:string){return ({HIGH:'red',MEDIUM:'orange',LOW:'green'} as Record<string,string>)[v]||'default';}
</script>
<style scoped>
.chart-empty{text-align:center;padding:32px 0;color:#8c8c8c}.dist-rows{display:flex;flex-direction:column;gap:8px}
.dist-row{display:flex;align-items:center;gap:10px}.dist-label{width:80px;font-size:13px;color:#333;flex-shrink:0;text-align:right}
.dist-track{flex:1;height:22px;background:#f5f5f5;border-radius:4px;overflow:hidden}
.dist-fill{height:100%;border-radius:4px;transition:width 0.4s;min-width:0}
.dist-money{font-size:12px;color:#595959;width:80px;text-align:right;flex-shrink:0}
</style>
