<template>
  <div class="approval-progress-flow">
    <div
      v-for="(step, index) in steps"
      :key="step.key || index"
      class="approval-progress-step"
      :class="`is-${step.state || 'waiting'}`"
    >
      <div class="approval-progress-node">
        <div class="approval-progress-avatar">
          {{ step.avatarText || initials(step.personName || step.title) }}
          <span class="approval-progress-badge">{{
            badgeText(step.state)
          }}</span>
        </div>
        <div v-if="index < steps.length - 1" class="approval-progress-line" />
      </div>
      <strong>{{ step.personName || "-" }}</strong>
      <span class="approval-progress-status">{{ step.title }}</span>
      <p v-if="step.time">{{ formatDateTime(step.time) }}</p>
      <small v-if="step.note">{{ step.note }}</small>
    </div>
  </div>
</template>

<script setup lang="ts">
export type ApprovalProgressState =
  | "done"
  | "pending"
  | "rejected"
  | "waiting"
  | "skipped";

export type ApprovalProgressStep = {
  key?: string;
  personName?: string;
  avatarText?: string;
  title: string;
  time?: string;
  note?: string;
  state?: ApprovalProgressState;
};

defineProps<{ steps: ApprovalProgressStep[] }>();

function initials(value?: string) {
  const text = (value || "-").trim();
  if (!text || text === "-") return "-";
  return text.slice(-2);
}

function badgeText(state?: ApprovalProgressState) {
  if (state === "done") return "✓";
  if (state === "rejected") return "!";
  if (state === "pending") return "";
  return "";
}

function formatDateTime(value: string) {
  if (!value) return "";
  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) return value;
  return value.slice(0, 16).replace("T", " ");
}
</script>

<style scoped>
.approval-progress-flow {
  display: flex;
  align-items: flex-start;
  gap: 0;
  overflow-x: auto;
  padding: 18px 4px 12px;
}

.approval-progress-step {
  position: relative;
  min-width: 150px;
  max-width: 190px;
  text-align: center;
  color: #262626;
}

.approval-progress-node {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 48px;
  margin-bottom: 8px;
}

.approval-progress-avatar {
  position: relative;
  z-index: 1;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #eef3f8;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
  box-shadow: inset 0 0 0 1px #d9e2ec;
}

.approval-progress-badge {
  position: absolute;
  right: -2px;
  bottom: 0;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid #fff;
  background: #d9d9d9;
  color: #fff;
  font-size: 10px;
  line-height: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.approval-progress-line {
  position: absolute;
  left: calc(50% + 30px);
  right: calc(-50% + 30px);
  top: 24px;
  height: 2px;
  background: #d9d9d9;
}

.approval-progress-step.is-done .approval-progress-line,
.approval-progress-step.is-rejected .approval-progress-line {
  background: #ff4d4f;
}

.approval-progress-step.is-done .approval-progress-badge {
  background: #18c037;
}

.approval-progress-step.is-rejected .approval-progress-badge {
  background: #ff4d4f;
}

.approval-progress-step.is-pending .approval-progress-badge {
  background: #d9d9d9;
}

.approval-progress-step strong {
  display: block;
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  overflow-wrap: anywhere;
}

.approval-progress-status {
  display: block;
  margin-top: 6px;
  color: #ff4d4f;
  font-size: 14px;
  line-height: 20px;
}

.approval-progress-step.is-pending .approval-progress-status {
  color: #262626;
}

.approval-progress-step.is-waiting .approval-progress-status,
.approval-progress-step.is-skipped .approval-progress-status {
  color: #8c8c8c;
}

.approval-progress-step p {
  margin: 8px 0 0;
  color: #595959;
  font-size: 13px;
  line-height: 18px;
}

.approval-progress-step small {
  display: block;
  margin-top: 4px;
  color: #8c8c8c;
  font-size: 12px;
  line-height: 18px;
  overflow-wrap: anywhere;
}
</style>
