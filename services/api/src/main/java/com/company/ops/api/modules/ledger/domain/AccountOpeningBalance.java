package com.company.ops.api.modules.ledger.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

@Entity
@Table(name = "fin_account_opening_balances", uniqueConstraints = @UniqueConstraint(
    name = "uk_opening_balance_tenant_year_account", columnNames = {"tenant_id", "fiscal_year", "account_code"}))
public class AccountOpeningBalance extends BaseEntity {
  @Column(name = "fiscal_year", nullable = false) private int fiscalYear;
  @Column(name = "account_code", nullable = false, length = 32) private String accountCode;
  @Column(name = "debit_balance", nullable = false, precision = 14, scale = 2) private BigDecimal debitBalance = BigDecimal.ZERO;
  @Column(name = "credit_balance", nullable = false, precision = 14, scale = 2) private BigDecimal creditBalance = BigDecimal.ZERO;
  @Column(length = 500) private String note;

  public int getFiscalYear() { return fiscalYear; }
  public void setFiscalYear(int value) { fiscalYear = value; }
  public String getAccountCode() { return accountCode; }
  public void setAccountCode(String value) { accountCode = value; }
  public BigDecimal getDebitBalance() { return debitBalance; }
  public void setDebitBalance(BigDecimal value) { debitBalance = value; }
  public BigDecimal getCreditBalance() { return creditBalance; }
  public void setCreditBalance(BigDecimal value) { creditBalance = value; }
  public String getNote() { return note; }
  public void setNote(String value) { note = value; }
}
