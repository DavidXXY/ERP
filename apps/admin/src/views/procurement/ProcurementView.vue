<template>
  <div class="page-stack">
    <a-card title="供应链采购">
      <template #extra>
        <a-button :loading="loading" @click="loadData">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </template>

      <a-alert
        v-if="errorMessage"
        class="section-alert"
        type="warning"
        show-icon
        :message="errorMessage"
        description="请确认后端服务已启动，并重新登录以刷新采购权限。"
      />

      <a-row :gutter="[16, 16]" class="metric-row">
        <a-col :xs="12" :lg="8" :xl="4"><a-statistic title="待审批申请" :value="pendingApprovalCount" suffix="条" /></a-col>
        <a-col :xs="12" :lg="8" :xl="4"><a-statistic title="待收货订单" :value="openOrderCount" suffix="单" /></a-col>
        <a-col :xs="12" :lg="8" :xl="4"><a-statistic title="累计入库金额" :value="receiptAmount" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="8" :xl="4"><a-statistic title="采购待付" :value="outstandingPayable" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="8" :xl="4"><a-statistic title="项目采购成本" :value="projectCostAmount" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="8" :xl="4"><a-statistic title="部门采购成本" :value="departmentCostAmount" :formatter="moneyFormatter" /></a-col>
      </a-row>

      <a-tabs :active-key="activeTab" @update:activeKey="handleTabChange">
        <a-tab-pane key="requests" tab="采购申请">
          <div class="table-toolbar">
            <a-space wrap>
              <a-button @click="handleExportRequests">导出</a-button>
              <a-button v-if="auth.can('procurement:purchase:create')" type="primary" @click="openRequest">
                <template #icon><PlusOutlined /></template>
                新增采购申请
              </a-button>
              <a-tag color="orange">待审批 {{ pendingApprovalCount }}</a-tag>
            </a-space>
            <a-space class="filter-space">
              <a-select v-model:value="requestFilters.status" allow-clear placeholder="申请状态" style="width:130px" :options="requestStatusOptions" />
              <a-select v-model:value="requestFilters.approvalStatus" allow-clear placeholder="审批状态" style="width:120px" :options="requestApprovalOptions" />
              <a-select v-model:value="requestFilters.costType" allow-clear placeholder="成本归属" style="width:120px" :options="costTypeOptions" />
              <a-input-search v-model:value="requestFilters.search" placeholder="搜索申请编号/物料/申请人" style="width:260px" @search="handleRequestFilter" />
              <a-button @click="handleRequestFilter">查询</a-button>
            </a-space>
          </div>
          <a-table
            :row-selection="{selectedRowKeys:selectedRequestKeys,onChange:(keys:any)=>selectedRequestKeys.values=keys}"
            :columns="requestColumns"
            :data-source="purchaseRequests"
            :loading="requestLoading"
            :pagination="{
              current: requestPage + 1,
              pageSize: requestSize,
              total: requestTotal,
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total: number) => `共 ${total} 条`,
              onChange: handleRequestPageChange,
            }"
            :row-key="(record: PurchaseRequest) => record.id"
            :scroll="{ x: 1120 }"
            size="middle"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'code'">
                <strong>{{ record.code }}</strong>
                <span class="table-subtitle">{{ record.requesterName }} · {{ record.reason || '无备注' }}</span>
              </template>
              <template v-else-if="column.key === 'part'">
                {{ record.partName }}
                <span class="table-subtitle">申请数量 {{ formatQuantity(record.quantity) }}</span>
                <span class="table-subtitle">预计 {{ formatMoney(record.totalAmount || 0) }} · 税率 {{ formatTaxRate(record.taxRate) }}</span>
              </template>
              <template v-else-if="column.key === 'costTarget'">
                <a-tag :color="costTypeColor(record.costType)">{{ costTypeLabel(record.costType) }}</a-tag>
                <strong>{{ record.costTargetName }}</strong>
                <span class="table-subtitle">{{ record.costTargetCode }}</span>
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="requestColor(record.status)">{{ requestLabel(record.status) }}</a-tag>
                <a-tag :color="approvalColor(record.approvalStatus)">{{ approvalLabel(record.approvalStatus) }}</a-tag>
                <span v-if="record.lastApproverName" class="table-subtitle">
                  {{ record.lastApproverName }} · {{ record.lastApprovalComment }}
                </span>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-space>
                  <a-button
                    v-if="auth.can('procurement:purchase:create') && (record.approvalStatus === 'PENDING' || record.approvalStatus === 'REJECTED')"
                    type="link"
                    size="small"
                    @click="openEditRequest(record)"
                  >
                    编辑
                  </a-button>
                  <a-button
                    v-if="auth.can('procurement:request:approve') && record.approvalStatus === 'PENDING'"
                    type="link"
                    size="small"
                    @click="openApproval(record)"
                  >
                    审批处理
                  </a-button>
                  <span v-else-if="!auth.can('procurement:purchase:create') || (record.approvalStatus !== 'PENDING' && record.approvalStatus !== 'REJECTED')" class="muted">
                    {{ record.approvalStatus === 'APPROVED' ? '已审批' : record.approvalStatus === 'REJECTED' ? '已驳回' : '无需处理' }}
                  </span>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="orders" tab="采购订单">
          <div class="table-toolbar">
            <a-space wrap>
              <a-button v-if="auth.can('procurement:purchase:create')" type="primary" @click="openOrder">
                <template #icon><ShoppingCartOutlined /></template>
                根据审批申请下单
              </a-button>
              <span class="muted">当前有 {{ requestOptions.length }} 条已审批申请可下单</span>
              <a-button style="float:right" size="small" @click="handleRequestFilter">刷新列表</a-button>
            </a-space>
            <a-space class="filter-space">
              <a-select v-model:value="orderFilters.status" allow-clear placeholder="订单状态" style="width:130px" :options="orderStatusOptions" />
              <a-select v-model:value="orderFilters.costType" allow-clear placeholder="成本归属" style="width:120px" :options="costTypeOptions" />
              <a-input-search v-model:value="orderFilters.search" placeholder="搜索订单编号/物料" style="width:220px" @search="handleRequestFilter" />
              <a-button @click="handleRequestFilter">查询</a-button>
            </a-space>
          </div>
          <a-table
            :columns="orderColumns"
            :data-source="purchaseOrders"
            :loading="orderLoading"
            :pagination="{
              current: orderPage + 1,
              pageSize: orderSize,
              total: orderTotal,
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total: number) => `共 ${total} 条`,
              onChange: handleRequestPageChange,
            }"
            :row-key="(record: PurchaseOrder) => record.id"
            :scroll="{ x: 1280 }"
            size="middle"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'code'">
                <strong>{{ record.code }}</strong>
                <span class="table-subtitle">来源 {{ record.requestCode || '未关联申请' }}</span>
              </template>
              <template v-else-if="column.key === 'part'">
                {{ record.partName || '未关联物料' }}
                <span class="table-subtitle">单价 {{ formatMoney(record.unitPrice) }} · 税率 {{ formatTaxRate(record.taxRate) }}</span>
              </template>
              <template v-else-if="column.key === 'quantity'">
                <strong>{{ formatQuantity(record.receivedQty) }} / {{ formatQuantity(record.orderedQty) }}</strong>
                <a-progress
                  :percent="receivedPercent(record)"
                  size="small"
                  :show-info="false"
                  :status="record.status === 'RECEIVED' ? 'success' : 'active'"
                />
              </template>
              <template v-else-if="column.key === 'amount'">
                <strong>{{ formatMoney(record.orderAmount) }}</strong>
              </template>
              <template v-else-if="column.key === 'costTarget'">
                <a-tag :color="costTypeColor(record.costType)">{{ costTypeLabel(record.costType) }}</a-tag>
                <span>{{ record.costTargetName }}</span>
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="orderColor(record.status)">{{ orderLabel(record.status) }}</a-tag>
              </template>
                            <template v-else-if="column.key === 'action'">
                <a-space>
                  <a-button
                    v-if="auth.can('procurement:order:receive') && canReceive(record)"
                    type="link"
                    size="small"
                    @click="openReceipt(record)"
                  >
                    到货入库
                  </a-button>
                  <a-button
                    v-if="auth.can('procurement:purchase:create') && record.status === 'ORDERED'"
                    type="link"
                    size="small"
                    danger
                    @click="handleCancelOrder(record)"
                  >
                    取消订单
                  </a-button>
                  <span v-else class="muted">
                    {{ record.status === 'RECEIVED' ? '已全部入库' : record.status === 'CANCELLED' ? '已取消' : '无需处理' }}
                  </span>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="receipts" tab="到货入库">
          <a-table
            :columns="receiptColumns"
            :data-source="goodsReceipts"
            :loading="loading"
            :pagination="{ pageSize: 8 }"
            :row-key="(record: GoodsReceipt) => record.id"
            :scroll="{ x: 1080 }"
            size="middle"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'receipt'">
                <strong>{{ record.code }}</strong>
                <span class="table-subtitle">订单 {{ record.orderCode }}</span>
              </template>
              <template v-else-if="column.key === 'quantity'">
                {{ formatQuantity(record.quantity) }} × {{ formatMoney(record.unitPrice) }}
              </template>
              <template v-else-if="column.key === 'amount'"><strong>{{ formatMoney(record.amount) }}</strong><span class="table-subtitle">税率 {{ formatTaxRate(record.taxRate) }}</span></template>
              <template v-else-if="column.key === 'costTarget'">
                <a-tag :color="costTypeColor(record.costType)">{{ costTypeLabel(record.costType) }}</a-tag>
                <span>{{ record.costTargetName }}</span>
              </template>
            </template>
            <template #emptyText>订单收货后，到货入库记录会显示在这里</template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="costs" tab="成本归集">
          <div class="table-toolbar">
            <a-segmented v-model:value="costFilter" :options="costFilterOptions" />
          </div>
          <a-table
            :columns="costAllocationColumns"
            :data-source="filteredCostAllocations"
            :loading="loading"
            :pagination="{ pageSize: 8 }"
            :row-key="(record: ProcurementCostAllocation) => record.id"
            :scroll="{ x: 980 }"
            size="middle"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'source'">
                <strong>{{ record.receiptCode }}</strong>
                <span class="table-subtitle">订单 {{ record.orderCode }}</span>
              </template>
              <template v-else-if="column.key === 'costTarget'">
                <a-tag :color="costTypeColor(record.costType)">{{ costTypeLabel(record.costType) }}</a-tag>
                <strong>{{ record.costTargetName }}</strong>
                <span class="table-subtitle">{{ record.costTargetCode }}</span>
              </template>
              <template v-else-if="column.key === 'amount'"><strong>{{ formatMoney(record.amount) }}</strong></template>
            </template>
            <template #emptyText>暂无采购成本归集记录</template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane v-if="auth.can('procurement:payable:view')" key="payables" tab="采购应付">
          <a-table
            :columns="payableColumns"
            :data-source="payables"
            :loading="loading"
            :pagination="{ pageSize: 8 }"
            :row-key="(record: ProcurementPayable) => record.id"
            :scroll="{ x: 1080 }"
            size="middle"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'payable'">
                <strong>{{ record.code }}</strong>
                <span class="table-subtitle">订单 {{ record.orderCode }}</span>
              </template>
              <template v-else-if="column.key === 'amount'">
                <strong>{{ formatMoney(record.amount) }}</strong>
                <span class="table-subtitle">待付 {{ formatMoney(record.outstandingAmount) }} · 税率 {{ formatTaxRate(record.taxRate) }}</span>
              </template>
              <template v-else-if="column.key === 'costTarget'">
                <a-tag :color="costTypeColor(record.costType)">{{ costTypeLabel(record.costType) }}</a-tag>
                <span>{{ record.costTargetName }}</span>
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="payableColor(record.status)">{{ payableLabel(record.status) }}</a-tag>
                <a-tag v-if="isOverdue(record)" color="red">已逾期</a-tag>
              </template>
            </template>
            <template #emptyText>采购收货后，应付单会自动生成</template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="suppliers" tab="供应商">
          <div class="table-toolbar">
            <a-button v-if="auth.can('procurement:supplier:create')" type="primary" @click="openSupplier">
              <template #icon><PlusOutlined /></template>
              新增供应商
            </a-button>
          </div>
          <a-table
            :columns="supplierColumns"
            :data-source="suppliers"
            :loading="loading"
            :pagination="{ pageSize: 8 }"
            :row-key="(record: Supplier) => record.id"
            :scroll="{ x: 900 }"
            size="middle"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                <strong>{{ record.name }}</strong>
                <span class="table-subtitle">{{ record.code }} · {{ record.category || '未分类' }}</span>
              </template>
              <template v-else-if="column.key === 'risk'">
                <a-tag :color="supplierRiskColor(record.riskStatus)">{{ supplierRiskLabel(record.riskStatus) }}</a-tag>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-modal v-model:open="supplierOpen" title="新增供应商" width="720px" :confirm-loading="savingSupplier" @ok="handleCreateSupplier">
      <a-form ref="supplierFormRef" :model="supplierForm" :rules="supplierRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="16"><a-form-item label="供应商名称" name="name"><a-input v-model:value="supplierForm.name" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="品类"><a-input v-model:value="supplierForm.category" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="账期"><a-input v-model:value="supplierForm.settlementTerms" placeholder="例如：月结30天" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="联系人"><a-input v-model:value="supplierForm.contactName" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="电话"><a-input v-model:value="supplierForm.phone" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="风险状态"><a-select v-model:value="supplierForm.riskStatus" :options="supplierRiskOptions" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal v-model:open="requestOpen" :title="editingRequestId ? '编辑采购申请' : '新增采购申请'" width="760px" :confirm-loading="savingRequest" @ok="handleSaveRequest">
      <a-form ref="requestFormRef" :model="requestForm" :rules="requestRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="8"><a-form-item label="申请人" name="requesterName"><a-input v-model:value="requestForm.requesterName" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="期望到货"><a-input v-model:value="requestForm.expectedDate" type="date" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="关联物料" name="partId"><a-select v-model:value="requestForm.partId" :options="partOptions" show-search option-filter-prop="label" placeholder="选择物料" @change="syncRequestPart" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="采购物料名称"><a-input v-model:value="requestForm.partName" disabled /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="成本归属" name="costType"><a-segmented v-model:value="requestForm.costType" :options="costTypeOptions" @change="changeCostType" /></a-form-item></a-col>
          <a-col v-if="requestForm.costType === 'PROJECT'" :span="24"><a-form-item label="关联项目" name="projectId" :rules="[{ required: true, message: '请选择成本项目' }]"><a-select v-model:value="requestForm.projectId" :options="projectOptions" show-search option-filter-prop="label" placeholder="选择已审批且未关闭的项目" /></a-form-item></a-col>
          <a-col v-else :span="24"><a-form-item label="成本部门" name="departmentId" :rules="[{ required: true, message: '请选择成本部门' }]"><a-select v-model:value="requestForm.departmentId" :options="departmentOptions" show-search option-filter-prop="label" placeholder="选择承担采购成本的部门" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="数量" name="quantity"><a-input-number v-model:value="requestForm.quantity" :min="0.01" :precision="2" class="full-input" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="预计单价"><a-input-number v-model:value="requestForm.unitPrice" :min="0" :precision="2" class="full-input" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="税率(%)"><a-input-number v-model:value="requestForm.taxRate" :min="0" :max="100" :precision="2" class="full-input" /></a-form-item></a-col>
          <a-col :xs="24" :md="16"><a-form-item label="采购原因"><a-input v-model:value="requestForm.reason" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal v-model:open="approvalOpen" title="采购申请审批" width="720px" :confirm-loading="savingApproval" @ok="handleApproval">
      <a-alert v-if="selectedRequest" class="section-alert" type="info" :message="`${selectedRequest.code} · ${selectedRequest.partName} · ${costTypeLabel(selectedRequest.costType)}：${selectedRequest.costTargetName}`" />
      <a-form ref="approvalFormRef" :model="approvalForm" :rules="approvalRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="10"><a-form-item label="审批结论" name="decision"><a-radio-group v-model:value="approvalForm.decision" button-style="solid"><a-radio-button value="APPROVED">通过</a-radio-button><a-radio-button value="REJECTED">驳回</a-radio-button></a-radio-group></a-form-item></a-col>
          <a-col :xs="24" :md="14"><a-form-item label="审批人" name="approverName"><a-input v-model:value="approvalForm.approverName" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="审批意见" name="comment"><a-textarea v-model:value="approvalForm.comment" :rows="3" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal v-model:open="orderOpen" title="根据审批申请创建采购订单" width="760px" :confirm-loading="savingOrder" @ok="handleCreateOrder">
      <a-form ref="orderFormRef" :model="orderForm" :rules="orderRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="16"><a-form-item label="关联采购申请" name="requestId"><a-select v-model:value="orderForm.requestId" :options="requestOptions" show-search option-filter-prop="label" placeholder="仅显示已审批且未下单的申请" @change="syncOrderRequest" /></a-form-item></a-col>
          <a-col :xs="24" :md="16"><a-form-item label="供应商" name="supplierId"><a-select v-model:value="orderForm.supplierId" :options="supplierOptions" show-search option-filter-prop="label" placeholder="停用供应商不会显示" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="预计到货"><a-input v-model:value="orderForm.expectedDeliveryDate" type="date" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="采购数量"><a-input :value="selectedOrderRequest ? formatQuantity(selectedOrderRequest.quantity) : ''" disabled /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="含税单价" name="unitPrice"><a-input-number v-model:value="orderForm.unitPrice" :min="0.01" :precision="2" class="full-input" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="税率(%)"><a-input-number v-model:value="orderForm.taxRate" :min="0" :max="100" :precision="2" class="full-input" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="订单金额"><a-input :value="formatMoney(orderAmount)" disabled /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="成本归属"><a-input :value="selectedOrderRequest ? `${costTypeLabel(selectedOrderRequest.costType)} · ${selectedOrderRequest.costTargetName}` : ''" disabled /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal v-model:open="receiptOpen" title="采购订单到货入库" width="760px" :confirm-loading="savingReceipt" @ok="handleReceive">
      <a-alert v-if="selectedOrder" class="section-alert" type="info" :message="`${selectedOrder.code} · 剩余可收 ${formatQuantity(remainingQuantity)} · ${costTypeLabel(selectedOrder.costType)}：${selectedOrder.costTargetName}`" />
      <a-form ref="receiptFormRef" :model="receiptForm" :rules="receiptRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="8"><a-form-item label="本次到货数量" name="quantity"><a-input-number v-model:value="receiptForm.quantity" :min="0.01" :max="remainingQuantity" :precision="2" class="full-input" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="到货日期" name="receivedDate"><a-input v-model:value="receiptForm.receivedDate" type="date" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="应付到期日" name="payableDueDate"><a-input v-model:value="receiptForm.payableDueDate" type="date" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="送货单号" name="deliveryNo"><a-input v-model:value="receiptForm.deliveryNo" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="收货人" name="receiverName"><a-input v-model:value="receiptForm.receiverName" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from "vue";
import { message, Modal } from "ant-design-vue";
import { useRoute } from "vue-router";
import PlusOutlined from "@ant-design/icons-vue/PlusOutlined";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import ShoppingCartOutlined from "@ant-design/icons-vue/ShoppingCartOutlined";
import {
  cancelPurchaseOrder,
  createPurchaseOrder,
  createPurchaseRequest,
  createSupplier,
  listGoodsReceipts,
  listProcurementCostAllocations,
  listProcurementCostTargets,
  listProcurementPayables,
  listPurchaseOrders,
  listPurchaseRequests,
  listSuppliers,
  processPurchaseRequestApproval,
  updatePurchaseRequest,
  receivePurchaseOrder,
  type ApprovalStatus,
  type CreatePurchaseOrderPayload,
  type CreatePurchaseRequestPayload,
  type CreateSupplierPayload,
  type GoodsReceipt,
  type PayableStatus,
  type ProcurementCostAllocation,
  type ProcurementCostTargetOptions,
  type ProcurementCostType,
  type ProcurementPayable,
  type PurchaseOrder,
  type PurchaseOrderStatus,
  type PurchaseRequest,
  type PurchaseRequestStatus,
  type Supplier,
  type SupplierRiskStatus,
} from "@/api/procurement";
import { listInventoryParts, type InventoryPart } from "@/api/inventory";
import { useAuthStore } from "@/stores/auth";
import { downloadCsv } from "@/views/crm/crm-export";

type ApprovalForm = { decision: ApprovalStatus; comment: string; approverName: string };
type ReceiptForm = { quantity: number; receivedDate: string; deliveryNo: string; receiverName: string; payableDueDate: string };

const auth = useAuthStore();
const activeTab = ref("requests");
function handleExportRequests() {
  const headers = ["申请编号", "物料", "规格型号", "数量", "单价", "金额", "需求日期", "状态", "申请人", "说明"];
  const rows = purchaseRequests.value.map((r: any) => [
    r.code || "", r.materialName || "", r.materialSpec || "", String(r.quantity || 0),
    String(r.unitPrice || 0), String(r.totalAmount || 0), r.requiredDate || "",
    r.status || "", r.applicantName || "", r.reason || "",
  ]);
  downloadCsv("采购申请.csv", headers, rows);
}

function handleTabChange(key: string) { nextTick(() => { activeTab.value = key; }); }

const route = useRoute();
const tabPathMap: Record<string, string> = {
  requests: "/procurement/requests", orders: "/procurement/orders",
  receipts: "/procurement/receipts", costs: "/procurement/costs",
  payables: "/procurement/payables", suppliers: "/procurement/suppliers",
};
function tabFromPath(p: string) {
  if (p.includes("/orders")) return "orders";
  if (p.includes("/receipts")) return "receipts";
  if (p.includes("/costs")) return "costs";
  if (p.includes("/payables")) return "payables";
  if (p.includes("/suppliers")) return "suppliers";
  return "requests";
}
watch(() => route.path, (path) => { const t = tabFromPath(path); if (t) activeTab.value = t; }, { immediate: true });
const suppliers = ref<Supplier[]>([]);
const purchaseRequests = ref<PurchaseRequest[]>([]);
const purchaseOrders = ref<PurchaseOrder[]>([]);
const goodsReceipts = ref<GoodsReceipt[]>([]);
const payables = ref<ProcurementPayable[]>([]);
const costAllocations = ref<ProcurementCostAllocation[]>([]);
const costTargets = ref<ProcurementCostTargetOptions>({ projects: [], departments: [] });
const costFilter = ref<"ALL" | ProcurementCostType>("ALL");
const parts = ref<InventoryPart[]>([]);
const loading = ref(false);
const requestLoading = ref(false);
const orderLoading = ref(false);
const errorMessage = ref("");

// Pagination state
const requestPage = ref(0);
const requestSize = ref(10);
const requestTotal = ref(0);
const orderPage = ref(0);
const orderSize = ref(10);
const orderTotal = ref(0);

// Filter state
const requestFilters = reactive<{ status?: PurchaseRequestStatus; approvalStatus?: ApprovalStatus; costType?: ProcurementCostType; search: string }>({
  status: undefined,
  approvalStatus: undefined,
  costType: undefined,
  search: "",
});
const orderFilters = reactive<{ status?: PurchaseOrderStatus; costType?: ProcurementCostType; search: string }>({
  status: undefined,
  costType: undefined,
  search: "",
});
const supplierOpen = ref(false);
const editingRequestId = ref<string | null>(null);
const requestOpen = ref(false);
const approvalOpen = ref(false);
const orderOpen = ref(false);
const receiptOpen = ref(false);
const savingSupplier = ref(false);
const savingRequest = ref(false);
const savingApproval = ref(false);
const savingOrder = ref(false);
const savingReceipt = ref(false);
const supplierFormRef = ref();
const requestFormRef = ref();
const approvalFormRef = ref();
const orderFormRef = ref();
const receiptFormRef = ref();
const selectedRequest = ref<PurchaseRequest | null>(null);
const selectedOrder = ref<PurchaseOrder | null>(null);
const supplierForm = reactive<CreateSupplierPayload>(initialSupplierForm());
const requestForm = reactive<CreatePurchaseRequestPayload>(initialRequestForm());
const approvalForm = reactive<ApprovalForm>(initialApprovalForm());
const orderForm = reactive<CreatePurchaseOrderPayload>(initialOrderForm());
const receiptForm = reactive<ReceiptForm>(initialReceiptForm());

// Filter options
const requestStatusOptions = [
  { value: "SUBMITTED", label: "已提交" },
  { value: "APPROVED", label: "已审批" },
  { value: "ORDERED", label: "已下单" },
  { value: "RECEIVED", label: "已到货" },
  { value: "CANCELLED", label: "已取消" },
];
const requestApprovalOptions = [
  { value: "PENDING", label: "待审批" },
  { value: "APPROVED", label: "审批通过" },
  { value: "REJECTED", label: "已驳回" },
];
const orderStatusOptions = [
  { value: "ORDERED", label: "已下单" },
  { value: "PARTIAL_RECEIVED", label: "部分到货" },
  { value: "RECEIVED", label: "已到货" },
  { value: "CANCELLED", label: "已取消" },
];

const requestColumns = [
  { title: "申请", key: "code", width: 260, sorter: true },
  { title: "物料 / 数量", key: "part", width: 210 },
  { title: "成本归属", key: "costTarget", width: 220 },
  { title: "期望到货", dataIndex: "expectedDate", width: 120, sorter: true },
  { title: "状态 / 审批", key: "status", width: 300 },
  { title: "操作", key: "action", width: 120, fixed: "right" },
];
const orderColumns = [
  { title: "订单", key: "code", width: 230, sorter: true },
  { title: "供应商", dataIndex: "supplierName", width: 200 },
  { title: "物料 / 单价", key: "part", width: 220 },
  { title: "成本归属", key: "costTarget", width: 200 },
  { title: "已收 / 订购", key: "quantity", width: 160 },
  { title: "订单金额", key: "amount", width: 140, sorter: true },
  { title: "预计到货", dataIndex: "expectedDeliveryDate", width: 120, sorter: true },
  { title: "状态", key: "status", width: 110 },
  { title: "操作", key: "action", width: 120, fixed: "right" },
];
const receiptColumns = [
  { title: "入库单", key: "receipt", width: 250 },
  { title: "物料", dataIndex: "partName", width: 190 },
  { title: "数量 / 单价", key: "quantity", width: 190 },
  { title: "入库金额", key: "amount", width: 140 },
  { title: "成本归属", key: "costTarget", width: 200 },
  { title: "到货日期", dataIndex: "receivedDate", width: 120 },
  { title: "送货单号", dataIndex: "deliveryNo", width: 150 },
  { title: "收货人", dataIndex: "receiverName", width: 110 },
];
const payableColumns = [
  { title: "应付单", key: "payable", width: 250 },
  { title: "供应商", dataIndex: "supplierName", width: 220 },
  { title: "金额", key: "amount", width: 180 },
  { title: "成本归属", key: "costTarget", width: 200 },
  { title: "已付金额", dataIndex: "paidAmount", customRender: ({ text }: { text: number }) => formatMoney(text), width: 140 },
  { title: "到期日", dataIndex: "dueDate", width: 120 },
  { title: "状态", key: "status", width: 160 },
];
const supplierColumns = [
  { title: "供应商", key: "name", width: 300 },
  { title: "联系人", dataIndex: "contactName", width: 120 },
  { title: "电话", dataIndex: "phone", width: 140 },
  { title: "账期", dataIndex: "settlementTerms", width: 180 },
  { title: "风险", key: "risk", width: 110 },
];
const costAllocationColumns = [
  { title: "来源单据", key: "source", width: 220 },
  { title: "成本归属", key: "costTarget", width: 260 },
  { title: "采购物料", dataIndex: "partName", width: 220 },
  { title: "归集金额", key: "amount", width: 150 },
  { title: "归集日期", dataIndex: "incurredDate", width: 130 },
];

const supplierRiskOptions = [
  { label: "正常", value: "NORMAL" },
  { label: "关注", value: "WATCHLIST" },
  { label: "停用", value: "BLOCKED" },
];
const costTypeOptions = [
  { label: "项目采购", value: "PROJECT" },
  { label: "部门采购", value: "DEPARTMENT" },
];
const costFilterOptions = [
  { label: "全部", value: "ALL" },
  ...costTypeOptions,
];
const supplierRules = { code: [{ required: true, message: "请输入供应商编码" }], name: [{ required: true, message: "请输入供应商名称" }] };
const requestRules = {
  
  requesterName: [{ required: true, message: "请输入申请人" }],
  partId: [{ required: true, message: "请选择关联物料" }],
  quantity: [{ required: true, message: "请输入数量" }],
  costType: [{ required: true, message: "请选择成本归属" }],
};
const approvalRules = {
  decision: [{ required: true, message: "请选择审批结论" }],
  approverName: [{ required: true, message: "请输入审批人" }],
  comment: [{ required: true, message: "请输入审批意见" }],
};
const orderRules = {
  
  requestId: [{ required: true, message: "请选择已审批采购申请" }],
  supplierId: [{ required: true, message: "请选择供应商" }],
  unitPrice: [{ required: true, message: "请输入含税单价" }],
};
const receiptRules = {
  quantity: [{ required: true, message: "请输入本次到货数量" }],
  receivedDate: [{ required: true, message: "请选择到货日期" }],
  payableDueDate: [{ required: true, message: "请选择应付到期日" }],
  deliveryNo: [{ required: true, message: "请输入送货单号" }],
  receiverName: [{ required: true, message: "请输入收货人" }],
};

const pendingApprovalCount = computed(() => purchaseRequests.value.filter((item) => item.approvalStatus === "PENDING").length);
const openOrderCount = computed(() => purchaseOrders.value.filter(canReceive).length);
const receiptAmount = computed(() => goodsReceipts.value.reduce((sum, item) => sum + Number(item.amount || 0), 0));
const outstandingPayable = computed(() => payables.value.reduce((sum, item) => sum + Number(item.outstandingAmount || 0), 0));
const projectCostAmount = computed(() => costAllocations.value.filter((item) => item.costType === "PROJECT").reduce((sum, item) => sum + Number(item.amount || 0), 0));
const departmentCostAmount = computed(() => costAllocations.value.filter((item) => item.costType === "DEPARTMENT").reduce((sum, item) => sum + Number(item.amount || 0), 0));
const filteredCostAllocations = computed(() => costFilter.value === "ALL" ? costAllocations.value : costAllocations.value.filter((item) => item.costType === costFilter.value));
const orderedRequestIds = computed(() => new Set(purchaseOrders.value.map((item) => item.requestId).filter(Boolean)));
const partOptions = computed(() => parts.value.map((part) => ({ label: `${part.name} (${part.code}) · 库存 ${formatQuantity(part.stockQty)}`, value: part.id })));
const supplierOptions = computed(() => suppliers.value.filter((item) => item.riskStatus !== "BLOCKED").map((item) => ({ label: `${item.name} (${item.code})${item.riskStatus === "WATCHLIST" ? " · 关注" : ""}`, value: item.id })));
const projectOptions = computed(() => costTargets.value.projects.map((item) => ({ label: `${item.name} (${item.code})`, value: item.id })));
const departmentOptions = computed(() => costTargets.value.departments.map((item) => ({ label: `${item.name} (${item.code})`, value: item.id })));
const requestOptions = computed(() => purchaseRequests.value
  .filter((item) => item.approvalStatus === "APPROVED" && item.status === "APPROVED" && !orderedRequestIds.value.has(item.id))
  .map((item) => ({ label: `${item.code} · ${item.partName} · ${item.costTargetName}`, value: item.id })));
const selectedOrderRequest = computed(() => purchaseRequests.value.find((item) => item.id === orderForm.requestId) || null);
const orderAmount = computed(() => Number(selectedOrderRequest.value?.quantity || 0) * Number(orderForm.unitPrice || 0));
const remainingQuantity = computed(() => Math.max(0, Number(selectedOrder.value?.orderedQty || 0) - Number(selectedOrder.value?.receivedQty || 0)));

onMounted(loadData);

async function loadData() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const [supplierData, requestData, orderData, partData, receiptData, payableData, targetData, allocationData] = await Promise.all([
      listSuppliers(), listPurchaseRequests(), listPurchaseOrders(), listInventoryParts(), listGoodsReceipts(),
      auth.can("procurement:payable:view") ? listProcurementPayables() : Promise.resolve([]),
      listProcurementCostTargets(), listProcurementCostAllocations(),
    ]);
    suppliers.value = supplierData.content || supplierData;
    purchaseRequests.value = requestData.content || requestData;
    purchaseOrders.value = orderData.content || orderData;
    parts.value = partData;
    goodsReceipts.value = receiptData;
    payables.value = payableData;
    costTargets.value = targetData;
    costAllocations.value = allocationData;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "采购数据加载失败";
  } finally {
    loading.value = false;
  }
}

const selectedRequestKeys = ref<string[]>([]);
function handleRequestFilter() { loadRequests(); }
function handleRequestPageChange() { loadRequests(); }

async function loadRequests() {
  try{const result=await listPurchaseRequests({page:0,size:999});purchaseRequests.value=result.content;}catch(e:any){message.error(e instanceof Error?e.message:'加载采购申请失败');}
}
async function loadOrders() {
  try{const result=await listPurchaseOrders({page:0,size:999});purchaseOrders.value=result.content;}catch(e:any){message.error(e instanceof Error?e.message:'加载采购订单失败');}
}

function openSupplier() { Object.assign(supplierForm, initialSupplierForm()); supplierOpen.value = true; }
function openEditRequest(record: PurchaseRequest) {
  editingRequestId.value = record.id;
  Object.assign(requestForm, {
    requesterName: record.requesterName,
    partId: record.partId as any,
    partName: record.partName,
    quantity: record.quantity,
    unitPrice: record.unitPrice || 0,
    taxRate: record.taxRate ?? 13,
    expectedDate: record.expectedDate,
    reason: record.reason || '',
    costType: record.costType,
    projectId: record.projectId as any,
    departmentId: record.departmentId as any,
  });
  requestOpen.value = true;
}

function openRequest() { editingRequestId.value = null; Object.assign(requestForm, initialRequestForm()); requestOpen.value = true; }
function openApproval(record: PurchaseRequest) {
  selectedRequest.value = record;
  Object.assign(approvalForm, initialApprovalForm());
  approvalOpen.value = true;
}
function openOrder() {
  if (!requestOptions.value.length) { message.warning("暂无已审批且未下单的采购申请"); return; }
  Object.assign(orderForm, initialOrderForm());
  orderOpen.value = true;
}
function openReceipt(record: PurchaseOrder) {
  selectedOrder.value = record;
  Object.assign(receiptForm, initialReceiptForm(), { quantity: Number(record.orderedQty) - Number(record.receivedQty) });
  receiptOpen.value = true;
}

function syncRequestPart(partId: string) { requestForm.partName = parts.value.find((item) => item.id === partId)?.name || ""; }
function changeCostType(value: string | number) {
  requestForm.costType = value as ProcurementCostType;
  requestForm.projectId = undefined;
  requestForm.departmentId = undefined;
  requestFormRef.value?.clearValidate(["projectId", "departmentId"]);
}
function syncOrderRequest(requestId: string) {
  const request = purchaseRequests.value.find((item) => item.id === requestId);
  const part = parts.value.find((item) => item.id === request?.partId);
  orderForm.expectedDeliveryDate = request?.expectedDate;
  orderForm.unitPrice = Number(request?.unitPrice || 0) > 0 ? Number(request?.unitPrice) : (Number(part?.unitCost || 0) > 0 ? Number(part?.unitCost) : 0.01);
  orderForm.taxRate = request?.taxRate ?? 13;
}

async function handleCreateSupplier() {
  await supplierFormRef.value?.validate();
  savingSupplier.value = true;
  try { await createSupplier({ ...supplierForm }); supplierOpen.value = false; message.success("供应商已新增"); await loadData(); }
  catch (error) { message.error(error instanceof Error ? error.message : "供应商新增失败"); }
  finally { savingSupplier.value = false; }
}
async function handleSaveRequest() {
  await requestFormRef.value?.validate();
  savingRequest.value = true;
  try {
    if (editingRequestId.value) {
      await updatePurchaseRequest(editingRequestId.value, { ...requestForm });
      message.success("采购申请已更新");
    } else {
      await createPurchaseRequest({ ...requestForm });
      message.success("采购申请已提交审批");
    }
    requestOpen.value = false;
    editingRequestId.value = null;
    await loadRequests();
  } catch (error) { message.error(error instanceof Error ? error.message : "操作失败"); }
  finally { savingRequest.value = false; }

function handleCancelOrder(record: PurchaseOrder) {
  Modal.confirm({
    title: "确认取消订单",
    content: "确定要取消订单 " + record.code + " 吗？此操作不可撤销。",
    okText: "确认取消",
    okType: "danger",
    cancelText: "暂不取消",
    onOk: async () => {
      try {
        await cancelPurchaseOrder(record.id);
        message.success("采购订单已取消");
        await loadOrders();
      } catch (error) {
        message.error(error instanceof Error ? error.message : "取消失败");
      }
    },
  });
}
}
async function handleApproval() {
  if (!selectedRequest.value) return;
  await approvalFormRef.value?.validate();
  savingApproval.value = true;
  try {
    await processPurchaseRequestApproval(selectedRequest.value.id, { ...approvalForm });
    approvalOpen.value = false;
    message.success(approvalForm.decision === "APPROVED" ? "采购申请已审批通过" : "采购申请已驳回");
    await loadRequests();
  } catch (error) { message.error(error instanceof Error ? error.message : "审批处理失败"); }
  finally { savingApproval.value = false; }
}
async function handleCancelOrder(record: PurchaseOrder) {
  message.warning('订单取消失败：该功能尚未实现');
}

async function handleCreateOrder() {
  await orderFormRef.value?.validate();
  savingOrder.value = true;
  try { await createPurchaseOrder({ ...orderForm }); orderOpen.value = false; activeTab.value = "orders"; message.success("采购订单已创建"); await loadOrders(); }
  catch (error) { message.error(error instanceof Error ? error.message : "采购订单新增失败"); }
  finally { savingOrder.value = false; }
}
async function handleReceive() {
  if (!selectedOrder.value) return;
  await receiptFormRef.value?.validate();
  savingReceipt.value = true;
  try {
    const result = await receivePurchaseOrder(selectedOrder.value.id, { ...receiptForm });
    receiptOpen.value = false;
    message.success(`入库完成，${result.costAllocation.costTargetName} 成本 ${formatMoney(result.costAllocation.amount)} 已归集`);
    await loadData();
  } catch (error) { message.error(error instanceof Error ? error.message : "到货入库失败"); }
  finally { savingReceipt.value = false; }
}

function initialSupplierForm(): CreateSupplierPayload { return { code: "", name: "", category: "", contactName: "", phone: "", settlementTerms: "", riskStatus: "NORMAL" }; }
function initialRequestForm(): CreatePurchaseRequestPayload { return { code: "", requesterName: auth.user?.displayName || "", partId: undefined, partName: "", quantity: 1, unitPrice: 0, taxRate: 13, expectedDate: addDays(7), reason: "", costType: "DEPARTMENT", projectId: undefined, departmentId: undefined }; }
function initialApprovalForm(): ApprovalForm { return { decision: "APPROVED", comment: "同意采购", approverName: auth.user?.displayName || "" }; }
function initialOrderForm(): CreatePurchaseOrderPayload { return { code: "", supplierId: "", requestId: "", unitPrice: 0.01, taxRate: 13, expectedDeliveryDate: undefined }; }
function initialReceiptForm(): ReceiptForm { return { quantity: 1, receivedDate: addDays(0), deliveryNo: "", receiverName: auth.user?.displayName || "", payableDueDate: addDays(30) }; }
function addDays(days: number) { const date = new Date(); date.setDate(date.getDate() + days); return date.toISOString().slice(0, 10); }

function canReceive(order: PurchaseOrder) { return order.status === "ORDERED" || order.status === "PARTIAL_RECEIVED"; }
function receivedPercent(order: PurchaseOrder) { return order.orderedQty > 0 ? Math.min(100, Math.round((Number(order.receivedQty) / Number(order.orderedQty)) * 100)) : 0; }
function isOverdue(item: ProcurementPayable) { return item.status !== "PAID" && item.status !== "CANCELLED" && item.dueDate < addDays(0); }
function moneyFormatter(value: number | string) { return formatMoney(Number(value)); }
function formatMoney(value: number) { return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY", minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value || 0); }
function formatQuantity(value: number) { return new Intl.NumberFormat("zh-CN", { maximumFractionDigits: 2 }).format(value || 0); }
function formatTaxRate(value?: number) { return `${Number(value ?? 13).toFixed(2).replace(/\.?0+$/, "")}%`; }
function supplierRiskLabel(status: SupplierRiskStatus) { return supplierRiskOptions.find((item) => item.value === status)?.label || status; }
function supplierRiskColor(status: SupplierRiskStatus) { return ({ NORMAL: "green", WATCHLIST: "orange", BLOCKED: "red" } as Record<SupplierRiskStatus, string>)[status]; }
function costTypeLabel(type: ProcurementCostType) { return type === "PROJECT" ? "项目采购" : "部门采购"; }
function costTypeColor(type: ProcurementCostType) { return type === "PROJECT" ? "blue" : "cyan"; }
function requestLabel(status: PurchaseRequestStatus) { return ({ DRAFT: "草稿", SUBMITTED: "已提交", APPROVED: "已审批", ORDERED: "已下单", RECEIVED: "已到货", CANCELLED: "已取消" } as Record<PurchaseRequestStatus, string>)[status]; }
function requestColor(status: PurchaseRequestStatus) { return ({ DRAFT: "default", SUBMITTED: "blue", APPROVED: "green", ORDERED: "purple", RECEIVED: "cyan", CANCELLED: "red" } as Record<PurchaseRequestStatus, string>)[status]; }
function approvalLabel(status: ApprovalStatus) { return ({ PENDING: "待审批", APPROVED: "审批通过", REJECTED: "已驳回" } as Record<ApprovalStatus, string>)[status]; }
function approvalColor(status: ApprovalStatus) { return ({ PENDING: "orange", APPROVED: "green", REJECTED: "red" } as Record<ApprovalStatus, string>)[status]; }
function orderLabel(status: PurchaseOrderStatus) { return ({ DRAFT: "草稿", ORDERED: "已下单", PARTIAL_RECEIVED: "部分到货", RECEIVED: "已到货", CLOSED: "已关闭", CANCELLED: "已取消" } as Record<PurchaseOrderStatus, string>)[status]; }
function orderColor(status: PurchaseOrderStatus) { return ({ DRAFT: "default", ORDERED: "blue", PARTIAL_RECEIVED: "orange", RECEIVED: "green", CLOSED: "default", CANCELLED: "red" } as Record<PurchaseOrderStatus, string>)[status]; }
function payableLabel(status: PayableStatus) { return ({ PENDING: "待付款", PARTIAL_PAID: "部分付款", PAID: "已付款", CANCELLED: "已取消" } as Record<PayableStatus, string>)[status]; }
function payableColor(status: PayableStatus) { return ({ PENDING: "orange", PARTIAL_PAID: "blue", PAID: "green", CANCELLED: "default" } as Record<PayableStatus, string>)[status]; }

function handleOrderFilter() {}
function handleOrderPageChange() {}
</script>
