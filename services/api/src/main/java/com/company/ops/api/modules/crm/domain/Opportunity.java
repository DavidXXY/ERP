package com.company.ops.api.modules.crm.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "crm_opportunities")
public class Opportunity extends BaseEntity {

  @Column(name = "customer_id")
  private UUID customerId;

  @Column(nullable = false, length = 64)
  private String code;

  @Column(length = 80)
  private String source;

  @Column(name = "need_summary", nullable = false, length = 500)
  private String needSummary;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private OpportunityStage stage = OpportunityStage.LEAD;

  @Column(name = "expected_amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal expectedAmount = BigDecimal.ZERO;

  @Column(nullable = false)
  private int probability;

  @Column(name = "next_action", length = 240)
  private String nextAction;

  @Column(name = "next_action_at")
  private LocalDate nextActionAt;

  @Column(name = "owner_name", length = 80)
  private String ownerName;

  public UUID getCustomerId() {
    return customerId;
  }

  public void setCustomerId(UUID customerId) {
    this.customerId = customerId;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getNeedSummary() {
    return needSummary;
  }

  public void setNeedSummary(String needSummary) {
    this.needSummary = needSummary;
  }

  public OpportunityStage getStage() {
    return stage;
  }

  public void setStage(OpportunityStage stage) {
    this.stage = stage;
  }

  public BigDecimal getExpectedAmount() {
    return expectedAmount;
  }

  public void setExpectedAmount(BigDecimal expectedAmount) {
    this.expectedAmount = expectedAmount;
  }

  public int getProbability() {
    return probability;
  }

  public void setProbability(int probability) {
    this.probability = probability;
  }

  public String getNextAction() {
    return nextAction;
  }

  public void setNextAction(String nextAction) {
    this.nextAction = nextAction;
  }

  public LocalDate getNextActionAt() {
    return nextActionAt;
  }

  public void setNextActionAt(LocalDate nextActionAt) {
    this.nextActionAt = nextActionAt;
  }

  public String getOwnerName() {
    return ownerName;
  }

  public void setOwnerName(String ownerName) {
    this.ownerName = ownerName;
  }
}
