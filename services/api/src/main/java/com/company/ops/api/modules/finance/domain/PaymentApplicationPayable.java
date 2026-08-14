package com.company.ops.api.modules.finance.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "fin_payment_application_payables")
public class PaymentApplicationPayable extends BaseEntity {

  @Column(name = "application_id", nullable = false)
  private UUID applicationId;

  @Column(name = "payable_id", nullable = false)
  private UUID payableId;

  @Column(name = "allocated_amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal allocatedAmount;

  public UUID getApplicationId() { return applicationId; }
  public void setApplicationId(UUID applicationId) { this.applicationId = applicationId; }
  public UUID getPayableId() { return payableId; }
  public void setPayableId(UUID payableId) { this.payableId = payableId; }
  public BigDecimal getAllocatedAmount() { return allocatedAmount; }
  public void setAllocatedAmount(BigDecimal allocatedAmount) { this.allocatedAmount = allocatedAmount; }
}
