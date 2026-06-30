package com.company.ops.api.modules.crm.domain;

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
@Table(name = "fin_receivables")
public class Receivable extends BaseEntity {

  @Column(name = "customer_id", nullable = false)
  private UUID customerId;

  @Column(name = "contract_id")
  private UUID contractId;

  @Column(nullable = false, length = 64)
  private String code;

  @Column(name = "source_no", nullable = false, length = 64)
  private String sourceNo;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  @Column(name = "due_date", nullable = false)
  private LocalDate dueDate;

  @Column(name = "invoice_no", length = 80)
  private String invoiceNo;

  @Column(name = "invoice_date")
  private LocalDate invoiceDate;

  @Column(name = "settled_amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal settledAmount = BigDecimal.ZERO;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private ReceivableStatus status = ReceivableStatus.INVOICE_PENDING;

  public UUID getCustomerId() {
    return customerId;
  }

  public void setCustomerId(UUID customerId) {
    this.customerId = customerId;
  }

  public UUID getContractId() {
    return contractId;
  }

  public void setContractId(UUID contractId) {
    this.contractId = contractId;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getSourceNo() {
    return sourceNo;
  }

  public void setSourceNo(String sourceNo) {
    this.sourceNo = sourceNo;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public void setDueDate(LocalDate dueDate) {
    this.dueDate = dueDate;
  }

  public ReceivableStatus getStatus() {
    return status;
  }

  public void setStatus(ReceivableStatus status) {
    this.status = status;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public void setInvoiceNo(String invoiceNo) {
    this.invoiceNo = invoiceNo;
  }

  public LocalDate getInvoiceDate() {
    return invoiceDate;
  }

  public void setInvoiceDate(LocalDate invoiceDate) {
    this.invoiceDate = invoiceDate;
  }

  public BigDecimal getSettledAmount() {
    return settledAmount;
  }

  public void setSettledAmount(BigDecimal settledAmount) {
    this.settledAmount = settledAmount;
  }
}
