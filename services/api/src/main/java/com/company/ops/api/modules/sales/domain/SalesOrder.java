package com.company.ops.api.modules.sales.domain;

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
@Table(name = "sales_orders")
public class SalesOrder extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(name = "customer_id", nullable = false)
  private UUID customerId;

  @Column(name = "contract_id")
  private UUID contractId;

  @Column(name = "order_date", nullable = false)
  private LocalDate orderDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private SalesOrderStatus status = SalesOrderStatus.DRAFT;

  @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal totalAmount = BigDecimal.ZERO;

  @Column(length = 500)
  private String remark;

  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public UUID getCustomerId() { return customerId; }
  public void setCustomerId(UUID customerId) { this.customerId = customerId; }
  public UUID getContractId() { return contractId; }
  public void setContractId(UUID contractId) { this.contractId = contractId; }
  public LocalDate getOrderDate() { return orderDate; }
  public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }
  public SalesOrderStatus getStatus() { return status; }
  public void setStatus(SalesOrderStatus status) { this.status = status; }
  public BigDecimal getTotalAmount() { return totalAmount; }
  public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
}
