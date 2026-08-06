<template>
  <div class="page-shell">
    <header class="page-heading">
      <div><p class="eyebrow">协作概览</p><h1>上午好，{{ store.session?.account.contactName }}</h1><p>这里汇总当前准入进度与待处理询价。</p></div>
      <a-button :loading="loading" @click="load"><ReloadOutlined /> 刷新</a-button>
    </header>

    <section class="metrics-grid" aria-label="关键状态">
      <article class="metric"><span class="metric-icon green"><SafetyCertificateOutlined /></span><div><small>供应商准入</small><strong>{{ admissionText }}</strong></div></article>
      <article class="metric"><span class="metric-icon amber"><ClockCircleOutlined /></span><div><small>待处理询价</small><strong>{{ pendingCount }}</strong></div></article>
      <article class="metric"><span class="metric-icon blue"><SendOutlined /></span><div><small>已提交报价</small><strong>{{ submittedCount }}</strong></div></article>
      <article class="metric"><span class="metric-icon gray"><FileDoneOutlined /></span><div><small>资质文件</small><strong>{{ documents.length }}</strong></div></article>
    </section>

    <section class="dashboard-grid">
      <div class="section-block">
        <div class="section-title"><div><h2>待处理询价</h2><p>优先处理临近截止且尚未提交的询价。</p></div><RouterLink to="/inquiries">查看全部</RouterLink></div>
        <a-skeleton v-if="loading" active />
        <a-empty v-else-if="pending.length === 0" description="暂无待处理询价" />
        <div v-else class="inquiry-list compact">
          <button v-for="item in pending.slice(0, 4)" :key="item.id" class="inquiry-row" @click="router.push('/inquiries')">
            <span><b>{{ item.title }}</b><small>{{ item.code }} · {{ item.lines.length }} 项物料</small></span>
            <span class="deadline" :class="{ urgent: daysLeft(item.deadline) <= 2 }"><ClockCircleOutlined /> {{ deadlineText(item.deadline) }}</span>
            <RightOutlined />
          </button>
        </div>
      </div>
      <aside class="section-block readiness">
        <div class="section-title"><div><h2>准入准备度</h2><p>完成资料有助于采购方快速审核。</p></div></div>
        <a-progress type="circle" :percent="readiness" :size="128" :stroke-color="readiness === 100 ? '#27845f' : '#b26a16'" />
        <div class="readiness-items">
          <span><CheckCircleOutlined :class="{ done: Boolean(store.session?.supplier.registeredAddress) }" /> 企业基础资料</span>
          <span><CheckCircleOutlined :class="{ done: documents.length > 0 }" /> 至少一份资质文件</span>
          <span><CheckCircleOutlined :class="{ done: store.session?.account.status === 'ACTIVE' }" /> 门户账号审核</span>
          <span><CheckCircleOutlined :class="{ done: store.session?.supplier.admissionStatus === 'APPROVED' }" /> 供应商准入审核</span>
        </div>
      </aside>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { message } from "ant-design-vue";
import { CheckCircleOutlined, ClockCircleOutlined, FileDoneOutlined, ReloadOutlined, RightOutlined, SafetyCertificateOutlined, SendOutlined } from "@ant-design/icons-vue";
import * as api from "../api";
import { usePortalStore } from "../store";

const store = usePortalStore(); const router = useRouter(); const loading = ref(false);
const inquiries = ref<api.PortalInquiry[]>([]); const documents = ref<api.PortalDocument[]>([]);
const pending = computed(() => inquiries.value.filter((i) => i.status === "OPEN" && i.quote?.status !== "SUBMITTED"));
const pendingCount = computed(() => pending.value.length);
const submittedCount = computed(() => inquiries.value.filter((i) => i.quote?.status === "SUBMITTED").length);
const admissionText = computed(() => ({ APPROVED: "已通过", REJECTED: "已退回", PENDING: "审核中" })[store.session?.supplier.admissionStatus || "PENDING"]);
const readiness = computed(() => [Boolean(store.session?.supplier.registeredAddress), documents.value.length > 0, store.session?.account.status === "ACTIVE", store.session?.supplier.admissionStatus === "APPROVED"].filter(Boolean).length * 25);
onMounted(load);
async function load() { loading.value = true; try { [inquiries.value, documents.value] = await Promise.all([api.listInquiries(), api.listDocuments()]); } catch (e) { message.error(e instanceof Error ? e.message : "加载失败"); } finally { loading.value = false; } }
function daysLeft(deadline?: string) { return deadline ? Math.ceil((new Date(`${deadline}T23:59:59`).getTime() - Date.now()) / 86400000) : 999; }
function deadlineText(deadline?: string) { const days = daysLeft(deadline); return !deadline ? "未设截止" : days < 0 ? "已截止" : days === 0 ? "今天截止" : `${days} 天后截止`; }
</script>
