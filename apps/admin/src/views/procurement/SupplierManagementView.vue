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
        <a-button
          v-if="auth.can('procurement:view')"
          @click="openPortalAccounts"
        >
          供应商门户账号
          <a-badge
            v-if="pendingPortalAccounts"
            :count="pendingPortalAccounts"
          />
        </a-button>
        <a-button
          v-if="auth.can('procurement:supplier:create')"
          @click="openSupplierCategoryDictionary"
        >
          <template #icon><TagsOutlined /></template>
          供应商类别字典
        </a-button>
        <a-button
          v-if="auth.can('procurement:view')"
          @click="openChangeRequests"
        >
          供应商变更审批
          <a-badge v-if="pendingChangeCount" :count="pendingChangeCount" />
        </a-button>
        <a-button :loading="exporting" @click="handleExport">
          <template #icon><DownloadOutlined /></template>导出 Excel
        </a-button>
      </a-space>

      <section class="supplier-score-panel">
        <div class="supplier-score-head">
          <div>
            <h3>供应商准入审批与资料完整度</h3>
            <p>
              新建供应商自动进入待审批，通过准入审批后才可参与询价、签约和采购下单。
            </p>
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
            <a-space wrap size="small" @click.stop>
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
                  auth.can('procurement:supplier:admission')
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
            ><a-form-item label="供应商类别" name="category"
              ><a-select
                v-model:value="form.category"
                :options="supplierCategoryOptions"
                placeholder="请选择供应商类别"
                allow-clear /></a-form-item
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
            ><a-form-item label="负责采购"
              ><a-input
                v-model:value="form.purchaserName"
                placeholder="内部采购负责人" /></a-form-item
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
                !portalAccountForSupplier(selectedSupplier.id) &&
                auth.can('procurement:portal-account:approve')
              "
              @click="openPortalAccount(selectedSupplier)"
              >开通门户账号</a-button
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
                >{{ admissionLabel(selectedSupplier.admissionStatus) }}</a-tag
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
            <a-descriptions-item label="累计签约金额（含税，元）">{{
              formatMoney(selectedSupplier.contractedAmount)
            }}</a-descriptions-item>
            <a-descriptions-item label="累计应付金额（含税，元）">{{
              formatMoney(selectedSupplier.payableAmount)
            }}</a-descriptions-item>
            <a-descriptions-item label="累计已付金额（含税，元）">{{
              formatMoney(selectedSupplier.paidAmount)
            }}</a-descriptions-item>
            <a-descriptions-item label="当前待付金额（含税，元）">{{
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
                  ><a-form-item label="供应商类别" name="category"
                    ><a-select
                      v-model:value="form.category"
                      :options="supplierCategoryOptions"
                      placeholder="请选择供应商类别"
                      allow-clear /></a-form-item
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
                  ><a-form-item label="负责采购"
                    ><a-input
                      v-model:value="form.purchaserName"
                      placeholder="内部采购负责人" /></a-form-item
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
                      :color="admissionColor(selectedSupplier.admissionStatus)"
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

    <a-drawer
      v-model:open="portalAccountsOpen"
      width="min(960px, 100vw)"
      title="供应商门户账号审核"
    >
      <a-alert
        type="info"
        show-icon
        message="门户账号审核与供应商准入审批相互独立；两项均通过后，供应商才可自行报价。"
        style="margin-bottom: 16px"
      />
      <a-table
        :columns="portalAccountColumns"
        :data-source="portalAccounts"
        :loading="portalLoading"
        row-key="id"
        :pagination="{ pageSize: 10 }"
        :scroll="{ x: 820 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'supplier'">
            <strong>{{ record.supplierName || "-" }}</strong>
            <span class="table-subtitle">{{
              record.supplierCode || "待分配编码"
            }}</span>
          </template>
          <template v-else-if="column.key === 'contact'">
            {{ record.contactName }}
            <span class="table-subtitle"
              >{{ record.email }} · {{ record.phone || "-" }}</span
            >
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="portalStatusColor(record.status)">{{
              portalStatusText(record.status)
            }}</a-tag>
            <a-tag v-if="record.mustChangePassword" color="orange"
              >需修改临时密码</a-tag
            >
            <span class="table-subtitle"
              >供应商准入：{{
                admissionLabel(record.supplierAdmissionStatus)
              }}</span
            >
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space wrap @click.stop>
              <a-button
                type="link"
                size="small"
                @click="openPortalDocuments(record)"
                >自助资料</a-button
              >
              <a-button
                v-if="
                  record.status === 'PENDING_REVIEW' &&
                  auth.can('procurement:portal-account:approve')
                "
                type="link"
                size="small"
                @click="openPortalReview(record, 'ACTIVE')"
                >通过</a-button
              >
              <a-button
                v-if="
                  record.status === 'PENDING_REVIEW' &&
                  auth.can('procurement:portal-account:approve')
                "
                type="link"
                danger
                size="small"
                @click="openPortalReview(record, 'REJECTED')"
                >驳回</a-button
              >
              <a-button
                v-if="
                  record.status === 'ACTIVE' &&
                  auth.can('procurement:portal-account:approve')
                "
                type="link"
                danger
                size="small"
                @click="changePortalAccountStatus(record, 'SUSPENDED')"
                >停用</a-button
              >
              <a-button
                v-if="
                  record.status === 'SUSPENDED' &&
                  auth.can('procurement:portal-account:approve')
                "
                type="link"
                size="small"
                @click="changePortalAccountStatus(record, 'ACTIVE')"
                >恢复</a-button
              >
              <a-button
                v-if="
                  ['ACTIVE', 'SUSPENDED'].includes(record.status) &&
                  auth.can('procurement:portal-account:approve')
                "
                type="link"
                size="small"
                @click="resetPortalPassword(record)"
                >重置密码</a-button
              >
            </a-space>
          </template>
        </template>
      </a-table>
    </a-drawer>

    <a-drawer
      v-model:open="changeRequestsOpen"
      width="min(1080px, 100vw)"
      title="供应商信息变更审批"
    >
      <a-alert
        type="info"
        show-icon
        message="供应商在门户提交的企业名称、信用代码、银行信息或结算条款变更在此审批；通过后立即生效并同步供应商主档。"
        style="margin-bottom: 16px"
      />
      <a-table
        :columns="changeRequestColumns"
        :data-source="changeRequests"
        :loading="changeLoading"
        row-key="id"
        :pagination="{ pageSize: 10 }"
        :scroll="{ x: 980 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'supplier'">
            <strong>{{ supplierName(record.supplierId) || "-" }}</strong>
            <span class="table-subtitle">{{
              changeTypeLabel(record.changeType)
            }}</span>
          </template>
          <template v-else-if="column.key === 'proposal'">
            <div>{{ changeProposal(record) }}</div>
            <span class="table-subtitle">{{ record.reason }}</span>
          </template>
          <template v-else-if="column.key === 'requester'">
            {{ record.requestedByName || "-" }}
            <span class="table-subtitle"
              >{{ record.requestSource === "PORTAL" ? "供应商门户" : "内部" }} ·
              {{ formatTime(record.createdAt) }}</span
            >
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="changeStatusColor(record.status)">{{
              changeStatusText(record.status)
            }}</a-tag>
            <span v-if="record.reviewComment" class="table-subtitle"
              >意见：{{ record.reviewComment }}</span
            >
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space wrap @click.stop>
              <a-button
                v-if="
                  record.status === 'PENDING' &&
                  auth.can('procurement:request:approve')
                "
                type="link"
                size="small"
                @click="openChangeReview(record, 'APPROVED')"
                >通过</a-button
              >
              <a-button
                v-if="
                  record.status === 'PENDING' &&
                  auth.can('procurement:request:approve')
                "
                type="link"
                danger
                size="small"
                @click="openChangeReview(record, 'REJECTED')"
                >驳回</a-button
              >
            </a-space>
          </template>
        </template>
      </a-table>
    </a-drawer>

    <a-modal
      v-model:open="changeReviewOpen"
      :title="
        changeReviewRecord
          ? `${changeTypeLabel(changeReviewRecord.changeType)}变更审批`
          : '变更审批'
      "
      ok-text="确认"
      cancel-text="取消"
      :ok-button-props="{ danger: changeReviewDecision === 'REJECTED' }"
      :confirm-loading="changeReviewSaving"
      @ok="submitChangeReview"
    >
      <a-alert
        v-if="changeReviewDecision === 'APPROVED'"
        type="success"
        show-icon
        message="通过后变更立即写入供应商主档。"
        style="margin-bottom: 16px"
      />
      <a-alert
        v-else
        type="warning"
        show-icon
        message="驳回时请填写意见，供应商可在门户查看。"
        style="margin-bottom: 16px"
      />
      <a-form layout="vertical">
        <a-form-item
          :label="
            changeReviewDecision === 'APPROVED'
              ? '审批意见（可选）'
              : '驳回原因'
          "
          :required="changeReviewDecision === 'REJECTED'"
        >
          <a-textarea
            v-model:value="changeReviewComment"
            :rows="3"
            :maxlength="500"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="portalOpenAccountOpen"
      title="为已有供应商开通门户账号"
      :confirm-loading="portalOpening"
      @ok="submitOpenPortalAccount"
    >
      <a-alert
        type="info"
        show-icon
        message="系统会生成一次性临时密码"
        description="账号开通后供应商可以登录门户，但首次登录必须修改临时密码；供应商能否报价仍取决于准入审批和风险状态。"
        style="margin-bottom: 16px"
      />
      <a-form layout="vertical">
        <a-form-item label="供应商">
          <a-input :value="portalOpenAccountSupplier?.name || ''" disabled />
        </a-form-item>
        <a-form-item label="门户登录邮箱" required>
          <a-input v-model:value="portalOpenAccountForm.email" type="email" />
        </a-form-item>
        <a-form-item label="联系人" required>
          <a-input v-model:value="portalOpenAccountForm.contactName" />
        </a-form-item>
        <a-form-item label="联系电话">
          <a-input v-model:value="portalOpenAccountForm.phone" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="portalReviewOpen"
      :title="
        portalReviewDecision === 'ACTIVE' ? '通过门户账号' : '驳回门户账号'
      "
      @ok="submitPortalReview"
    >
      <a-form layout="vertical"
        ><a-form-item
          label="审核意见"
          :required="portalReviewDecision === 'REJECTED'"
          ><a-textarea
            v-model:value="portalReviewComment"
            :rows="3" /></a-form-item
      ></a-form>
    </a-modal>

    <a-drawer
      v-model:open="portalDocumentsOpen"
      width="min(760px, 100vw)"
      title="供应商自助上传资料"
    >
      <a-table
        :columns="portalDocumentColumns"
        :data-source="portalDocuments"
        :loading="portalDocumentsLoading"
        row-key="id"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'file'"
            ><strong>{{ record.documentName }}</strong
            ><span class="table-subtitle"
              >{{ portalDocumentTypeText(record.documentType) }} ·
              {{ formatFileSize(record.sizeBytes) }}</span
            ></template
          >
          <template v-else-if="column.key === 'review'"
            ><a-tag
              :color="
                record.reviewStatus === 'APPROVED'
                  ? 'green'
                  : record.reviewStatus === 'REJECTED'
                    ? 'red'
                    : 'orange'
              "
              >{{
                record.reviewStatus === "APPROVED"
                  ? "已通过"
                  : record.reviewStatus === "REJECTED"
                    ? "已退回"
                    : "待审核"
              }}</a-tag
            ><span v-if="record.reviewComment" class="table-subtitle">{{
              record.reviewComment
            }}</span></template
          >
          <template v-else-if="column.key === 'action'"
            ><a-space
              ><a-button
                v-if="
                  record.reviewStatus !== 'APPROVED' &&
                  auth.can('procurement:portal-document:approve')
                "
                type="link"
                size="small"
                @click="reviewPortalDocument(record, 'APPROVED')"
                >通过</a-button
              ><a-button
                v-if="
                  record.reviewStatus !== 'REJECTED' &&
                  auth.can('procurement:portal-document:approve')
                "
                type="link"
                danger
                size="small"
                @click="reviewPortalDocument(record, 'REJECTED')"
                >退回</a-button
              ></a-space
            ></template
          >
        </template>
      </a-table>
    </a-drawer>

    <a-drawer
      v-model:open="documentOpen"
      width="min(820px, 100vw)"
      title="供应商档案"
    >
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

    <a-modal
      v-model:open="supplierCategoryOpen"
      title="供应商类别字典"
      width="760px"
      :confirm-loading="supplierCategorySaving"
      :footer="null"
    >
      <a-space direction="vertical" style="width: 100%" size="middle">
        <a-form
          ref="supplierCategoryFormRef"
          :model="supplierCategoryForm"
          layout="inline"
          @finish="saveSupplierCategory"
        >
          <a-form-item
            label="类别名称"
            name="name"
            :rules="[{ required: true, message: '请输入类别名称' }]"
          >
            <a-input
              v-model:value="supplierCategoryForm.name"
              placeholder="如：原材料与辅料"
              style="width: 180px"
            />
          </a-form-item>
          <a-form-item label="说明" name="description">
            <a-input
              v-model:value="supplierCategoryForm.description"
              placeholder="可选"
              style="width: 180px"
            />
          </a-form-item>
          <a-form-item label="排序" name="sortOrder">
            <a-input-number
              v-model:value="supplierCategoryForm.sortOrder"
              :min="0"
              style="width: 90px"
            />
          </a-form-item>
          <a-form-item name="enabled">
            <a-checkbox v-model:checked="supplierCategoryForm.enabled"
              >启用</a-checkbox
            >
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button
                type="primary"
                html-type="submit"
                :loading="supplierCategorySaving"
              >
                {{ editingSupplierCategory ? "保存" : "新增" }}
              </a-button>
              <a-button
                v-if="editingSupplierCategory"
                @click="resetSupplierCategoryForm"
                >取消编辑</a-button
              >
            </a-space>
          </a-form-item>
        </a-form>
        <a-table
          size="small"
          :columns="supplierCategoryColumns"
          :data-source="supplierCategories"
          :loading="supplierCategoryLoading"
          row-key="id"
          :pagination="false"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <a-tag :color="record.enabled ? 'green' : 'default'">
                {{ record.enabled ? "启用" : "停用" }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'builtIn'">
              {{ record.builtIn ? "系统预置" : "自定义" }}
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button
                type="link"
                size="small"
                @click="editSupplierCategory(record)"
                >编辑</a-button
              >
            </template>
          </template>
        </a-table>
      </a-space>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { message, Modal } from "ant-design-vue";
import DownloadOutlined from "@ant-design/icons-vue/DownloadOutlined";
import PlusOutlined from "@ant-design/icons-vue/PlusOutlined";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import TagsOutlined from "@ant-design/icons-vue/TagsOutlined";
import UploadOutlined from "@ant-design/icons-vue/UploadOutlined";
import {
  listSuppliers,
  listPurchaseOrders,
  createSupplier,
  reviewSupplierAdmission,
  listSupplierPortalAccounts,
  openSupplierPortalAccount,
  listSupplierPortalDocuments,
  resetSupplierPortalPassword,
  reviewSupplierPortalAccount,
  reviewSupplierPortalDocument,
  updateSupplierPortalAccountStatus,
  listSupplierChangeRequests,
  reviewSupplierChangeRequest,
  updateSupplier,
  type CreateSupplierPayload,
  type Supplier,
  type PurchaseOrder,
  type SupplierPortalAccount,
  type SupplierPortalDocument,
  type SupplierChangeRequest,
  type SupplierCategory,
  listSupplierCategories,
  createSupplierCategory,
  updateSupplierCategory,
  exportProcurementSuppliers,
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
const exporting = ref(false);
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
const portalAccountsOpen = ref(false);
const portalOpenAccountOpen = ref(false);
const portalReviewOpen = ref(false);
const portalDocumentsOpen = ref(false);
const portalLoading = ref(false);
const portalDocumentsLoading = ref(false);
const portalAccounts = ref<SupplierPortalAccount[]>([]);
const changeRequestsOpen = ref(false);
const changeRequests = ref<SupplierChangeRequest[]>([]);
const changeLoading = ref(false);
const changeReviewOpen = ref(false);
const changeReviewRecord = ref<SupplierChangeRequest | null>(null);
const changeReviewDecision = ref<"APPROVED" | "REJECTED">("APPROVED");
const changeReviewComment = ref("");
const changeReviewSaving = ref(false);
const pendingChangeCount = computed(
  () => changeRequests.value.filter((item) => item.status === "PENDING").length,
);
const changeRequestColumns = [
  { title: "供应商 / 变更类型", key: "supplier", width: 220 },
  { title: "变更内容", key: "proposal" },
  { title: "申请人 / 时间", key: "requester", width: 200 },
  { title: "状态", key: "status", width: 160 },
  { title: "操作", key: "action", width: 140 },
];
const portalDocuments = ref<SupplierPortalDocument[]>([]);
const portalReviewTarget = ref<SupplierPortalAccount | null>(null);
const portalDocumentSupplierId = ref("");
const portalReviewDecision = ref<"ACTIVE" | "REJECTED">("ACTIVE");
const portalReviewComment = ref("");
const portalOpening = ref(false);
const portalOpenAccountSupplier = ref<Supplier | null>(null);
const portalOpenAccountForm = reactive({
  email: "",
  phone: "",
  contactName: "",
});
const reviewTarget = ref<Supplier | null>(null);
const reviewForm = reactive<{
  decision: "APPROVED" | "REJECTED";
  comment: string;
}>({ decision: "APPROVED", comment: "供应商资料核验通过，同意准入" });
const formRef = ref();
const profileFormRef = ref();
const supplierEditing = ref(false);
const form = reactive<CreateSupplierPayload>(emptySupplierForm());
const supplierCategories = ref<SupplierCategory[]>([]);
const supplierCategoryOpen = ref(false);
const supplierCategoryLoading = ref(false);
const supplierCategorySaving = ref(false);
const supplierCategoryFormRef = ref();
const editingSupplierCategory = ref<SupplierCategory | null>(null);
const supplierCategoryForm = reactive({
  name: "",
  description: "",
  sortOrder: 0,
  enabled: true,
});

const rules = {
  code: [{ required: true, message: "请输入供应商编码" }],
  name: [{ required: true, message: "请输入供应商名称" }],
  category: [{ required: true, message: "请选择供应商类别" }],
};
const supplierCategoryOptions = computed(() =>
  supplierCategories.value
    .filter((item) => item.enabled || item.name === form.category)
    .map((item) => ({
      label: item.name,
      value: item.name,
      disabled: !item.enabled,
    })),
);
const supplierCategoryColumns = [
  { title: "类别", dataIndex: "name", key: "name", width: 180 },
  { title: "说明", dataIndex: "description", key: "description" },
  { title: "排序", dataIndex: "sortOrder", key: "sortOrder", width: 80 },
  { title: "状态", key: "status", width: 90 },
  {
    title: "供应商数",
    dataIndex: "supplierCount",
    key: "supplierCount",
    width: 100,
  },
  { title: "来源", key: "builtIn", width: 90 },
  { title: "操作", key: "action", width: 80 },
];
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
  { title: "负责采购", dataIndex: "purchaserName", width: 120 },
  { title: "签约金额（含税，元）", key: "contractedAmount", width: 190 },
  { title: "应付金额（含税，元）", key: "payableAmount", width: 190 },
  { title: "已付金额（含税，元）", key: "paidAmount", width: 190 },
  { title: "待付金额（含税，元）", key: "outstandingAmount", width: 190 },
  { title: "资料完整度", key: "profile", width: 180 },
  { title: "有效期", key: "validity", width: 170 },
  { title: "评分", key: "score", width: 160 },
  { title: "准入审批", key: "admission", width: 170 },
  { title: "风险", key: "status", width: 100 },
  { title: "操作", key: "action", width: 210, fixed: "right" },
];
const portalAccountColumns = [
  { title: "供应商", key: "supplier", width: 210 },
  { title: "联系人", key: "contact", width: 260 },
  { title: "账号与准入", key: "status", width: 190 },
  { title: "注册时间", dataIndex: "createdAt", width: 190 },
  { title: "操作", key: "action", width: 180, fixed: "right" },
];
const portalDocumentColumns = [
  { title: "文件", key: "file" },
  { title: "有效期", dataIndex: "validTo", width: 120 },
  { title: "审核", key: "review", width: 160 },
  { title: "操作", key: "action", width: 120 },
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
  { title: "订单金额（含税，元）", key: "amount", width: 200 },
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
const pendingPortalAccounts = computed(
  () =>
    portalAccounts.value.filter((item) => item.status === "PENDING_REVIEW")
      .length,
);
function portalAccountForSupplier(supplierId: string) {
  return portalAccounts.value.find((item) => item.supplierId === supplierId);
}
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
    const [result, accounts, categories] = await Promise.all([
      listSuppliers(0, 999),
      listSupplierPortalAccounts(),
      listSupplierCategories(),
    ]);
    suppliers.value = result.content;
    portalAccounts.value = accounts;
    supplierCategories.value = categories;
    if (!form.category) {
      form.category = categories.find((item) => item.enabled)?.name || "";
    }
  } catch (error) {
    message.error(error instanceof Error ? error.message : "加载失败");
  } finally {
    loading.value = false;
  }
}

async function openPortalAccounts() {
  portalAccountsOpen.value = true;
  portalLoading.value = true;
  try {
    portalAccounts.value = await listSupplierPortalAccounts();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "门户账号加载失败");
  } finally {
    portalLoading.value = false;
  }
}

async function openChangeRequests() {
  changeRequestsOpen.value = true;
  changeLoading.value = true;
  try {
    changeRequests.value = await listSupplierChangeRequests();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "变更申请加载失败");
  } finally {
    changeLoading.value = false;
  }
}

function supplierName(supplierId: string) {
  return suppliers.value.find((item) => item.id === supplierId)?.name;
}

function changeTypeLabel(type: string) {
  return (
    (
      {
        NAME: "企业名称",
        CREDIT_CODE: "统一社会信用代码",
        BANK_INFO: "银行信息",
        SETTLEMENT_TERMS: "结算条款",
        ADMISSION: "供应商准入",
        RISK: "风险状态",
      } as Record<string, string>
    )[type] || type
  );
}

function changeProposal(record: SupplierChangeRequest) {
  const value =
    record.proposedName ||
    record.proposedCreditCode ||
    record.proposedBankName ||
    record.proposedBankAccount ||
    record.proposedSettlementTerms ||
    "-";
  return `${changeTypeLabel(record.changeType)}：${value}`;
}

function changeStatusText(status: string) {
  return (
    (
      { PENDING: "待审批", APPROVED: "已通过", REJECTED: "已退回" } as Record<
        string,
        string
      >
    )[status] || status
  );
}

function changeStatusColor(status: string) {
  return (
    (
      { PENDING: "orange", APPROVED: "green", REJECTED: "red" } as Record<
        string,
        string
      >
    )[status] || "default"
  );
}

function formatTime(value?: string) {
  return value
    ? new Intl.DateTimeFormat("zh-CN", { hour12: false }).format(
        new Date(value),
      )
    : "";
}

function openChangeReview(
  record: SupplierChangeRequest,
  decision: "APPROVED" | "REJECTED",
) {
  changeReviewRecord.value = record;
  changeReviewDecision.value = decision;
  changeReviewComment.value = "";
  changeReviewOpen.value = true;
}

async function submitChangeReview() {
  const record = changeReviewRecord.value;
  if (!record) return;
  if (
    changeReviewDecision.value === "REJECTED" &&
    !changeReviewComment.value.trim()
  ) {
    message.warning("驳回时必须填写原因");
    return;
  }
  changeReviewSaving.value = true;
  try {
    await reviewSupplierChangeRequest(record.id, {
      decision: changeReviewDecision.value,
      comment: changeReviewComment.value.trim() || undefined,
    });
    message.success("变更申请已处理");
    changeReviewOpen.value = false;
    await openChangeRequests();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "审批失败");
  } finally {
    changeReviewSaving.value = false;
  }
}

function openPortalAccount(supplier: Supplier) {
  portalOpenAccountSupplier.value = supplier;
  portalOpenAccountForm.email = "";
  portalOpenAccountForm.phone = supplier.phone || "";
  portalOpenAccountForm.contactName = supplier.contactName || "";
  portalOpenAccountOpen.value = true;
}

async function submitOpenPortalAccount() {
  const supplier = portalOpenAccountSupplier.value;
  if (!supplier) return;
  if (
    !portalOpenAccountForm.email.trim() ||
    !portalOpenAccountForm.contactName.trim()
  ) {
    message.warning("请填写门户登录邮箱和联系人");
    return;
  }
  portalOpening.value = true;
  try {
    const result = await openSupplierPortalAccount(supplier.id, {
      email: portalOpenAccountForm.email.trim(),
      phone: portalOpenAccountForm.phone.trim() || undefined,
      contactName: portalOpenAccountForm.contactName.trim(),
    });
    portalOpenAccountOpen.value = false;
    Modal.info({
      title: "供应商门户账号已开通",
      content: `登录邮箱：${result.account.email}\n临时密码：${result.temporaryPassword}\n请通过可信渠道交给供应商，首次登录后必须修改密码。`,
      okText: "我已记录",
    });
    portalAccounts.value = await listSupplierPortalAccounts();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "门户账号开通失败");
  } finally {
    portalOpening.value = false;
  }
}

function openPortalReview(
  account: SupplierPortalAccount,
  decision: "ACTIVE" | "REJECTED",
) {
  portalReviewTarget.value = account;
  portalReviewDecision.value = decision;
  portalReviewComment.value = decision === "ACTIVE" ? "账号身份核验通过" : "";
  portalReviewOpen.value = true;
}

async function submitPortalReview() {
  if (!portalReviewTarget.value) return;
  if (
    portalReviewDecision.value === "REJECTED" &&
    !portalReviewComment.value.trim()
  ) {
    message.warning("驳回时请填写原因");
    return;
  }
  await reviewSupplierPortalAccount(
    portalReviewTarget.value.id,
    portalReviewDecision.value,
    portalReviewComment.value,
  );
  portalReviewOpen.value = false;
  message.success("门户账号审核已完成");
  await openPortalAccounts();
}

function changePortalAccountStatus(
  account: SupplierPortalAccount,
  status: "ACTIVE" | "SUSPENDED",
) {
  Modal.confirm({
    title: status === "SUSPENDED" ? "停用门户账号？" : "恢复门户账号？",
    content:
      status === "SUSPENDED"
        ? "停用后现有登录令牌立即失效，供应商无法继续报价。"
        : "恢复后供应商可重新登录；是否允许报价仍取决于供应商准入状态。",
    okText: status === "SUSPENDED" ? "确认停用" : "确认恢复",
    okButtonProps: { danger: status === "SUSPENDED" },
    async onOk() {
      await updateSupplierPortalAccountStatus(
        account.id,
        status,
        status === "SUSPENDED"
          ? "采购管理员停用门户账号"
          : "采购管理员恢复门户账号",
      );
      message.success(status === "SUSPENDED" ? "账号已停用" : "账号已恢复");
      await openPortalAccounts();
    },
  });
}

function resetPortalPassword(account: SupplierPortalAccount) {
  Modal.confirm({
    title: "重置门户密码？",
    content: "重置后现有登录令牌立即失效，新密码只展示一次。",
    okText: "确认重置",
    async onOk() {
      const result = await resetSupplierPortalPassword(account.id);
      Modal.info({
        title: "临时密码已生成",
        content: `临时密码：${result.temporaryPassword}。请通过可信渠道交给供应商，首次登录后必须修改。`,
        okText: "我已记录",
      });
      await openPortalAccounts();
    },
  });
}

async function openPortalDocuments(account: SupplierPortalAccount) {
  portalDocumentSupplierId.value = account.supplierId;
  portalDocumentsOpen.value = true;
  portalDocumentsLoading.value = true;
  try {
    portalDocuments.value = await listSupplierPortalDocuments(
      account.supplierId,
    );
  } finally {
    portalDocumentsLoading.value = false;
  }
}

async function reviewPortalDocument(
  document: SupplierPortalDocument,
  decision: "APPROVED" | "REJECTED",
) {
  await reviewSupplierPortalDocument(
    document.id,
    decision,
    decision === "APPROVED" ? "资料核验通过" : "资料不符合要求，请重新上传",
  );
  portalDocuments.value = await listSupplierPortalDocuments(
    portalDocumentSupplierId.value,
  );
  message.success("资料审核已更新");
}

function portalStatusText(status: string) {
  return (
    {
      PENDING_REVIEW: "待审核",
      ACTIVE: "已启用",
      REJECTED: "已驳回",
      SUSPENDED: "已停用",
    }[status] || status
  );
}

function portalStatusColor(status: string) {
  return (
    {
      PENDING_REVIEW: "orange",
      ACTIVE: "green",
      REJECTED: "red",
      SUSPENDED: "default",
    }[status] || "default"
  );
}

function portalDocumentTypeText(type: string) {
  return (
    {
      BUSINESS_LICENSE: "营业执照",
      QUALIFICATION: "行业资质",
      BANK_PROOF: "银行证明",
      TAX_DOCUMENT: "税务资料",
      OTHER: "其他",
    }[type] || type
  );
}

function openCreate() {
  Object.assign(form, emptySupplierForm(), {
    code: generateCode("GYS"),
    purchaserName: auth.user?.displayName || "",
  });
  form.category =
    supplierCategories.value.find((item) => item.enabled)?.name || "";
  createOpen.value = true;
}

async function openSupplierCategoryDictionary() {
  supplierCategoryOpen.value = true;
  resetSupplierCategoryForm();
  supplierCategoryLoading.value = true;
  try {
    supplierCategories.value = await listSupplierCategories();
  } catch (error) {
    message.error(
      error instanceof Error ? error.message : "供应商类别加载失败",
    );
  } finally {
    supplierCategoryLoading.value = false;
  }
}

function resetSupplierCategoryForm() {
  editingSupplierCategory.value = null;
  Object.assign(supplierCategoryForm, {
    name: "",
    description: "",
    sortOrder: supplierCategories.value.length + 1,
    enabled: true,
  });
}

function editSupplierCategory(category: SupplierCategory) {
  editingSupplierCategory.value = category;
  Object.assign(supplierCategoryForm, {
    name: category.name,
    description: category.description || "",
    sortOrder: category.sortOrder,
    enabled: category.enabled,
  });
}

async function saveSupplierCategory() {
  supplierCategorySaving.value = true;
  try {
    const payload = {
      name: supplierCategoryForm.name.trim(),
      description: supplierCategoryForm.description.trim() || undefined,
      sortOrder: supplierCategoryForm.sortOrder,
      enabled: supplierCategoryForm.enabled,
    };
    if (!payload.name) {
      message.warning("请输入类别名称");
      return;
    }
    if (editingSupplierCategory.value) {
      await updateSupplierCategory(editingSupplierCategory.value.id, payload);
      message.success("供应商类别已更新");
    } else {
      await createSupplierCategory(payload);
      message.success("供应商类别已新增");
    }
    supplierCategories.value = await listSupplierCategories();
    resetSupplierCategoryForm();
  } catch (error) {
    message.error(
      error instanceof Error ? error.message : "供应商类别保存失败",
    );
  } finally {
    supplierCategorySaving.value = false;
  }
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
    purchaserName: "",
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
    purchaserName: record.purchaserName || "",
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
async function handleExport() {
  exporting.value = true;
  try {
    await exportProcurementSuppliers();
    message.success("供应商已导出");
  } catch (e) {
    message.error(e instanceof Error ? e.message : "导出失败");
  } finally {
    exporting.value = false;
  }
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
