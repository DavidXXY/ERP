package com.company.ops.api.modules.sales.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "sales_order_lines")
public class SalesOrderLine extends BaseEntity {

  @Column(name = "order_id", nullable = false)
  private UUID orderId;

  @Column(name = "part_id", nullable = false)
  private UUID partId;

  @Column(name = "part_name", nullable = false, length = 160)
  private String partName;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal quantity;

  @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
  private BigDecimal unitPrice;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  @Column(name = "shipped_qty", nullable = false, precision = 14, scale = 2)
  private BigDecimal shippedQty = BigDecimal.ZERO;

  @Column(name = "returned_qty", nullable = false, precision = 14, scale = 2)
  private BigDecimal returnedQty = BigDecimal.ZERO;

  public UUID getOrderId() { return orderId; }
  public void setOrderId(UUID orderId) { this.orderId = orderId; }
  public UUID getPartId() { return partId; }
  public void setPartId(UUID partId) { this.partId = partId; }
  public String getPartName() { return partName; }
  public void setPartName(String partName) { this.partName = partName; }
  public BigDecimal getQuantity() { return quantity; }
  public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
  public BigDecimal getUnitPrice() { return unitPrice; }
  public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }
  public BigDecimal getShippedQty() { return shippedQty; }
  public void setShippedQty(BigDecimal shippedQty) { this.shippedQty = shippedQty; }
  public BigDecimal getReturnedQty() { return returnedQty; }
  public void setReturnedQty(BigDecimal returnedQty) { this.returnedQty = returnedQty; }
}
