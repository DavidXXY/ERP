package com.company.ops.api.modules.ledger.domain;
import com.company.ops.api.common.domain.BaseEntity; import jakarta.persistence.*; import java.math.BigDecimal; import java.time.LocalDate; import java.time.OffsetDateTime; import java.util.UUID;
@Entity @Table(name="fin_accounting_vouchers")
public class AccountingVoucher extends BaseEntity {
  @Column(nullable=false,length=64) private String code; @Column(name="biz_type",nullable=false,length=60) private String bizType;
  @Column(name="biz_no",nullable=false,length=80) private String bizNo; @Column(name="voucher_date",nullable=false) private LocalDate voucherDate;
  @Column(nullable=false,length=500) private String description; @Enumerated(EnumType.STRING) @Column(nullable=false,length=32) private VoucherStatus status;
  @Column(name="total_debit",nullable=false,precision=14,scale=2) private BigDecimal totalDebit; @Column(name="total_credit",nullable=false,precision=14,scale=2) private BigDecimal totalCredit;
  @Column(name="reviewed_at") private OffsetDateTime reviewedAt; @Column(name="reviewed_by",length=80) private String reviewedBy;
  @Column(name="posted_at") private OffsetDateTime postedAt; @Column(name="posted_by",length=80) private String postedBy;
  @Column(name="reversed_at") private OffsetDateTime reversedAt; @Column(name="reversed_by",length=80) private String reversedBy;
  @Column(name="reversal_reason",length=500) private String reversalReason; @Column(name="reversal_voucher_id") private UUID reversalVoucherId;
  public String getCode(){return code;} public void setCode(String v){code=v;} public String getBizType(){return bizType;} public void setBizType(String v){bizType=v;} public String getBizNo(){return bizNo;} public void setBizNo(String v){bizNo=v;} public LocalDate getVoucherDate(){return voucherDate;} public void setVoucherDate(LocalDate v){voucherDate=v;} public String getDescription(){return description;} public void setDescription(String v){description=v;} public VoucherStatus getStatus(){return status;} public void setStatus(VoucherStatus v){status=v;} public BigDecimal getTotalDebit(){return totalDebit;} public void setTotalDebit(BigDecimal v){totalDebit=v;} public BigDecimal getTotalCredit(){return totalCredit;} public void setTotalCredit(BigDecimal v){totalCredit=v;}
  public OffsetDateTime getReviewedAt(){return reviewedAt;} public void setReviewedAt(OffsetDateTime v){reviewedAt=v;} public String getReviewedBy(){return reviewedBy;} public void setReviewedBy(String v){reviewedBy=v;}
  public OffsetDateTime getPostedAt(){return postedAt;} public void setPostedAt(OffsetDateTime v){postedAt=v;} public String getPostedBy(){return postedBy;} public void setPostedBy(String v){postedBy=v;}
  public OffsetDateTime getReversedAt(){return reversedAt;} public void setReversedAt(OffsetDateTime v){reversedAt=v;} public String getReversedBy(){return reversedBy;} public void setReversedBy(String v){reversedBy=v;}
  public String getReversalReason(){return reversalReason;} public void setReversalReason(String v){reversalReason=v;} public UUID getReversalVoucherId(){return reversalVoucherId;} public void setReversalVoucherId(UUID v){reversalVoucherId=v;}
}
