<template>
  <div class="business-detail-page">
    <a-card :bordered="false" class="detail-hero">
      <a-spin :spinning="loading">
        <div class="hero-main">
          <a-button class="back-button" type="text" @click="goBack"
            >← 返回</a-button
          >
          <div class="hero-copy">
            <div class="hero-title-row">
              <h2>{{ title || "详情" }}</h2>
              <a-tag v-if="statusLabel" :color="statusColor">{{
                statusLabel
              }}</a-tag>
              <a-tag v-if="riskLabel" :color="riskColor">{{ riskLabel }}</a-tag>
            </div>
            <p>
              {{ [code, subtitle].filter(Boolean).join(" · ") || "业务详情" }}
            </p>
          </div>
          <a-space wrap class="hero-actions">
            <slot name="actions" />
            <a-button :loading="loading" @click="$emit('refresh')"
              >刷新</a-button
            >
          </a-space>
        </div>

        <a-row v-if="stats.length" :gutter="[12, 12]" class="metric-grid">
          <a-col
            v-for="item in stats"
            :key="item.label"
            :xs="12"
            :sm="8"
            :lg="6"
            :xl="Math.max(4, Math.floor(24 / Math.min(stats.length, 6)))"
          >
            <div
              class="metric-card"
              :class="{ danger: item.danger, warning: item.warning }"
            >
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
              <small v-if="item.hint">{{ item.hint }}</small>
            </div>
          </a-col>
        </a-row>
      </a-spin>
    </a-card>

    <a-card v-if="$slots.relations" :bordered="false" class="detail-relations">
      <div class="section-heading">
        <div>
          <strong>业务链路</strong>
          <span>从来源单据到履约、结算的完整关联</span>
        </div>
      </div>
      <slot name="relations" />
    </a-card>

    <a-spin :spinning="loading">
      <slot />
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from "vue-router";

export type DetailMetric = {
  label: string;
  value: string | number;
  hint?: string;
  danger?: boolean;
  warning?: boolean;
};

const props = withDefaults(
  defineProps<{
    title?: string;
    code?: string;
    subtitle?: string;
    loading?: boolean;
    backTo?: string;
    statusLabel?: string;
    statusColor?: string;
    riskLabel?: string;
    riskColor?: string;
    stats?: DetailMetric[];
  }>(),
  {
    title: "",
    code: "",
    subtitle: "",
    loading: false,
    backTo: "",
    statusLabel: "",
    statusColor: "blue",
    riskLabel: "",
    riskColor: "orange",
    stats: () => [],
  },
);

defineEmits<{ refresh: [] }>();
const router = useRouter();

function goBack() {
  if (props.backTo) router.push(props.backTo);
  else router.back();
}
</script>

<style scoped>
.business-detail-page {
  display: grid;
  gap: 16px;
}
.detail-hero,
.detail-relations {
  border-radius: 14px;
  box-shadow: 0 4px 18px rgb(15 23 42 / 5%);
}
.hero-main {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}
.back-button {
  margin-top: 2px;
}
.hero-copy {
  min-width: 0;
  flex: 1;
}
.hero-title-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.hero-title-row h2 {
  margin: 0;
  color: #172033;
  font-size: 22px;
}
.hero-copy p {
  margin: 6px 0 0;
  color: #738095;
}
.hero-actions {
  justify-content: flex-end;
}
.metric-grid {
  margin-top: 18px;
}
.metric-card {
  min-height: 88px;
  padding: 14px 16px;
  border: 1px solid #e8edf4;
  border-radius: 10px;
  background: linear-gradient(145deg, #fff, #f8fbff);
}
.metric-card span,
.metric-card small {
  display: block;
  color: #7b8799;
}
.metric-card strong {
  display: block;
  margin: 5px 0 2px;
  color: #172033;
  font-size: 20px;
}
.metric-card.danger {
  border-color: #ffccc7;
  background: #fff7f6;
}
.metric-card.danger strong {
  color: #cf1322;
}
.metric-card.warning {
  border-color: #ffe58f;
  background: #fffdf1;
}
.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.section-heading strong,
.section-heading span {
  display: block;
}
.section-heading strong {
  font-size: 16px;
}
.section-heading span {
  margin-top: 3px;
  color: #8a95a5;
  font-size: 12px;
}
@media (max-width: 768px) {
  .hero-main {
    flex-wrap: wrap;
  }
  .hero-copy {
    flex-basis: calc(100% - 60px);
  }
  .hero-actions {
    width: 100%;
  }
}
</style>
