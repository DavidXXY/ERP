package com.company.ops.api.modules.sales.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "sales_shipment_lines")
public class SalesShipmentLine extends BaseEntity {

  @Column(name = "shipment_id", nullable = false)
  private UUID shipmentId;

  @Column(name = "order_line_id", nullable = false)
  private UUID orderLineId;

  @Column(name = "part_id", nullable = false)
  private UUID partId;

  @Column(name = "part_name", nullable = false, length = 160)
  private String partName;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal quantity;

  @Column(name = "unit_cost", nullable = false, precision = 14, scale = 4)
  private BigDecimal unitCost;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  @Column(name = "returned_qty", nullable = false, precision = 14, scale = 2)
  private BigDecimal returnedQty = BigDecimal.ZERO;

  public UUID getShipmentId() { return shipmentId; }
  public void setShipmentId(UUID shipmentId) { this.shipmentId = shipmentId; }
  public UUID getOrderLineId() { return orderLineId; }
  public void setOrderLineId(UUID orderLineId) { this.orderLineId = orderLineId; }
  public UUID getPartId() { return partId; }
  public void setPartId(UUID partId) { this.partId = partId; }
  public String getPartName() { return partName; }
  public void setPartName(String partName) { this.partName = partName; }
  public BigDecimal getQuantity() { return quantity; }
  public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
  public BigDecimal getUnitCost() { return unitCost; }
  public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }
  public BigDecimal getReturnedQty() { return returnedQty; }
  public void setReturnedQty(BigDecimal returnedQty) { this.returnedQty = returnedQty; }
}
