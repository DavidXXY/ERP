package com.company.ops.api.modules.office.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity @Table(name = "oa_approval_actions")
public class ApprovalAction extends BaseEntity {
  @Column(name = "approval_id", nullable = false) private UUID approvalId;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private ApprovalStatus decision;
  @Column(name = "operator_name", nullable = false, length = 80) private String operatorName;
  @Column(nullable = false, length = 500) private String comment;
  public UUID getApprovalId() { return approvalId; } public void setApprovalId(UUID v) { approvalId = v; }
  public ApprovalStatus getDecision() { return decision; } public void setDecision(ApprovalStatus v) { decision = v; }
  public String getOperatorName() { return operatorName; } public void setOperatorName(String v) { operatorName = v; }
  public String getComment() { return comment; } public void setComment(String v) { comment = v; }
}
