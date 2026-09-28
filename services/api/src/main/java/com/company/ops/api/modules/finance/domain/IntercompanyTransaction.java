package com.company.ops.api.modules.finance.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fin_intercompany_transactions")
public class IntercompanyTransaction extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(name = "from_entity_id", nullable = false)
  private UUID fromEntityId;

  @Column(name = "to_entity_id", nullable = false)
  private UUID toEntityId;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 24)
  private String direction = "RECEIVABLE";

  @Column(name = "transaction_date", nullable = false)
  private LocalDate transactionDate;

  @Column(length = 500)
  private String reason;

  @Column(nullable = false, length = 16)
  private String status = "POSTED";

  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public UUID getFromEntityId() { return fromEntityId; }
  public void setFromEntityId(UUID fromEntityId) { this.fromEntityId = fromEntityId; }
  public UUID getToEntityId() { return toEntityId; }
  public void setToEntityId(UUID toEntityId) { this.toEntityId = toEntityId; }
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }
  public String getDirection() { return direction; }
  public void setDirection(String direction) { this.direction = direction; }
  public LocalDate getTransactionDate() { return transactionDate; }
  public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }
  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
}