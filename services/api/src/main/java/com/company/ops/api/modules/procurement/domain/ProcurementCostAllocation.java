package com.company.ops.api.modules.procurement.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "procurement_cost_allocations", uniqueConstraints = @UniqueConstraint(
    name = "uk_procurement_allocation_tenant_receipt", columnNames = {"tenant_id", "receipt_id"}))
public class ProcurementCostAllocation extends BaseEntity {

  @Column(name = "order_id", nullable = false)
  private UUID orderId;

  @Column(name = "receipt_id", nullable = false)
  private UUID receiptId;

  @Enumerated(EnumType.STRING)
  @Column(name = "cost_type", nullable = false, length = 24)
  private ProcurementCostType costType;

  @Column(name = "project_id")
  private UUID projectId;

  @Column(name = "department_id")
  private UUID departmentId;

  @Column(name = "target_code", nullable = false, length = 64)
  private String targetCode;

  @Column(name = "target_name", nullable = false, length = 180)
  private String targetName;

  @Column(name = "part_name", nullable = false, length = 160)
  private String partName;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  @Column(name = "incurred_date", nullable = false)
  private LocalDate incurredDate;

  public UUID getOrderId() {
    return orderId;
  }

  public void setOrderId(UUID orderId) {
    this.orderId = orderId;
  }

  public UUID getReceiptId() {
    return receiptId;
  }

  public void setReceiptId(UUID receiptId) {
    this.receiptId = receiptId;
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

  public String getTargetCode() {
    return targetCode;
  }

  public void setTargetCode(String targetCode) {
    this.targetCode = targetCode;
  }

  public String getTargetName() {
    return targetName;
  }

  public void setTargetName(String targetName) {
    this.targetName = targetName;
  }

  public String getPartName() {
    return partName;
  }

  public void setPartName(String partName) {
    this.partName = partName;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public LocalDate getIncurredDate() {
    return incurredDate;
  }

  public void setIncurredDate(LocalDate incurredDate) {
    this.incurredDate = incurredDate;
  }
}
