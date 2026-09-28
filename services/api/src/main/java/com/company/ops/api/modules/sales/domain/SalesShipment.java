package com.company.ops.api.modules.sales.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "sales_shipments")
public class SalesShipment extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(name = "order_id", nullable = false)
  private UUID orderId;

  @Column(name = "customer_id", nullable = false)
  private UUID customerId;

  @Column(name = "contract_id")
  private UUID contractId;

  @Column(name = "shipment_date", nullable = false)
  private LocalDate shipmentDate;

  @Column(name = "receiver_name", length = 80)
  private String receiverName;

  @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal totalAmount = BigDecimal.ZERO;

  @Column(length = 500)
  private String remark;

  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public UUID getOrderId() { return orderId; }
  public void setOrderId(UUID orderId) { this.orderId = orderId; }
  public UUID getCustomerId() { return customerId; }
  public void setCustomerId(UUID customerId) { this.customerId = customerId; }
  public UUID getContractId() { return contractId; }
  public void setContractId(UUID contractId) { this.contractId = contractId; }
  public LocalDate getShipmentDate() { return shipmentDate; }
  public void setShipmentDate(LocalDate shipmentDate) { this.shipmentDate = shipmentDate; }
  public String getReceiverName() { return receiverName; }
  public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
  public BigDecimal getTotalAmount() { return totalAmount; }
  public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
}
