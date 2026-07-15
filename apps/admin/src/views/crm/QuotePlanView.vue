<template>
  <div class="page-stack quote-page">
    <a-card title="报价方案">
      <template #extra>
        <a-space>
          <a-button @click="loadData">刷新</a-button>
          <a-button v-if="auth.can('crm:quote:create')" type="primary" @click="openCreate">新增报价</a-button>
        </a-space>
      </template>

      <a-row :gutter="[16, 16]" class="metric-row">
        <a-col :xs="12" :lg="6"><a-statistic title="报价总数" :value="quotes.length" suffix="份" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="当前报价总额" :value="totalAmount" :formatter="moneyFormatter" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="待客户确认" :value="customerPendingCount" suffix="份" /></a-col>
        <a-col :xs="12" :lg="6"><a-statistic title="低毛利预警" :value="lowMarginCount" suffix="份" :value-style="{ color: lowMarginCount > 0 ? '#ff4d4f' : '#52c41a' }" /></a-col>
      </a-row>
      <a-alert class="section-alert" type="info" show-icon message="报价利润校验：报价阶段必须确认项目预算，系统按预算测算毛利率；低于 15% 的报价可提交内部审批，审批时重点提醒。" />

      <div class="quote-toolbar">
        <a-input v-model:value="keyword" allow-clear placeholder="搜索报价编号、客户或服务范围" />
        <a-select v-model:value="statusFilter" :options="statusOptions" />
        <a-checkbox v-model:checked="showConverted">显示已转合同</a-checkbox>
      </div>

      <!-- desktop-table --><div class="desktop-table">
<a-table
        :columns="columns"
        :data-source="filteredQuotes"
        :loading="loading"
        :row-key="(record: any) => record.id"
        :scroll="{ x: 1280 }"
        :customRow="(record: any) => ({ onClick: (e: any) => { if (e.target?.closest?.('button,a,.ant-btn,.ant-tag,.ant-popconfirm,.ant-dropdown-trigger,input,select,[role=button]')) return; router.push('/crm/quotes/' + record.id) } })"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'quote'">
            <div class="quote-title-line">
              <strong>{{ record.code }}</strong>
              <a-tag>V{{ record.versionNo }}</a-tag>
            </div>
            <span class="table-subtitle">{{ record.customerName }} · {{ record.opportunityCode || "未关联商机" }}</span>
          </template>
          <template v-else-if="column.key === 'scope'">
            <span class="line-clamp-2">{{ record.serviceScope }}</span>
            <span v-if="record.inspectCycle" class="table-subtitle">{{ record.inspectCycle }}</span>
          </template>
          <template v-else-if="column.key === 'amount'"><strong>{{ formatMoney(record.amount) }}</strong><span class="table-subtitle">未税 {{ formatMoney(record.netAmount ?? calcNetAmount(record.amount, record.taxRate)) }} · 税率 {{ formatTaxRate(record.taxRate) }}</span></template>
          <template v-else-if="column.key === 'margin'">
            <a-tag :color="quoteMargin(record).rate < 15 ? 'red' : quoteMargin(record).rate < 25 ? 'orange' : 'green'">{{ quoteMargin(record).rate.toFixed(1) }}%</a-tag>
            <span class="table-subtitle">预算 {{ formatMoney(quoteMargin(record).cost) }} · 毛利 {{ formatMoney(quoteMargin(record).gross) }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="quoteStatusColor(record.status)">{{ quoteStatusLabel(record.status) }}</a-tag>
            <span v-if="record.lastApproverName" class="table-subtitle">
              {{ record.lastApproverName }} · {{ record.lastApprovalComment }}
            </span>
          </template>
          <template v-else-if="column.key === 'customerResult'">
            <template v-if="record.customerDecision">
              <span class="result-text">{{ record.customerComment }}</span>
              <span class="table-subtitle">{{ record.customerDecisionBy }} · {{ formatDateTime(record.customerDecidedAt) }}</span>
            </template>
            <span v-else class="muted-text">尚未登记</span>
          </template>
          <template v-else-if="column.key === 'updatedAt'">{{ formatDateTime(record.updatedAt) }}</template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small" wrap>
              <a-button
                v-if="canEdit(record)"
                size="small"
                type="link"
                @click="openEdit(record)"
              >
                修订报价
              </a-button>
              <a-popconfirm
                v-if="auth.can('crm:quote:update') && canRequestCost(record)"
                title="确认向项目管理发起成本询价？"
                @confirm="openCostRequest(record)"
              >
                <a-button size="small" type="link">发起询价</a-button>
              </a-popconfirm>
              <a-button
                v-if="auth.can('crm:quote:cost') && canSubmitCost(record)"
                size="small"
                type="link"
                @click="openCostSubmit(record)"
              >
                填写成本
              </a-button>
              <a-button
                v-if="auth.can('crm:quote:cost') && canApproveCost(record)"
                size="small"
                type="link"
                @click="openCostApproval(record)"
              >
                成本审批
              </a-button>
              <a-popconfirm
                v-if="auth.can('crm:quote:submit') && (record.status === 'DRAFT' || record.status === 'COST_APPROVED')"
                :title="quoteSubmitTip(record)"
                @confirm="handleSubmit(record)"
              >
                <a-button size="small" type="link" :disabled="!quoteBudgetConfirmed(record)">提交审批</a-button>
              </a-popconfirm>
              <a-button
                v-if="auth.can('crm:quote:approve') && record.status === 'PENDING_APPROVAL'"
                size="small"
                type="link"
                @click="openApproval(record)"
              >
                审批处理
              </a-button>
              <a-button
                v-if="auth.can('crm:quote:customer-result') && record.status === 'APPROVED'"
                size="small"
                type="link"
                @click="openCustomerResult(record)"
              >
                客户结果
              </a-button>
              <a-button
                v-if="auth.can('crm:quote:convert') && record.status === 'CUSTOMER_ACCEPTED'"
                size="small"
                type="link"
                @click="openConversion(record)"
              >
                转合同
              </a-button>
              <a-button size="small" type="link" @click="openRevisions(record)">版本记录</a-button>
              <span @click.stop>
                <a-popconfirm
                  v-if="auth.can('crm:quote:delete')"
                  title="确实删除此报价？"
                  @confirm="handleDeleteQuote(record)"
                >
                  <a-button size="small" type="link" danger>删除</a-button>
                </a-popconfirm>
              </span>
            </a-space>
          </template>
        </template>
      </a-table>
</div><!-- end desktop-table -->
    <div class="mobile-only">
      <div v-for="record in filteredQuotes" :key="record.id" class="mobile-card-item" @click="router.push('/crm/quotes/' + record.id)">
        <div class="mobile-card-header"><strong>{{ record.code }}</strong><a-tag :color="quoteStatusColor(record.status)">{{ quoteStatusLabel(record.status) }}</a-tag></div>
        <div class="mobile-card-body"><span>{{ record.customerName }}</span><strong>{{ formatMoney(record.amount) }}</strong></div>
        <div class="mobile-card-tags">未税 {{ formatMoney(record.netAmount ?? calcNetAmount(record.amount, record.taxRate)) }} · 税率 {{ formatTaxRate(record.taxRate) }}</div>
        <div class="mobile-card-tags">{{ record.opportunityCode || "未关联商机" }}</div>
      </div>
    </div>
    </a-card>

    <a-modal
      v-model:open="editorOpen"
      :title="editingQuote ? `修订报价 · ${editingQuote.code} V${editingQuote.versionNo}` : '新增报价方案'"
      width="780px"
      :confirm-loading="saving"
      @ok="handleSaveQuote"
    >
      <a-alert
        v-if="editingQuote"
        class="section-alert"
        type="info"
        show-icon
        message="保存后将生成新版本并回到草稿状态，需重新提交内部审批。"
      />
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="8">
          </a-col>
          <a-col :xs="24" :md="16">
            <a-form-item label="关联商机" name="opportunityId">
              <a-select
                v-model:value="form.opportunityId"
                show-search
                option-filter-prop="label"
                :disabled="Boolean(editingQuote)"
                :options="opportunityOptions"
                @change="syncCustomer"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24"><a-form-item label="服务范围" name="serviceScope"><a-textarea v-model:value="form.serviceScope" :rows="3" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="服务频次"><a-input v-model:value="form.inspectCycle" placeholder="例如：季度服务，年度检测" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="报价金额" name="amount"><a-input-number v-model:value="form.amount" :min="0" class="full-input" /></a-form-item></a-col>
          <a-col :xs="24" :md="12"><a-form-item label="税率(%)"><a-input-number v-model:value="form.taxRate" :min="0" :max="100" :precision="2" class="full-input" /></a-form-item></a-col>
          <a-col :span="24">
            <div class="quote-budget-panel">
              <div class="quote-budget-head">
                <strong>项目预算确认</strong>
                <a-space>
                  <a-tag :color="formMargin.rate < 15 ? 'red' : formMargin.rate < 25 ? 'orange' : 'green'">毛利率 {{ formMargin.rate.toFixed(1) }}%</a-tag>
                  <a-button size="small" @click="applyDefaultBudget">按标准比例生成</a-button>
                </a-space>
              </div>
              <a-row :gutter="12">
                <a-col :xs="12" :md="8"><a-form-item label="人工预算"><a-input-number v-model:value="form.laborBudget" :min="0" :precision="2" class="full-input" /></a-form-item></a-col>
                <a-col :xs="12" :md="8"><a-form-item label="材料预算"><a-input-number v-model:value="form.materialBudget" :min="0" :precision="2" class="full-input" /></a-form-item></a-col>
                <a-col :xs="12" :md="8"><a-form-item label="外包预算"><a-input-number v-model:value="form.subcontractBudget" :min="0" :precision="2" class="full-input" /></a-form-item></a-col>
                <a-col :xs="12" :md="8"><a-form-item label="差旅预算"><a-input-number v-model:value="form.travelBudget" :min="0" :precision="2" class="full-input" /></a-form-item></a-col>
                <a-col :xs="12" :md="8"><a-form-item label="其他预算"><a-input-number v-model:value="form.otherBudget" :min="0" :precision="2" class="full-input" /></a-form-item></a-col>
                <a-col :xs="12" :md="8">
                  <div class="quote-budget-summary">
                    <span>预算合计</span>
                    <strong>{{ formatMoney(formMargin.cost) }}</strong>
                    <p>预计毛利 {{ formatMoney(formMargin.gross) }}</p>
                  </div>
                </a-col>
              </a-row>
            </div>
          </a-col>
          <a-col :span="24"><a-form-item label="付款节点"><a-input v-model:value="form.paymentNodes" placeholder="例如：签约30%，半年节点30%，验收40%" /></a-form-item></a-col>
          <a-col v-if="editingQuote" :span="24">
            <a-form-item label="本次修订说明" required>
              <a-textarea v-model:value="form.revisionNote" :rows="2" placeholder="说明本次价格、范围、频次或付款节点的调整" />
            </a-form-item>
          </a-col>
          <a-col :span="24"><a-form-item :label="editingQuote ? '修订人' : '创建人'"><a-input v-model:value="form.editorName" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal v-model:open="approvalOpen" title="内部报价审批" width="640px" :confirm-loading="saving" @ok="handleApproval">
      <a-alert
        v-if="selectedQuote"
        class="section-alert"
        :type="selectedQuoteMargin.rate < 15 ? 'warning' : 'info'"
        show-icon
        :message="`${selectedQuote.code} V${selectedQuote.versionNo} · ${selectedQuote.customerName} · ${formatMoney(selectedQuote.amount)} · 毛利率 ${selectedQuoteMargin.rate.toFixed(1)}%`"
        :description="selectedQuoteMargin.rate < 15 ? `毛利率低于 15%，预算 ${formatMoney(selectedQuoteMargin.cost)}，预计毛利 ${formatMoney(selectedQuoteMargin.gross)}。请重点复核预算构成、折扣授权和服务范围，必要时在审批意见中说明风险。` : '审批通过仅代表允许向客户发送本版报价，不会生成合同或应收。'"
      />
      <div v-if="selectedQuote" class="approval-margin-card" :class="{ warning: selectedQuoteMargin.rate < 15 }">
        <div>
          <span>预算成本</span>
          <strong>{{ formatMoney(selectedQuoteMargin.cost) }}</strong>
        </div>
        <div>
          <span>预计毛利</span>
          <strong>{{ formatMoney(selectedQuoteMargin.gross) }}</strong>
        </div>
        <div>
          <span>毛利率</span>
          <strong>{{ selectedQuoteMargin.rate.toFixed(1) }}%</strong>
        </div>
      </div>
      <a-form ref="approvalFormRef" :model="approvalForm" :rules="approvalRules" layout="vertical">
        <a-form-item label="审批结论" name="decision">
          <a-radio-group v-model:value="approvalForm.decision" button-style="solid">
            <a-radio-button value="APPROVED">通过</a-radio-button>
            <a-radio-button value="REJECTED">驳回</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="审批人" name="approverName"><a-input v-model:value="approvalForm.approverName" /></a-form-item>
        <a-form-item label="审批意见" name="comment"><a-textarea v-model:value="approvalForm.comment" :rows="3" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="customerResultOpen" title="登记客户结果" width="640px" :confirm-loading="saving" @ok="handleCustomerResult">
      <a-alert
        v-if="selectedQuote"
        class="section-alert"
        type="info"
        show-icon
        :message="`${selectedQuote.code} V${selectedQuote.versionNo} · ${selectedQuote.customerName}`"
        description="客户拒绝后仍可继续修订报价；客户接受后才可以进入转合同流程。"
      />
      <a-form ref="customerResultFormRef" :model="customerResultForm" :rules="customerResultRules" layout="vertical">
        <a-form-item label="客户结果" name="decision">
          <a-radio-group v-model:value="customerResultForm.decision" button-style="solid" @change="syncCustomerComment">
            <a-radio-button value="ACCEPTED">客户接受</a-radio-button>
            <a-radio-button value="DECLINED">客户拒绝</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="登记人" name="operatorName"><a-input v-model:value="customerResultForm.operatorName" /></a-form-item>
        <a-form-item label="结果说明" name="comment"><a-textarea v-model:value="customerResultForm.comment" :rows="3" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="conversionOpen" title="报价转合同" width="860px" :confirm-loading="saving" @ok="handleConversion">
      <a-alert
        v-if="selectedQuote"
        class="section-alert"
        type="success"
        show-icon
        :message="`${selectedQuote.code} V${selectedQuote.versionNo} 已获客户接受`"
        description="确认后生成合同及全部应收，同时将关联商机标记为赢单。"
      />
      <a-form ref="conversionFormRef" :model="conversionForm" :rules="conversionRules" layout="vertical">
        <a-divider>合同信息</a-divider>
        <a-row :gutter="16">
          <a-col :xs="24" :md="8"><a-form-item label="合同编号" name="contractCode"><a-input v-model:value="conversionForm.contractCode" /></a-form-item></a-col>
          <a-col :xs="24" :md="10"><a-form-item label="合同项目名称" name="projectName"><a-input v-model:value="conversionForm.projectName" /></a-form-item></a-col>
          <a-col :xs="24" :md="6"><a-form-item label="合同类型" name="contractType"><a-select v-model:value="conversionForm.contractType" :options="contractTypeOptions" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="开始日期" name="startDate"><a-input v-model:value="conversionForm.startDate" type="date" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="结束日期" name="endDate"><a-input v-model:value="conversionForm.endDate" type="date" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item label="服务周期"><a-input v-model:value="conversionForm.serviceCycle" /></a-form-item></a-col>
        </a-row>

        <a-form-item label="合同附件" required>
          <a-upload :show-upload-list="false" :before-upload="selectConversionAttachment" accept=".jpg,.jpeg,.png,.webp,.pdf,.doc,.docx,.xls,.xlsx">
            <a-button><template #icon><UploadOutlined /></template>选择合同附件</a-button>
          </a-upload>
          <div v-if="conversionAttachment" class="selected-attachment">
            <span>{{ conversionAttachment.name }}</span>
            <a-button type="link" size="small" @click="conversionAttachment = null">移除</a-button>
          </div>
          <div v-else class="form-help">请上传合同文件，转合同后将自动归档到合同附件。</div>
        </a-form-item>

        <a-divider>应收账款分期</a-divider><div class="receivable-summary" style="margin-bottom: 12px"><a-statistic title="合同总额" :value="selectedQuote?.amount || 0" :formatter="moneyFormatter" style="display: inline-block; margin-right: 32px" /><a-statistic title="已分配" :value="receivableAllocated" :formatter="moneyFormatter" style="display: inline-block; margin-right: 32px" /><a-statistic title="剩余" :value="Math.max(0, (selectedQuote?.amount || 0) - receivableAllocated)" :formatter="moneyFormatter" style="display: inline-block" :value-style="{ color: receivableAllocated > (selectedQuote?.amount || 0) ? '#ff4d4f' : '#52c41a' }" /></div><a-table :data-source="conversionForm.receivables" :columns="receivableColumns" :pagination="false" row-key="rowKey" size="small"><template #bodyCell="{ column, record, index }"><template v-if="column.key === 'amount'"><a-input-number v-model:value="record.amount" :min="0" :precision="2" style="width: 100%" @change="(val: any) => syncReceivable(index, 'amount', val)" /></template><template v-else-if="column.key === 'ratio'"><a-input-number v-model:value="record.ratio" :min="0" :max="100" :precision="1" style="width: 100%" @change="(val: any) => syncReceivable(index, 'ratio', val)" /></template><template v-else-if="column.key === 'dueDate'"><a-input v-model:value="record.dueDate" type="date" style="width: 100%" /></template><template v-else-if="column.key === 'action'"><a-button type="link" danger size="small" @click="removeReceivable(index)">删除</a-button></template></template></a-table><a-button type="dashed" block style="margin-top: 8px" @click="addReceivable">+ 新增分期</a-button>
      </a-form>
    </a-modal>

    <a-modal v-model:open="costRequestOpen" title="发起项目询价" width="520px" :confirm-loading="saving" @ok="handleCostRequest">
      <a-alert
        v-if="selectedQuote"
        class="section-alert"
        type="info"
        show-icon
        :message="`${selectedQuote.code} · ${selectedQuote.customerName}`"
        description="项目管理填写成本并审批通过后，成本会自动回写到报价预算。"
      />
      <a-form ref="costRequestFormRef" :model="costRequestForm" :rules="costRequestRules" layout="vertical">
        <a-form-item label="询价发起人" name="requestedBy"><a-input v-model:value="costRequestForm.requestedBy" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="costSubmitOpen" title="填写售前支持成本" width="860px" :confirm-loading="saving" @ok="handleCostSubmit">
      <a-alert
        v-if="selectedQuote"
        class="section-alert"
        type="info"
        show-icon
        :message="`${selectedQuote.code} · ${selectedQuote.customerName}`"
        description="人工默认税率 6%，材料和设备默认税率 13%，差旅、风险预留和其他成本不设置税率。"
      />
      <a-form ref="costSubmitFormRef" :model="costSubmitForm" :rules="costSubmitRules" layout="vertical">
        <div class="cost-summary-strip">
          <a-form-item label="项目负责人" name="projectManager" class="cost-summary-owner">
            <a-input v-model:value="costSubmitForm.projectManager" />
          </a-form-item>
          <div>
            <span>成本合计</span>
            <strong>{{ formatMoney(costSubmitTotal) }}</strong>
          </div>
          <a-form-item label="建议报价" class="cost-summary-price">
            <a-input-number v-model:value="costSubmitForm.suggestedPrice" :min="0" :precision="2" class="full-input" />
          </a-form-item>
        </div>

        <div class="cost-entry-table">
          <div class="cost-entry-head">
            <span>成本类型</span>
            <span>金额</span>
            <span>税率</span>
          </div>
          <div v-for="item in costEntryRows" :key="item.amountKey" class="cost-entry-row">
            <div>
              <strong>{{ item.title }}</strong>
              <span>{{ item.description }}</span>
            </div>
            <a-input-number v-model:value="costSubmitForm[item.amountKey]" :min="0" :precision="2" class="full-input" />
            <a-input-number
              v-if="item.taxKey"
              v-model:value="costSubmitForm[item.taxKey]"
              :min="0"
              :max="100"
              :precision="2"
              addon-after="%"
              class="full-input"
            />
            <a-tag v-else color="default">无税率</a-tag>
          </div>
        </div>

        <a-form-item label="成本说明">
          <a-textarea v-model:value="costSubmitForm.costRemark" :rows="3" placeholder="填写成本口径、供应商依据、风险说明或售前支持范围" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="costApprovalOpen" title="项目成本审批" width="620px" :confirm-loading="saving" @ok="handleCostApproval">
      <a-alert
        v-if="selectedCostRequest"
        class="section-alert"
        type="warning"
        show-icon
        :message="`成本合计 ${formatMoney(selectedCostRequest.totalCost || 0)} · 建议报价 ${formatMoney(selectedCostRequest.suggestedPrice || 0)}`"
        :description="selectedCostRequest.costRemark || '项目管理未填写备注'"
      />
      <a-form ref="costApprovalFormRef" :model="costApprovalForm" :rules="costApprovalRules" layout="vertical">
        <a-form-item label="审批结论" name="decision">
          <a-radio-group v-model:value="costApprovalForm.decision" button-style="solid">
            <a-radio-button value="APPROVED">通过</a-radio-button>
            <a-radio-button value="REJECTED">驳回</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="审批人" name="approverName"><a-input v-model:value="costApprovalForm.approverName" /></a-form-item>
        <a-form-item label="审批意见" name="comment"><a-textarea v-model:value="costApprovalForm.comment" :rows="3" /></a-form-item>
      </a-form>
    </a-modal>

    <a-drawer v-model:open="revisionsOpen" width="720" title="报价版本记录">
      <div v-if="revisionQuote" class="revision-heading">
        <div>
          <strong>{{ revisionQuote.code }}</strong>
          <span>{{ revisionQuote.customerName }}</span>
        </div>
        <a-tag color="blue">当前 V{{ revisionQuote.versionNo }}</a-tag>
      </div>
      <a-table
        size="small"
        :columns="revisionColumns"
        :data-source="revisions"
        :loading="revisionLoading"
        :pagination="false"
        :row-key="(record: any) => record.id"
        :scroll="{ x: 760 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'version'"><strong>V{{ record.versionNo }}</strong></template>
          <template v-else-if="column.key === 'content'">
            <span class="line-clamp-2">{{ record.serviceScope }}</span>
            <span class="table-subtitle">{{ record.inspectCycle || "未设置频次" }} · {{ formatMoney(record.amount) }} · 毛利率 {{ quoteMargin(record).rate.toFixed(1) }}%</span>
          </template>
          <template v-else-if="column.key === 'revision'">
            <span>{{ record.revisionNote }}</span>
            <span class="table-subtitle">{{ record.editorName }} · {{ formatDateTime(record.revisedAt) }}</span>
          </template>
        </template>
      </a-table>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { message } from "ant-design-vue";
import UploadOutlined from "@ant-design/icons-vue/UploadOutlined";
import {
  convertQuote,
  createQuote,
  approveQuoteCost,
  listOpportunities,
  listQuoteRevisions,
  listQuotes,
  processQuoteApproval,
  processQuoteCustomerResult,
  requestQuoteCost,
  submitQuoteCost,
  submitQuote,
  updateQuote,
  uploadAttachment,
  type ApprovalDecision,
  type Opportunity,
  type QuoteCustomerDecision,
  type QuotePlan,
  type QuoteRevision,
  type QuoteStatus,
  type QuoteCostRequest,
} from "@/api/crm";
import { useAuthStore } from "@/stores/auth";
import { formatMoney, generateCode, quoteStatusColor, quoteStatusLabel } from "./crm-options";
import { deleteQuote } from "@/api/crm";

const router = useRouter();
const auth = useAuthStore();
const quotes = ref<QuotePlan[]>([]);
const opportunities = ref<Opportunity[]>([]);
const revisions = ref<QuoteRevision[]>([]);
const loading = ref(false);
const saving = ref(false);
const revisionLoading = ref(false);
const editorOpen = ref(false);
const approvalOpen = ref(false);
const customerResultOpen = ref(false);
const conversionOpen = ref(false);
const revisionsOpen = ref(false);
const costRequestOpen = ref(false);
const costSubmitOpen = ref(false);
const costApprovalOpen = ref(false);
const formRef = ref();
const approvalFormRef = ref();
const customerResultFormRef = ref();
const conversionFormRef = ref();
const costRequestFormRef = ref();
const costSubmitFormRef = ref();
const costApprovalFormRef = ref();
const selectedQuote = ref<QuotePlan | null>(null);
const editingQuote = ref<QuotePlan | null>(null);
const revisionQuote = ref<QuotePlan | null>(null);
const selectedCostRequest = ref<QuoteCostRequest | null>(null);
const keyword = ref("");
const statusFilter = ref<QuoteStatus | "ALL">("ALL");
const showConverted = ref(false);
const form = reactive(initialForm());
const approvalForm = reactive(initialApprovalForm());
const customerResultForm = reactive(initialCustomerResultForm());
const conversionForm = reactive(initialConversionForm());
const costRequestForm = reactive(initialCostRequestForm());
const costSubmitForm = reactive(initialCostSubmitForm());
const costApprovalForm = reactive(initialCostApprovalForm());
const conversionAttachment = ref<File | null>(null);

const rules = {
  code: [],
  opportunityId: [{ required: true, message: "请选择关联商机" }],
  serviceScope: [{ required: true, message: "请输入服务范围" }],
  amount: [{ required: true, message: "请输入报价金额" }],
};
const approvalRules = {
  decision: [{ required: true, message: "请选择审批结论" }],
  comment: [{ required: true, message: "请输入审批意见" }],
  approverName: [{ required: true, message: "请输入审批人" }],
};
const customerResultRules = {
  decision: [{ required: true, message: "请选择客户结果" }],
  comment: [{ required: true, message: "请输入结果说明" }],
  operatorName: [{ required: true, message: "请输入登记人" }],
};
const costRequestRules = {
  requestedBy: [{ required: true, message: "请输入询价发起人" }],
};
const costSubmitRules = {
  projectManager: [{ required: true, message: "请输入项目负责人" }],
};
const costApprovalRules = {
  decision: [{ required: true, message: "请选择审批结论" }],
  approverName: [{ required: true, message: "请输入审批人" }],
  comment: [{ required: true, message: "请输入审批意见" }],
};
const conversionRules = {
  contractCode: [{ required: true, message: "请输入合同编号" }],
  projectName: [{ required: true, message: "请输入合同项目名称" }],
  contractType: [{ required: true, message: "请选择合同类型" }],
  startDate: [{ required: true, message: "请选择开始日期" }],
  endDate: [{ required: true, message: "请选择结束日期" }],
};
const contractTypeOptions = [
  { label: "年度服务合同", value: "年度服务合同" },
  { label: "单次服务合同", value: "单次服务合同" },
  { label: "驻点服务合同", value: "驻点服务合同" },
  { label: "设备质保合同", value: "设备质保合同" },
];
const statusOptions = [
  { label: "全部状态", value: "ALL" },
  { label: "草稿", value: "DRAFT" },
  { label: "已询价", value: "COST_REQUESTED" },
  { label: "成本测算中", value: "COSTING" },
  { label: "成本已确认", value: "COST_APPROVED" },
  { label: "审批中", value: "PENDING_APPROVAL" },
  { label: "待客户确认", value: "APPROVED" },
  { label: "内部驳回", value: "REJECTED" },
  { label: "客户已接受", value: "CUSTOMER_ACCEPTED" },
  { label: "客户已拒绝", value: "CUSTOMER_DECLINED" },
  { label: "已转合同", value: "CONVERTED" },
];
;
const receivableAllocated = computed(() =>
  (conversionForm.receivables || []).reduce((sum, r) => sum + Number(r.amount || 0), 0)
);
const receivableColumns = [
  { title: "应收金额", key: "amount", width: 160 },
  { title: "比例(%)", key: "ratio", width: 120 },
  { title: "到期日", key: "dueDate", width: 140 },
  { title: "操作", key: "action", width: 70 },
];
function addReceivable() {
  conversionForm.receivables.push({
    rowKey: "r" + Date.now(),
    receivableCode: "",
    amount: 0,
    ratio: 0,
    dueDate: "",
  });
}
function removeReceivable(index: number) {
  conversionForm.receivables.splice(index, 1);
}
function syncReceivable(index: number, field: string, value: string | number) {
  const item = conversionForm.receivables[index];
  if (!item) return;
  const total = Number(selectedQuote.value?.amount || 0);
  if (total <= 0) return;
  if (field === "amount") {
    item.ratio = Math.round(Number(value || 0) / total * 100 * 10) / 10;
  } else if (field === "ratio") {
    item.amount = Math.round(total * Number(value || 0) / 100 * 100) / 100;
  }
}

const columns = [
  { title: "报价 / 客户", key: "quote", width: 250 },
  { title: "方案内容", key: "scope", width: 320 },
  { title: "报价金额", key: "amount", width: 180 },
  { title: "毛利校验", key: "margin", width: 150 },
  { title: "流程状态", key: "status", width: 220 },
  { title: "客户反馈", key: "customerResult", width: 260 },
  { title: "更新时间", key: "updatedAt", width: 150 },
  { title: "操作", key: "action", width: 280, fixed: "right" },
];
const revisionColumns = [
  { title: "版本", key: "version", width: 80 },
  { title: "报价内容", key: "content", width: 330 },
  { title: "修订记录", key: "revision", width: 320 },
];
const opportunityOptions = computed(() => opportunities.value
  .filter((item) => item.stage !== "LOST")
  .map((item) => ({ label: `${item.code} · ${item.customerName} · ${item.needSummary}`, value: item.id })));
const filteredQuotes = computed(() => {
  const normalized = keyword.value.trim().toLowerCase();
  return quotes.value.filter((item) => {
    const matchesStatus = statusFilter.value === "ALL" || item.status === statusFilter.value;
    const matchesKeyword = !normalized || [item.code, item.customerName, item.opportunityCode, item.serviceScope]
      .some((value) => value?.toLowerCase().includes(normalized));
    if (!showConverted.value && item.status === "CONVERTED") return false;
    return matchesStatus && matchesKeyword;
  });
});
const totalAmount = computed(() => quotes.value.reduce((sum, item) => sum + Number(item.amount || 0), 0));
const customerPendingCount = computed(() => quotes.value.filter((item) => item.status === "APPROVED").length);
const customerAcceptedCount = computed(() => quotes.value.filter((item) => item.status === "CUSTOMER_ACCEPTED").length);
const lowMarginCount = computed(() => quotes.value.filter((item) => quoteMargin(item).rate < 15).length);
const selectedQuoteMargin = computed(() => selectedQuote.value ? quoteMargin(selectedQuote.value) : { cost: 0, gross: 0, rate: 0 });
const formMargin = computed(() => {
  const amount = Number(form.amount || 0);
  const cost = quoteBudgetTotal(form);
  const gross = amount - cost;
  return { cost, gross, rate: amount > 0 ? gross / amount * 100 : 0 };
});
const costSubmitTotal = computed(() => quoteCostTotal(costSubmitForm));
type CostAmountKey = "laborCost" | "materialCost" | "subcontractCost" | "travelCost" | "equipmentCost" | "riskReserve" | "otherCost";
type CostTaxKey = "laborTaxRate" | "materialTaxRate" | "subcontractTaxRate" | "equipmentTaxRate";
const costEntryRows: Array<{ title: string; description: string; amountKey: CostAmountKey; taxKey?: CostTaxKey }> = [
  { title: "人工成本", description: "售前方案、现场踏勘、技术支持等人工投入", amountKey: "laborCost", taxKey: "laborTaxRate" },
  { title: "材料成本", description: "项目所需材料、耗材、配件", amountKey: "materialCost", taxKey: "materialTaxRate" },
  { title: "外包成本", description: "第三方服务、专业分包、外协支持", amountKey: "subcontractCost", taxKey: "subcontractTaxRate" },
  { title: "差旅成本", description: "交通、住宿、现场补贴等，不设置税率", amountKey: "travelCost" },
  { title: "设备成本", description: "设备采购、租赁、检测工具", amountKey: "equipmentCost", taxKey: "equipmentTaxRate" },
  { title: "风险预留", description: "范围变更、不可预见费用，不设置税率", amountKey: "riskReserve" },
  { title: "其他成本", description: "无法归类的售前支持费用，不设置税率", amountKey: "otherCost" },
];

function quoteMargin(record: Pick<QuotePlan, "amount" | "laborBudget" | "materialBudget" | "subcontractBudget" | "travelBudget" | "otherBudget" | "budgetAmount" | "grossMargin" | "grossMarginRate">) {
  const amount = Number(record.amount || 0);
  const cost = Number(record.budgetAmount ?? quoteBudgetTotal(record));
  const gross = Number(record.grossMargin ?? (amount - cost));
  const rate = Number(record.grossMarginRate ?? (amount > 0 ? gross / amount * 100 : 0));
  return { cost, gross, rate };
}

function quoteBudgetTotal(record: { laborBudget?: number; materialBudget?: number; subcontractBudget?: number; travelBudget?: number; otherBudget?: number; amount?: number }) {
  return quoteBudgetRawTotal(record);
}

function quoteBudgetRawTotal(record: { laborBudget?: number; materialBudget?: number; subcontractBudget?: number; travelBudget?: number; otherBudget?: number }) {
  return Number(record.laborBudget || 0)
    + Number(record.materialBudget || 0)
    + Number(record.subcontractBudget || 0)
    + Number(record.travelBudget || 0)
    + Number(record.otherBudget || 0);
}

function quoteCostTotal(record: { laborCost?: number; materialCost?: number; subcontractCost?: number; travelCost?: number; equipmentCost?: number; riskReserve?: number; otherCost?: number }) {
  return Number(record.laborCost || 0)
    + Number(record.materialCost || 0)
    + Number(record.subcontractCost || 0)
    + Number(record.travelCost || 0)
    + Number(record.equipmentCost || 0)
    + Number(record.riskReserve || 0)
    + Number(record.otherCost || 0);
}

function applyDefaultBudget() {
  const amount = Number(form.amount || 0);
  form.laborBudget = roundMoney(amount * 0.2);
  form.materialBudget = roundMoney(amount * 0.35);
  form.subcontractBudget = roundMoney(amount * 0.1);
  form.travelBudget = roundMoney(amount * 0.03);
  form.otherBudget = roundMoney(amount * 0.02);
}

function roundMoney(value: number) {
  return Math.round(Number(value || 0) * 100) / 100;
}

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    [quotes.value, opportunities.value] = await Promise.all([listQuotes(), listOpportunities()]);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "报价加载失败");
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  editingQuote.value = null;
  Object.assign(form, initialForm());
  editorOpen.value = true;
}

function openEdit(record: QuotePlan) {
  editingQuote.value = record;
  Object.assign(form, {
    customerId: record.customerId,
    opportunityId: record.opportunityId,
    code: record.code,
    serviceScope: record.serviceScope,
    inspectCycle: record.inspectCycle || "",
    paymentNodes: record.paymentNodes || "",
    amount: Number(record.amount),
    taxRate: Number(record.taxRate ?? 13),
    laborBudget: Number(record.laborBudget || 0),
    materialBudget: Number(record.materialBudget || 0),
    subcontractBudget: Number(record.subcontractBudget || 0),
    travelBudget: Number(record.travelBudget || 0),
    otherBudget: Number(record.otherBudget || 0),
    revisionNote: "",
    editorName: auth.user?.displayName || "",
  });
  editorOpen.value = true;
}

function syncCustomer(opportunityId: string) {
  const opp = opportunities.value.find((item) => item.id === opportunityId);
  form.customerId = opp?.customerId;
  if (opp?.code) {
    form.code = "BJ-" + opp.code.replace(/^SJ-/, "");
  }
}

async function handleSaveQuote() {
  await formRef.value?.validate();
  if (!form.editorName.trim()) {
    message.warning("请输入创建人或修订人");
    return;
  }
  if (editingQuote.value && !form.revisionNote.trim()) {
    message.warning("请填写本次修订说明");
    return;
  }
  saving.value = true;
  try {
    if (editingQuote.value) {
      await updateQuote(editingQuote.value.id, {
        serviceScope: form.serviceScope,
        inspectCycle: form.inspectCycle,
        paymentNodes: form.paymentNodes,
        amount: form.amount,
        taxRate: form.taxRate,
        laborBudget: form.laborBudget,
        materialBudget: form.materialBudget,
        subcontractBudget: form.subcontractBudget,
        travelBudget: form.travelBudget,
        otherBudget: form.otherBudget,
        revisionNote: form.revisionNote,
        editorName: form.editorName,
      });
      message.success(`已生成 ${editingQuote.value.code} V${editingQuote.value.versionNo + 1}`);
    } else {
      await createQuote({
        customerId: form.customerId,
        opportunityId: form.opportunityId,
        code: form.code,
        serviceScope: form.serviceScope,
        inspectCycle: form.inspectCycle,
        paymentNodes: form.paymentNodes,
        amount: form.amount,
        taxRate: form.taxRate,
        laborBudget: form.laborBudget,
        materialBudget: form.materialBudget,
        subcontractBudget: form.subcontractBudget,
        travelBudget: form.travelBudget,
        otherBudget: form.otherBudget,
        editorName: form.editorName,
      });
      message.success("报价方案已保存为 V1 草稿");
    }
    editorOpen.value = false;
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "报价保存失败");
  } finally {
    saving.value = false;
  }
}

async function handleSubmit(record: QuotePlan) {
  const margin = quoteMargin(record);
  if (!quoteBudgetConfirmed(record)) {
    message.warning("请先修订报价并确认项目预算，再提交审批");
    return;
  }
  try {
    await submitQuote(record.id);
    message.success(margin.rate < 15
      ? `${record.code} V${record.versionNo} 已提交内部审批，毛利率 ${margin.rate.toFixed(1)}%，请审批重点关注`
      : `${record.code} V${record.versionNo} 已提交内部审批，毛利率 ${margin.rate.toFixed(1)}%`);
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "提交审批失败");
  }
}

function canRequestCost(record: QuotePlan) {
  return record.status === "DRAFT" || record.status === "REJECTED";
}

function canSubmitCost(record: QuotePlan) {
  return record.status === "COST_REQUESTED" && Boolean(record.costRequest?.id);
}

function canApproveCost(record: QuotePlan) {
  return record.status === "COSTING" && record.costRequest?.status === "SUBMITTED";
}

function openCostRequest(record: QuotePlan) {
  selectedQuote.value = record;
  Object.assign(costRequestForm, initialCostRequestForm());
  costRequestOpen.value = true;
}

async function handleCostRequest() {
  await costRequestFormRef.value?.validate();
  if (!selectedQuote.value) return;
  saving.value = true;
  try {
    await requestQuoteCost(selectedQuote.value.id, { ...costRequestForm });
    costRequestOpen.value = false;
    message.success("已发起项目成本询价");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "发起询价失败");
  } finally {
    saving.value = false;
  }
}

function openCostSubmit(record: QuotePlan) {
  if (!record.costRequest?.id) {
    message.warning("请先发起项目询价");
    return;
  }
  selectedQuote.value = record;
  selectedCostRequest.value = record.costRequest;
  Object.assign(costSubmitForm, initialCostSubmitForm(record.costRequest));
  costSubmitOpen.value = true;
}

async function handleCostSubmit() {
  await costSubmitFormRef.value?.validate();
  if (!selectedCostRequest.value) return;
  if (costSubmitTotal.value <= 0) {
    message.warning("请至少填写一项成本");
    return;
  }
  saving.value = true;
  try {
    await submitQuoteCost(selectedCostRequest.value.id, { ...costSubmitForm });
    costSubmitOpen.value = false;
    message.success("项目成本已提交审批");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "成本提交失败");
  } finally {
    saving.value = false;
  }
}

function openCostApproval(record: QuotePlan) {
  if (!record.costRequest?.id) {
    message.warning("未找到成本单");
    return;
  }
  selectedQuote.value = record;
  selectedCostRequest.value = record.costRequest;
  Object.assign(costApprovalForm, initialCostApprovalForm());
  costApprovalOpen.value = true;
}

async function handleCostApproval() {
  await costApprovalFormRef.value?.validate();
  if (!selectedCostRequest.value) return;
  saving.value = true;
  try {
    await approveQuoteCost(selectedCostRequest.value.id, { ...costApprovalForm });
    costApprovalOpen.value = false;
    message.success(costApprovalForm.decision === "APPROVED" ? "成本已确认，销售可填写报价并提交审批" : "成本已驳回，可重新填写");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "成本审批失败");
  } finally {
    saving.value = false;
  }
}

function quoteBudgetConfirmed(record: QuotePlan) {
  return record.costRequest?.status === "APPROVED" || quoteBudgetRawTotal(record) > 0;
}

function quoteSubmitTip(record: QuotePlan) {
  const margin = quoteMargin(record);
  if (!quoteBudgetConfirmed(record)) {
    return "请先修订报价并确认项目预算，再提交审批";
  }
  if (margin.rate < 15) {
    return `当前毛利率 ${margin.rate.toFixed(1)}%，低于 15%，将作为审批重点提醒。确认提交？`;
  }
  return "当前版本将进入内部审批，确认提交？";
}

function openApproval(record: QuotePlan) {
  selectedQuote.value = record;
  Object.assign(approvalForm, initialApprovalForm());
  approvalOpen.value = true;
}

async function handleApproval() {
  await approvalFormRef.value?.validate();
  if (!selectedQuote.value) return;
  saving.value = true;
  try {
    const result = await processQuoteApproval(selectedQuote.value.id, { ...approvalForm });
    Object.assign(selectedQuote.value, result);
    approvalOpen.value = false;
    message.success(result.status === "APPROVED" ? "内部审批已通过，等待客户确认" : "报价已驳回，请修订后重新提交");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "审批处理失败");
  } finally {
    saving.value = false;
  }
}

function openCustomerResult(record: QuotePlan) {
  selectedQuote.value = record;
  Object.assign(customerResultForm, initialCustomerResultForm());
  customerResultOpen.value = true;
}

function syncCustomerComment() {
  customerResultForm.comment = customerResultForm.decision === "ACCEPTED"
    ? "客户确认接受本版报价，进入合同签署准备。"
    : "客户未接受本版报价，记录原因后继续修订。";
}

async function handleCustomerResult() {
  await customerResultFormRef.value?.validate();
  if (!selectedQuote.value) return;
  saving.value = true;
  try {
    const result = await processQuoteCustomerResult(selectedQuote.value.id, { ...customerResultForm });
    Object.assign(selectedQuote.value, result);
    customerResultOpen.value = false;
    message.success(result.status === "CUSTOMER_ACCEPTED" ? "客户已接受，可继续转合同" : "已登记客户拒绝，可继续修订报价");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "客户结果登记失败");
  } finally {
    saving.value = false;
  }
}

function openConversion(record: QuotePlan) {
  selectedQuote.value = record;
  Object.assign(conversionForm, initialConversionForm(record));
  conversionAttachment.value = null;
  conversionOpen.value = true;
}

function selectConversionAttachment(file: File) {
  conversionAttachment.value = file;
  return false;
}

async function handleConversion() {
  await conversionFormRef.value?.validate();
  if (!selectedQuote.value) return;
  if (!conversionAttachment.value) {
    message.warning("请先上传合同附件");
    return;
  }
  saving.value = true;
  try {
    const result = await convertQuote(selectedQuote.value.id, { ...conversionForm });
    if (!result.contract?.id) throw new Error("合同生成结果缺少合同编号");
    try {
      await uploadAttachment("CONTRACT", result.contract.id, "SIGNED_DOC", conversionAttachment.value);
    } catch (error) {
      conversionOpen.value = false;
      message.warning(`合同已生成，但附件上传失败：${error instanceof Error ? error.message : "请到合同详情页补传"}`);
      await loadData();
      return;
    }
    conversionOpen.value = false;
    message.success(`已生成合同 ${result.contract.code || ""}，合同附件已归档`);
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "转合同失败");
  } finally {
    saving.value = false;
  }
}

async function handleDeleteQuote(record: any) {
  try {
    await deleteQuote(record.id);
    message.success("报价已删除");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "删除失败");
  }
}

async function openRevisions(record: QuotePlan) {
  revisionQuote.value = record;
  revisions.value = [];
  revisionsOpen.value = true;
  revisionLoading.value = true;
  try {
    revisions.value = await listQuoteRevisions(record.id);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "版本记录加载失败");
  } finally {
    revisionLoading.value = false;
  }
}

function canEdit(record: QuotePlan) {
  return auth.can("crm:quote:update") && record.status !== "PENDING_APPROVAL" && record.status !== "CONVERTED";
}

function initialForm() {
  return {
    customerId: undefined as string | undefined,
    opportunityId: undefined as string | undefined,
    code: generateCode("BJ"),
    serviceScope: "",
    inspectCycle: "",
    paymentNodes: "",
    amount: 0,
    taxRate: 13,
    laborBudget: 0,
    materialBudget: 0,
    subcontractBudget: 0,
    travelBudget: 0,
    otherBudget: 0,
    revisionNote: "",
    editorName: auth.user?.displayName || "",
  };
}

function initialApprovalForm() {
  return {
    decision: "APPROVED" as ApprovalDecision,
    comment: "同意本版报价对外发送，后续跟踪客户反馈。",
    approverName: auth.user?.displayName || "",
  };
}

function initialCustomerResultForm() {
  return {
    decision: "ACCEPTED" as QuoteCustomerDecision,
    comment: "客户确认接受本版报价，进入合同签署准备。",
    operatorName: auth.user?.displayName || "",
  };
}

function initialCostRequestForm() {
  return {
    requestedBy: auth.user?.displayName || "",
  };
}

function initialCostSubmitForm(cost?: QuoteCostRequest) {
  return {
    projectManager: cost?.projectManager || auth.user?.displayName || "",
    laborCost: Number(cost?.laborCost || 0),
    laborTaxRate: Number(cost?.laborTaxRate ?? 6),
    materialCost: Number(cost?.materialCost || 0),
    materialTaxRate: Number(cost?.materialTaxRate ?? 13),
    subcontractCost: Number(cost?.subcontractCost || 0),
    subcontractTaxRate: Number(cost?.subcontractTaxRate ?? 6),
    travelCost: Number(cost?.travelCost || 0),
    equipmentCost: Number(cost?.equipmentCost || 0),
    equipmentTaxRate: Number(cost?.equipmentTaxRate ?? 13),
    riskReserve: Number(cost?.riskReserve || 0),
    otherCost: Number(cost?.otherCost || 0),
    suggestedPrice: Number(cost?.suggestedPrice || selectedQuote.value?.amount || 0),
    costRemark: cost?.costRemark || "",
  };
}

function initialCostApprovalForm() {
  return {
    decision: "APPROVED" as ApprovalDecision,
    approverName: auth.user?.displayName || "",
    comment: "同意项目成本测算，作为本次报价毛利核算依据。",
  };
}

function initialConversionForm(record?: QuotePlan) {
  const today = new Date();
  const end = new Date(today);
  end.setFullYear(end.getFullYear() + 1);
  end.setDate(end.getDate() - 1);
  const due = new Date(today);
  due.setDate(due.getDate() + 30);
  const suffix = (record?.code || "").replace(/^BJ-?/, "") || "";
  return {
    contractCode: suffix ? `HT-${suffix}` : "",
    projectName: record?.serviceScope.slice(0, 40) || "",
    contractType: "年度服务合同",
    startDate: formatDate(today),
    endDate: formatDate(end),
    serviceCycle: record?.inspectCycle || "",
    receivables: [
      { rowKey: "r1", receivableCode: "", amount: Math.round(Number(record?.amount || 0) * 50 / 100), ratio: 50, dueDate: formatDate(new Date(Date.now() + 30*86400000)) },
      { rowKey: "r2", receivableCode: "", amount: Number(record?.amount || 0) - Math.round(Number(record?.amount || 0) * 50 / 100), ratio: 50, dueDate: formatDate(new Date(Date.now() + 90*86400000)) },
    ],  };
}



function formatDate(value: Date) {
  return new Date(value.getTime() - value.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
}

function formatDateTime(value?: string) {
  if (!value) return "";
  return new Date(value).toLocaleString("zh-CN", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" });
}

function moneyFormatter({ value }: { value: number }) {
  return formatMoney(value);
}

function formatTaxRate(value?: number) {
  return `${Number(value ?? 13).toFixed(2).replace(/\.?0+$/, "")}%`;
}

function calcNetAmount(amount?: number, taxRate?: number) {
  const rate = Number(taxRate ?? 13);
  const divisor = 1 + rate / 100;
  return divisor > 0 ? Number(amount || 0) / divisor : Number(amount || 0);
}
</script>

<style scoped>
.quote-toolbar {
  display: flex;
  gap: 12px;
  margin: 20px 0 16px;
}

.quote-toolbar .ant-input-affix-wrapper {
  width: min(360px, 100%);
}

.quote-toolbar .ant-select {
  width: 180px;
}

.quote-title-line,
.revision-heading {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quote-title-line .ant-tag {
  margin: 0;
}

.result-text,
.muted-text {
  display: block;
}

.muted-text {
  color: #8c8c8c;
}

.selected-attachment {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  color: #475467;
}

.form-help {
  margin-top: 6px;
  color: #8c8c8c;
  font-size: 12px;
}

.cost-summary-strip {
  display: grid;
  align-items: end;
  grid-template-columns: minmax(180px, 1.1fr) minmax(140px, 0.8fr) minmax(160px, 0.9fr);
  gap: 12px;
  margin-bottom: 14px;
  padding: 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  background: #fafafa;
}

.cost-summary-strip > div:not(.ant-form-item) span {
  display: block;
  color: #667085;
  font-size: 12px;
}

.cost-summary-strip > div:not(.ant-form-item) strong {
  display: block;
  margin-top: 6px;
  color: #172033;
  font-size: 20px;
}

.cost-summary-owner,
.cost-summary-price {
  margin-bottom: 0;
}

.cost-entry-table {
  overflow: hidden;
  margin-bottom: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.cost-entry-head,
.cost-entry-row {
  display: grid;
  align-items: center;
  grid-template-columns: minmax(240px, 1.4fr) minmax(150px, 0.8fr) minmax(130px, 0.65fr);
  gap: 12px;
  padding: 10px 12px;
}

.cost-entry-head {
  color: #667085;
  font-size: 12px;
  font-weight: 600;
  background: #f7f8fa;
}

.cost-entry-row + .cost-entry-row {
  border-top: 1px solid #eef0f3;
}

.cost-entry-row strong,
.cost-entry-row span {
  display: block;
}

.cost-entry-row strong {
  color: #172033;
}

.cost-entry-row span {
  margin-top: 3px;
  color: #667085;
  font-size: 12px;
}

.approval-margin-card {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin: 12px 0 16px;
  padding: 12px;
  border: 1px solid #d6e4ff;
  border-radius: 8px;
  background: #f5f9ff;
}

.approval-margin-card.warning {
  border-color: #ffccc7;
  background: #fff7f6;
}

.approval-margin-card div {
  min-width: 0;
}

.approval-margin-card span {
  display: block;
  color: #667085;
  font-size: 12px;
}

.approval-margin-card strong {
  display: block;
  overflow: hidden;
  margin-top: 5px;
  color: #172033;
  font-size: 17px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.approval-margin-card.warning strong {
  color: #cf1322;
}

.revision-heading {
  justify-content: space-between;
  padding: 0 0 18px;
}

.revision-heading div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.revision-heading span {
  color: #667085;
}

@media (max-width: 720px) {
  .quote-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .quote-toolbar .ant-input-affix-wrapper,
  .quote-toolbar .ant-select {
    width: 100%;
  }

  .cost-summary-strip,
  .cost-entry-head,
  .cost-entry-row {
    grid-template-columns: 1fr;
  }

  .cost-entry-head {
    display: none;
  }
}
</style>

