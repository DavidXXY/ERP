package com.company.ops.api.modules.crm.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private QuoteStatus status = QuoteStatus.DRAFT;

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

  public QuoteStatus getStatus() {
    return status;
  }

  public void setStatus(QuoteStatus status) {
    this.status = status;
  }
}
