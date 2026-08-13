<template>
  <BusinessDetailPage
    :title="
      order ? `${order.supplierName || '供应商'}采购订单` : '采购订单详情'
    "
    :code="order?.code"
    :subtitle="order ? `${order.partName} · ${order.costTargetName}` : ''"
    :loading="loading"
    back-to="/procurement/orders"
    :status-label="statusLabel(order?.status)"
    :status-color="
      order?.status === 'CLOSED' || order?.status === 'RECEIVED'
        ? 'green'
        : order?.status === 'CANCELLED'
          ? 'red'
          : 'blue'
    "
    :risk-label="riskLabel"
    :risk-color="riskLabel ? 'orange' : 'green'"
    :stats="metrics"
    @refresh="loadData"
  >
    <template #actions>
      <a-button
        v-if="order?.supplierId"
        @click="router.push(`/procurement/suppliers/${order.supplierId}`)"
        >查看供应商</a-button
      >
      <a-button
        v-if="
          order?.status === 'DRAFT' &&
          auth.can('procurement:purchase:create') &&
          (!order.submittedAt || order.approvalStatus === 'REJECTED')
        "
        type="primary"
        :loading="saving"
        @click="handleSubmit"
        >提交订单审批</a-button
      >
      <a-button
        v-if="
          order?.approvalStatus === 'PENDING' &&
          order?.submittedAt &&
          auth.can('procurement:request:approve')
        "
        type="primary"
        @click="approvalOpen = true"
        >审批订单</a-button
      >
    </template>
    <template #relations>
      <a-steps size="small" :current="currentStep" responsive>
        <a-step
          title="采购申请"
          :description="order?.requestCode || '未关联'"
        />
        <a-step title="询价定标" :description="inquiry?.code || '直接采购'" />
        <a-step
          title="订单审批"
          :description="approvalLabel(order?.approvalStatus)"
        />
        <a-step title="到货质检" :description="`${receipts.length} 笔到货`" />
        <a-step title="发票应付" :description="`${invoices.length} 张发票`" />
        <a-step title="付款核销" :description="money(paidAmount)" />
      </a-steps>
    </template>

    <a-card v-if="order" :bordered="false">
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="overview" tab="订单总览">
          <a-row :gutter="[16, 16]">
            <a-col :xs="24" :xl="14">
              <a-descriptions
                bordered
                :column="{ xs: 1, md: 2 }"
                size="small"
                title="订单信息"
              >
                <a-descriptions-item label="供应商"
                  ><a
                    @click="
                      router.push(`/procurement/suppliers/${order.supplierId}`)
                    "
                    >{{ order.supplierName || "-" }}</a
                  ></a-descriptions-item
                >
                <a-descriptions-item label="采购申请">{{
                  order.requestCode || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="负责人">{{
                  order.responsibleName || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="物料">{{
                  order.partName
                }}</a-descriptions-item>
                <a-descriptions-item label="成本归属"
                  >{{ order.costTargetName }}（{{
                    order.costTargetCode
                  }}）</a-descriptions-item
                >
                <a-descriptions-item label="订购/已收"
                  >{{ order.orderedQty }} /
                  {{ order.receivedQty }}</a-descriptions-item
                >
                <a-descriptions-item label="预计交付">{{
                  order.expectedDeliveryDate || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="单价（含税，元）">{{
                  money(order.unitPrice)
                }}</a-descriptions-item>
                <a-descriptions-item label="税率"
                  >{{ Number(order.taxRate || 0) }}%</a-descriptions-item
                >
                <a-descriptions-item label="订单金额（含税，元）" :span="2"
                  ><strong>{{
                    money(order.orderAmount)
                  }}</strong></a-descriptions-item
                >
                <a-descriptions-item label="订单版本"
                  >V{{ order.orderVersion || 1 }}</a-descriptions-item
                >
                <a-descriptions-item label="币种">{{
                  order.currency || "CNY"
                }}</a-descriptions-item>
                <a-descriptions-item label="运费">{{
                  money(order.freightAmount || 0)
                }}</a-descriptions-item>
                <a-descriptions-item label="询价单">{{
                  order.inquiryCode || order.inquiryId || "未关联"
                }}</a-descriptions-item>
                <a-descriptions-item label="订单合同"
                  ><strong v-if="order.contractNo">{{
                    order.contractNo
                  }}</strong
                  ><span v-else>{{ order.contractId || "未关联" }}</span
                  ><a-tag
                    v-if="order.contractStatus === 'ACTIVE'"
                    color="green"
                    style="margin-left: 8px"
                    >合同已生效</a-tag
                  ><a-tag
                    v-else-if="order.contractStatus"
                    color="orange"
                    style="margin-left: 8px"
                    >待生效</a-tag
                  ><a-tag
                    v-if="order.contractAcknowledged"
                    color="green"
                    style="margin-left: 8px"
                    >供应商已确认{{
                      order.contractAcknowledgedByName
                        ? " · " + order.contractAcknowledgedByName
                        : ""
                    }}</a-tag
                  ><a-tag
                    v-else-if="order.contractId || order.contractNo"
                    color="default"
                    style="margin-left: 8px"
                    >待供应商确认</a-tag
                  ></a-descriptions-item
                >
                <a-descriptions-item
                  v-if="order.contractName"
                  label="合同名称"
                  >{{ order.contractName }}</a-descriptions-item
                >
                <a-descriptions-item
                  v-if="order.contractStartDate || order.contractEndDate"
                  label="合同有效期"
                  >{{ order.contractStartDate || "-" }} 至
                  {{ order.contractEndDate || "-" }}</a-descriptions-item
                >
                <a-descriptions-item
                  v-if="order.contractPaymentTerms"
                  label="付款条款"
                  >{{ order.contractPaymentTerms }}</a-descriptions-item
                >
                <a-descriptions-item label="寻源/直接采购依据" :span="2">{{
                  order.sourceReason || "-"
                }}</a-descriptions-item>
              </a-descriptions>
            </a-col>
            <a-col :xs="24" :xl="10">
              <a-descriptions
                bordered
                :column="1"
                size="small"
                title="审批与履约"
              >
                <a-descriptions-item label="审批状态">{{
                  approvalLabel(order.approvalStatus)
                }}</a-descriptions-item>
                <a-descriptions-item label="审批人">{{
                  order.approverName || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="审批时间">{{
                  dateTime(order.approvedAt)
                }}</a-descriptions-item>
                <a-descriptions-item label="审批意见">{{
                  order.approvalComment || "-"
                }}</a-descriptions-item>
                <a-descriptions-item label="订单状态">{{
                  statusLabel(order.status)
                }}</a-descriptions-item>
              </a-descriptions>
            </a-col>
          </a-row>
          <a-alert
            v-if="riskLabel"
            class="section-gap"
            type="warning"
            show-icon
            :message="riskLabel"
            :description="riskDescription"
          />
        </a-tab-pane>

        <a-tab-pane key="source" tab="申请与询价定标">
          <a-descriptions
            v-if="request"
            bordered
            :column="{ xs: 1, md: 2 }"
            size="small"
            title="采购申请"
          >
            <a-descriptions-item label="申请编号">{{
              request.code
            }}</a-descriptions-item
            ><a-descriptions-item label="申请人">{{
              request.applicantName || request.requesterName
            }}</a-descriptions-item>
            <a-descriptions-item label="物料"
              >{{ request.materialName || request.partName }}
              {{ request.materialSpec }}</a-descriptions-item
            ><a-descriptions-item label="数量"
              >{{ request.quantity }}
              {{ request.unit || "" }}</a-descriptions-item
            >
            <a-descriptions-item label="需求日期">{{
              request.requiredDate || request.expectedDate || "-"
            }}</a-descriptions-item
            ><a-descriptions-item label="成本归属">{{
              request.costTargetName
            }}</a-descriptions-item>
            <a-descriptions-item label="申请原因" :span="2">{{
              request.reason || request.description || "-"
            }}</a-descriptions-item>
          </a-descriptions>
          <a-empty v-else description="未找到关联采购申请" />
          <a-divider>询价与供应商报价</a-divider>
          <a-table
            :columns="quotationColumns"
            :data-source="inquiry?.quotes || []"
            row-key="id"
            :pagination="false"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'supplier'"
                ><strong>{{ record.supplierName }}</strong
                ><a-tag v-if="record.selected" color="green"
                  >已中选</a-tag
                ></template
              >
              <template v-else-if="column.key === 'price'">{{
                money(record.unitPrice)
              }}</template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="delivery" :tab="`到货质检 (${receipts.length})`">
          <a-table
            :columns="receiptColumns"
            :data-source="receipts"
            row-key="id"
            :scroll="{ x: 1050 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'amount'">{{
                money(record.amount)
              }}</template>
              <template v-else-if="column.key === 'inspection'"
                ><a-tag
                  :color="
                    record.inspectionStatus === 'PASSED'
                      ? 'green'
                      : record.inspectionStatus === 'REJECTED'
                        ? 'red'
                        : 'orange'
                  "
                  >{{ inspectionLabel(record.inspectionStatus) }}</a-tag
                ><span class="sub"
                  >合格 {{ record.qualifiedQty || 0 }} · 不合格
                  {{ record.rejectedQty || 0 }}</span
                ></template
              >
            </template>
          </a-table>
          <a-divider>退货记录</a-divider>
          <a-table
            :columns="returnColumns"
            :data-source="returns"
            row-key="id"
            size="small"
            :pagination="false"
            ><template #bodyCell="{ column, record }"
              ><template v-if="column.key === 'amount'">{{
                money(record.amount)
              }}</template></template
            ></a-table
          >
        </a-tab-pane>

        <a-tab-pane key="invoice" :tab="`发票与三单匹配 (${invoices.length})`">
          <a-table
            :columns="invoiceColumns"
            :data-source="invoices"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'amount'">{{
                money(record.amount)
              }}</template>
              <template v-else-if="column.key === 'difference'"
                ><span :class="{ danger: record.differenceAmount }">{{
                  money(record.differenceAmount)
                }}</span></template
              >
              <template v-else-if="column.key === 'match'"
                ><a-tag
                  :color="record.matchStatus === 'MATCHED' ? 'green' : 'red'"
                  >{{
                    record.matchStatus === "MATCHED" ? "匹配" : "差异"
                  }}</a-tag
                ></template
              >
            </template>
          </a-table>
          <a-result
            :status="
              matching?.matchStatus === 'MATCHED' ? 'success' : 'warning'
            "
            :title="
              matching?.matchStatus === 'MATCHED'
                ? '订单、收货、应付三单匹配'
                : '存在数量或金额差异'
            "
            :sub-title="matching?.riskMessage || '等待业务数据形成完整匹配结果'"
          />
        </a-tab-pane>

        <a-tab-pane
          key="payment"
          :tab="`应付付款 (${payables.length}/${applications.length})`"
        >
          <a-table
            :columns="payableColumns"
            :data-source="payables"
            row-key="id"
            :pagination="false"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'code'"
                ><a @click="router.push(`/finance/payables/${record.id}`)">{{
                  record.code || record.orderCode
                }}</a></template
              >
              <template
                v-else-if="
                  ['amount', 'paid', 'outstanding'].includes(column.key)
                "
                >{{
                  money(
                    column.key === "amount"
                      ? record.amount
                      : column.key === "paid"
                        ? record.paidAmount
                        : record.outstandingAmount,
                  )
                }}</template
              >
            </template>
          </a-table>
          <a-divider>付款申请</a-divider>
          <a-table
            :columns="applicationColumns"
            :data-source="applications"
            row-key="id"
            size="small"
          >
            <template #bodyCell="{ column, record }"
              ><template v-if="column.key === 'code'"
                ><a
                  @click="
                    router.push(`/finance/payment-applications/${record.id}`)
                  "
                  >{{ record.code }}</a
                ></template
              ><template v-else-if="column.key === 'amount'">{{
                money(record.requestedAmount)
              }}</template></template
            >
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="shipments" :tab="`发货协同 (${shipments.length})`">
          <a-alert
            type="info"
            show-icon
            message="供应商在门户回传的送货单号、承运方与预计到货时间，收货登记时可参考。"
          />
          <a-table
            :columns="shipmentColumns"
            :data-source="shipments"
            row-key="id"
            size="small"
            :pagination="false"
            :loading="loadingShipments"
            style="margin-top: 12px"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'arrival'">{{
                record.expectedArrival || "-"
              }}</template>
              <template v-else-if="column.key === 'date'">{{
                dateTime(record.createdAt)
              }}</template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="shipmentStatusColor(record.status)">{{
                  shipmentStatusText(record.status)
                }}</a-tag>
                <div
                  v-if="record.reviewComment"
                  class="shipment-review"
                >
                  <template v-if="record.status === 'REJECTED'"
                    >退回原因：</template
                  >{{ record.reviewComment }}
                </div>
              </template>
              <template v-else-if="column.key === 'actions'">
                <template v-if="record.status === 'PENDING'">
                  <a-popconfirm
                    v-if="canConfirmShipment"
                    title="确认该送货单已到货？"
                    @confirm="handleConfirmShipment(record)"
                  >
                    <a-button type="link" size="small">确认到货</a-button>
                  </a-popconfirm>
                  <a-button
                    v-if="canConfirmShipment"
                    type="link"
                    size="small"
                    danger
                    @click="openReject(record)"
                    >退回</a-button
                  >
                </template>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="documents" tab="合同附件">
          <a-space direction="vertical" style="width: 100%">
            <a-alert
              type="info"
              show-icon
              message="采购合同请分别上传原件和双方盖章件，供应商门户可同步查看下载。"
            />
            <a-space>
              <a-radio-group
                v-model:value="documentType"
                :disabled="uploadingDocument || !canManageDocuments"
              >
                <a-radio-button value="ORIGINAL">原件</a-radio-button>
                <a-radio-button value="STAMPED">盖章件</a-radio-button>
                <a-radio-button value="OTHER">其他附件</a-radio-button>
              </a-radio-group>
              <a-upload
                :show-upload-list="false"
                :before-upload="handleUploadDocument"
                :disabled="uploadingDocument || !canManageDocuments"
                accept=".jpg,.jpeg,.png,.webp,.pdf,.doc,.docx,.xls,.xlsx"
              >
                <a-button
                  v-if="canManageDocuments"
                  :loading="uploadingDocument"
                  type="primary"
                  ><template #icon><UploadOutlined /></template>上传{{
                    documentTypeLabel
                  }}</a-button
                >
              </a-upload>
            </a-space>
            <a-table
              :columns="documentColumns"
              :data-source="orderDocuments"
              row-key="id"
              size="small"
              :pagination="false"
              :loading="loadingDocuments"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'file'">
                  <a @click="downloadOrderDocument(record)">{{
                    record.fileName
                  }}</a>
                  <span class="sub">{{ formatBytes(record.sizeBytes) }}</span>
                </template>
                <template v-else-if="column.key === 'uploadedAt'">{{
                  dateTime(record.uploadedAt)
                }}</template>
                <template v-else-if="column.key === 'docType'">
                  <a-tag :color="docTypeColor(record.docType)">{{
                    docTypeLabel(record.docType)
                  }}</a-tag>
                </template>
                <template v-else-if="column.key === 'actions'">
                  <a-popconfirm
                    v-if="canManageDocuments"
                    title="确认删除该合同附件？"
                    @confirm="handleDeleteDocument(record)"
                  >
                    <a-button type="link" size="small" danger>删除</a-button>
                  </a-popconfirm>
                </template>
              </template>
            </a-table>
          </a-space>
        </a-tab-pane>

        <a-tab-pane key="changes" :tab="`订单变更 (${changes.length})`">
          <a-space direction="vertical" style="width: 100%">
            <a-alert
              type="info"
              show-icon
              message="数量、单价、交期变更通过变更单留痕，审批通过后自动应用到订单并将订单版本 +1。"
            />
            <a-space>
              <a-button
                v-if="auth.can('procurement:purchase:create')"
                type="primary"
                :disabled="
                  !order ||
                  order.status === 'CANCELLED' ||
                  order.status === 'CLOSED'
                "
                @click="openChange"
                ><template #icon><PlusOutlined /></template>发起变更</a-button
              >
            </a-space>
            <a-table
              :columns="changeColumns"
              :data-source="changes"
              row-key="id"
              size="small"
              :pagination="false"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'no'">
                  <strong>{{ record.changeNo }}</strong>
                  <span class="sub">订单 v{{ record.orderVersionBefore }}</span>
                </template>
                <template v-else-if="column.key === 'type'">
                  <a-tag>{{ changeTypeLabel(record.changeType) }}</a-tag>
                </template>
                <template v-else-if="column.key === 'qty'">
                  <span v-if="record.quantityBefore != null"
                    >{{ record.quantityBefore }} →
                    {{ record.quantityAfter }}</span
                  >
                  <span v-else>-</span>
                </template>
                <template v-else-if="column.key === 'price'">
                  <span v-if="record.unitPriceBefore != null"
                    >{{ money(record.unitPriceBefore) }} →
                    {{ money(record.unitPriceAfter) }}</span
                  >
                  <span v-else>-</span>
                </template>
                <template v-else-if="column.key === 'date'">
                  <span v-if="record.expectedDateBefore != null"
                    >{{ record.expectedDateBefore }} →
                    {{ record.expectedDateAfter }}</span
                  >
                  <span v-else>-</span>
                </template>
                <template v-else-if="column.key === 'status'">
                  <a-tag
                    :color="
                      record.status === 'APPROVED'
                        ? 'green'
                        : record.status === 'REJECTED'
                          ? 'red'
                          : 'orange'
                    "
                    >{{ changeStatusLabel(record.status) }}</a-tag
                  >
                  <span v-if="record.decidedByName" class="sub">{{
                    record.decidedByName
                  }}</span>
                </template>
                <template v-else-if="column.key === 'actions'">
                  <a-button
                    v-if="
                      record.status === 'PENDING' &&
                      auth.can('procurement:request:approve')
                    "
                    type="link"
                    size="small"
                    @click="openChangeDecide(record)"
                    >审批</a-button
                  >
                  <span v-else class="sub">{{
                    dateTime(record.appliedAt)
                  }}</span>
                </template>
              </template>
            </a-table>
          </a-space>
        </a-tab-pane>
        <a-tab-pane key="audit" tab="审批与审计">
          <a-timeline>
            <a-timeline-item color="green"
              ><strong>采购订单创建</strong>
              <p>
                {{ dateTime(order.createdAt || order.orderedAt) }} ·
                {{ order.code }}
              </p></a-timeline-item
            >
            <a-timeline-item
              :color="
                order.approvalStatus === 'APPROVED'
                  ? 'green'
                  : order.approvalStatus === 'REJECTED'
                    ? 'red'
                    : 'blue'
              "
              ><strong
                >订单审批：{{ approvalLabel(order.approvalStatus) }}</strong
              >
              <p>
                {{ order.approverName || "等待审批" }} ·
                {{ order.approvalComment || "暂无意见" }}
              </p></a-timeline-item
            >
            <a-timeline-item
              v-for="receipt in receipts"
              :key="receipt.id"
              :color="
                receipt.inspectionStatus === 'PASSED' ? 'green' : 'orange'
              "
              ><strong
                >{{ receipt.code }} 到货并{{
                  inspectionLabel(receipt.inspectionStatus)
                }}</strong
              >
              <p>
                {{ receipt.receiverName }} · {{ receipt.receivedDate }} · 数量
                {{ receipt.quantity }}
              </p></a-timeline-item
            >
          </a-timeline>
        </a-tab-pane>
      </a-tabs>
    </a-card>
    <a-result v-else-if="!loading" status="404" title="采购订单不存在" />
    <a-modal
      v-model:open="approvalOpen"
      title="采购订单审批"
      :confirm-loading="saving"
      @ok="handleApproval"
    >
      <a-form layout="vertical">
        <a-form-item label="审批结论"
          ><a-radio-group
            v-model:value="approvalForm.decision"
            button-style="solid"
            ><a-radio-button value="APPROVED">通过</a-radio-button
            ><a-radio-button value="REJECTED"
              >驳回</a-radio-button
            ></a-radio-group
          ></a-form-item
        >
        <a-form-item label="审批意见"
          ><a-textarea v-model:value="approvalForm.comment" :rows="3"
        /></a-form-item>
        <a-form-item label="审批人"
          ><a-input v-model:value="approvalForm.approverName"
        /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="changeOpen"
      title="发起订单变更"
      width="640px"
      :confirm-loading="changeSaving"
      @ok="submitChange"
    >
      <a-form layout="vertical">
        <a-alert
          v-if="order"
          class="section-alert"
          type="info"
          :message="`${order.code} · 当前数量 ${order.orderedQty} · 单价 ${money(order.unitPrice)} · 版本 v${order.orderVersion || 1}`"
        />
        <a-form-item label="变更后数量">
          <a-input-number
            v-model:value="changeForm.quantityAfter"
            :min="0.01"
            :precision="2"
            class="full-input"
          />
        </a-form-item>
        <a-form-item label="变更后单价（含税，元）">
          <a-input-number
            v-model:value="changeForm.unitPriceAfter"
            :min="0.01"
            :precision="2"
            class="full-input"
          />
        </a-form-item>
        <a-form-item label="变更后期望交期">
          <a-input v-model:value="changeForm.expectedDateAfter" type="date" />
        </a-form-item>
        <a-form-item label="变更原因" required>
          <a-textarea v-model:value="changeForm.reason" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="changeDecideOpen"
      title="处理订单变更"
      width="560px"
      @ok="submitChangeDecision"
    >
      <a-form layout="vertical">
        <a-form-item label="审批结论">
          <a-radio-group
            v-model:value="changeDecideForm.decision"
            button-style="solid"
          >
            <a-radio-button value="APPROVED">通过</a-radio-button>
            <a-radio-button value="REJECTED">驳回</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="意见">
          <a-textarea v-model:value="changeDecideForm.comment" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="rejectOpen"
      title="退回发货信息"
      :confirm-loading="rejectSaving"
      @ok="submitReject"
    >
      <a-alert
        type="warning"
        show-icon
        message="退回后供应商可在门户看到原因，并修改或删除该发货记录。"
        style="margin-bottom: 12px"
      />
      <a-form layout="vertical">
        <a-form-item label="退回原因">
          <a-textarea v-model:value="rejectComment" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </BusinessDetailPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { message } from "ant-design-vue";
import UploadOutlined from "@ant-design/icons-vue/UploadOutlined";
import PlusOutlined from "@ant-design/icons-vue/PlusOutlined";
import BusinessDetailPage, {
  type DetailMetric,
} from "@/components/BusinessDetailPage.vue";
import { useAuthStore } from "@/stores/auth";
import {
  approvePurchaseOrder,
  confirmOrderShipment,
  createOrderChange,
  decideOrderChange,
  deleteOrderDocument,
  downloadOrderDocument,
  listOrderChanges,
  listGoodsReceipts,
  listOrderDocuments,
  listOrderShipments,
  listProcurementInquiries,
  listProcurementMatching,
  listProcurementPayables,
  listProcurementReturns,
  listPurchaseOrders,
  listPurchaseRequests,
  listSupplierInvoices,
  submitPurchaseOrder,
  uploadOrderDocument,
  type GoodsReceipt,
  type OrderDocument,
  type ProcurementInquiry,
  type ProcurementMatching,
  type ProcurementPayable,
  type ProcurementReturnOrder,
  type ProcurementShipment,
  type PurchaseOrder,
  type PurchaseOrderChange,
  type PurchaseRequest,
  type SupplierInvoice,
} from "@/api/procurement";
import {
  listPaymentApplications,
  type PaymentApplication,
} from "@/api/finance";
const route = useRoute(),
  router = useRouter(),
  auth = useAuthStore(),
  loading = ref(false),
  saving = ref(false),
  approvalOpen = ref(false),
  order = ref<PurchaseOrder | null>(null),
  request = ref<PurchaseRequest | null>(null),
  inquiry = ref<ProcurementInquiry | null>(null),
  receipts = ref<GoodsReceipt[]>([]),
  payables = ref<ProcurementPayable[]>([]),
  returns = ref<ProcurementReturnOrder[]>([]),
  invoices = ref<SupplierInvoice[]>([]),
  applications = ref<PaymentApplication[]>([]),
  matching = ref<ProcurementMatching | null>(null),
  orderDocuments = ref<OrderDocument[]>([]),
  shipments = ref<ProcurementShipment[]>([]),
  loadingShipments = ref(false),
  changes = ref<PurchaseOrderChange[]>([]),
  changeOpen = ref(false),
  changeSaving = ref(false),
  changeDecideOpen = ref(false),
  loadingDocuments = ref(false),
  uploadingDocument = ref(false),
  rejectOpen = ref(false),
  rejectSaving = ref(false),
  rejectComment = ref(""),
  rejectTarget = ref<ProcurementShipment | null>(null),
  documentType = ref("ORIGINAL"),
  activeTab = ref(
    typeof route.query.tab === "string" ? route.query.tab : "overview",
  );
const approvalForm = reactive<{
  decision: "APPROVED" | "REJECTED";
  comment: string;
  approverName: string;
}>({
  decision: "APPROVED",
  comment: "同意采购",
  approverName: auth.user?.displayName || "",
});
const changeForm = reactive<{
  quantityAfter?: number;
  unitPriceAfter?: number;
  expectedDateAfter?: string;
  reason: string;
}>({
  quantityAfter: undefined,
  unitPriceAfter: undefined,
  expectedDateAfter: undefined,
  reason: "",
});
const changeDecideForm = reactive<{
  id: string | null;
  decision: "APPROVED" | "REJECTED";
  comment: string;
}>({
  id: null,
  decision: "APPROVED",
  comment: "",
});
const paidAmount = computed(() =>
    payables.value.reduce((s, i) => s + Number(i.paidAmount || 0), 0),
  ),
  outstanding = computed(() =>
    payables.value.reduce((s, i) => s + Number(i.outstandingAmount || 0), 0),
  );
const canManageDocuments = computed(() =>
  auth.can("procurement:purchase:create"),
);
const canConfirmShipment = computed(() =>
  auth.can("procurement:purchase:create"),
);
const metrics = computed<DetailMetric[]>(() =>
  order.value
    ? [
        {
          label: "订单金额（含税，元）",
          value: money(order.value.orderAmount),
        },
        {
          label: "到货进度",
          value: `${order.value.receivedQty}/${order.value.orderedQty}`,
          hint: `${Math.round((order.value.receivedQty / order.value.orderedQty) * 100) || 0}%`,
        },
        { label: "已付金额（含税，元）", value: money(paidAmount.value) },
        {
          label: "待付金额（含税，元）",
          value: money(outstanding.value),
          warning: outstanding.value > 0,
        },
      ]
    : [],
);
const currentStep = computed(() =>
  outstanding.value === 0 && paidAmount.value > 0
    ? 5
    : invoices.value.length || payables.value.length
      ? 4
      : receipts.value.length
        ? 3
        : order.value?.approvalStatus === "APPROVED"
          ? 2
          : inquiry.value
            ? 1
            : 0,
);
const riskLabel = computed(() =>
  matching.value && matching.value.matchStatus !== "MATCHED"
    ? "三单匹配存在差异"
    : receipts.value.some((i) => Number(i.rejectedQty || 0) > 0)
      ? "到货存在不合格品"
      : order.value?.expectedDeliveryDate &&
          new Date(order.value.expectedDeliveryDate) < new Date() &&
          Number(order.value.receivedQty) < Number(order.value.orderedQty)
        ? "采购交付已逾期"
        : "",
);
const riskDescription = computed(
  () =>
    matching.value?.riskMessage ||
    "请处理退货、补货或供应商质量改进，确认后再进入发票和付款环节。",
);
const quotationColumns = [
  { title: "供应商", key: "supplier", width: 240 },
  { title: "单价（含税，元）", key: "price", width: 180 },
  { title: "税率", dataIndex: "taxRate", width: 90 },
  { title: "交付日期", dataIndex: "deliveryDate", width: 120 },
  { title: "付款条件", dataIndex: "paymentTerms" },
  { title: "备注", dataIndex: "remark" },
];
const receiptColumns = [
  { title: "收货单", dataIndex: "code", width: 180 },
  { title: "数量", dataIndex: "quantity", width: 90 },
  { title: "收货金额（含税，元）", key: "amount", width: 190 },
  { title: "日期", dataIndex: "receivedDate", width: 120 },
  { title: "送货单", dataIndex: "deliveryNo", width: 160 },
  { title: "承运方", dataIndex: "carrier", width: 120 },
  { title: "质检结果", key: "inspection", width: 180 },
  { title: "质检人", dataIndex: "inspectorName", width: 120 },
];
const returnColumns = [
  { title: "退货单", dataIndex: "code" },
  { title: "数量", dataIndex: "quantity", width: 90 },
  { title: "质检金额（含税，元）", key: "amount", width: 190 },
  { title: "原因", dataIndex: "reason" },
  { title: "日期", dataIndex: "returnDate", width: 120 },
  { title: "处理人", dataIndex: "handlerName", width: 120 },
];
const invoiceColumns = [
  { title: "发票号", dataIndex: "invoiceNo" },
  { title: "发票金额（含税，元）", key: "amount", width: 190 },
  { title: "税率", dataIndex: "taxRate", width: 90 },
  { title: "日期", dataIndex: "invoiceDate", width: 120 },
  { title: "差异", key: "difference", width: 140 },
  { title: "匹配", key: "match", width: 100 },
];
const payableColumns = [
  { title: "应付单", key: "code" },
  { title: "应付金额（含税，元）", key: "amount", width: 190 },
  { title: "已付金额（含税，元）", key: "paid", width: 190 },
  { title: "待付金额（含税，元）", key: "outstanding", width: 190 },
  { title: "到期日", dataIndex: "dueDate", width: 120 },
  { title: "状态", dataIndex: "status", width: 110 },
];
const applicationColumns = [
  { title: "申请单", key: "code" },
  { title: "付款金额（含税，元）", key: "amount", width: 190 },
  { title: "申请人", dataIndex: "applicantName", width: 120 },
  { title: "申请日期", dataIndex: "requestedDate", width: 120 },
  { title: "状态", dataIndex: "status", width: 120 },
];
const changeColumns = [
  { title: "变更单", key: "no", width: 180 },
  { title: "类型", key: "type", width: 90 },
  { title: "数量", key: "qty", width: 150 },
  { title: "单价（含税，元）", key: "price", width: 170 },
  { title: "期望交期", key: "date", width: 190 },
  { title: "变更原因", dataIndex: "reason" },
  { title: "状态", dataIndex: "status", width: 100 },
  { title: "申请人", dataIndex: "createdByName", width: 110 },
  { title: "操作", key: "actions", width: 150 },
];
const documentColumns = [
  { title: "文件", key: "file", width: 260 },
  { title: "类型", key: "docType", width: 100 },
  { title: "上传人", dataIndex: "uploadedBy", width: 140 },
  { title: "上传时间", key: "uploadedAt", width: 180 },
  { title: "操作", key: "actions", width: 100 },
];
const shipmentColumns = [
  { title: "送货单号", dataIndex: "deliveryNo", width: 180 },
  { title: "承运方", dataIndex: "carrier", width: 140 },
  { title: "预计到货", key: "arrival", width: 130 },
  { title: "状态", key: "status", width: 130 },
  { title: "备注", dataIndex: "remark" },
  { title: "回传时间", key: "date", width: 180 },
  { title: "操作", key: "actions", width: 140 },
];
onMounted(loadData);
async function loadData() {
  loading.value = true;
  try {
    const id = String(route.params.id);
    const [os, rs, ps, qs, rts, isx, ms, prs, apps] = await Promise.all([
      listPurchaseOrders({ page: 0, size: 999 }),
      listGoodsReceipts(),
      listProcurementPayables(),
      listProcurementInquiries(),
      listProcurementReturns(),
      listSupplierInvoices(),
      listProcurementMatching(),
      listPurchaseRequests({ page: 0, size: 999 }),
      listPaymentApplications(),
    ]);
    order.value = os.content.find((i) => i.id === id) || null;
    if (order.value) {
      request.value =
        prs.content.find((i) => i.id === order.value!.requestId) || null;
      inquiry.value =
        qs.find((i) => i.requestId === order.value!.requestId) || null;
      receipts.value = rs.filter((i) => i.orderId === id);
      payables.value = ps.filter((i) => i.orderId === id);
      returns.value = rts.filter((i) => i.orderId === id);
      invoices.value = isx.filter((i) => i.orderId === id);
      matching.value = ms.find((i) => i.orderId === id) || null;
      await loadDocuments(id);
      await loadShipments(id);
      await loadChanges(id);
      const payableIds = new Set(payables.value.map((i) => i.id));
      applications.value = apps.filter((i) => payableIds.has(i.payableId));
    }
  } catch (e) {
    message.error(e instanceof Error ? e.message : "采购订单详情加载失败");
  } finally {
    loading.value = false;
  }
}
async function loadDocuments(orderId: string) {
  loadingDocuments.value = true;
  try {
    orderDocuments.value = await listOrderDocuments(orderId);
  } catch (e) {
    message.error(e instanceof Error ? e.message : "合同附件加载失败");
  } finally {
    loadingDocuments.value = false;
  }
}
async function loadShipments(orderId: string) {
  loadingShipments.value = true;
  try {
    shipments.value = await listOrderShipments(orderId);
  } catch (e) {
    message.error(e instanceof Error ? e.message : "发货信息加载失败");
  } finally {
    loadingShipments.value = false;
  }
}
async function loadChanges(orderId: string) {
  try {
    changes.value = await listOrderChanges(orderId);
  } catch (e) {
    message.error(e instanceof Error ? e.message : "变更记录加载失败");
  }
}
function openChange() {
  if (!order.value) return;
  changeForm.quantityAfter = Number(order.value.orderedQty);
  changeForm.unitPriceAfter = Number(order.value.unitPrice);
  changeForm.expectedDateAfter = order.value.expectedDeliveryDate || undefined;
  changeForm.reason = "";
  changeOpen.value = true;
}
async function submitChange() {
  if (!order.value) return;
  if (!changeForm.reason.trim()) {
    message.warning("请填写变更原因");
    return;
  }
  changeSaving.value = true;
  try {
    await createOrderChange(order.value.id, {
      changeType: "MIXED",
      quantityAfter: changeForm.quantityAfter,
      unitPriceAfter: changeForm.unitPriceAfter,
      expectedDateAfter: changeForm.expectedDateAfter,
      reason: changeForm.reason.trim(),
    });
    changeOpen.value = false;
    message.success("变更单已提交，等待审批");
    await loadChanges(order.value.id);
  } catch (e) {
    message.error(e instanceof Error ? e.message : "变更单提交失败");
  } finally {
    changeSaving.value = false;
  }
}
function openChangeDecide(record: PurchaseOrderChange) {
  changeDecideForm.id = record.id;
  changeDecideForm.decision = "APPROVED";
  changeDecideForm.comment = "";
  changeDecideOpen.value = true;
}
async function submitChangeDecision() {
  if (!changeDecideForm.id) return;
  const id = changeDecideForm.id;
  try {
    const result = await decideOrderChange(id, {
      decision: changeDecideForm.decision,
      comment: changeDecideForm.comment,
    });
    changeDecideOpen.value = false;
    message.success(
      result.status === "APPROVED" ? "变更已通过并应用到订单" : "变更已驳回",
    );
    await loadData();
  } catch (e) {
    message.error(e instanceof Error ? e.message : "变更处理失败");
  }
}
function changeTypeLabel(type?: string) {
  return (
    (
      {
        QTY: "改数量",
        PRICE: "改价格",
        DATE: "改交期",
        MIXED: "综合变更",
      } as Record<string, string>
    )[type || ""] ||
    type ||
    "-"
  );
}
function changeStatusLabel(status?: string) {
  return (
    (
      {
        PENDING: "待审批",
        APPROVED: "已通过",
        REJECTED: "已驳回",
      } as Record<string, string>
    )[status || ""] ||
    status ||
    "-"
  );
}
async function handleUploadDocument(file: File) {
  if (!order.value) return false;
  uploadingDocument.value = true;
  try {
    await uploadOrderDocument(order.value.id, file, documentType.value);
    message.success("合同附件已上传，供应商门户同步可见");
    await loadDocuments(order.value.id);
  } catch (e) {
    message.error(e instanceof Error ? e.message : "合同附件上传失败");
  } finally {
    uploadingDocument.value = false;
  }
  return false;
}
function docTypeLabel(type?: string) {
  return (
    (
      {
        ORIGINAL: "原件",
        STAMPED: "盖章件",
        OTHER: "其他附件",
      } as Record<string, string>
    )[type || ""] || "其他附件"
  );
}
function docTypeColor(type?: string) {
  return (
    (
      {
        ORIGINAL: "blue",
        STAMPED: "green",
        OTHER: "default",
      } as Record<string, string>
    )[type || ""] || "default"
  );
}
const documentTypeLabel = computed(() => docTypeLabel(documentType.value));
async function handleDeleteDocument(record: OrderDocument) {
  if (!order.value) return;
  try {
    await deleteOrderDocument(order.value.id, record.id);
    message.success("合同附件已删除");
    orderDocuments.value = orderDocuments.value.filter(
      (item) => item.id !== record.id,
    );
  } catch (e) {
    message.error(e instanceof Error ? e.message : "删除失败");
  }
}
function formatBytes(bytes?: number) {
  const value = Number(bytes || 0);
  if (value >= 1048576) return `${(value / 1048576).toFixed(1)} MB`;
  if (value >= 1024) return `${Math.ceil(value / 1024)} KB`;
  return `${value} B`;
}
async function handleSubmit() {
  if (!order.value) return;
  saving.value = true;
  try {
    order.value = await submitPurchaseOrder(order.value.id);
    message.success("采购订单已提交审批");
  } catch (e) {
    message.error(e instanceof Error ? e.message : "提交失败");
  } finally {
    saving.value = false;
  }
}
async function handleApproval() {
  if (!order.value || !approvalForm.comment || !approvalForm.approverName) {
    message.warning("请填写审批意见和审批人");
    return;
  }
  saving.value = true;
  try {
    order.value = await approvePurchaseOrder(order.value.id, {
      ...approvalForm,
    });
    approvalOpen.value = false;
    message.success(
      approvalForm.decision === "APPROVED" ? "订单审批通过" : "订单已驳回",
    );
    await loadData();
  } catch (e) {
    message.error(e instanceof Error ? e.message : "审批失败");
  } finally {
    saving.value = false;
  }
}
function money(v?: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    maximumFractionDigits: 0,
  }).format(Number(v || 0));
}
function dateTime(v?: string) {
  return v ? v.slice(0, 16).replace("T", " ") : "-";
}
function statusLabel(v?: string) {
  return (
    (
      {
        DRAFT: "草稿",
        ORDERED: "已下单",
        PARTIAL_RECEIVED: "部分收货",
        RECEIVED: "已收货",
        CLOSED: "已关闭",
        CANCELLED: "已取消",
      } as Record<string, string>
    )[v || ""] || ""
  );
}
function approvalLabel(v?: string) {
  return (
    (
      { PENDING: "待审批", APPROVED: "已通过", REJECTED: "已驳回" } as Record<
        string,
        string
      >
    )[v || ""] || "-"
  );
}
function shipmentStatusText(v?: string) {
  return (
    (
      {
        PENDING: "待确认",
        CONFIRMED: "已确认到货",
        REJECTED: "已退回",
      } as Record<string, string>
    )[v || ""] || v || "-"
  );
}
function shipmentStatusColor(v?: string) {
  return (
    (
      {
        PENDING: "orange",
        CONFIRMED: "green",
        REJECTED: "red",
      } as Record<string, string>
    )[v || ""] || "default"
  );
}
async function handleConfirmShipment(shipment: ProcurementShipment) {
  if (!order.value) return;
  try {
    await confirmOrderShipment(order.value.id, shipment.id, {
      action: "CONFIRMED",
    });
    message.success("已确认到货，并通知供应商");
    await loadShipments(order.value.id);
  } catch (e) {
    message.error(e instanceof Error ? e.message : "确认失败");
  }
}
function openReject(shipment: ProcurementShipment) {
  rejectTarget.value = shipment;
  rejectComment.value = "";
  rejectOpen.value = true;
}
async function submitReject() {
  if (!rejectTarget.value || !order.value) return;
  rejectSaving.value = true;
  try {
    await confirmOrderShipment(order.value.id, rejectTarget.value.id, {
      action: "REJECTED",
      comment: rejectComment.value.trim() || undefined,
    });
    message.success("已退回发货信息，并通知供应商");
    rejectOpen.value = false;
    rejectTarget.value = null;
    await loadShipments(order.value.id);
  } catch (e) {
    message.error(e instanceof Error ? e.message : "退回失败");
  } finally {
    rejectSaving.value = false;
  }
}
function inspectionLabel(v?: string) {
  return (
    (
      {
        PENDING: "待质检",
        PASSED: "质检通过",
        PARTIAL: "部分合格",
        REJECTED: "不合格",
      } as Record<string, string>
    )[v || ""] || "待质检"
  );
}
</script>
<style scoped>
.section-gap {
  margin-top: 16px;
}
.sub {
  display: block;
  color: #8c96a5;
  font-size: 12px;
}
.shipment-review {
  margin-top: 4px;
  color: #cf1322;
  font-size: 12px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}
.danger {
  color: #cf1322;
  font-weight: 600;
}
.ant-timeline p {
  margin: 5px 0;
  color: #657186;
}
</style>
