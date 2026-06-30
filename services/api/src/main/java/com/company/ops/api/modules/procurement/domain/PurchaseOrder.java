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
@Table(name = "procurement_purchase_orders")
public class PurchaseOrder extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(name = "supplier_id", nullable = false)
  private UUID supplierId;

  @Column(name = "request_id")
  private UUID requestId;

  @Column(name = "part_id")
  private UUID partId;

  @Column(name = "part_name", nullable = false, length = 160)
  private String partName;

  @Column(name = "ordered_qty", nullable = false, precision = 14, scale = 2)
  private BigDecimal orderedQty = BigDecimal.ZERO;

  @Column(name = "received_qty", nullable = false, precision = 14, scale = 2)
  private BigDecimal receivedQty = BigDecimal.ZERO;

  @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
  private BigDecimal unitPrice = BigDecimal.ZERO;

  @Column(name = "order_amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal orderAmount = BigDecimal.ZERO;

  @Column(name = "expected_delivery_date")
  private LocalDate expectedDeliveryDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private PurchaseOrderStatus status = PurchaseOrderStatus.ORDERED;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public UUID getSupplierId() {
    return supplierId;
  }

  public void setSupplierId(UUID supplierId) {
    this.supplierId = supplierId;
  }

  public UUID getRequestId() {
    return requestId;
  }

  public void setRequestId(UUID requestId) {
    this.requestId = requestId;
  }

  public BigDecimal getOrderAmount() {
    return orderAmount;
  }

  public void setOrderAmount(BigDecimal orderAmount) {
    this.orderAmount = orderAmount;
  }

  public LocalDate getExpectedDeliveryDate() {
    return expectedDeliveryDate;
  }

  public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
    this.expectedDeliveryDate = expectedDeliveryDate;
  }

  public PurchaseOrderStatus getStatus() {
    return status;
  }

  public void setStatus(PurchaseOrderStatus status) {
    this.status = status;
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

  public BigDecimal getOrderedQty() {
    return orderedQty;
  }

  public void setOrderedQty(BigDecimal orderedQty) {
    this.orderedQty = orderedQty;
  }

  public BigDecimal getReceivedQty() {
    return receivedQty;
  }

  public void setReceivedQty(BigDecimal receivedQty) {
    this.receivedQty = receivedQty;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(BigDecimal unitPrice) {
    this.unitPrice = unitPrice;
  }
}
