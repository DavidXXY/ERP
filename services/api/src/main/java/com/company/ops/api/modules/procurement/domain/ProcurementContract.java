package com.company.ops.api.modules.procurement.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "procurement_contracts")
public class ProcurementContract extends BaseEntity {
  @Column(name = "contract_no", nullable = false, length = 80)
  private String contractNo;

  @Column(nullable = false, length = 180)
  private String name;

  @Column(name = "supplier_id", nullable = false)
  private UUID supplierId;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount = BigDecimal.ZERO;

  @Column(nullable = false, length = 8)
  private String currency = "CNY";

  @Column(name = "start_date")
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @Column(name = "payment_terms", length = 500)
  private String paymentTerms;

  @Column(nullable = false, length = 32)
  private String status = "DRAFT";

  @Column(name = "approval_status", nullable = false, length = 32)
  private String approvalStatus = "PENDING";

  @Column(name = "version_no", nullable = false)
  private int versionNo = 1;

  @Column(name = "parent_contract_id")
  private UUID parentContractId;

  @Column(name = "change_reason", length = 1000)
  private String changeReason;

  @Column(name = "submitted_by_name", length = 80)
  private String submittedByName;

  @Column(name = "submitted_at")
  private OffsetDateTime submittedAt;

  @Column(name = "approved_by_name", length = 80)
  private String approvedByName;

  @Column(name = "approval_comment", length = 500)
  private String approvalComment;

  @Column(name = "approved_at")
  private OffsetDateTime approvedAt;

  @Column(length = 1000)
  private String remark;

  public String getContractNo() { return contractNo; }
  public void setContractNo(String contractNo) { this.contractNo = contractNo; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public UUID getSupplierId() { return supplierId; }
  public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }
  public String getCurrency() { return currency; }
  public void setCurrency(String currency) { this.currency = currency; }
  public LocalDate getStartDate() { return startDate; }
  public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
  public LocalDate getEndDate() { return endDate; }
  public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
  public String getPaymentTerms() { return paymentTerms; }
  public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getApprovalStatus() { return approvalStatus; }
  public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }
  public int getVersionNo() { return versionNo; }
  public void setVersionNo(int versionNo) { this.versionNo = versionNo; }
  public UUID getParentContractId() { return parentContractId; }
  public void setParentContractId(UUID parentContractId) { this.parentContractId = parentContractId; }
  public String getChangeReason() { return changeReason; }
  public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
  public String getSubmittedByName() { return submittedByName; }
  public void setSubmittedByName(String submittedByName) { this.submittedByName = submittedByName; }
  public OffsetDateTime getSubmittedAt() { return submittedAt; }
  public void setSubmittedAt(OffsetDateTime submittedAt) { this.submittedAt = submittedAt; }
  public String getApprovedByName() { return approvedByName; }
  public void setApprovedByName(String approvedByName) { this.approvedByName = approvedByName; }
  public String getApprovalComment() { return approvalComment; }
  public void setApprovalComment(String approvalComment) { this.approvalComment = approvalComment; }
  public OffsetDateTime getApprovedAt() { return approvedAt; }
  public void setApprovedAt(OffsetDateTime approvedAt) { this.approvedAt = approvedAt; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
}
