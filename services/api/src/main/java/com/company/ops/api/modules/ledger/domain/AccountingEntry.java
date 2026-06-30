package com.company.ops.api.modules.ledger.domain;
import com.company.ops.api.common.domain.BaseEntity; import jakarta.persistence.*; import java.math.BigDecimal; import java.util.UUID;
@Entity @Table(name="fin_accounting_entries")
public class AccountingEntry extends BaseEntity {
  @Column(name="voucher_id",nullable=false) private UUID voucherId; @Column(name="account_code",nullable=false,length=32) private String accountCode;
  @Column(name="account_name",nullable=false,length=120) private String accountName; @Column(nullable=false,precision=14,scale=2) private BigDecimal debit=BigDecimal.ZERO;
  @Column(nullable=false,precision=14,scale=2) private BigDecimal credit=BigDecimal.ZERO; @Column(length=300) private String summary;
  public UUID getVoucherId(){return voucherId;} public void setVoucherId(UUID v){voucherId=v;} public String getAccountCode(){return accountCode;} public void setAccountCode(String v){accountCode=v;} public String getAccountName(){return accountName;} public void setAccountName(String v){accountName=v;} public BigDecimal getDebit(){return debit;} public void setDebit(BigDecimal v){debit=v;} public BigDecimal getCredit(){return credit;} public void setCredit(BigDecimal v){credit=v;} public String getSummary(){return summary;} public void setSummary(String v){summary=v;}
}
