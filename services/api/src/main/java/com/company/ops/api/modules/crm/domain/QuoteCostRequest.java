package com.company.ops.api.modules.crm.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "crm_quote_cost_requests")
public class QuoteCostRequest extends BaseEntity {

  @Column(name = "quote_id", nullable = false)
  private UUID quoteId;

  @Column(name = "opportunity_id")
  private UUID opportunityId;

  @Column(name = "customer_id")
  private UUID customerId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private QuoteCostStatus status = QuoteCostStatus.REQUESTED;

  @Column(name = "requested_by", nullable = false, length = 80)
  private String requestedBy;

  @Column(name = "requested_at", nullable = false)
  private OffsetDateTime requestedAt;

  @Column(name = "project_manager", length = 80)
  private String projectManager;

  @Column(name = "labor_cost", nullable = false, precision = 14, scale = 2)
  private BigDecimal laborCost = BigDecimal.ZERO;

  @Column(name = "material_cost", nullable = false, precision = 14, scale = 2)
  private BigDecimal materialCost = BigDecimal.ZERO;

  @Column(name = "subcontract_cost", nullable = false, precision = 14, scale = 2)
  private BigDecimal subcontractCost = BigDecimal.ZERO;

  @Column(name = "subcontract_tax_rate", nullable = false, precision = 5, scale = 2)
  private BigDecimal subcontractTaxRate = BigDecimal.valueOf(13);

  @Column(name = "travel_cost", nullable = false, precision = 14, scale = 2)
  private BigDecimal travelCost = BigDecimal.ZERO;

  @Column(name = "travel_tax_rate", nullable = false, precision = 5, scale = 2)
  private BigDecimal travelTaxRate = BigDecimal.ZERO;

  @Column(name = "equipment_cost", nullable = false, precision = 14, scale = 2)
  private BigDecimal equipmentCost = BigDecimal.ZERO;

  @Column(name = "risk_reserve", nullable = false, precision = 14, scale = 2)
  private BigDecimal riskReserve = BigDecimal.ZERO;

  @Column(name = "risk_reserve_tax_rate", nullable = false, precision = 5, scale = 2)
  private BigDecimal riskReserveTaxRate = BigDecimal.ZERO;

  @Column(name = "other_cost", nullable = false, precision = 14, scale = 2)
  private BigDecimal otherCost = BigDecimal.ZERO;

  @Column(name = "other_tax_rate", nullable = false, precision = 5, scale = 2)
  private BigDecimal otherTaxRate = BigDecimal.valueOf(13);

  @Column(name = "suggested_price", precision = 14, scale = 2)
  private BigDecimal suggestedPrice;

  @Column(name = "cost_remark", length = 800)
  private String costRemark;

  @Column(name = "submitted_at")
  private OffsetDateTime submittedAt;

  @Column(name = "approved_by", length = 80)
  private String approvedBy;

  @Column(name = "approved_at")
  private OffsetDateTime approvedAt;

  @Column(name = "approval_comment", length = 500)
  private String approvalComment;

  public UUID getQuoteId() {
    return quoteId;
  }

  public void setQuoteId(UUID quoteId) {
    this.quoteId = quoteId;
  }

  public UUID getOpportunityId() {
    return opportunityId;
  }

  public void setOpportunityId(UUID opportunityId) {
    this.opportunityId = opportunityId;
  }

  public UUID getCustomerId() {
    return customerId;
  }

  public void setCustomerId(UUID customerId) {
    this.customerId = customerId;
  }

  public QuoteCostStatus getStatus() {
    return status;
  }

  public void setStatus(QuoteCostStatus status) {
    this.status = status;
  }

  public String getRequestedBy() {
    return requestedBy;
  }

  public void setRequestedBy(String requestedBy) {
    this.requestedBy = requestedBy;
  }

  public OffsetDateTime getRequestedAt() {
    return requestedAt;
  }

  public void setRequestedAt(OffsetDateTime requestedAt) {
    this.requestedAt = requestedAt;
  }

  public String getProjectManager() {
    return projectManager;
  }

  public void setProjectManager(String projectManager) {
    this.projectManager = projectManager;
  }

  public BigDecimal getLaborCost() {
    return laborCost;
  }

  public void setLaborCost(BigDecimal laborCost) {
    this.laborCost = laborCost;
  }

  public BigDecimal getMaterialCost() {
    return materialCost;
  }

  public void setMaterialCost(BigDecimal materialCost) {
    this.materialCost = materialCost;
  }

  public BigDecimal getSubcontractCost() {
    return subcontractCost;
  }

  public void setSubcontractCost(BigDecimal subcontractCost) {
    this.subcontractCost = subcontractCost;
  }

  public BigDecimal getTravelCost() {
    return travelCost;
  }

  public void setTravelCost(BigDecimal travelCost) {
    this.travelCost = travelCost;
  }

  public BigDecimal getTravelTaxRate() {
    return travelTaxRate;
  }

  public void setTravelTaxRate(BigDecimal travelTaxRate) {
    this.travelTaxRate = travelTaxRate;
  }

  public BigDecimal getEquipmentCost() {
    return equipmentCost;
  }

  public void setEquipmentCost(BigDecimal equipmentCost) {
    this.equipmentCost = equipmentCost;
  }

  public BigDecimal getRiskReserve() {
    return riskReserve;
  }

  public void setRiskReserve(BigDecimal riskReserve) {
    this.riskReserve = riskReserve;
  }

  public BigDecimal getRiskReserveTaxRate() {
    return riskReserveTaxRate;
  }

  public void setRiskReserveTaxRate(BigDecimal riskReserveTaxRate) {
    this.riskReserveTaxRate = riskReserveTaxRate;
  }

  public BigDecimal getOtherCost() {
    return otherCost;
  }

  public void setOtherCost(BigDecimal otherCost) {
    this.otherCost = otherCost;
  }

  public BigDecimal getOtherTaxRate() {
    return otherTaxRate;
  }

  public void setOtherTaxRate(BigDecimal otherTaxRate) {
    this.otherTaxRate = otherTaxRate;
  }

  public BigDecimal getSuggestedPrice() {
    return suggestedPrice;
  }

  public void setSuggestedPrice(BigDecimal suggestedPrice) {
    this.suggestedPrice = suggestedPrice;
  }

  public String getCostRemark() {
    return costRemark;
  }

  public void setCostRemark(String costRemark) {
    this.costRemark = costRemark;
  }

  public OffsetDateTime getSubmittedAt() {
    return submittedAt;
  }

  public void setSubmittedAt(OffsetDateTime submittedAt) {
    this.submittedAt = submittedAt;
  }

  public String getApprovedBy() {
    return approvedBy;
  }

  public void setApprovedBy(String approvedBy) {
    this.approvedBy = approvedBy;
  }

  public OffsetDateTime getApprovedAt() {
    return approvedAt;
  }

  public void setApprovedAt(OffsetDateTime approvedAt) {
    this.approvedAt = approvedAt;
  }

  public String getApprovalComment() {
    return approvalComment;
  }

  public void setApprovalComment(String approvalComment) {
    this.approvalComment = approvalComment;
  }
}
