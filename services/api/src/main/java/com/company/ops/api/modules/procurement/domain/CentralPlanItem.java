package com.company.ops.api.modules.procurement.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "procurement_central_plan_items")
public class CentralPlanItem extends BaseEntity {

  @Column(name = "plan_id", nullable = false)
  private UUID planId;

  @Column(name = "part_id", nullable = false)
  private UUID partId;

  @Column(name = "part_name", nullable = false, length = 160)
  private String partName;

  @Column(name = "planned_qty", nullable = false, precision = 14, scale = 2)
  private BigDecimal plannedQty;

  @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
  private BigDecimal unitPrice = BigDecimal.ZERO;

  @Column(name = "expected_date")
  private LocalDate expectedDate;

  @Column(name = "request_id")
  private UUID requestId;

  @Column(nullable = false, length = 24)
  private String status = "PLANNED";

  public UUID getPlanId() { return planId; }
  public void setPlanId(UUID v) { planId = v; }
  public UUID getPartId() { return partId; }
  public void setPartId(UUID v) { partId = v; }
  public String getPartName() { return partName; }
  public void setPartName(String v) { partName = v; }
  public BigDecimal getPlannedQty() { return plannedQty; }
  public void setPlannedQty(BigDecimal v) { plannedQty = v; }
  public BigDecimal getUnitPrice() { return unitPrice; }
  public void setUnitPrice(BigDecimal v) { unitPrice = v; }
  public LocalDate getExpectedDate() { return expectedDate; }
  public void setExpectedDate(LocalDate v) { expectedDate = v; }
  public UUID getRequestId() { return requestId; }
  public void setRequestId(UUID v) { requestId = v; }
  public String getStatus() { return status; }
  public void setStatus(String v) { status = v; }
}
