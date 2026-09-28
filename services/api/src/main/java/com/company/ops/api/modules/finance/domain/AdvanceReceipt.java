package com.company.ops.api.modules.finance.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fin_advance_receipts")
public class AdvanceReceipt extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(name = "customer_id", nullable = false)
  private UUID customerId;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  @Column(name = "settled_amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal settledAmount = BigDecimal.ZERO;

  @Column(name = "received_date", nullable = false)
  private LocalDate receivedDate;

  @Column(name = "reference_no", length = 80)
  private String referenceNo;

  @Column(nullable = false, length = 16)
  private String status = "OPEN";

  @Column(length = 500)
  private String remark;

  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public UUID getCustomerId() { return customerId; }
  public void setCustomerId(UUID customerId) { this.customerId = customerId; }
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }
  public BigDecimal getSettledAmount() { return settledAmount; }
  public void setSettledAmount(BigDecimal settledAmount) { this.settledAmount = settledAmount; }
  public LocalDate getReceivedDate() { return receivedDate; }
  public void setReceivedDate(LocalDate receivedDate) { this.receivedDate = receivedDate; }
  public String getReferenceNo() { return referenceNo; }
  public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
}
