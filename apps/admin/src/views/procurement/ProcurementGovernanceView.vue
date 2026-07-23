<template>
  <div class="page-stack">
    <a-card title="供应采购治理中心">
      <template #extra><a-button :loading="loading" @click="load">刷新</a-button></template>
      <a-alert
        type="info"
        show-icon
        message="合同、供应商主数据、绩效、退换索赔、供应商协同与库存补货统一在此管理，所有关键动作保留审批和追溯记录。"
        style="margin-bottom: 16px"
      />
      <a-tabs v-model:active-key="activeTab">
        <a-tab-pane key="contracts" tab="采购合同">
          <a-button type="primary" @click="contractOpen = true">新建合同</a-button>
          <a-table :columns="contractColumns" :data-source="contracts" row-key="id" style="margin-top: 12px" :scroll="{ x: 1100 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'amount'">{{ money(record.amount) }} {{ record.currency }}</template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="record.status === 'ACTIVE' ? 'green' : record.status === 'REJECTED' ? 'red' : 'orange'">
                  {{ contractStatus(record.status) }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'actions'">
                <a-space>
                  <a-button v-if="record.status === 'DRAFT'" type="link" @click="submitContract(record)">提交</a-button>
                  <a-button v-if="record.status === 'PENDING_APPROVAL' && canApprove" type="link" @click="reviewContract(record, 'APPROVED')">通过</a-button>
                  <a-button v-if="record.status === 'PENDING_APPROVAL' && canApprove" danger type="link" @click="reviewContract(record, 'REJECTED')">驳回</a-button>
                  <a-button v-if="record.status === 'ACTIVE'" type="link" @click="openAmend(record)">变更</a-button>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="supplier" tab="供应商准入与变更">
          <a-button type="primary" @click="supplierChangeOpen = true">发起变更审批</a-button>
          <a-table :columns="supplierChangeColumns" :data-source="supplierChanges" row-key="id" style="margin-top: 12px">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'supplier'">{{ supplierName(record.supplierId) }}</template>
              <template v-else-if="column.key === 'status'"><a-tag :color="record.status === 'APPROVED' ? 'green' : record.status === 'REJECTED' ? 'red' : 'orange'">{{ statusText(record.status) }}</a-tag></template>
              <template v-else-if="column.key === 'actions' && record.status === 'PENDING' && canApprove">
                <a-button type="link" @click="reviewChange(record, 'APPROVED')">通过</a-button>
                <a-button type="link" danger @click="reviewChange(record, 'REJECTED')">驳回</a-button>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="performance" tab="供应商绩效">
          <a-button type="primary" @click="performanceOpen = true">生成月度考核</a-button>
          <a-table :columns="performanceColumns" :data-source="reviews" row-key="id" style="margin-top: 12px">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'supplier'">{{ supplierName(record.supplierId) }}</template>
              <template v-else-if="column.key === 'rate'">{{ record[column.dataIndex] }}%</template>
              <template v-else-if="column.key === 'score'"><strong>{{ record.totalScore }}</strong> <a-tag :color="record.grade === 'A' ? 'green' : record.grade === 'D' ? 'red' : 'blue'">{{ record.grade }}</a-tag></template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="collaboration" tab="订单协同 / ASN">
          <a-button type="primary" @click="collaborationOpen = true">登记协同事件</a-button>
          <a-table :columns="collaborationColumns" :data-source="events" row-key="id" style="margin-top: 12px">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'supplier'">{{ supplierName(record.supplierId) }}</template>
              <template v-else-if="column.key === 'order'">{{ orderName(record.orderId) }}</template>
              <template v-else-if="column.key === 'type'">{{ eventTypeText(record.eventType) }}</template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="replenishment" tab="库存补货转采购">
          <a-table :columns="replenishmentColumns" :data-source="suggestions" row-key="partId">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'stock'">{{ record.stockQty }} / 安全库存 {{ record.safetyQty }}</template>
              <template v-else-if="column.key === 'priority'"><a-tag :color="record.priority === 'HIGH' ? 'red' : 'orange'">{{ record.priority }}</a-tag></template>
              <template v-else-if="column.key === 'action'"><a-button type="link" @click="openReplenishment(record)">转采购申请</a-button></template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-modal v-model:open="contractOpen" title="新建采购合同" @ok="saveContract">
      <a-form layout="vertical">
        <a-form-item label="合同编号"><a-input v-model:value="contractForm.contractNo" /></a-form-item>
        <a-form-item label="合同名称"><a-input v-model:value="contractForm.name" /></a-form-item>
        <a-form-item label="供应商"><a-select v-model:value="contractForm.supplierId" :options="supplierOptions" /></a-form-item>
        <a-form-item label="合同金额"><a-input-number v-model:value="contractForm.amount" :min="0.01" style="width: 100%" /></a-form-item>
        <a-row :gutter="12"><a-col :span="12"><a-form-item label="开始日"><a-input v-model:value="contractForm.startDate" type="date" /></a-form-item></a-col><a-col :span="12"><a-form-item label="结束日"><a-input v-model:value="contractForm.endDate" type="date" /></a-form-item></a-col></a-row>
        <a-form-item label="付款条件"><a-input v-model:value="contractForm.paymentTerms" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="amendOpen" title="合同变更" @ok="saveAmendment">
      <a-form layout="vertical">
        <a-form-item label="变更后金额"><a-input-number v-model:value="amendForm.amount" :min="0.01" style="width: 100%" /></a-form-item>
        <a-form-item label="变更原因"><a-textarea v-model:value="amendForm.changeReason" /></a-form-item>
        <a-form-item label="付款条件"><a-input v-model:value="amendForm.paymentTerms" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="supplierChangeOpen" title="供应商主数据变更审批" @ok="saveSupplierChange">
      <a-form layout="vertical">
        <a-form-item label="供应商"><a-select v-model:value="changeForm.supplierId" :options="supplierOptions" /></a-form-item>
        <a-form-item label="变更类型"><a-select v-model:value="changeForm.changeType" :options="changeTypes" /></a-form-item>
        <a-form-item label="准入状态"><a-select v-model:value="changeForm.proposedAdmissionStatus" allow-clear :options="admissionOptions" /></a-form-item>
        <a-form-item label="风险状态"><a-select v-model:value="changeForm.proposedRiskStatus" allow-clear :options="riskOptions" /></a-form-item>
        <a-form-item label="开户行"><a-input v-model:value="changeForm.proposedBankName" /></a-form-item>
        <a-form-item label="银行账号"><a-input v-model:value="changeForm.proposedBankAccount" /></a-form-item>
        <a-form-item label="结算条件"><a-input v-model:value="changeForm.proposedSettlementTerms" /></a-form-item>
        <a-form-item label="申请原因"><a-textarea v-model:value="changeForm.reason" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="performanceOpen" title="生成供应商月度绩效" @ok="savePerformance">
      <a-form layout="vertical">
        <a-form-item label="供应商"><a-select v-model:value="performanceForm.supplierId" :options="supplierOptions" /></a-form-item>
        <a-form-item label="考核月份"><a-input v-model:value="performanceForm.reviewPeriod" type="month" /></a-form-item>
        <a-form-item label="协同响应得分"><a-input-number v-model:value="performanceForm.responseScore" :min="0" :max="100" /></a-form-item>
        <a-form-item label="改善要求"><a-textarea v-model:value="performanceForm.improvementAction" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="collaborationOpen" title="登记供应商协同事件" @ok="saveCollaboration">
      <a-form layout="vertical">
        <a-form-item label="供应商"><a-select v-model:value="collaborationForm.supplierId" :options="supplierOptions" /></a-form-item>
        <a-form-item label="采购订单"><a-select v-model:value="collaborationForm.orderId" allow-clear :options="orderOptions" /></a-form-item>
        <a-form-item label="事件类型"><a-select v-model:value="collaborationForm.eventType" :options="eventTypes" /></a-form-item>
        <a-form-item label="参考编号 / ASN"><a-input v-model:value="collaborationForm.referenceNo" /></a-form-item>
        <a-row :gutter="12"><a-col :span="12"><a-form-item label="事件日期"><a-input v-model:value="collaborationForm.eventDate" type="date" /></a-form-item></a-col><a-col :span="12"><a-form-item label="承诺日期"><a-input v-model:value="collaborationForm.promisedDate" type="date" /></a-form-item></a-col></a-row>
        <a-form-item label="说明"><a-textarea v-model:value="collaborationForm.content" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="replenishmentOpen" title="补货建议转采购申请" @ok="saveReplenishment">
      <a-form layout="vertical">
        <a-form-item label="采购数量"><a-input-number v-model:value="replenishmentForm.quantity" :min="0.01" style="width: 100%" /></a-form-item>
        <a-form-item label="预计单价"><a-input-number v-model:value="replenishmentForm.unitPrice" :min="0" style="width: 100%" /></a-form-item>
        <a-form-item label="需求日期"><a-input v-model:value="replenishmentForm.expectedDate" type="date" /></a-form-item>
        <a-form-item label="成本类型"><a-radio-group v-model:value="replenishmentForm.costType"><a-radio value="DEPARTMENT">部门</a-radio><a-radio value="PROJECT">项目</a-radio></a-radio-group></a-form-item>
        <a-form-item :label="replenishmentForm.costType === 'PROJECT' ? '项目' : '部门'"><a-select v-model:value="replenishmentForm.targetId" :options="replenishmentForm.costType === 'PROJECT' ? projectOptions : departmentOptions" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message, Modal } from "ant-design-vue";
import { useAuthStore } from "@/stores/auth";
import { listReplenishmentSuggestions, type ReplenishmentSuggestion } from "@/api/inventory";
import * as api from "@/api/procurement";

const auth = useAuthStore();
const loading = ref(false);
const activeTab = ref("contracts");
const contracts = ref<api.ProcurementContract[]>([]);
const supplierChanges = ref<api.SupplierChangeRequest[]>([]);
const reviews = ref<api.SupplierPerformanceReview[]>([]);
const events = ref<api.ProcurementCollaborationEvent[]>([]);
const suppliers = ref<api.Supplier[]>([]);
const orders = ref<api.PurchaseOrder[]>([]);
const suggestions = ref<ReplenishmentSuggestion[]>([]);
const targets = ref<api.ProcurementCostTargetOptions>({ projects: [], departments: [] });
const canApprove = computed(() => auth.can("procurement:request:approve"));

const contractOpen = ref(false), amendOpen = ref(false), supplierChangeOpen = ref(false), performanceOpen = ref(false), collaborationOpen = ref(false), replenishmentOpen = ref(false);
const selectedContract = ref<api.ProcurementContract | null>(null);
const contractForm = reactive({ contractNo: "", name: "", supplierId: "", amount: 0, currency: "CNY", startDate: "", endDate: "", paymentTerms: "", remark: "" });
const amendForm = reactive({ amount: 0, startDate: "", endDate: "", paymentTerms: "", changeReason: "", remark: "" });
const changeForm = reactive({ supplierId: "", changeType: "ADMISSION", proposedAdmissionStatus: undefined as string | undefined, proposedRiskStatus: undefined as string | undefined, proposedBankName: "", proposedBankAccount: "", proposedSettlementTerms: "", reason: "" });
const performanceForm = reactive({ supplierId: "", reviewPeriod: new Date().toISOString().slice(0, 7), responseScore: 100, improvementAction: "" });
const collaborationForm = reactive({ supplierId: "", orderId: undefined as string | undefined, eventType: "ORDER_ACK", referenceNo: "", eventDate: today(), promisedDate: "", content: "" });
const replenishmentForm = reactive({ partId: "", quantity: 0, unitPrice: 0, expectedDate: addDays(7), costType: "DEPARTMENT" as api.ProcurementCostType, targetId: "" });

const supplierOptions = computed(() => suppliers.value.map(item => ({ label: `${item.name}（${item.code}）`, value: item.id })));
const orderOptions = computed(() => orders.value.map(item => ({ label: `${item.code} · ${item.supplierName}`, value: item.id })));
const projectOptions = computed(() => targets.value.projects.map(item => ({ label: item.name, value: item.id })));
const departmentOptions = computed(() => targets.value.departments.map(item => ({ label: item.name, value: item.id })));
const changeTypes = [{ label: "准入", value: "ADMISSION" }, { label: "风险状态", value: "RISK" }, { label: "银行账户", value: "BANK_ACCOUNT" }, { label: "结算条件", value: "SETTLEMENT" }];
const admissionOptions = [{ label: "待准入", value: "PENDING" }, { label: "已准入", value: "APPROVED" }, { label: "未准入", value: "REJECTED" }, { label: "暂停", value: "SUSPENDED" }];
const riskOptions = [{ label: "正常", value: "NORMAL" }, { label: "关注", value: "WATCHLIST" }, { label: "冻结", value: "BLOCKED" }];
const eventTypes = [{ label: "订单确认", value: "ORDER_ACK" }, { label: "发货通知 ASN", value: "ASN" }, { label: "交期承诺", value: "DELIVERY_PROMISE" }, { label: "电子发票", value: "E_INVOICE" }, { label: "异常反馈", value: "EXCEPTION" }, { label: "整改 CAPA", value: "CAPA" }];

const contractColumns = [{ title: "合同编号", dataIndex: "contractNo" }, { title: "合同名称", dataIndex: "name" }, { title: "版本", dataIndex: "versionNo", width: 70 }, { title: "金额", key: "amount" }, { title: "有效期", customRender: ({ record }: any) => `${record.startDate || "-"} 至 ${record.endDate || "-"}` }, { title: "状态", key: "status" }, { title: "操作", key: "actions", fixed: "right", width: 210 }];
const supplierChangeColumns = [{ title: "供应商", key: "supplier" }, { title: "变更类型", dataIndex: "changeType" }, { title: "原因", dataIndex: "reason" }, { title: "申请人", dataIndex: "requestedByName" }, { title: "状态", key: "status" }, { title: "操作", key: "actions", width: 130 }];
const performanceColumns = [{ title: "供应商", key: "supplier" }, { title: "期间", dataIndex: "reviewPeriod" }, { title: "准时率", dataIndex: "onTimeRate", key: "rate" }, { title: "质量合格率", dataIndex: "qualityRate", key: "rate" }, { title: "发票匹配率", dataIndex: "invoiceMatchRate", key: "rate" }, { title: "总分/等级", key: "score" }, { title: "改善要求", dataIndex: "improvementAction" }];
const collaborationColumns = [{ title: "日期", dataIndex: "eventDate" }, { title: "供应商", key: "supplier" }, { title: "订单", key: "order" }, { title: "类型", key: "type" }, { title: "参考编号", dataIndex: "referenceNo" }, { title: "承诺日期", dataIndex: "promisedDate" }, { title: "说明", dataIndex: "content" }];
const replenishmentColumns = [{ title: "物料", dataIndex: "partName" }, { title: "库存", key: "stock" }, { title: "近30日出库", dataIndex: "recentOutboundQty" }, { title: "建议采购", dataIndex: "suggestedQty" }, { title: "优先级", key: "priority" }, { title: "原因", dataIndex: "reason" }, { title: "操作", key: "action" }];

onMounted(load);
async function load() {
  loading.value = true;
  try {
    const [c, sc, sr, ce, s, o, rs, t] = await Promise.all([
      api.listProcurementContracts(), api.listSupplierChanges(), api.listSupplierPerformanceReviews(),
      api.listProcurementCollaborationEvents(), api.listSuppliers(0, 999), api.listPurchaseOrders({ page: 0, size: 999 }),
      listReplenishmentSuggestions(), api.listProcurementCostTargets(),
    ]);
    contracts.value = c; supplierChanges.value = sc; reviews.value = sr; events.value = ce;
    suppliers.value = s.content; orders.value = o.content; suggestions.value = rs; targets.value = t;
  } catch (error) { message.error(error instanceof Error ? error.message : "加载失败"); }
  finally { loading.value = false; }
}
async function saveContract() { await api.createProcurementContract({ ...contractForm }); contractOpen.value = false; message.success("合同已保存为草稿"); await load(); }
async function submitContract(record: api.ProcurementContract) { await api.submitProcurementContract(record.id); message.success("合同已提交审批"); await load(); }
function reviewContract(record: api.ProcurementContract, decision: "APPROVED" | "REJECTED") { Modal.confirm({ title: decision === "APPROVED" ? "确认通过合同？" : "确认驳回合同？", async onOk() { await api.reviewProcurementContract(record.id, { decision, comment: decision === "APPROVED" ? "合同条款审核通过" : "合同条款需调整" }); await load(); } }); }
function openAmend(record: api.ProcurementContract) { selectedContract.value = record; Object.assign(amendForm, { amount: record.amount, startDate: record.startDate || "", endDate: record.endDate || "", paymentTerms: record.paymentTerms || "", changeReason: "", remark: record.remark || "" }); amendOpen.value = true; }
async function saveAmendment() { if (!selectedContract.value) return; await api.amendProcurementContract(selectedContract.value.id, { ...amendForm }); amendOpen.value = false; message.success("合同变更版本已创建，需重新审批"); await load(); }
async function saveSupplierChange() { await api.createSupplierChange({ ...changeForm }); supplierChangeOpen.value = false; message.success("供应商变更已提交审批"); await load(); }
function reviewChange(record: api.SupplierChangeRequest, decision: "APPROVED" | "REJECTED") { Modal.confirm({ title: `确认${decision === "APPROVED" ? "通过" : "驳回"}供应商变更？`, async onOk() { await api.reviewSupplierChange(record.id, { decision, comment: decision === "APPROVED" ? "同意变更" : "不同意变更" }); await load(); } }); }
async function savePerformance() { await api.calculateSupplierPerformance({ ...performanceForm }); performanceOpen.value = false; message.success("月度绩效已生成"); await load(); }
async function saveCollaboration() { await api.createProcurementCollaborationEvent({ ...collaborationForm }); collaborationOpen.value = false; message.success("供应商协同事件已登记"); await load(); }
function openReplenishment(record: ReplenishmentSuggestion) { Object.assign(replenishmentForm, { partId: record.partId, quantity: record.suggestedQty, unitPrice: 0, expectedDate: addDays(7), costType: "DEPARTMENT", targetId: "" }); replenishmentOpen.value = true; }
async function saveReplenishment() { await api.convertReplenishmentToRequest({ partId: replenishmentForm.partId, quantity: replenishmentForm.quantity, unitPrice: replenishmentForm.unitPrice, expectedDate: replenishmentForm.expectedDate, costType: replenishmentForm.costType, projectId: replenishmentForm.costType === "PROJECT" ? replenishmentForm.targetId : undefined, departmentId: replenishmentForm.costType === "DEPARTMENT" ? replenishmentForm.targetId : undefined }); replenishmentOpen.value = false; message.success("已生成采购申请并进入审批"); await load(); }
function supplierName(id: string) { return suppliers.value.find(item => item.id === id)?.name || id.slice(0, 8); }
function orderName(id?: string) { return id ? orders.value.find(item => item.id === id)?.code || id.slice(0, 8) : "-"; }
function statusText(value: string) { return ({ PENDING: "待审批", APPROVED: "已通过", REJECTED: "已驳回" } as Record<string, string>)[value] || value; }
function contractStatus(value: string) { return ({ DRAFT: "草稿", PENDING_APPROVAL: "待审批", ACTIVE: "已生效", REJECTED: "已驳回", SUPERSEDED: "已变更" } as Record<string, string>)[value] || value; }
function eventTypeText(value: string) { return eventTypes.find(item => item.value === value)?.label || value; }
function money(value: number) { return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY" }).format(value || 0); }
function today() { return new Date().toISOString().slice(0, 10); }
function addDays(days: number) { const date = new Date(); date.setDate(date.getDate() + days); return date.toISOString().slice(0, 10); }
</script>
