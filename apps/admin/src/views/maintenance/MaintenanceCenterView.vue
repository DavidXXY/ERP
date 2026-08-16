<template>
  <div class="maintenance-page">
    <div class="summary-grid">
      <div>
        <span>开放工单</span><strong>{{ dashboard.open }}</strong>
      </div>
      <div>
        <span>紧急工单</span
        ><strong class="urgent">{{ dashboard.urgent }}</strong>
      </div>
      <div>
        <span>已验收</span><strong>{{ dashboard.closed }}</strong>
      </div>
      <div>
        <span>设备总量</span><strong>{{ dashboard.equipmentCount }}</strong>
      </div>
    </div>

    <a-card :bordered="false">
      <template #title>售后维保中心</template>
      <template #extra
        ><a-button :loading="loading" @click="loadAll">刷新</a-button></template
      >
      <a-tabs v-model:active-key="activeTab">
        <a-tab-pane key="orders" tab="维保工单">
          <div class="toolbar">
            <a-input-search
              v-model:value="keyword"
              allow-clear
              placeholder="搜索工单、设备或客户"
              style="width: 300px"
            /><a-button v-if="canManage" type="primary" @click="openOrder"
              >新建工单</a-button
            >
          </div>
          <a-table
            :columns="orderColumns"
            :data-source="filteredOrders"
            :loading="loading"
            row-key="id"
            :pagination="{ pageSize: 10 }"
            :scroll="{ x: 1100 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'order'"
                ><strong>{{ record.code }}</strong>
                <div class="sub">{{ record.title }}</div></template
              >
              <template v-else-if="column.key === 'status'"
                ><a-tag :color="statusColor(record.status)">{{
                  statusLabel(record.status)
                }}</a-tag></template
              >
              <template v-else-if="column.key === 'priority'"
                ><a-tag
                  :color="
                    record.priority === 'URGENT'
                      ? 'red'
                      : record.priority === 'HIGH'
                        ? 'orange'
                        : 'default'
                  "
                  >{{ priorityLabel(record.priority) }}</a-tag
                ></template
              >
              <template v-else-if="column.key === 'action'"
                ><a-space size="small"
                  ><a-button
                    v-if="
                      canManage &&
                      ['CREATED', 'ASSIGNED'].includes(record.status)
                    "
                    type="link"
                    size="small"
                    @click="openAssign(record)"
                    >派工</a-button
                  ><a-tooltip title="客户验收并生成应收"
                    ><a-button
                      v-if="canManage && record.status === 'COMPLETED'"
                      type="text"
                      size="small"
                      aria-label="客户验收"
                      @click="openAccept(record)"
                      ><template #icon
                        ><CheckCircleOutlined /></template></a-button></a-tooltip></a-space
              ></template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="equipment" tab="设备台账">
          <div class="toolbar">
            <a-button v-if="canManage" type="primary" @click="openEquipment()"
              >新增设备</a-button
            >
          </div>
          <a-table
            :columns="equipmentColumns"
            :data-source="equipment"
            :loading="loading"
            row-key="id"
            :pagination="{ pageSize: 10 }"
            :scroll="{ x: 1120 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'equipment'"
                ><strong>{{ record.name }}</strong>
                <div class="sub">
                  {{ record.code }} · {{ record.category }}
                </div></template
              >
              <template v-else-if="column.key === 'nextDate'"
                ><span
                  :class="{ overdue: isOverdue(record.nextMaintenanceDate) }"
                  >{{ record.nextMaintenanceDate || "未设置" }}</span
                ></template
              >
              <template v-else-if="column.key === 'action'"
                ><a-button
                  v-if="canManage"
                  type="link"
                  size="small"
                  @click="openEquipment(record)"
                  >编辑</a-button
                ></template
              >
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="plans" tab="维护计划">
          <div class="toolbar">
            <a-button v-if="canManage" @click="runDuePlans"
              >生成到期工单</a-button
            ><a-button v-if="canManage" type="primary" @click="openPlan()"
              >新增计划</a-button
            >
          </div>
          <a-table
            :columns="planColumns"
            :data-source="plans"
            :loading="loading"
            row-key="id"
            :pagination="{ pageSize: 10 }"
            :scroll="{ x: 1050 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'plan'"
                ><strong>{{ record.name }}</strong>
                <div class="sub">
                  {{ record.code }} · {{ record.assetName }}
                </div></template
              >
              <template v-else-if="column.key === 'enabled'"
                ><a-switch
                  v-model:checked="record.enabled"
                  :disabled="!canManage"
                  @change="(value: boolean) => togglePlan(record, value)"
              /></template>
              <template v-else-if="column.key === 'auto'"
                ><a-tag :color="record.autoGenerate ? 'green' : 'default'">{{
                  record.autoGenerate ? "自动" : "手动"
                }}</a-tag></template
              >
              <template v-else-if="column.key === 'action'"
                ><a-space
                  ><a-button
                    v-if="canManage"
                    type="link"
                    size="small"
                    @click="generateOne(record)"
                    >生成工单</a-button
                  ><a-button
                    v-if="canManage"
                    type="link"
                    size="small"
                    @click="openPlan(record)"
                    >编辑</a-button
                  ></a-space
                ></template
              >
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="workforce" tab="排班考勤"
          ><WorkforceView
        /></a-tab-pane>

        <a-tab-pane key="certificates" tab="现场资质">
          <div class="toolbar">
            <a-button v-if="canManage" type="primary" @click="openCertificate"
              >新增证书</a-button
            >
          </div>
          <a-table
            :columns="certificateColumns"
            :data-source="certificates"
            :loading="loading"
            row-key="id"
            :pagination="{ pageSize: 10 }"
            :scroll="{ x: 900 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'expiry'"
                ><span
                  :class="{
                    overdue: record.daysUntilExpiry < 0,
                    warning:
                      record.daysUntilExpiry >= 0 &&
                      record.daysUntilExpiry <= 30,
                  }"
                  >{{ record.expiryDate }}（{{
                    expiryText(record.daysUntilExpiry)
                  }}）</span
                ></template
              >
              <template v-else-if="column.key === 'action'"
                ><a-popconfirm
                  title="确认删除该证书？"
                  @confirm="removeCertificate(record.id)"
                  ><a-button v-if="canManage" type="link" danger size="small"
                    >删除</a-button
                  ></a-popconfirm
                ></template
              >
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-modal
      v-model:open="equipmentOpen"
      :title="editingEquipmentId ? '编辑设备' : '新增设备'"
      :confirm-loading="saving"
      width="720px"
      @ok="saveEquipment"
    >
      <a-form layout="vertical"
        ><a-row :gutter="12">
          <a-col :span="12"
            ><a-form-item label="客户" required
              ><a-select
                v-model:value="equipmentForm.customerId"
                show-search
                option-filter-prop="label"
                :options="customerOptions" /></a-form-item
          ></a-col>
          <a-col :span="12"
            ><a-form-item label="关联合同"
              ><a-select
                v-model:value="equipmentForm.contractId"
                allow-clear
                show-search
                option-filter-prop="label"
                :options="contractOptions" /></a-form-item
          ></a-col>
          <a-col :span="12"
            ><a-form-item label="设备名称" required
              ><a-input v-model:value="equipmentForm.name" /></a-form-item
          ></a-col>
          <a-col :span="12"
            ><a-form-item label="设备编码"
              ><a-input
                v-model:value="equipmentForm.code"
                placeholder="留空自动生成" /></a-form-item
          ></a-col>
          <a-col :span="8"
            ><a-form-item label="类别" required
              ><a-input v-model:value="equipmentForm.category" /></a-form-item
          ></a-col>
          <a-col :span="8"
            ><a-form-item label="型号"
              ><a-input v-model:value="equipmentForm.model" /></a-form-item
          ></a-col>
          <a-col :span="8"
            ><a-form-item label="序列号"
              ><a-input v-model:value="equipmentForm.serialNo" /></a-form-item
          ></a-col>
          <a-col :span="16"
            ><a-form-item label="现场地址" required
              ><a-input
                v-model:value="equipmentForm.siteAddress" /></a-form-item
          ></a-col>
          <a-col :span="8"
            ><a-form-item label="维保周期（天）"
              ><a-input-number
                v-model:value="equipmentForm.maintenanceCycleDays"
                :min="1"
                style="width: 100%" /></a-form-item
          ></a-col>
          <a-col :span="12"
            ><a-form-item label="下次维保"
              ><a-date-picker
                v-model:value="equipmentForm.nextMaintenanceDate"
                style="width: 100%" /></a-form-item
          ></a-col>
          <a-col :span="12"
            ><a-form-item label="派工所需证书"
              ><a-input
                v-model:value="equipmentForm.requiredCertificate"
                placeholder="例如：低压电工证" /></a-form-item
          ></a-col> </a-row
      ></a-form>
    </a-modal>

    <a-modal
      v-model:open="acceptOpen"
      title="客户验收"
      :confirm-loading="saving"
      @ok="saveAccept"
    >
      <a-form layout="vertical">
        <a-form-item label="工单"
          ><a-input :value="acceptingOrder?.code" disabled
        /></a-form-item>
        <a-form-item label="应计费金额（含税，元）">
          <a-input-number
            :value="acceptingOrder?.billableAmount || 0"
            disabled
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="确认实际成本（含税，元）">
          <a-input-number
            v-model:value="acceptForm.actualCost"
            :min="0"
            :precision="2"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="验收意见">
          <a-textarea
            v-model:value="acceptForm.remarks"
            :rows="3"
            :maxlength="500"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="planOpen"
      :title="editingPlanId ? '编辑维护计划' : '新增维护计划'"
      :confirm-loading="saving"
      @ok="savePlan"
    >
      <a-form layout="vertical">
        <a-form-item label="设备" required
          ><a-select
            v-model:value="planForm.assetId"
            show-search
            option-filter-prop="label"
            :options="equipmentOptions"
        /></a-form-item>
        <a-form-item label="计划名称" required
          ><a-input v-model:value="planForm.name"
        /></a-form-item>
        <a-row :gutter="12"
          ><a-col :span="12"
            ><a-form-item label="作业类型"
              ><a-select
                v-model:value="planForm.workType"
                :options="workTypeOptions" /></a-form-item></a-col
          ><a-col :span="12"
            ><a-form-item label="优先级"
              ><a-select
                v-model:value="planForm.priority"
                :options="priorityOptions" /></a-form-item></a-col
        ></a-row>
        <a-row :gutter="12"
          ><a-col :span="12"
            ><a-form-item label="周期（天）" required
              ><a-input-number
                v-model:value="planForm.cycleDays"
                :min="1"
                style="width: 100%" /></a-form-item></a-col
          ><a-col :span="12"
            ><a-form-item label="下次执行" required
              ><a-date-picker
                v-model:value="planForm.nextRunDate"
                style="width: 100%" /></a-form-item></a-col
        ></a-row>
        <a-form-item
          ><a-checkbox v-model:checked="planForm.autoGenerate"
            >到期自动生成工单</a-checkbox
          ></a-form-item
        >
        <a-form-item label="作业说明"
          ><a-textarea v-model:value="planForm.description" :rows="3"
        /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="orderOpen"
      title="新建维保工单"
      :confirm-loading="saving"
      @ok="saveOrder"
    >
      <a-form layout="vertical"
        ><a-form-item label="工单标题" required
          ><a-input v-model:value="orderForm.title" /></a-form-item
        ><a-form-item label="设备"
          ><a-select
            v-model:value="orderForm.equipmentId"
            allow-clear
            show-search
            option-filter-prop="label"
            :options="equipmentOptions" /></a-form-item
        ><a-row :gutter="12"
          ><a-col :span="12"
            ><a-form-item label="作业类型"
              ><a-select
                v-model:value="orderForm.workType"
                :options="workTypeOptions" /></a-form-item></a-col
          ><a-col :span="12"
            ><a-form-item label="优先级"
              ><a-select
                v-model:value="orderForm.priority"
                :options="priorityOptions" /></a-form-item></a-col></a-row
        ><a-form-item label="问题描述"
          ><a-textarea
            v-model:value="orderForm.description"
            :rows="3" /></a-form-item
      ></a-form>
    </a-modal>

    <a-modal
      v-model:open="assignOpen"
      title="工单派工"
      :confirm-loading="saving"
      @ok="saveAssign"
      ><a-form layout="vertical"
        ><a-form-item label="负责人" required
          ><a-select
            v-model:value="assignUserId"
            show-search
            option-filter-prop="label"
            :options="assigneeOptions" /></a-form-item></a-form
    ></a-modal>

    <a-modal
      v-model:open="certificateOpen"
      title="新增现场作业证书"
      :confirm-loading="saving"
      @ok="saveCertificate"
    >
      <a-form layout="vertical"
        ><a-form-item label="员工" required
          ><a-select
            v-model:value="certificateForm.userId"
            show-search
            option-filter-prop="label"
            :options="assigneeOptions" /></a-form-item
        ><a-row :gutter="12"
          ><a-col :span="12"
            ><a-form-item label="证书类型" required
              ><a-input
                v-model:value="
                  certificateForm.certificateType
                " /></a-form-item></a-col
          ><a-col :span="12"
            ><a-form-item label="证书编号" required
              ><a-input
                v-model:value="
                  certificateForm.certificateNo
                " /></a-form-item></a-col></a-row
        ><a-row :gutter="12"
          ><a-col :span="12"
            ><a-form-item label="签发日期"
              ><a-date-picker
                v-model:value="certificateForm.issueDate"
                style="width: 100%" /></a-form-item></a-col
          ><a-col :span="12"
            ><a-form-item label="到期日期" required
              ><a-date-picker
                v-model:value="certificateForm.expiryDate"
                style="width: 100%" /></a-form-item></a-col></a-row
        ><a-form-item label="发证机关"
          ><a-input
            v-model:value="certificateForm.issuingAuthority" /></a-form-item
      ></a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import dayjs, { type Dayjs } from "dayjs";
import { message } from "ant-design-vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import CheckCircleOutlined from "@ant-design/icons-vue/CheckCircleOutlined";
import WorkforceView from "@/views/hr/WorkforceView.vue";
import * as api from "@/api/maintenance";
import type {
  Assignee,
  Certificate,
  Dashboard,
  Equipment,
  Plan,
  References,
  WorkOrder,
} from "@/api/maintenance";

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const canManage = computed(
  () =>
    auth.can("maintenance:order:manage") || auth.can("maintenance:plan:manage"),
);
const loading = ref(false);
const saving = ref(false);
const keyword = ref("");
const dashboard = ref<Dashboard>({
  open: 0,
  closed: 0,
  urgent: 0,
  equipmentCount: 0,
});
const references = ref<References>({
  customers: [],
  equipment: [],
  contracts: [],
});
const orders = ref<WorkOrder[]>([]);
const equipment = ref<Equipment[]>([]);
const plans = ref<Plan[]>([]);
const certificates = ref<Certificate[]>([]);
const assignees = ref<Assignee[]>([]);
const allowedTabs = [
  "orders",
  "equipment",
  "plans",
  "workforce",
  "certificates",
];
const activeTab = ref(
  allowedTabs.includes(String(route.query.tab))
    ? String(route.query.tab)
    : route.path.endsWith("/equipment")
      ? "equipment"
      : route.path.endsWith("/certificates")
        ? "certificates"
        : "orders",
);
watch(activeTab, (tab) =>
  router.replace({ path: "/maintenance", query: { tab } }),
);
const filteredOrders = computed(() => {
  const key = keyword.value.trim().toLowerCase();
  return key
    ? orders.value.filter((item) =>
        `${item.code} ${item.title} ${item.customerName || ""} ${item.equipmentName || ""}`
          .toLowerCase()
          .includes(key),
      )
    : orders.value;
});
const customerOptions = computed(() =>
  references.value.customers.map((i) => ({ value: i.id, label: i.name })),
);
const contractOptions = computed(() =>
  references.value.contracts.map((i) => ({ value: i.id, label: i.name })),
);
const equipmentOptions = computed(() =>
  equipment.value.map((i) => ({ value: i.id, label: `${i.code} · ${i.name}` })),
);
const assigneeOptions = computed(() =>
  assignees.value.map((i) => ({ value: i.id, label: i.displayName })),
);
const workTypeOptions = [
  { value: "INSPECTION", label: "巡检" },
  { value: "REPAIR", label: "维修" },
  { value: "REPLACEMENT", label: "更换" },
  { value: "ANNUAL_TEST", label: "年检" },
  { value: "MODIFICATION", label: "改造" },
  { value: "ON_SITE_SERVICE", label: "现场服务" },
];
const priorityOptions = [
  { value: "LOW", label: "低" },
  { value: "NORMAL", label: "普通" },
  { value: "HIGH", label: "高" },
  { value: "URGENT", label: "紧急" },
];
const orderColumns = [
  { title: "工单", key: "order", width: 260 },
  { title: "客户", dataIndex: "customerName", width: 150 },
  { title: "设备", dataIndex: "equipmentName", width: 160 },
  { title: "负责人", dataIndex: "assigneeName", width: 120 },
  { title: "计划日期", dataIndex: "plannedDate", width: 120 },
  { title: "优先级", key: "priority", width: 90 },
  { title: "状态", key: "status", width: 100 },
  { title: "操作", key: "action", fixed: "right", width: 120 },
];
const equipmentColumns = [
  { title: "设备", key: "equipment", width: 240 },
  { title: "客户", dataIndex: "customerName", width: 160 },
  { title: "型号", dataIndex: "model", width: 130 },
  { title: "序列号", dataIndex: "serialNo", width: 150 },
  { title: "现场地址", dataIndex: "siteAddress", width: 240 },
  { title: "下次维保", key: "nextDate", width: 130 },
  { title: "工单数", dataIndex: "orderCount", width: 90 },
  { title: "操作", key: "action", fixed: "right", width: 80 },
];
const planColumns = [
  { title: "计划", key: "plan", width: 270 },
  { title: "周期(天)", dataIndex: "cycleDays", width: 100 },
  { title: "下次执行", dataIndex: "nextRunDate", width: 120 },
  { title: "生成方式", key: "auto", width: 100 },
  { title: "启用", key: "enabled", width: 80 },
  { title: "操作", key: "action", fixed: "right", width: 180 },
];
const certificateColumns = [
  { title: "员工", dataIndex: "employeeName", width: 130 },
  { title: "证书类型", dataIndex: "certificateType", width: 160 },
  { title: "证书编号", dataIndex: "certificateNo", width: 160 },
  { title: "发证机关", dataIndex: "issuingAuthority", width: 180 },
  { title: "有效期", key: "expiry", width: 220 },
  { title: "操作", key: "action", fixed: "right", width: 80 },
];

async function loadAll() {
  loading.value = true;
  try {
    [
      dashboard.value,
      references.value,
      orders.value,
      equipment.value,
      plans.value,
      certificates.value,
    ] = await Promise.all([
      api.getMaintenanceDashboard(),
      api.getMaintenanceReferences(),
      api.listWorkOrders(),
      api.listEquipment(),
      api.listPlans(),
      api.listCertificates(),
    ]);
    if (canManage.value) assignees.value = await api.listAssignees();
  } catch (error: any) {
    message.error(error.message || "维保数据加载失败");
  } finally {
    loading.value = false;
  }
}

const equipmentOpen = ref(false);
const editingEquipmentId = ref<string>();
const equipmentForm = reactive<{
  customerId?: string;
  contractId?: string;
  code?: string;
  name: string;
  category: string;
  model?: string;
  serialNo?: string;
  siteAddress: string;
  maintenanceCycleDays: number;
  nextMaintenanceDate?: Dayjs;
  requiredCertificate?: string;
}>({ name: "", category: "", siteAddress: "", maintenanceCycleDays: 90 });
function openEquipment(item?: Equipment) {
  editingEquipmentId.value = item?.id;
  Object.assign(equipmentForm, {
    customerId: item?.customerId,
    contractId: item?.contractId,
    code: item?.code,
    name: item?.name || "",
    category: item?.category || "",
    model: item?.model,
    serialNo: item?.serialNo,
    siteAddress: item?.siteAddress || "",
    maintenanceCycleDays: item?.maintenanceCycleDays || 90,
    nextMaintenanceDate: item?.nextMaintenanceDate
      ? dayjs(item.nextMaintenanceDate)
      : undefined,
    requiredCertificate: item?.requiredCertificate,
  });
  equipmentOpen.value = true;
}
async function saveEquipment() {
  if (
    !equipmentForm.customerId ||
    !equipmentForm.name.trim() ||
    !equipmentForm.category.trim() ||
    !equipmentForm.siteAddress.trim()
  ) {
    message.warning("请填写客户、名称、类别和现场地址");
    return;
  }
  saving.value = true;
  try {
    const payload = {
      ...equipmentForm,
      customerId: equipmentForm.customerId,
      name: equipmentForm.name.trim(),
      category: equipmentForm.category.trim(),
      siteAddress: equipmentForm.siteAddress.trim(),
      nextMaintenanceDate:
        equipmentForm.nextMaintenanceDate?.format("YYYY-MM-DD"),
    };
    if (editingEquipmentId.value)
      await api.updateEquipment(editingEquipmentId.value, payload);
    else await api.createEquipment(payload);
    equipmentOpen.value = false;
    message.success("设备已保存");
    await loadAll();
  } catch (error: any) {
    message.error(error.message || "保存失败");
  } finally {
    saving.value = false;
  }
}

const planOpen = ref(false);
const editingPlanId = ref<string>();
const planForm = reactive<{
  assetId?: string;
  name: string;
  description?: string;
  workType: string;
  priority: string;
  cycleDays: number;
  autoGenerate: boolean;
  nextRunDate?: Dayjs;
}>({
  name: "",
  workType: "INSPECTION",
  priority: "NORMAL",
  cycleDays: 90,
  autoGenerate: true,
});
function openPlan(item?: Plan) {
  editingPlanId.value = item?.id;
  Object.assign(planForm, {
    assetId: item?.assetId,
    name: item?.name || "",
    description: item?.description,
    workType: item?.workType || "INSPECTION",
    priority: item?.priority || "NORMAL",
    cycleDays: item?.cycleDays || 90,
    autoGenerate: item?.autoGenerate ?? true,
    nextRunDate: item?.nextRunDate
      ? dayjs(item.nextRunDate)
      : dayjs().add(7, "day"),
  });
  planOpen.value = true;
}
async function savePlan() {
  if (!planForm.assetId || !planForm.name.trim() || !planForm.nextRunDate) {
    message.warning("请完整填写计划");
    return;
  }
  saving.value = true;
  try {
    const payload = {
      assetId: planForm.assetId,
      name: planForm.name.trim(),
      description: planForm.description,
      workType: planForm.workType,
      priority: planForm.priority,
      cycleDays: planForm.cycleDays,
      autoGenerate: planForm.autoGenerate,
      nextRunDate: planForm.nextRunDate.format("YYYY-MM-DD"),
    };
    if (editingPlanId.value) await api.updatePlan(editingPlanId.value, payload);
    else await api.createPlan(payload);
    planOpen.value = false;
    message.success("计划已保存");
    await loadAll();
  } catch (error: any) {
    message.error(error.message || "保存失败");
  } finally {
    saving.value = false;
  }
}
async function togglePlan(item: Plan, value: boolean) {
  try {
    await api.setPlanEnabled(item.id, value);
    message.success(value ? "计划已启用" : "计划已停用");
  } catch (error: any) {
    item.enabled = !value;
    message.error(error.message || "操作失败");
  }
}
async function generateOne(item: Plan) {
  try {
    const result = await api.generatePlans(item.id);
    message.success(`已生成 ${result.generated} 张工单`);
    await loadAll();
  } catch (error: any) {
    message.error(error.message || "生成失败");
  }
}
async function runDuePlans() {
  try {
    const result = await api.generatePlans();
    message.success(`已生成 ${result.generated} 张到期工单`);
    await loadAll();
  } catch (error: any) {
    message.error(error.message || "生成失败");
  }
}

const orderOpen = ref(false);
const orderForm = reactive<{
  title: string;
  description?: string;
  equipmentId?: string;
  workType: string;
  priority: string;
}>({ title: "", workType: "REPAIR", priority: "NORMAL" });
function openOrder() {
  Object.assign(orderForm, {
    title: "",
    description: undefined,
    equipmentId: undefined,
    workType: "REPAIR",
    priority: "NORMAL",
  });
  orderOpen.value = true;
}
async function saveOrder() {
  if (!orderForm.title.trim()) {
    message.warning("请填写工单标题");
    return;
  }
  saving.value = true;
  try {
    const asset = equipment.value.find((i) => i.id === orderForm.equipmentId);
    await api.createWorkOrder({
      ...orderForm,
      title: orderForm.title.trim(),
      customerId: asset?.customerId,
      source: "MANUAL",
    });
    orderOpen.value = false;
    message.success("工单已创建");
    await loadAll();
  } catch (error: any) {
    message.error(error.message || "创建失败");
  } finally {
    saving.value = false;
  }
}
const assignOpen = ref(false);
const assigningOrder = ref<WorkOrder>();
const assignUserId = ref<string>();
function openAssign(item: WorkOrder) {
  assigningOrder.value = item;
  assignUserId.value = item.assigneeId;
  assignOpen.value = true;
}
async function saveAssign() {
  const user = assignees.value.find((i) => i.id === assignUserId.value);
  if (!assigningOrder.value || !user) {
    message.warning("请选择负责人");
    return;
  }
  saving.value = true;
  try {
    await api.assignWorkOrder(assigningOrder.value.id, {
      assigneeId: user.id,
      assigneeName: user.displayName,
    });
    assignOpen.value = false;
    message.success("派工完成");
    await loadAll();
  } catch (error: any) {
    message.error(error.message || "派工失败");
  } finally {
    saving.value = false;
  }
}

const acceptOpen = ref(false);
const acceptingOrder = ref<WorkOrder>();
const acceptForm = reactive<{ actualCost?: number; remarks: string }>({
  remarks: "",
});
function openAccept(item: WorkOrder) {
  acceptingOrder.value = item;
  Object.assign(acceptForm, { actualCost: item.costAmount || 0, remarks: "" });
  acceptOpen.value = true;
}
async function saveAccept() {
  if (!acceptingOrder.value) return;
  saving.value = true;
  try {
    await api.acceptWorkOrder(acceptingOrder.value.id, acceptForm);
    acceptOpen.value = false;
    message.success(
      Number(acceptingOrder.value.billableAmount || 0) > 0
        ? "验收完成，应收单已生成"
        : "验收完成",
    );
    await loadAll();
  } catch (error: any) {
    message.error(error.message || "验收失败");
  } finally {
    saving.value = false;
  }
}

const certificateOpen = ref(false);
const certificateForm = reactive<{
  userId?: string;
  certificateType: string;
  certificateNo: string;
  issueDate?: Dayjs;
  expiryDate?: Dayjs;
  issuingAuthority?: string;
}>({ certificateType: "", certificateNo: "" });
function openCertificate() {
  Object.assign(certificateForm, {
    userId: undefined,
    certificateType: "",
    certificateNo: "",
    issueDate: undefined,
    expiryDate: dayjs().add(1, "year"),
    issuingAuthority: undefined,
  });
  certificateOpen.value = true;
}
async function saveCertificate() {
  if (
    !certificateForm.userId ||
    !certificateForm.certificateType.trim() ||
    !certificateForm.certificateNo.trim() ||
    !certificateForm.expiryDate
  ) {
    message.warning("请完整填写证书信息");
    return;
  }
  saving.value = true;
  try {
    await api.createCertificate({
      ...certificateForm,
      issueDate: certificateForm.issueDate?.format("YYYY-MM-DD"),
      expiryDate: certificateForm.expiryDate.format("YYYY-MM-DD"),
    });
    certificateOpen.value = false;
    message.success("证书已保存");
    await loadAll();
  } catch (error: any) {
    message.error(error.message || "保存失败");
  } finally {
    saving.value = false;
  }
}
async function removeCertificate(id: string) {
  try {
    await api.deleteCertificate(id);
    message.success("证书已删除");
    await loadAll();
  } catch (error: any) {
    message.error(error.message || "删除失败");
  }
}
function isOverdue(value?: string) {
  return Boolean(value && dayjs(value).isBefore(dayjs(), "day"));
}
function expiryText(days: number) {
  return days < 0
    ? `过期 ${Math.abs(days)} 天`
    : days === 0
      ? "今天到期"
      : `${days} 天后到期`;
}
function statusLabel(value: string) {
  return (
    (
      {
        CREATED: "待派工",
        ASSIGNED: "待接单",
        IN_PROGRESS: "进行中",
        COMPLETED: "待验收",
        ACCEPTED: "已完成",
        CANCELLED: "已取消",
      } as Record<string, string>
    )[value] || value
  );
}
function statusColor(value: string) {
  return (
    (
      {
        CREATED: "default",
        ASSIGNED: "blue",
        IN_PROGRESS: "gold",
        COMPLETED: "cyan",
        ACCEPTED: "green",
        CANCELLED: "red",
      } as Record<string, string>
    )[value] || "default"
  );
}
function priorityLabel(value: string) {
  return (
    (
      { LOW: "低", NORMAL: "普通", HIGH: "高", URGENT: "紧急" } as Record<
        string,
        string
      >
    )[value] || value
  );
}
onMounted(loadAll);
</script>

<style scoped>
.maintenance-page {
  min-width: 0;
}
.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}
.summary-grid > div {
  padding: 15px 18px;
  background: #fff;
  border: 1px solid #e2e7e9;
  border-radius: 6px;
}
.summary-grid span {
  display: block;
  color: #68747f;
  font-size: 12px;
}
.summary-grid strong {
  display: block;
  margin-top: 3px;
  color: #17212b;
  font-size: 26px;
}
.summary-grid .urgent,
.overdue {
  color: #cf3f35;
}
.warning {
  color: #ad6800;
}
.toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-bottom: 12px;
}
.sub {
  margin-top: 2px;
  color: #788690;
  font-size: 12px;
}
@media (max-width: 760px) {
  .summary-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .toolbar {
    flex-wrap: wrap;
    justify-content: flex-start;
  }
}
</style>
