<template>
  <div class="page-shell">
    <header class="page-heading">
      <div>
        <p class="eyebrow">采购协作</p>
        <h1>采购订单</h1>
        <p>确认中标项目、跟踪采购订单，并在发货后回传送货信息。</p>
      </div>
      <a-button :loading="loading" @click="load"
        ><ReloadOutlined /> 刷新</a-button
      >
    </header>

    <section class="metrics-grid" aria-label="订单概览">
      <article class="metric"
        ><span class="metric-icon green"><TrophyOutlined /></span
        ><div><small>中标项目</small><strong>{{ awardedCount }}</strong></div></article
      >
      <article class="metric"
        ><span class="metric-icon amber"><CheckCircleOutlined /></span
        ><div><small>待确认中标</small><strong>{{ pendingAcknowledgeCount }}</strong></div></article
      >
      <article class="metric"
        ><span class="metric-icon blue"><ProfileOutlined /></span
        ><div><small>采购订单</small><strong>{{ orderCount }}</strong></div></article
      >
      <article class="metric"
        ><span class="metric-icon gray"><CarOutlined /></span
        ><div><small>待交货</small><strong>{{ deliverableCount }}</strong></div></article
      >
    </section>

    <div class="filter-row">
      <a-segmented v-model:value="filter" :options="filterOptions" /><span
        >{{ filtered.length }} 条记录</span
      >
    </div>

    <a-skeleton v-if="loading" active />
    <a-empty v-else-if="filtered.length === 0" description="暂无相关记录" />
    <div v-else class="order-list">
      <article v-for="entry in filtered" :key="entryKey(entry)" class="order-card">
        <div class="order-head">
          <div class="order-title-row">
            <a-tag v-if="entry.order" color="blue">{{
              entry.order.code
            }}</a-tag>
            <a-tag v-else color="purple">中标项目</a-tag>
            <a-tag v-if="entry.inquiry" color="default">{{
              entry.inquiry.code
            }}</a-tag>
            <a-tag v-if="entry.order" :color="orderStatus(entry.order.status).color">{{
              orderStatus(entry.order.status).text
            }}</a-tag>
            <a-tag
              v-if="entry.contract && !entry.contract.acknowledged"
              color="orange"
              >待确认中标</a-tag
            >
            <a-tag
              v-else-if="entry.contract?.acknowledged"
              color="green"
              >已确认中标</a-tag
            >
          </div>
          <div class="order-amount">
            <strong>{{
              entry.order
                ? money(entry.order.orderAmount, entry.order.currency) + " " + (entry.order.currency || "")
                : entry.contract
                  ? money(entry.contract.amount, entry.contract.currency) + " " + (entry.contract.currency || "")
                  : ""
            }}</strong>
            <small v-if="entry.order">含税总额</small>
            <small v-else-if="entry.contract">合同金额</small>
          </div>
        </div>

        <div class="order-main">
          <div class="order-facts">
            <h2>{{ entry.inquiry?.title || entry.order?.partName || "中标项目" }}</h2>
            <p v-if="entry.order">
              {{ entry.order.partName }} × {{ entry.order.orderedQty }} · 单价
              {{ money(entry.order.unitPrice, entry.order.currency) }} · 税率
              {{ entry.order.taxRate }}%
            </p>
            <p v-if="entry.order" class="order-meta">
              已收货 {{ entry.order.receivedQty }} · 预计交货
              {{ entry.order.expectedDeliveryDate || "未设置" }}
              <template v-if="entry.order.costTargetName">
                · {{ entry.order.costTargetName }}
              </template>
            </p>
            <p class="order-meta">
              <template v-if="entry.contract"
                >合同 {{ entry.contract.contractNo }} ·
                {{ contractStatusText(entry.contract.status) }} ·
                {{ entry.contract.approvalStatus === "APPROVED" ? "审批通过" : "审批中"
                }}<template v-if="entry.contract.acknowledged"
                  > · {{ formatDate(entry.contract.acknowledgedAt) }} 已确认</template
                ></template
              >
              <template v-if="!entry.order"> · 采购方下单后生成采购订单</template>
            </p>
            <p class="order-meta">
              中标日期 {{ formatDate(entry.inquiry?.awardedAt) }} · 创建于
              {{ formatDate(entry.order?.createdAt || entry.contract?.acknowledgedAt) }}
            </p>
          </div>
          <div class="order-actions">
            <a-button
              v-if="entry.contract && !entry.contract.acknowledged"
              type="primary"
              :loading="acknowledgingId === entry.contract.id"
              @click="acknowledge(entry)"
              ><CheckOutlined /> 确认中标</a-button
            >
            <a-button
              v-if="entry.order && canDeliver(entry.order)"
              type="primary"
              ghost
              @click="openDelivery(entry)"
              ><CarOutlined /> 交货提交</a-button
            >
            <a-button
              v-if="entry.order || entry.inquiry"
              @click="openDetail(entry)"
              >查看详情 <RightOutlined /></a-button
            >
          </div>
        </div>
      </article>
    </div>

    <a-drawer
      v-model:open="detailOpen"
      :title="detailEntry?.order ? '采购订单详情' : '中标项目详情'"
      width="min(820px, 100vw)"
    >
      <template v-if="detailEntry">
        <template v-if="detailEntry.order">
          <div class="detail-head">
            <div>
              <h2>{{ detailEntry.order.partName }}</h2>
              <p>
                {{ detailEntry.order.code }} · 下单于
                {{ formatDate(detailEntry.order.submittedAt || detailEntry.order.createdAt) }}
              </p>
            </div>
            <div class="detail-amount">
              <strong>{{ money(detailEntry.order.orderAmount, detailEntry.order.currency) }}</strong>
              <small>{{ detailEntry.order.currency || "CNY" }} 含税总额</small>
            </div>
            <a-button
              type="primary"
              ghost
              size="small"
              @click="exportOrderPdf"
              ><DownloadOutlined /> 导出订单 PDF</a-button
            >
            <a-button
              type="primary"
              ghost
              size="small"
              @click="exportOrderExcel"
              ><FileExcelOutlined /> 导出订单 Excel</a-button
            >
          </div>

          <section class="detail-block">
            <div class="section-title">
              <div><h3>订单信息</h3><p>采购方下达的采购订单基本信息。</p></div>
            </div>
            <a-descriptions bordered size="small" :column="2">
              <a-descriptions-item label="订单编号">{{
                detailEntry.order.code
              }}</a-descriptions-item>
              <a-descriptions-item label="订单状态">{{
                orderStatus(detailEntry.order.status).text
              }}</a-descriptions-item>
              <a-descriptions-item label="审批状态">{{
                approvalStatusText(detailEntry.order.approvalStatus)
              }}</a-descriptions-item>
              <a-descriptions-item label="审批人">{{
                detailEntry.order.approverName || "—"
              }}</a-descriptions-item>
              <a-descriptions-item label="订购数量">{{
                detailEntry.order.orderedQty
              }}</a-descriptions-item>
              <a-descriptions-item label="已收 / 待收">{{
                `${detailEntry.order.receivedQty} / ${detailEntry.order.remainingQty}`
              }}</a-descriptions-item>
              <a-descriptions-item label="单价">{{
                money(detailEntry.order.unitPrice, detailEntry.order.currency)
              }}</a-descriptions-item>
              <a-descriptions-item label="税率">{{
                detailEntry.order.taxRate + "%"
              }}</a-descriptions-item>
              <a-descriptions-item label="预计交货">{{
                detailEntry.order.expectedDeliveryDate || "未设置"
              }}</a-descriptions-item>
              <a-descriptions-item label="成本归属">{{
                detailEntry.order.costTargetName || "—"
              }}</a-descriptions-item>
              <a-descriptions-item label="采购负责人">{{
                detailEntry.order.responsibleName || "—"
              }}</a-descriptions-item>
              <a-descriptions-item label="采购说明">{{
                detailEntry.order.sourceReason || "—"
              }}</a-descriptions-item>
            </a-descriptions>
          </section>

          <section class="detail-block">
            <div class="section-title">
              <div><h3>金额构成</h3><p>按含税口径计算。</p></div>
            </div>
            <a-descriptions bordered size="small" :column="3">
              <a-descriptions-item label="物料金额">{{
                money(detailEntry.order.materialAmount, detailEntry.order.currency)
              }}</a-descriptions-item>
              <a-descriptions-item label="运费">{{
                money(detailEntry.order.freightAmount, detailEntry.order.currency)
              }}</a-descriptions-item>
              <a-descriptions-item label="订单总额">{{
                money(detailEntry.order.orderAmount, detailEntry.order.currency)
              }}</a-descriptions-item>
            </a-descriptions>
          </section>

          <section class="detail-block">
            <div class="section-title">
              <div><h3>交货与收货</h3><p>发货记录与采购方收货、质检进度。</p></div>
            </div>
            <a-timeline v-if="deliveryTimeline.length > 0" style="margin-bottom: 18px">
              <a-timeline-item
                v-for="event in deliveryTimeline"
                :key="event.time + event.title"
                :color="event.color"
              >
                <div class="timeline-title">{{ event.title }}</div>
                <div class="timeline-desc">{{ event.description }}</div>
                <small class="timeline-time">{{ formatDateTime(event.time) }}</small>
              </a-timeline-item>
            </a-timeline>
            <div class="receipt-progress">
              <a-progress
                :percent="receiptPercent"
                :status="receiptPercent >= 100 ? 'success' : 'active'"
                size="small"
              />
              <span>已收货 {{ detailEntry.order.receivedQty }} / {{ detailEntry.order.orderedQty }}</span>
            </div>
            <a-table
              v-if="detailEntry.receipts.length > 0"
              size="small"
              row-key="id"
              :data-source="detailEntry.receipts"
              :columns="receiptColumns"
              :pagination="false"
              style="margin-top: 12px"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'action'">
                  <a-button
                    v-if="canAppeal(record)"
                    type="link"
                    size="small"
                    @click="openAppeal(record)"
                    >申诉</a-button
                  >
                  <span v-else-if="record.appealStatus === 'PENDING'">
                    <a-tag color="orange">申诉处理中</a-tag>
                  </span>
                  <span v-else-if="record.appealStatus === 'REOPENED'">
                    <a-tag color="blue">申诉已受理 · 重新质检</a-tag>
                    <span class="table-subtitle">{{
                      record.appealReviewComment || "采购方已受理申诉，等待重新质检"
                    }}</span>
                  </span>
                  <span v-else-if="record.appealStatus === 'DISMISSED'">
                    <a-tag color="red">申诉未成立</a-tag>
                    <span class="table-subtitle">{{
                      record.appealReviewComment || "维持原质检结果"
                    }}</span>
                  </span>
                  <span v-else>—</span>
                </template>
              </template>
            </a-table>
            <a-empty
              v-else
              :image="Empty.PRESENTED_IMAGE_SIMPLE"
              description="暂无收货记录"
              style="margin-top: 12px"
            />
            <h4 style="margin: 16px 0 8px">发货记录</h4>
            <a-table
              v-if="detailEntry.shipments.length > 0"
              size="small"
              row-key="id"
              :data-source="detailEntry.shipments"
              :columns="shipmentColumns"
              :pagination="false"
            />
            <a-empty
              v-else
              :image="Empty.PRESENTED_IMAGE_SIMPLE"
              description="暂无发货记录"
            />
          </section>

          <section v-if="detailEntry.changes.length > 0" class="detail-block">
            <div class="section-title">
              <div><h3>订单变更记录</h3><p>采购方对数量、价格或交期的变更审批记录。</p></div>
            </div>
            <a-table
              size="small"
              row-key="id"
              :data-source="detailEntry.changes"
              :columns="changeColumns"
              :pagination="false"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'action'">
                  <a-button
                    v-if="canRespondChange(record)"
                    type="link"
                    size="small"
                    @click="openChangeRespond(record, 'AGREE')"
                    >同意</a-button
                  >
                  <a-button
                    v-if="canRespondChange(record)"
                    type="link"
                    size="small"
                    danger
                    @click="openChangeRespond(record, 'OBJECT')"
                    >异议</a-button
                  >
                  <span v-else-if="record.supplierResponse">{{
                    supplierResponseText(record)
                  }}</span>
                  <span v-else>—</span>
                </template>
              </template>
            </a-table>
          </section>
        </template>

        <template v-else-if="detailEntry.inquiry">
          <div class="detail-head">
            <div>
              <h2>{{ detailEntry.inquiry.title }}</h2>
              <p>{{ detailEntry.inquiry.code }} · 中标项目，采购方下单后生成采购订单</p>
            </div>
            <div class="detail-amount">
              <strong>{{ money(detailEntry.contract?.amount, detailEntry.contract?.currency) }}</strong>
              <small>合同金额</small>
            </div>
          </div>
        </template>

        <section v-if="detailEntry.contract" class="detail-block">
          <div class="section-title">
            <div><h3>合同信息</h3><p>中标后随订单生成的采购合同。</p></div>
            <a-tag
              v-if="detailEntry.contract.acknowledged"
              color="green"
              >已确认{{
                detailEntry.contract.acknowledgedByName
                  ? " · " + detailEntry.contract.acknowledgedByName
                  : ""
              }}</a-tag
            >
            <a-tag v-else color="orange">待确认中标</a-tag>
          </div>
          <a-descriptions bordered size="small" :column="2">
            <a-descriptions-item label="合同编号">{{
              detailEntry.contract.contractNo
            }}</a-descriptions-item>
            <a-descriptions-item label="合同名称">{{
              detailEntry.contract.name
            }}</a-descriptions-item>
            <a-descriptions-item label="合同状态">{{
              contractStatusText(detailEntry.contract.status)
            }}</a-descriptions-item>
            <a-descriptions-item label="审批状态">{{
              detailEntry.contract.approvalStatus === "APPROVED" ? "审批通过" : "审批中"
            }}</a-descriptions-item>
            <a-descriptions-item label="合同金额">{{
              money(detailEntry.contract.amount, detailEntry.contract.currency) + " " + (detailEntry.contract.currency || "")
            }}</a-descriptions-item>
            <a-descriptions-item label="有效期">{{
              `${formatDate(detailEntry.contract.startDate)} 至 ${formatDate(detailEntry.contract.endDate)}`
            }}</a-descriptions-item>
            <a-descriptions-item label="付款条款">{{
              detailEntry.contract.paymentTerms || "—"
            }}</a-descriptions-item>
            <a-descriptions-item label="确认时间">{{
              detailEntry.contract.acknowledgedAt
                ? formatDate(detailEntry.contract.acknowledgedAt)
                : "尚未确认"
            }}</a-descriptions-item>
          </a-descriptions>
        </section>

        <section v-if="detailEntry.inquiry" class="detail-block">
          <div class="section-title">
            <div><h3>中标信息</h3><p>与本次订单相关的询价及定标信息。</p></div>
          </div>
          <a-descriptions bordered size="small" :column="2">
            <a-descriptions-item label="询价单">{{
              `${detailEntry.inquiry.code} ${detailEntry.inquiry.title}`
            }}</a-descriptions-item>
            <a-descriptions-item label="中标日期">{{
              formatDate(detailEntry.inquiry.awardedAt)
            }}</a-descriptions-item>
            <a-descriptions-item label="定标原因">{{
              detailEntry.inquiry.selectionReason || "—"
            }}</a-descriptions-item>
            <a-descriptions-item label="定标人">{{
              detailEntry.inquiry.selectedByName || "—"
            }}</a-descriptions-item>
            <a-descriptions-item label="报价承诺交期">{{
              quoteDeliveryDate(detailEntry.quote) || "—"
            }}</a-descriptions-item>
            <a-descriptions-item label="报价有效期">{{
              formatDate(detailEntry.quote?.validUntil) || "—"
            }}</a-descriptions-item>
            <a-descriptions-item label="付款条款">{{
              detailEntry.quote?.paymentTerms || "—"
            }}</a-descriptions-item>
            <a-descriptions-item label="报价总额">{{
              detailEntry.quote ? money(detailEntry.quote.totalAmount, detailEntry.quote.currency) : "—"
            }}</a-descriptions-item>
          </a-descriptions>
        </section>

        <section v-if="allDocuments.length > 0" class="detail-block">
          <div class="section-title">
            <div><h3>附件</h3><p>询价阶段的技术图纸及合同随附文件，可下载留存。</p></div>
          </div>
          <a-list size="small" :data-source="allDocuments">
            <template #renderItem="{ item: doc }">
              <a-list-item>
                <span>
                  <a-tag v-if="doc.kind === 'quote'" color="blue">询价附件</a-tag>
                  <a-tag v-else color="green">合同/订单附件</a-tag>
                  {{ doc.fileName }} · {{ fileSize(doc.sizeBytes) }}
                </span>
                <a-button type="link" size="small" @click="downloadContractDocument(doc)"
                  ><DownloadOutlined /> 下载</a-button
                >
              </a-list-item>
            </template>
          </a-list>
        </section>
      </template>
    </a-drawer>

    <a-modal
      v-model:open="changeRespondOpen"
      :title="changeRespondTarget?.response === 'OBJECT' ? '对订单变更提出异议' : '确认订单变更'"
      :ok-text="changeRespondTarget?.response === 'OBJECT' ? '提交异议' : '同意变更'"
      :confirm-loading="changeRespondSaving"
      @ok="submitChangeRespond"
    >
      <a-alert
        v-if="changeRespondTarget?.change"
        type="info"
        show-icon
        :message="`${changeRespondTarget.change.changeNo} · ${changeTypeText(changeRespondTarget.change.changeType)}`"
        :description="changeRespondTarget.change.reason"
        style="margin-bottom: 14px"
      />
      <a-form-item
        v-if="changeRespondTarget?.response === 'OBJECT'"
        label="异议说明"
        required
      >
        <a-textarea
          v-model:value="changeRespondComment"
          :rows="3"
          placeholder="请说明不同意该变更的原因，供采购方复核。"
        />
      </a-form-item>
    </a-modal>

    <a-modal
      v-model:open="appealOpen"
      title="质检结果申诉"
      ok-text="提交申诉"
      :confirm-loading="appealSaving"
      @ok="submitAppeal"
    >
      <a-alert
        v-if="appealTarget"
        type="error"
        show-icon
        :message="`${appealTarget.code} · ${inspectionStatusText(appealTarget.inspectionStatus)}`"
        :description="appealTarget.inspectionComment || '采购方判定存在不合格数量'"
        style="margin-bottom: 14px"
      />
      <a-form-item label="申诉理由" required>
        <a-textarea
          v-model:value="appealReason"
          :rows="3"
          placeholder="请说明异议依据，例如返工记录、检测报告等。"
        />
      </a-form-item>
    </a-modal>

    <a-drawer
      v-model:open="deliveryOpen"
      :title="shipmentEditTarget ? '修改发货信息' : '交货提交'"
      width="min(640px, 100vw)"
    >
      <template v-if="deliveryEntry">
        <a-alert
          type="info"
          show-icon
          :message="`订单 ${deliveryEntry.order?.code || ''}`"
          :description="`发货后回传送货单号与预计到货，采购方可在订单中查看。`"
          style="margin-bottom: 18px"
        />
        <a-form layout="vertical">
          <a-form-item label="送货单号" required>
            <a-input
              v-model:value="shipmentForm.deliveryNo"
              placeholder="例如：SF1234567890"
            />
          </a-form-item>
          <a-form-item label="承运方">
            <a-input v-model:value="shipmentForm.carrier" placeholder="例如：顺丰" />
          </a-form-item>
          <a-form-item label="预计到货">
            <a-input v-model:value="shipmentForm.expectedArrival" type="date" />
          </a-form-item>
          <a-form-item label="备注">
            <a-textarea v-model:value="shipmentForm.remark" :rows="3" placeholder="可选" />
          </a-form-item>
          <a-button
            type="primary"
            block
            :loading="shipmentSaving"
            @click="submitShipment"
            ><SendOutlined />
            {{ shipmentEditTarget ? "保存修改" : "提交交货信息" }}</a-button
          >
          <a-button
            v-if="shipmentEditTarget"
            block
            style="margin-top: 8px"
            @click="cancelEditShipment"
            >取消修改</a-button
          >
        </a-form>
        <a-alert
          v-if="shipmentSent"
          type="success"
          show-icon
          message="发货信息已提交"
          description="采购方可在订单中查看，感谢配合。"
          style="margin: 16px 0"
        />
        <a-divider style="margin: 18px 0" />
        <h3 style="margin-bottom: 10px">历史发货记录</h3>
        <a-table
          v-if="deliveryEntry.shipments.length > 0"
          size="small"
          row-key="id"
          :data-source="deliveryEntry.shipments"
          :columns="shipmentColumns"
          :pagination="false"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
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
            <template v-else-if="column.key === 'action'">
              <a-space :size="0" wrap>
                <a-button
                  type="link"
                  size="small"
                  @click="openShipmentAttachments(record)"
                  ><PaperClipOutlined /> 附件</a-button
                >
                <template v-if="record.status === 'PENDING'">
                  <a-button
                    type="link"
                    size="small"
                    @click="startEditShipment(record)"
                    ><EditOutlined /> 编辑</a-button
                  >
                  <a-popconfirm
                    title="确认删除该发货记录？"
                    @confirm="removeShipment(record)"
                  >
                    <a-button type="link" size="small" danger
                      ><DeleteOutlined /> 删除</a-button
                    >
                  </a-popconfirm>
                </template>
              </a-space>
            </template>
          </template>
        </a-table>
        <a-empty
          v-else
          :image="Empty.PRESENTED_IMAGE_SIMPLE"
          description="该订单暂无发货记录"
        />
        <template v-if="attachmentShipment">
          <a-divider style="margin: 18px 0" />
          <h3 style="margin-bottom: 10px">
            发货附件
            <span class="attachment-shipment-label"
              >{{ attachmentShipment.deliveryNo || "本次发货" }}</span
            >
          </h3>
          <a-upload
            :show-upload-list="false"
            :before-upload="uploadShipmentAttachment"
            accept=".jpg,.jpeg,.png,.webp,.pdf,.doc,.docx,.xls,.xlsx"
          >
            <a-button type="dashed" block :loading="attachmentUploading">
              <UploadOutlined /> 上传照片 / 送货单 / 凭证
            </a-button>
          </a-upload>
          <a-list
            size="small"
            :loading="attachmentLoading"
            :data-source="shipmentAttachments"
            style="margin-top: 12px"
          >
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta>
                  <template #title>
                    <span class="attachment-name">
                      <FileImageOutlined
                        v-if="isImage(item.contentType)"
                        class="attachment-icon image"
                      />
                      <FileOutlined v-else class="attachment-icon" />
                      {{ item.fileName }}
                      <a-tag>{{ fileSize(item.sizeBytes) }}</a-tag>
                    </span>
                  </template>
                  <template #description>
                    {{ formatDate(item.createdAt) }} {{ formatTime(item.createdAt) }}
                  </template>
                </a-list-item-meta>
                <template #actions>
                  <a @click="downloadShipmentAttachment(item)"
                    ><DownloadOutlined /> 下载</a
                  >
                  <a-popconfirm
                    title="确认删除该附件？"
                    @confirm="deleteShipmentAttachment(item)"
                  >
                    <a class="attachment-delete"><DeleteOutlined /> 删除</a>
                  </a-popconfirm>
                </template>
              </a-list-item>
            </template>
            <template #empty>
              <a-empty
                :image="Empty.PRESENTED_IMAGE_SIMPLE"
                description="暂无附件，可上传照片或送货凭证"
              />
            </template>
          </a-list>
        </template>
      </template>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { message } from "ant-design-vue";
import { Empty } from "ant-design-vue";
import {
  CarOutlined,
  CheckCircleOutlined,
  CheckOutlined,
  DeleteOutlined,
  DownloadOutlined,
  EditOutlined,
  FileExcelOutlined,
  FileImageOutlined,
  FileOutlined,
  PaperClipOutlined,
  ProfileOutlined,
  ReloadOutlined,
  RightOutlined,
  SendOutlined,
  TrophyOutlined,
  UploadOutlined,
} from "@ant-design/icons-vue";
import * as api from "../api";
import {
  contractStatusText,
  fileSize,
  formatDate,
  formatTime,
  money,
  shipmentStatusColor,
  shipmentStatusText,
  validateUploadFile,
} from "../utils/quote";

const loading = ref(false);
const entries = ref<api.PortalOrderEntry[]>([]);
const filter = ref("ALL");
const acknowledgingId = ref<string>();
const detailOpen = ref(false);
const deliveryOpen = ref(false);
const detailEntry = ref<api.PortalOrderEntry>();
const deliveryEntry = ref<api.PortalOrderEntry>();
const changeRespondOpen = ref(false);
const changeRespondSaving = ref(false);
const changeRespondComment = ref("");
const changeRespondTarget = ref<{
  orderId: string;
  change: api.PortalOrderChange;
  response: "AGREE" | "OBJECT";
}>();
const appealOpen = ref(false);
const appealSaving = ref(false);
const appealReason = ref("");
const appealTarget = ref<api.PortalReceipt>();
const route = useRoute();
const router = useRouter();
const shipmentSaving = ref(false);
const shipmentSent = ref(false);
const shipmentEditTarget = ref<api.ProcurementShipment>();
const shipmentForm = reactive({
  deliveryNo: "",
  carrier: "",
  expectedArrival: "",
  remark: "",
});
const attachmentShipment = ref<api.ProcurementShipment>();
const shipmentAttachments = ref<api.ShipmentAttachment[]>([]);
const attachmentLoading = ref(false);
const attachmentUploading = ref(false);

const filterOptions = [
  { label: "全部", value: "ALL" },
  { label: "待确认中标", value: "CONFIRM" },
  { label: "采购订单", value: "ORDERED" },
  { label: "待交货", value: "DELIVERY" },
];

const shipmentColumns = [
  { title: "送货单号", dataIndex: "deliveryNo" },
  { title: "承运方", dataIndex: "carrier" },
  { title: "预计到货", dataIndex: "expectedArrival" },
  {
    title: "状态",
    key: "status",
    dataIndex: "status",
    customRender: ({ text }: { text: string }) => shipmentStatusText(text),
  },
  {
    title: "提交时间",
    dataIndex: "createdAt",
    customRender: ({ text }: { text: string }) => formatTime(text),
  },
  { title: "操作", key: "action", width: 190 },
];

const receiptColumns = [
  { title: "到货单号", dataIndex: "code" },
  { title: "到货日期", dataIndex: "receivedDate" },
  { title: "数量", dataIndex: "quantity" },
  { title: "金额", dataIndex: "amount" },
  { title: "送货单号", dataIndex: "deliveryNo" },
  { title: "收货人", dataIndex: "receiverName" },
  {
    title: "质检状态",
    dataIndex: "inspectionStatus",
    customRender: ({ text }: { text: string }) => inspectionStatusText(text),
  },
  { title: "操作", key: "action", width: 110 },
];

const changeColumns = [
  { title: "变更单号", dataIndex: "changeNo" },
  {
    title: "类型",
    dataIndex: "changeType",
    customRender: ({ text }: { text: string }) => changeTypeText(text),
  },
  {
    title: "数量",
    customRender: ({ record }: { record: api.PortalOrderChange }) =>
      record.quantityBefore == null
        ? "—"
        : `${record.quantityBefore} → ${record.quantityAfter}`,
  },
  {
    title: "单价",
    customRender: ({ record }: { record: api.PortalOrderChange }) =>
      record.unitPriceBefore == null
        ? "—"
        : `${money(record.unitPriceBefore, detailEntry.value?.order?.currency)} → ${money(record.unitPriceAfter, detailEntry.value?.order?.currency)}`,
  },
  {
    title: "预计交期",
    customRender: ({ record }: { record: api.PortalOrderChange }) =>
      record.expectedDateBefore
        ? `${record.expectedDateBefore} → ${record.expectedDateAfter || "—"}`
        : "—",
  },
  {
    title: "状态",
    dataIndex: "status",
    customRender: ({ text }: { text: string }) => changeStatusText(text),
  },
  { title: "原因", dataIndex: "reason" },
  { title: "提交人", dataIndex: "createdByName" },
  { title: "操作", key: "action", width: 110 },
];

const awardedCount = computed(
  () => entries.value.filter((item) => item.contract).length,
);
const pendingAcknowledgeCount = computed(
  () => entries.value.filter((item) => item.contract && !item.contract.acknowledged).length,
);
const orderCount = computed(
  () => entries.value.filter((item) => item.order).length,
);
const deliverableCount = computed(
  () => entries.value.filter((item) => item.order && canDeliver(item.order)).length,
);
const filtered = computed(() =>
  entries.value.filter((entry) => {
    if (filter.value === "ALL") return true;
    if (filter.value === "CONFIRM")
      return Boolean(entry.contract && !entry.contract.acknowledged);
    if (filter.value === "ORDERED") return Boolean(entry.order);
    if (filter.value === "DELIVERY")
      return Boolean(entry.order && canDeliver(entry.order));
    return true;
  }),
);

const receiptPercent = computed(() => {
  const order = detailEntry.value?.order;
  if (!order || !order.orderedQty) return 0;
  return Math.min(
    100,
    Math.max(0, Math.round((order.receivedQty / order.orderedQty) * 100)),
  );
});

type TimelineEvent = {
  time: string;
  title: string;
  description: string;
  color?: string;
};

const deliveryTimeline = computed<TimelineEvent[]>(() => {
  const entry = detailEntry.value;
  if (!entry?.order) return [];
  const events: TimelineEvent[] = [];
  const order = entry.order;
  if (order.submittedAt || order.createdAt) {
    events.push({
      time: order.submittedAt || order.createdAt || "",
      title: "采购方下达订单",
      description: `${order.code} 已下单，请按约交付。`,
    });
  }
  for (const shipment of entry.shipments) {
    const parts = [
      shipment.deliveryNo ? `送货单号 ${shipment.deliveryNo}` : "",
      shipment.carrier || "",
      shipment.expectedArrival ? `预计到货 ${shipment.expectedArrival}` : "",
    ].filter(Boolean);
    events.push({
      time: shipment.createdAt,
      title: "供应商发货",
      description: parts.join(" · ") || "已回传发货信息",
      color: "blue",
    });
  }
  for (const receipt of entry.receipts) {
    if (receipt.receivedDate) {
      events.push({
        time: receipt.receivedDate,
        title: "采购方收货",
        description: `${receipt.code} · ${receipt.quantity} 件${receipt.receiverName ? " · 收货人 " + receipt.receiverName : ""}`,
        color: "green",
      });
    }
    if (receipt.inspectedAt) {
      const rejected = Number(receipt.rejectedQty || 0);
      events.push({
        time: receipt.inspectedAt,
        title: `质检${inspectionStatusText(receipt.inspectionStatus)}`,
        description: `${Number(receipt.qualifiedQty || 0)} 件合格 / ${rejected} 件不合格${receipt.inspectionComment ? " · " + receipt.inspectionComment : ""}`,
        color: receipt.inspectionStatus === "REJECTED" ? "red" : "green",
      });
    }
  }
  return events.sort(
    (a, b) => new Date(a.time).getTime() - new Date(b.time).getTime(),
  );
});

function formatDateTime(value: string) {
  if (!value) return "";
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleString("zh-CN", { hour12: false });
}

type AttachmentItem = {
  id: string;
  fileName: string;
  sizeBytes: number;
  kind: "quote" | "contract";
  downloadUrl: string;
};

const allDocuments = computed<AttachmentItem[]>(() => {
  const seen = new Set<string>();
  const items: AttachmentItem[] = [];
  const push = (
    doc: { id: string; fileName: string; sizeBytes: number },
    kind: AttachmentItem["kind"],
    url: string,
  ) => {
    if (!doc || seen.has(doc.id)) return;
    seen.add(doc.id);
    items.push({
      id: doc.id,
      fileName: doc.fileName,
      sizeBytes: doc.sizeBytes,
      kind,
      downloadUrl: url,
    });
  };
  for (const doc of detailEntry.value?.contract?.documents || []) {
    push(doc, "contract", api.contractDocumentDownloadUrl(doc.id));
  }
  for (const doc of detailEntry.value?.documents || []) {
    push(doc, "contract", api.contractDocumentDownloadUrl(doc.id));
  }
  const inquiryId = detailEntry.value?.inquiry?.id;
  for (const doc of detailEntry.value?.quoteAttachments || []) {
    if (!inquiryId) continue;
    push(doc, "quote", api.quoteAttachmentDownloadUrl(inquiryId, doc.id));
  }
  return items;
});

function approvalStatusText(value?: string) {
  return (
    { PENDING: "待审批", APPROVED: "审批通过", REJECTED: "已驳回" } as Record<
      string,
      string
    >
  )[value || ""] || value || "—";
}

function inspectionStatusText(value: string) {
  return (
    {
      PENDING: "待质检",
      PASSED: "合格",
      REJECTED: "不合格",
      PARTIAL: "部分合格",
    } as Record<string, string>
  )[value] || value || "—";
}

function changeTypeText(value: string) {
  return (
    {
      QTY: "数量变更",
      PRICE: "价格变更",
      DATE: "交期变更",
      MIXED: "多项变更",
    } as Record<string, string>
  )[value] || value || "—";
}

function changeStatusText(value: string) {
  return (
    { PENDING: "待审批", APPROVED: "已通过", REJECTED: "已驳回" } as Record<
      string,
      string
    >
  )[value] || value || "—";
}

function quoteDeliveryDate(quote?: api.PortalQuote | null) {
  return (
    quote?.lines.find((line) => line.deliveryDate)?.deliveryDate || ""
  );
}

function entryKey(entry: api.PortalOrderEntry) {
  return entry.order?.id || entry.contract?.id || entry.inquiry?.id || "";
}

function orderStatus(status?: string) {
  return (
    {
      DRAFT: { color: "default", text: "待提交" },
      ORDERED: { color: "blue", text: "已下单" },
      PARTIAL_RECEIVED: { color: "orange", text: "部分收货" },
      RECEIVED: { color: "green", text: "已收货" },
      CLOSED: { color: "default", text: "已关闭" },
      CANCELLED: { color: "red", text: "已取消" },
    } as Record<string, { color: string; text: string }>
  )[status || ""] || { color: "default", text: status || "未知" };
}

function canDeliver(order: api.PortalOrder) {
  return order.status === "ORDERED" || order.status === "PARTIAL_RECEIVED";
}

function openDetail(entry: api.PortalOrderEntry) {
  detailEntry.value = entry;
  detailOpen.value = true;
}

function openDelivery(entry: api.PortalOrderEntry) {
  deliveryEntry.value = entry;
  attachmentShipment.value = undefined;
  shipmentAttachments.value = [];
  shipmentSent.value = false;
  shipmentEditTarget.value = undefined;
  resetShipmentForm();
  deliveryOpen.value = true;
}

function resetShipmentForm() {
  shipmentForm.deliveryNo = "";
  shipmentForm.carrier = "";
  shipmentForm.expectedArrival = "";
  shipmentForm.remark = "";
}

function startEditShipment(shipment: api.ProcurementShipment) {
  shipmentEditTarget.value = shipment;
  shipmentSent.value = false;
  shipmentForm.deliveryNo = shipment.deliveryNo || "";
  shipmentForm.carrier = shipment.carrier || "";
  shipmentForm.expectedArrival = shipment.expectedArrival || "";
  shipmentForm.remark = shipment.remark || "";
  attachmentShipment.value = undefined;
}

function cancelEditShipment() {
  shipmentEditTarget.value = undefined;
  resetShipmentForm();
}

function downloadContractDocument(doc: AttachmentItem) {
  window.location.href = doc.downloadUrl;
}

function exportOrderPdf() {
  const order = detailEntry.value?.order;
  if (!order) return;
  window.location.href = api.orderPdfUrl(order.id);
}

function exportOrderExcel() {
  const order = detailEntry.value?.order;
  if (!order) return;
  window.location.href = api.orderExcelUrl(order.id);
}

function canRespondChange(change: api.PortalOrderChange) {
  return (
    !change.supplierResponse &&
    (change.status === "PENDING" || change.status === "APPROVED")
  );
}

function supplierResponseText(change: api.PortalOrderChange) {
  const base = change.supplierResponse === "AGREE" ? "已同意" : "已异议";
  return change.supplierComment ? `${base} · ${change.supplierComment}` : base;
}

function openChangeRespond(change: api.PortalOrderChange, response: "AGREE" | "OBJECT") {
  if (!detailEntry.value?.order) return;
  changeRespondTarget.value = {
    orderId: detailEntry.value.order.id,
    change,
    response,
  };
  changeRespondComment.value = "";
  changeRespondOpen.value = true;
}

async function submitChangeRespond() {
  const target = changeRespondTarget.value;
  if (!target) return;
  if (target.response === "OBJECT" && !changeRespondComment.value.trim()) {
    message.warning("请填写异议说明");
    return;
  }
  changeRespondSaving.value = true;
  try {
    const updated = await api.respondOrderChange(target.orderId, target.change.id, {
      response: target.response,
      comment: changeRespondComment.value.trim() || undefined,
    });
    Object.assign(target.change, updated);
    changeRespondOpen.value = false;
    message.success(target.response === "AGREE" ? "已同意该变更" : "异议已提交，采购方可复核");
  } catch (e) {
    message.error(e instanceof Error ? e.message : "提交失败");
  } finally {
    changeRespondSaving.value = false;
  }
}

function canAppeal(receipt: api.PortalReceipt) {
  return (
    (receipt.inspectionStatus === "REJECTED" ||
      receipt.inspectionStatus === "PARTIAL") &&
    (!receipt.appealStatus || receipt.appealStatus === "NONE")
  );
}

function openAppeal(receipt: api.PortalReceipt) {
  appealTarget.value = receipt;
  appealReason.value = "";
  appealOpen.value = true;
}

async function submitAppeal() {
  const receipt = appealTarget.value;
  if (!receipt) return;
  if (!appealReason.value.trim()) {
    message.warning("请填写申诉理由");
    return;
  }
  appealSaving.value = true;
  try {
    const updated = await api.appealReceipt(receipt.id, appealReason.value.trim());
    Object.assign(receipt, updated);
    appealOpen.value = false;
    message.success("申诉已提交，采购方将复核质检结果");
  } catch (e) {
    message.error(e instanceof Error ? e.message : "提交失败");
  } finally {
    appealSaving.value = false;
  }
}

async function acknowledge(entry: api.PortalOrderEntry) {
  if (!entry.contract) return;
  acknowledgingId.value = entry.contract.id;
  try {
    await api.acknowledgeContract(entry.contract.id);
    message.success("已确认中标，感谢贵司合作");
    await load();
  } catch (e) {
    message.error(e instanceof Error ? e.message : "确认失败");
  } finally {
    acknowledgingId.value = undefined;
  }
}

async function submitShipment() {
  const entry = deliveryEntry.value;
  if (!entry?.order) return;
  if (!shipmentForm.deliveryNo.trim()) {
    message.warning("请填写送货单号");
    return;
  }
  const payload = {
    deliveryNo: shipmentForm.deliveryNo.trim(),
    carrier: shipmentForm.carrier.trim() || undefined,
    expectedArrival: shipmentForm.expectedArrival || undefined,
    remark: shipmentForm.remark.trim() || undefined,
  };
  const editing = shipmentEditTarget.value;
  shipmentSaving.value = true;
  try {
    if (editing) {
      await api.updateShipment(editing.id, payload);
      message.success("发货信息已更新");
      shipmentEditTarget.value = undefined;
      resetShipmentForm();
    } else {
      await api.createShipment(entry.order.id, payload);
      message.success("发货信息已提交");
      shipmentSent.value = true;
      resetShipmentForm();
    }
    await load();
    const refreshed = entries.value.find(
      (item) => item.order?.id === entry.order?.id,
    );
    if (refreshed) deliveryEntry.value = refreshed;
  } catch (e) {
    message.error(e instanceof Error ? e.message : "提交失败");
  } finally {
    shipmentSaving.value = false;
  }
}

async function removeShipment(shipment: api.ProcurementShipment) {
  try {
    await api.deleteShipment(shipment.id);
    message.success("发货记录已删除");
    if (attachmentShipment.value?.id === shipment.id) {
      attachmentShipment.value = undefined;
      shipmentAttachments.value = [];
    }
    await load();
    const entry = deliveryEntry.value;
    if (entry) {
      const refreshed = entries.value.find(
        (item) => item.order?.id === entry.order?.id,
      );
      if (refreshed) deliveryEntry.value = refreshed;
    }
  } catch (e) {
    message.error(e instanceof Error ? e.message : "删除失败");
  }
}

function isImage(contentType?: string) {
  return !!contentType && contentType.startsWith("image/");
}

async function openShipmentAttachments(shipment: api.ProcurementShipment) {
  attachmentShipment.value = shipment;
  shipmentAttachments.value = [];
  attachmentLoading.value = true;
  try {
    shipmentAttachments.value = await api.listShipmentAttachments(shipment.id);
  } catch (e) {
    message.error(e instanceof Error ? e.message : "加载附件失败");
  } finally {
    attachmentLoading.value = false;
  }
}

async function uploadShipmentAttachment(file: File) {
  const shipment = attachmentShipment.value;
  if (!shipment) return false;
  const invalid = validateUploadFile(file);
  if (invalid) {
    message.warning(invalid);
    return false;
  }
  const form = new FormData();
  form.append("file", file);
  attachmentUploading.value = true;
  try {
    const item = await api.uploadShipmentAttachment(shipment.id, form);
    shipmentAttachments.value = [item, ...shipmentAttachments.value];
    message.success("附件上传成功");
  } catch (e) {
    message.error(e instanceof Error ? e.message : "上传失败");
  } finally {
    attachmentUploading.value = false;
  }
  return false;
}

function downloadShipmentAttachment(item: api.ShipmentAttachment) {
  window.location.href = api.shipmentAttachmentDownloadUrl(
    item.shipmentId,
    item.id,
  );
}

async function deleteShipmentAttachment(item: api.ShipmentAttachment) {
  try {
    await api.deleteShipmentAttachment(item.shipmentId, item.id);
    shipmentAttachments.value = shipmentAttachments.value.filter(
      (it) => it.id !== item.id,
    );
    message.success("附件已删除");
  } catch (e) {
    message.error(e instanceof Error ? e.message : "删除失败");
  }
}

onMounted(load);
async function load() {
  loading.value = true;
  try {
    entries.value = await api.listOrders();
    const orderId =
      typeof route.query.order === "string" ? route.query.order : undefined;
    if (orderId) {
      const target = entries.value.find(
        (item) =>
          item.order?.id === orderId || item.contract?.orderId === orderId,
      );
      if (target) openDetail(target);
      await router.replace({ path: "/orders" });
    }
  } catch (e) {
    message.error(e instanceof Error ? e.message : "加载失败");
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.order-list {
  display: grid;
  gap: 14px;
}
.order-card {
  background: #fff;
  border: 1px solid #dfe6e2;
  border-left: 4px solid #176b4d;
  border-radius: 7px;
  padding: 20px 22px;
}
.order-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 14px;
}
.order-title-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.order-amount {
  text-align: right;
}
.order-amount strong {
  display: block;
  font-size: 18px;
}
.order-amount small {
  color: #75817c;
}
.order-main {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-end;
}
.order-facts h2 {
  font-size: 17px;
  margin: 0 0 6px;
}
.order-facts p {
  margin: 3px 0;
  color: #41504a;
}
.order-facts .order-meta {
  color: #75817c;
  font-size: 13px;
}
.order-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.detail-block {
  margin-top: 20px;
}
.detail-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid #e7ece9;
  margin-bottom: 18px;
}
.detail-head h2 {
  margin: 0 0 4px;
  font-size: 18px;
}
.detail-head p {
  margin: 0;
  color: #75817c;
  font-size: 13px;
}
.detail-amount {
  text-align: right;
  flex: none;
}
.detail-amount strong {
  display: block;
  font-size: 20px;
  color: #176b4d;
}
.detail-amount small {
  color: #75817c;
}
.receipt-progress {
  display: flex;
  align-items: center;
  gap: 12px;
}
.receipt-progress .ant-progress {
  flex: 1;
  margin: 0;
}
.receipt-progress span {
  color: #68756f;
  font-size: 13px;
  white-space: nowrap;
}
@media (max-width: 760px) {
  .order-head,
  .order-main {
    flex-direction: column;
    align-items: stretch;
  }
  .order-amount {
    text-align: left;
  }
  .order-actions .ant-btn {
    flex: 1;
  }
  .detail-head {
    flex-direction: column;
  }
  .detail-amount {
    text-align: left;
  }
  .receipt-progress {
    flex-direction: column;
    align-items: stretch;
  }
  .timeline-title,
  .timeline-desc {
    overflow-wrap: anywhere;
  }
  .detail-block h4 {
    margin-top: 18px;
  }
}
.shipment-review {
  margin-top: 4px;
  color: #cf1322;
  font-size: 12px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}
.attachment-shipment-label {
  margin-left: 6px;
  font-size: 13px;
  font-weight: 400;
  color: #68756f;
}
.attachment-name {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  overflow-wrap: anywhere;
}
.attachment-icon {
  color: #68756f;
}
.attachment-icon.image {
  color: #1890ff;
}
.attachment-delete {
  color: #ff4d4f;
}
</style>
