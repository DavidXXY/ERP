package com.company.ops.api.modules.procurement.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "procurement_purchase_requests")
public class PurchaseRequest extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(name = "requester_name", nullable = false, length = 80)
  private String requesterName;

  @Column(name = "part_id")
  private UUID partId;

  @Column(name = "part_name", nullable = false, length = 160)
  private String partName;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal quantity = BigDecimal.ZERO;

  @Column(name = "expected_date")
  private LocalDate expectedDate;

  @Column(length = 300)
  private String reason;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private PurchaseRequestStatus status = PurchaseRequestStatus.SUBMITTED;

  @Enumerated(EnumType.STRING)
  @Column(name = "approval_status", nullable = false, length = 32)
  private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getRequesterName() {
    return requesterName;
  }

  public void setRequesterName(String requesterName) {
    this.requesterName = requesterName;
  }

  public UUID getPartId() {
    return partId;
  }

  public void setPartId(UUID partId) {
    this.partId = partId;
  }

  public String getPartName() {
    return partName;
  }

  public void setPartName(String partName) {
    this.partName = partName;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public void setQuantity(BigDecimal quantity) {
    this.quantity = quantity;
  }

  public LocalDate getExpectedDate() {
    return expectedDate;
  }

  public void setExpectedDate(LocalDate expectedDate) {
    this.expectedDate = expectedDate;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public PurchaseRequestStatus getStatus() {
    return status;
  }

  public void setStatus(PurchaseRequestStatus status) {
    this.status = status;
  }

  public ApprovalStatus getApprovalStatus() {
    return approvalStatus;
  }

  public void setApprovalStatus(ApprovalStatus approvalStatus) {
    this.approvalStatus = approvalStatus;
  }
}
