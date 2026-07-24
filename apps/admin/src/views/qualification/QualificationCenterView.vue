<template>
  <div class="page-stack qualification-page">
    <template v-if="mode === 'dashboard'">
      <a-row :gutter="16" class="metric-row">
        <a-col
          v-for="item in dashboardMetrics"
          :key="item.title"
          :xs="12"
          :lg="4"
        >
          <a-card
            ><a-statistic
              :title="item.title"
              :value="item.value"
              :value-style="item.danger ? { color: '#cf1322' } : undefined"
          /></a-card>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :xs="24" :xl="12">
          <a-card title="公司资质分类"
            ><a-table
              size="small"
              :columns="distributionColumns"
              :data-source="dashboard.companyCategoryDistribution"
              :pagination="false"
              row-key="name"
          /></a-card>
        </a-col>
        <a-col :xs="24" :xl="12">
          <a-card title="人员证书专业分布"
            ><a-table
              size="small"
              :columns="distributionColumns"
              :data-source="
                dashboard.certificateSpecialtyDistribution.slice(0, 12)
              "
              :pagination="false"
              row-key="name"
          /></a-card>
        </a-col>
      </a-row>
      <a-card title="近期资质风险"
        ><a-table
          :columns="warningColumns"
          :data-source="dashboard.recentWarnings"
          :pagination="false"
          :row-key="warningRowKey"
          ><template #bodyCell="{ column, record }"
            ><template v-if="column.key === 'warningStatus'"
              ><a-tag :color="warningColor(record.level)">{{
                warningStatusLabel(record)
              }}</a-tag></template
            ></template
          ></a-table
        ></a-card
      >
    </template>

    <template v-else-if="mode === 'companies'">
      <a-card title="公司资质台账">
        <a-space wrap class="table-toolbar">
          <a-input
            v-model:value="companyFilters.keyword"
            allow-clear
            placeholder="资质名称 / 编号 / 类别"
            @press-enter="loadCompanies"
          />
          <a-select
            v-model:value="companyFilters.subjectCompany"
            allow-clear
            placeholder="所属公司"
            :options="subjectOptions"
          />
          <a-select
            v-model:value="companyFilters.status"
            allow-clear
            placeholder="状态"
            :options="statusOptions"
          />
          <a-button type="primary" @click="loadCompanies"
            ><template #icon><SearchOutlined /></template>查询</a-button
          >
          <a-button
            v-if="auth.can('qualification:company:manage')"
            @click="openCompany()"
            ><template #icon><PlusOutlined /></template>新增资质</a-button
          >
        </a-space>
        <a-table
          :loading="loading"
          :columns="companyColumns"
          :data-source="companies"
          row-key="id"
          :scroll="{ x: 1350 }"
          :pagination="{ pageSize: 15 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'"
              ><strong>{{ record.name }}</strong
              ><span class="table-subtitle">{{
                record.certificateNo || "未登记证书编号"
              }}</span></template
            >
            <template v-else-if="column.key === 'status'"
              ><a-tag :color="statusColor(record.status)">{{
                statusLabel(record.status)
              }}</a-tag></template
            >
            <template v-else-if="column.key === 'tender'"
              ><a-tag
                :color="record.availableForTender ? 'green' : 'default'"
                >{{ record.availableForTender ? "可投标" : "不可投标" }}</a-tag
              ></template
            >
            <template v-else-if="column.key === 'attachment'"
              ><a-button
                v-if="record.attachments.length"
                type="link"
                size="small"
                @click="showAttachments(record.attachments)"
                ><template #icon><EyeOutlined /></template
                >{{ record.attachments.length }}份</a-button
              ><span v-else>-</span></template
            >
            <template v-else-if="column.key === 'actions'"
              ><a-space
                ><a-button type="text" size="small" @click="openCompany(record)"
                  ><template #icon><EditOutlined /></template></a-button
                ><a-popconfirm
                  title="确认删除该公司资质？"
                  @confirm="removeCompany(record.id)"
                  ><a-button danger type="text" size="small"
                    ><template #icon
                      ><DeleteOutlined /></template></a-button></a-popconfirm></a-space
            ></template>
          </template>
        </a-table>
      </a-card>
    </template>

    <template v-else-if="mode === 'employees'">
      <section class="employee-directory">
        <header class="employee-directory-header">
          <div>
            <div class="employee-title-row">
              <h2>员工档案</h2>
              <span class="employee-count">共 {{ employees.length }} 人</span>
            </div>
            <div class="employee-summary" aria-label="员工档案概况">
              <span
                ><strong>{{ activeEmployeeCount }}</strong> 人在职</span
              >
              <span
                ><strong>{{ linkedAccountCount }}</strong> 人已开通账号</span
              >
            </div>
          </div>
          <a-button
            v-if="auth.can('qualification:employee:manage')"
            type="primary"
            class="employee-create-button"
            @click="openEmployee()"
          >
            <template #icon><PlusOutlined /></template>新增人员
          </a-button>
        </header>

        <div class="employee-filter-bar">
          <a-input
            v-model:value="employeeFilters.keyword"
            class="employee-filter-search"
            allow-clear
            placeholder="搜索姓名、工号、部门或身份证号"
            @press-enter="loadEmployees"
          >
            <template #prefix><SearchOutlined /></template>
          </a-input>
          <a-select
            v-model:value="employeeFilters.organizationId"
            class="employee-organization-filter"
            allow-clear
            show-search
            option-filter-prop="label"
            placeholder="全部组织"
            :options="employeeOrganizationOptions"
          />
          <a-select
            v-model:value="employeeFilters.employmentStatus"
            class="employee-status-filter"
            allow-clear
            placeholder="全部人员状态"
            :options="employmentOptions"
          />
          <a-button class="employee-filter-button" @click="loadEmployees"
            >查询</a-button
          >
        </div>

        <div class="employee-desktop-table">
          <a-table
            size="middle"
            :loading="loading"
            :columns="employeeColumns"
            :data-source="employees"
            row-key="id"
            :scroll="{ x: 900 }"
            :pagination="{ pageSize: 20, showSizeChanger: false }"
            :custom-row="employeeRow"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                <div class="employee-table-person">
                  <span class="employee-avatar" aria-hidden="true">{{
                    employeeInitial(record.name)
                  }}</span>
                  <div>
                    <a-button
                      type="link"
                      class="table-link"
                      @click.stop="showEmployee(record.id)"
                      >{{ record.name }}</a-button
                    >
                    <span class="table-subtitle">{{
                      record.workNo || "未登记工号"
                    }}</span>
                  </div>
                </div>
              </template>
              <template v-else-if="column.key === 'organization'"
                ><strong class="employee-cell-primary">{{
                  record.organizationName || "未分配部门"
                }}</strong
                ><span class="table-subtitle">{{
                  record.position || record.organizationPath || "未设置岗位"
                }}</span></template
              >
              <template v-else-if="column.key === 'contact'"
                ><strong class="employee-cell-primary">{{
                  record.phone || "未登记手机号"
                }}</strong
                ><span class="table-subtitle">{{
                  record.idCard || "未登记身份证号"
                }}</span></template
              >
              <template v-else-if="column.key === 'entryDate'">{{
                record.entryDate || "-"
              }}</template>
              <template v-else-if="column.key === 'contract'"
                ><span v-if="record.contractEnd"
                  >{{ record.contractEnd
                  }}<a-tag
                    v-if="isContractExpiring(record.contractEnd)"
                    :color="contractTagColor(record.contractEnd)"
                    style="margin-left: 4px"
                    >{{ contractTagLabel(record.contractEnd) }}</a-tag
                  ></span
                ><span v-else>—</span></template
              >
              <template v-else-if="column.key === 'employmentStatus'"
                ><a-tag
                  :color="
                    record.employmentStatus === 'ACTIVE' ? 'green' : 'default'
                  "
                  >{{ employmentLabel(record.employmentStatus) }}</a-tag
                ></template
              >
              <template v-else-if="column.key === 'certificates'"
                ><strong>{{ record.validCertificateCount }}</strong
                ><span class="employee-cert-total">
                  / {{ record.certificateCount }}</span
                ></template
              >
              <template v-else-if="column.key === 'account'"
                ><a-tag
                  :color="
                    record.account?.enabled
                      ? 'green'
                      : record.account
                        ? 'red'
                        : 'default'
                  "
                  >{{ record.account?.username || "未开通" }}</a-tag
                ></template
              >
              <template v-else-if="column.key === 'actions'">
                <a-space :size="4">
                  <a-button
                    type="text"
                    size="small"
                    title="编辑员工"
                    aria-label="编辑员工"
                    @click.stop="openEmployee(record)"
                    ><template #icon><EditOutlined /></template
                  ></a-button>
                  <a-popconfirm
                    title="删除人员将同时删除其证书，确认继续？"
                    @confirm="removeEmployee(record.id)"
                    ><a-button
                      danger
                      type="text"
                      size="small"
                      title="删除员工"
                      aria-label="删除员工"
                      @click.stop
                      ><template #icon><DeleteOutlined /></template></a-button
                  ></a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </div>

        <div class="employee-mobile-list">
          <a-spin :spinning="loading">
            <a-empty
              v-if="!loading && !employees.length"
              description="暂无符合条件的员工"
            />
            <div v-else class="employee-mobile-items">
              <article
                v-for="record in employees"
                :key="record.id"
                class="employee-mobile-item"
                role="button"
                tabindex="0"
                @click="showEmployee(record.id)"
                @keydown.enter.prevent="showEmployee(record.id)"
              >
                <div class="employee-mobile-heading">
                  <span class="employee-avatar" aria-hidden="true">{{
                    employeeInitial(record.name)
                  }}</span>
                  <div class="employee-mobile-identity">
                    <div class="employee-mobile-name">
                      <strong>{{ record.name }}</strong
                      ><a-tag
                        :color="
                          record.employmentStatus === 'ACTIVE'
                            ? 'green'
                            : 'default'
                        "
                        >{{ employmentLabel(record.employmentStatus) }}</a-tag
                      >
                    </div>
                    <span
                      >{{ record.workNo || "未登记工号"
                      }}<template v-if="record.position">
                        · {{ record.position }}</template
                      ></span
                    >
                  </div>
                  <div class="employee-mobile-actions">
                    <a-button
                      v-if="auth.can('qualification:employee:manage')"
                      type="text"
                      size="small"
                      title="编辑员工"
                      aria-label="编辑员工"
                      @click.stop="openEmployee(record)"
                      ><template #icon><EditOutlined /></template
                    ></a-button>
                    <a-popconfirm
                      v-if="auth.can('qualification:employee:manage')"
                      title="删除人员将同时删除其证书，确认继续？"
                      @confirm="removeEmployee(record.id)"
                      ><a-button
                        danger
                        type="text"
                        size="small"
                        title="删除员工"
                        aria-label="删除员工"
                        @click.stop
                        ><template #icon><DeleteOutlined /></template></a-button
                    ></a-popconfirm>
                    <RightOutlined
                      class="employee-detail-arrow"
                      aria-hidden="true"
                    />
                  </div>
                </div>
                <div class="employee-mobile-meta">
                  <div>
                    <span>部门</span
                    ><strong>{{ record.organizationName || "未分配" }}</strong>
                  </div>
                  <div>
                    <span>登录账号</span
                    ><strong
                      :class="{
                        'employee-account-disabled':
                          record.account && !record.account.enabled,
                      }"
                      >{{ record.account?.username || "未开通" }}</strong
                    >
                  </div>
                  <div>
                    <span>有效证书</span
                    ><strong
                      >{{ record.validCertificateCount }} /
                      {{ record.certificateCount }}</strong
                    >
                  </div>
                </div>
              </article>
            </div>
          </a-spin>
        </div>
      </section>
    </template>

    <template v-else-if="mode === 'certificates'">
      <a-card title="人员证书台账">
        <a-space wrap class="table-toolbar">
          <a-input
            v-model:value="certificateFilters.keyword"
            allow-clear
            placeholder="证书 / 编号 / 人员 / 专业"
            @press-enter="loadCertificates"
          />
          <a-select
            v-model:value="certificateFilters.specialty"
            allow-clear
            show-search
            placeholder="证书专业"
            :options="specialtyOptions"
          />
          <a-select
            v-model:value="certificateFilters.status"
            allow-clear
            placeholder="状态"
            :options="statusOptions"
          />
          <a-button type="primary" @click="loadCertificates"
            ><template #icon><SearchOutlined /></template>查询</a-button
          >
          <a-button
            v-if="auth.can('qualification:certificate:manage')"
            @click="openCertificate()"
            ><template #icon><PlusOutlined /></template>新增证书</a-button
          >
        </a-space>
        <a-table
          :loading="loading"
          :columns="certificateColumns"
          :data-source="certificates"
          row-key="id"
          :scroll="{ x: 1450 }"
          :pagination="{ pageSize: 20 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'certificate'"
              ><strong>{{ record.name }}</strong
              ><span class="table-subtitle">{{
                record.certificateNo || "未识别编号"
              }}</span></template
            >
            <template v-else-if="column.key === 'employee'"
              ><strong>{{ record.employeeName }}</strong></template
            >
            <template v-else-if="column.key === 'status'"
              ><a-tag :color="statusColor(record.status)">{{
                statusLabel(record.status)
              }}</a-tag></template
            >
            <template v-else-if="column.key === 'registered'"
              ><a-tag :color="record.companyRegistered ? 'green' : 'default'">{{
                record.companyRegistered ? "本单位" : "非本单位"
              }}</a-tag></template
            >
            <template v-else-if="column.key === 'attachment'"
              ><a-button
                v-if="record.attachments.length"
                type="link"
                size="small"
                @click="showAttachments(record.attachments)"
                ><template #icon><EyeOutlined /></template
                >{{ record.attachments.length }}张</a-button
              ><span v-else>-</span></template
            >
            <template v-else-if="column.key === 'actions'"
              ><a-space
                ><a-button
                  type="text"
                  size="small"
                  @click="openCertificate(record)"
                  ><template #icon><EditOutlined /></template></a-button
                ><a-popconfirm
                  title="确认删除该人员证书？"
                  @confirm="removeCertificate(record.id)"
                  ><a-button danger type="text" size="small"
                    ><template #icon
                      ><DeleteOutlined /></template></a-button></a-popconfirm></a-space
            ></template>
          </template>
        </a-table>
      </a-card>
    </template>

    <template v-else-if="mode === 'performances'">
      <a-card title="项目业绩台账">
        <a-space wrap class="table-toolbar">
          <a-input
            v-model:value="performanceFilters.keyword"
            allow-clear
            placeholder="项目名称 / 客户 / 合同号"
            @press-enter="loadPerformances"
          />
          <a-select
            v-model:value="performanceFilters.subjectCompany"
            allow-clear
            placeholder="所属公司"
            :options="subjectOptions"
          />
          <a-select
            v-model:value="performanceFilters.projectType"
            allow-clear
            show-search
            placeholder="项目类型"
            :options="projectTypeOptions"
          />
          <a-button type="primary" @click="loadPerformances"
            ><template #icon><SearchOutlined /></template>查询</a-button
          >
          <a-button
            v-if="auth.can('qualification:performance:manage')"
            @click="openPerformance()"
            ><template #icon><PlusOutlined /></template>新增业绩</a-button
          >
        </a-space>
        <a-table
          :loading="loading"
          :columns="performanceColumns"
          :data-source="performances"
          row-key="id"
          :scroll="{ x: 1300 }"
          :pagination="{ pageSize: 20 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'"
              ><strong>{{ record.name }}</strong
              ><span class="table-subtitle">{{
                record.clientName || record.remark || "未登记客户"
              }}</span></template
            >
            <template v-else-if="column.key === 'attachment'"
              ><a-button
                v-if="record.attachments.length"
                type="link"
                size="small"
                @click="showAttachments(record.attachments)"
                ><template #icon><EyeOutlined /></template>查看原件</a-button
              ><span v-else>-</span></template
            >
            <template v-else-if="column.key === 'actions'"
              ><a-space
                ><a-button
                  type="text"
                  size="small"
                  @click="openPerformance(record)"
                  ><template #icon><EditOutlined /></template></a-button
                ><a-popconfirm
                  title="确认删除该项目业绩？"
                  @confirm="removePerformance(record.id)"
                  ><a-button danger type="text" size="small"
                    ><template #icon
                      ><DeleteOutlined /></template></a-button></a-popconfirm></a-space
            ></template>
          </template>
        </a-table>
      </a-card>
    </template>

    <template v-else-if="mode === 'tender'">
      <a-card title="投标人员资质组合查询">
        <a-row :gutter="12" class="table-toolbar">
          <a-col :xs="24" :lg="5"
            ><a-input
              v-model:value="tenderFilters.keyword"
              allow-clear
              placeholder="人员姓名 / 工号"
          /></a-col>
          <a-col :xs="24" :lg="10"
            ><a-select
              v-model:value="tenderFilters.specialties"
              mode="multiple"
              allow-clear
              show-search
              class="full-input"
              placeholder="同时具备的证书专业"
              :options="specialtyOptions"
          /></a-col>
          <a-col :xs="12" :lg="3"
            ><a-checkbox v-model:checked="tenderFilters.registeredOnly"
              >仅本单位注册</a-checkbox
            ></a-col
          >
          <a-col :xs="12" :lg="3"
            ><a-checkbox v-model:checked="tenderFilters.availableOnly"
              >仅可用</a-checkbox
            ></a-col
          >
          <a-col :xs="24" :lg="3"
            ><a-button type="primary" block @click="loadTender"
              ><template #icon><SearchOutlined /></template>查询</a-button
            ></a-col
          >
        </a-row>
        <a-space wrap class="table-toolbar"
          ><span class="muted">快捷组合</span
          ><a-button size="small" @click="applyTenderPreset"
            >登高 + 高压 + 低压</a-button
          ><a-tag color="blue"
            >匹配 {{ tenderEmployees.length }} 人</a-tag
          ></a-space
        >
        <a-table
          :loading="loading"
          :columns="tenderColumns"
          :data-source="tenderEmployees"
          row-key="employeeId"
          :pagination="{ pageSize: 15 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'employee'"
              ><strong>{{ record.employeeName }}</strong
              ><span class="table-subtitle">{{
                record.workNo || "无工号"
              }}</span></template
            >
            <template v-else-if="column.key === 'certificates'"
              ><a-space wrap
                ><a-tag
                  v-for="cert in record.certificates"
                  :key="cert.id"
                  :color="statusColor(cert.status)"
                  >{{ cert.specialty || cert.name }}</a-tag
                ></a-space
              ></template
            >
          </template>
        </a-table>
      </a-card>
    </template>

    <template v-else-if="mode === 'warnings'">
      <a-card title="资质预警中心">
        <a-row :gutter="16" class="metric-row">
          <a-col :xs="12" :lg="6"
            ><a-statistic title="全部预警" :value="warnings.length"
          /></a-col>
          <a-col :xs="12" :lg="6"
            ><a-statistic
              title="已逾期"
              :value="warnings.filter((i) => i.status === 'OVERDUE').length"
              :value-style="{ color: '#cf1322' }"
          /></a-col>
          <a-col :xs="12" :lg="6"
            ><a-statistic
              title="30天内"
              :value="
                warnings.filter((i) => i.daysLeft >= 0 && i.daysLeft <= 30)
                  .length
              "
          /></a-col>
          <a-col :xs="12" :lg="6"
            ><a-statistic
              title="90天内"
              :value="
                warnings.filter((i) => i.daysLeft > 30 && i.daysLeft <= 90)
                  .length
              "
          /></a-col>
        </a-row>
        <a-table
          :loading="loading"
          :columns="warningColumns"
          :data-source="warnings"
          :row-key="warningRowKey"
          :pagination="{ pageSize: 20 }"
          ><template #bodyCell="{ column, record }"
            ><template v-if="column.key === 'warningStatus'"
              ><a-tag :color="warningColor(record.level)">{{
                warningStatusLabel(record)
              }}</a-tag></template
            ></template
          ></a-table
        >
      </a-card>
    </template>

    <a-modal
      v-model:open="companyOpen"
      :title="companyEditingId ? '编辑公司资质' : '新增公司资质'"
      width="900px"
      :confirm-loading="saving"
      @ok="saveCompany"
    >
      <a-form ref="companyFormRef" :model="companyForm" layout="vertical">
        <a-row :gutter="14"
          ><a-col :span="8"
            ><a-form-item
              label="所属公司"
              name="subjectCompany"
              :rules="requiredRule"
              ><a-select
                v-model:value="companyForm.subjectCompany"
                show-search
                :options="subjectOptions" /></a-form-item></a-col
          ><a-col :span="10"
            ><a-form-item label="资质名称" name="name" :rules="requiredRule"
              ><a-input v-model:value="companyForm.name" /></a-form-item></a-col
          ><a-col :span="6"
            ><a-form-item label="类别" name="category" :rules="requiredRule"
              ><a-select
                v-model:value="companyForm.category"
                show-search
                :options="categoryOptions" /></a-form-item></a-col
        ></a-row>
        <a-row :gutter="14"
          ><a-col :span="8"
            ><a-form-item label="证书编号"
              ><a-input
                v-model:value="
                  companyForm.certificateNo
                " /></a-form-item></a-col
          ><a-col :span="6"
            ><a-form-item label="等级"
              ><a-input
                v-model:value="companyForm.level" /></a-form-item></a-col
          ><a-col :span="10"
            ><a-form-item label="发证机关"
              ><a-input
                v-model:value="companyForm.issuer" /></a-form-item></a-col
        ></a-row>
        <a-row :gutter="14"
          ><a-col :span="6"
            ><a-form-item label="发证日期"
              ><a-input
                v-model:value="companyForm.issueDate"
                type="date" /></a-form-item></a-col
          ><a-col :span="6"
            ><a-form-item label="有效期至"
              ><a-input
                v-model:value="companyForm.validTo"
                type="date" /></a-form-item></a-col
          ><a-col :span="6"
            ><a-form-item label="年审日期"
              ><a-input
                v-model:value="companyForm.annualReviewDate"
                type="date" /></a-form-item></a-col
          ><a-col :span="6"
            ><a-form-item label="延续日期"
              ><a-input
                v-model:value="companyForm.renewalDate"
                type="date" /></a-form-item></a-col
        ></a-row>
        <a-form-item label="适用项目"
          ><a-select
            v-model:value="companyForm.projectTypes"
            mode="tags"
            :options="projectTypeOptions"
        /></a-form-item>
        <a-form-item label="资质范围"
          ><a-textarea v-model:value="companyForm.scope" :rows="3"
        /></a-form-item>
        <a-row :gutter="14"
          ><a-col :span="12"
            ><a-form-item label="存放位置"
              ><a-input
                v-model:value="
                  companyForm.storageLocation
                " /></a-form-item></a-col
          ><a-col :span="6"
            ><a-form-item label="用于投标"
              ><a-switch
                v-model:checked="
                  companyForm.availableForTender
                " /></a-form-item></a-col
          ><a-col :span="6"
            ><a-form-item label="锁定"
              ><a-switch
                v-model:checked="companyForm.locked" /></a-form-item></a-col
        ></a-row>
        <a-form-item label="附件原件"
          ><a-space wrap
            ><a-upload
              :show-upload-list="false"
              :before-upload="uploadCompanyFile"
              ><a-button
                ><template #icon><UploadOutlined /></template>上传附件</a-button
              ></a-upload
            ><a-tag
              v-for="(item, index) in companyForm.attachments"
              :key="item.dataUrl"
              closable
              @close="companyForm.attachments.splice(index, 1)"
              >{{ item.name }}</a-tag
            ></a-space
          ></a-form-item
        >
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="employeeOpen"
      :title="employeeEditingId ? '编辑人员档案' : '新增人员档案'"
      width="820px"
      :confirm-loading="saving"
      @ok="saveEmployee"
    >
      <a-form ref="employeeFormRef" :model="employeeForm" layout="vertical">
        <a-row :gutter="14"
          ><a-col :span="12"
            ><a-form-item label="姓名" name="name" :rules="requiredRule"
              ><a-input
                v-model:value="employeeForm.name" /></a-form-item></a-col
          ><a-col :span="12"
            ><a-form-item label="工号"
              ><a-input
                v-model:value="employeeForm.workNo" /></a-form-item></a-col
        ></a-row>
        <a-row :gutter="14"
          ><a-col :span="10"
            ><a-form-item
              label="所属组织"
              name="organizationId"
              :rules="requiredRule"
              ><a-select
                v-model:value="employeeForm.organizationId"
                show-search
                option-filter-prop="label"
                placeholder="选择公司、部门或团队"
                :options="employeeOrganizationOptions" /></a-form-item></a-col
          ><a-col :span="7"
            ><a-form-item label="岗位"
              ><a-input
                v-model:value="employeeForm.position" /></a-form-item></a-col
          ><a-col :span="7"
            ><a-form-item label="状态"
              ><a-select
                v-model:value="employeeForm.employmentStatus"
                :options="employmentOptions" /></a-form-item></a-col
        ></a-row>
        <a-row :gutter="14"
          ><a-col :span="12"
            ><a-form-item label="身份证号"
              ><a-input
                v-model:value="employeeForm.idCard" /></a-form-item></a-col
          ><a-col :span="12"
            ><a-form-item label="手机号"
              ><a-input
                v-model:value="employeeForm.phone" /></a-form-item></a-col
        ></a-row>
        <a-row :gutter="14"
          ><a-col :span="24"
            ><a-form-item label="入职日期"
              ><a-input
                v-model:value="employeeForm.entryDate"
                type="date" /></a-form-item></a-col
        ></a-row>
        <a-row :gutter="14"
          ><a-col :span="12"
            ><a-form-item label="社保单位"
              ><a-input
                v-model:value="
                  employeeForm.socialSecurityUnit
                " /></a-form-item></a-col
          ><a-col :span="12"
            ><a-form-item label="社保起始"
              ><a-input
                v-model:value="employeeForm.socialSecurityStart"
                type="date" /></a-form-item></a-col
        ></a-row>
        <a-row :gutter="14"
          ><a-col :span="12"
            ><a-form-item label="社保截止"
              ><a-input
                v-model:value="employeeForm.socialSecurityEnd"
                type="date" /></a-form-item></a-col
          ><a-col :span="12"
            ><a-form-item label="合同起始"
              ><a-input
                v-model:value="employeeForm.contractStart"
                type="date" /></a-form-item></a-col
        ></a-row>
        <a-row :gutter="14"
          ><a-col :span="12"
            ><a-form-item label="合同截止"
              ><a-input
                v-model:value="employeeForm.contractEnd"
                type="date" /></a-form-item></a-col
          ><a-col :span="12"
            ><a-form-item label="备注"
              ><a-textarea
                v-model:value="employeeForm.remark"
                :rows="2" /></a-form-item></a-col
        ></a-row>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="certificateOpen"
      :title="certificateEditingId ? '编辑人员证书' : '新增人员证书'"
      width="860px"
      :confirm-loading="saving"
      @ok="saveCertificate"
    >
      <a-form
        ref="certificateFormRef"
        :model="certificateForm"
        layout="vertical"
      >
        <a-row :gutter="14"
          ><a-col :span="10"
            ><a-form-item
              label="所属人员"
              name="employeeId"
              :rules="requiredRule"
              ><a-select
                v-model:value="certificateForm.employeeId"
                show-search
                option-filter-prop="label"
                :options="employeeOptions" /></a-form-item></a-col
          ><a-col :span="8"
            ><a-form-item label="证书名称" name="name" :rules="requiredRule"
              ><a-input
                v-model:value="certificateForm.name" /></a-form-item></a-col
          ><a-col :span="6"
            ><a-form-item label="证书类型"
              ><a-select
                v-model:value="certificateForm.type"
                show-search
                :options="certificateTypeOptions" /></a-form-item></a-col
        ></a-row>
        <a-row :gutter="14"
          ><a-col :span="8"
            ><a-form-item label="证书编号"
              ><a-input
                v-model:value="
                  certificateForm.certificateNo
                " /></a-form-item></a-col
          ><a-col :span="8"
            ><a-form-item label="专业"
              ><a-select
                v-model:value="certificateForm.specialty"
                show-search
                :options="specialtyOptions" /></a-form-item></a-col
          ><a-col :span="8"
            ><a-form-item label="发证日期"
              ><a-input
                v-model:value="certificateForm.issueDate"
                type="date" /></a-form-item></a-col
        ></a-row>
        <a-row :gutter="14"
          ><a-col :span="8"
            ><a-form-item label="有效期至"
              ><a-input
                v-model:value="certificateForm.validTo"
                type="date" /></a-form-item></a-col
          ><a-col :span="8"
            ><a-form-item label="复审日期"
              ><a-input
                v-model:value="certificateForm.reviewDate"
                type="date" /></a-form-item></a-col
          ><a-col :span="4"
            ><a-form-item label="本单位注册"
              ><a-switch
                v-model:checked="
                  certificateForm.companyRegistered
                " /></a-form-item></a-col
          ><a-col :span="4"
            ><a-form-item label="可投标"
              ><a-switch
                v-model:checked="
                  certificateForm.availableForTender
                " /></a-form-item></a-col
        ></a-row>
        <a-form-item label="证书原件"
          ><a-space wrap
            ><a-upload
              :show-upload-list="false"
              :before-upload="uploadCertificateFile"
              ><a-button
                ><template #icon><UploadOutlined /></template>上传附件</a-button
              ></a-upload
            ><a-tag
              v-for="(item, index) in certificateForm.attachments"
              :key="item.dataUrl"
              closable
              @close="certificateForm.attachments.splice(index, 1)"
              >{{ item.name }}</a-tag
            ></a-space
          ></a-form-item
        >
        <a-form-item label="备注"
          ><a-textarea v-model:value="certificateForm.remark" :rows="2"
        /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="performanceOpen"
      :title="performanceEditingId ? '编辑项目业绩' : '新增项目业绩'"
      width="820px"
      :confirm-loading="saving"
      @ok="savePerformance"
    >
      <a-form
        ref="performanceFormRef"
        :model="performanceForm"
        layout="vertical"
      >
        <a-row :gutter="14"
          ><a-col :span="8"
            ><a-form-item
              label="所属公司"
              name="subjectCompany"
              :rules="requiredRule"
              ><a-select
                v-model:value="performanceForm.subjectCompany"
                show-search
                :options="subjectOptions" /></a-form-item></a-col
          ><a-col :span="16"
            ><a-form-item label="项目名称" name="name" :rules="requiredRule"
              ><a-input
                v-model:value="performanceForm.name" /></a-form-item></a-col
        ></a-row>
        <a-row :gutter="14"
          ><a-col :span="8"
            ><a-form-item label="客户名称"
              ><a-input
                v-model:value="
                  performanceForm.clientName
                " /></a-form-item></a-col
          ><a-col :span="8"
            ><a-form-item label="合同编号"
              ><a-input
                v-model:value="
                  performanceForm.contractNo
                " /></a-form-item></a-col
          ><a-col :span="8"
            ><a-form-item label="合同日期"
              ><a-input
                v-model:value="performanceForm.contractDate"
                type="date" /></a-form-item></a-col
        ></a-row>
        <a-row :gutter="14"
          ><a-col :span="8"
            ><a-form-item label="合同金额"
              ><a-input
                v-model:value="
                  performanceForm.contractAmount
                " /></a-form-item></a-col
          ><a-col :span="16"
            ><a-form-item label="项目类型"
              ><a-select
                v-model:value="performanceForm.projectType"
                show-search
                :options="projectTypeOptions" /></a-form-item></a-col
        ></a-row>
        <a-form-item label="业绩附件"
          ><a-space wrap
            ><a-upload
              :show-upload-list="false"
              :before-upload="uploadPerformanceFile"
              ><a-button
                ><template #icon><UploadOutlined /></template>上传附件</a-button
              ></a-upload
            ><a-tag
              v-for="(item, index) in performanceForm.attachments"
              :key="item.dataUrl"
              closable
              @close="performanceForm.attachments.splice(index, 1)"
              >{{ item.name }}</a-tag
            ></a-space
          ></a-form-item
        >
        <a-form-item label="备注"
          ><a-textarea v-model:value="performanceForm.remark" :rows="3"
        /></a-form-item>
      </a-form>
    </a-modal>

    <a-drawer
      v-model:open="employeeDetailOpen"
      title="员工档案详情"
      width="960px"
    >
      <template v-if="employeeDetail" #extra>
        <a-button type="link" size="small" @click="goToDetail">
          <template #icon><ArrowRightOutlined /></template>查看完整档案
        </a-button>
      </template>
      <template v-if="employeeDetail">
        <a-descriptions bordered :column="2" size="small"
          ><a-descriptions-item label="姓名">{{
            employeeDetail.employee.name
          }}</a-descriptions-item
          ><a-descriptions-item label="状态"
            ><a-tag
              :color="
                statusColor(
                  employeeDetail.employee
                    .employmentStatus as QualificationStatus,
                )
              "
              >{{
                employmentLabel(employeeDetail.employee.employmentStatus)
              }}</a-tag
            ></a-descriptions-item
          ><a-descriptions-item label="工号">{{
            employeeDetail.employee.workNo || "-"
          }}</a-descriptions-item
          ><a-descriptions-item label="部门岗位"
            >{{ employeeDetail.employee.organizationName || "未分配" }} /
            {{ employeeDetail.employee.position || "-" }}</a-descriptions-item
          ><a-descriptions-item label="入职日期">{{
            employeeDetail.employee.entryDate || "-"
          }}</a-descriptions-item
          ><a-descriptions-item label="身份证号">{{
            employeeDetail.employee.idCard || "-"
          }}</a-descriptions-item
          ><a-descriptions-item label="手机号">{{
            employeeDetail.employee.phone || "-"
          }}</a-descriptions-item
          ><a-descriptions-item label="社保单位">{{
            employeeDetail.employee.socialSecurityUnit || "-"
          }}</a-descriptions-item
          ><a-descriptions-item label="社保起始">{{
            employeeDetail.employee.socialSecurityStart || "-"
          }}</a-descriptions-item
          ><a-descriptions-item label="社保截止">{{
            employeeDetail.employee.socialSecurityEnd || "-"
          }}</a-descriptions-item
          ><a-descriptions-item label="合同起始">{{
            employeeDetail.employee.contractStart || "-"
          }}</a-descriptions-item
          ><a-descriptions-item label="合同截止"
            ><span v-if="employeeDetail.employee.contractEnd"
              >{{ employeeDetail.employee.contractEnd
              }}<a-tag
                v-if="isContractExpiring(employeeDetail.employee.contractEnd)"
                :color="contractTagColor(employeeDetail.employee.contractEnd)"
                style="margin-left: 6px"
                >{{
                  contractTagLabel(employeeDetail.employee.contractEnd)
                }}</a-tag
              ></span
            ><span v-else>无固定期限</span></a-descriptions-item
          ><a-descriptions-item label="组织路径" :span="2">{{
            employeeDetail.employee.organizationPath || "未分配组织"
          }}</a-descriptions-item></a-descriptions
        >
        <a-tabs
          v-model:active-key="employeeDetailTab"
          class="employee-detail-tabs"
        >
          <a-tab-pane key="contracts" tab="员工合同">
            <a-space class="table-toolbar"
              ><a-button
                v-if="auth.can('qualification:employee:manage')"
                type="primary"
                @click="openEmployeeContract()"
                ><template #icon><PlusOutlined /></template>新增合同</a-button
              ></a-space
            >
            <a-table
              size="small"
              :columns="employeeContractColumns"
              :data-source="employeeDetail.contracts"
              row-key="id"
              :pagination="false"
              :scroll="{ x: 900 }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'contract'"
                  ><strong>{{ record.contractNo }}</strong
                  ><span class="table-subtitle">{{
                    contractTypeLabel(record.contractType)
                  }}</span></template
                >
                <template v-else-if="column.key === 'period'"
                  >{{ record.startDate }} 至
                  {{ record.endDate || "长期" }}</template
                >
                <template v-else-if="column.key === 'status'"
                  ><a-tag :color="contractStatusColor(record.status)">{{
                    contractStatusLabel(record.status)
                  }}</a-tag></template
                >
                <template v-else-if="column.key === 'attachments'"
                  ><a-button
                    v-if="record.attachments.length"
                    type="link"
                    size="small"
                    @click="showAttachments(record.attachments)"
                    >{{ record.attachments.length }}份</a-button
                  ><span v-else>-</span></template
                >
                <template v-else-if="column.key === 'actions'"
                  ><a-space
                    ><a-button
                      type="text"
                      size="small"
                      @click="openEmployeeContract(record)"
                      ><template #icon><EditOutlined /></template></a-button
                    ><a-popconfirm
                      title="确认删除该员工合同？"
                      @confirm="removeEmployeeContract(record.id)"
                      ><a-button danger type="text" size="small"
                        ><template #icon
                          ><DeleteOutlined /></template></a-button></a-popconfirm></a-space
                ></template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="certificates" tab="人员证书">
            <a-table
              size="small"
              :columns="detailCertificateColumns"
              :data-source="employeeDetail.certificates"
              row-key="id"
              :pagination="false"
              ><template #bodyCell="{ column, record }"
                ><template v-if="column.key === 'status'"
                  ><a-tag :color="statusColor(record.status)">{{
                    statusLabel(record.status)
                  }}</a-tag></template
                ><template v-else-if="column.key === 'attachment'"
                  ><a-button
                    v-if="record.attachments.length"
                    type="link"
                    size="small"
                    @click="showAttachments(record.attachments)"
                    >查看</a-button
                  ></template
                ></template
              ></a-table
            >
          </a-tab-pane>
          <a-tab-pane key="account" tab="登录账号">
            <template v-if="employeeDetail.employee.account">
              <a-descriptions bordered :column="2" size="small"
                ><a-descriptions-item label="登录名">{{
                  employeeDetail.employee.account.username
                }}</a-descriptions-item
                ><a-descriptions-item label="状态"
                  ><a-tag
                    :color="
                      employeeDetail.employee.account.enabled ? 'green' : 'red'
                    "
                    >{{
                      employeeDetail.employee.account.enabled ? "启用" : "禁用"
                    }}</a-tag
                  ></a-descriptions-item
                ><a-descriptions-item label="账号姓名">{{
                  employeeDetail.employee.account.displayName
                }}</a-descriptions-item
                ><a-descriptions-item label="角色">{{
                  employeeDetail.employee.account.roles.join("、") || "-"
                }}</a-descriptions-item
                ><a-descriptions-item label="手机号">{{
                  employeeDetail.employee.account.phone || "-"
                }}</a-descriptions-item
                ><a-descriptions-item label="邮箱">{{
                  employeeDetail.employee.account.email || "-"
                }}</a-descriptions-item></a-descriptions
              >
              <a-space class="account-actions"
                ><a-button
                  v-if="auth.can('system:user:update')"
                  type="primary"
                  @click="openEmployeeAccount"
                  >编辑账号</a-button
                ><a-button
                  v-if="auth.can('system:user:reset-password')"
                  @click="openPasswordReset"
                  >重置密码</a-button
                ><a-popconfirm
                  title="仅解除员工与账号的关联，账号不会删除。确认继续？"
                  @confirm="unlinkEmployeeAccount"
                  ><a-button
                    v-if="auth.can('qualification:employee:manage')"
                    danger
                    >解除关联</a-button
                  ></a-popconfirm
                ></a-space
              >
            </template>
            <a-empty v-else description="该员工尚未开通登录账号"
              ><a-button
                v-if="
                  auth.can('system:user:create') &&
                  auth.can('qualification:employee:manage')
                "
                type="primary"
                @click="openEmployeeAccount"
                >开通登录账号</a-button
              ></a-empty
            >
          </a-tab-pane>
        </a-tabs>
      </template>
    </a-drawer>

    <a-modal
      v-model:open="employeeContractOpen"
      :title="employeeContractEditingId ? '编辑员工合同' : '新增员工合同'"
      width="760px"
      :confirm-loading="saving"
      @ok="saveEmployeeContract"
    >
      <a-form
        ref="employeeContractFormRef"
        :model="employeeContractForm"
        layout="vertical"
      >
        <a-row :gutter="14"
          ><a-col :span="12"
            ><a-form-item
              label="合同编号"
              name="contractNo"
              :rules="requiredRule"
              ><a-input
                v-model:value="
                  employeeContractForm.contractNo
                " /></a-form-item></a-col
          ><a-col :span="12"
            ><a-form-item
              label="合同类型"
              name="contractType"
              :rules="requiredRule"
              ><a-select
                v-model:value="employeeContractForm.contractType"
                :options="contractTypeOptions" /></a-form-item></a-col
        ></a-row>
        <a-row :gutter="14"
          ><a-col :span="8"
            ><a-form-item label="签订日期"
              ><a-input
                v-model:value="employeeContractForm.signDate"
                type="date" /></a-form-item></a-col
          ><a-col :span="8"
            ><a-form-item
              label="开始日期"
              name="startDate"
              :rules="requiredRule"
              ><a-input
                v-model:value="employeeContractForm.startDate"
                type="date" /></a-form-item></a-col
          ><a-col :span="8"
            ><a-form-item label="结束日期"
              ><a-input
                v-model:value="employeeContractForm.endDate"
                type="date" /></a-form-item></a-col
        ></a-row>
        <a-row :gutter="14"
          ><a-col :span="12"
            ><a-form-item label="试用期截止"
              ><a-input
                v-model:value="employeeContractForm.probationEndDate"
                type="date" /></a-form-item></a-col
          ><a-col :span="12"
            ><a-form-item label="合同状态" name="status" :rules="requiredRule"
              ><a-select
                v-model:value="employeeContractForm.status"
                :options="contractStatusOptions" /></a-form-item></a-col
        ></a-row>
        <a-form-item label="合同附件"
          ><a-space wrap
            ><a-upload
              :show-upload-list="false"
              :before-upload="uploadEmployeeContractFile"
              ><a-button
                ><template #icon><UploadOutlined /></template>上传合同</a-button
              ></a-upload
            ><a-tag
              v-for="(item, index) in employeeContractForm.attachments"
              :key="item.dataUrl"
              closable
              @close="employeeContractForm.attachments.splice(index, 1)"
              >{{ item.name }}</a-tag
            ></a-space
          ></a-form-item
        >
        <a-form-item label="备注"
          ><a-textarea v-model:value="employeeContractForm.remark" :rows="2"
        /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="employeeAccountOpen"
      :title="accountEditingId ? '编辑登录账号' : '开通登录账号'"
      width="640px"
      :confirm-loading="saving"
      @ok="saveEmployeeAccount"
    >
      <a-form
        ref="employeeAccountFormRef"
        :model="employeeAccountForm"
        layout="vertical"
      >
        <a-row :gutter="14"
          ><a-col :span="12"
            ><a-form-item label="登录名" name="username" :rules="requiredRule"
              ><a-input
                v-model:value="employeeAccountForm.username"
                :disabled="Boolean(accountEditingId)" /></a-form-item></a-col
          ><a-col :span="12"
            ><a-form-item
              v-if="!accountEditingId"
              label="初始密码"
              name="password"
              :rules="passwordRules"
              ><a-input-password
                v-model:value="employeeAccountForm.password" /></a-form-item
            ><a-form-item v-else label="账号状态"
              ><a-switch
                v-model:checked="employeeAccountForm.enabled"
                checked-children="启用"
                un-checked-children="禁用" /></a-form-item></a-col
        ></a-row>
        <a-row :gutter="14"
          ><a-col :span="12"
            ><a-form-item label="姓名" name="displayName" :rules="requiredRule"
              ><a-input
                v-model:value="
                  employeeAccountForm.displayName
                " /></a-form-item></a-col
          ><a-col :span="12"
            ><a-form-item label="手机号"
              ><a-input
                v-model:value="
                  employeeAccountForm.phone
                " /></a-form-item></a-col
        ></a-row>
        <a-form-item label="邮箱"
          ><a-input v-model:value="employeeAccountForm.email"
        /></a-form-item>
        <a-row :gutter="14"
          ><a-col :span="12"
            ><a-form-item label="所属组织"
              ><a-select
                v-model:value="employeeAccountForm.orgId"
                disabled
                :options="organizationOptions" /></a-form-item></a-col
          ><a-col :span="12"
            ><a-form-item label="角色"
              ><a-select
                v-model:value="employeeAccountForm.roleIds"
                mode="multiple"
                :options="roleOptions" /></a-form-item></a-col
        ></a-row>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="passwordResetOpen"
      title="重置登录密码"
      :confirm-loading="saving"
      @ok="resetEmployeePassword"
    >
      <a-form layout="vertical"
        ><a-form-item label="新密码" required
          ><a-input-password v-model:value="newPassword" /></a-form-item
      ></a-form>
    </a-modal>

    <a-modal
      v-model:open="attachmentOpen"
      title="附件原件"
      width="900px"
      :footer="null"
    >
      <div class="qualification-attachments">
        <figure v-for="item in previewAttachments" :key="item.dataUrl">
          <a
            v-if="
              item.type === 'application/pdf' ||
              item.dataUrl.toLowerCase().endsWith('.pdf')
            "
            :href="item.dataUrl"
            target="_blank"
            rel="noopener"
            >打开 PDF 原件</a
          ><img v-else :src="item.dataUrl" :alt="item.name" />
          <figcaption>{{ item.name }}</figcaption>
        </figure>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { message, type FormInstance } from "ant-design-vue";
import DeleteOutlined from "@ant-design/icons-vue/DeleteOutlined";
import EditOutlined from "@ant-design/icons-vue/EditOutlined";
import EyeOutlined from "@ant-design/icons-vue/EyeOutlined";
import PlusOutlined from "@ant-design/icons-vue/PlusOutlined";
import RightOutlined from "@ant-design/icons-vue/RightOutlined";
import SearchOutlined from "@ant-design/icons-vue/SearchOutlined";
import UploadOutlined from "@ant-design/icons-vue/UploadOutlined";
import { useAuthStore } from "@/stores/auth";
import {
  createCompanyQualification,
  createEmployeeContract,
  createPersonnelCertificate,
  createQualificationEmployee,
  createQualificationPerformance,
  deleteCompanyQualification,
  deleteEmployeeContract,
  deletePersonnelCertificate,
  deleteQualificationEmployee,
  deleteQualificationPerformance,
  getQualificationDashboard,
  getQualificationEmployee,
  getQualificationReferences,
  listCompanyQualifications,
  listPersonnelCertificates,
  listQualificationEmployees,
  listQualificationPerformances,
  listQualificationWarnings,
  searchTenderQualifications,
  updateCompanyQualification,
  updateEmployeeContract,
  updatePersonnelCertificate,
  updateQualificationEmployee,
  updateQualificationPerformance,
  type Attachment,
  type CompanyQualification,
  type CompanyQualificationPayload,
  type EmployeeContract,
  type EmployeeContractPayload,
  type EmployeeDetail,
  type EmployeePayload,
  type PerformancePayload,
  type PersonnelCertificate,
  type CertificatePayload,
  type QualificationDashboard,
  type QualificationEmployee,
  type QualificationPerformance,
  type QualificationReferences,
  type QualificationStatus,
  type QualificationWarning,
  type TenderEmployee,
  getQualificationAttachmentObjectUrl,
  uploadQualificationAttachment,
} from "@/api/qualification";
import {
  createUserApi,
  getUserApi,
  listOrganizationsFlatApi,
  listRolesApi,
  listUsersApi,
  resetPasswordApi,
  updateUserApi,
  type OrganizationResponse,
  type RoleResponse,
  type UserResponse,
} from "@/api/system";

const emit = defineEmits<{ dataChanged: [] }>();

type QualificationViewMode =
  | "dashboard"
  | "companies"
  | "employees"
  | "certificates"
  | "performances"
  | "tender"
  | "warnings";

const props = defineProps<{ displayMode?: QualificationViewMode }>();
const route = useRoute();
const auth = useAuthStore();
const mode = computed<QualificationViewMode>(
  () =>
    props.displayMode ||
    (route.path.split("/").pop() as QualificationViewMode) ||
    "dashboard",
);
const loading = ref(false);
const saving = ref(false);
const references = reactive<QualificationReferences>({
  subjectCompanies: [],
  qualificationCategories: [],
  certificateTypes: [],
  specialties: [],
  projectTypes: [],
  employees: [],
  organizations: [],
});
const dashboard = reactive<QualificationDashboard>({
  companyQualificationCount: 0,
  employeeCount: 0,
  certificateCount: 0,
  tenderAvailableCertificateCount: 0,
  pendingWarningCount: 0,
  expiredCount: 0,
  companyCategoryDistribution: [],
  certificateSpecialtyDistribution: [],
  recentWarnings: [],
});
const companies = ref<CompanyQualification[]>([]);
const employees = ref<QualificationEmployee[]>([]);
const certificates = ref<PersonnelCertificate[]>([]);
const performances = ref<QualificationPerformance[]>([]);
const tenderEmployees = ref<TenderEmployee[]>([]);
const warnings = ref<QualificationWarning[]>([]);

const subjectOptions = computed(() =>
  references.subjectCompanies.map((value) => ({ label: value, value })),
);
const categoryOptions = computed(() =>
  references.qualificationCategories.map((value) => ({ label: value, value })),
);
const certificateTypeOptions = computed(() =>
  references.certificateTypes.map((value) => ({ label: value, value })),
);
const specialtyOptions = computed(() =>
  references.specialties.map((value) => ({ label: value, value })),
);
const projectTypeOptions = computed(() =>
  references.projectTypes.map((value) => ({ label: value, value })),
);
const employeeOptions = computed(() =>
  references.employees.map((item) => ({
    label: `${item.name} · ${item.workNo || "无工号"}`,
    value: item.id,
  })),
);
const employeeOrganizationOptions = computed(() =>
  references.organizations
    .filter((item) => item.enabled)
    .map((item) => ({ label: item.fullPath || item.name, value: item.id })),
);
const accountUsers = ref<UserResponse[]>([]);
const accountRoles = ref<RoleResponse[]>([]);
const accountOrganizations = ref<OrganizationResponse[]>([]);
const accountOptions = computed(() =>
  accountUsers.value
    .filter(
      (user) =>
        !employees.value.some(
          (employee) =>
            employee.systemUserId === user.id &&
            employee.id !== employeeEditingId.value,
        ),
    )
    .map((user) => ({
      label: `${user.displayName} · ${user.username}`,
      value: user.id,
    })),
);
const roleOptions = computed(() =>
  accountRoles.value.map((item) => ({ label: item.name, value: item.id })),
);
const organizationOptions = computed(() =>
  accountOrganizations.value.map((item) => ({
    label: item.fullPath || item.name,
    value: item.id,
  })),
);
const activeEmployeeCount = computed(
  () =>
    employees.value.filter((employee) => employee.employmentStatus === "ACTIVE")
      .length,
);
const linkedAccountCount = computed(
  () => employees.value.filter((employee) => Boolean(employee.account)).length,
);
const statusOptions = [
  "VALID",
  "EXPIRING",
  "EXPIRED",
  "UNVERIFIED",
  "LOCKED",
  "VOIDED",
].map((value) => ({ value, label: statusLabel(value as QualificationStatus) }));
const employmentOptions = [
  { value: "ACTIVE", label: "在职" },
  { value: "LEFT", label: "离职" },
  { value: "DISABLED", label: "停用" },
];
const contractTypeOptions = [
  { value: "FIXED_TERM", label: "固定期限" },
  { value: "OPEN_ENDED", label: "无固定期限" },
  { value: "PART_TIME", label: "非全日制" },
  { value: "INTERNSHIP", label: "实习协议" },
  { value: "OTHER", label: "其他" },
];
const contractStatusOptions = [
  { value: "ACTIVE", label: "履行中" },
  { value: "DRAFT", label: "待生效" },
  { value: "TERMINATED", label: "已终止" },
];
const requiredRule = [{ required: true, message: "请填写必填项" }];
const passwordRules = [
  { required: true, message: "请输入初始密码" },
  { min: 6, message: "密码至少6位" },
];
const dashboardMetrics = computed(() => [
  { title: "公司资质", value: dashboard.companyQualificationCount },
  { title: "资质人员", value: dashboard.employeeCount },
  { title: "人员证书", value: dashboard.certificateCount },
  { title: "投标可用证书", value: dashboard.tenderAvailableCertificateCount },
  {
    title: "待处理预警",
    value: dashboard.pendingWarningCount,
    danger: dashboard.pendingWarningCount > 0,
  },
  {
    title: "已过期",
    value: dashboard.expiredCount,
    danger: dashboard.expiredCount > 0,
  },
]);
const distributionColumns = [
  { title: "分类", dataIndex: "name" },
  { title: "数量", dataIndex: "count", width: 120 },
];
const warningColumns = [
  { title: "预警事项", dataIndex: "title", width: 150 },
  { title: "对象", dataIndex: "sourceName" },
  { title: "到期日期", dataIndex: "dueDate", width: 130 },
  { title: "剩余天数", dataIndex: "daysLeft", width: 110 },
  { title: "状态", key: "warningStatus", width: 100 },
];
const companyColumns = [
  { title: "资质", key: "name", width: 260 },
  { title: "所属公司", dataIndex: "subjectCompany", width: 130 },
  { title: "类别", dataIndex: "category", width: 120 },
  { title: "等级", dataIndex: "level", width: 130 },
  { title: "有效期至", dataIndex: "validTo", width: 120 },
  { title: "状态", key: "status", width: 100 },
  { title: "投标", key: "tender", width: 90 },
  { title: "附件", key: "attachment", width: 90 },
  { title: "操作", key: "actions", width: 100, fixed: "right" },
];
const employeeColumns = [
  { title: "员工", key: "name", width: 180 },
  { title: "部门 / 岗位", key: "organization", width: 160 },
  { title: "联系方式 / 身份证", key: "contact", width: 180 },
  { title: "入职日期", key: "entryDate", width: 110 },
  { title: "合同状态", key: "contract", width: 110 },
  { title: "状态", key: "employmentStatus", width: 78 },
  { title: "登录账号", key: "account", width: 112 },
  { title: "有效 / 全部证书", key: "certificates", width: 112 },
  { title: "操作", key: "actions", width: 74, fixed: "right" },
];
const certificateColumns = [
  { title: "证书", key: "certificate", width: 260 },
  { title: "人员", key: "employee", width: 170 },
  { title: "类型", dataIndex: "type", width: 110 },
  { title: "专业", dataIndex: "specialty", width: 150 },
  { title: "有效期至", dataIndex: "validTo", width: 120 },
  { title: "复审日期", dataIndex: "reviewDate", width: 120 },
  { title: "注册", key: "registered", width: 90 },
  { title: "状态", key: "status", width: 100 },
  { title: "原件", key: "attachment", width: 90 },
  { title: "操作", key: "actions", width: 100, fixed: "right" },
];
const performanceColumns = [
  { title: "项目名称", key: "name", width: 420 },
  { title: "所属公司", dataIndex: "subjectCompany", width: 130 },
  { title: "项目类型", dataIndex: "projectType", width: 160 },
  { title: "合同金额", dataIndex: "contractAmount", width: 130 },
  { title: "合同日期", dataIndex: "contractDate", width: 120 },
  { title: "附件", key: "attachment", width: 110 },
  { title: "操作", key: "actions", width: 100, fixed: "right" },
];
const tenderColumns = [
  { title: "人员", key: "employee", width: 220 },
  { title: "部门", dataIndex: "department", width: 150 },
  { title: "岗位", dataIndex: "position", width: 140 },
  { title: "匹配证书", key: "certificates" },
];
const detailCertificateColumns = [
  { title: "证书", dataIndex: "name" },
  { title: "专业", dataIndex: "specialty", width: 150 },
  { title: "有效期至", dataIndex: "validTo", width: 120 },
  { title: "状态", key: "status", width: 90 },
  { title: "原件", key: "attachment", width: 70 },
];
const employeeContractColumns = [
  { title: "合同", key: "contract", width: 190 },
  { title: "合同期限", key: "period", width: 220 },
  { title: "签订日期", dataIndex: "signDate", width: 110 },
  { title: "状态", key: "status", width: 90 },
  { title: "附件", key: "attachments", width: 80 },
  { title: "操作", key: "actions", width: 90, fixed: "right" },
];

const companyFilters = reactive({
  keyword: "",
  subjectCompany: undefined as string | undefined,
  status: undefined as string | undefined,
});
const employeeFilters = reactive({
  keyword: "",
  organizationId:
    typeof route.query.organizationId === "string"
      ? route.query.organizationId
      : (undefined as string | undefined),
  employmentStatus: undefined as string | undefined,
});
const certificateFilters = reactive({
  keyword: "",
  specialty: undefined as string | undefined,
  status: undefined as string | undefined,
});
const performanceFilters = reactive({
  keyword: "",
  subjectCompany: undefined as string | undefined,
  projectType: undefined as string | undefined,
});
const tenderFilters = reactive({
  keyword: "",
  specialties: [] as string[],
  registeredOnly: false,
  availableOnly: true,
});

const companyOpen = ref(false);
const companyEditingId = ref("");
const companyFormRef = ref<FormInstance>();
const companyForm = reactive<CompanyQualificationPayload>(emptyCompany());
const employeeOpen = ref(false);
const employeeEditingId = ref("");
const employeeFormRef = ref<FormInstance>();
const employeeForm = reactive<EmployeePayload>(emptyEmployee());
const certificateOpen = ref(false);
const certificateEditingId = ref("");
const certificateFormRef = ref<FormInstance>();
const certificateForm = reactive<CertificatePayload>(emptyCertificate());
const performanceOpen = ref(false);
const performanceEditingId = ref("");
const performanceFormRef = ref<FormInstance>();
const performanceForm = reactive<PerformancePayload>(emptyPerformance());
const employeeDetailOpen = ref(false);
const employeeDetail = ref<EmployeeDetail>();
const employeeDetailTab = ref("contracts");
const employeeContractOpen = ref(false);
const employeeContractEditingId = ref("");
const employeeContractFormRef = ref<FormInstance>();
const employeeContractForm = reactive<EmployeeContractPayload>(
  emptyEmployeeContract(),
);
const employeeAccountOpen = ref(false);
const accountEditingId = ref("");
const employeeAccountFormRef = ref<FormInstance>();
const employeeAccountForm = reactive({
  username: "",
  password: "",
  displayName: "",
  phone: "",
  email: "",
  enabled: true,
  orgId: undefined as string | undefined,
  roleIds: [] as string[],
});
const passwordResetOpen = ref(false);
const newPassword = ref("");
const attachmentOpen = ref(false);
const previewAttachments = ref<Attachment[]>([]);

watch([() => route.path, () => props.displayMode], loadActive, {
  immediate: true,
});
watch(
  () => route.query.organizationId,
  (value) => {
    if (mode.value !== "employees") return;
    employeeFilters.organizationId =
      typeof value === "string" ? value : undefined;
    loadEmployees();
  },
);

async function ensureReferences() {
  if (!references.subjectCompanies.length)
    Object.assign(references, await getQualificationReferences());
}
async function loadActive() {
  loading.value = true;
  try {
    await ensureReferences();
    if (mode.value === "dashboard")
      Object.assign(dashboard, await getQualificationDashboard());
    else if (mode.value === "companies") await loadCompanies();
    else if (mode.value === "employees")
      await Promise.all([loadEmployees(), loadAccountReferences()]);
    else if (mode.value === "certificates") await loadCertificates();
    else if (mode.value === "performances") await loadPerformances();
    else if (mode.value === "tender") await loadTender();
    else if (mode.value === "warnings")
      warnings.value = await listQualificationWarnings();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "资质数据加载失败");
  } finally {
    loading.value = false;
  }
}
async function loadCompanies() {
  loading.value = true;
  try {
    companies.value = await listCompanyQualifications(
      cleanParams(companyFilters),
    );
  } finally {
    loading.value = false;
  }
}
async function loadEmployees() {
  loading.value = true;
  try {
    employees.value = await listQualificationEmployees(
      cleanParams(employeeFilters),
    );
  } finally {
    loading.value = false;
  }
}
async function loadAccountReferences() {
  if (auth.can("system:user:view"))
    accountUsers.value = (await listUsersApi(0, 200)).content;
  if (auth.can("system:role:view"))
    accountRoles.value = (await listRolesApi(0, 200)).content;
  if (auth.can("system:organization:view"))
    accountOrganizations.value = await listOrganizationsFlatApi();
}
async function loadCertificates() {
  loading.value = true;
  try {
    certificates.value = await listPersonnelCertificates(
      cleanParams(certificateFilters),
    );
  } finally {
    loading.value = false;
  }
}
async function loadPerformances() {
  loading.value = true;
  try {
    performances.value = await listQualificationPerformances(
      cleanParams(performanceFilters),
    );
  } finally {
    loading.value = false;
  }
}
async function loadTender() {
  loading.value = true;
  try {
    tenderEmployees.value = await searchTenderQualifications({
      ...tenderFilters,
    });
  } finally {
    loading.value = false;
  }
}

function openCompany(record?: CompanyQualification) {
  companyEditingId.value = record?.id || "";
  Object.assign(
    companyForm,
    record
      ? {
          ...record,
          projectTypes: [...record.projectTypes],
          attachments: [...record.attachments],
        }
      : emptyCompany(),
  );
  companyOpen.value = true;
}
async function saveCompany() {
  await companyFormRef.value?.validate();
  saving.value = true;
  try {
    if (companyEditingId.value)
      await updateCompanyQualification(companyEditingId.value, companyForm);
    else await createCompanyQualification(companyForm);
    companyOpen.value = false;
    message.success("公司资质已保存");
    await refreshReferences();
    await loadCompanies();
    emit("dataChanged");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "保存失败");
  } finally {
    saving.value = false;
  }
}
async function removeCompany(id: string) {
  await deleteCompanyQualification(id);
  message.success("公司资质已删除");
  await loadCompanies();
  emit("dataChanged");
}
function goToEmployeeDetail(id: string) {
  window.open(`/hr/employees/${id}`, "_blank");
}
function openEmployee(record?: QualificationEmployee) {
  employeeEditingId.value = record?.id || "";
  Object.assign(
    employeeForm,
    emptyEmployee(),
    record ? employeePayload(record) : {},
  );
  employeeOpen.value = true;
}
async function saveEmployee() {
  await employeeFormRef.value?.validate();
  saving.value = true;
  try {
    if (employeeEditingId.value)
      await updateQualificationEmployee(employeeEditingId.value, employeeForm);
    else {
      const created = await createQualificationEmployee(employeeForm);
      try {
        const { initLeaveBalances } = await import("@/api/hr");
        initLeaveBalances(created.id);
      } catch {}
    }
    employeeOpen.value = false;
    message.success("人员档案已保存");
    await refreshReferences();
    await loadEmployees();
    emit("dataChanged");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "保存失败");
  } finally {
    saving.value = false;
  }
}
async function removeEmployee(id: string) {
  await deleteQualificationEmployee(id);
  message.success("人员档案已删除");
  await refreshReferences();
  await loadEmployees();
  emit("dataChanged");
}
async function showEmployee(id: string) {
  employeeDetail.value = await getQualificationEmployee(id);
  employeeDetailTab.value = "contracts";
  employeeDetailOpen.value = true;
}
function employeeRow(record: QualificationEmployee) {
  return { onDblclick: () => showEmployee(record.id) };
}
function goToDetail() {
  if (!employeeDetail.value) return;
  window.open(`/hr/employees/${employeeDetail.value.employee.id}`, "_blank");
}
function openEmployeeContract(record?: EmployeeContract) {
  employeeContractEditingId.value = record?.id || "";
  Object.assign(
    employeeContractForm,
    record
      ? { ...record, attachments: [...record.attachments] }
      : emptyEmployeeContract(),
  );
  employeeContractOpen.value = true;
}
async function saveEmployeeContract() {
  await employeeContractFormRef.value?.validate();
  if (!employeeDetail.value) return;
  saving.value = true;
  try {
    if (employeeContractEditingId.value)
      await updateEmployeeContract(
        employeeContractEditingId.value,
        employeeContractForm,
      );
    else
      await createEmployeeContract(
        employeeDetail.value.employee.id,
        employeeContractForm,
      );
    employeeContractOpen.value = false;
    message.success("员工合同已保存");
    await reloadEmployeeDetail();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "合同保存失败");
  } finally {
    saving.value = false;
  }
}
async function removeEmployeeContract(id: string) {
  await deleteEmployeeContract(id);
  message.success("员工合同已删除");
  await reloadEmployeeDetail();
}
async function openEmployeeAccount() {
  if (!employeeDetail.value) return;
  await loadAccountReferences();
  const account = employeeDetail.value.employee.account;
  accountEditingId.value = account?.id || "";
  if (account) {
    const user = await getUserApi(account.id);
    Object.assign(employeeAccountForm, {
      username: user.username,
      password: "",
      displayName: user.displayName,
      phone: user.phone || "",
      email: user.email || "",
      enabled: user.enabled,
      orgId: employeeDetail.value.employee.organizationId,
      roleIds: user.roles.map((role) => role.id),
    });
  } else {
    const employee = employeeDetail.value.employee;
    Object.assign(employeeAccountForm, {
      username: (employee.workNo || "").toLowerCase(),
      password: "",
      displayName: employee.name,
      phone: employee.phone || "",
      email: "",
      enabled: true,
      orgId: employee.organizationId,
      roleIds: [],
    });
  }
  employeeAccountOpen.value = true;
}
async function saveEmployeeAccount() {
  await employeeAccountFormRef.value?.validate();
  if (!employeeDetail.value) return;
  saving.value = true;
  try {
    let user: UserResponse;
    if (accountEditingId.value) {
      user = await updateUserApi(accountEditingId.value, {
        orgId: employeeAccountForm.orgId,
        displayName: employeeAccountForm.displayName,
        phone: employeeAccountForm.phone,
        email: employeeAccountForm.email,
        enabled: employeeAccountForm.enabled,
        roleIds: employeeAccountForm.roleIds,
      });
    } else {
      user = await createUserApi({
        orgId: employeeAccountForm.orgId,
        username: employeeAccountForm.username,
        displayName: employeeAccountForm.displayName,
        password: employeeAccountForm.password,
        phone: employeeAccountForm.phone,
        email: employeeAccountForm.email,
        roleIds: employeeAccountForm.roleIds,
      });
      await updateQualificationEmployee(
        employeeDetail.value.employee.id,
        employeePayload(employeeDetail.value.employee, user.id),
      );
    }
    employeeAccountOpen.value = false;
    message.success(
      accountEditingId.value ? "登录账号已更新" : "登录账号已开通",
    );
    await Promise.all([
      reloadEmployeeDetail(),
      loadEmployees(),
      loadAccountReferences(),
    ]);
  } catch (error) {
    message.error(error instanceof Error ? error.message : "账号保存失败");
  } finally {
    saving.value = false;
  }
}
async function unlinkEmployeeAccount() {
  if (!employeeDetail.value) return;
  await updateQualificationEmployee(
    employeeDetail.value.employee.id,
    employeePayload(employeeDetail.value.employee, null),
  );
  message.success("已解除账号关联");
  await Promise.all([
    reloadEmployeeDetail(),
    loadEmployees(),
    loadAccountReferences(),
  ]);
}
function openPasswordReset() {
  newPassword.value = "";
  passwordResetOpen.value = true;
}
async function resetEmployeePassword() {
  if (!employeeDetail.value?.employee.account) return;
  if (newPassword.value.length < 6) {
    message.error("密码至少6位");
    return;
  }
  saving.value = true;
  try {
    await resetPasswordApi(
      employeeDetail.value.employee.account.id,
      newPassword.value,
    );
    passwordResetOpen.value = false;
    message.success("登录密码已重置");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "密码重置失败");
  } finally {
    saving.value = false;
  }
}
async function reloadEmployeeDetail() {
  if (employeeDetail.value)
    employeeDetail.value = await getQualificationEmployee(
      employeeDetail.value.employee.id,
    );
}
function openCertificate(record?: PersonnelCertificate) {
  certificateEditingId.value = record?.id || "";
  Object.assign(
    certificateForm,
    record
      ? { ...record, attachments: [...record.attachments] }
      : emptyCertificate(),
  );
  certificateOpen.value = true;
}
async function saveCertificate() {
  await certificateFormRef.value?.validate();
  saving.value = true;
  try {
    if (certificateEditingId.value)
      await updatePersonnelCertificate(
        certificateEditingId.value,
        certificateForm,
      );
    else await createPersonnelCertificate(certificateForm);
    certificateOpen.value = false;
    message.success("人员证书已保存");
    await refreshReferences();
    await loadCertificates();
    emit("dataChanged");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "保存失败");
  } finally {
    saving.value = false;
  }
}
async function removeCertificate(id: string) {
  await deletePersonnelCertificate(id);
  message.success("人员证书已删除");
  await loadCertificates();
  emit("dataChanged");
}
function openPerformance(record?: QualificationPerformance) {
  performanceEditingId.value = record?.id || "";
  Object.assign(
    performanceForm,
    record
      ? { ...record, attachments: [...record.attachments] }
      : emptyPerformance(),
  );
  performanceOpen.value = true;
}
async function savePerformance() {
  await performanceFormRef.value?.validate();
  saving.value = true;
  try {
    if (performanceEditingId.value)
      await updateQualificationPerformance(
        performanceEditingId.value,
        performanceForm,
      );
    else await createQualificationPerformance(performanceForm);
    performanceOpen.value = false;
    message.success("项目业绩已保存");
    await refreshReferences();
    await loadPerformances();
  } catch (error) {
    message.error(error instanceof Error ? error.message : "保存失败");
  } finally {
    saving.value = false;
  }
}
async function removePerformance(id: string) {
  await deleteQualificationPerformance(id);
  message.success("项目业绩已删除");
  await loadPerformances();
}
async function showAttachments(items: Attachment[]) {
  try {
    previewAttachments.value = await Promise.all(
      items.map(async (item) => ({
        ...item,
        dataUrl: await getQualificationAttachmentObjectUrl(item.dataUrl),
      })),
    );
    attachmentOpen.value = true;
  } catch (error) {
    message.error(error instanceof Error ? error.message : "附件加载失败");
  }
}
function uploadCompanyFile(file: File) {
  uploadAttachment(file, companyForm.attachments);
  return false;
}
function uploadEmployeeContractFile(file: File) {
  uploadAttachment(file, employeeContractForm.attachments);
  return false;
}
function uploadCertificateFile(file: File) {
  uploadAttachment(file, certificateForm.attachments);
  return false;
}
function uploadPerformanceFile(file: File) {
  uploadAttachment(file, performanceForm.attachments);
  return false;
}
async function uploadAttachment(file: File, target: Attachment[]) {
  saving.value = true;
  try {
    target.push(
      await uploadQualificationAttachment(
        file,
        auth.user?.displayName || "当前用户",
      ),
    );
    message.success("附件已上传");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "附件上传失败");
  } finally {
    saving.value = false;
  }
}
function applyTenderPreset() {
  tenderFilters.specialties = [
    "高处安装、维护、拆除作业",
    "高压电工作业",
    "低压电工作业",
  ].filter((value) => references.specialties.includes(value));
  loadTender();
}
async function refreshReferences() {
  Object.assign(references, await getQualificationReferences());
}
function cleanParams(value: Record<string, unknown>) {
  return Object.fromEntries(
    Object.entries(value).filter(
      ([, item]) => item !== undefined && item !== "",
    ),
  );
}
function employeeInitial(name: string) {
  return name.trim().slice(0, 1) || "员";
}
function employeePayload(
  record: QualificationEmployee,
  systemUserId: string | null | undefined = record.systemUserId,
): EmployeePayload {
  return {
    name: record.name,
    workNo: record.workNo,
    organizationId: record.organizationId,
    department: record.organizationName || record.department,
    position: record.position,
    idCard: record.idCard,
    phone: record.phone,
    entryDate: record.entryDate,
    employmentStatus: record.employmentStatus,
    contractStart: record.contractStart,
    contractEnd: record.contractEnd,
    socialSecurityUnit: record.socialSecurityUnit,
    socialSecurityStart: record.socialSecurityStart,
    socialSecurityEnd: record.socialSecurityEnd,
    remark: record.remark,
    systemUserId,
  };
}
function emptyCompany(): CompanyQualificationPayload {
  return {
    subjectCompany: "",
    name: "",
    category: "",
    level: "",
    certificateNo: "",
    issuer: "",
    issueDate: undefined,
    validFrom: undefined,
    validTo: undefined,
    annualReviewDate: undefined,
    renewalDate: undefined,
    scope: "",
    projectTypes: [],
    holderBranch: "",
    storageLocation: "",
    availableForTender: true,
    manualStatus: "NORMAL",
    locked: false,
    attachments: [],
    remark: "",
  };
}
function emptyEmployee(): EmployeePayload {
  return {
    name: "",
    workNo: "",
    organizationId: undefined,
    department: "",
    position: "",
    idCard: "",
    phone: "",
    entryDate: undefined,
    employmentStatus: "ACTIVE",
    contractStart: undefined,
    contractEnd: undefined,
    socialSecurityUnit: "",
    socialSecurityStart: undefined,
    socialSecurityEnd: undefined,
    remark: "",
    systemUserId: undefined,
  };
}
function emptyEmployeeContract(): EmployeeContractPayload {
  return {
    contractNo: "",
    contractType: "FIXED_TERM",
    signDate: undefined,
    startDate: "",
    endDate: undefined,
    probationEndDate: undefined,
    status: "ACTIVE",
    attachments: [],
    remark: "",
  };
}
function emptyCertificate(): CertificatePayload {
  return {
    employeeId: "",
    name: "",
    type: "",
    certificateNo: "",
    specialty: "",
    companyRegistered: true,
    issueDate: undefined,
    validTo: undefined,
    reviewDate: undefined,
    availableForTender: true,
    manualStatus: "NORMAL",
    locked: false,
    attachments: [],
    remark: "",
  };
}
function emptyPerformance(): PerformancePayload {
  return {
    subjectCompany: "",
    name: "",
    clientName: "",
    contractNo: "",
    contractDate: undefined,
    contractAmount: "",
    projectType: "",
    attachments: [],
    remark: "",
  };
}
function statusLabel(status: QualificationStatus) {
  return (
    (
      {
        VALID: "有效",
        EXPIRING: "临期",
        EXPIRED: "已过期",
        VOIDED: "已作废",
        LOCKED: "已锁定",
        UNVERIFIED: "待核验",
      } as Record<string, string>
    )[status] || status
  );
}
function statusColor(status: QualificationStatus) {
  return (
    (
      {
        VALID: "green",
        EXPIRING: "orange",
        EXPIRED: "red",
        VOIDED: "default",
        LOCKED: "purple",
        UNVERIFIED: "blue",
      } as Record<string, string>
    )[status] || "default"
  );
}
function isContractExpiring(endDate?: string): boolean {
  if (!endDate) return false;
  const end = new Date(endDate);
  const now = new Date();
  const diff = (end.getTime() - now.getTime()) / (1000 * 60 * 60 * 24);
  return diff <= 90;
}
function contractTagColor(endDate?: string): string {
  if (!endDate) return "default";
  const end = new Date(endDate);
  const now = new Date();
  const diff = (end.getTime() - now.getTime()) / (1000 * 60 * 60 * 24);
  if (diff <= 0) return "red";
  if (diff <= 30) return "red";
  if (diff <= 90) return "orange";
  return "green";
}
function contractTagLabel(endDate?: string): string {
  if (!endDate) return "";
  const end = new Date(endDate);
  const now = new Date();
  const diff = (end.getTime() - now.getTime()) / (1000 * 60 * 60 * 24);
  if (diff <= 0) return "已到期";
  if (diff <= 30) return "即将到期";
  if (diff <= 90) return "即将到期";
  return "";
}
function employmentLabel(status: string) {
  return (
    (
      { ACTIVE: "在职", LEFT: "离职", DISABLED: "停用" } as Record<
        string,
        string
      >
    )[status] || status
  );
}
function contractTypeLabel(value: string) {
  return (
    (
      {
        FIXED_TERM: "固定期限",
        OPEN_ENDED: "无固定期限",
        PART_TIME: "非全日制",
        INTERNSHIP: "实习协议",
        OTHER: "其他",
      } as Record<string, string>
    )[value] || value
  );
}
function contractStatusLabel(value: string) {
  return (
    (
      {
        ACTIVE: "履行中",
        DRAFT: "待生效",
        TERMINATED: "已终止",
        EXPIRED: "已到期",
      } as Record<string, string>
    )[value] || value
  );
}
function contractStatusColor(value: string) {
  return (
    (
      {
        ACTIVE: "green",
        DRAFT: "blue",
        TERMINATED: "default",
        EXPIRED: "red",
      } as Record<string, string>
    )[value] || "default"
  );
}
function warningColor(level: string) {
  return level === "DANGER" ? "red" : level === "WARNING" ? "orange" : "blue";
}
function warningStatusLabel(item: QualificationWarning) {
  return item.status === "OVERDUE"
    ? `逾期 ${Math.abs(item.daysLeft)} 天`
    : `剩余 ${item.daysLeft} 天`;
}
function warningRowKey(item: QualificationWarning) {
  return `${item.sourceType}-${item.sourceId}-${item.warningType}`;
}
</script>

<style scoped>
.qualification-page :deep(.ant-statistic-content) {
  font-size: 24px;
}
.employee-directory {
  container-name: employee-directory;
  container-type: inline-size;
  overflow: hidden;
  border: 1px solid #dfe5e8;
  border-radius: 8px;
  background: #fff;
}
.employee-directory-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  min-height: 86px;
  padding: 18px 20px;
  border-bottom: 1px solid #e8ecef;
}
.employee-title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}
.employee-title-row h2 {
  margin: 0;
  color: #17212b;
  font-size: 18px;
  line-height: 1.4;
}
.employee-count {
  color: #64748b;
  font-size: 12px;
}
.employee-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  margin-top: 7px;
  color: #65717e;
  font-size: 12px;
}
.employee-summary strong {
  color: #263442;
  font-size: 13px;
}
.employee-create-button {
  flex: none;
}
.employee-filter-bar {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) minmax(200px, 260px) 180px auto;
  gap: 10px;
  padding: 14px 20px;
  border-bottom: 1px solid #e8ecef;
  background: #f7f9fa;
}
.employee-filter-search :deep(.ant-input-prefix) {
  color: #83909d;
}
.employee-filter-button {
  min-width: 76px;
}
.employee-desktop-table {
  padding: 0 20px 18px;
}
.employee-desktop-table :deep(.ant-table-thead > tr > th) {
  height: 44px;
  background: #fff;
  color: #596675;
  font-size: 12px;
  font-weight: 600;
}
.employee-desktop-table :deep(.ant-table-tbody > tr > td) {
  padding-top: 13px;
  padding-bottom: 13px;
}
.employee-desktop-table :deep(.ant-table-tbody > tr:hover > td) {
  background: #f5faf8;
}
.employee-table-person {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.employee-avatar {
  display: grid;
  flex: 0 0 36px;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 50%;
  background: #e1f0ec;
  color: #146354;
  font-size: 14px;
  font-weight: 700;
}
.employee-cell-primary {
  display: block;
  overflow: hidden;
  color: #283541;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.employee-cert-total {
  color: #84909c;
}
.employee-mobile-list {
  display: none;
}
.employee-mobile-items {
  display: grid;
  gap: 10px;
}
.employee-mobile-item {
  min-width: 0;
  padding: 14px;
  border: 1px solid #e2e7ea;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  outline: none;
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease;
}
.employee-mobile-item:hover,
.employee-mobile-item:focus-visible {
  border-color: #8cbcaf;
  box-shadow: 0 2px 8px rgba(25, 72, 61, 0.08);
}
.employee-mobile-heading {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
}
.employee-mobile-identity {
  min-width: 0;
}
.employee-mobile-name {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
}
.employee-mobile-name strong {
  overflow: hidden;
  color: #1d2935;
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.employee-mobile-name :deep(.ant-tag) {
  margin-inline-end: 0;
}
.employee-mobile-identity > span {
  display: block;
  overflow: hidden;
  margin-top: 3px;
  color: #6a7784;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.employee-mobile-actions {
  display: flex;
  align-items: center;
  gap: 1px;
}
.employee-detail-arrow {
  margin-left: 2px;
  color: #9aa5af;
  font-size: 12px;
}
.employee-mobile-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 13px;
  padding-top: 12px;
  border-top: 1px solid #edf0f2;
}
.employee-mobile-meta div {
  min-width: 0;
}
.employee-mobile-meta span,
.employee-mobile-meta strong {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.employee-mobile-meta span {
  margin-bottom: 4px;
  color: #88939e;
  font-size: 11px;
}
.employee-mobile-meta strong {
  color: #35424e;
  font-size: 12px;
  font-weight: 600;
}
.employee-account-disabled {
  color: #b42318 !important;
}
.qualification-attachments {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 14px;
}
.qualification-attachments figure {
  margin: 0;
  border: 1px solid #e6eaee;
  border-radius: 6px;
  overflow: hidden;
  background: #f8fafb;
}
.qualification-attachments img {
  display: block;
  width: 100%;
  max-height: 620px;
  object-fit: contain;
}
.qualification-attachments figure > a {
  display: grid;
  min-height: 180px;
  place-items: center;
  font-weight: 600;
}
.qualification-attachments figcaption {
  padding: 10px 12px;
  color: #475569;
  font-size: 13px;
}
@container employee-directory (max-width: 860px) {
  .employee-desktop-table {
    display: none;
  }
  .employee-mobile-list {
    display: block;
    padding: 12px;
  }
  .employee-mobile-list :deep(.ant-spin-nested-loading),
  .employee-mobile-list :deep(.ant-spin-container) {
    min-height: 56px;
  }
}
@container employee-directory (max-width: 620px) {
  .employee-directory-header {
    min-height: 78px;
    padding: 15px 14px;
  }
  .employee-summary {
    gap: 12px;
  }
  .employee-filter-bar {
    grid-template-columns: minmax(0, 1fr) 82px;
    padding: 12px 14px;
  }
  .employee-filter-search,
  .employee-organization-filter {
    grid-column: 1 / -1;
  }
  .employee-status-filter {
    min-width: 0;
  }
  .employee-filter-button {
    min-width: 0;
  }
}
@container employee-directory (max-width: 420px) {
  .employee-directory-header {
    align-items: stretch;
    flex-direction: column;
    gap: 12px;
  }
  .employee-create-button {
    width: 100%;
  }
  .employee-mobile-actions :deep(.ant-btn) {
    width: 28px;
    padding-inline: 4px;
  }
  .employee-mobile-meta {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 900px) {
  .employee-desktop-table {
    display: none;
  }
  .employee-mobile-list {
    display: block;
    padding: 12px;
  }
  .employee-mobile-list :deep(.ant-spin-nested-loading),
  .employee-mobile-list :deep(.ant-spin-container) {
    min-height: 56px;
  }
  .qualification-attachments {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 620px) {
  .employee-directory-header {
    min-height: 78px;
    padding: 15px 14px;
  }
  .employee-summary {
    gap: 12px;
  }
  .employee-filter-bar {
    grid-template-columns: minmax(0, 1fr) 82px;
    padding: 12px 14px;
  }
  .employee-filter-search,
  .employee-organization-filter {
    grid-column: 1 / -1;
  }
  .employee-status-filter {
    min-width: 0;
  }
  .employee-filter-button {
    min-width: 0;
  }
}
@media (max-width: 420px) {
  .employee-directory-header {
    align-items: stretch;
    flex-direction: column;
    gap: 12px;
  }
  .employee-create-button {
    width: 100%;
  }
  .employee-mobile-actions :deep(.ant-btn) {
    width: 28px;
    padding-inline: 4px;
  }
  .employee-mobile-meta {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
