package com.company.ops.api.modules.inventory.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "inventory_stock_movements")
public class StockMovement extends BaseEntity {

  @Column(name = "part_id", nullable = false)
  private UUID partId;

  @Enumerated(EnumType.STRING)
  @Column(name = "movement_type", nullable = false, length = 40)
  private StockMovementType movementType;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal quantity;

  @Column(name = "source_no", length = 64)
  private String sourceNo;

  @Column(length = 300)
  private String remark;

  public UUID getPartId() {
    return partId;
  }

  public void setPartId(UUID partId) {
    this.partId = partId;
  }

  public StockMovementType getMovementType() {
    return movementType;
  }

  public void setMovementType(StockMovementType movementType) {
    this.movementType = movementType;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public void setQuantity(BigDecimal quantity) {
    this.quantity = quantity;
  }

  public String getSourceNo() {
    return sourceNo;
  }

  public void setSourceNo(String sourceNo) {
    this.sourceNo = sourceNo;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }
}
