package com.company.ops.api.modules.finance.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "fin_payment_applications")
public class PaymentApplication extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(name = "payable_id", nullable = false)
  private UUID payableId;

  @Column(name = "supplier_id", nullable = false)
  private UUID supplierId;

  @Column(name = "requested_amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal requestedAmount;

  @Column(name = "requested_date", nullable = false)
  private LocalDate requestedDate;

  @Column(name = "applicant_name", nullable = false, length = 80)
  private String applicantName;

  @Column(nullable = false, length = 300)
  private String purpose;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private PaymentApplicationStatus status = PaymentApplicationStatus.PENDING_APPROVAL;

  @Column(name = "approval_comment", length = 500)
  private String approvalComment;

  @Column(name = "approver_name", length = 80)
  private String approverName;

  @Column(name = "approved_at")
  private OffsetDateTime approvedAt;

  @Column(name = "payment_id")
  private UUID paymentId;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public UUID getPayableId() {
    return payableId;
  }

  public void setPayableId(UUID payableId) {
    this.payableId = payableId;
  }

  public UUID getSupplierId() {
    return supplierId;
  }

  public void setSupplierId(UUID supplierId) {
    this.supplierId = supplierId;
  }

  public BigDecimal getRequestedAmount() {
    return requestedAmount;
  }

  public void setRequestedAmount(BigDecimal requestedAmount) {
    this.requestedAmount = requestedAmount;
  }

  public LocalDate getRequestedDate() {
    return requestedDate;
  }

  public void setRequestedDate(LocalDate requestedDate) {
    this.requestedDate = requestedDate;
  }

  public String getApplicantName() {
    return applicantName;
  }

  public void setApplicantName(String applicantName) {
    this.applicantName = applicantName;
  }

  public String getPurpose() {
    return purpose;
  }

  public void setPurpose(String purpose) {
    this.purpose = purpose;
  }

  public PaymentApplicationStatus getStatus() {
    return status;
  }

  public void setStatus(PaymentApplicationStatus status) {
    this.status = status;
  }

  public String getApprovalComment() {
    return approvalComment;
  }

  public void setApprovalComment(String approvalComment) {
    this.approvalComment = approvalComment;
  }

  public String getApproverName() {
    return approverName;
  }

  public void setApproverName(String approverName) {
    this.approverName = approverName;
  }

  public OffsetDateTime getApprovedAt() {
    return approvedAt;
  }

  public void setApprovedAt(OffsetDateTime approvedAt) {
    this.approvedAt = approvedAt;
  }

  public UUID getPaymentId() {
    return paymentId;
  }

  public void setPaymentId(UUID paymentId) {
    this.paymentId = paymentId;
  }
}
