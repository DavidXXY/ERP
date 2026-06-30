package com.company.ops.api.modules.crm.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "crm_quote_approval_records")
public class QuoteApprovalRecord extends BaseEntity {

  @Column(name = "quote_id", nullable = false)
  private UUID quoteId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private ApprovalDecision decision;

  @Column(nullable = false, length = 500)
  private String comment;

  @Column(name = "approver_name", nullable = false, length = 80)
  private String approverName;

  @Column(name = "decided_at", nullable = false)
  private OffsetDateTime decidedAt;

  @Column(name = "generated_contract_id")
  private UUID generatedContractId;

  public UUID getQuoteId() {
    return quoteId;
  }

  public void setQuoteId(UUID quoteId) {
    this.quoteId = quoteId;
  }

  public ApprovalDecision getDecision() {
    return decision;
  }

  public void setDecision(ApprovalDecision decision) {
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

  public UUID getGeneratedContractId() {
    return generatedContractId;
  }

  public void setGeneratedContractId(UUID generatedContractId) {
    this.generatedContractId = generatedContractId;
  }
}
