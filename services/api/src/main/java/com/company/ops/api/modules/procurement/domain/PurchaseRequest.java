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

  @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
  private BigDecimal unitPrice = BigDecimal.ZERO;

  @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
  private BigDecimal taxRate = BigDecimal.valueOf(13);

  @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal totalAmount = BigDecimal.ZERO;

  @Column(name = "expected_date")
  private LocalDate expectedDate;

  @Column(length = 300)
  private String reason;

  @Enumerated(EnumType.STRING)
  @Column(name = "cost_type", nullable = false, length = 24)
  private ProcurementCostType costType = ProcurementCostType.DEPARTMENT;

  @Column(name = "project_id")
  private UUID projectId;

  @Column(name = "department_id")
  private UUID departmentId;

  @Column(name = "cost_target_code", nullable = false, length = 64)
  private String costTargetCode;

  @Column(name = "cost_target_name", nullable = false, length = 180)
  private String costTargetName;

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

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(BigDecimal unitPrice) {
    this.unitPrice = unitPrice;
  }

  public BigDecimal getTaxRate() {
    return taxRate;
  }

  public void setTaxRate(BigDecimal taxRate) {
    this.taxRate = taxRate;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public void setTotalAmount(BigDecimal totalAmount) {
    this.totalAmount = totalAmount;
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

  public ProcurementCostType getCostType() {
    return costType;
  }

  public void setCostType(ProcurementCostType costType) {
    this.costType = costType;
  }

  public UUID getProjectId() {
    return projectId;
  }

  public void setProjectId(UUID projectId) {
    this.projectId = projectId;
  }

  public UUID getDepartmentId() {
    return departmentId;
  }

  public void setDepartmentId(UUID departmentId) {
    this.departmentId = departmentId;
  }

  public String getCostTargetCode() {
    return costTargetCode;
  }

  public void setCostTargetCode(String costTargetCode) {
    this.costTargetCode = costTargetCode;
  }

  public String getCostTargetName() {
    return costTargetName;
  }

  public void setCostTargetName(String costTargetName) {
    this.costTargetName = costTargetName;
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
