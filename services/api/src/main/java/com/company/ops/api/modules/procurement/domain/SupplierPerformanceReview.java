package com.company.ops.api.modules.procurement.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "procurement_supplier_reviews")
public class SupplierPerformanceReview extends BaseEntity {
  @Column(name = "supplier_id", nullable = false)
  private UUID supplierId;
  @Column(name = "review_period", nullable = false, length = 20)
  private String reviewPeriod;
  @Column(name = "on_time_rate", nullable = false, precision = 5, scale = 2)
  private BigDecimal onTimeRate = BigDecimal.ZERO;
  @Column(name = "quality_rate", nullable = false, precision = 5, scale = 2)
  private BigDecimal qualityRate = BigDecimal.ZERO;
  @Column(name = "invoice_match_rate", nullable = false, precision = 5, scale = 2)
  private BigDecimal invoiceMatchRate = BigDecimal.ZERO;
  @Column(name = "response_score", nullable = false, precision = 5, scale = 2)
  private BigDecimal responseScore = BigDecimal.valueOf(100);
  @Column(name = "total_score", nullable = false, precision = 5, scale = 2)
  private BigDecimal totalScore = BigDecimal.ZERO;
  @Column(nullable = false, length = 20)
  private String grade;
  @Column(name = "reviewer_name", nullable = false, length = 80)
  private String reviewerName;
  @Column(name = "improvement_action", length = 1000)
  private String improvementAction;
  @Column(nullable = false, length = 32)
  private String status = "ACTIVE";

  @Column(name = "appeal_status", nullable = false, length = 20)
  private String appealStatus = "NONE";

  @Column(name = "appeal_reason", length = 1000)
  private String appealReason;

  @Column(name = "appealed_at")
  private OffsetDateTime appealedAt;

  @Column(name = "appeal_resolution", length = 32)
  private String appealResolution;

  @Column(name = "appeal_review_comment", length = 1000)
  private String appealReviewComment;

  @Column(name = "appeal_reviewed_by", length = 80)
  private String appealReviewedBy;

  @Column(name = "appeal_reviewed_at")
  private OffsetDateTime appealReviewedAt;

  public UUID getSupplierId() { return supplierId; }
  public void setSupplierId(UUID value) { supplierId = value; }
  public String getReviewPeriod() { return reviewPeriod; }
  public void setReviewPeriod(String value) { reviewPeriod = value; }
  public BigDecimal getOnTimeRate() { return onTimeRate; }
  public void setOnTimeRate(BigDecimal value) { onTimeRate = value; }
  public BigDecimal getQualityRate() { return qualityRate; }
  public void setQualityRate(BigDecimal value) { qualityRate = value; }
  public BigDecimal getInvoiceMatchRate() { return invoiceMatchRate; }
  public void setInvoiceMatchRate(BigDecimal value) { invoiceMatchRate = value; }
  public BigDecimal getResponseScore() { return responseScore; }
  public void setResponseScore(BigDecimal value) { responseScore = value; }
  public BigDecimal getTotalScore() { return totalScore; }
  public void setTotalScore(BigDecimal value) { totalScore = value; }
  public String getGrade() { return grade; }
  public void setGrade(String value) { grade = value; }
  public String getReviewerName() { return reviewerName; }
  public void setReviewerName(String value) { reviewerName = value; }
  public String getImprovementAction() { return improvementAction; }
  public void setImprovementAction(String value) { improvementAction = value; }
  public String getStatus() { return status; }
  public void setStatus(String value) { status = value; }

  public String getAppealStatus() { return appealStatus; }
  public void setAppealStatus(String value) { appealStatus = value; }
  public String getAppealReason() { return appealReason; }
  public void setAppealReason(String value) { appealReason = value; }
  public OffsetDateTime getAppealedAt() { return appealedAt; }
  public void setAppealedAt(OffsetDateTime value) { appealedAt = value; }
  public String getAppealResolution() { return appealResolution; }
  public void setAppealResolution(String value) { appealResolution = value; }
  public String getAppealReviewComment() { return appealReviewComment; }
  public void setAppealReviewComment(String value) { appealReviewComment = value; }
  public String getAppealReviewedBy() { return appealReviewedBy; }
  public void setAppealReviewedBy(String value) { appealReviewedBy = value; }
  public OffsetDateTime getAppealReviewedAt() { return appealReviewedAt; }
  public void setAppealReviewedAt(OffsetDateTime value) { appealReviewedAt = value; }
}
