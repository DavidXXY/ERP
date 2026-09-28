package com.company.ops.api.modules.sales.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "sales_return_orders")
public class SalesReturnOrder extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(name = "shipment_id", nullable = false)
  private UUID shipmentId;

  @Column(name = "order_id", nullable = false)
  private UUID orderId;

  @Column(name = "customer_id", nullable = false)
  private UUID customerId;

  @Column(name = "contract_id")
  private UUID contractId;

  @Column(name = "return_date", nullable = false)
  private LocalDate returnDate;

  @Column(length = 500)
  private String reason;

  @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal totalAmount = BigDecimal.ZERO;

  @Column(nullable = false, length = 24)
  private String status = "POSTED";

  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public UUID getShipmentId() { return shipmentId; }
  public void setShipmentId(UUID shipmentId) { this.shipmentId = shipmentId; }
  public UUID getOrderId() { return orderId; }
  public void setOrderId(UUID orderId) { this.orderId = orderId; }
  public UUID getCustomerId() { return customerId; }
  public void setCustomerId(UUID customerId) { this.customerId = customerId; }
  public UUID getContractId() { return contractId; }
  public void setContractId(UUID contractId) { this.contractId = contractId; }
  public LocalDate getReturnDate() { return returnDate; }
  public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }
  public BigDecimal getTotalAmount() { return totalAmount; }
  public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
}
