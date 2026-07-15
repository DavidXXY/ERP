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
@Table(name = "crm_quote_plans")
public class QuotePlan extends BaseEntity {

  @Column(name = "customer_id")
  private UUID customerId;

  @Column(name = "opportunity_id")
  private UUID opportunityId;

  @Column(nullable = false, length = 64)
  private String code;

  @Column(name = "service_scope", nullable = false, length = 800)
  private String serviceScope;

  @Column(name = "inspect_cycle", length = 120)
  private String inspectCycle;

  @Column(name = "payment_nodes", length = 300)
  private String paymentNodes;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount = BigDecimal.ZERO;

  @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
  private BigDecimal taxRate = BigDecimal.valueOf(13);

  @Column(name = "labor_budget", nullable = false, precision = 14, scale = 2)
  private BigDecimal laborBudget = BigDecimal.ZERO;

  @Column(name = "material_budget", nullable = false, precision = 14, scale = 2)
  private BigDecimal materialBudget = BigDecimal.ZERO;

  @Column(name = "subcontract_budget", nullable = false, precision = 14, scale = 2)
  private BigDecimal subcontractBudget = BigDecimal.ZERO;

  @Column(name = "travel_budget", nullable = false, precision = 14, scale = 2)
  private BigDecimal travelBudget = BigDecimal.ZERO;

  @Column(name = "other_budget", nullable = false, precision = 14, scale = 2)
  private BigDecimal otherBudget = BigDecimal.ZERO;

  @Column(name = "version_no", nullable = false)
  private int versionNo = 1;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private QuoteStatus status = QuoteStatus.DRAFT;

  @Enumerated(EnumType.STRING)
  @Column(name = "customer_decision", length = 32)
  private QuoteCustomerDecision customerDecision;

  @Column(name = "customer_comment", length = 500)
  private String customerComment;

  @Column(name = "customer_decision_by", length = 80)
  private String customerDecisionBy;

  @Column(name = "customer_decided_at")
  private OffsetDateTime customerDecidedAt;

  public UUID getCustomerId() {
    return customerId;
  }

  public void setCustomerId(UUID customerId) {
    this.customerId = customerId;
  }

  public UUID getOpportunityId() {
    return opportunityId;
  }

  public void setOpportunityId(UUID opportunityId) {
    this.opportunityId = opportunityId;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getServiceScope() {
    return serviceScope;
  }

  public void setServiceScope(String serviceScope) {
    this.serviceScope = serviceScope;
  }

  public String getInspectCycle() {
    return inspectCycle;
  }

  public void setInspectCycle(String inspectCycle) {
    this.inspectCycle = inspectCycle;
  }

  public String getPaymentNodes() {
    return paymentNodes;
  }

  public void setPaymentNodes(String paymentNodes) {
    this.paymentNodes = paymentNodes;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public BigDecimal getTaxRate() {
    return taxRate;
  }

  public void setTaxRate(BigDecimal taxRate) {
    this.taxRate = taxRate;
  }

  public BigDecimal getLaborBudget() {
    return laborBudget;
  }

  public void setLaborBudget(BigDecimal laborBudget) {
    this.laborBudget = laborBudget;
  }

  public BigDecimal getMaterialBudget() {
    return materialBudget;
  }

  public void setMaterialBudget(BigDecimal materialBudget) {
    this.materialBudget = materialBudget;
  }

  public BigDecimal getSubcontractBudget() {
    return subcontractBudget;
  }

  public void setSubcontractBudget(BigDecimal subcontractBudget) {
    this.subcontractBudget = subcontractBudget;
  }

  public BigDecimal getTravelBudget() {
    return travelBudget;
  }

  public void setTravelBudget(BigDecimal travelBudget) {
    this.travelBudget = travelBudget;
  }

  public BigDecimal getOtherBudget() {
    return otherBudget;
  }

  public void setOtherBudget(BigDecimal otherBudget) {
    this.otherBudget = otherBudget;
  }

  public int getVersionNo() {
    return versionNo;
  }

  public void setVersionNo(int versionNo) {
    this.versionNo = versionNo;
  }

  public QuoteStatus getStatus() {
    return status;
  }

  public void setStatus(QuoteStatus status) {
    this.status = status;
  }

  public QuoteCustomerDecision getCustomerDecision() {
    return customerDecision;
  }

  public void setCustomerDecision(QuoteCustomerDecision customerDecision) {
    this.customerDecision = customerDecision;
  }

  public String getCustomerComment() {
    return customerComment;
  }

  public void setCustomerComment(String customerComment) {
    this.customerComment = customerComment;
  }

  public String getCustomerDecisionBy() {
    return customerDecisionBy;
  }

  public void setCustomerDecisionBy(String customerDecisionBy) {
    this.customerDecisionBy = customerDecisionBy;
  }

  public OffsetDateTime getCustomerDecidedAt() {
    return customerDecidedAt;
  }

  public void setCustomerDecidedAt(OffsetDateTime customerDecidedAt) {
    this.customerDecidedAt = customerDecidedAt;
  }
}
