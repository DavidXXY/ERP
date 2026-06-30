package com.company.ops.api.modules.crm.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "crm_follow_ups")
public class FollowUp extends BaseEntity {

  @Column(name = "customer_id", nullable = false)
  private UUID customerId;

  @Column(name = "opportunity_id")
  private UUID opportunityId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private FollowUpType type;

  @Column(nullable = false, length = 160)
  private String subject;

  @Column(nullable = false, length = 1200)
  private String content;

  @Column(name = "followed_at", nullable = false)
  private OffsetDateTime followedAt;

  @Column(name = "next_action", length = 240)
  private String nextAction;

  @Column(name = "owner_name", nullable = false, length = 80)
  private String ownerName;

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

  public FollowUpType getType() {
    return type;
  }

  public void setType(FollowUpType type) {
    this.type = type;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public OffsetDateTime getFollowedAt() {
    return followedAt;
  }

  public void setFollowedAt(OffsetDateTime followedAt) {
    this.followedAt = followedAt;
  }

  public String getNextAction() {
    return nextAction;
  }

  public void setNextAction(String nextAction) {
    this.nextAction = nextAction;
  }

  public String getOwnerName() {
    return ownerName;
  }

  public void setOwnerName(String ownerName) {
    this.ownerName = ownerName;
  }
}
