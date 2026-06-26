<template>
  <div class="page-stack">
    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :md="12" :xl="6" v-for="item in metrics" :key="item.label">
        <a-card>
          <a-statistic :title="item.label" :value="item.value" :suffix="item.suffix" />
          <p class="muted">{{ item.meta }}</p>
        </a-card>
      </a-col>
    </a-row>

    <a-card title="建设顺序">
      <a-steps :current="0" :items="steps" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
const metrics = [
  { label: "客户档案", value: 3, suffix: "家", meta: "CRM 第一批接口已接入" },
  { label: "基础模块", value: 8, suffix: "个", meta: "系统、CRM、项目、工单、仓储、财务、OA、档案" },
  { label: "后端接口", value: 3, suffix: "个", meta: "客户列表、详情、新增" },
  { label: "基础设施", value: 3, suffix: "项", meta: "PostgreSQL、Redis、MinIO" },
];

const steps = [
  { title: "底层架构", description: "应用壳、数据库、接口规范" },
  { title: "CRM", description: "客户池、商机、报价、合同" },
  { title: "项目工单", description: "项目成本和现场执行" },
  { title: "库存财务", description: "领料、采购、应收应付" },
];
</script>

