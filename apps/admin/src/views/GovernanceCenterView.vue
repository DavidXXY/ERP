<template>
  <div class="page-stack governance-page">
    <a-card title="经营治理中心">
      <template #extra>
        <a-space>
          <a-button :loading="loading" @click="loadAll">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
          <a-button v-if="canManage" type="primary" @click="openControlModal()">
            <template #icon><PlusOutlined /></template>
            新建控制项
          </a-button>
        </a-space>
      </template>

      <a-row :gutter="[16, 16]" class="metric-row">
        <a-col :xs="12" :xl="4"
          ><a-statistic
            title="执行中"
            :value="overview.activeControls"
            suffix="项"
        /></a-col>
        <a-col :xs="12" :xl="4"
          ><a-statistic
            title="阻塞"
            :value="overview.blockedControls"
            suffix="项"
            :value-style="dangerStyle(overview.blockedControls)"
        /></a-col>
        <a-col :xs="12" :xl="4"
          ><a-statistic
            title="已逾期"
            :value="overview.overdueControls"
            suffix="项"
            :value-style="dangerStyle(overview.overdueControls)"
        /></a-col>
        <a-col :xs="12" :xl="4"
          ><a-statistic
            title="预测偏差"
            :value="overview.forecastVariance"
            :formatter="moneyFormatter"
            :value-style="dangerStyle(overview.forecastVariance)"
        /></a-col>
        <a-col :xs="12" :xl="4"
          ><a-statistic
            title="未对账流水"
            :value="overview.unmatchedBankLines"
            suffix="条"
            :value-style="dangerStyle(overview.unmatchedBankLines)"
        /></a-col>
        <a-col :xs="12" :xl="4"
          ><a-statistic
            title="已关账期间"
            :value="overview.closedPeriods"
            suffix="期"
        /></a-col>
      </a-row>

      <div class="governance-domain-grid">
        <button
          v-for="item in overview.domains"
          :key="item.domain"
          type="button"
          @click="selectDomain(item.domain)"
        >
          <span>{{ domainLabel(item.domain) }}</span>
          <strong>{{ item.active }} / {{ item.total }}</strong>
          <small :class="{ danger: item.exceptionCount > 0 }"
            >{{ item.exceptionCount }} 项异常 ·
            {{ formatMoney(item.exposureAmount) }}</small
          >
        </button>
      </div>
    </a-card>

    <a-card>
      <a-tabs v-model:active-key="activeTab">
        <a-tab-pane key="exceptions">
          <template #tab
            ><Badge :count="exceptions.length" :offset="[8, -3]"
              >经营异常</Badge
            ></template
          >
          <a-table
            :columns="exceptionColumns"
            :data-source="exceptions"
            :loading="loading"
            :pagination="{ pageSize: 10 }"
            row-key="key"
            :scroll="{ x: 1040 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'severity'"
                ><a-tag :color="riskColor(record.severity)">{{
                  riskLabel(record.severity)
                }}</a-tag></template
              >
              <template v-else-if="column.key === 'name'"
                ><strong>{{ record.name }}</strong
                ><span class="table-subtitle"
                  >{{ record.controlCode }} · {{ record.message }}</span
                ></template
              >
              <template v-else-if="column.key === 'type'">{{
                typeLabel(record.controlType)
              }}</template>
              <template v-else-if="column.key === 'exposure'">{{
                formatMoney(record.exposureAmount)
              }}</template>
              <template v-else-if="column.key === 'action'"
                ><a-button
                  type="link"
                  size="small"
                  @click="focusControl(record.controlId)"
                  >处理</a-button
                ></template
              >
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="controls" tab="控制台账">
          <a-space wrap class="table-toolbar">
            <a-input-search
              v-model:value="filters.keyword"
              allow-clear
              placeholder="搜索编号、业务单号、名称或负责人"
              style="width: 300px"
            />
            <a-select
              v-model:value="filters.type"
              allow-clear
              show-search
              option-filter-prop="label"
              placeholder="控制类型"
              :options="typeOptions"
              style="width: 240px"
            />
            <a-select
              v-model:value="filters.status"
              allow-clear
              placeholder="执行状态"
              :options="statusOptions"
              style="width: 140px"
            />
            <a-button @click="loadControls">查询</a-button>
          </a-space>
          <a-table
            :columns="controlColumns"
            :data-source="controls"
            :loading="loading"
            :pagination="{ pageSize: 10 }"
            row-key="id"
            :scroll="{ x: 1420 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'control'"
                ><strong>{{ record.name }}</strong
                ><span class="table-subtitle"
                  >{{ record.controlCode }} ·
                  {{ record.businessNo || "未关联单号" }}</span
                ></template
              >
              <template v-else-if="column.key === 'type'"
                ><a-tag>{{ record.typeLabel }}</a-tag></template
              >
              <template v-else-if="column.key === 'status'"
                ><a-tag :color="statusColor(record.status)">{{
                  statusLabel(record.status)
                }}</a-tag></template
              >
              <template v-else-if="column.key === 'risk'"
                ><a-tag :color="riskColor(record.riskLevel)">{{
                  riskLabel(record.riskLevel)
                }}</a-tag></template
              >
              <template v-else-if="column.key === 'amounts'"
                ><span>{{ formatMoney(record.budgetAmount || 0) }}</span
                ><span class="table-subtitle"
                  >承诺 {{ formatMoney(record.committedAmount || 0) }} · 预测
                  {{ formatMoney(record.forecastAmount || 0) }}</span
                ></template
              >
              <template v-else-if="column.key === 'progress'"
                ><a-progress
                  :percent="Number(record.progressPercent || 0)"
                  size="small"
              /></template>
              <template v-else-if="column.key === 'action'">
                <a-space size="small">
                  <a-button
                    v-if="canManage && !isFinal(record.status)"
                    type="link"
                    size="small"
                    @click="openControlModal(record)"
                    >编辑</a-button
                  >
                  <a-button
                    v-if="canManage && !isFinal(record.status)"
                    type="link"
                    size="small"
                    @click="openTransitionModal(record)"
                    >推进</a-button
                  >
                  <a-button
                    v-if="
                      canManage && ['ACTIVE', 'BLOCKED'].includes(record.status)
                    "
                    type="link"
                    size="small"
                    @click="handleReview(record)"
                    >复核</a-button
                  >
                  <a-button
                    type="link"
                    size="small"
                    @click="
                      openHistory('CONTROL', record.id, record.controlCode)
                    "
                    >轨迹</a-button
                  >
                </a-space>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="registers" tab="专项业务台账">
          <div class="register-toolbar">
            <a-segmented
              v-model:value="registerDomain"
              :options="registerDomainOptions"
            />
            <a-button
              v-if="canManage"
              type="primary"
              @click="openRegisterControl"
              >新增{{ registerTitle }}</a-button
            >
          </div>
          <a-row :gutter="12" class="register-metrics">
            <a-col :xs="12" :md="6"
              ><a-statistic title="台账总数" :value="registerControls.length"
            /></a-col>
            <a-col :xs="12" :md="6"
              ><a-statistic title="执行/阻塞" :value="registerActiveCount"
            /></a-col>
            <a-col :xs="12" :md="6"
              ><a-statistic
                title="计划金额（税价随来源单据，元）"
                :value="registerBudget"
                :formatter="moneyFormatter"
            /></a-col>
            <a-col :xs="12" :md="6"
              ><a-statistic
                title="预测偏差"
                :value="registerVariance"
                :formatter="moneyFormatter"
                :value-style="dangerStyle(registerVariance)"
            /></a-col>
          </a-row>
          <a-table
            :columns="registerColumns"
            :data-source="registerControls"
            row-key="id"
            :pagination="{ pageSize: 10 }"
            :scroll="{ x: 1120 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'business'"
                ><strong>{{ record.name }}</strong
                ><span class="table-subtitle"
                  >{{ record.businessNo || record.controlCode }} ·
                  {{ record.typeLabel }}</span
                ></template
              >
              <template v-else-if="column.key === 'status'"
                ><a-tag :color="statusColor(record.status)">{{
                  statusLabel(record.status)
                }}</a-tag></template
              >
              <template v-else-if="column.key === 'amount'"
                >{{
                  formatMoney(
                    record.forecastAmount || record.actualAmount || 0,
                  )
                }}<span class="table-subtitle"
                  >基准 {{ formatMoney(record.budgetAmount || 0) }}</span
                ></template
              >
              <template v-else-if="column.key === 'progress'"
                ><a-progress
                  :percent="Number(record.progressPercent || 0)"
                  size="small"
              /></template>
              <template v-else-if="column.key === 'action'"
                ><a-button
                  type="link"
                  size="small"
                  @click="openControlModal(record)"
                  >查看/维护</a-button
                ></template
              >
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="forecast" tab="预测决策">
          <a-alert
            type="info"
            show-icon
            message="滚动预测以当前有效版本为准，偏差 = 预测金额 - 预算/基准金额。"
            class="forecast-note"
          />
          <div class="forecast-kpis">
            <div>
              <span>预测事项</span
              ><strong>{{ forecastControls.length }}</strong>
            </div>
            <div>
              <span>预算基准</span
              ><strong>{{ formatMoney(forecastBudget) }}</strong>
            </div>
            <div>
              <span>最新预测</span
              ><strong>{{ formatMoney(forecastAmount) }}</strong>
            </div>
            <div>
              <span>总体偏差</span
              ><strong :class="{ danger: forecastVariance > 0 }">{{
                formatMoney(forecastVariance)
              }}</strong>
            </div>
          </div>
          <a-table
            :columns="forecastColumns"
            :data-source="forecastControls"
            row-key="id"
            :pagination="{ pageSize: 10 }"
            :scroll="{ x: 1080 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'forecast'"
                ><strong>{{ record.name }}</strong
                ><span class="table-subtitle"
                  >{{ record.typeLabel }} ·
                  {{ record.businessNo || record.controlCode }}</span
                ></template
              >
              <template v-else-if="column.key === 'budget'">{{
                formatMoney(record.budgetAmount || 0)
              }}</template>
              <template v-else-if="column.key === 'actual'">{{
                formatMoney(record.actualAmount || 0)
              }}</template>
              <template v-else-if="column.key === 'forecastAmount'">{{
                formatMoney(record.forecastAmount || 0)
              }}</template>
              <template v-else-if="column.key === 'variance'"
                ><span
                  :class="{
                    'variance-danger': forecastRowVariance(record) > 0,
                  }"
                  >{{ formatMoney(forecastRowVariance(record)) }}</span
                ></template
              >
              <template v-else-if="column.key === 'risk'"
                ><a-tag :color="riskColor(record.riskLevel)">{{
                  riskLabel(record.riskLevel)
                }}</a-tag></template
              >
              <template v-else-if="column.key === 'action'"
                ><a-button
                  type="link"
                  size="small"
                  @click="openControlModal(record)"
                  >更新预测</a-button
                ></template
              >
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="periods" tab="会计期间">
          <div class="tab-command-bar">
            <a-space>
              <a-button
                v-if="canClosePeriod"
                type="primary"
                @click="handleOpenCurrentPeriod"
                ><template #icon><CalendarOutlined /></template
                >开启本期</a-button
              >
            </a-space>
          </div>
          <a-table
            :columns="periodColumns"
            :data-source="periods"
            :loading="loading"
            :pagination="false"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'period'"
                ><strong
                  >{{ record.fiscalYear }}-{{
                    String(record.periodNo).padStart(2, "0")
                  }}</strong
                ></template
              >
              <template v-else-if="column.key === 'status'"
                ><a-space size="small"
                  ><a-tag :color="periodStatusColor(record.status)">{{
                    periodStatusLabel(record.status)
                  }}</a-tag
                  ><a-tag v-if="record.pendingAction" color="orange"
                    >待复核</a-tag
                  ></a-space
                ></template
              >
              <template v-else-if="column.key === 'operator'">{{
                record.actionRequestedBy ||
                record.closedBy ||
                record.reopenedBy ||
                "-"
              }}</template>
              <template v-else-if="column.key === 'reason'">{{
                record.actionRequestReason ||
                record.closeReason ||
                record.reopenReason ||
                "-"
              }}</template>
              <template v-else-if="column.key === 'action'">
                <a-button
                  v-if="canClosePeriod && record.status === 'OPEN'"
                  type="link"
                  size="small"
                  @click="prepareClose(record)"
                  >{{
                    record.pendingAction === "FORCE_CLOSE"
                      ? "复核强制关账"
                      : "关账"
                  }}</a-button
                >
                <a-button
                  v-if="canClosePeriod && record.status === 'CLOSED'"
                  type="link"
                  size="small"
                  danger
                  @click="prepareReopen(record)"
                  >{{
                    record.pendingAction === "REOPEN" ? "复核反结账" : "反结账"
                  }}</a-button
                >
                <a-button
                  type="link"
                  size="small"
                  @click="
                    openHistory(
                      'PERIOD',
                      record.id,
                      `${record.fiscalYear}-${String(record.periodNo).padStart(2, '0')}`,
                    )
                  "
                  >轨迹</a-button
                >
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="bank" tab="银行对账">
          <div class="tab-command-bar">
            <a-space wrap>
              <a-select
                v-model:value="bankStatus"
                allow-clear
                placeholder="对账状态"
                :options="bankStatusOptions"
                style="width: 150px"
                @change="loadBankLines"
              />
              <a-button @click="downloadBankTemplate"
                ><template #icon><DownloadOutlined /></template
                >导入模板</a-button
              >
              <a-upload
                v-if="canReconcile"
                :show-upload-list="false"
                accept=".csv,text/csv"
                :before-upload="handleBankFile"
                ><a-button type="primary"
                  ><template #icon><UploadOutlined /></template
                  >导入流水</a-button
                ></a-upload
              >
            </a-space>
          </div>
          <a-table
            :columns="bankColumns"
            :data-source="bankLines"
            :loading="loading"
            :pagination="{ pageSize: 10 }"
            row-key="id"
            :scroll="{ x: 1160 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'direction'"
                ><a-tag
                  :color="record.direction === 'IN' ? 'green' : 'volcano'"
                  >{{ record.direction === "IN" ? "入账" : "出账" }}</a-tag
                ></template
              >
              <template v-else-if="column.key === 'amount'">{{
                formatMoney(record.amount)
              }}</template>
              <template v-else-if="column.key === 'status'"
                ><a-tag :color="bankStatusColor(record.reconciliationStatus)">{{
                  bankStatusLabel(record.reconciliationStatus)
                }}</a-tag></template
              >
              <template v-else-if="column.key === 'match'"
                ><span>{{ record.matchedBizNo || "-" }}</span
                ><span v-if="record.matchedBizType" class="table-subtitle">{{
                  record.matchedBizType
                }}</span></template
              >
              <template v-else-if="column.key === 'action'">
                <a-button
                  v-if="
                    canReconcile && record.reconciliationStatus !== 'MATCHED'
                  "
                  type="link"
                  size="small"
                  @click="openReconcileModal(record)"
                  >{{
                    record.reconciliationStatus === "SUGGESTED"
                      ? "确认匹配"
                      : "手工匹配"
                  }}</a-button
                >
                <a-button
                  v-if="
                    canReconcile && record.reconciliationStatus === 'MATCHED'
                  "
                  type="link"
                  size="small"
                  danger
                  @click="openUnreconcileModal(record)"
                  >解除</a-button
                >
                <a-button
                  type="link"
                  size="small"
                  @click="
                    openHistory('BANK_LINE', record.id, record.bankReference)
                  "
                  >轨迹</a-button
                >
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-modal
      v-model:open="controlModalOpen"
      :title="editingControl ? '编辑经营控制项' : '新建经营控制项'"
      width="820px"
      :confirm-loading="submitting"
      @ok="saveControl"
    >
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="12"
            ><a-form-item label="控制类型" required
              ><a-select
                v-model:value="controlForm.controlType"
                show-search
                option-filter-prop="label"
                :disabled="Boolean(editingControl)"
                :options="typeOptions" /></a-form-item
          ></a-col>
          <a-col :xs="24" :md="12"
            ><a-form-item label="风险等级" required
              ><a-segmented
                v-model:value="controlForm.riskLevel"
                :options="riskOptions"
                block /></a-form-item
          ></a-col>
          <a-col :xs="24" :md="16"
            ><a-form-item label="控制事项" required
              ><a-input
                v-model:value="controlForm.name"
                :maxlength="180" /></a-form-item
          ></a-col>
          <a-col :xs="24" :md="8"
            ><a-form-item label="负责人" required
              ><a-input
                v-model:value="controlForm.owner"
                :maxlength="80" /></a-form-item
          ></a-col>
          <a-col :xs="24" :md="12"
            ><a-form-item label="关联业务单号"
              ><a-input
                v-model:value="controlForm.businessNo"
                :maxlength="100" /></a-form-item
          ></a-col>
          <a-col :xs="12" :md="6"
            ><a-form-item label="计划开始"
              ><a-input
                v-model:value="controlForm.plannedStart"
                type="date" /></a-form-item
          ></a-col>
          <a-col :xs="12" :md="6"
            ><a-form-item label="计划完成"
              ><a-input
                v-model:value="controlForm.plannedEnd"
                type="date" /></a-form-item
          ></a-col>
          <a-col v-for="field in amountFields" :key="field.key" :xs="12" :md="6"
            ><a-form-item :label="field.label"
              ><a-input-number
                v-model:value="controlForm[field.key]"
                :min="0"
                :precision="2"
                style="width: 100%" /></a-form-item
          ></a-col>
          <a-col :xs="12" :md="6"
            ><a-form-item label="完成进度"
              ><a-input-number
                v-model:value="controlForm.progressPercent"
                :min="0"
                :max="100"
                :precision="2"
                style="width: 100%"
                addon-after="%" /></a-form-item
          ></a-col>
          <a-col :xs="12" :md="6"
            ><a-form-item label="复核周期"
              ><a-input-number
                v-model:value="controlForm.reviewFrequencyDays"
                :min="1"
                :max="3660"
                style="width: 100%"
                addon-after="天" /></a-form-item
          ></a-col>
          <a-col :xs="12" :md="6"
            ><a-form-item label="生效日期"
              ><a-input
                v-model:value="controlForm.effectiveFrom"
                type="date" /></a-form-item
          ></a-col>
          <a-col :xs="12" :md="6"
            ><a-form-item label="失效日期"
              ><a-input
                v-model:value="controlForm.effectiveTo"
                type="date" /></a-form-item
          ></a-col>
          <a-col :span="24"
            ><a-form-item label="扩展业务数据"
              ><a-textarea
                v-model:value="controlForm.detailsText"
                :rows="4"
                placeholder='{"口径":"含税","版本":"V1"}' /></a-form-item
          ></a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="transitionModalOpen"
      title="推进控制事项"
      :confirm-loading="submitting"
      @ok="submitTransition"
    >
      <a-form layout="vertical">
        <a-form-item label="目标状态" required
          ><a-select
            v-model:value="transitionForm.status"
            :options="transitionOptions"
        /></a-form-item>
        <a-form-item
          label="处理说明"
          :required="transitionForm.status !== 'ACTIVE'"
          ><a-textarea
            v-model:value="transitionForm.note"
            :rows="4"
            :maxlength="1000"
        /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="periodModalOpen"
      :title="periodAction === 'close' ? '会计期间关账' : '会计期间反结账'"
      :confirm-loading="submitting"
      @ok="submitPeriodAction"
    >
      <a-alert
        v-if="periodBlockers.length"
        type="warning"
        show-icon
        message="关账检查存在阻断项"
      >
        <template #description
          ><ul>
            <li v-for="item in periodBlockers" :key="item">{{ item }}</li>
          </ul></template
        >
      </a-alert>
      <a-form layout="vertical" class="modal-form-gap">
        <a-form-item label="期间"
          ><a-input
            :value="
              selectedPeriod
                ? `${selectedPeriod.fiscalYear}-${String(selectedPeriod.periodNo).padStart(2, '0')}`
                : ''
            "
            disabled
        /></a-form-item>
        <a-form-item
          label="操作原因"
          :required="periodAction === 'reopen' || periodBlockers.length > 0"
          ><a-textarea v-model:value="periodReason" :rows="4" :maxlength="500"
        /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="reconcileModalOpen"
      :title="unreconcileMode ? '解除银行匹配' : '银行流水匹配'"
      :confirm-loading="submitting"
      @ok="submitReconciliation"
    >
      <a-form layout="vertical">
        <template v-if="!unreconcileMode">
          <a-form-item label="业务类型" required
            ><a-segmented
              v-model:value="reconcileForm.businessType"
              :options="[
                { label: '收款', value: 'RECEIPT' },
                { label: '付款', value: 'PAYMENT' },
              ]"
              block
          /></a-form-item>
          <a-form-item label="业务记录ID" required
            ><a-input v-model:value="reconcileForm.businessId"
          /></a-form-item>
          <a-form-item label="业务单号" required
            ><a-input v-model:value="reconcileForm.businessNo"
          /></a-form-item>
        </template>
        <a-form-item label="匹配说明" :required="unreconcileMode"
          ><a-textarea
            v-model:value="reconcileForm.note"
            :rows="4"
            :maxlength="500"
        /></a-form-item>
      </a-form>
    </a-modal>

    <a-drawer v-model:open="historyDrawerOpen" title="治理操作轨迹" width="520">
      <h3 class="history-title">{{ historyTitle }}</h3>
      <a-timeline v-if="historyActions.length">
        <a-timeline-item v-for="item in historyActions" :key="item.id">
          <strong>{{ actionLabel(item.actionType) }}</strong>
          <span class="history-meta"
            >{{ item.operatorName }} ·
            {{ formatDateTime(item.createdAt) }}</span
          >
          <a-tag v-if="item.fromStatus || item.toStatus"
            >{{ item.fromStatus || "-" }} → {{ item.toStatus || "-" }}</a-tag
          >
          <p v-if="item.note">{{ item.note }}</p>
        </a-timeline-item>
      </a-timeline>
      <a-empty v-else description="暂无操作轨迹" />
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { Badge, message } from "ant-design-vue";
import CalendarOutlined from "@ant-design/icons-vue/CalendarOutlined";
import DownloadOutlined from "@ant-design/icons-vue/DownloadOutlined";
import PlusOutlined from "@ant-design/icons-vue/PlusOutlined";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import UploadOutlined from "@ant-design/icons-vue/UploadOutlined";
import { useAuthStore } from "@/stores/auth";
import { todayLocal } from "@/utils/date";
import {
  closeAccountingPeriod,
  createControl,
  getCloseReadiness,
  getGovernanceOverview,
  importBankLines,
  listAccountingPeriods,
  listBankLines,
  listControlExceptions,
  listControls,
  listControlTypes,
  listGovernanceActions,
  openAccountingPeriod,
  reconcileBankLine,
  reopenAccountingPeriod,
  reviewControl,
  transitionControl,
  unreconcileBankLine,
  updateControl,
  type AccountingPeriod,
  type BankImportLine,
  type BankLine,
  type ControlException,
  type ControlPayload,
  type ControlRecord,
  type ControlStatus,
  type ControlTypeOption,
  type GovernanceOverview,
  type GovernanceAction,
  type ReconciliationStatus,
} from "@/api/governance";

type ControlForm = ControlPayload & { detailsText: string };
const auth = useAuthStore();
const canManage = computed(() => auth.can("governance:manage"));
const canClosePeriod = computed(() => auth.can("governance:period:close"));
const canReconcile = computed(() => auth.can("governance:bank:reconcile"));
const loading = ref(false);
const submitting = ref(false);
const activeTab = ref("exceptions");
const controls = ref<ControlRecord[]>([]);
const exceptions = ref<ControlException[]>([]);
const periods = ref<AccountingPeriod[]>([]);
const bankLines = ref<BankLine[]>([]);
const controlTypes = ref<ControlTypeOption[]>([]);
const bankStatus = ref<ReconciliationStatus>();
const filters = reactive<{
  keyword?: string;
  type?: string;
  status?: ControlStatus;
}>({});
const overview = reactive<GovernanceOverview>({
  totalControls: 0,
  activeControls: 0,
  blockedControls: 0,
  overdueControls: 0,
  highRiskControls: 0,
  budgetAmount: 0,
  committedAmount: 0,
  actualAmount: 0,
  forecastAmount: 0,
  forecastVariance: 0,
  unmatchedBankLines: 0,
  matchedBankLines: 0,
  closedPeriods: 0,
  domains: [],
});

const controlModalOpen = ref(false);
const editingControl = ref<ControlRecord>();
const emptyControlForm = (): ControlForm => ({
  controlType: "CONTRACT_MILESTONE",
  name: "",
  owner: "",
  riskLevel: "LOW",
  budgetAmount: 0,
  committedAmount: 0,
  actualAmount: 0,
  forecastAmount: 0,
  progressPercent: 0,
  detailsText: "{}",
});
const controlForm = reactive<ControlForm>(emptyControlForm());
const transitionModalOpen = ref(false);
const transitionRecord = ref<ControlRecord>();
const transitionForm = reactive<{ status: ControlStatus; note: string }>({
  status: "ACTIVE",
  note: "",
});
const periodModalOpen = ref(false);
const periodAction = ref<"close" | "reopen">("close");
const selectedPeriod = ref<AccountingPeriod>();
const periodBlockers = ref<string[]>([]);
const periodReason = ref("");
const reconcileModalOpen = ref(false);
const selectedBankLine = ref<BankLine>();
const unreconcileMode = ref(false);
const reconcileForm = reactive({
  businessType: "RECEIPT",
  businessId: "",
  businessNo: "",
  note: "",
});
const historyDrawerOpen = ref(false);
const historyTitle = ref("");
const historyActions = ref<GovernanceAction[]>([]);
const registerDomain = ref<"CONTRACT" | "PROJECT" | "ASSET">("CONTRACT");
const registerDomainOptions = [
  { label: "合同履约", value: "CONTRACT" },
  { label: "项目 WBS / EAC", value: "PROJECT" },
  { label: "固定资产", value: "ASSET" },
];
const registerTypes: Record<string, string[]> = {
  CONTRACT: [
    "CONTRACT_MILESTONE",
    "REVENUE_OBLIGATION",
    "CONTRACT_CHANGE",
    "WARRANTY_RENEWAL",
  ],
  PROJECT: [
    "PROJECT_WBS",
    "PROJECT_FORECAST",
    "PROJECT_CLOSEOUT",
    "RESOURCE_CAPACITY",
  ],
  ASSET: ["FIXED_ASSET"],
};
const registerTitle = computed(
  () =>
    ({ CONTRACT: "合同履约项", PROJECT: "项目控制项", ASSET: "固定资产" })[
      registerDomain.value
    ],
);
const registerControls = computed(() =>
  controls.value.filter((item) =>
    registerTypes[registerDomain.value].includes(item.controlType),
  ),
);
const registerActiveCount = computed(
  () =>
    registerControls.value.filter((item) =>
      ["ACTIVE", "BLOCKED"].includes(item.status),
    ).length,
);
const registerBudget = computed(() =>
  registerControls.value.reduce(
    (sum, item) => sum + Number(item.budgetAmount || 0),
    0,
  ),
);
const registerVariance = computed(() =>
  registerControls.value.reduce(
    (sum, item) =>
      sum + Number(item.forecastAmount || 0) - Number(item.budgetAmount || 0),
    0,
  ),
);
const forecastTypes = [
  "CASH_FORECAST",
  "PROJECT_FORECAST",
  "BUSINESS_FORECAST",
];
const forecastControls = computed(() =>
  controls.value.filter((item) => forecastTypes.includes(item.controlType)),
);
const forecastBudget = computed(() =>
  forecastControls.value.reduce(
    (sum, item) => sum + Number(item.budgetAmount || 0),
    0,
  ),
);
const forecastAmount = computed(() =>
  forecastControls.value.reduce(
    (sum, item) => sum + Number(item.forecastAmount || 0),
    0,
  ),
);
const forecastVariance = computed(
  () => forecastAmount.value - forecastBudget.value,
);

const typeOptions = computed(() =>
  controlTypes.value.map((item) => ({
    value: item.type,
    label: `${domainLabel(item.domain)} · ${item.label}`,
  })),
);
const statusOptions = [
  { value: "DRAFT", label: "草稿" },
  { value: "ACTIVE", label: "执行中" },
  { value: "BLOCKED", label: "阻塞" },
  { value: "COMPLETED", label: "已完成" },
  { value: "CANCELLED", label: "已取消" },
];
const bankStatusOptions = [
  { value: "UNMATCHED", label: "未匹配" },
  { value: "SUGGESTED", label: "待确认" },
  { value: "MATCHED", label: "已匹配" },
];
const riskOptions = [
  { value: "LOW", label: "低" },
  { value: "MEDIUM", label: "中" },
  { value: "HIGH", label: "高" },
];
const amountFields: {
  key: "budgetAmount" | "committedAmount" | "actualAmount" | "forecastAmount";
  label: string;
}[] = [
  { key: "budgetAmount", label: "预算/基准（含税，元）" },
  { key: "committedAmount", label: "承诺金额（含税，元）" },
  { key: "actualAmount", label: "实际金额（含税，元）" },
  { key: "forecastAmount", label: "预测金额（含税，元）" },
];
const transitionOptions = computed(() => {
  const status = transitionRecord.value?.status;
  if (status === "DRAFT")
    return [
      { value: "ACTIVE", label: "启用" },
      { value: "CANCELLED", label: "取消" },
    ];
  if (status === "ACTIVE")
    return [
      { value: "BLOCKED", label: "标记阻塞" },
      { value: "COMPLETED", label: "完成" },
      { value: "CANCELLED", label: "取消" },
    ];
  return [
    { value: "ACTIVE", label: "解除阻塞" },
    { value: "CANCELLED", label: "取消" },
  ];
});

const exceptionColumns = [
  { title: "风险", key: "severity", width: 90 },
  { title: "控制事项", key: "name", width: 300 },
  { title: "类型", key: "type", width: 210 },
  { title: "负责人", dataIndex: "owner", width: 110 },
  { title: "到期日", dataIndex: "dueDate", width: 120 },
  {
    title: "金额敞口（元，税价随来源单据）",
    key: "exposure",
    width: 240,
    align: "right",
  },
  { title: "操作", key: "action", width: 80, fixed: "right" },
];
const controlColumns = [
  { title: "控制事项", key: "control", width: 300 },
  { title: "类型", key: "type", width: 230 },
  { title: "状态", key: "status", width: 100 },
  { title: "风险", key: "risk", width: 90 },
  { title: "负责人", dataIndex: "owner", width: 110 },
  { title: "计划完成", dataIndex: "plannedEnd", width: 120 },
  { title: "预算 / 执行（含税，元）", key: "amounts", width: 250 },
  { title: "进度", key: "progress", width: 150 },
  { title: "操作", key: "action", width: 240, fixed: "right" },
];
const periodColumns = [
  { title: "会计期间", key: "period" },
  { title: "状态", key: "status" },
  { title: "关账时间", dataIndex: "closedAt" },
  { title: "最近操作人", key: "operator" },
  { title: "关账/反结账原因", key: "reason" },
  { title: "操作", key: "action", width: 160 },
];
const bankColumns = [
  { title: "交易日期", dataIndex: "transactionDate", width: 120 },
  { title: "方向", key: "direction", width: 90 },
  {
    title: "流水金额（元，税价不适用）",
    key: "amount",
    width: 220,
    align: "right",
  },
  { title: "对方户名", dataIndex: "counterparty", width: 180 },
  { title: "银行流水号", dataIndex: "bankReference", width: 180 },
  { title: "状态", key: "status", width: 100 },
  { title: "匹配业务", key: "match", width: 180 },
  { title: "操作", key: "action", width: 160, fixed: "right" },
];
const registerColumns = [
  { title: "业务事项", key: "business", width: 300 },
  { title: "负责人", dataIndex: "owner", width: 120 },
  { title: "计划完成", dataIndex: "plannedEnd", width: 120 },
  { title: "状态", key: "status", width: 100 },
  { title: "预测/基准", key: "amount", width: 210 },
  { title: "执行进度", key: "progress", width: 170 },
  { title: "操作", key: "action", fixed: "right", width: 110 },
];
const forecastColumns = [
  { title: "预测事项", key: "forecast", width: 280 },
  { title: "负责人", dataIndex: "owner", width: 110 },
  { title: "预算基准", key: "budget", width: 140 },
  { title: "实际发生", key: "actual", width: 140 },
  { title: "最新预测", key: "forecastAmount", width: 140 },
  { title: "预测偏差", key: "variance", width: 140 },
  { title: "风险", key: "risk", width: 90 },
  { title: "操作", key: "action", fixed: "right", width: 100 },
];

async function loadAll() {
  loading.value = true;
  try {
    const [summary, types, rows, issues, periodRows, bankRows] =
      await Promise.all([
        getGovernanceOverview(),
        listControlTypes(),
        listControls(filters),
        listControlExceptions(),
        listAccountingPeriods(),
        listBankLines(bankStatus.value),
      ]);
    Object.assign(overview, summary);
    controlTypes.value = types;
    controls.value = rows;
    exceptions.value = issues;
    periods.value = periodRows;
    bankLines.value = bankRows;
  } catch (error) {
    message.error((error as Error).message);
  } finally {
    loading.value = false;
  }
}
async function loadControls() {
  try {
    controls.value = await listControls(filters);
  } catch (error) {
    message.error((error as Error).message);
  }
}
async function loadBankLines() {
  try {
    bankLines.value = await listBankLines(bankStatus.value);
  } catch (error) {
    message.error((error as Error).message);
  }
}
async function openHistory(
  entityType: string,
  entityId: string,
  title: string,
) {
  historyTitle.value = title;
  historyDrawerOpen.value = true;
  try {
    historyActions.value = await listGovernanceActions(entityType, entityId);
  } catch (error) {
    historyActions.value = [];
    message.error((error as Error).message);
  }
}
function selectDomain(domain: string) {
  const first = controlTypes.value.find((item) => item.domain === domain);
  filters.type = first?.type;
  activeTab.value = "controls";
  void loadControls();
}
function focusControl(id: string) {
  filters.keyword =
    controls.value.find((item) => item.id === id)?.controlCode || id;
  activeTab.value = "controls";
  void loadControls();
}

function openControlModal(record?: ControlRecord) {
  editingControl.value = record;
  Object.assign(
    controlForm,
    emptyControlForm(),
    record
      ? {
          ...record,
          detailsText: JSON.stringify(record.details || {}, null, 2),
        }
      : {},
  );
  controlModalOpen.value = true;
}
function openRegisterControl() {
  openControlModal();
  controlForm.controlType = registerTypes[registerDomain.value][0];
}
function forecastRowVariance(record: ControlRecord) {
  return Number(record.forecastAmount || 0) - Number(record.budgetAmount || 0);
}
async function saveControl() {
  if (
    !controlForm.controlType ||
    !controlForm.name.trim() ||
    !controlForm.owner.trim()
  ) {
    message.warning("请完整填写控制类型、事项和负责人");
    return;
  }
  let details: Record<string, unknown>;
  try {
    details = controlForm.detailsText.trim()
      ? JSON.parse(controlForm.detailsText)
      : {};
    if (Array.isArray(details) || typeof details !== "object")
      throw new Error();
  } catch {
    message.error("扩展业务数据必须是有效的 JSON 对象");
    return;
  }
  const { detailsText: _detailsText, ...payload } = controlForm;
  submitting.value = true;
  try {
    if (editingControl.value)
      await updateControl(editingControl.value.id, { ...payload, details });
    else await createControl({ ...payload, details });
    message.success(editingControl.value ? "控制项已更新" : "控制项已创建");
    controlModalOpen.value = false;
    await loadAll();
  } catch (error) {
    message.error((error as Error).message);
  } finally {
    submitting.value = false;
  }
}
function openTransitionModal(record: ControlRecord) {
  transitionRecord.value = record;
  transitionForm.status = transitionOptions.value[0].value as ControlStatus;
  transitionForm.note = "";
  transitionModalOpen.value = true;
}
async function submitTransition() {
  if (!transitionRecord.value) return;
  submitting.value = true;
  try {
    await transitionControl(
      transitionRecord.value.id,
      transitionForm.status,
      transitionForm.note,
    );
    message.success("控制状态已更新");
    transitionModalOpen.value = false;
    await loadAll();
  } catch (error) {
    message.error((error as Error).message);
  } finally {
    submitting.value = false;
  }
}
async function handleReview(record: ControlRecord) {
  try {
    await reviewControl(
      record.id,
      todayLocal(),
      "周期复核完成",
    );
    message.success("复核日期已更新");
    await loadAll();
  } catch (error) {
    message.error((error as Error).message);
  }
}

async function handleOpenCurrentPeriod() {
  const now = new Date();
  try {
    await openAccountingPeriod(now.getFullYear(), now.getMonth() + 1);
    message.success("本期已开启");
    await loadAll();
  } catch (error) {
    message.error((error as Error).message);
  }
}
async function prepareClose(period: AccountingPeriod) {
  try {
    const readiness = await getCloseReadiness(
      period.fiscalYear,
      period.periodNo,
    );
    selectedPeriod.value = period;
    periodBlockers.value = readiness.blockers;
    periodReason.value = period.actionRequestReason || "";
    periodAction.value = "close";
    periodModalOpen.value = true;
  } catch (error) {
    message.error((error as Error).message);
  }
}
function prepareReopen(period: AccountingPeriod) {
  selectedPeriod.value = period;
  periodBlockers.value = [];
  periodReason.value = period.actionRequestReason || "";
  periodAction.value = "reopen";
  periodModalOpen.value = true;
}
async function submitPeriodAction() {
  const period = selectedPeriod.value;
  if (!period) return;
  if (
    (periodAction.value === "reopen" || periodBlockers.value.length > 0) &&
    periodReason.value.trim().length < 5
  ) {
    message.warning("请填写至少 5 个字的操作原因");
    return;
  }
  submitting.value = true;
  try {
    const result =
      periodAction.value === "close"
        ? await closeAccountingPeriod(
            period.fiscalYear,
            period.periodNo,
            periodBlockers.value.length > 0,
            periodReason.value,
          )
        : await reopenAccountingPeriod(
            period.fiscalYear,
            period.periodNo,
            periodReason.value,
          );
    message.success(
      result.pendingAction
        ? "申请已提交，等待另一位财务人员复核"
        : periodAction.value === "close"
          ? "会计期间已关账"
          : "会计期间已反结账",
    );
    periodModalOpen.value = false;
    await loadAll();
  } catch (error) {
    message.error((error as Error).message);
  } finally {
    submitting.value = false;
  }
}

function openReconcileModal(line: BankLine) {
  selectedBankLine.value = line;
  unreconcileMode.value = false;
  Object.assign(reconcileForm, {
    businessType:
      line.matchedBizType || (line.direction === "IN" ? "RECEIPT" : "PAYMENT"),
    businessId: line.matchedBizId || "",
    businessNo: line.matchedBizNo || "",
    note: line.reconciliationStatus === "SUGGESTED" ? "确认系统候选匹配" : "",
  });
  reconcileModalOpen.value = true;
}
function openUnreconcileModal(line: BankLine) {
  selectedBankLine.value = line;
  unreconcileMode.value = true;
  Object.assign(reconcileForm, {
    businessType: line.matchedBizType || "RECEIPT",
    businessId: line.matchedBizId || "",
    businessNo: line.matchedBizNo || "",
    note: "",
  });
  reconcileModalOpen.value = true;
}
async function submitReconciliation() {
  const line = selectedBankLine.value;
  if (!line) return;
  if (unreconcileMode.value && !reconcileForm.note.trim()) {
    message.warning("请填写解除原因");
    return;
  }
  if (
    !unreconcileMode.value &&
    (!reconcileForm.businessId.trim() || !reconcileForm.businessNo.trim())
  ) {
    message.warning("请填写业务记录 ID 和单号");
    return;
  }
  submitting.value = true;
  try {
    if (unreconcileMode.value)
      await unreconcileBankLine(line.id, reconcileForm.note);
    else await reconcileBankLine(line.id, reconcileForm);
    message.success(
      unreconcileMode.value ? "银行匹配已解除" : "银行流水已匹配",
    );
    reconcileModalOpen.value = false;
    await loadAll();
  } catch (error) {
    message.error((error as Error).message);
  } finally {
    submitting.value = false;
  }
}
async function handleBankFile(file: File) {
  try {
    const rows = parseCsv(await file.text());
    if (rows.length < 2) throw new Error("CSV 文件没有可导入的流水");
    const headers = rows[0];
    const required = [
      "accountNoMasked",
      "transactionDate",
      "direction",
      "amount",
      "bankReference",
    ];
    if (required.some((key) => !headers.includes(key)))
      throw new Error("CSV 表头不完整，请使用导入模板");
    const lines = rows
      .slice(1)
      .filter((row) => row.some(Boolean))
      .map((row) =>
        Object.fromEntries(
          headers.map((key, index) => [key, row[index] || ""]),
        ),
      )
      .map(
        (item) =>
          ({
            accountNoMasked: item.accountNoMasked,
            transactionDate: item.transactionDate,
            direction: item.direction.toUpperCase() as "IN" | "OUT",
            amount: Number(item.amount),
            counterparty: item.counterparty || undefined,
            bankReference: item.bankReference,
            summary: item.summary || undefined,
          }) satisfies BankImportLine,
      );
    if (
      lines.some(
        (item) =>
          !item.accountNoMasked ||
          !item.transactionDate ||
          !item.bankReference ||
          !Number.isFinite(item.amount) ||
          item.amount <= 0,
      )
    )
      throw new Error("CSV 中存在账号、日期、金额或流水号不合法的记录");
    const result = await importBankLines(lines);
    message.success(
      `导入 ${result.imported} 条，重复 ${result.duplicates} 条，候选匹配 ${result.suggested} 条`,
    );
    await loadAll();
  } catch (error) {
    message.error((error as Error).message);
  }
  return false;
}
function parseCsv(text: string) {
  const content = text.replace(/^\uFEFF/, "");
  const rows: string[][] = [];
  let row: string[] = [];
  let cell = "";
  let quoted = false;
  for (let i = 0; i < content.length; i += 1) {
    const char = content[i];
    if (char === '"') {
      if (quoted && content[i + 1] === '"') {
        cell += '"';
        i += 1;
      } else quoted = !quoted;
    } else if (char === "," && !quoted) {
      row.push(cell.trim());
      cell = "";
    } else if ((char === "\n" || char === "\r") && !quoted) {
      if (char === "\r" && content[i + 1] === "\n") i += 1;
      row.push(cell.trim());
      if (row.some(Boolean)) rows.push(row);
      row = [];
      cell = "";
    } else cell += char;
  }
  row.push(cell.trim());
  if (row.some(Boolean)) rows.push(row);
  return rows;
}
function downloadBankTemplate() {
  const csv =
    "accountNoMasked,transactionDate,direction,amount,counterparty,bankReference,summary\n6222****1234,2026-07-26,IN,1000.00,示例客户,BANK-001,项目回款\n";
  const url = URL.createObjectURL(
    new Blob(["\uFEFF" + csv], { type: "text/csv;charset=utf-8" }),
  );
  const link = document.createElement("a");
  link.href = url;
  link.download = "bank-statement-template.csv";
  link.click();
  URL.revokeObjectURL(url);
}

const domainNames: Record<string, string> = {
  FINANCE: "财务",
  CRM: "合同客户",
  PROJECT: "项目",
  PROCUREMENT: "采购",
  INVENTORY: "库存",
  ASSET: "资产",
  SERVICE: "服务",
  MASTER_DATA: "主数据",
  BI: "经营预测",
};
const statusNames: Record<string, string> = {
  DRAFT: "草稿",
  ACTIVE: "执行中",
  BLOCKED: "阻塞",
  COMPLETED: "已完成",
  CANCELLED: "已取消",
};
function domainLabel(value: string) {
  return domainNames[value] || value;
}
function typeLabel(value: string) {
  return controlTypes.value.find((item) => item.type === value)?.label || value;
}
function statusLabel(value: string) {
  return statusNames[value] || value;
}
function riskLabel(value: string) {
  return (
    ({ LOW: "低", MEDIUM: "中", HIGH: "高" } as Record<string, string>)[
      value
    ] || value
  );
}
function riskColor(value: string) {
  return value === "HIGH" ? "red" : value === "MEDIUM" ? "orange" : "green";
}
function statusColor(value: string) {
  return (
    (
      {
        DRAFT: "default",
        ACTIVE: "blue",
        BLOCKED: "red",
        COMPLETED: "green",
        CANCELLED: "default",
      } as Record<string, string>
    )[value] || "default"
  );
}
function bankStatusLabel(value: string) {
  return (
    (
      { UNMATCHED: "未匹配", SUGGESTED: "待确认", MATCHED: "已匹配" } as Record<
        string,
        string
      >
    )[value] || value
  );
}
function bankStatusColor(value: string) {
  return value === "MATCHED"
    ? "green"
    : value === "SUGGESTED"
      ? "blue"
      : "orange";
}
function periodStatusLabel(value: string) {
  return (
    (
      { OPEN: "开放", CLOSING: "关账中", CLOSED: "已关账" } as Record<
        string,
        string
      >
    )[value] || value
  );
}
function periodStatusColor(value: string) {
  return value === "OPEN" ? "green" : value === "CLOSED" ? "default" : "orange";
}
function isFinal(value: string) {
  return value === "COMPLETED" || value === "CANCELLED";
}
function formatMoney(value: number) {
  return `¥${Number(value || 0).toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}
function moneyFormatter(value: string | number) {
  return formatMoney(Number(value));
}
function dangerStyle(value: number) {
  return Number(value) > 0 ? { color: "#cf1322" } : { color: "#237804" };
}
function actionLabel(value: string) {
  return (
    (
      {
        CREATE: "创建",
        UPDATE: "更新",
        TRANSITION: "状态推进",
        REVIEW: "周期复核",
        OPEN: "开启期间",
        CLOSE: "关账",
        REOPEN: "反结账",
        RECONCILE: "完成匹配",
        UNRECONCILE: "解除匹配",
      } as Record<string, string>
    )[value] || value
  );
}
function formatDateTime(value: string) {
  return value
    ? new Date(value).toLocaleString("zh-CN", { hour12: false })
    : "-";
}

onMounted(loadAll);
</script>

<style scoped>
.governance-domain-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 10px;
}
.governance-domain-grid button {
  min-height: 86px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  text-align: left;
  cursor: pointer;
}
.governance-domain-grid button:hover {
  border-color: #1677ff;
}
.governance-domain-grid span,
.governance-domain-grid small {
  display: block;
  color: #64748b;
}
.governance-domain-grid strong {
  display: block;
  margin: 6px 0 4px;
  font-size: 20px;
}
.governance-domain-grid .danger {
  color: #cf1322;
}
.table-subtitle {
  display: block;
  margin-top: 3px;
  color: #64748b;
  font-size: 12px;
}
.tab-command-bar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}
.register-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}
.register-metrics {
  margin-bottom: 16px;
}
.forecast-note {
  margin-bottom: 14px;
}
.forecast-kpis {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.forecast-kpis > div {
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}
.forecast-kpis span {
  display: block;
  color: #64748b;
  font-size: 12px;
}
.forecast-kpis strong {
  display: block;
  margin-top: 4px;
  font-size: 20px;
}
.forecast-kpis .danger,
.variance-danger {
  color: #cf1322;
}
.modal-form-gap {
  margin-top: 16px;
}
.modal-form-gap ul {
  margin: 0;
  padding-left: 18px;
}
.history-title {
  margin: 0 0 24px;
  font-size: 16px;
}
.history-meta {
  display: block;
  margin: 3px 0 8px;
  color: #64748b;
  font-size: 12px;
}
@media (max-width: 768px) {
  .governance-domain-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .tab-command-bar {
    justify-content: flex-start;
  }
  .register-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
  .forecast-kpis {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
