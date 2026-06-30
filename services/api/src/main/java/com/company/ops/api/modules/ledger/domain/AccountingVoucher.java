package com.company.ops.api.modules.ledger.domain;
import com.company.ops.api.common.domain.BaseEntity; import jakarta.persistence.*; import java.math.BigDecimal; import java.time.LocalDate;
@Entity @Table(name="fin_accounting_vouchers")
public class AccountingVoucher extends BaseEntity {
  @Column(nullable=false,length=64) private String code; @Column(name="biz_type",nullable=false,length=60) private String bizType;
  @Column(name="biz_no",nullable=false,length=80) private String bizNo; @Column(name="voucher_date",nullable=false) private LocalDate voucherDate;
  @Column(nullable=false,length=500) private String description; @Enumerated(EnumType.STRING) @Column(nullable=false,length=32) private VoucherStatus status;
  @Column(name="total_debit",nullable=false,precision=14,scale=2) private BigDecimal totalDebit; @Column(name="total_credit",nullable=false,precision=14,scale=2) private BigDecimal totalCredit;
  public String getCode(){return code;} public void setCode(String v){code=v;} public String getBizType(){return bizType;} public void setBizType(String v){bizType=v;} public String getBizNo(){return bizNo;} public void setBizNo(String v){bizNo=v;} public LocalDate getVoucherDate(){return voucherDate;} public void setVoucherDate(LocalDate v){voucherDate=v;} public String getDescription(){return description;} public void setDescription(String v){description=v;} public VoucherStatus getStatus(){return status;} public void setStatus(VoucherStatus v){status=v;} public BigDecimal getTotalDebit(){return totalDebit;} public void setTotalDebit(BigDecimal v){totalDebit=v;} public BigDecimal getTotalCredit(){return totalCredit;} public void setTotalCredit(BigDecimal v){totalCredit=v;}
}
