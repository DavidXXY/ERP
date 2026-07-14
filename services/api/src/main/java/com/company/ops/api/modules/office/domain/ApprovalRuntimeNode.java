package com.company.ops.api.modules.office.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "oa_approval_runtime_nodes")
public class ApprovalRuntimeNode extends BaseEntity {
  @Column(name = "approval_id", nullable = false) private UUID approvalId;
  @Column(name = "step_no", nullable = false) private int stepNo;
  @Column(name = "node_status", nullable = false, length = 32) private String nodeStatus = "PENDING";
  @Column(name = "approval_mode", length = 20) private String approvalMode;
  @Column(name = "step_policy", length = 32) private String stepPolicy = "ANY_APPROVE";
  @Column(name = "assignee_type", length = 20) private String assigneeType;
  @Column(name = "assignee_id") private UUID assigneeId;
  @Column(name = "assignee_name", length = 120) private String assigneeName;
  @Column(name = "source_type", length = 40) private String sourceType;
  @Column(name = "source_value", length = 120) private String sourceValue;
  @Column(name = "condition_text", length = 500) private String conditionText;
  @Column(name = "sla_hours") private Integer slaHours;
  @Column(name = "due_at") private OffsetDateTime dueAt;
  @Column(name = "reminded_at") private OffsetDateTime remindedAt;
  @Column(name = "escalation_role_id") private UUID escalationRoleId;
  @Column(name = "escalated_at") private OffsetDateTime escalatedAt;
  @Column(name = "completed_at") private OffsetDateTime completedAt;
  @Column(name = "approver_id") private UUID approverId;
  @Column(name = "approver_name", length = 80) private String approverName;
  @Column(name = "approval_comment", length = 500) private String approvalComment;

  public UUID getApprovalId() { return approvalId; } public void setApprovalId(UUID v) { approvalId = v; }
  public int getStepNo() { return stepNo; } public void setStepNo(int v) { stepNo = v; }
  public String getNodeStatus() { return nodeStatus; } public void setNodeStatus(String v) { nodeStatus = v; }
  public String getApprovalMode() { return approvalMode; } public void setApprovalMode(String v) { approvalMode = v; }
  public String getStepPolicy() { return stepPolicy; } public void setStepPolicy(String v) { stepPolicy = v; }
  public String getAssigneeType() { return assigneeType; } public void setAssigneeType(String v) { assigneeType = v; }
  public UUID getAssigneeId() { return assigneeId; } public void setAssigneeId(UUID v) { assigneeId = v; }
  public String getAssigneeName() { return assigneeName; } public void setAssigneeName(String v) { assigneeName = v; }
  public String getSourceType() { return sourceType; } public void setSourceType(String v) { sourceType = v; }
  public String getSourceValue() { return sourceValue; } public void setSourceValue(String v) { sourceValue = v; }
  public String getConditionText() { return conditionText; } public void setConditionText(String v) { conditionText = v; }
  public Integer getSlaHours() { return slaHours; } public void setSlaHours(Integer v) { slaHours = v; }
  public OffsetDateTime getDueAt() { return dueAt; } public void setDueAt(OffsetDateTime v) { dueAt = v; }
  public OffsetDateTime getRemindedAt() { return remindedAt; } public void setRemindedAt(OffsetDateTime v) { remindedAt = v; }
  public UUID getEscalationRoleId() { return escalationRoleId; } public void setEscalationRoleId(UUID v) { escalationRoleId = v; }
  public OffsetDateTime getEscalatedAt() { return escalatedAt; } public void setEscalatedAt(OffsetDateTime v) { escalatedAt = v; }
  public OffsetDateTime getCompletedAt() { return completedAt; } public void setCompletedAt(OffsetDateTime v) { completedAt = v; }
  public UUID getApproverId() { return approverId; } public void setApproverId(UUID v) { approverId = v; }
  public String getApproverName() { return approverName; } public void setApproverName(String v) { approverName = v; }
  public String getApprovalComment() { return approvalComment; } public void setApprovalComment(String v) { approvalComment = v; }
}
