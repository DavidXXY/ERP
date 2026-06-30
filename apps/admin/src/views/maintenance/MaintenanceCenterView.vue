<template>
  <div class="page-stack">
    <a-card title="服务执行中心">
      <template #extra>
        <a-button :loading="loading" @click="loadAll"><template #icon><ReloadOutlined /></template>刷新</a-button>
      </template>
      <a-row :gutter="[16, 16]" class="metric-row">
        <a-col :xs="12" :xl="4"><a-statistic title="设备总数" :value="dashboard.equipmentCount" /></a-col>
        <a-col :xs="12" :xl="4"><a-statistic title="待服务设备" :value="dashboard.dueEquipmentCount" :value-style="dangerStyle(dashboard.dueEquipmentCount)" /></a-col>
        <a-col :xs="12" :xl="4"><a-statistic title="进行中工单" :value="dashboard.openWorkOrders" /></a-col>
        <a-col :xs="12" :xl="4"><a-statistic title="逾期工单" :value="dashboard.overdueWorkOrders" :value-style="dangerStyle(dashboard.overdueWorkOrders)" /></a-col>
        <a-col :xs="12" :xl="4"><a-statistic title="本月服务成本" :value="dashboard.monthCost" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :xl="4"><a-statistic title="本月可计费" :value="dashboard.monthBillable" :formatter="moneyFormatter" /></a-col>
      </a-row>
    </a-card>

    <a-card>
      <a-tabs v-model:active-key="activeTab">
        <a-tab-pane key="orders" tab="服务工单">
          <a-space wrap class="table-toolbar">
            <a-input-search v-model:value="orderKeyword" allow-clear placeholder="搜索工单、客户、设备、工程师" style="width: 290px" />
            <a-select v-model:value="orderStatus" allow-clear placeholder="全部状态" :options="orderStatusOptions" style="width: 140px" />
            <a-button v-if="auth.can('maintenance:workorder:create')" type="primary" @click="openCreateOrder"><template #icon><PlusOutlined /></template>新建工单</a-button>
          </a-space>
          <a-table :columns="orderColumns" :data-source="filteredOrders" :loading="loading" :pagination="{ pageSize: 8 }" :row-key="(item: WorkOrder) => item.id" :scroll="{ x: 1500 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'order'"><a-button type="link" class="table-link" @click="openDetail(record)">{{ record.code }}</a-button><span class="table-subtitle">{{ record.title }}</span></template>
              <template v-else-if="column.key === 'customer'">{{ record.customerName }}<span class="table-subtitle">{{ record.equipmentName || '未绑定设备' }}</span></template>
              <template v-else-if="column.key === 'type'"><a-tag>{{ typeLabel(record.workType) }}</a-tag><span class="table-subtitle">{{ sourceLabel(record.source) }}</span></template>
              <template v-else-if="column.key === 'priority'"><a-tag :color="priorityColor(record.priority)">{{ priorityLabel(record.priority) }}</a-tag></template>
              <template v-else-if="column.key === 'status'"><a-tag :color="orderStatusColor(record.status)">{{ orderStatusLabel(record.status) }}</a-tag></template>
              <template v-else-if="column.key === 'engineer'">{{ record.engineerName || '待派工' }}<span v-if="record.requiredCertificate" class="table-subtitle">需 {{ record.requiredCertificate }}</span></template>
              <template v-else-if="column.key === 'cost'">{{ formatMoney(record.costAmount) }}<span class="table-subtitle">可计费 {{ formatMoney(record.billableAmount) }}</span></template>
              <template v-else-if="column.key === 'action'">
                <a-space size="small">
                  <a-button v-if="record.status === 'CREATED' && auth.can('maintenance:workorder:assign')" type="link" size="small" @click="openAssign(record)">派工</a-button>
                  <a-button v-if="record.status === 'ASSIGNED' && auth.can('maintenance:workorder:execute')" type="link" size="small" @click="openCheckIn(record)">签到开工</a-button>
                  <a-button v-if="record.status === 'IN_PROGRESS' && auth.can('maintenance:workorder:execute')" type="link" size="small" @click="openComplete(record)">完工</a-button>
                  <a-button v-if="record.status === 'COMPLETED' && auth.can('maintenance:workorder:accept')" type="link" size="small" @click="openAccept(record)">验收</a-button>
                  <a-button type="link" size="small" @click="openDetail(record)">详情</a-button>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="equipment" tab="资产设备">
          <a-space wrap class="table-toolbar">
            <a-input-search v-model:value="equipmentKeyword" allow-clear placeholder="搜索设备、客户、地址" style="width: 290px" />
            <a-button v-if="auth.can('maintenance:equipment:create')" type="primary" @click="openCreateEquipment"><template #icon><PlusOutlined /></template>新增设备</a-button>
          </a-space>
          <a-table :columns="equipmentColumns" :data-source="filteredEquipment" :loading="loading" :pagination="{ pageSize: 8 }" :row-key="(item: Equipment) => item.id" :scroll="{ x: 1360 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'equipment'"><strong>{{ record.code }}</strong><span class="table-subtitle">{{ record.name }} · {{ record.model || '无型号' }}</span></template>
              <template v-else-if="column.key === 'customer'">{{ record.customerName }}<span class="table-subtitle">{{ record.contractCode || '未绑定合同' }}</span></template>
              <template v-else-if="column.key === 'site'">{{ record.siteAddress }}</template>
              <template v-else-if="column.key === 'maintenance'">{{ record.nextMaintenanceDate || '-' }}<span class="table-subtitle">周期 {{ record.maintenanceCycleDays }} 天</span></template>
              <template v-else-if="column.key === 'history'">{{ record.workOrderCount }} 次<span class="table-subtitle">故障 {{ record.faultCount }} 次</span></template>
              <template v-else-if="column.key === 'status'"><a-tag :color="equipmentStatusColor(record.status)">{{ equipmentStatusLabel(record.status) }}</a-tag></template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="plans" tab="服务计划">
          <a-space wrap class="table-toolbar">
            <a-button v-if="auth.can('maintenance:plan:create')" @click="openCreatePlan"><template #icon><PlusOutlined /></template>新增计划</a-button>
            <a-button v-if="auth.can('maintenance:plan:generate')" type="primary" :loading="generating" @click="handleGenerate"><template #icon><ThunderboltOutlined /></template>生成到期工单</a-button>
          </a-space>
          <a-table :columns="planColumns" :data-source="plans" :loading="loading" :pagination="{ pageSize: 8 }" :row-key="(item: MaintenancePlan) => item.id" :scroll="{ x: 1050 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'plan'"><strong>{{ record.code }}</strong><span class="table-subtitle">{{ record.planName }}</span></template>
              <template v-else-if="column.key === 'asset'">{{ record.assetCode }} · {{ record.assetName }}</template>
              <template v-else-if="column.key === 'cycle'">每 {{ record.cycleDays }} 天</template>
              <template v-else-if="column.key === 'due'"><span :class="{ 'text-danger': record.due }">{{ record.nextDueDate }}</span><a-tag v-if="record.due" color="red">已到期</a-tag></template>
              <template v-else-if="column.key === 'status'"><a-tag :color="record.active ? 'green' : 'default'">{{ record.active ? '生效' : '停用' }}</a-tag></template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-modal v-model:open="equipmentOpen" title="新增资产设备" width="820px" :confirm-loading="saving" @ok="handleCreateEquipment">
      <a-form ref="equipmentFormRef" :model="equipmentForm" :rules="equipmentRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="12"><a-form-item label="客户" name="customerId"><a-select v-model:value="equipmentForm.customerId" show-search option-filter-prop="label" :options="customerOptions" @change="equipmentForm.contractId = undefined" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="服务合同"><a-select v-model:value="equipmentForm.contractId" allow-clear :options="equipmentContractOptions" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="设备编号" name="code"><a-input v-model:value="equipmentForm.code" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="设备名称" name="name"><a-input v-model:value="equipmentForm.name" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="设备类别" name="category"><a-input v-model:value="equipmentForm.category" placeholder="配电柜、泵组等" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="型号"><a-input v-model:value="equipmentForm.model" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="出厂序列号"><a-input v-model:value="equipmentForm.serialNo" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="要求证书"><a-input v-model:value="equipmentForm.requiredCertificate" placeholder="如：低压电工证" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="安装地址" name="siteAddress"><a-input v-model:value="equipmentForm.siteAddress" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="安装日期"><a-input v-model:value="equipmentForm.installedDate" type="date" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="质保到期"><a-input v-model:value="equipmentForm.warrantyEndDate" type="date" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="服务周期（天）" name="maintenanceCycleDays"><a-input-number v-model:value="equipmentForm.maintenanceCycleDays" :min="1" class="full-input" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="下次服务日期" name="nextMaintenanceDate"><a-input v-model:value="equipmentForm.nextMaintenanceDate" type="date" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="同步建立周期计划"><a-switch v-model:checked="equipmentForm.createPlan" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="备注"><a-textarea v-model:value="equipmentForm.notes" :rows="2" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal v-model:open="planOpen" title="新增服务计划" width="650px" :confirm-loading="saving" @ok="handleCreatePlan">
      <a-form ref="planFormRef" :model="planForm" :rules="planRules" layout="vertical">
        <a-form-item label="设备" name="assetId"><a-select v-model:value="planForm.assetId" show-search option-filter-prop="label" :options="assetOptions" @change="fillPlanFromAsset" /></a-form-item>
        <a-row :gutter="16">
          <a-col :span="12"><a-form-item label="计划编号" name="code"><a-input v-model:value="planForm.code" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="计划名称" name="planName"><a-input v-model:value="planForm.planName" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="周期（天）" name="cycleDays"><a-input-number v-model:value="planForm.cycleDays" :min="1" class="full-input" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="下次执行" name="nextDueDate"><a-input v-model:value="planForm.nextDueDate" type="date" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal v-model:open="orderOpen" title="新建服务工单" width="860px" :confirm-loading="saving" @ok="handleCreateOrder">
      <a-form ref="orderFormRef" :model="orderForm" :rules="orderRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="8"><a-form-item label="工单编号" name="code"><a-input v-model:value="orderForm.code" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="来源" name="source"><a-select v-model:value="orderForm.source" :options="sourceOptions" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="工单类型" name="workType"><a-select v-model:value="orderForm.workType" :options="typeOptions" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="优先级" name="priority"><a-select v-model:value="orderForm.priority" :options="priorityOptions" /></a-form-item></a-col>
          <a-col :xs="24" :md="16"><a-form-item label="工单标题" name="title"><a-input v-model:value="orderForm.title" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="客户" name="customerId"><a-select v-model:value="orderForm.customerId" show-search option-filter-prop="label" :options="customerOptions" @change="onOrderCustomerChange" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="合同"><a-select v-model:value="orderForm.contractId" allow-clear :options="orderContractOptions" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="设备"><a-select v-model:value="orderForm.equipmentId" allow-clear show-search option-filter-prop="label" :options="orderAssetOptions" @change="onOrderAssetChange" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="计划日期" name="plannedDate"><a-input v-model:value="orderForm.plannedDate" type="date" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="现场地址" name="siteAddress"><a-input v-model:value="orderForm.siteAddress" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="报修 / 作业内容" name="problemDescription"><a-textarea v-model:value="orderForm.problemDescription" :rows="3" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="可计费金额"><a-input-number v-model:value="orderForm.billableAmount" :min="0" :precision="2" class="full-input" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="质保免费"><a-switch v-model:checked="orderForm.freeWarranty" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal v-model:open="assignOpen" title="工单派工" :confirm-loading="saving" @ok="handleAssign">
      <a-alert v-if="selectedOrder" class="section-alert" type="info" :message="`${selectedOrder.code} · ${selectedOrder.plannedDate} · ${selectedOrder.requiredCertificate ? `要求${selectedOrder.requiredCertificate}` : '无证书限制'}`" />
      <a-form ref="assignFormRef" :model="assignForm" :rules="assignRules" layout="vertical">
        <a-form-item label="符合条件的工程师" name="assigneeId"><a-select v-model:value="assignForm.assigneeId" :loading="employeeLoading" :options="eligibleEmployees.map(item => ({ label: `${item.displayName}${item.validCertificates.length ? ` · ${item.validCertificates.join('、')}` : ''}`, value: item.id }))" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="checkInOpen" title="现场签到并开工" :confirm-loading="saving" @ok="handleCheckIn">
      <a-form ref="checkInFormRef" :model="checkInForm" :rules="checkInRules" layout="vertical">
        <a-form-item label="签到位置" name="location"><a-input v-model:value="checkInForm.location" placeholder="填写或粘贴现场定位地址" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="completeOpen" title="现场完工登记" width="820px" :confirm-loading="saving" @ok="handleComplete">
      <a-form ref="completeFormRef" :model="completeForm" :rules="completeRules" layout="vertical">
        <a-form-item label="处理结果" name="serviceResult"><a-textarea v-model:value="completeForm.serviceResult" :rows="3" /></a-form-item>
        <a-row :gutter="16">
          <a-col :xs="12" :md="6"><a-form-item label="工时"><a-input-number v-model:value="completeForm.laborHours" :min="0" :precision="2" class="full-input" /></a-form-item></a-col>
          <a-col :xs="12" :md="6"><a-form-item label="人工成本"><a-input-number v-model:value="completeForm.laborCost" :min="0" :precision="2" class="full-input" /></a-form-item></a-col>
          <a-col :xs="12" :md="6"><a-form-item label="差旅成本"><a-input-number v-model:value="completeForm.travelCost" :min="0" :precision="2" class="full-input" /></a-form-item></a-col>
          <a-col :xs="12" :md="6"><a-form-item label="外包成本"><a-input-number v-model:value="completeForm.outsourcingCost" :min="0" :precision="2" class="full-input" /></a-form-item></a-col>
        </a-row>
        <a-divider orientation="left">消耗物料</a-divider>
        <div v-for="(line, index) in completeForm.materials" :key="index" class="material-line">
          <a-select v-model:value="line.partId" show-search option-filter-prop="label" :options="partOptions" placeholder="选择物料" />
          <a-input-number v-model:value="line.quantity" :min="0.01" :precision="2" />
          <a-button danger type="text" @click="completeForm.materials.splice(index, 1)"><template #icon><DeleteOutlined /></template></a-button>
        </div>
        <a-button type="dashed" block @click="addMaterial"><template #icon><PlusOutlined /></template>添加物料</a-button>
      </a-form>
    </a-modal>

    <a-modal v-model:open="acceptOpen" title="客户验收" :confirm-loading="saving" @ok="handleAccept">
      <a-alert v-if="selectedOrder && !selectedOrder.freeWarranty && selectedOrder.billableAmount > 0" class="section-alert" type="warning" :message="`验收后将自动生成 ${formatMoney(selectedOrder.billableAmount)} 应收款`" />
      <a-form ref="acceptFormRef" :model="acceptForm" :rules="acceptRules" layout="vertical">
        <a-form-item label="客户签字人" name="customerSigner"><a-input v-model:value="acceptForm.customerSigner" /></a-form-item>
        <a-form-item label="验收意见" name="acceptanceNote"><a-textarea v-model:value="acceptForm.acceptanceNote" :rows="3" /></a-form-item>
      </a-form>
    </a-modal>

    <a-drawer v-model:open="detailOpen" title="工单全貌" width="760">
      <a-spin :spinning="detailLoading">
        <template v-if="detailOrder">
          <a-descriptions bordered :column="2" size="small">
            <a-descriptions-item label="工单">{{ detailOrder.code }}</a-descriptions-item><a-descriptions-item label="状态"><a-tag :color="orderStatusColor(detailOrder.status)">{{ orderStatusLabel(detailOrder.status) }}</a-tag></a-descriptions-item>
            <a-descriptions-item label="客户">{{ detailOrder.customerName }}</a-descriptions-item><a-descriptions-item label="设备">{{ detailOrder.equipmentName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="工程师">{{ detailOrder.engineerName || '-' }}</a-descriptions-item><a-descriptions-item label="计划日期">{{ detailOrder.plannedDate }}</a-descriptions-item>
            <a-descriptions-item label="现场地址" :span="2">{{ detailOrder.siteAddress }}</a-descriptions-item>
            <a-descriptions-item label="作业内容" :span="2">{{ detailOrder.problemDescription }}</a-descriptions-item>
            <a-descriptions-item label="处理结果" :span="2">{{ detailOrder.serviceResult || '-' }}</a-descriptions-item>
            <a-descriptions-item label="工时">{{ detailOrder.laborHours }}</a-descriptions-item><a-descriptions-item label="总成本">{{ formatMoney(detailOrder.costAmount) }}</a-descriptions-item>
            <a-descriptions-item label="客户签字">{{ detailOrder.customerSigner || '-' }}</a-descriptions-item><a-descriptions-item label="计费金额">{{ formatMoney(detailOrder.billableAmount) }}</a-descriptions-item>
          </a-descriptions>
          <a-divider orientation="left">物料消耗</a-divider>
          <a-table size="small" :columns="materialColumns" :data-source="detailOrder.materials" :pagination="false" :row-key="(item: WorkOrderMaterial) => item.id" />
          <a-divider orientation="left">流转记录</a-divider>
          <a-list size="small" bordered :data-source="detailOrder.statusLogs">
            <template #renderItem="{ item }"><a-list-item><strong>{{ orderStatusLabel(item.toStatus) }}</strong>&nbsp;{{ item.operatorName }} · {{ item.remark }}<span class="table-subtitle">{{ formatDateTime(item.createdAt) }}</span></a-list-item></template>
          </a-list>
        </template>
      </a-spin>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { message } from "ant-design-vue";
import DeleteOutlined from "@ant-design/icons-vue/DeleteOutlined";
import PlusOutlined from "@ant-design/icons-vue/PlusOutlined";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import ThunderboltOutlined from "@ant-design/icons-vue/ThunderboltOutlined";
import {
  acceptWorkOrder, assignWorkOrder, checkInWorkOrder, completeWorkOrder,
  createEquipment as createEquipmentApi, createMaintenancePlan, createWorkOrder as createWorkOrderApi,
  generateMaintenanceWorkOrders, getMaintenanceDashboard, getMaintenanceReferences, getWorkOrder,
  listEligibleEmployees, listEquipment, listMaintenancePlans, listWorkOrders,
  type EmployeeOption, type Equipment, type EquipmentStatus, type MaintenanceDashboard,
  type MaintenancePlan, type MaintenanceReferences, type WorkOrder, type WorkOrderMaterial,
  type WorkOrderPriority, type WorkOrderSource, type WorkOrderStatus, type WorkOrderType,
} from "@/api/maintenance";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
const route = useRoute(); const router = useRouter();
const loading = ref(false); const saving = ref(false); const generating = ref(false); const employeeLoading = ref(false); const detailLoading = ref(false);
const activeTab = ref(tabFromPath(route.path)); const equipmentOpen = ref(false); const planOpen = ref(false); const orderOpen = ref(false);
const assignOpen = ref(false); const checkInOpen = ref(false); const completeOpen = ref(false); const acceptOpen = ref(false); const detailOpen = ref(false);
const equipmentFormRef = ref(); const planFormRef = ref(); const orderFormRef = ref(); const assignFormRef = ref(); const checkInFormRef = ref(); const completeFormRef = ref(); const acceptFormRef = ref();
const equipment = ref<Equipment[]>([]); const plans = ref<MaintenancePlan[]>([]); const orders = ref<WorkOrder[]>([]);
const references = reactive<MaintenanceReferences>({ customers: [], contracts: [], parts: [], users: [] });
const dashboard = reactive<MaintenanceDashboard>({ equipmentCount: 0, dueEquipmentCount: 0, openWorkOrders: 0, overdueWorkOrders: 0, expiringCertificates: 0, monthCost: 0, monthBillable: 0 });
const selectedOrder = ref<WorkOrder | null>(null); const detailOrder = ref<WorkOrder | null>(null); const eligibleEmployees = ref<EmployeeOption[]>([]);
const orderKeyword = ref(""); const equipmentKeyword = ref(""); const orderStatus = ref<WorkOrderStatus>();

const equipmentForm = reactive({ customerId: "", contractId: undefined as string | undefined, code: "", name: "", category: "", model: "", serialNo: "", siteAddress: "", installedDate: "", warrantyEndDate: "", maintenanceCycleDays: 90, nextMaintenanceDate: today(), requiredCertificate: "", notes: "", createPlan: true });
const planForm = reactive({ code: "", assetId: "", planName: "", cycleDays: 90, nextDueDate: today() });
const orderForm = reactive<{ code: string; source: WorkOrderSource; workType: WorkOrderType; priority: WorkOrderPriority; title: string; customerId: string; contractId?: string; equipmentId?: string; plannedDate: string; siteAddress: string; problemDescription: string; billableAmount: number; freeWarranty: boolean }>({ code: "", source: "CUSTOMER_REPAIR", workType: "REPAIR", priority: "NORMAL", title: "", customerId: "", contractId: undefined, equipmentId: undefined, plannedDate: today(), siteAddress: "", problemDescription: "", billableAmount: 0, freeWarranty: false });
const assignForm = reactive({ assigneeId: "" }); const checkInForm = reactive({ location: "" });
const completeForm = reactive<{ serviceResult: string; laborHours: number; laborCost: number; travelCost: number; outsourcingCost: number; materials: Array<{ partId: string; quantity: number }> }>({ serviceResult: "", laborHours: 0, laborCost: 0, travelCost: 0, outsourcingCost: 0, materials: [] });
const acceptForm = reactive({ customerSigner: "", acceptanceNote: "验收合格" });

const sourceOptions = [{ label: "客户申请", value: "CUSTOMER_REPAIR" }, { label: "服务计划", value: "MAINTENANCE_PLAN" }, { label: "工程项目", value: "PROJECT" }, { label: "设备检测", value: "EQUIPMENT_INSPECTION" }, { label: "手工创建", value: "MANUAL" }];
const typeOptions = [{ label: "定期检查", value: "INSPECTION" }, { label: "故障处理", value: "REPAIR" }, { label: "设备更换", value: "REPLACEMENT" }, { label: "年度检测", value: "ANNUAL_TEST" }, { label: "改造施工", value: "MODIFICATION" }, { label: "现场服务", value: "ON_SITE_SERVICE" }];
const priorityOptions = [{ label: "低", value: "LOW" }, { label: "普通", value: "NORMAL" }, { label: "高", value: "HIGH" }, { label: "紧急", value: "URGENT" }];
const orderStatusOptions = [{ label: "待派工", value: "CREATED" }, { label: "待开工", value: "ASSIGNED" }, { label: "处理中", value: "IN_PROGRESS" }, { label: "待验收", value: "COMPLETED" }, { label: "已闭环", value: "ACCEPTED" }];
const customerOptions = computed(() => references.customers.map(item => ({ label: `${item.code} · ${item.name}`, value: item.id })));
const equipmentContractOptions = computed(() => references.contracts.filter(item => item.customerId === equipmentForm.customerId).map(item => ({ label: `${item.code} · ${item.projectName}`, value: item.id })));
const orderContractOptions = computed(() => references.contracts.filter(item => item.customerId === orderForm.customerId).map(item => ({ label: `${item.code} · ${item.projectName}`, value: item.id })));
const orderAssetOptions = computed(() => equipment.value.filter(item => item.customerId === orderForm.customerId).map(item => ({ label: `${item.code} · ${item.name}`, value: item.id })));
const assetOptions = computed(() => equipment.value.map(item => ({ label: `${item.code} · ${item.name} · ${item.customerName}`, value: item.id })));
const partOptions = computed(() => references.parts.filter(item => item.stockQty > 0).map(item => ({ label: `${item.code} · ${item.name} · 库存 ${item.stockQty}`, value: item.id })));
const filteredOrders = computed(() => orders.value.filter(item => { const term = orderKeyword.value.trim().toLowerCase(); const text = `${item.code} ${item.title} ${item.customerName} ${item.equipmentName || ""} ${item.engineerName || ""}`.toLowerCase(); return (!orderStatus.value || item.status === orderStatus.value) && (!term || text.includes(term)); }));
const filteredEquipment = computed(() => equipment.value.filter(item => { const term = equipmentKeyword.value.trim().toLowerCase(); return !term || `${item.code} ${item.name} ${item.customerName} ${item.siteAddress}`.toLowerCase().includes(term); }));

const orderColumns = [{ title: "工单", key: "order", width: 230 }, { title: "客户 / 设备", key: "customer", width: 220 }, { title: "类型 / 来源", key: "type", width: 160 }, { title: "优先级", key: "priority", width: 100 }, { title: "计划日期", dataIndex: "plannedDate", width: 120 }, { title: "工程师", key: "engineer", width: 170 }, { title: "状态", key: "status", width: 110 }, { title: "成本 / 计费", key: "cost", width: 180 }, { title: "操作", key: "action", width: 260, fixed: "right" }];
const equipmentColumns = [{ title: "设备", key: "equipment", width: 250 }, { title: "客户 / 合同", key: "customer", width: 230 }, { title: "安装地址", key: "site", width: 300 }, { title: "下次服务", key: "maintenance", width: 170 }, { title: "证书要求", dataIndex: "requiredCertificate", width: 150 }, { title: "服务历史", key: "history", width: 110 }, { title: "状态", key: "status", width: 120 }];
const planColumns = [{ title: "计划", key: "plan", width: 260 }, { title: "设备", key: "asset", width: 250 }, { title: "合同", dataIndex: "contractCode", width: 180 }, { title: "周期", key: "cycle", width: 120 }, { title: "下次执行", key: "due", width: 190 }, { title: "上次生成", dataIndex: "lastGeneratedDate", width: 130 }, { title: "状态", key: "status", width: 100 }];
const materialColumns = [{ title: "物料", dataIndex: "partName" }, { title: "数量", dataIndex: "quantity", width: 100 }, { title: "单价", dataIndex: "unitCost", width: 120 }, { title: "金额", dataIndex: "amount", width: 120 }];
const equipmentRules = { customerId: [{ required: true, message: "请选择客户" }], code: [{ required: true }], name: [{ required: true }], category: [{ required: true }], siteAddress: [{ required: true }], maintenanceCycleDays: [{ required: true }], nextMaintenanceDate: [{ required: true }] };
const planRules = { assetId: [{ required: true, message: "请选择设备" }], code: [{ required: true }], planName: [{ required: true }], cycleDays: [{ required: true }], nextDueDate: [{ required: true }] };
const orderRules = { code: [{ required: true }], source: [{ required: true }], workType: [{ required: true }], priority: [{ required: true }], title: [{ required: true }], customerId: [{ required: true }], plannedDate: [{ required: true }], siteAddress: [{ required: true }], problemDescription: [{ required: true }] };
const assignRules = { assigneeId: [{ required: true, message: "请选择工程师" }] }; const checkInRules = { location: [{ required: true, message: "请输入签到位置" }] }; const completeRules = { serviceResult: [{ required: true, message: "请填写处理结果" }] }; const acceptRules = { customerSigner: [{ required: true }], acceptanceNote: [{ required: true }] };

onMounted(loadAll);
watch(() => route.path, (path) => { activeTab.value = tabFromPath(path); });
watch(activeTab, (tab) => { const path = ({ orders: "/maintenance/work-orders", equipment: "/maintenance/equipment", plans: "/maintenance/plans" } as Record<string, string>)[tab]; if (path && route.path !== path) router.push(path); });
async function loadAll() { loading.value = true; try { const [summary, refs, assetRows, planRows, orderRows] = await Promise.all([getMaintenanceDashboard(), getMaintenanceReferences(), listEquipment(), listMaintenancePlans(), listWorkOrders()]); Object.assign(dashboard, summary); Object.assign(references, refs); equipment.value = assetRows; plans.value = planRows; orders.value = orderRows; } catch (error) { message.error(error instanceof Error ? error.message : "服务数据加载失败"); } finally { loading.value = false; } }
function openCreateEquipment() { Object.assign(equipmentForm, { customerId: "", contractId: undefined, code: generateCode("SB"), name: "", category: "", model: "", serialNo: "", siteAddress: "", installedDate: "", warrantyEndDate: "", maintenanceCycleDays: 90, nextMaintenanceDate: today(), requiredCertificate: "", notes: "", createPlan: true }); equipmentOpen.value = true; }
async function handleCreateEquipment() { await equipmentFormRef.value?.validate(); saving.value = true; try { await createEquipmentApi({ ...equipmentForm, installedDate: equipmentForm.installedDate || undefined, warrantyEndDate: equipmentForm.warrantyEndDate || undefined }); equipmentOpen.value = false; message.success("设备与服务资料已建立"); await loadAll(); } catch (error) { message.error(error instanceof Error ? error.message : "设备新增失败"); } finally { saving.value = false; } }
function openCreatePlan() { Object.assign(planForm, { code: generateCode("JH"), assetId: "", planName: "", cycleDays: 90, nextDueDate: today() }); planOpen.value = true; }
function fillPlanFromAsset(value: string) { const asset = equipment.value.find(item => item.id === value); if (asset) Object.assign(planForm, { planName: `${asset.name}定期服务`, cycleDays: asset.maintenanceCycleDays, nextDueDate: asset.nextMaintenanceDate || today() }); }
async function handleCreatePlan() { await planFormRef.value?.validate(); saving.value = true; try { await createMaintenancePlan({ ...planForm }); planOpen.value = false; message.success("服务计划已建立"); await loadAll(); } catch (error) { message.error(error instanceof Error ? error.message : "计划新增失败"); } finally { saving.value = false; } }
async function handleGenerate() { generating.value = true; try { const result = await generateMaintenanceWorkOrders({ throughDate: today(), operatorName: auth.user?.displayName || "系统" }); message.success(result.generatedCount ? `已生成 ${result.generatedCount} 张工单` : "当前没有到期计划"); await loadAll(); activeTab.value = "orders"; } catch (error) { message.error(error instanceof Error ? error.message : "工单生成失败"); } finally { generating.value = false; } }
function openCreateOrder() { Object.assign(orderForm, { code: generateCode("GD"), source: "CUSTOMER_REPAIR", workType: "REPAIR", priority: "NORMAL", title: "", customerId: "", contractId: undefined, equipmentId: undefined, plannedDate: today(), siteAddress: "", problemDescription: "", billableAmount: 0, freeWarranty: false }); orderOpen.value = true; }
function onOrderCustomerChange() { orderForm.contractId = undefined; orderForm.equipmentId = undefined; orderForm.siteAddress = ""; }
function onOrderAssetChange(value?: string) { const asset = equipment.value.find(item => item.id === value); if (asset) { orderForm.siteAddress = asset.siteAddress; orderForm.contractId = asset.contractId; if (!orderForm.title) orderForm.title = `${asset.name}故障处理`; } }
async function handleCreateOrder() { await orderFormRef.value?.validate(); saving.value = true; try { await createWorkOrderApi({ ...orderForm, operatorName: auth.user?.displayName || "系统" }); orderOpen.value = false; message.success("工单已创建，等待派工"); await loadAll(); } catch (error) { message.error(error instanceof Error ? error.message : "工单创建失败"); } finally { saving.value = false; } }
async function openAssign(item: WorkOrder) { selectedOrder.value = item; assignForm.assigneeId = ""; assignOpen.value = true; employeeLoading.value = true; try { eligibleEmployees.value = await listEligibleEmployees(item.id); if (!eligibleEmployees.value.length) message.warning("没有满足证书与排班条件的工程师"); } catch (error) { message.error(error instanceof Error ? error.message : "工程师加载失败"); } finally { employeeLoading.value = false; } }
async function handleAssign() { await assignFormRef.value?.validate(); if (!selectedOrder.value) return; saving.value = true; try { await assignWorkOrder(selectedOrder.value.id, { ...assignForm, operatorName: auth.user?.displayName || "系统" }); assignOpen.value = false; message.success("派工完成"); await loadAll(); } catch (error) { message.error(error instanceof Error ? error.message : "派工失败"); } finally { saving.value = false; } }
function openCheckIn(item: WorkOrder) { selectedOrder.value = item; checkInForm.location = item.siteAddress; checkInOpen.value = true; }
async function handleCheckIn() { await checkInFormRef.value?.validate(); if (!selectedOrder.value) return; saving.value = true; try { await checkInWorkOrder(selectedOrder.value.id, { ...checkInForm, operatorName: auth.user?.displayName || "系统" }); checkInOpen.value = false; message.success("签到成功，工单已开工"); await loadAll(); } catch (error) { message.error(error instanceof Error ? error.message : "签到失败"); } finally { saving.value = false; } }
function openComplete(item: WorkOrder) { selectedOrder.value = item; Object.assign(completeForm, { serviceResult: "", laborHours: 0, laborCost: 0, travelCost: 0, outsourcingCost: 0, materials: [] }); completeOpen.value = true; }
function addMaterial() { completeForm.materials.push({ partId: "", quantity: 1 }); }
async function handleComplete() { await completeFormRef.value?.validate(); if (!selectedOrder.value) return; if (completeForm.materials.some(item => !item.partId || item.quantity <= 0)) { message.error("请完整填写物料明细"); return; } saving.value = true; try { await completeWorkOrder(selectedOrder.value.id, { ...completeForm, operatorName: auth.user?.displayName || "系统" }); completeOpen.value = false; message.success("工单已完工，库存和成本已同步"); await loadAll(); } catch (error) { message.error(error instanceof Error ? error.message : "完工登记失败"); } finally { saving.value = false; } }
function openAccept(item: WorkOrder) { selectedOrder.value = item; Object.assign(acceptForm, { customerSigner: "", acceptanceNote: "验收合格" }); acceptOpen.value = true; }
async function handleAccept() { await acceptFormRef.value?.validate(); if (!selectedOrder.value) return; saving.value = true; try { await acceptWorkOrder(selectedOrder.value.id, { ...acceptForm, operatorName: auth.user?.displayName || "系统" }); acceptOpen.value = false; message.success("客户验收完成，工单已闭环"); await loadAll(); } catch (error) { message.error(error instanceof Error ? error.message : "验收失败"); } finally { saving.value = false; } }
async function openDetail(item: WorkOrder) { detailOpen.value = true; detailLoading.value = true; try { detailOrder.value = await getWorkOrder(item.id); } catch (error) { message.error(error instanceof Error ? error.message : "工单详情加载失败"); } finally { detailLoading.value = false; } }
function today() { const value = new Date(); return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, "0")}-${String(value.getDate()).padStart(2, "0")}`; }
function generateCode(prefix: string) { const value = new Date(); return `${prefix}-${today().replaceAll("-", "")}-${String(value.getHours()).padStart(2, "0")}${String(value.getMinutes()).padStart(2, "0")}${String(value.getSeconds()).padStart(2, "0")}${String(value.getMilliseconds()).padStart(3, "0")}`; }
function sourceLabel(value: WorkOrderSource) { return ({ MAINTENANCE_PLAN: "服务计划", CUSTOMER_REPAIR: "客户申请", PROJECT: "工程项目", EQUIPMENT_INSPECTION: "设备检测", MANUAL: "手工创建" } as Record<WorkOrderSource, string>)[value]; }
function typeLabel(value: WorkOrderType) { return ({ INSPECTION: "定期检查", REPAIR: "故障处理", REPLACEMENT: "设备更换", ANNUAL_TEST: "年度检测", MODIFICATION: "改造施工", ON_SITE_SERVICE: "现场服务" } as Record<WorkOrderType, string>)[value]; }
function priorityLabel(value: WorkOrderPriority) { return ({ LOW: "低", NORMAL: "普通", HIGH: "高", URGENT: "紧急" } as Record<WorkOrderPriority, string>)[value]; }
function priorityColor(value: WorkOrderPriority) { return ({ LOW: "default", NORMAL: "blue", HIGH: "orange", URGENT: "red" } as Record<WorkOrderPriority, string>)[value]; }
function orderStatusLabel(value: WorkOrderStatus) { return ({ CREATED: "待派工", ASSIGNED: "待开工", IN_PROGRESS: "处理中", COMPLETED: "待验收", ACCEPTED: "已闭环", CANCELLED: "已取消" } as Record<WorkOrderStatus, string>)[value]; }
function orderStatusColor(value: WorkOrderStatus) { return ({ CREATED: "orange", ASSIGNED: "cyan", IN_PROGRESS: "blue", COMPLETED: "purple", ACCEPTED: "green", CANCELLED: "default" } as Record<WorkOrderStatus, string>)[value]; }
function equipmentStatusLabel(value: EquipmentStatus) { return ({ ACTIVE: "运行中", MAINTENANCE_DUE: "待服务", OUT_OF_SERVICE: "停用", RETIRED: "已报废" } as Record<EquipmentStatus, string>)[value]; }
function equipmentStatusColor(value: EquipmentStatus) { return ({ ACTIVE: "green", MAINTENANCE_DUE: "red", OUT_OF_SERVICE: "orange", RETIRED: "default" } as Record<EquipmentStatus, string>)[value]; }
function formatMoney(value: number) { return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY", minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value || 0); }
function moneyFormatter(value: number | string) { return formatMoney(Number(value)); }
function dangerStyle(value: number) { return value > 0 ? { color: "#cf1322" } : {}; }
function formatDateTime(value?: string) { return value ? new Date(value).toLocaleString("zh-CN", { hour12: false }) : "-"; }
function tabFromPath(path: string) { return path.endsWith("/equipment") ? "equipment" : path.endsWith("/plans") ? "plans" : "orders"; }
</script>

<style scoped>
.material-line { display: grid; grid-template-columns: minmax(0, 1fr) 140px 40px; gap: 10px; margin-bottom: 10px; }
@media (max-width: 700px) { .material-line { grid-template-columns: minmax(0, 1fr) 100px 36px; } }
</style>
