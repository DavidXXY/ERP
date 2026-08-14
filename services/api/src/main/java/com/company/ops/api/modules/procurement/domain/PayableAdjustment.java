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
@Table(name = "fin_procurement_payable_adjustments")
public class PayableAdjustment extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(name = "payable_id", nullable = false)
  private UUID payableId;

  @Column(name = "order_id", nullable = false)
  private UUID orderId;

  @Column(name = "supplier_id", nullable = false)
  private UUID supplierId;

  @Enumerated(EnumType.STRING)
  @Column(name = "adjustment_type", nullable = false, length = 32)
  private PayableAdjustmentType adjustmentType;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  @Column(length = 500)
  private String reason;

  @Column(name = "operator_name", nullable = false, length = 80)
  private String operatorName;

  @Column(name = "applied_at", nullable = false)
  private LocalDate appliedAt;

  @Column(nullable = false, length = 20)
  private String status = "APPLIED";

  @Column(nullable = false, length = 32)
  private String source = "MANUAL";

  @Column(name = "source_id")
  private UUID sourceId;

  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public UUID getPayableId() { return payableId; }
  public void setPayableId(UUID payableId) { this.payableId = payableId; }
  public UUID getOrderId() { return orderId; }
  public void setOrderId(UUID orderId) { this.orderId = orderId; }
  public UUID getSupplierId() { return supplierId; }
  public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }
  public PayableAdjustmentType getAdjustmentType() { return adjustmentType; }
  public void setAdjustmentType(PayableAdjustmentType adjustmentType) { this.adjustmentType = adjustmentType; }
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }
  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }
  public String getOperatorName() { return operatorName; }
  public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
  public LocalDate getAppliedAt() { return appliedAt; }
  public void setAppliedAt(LocalDate appliedAt) { this.appliedAt = appliedAt; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getSource() { return source; }
  public void setSource(String source) { this.source = source; }
  public UUID getSourceId() { return sourceId; }
  public void setSourceId(UUID sourceId) { this.sourceId = sourceId; }
}
