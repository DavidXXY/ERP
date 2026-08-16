<template>
  <div class="flow-designer">
    <!-- 开始节点 -->
    <div class="terminal-node is-start">
      <div class="terminal-dot" />
      <span>提交申请</span>
    </div>

    <template v-for="(step, stepIndex) in steps" :key="step.stepNo">
      <div class="connector" aria-hidden="true">
        <span class="connector-line" />
        <span class="connector-arrow">▼</span>
      </div>

      <div
        class="step-card"
        :class="{ 'is-drag-over': dragOver === 'step-' + stepIndex }"
        @dragover.prevent="onStepDragOver(stepIndex)"
        @dragleave="clearDragOver"
        @drop="onStepDrop(stepIndex)"
      >
        <div
          class="step-header"
          draggable="true"
          @dragstart="onStepDragStart(stepIndex)"
          @dragend="clearDrag"
        >
          <span class="drag-handle" title="拖拽调整步骤顺序">⠿</span>
          <a-tag color="blue">第 {{ stepIndex + 1 }} 步</a-tag>
          <span class="step-meta">
            {{ step.rules.length }} 个分支 ·
            {{ enabledCount(step) }} 启用
          </span>
          <span class="step-actions">
            <a-button
              size="small"
              type="text"
              :disabled="stepIndex === 0"
              title="上移"
              @click.stop="emit('reorder-step', stepIndex, stepIndex - 1)"
            >
              ↑
            </a-button>
            <a-button
              size="small"
              type="text"
              :disabled="stepIndex === steps.length - 1"
              title="下移"
              @click.stop="emit('reorder-step', stepIndex, stepIndex + 1)"
            >
              ↓
            </a-button>
            <a-popconfirm
              title="删除该步骤及其下所有分支？"
              @confirm="emit('delete-step', stepIndex)"
            >
              <a-button size="small" type="text" danger @click.stop>删除</a-button>
            </a-popconfirm>
          </span>
        </div>

        <div class="branch-list">
          <div
            v-for="(rule, branchIndex) in step.rules"
            :key="rule.id"
            class="branch-row"
            :class="{
              'is-drag-over': dragOver === 'branch-' + stepIndex + '-' + branchIndex,
              'is-disabled': !rule.enabled,
            }"
            draggable="true"
            @dragstart.stop="onBranchDragStart(stepIndex, branchIndex)"
            @dragend="clearDrag"
            @dragover.prevent="onBranchDragOver(stepIndex, branchIndex)"
            @dragleave="clearDragOver"
            @drop.stop="onBranchDrop(stepIndex, branchIndex)"
          >
            <span class="drag-handle small" title="拖拽移动分支">⠿</span>
            <span class="branch-condition">
              <a-tag :color="conditionColor(rule.conditionType)">
                {{ conditionLabel(rule.conditionType) }}
              </a-tag>
              <span class="condition-text">{{ conditionText(rule) }}</span>
            </span>
            <span class="branch-arrow">→</span>
            <span class="branch-assignee">
              <a-tag :color="assigneeTypeColor(rule.assigneeType)">
                {{ assigneeTypeLabel(rule.assigneeType) }}
              </a-tag>
              <span class="assignee-name">{{ rule.assigneeName || "未指定" }}</span>
              <a-tag v-if="!rule.enabled" color="default">停用</a-tag>
              <a-tag
                v-if="rule.approvalMode === 'SEQUENTIAL'"
                color="blue"
                >依次</a-tag
              >
              <a-tag v-else color="green">同步</a-tag>
              <a-tag v-if="rule.slaHours" color="orange"
                >SLA {{ rule.slaHours }}h</a-tag
              >
            </span>
            <span class="branch-actions">
              <a-button
                size="small"
                type="text"
                :disabled="branchIndex === 0"
                title="上移"
                @click.stop="emit('move-branch', stepIndex, branchIndex, stepIndex, branchIndex - 1)"
              >
                ↑
              </a-button>
              <a-button
                size="small"
                type="text"
                :disabled="branchIndex === step.rules.length - 1"
                title="下移"
                @click.stop="emit('move-branch', stepIndex, branchIndex, stepIndex, branchIndex + 1)"
              >
                ↓
              </a-button>
              <a-button size="small" type="text" @click.stop="emit('edit-branch', rule)">
                编辑
              </a-button>
              <a-popconfirm title="移除该审批分支？" @confirm="emit('delete-branch', rule)">
                <a-button size="small" type="text" danger @click.stop>移除</a-button>
              </a-popconfirm>
            </span>
          </div>
          <div
            v-if="!step.rules.length"
            class="branch-empty"
            @drop.stop="onBranchDrop(stepIndex, 0)"
            @dragover.prevent="onBranchDragOver(stepIndex, 0)"
          >
            拖拽分支到这里，或点击下方按钮新增
          </div>
        </div>

        <a-button block type="dashed" size="small" class="add-branch" @click="emit('add-branch', stepIndex)">
          + 添加审批人 / 分支
        </a-button>
      </div>
    </template>

    <div class="connector" aria-hidden="true">
      <span class="connector-line" />
      <span class="connector-arrow">▼</span>
    </div>

    <a-button block type="dashed" class="add-step" @click="emit('add-step')">
      + 添加审批步骤
    </a-button>

    <div class="connector" aria-hidden="true">
      <span class="connector-line" />
      <span class="connector-arrow">▼</span>
    </div>

    <!-- 结束节点 -->
    <div class="terminal-node is-end">
      <span>流程结束</span>
      <div class="terminal-dot" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import type { ApprovalConfigResponse } from "@/api/system";

type DesignerStep = {
  stepNo: number;
  rules: ApprovalConfigResponse[];
};

defineProps<{ steps: DesignerStep[] }>();

const emit = defineEmits<{
  (e: "reorder-step", from: number, to: number): void;
  (e: "move-branch", fromStep: number, fromIndex: number, toStep: number, toIndex: number): void;
  (e: "add-step"): void;
  (e: "add-branch", stepIndex: number): void;
  (e: "edit-branch", rule: ApprovalConfigResponse): void;
  (e: "delete-branch", rule: ApprovalConfigResponse): void;
  (e: "delete-step", stepIndex: number): void;
}>();

type DragState =
  | { type: "step"; index: number }
  | { type: "branch"; stepIndex: number; index: number };

const drag = ref<DragState | null>(null);
const dragOver = ref<string>("");

function onStepDragStart(index: number) {
  drag.value = { type: "step", index };
}
function onStepDragOver(index: number) {
  if (drag.value?.type === "step") dragOver.value = "step-" + index;
}
function onStepDrop(index: number) {
  const d = drag.value;
  clearDrag();
  if (d?.type === "step" && d.index !== index) {
    emit("reorder-step", d.index, index);
  }
}
function onBranchDragStart(stepIndex: number, index: number) {
  drag.value = { type: "branch", stepIndex, index };
}
function onBranchDragOver(stepIndex: number, index: number) {
  if (drag.value?.type === "branch") dragOver.value = "branch-" + stepIndex + "-" + index;
}
function onBranchDrop(stepIndex: number, index: number) {
  const d = drag.value;
  clearDrag();
  if (d?.type === "branch") {
    emit("move-branch", d.stepIndex, d.index, stepIndex, index);
  }
}
function clearDrag() {
  drag.value = null;
}
function clearDragOver() {
  dragOver.value = "";
}

function enabledCount(step: DesignerStep) {
  return step.rules.filter((r) => r.enabled).length;
}

function conditionLabel(value: ApprovalConfigResponse["conditionType"]) {
  return (
    (
      {
        ANY: "全部",
        AMOUNT: "金额",
        DEPARTMENT: "部门",
        AMOUNT_AND_DEPARTMENT: "金额+部门",
        BUSINESS_TYPE: "业务",
        PROJECT: "项目",
        SUPPLIER_RISK: "供应商",
        CUSTOMER_LEVEL: "客户",
        COMPOSITE: "复合",
      } as Record<string, string>
    )[value] || value
  );
}
function conditionColor(value: ApprovalConfigResponse["conditionType"]) {
  return (
    (
      {
        ANY: "default",
        AMOUNT: "orange",
        DEPARTMENT: "blue",
        AMOUNT_AND_DEPARTMENT: "purple",
      } as Record<string, string>
    )[value] || "default"
  );
}
function assigneeTypeLabel(value: ApprovalConfigResponse["assigneeType"]) {
  return (
    (
      { ROLE: "角色", USER: "人员", DYNAMIC: "动态", AUTO: "自动" } as Record<string, string>
    )[value] || value
  );
}
function assigneeTypeColor(value: ApprovalConfigResponse["assigneeType"]) {
  return (
    (
      {
        ROLE: "purple",
        USER: "blue",
        DYNAMIC: "cyan",
        AUTO: "green",
      } as Record<string, string>
    )[value] || "default"
  );
}
function conditionText(record: ApprovalConfigResponse) {
  const parts: string[] = [];
  if (record.minAmount != null || record.maxAmount != null)
    parts.push(`${record.minAmount ?? 0} - ${record.maxAmount ?? "不限"}`);
  if (record.departmentName) parts.push(record.departmentName);
  if (record.businessType) parts.push(record.businessType);
  if (record.projectCode) parts.push(record.projectCode);
  if (record.supplierRisk) parts.push(record.supplierRisk);
  if (record.customerLevel) parts.push(record.customerLevel);
  if (record.remark) parts.push(record.remark);
  return parts.join(" · ") || "所有单据适用";
}
</script>

<style scoped>
.flow-designer {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  max-width: 860px;
  margin: 0 auto;
  padding: 8px 4px 24px;
}

.terminal-node {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  align-self: center;
  min-width: 180px;
  padding: 10px 22px;
  border-radius: 999px;
  font-weight: 600;
  font-size: 14px;
}
.terminal-node.is-start {
  color: #fff;
  background: linear-gradient(135deg, #1677ff, #4096ff);
  box-shadow: 0 4px 14px rgba(22, 119, 255, 0.28);
}
.terminal-node.is-end {
  color: #52c41a;
  border: 1px solid #b7eb8f;
  background: #f6ffed;
}
.terminal-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
}

.connector {
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 34px;
}
.connector-line {
  flex: 1;
  width: 2px;
  background: #c3d0e0;
}
.connector-arrow {
  margin-top: -6px;
  color: #c3d0e0;
  font-size: 12px;
  line-height: 12px;
}

.step-card {
  border: 1px solid #dbe3ee;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.05);
  overflow: hidden;
  transition: box-shadow 0.15s ease, border-color 0.15s ease;
}
.step-card:hover {
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.08);
}
.step-card.is-drag-over {
  border-color: #1677ff;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.18);
}

.step-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: #f6f9fd;
  border-bottom: 1px solid #eef2f7;
  cursor: grab;
}
.step-header:active {
  cursor: grabbing;
}
.drag-handle {
  color: #94a3b8;
  font-size: 16px;
  line-height: 1;
  user-select: none;
}
.drag-handle.small {
  font-size: 13px;
}
.step-meta {
  flex: 1;
  color: #64748b;
  font-size: 12px;
}
.step-actions {
  display: inline-flex;
  gap: 2px;
}

.branch-list {
  display: flex;
  flex-direction: column;
}
.branch-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-bottom: 1px dashed #eef2f7;
  cursor: grab;
  transition: background 0.12s ease;
}
.branch-row:last-child {
  border-bottom: none;
}
.branch-row:hover {
  background: #f8fafc;
}
.branch-row.is-drag-over {
  background: #e6f4ff;
  box-shadow: inset 0 2px 0 #1677ff;
}
.branch-row.is-disabled {
  opacity: 0.6;
}
.branch-condition {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  flex: 1;
}
.condition-text {
  color: #475569;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.branch-arrow {
  color: #94a3b8;
  font-weight: 700;
}
.branch-assignee {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  flex: 1;
}
.assignee-name {
  color: #0f172a;
  font-weight: 600;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.branch-actions {
  display: inline-flex;
  gap: 0;
  flex-shrink: 0;
}
.branch-empty {
  padding: 14px;
  text-align: center;
  color: #94a3b8;
  font-size: 12px;
  border-bottom: 1px dashed #eef2f7;
}
.add-branch {
  margin: 8px 12px 12px;
  width: calc(100% - 24px);
}
.add-step {
  max-width: 860px;
}
</style>
