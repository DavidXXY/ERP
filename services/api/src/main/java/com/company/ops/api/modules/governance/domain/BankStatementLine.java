package com.company.ops.api.modules.governance.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "biz_bank_statement_lines", uniqueConstraints = @UniqueConstraint(
    name = "uk_bank_line_tenant_ref", columnNames = {"tenant_id", "account_no_masked", "bank_reference"}))
public class BankStatementLine extends BaseEntity {
  @Column(name = "account_no_masked", nullable = false, length = 80) private String accountNoMasked;
  @Column(name = "transaction_date", nullable = false) private LocalDate transactionDate;
  @Column(nullable = false, length = 12) private String direction;
  @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount;
  @Column(length = 180) private String counterparty;
  @Column(name = "bank_reference", nullable = false, length = 120) private String bankReference;
  @Column(length = 500) private String summary;
  @Enumerated(EnumType.STRING) @Column(name = "reconciliation_status", nullable = false, length = 24) private ReconciliationStatus reconciliationStatus = ReconciliationStatus.UNMATCHED;
  @Column(name = "matched_biz_type", length = 60) private String matchedBizType;
  @Column(name = "matched_biz_id") private UUID matchedBizId;
  @Column(name = "matched_biz_no", length = 100) private String matchedBizNo;
  @Column(name = "matched_at") private OffsetDateTime matchedAt;
  @Column(name = "matched_by", length = 80) private String matchedBy;
  @Column(name = "match_note", length = 500) private String matchNote;

  public String getAccountNoMasked() { return accountNoMasked; }
  public void setAccountNoMasked(String v) { accountNoMasked = v; }
  public LocalDate getTransactionDate() { return transactionDate; }
  public void setTransactionDate(LocalDate v) { transactionDate = v; }
  public String getDirection() { return direction; }
  public void setDirection(String v) { direction = v; }
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal v) { amount = v; }
  public String getCounterparty() { return counterparty; }
  public void setCounterparty(String v) { counterparty = v; }
  public String getBankReference() { return bankReference; }
  public void setBankReference(String v) { bankReference = v; }
  public String getSummary() { return summary; }
  public void setSummary(String v) { summary = v; }
  public ReconciliationStatus getReconciliationStatus() { return reconciliationStatus; }
  public void setReconciliationStatus(ReconciliationStatus v) { reconciliationStatus = v; }
  public String getMatchedBizType() { return matchedBizType; }
  public void setMatchedBizType(String v) { matchedBizType = v; }
  public UUID getMatchedBizId() { return matchedBizId; }
  public void setMatchedBizId(UUID v) { matchedBizId = v; }
  public String getMatchedBizNo() { return matchedBizNo; }
  public void setMatchedBizNo(String v) { matchedBizNo = v; }
  public OffsetDateTime getMatchedAt() { return matchedAt; }
  public void setMatchedAt(OffsetDateTime v) { matchedAt = v; }
  public String getMatchedBy() { return matchedBy; }
  public void setMatchedBy(String v) { matchedBy = v; }
  public String getMatchNote() { return matchNote; }
  public void setMatchNote(String v) { matchNote = v; }
}
