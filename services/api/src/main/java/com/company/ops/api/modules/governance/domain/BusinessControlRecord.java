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
@Table(name = "biz_control_records", uniqueConstraints = @UniqueConstraint(
    name = "uk_control_record_tenant_code", columnNames = {"tenant_id", "control_code"}))
public class BusinessControlRecord extends BaseEntity {
  @Column(name = "control_code", nullable = false, length = 64) private String controlCode;
  @Enumerated(EnumType.STRING) @Column(name = "control_type", nullable = false, length = 48) private ControlType controlType;
  @Column(name = "business_domain", nullable = false, length = 32) private String businessDomain;
  @Column(name = "business_id") private UUID businessId;
  @Column(name = "business_no", length = 100) private String businessNo;
  @Column(nullable = false, length = 180) private String name;
  @Column(nullable = false, length = 80) private String owner;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private ControlStatus status = ControlStatus.DRAFT;
  @Column(name = "risk_level", nullable = false, length = 16) private String riskLevel = "LOW";
  @Column(name = "planned_start") private LocalDate plannedStart;
  @Column(name = "planned_end") private LocalDate plannedEnd;
  @Column(name = "effective_from") private LocalDate effectiveFrom;
  @Column(name = "effective_to") private LocalDate effectiveTo;
  @Column(name = "budget_amount", nullable = false, precision = 14, scale = 2) private BigDecimal budgetAmount = BigDecimal.ZERO;
  @Column(name = "committed_amount", nullable = false, precision = 14, scale = 2) private BigDecimal committedAmount = BigDecimal.ZERO;
  @Column(name = "actual_amount", nullable = false, precision = 14, scale = 2) private BigDecimal actualAmount = BigDecimal.ZERO;
  @Column(name = "forecast_amount", nullable = false, precision = 14, scale = 2) private BigDecimal forecastAmount = BigDecimal.ZERO;
  @Column(name = "progress_percent", nullable = false, precision = 5, scale = 2) private BigDecimal progressPercent = BigDecimal.ZERO;
  @Column(name = "review_frequency_days") private Integer reviewFrequencyDays;
  @Column(name = "last_reviewed_on") private LocalDate lastReviewedOn;
  @Column(name = "next_review_on") private LocalDate nextReviewOn;
  @Column(columnDefinition = "text") private String details;
  @Column(name = "activated_at") private OffsetDateTime activatedAt;
  @Column(name = "completed_at") private OffsetDateTime completedAt;
  @Column(name = "completed_by", length = 80) private String completedBy;
  @Column(name = "completion_note", length = 1000) private String completionNote;

  public String getControlCode() { return controlCode; } public void setControlCode(String v) { controlCode = v; }
  public ControlType getControlType() { return controlType; } public void setControlType(ControlType v) { controlType = v; }
  public String getBusinessDomain() { return businessDomain; } public void setBusinessDomain(String v) { businessDomain = v; }
  public UUID getBusinessId() { return businessId; } public void setBusinessId(UUID v) { businessId = v; }
  public String getBusinessNo() { return businessNo; } public void setBusinessNo(String v) { businessNo = v; }
  public String getName() { return name; } public void setName(String v) { name = v; }
  public String getOwner() { return owner; } public void setOwner(String v) { owner = v; }
  public ControlStatus getStatus() { return status; } public void setStatus(ControlStatus v) { status = v; }
  public String getRiskLevel() { return riskLevel; } public void setRiskLevel(String v) { riskLevel = v; }
  public LocalDate getPlannedStart() { return plannedStart; } public void setPlannedStart(LocalDate v) { plannedStart = v; }
  public LocalDate getPlannedEnd() { return plannedEnd; } public void setPlannedEnd(LocalDate v) { plannedEnd = v; }
  public LocalDate getEffectiveFrom() { return effectiveFrom; } public void setEffectiveFrom(LocalDate v) { effectiveFrom = v; }
  public LocalDate getEffectiveTo() { return effectiveTo; } public void setEffectiveTo(LocalDate v) { effectiveTo = v; }
  public BigDecimal getBudgetAmount() { return budgetAmount; } public void setBudgetAmount(BigDecimal v) { budgetAmount = v; }
  public BigDecimal getCommittedAmount() { return committedAmount; } public void setCommittedAmount(BigDecimal v) { committedAmount = v; }
  public BigDecimal getActualAmount() { return actualAmount; } public void setActualAmount(BigDecimal v) { actualAmount = v; }
  public BigDecimal getForecastAmount() { return forecastAmount; } public void setForecastAmount(BigDecimal v) { forecastAmount = v; }
  public BigDecimal getProgressPercent() { return progressPercent; } public void setProgressPercent(BigDecimal v) { progressPercent = v; }
  public Integer getReviewFrequencyDays() { return reviewFrequencyDays; } public void setReviewFrequencyDays(Integer v) { reviewFrequencyDays = v; }
  public LocalDate getLastReviewedOn() { return lastReviewedOn; } public void setLastReviewedOn(LocalDate v) { lastReviewedOn = v; }
  public LocalDate getNextReviewOn() { return nextReviewOn; } public void setNextReviewOn(LocalDate v) { nextReviewOn = v; }
  public String getDetails() { return details; } public void setDetails(String v) { details = v; }
  public OffsetDateTime getActivatedAt() { return activatedAt; } public void setActivatedAt(OffsetDateTime v) { activatedAt = v; }
  public OffsetDateTime getCompletedAt() { return completedAt; } public void setCompletedAt(OffsetDateTime v) { completedAt = v; }
  public String getCompletedBy() { return completedBy; } public void setCompletedBy(String v) { completedBy = v; }
  public String getCompletionNote() { return completionNote; } public void setCompletionNote(String v) { completionNote = v; }
}
