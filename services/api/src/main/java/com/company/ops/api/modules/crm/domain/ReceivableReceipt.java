package com.company.ops.api.modules.crm.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fin_receivable_receipts")
public class ReceivableReceipt extends BaseEntity {

  @Column(name = "receivable_id", nullable = false)
  private UUID receivableId;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  @Column(name = "received_date", nullable = false)
  private LocalDate receivedDate;

  @Column(name = "reference_no", nullable = false, length = 80)
  private String referenceNo;

  @Column(name = "recorder_name", nullable = false, length = 80)
  private String recorderName;

  public UUID getReceivableId() {
    return receivableId;
  }

  public void setReceivableId(UUID receivableId) {
    this.receivableId = receivableId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public LocalDate getReceivedDate() {
    return receivedDate;
  }

  public void setReceivedDate(LocalDate receivedDate) {
    this.receivedDate = receivedDate;
  }

  public String getReferenceNo() {
    return referenceNo;
  }

  public void setReferenceNo(String referenceNo) {
    this.referenceNo = referenceNo;
  }

  public String getRecorderName() {
    return recorderName;
  }

  public void setRecorderName(String recorderName) {
    this.recorderName = recorderName;
  }
}
