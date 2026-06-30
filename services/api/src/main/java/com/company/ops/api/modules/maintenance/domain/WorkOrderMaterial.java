package com.company.ops.api.modules.maintenance.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "work_order_materials")
public class WorkOrderMaterial extends BaseEntity {
  @Column(name = "work_order_id", nullable = false) private UUID workOrderId;
  @Column(name = "part_id", nullable = false) private UUID partId;
  @Column(name = "part_name", nullable = false, length = 160) private String partName;
  @Column(nullable = false, precision = 14, scale = 2) private BigDecimal quantity;
  @Column(name = "unit_cost", nullable = false, precision = 14, scale = 2) private BigDecimal unitCost;
  @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount;
  public UUID getWorkOrderId() { return workOrderId; } public void setWorkOrderId(UUID value) { workOrderId = value; }
  public UUID getPartId() { return partId; } public void setPartId(UUID value) { partId = value; }
  public String getPartName() { return partName; } public void setPartName(String value) { partName = value; }
  public BigDecimal getQuantity() { return quantity; } public void setQuantity(BigDecimal value) { quantity = value; }
  public BigDecimal getUnitCost() { return unitCost; } public void setUnitCost(BigDecimal value) { unitCost = value; }
  public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal value) { amount = value; }
}
