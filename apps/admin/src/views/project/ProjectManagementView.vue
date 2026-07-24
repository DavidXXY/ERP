<template>
  <div class="page-stack">
    <a-card :title="pageTitle">
      <template #extra>
        <a-space>
          <a-button :loading="loading" @click="loadData">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
          <a-button
            v-if="auth.can('project:create')"
            type="primary"
            @click="openCreate"
          >
            <template #icon><PlusOutlined /></template>
            新增项目
          </a-button>
        </a-space>
      </template>

      <a-alert
        v-if="errorMessage"
        class="section-alert"
        type="warning"
        show-icon
        :message="errorMessage"
      />

      <a-row :gutter="[16, 16]" class="metric-row">
        <a-col :xs="12" :lg="6"
          ><a-statistic
            title="合同总额"
            :value="totalContract"
            :formatter="moneyFormatter"
        /></a-col>
        <a-col :xs="12" :lg="6"
          ><a-statistic
            title="项目预算"
            :value="totalBudget"
            :formatter="moneyFormatter"
        /></a-col>
        <a-col :xs="12" :lg="6"
          ><a-statistic
            title="已归集成本"
            :value="totalActualCost"
            :formatter="moneyFormatter"
        /></a-col>
        <a-col :xs="12" :lg="6"
          ><a-statistic
            title="高风险项目"
            :value="highRiskProjectCount"
            suffix="个"
            :value-style="{
              color: highRiskProjectCount > 0 ? '#ff4d4f' : '#52c41a',
            }"
        /></a-col>
      </a-row>

      <template v-if="pageMode === 'list'">
        <section class="project-workbench">
          <div class="workbench-title">
            <div>
              <h3>项目利润复盘</h3>
              <p>
                关闭项目自动进入复盘视角，核对合同额、预算、实际成本和毛利。
              </p>
            </div>
            <a-tag :color="closedReviewRows.length ? 'blue' : 'default'"
              >已关闭 {{ closedReviewRows.length }} 个</a-tag
            >
          </div>
          <div class="project-summary-grid">
            <button
              v-for="item in profitReviewCards"
              :key="item.label"
              class="summary-card"
              type="button"
              @click="item.action"
            >
              <span>{{ item.label }}</span>
              <strong :class="{ 'text-danger': item.danger }">{{
                item.value
              }}</strong>
              <em>{{ item.hint }}</em>
            </button>
          </div>
        </section>
        <a-space wrap class="table-toolbar">
          <a-input-search
            v-model:value="keyword"
            allow-clear
            placeholder="搜索项目、客户、负责人"
            style="width: 260px"
          />
          <a-select
            v-model:value="approvalFilter"
            :options="approvalFilterOptions"
            style="width: 140px"
          />
          <a-select
            v-model:value="stageFilter"
            :options="stageFilterOptions"
            style="width: 140px"
          />
          <a-tag color="orange">待分配 {{ pendingApprovalCount }}</a-tag>
        </a-space>

        <a-table
          :columns="columns"
          :data-source="filteredProjects"
          :loading="loading"
          :pagination="{ pageSize: 8 }"
          :row-key="(record: Project) => record.id"
          :scroll="{ x: 1490 }"
          size="middle"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'">
              <a-button
                type="link"
                class="table-link"
                @click="router.push(`/projects/${record.id}`)"
                >{{ record.name }}</a-button
              >
              <span class="table-subtitle"
                >{{ record.code }} ·
                {{ record.customerName || "未关联客户" }}</span
              >
            </template>
            <template v-else-if="column.key === 'owner'">
              {{ projectTypeLabel(record.projectType) }}
            </template>
            <template v-else-if="column.key === 'manager'">
              <strong>{{ record.managerName || "待分配" }}</strong>
            </template>
            <template v-else-if="column.key === 'stage'">
              <a-tag :color="stageColor(record.stage)">{{
                stageLabel(record.stage)
              }}</a-tag>
            </template>
            <template v-else-if="column.key === 'approval'">
              <a-tag :color="approvalColor(record.approvalStatus)">{{
                approvalLabel(record.approvalStatus)
              }}</a-tag>
              <span v-if="record.approverName" class="table-subtitle"
                >{{ record.approverName }} · {{ record.approvalComment }}</span
              >
            </template>
            <template v-else-if="column.key === 'contract'"
              ><strong>{{
                formatMoney(record.contractAmount)
              }}</strong></template
            >
            <template v-else-if="column.key === 'cost'">
              {{ formatMoney(record.budgetAmount) }}
              <span class="table-subtitle"
                >实际 {{ formatMoney(record.actualCost) }}</span
              >
            </template>
            <template v-else-if="column.key === 'gross'">
              <strong :class="{ 'text-danger': record.grossMargin < 0 }">{{
                formatMoney(record.grossMargin)
              }}</strong>
              <span class="table-subtitle"
                >预算余额 {{ formatMoney(record.budgetVariance) }}</span
              >
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space size="small">
                <a-button
                  type="link"
                  size="small"
                  @click="router.push(`/projects/${record.id}`)"
                  >详情</a-button
                >
                <a-popconfirm
                  v-if="auth.can('project:delete')"
                  title="确认删除该项目？相关预算、成本、采购、领料、费用和工单记录也会同步清理。"
                  @confirm="removeProject(record)"
                >
                  <a-button danger type="link" size="small">删除</a-button>
                </a-popconfirm>
                <a-button
                  v-if="
                    auth.can('project:approve') &&
                    record.approvalStatus === 'PENDING'
                  "
                  type="link"
                  size="small"
                  @click="openApproval(record)"
                >
                  分配
                </a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </template>

      <template v-else-if="pageMode === 'budget'">
        <section class="project-workbench">
          <div class="workbench-title">
            <div>
              <h3>预算执行概览</h3>
              <p>按项目跟踪预算使用、实际成本、预算余额和利润风险。</p>
            </div>
            <a-tag :color="budgetOverrunCount > 0 ? 'red' : 'green'"
              >超预算 {{ budgetOverrunCount }} 个</a-tag
            >
          </div>
          <div class="project-summary-grid">
            <button
              v-for="item in budgetCards"
              :key="item.label"
              class="summary-card"
              type="button"
              @click="item.action"
            >
              <span>{{ item.label }}</span>
              <strong :class="{ 'text-danger': item.danger }">{{
                item.value
              }}</strong>
              <em>{{ item.hint }}</em>
            </button>
          </div>
        </section>
        <a-table
          :columns="budgetExecutionColumns"
          :data-source="budgetExecutionRows"
          :loading="loading"
          :pagination="{ pageSize: 10 }"
          :row-key="(record: ProjectBudgetRow) => record.project.id"
          :scroll="{ x: 1120 }"
          size="middle"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'project'">
              <a-button
                type="link"
                class="table-link"
                @click="openDetail(record.project)"
                >{{ record.project.name }}</a-button
              >
              <span class="table-subtitle"
                >{{ record.project.code }} ·
                {{ record.project.managerName }}</span
              >
            </template>
            <template v-else-if="column.key === 'budget'">
              <strong>{{ formatMoney(record.project.budgetAmount) }}</strong>
              <span class="table-subtitle"
                >合同 {{ formatMoney(record.project.contractAmount) }}</span
              >
            </template>
            <template v-else-if="column.key === 'actual'">
              {{ formatMoney(record.project.actualCost) }}
              <a-progress
                :percent="Math.min(record.usageRate, 100)"
                size="small"
                :show-info="false"
                :stroke-color="
                  record.usageRate >= 100
                    ? '#ff4d4f'
                    : record.usageRate >= 85
                      ? '#faad14'
                      : '#52c41a'
                "
              />
            </template>
            <template v-else-if="column.key === 'variance'"
              ><span
                :class="{ 'text-danger': record.project.budgetVariance < 0 }"
                >{{ formatMoney(record.project.budgetVariance) }}</span
              ></template
            >
            <template v-else-if="column.key === 'risk'">
              <a-tag :color="riskColor(record.riskLevel)">{{
                riskLabel(record.riskLevel)
              }}</a-tag>
              <span class="table-subtitle">{{ record.riskMessage }}</span>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button
                type="link"
                size="small"
                @click="openDetail(record.project)"
                >查看预算</a-button
              >
            </template>
          </template>
        </a-table>
      </template>

      <template v-else-if="pageMode === 'presales'">
        <section class="project-workbench">
          <div class="workbench-title">
            <div>
              <h3>售前支持</h3>
              <p>
                销售发起售前支持后，由项目管理填写成本并完成审批；通过后报价板块才可以继续报价。
              </p>
            </div>
            <a-button :loading="loading" @click="loadPreSalesSupport"
              >刷新售前支持</a-button
            >
          </div>
          <div class="project-summary-grid">
            <button class="summary-card" type="button">
              <span>待填写成本</span>
              <strong>{{
                preSalesRows.filter((item) => item.status === "COST_REQUESTED")
                  .length
              }}</strong>
              <em>项目管理待处理</em>
            </button>
            <button class="summary-card" type="button">
              <span>待审批</span>
              <strong>{{
                preSalesRows.filter((item) => item.status === "COSTING").length
              }}</strong>
              <em>成本负责人已提交</em>
            </button>
            <button class="summary-card" type="button">
              <span>已核对</span>
              <strong>{{
                preSalesRows.filter((item) => item.status === "COST_APPROVED")
                  .length
              }}</strong>
              <em>销售可继续报价</em>
            </button>
          </div>
        </section>
        <a-table
          :columns="preSalesColumns"
          :data-source="preSalesRows"
          :loading="loading"
          :pagination="{ pageSize: 10 }"
          :row-key="(record: QuotePlan) => record.id"
          :scroll="{ x: 1280 }"
          size="middle"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'quote'">
              <strong>{{ record.code }}</strong>
              <span class="table-subtitle"
                >{{ record.customerName }} ·
                {{ record.opportunityCode || "未关联商机" }}</span
              >
            </template>
            <template v-else-if="column.key === 'scope'">
              <span class="line-clamp-2">{{ record.serviceScope }}</span>
              <span class="table-subtitle">{{
                record.inspectCycle || "未填写服务周期"
              }}</span>
            </template>
            <template v-else-if="column.key === 'amount'">
              <strong>{{ formatMoney(record.amount) }}</strong>
              <span class="table-subtitle"
                >未税
                {{
                  formatMoney(
                    record.netAmount ||
                      calcNetAmount(record.amount, record.taxRate),
                  )
                }}</span
              >
            </template>
            <template v-else-if="column.key === 'cost'">
              <strong>{{
                formatMoney(
                  record.costRequest?.netTotalCost ||
                    record.budgetAmount ||
                    preSalesCostNetTotal(record.costRequest || {}),
                )
              }}</strong>
              <span class="table-subtitle"
                >含税
                {{
                  formatMoney(
                    record.costRequest?.totalCost ||
                      preSalesCostTotal(record.costRequest || {}),
                  )
                }}</span
              >
              <span class="table-subtitle">{{
                record.costRequest?.projectManager || "未填写成本负责人"
              }}</span>
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="preSalesStatusColor(record.status)">{{
                preSalesStatusLabel(record.status)
              }}</a-tag>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space size="small">
                <a-button
                  v-if="
                    auth.can('project:cost:create') &&
                    record.status === 'COST_REQUESTED'
                  "
                  type="link"
                  size="small"
                  @click="openPreSalesCost(record)"
                  >填写成本</a-button
                >
                <a-button
                  v-if="
                    auth.can('project:approve') && record.status === 'COSTING'
                  "
                  type="link"
                  size="small"
                  @click="openPreSalesApproval(record)"
                  >成本审批</a-button
                >
                <a-tag v-if="record.status === 'COST_APPROVED'" color="green"
                  >销售可报价</a-tag
                >
              </a-space>
            </template>
          </template>
          <template #emptyText>暂无售前支持请求</template>
        </a-table>
      </template>

      <template v-else-if="pageMode === 'costs'">
        <section class="project-workbench">
          <div class="workbench-title">
            <div>
              <h3>成本明细</h3>
              <p>汇总项目成本来源，快速定位人工、材料、外包等成本归集。</p>
            </div>
            <a-button
              :loading="detailHydrating"
              @click="hydrateProjectDetails(true)"
              >刷新明细</a-button
            >
          </div>
          <div class="project-summary-grid">
            <button
              v-for="item in costCards"
              :key="item.label"
              class="summary-card"
              type="button"
              @click="item.action"
            >
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
              <em>{{ item.hint }}</em>
            </button>
          </div>
        </section>
        <a-table
          :columns="flatCostColumns"
          :data-source="flatCostEntries"
          :loading="loading || detailHydrating"
          :pagination="{ pageSize: 12 }"
          :row-key="(record: FlatCostEntry) => record.id"
          :scroll="{ x: 980 }"
          size="middle"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'project'">
              <a-button
                type="link"
                class="table-link"
                @click="openDetail(record.project)"
                >{{ record.project.name }}</a-button
              >
              <span class="table-subtitle">{{ record.project.code }}</span>
            </template>
            <template v-else-if="column.key === 'category'"
              ><a-tag>{{ categoryLabel(record.category) }}</a-tag></template
            >
            <template v-else-if="column.key === 'source'"
              >{{ sourceLabel(record.sourceType)
              }}<span class="table-subtitle">{{
                record.sourceNo || "无来源单号"
              }}</span></template
            >
            <template v-else-if="column.key === 'amount'"
              ><strong>{{ formatMoney(record.amount) }}</strong></template
            >
          </template>
          <template #emptyText>暂无成本明细，可进入项目详情登记成本</template>
        </a-table>
      </template>

      <template v-else>
        <section class="project-workbench">
          <div class="workbench-title">
            <div>
              <h3>项目阶段总览</h3>
              <p>先看项目当前进度，再查看每次阶段推进记录。</p>
            </div>
            <a-button
              :loading="detailHydrating"
              @click="hydrateProjectDetails(true)"
              >刷新履历</a-button
            >
          </div>
          <div class="stage-kanban-grid">
            <button
              class="stage-card"
              :class="{ active: stageFilter === 'ALL' }"
              type="button"
              @click="stageFilter = 'ALL'"
            >
              <span>全部项目</span>
              <strong>{{ projects.length }}</strong>
              <em>{{ formatMoney(totalContract) }}</em>
            </button>
            <button
              v-for="item in stageCards"
              :key="item.stage"
              class="stage-card"
              :class="{ active: stageFilter === item.stage }"
              type="button"
              @click="stageFilter = item.stage"
            >
              <span>{{ item.label }}</span>
              <strong>{{ item.count }}</strong>
              <em>{{ item.amount }}</em>
            </button>
          </div>
        </section>

        <div class="stage-section-header">
          <div>
            <h4>项目当前进度</h4>
            <span>共 {{ stageProjectRows.length }} 个项目</span>
          </div>
          <a-space wrap>
            <a-input-search
              v-model:value="keyword"
              allow-clear
              placeholder="搜索项目、客户、负责人"
              style="width: 240px"
            />
            <a-select
              v-model:value="stageFilter"
              :options="stageFilterOptions"
              style="width: 140px"
            />
          </a-space>
        </div>
        <a-table
          :columns="stageProjectColumns"
          :data-source="stageProjectRows"
          :loading="loading || detailHydrating"
          :pagination="{ pageSize: 10 }"
          :row-key="(record: ProjectStageOverviewRow) => record.project.id"
          :scroll="{ x: 920 }"
          size="middle"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'project'">
              <a-button
                type="link"
                class="table-link"
                @click="openDetail(record.project)"
                >{{ record.project.name }}</a-button
              >
              <span class="table-subtitle"
                >{{ record.project.code || "-" }} ·
                {{ record.project.managerName || "待分配负责人" }}</span
              >
            </template>
            <template v-else-if="column.key === 'stage'">
              <a-tag :color="stageColor(record.project.stage)">{{
                stageLabel(record.project.stage)
              }}</a-tag>
            </template>
            <template v-else-if="column.key === 'progress'">
              <div class="stage-progress-cell">
                <a-progress
                  :percent="normalizedProgress(record.project.progress)"
                  size="small"
                  :status="
                    normalizedProgress(record.project.progress) >= 100
                      ? 'success'
                      : 'active'
                  "
                />
              </div>
            </template>
            <template v-else-if="column.key === 'schedule'">
              {{ record.project.plannedStartDate || "未设置" }}
              <span class="table-subtitle"
                >至 {{ record.project.plannedEndDate || "未设置" }}</span
              >
            </template>
            <template v-else-if="column.key === 'latest'">
              <template v-if="record.latestRecord">
                <span
                  >{{ stageLabel(record.latestRecord.fromStage) }} →
                  {{ stageLabel(record.latestRecord.toStage) }}</span
                >
                <span class="table-subtitle"
                  >{{ record.latestRecord.operatorName }} ·
                  {{ formatDateTime(record.latestRecord.changedAt) }}</span
                >
              </template>
              <span v-else class="text-muted">尚无推进记录</span>
            </template>
          </template>
          <template #emptyText>当前筛选条件下暂无项目</template>
        </a-table>

        <div class="stage-section-header stage-history-header">
          <div>
            <h4>阶段变更记录</h4>
            <span>按时间倒序展示，共 {{ flatStageRecords.length }} 条</span>
          </div>
        </div>
        <a-table
          :columns="flatStageColumns"
          :data-source="flatStageRecords"
          :loading="loading || detailHydrating"
          :pagination="{ pageSize: 12 }"
          :row-key="(record: FlatStageRecord) => record.id"
          :scroll="{ x: 1180 }"
          size="middle"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'time'">
              {{ formatDateTime(record.changedAt) }}
            </template>
            <template v-else-if="column.key === 'project'">
              <a-button
                type="link"
                class="table-link"
                @click="openDetail(record.project)"
                >{{ record.project.name }}</a-button
              >
              <span class="table-subtitle">{{
                record.project.managerName
              }}</span>
            </template>
            <template v-else-if="column.key === 'change'">
              <a-tag :color="stageColor(record.fromStage)">{{
                stageLabel(record.fromStage)
              }}</a-tag>
              →
              <a-tag :color="stageColor(record.toStage)">{{
                stageLabel(record.toStage)
              }}</a-tag>
            </template>
            <template v-else-if="column.key === 'progress'">
              <strong>{{ normalizedProgress(record.progress) }}%</strong>
            </template>
            <template v-else-if="column.key === 'operator'">{{
              record.operatorName || "-"
            }}</template>
          </template>
          <template #emptyText>暂无阶段推进记录</template>
        </a-table>
      </template>
    </a-card>

    <a-modal
      v-model:open="preSalesCostOpen"
      title="填写售前支持成本"
      width="860px"
      :confirm-loading="saving"
      @ok="handlePreSalesCost"
    >
      <a-alert
        v-if="selectedPreSales"
        class="section-alert"
        type="info"
        show-icon
        :message="`${selectedPreSales.code} · ${selectedPreSales.customerName}`"
        :description="`含税成本 ${formatMoney(preSalesCostTotal(preSalesCostForm))}，未税成本 ${formatMoney(preSalesCostNetTotal(preSalesCostForm))}，建议报价 ${formatMoney(preSalesCostForm.suggestedPrice || 0)}`"
      />
      <a-form :model="preSalesCostForm" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :md="12">
            <a-form-item label="成本负责人" required>
              <a-select
                v-model:value="preSalesCostForm.projectManager"
                show-search
                allow-clear
                option-filter-prop="label"
                placeholder="选择成本负责人"
                :options="visibleUserOptions"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12"
            ><a-form-item label="建议报价"
              ><a-input-number
                v-model:value="preSalesCostForm.suggestedPrice"
                :min="0"
                :precision="2"
                class="full-input" /></a-form-item
          ></a-col>
          <a-col :span="24">
            <div class="presales-cost-table">
              <div class="presales-cost-head">
                <span>成本项</span>
                <span>含税价</span>
                <span>不含税价</span>
                <span>税率</span>
              </div>
              <div class="presales-cost-row">
                <strong>人工</strong>
                <a-input-number
                  v-model:value="preSalesCostForm.laborCost"
                  :min="0"
                  :precision="2"
                  class="full-input"
                />
                <em>{{
                  formatMoney(
                    calcNetAmount(
                      preSalesCostForm.laborCost,
                      preSalesCostForm.laborTaxRate,
                    ),
                  )
                }}</em>
                <a-input-number
                  v-model:value="preSalesCostForm.laborTaxRate"
                  :min="0"
                  :max="100"
                  :precision="2"
                  class="full-input"
                />
              </div>
              <div class="presales-cost-row">
                <strong>材料</strong>
                <a-input-number
                  v-model:value="preSalesCostForm.materialCost"
                  :min="0"
                  :precision="2"
                  class="full-input"
                />
                <em>{{
                  formatMoney(
                    calcNetAmount(
                      preSalesCostForm.materialCost,
                      preSalesCostForm.materialTaxRate,
                    ),
                  )
                }}</em>
                <a-input-number
                  v-model:value="preSalesCostForm.materialTaxRate"
                  :min="0"
                  :max="100"
                  :precision="2"
                  class="full-input"
                />
              </div>
              <div class="presales-cost-row">
                <strong>外包</strong>
                <a-input-number
                  v-model:value="preSalesCostForm.subcontractCost"
                  :min="0"
                  :precision="2"
                  class="full-input"
                />
                <em>{{
                  formatMoney(
                    calcNetAmount(
                      preSalesCostForm.subcontractCost,
                      preSalesCostForm.subcontractTaxRate,
                    ),
                  )
                }}</em>
                <a-input-number
                  v-model:value="preSalesCostForm.subcontractTaxRate"
                  :min="0"
                  :max="100"
                  :precision="2"
                  class="full-input"
                />
              </div>
              <div class="presales-cost-row">
                <strong>差旅</strong>
                <a-input-number
                  v-model:value="preSalesCostForm.travelCost"
                  :min="0"
                  :precision="2"
                  class="full-input"
                />
                <em>{{
                  formatMoney(calcNetAmount(preSalesCostForm.travelCost, 0))
                }}</em>
                <span class="tax-static">无税率</span>
              </div>
              <div class="presales-cost-row">
                <strong>设备</strong>
                <a-input-number
                  v-model:value="preSalesCostForm.equipmentCost"
                  :min="0"
                  :precision="2"
                  class="full-input"
                />
                <em>{{
                  formatMoney(
                    calcNetAmount(
                      preSalesCostForm.equipmentCost,
                      preSalesCostForm.equipmentTaxRate,
                    ),
                  )
                }}</em>
                <a-input-number
                  v-model:value="preSalesCostForm.equipmentTaxRate"
                  :min="0"
                  :max="100"
                  :precision="2"
                  class="full-input"
                />
              </div>
              <div class="presales-cost-row">
                <strong>风险金</strong>
                <a-input-number
                  v-model:value="preSalesCostForm.riskReserve"
                  :min="0"
                  :precision="2"
                  class="full-input"
                />
                <em>{{
                  formatMoney(calcNetAmount(preSalesCostForm.riskReserve, 0))
                }}</em>
                <span class="tax-static">无税率</span>
              </div>
              <div class="presales-cost-row">
                <strong>其他</strong>
                <a-input-number
                  v-model:value="preSalesCostForm.otherCost"
                  :min="0"
                  :precision="2"
                  class="full-input"
                />
                <em>{{
                  formatMoney(
                    calcNetAmount(
                      preSalesCostForm.otherCost,
                      preSalesCostForm.otherTaxRate,
                    ),
                  )
                }}</em>
                <a-input-number
                  v-model:value="preSalesCostForm.otherTaxRate"
                  :min="0"
                  :max="100"
                  :precision="2"
                  class="full-input"
                />
              </div>
            </div>
          </a-col>
          <a-col :span="24"
            ><a-form-item label="成本说明"
              ><a-textarea
                v-model:value="preSalesCostForm.costRemark"
                :rows="3" /></a-form-item
          ></a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="preSalesApprovalOpen"
      title="售前支持成本审批"
      width="620px"
      :confirm-loading="saving"
      @ok="handlePreSalesApproval"
    >
      <a-alert
        v-if="selectedPreSales?.costRequest"
        class="section-alert"
        show-icon
        type="info"
        :message="`成本合计 ${formatMoney(selectedPreSales.costRequest.totalCost || 0)} · 建议报价 ${formatMoney(selectedPreSales.costRequest.suggestedPrice || 0)}`"
        :description="
          selectedPreSales.costRequest.costRemark || '成本负责人未填写备注'
        "
      />
      <a-form :model="preSalesApprovalForm" layout="vertical">
        <a-form-item label="审批结论">
          <a-radio-group
            v-model:value="preSalesApprovalForm.decision"
            button-style="solid"
          >
            <a-radio-button value="APPROVED">通过</a-radio-button>
            <a-radio-button value="REJECTED">驳回</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="审批人" required
          ><a-input v-model:value="preSalesApprovalForm.approverName"
        /></a-form-item>
        <a-form-item label="审批意见" required
          ><a-textarea v-model:value="preSalesApprovalForm.comment" :rows="3"
        /></a-form-item>
      </a-form>
    </a-modal>

    <a-drawer v-model:open="detailOpen" :width="920" :destroy-on-close="false">
      <template #title>
        <div v-if="detail" class="detail-title">
          <span>{{ detail.project.name }}</span>
          <a-tag :color="stageColor(detail.project.stage)">{{
            stageLabel(detail.project.stage)
          }}</a-tag>
          <a-tag :color="approvalColor(detail.project.approvalStatus)">{{
            approvalLabel(detail.project.approvalStatus)
          }}</a-tag>
        </div>
        <span v-else>项目详情</span>
      </template>
      <template #extra>
        <a-space v-if="detail">
          <a-button
            v-if="
              auth.can('project:approve') && canAssignManager(detail.project)
            "
            @click="openApproval(detail.project)"
          >
            {{
              detail.project.approvalStatus === "PENDING"
                ? "分配项目经理"
                : "变更负责人"
            }}
          </a-button>
          <a-button
            v-if="auth.can('project:cost:create') && canExecute(detail.project)"
            @click="openCost"
          >
            登记成本
          </a-button>
          <a-button
            v-if="
              auth.can('project:stage:update') && canAdvance(detail.project)
            "
            type="primary"
            @click="openStage"
          >
            推进阶段
          </a-button>
        </a-space>
      </template>

      <a-spin :spinning="detailLoading">
        <template v-if="detail">
          <a-descriptions bordered :column="2" size="small">
            <a-descriptions-item label="项目类型">{{
              projectTypeLabel(detail.project.projectType)
            }}</a-descriptions-item>
            <a-descriptions-item label="客户">{{
              detail.project.customerName
            }}</a-descriptions-item>
            <a-descriptions-item label="项目负责人">{{
              detail.project.managerName
            }}</a-descriptions-item>
            <a-descriptions-item label="现场地址" :span="2">{{
              detail.project.siteAddress
            }}</a-descriptions-item>
            <a-descriptions-item label="计划周期"
              >{{ detail.project.plannedStartDate }} 至
              {{ detail.project.plannedEndDate }}</a-descriptions-item
            >
            <a-descriptions-item label="质保截止">{{
              detail.project.warrantyEndDate || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="合同金额">{{
              formatMoney(detail.project.contractAmount)
            }}</a-descriptions-item>
            <a-descriptions-item label="当前毛利">{{
              formatMoney(detail.project.grossMargin)
            }}</a-descriptions-item>
            <a-descriptions-item
              v-if="detail.project.approvalComment"
              label="分配记录"
              :span="2"
            >
              {{ detail.project.approverName }} ·
              {{ detail.project.approvalComment }}
            </a-descriptions-item>
          </a-descriptions>
          <a-card title="审批进展" size="small" style="margin-top: 12px">
            <ApprovalProgressFlow
              :steps="projectApprovalSteps(detail.project)"
            />
          </a-card>

          <a-row :gutter="[16, 16]" class="drawer-metrics">
            <a-col :span="8"
              ><a-statistic
                title="预算成本"
                :value="detail.project.budgetAmount"
                :formatter="moneyFormatter"
            /></a-col>
            <a-col :span="8"
              ><a-statistic
                title="实际成本"
                :value="detail.project.actualCost"
                :formatter="moneyFormatter"
            /></a-col>
            <a-col :span="8"
              ><a-statistic
                title="预算余额"
                :value="detail.project.budgetVariance"
                :formatter="moneyFormatter"
            /></a-col>
          </a-row>

          <a-tabs>
            <a-tab-pane key="budget" tab="预算执行">
              <div v-if="materialBudgetItem" class="project-inline-actions">
                <a-alert
                  type="info"
                  show-icon
                  :message="`物料预算 ${formatMoney(materialBudgetItem.plannedAmount)}，剩余 ${formatMoney(materialBudgetItem.variance)}`"
                  description="项目物料采购申请会自动归集到当前项目预算。"
                />
                <a-button
                  v-if="auth.can('procurement:purchase:create')"
                  type="primary"
                  @click="openProjectPurchaseRequest"
                >
                  发起采购申请
                </a-button>
              </div>
              <a-table
                :columns="budgetColumns"
                :data-source="detail.budgetItems"
                :pagination="false"
                :row-key="(item: ProjectBudgetItem) => item.id"
                size="small"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'category'">{{
                    categoryLabel(record.category)
                  }}</template>
                  <template v-else-if="column.key === 'planned'">{{
                    formatMoney(record.plannedAmount)
                  }}</template>
                  <template v-else-if="column.key === 'actual'">{{
                    formatMoney(record.actualAmount)
                  }}</template>
                  <template v-else-if="column.key === 'variance'"
                    ><span :class="{ 'text-danger': record.variance < 0 }">{{
                      formatMoney(record.variance)
                    }}</span></template
                  >
                </template>
              </a-table>
            </a-tab-pane>
            <a-tab-pane
              key="costs"
              :tab="`成本明细 (${detail.costEntries.length})`"
            >
              <a-table
                :columns="costColumns"
                :data-source="detail.costEntries"
                :pagination="{ pageSize: 6 }"
                :row-key="(item: ProjectCostEntry) => item.id"
                size="small"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'category'"
                    ><a-tag>{{
                      categoryLabel(record.category)
                    }}</a-tag></template
                  >
                  <template v-else-if="column.key === 'source'"
                    >{{ sourceLabel(record.sourceType)
                    }}<span class="table-subtitle">{{
                      record.sourceNo || "无来源单号"
                    }}</span></template
                  >
                  <template v-else-if="column.key === 'amount'"
                    ><strong>{{ formatMoney(record.amount) }}</strong></template
                  >
                </template>
                <template #emptyText>暂无成本明细</template>
              </a-table>
            </a-tab-pane>
            <a-tab-pane
              key="stages"
              :tab="`阶段履历 (${detail.stageRecords.length})`"
            >
              <a-table
                :columns="stageRecordColumns"
                :data-source="detail.stageRecords"
                :pagination="{ pageSize: 6 }"
                :row-key="(item: ProjectStageRecord) => item.id"
                size="small"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'change'"
                    >{{ stageLabel(record.fromStage) }} →
                    {{ stageLabel(record.toStage) }}</template
                  >
                  <template v-else-if="column.key === 'operator'"
                    >{{ record.operatorName
                    }}<span class="table-subtitle">{{
                      formatDateTime(record.changedAt)
                    }}</span></template
                  >
                </template>
                <template #emptyText>项目尚未推进阶段</template>
              </a-table>
            </a-tab-pane>
            <a-tab-pane key="trace" tab="经营链路">
              <BusinessTraceTimeline :project-detail="detail" />
            </a-tab-pane>
          </a-tabs>
        </template>
      </a-spin>
    </a-drawer>

    <a-modal
      v-model:open="projectPurchaseOpen"
      title="项目物料采购申请"
      width="720px"
      :confirm-loading="savingPurchase"
      @ok="handleProjectPurchaseRequest"
    >
      <a-alert
        v-if="detail"
        class="section-alert"
        type="info"
        show-icon
        :message="`${detail.project.code} · ${detail.project.name}`"
        :description="
          materialBudgetItem
            ? `物料预算 ${formatMoney(materialBudgetItem.plannedAmount)}，当前剩余 ${formatMoney(materialBudgetItem.variance)}。`
            : '当前项目无物料预算。'
        "
      />
      <a-form
        ref="projectPurchaseFormRef"
        :model="projectPurchaseForm"
        :rules="projectPurchaseRules"
        layout="vertical"
      >
        <a-row :gutter="16">
          <a-col :xs="24" :md="12"
            ><a-form-item label="物料名称" name="materialName"
              ><a-input
                v-model:value="projectPurchaseForm.materialName" /></a-form-item
          ></a-col>
          <a-col :xs="24" :md="12"
            ><a-form-item label="规格型号"
              ><a-input
                v-model:value="projectPurchaseForm.materialSpec" /></a-form-item
          ></a-col>
          <a-col :xs="12" :md="6"
            ><a-form-item label="数量" name="quantity"
              ><a-input-number
                v-model:value="projectPurchaseForm.quantity"
                :min="1"
                :precision="2"
                class="full-input" /></a-form-item
          ></a-col>
          <a-col :xs="12" :md="6"
            ><a-form-item label="单位"
              ><a-input v-model:value="projectPurchaseForm.unit" /></a-form-item
          ></a-col>
          <a-col :xs="12" :md="6"
            ><a-form-item label="预计单价"
              ><a-input-number
                v-model:value="projectPurchaseForm.unitPrice"
                :min="0"
                :precision="2"
                class="full-input" /></a-form-item
          ></a-col>
          <a-col :xs="12" :md="6"
            ><a-form-item label="税率(%)"
              ><a-input-number
                v-model:value="projectPurchaseForm.taxRate"
                :min="0"
                :max="100"
                :precision="2"
                class="full-input" /></a-form-item
          ></a-col>
          <a-col :xs="24" :md="12"
            ><a-form-item label="需求日期"
              ><a-input
                v-model:value="projectPurchaseForm.requiredDate"
                type="date" /></a-form-item
          ></a-col>
          <a-col :span="24"
            ><a-form-item label="申请原因" name="reason"
              ><a-textarea
                v-model:value="projectPurchaseForm.reason"
                :rows="3" /></a-form-item
          ></a-col>
        </a-row>
      </a-form>
    </a-modal>

    <ProjectModals
      v-model:create-open="createOpen"
      v-model:approval-open="approvalOpen"
      v-model:stage-open="stageOpen"
      v-model:cost-open="costOpen"
      :saving="saving"
      :customer-options="customerOptions"
      :category-options="categoryOptions"
      :project-type-options="projectTypeOptions"
      :source-options="sourceOptions"
      :user-options="visibleUserOptions"
      :detail="detail"
      :active-project="activeProject"
      :next-stage="nextStage"
      @created="
        createOpen = false;
        loadData();
      "
      @updated="loadData()"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { message } from "ant-design-vue";
import PlusOutlined from "@ant-design/icons-vue/PlusOutlined";
import ReloadOutlined from "@ant-design/icons-vue/ReloadOutlined";
import { useRoute, useRouter } from "vue-router";
import { listCustomers, type CustomerSummary } from "@/api/crm";
import {
  listProjects,
  getProject,
  listProjectProfitability,
  deleteProject,
  listPreSalesSupport,
  submitPreSalesCost,
  approvePreSalesCost,
} from "@/api/project";
import { listUsersApi, type UserResponse } from "@/api/system";
import {
  type Project,
  type ProjectApprovalStatus,
  type ProjectBudgetItem,
  type ProjectCostCategory,
  type ProjectCostEntry,
  type ProjectCostSource,
  type ProjectDetail,
  type ProjectStage,
  type ProjectStageRecord,
  type ProjectType,
  type ProjectProfitability,
} from "@/api/project";
import { createPurchaseRequest } from "@/api/procurement";
import type { ApprovalDecision, QuoteCostRequest, QuotePlan } from "@/api/crm";
import ApprovalProgressFlow, {
  type ApprovalProgressStep,
} from "@/components/ApprovalProgressFlow.vue";
import { useAuthStore } from "@/stores/auth";
import ProjectModals from "./ProjectModals.vue";
import BusinessTraceTimeline from "@/components/business/BusinessTraceTimeline.vue";

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const projects = ref<Project[]>([]);
const profitabilityRows = ref<ProjectProfitability[]>([]);
const detailCache = ref<Record<string, ProjectDetail>>({});
const pageMeta = ref({ totalElements: 0, totalPages: 0, number: 0, size: 999 });
const customers = ref<CustomerSummary[]>([]);
const visibleUsers = ref<UserResponse[]>([]);
const detail = ref<ProjectDetail | null>(null);
const activeProject = ref<Project | null>(null);
const loading = ref(false);
const detailLoading = ref(false);
const detailHydrating = ref(false);
const saving = ref(false);
const createOpen = ref(false);
const detailOpen = ref(false);
const approvalOpen = ref(false);
const stageOpen = ref(false);
const costOpen = ref(false);
const projectPurchaseOpen = ref(false);
const savingPurchase = ref(false);
const projectPurchaseFormRef = ref();
const preSalesCostOpen = ref(false);
const preSalesApprovalOpen = ref(false);
const errorMessage = ref("");
const keyword = ref("");
const approvalFilter = ref("ALL");
const stageFilter = ref("ALL");
type PageMode = "list" | "budget" | "costs" | "stages" | "presales";
type ProjectBudgetRow = {
  project: Project;
  riskLevel: string;
  riskMessage: string;
  usageRate: number;
};
type FlatCostEntry = ProjectCostEntry & { project: Project };
type FlatStageRecord = ProjectStageRecord & { project: Project };
type ProjectStageOverviewRow = {
  project: Project;
  latestRecord: ProjectStageRecord | null;
};
const preSalesRows = ref<QuotePlan[]>([]);
const selectedPreSales = ref<QuotePlan | null>(null);
const preSalesCostForm = reactive(initialPreSalesCostForm());
const preSalesApprovalForm = reactive(initialPreSalesApprovalForm());
const projectPurchaseForm = reactive(initialProjectPurchaseForm());
const projectPurchaseRules = {
  materialName: [{ required: true, message: "请输入物料名称" }],
  quantity: [{ required: true, message: "请输入数量" }],
  reason: [{ required: true, message: "请输入申请原因" }],
};

const stageOptions: Array<{ label: string; value: ProjectStage }> = [
  { label: "入场", value: "ENTRY" },
  { label: "施工", value: "CONSTRUCTION" },
  { label: "调试", value: "COMMISSIONING" },
  { label: "初验", value: "INITIAL_ACCEPTANCE" },
  { label: "终验", value: "FINAL_ACCEPTANCE" },
  { label: "质保", value: "WARRANTY" },
  { label: "关闭", value: "CLOSED" },
];
const projectTypeOptions = [
  { label: "新建工程", value: "NEW_CONSTRUCTION" },
  { label: "改造工程", value: "RENOVATION" },
  { label: "升级改造", value: "O_M_RENOVATION" },
];
const categoryOptions: Array<{ label: string; value: ProjectCostCategory }> = [
  { label: "人工", value: "LABOR" },
  { label: "材料物料", value: "MATERIAL" },
  { label: "外包", value: "SUBCONTRACT" },
  { label: "差旅", value: "TRAVEL" },
  { label: "其他", value: "OTHER" },
];
const sourceOptions = [
  { label: "手工登记", value: "MANUAL" },
  { label: "仓库领料", value: "INVENTORY" },
  { label: "采购入库（自动）", value: "PROCUREMENT", disabled: true },
  { label: "费用报销", value: "EXPENSE" },
  { label: "外包结算", value: "SUBCONTRACT" },
];
const approvalFilterOptions = [
  { label: "全部分配状态", value: "ALL" },
  { label: "待分配", value: "PENDING" },
  { label: "已分配", value: "APPROVED" },
  { label: "已退回", value: "REJECTED" },
];
const stageFilterOptions = computed(() => [
  { label: "全部阶段", value: "ALL" },
  ...stageOptions,
]);
const pageMode = computed<PageMode>(() => {
  if (route.path.endsWith("/presales-support")) return "presales";
  if (route.path.endsWith("/budget")) return "budget";
  if (route.path.endsWith("/costs")) return "costs";
  if (route.path.endsWith("/stages")) return "stages";
  return "list";
});
const pageTitle = computed(
  () =>
    (
      ({
        list: "项目列表",
        budget: "预算执行",
        costs: "成本明细",
        stages: "阶段履历",
        presales: "售前支持",
      }) as Record<PageMode, string>
    )[pageMode.value],
);
const columns = [
  { title: "项目 / 客户", key: "name", width: 280 },
  { title: "项目类型", key: "owner", width: 130 },
  { title: "项目负责人", key: "manager", width: 130 },
  { title: "阶段", key: "stage", width: 150 },
  { title: "项目经理分配", key: "approval", width: 220 },
  { title: "合同金额", key: "contract", width: 140 },
  { title: "预算 / 实际", key: "cost", width: 180 },
  { title: "毛利 / 余额", key: "gross", width: 180 },
  { title: "操作", key: "action", width: 130, fixed: "right" },
];
const budgetColumns = [
  { title: "成本分类", key: "category", width: 140 },
  { title: "预算金额", key: "planned", width: 150 },
  { title: "实际发生", key: "actual", width: 150 },
  { title: "预算余额", key: "variance", width: 150 },
  { title: "备注", dataIndex: "remark" },
];
const costColumns = [
  { title: "分类", key: "category", width: 110 },
  { title: "来源", key: "source", width: 170 },
  { title: "成本说明", dataIndex: "description" },
  { title: "发生日期", dataIndex: "incurredDate", width: 120 },
  { title: "金额", key: "amount", width: 130 },
];
const stageRecordColumns = [
  { title: "阶段变化", key: "change", width: 220 },
  { title: "节点说明", dataIndex: "comment" },
  { title: "操作记录", key: "operator", width: 190 },
];
const budgetExecutionColumns = [
  { title: "项目 / 负责人", key: "project", width: 280 },
  { title: "预算 / 合同", key: "budget", width: 190 },
  { title: "实际成本 / 使用率", key: "actual", width: 210 },
  { title: "预算余额", key: "variance", width: 150 },
  { title: "风险", key: "risk", width: 300 },
  { title: "操作", key: "action", width: 110, fixed: "right" as const },
];
const flatCostColumns = [
  { title: "项目", key: "project", width: 240 },
  { title: "分类", key: "category", width: 120 },
  { title: "来源", key: "source", width: 180 },
  { title: "成本说明", dataIndex: "description" },
  { title: "发生日期", dataIndex: "incurredDate", width: 120 },
  { title: "金额", key: "amount", width: 140 },
];
const flatStageColumns = [
  { title: "推进时间", key: "time", width: 170 },
  { title: "项目", key: "project", width: 240 },
  { title: "阶段变化", key: "change", width: 240 },
  { title: "完成进度", key: "progress", width: 110 },
  { title: "推进说明", dataIndex: "comment" },
  { title: "操作人", key: "operator", width: 140 },
];
const stageProjectColumns = [
  { title: "项目 / 负责人", key: "project", width: 250 },
  { title: "当前阶段", key: "stage", width: 110 },
  { title: "项目进度", key: "progress", width: 180 },
  { title: "计划周期", key: "schedule", width: 170 },
  { title: "最近推进", key: "latest", width: 210 },
];
const preSalesColumns = [
  { title: "报价 / 客户", key: "quote", width: 260 },
  { title: "售前需求", key: "scope", width: 330 },
  { title: "报价金额", key: "amount", width: 150 },
  { title: "成本核算", key: "cost", width: 220 },
  { title: "状态", key: "status", width: 160 },
  { title: "操作", key: "action", width: 190, fixed: "right" as const },
];

const filteredProjects = computed(() =>
  projects.value.filter((item) => {
    const search = keyword.value.trim().toLowerCase();
    const textMatched =
      !search ||
      [item.code, item.name, item.customerName, item.managerName].some(
        (value) => value?.toLowerCase().includes(search),
      );
    return (
      textMatched &&
      (approvalFilter.value === "ALL" ||
        item.approvalStatus === approvalFilter.value) &&
      (stageFilter.value === "ALL" || item.stage === stageFilter.value)
    );
  }),
);
const customerOptions = computed(() =>
  customers.value.map((item) => ({
    label: `${item.name} (${item.code})`,
    value: item.id,
  })),
);
const visibleUserOptions = computed(() => {
  const options = visibleUsers.value
    .filter((item) => item.enabled)
    .map((item) => ({
      label: `${item.displayName} · ${item.username}`,
      value: item.displayName,
    }));
  const selected = preSalesCostForm.projectManager;
  return selected && !options.some((item) => item.value === selected)
    ? [{ label: selected, value: selected }, ...options]
    : options;
});
const totalContract = computed(() =>
  projects.value.reduce(
    (sum, item) => sum + Number(item.contractAmount || 0),
    0,
  ),
);
const totalBudget = computed(() =>
  projects.value.reduce((sum, item) => sum + Number(item.budgetAmount || 0), 0),
);
const totalActualCost = computed(() =>
  projects.value.reduce((sum, item) => sum + Number(item.actualCost || 0), 0),
);
const pendingApprovalCount = computed(
  () =>
    projects.value.filter((item) => item.approvalStatus === "PENDING").length,
);
const highRiskProjectCount = computed(
  () =>
    profitabilityRows.value.filter((item) => item.riskLevel === "HIGH").length,
);
const budgetOverrunCount = computed(
  () =>
    projects.value.filter((item) => Number(item.budgetVariance || 0) < 0)
      .length,
);
const nextStage = computed<ProjectStage | null>(() => {
  if (!detail.value || detail.value.project.stage === "CLOSED") return null;
  const currentIndex = stageOptions.findIndex(
    (item) => item.value === detail.value?.project.stage,
  );
  const normalizedIndex = currentIndex >= 0 ? currentIndex : 0;
  return stageOptions[normalizedIndex + 1]?.value || null;
});
const profitabilityMap = computed(
  () => new Map(profitabilityRows.value.map((item) => [item.projectId, item])),
);
const budgetExecutionRows = computed<ProjectBudgetRow[]>(() =>
  filteredProjects.value
    .map((project) => {
      const risk = profitabilityMap.value.get(project.id);
      return {
        project,
        riskLevel:
          risk?.riskLevel || (project.budgetVariance < 0 ? "HIGH" : "LOW"),
        riskMessage:
          risk?.riskMessage ||
          (project.budgetVariance < 0 ? "实际成本已超过预算" : "预算执行正常"),
        usageRate:
          project.budgetAmount > 0
            ? Math.round(
                (Number(project.actualCost || 0) /
                  Number(project.budgetAmount || 1)) *
                  100,
              )
            : 0,
      };
    })
    .sort((a, b) => b.usageRate - a.usageRate),
);
const materialBudgetItem = computed(
  () =>
    detail.value?.budgetItems.find(
      (item) =>
        item.category === "MATERIAL" && Number(item.plannedAmount || 0) > 0,
    ) || null,
);
const detailRows = computed(() => Object.values(detailCache.value));
const stageFilteredProjects = computed(() =>
  projects.value.filter((project) => {
    const search = keyword.value.trim().toLowerCase();
    const textMatched =
      !search ||
      [
        project.code,
        project.name,
        project.customerName,
        project.managerName,
      ].some((value) => value?.toLowerCase().includes(search));
    return (
      textMatched &&
      (stageFilter.value === "ALL" || project.stage === stageFilter.value)
    );
  }),
);
const latestStageRecordMap = computed(() => {
  const rows = new Map<string, ProjectStageRecord>();
  detailRows.value.forEach((detailRow) => {
    const latest = [...detailRow.stageRecords].sort((a, b) =>
      (b.changedAt || "").localeCompare(a.changedAt || ""),
    )[0];
    if (latest) rows.set(detailRow.project.id, latest);
  });
  return rows;
});
const stageProjectRows = computed<ProjectStageOverviewRow[]>(() =>
  [...stageFilteredProjects.value]
    .sort(
      (a, b) =>
        stageSequenceIndex(a.stage) - stageSequenceIndex(b.stage) ||
        a.name.localeCompare(b.name, "zh-CN"),
    )
    .map((project) => ({
      project,
      latestRecord: latestStageRecordMap.value.get(project.id) || null,
    })),
);
const flatCostEntries = computed<FlatCostEntry[]>(() =>
  detailRows.value
    .flatMap((row) =>
      row.costEntries.map((item) => ({ ...item, project: row.project })),
    )
    .sort((a, b) => (b.incurredDate || "").localeCompare(a.incurredDate || "")),
);
const flatStageRecords = computed<FlatStageRecord[]>(() =>
  (() => {
    const visibleProjectIds = new Set(
      stageFilteredProjects.value.map((project) => project.id),
    );
    return detailRows.value
      .filter((row) => visibleProjectIds.has(row.project.id))
      .flatMap((row) =>
        row.stageRecords.map((item) => ({ ...item, project: row.project })),
      )
      .sort((a, b) => (b.changedAt || "").localeCompare(a.changedAt || ""));
  })(),
);
const budgetCards = computed(() => [
  {
    label: "预算使用率",
    value: formatPercent(
      totalBudget.value ? (totalActualCost.value / totalBudget.value) * 100 : 0,
    ),
    hint: `${formatMoney(totalActualCost.value)} / ${formatMoney(totalBudget.value)}`,
    danger: totalActualCost.value > totalBudget.value,
    action: () => {
      stageFilter.value = "ALL";
    },
  },
  {
    label: "预算余额",
    value: formatMoney(totalBudget.value - totalActualCost.value),
    hint: "全部项目合计",
    danger: totalActualCost.value > totalBudget.value,
    action: () => {
      approvalFilter.value = "ALL";
    },
  },
  {
    label: "超预算项目",
    value: `${budgetOverrunCount.value} 个`,
    hint: "实际成本超过预算",
    danger: budgetOverrunCount.value > 0,
    action: () => {
      keyword.value = "";
    },
  },
  {
    label: "高风险项目",
    value: `${highRiskProjectCount.value} 个`,
    hint: "利润或预算异常",
    danger: highRiskProjectCount.value > 0,
    action: () => {
      keyword.value = "";
    },
  },
]);
const costCards = computed(() => [
  {
    label: "成本记录",
    value: `${flatCostEntries.value.length} 条`,
    hint: "已登记明细",
    action: () => hydrateProjectDetails(true),
  },
  {
    label: "成本总额",
    value: formatMoney(
      flatCostEntries.value.reduce(
        (sum, item) => sum + Number(item.amount || 0),
        0,
      ),
    ),
    hint: "按明细汇总",
    action: () => {},
  },
  {
    label: "材料成本",
    value: formatMoney(
      flatCostEntries.value
        .filter((item) => item.category === "MATERIAL")
        .reduce((sum, item) => sum + Number(item.amount || 0), 0),
    ),
    hint: "仓库/采购相关",
    action: () => {},
  },
  {
    label: "外包成本",
    value: formatMoney(
      flatCostEntries.value
        .filter((item) => item.category === "SUBCONTRACT")
        .reduce((sum, item) => sum + Number(item.amount || 0), 0),
    ),
    hint: "外包与分包",
    action: () => {},
  },
]);
const stageCards = computed(() =>
  stageOptions.map((stage) => {
    const rows = projects.value.filter((item) => item.stage === stage.value);
    return {
      stage: stage.value,
      label: stage.label,
      count: rows.length,
      amount: formatMoney(
        rows.reduce((sum, item) => sum + Number(item.contractAmount || 0), 0),
      ),
    };
  }),
);
const closedReviewRows = computed(() =>
  projects.value.filter((item) => item.stage === "CLOSED"),
);
const profitReviewCards = computed(() => {
  const contractAmount = closedReviewRows.value.reduce(
    (sum, item) => sum + Number(item.contractAmount || 0),
    0,
  );
  const actualCost = closedReviewRows.value.reduce(
    (sum, item) => sum + Number(item.actualCost || 0),
    0,
  );
  const gross = closedReviewRows.value.reduce(
    (sum, item) => sum + Number(item.grossMargin || 0),
    0,
  );
  const marginRate = contractAmount > 0 ? (gross / contractAmount) * 100 : 0;
  return [
    {
      label: "关闭项目",
      value: `${closedReviewRows.value.length} 个`,
      hint: "已完成复盘对象",
      danger: false,
      action: () => {
        stageFilter.value = "CLOSED";
      },
    },
    {
      label: "合同额",
      value: formatMoney(contractAmount),
      hint: "关闭项目合计",
      danger: false,
      action: () => {},
    },
    {
      label: "实际成本",
      value: formatMoney(actualCost),
      hint: "采购/领料/人工/外包",
      danger: false,
      action: () => {},
    },
    {
      label: "毛利率",
      value: formatPercent(marginRate),
      hint: `毛利 ${formatMoney(gross)}`,
      danger: marginRate < 10,
      action: () => {},
    },
  ];
});
onMounted(loadData);
watch(
  () => route.path,
  () => {
    if (pageMode.value === "presales") loadPreSalesSupport();
    if (pageMode.value === "costs" || pageMode.value === "stages")
      hydrateProjectDetails();
  },
);

async function loadData() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const [projectPage, customerRows, profitability, preSales] =
      await Promise.all([
        listProjects(0, 999),
        listCustomers(),
        listProjectProfitability(),
        pageMode.value === "presales"
          ? listPreSalesSupport()
          : Promise.resolve([]),
      ]);
    projects.value = projectPage.content;
    pageMeta.value = {
      totalElements: projectPage.totalElements,
      totalPages: projectPage.totalPages,
      number: projectPage.number,
      size: projectPage.size,
    };
    customers.value = customerRows;
    profitabilityRows.value = profitability;
    preSalesRows.value = preSales;
    await loadVisibleUsers();
    if (pageMode.value === "costs" || pageMode.value === "stages")
      await hydrateProjectDetails();
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : "项目数据加载失败";
  } finally {
    loading.value = false;
  }
}

async function loadPreSalesSupport() {
  loading.value = true;
  try {
    preSalesRows.value = await listPreSalesSupport();
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : "售前支持加载失败";
  } finally {
    loading.value = false;
  }
}
function openCreate() {
  createOpen.value = true;
}
function openApproval(project: Project) {
  activeProject.value = project;
  if (!visibleUsers.value.length) void loadVisibleUsers();
  approvalOpen.value = true;
}
function openStage() {
  if (!detail.value || !nextStage.value) return;
  stageOpen.value = true;
}
function openCost() {
  costOpen.value = true;
}

function openProjectPurchaseRequest() {
  if (!detail.value || !materialBudgetItem.value) return;
  Object.assign(projectPurchaseForm, initialProjectPurchaseForm());
  projectPurchaseOpen.value = true;
}

async function handleProjectPurchaseRequest() {
  if (!detail.value) return;
  await projectPurchaseFormRef.value?.validate();
  savingPurchase.value = true;
  try {
    await createPurchaseRequest({
      requesterName: auth.user?.displayName || "",
      materialName: projectPurchaseForm.materialName,
      materialSpec: projectPurchaseForm.materialSpec || undefined,
      partName: projectPurchaseForm.materialName,
      quantity: projectPurchaseForm.quantity,
      unitPrice: projectPurchaseForm.unitPrice,
      taxRate: projectPurchaseForm.taxRate,
      requiredDate: projectPurchaseForm.requiredDate || undefined,
      expectedDate: projectPurchaseForm.requiredDate || undefined,
      reason: projectPurchaseForm.reason,
      description: projectPurchaseForm.reason,
      costType: "PROJECT",
      projectId: detail.value.project.id,
    });
    projectPurchaseOpen.value = false;
    message.success("项目采购申请已提交审批");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "采购申请提交失败");
  } finally {
    savingPurchase.value = false;
  }
}

function openPreSalesCost(record: QuotePlan) {
  selectedPreSales.value = record;
  Object.assign(preSalesCostForm, initialPreSalesCostForm(record.costRequest));
  if (!visibleUsers.value.length) void loadVisibleUsers();
  preSalesCostOpen.value = true;
}

function openPreSalesApproval(record: QuotePlan) {
  selectedPreSales.value = record;
  Object.assign(preSalesApprovalForm, initialPreSalesApprovalForm());
  preSalesApprovalOpen.value = true;
}

async function handlePreSalesCost() {
  if (!selectedPreSales.value?.costRequest?.id) {
    message.warning("未找到售前支持单");
    return;
  }
  const total = preSalesCostTotal(preSalesCostForm);
  if (!preSalesCostForm.projectManager.trim()) {
    message.warning("请输入成本负责人");
    return;
  }
  if (total <= 0) {
    message.warning("请至少填写一项成本");
    return;
  }
  saving.value = true;
  try {
    await submitPreSalesCost(selectedPreSales.value.costRequest.id, {
      ...preSalesCostForm,
      travelTaxRate: 0,
      riskReserveTaxRate: 0,
    });
    preSalesCostOpen.value = false;
    message.success("售前成本已提交审批");
    await loadPreSalesSupport();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "售前成本提交失败");
  } finally {
    saving.value = false;
  }
}

async function loadVisibleUsers() {
  try {
    const page = await listUsersApi(0, 999);
    visibleUsers.value = page.content;
  } catch {
    visibleUsers.value = [
      {
        id: "current",
        username: auth.user?.username || auth.user?.displayName || "current",
        displayName: auth.user?.displayName || "当前用户",
        enabled: true,
        roles: [],
      },
    ];
  }
}

async function handlePreSalesApproval() {
  if (!selectedPreSales.value?.costRequest?.id) {
    message.warning("未找到售前支持单");
    return;
  }
  if (
    !preSalesApprovalForm.approverName.trim() ||
    !preSalesApprovalForm.comment.trim()
  ) {
    message.warning("请填写审批人和审批意见");
    return;
  }
  saving.value = true;
  try {
    await approvePreSalesCost(selectedPreSales.value.costRequest.id, {
      ...preSalesApprovalForm,
    });
    preSalesApprovalOpen.value = false;
    message.success(
      preSalesApprovalForm.decision === "APPROVED"
        ? "售前成本已通过，销售报价可继续"
        : "售前成本已驳回",
    );
    await loadPreSalesSupport();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "售前成本审批失败");
  } finally {
    saving.value = false;
  }
}

async function openDetail(project: Project) {
  detailOpen.value = true;
  detailLoading.value = true;
  try {
    detail.value = await getProject(project.id);
    detailCache.value = { ...detailCache.value, [project.id]: detail.value };
  } catch (error) {
    message.error(error instanceof Error ? error.message : "项目详情加载失败");
  } finally {
    detailLoading.value = false;
  }
}
async function removeProject(project: Project) {
  try {
    await deleteProject(project.id);
    const nextCache = { ...detailCache.value };
    delete nextCache[project.id];
    detailCache.value = nextCache;
    if (detail.value?.project.id === project.id) {
      detail.value = null;
      detailOpen.value = false;
    }
    message.success("项目已删除");
    await loadData();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "项目删除失败");
  }
}

async function hydrateProjectDetails(force = false) {
  if (detailHydrating.value) return;
  detailHydrating.value = true;
  try {
    const targets = projects.value
      .filter((project) => force || !detailCache.value[project.id])
      .slice(0, 80);
    const details = await Promise.all(
      targets.map((project) => getProject(project.id).catch(() => null)),
    );
    detailCache.value = {
      ...detailCache.value,
      ...Object.fromEntries(
        details
          .filter((item): item is ProjectDetail => Boolean(item))
          .map((item) => [item.project.id, item]),
      ),
    };
  } finally {
    detailHydrating.value = false;
  }
}

function canExecute(project: Project) {
  return project.approvalStatus === "APPROVED" && project.stage !== "CLOSED";
}
function canAdvance(project: Project) {
  return canExecute(project) && project.stage !== "CLOSED";
}
function canAssignManager(project: Project) {
  return (
    auth.can("project:approve") &&
    (project.approvalStatus === "PENDING" ||
      project.approvalStatus === "APPROVED")
  );
}
function stageLabel(stage: ProjectStage) {
  if (stage === "INITIATED" || stage === "BIDDING") return "入场";
  return stageOptions.find((item) => item.value === stage)?.label || stage;
}
function stageColor(stage: ProjectStage) {
  return (
    {
      INITIATED: "blue",
      BIDDING: "cyan",
      ENTRY: "geekblue",
      CONSTRUCTION: "orange",
      COMMISSIONING: "purple",
      INITIAL_ACCEPTANCE: "gold",
      FINAL_ACCEPTANCE: "green",
      WARRANTY: "lime",
      CLOSED: "default",
    } as Record<ProjectStage, string>
  )[stage];
}
function stageSequenceIndex(stage: ProjectStage) {
  if (stage === "INITIATED" || stage === "BIDDING") return 0;
  const index = stageOptions.findIndex((item) => item.value === stage);
  return index >= 0 ? index : stageOptions.length;
}
function approvalLabel(status: ProjectApprovalStatus) {
  return (
    { PENDING: "待分配", APPROVED: "已分配", REJECTED: "已退回" } as Record<
      ProjectApprovalStatus,
      string
    >
  )[status];
}
function approvalColor(status: ProjectApprovalStatus) {
  return (
    { PENDING: "orange", APPROVED: "green", REJECTED: "red" } as Record<
      ProjectApprovalStatus,
      string
    >
  )[status];
}
function projectApprovalSteps(project: Project): ApprovalProgressStep[] {
  return [
    {
      key: "start",
      personName: project.customerName || "发起人",
      title: "发起项目",
      note: project.contractCode || project.name,
      state: "done",
    },
    {
      key: "approval",
      personName: project.approverName || "当前审批人",
      title:
        project.approvalStatus === "PENDING"
          ? "待审批"
          : project.approvalStatus === "REJECTED"
            ? "已驳回"
            : "已同意",
      time: project.approvedAt,
      note:
        project.approvalStatus === "PENDING"
          ? "等待项目审批/负责人分配"
          : project.approvalComment || approvalLabel(project.approvalStatus),
      state:
        project.approvalStatus === "PENDING"
          ? "pending"
          : project.approvalStatus === "REJECTED"
            ? "rejected"
            : "done",
    },
    {
      key: "execute",
      personName: project.managerName || "项目负责人",
      title: project.approvalStatus === "APPROVED" ? "已进入执行" : "待执行",
      note:
        project.approvalStatus === "APPROVED"
          ? stageLabel(project.stage)
          : "审批通过后开始执行",
      state: project.approvalStatus === "APPROVED" ? "done" : "waiting",
    },
  ];
}
function projectTypeLabel(type: ProjectType) {
  return projectTypeOptions.find((item) => item.value === type)?.label || type;
}
function categoryLabel(category: ProjectCostCategory) {
  return (
    categoryOptions.find((item) => item.value === category)?.label || category
  );
}
function sourceLabel(source: ProjectCostSource) {
  return sourceOptions.find((item) => item.value === source)?.label || source;
}
function preSalesStatusLabel(status: string) {
  return (
    (
      {
        COST_REQUESTED: "待填写成本",
        COSTING: "待成本审批",
        COST_APPROVED: "成本已核对",
      } as Record<string, string>
    )[status] || status
  );
}
function preSalesStatusColor(status: string) {
  return (
    (
      {
        COST_REQUESTED: "cyan",
        COSTING: "orange",
        COST_APPROVED: "green",
      } as Record<string, string>
    )[status] || "default"
  );
}
function preSalesCostTotal(record: {
  laborCost?: number;
  materialCost?: number;
  subcontractCost?: number;
  travelCost?: number;
  equipmentCost?: number;
  riskReserve?: number;
  otherCost?: number;
}) {
  return (
    Number(record.laborCost || 0) +
    Number(record.materialCost || 0) +
    Number(record.subcontractCost || 0) +
    Number(record.travelCost || 0) +
    Number(record.equipmentCost || 0) +
    Number(record.riskReserve || 0) +
    Number(record.otherCost || 0)
  );
}
function preSalesCostNetTotal(record: {
  laborCost?: number;
  laborTaxRate?: number;
  materialCost?: number;
  materialTaxRate?: number;
  subcontractCost?: number;
  subcontractTaxRate?: number;
  travelCost?: number;
  travelTaxRate?: number;
  equipmentCost?: number;
  equipmentTaxRate?: number;
  riskReserve?: number;
  riskReserveTaxRate?: number;
  otherCost?: number;
  otherTaxRate?: number;
}) {
  return (
    calcNetAmount(record.laborCost, record.laborTaxRate) +
    calcNetAmount(record.materialCost, record.materialTaxRate) +
    calcNetAmount(record.subcontractCost, record.subcontractTaxRate) +
    calcNetAmount(record.travelCost, 0) +
    calcNetAmount(record.equipmentCost, record.equipmentTaxRate) +
    calcNetAmount(record.riskReserve, 0) +
    calcNetAmount(record.otherCost, record.otherTaxRate)
  );
}
function initialPreSalesCostForm(cost?: QuoteCostRequest) {
  return {
    projectManager: cost?.projectManager || auth.user?.displayName || "",
    laborCost: Number(cost?.laborCost || 0),
    laborTaxRate: Number(cost?.laborTaxRate ?? 6),
    materialCost: Number(cost?.materialCost || 0),
    materialTaxRate: Number(cost?.materialTaxRate ?? 13),
    subcontractCost: Number(cost?.subcontractCost || 0),
    subcontractTaxRate: Number(cost?.subcontractTaxRate ?? 13),
    travelCost: Number(cost?.travelCost || 0),
    travelTaxRate: 0,
    equipmentCost: Number(cost?.equipmentCost || 0),
    equipmentTaxRate: Number(cost?.equipmentTaxRate ?? 13),
    riskReserve: Number(cost?.riskReserve || 0),
    riskReserveTaxRate: 0,
    otherCost: Number(cost?.otherCost || 0),
    otherTaxRate: Number(cost?.otherTaxRate ?? 13),
    suggestedPrice: Number(
      cost?.suggestedPrice || selectedPreSales.value?.amount || 0,
    ),
    costRemark: cost?.costRemark || "",
  };
}
function initialPreSalesApprovalForm() {
  return {
    decision: "APPROVED" as ApprovalDecision,
    approverName: auth.user?.displayName || "",
    comment: "同意售前成本测算，作为报价毛利核算依据。",
  };
}
function initialProjectPurchaseForm() {
  return {
    materialName: "",
    materialSpec: "",
    quantity: 1,
    unit: "个",
    unitPrice: 0,
    taxRate: 13,
    requiredDate: dateAfter(7),
    reason: "",
  };
}
function riskLabel(level: string) {
  return (
    ({ HIGH: "高风险", MEDIUM: "关注", LOW: "正常" } as Record<string, string>)[
      level
    ] || level
  );
}
function riskColor(level: string) {
  return (
    ({ HIGH: "red", MEDIUM: "orange", LOW: "green" } as Record<string, string>)[
      level
    ] || "default"
  );
}
function formatMoney(value: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value || 0);
}
function moneyFormatter(value: number | string) {
  return formatMoney(Number(value));
}
function calcNetAmount(amount?: number, taxRate?: number) {
  const rate = Number(taxRate ?? 13);
  const divisor = 1 + rate / 100;
  return divisor > 0 ? Number(amount || 0) / divisor : Number(amount || 0);
}
function formatPercent(value: number) {
  return `${Number(value || 0).toFixed(1)}%`;
}
function normalizedProgress(value?: number) {
  return Math.min(Math.max(Math.round(Number(value || 0)), 0), 100);
}
function formatDateTime(value: string) {
  return value
    ? new Intl.DateTimeFormat("zh-CN", {
        dateStyle: "medium",
        timeStyle: "short",
      }).format(new Date(value))
    : "-";
}
function dateAfter(days: number) {
  const date = new Date();
  date.setDate(date.getDate() + days);
  return date.toISOString().slice(0, 10);
}
</script>

<style scoped>
.drawer-metrics {
  margin: 20px 0 8px;
}
.project-workbench {
  margin-bottom: 16px;
  padding: 14px;
  border: 1px solid #e5e7eb;
  background: #fbfcfe;
}
.workbench-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}
.workbench-title h3 {
  margin: 0;
  color: #111827;
  font-size: 15px;
  font-weight: 600;
}
.workbench-title p {
  margin: 4px 0 0;
  color: #6b7280;
  font-size: 12px;
}
.project-summary-grid,
.stage-kanban-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}
.stage-kanban-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}
.presales-cost-table {
  display: grid;
  gap: 0;
  overflow: hidden;
  margin-bottom: 16px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fff;
}
.presales-cost-head,
.presales-cost-row {
  display: grid;
  grid-template-columns: 110px minmax(150px, 1fr) minmax(150px, 1fr) 120px;
  gap: 12px;
  align-items: center;
  padding: 10px 12px;
}
.presales-cost-head {
  background: #f8fafc;
  color: #667085;
  font-size: 12px;
  font-weight: 600;
}
.presales-cost-row {
  border-top: 1px solid #eef2f7;
}
.presales-cost-row strong {
  color: #101828;
  font-size: 13px;
  font-weight: 600;
}
.presales-cost-row em {
  color: #344054;
  font-size: 13px;
  font-style: normal;
  font-weight: 600;
}
.tax-static {
  display: flex;
  align-items: center;
  min-height: 32px;
  padding: 0 11px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  background: #f5f5f5;
  color: #667085;
  font-size: 13px;
}
.project-inline-actions {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
}
.summary-card,
.stage-card {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 12px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  text-align: left;
}
.summary-card:hover,
.stage-card:hover {
  border-color: #91caff;
  background: #f6faff;
}
.stage-card.active {
  border-color: #1677ff;
  background: #eaf4ff;
  box-shadow: inset 0 0 0 1px #1677ff;
}
.stage-card.active span,
.stage-card.active strong {
  color: #0958d9;
}
.summary-card span,
.stage-card span {
  color: #667085;
  font-size: 12px;
}
.summary-card strong,
.stage-card strong {
  overflow: hidden;
  color: #101828;
  font-size: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.summary-card em,
.stage-card em {
  overflow: hidden;
  color: #98a2b3;
  font-size: 12px;
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.stage-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin: 20px 0 10px;
}
.stage-section-header > div:first-child {
  min-width: 0;
}
.stage-section-header h4 {
  margin: 0;
  color: #101828;
  font-size: 14px;
  font-weight: 600;
}
.stage-section-header span {
  display: block;
  margin-top: 2px;
  color: #667085;
  font-size: 12px;
}
.stage-history-header {
  padding-top: 4px;
  border-top: 1px solid #eef2f7;
}
.stage-progress-cell {
  width: 150px;
}
.text-muted {
  color: #98a2b3;
}
@media (max-width: 900px) {
  .workbench-title {
    flex-direction: column;
  }
  .project-summary-grid,
  .stage-kanban-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .stage-section-header {
    align-items: flex-start;
    flex-direction: column;
  }
  .project-inline-actions {
    grid-template-columns: 1fr;
  }
  .presales-cost-head {
    display: none;
  }
  .presales-cost-row {
    grid-template-columns: 80px minmax(0, 1fr);
  }
  .presales-cost-row :nth-child(3),
  .presales-cost-row :nth-child(4) {
    grid-column: 2;
  }
}
</style>
