package com.company.ops.api.modules.procurement.domain;
import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal; import java.time.LocalDate; import java.time.OffsetDateTime; import java.util.UUID;
@Entity @Table(name="procurement_supplier_quotes")
public class SupplierQuotation extends BaseEntity {
  @Column(name="inquiry_id",nullable=false) private UUID inquiryId;
  @Column(name="supplier_id",nullable=false) private UUID supplierId;
  @Column(name="unit_price",nullable=false) private BigDecimal unitPrice;
  @Column(name="tax_rate",nullable=false) private BigDecimal taxRate;
  @Column(name="delivery_date") private LocalDate deliveryDate;
  @Column(name="payment_terms",length=180) private String paymentTerms;
  @Column(length=500) private String remark;
  @Column(nullable=false) private boolean selected;
  @Column(nullable=false,length=8) private String currency="CNY";
  @Column(name="freight_amount",nullable=false) private BigDecimal freightAmount=BigDecimal.ZERO;
  @Column(name="other_cost_amount",nullable=false) private BigDecimal otherCostAmount=BigDecimal.ZERO;
  @Column(name="technical_score",nullable=false) private BigDecimal technicalScore=BigDecimal.valueOf(100);
  @Column(name="commercial_score",nullable=false) private BigDecimal commercialScore=BigDecimal.valueOf(100);
  @Column(name="total_score",nullable=false) private BigDecimal totalScore=BigDecimal.valueOf(100);
  @Column(name="valid_until") private LocalDate validUntil;
  @Column(name="submission_source",nullable=false,length=32) private String submissionSource="INTERNAL_ENTRY";
  @Column(name="submission_status",nullable=false,length=32) private String submissionStatus="SUBMITTED";
  @Column(name="version_no",nullable=false) private Integer versionNo=1;
  @Column(name="submitted_by_type",nullable=false,length=32) private String submittedByType="INTERNAL_USER";
  @Column(name="submitted_by_id") private UUID submittedById;
  @Column(name="submitted_by_name",length=80) private String submittedByName;
  @Column(name="submitted_at") private OffsetDateTime submittedAt;
  @Column(name="confirmed_by_account_id") private UUID confirmedByAccountId;
  @Column(name="confirmed_at") private OffsetDateTime confirmedAt;
  @Column(name="declined_at") private OffsetDateTime declinedAt;
  @Column(name="decline_reason",length=500) private String declineReason;
  public UUID getInquiryId(){return inquiryId;} public void setInquiryId(UUID v){inquiryId=v;}
  public UUID getSupplierId(){return supplierId;} public void setSupplierId(UUID v){supplierId=v;}
  public BigDecimal getUnitPrice(){return unitPrice;} public void setUnitPrice(BigDecimal v){unitPrice=v;}
  public BigDecimal getTaxRate(){return taxRate;} public void setTaxRate(BigDecimal v){taxRate=v;}
  public LocalDate getDeliveryDate(){return deliveryDate;} public void setDeliveryDate(LocalDate v){deliveryDate=v;}
  public String getPaymentTerms(){return paymentTerms;} public void setPaymentTerms(String v){paymentTerms=v;}
  public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
  public boolean isSelected(){return selected;} public void setSelected(boolean v){selected=v;}
  public String getCurrency(){return currency;} public void setCurrency(String v){currency=v;}
  public BigDecimal getFreightAmount(){return freightAmount;} public void setFreightAmount(BigDecimal v){freightAmount=v;}
  public BigDecimal getOtherCostAmount(){return otherCostAmount;} public void setOtherCostAmount(BigDecimal v){otherCostAmount=v;}
  public BigDecimal getTechnicalScore(){return technicalScore;} public void setTechnicalScore(BigDecimal v){technicalScore=v;}
  public BigDecimal getCommercialScore(){return commercialScore;} public void setCommercialScore(BigDecimal v){commercialScore=v;}
  public BigDecimal getTotalScore(){return totalScore;} public void setTotalScore(BigDecimal v){totalScore=v;}
  public LocalDate getValidUntil(){return validUntil;} public void setValidUntil(LocalDate v){validUntil=v;}
  public String getSubmissionSource(){return submissionSource;} public void setSubmissionSource(String v){submissionSource=v;}
  public String getSubmissionStatus(){return submissionStatus;} public void setSubmissionStatus(String v){submissionStatus=v;}
  public Integer getVersionNo(){return versionNo;} public void setVersionNo(Integer v){versionNo=v;}
  public String getSubmittedByType(){return submittedByType;} public void setSubmittedByType(String v){submittedByType=v;}
  public UUID getSubmittedById(){return submittedById;} public void setSubmittedById(UUID v){submittedById=v;}
  public String getSubmittedByName(){return submittedByName;} public void setSubmittedByName(String v){submittedByName=v;}
  public OffsetDateTime getSubmittedAt(){return submittedAt;} public void setSubmittedAt(OffsetDateTime v){submittedAt=v;}
  public UUID getConfirmedByAccountId(){return confirmedByAccountId;} public void setConfirmedByAccountId(UUID v){confirmedByAccountId=v;}
  public OffsetDateTime getConfirmedAt(){return confirmedAt;} public void setConfirmedAt(OffsetDateTime v){confirmedAt=v;}
  public OffsetDateTime getDeclinedAt(){return declinedAt;} public void setDeclinedAt(OffsetDateTime v){declinedAt=v;}
  public String getDeclineReason(){return declineReason;} public void setDeclineReason(String v){declineReason=v;}
}
