package com.company.ops.api.modules.finance.domain;

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
@Table(name = "fin_payment_records")
public class PaymentRecord extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(name = "application_id", nullable = false)
  private UUID applicationId;

  @Column(name = "payable_id", nullable = false)
  private UUID payableId;

  @Column(name = "supplier_id", nullable = false)
  private UUID supplierId;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  @Column(name = "paid_date", nullable = false)
  private LocalDate paidDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_method", nullable = false, length = 32)
  private PaymentMethod paymentMethod;

  @Column(name = "bank_reference", nullable = false, length = 100)
  private String bankReference;

  @Column(name = "payer_name", nullable = false, length = 80)
  private String payerName;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public UUID getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(UUID applicationId) {
    this.applicationId = applicationId;
  }

  public UUID getPayableId() {
    return payableId;
  }

  public void setPayableId(UUID payableId) {
    this.payableId = payableId;
  }

  public UUID getSupplierId() {
    return supplierId;
  }

  public void setSupplierId(UUID supplierId) {
    this.supplierId = supplierId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public LocalDate getPaidDate() {
    return paidDate;
  }

  public void setPaidDate(LocalDate paidDate) {
    this.paidDate = paidDate;
  }

  public PaymentMethod getPaymentMethod() {
    return paymentMethod;
  }

  public void setPaymentMethod(PaymentMethod paymentMethod) {
    this.paymentMethod = paymentMethod;
  }

  public String getBankReference() {
    return bankReference;
  }

  public void setBankReference(String bankReference) {
    this.bankReference = bankReference;
  }

  public String getPayerName() {
    return payerName;
  }

  public void setPayerName(String payerName) {
    this.payerName = payerName;
  }
}
