<template>
  <div class="page-stack">
    <a-card title="OA协同中心">
      <template #extra><a-button :loading="loading" @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button></template>
      <a-row :gutter="[16, 16]" class="metric-row">
        <a-col :xs="12" :xl="4"><a-statistic title="待审批" :value="overview.pendingApprovals" /></a-col>
        <a-col :xs="12" :xl="4"><a-statistic title="待批报销" :value="overview.pendingExpenseAmount" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :xl="4"><a-statistic title="已批报销" :value="overview.approvedExpenseAmount" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :xl="4"><a-statistic title="执行中外包" :value="overview.activeOutsourceOrders" /></a-col>
        <a-col :xs="12" :xl="4"><a-statistic title="未读消息" :value="overview.unreadNotifications" /></a-col>
        <a-col :xs="12" :xl="4"><a-statistic title="归档文件" :value="overview.documentCount" /></a-col>
      </a-row>
    </a-card>

    <a-card>
      <a-tabs v-model:active-key="activeTab">
        <a-tab-pane key="approvals" tab="审批中心">
          <a-space wrap class="table-toolbar"><a-button v-if="auth.can('office:approval:create')" type="primary" @click="openApprovalCreate"><template #icon><PlusOutlined /></template>发起审批</a-button></a-space>
          <a-table :columns="approvalColumns" :data-source="approvals" :loading="loading" :pagination="{ pageSize: 8 }" :row-key="(item: Approval) => item.id" :scroll="{ x: 1250 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'approval'"><strong>{{ record.code }}</strong><span class="table-subtitle">{{ record.title }}</span></template>
              <template v-else-if="column.key === 'type'">{{ approvalTypeLabel(record.approvalType) }}</template>
              <template v-else-if="column.key === 'amount'">{{ formatMoney(record.amount) }}</template>
              <template v-else-if="column.key === 'status'"><a-tag :color="approvalStatusColor(record.status)">{{ approvalStatusLabel(record.status) }}</a-tag></template>
              <template v-else-if="column.key === 'result'"><template v-if="record.approverName">{{ record.approverName }}<span class="table-subtitle">{{ record.approvalComment }}</span></template><span v-else class="muted">待处理</span></template>
              <template v-else-if="column.key === 'action'"><a-button v-if="record.status === 'PENDING' && auth.can('office:approval:process')" type="link" size="small" @click="openProcess(record)">处理审批</a-button></template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="expenses" tab="费用报销">
          <a-space wrap class="table-toolbar"><a-button v-if="auth.can('office:expense:create')" type="primary" @click="openExpense"><template #icon><PlusOutlined /></template>新增报销</a-button></a-space>
          <a-table :columns="expenseColumns" :data-source="expenses" :loading="loading" :pagination="{ pageSize: 8 }" :row-key="(item: Expense) => item.id" :scroll="{ x: 1120 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'expense'"><strong>{{ record.code }}</strong><span class="table-subtitle">{{ record.description }}</span></template>
              <template v-else-if="column.key === 'binding'">{{ record.projectCode || record.workOrderCode || '管理费用' }}</template>
              <template v-else-if="column.key === 'type'">{{ expenseTypeLabel(record.expenseType) }}</template>
              <template v-else-if="column.key === 'amount'"><strong>{{ formatMoney(record.amount) }}</strong></template>
              <template v-else-if="column.key === 'status'"><a-tag :color="expenseStatusColor(record.status)">{{ expenseStatusLabel(record.status) }}</a-tag></template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="outsourcing" tab="外包服务">
          <a-space wrap class="table-toolbar"><a-button v-if="auth.can('office:outsource:create')" type="primary" @click="openOutsource"><template #icon><PlusOutlined /></template>新增外包</a-button></a-space>
          <a-table :columns="outsourceColumns" :data-source="outsourcing" :loading="loading" :pagination="{ pageSize: 8 }" :row-key="(item: Outsource) => item.id" :scroll="{ x: 1300 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'order'"><strong>{{ record.code }}</strong><span class="table-subtitle">{{ record.serviceType }}</span></template>
              <template v-else-if="column.key === 'binding'">{{ record.projectCode || record.workOrderCode || '-' }}</template>
              <template v-else-if="column.key === 'amount'"><strong>{{ formatMoney(record.amount) }}</strong></template>
              <template v-else-if="column.key === 'status'"><a-tag :color="outsourceStatusColor(record.status)">{{ outsourceStatusLabel(record.status) }}</a-tag></template>
              <template v-else-if="column.key === 'action'"><a-button v-if="record.status === 'APPROVED' && auth.can('office:outsource:complete')" type="link" size="small" @click="openOutsourceComplete(record)">验收结算</a-button></template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="documents" tab="电子档案">
          <a-space wrap class="table-toolbar"><a-button v-if="auth.can('office:document:upload')" type="primary" @click="openUpload"><template #icon><UploadOutlined /></template>上传档案</a-button></a-space>
          <a-table :columns="documentColumns" :data-source="documents" :loading="loading" :pagination="{ pageSize: 8 }" :row-key="(item: DocumentRecord) => item.id">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'file'"><strong>{{ record.fileName }}</strong><span class="table-subtitle">{{ record.contentType || '未知格式' }}</span></template>
              <template v-else-if="column.key === 'size'">{{ formatFileSize(record.sizeBytes) }}</template>
              <template v-else-if="column.key === 'created'">{{ formatDateTime(record.createdAt) }}</template>
              <template v-else-if="column.key === 'action'"><a-button type="link" size="small" @click="handleDownload(record)"><template #icon><DownloadOutlined /></template>下载</a-button></template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="notifications" tab="消息中心">
          <a-space class="table-toolbar"><a-button @click="handleRefreshNotifications"><template #icon><ReloadOutlined /></template>扫描业务预警</a-button></a-space>
          <a-list bordered :data-source="notifications" :loading="loading">
            <template #renderItem="{ item }"><a-list-item><a-list-item-meta :title="item.title" :description="`${item.content} · ${formatDateTime(item.createdAt)}`" /><template #actions><a-tag v-if="!item.read" color="blue">未读</a-tag><a-button v-if="!item.read" type="link" size="small" @click="handleRead(item)">标记已读</a-button></template></a-list-item></template>
          </a-list>
        </a-tab-pane>

        <a-tab-pane key="audits" tab="操作审计">
          <a-table :columns="auditColumns" :data-source="audits" :loading="loading" :pagination="{ pageSize: 10 }" :row-key="(item: AuditRecord) => item.id" :scroll="{ x: 1000 }">
            <template #bodyCell="{ column, record }"><template v-if="column.key === 'status'"><a-tag :color="record.responseStatus < 400 ? 'green' : 'red'">{{ record.responseStatus }}</a-tag></template><template v-else-if="column.key === 'created'">{{ formatDateTime(record.createdAt) }}</template></template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-modal v-model:open="approvalCreateOpen" title="发起通用审批" width="680px" :confirm-loading="saving" @ok="handleApprovalCreate">
      <a-form ref="approvalCreateFormRef" :model="approvalCreateForm" :rules="approvalCreateRules" layout="vertical">
        <a-form-item label="标题" name="title"><a-input v-model:value="approvalCreateForm.title" /></a-form-item>
        <a-row :gutter="16"><a-col :span="12"><a-form-item label="来源单号"><a-input v-model:value="approvalCreateForm.sourceNo" /></a-form-item></a-col><a-col :span="12"><a-form-item label="金额"><a-input-number v-model:value="approvalCreateForm.amount" :min="0" :precision="2" class="full-input" /></a-form-item></a-col></a-row>
        <a-form-item label="申请人" name="applicantName"><a-input v-model:value="approvalCreateForm.applicantName" /></a-form-item><a-form-item label="申请内容" name="content"><a-textarea v-model:value="approvalCreateForm.content" :rows="3" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="processOpen" title="处理审批" :confirm-loading="saving" @ok="handleProcess">
      <a-alert v-if="selectedApproval" class="section-alert" type="info" :message="`${selectedApproval.code} · ${selectedApproval.title} · ${formatMoney(selectedApproval.amount)}`" />
      <a-form ref="processFormRef" :model="processForm" :rules="processRules" layout="vertical"><a-form-item label="审批结论" name="decision"><a-radio-group v-model:value="processForm.decision" button-style="solid"><a-radio-button value="APPROVED">通过</a-radio-button><a-radio-button value="REJECTED">驳回</a-radio-button></a-radio-group></a-form-item><a-form-item label="审批意见" name="comment"><a-textarea v-model:value="processForm.comment" :rows="3" /></a-form-item><a-form-item label="审批人" name="approverName"><a-input v-model:value="processForm.approverName" /></a-form-item></a-form>
    </a-modal>

    <a-modal v-model:open="expenseOpen" title="新增费用报销" width="720px" :confirm-loading="saving" @ok="handleExpense">
    </a-modal>

    <a-modal v-model:open="outsourceOpen" title="新增外包服务" width="760px" :confirm-loading="saving" @ok="handleOutsource">
    </a-modal>

    <a-modal v-model:open="outsourceCompleteOpen" title="外包验收" :confirm-loading="saving" @ok="handleOutsourceComplete"><a-alert v-if="selectedOutsource" class="section-alert" type="warning" :message="`${selectedOutsource.code} · ${selectedOutsource.supplierName} · ${formatMoney(selectedOutsource.amount)}`" /><a-form ref="outsourceCompleteFormRef" :model="outsourceCompleteForm" :rules="outsourceCompleteRules" layout="vertical"><a-form-item label="验收意见" name="acceptanceNote"><a-textarea v-model:value="outsourceCompleteForm.acceptanceNote" :rows="3" /></a-form-item></a-form></a-modal>

    <a-modal v-model:open="uploadOpen" title="上传电子档案" :confirm-loading="saving" @ok="handleUpload">
      <a-form ref="uploadFormRef" :model="uploadForm" :rules="uploadRules" layout="vertical"><a-form-item label="档案类别" name="bizType"><a-select v-model:value="uploadForm.bizType" :options="documentTypeOptions" /></a-form-item><a-form-item label="业务记录编号"><a-input v-model:value="uploadForm.bizId" placeholder="可留空，或填写关联记录UUID" /></a-form-item><a-form-item label="文件" name="file"><a-upload :before-upload="captureFile" :max-count="1"><a-button><template #icon><UploadOutlined /></template>选择文件</a-button></a-upload></a-form-item></a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { message } from "ant-design-vue";
import DownloadOutlined from "@ant-design/icons-vue/DownloadOutlined"; import PlusOutlined from "@ant-design/icons-vue/PlusOutlined"; import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined"; import UploadOutlined from "@ant-design/icons-vue/UploadOutlined";
import { completeOutsource, createApproval, createExpense, createOutsource, downloadDocument, getOfficeOverview, getOfficeReferences, listApprovals, listAudits, listDocuments, listExpenses, listNotifications, listOutsourcing, processApproval, readNotification, refreshNotifications, uploadDocument, type Approval, type ApprovalStatus, type ApprovalType, type AuditRecord, type DocumentRecord, type Expense, type ExpenseStatus, type ExpenseType, type NotificationRecord, type OfficeOverview, type OfficeReferences, type Outsource, type OutsourceStatus } from "@/api/office";
import { useAuthStore } from "@/stores/auth";

const auth=useAuthStore(); const route=useRoute(); const router=useRouter(); const activeTab=ref(tabFromPath(route.path)); const loading=ref(false); const saving=ref(false);
const approvals=ref<Approval[]>([]); const expenses=ref<Expense[]>([]); const outsourcing=ref<Outsource[]>([]); const documents=ref<DocumentRecord[]>([]); const notifications=ref<NotificationRecord[]>([]); const audits=ref<AuditRecord[]>([]);
const overview=reactive<OfficeOverview>({pendingApprovals:0,pendingExpenseAmount:0,approvedExpenseAmount:0,activeOutsourceOrders:0,outsourceAmount:0,unreadNotifications:0,documentCount:0}); const references=reactive<OfficeReferences>({suppliers:[],projects:[],workOrders:[],users:[]});
const approvalCreateOpen=ref(false); const processOpen=ref(false); const expenseOpen=ref(false); const outsourceOpen=ref(false); const outsourceCompleteOpen=ref(false); const uploadOpen=ref(false);
const approvalCreateFormRef=ref(); const processFormRef=ref(); const expenseFormRef=ref(); const outsourceFormRef=ref(); const outsourceCompleteFormRef=ref(); const uploadFormRef=ref(); const selectedApproval=ref<Approval|null>(null); const selectedOutsource=ref<Outsource|null>(null);
const approvalCreateForm=reactive<{code:string;approvalType:ApprovalType;title:string;sourceNo:string;amount:number;applicantName:string;content:string}>({code:"",approvalType:"OTHER",title:"",sourceNo:"",amount:0,applicantName:"",content:""}); const processForm=reactive<{decision:"APPROVED"|"REJECTED";comment:string;approverName:string}>({decision:"APPROVED",comment:"同意",approverName:""});
const expenseForm=reactive<{code:string;claimantId?:string;claimantName:string;projectId?:string;workOrderId?:string;expenseType:ExpenseType;amount:number;expenseDate:string;description:string}>({code:"",claimantId:undefined,claimantName:"",projectId:undefined,workOrderId:undefined,expenseType:"TRAVEL",amount:0,expenseDate:today(),description:""});
const outsourceForm=reactive({code:"",supplierId:"",projectId:undefined as string|undefined,workOrderId:undefined as string|undefined,serviceType:"",description:"",amount:0,plannedDate:today()}); const outsourceCompleteForm=reactive({acceptanceNote:"验收合格"}); const uploadForm=reactive<{bizType:string;bizId:string;file?:File}>({bizType:"CONTRACT",bizId:"",file:undefined});
const approvalTypeOptions=[{label:"合同",value:"CONTRACT"},{label:"采购",value:"PURCHASE"},{label:"付款",value:"PAYMENT"},{label:"用章",value:"SEAL"},{label:"请假",value:"LEAVE"},{label:"出差",value:"TRAVEL"},{label:"其他",value:"OTHER"}]; const expenseTypeOptions=[{label:"差旅",value:"TRAVEL"},{label:"交通",value:"TRANSPORT"},{label:"住宿",value:"ACCOMMODATION"},{label:"工具采购",value:"TOOL"},{label:"其他",value:"OTHER"}]; const documentTypeOptions=[{label:"客户合同",value:"CONTRACT"},{label:"设备图纸",value:"EQUIPMENT_DRAWING"},{label:"检测报告",value:"INSPECTION_REPORT"},{label:"项目资料",value:"COMPLETION_DOCUMENT"},{label:"人员证书",value:"EMPLOYEE_CERTIFICATE"},{label:"验收单",value:"ACCEPTANCE"},{label:"招投标文件",value:"BID"}];
const userOptions=computed(()=>references.users.filter(i=>i.enabled).map(i=>({label:i.displayName,value:i.id}))); const projectOptions=computed(()=>references.projects.map(i=>({label:`${i.code} · ${i.name}`,value:i.id}))); const workOrderOptions=computed(()=>references.workOrders.map(i=>({label:`${i.code} · ${i.title}`,value:i.id}))); const supplierOptions=computed(()=>references.suppliers.map(i=>({label:`${i.code} · ${i.name}`,value:i.id})));
const approvalColumns=[{title:"审批单",key:"approval",width:250},{title:"类型",key:"type",width:110},{title:"来源单号",dataIndex:"sourceNo",width:170},{title:"金额",key:"amount",width:140},{title:"申请人",dataIndex:"applicantName",width:130},{title:"状态",key:"status",width:110},{title:"审批结果",key:"result",width:230},{title:"操作",key:"action",width:120,fixed:"right"}]; const expenseColumns=[{title:"报销单",key:"expense",width:280},{title:"报销人",dataIndex:"claimantName",width:130},{title:"类型",key:"type",width:110},{title:"绑定业务",key:"binding",width:180},{title:"发生日期",dataIndex:"expenseDate",width:120},{title:"金额",key:"amount",width:140},{title:"状态",key:"status",width:130}]; const outsourceColumns=[{title:"外包单",key:"order",width:230},{title:"服务商",dataIndex:"supplierName",width:220},{title:"绑定业务",key:"binding",width:170},{title:"计划日期",dataIndex:"plannedDate",width:120},{title:"金额",key:"amount",width:140},{title:"状态",key:"status",width:130},{title:"说明",dataIndex:"description",width:260},{title:"操作",key:"action",width:120,fixed:"right"}]; const documentColumns=[{title:"文件",key:"file"},{title:"类别",dataIndex:"bizType",width:180},{title:"大小",key:"size",width:120},{title:"上传时间",key:"created",width:190},{title:"操作",key:"action",width:110}]; const auditColumns=[{title:"时间",key:"created",width:190},{title:"用户",dataIndex:"username",width:130},{title:"方法",dataIndex:"httpMethod",width:90},{title:"访问路径",dataIndex:"requestPath"},{title:"状态",key:"status",width:90},{title:"耗时(ms)",dataIndex:"durationMs",width:110},{title:"来源IP",dataIndex:"clientIp",width:150}];
const approvalCreateRules={code: [],approvalType:[{required:true}],title:[{required:true}],applicantName:[{required:true}],content:[{required:true}]}; const processRules={decision:[{required:true}],comment:[{required:true}],approverName:[{required:true}]}; const expenseRules={code: [],claimantId:[{required:true}],expenseType:[{required:true}],amount:[{required:true}],expenseDate:[{required:true}],description:[{required:true}]}; const outsourceRules={code: [],supplierId:[{required:true}],serviceType:[{required:true}],amount:[{required:true}],plannedDate:[{required:true}],description:[{required:true}]}; const outsourceCompleteRules={acceptanceNote:[{required:true}]}; const uploadRules={bizType:[{required:true}],file:[{required:true,message:"请选择文件"}]};
onMounted(loadData); watch(()=>route.path,p=>activeTab.value=tabFromPath(p)); watch(activeTab,t=>{const p=({approvals:"/office/approvals",expenses:"/office/expenses",outsourcing:"/office/outsourcing",documents:"/office/documents",notifications:"/office/notifications",audits:"/office/audits"} as Record<string,string>)[t];if(p&&route.path!==p)router.push(p);});
async function loadData(){loading.value=true;try{const [o,r,a,e,s,d,n,u]=await Promise.all([getOfficeOverview(),getOfficeReferences(),listApprovals(),listExpenses(),listOutsourcing(),listDocuments(),listNotifications(),listAudits()]);Object.assign(overview,o);Object.assign(references,r);approvals.value=a;expenses.value=e;outsourcing.value=s;documents.value=d;notifications.value=n;audits.value=u;}catch(error){message.error(error instanceof Error?error.message:"OA数据加载失败");}finally{loading.value=false;}}
function openApprovalCreate(){Object.assign(approvalCreateForm,{code:generateCode("SP"),approvalType:"OTHER",title:"",sourceNo:"",amount:0,applicantName:auth.user?.displayName||"",content:""});approvalCreateOpen.value=true;} async function handleApprovalCreate(){await approvalCreateFormRef.value?.validate();saving.value=true;try{await createApproval({...approvalCreateForm});approvalCreateOpen.value=false;message.success("审批已发起");await loadData();}catch(error){message.error(error instanceof Error?error.message:"审批发起失败");}finally{saving.value=false;}}
function openProcess(item:Approval){selectedApproval.value=item;Object.assign(processForm,{decision:"APPROVED",comment:"同意",approverName:auth.user?.displayName||""});processOpen.value=true;} async function handleProcess(){await processFormRef.value?.validate();if(!selectedApproval.value)return;saving.value=true;try{await processApproval(selectedApproval.value.id,{...processForm});processOpen.value=false;message.success(processForm.decision==="APPROVED"?"审批已通过":"审批已驳回");await loadData();}catch(error){message.error(error instanceof Error?error.message:"审批处理失败");}finally{saving.value=false;}}
function openExpense(){Object.assign(expenseForm,{code:generateCode("BX"),claimantId:auth.user?.id,claimantName:auth.user?.displayName||"",projectId:undefined,workOrderId:undefined,expenseType:"TRAVEL",amount:0,expenseDate:today(),description:""});expenseOpen.value=true;} function onClaimantChange(id:string){expenseForm.claimantName=references.users.find(i=>i.id===id)?.displayName||"";} async function handleExpense(){await expenseFormRef.value?.validate();saving.value=true;try{await createExpense({...expenseForm});expenseOpen.value=false;message.success("报销单已提交审批");await loadData();activeTab.value="approvals";}catch(error){message.error(error instanceof Error?error.message:"报销提交失败");}finally{saving.value=false;}}
function openOutsource(){Object.assign(outsourceForm,{code:generateCode("WB"),supplierId:"",projectId:undefined,workOrderId:undefined,serviceType:"",description:"",amount:0,plannedDate:today()});outsourceOpen.value=true;} async function handleOutsource(){await outsourceFormRef.value?.validate();saving.value=true;try{await createOutsource({...outsourceForm,applicantName:auth.user?.displayName||""});outsourceOpen.value=false;message.success("外包单已提交审批");await loadData();activeTab.value="approvals";}catch(error){message.error(error instanceof Error?error.message:"外包单提交失败");}finally{saving.value=false;}}
function openOutsourceComplete(item:Outsource){selectedOutsource.value=item;outsourceCompleteForm.acceptanceNote="验收合格";outsourceCompleteOpen.value=true;} async function handleOutsourceComplete(){await outsourceCompleteFormRef.value?.validate();if(!selectedOutsource.value)return;saving.value=true;try{await completeOutsource(selectedOutsource.value.id,{...outsourceCompleteForm});outsourceCompleteOpen.value=false;message.success("外包服务已验收，成本已归集");await loadData();}catch(error){message.error(error instanceof Error?error.message:"外包验收失败");}finally{saving.value=false;}}
function openUpload(){Object.assign(uploadForm,{bizType:"CONTRACT",bizId:"",file:undefined});uploadOpen.value=true;} function captureFile(file:File){uploadForm.file=file;return false;} async function handleUpload(){if(!uploadForm.file){message.error("请选择文件");return;}saving.value=true;try{await uploadDocument({bizType:uploadForm.bizType,bizId:uploadForm.bizId||undefined,file:uploadForm.file});uploadOpen.value=false;message.success("档案已上传");await loadData();}catch(error){message.error(error instanceof Error?error.message:"档案上传失败");}finally{saving.value=false;}} async function handleDownload(item:DocumentRecord){try{await downloadDocument(item);}catch(error){message.error(error instanceof Error?error.message:"档案下载失败");}} async function handleRead(item:NotificationRecord){try{await readNotification(item.id);await loadData();}catch(error){message.error(error instanceof Error?error.message:"消息处理失败");}}
async function handleRefreshNotifications(){try{const count=await refreshNotifications();message.success(count?`新增 ${count} 条业务预警`:"当前没有新的业务预警");await loadData();}catch(error){message.error(error instanceof Error?error.message:"预警扫描失败");}}
function tabFromPath(p:string){if(p.endsWith("/expenses"))return"expenses";if(p.endsWith("/outsourcing"))return"outsourcing";if(p.endsWith("/documents"))return"documents";if(p.endsWith("/notifications"))return"notifications";if(p.endsWith("/audits"))return"audits";return"approvals";} function today(){const d=new Date();return`${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,"0")}-${String(d.getDate()).padStart(2,"0")}`;} function generateCode(prefix:string){const d=new Date();return`${prefix}-${today().replaceAll("-","")}-${String(d.getHours()).padStart(2,"0")}${String(d.getMinutes()).padStart(2,"0")}${String(d.getSeconds()).padStart(2,"0")}${String(d.getMilliseconds()).padStart(3,"0")}`;}
function approvalTypeLabel(v:ApprovalType){return({QUOTE:"报价",CONTRACT:"合同",PURCHASE:"采购",OUTSOURCE:"外包",EXPENSE:"报销",PAYMENT:"付款",SEAL:"用章",LEAVE:"请假",TRAVEL:"出差",OTHER:"其他"} as Record<ApprovalType,string>)[v];} function approvalStatusLabel(v:ApprovalStatus){return({PENDING:"待审批",APPROVED:"已通过",REJECTED:"已驳回"} as Record<ApprovalStatus,string>)[v];} function approvalStatusColor(v:ApprovalStatus){return({PENDING:"orange",APPROVED:"green",REJECTED:"red"} as Record<ApprovalStatus,string>)[v];} function expenseTypeLabel(v:ExpenseType){return({TRAVEL:"差旅",TRANSPORT:"交通",ACCOMMODATION:"住宿",TOOL:"工具采购",OTHER:"其他"} as Record<ExpenseType,string>)[v];} function expenseStatusLabel(v:ExpenseStatus){return({PENDING_APPROVAL:"待审批",APPROVED:"已通过",REJECTED:"已驳回",PAID:"已付款"} as Record<ExpenseStatus,string>)[v];} function expenseStatusColor(v:ExpenseStatus){return({PENDING_APPROVAL:"orange",APPROVED:"blue",REJECTED:"red",PAID:"green"} as Record<ExpenseStatus,string>)[v];} function outsourceStatusLabel(v:OutsourceStatus){return({PENDING_APPROVAL:"待审批",APPROVED:"待执行",IN_PROGRESS:"执行中",COMPLETED:"已验收",SETTLED:"已结算",REJECTED:"已驳回"} as Record<OutsourceStatus,string>)[v];} function outsourceStatusColor(v:OutsourceStatus){return({PENDING_APPROVAL:"orange",APPROVED:"blue",IN_PROGRESS:"cyan",COMPLETED:"purple",SETTLED:"green",REJECTED:"red"} as Record<OutsourceStatus,string>)[v];}
function formatMoney(v:number){return new Intl.NumberFormat("zh-CN",{style:"currency",currency:"CNY",minimumFractionDigits:2,maximumFractionDigits:2}).format(v||0);} function moneyFormatter(v:number|string){return formatMoney(Number(v));} function formatDateTime(v?:string){return v?new Date(v).toLocaleString("zh-CN",{hour12:false}):"-";} function formatFileSize(v:number){if(!v)return"0 B";if(v<1024)return`${v} B`;if(v<1024*1024)return`${(v/1024).toFixed(1)} KB`;return`${(v/1024/1024).toFixed(1)} MB`;}
</script>
