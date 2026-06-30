package com.company.ops.api.modules.procurement.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "procurement_request_approval_records")
public class PurchaseRequestApprovalRecord extends BaseEntity {

  @Column(name = "request_id", nullable = false)
  private UUID requestId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private ApprovalStatus decision;

  @Column(nullable = false, length = 500)
  private String comment;

  @Column(name = "approver_name", nullable = false, length = 80)
  private String approverName;

  @Column(name = "decided_at", nullable = false)
  private OffsetDateTime decidedAt;

  public UUID getRequestId() {
    return requestId;
  }

  public void setRequestId(UUID requestId) {
    this.requestId = requestId;
  }

  public ApprovalStatus getDecision() {
    return decision;
  }

  public void setDecision(ApprovalStatus decision) {
    this.decision = decision;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public String getApproverName() {
    return approverName;
  }

  public void setApproverName(String approverName) {
    this.approverName = approverName;
  }

  public OffsetDateTime getDecidedAt() {
    return decidedAt;
  }

  public void setDecidedAt(OffsetDateTime decidedAt) {
    this.decidedAt = decidedAt;
  }
}
