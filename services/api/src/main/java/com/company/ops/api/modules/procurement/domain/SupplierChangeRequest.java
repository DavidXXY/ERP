package com.company.ops.api.modules.procurement.domain;

import com.company.ops.api.common.domain.BaseEntity;
import com.company.ops.api.common.security.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "procurement_supplier_change_requests")
public class SupplierChangeRequest extends BaseEntity {
  @Column(name = "supplier_id", nullable = false)
  private UUID supplierId;

  @Column(name = "change_type", nullable = false, length = 40)
  private String changeType;

  @Column(name = "proposed_admission_status", length = 40)
  private String proposedAdmissionStatus;

  @Column(name = "proposed_risk_status", length = 32)
  private String proposedRiskStatus;

  @Column(name = "proposed_bank_name", length = 120)
  private String proposedBankName;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "proposed_bank_account", length = 512)
  private String proposedBankAccount;

  @Column(name = "proposed_settlement_terms", length = 160)
  private String proposedSettlementTerms;

  @Column(nullable = false, length = 1000)
  private String reason;

  @Column(nullable = false, length = 32)
  private String status = "PENDING";

  @Column(name = "requested_by_name", nullable = false, length = 80)
  private String requestedByName;

  @Column(name = "reviewed_by_name", length = 80)
  private String reviewedByName;

  @Column(name = "review_comment", length = 500)
  private String reviewComment;

  @Column(name = "reviewed_at")
  private OffsetDateTime reviewedAt;

  public UUID getSupplierId() { return supplierId; }
  public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }
  public String getChangeType() { return changeType; }
  public void setChangeType(String changeType) { this.changeType = changeType; }
  public String getProposedAdmissionStatus() { return proposedAdmissionStatus; }
  public void setProposedAdmissionStatus(String value) { proposedAdmissionStatus = value; }
  public String getProposedRiskStatus() { return proposedRiskStatus; }
  public void setProposedRiskStatus(String value) { proposedRiskStatus = value; }
  public String getProposedBankName() { return proposedBankName; }
  public void setProposedBankName(String value) { proposedBankName = value; }
  public String getProposedBankAccount() { return proposedBankAccount; }
  public void setProposedBankAccount(String value) { proposedBankAccount = value; }
  public String getProposedSettlementTerms() { return proposedSettlementTerms; }
  public void setProposedSettlementTerms(String value) { proposedSettlementTerms = value; }
  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getRequestedByName() { return requestedByName; }
  public void setRequestedByName(String value) { requestedByName = value; }
  public String getReviewedByName() { return reviewedByName; }
  public void setReviewedByName(String value) { reviewedByName = value; }
  public String getReviewComment() { return reviewComment; }
  public void setReviewComment(String value) { reviewComment = value; }
  public OffsetDateTime getReviewedAt() { return reviewedAt; }
  public void setReviewedAt(OffsetDateTime value) { reviewedAt = value; }
}
