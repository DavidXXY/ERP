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
@Table(name = "procurement_order_changes")
public class PurchaseOrderChange extends BaseEntity {

  @Column(name = "order_id", nullable = false)
  private UUID orderId;

  @Column(name = "change_no", nullable = false, length = 64)
  private String changeNo;

  @Column(name = "change_type", nullable = false, length = 24)
  private String changeType;

  @Column(name = "quantity_before", precision = 14, scale = 2)
  private BigDecimal quantityBefore;

  @Column(name = "quantity_after", precision = 14, scale = 2)
  private BigDecimal quantityAfter;

  @Column(name = "unit_price_before", precision = 14, scale = 2)
  private BigDecimal unitPriceBefore;

  @Column(name = "unit_price_after", precision = 14, scale = 2)
  private BigDecimal unitPriceAfter;

  @Column(name = "expected_date_before")
  private LocalDate expectedDateBefore;

  @Column(name = "expected_date_after")
  private LocalDate expectedDateAfter;

  @Column(nullable = false, length = 500)
  private String reason;

  @Column(nullable = false, length = 24)
  private String status = "PENDING";

  @Column(name = "created_by_name", nullable = false, length = 80)
  private String createdByName;

  @Column(name = "decided_by_name", length = 80)
  private String decidedByName;

  @Column(name = "decision_comment", length = 500)
  private String decisionComment;

  @Column(name = "order_version_before", nullable = false)
  private Integer orderVersionBefore;

  @Column(name = "order_version_after")
  private Integer orderVersionAfter;

  @Column(name = "applied_at")
  private OffsetDateTime appliedAt;

  public UUID getOrderId() { return orderId; }
  public void setOrderId(UUID v) { orderId = v; }
  public String getChangeNo() { return changeNo; }
  public void setChangeNo(String v) { changeNo = v; }
  public String getChangeType() { return changeType; }
  public void setChangeType(String v) { changeType = v; }
  public BigDecimal getQuantityBefore() { return quantityBefore; }
  public void setQuantityBefore(BigDecimal v) { quantityBefore = v; }
  public BigDecimal getQuantityAfter() { return quantityAfter; }
  public void setQuantityAfter(BigDecimal v) { quantityAfter = v; }
  public BigDecimal getUnitPriceBefore() { return unitPriceBefore; }
  public void setUnitPriceBefore(BigDecimal v) { unitPriceBefore = v; }
  public BigDecimal getUnitPriceAfter() { return unitPriceAfter; }
  public void setUnitPriceAfter(BigDecimal v) { unitPriceAfter = v; }
  public LocalDate getExpectedDateBefore() { return expectedDateBefore; }
  public void setExpectedDateBefore(LocalDate v) { expectedDateBefore = v; }
  public LocalDate getExpectedDateAfter() { return expectedDateAfter; }
  public void setExpectedDateAfter(LocalDate v) { expectedDateAfter = v; }
  public String getReason() { return reason; }
  public void setReason(String v) { reason = v; }
  public String getStatus() { return status; }
  public void setStatus(String v) { status = v; }
  public String getCreatedByName() { return createdByName; }
  public void setCreatedByName(String v) { createdByName = v; }
  public String getDecidedByName() { return decidedByName; }
  public void setDecidedByName(String v) { decidedByName = v; }
  public String getDecisionComment() { return decisionComment; }
  public void setDecisionComment(String v) { decisionComment = v; }
  public Integer getOrderVersionBefore() { return orderVersionBefore; }
  public void setOrderVersionBefore(Integer v) { orderVersionBefore = v; }
  public Integer getOrderVersionAfter() { return orderVersionAfter; }
  public void setOrderVersionAfter(Integer v) { orderVersionAfter = v; }
  public OffsetDateTime getAppliedAt() { return appliedAt; }
  public void setAppliedAt(OffsetDateTime v) { appliedAt = v; }
}
