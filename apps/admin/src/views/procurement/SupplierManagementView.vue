<template>
  <div class="page-stack">
    <a-card>
      <template #title>供应商</template>
      <template #extra>
        <a-space>
          <a-button @click="router.push('/procurement')">返回采购管理</a-button>
          <a-button :loading="loading" @click="loadData"
            ><template #icon><ReloadOutlined /></template>刷新</a-button
          >
        </a-space>
      </template>

      <a-space class="table-toolbar">
        <a-button
          v-if="auth.can('procurement:supplier:create')"
          type="primary"
          @click="openCreate"
        >
          <template #icon><PlusOutlined /></template>新增供应商
        </a-button>
      </a-space>

      <section class="supplier-score-panel">
        <div class="supplier-score-head">
          <div>
            <h3>供应商准入审批与资料完整度</h3>
            <p>新建供应商自动进入待审批，通过准入审批后才可参与询价、签约和采购下单。</p>
          </div>
          <a-tag :color="watchSupplierCount ? 'orange' : 'green'"
            >关注 {{ watchSupplierCount }} 家</a-tag
          >
        </div>
        <div class="supplier-score-grid">
          <button
            v-for="item in supplierScoreCards"
            :key="item.label"
            class="supplier-score-card"
            type="button"
          >
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <em>{{ item.hint }}</em>
          </button>
        </div>
      </section>

      <a-table
        :columns="supplierColumns"
        :data-source="suppliers"
        :loading="loading"
        :pagination="{ pageSize: 10 }"
        :row-key="(r: any) => r.id"
        :scroll="{ x: 1580 }"
        :custom-row="
          (record: Supplier) => ({ onClick: () => openProfile(record) })
        "
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'supplier'">
            <strong>{{ record.code }}</strong>
            <span class="table-subtitle">{{ record.name }}</span>
            <span class="table-subtitle"
              >{{ record.category || "未设置类别" }} ·
              {{ admissionLabel(record.admissionStatus) }}</span
            >
          </template>
          <template v-else-if="column.key === 'license'">
            <span>{{ record.unifiedSocialCreditCode || "-" }}</span>
            <span class="table-subtitle"
              >法人：{{ record.legalRepresentative || "-" }}</span
            >
          </template>
          <template v-else-if="column.key === 'contact'">
            {{ record.contactName || "-" }}<br />
            <span class="table-subtitle">{{ record.phone || "" }}</span>
          </template>
          <template v-else-if="column.key === 'contractedAmount'">
            <strong>{{ formatMoney(record.contractedAmount) }}</strong>
          </template>
          <template v-else-if="column.key === 'payableAmount'">
            <strong>{{ formatMoney(record.payableAmount) }}</strong>
          </template>
          <template v-else-if="column.key === 'paidAmount'">
            <strong>{{ formatMoney(record.paidAmount) }}</strong>
          </template>
          <template v-else-if="column.key === 'outstandingAmount'">
            <strong>{{ formatMoney(record.outstandingAmount) }}</strong>
          </template>
          <template v-else-if="column.key === 'profile'">
            <a-progress
              :percent="profileCompleteness(record)"
              size="small"
              :stroke-color="
                profileCompleteness(record) >= 80
                  ? '#52c41a'
                  : profileCompleteness(record) >= 60
                    ? '#faad14'
                    : '#ff4d4f'
              "
            />
            <span class="table-subtitle">{{
              missingProfileItems(record).length
                ? `缺 ${missingProfileItems(record).slice(0, 2).join("、")}`
                : "资料完整"
            }}</span>
          </template>
          <template v-else-if="column.key === 'validity'">
            <a-space direction="vertical" size="small">
              <a-tag :color="validityColor(record.licenseValidTo)"
                >执照 {{ validityText(record.licenseValidTo) }}</a-tag
              >
              <a-tag :color="validityColor(record.qualificationValidTo)"
                >资质 {{ validityText(record.qualificationValidTo) }}</a-tag
              >
            </a-space>
          </template>
          <template v-else-if="column.key === 'score'">
            <a-progress
              :percent="supplierScore(record)"
              size="small"
              :stroke-color="
                supplierScore(record) >= 80
                  ? '#52c41a'
                  : supplierScore(record) >= 60
                    ? '#faad14'
                    : '#ff4d4f'
              "
            />
            <span class="table-subtitle">{{ supplierGrade(record) }}</span>
          </template>
          <template v-else-if="column.key === 'admission'">
            <a-tag :color="admissionColor(record.admissionStatus)">
              {{ admissionLabel(record.admissionStatus) }}
            </a-tag>
            <span v-if="record.admissionReviewerName" class="table-subtitle">
              {{ record.admissionReviewerName }} ·
              {{ formatDateTime(record.admissionReviewedAt) }}
            </span>
            <span
              v-else-if="record.admissionStatus === 'PENDING'"
              class="table-subtitle"
            >
              等待审批
            </span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag
              :color="
                record.riskStatus === 'NORMAL'
                  ? 'green'
                  : record.riskStatus === 'WATCHLIST'
                    ? 'orange'
                    : 'red'
              "
            >
              {{
                ({ NORMAL: "正常", WATCHLIST: "关注", BLOCKED: "冻结" } as any)[
                  record.riskStatus
                ] || record.riskStatus
              }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space @click.stop>
              <a-button type="link" size="small" @click="openProfile(record)"
                >资料</a-button
              >
              <a-button
                type="link"
                size="small"
                @click="router.push(`/procurement/suppliers/${record.id}`)"
                >全景详情</a-button
              >
              <a-button type="link" size="small" @click="openDocuments(record)"
                >档案</a-button
              >
              <a-button
                v-if="
                  record.admissionStatus === 'PENDING' &&
                  auth.can('procurement:request:approve')
                "
                type="link"
                size="small"
                @click="openAdmissionReview(record)"
                >准入审批</a-button
              >
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="createOpen"
      title="新增供应商"
      width="980px"
      :confirm-loading="saving"
      @ok="handleCreate"
    >
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-divider>基本信息</a-divider>
        <a-row :gutter="16">
          <a-col :xs="24" :md="8"
            ><a-form-item label="供应商编码" name="code"
              ><a-input v-model:value="form.code" /></a-form-item
          ></a-col>
          <a-col :xs="24" :md="10"
            ><a-form-item label="供应商名称" name="name"
              ><a-input v-model:value="form.name" /></a-form-item
          ></a-col>
          <a-col :xs="24" :md="6"
            ><a-form-item label="供应商类别"
              ><a-input
                v-model:value="form.category"
                placeholder="材料/外包/设备/服务" /></a-form-item
          ></a-col>
          <a-col :xs="24" :md="8"
            ><a-form-item label="联系人"
              ><a-input v-model:value="form.contactName" /></a-form-item
          ></a-col>
          <a-col :xs="24" :md="8"
            ><a-form-item label="联系电话"
              ><a-input v-model:value="form.phone" /></a-form-item
          ></a-col>
          <a-col :xs="24" :md="8"
            ><a-form-item label="结算条款"
              ><a-input
                v-model:value="form.settlementTerms"
                placeholder="月结30天/预付/验收后付款" /></a-form-item
          ></a-col>
        </a-row>

        <a-divider>工商与资质</a-divider>
        <a-row :gutter="16">
          <a-col :xs="24" :md="8"
            ><a-form-item label="统一社会信用代码"
              ><a-input
                v-model:value="form.unifiedSocialCreditCode" /></a-form-item
          ></a-col>
          <a-col :xs="24" :md="8"
            ><a-form-item label="法定代表人"
              ><a-input v-model:value="form.legalRepresentative" /></a-form-item
          ></a-col>
          <a-col :xs="24" :md="8"
            ><a-form-item label="注册资本"
              ><a-input v-model:value="form.registeredCapital" /></a-form-item
          ></a-col>
          <a-col :xs="24" :md="8"
            ><a-form-item label="营业执照有效期"
              ><a-input
                v-model:value="form.licenseValidTo"
                type="date" /></a-form-item
          ></a-col>
          <a-col :xs="24" :md="8"
            ><a-form-item label="资质有效期"
              ><a-input
                v-model:value="form.qualificationValidTo"
                type="date" /></a-form-item
          ></a-col>
          <a-col :span="24">
            <a-alert
              type="info"
              show-icon
              message="保存后自动提交准入审批"
              description="审批通过前，该供应商不会出现在询价、合同和采购订单的可用供应商列表中。"
            />
          </a-col>
          <a-col :span="24"
            ><a-form-item label="注册地址"
              ><a-input v-model:value="form.registeredAddress" /></a-form-item
          ></a-col>
          <a-col :span="24"
            ><a-form-item label="经营范围"
              ><a-textarea
                v-model:value="form.businessScope"
                :rows="2" /></a-form-item
          ></a-col>
        </a-row>

        <a-divider>税务与银行</a-divider>
        <a-row :gutter="16">
          <a-col :xs="24" :md="8"
            ><a-form-item label="纳税人类型"
              ><a-input
                v-model:value="form.taxpayerType"
                placeholder="一般纳税人/小规模纳税人" /></a-form-item
          ></a-col>
          <a-col :xs="24" :md="8"
            ><a-form-item label="开户银行"
              ><a-input v-model:value="form.bankName" /></a-form-item
          ></a-col>
          <a-col :xs="24" :md="8"
            ><a-form-item label="银行账号"
              ><a-input v-model:value="form.bankAccount" /></a-form-item
          ></a-col>
          <a-col :span="24"
            ><a-form-item label="备注"
              ><a-textarea v-model:value="form.remark" :rows="2" /></a-form-item
          ></a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-drawer
      v-model:open="profileOpen"
      width="min(1080px, calc(100vw - 24px))"
      title="供应商资料"
    >
      <template #extra>
        <a-space v-if="selectedSupplier">
          <template v-if="supplierEditing">
            <a-button @click="cancelSupplierEdit">取消</a-button>
            <a-button type="primary" :loading="saving" @click="saveSupplier"
              >保存</a-button
            >
          </template>
          <template v-else>
            <a-button @click="openDocuments(selectedSupplier)"
              >资料档案</a-button
            >
            <a-button
              v-if="
                selectedSupplier.admissionStatus === 'PENDING' &&
                auth.can('procurement:request:approve')
              "
              @click="openAdmissionReview(selectedSupplier)"
              >准入审批</a-button
            >
            <a-button
              type="primary"
              @click="startSupplierEdit(selectedSupplier)"
              >编辑资料</a-button
            >
          </template>
        </a-space>
      </template>
      <template v-if="selectedSupplier">
        <div class="profile-heading">
          <div>
            <strong>{{ selectedSupplier.name }}</strong>
            <span
              >{{ selectedSupplier.code }} ·
              {{ selectedSupplier.category || "未设置类别" }}</span
            >
          </div>
          <a-tag
            :color="
              profileCompleteness(selectedSupplier) >= 80 ? 'green' : 'orange'
            "
            >完整度 {{ profileCompleteness(selectedSupplier) }}%</a-tag
          >
        </div>
        <template v-if="!supplierEditing">
          <a-descriptions bordered :column="2" size="small">
            <a-descriptions-item label="供应商编码">{{
              selectedSupplier.code
            }}</a-descriptions-item>
            <a-descriptions-item label="准入状态">
              <a-tag
                :color="admissionColor(selectedSupplier.admissionStatus)"
                >{{
                  admissionLabel(selectedSupplier.admissionStatus)
                }}</a-tag
              >
            </a-descriptions-item>
            <a-descriptions-item label="提交审批时间">{{
              formatDateTime(selectedSupplier.admissionSubmittedAt) || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="审批人">{{
              selectedSupplier.admissionReviewerName || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="审批时间">{{
              formatDateTime(selectedSupplier.admissionReviewedAt) || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="审批意见" :span="2">{{
              selectedSupplier.admissionReviewComment || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="统一社会信用代码">{{
              selectedSupplier.unifiedSocialCreditCode || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="法定代表人">{{
              selectedSupplier.legalRepresentative || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="注册资本">{{
              selectedSupplier.registeredCapital || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="供应商类别">{{
              selectedSupplier.category || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="联系人">{{
              selectedSupplier.contactName || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="联系电话">{{
              selectedSupplier.phone || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="营业执照有效期">{{
              selectedSupplier.licenseValidTo || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="资质有效期">{{
              selectedSupplier.qualificationValidTo || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="纳税人类型">{{
              selectedSupplier.taxpayerType || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="开户银行">{{
              selectedSupplier.bankName || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="银行账号">{{
              selectedSupplier.bankAccount || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="结算条款">{{
              selectedSupplier.settlementTerms || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="累计签约金额">{{
              formatMoney(selectedSupplier.contractedAmount)
            }}</a-descriptions-item>
            <a-descriptions-item label="累计应付金额">{{
              formatMoney(selectedSupplier.payableAmount)
            }}</a-descriptions-item>
            <a-descriptions-item label="累计已付金额">{{
              formatMoney(selectedSupplier.paidAmount)
            }}</a-descriptions-item>
            <a-descriptions-item label="当前待付金额">{{
              formatMoney(selectedSupplier.outstandingAmount)
            }}</a-descriptions-item>
            <a-descriptions-item label="注册地址" :span="2">{{
              selectedSupplier.registeredAddress || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="经营范围" :span="2">{{
              selectedSupplier.businessScope || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="备注" :span="2">{{
              selectedSupplier.remark || "-"
            }}</a-descriptions-item>
          </a-descriptions>
          <a-alert
            v-if="missingProfileItems(selectedSupplier).length"
            class="profile-alert"
            type="warning"
            show-icon
            :message="`资料待完善：${missingProfileItems(selectedSupplier).join('、')}`"
          />
          <a-divider>采购订单</a-divider>
          <a-table
            size="small"
            :columns="supplierOrderColumns"
            :data-source="supplierOrders"
            :loading="ordersLoading"
            :pagination="{ pageSize: 8 }"
            row-key="id"
            :scroll="{ x: 980 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'order'">
                <strong>{{ record.code || "-" }}</strong>
                <span class="table-subtitle">{{ record.partName || "-" }}</span>
              </template>
              <template v-else-if="column.key === 'target'">
                {{ record.costTargetName || "-" }}
                <span class="table-subtitle">{{
                  record.costTargetCode || ""
                }}</span>
              </template>
              <template v-else-if="column.key === 'amount'">
                <strong>{{ formatMoney(record.orderAmount) }}</strong>
                <span class="table-subtitle"
                  >单价 {{ formatMoney(record.unitPrice) }}</span
                >
              </template>
              <template v-else-if="column.key === 'receipt'">
                {{ Number(record.receivedQty || 0) }} /
                {{ Number(record.orderedQty || 0) }}
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="orderStatusColor(record.status)">{{
                  orderStatusLabel(record.status)
                }}</a-tag>
              </template>
            </template>
            <template #emptyText>该供应商暂无采购订单</template>
          </a-table>
        </template>
        <a-form
          v-else
          ref="profileFormRef"
          :model="form"
          :rules="rules"
          layout="vertical"
        >
          <a-tabs>
            <a-tab-pane key="base" tab="基本资料">
              <a-row :gutter="16">
                <a-col :xs="24" :md="8"
                  ><a-form-item label="供应商编码" name="code"
                    ><a-input v-model:value="form.code" disabled /></a-form-item
                ></a-col>
                <a-col :xs="24" :md="10"
                  ><a-form-item label="供应商名称" name="name"
                    ><a-input v-model:value="form.name" /></a-form-item
                ></a-col>
                <a-col :xs="24" :md="6"
                  ><a-form-item label="供应商类别"
                    ><a-input v-model:value="form.category" /></a-form-item
                ></a-col>
                <a-col :xs="24" :md="8"
                  ><a-form-item label="联系人"
                    ><a-input v-model:value="form.contactName" /></a-form-item
                ></a-col>
                <a-col :xs="24" :md="8"
                  ><a-form-item label="联系电话"
                    ><a-input v-model:value="form.phone" /></a-form-item
                ></a-col>
                <a-col :xs="24" :md="8"
                  ><a-form-item label="风险状态"
                    ><a-select
                      v-model:value="form.riskStatus"
                      :options="riskOptions" /></a-form-item
                ></a-col>
              </a-row>
            </a-tab-pane>
            <a-tab-pane key="license" tab="工商与资质">
              <a-row :gutter="16">
                <a-col :xs="24" :md="8"
                  ><a-form-item label="统一社会信用代码"
                    ><a-input
                      v-model:value="
                        form.unifiedSocialCreditCode
                      " /></a-form-item
                ></a-col>
                <a-col :xs="24" :md="8"
                  ><a-form-item label="法定代表人"
                    ><a-input
                      v-model:value="form.legalRepresentative" /></a-form-item
                ></a-col>
                <a-col :xs="24" :md="8"
                  ><a-form-item label="注册资本"
                    ><a-input
                      v-model:value="form.registeredCapital" /></a-form-item
                ></a-col>
                <a-col :xs="24" :md="8"
                  ><a-form-item label="营业执照有效期"
                    ><a-input
                      v-model:value="form.licenseValidTo"
                      type="date" /></a-form-item
                ></a-col>
                <a-col :xs="24" :md="8"
                  ><a-form-item label="资质有效期"
                    ><a-input
                      v-model:value="form.qualificationValidTo"
                      type="date" /></a-form-item
                ></a-col>
                <a-col :xs="24" :md="8">
                  <a-form-item label="准入状态">
                    <a-tag
                      :color="
                        admissionColor(selectedSupplier.admissionStatus)
                      "
                    >
                      {{ admissionLabel(selectedSupplier.admissionStatus) }}
                    </a-tag>
                    <span class="table-subtitle">准入状态只能通过审批变更</span>
                  </a-form-item>
                </a-col>
                <a-col :span="24"
                  ><a-form-item label="注册地址"
                    ><a-input
                      v-model:value="form.registeredAddress" /></a-form-item
                ></a-col>
                <a-col :span="24"
                  ><a-form-item label="经营范围"
                    ><a-textarea
                      v-model:value="form.businessScope"
                      :rows="3" /></a-form-item
                ></a-col>
              </a-row>
            </a-tab-pane>
            <a-tab-pane key="finance" tab="税务与银行">
              <a-row :gutter="16">
                <a-col :xs="24" :md="8"
                  ><a-form-item label="纳税人类型"
                    ><a-input v-model:value="form.taxpayerType" /></a-form-item
                ></a-col>
                <a-col :xs="24" :md="8"
                  ><a-form-item label="开户银行"
                    ><a-input v-model:value="form.bankName" /></a-form-item
                ></a-col>
                <a-col :xs="24" :md="8"
                  ><a-form-item label="银行账号"
                    ><a-input v-model:value="form.bankAccount" /></a-form-item
                ></a-col>
                <a-col :xs="24" :md="8"
                  ><a-form-item label="结算条款"
                    ><a-input
                      v-model:value="form.settlementTerms" /></a-form-item
                ></a-col>
                <a-col :span="24"
                  ><a-form-item label="备注"
                    ><a-textarea
                      v-model:value="form.remark"
                      :rows="3" /></a-form-item
                ></a-col>
              </a-row>
            </a-tab-pane>
          </a-tabs>
        </a-form>
      </template>
    </a-drawer>

    <a-modal
      v-model:open="admissionReviewOpen"
      title="供应商准入审批"
      :confirm-loading="reviewing"
      :ok-button-props="{
        disabled:
          reviewForm.decision === 'APPROVED' &&
          !!reviewTarget &&
          missingProfileItems(reviewTarget).length > 0,
      }"
      @ok="handleAdmissionReview"
    >
      <template v-if="reviewTarget">
        <a-descriptions bordered :column="1" size="small">
          <a-descriptions-item label="供应商">
            {{ reviewTarget.name }}（{{ reviewTarget.code }}）
          </a-descriptions-item>
          <a-descriptions-item label="资料完整度">
            {{ profileCompleteness(reviewTarget) }}%
          </a-descriptions-item>
          <a-descriptions-item label="待补资料">
            {{
              missingProfileItems(reviewTarget).length
                ? missingProfileItems(reviewTarget).join("、")
                : "无"
            }}
          </a-descriptions-item>
        </a-descriptions>
        <a-alert
          v-if="missingProfileItems(reviewTarget).length"
          class="profile-alert"
          type="warning"
          show-icon
          message="资料未满足准入条件，暂不能审批通过；可以驳回并要求补充。"
        />
        <a-form layout="vertical" class="profile-alert">
          <a-form-item label="审批结论">
            <a-radio-group v-model:value="reviewForm.decision">
              <a-radio value="APPROVED">审批通过</a-radio>
              <a-radio value="REJECTED">驳回补充</a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item
            :label="
              reviewForm.decision === 'REJECTED' ? '驳回原因' : '审批意见'
            "
            required
          >
            <a-textarea
              v-model:value="reviewForm.comment"
              :rows="3"
              :placeholder="
                reviewForm.decision === 'REJECTED'
                  ? '请说明需要补充或修正的资料'
                  : '请输入审批意见'
              "
            />
          </a-form-item>
        </a-form>
      </template>
    </a-modal>

    <a-drawer v-model:open="documentOpen" width="820" title="供应商档案">
      <template v-if="selectedSupplier">
        <div class="profile-heading">
          <div>
            <strong>{{ selectedSupplier.name }}</strong>
            <span>营业执照、资质证书、税务银行、协议合同等资料归档</span>
          </div>
          <a-upload
            :show-upload-list="false"
            :before-upload="uploadSupplierDocument"
            accept=".jpg,.jpeg,.png,.webp,.pdf,.doc,.docx,.xls,.xlsx"
          >
            <a-button type="primary"
              ><template #icon><UploadOutlined /></template>上传资料</a-button
            >
          </a-upload>
        </div>
        <div class="doc-type-grid">
          <div
            v-for="item in requiredDocTypes"
            :key="item.key"
            class="doc-type-card"
            :class="{ done: documentTypeDone(item.key) }"
          >
            <span>{{ item.label }}</span>
            <strong>{{
              documentTypeDone(item.key) ? "已归档" : "待上传"
            }}</strong>
          </div>
        </div>
        <a-table
          size="small"
          :columns="documentColumns"
          :data-source="supplierDocuments"
          :loading="documentsLoading"
          :pagination="false"
          row-key="id"
          style="margin-top: 14px"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'file'">
              <strong>{{ record.fileName }}</strong>
              <span class="table-subtitle"
                >{{ formatFileSize(record.sizeBytes) }} ·
                {{ formatDateTime(record.createdAt) }}</span
              >
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space>
                <a-button
                  type="link"
                  size="small"
                  @click="previewDocument(record)"
                  >预览</a-button
                >
                <a-button
                  type="link"
                  size="small"
                  @click="downloadDocument(record)"
                  >下载</a-button
                >
              </a-space>
            </template>
          </template>
          <template #emptyText
            >暂无供应商资料，请上传营业执照、资质证书、开户许可证等文件</template
          >
        </a-table>
      </template>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { message } from "ant-design-vue";
import PlusOutlined from "@ant-design/icons-vue/PlusOutlined";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import UploadOutlined from "@ant-design/icons-vue/UploadOutlined";
import {
  listSuppliers,
  listPurchaseOrders,
  createSupplier,
  reviewSupplierAdmission,
  updateSupplier,
  type CreateSupplierPayload,
  type Supplier,
  type PurchaseOrder,
} from "@/api/procurement";
import {
  downloadDocument,
  listDocumentsByBiz,
  previewDocument,
  uploadDocument,
  type DocumentRecord,
} from "@/api/office";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
const router = useRouter();
const loading = ref(false);
const saving = ref(false);
const reviewing = ref(false);
const documentsLoading = ref(false);
const ordersLoading = ref(false);
const suppliers = ref<Supplier[]>([]);
const supplierOrders = ref<PurchaseOrder[]>([]);
const supplierDocuments = ref<DocumentRecord[]>([]);
const selectedSupplier = ref<Supplier | null>(null);
const createOpen = ref(false);
const profileOpen = ref(false);
const documentOpen = ref(false);
const admissionReviewOpen = ref(false);
const reviewTarget = ref<Supplier | null>(null);
const reviewForm = reactive<{
  decision: "APPROVED" | "REJECTED";
  comment: string;
}>({ decision: "APPROVED", comment: "供应商资料核验通过，同意准入" });
const formRef = ref();
const profileFormRef = ref();
const supplierEditing = ref(false);
const form = reactive<CreateSupplierPayload>(emptySupplierForm());

const rules = { code: [{ required: true }], name: [{ required: true }] };
const riskOptions = [
  { label: "正常", value: "NORMAL" },
  { label: "关注", value: "WATCHLIST" },
  { label: "冻结", value: "BLOCKED" },
];
const requiredDocTypes = [
  { key: "营业执照", label: "营业执照" },
  { key: "资质证书", label: "资质证书" },
  { key: "开户", label: "开户资料" },
  { key: "税务", label: "税务资料" },
  { key: "协议", label: "协议合同" },
  { key: "廉洁", label: "廉洁承诺" },
];
const supplierColumns = [
  { title: "供应商", key: "supplier", width: 260 },
  { title: "工商信息", key: "license", width: 240 },
  { title: "联系人", key: "contact", width: 170 },
  { title: "签约金额", key: "contractedAmount", width: 150 },
  { title: "应付金额", key: "payableAmount", width: 150 },
  { title: "已付金额", key: "paidAmount", width: 150 },
  { title: "待付金额", key: "outstandingAmount", width: 150 },
  { title: "资料完整度", key: "profile", width: 180 },
  { title: "有效期", key: "validity", width: 170 },
  { title: "评分", key: "score", width: 160 },
  { title: "准入审批", key: "admission", width: 170 },
  { title: "风险", key: "status", width: 100 },
  { title: "操作", key: "action", width: 210, fixed: "right" },
];
function formatMoney(value?: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    minimumFractionDigits: 2,
  }).format(Number(value || 0));
}
const documentColumns = [
  { title: "文件", key: "file" },
  { title: "业务类型", dataIndex: "bizType", width: 120 },
  { title: "操作", key: "action", width: 130 },
];
const supplierOrderColumns = [
  { title: "订单 / 物料", key: "order", width: 210 },
  { title: "成本归属", key: "target", width: 210 },
  { title: "订单金额", key: "amount", width: 170 },
  { title: "收货数量", key: "receipt", width: 110 },
  { title: "预计交付", dataIndex: "expectedDeliveryDate", width: 120 },
  { title: "状态", key: "status", width: 100 },
];
const watchSupplierCount = computed(
  () =>
    suppliers.value.filter(
      (item) =>
        item.riskStatus !== "NORMAL" ||
        supplierScore(item) < 70 ||
        missingProfileItems(item).length > 0 ||
        isExpiring(item.licenseValidTo) ||
        isExpiring(item.qualificationValidTo),
    ).length,
);
const supplierScoreCards = computed(() => [
  {
    label: "供应商总数",
    value: `${suppliers.value.length} 家`,
    hint: "已建档供应商",
  },
  {
    label: "待准入审批",
    value: `${suppliers.value.filter((item) => item.admissionStatus === "PENDING").length} 家`,
    hint: "审批通过前不可采购",
  },
  {
    label: "到期预警",
    value: `${suppliers.value.filter((item) => isExpiring(item.licenseValidTo) || isExpiring(item.qualificationValidTo)).length} 家`,
    hint: "执照或资质90天内到期",
  },
  {
    label: "冻结供应商",
    value: `${suppliers.value.filter((item) => item.riskStatus === "BLOCKED").length} 家`,
    hint: "禁止采购下单",
  },
]);

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    const result = await listSuppliers(0, 999);
    suppliers.value = result.content;
  } catch (error) {
    message.error(error instanceof Error ? error.message : "加载失败");
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  Object.assign(form, emptySupplierForm(), { code: generateCode("GYS") });
  createOpen.value = true;
}

async function handleCreate() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    const created = await createSupplier({ ...form });
    createOpen.value = false;
    message.success("供应商已创建并提交准入审批，审批通过后方可使用");
    await loadData();
    openDocuments(created);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "创建失败");
  } finally {
    saving.value = false;
  }
}

async function openProfile(record: Supplier) {
  selectedSupplier.value = record;
  supplierEditing.value = false;
  Object.assign(form, supplierToForm(record));
  profileOpen.value = true;
  await loadSupplierOrders(record.id);
}

async function loadSupplierOrders(supplierId: string) {
  ordersLoading.value = true;
  supplierOrders.value = [];
  try {
    const result = await listPurchaseOrders({ page: 0, size: 999 });
    supplierOrders.value = result.content.filter(
      (order) => order.supplierId === supplierId,
    );
  } catch (error) {
    message.error(error instanceof Error ? error.message : "采购订单加载失败");
  } finally {
    ordersLoading.value = false;
  }
}

function orderStatusLabel(status: PurchaseOrder["status"]) {
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
    )[status] || status
  );
}

function orderStatusColor(status: PurchaseOrder["status"]) {
  return (
    (
      {
        DRAFT: "default",
        ORDERED: "blue",
        PARTIAL_RECEIVED: "orange",
        RECEIVED: "green",
        CLOSED: "cyan",
        CANCELLED: "red",
      } as Record<string, string>
    )[status] || "default"
  );
}

function startSupplierEdit(record: Supplier) {
  Object.assign(form, supplierToForm(record));
  supplierEditing.value = true;
}

function cancelSupplierEdit() {
  supplierEditing.value = false;
  if (selectedSupplier.value) {
    Object.assign(form, supplierToForm(selectedSupplier.value));
  }
}

async function saveSupplier() {
  if (!selectedSupplier.value) return;
  await profileFormRef.value?.validate();
  saving.value = true;
  try {
    const wasRejected = selectedSupplier.value.admissionStatus === "REJECTED";
    const saved = await updateSupplier(selectedSupplier.value.id, { ...form });
    selectedSupplier.value = saved;
    const index = suppliers.value.findIndex((item) => item.id === saved.id);
    if (index >= 0) {
      suppliers.value[index] = saved;
    }
    supplierEditing.value = false;
    message.success(
      saved.admissionStatus === "PENDING" && wasRejected
        ? "供应商资料已更新并重新提交准入审批"
        : "供应商资料已更新",
    );
  } catch (error) {
    message.error(error instanceof Error ? error.message : "保存失败");
  } finally {
    saving.value = false;
  }
}

function openAdmissionReview(record: Supplier) {
  reviewTarget.value = record;
  Object.assign(reviewForm, {
    decision: missingProfileItems(record).length ? "REJECTED" : "APPROVED",
    comment: missingProfileItems(record).length
      ? `请补充：${missingProfileItems(record).join("、")}`
      : "供应商资料核验通过，同意准入",
  });
  admissionReviewOpen.value = true;
}

async function handleAdmissionReview() {
  if (!reviewTarget.value) return;
  if (!reviewForm.comment.trim()) {
    message.warning("请填写审批意见");
    return;
  }
  if (
    reviewForm.decision === "APPROVED" &&
    missingProfileItems(reviewTarget.value).length
  ) {
    message.warning("供应商资料不完整，不能审批通过");
    return;
  }
  reviewing.value = true;
  try {
    const reviewed = await reviewSupplierAdmission(reviewTarget.value.id, {
      ...reviewForm,
    });
    admissionReviewOpen.value = false;
    selectedSupplier.value =
      selectedSupplier.value?.id === reviewed.id
        ? reviewed
        : selectedSupplier.value;
    message.success(
      reviewForm.decision === "APPROVED"
        ? "供应商准入审批已通过，现在可以参与采购"
        : "供应商准入已驳回，修改资料后会重新提交审批",
    );
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "准入审批失败");
  } finally {
    reviewing.value = false;
  }
}

async function openDocuments(record: Supplier) {
  selectedSupplier.value = record;
  documentOpen.value = true;
  await loadSupplierDocuments();
}

async function loadSupplierDocuments() {
  if (!selectedSupplier.value) return;
  documentsLoading.value = true;
  try {
    supplierDocuments.value = await listDocumentsByBiz(
      "SUPPLIER",
      selectedSupplier.value.id,
    );
  } catch (error) {
    message.error(
      error instanceof Error ? error.message : "供应商档案加载失败",
    );
  } finally {
    documentsLoading.value = false;
  }
}

async function uploadSupplierDocument(file: File) {
  if (!selectedSupplier.value) return false;
  try {
    await uploadDocument({
      bizType: "SUPPLIER",
      bizId: selectedSupplier.value.id,
      file,
    });
    message.success("供应商资料已上传");
    await loadSupplierDocuments();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "资料上传失败");
  }
  return false;
}

function emptySupplierForm(): CreateSupplierPayload {
  return {
    code: "",
    name: "",
    category: "",
    contactName: "",
    phone: "",
    settlementTerms: "",
    legalRepresentative: "",
    unifiedSocialCreditCode: "",
    registeredCapital: "",
    registeredAddress: "",
    businessScope: "",
    licenseValidTo: "",
    qualificationValidTo: "",
    taxpayerType: "",
    bankName: "",
    bankAccount: "",
    admissionStatus: "PENDING",
    remark: "",
    riskStatus: "NORMAL",
  };
}

function supplierToForm(record: Supplier): CreateSupplierPayload {
  return {
    code: record.code || "",
    name: record.name,
    category: record.category || "",
    contactName: record.contactName || "",
    phone: record.phone || "",
    settlementTerms: record.settlementTerms || "",
    legalRepresentative: record.legalRepresentative || "",
    unifiedSocialCreditCode: record.unifiedSocialCreditCode || "",
    registeredCapital: record.registeredCapital || "",
    registeredAddress: record.registeredAddress || "",
    businessScope: record.businessScope || "",
    licenseValidTo: record.licenseValidTo || "",
    qualificationValidTo: record.qualificationValidTo || "",
    taxpayerType: record.taxpayerType || "",
    bankName: record.bankName || "",
    bankAccount: record.bankAccount || "",
    admissionStatus: record.admissionStatus || "PENDING",
    remark: record.remark || "",
    riskStatus: record.riskStatus || "NORMAL",
  };
}

function profileCompleteness(record: Supplier) {
  const required = [
    "name",
    "contactName",
    "phone",
    "settlementTerms",
    "legalRepresentative",
    "unifiedSocialCreditCode",
    "registeredAddress",
    "licenseValidTo",
    "taxpayerType",
    "bankName",
    "bankAccount",
  ];
  const done = required.filter((key) => Boolean((record as any)[key])).length;
  return Math.round((done / required.length) * 100);
}

function missingProfileItems(record: Supplier) {
  const labels: Array<[keyof Supplier, string]> = [
    ["category", "供应商类别"],
    ["contactName", "联系人"],
    ["phone", "联系电话"],
    ["unifiedSocialCreditCode", "信用代码"],
    ["legalRepresentative", "法人"],
    ["registeredAddress", "注册地址"],
    ["licenseValidTo", "执照有效期"],
    ["taxpayerType", "纳税人类型"],
    ["bankName", "开户银行"],
    ["bankAccount", "银行账号"],
    ["settlementTerms", "结算条款"],
  ];
  return labels.filter(([key]) => !record[key]).map(([, label]) => label);
}

function supplierScore(record: Supplier) {
  let score = 100;
  if (record.riskStatus === "WATCHLIST") score -= 20;
  if (record.riskStatus === "BLOCKED") score -= 55;
  score -= Math.min(35, missingProfileItems(record).length * 5);
  if (isExpiring(record.licenseValidTo)) score -= 12;
  if (isExpiring(record.qualificationValidTo)) score -= 12;
  if (record.admissionStatus === "INCOMPLETE") score -= 10;
  if (record.admissionStatus === "SUSPENDED") score -= 25;
  return Math.max(0, score);
}

function supplierGrade(record: Supplier) {
  const score = supplierScore(record);
  return score >= 80 ? "优选" : score >= 60 ? "观察" : "高风险";
}

function validityText(value?: string) {
  if (!value) return "未录入";
  const days = daysLeft(value);
  if (days < 0) return `已过期 ${Math.abs(days)} 天`;
  if (days <= 90) return `${days} 天后到期`;
  return value;
}

function validityColor(value?: string) {
  if (!value) return "default";
  const days = daysLeft(value);
  if (days < 0) return "red";
  if (days <= 90) return "orange";
  return "green";
}

function isExpiring(value?: string) {
  if (!value) return false;
  const days = daysLeft(value);
  return days <= 90;
}

function daysLeft(value: string) {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return Math.ceil((new Date(value).getTime() - today.getTime()) / 86400000);
}

function admissionLabel(value?: string) {
  return (
    (
      {
        PENDING: "待准入",
        APPROVED: "已准入",
        REJECTED: "已驳回",
        INCOMPLETE: "资料待补",
        SUSPENDED: "暂停合作",
      } as Record<string, string>
    )[value || ""] || "未设置准入"
  );
}

function admissionColor(value?: string) {
  return (
    (
      {
        PENDING: "orange",
        APPROVED: "green",
        REJECTED: "red",
        INCOMPLETE: "gold",
        SUSPENDED: "red",
      } as Record<string, string>
    )[value || ""] || "default"
  );
}

function documentTypeDone(keyword: string) {
  return supplierDocuments.value.some((item) =>
    item.fileName.includes(keyword),
  );
}

function generateCode(prefix: string) {
  const d = new Date();
  return `${prefix}-${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, "0")}${String(d.getDate()).padStart(2, "0")}-${String(d.getHours()).padStart(2, "0")}${String(d.getMinutes()).padStart(2, "0")}`;
}

function formatFileSize(value: number) {
  if (value >= 1024 * 1024) return `${(value / 1024 / 1024).toFixed(1)} MB`;
  return `${(value / 1024).toFixed(1)} KB`;
}

function formatDateTime(value?: string) {
  if (!value) return "";
  return new Date(value).toLocaleString("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}
</script>

<style scoped>
.supplier-score-panel {
  margin-bottom: 14px;
  padding: 14px;
  border: 1px solid #e5e7eb;
  background: #fbfcfe;
}
.supplier-score-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 12px;
}
.supplier-score-head h3 {
  margin: 0;
  color: #111827;
  font-size: 15px;
  font-weight: 600;
}
.supplier-score-head p {
  margin: 4px 0 0;
  color: #6b7280;
  font-size: 12px;
}
.supplier-score-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 10px;
}
.supplier-score-card {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 12px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fff;
  text-align: left;
}
.supplier-score-card span {
  color: #667085;
  font-size: 12px;
}
.supplier-score-card strong {
  color: #101828;
  font-size: 20px;
}
.supplier-score-card em {
  overflow: hidden;
  color: #98a2b3;
  font-size: 12px;
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.profile-heading {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 16px;
}
.profile-heading div {
  display: grid;
  gap: 4px;
}
.profile-heading strong {
  color: #172033;
  font-size: 16px;
}
.profile-heading span {
  color: #667085;
  font-size: 12px;
}
.profile-alert {
  margin-top: 14px;
}
.doc-type-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}
.doc-type-card {
  padding: 10px 12px;
  border: 1px solid #ffe7ba;
  border-radius: 8px;
  background: #fffaf0;
}
.doc-type-card.done {
  border-color: #b7eb8f;
  background: #f6ffed;
}
.doc-type-card span {
  display: block;
  color: #667085;
  font-size: 12px;
}
.doc-type-card strong {
  display: block;
  margin-top: 4px;
  color: #172033;
  font-size: 14px;
}
@media (max-width: 900px) {
  .supplier-score-head,
  .profile-heading {
    flex-direction: column;
  }
  .supplier-score-grid,
  .doc-type-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
