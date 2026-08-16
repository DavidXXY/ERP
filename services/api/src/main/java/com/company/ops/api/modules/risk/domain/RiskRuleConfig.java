package com.company.ops.api.modules.risk.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "risk_rule_configs", uniqueConstraints = @UniqueConstraint(
    name = "uk_risk_rule_tenant_code", columnNames = {"tenant_id", "rule_code"}))
public class RiskRuleConfig extends BaseEntity {
  @Column(name = "rule_code", nullable = false, length = 80)
  private String ruleCode;
  @Column(nullable = false, length = 120)
  private String name;
  @Column(nullable = false, length = 40)
  private String module;
  @Column(nullable = false)
  private boolean enabled = true;
  @Column(name = "high_threshold", precision = 18, scale = 4)
  private BigDecimal highThreshold;
  @Column(name = "medium_threshold", precision = 18, scale = 4)
  private BigDecimal mediumThreshold;
  @Column(name = "warning_days")
  private Integer warningDays;
  @Column(name = "sla_hours")
  private Integer slaHours;
  @Column(name = "default_owner", length = 80)
  private String defaultOwner;
  @Column(name = "default_owner_type", length = 20)
  private String defaultOwnerType;
  @Column(name = "default_owner_user_id")
  private UUID defaultOwnerUserId;
  @Column(name = "default_owner_role_id")
  private UUID defaultOwnerRoleId;
  @Column(name = "default_owner_position", length = 120)
  private String defaultOwnerPosition;
  @Column(name = "default_owner_dynamic", length = 40)
  private String defaultOwnerDynamic;
  @Column(name = "escalation_owner", length = 80)
  private String escalationOwner;
  @Column(name = "escalation_owner_type", length = 20)
  private String escalationOwnerType;
  @Column(name = "escalation_owner_user_id")
  private UUID escalationOwnerUserId;
  @Column(name = "escalation_owner_role_id")
  private UUID escalationOwnerRoleId;
  @Column(name = "escalation_owner_position", length = 120)
  private String escalationOwnerPosition;
  @Column(name = "escalation_owner_dynamic", length = 40)
  private String escalationOwnerDynamic;
  @Column(length = 500)
  private String remark;

  public String getRuleCode() { return ruleCode; }
  public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getModule() { return module; }
  public void setModule(String module) { this.module = module; }
  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean enabled) { this.enabled = enabled; }
  public BigDecimal getHighThreshold() { return highThreshold; }
  public void setHighThreshold(BigDecimal highThreshold) { this.highThreshold = highThreshold; }
  public BigDecimal getMediumThreshold() { return mediumThreshold; }
  public void setMediumThreshold(BigDecimal mediumThreshold) { this.mediumThreshold = mediumThreshold; }
  public Integer getWarningDays() { return warningDays; }
  public void setWarningDays(Integer warningDays) { this.warningDays = warningDays; }
  public Integer getSlaHours() { return slaHours; }
  public void setSlaHours(Integer slaHours) { this.slaHours = slaHours; }
  public String getDefaultOwner() { return defaultOwner; }
  public void setDefaultOwner(String defaultOwner) { this.defaultOwner = defaultOwner; }
  public String getDefaultOwnerType() { return defaultOwnerType; }
  public void setDefaultOwnerType(String defaultOwnerType) { this.defaultOwnerType = defaultOwnerType; }
  public UUID getDefaultOwnerUserId() { return defaultOwnerUserId; }
  public void setDefaultOwnerUserId(UUID defaultOwnerUserId) { this.defaultOwnerUserId = defaultOwnerUserId; }
  public UUID getDefaultOwnerRoleId() { return defaultOwnerRoleId; }
  public void setDefaultOwnerRoleId(UUID defaultOwnerRoleId) { this.defaultOwnerRoleId = defaultOwnerRoleId; }
  public String getDefaultOwnerPosition() { return defaultOwnerPosition; }
  public void setDefaultOwnerPosition(String defaultOwnerPosition) { this.defaultOwnerPosition = defaultOwnerPosition; }
  public String getDefaultOwnerDynamic() { return defaultOwnerDynamic; }
  public void setDefaultOwnerDynamic(String defaultOwnerDynamic) { this.defaultOwnerDynamic = defaultOwnerDynamic; }
  public String getEscalationOwner() { return escalationOwner; }
  public void setEscalationOwner(String escalationOwner) { this.escalationOwner = escalationOwner; }
  public String getEscalationOwnerType() { return escalationOwnerType; }
  public void setEscalationOwnerType(String escalationOwnerType) { this.escalationOwnerType = escalationOwnerType; }
  public UUID getEscalationOwnerUserId() { return escalationOwnerUserId; }
  public void setEscalationOwnerUserId(UUID escalationOwnerUserId) { this.escalationOwnerUserId = escalationOwnerUserId; }
  public UUID getEscalationOwnerRoleId() { return escalationOwnerRoleId; }
  public void setEscalationOwnerRoleId(UUID escalationOwnerRoleId) { this.escalationOwnerRoleId = escalationOwnerRoleId; }
  public String getEscalationOwnerPosition() { return escalationOwnerPosition; }
  public void setEscalationOwnerPosition(String escalationOwnerPosition) { this.escalationOwnerPosition = escalationOwnerPosition; }
  public String getEscalationOwnerDynamic() { return escalationOwnerDynamic; }
  public void setEscalationOwnerDynamic(String escalationOwnerDynamic) { this.escalationOwnerDynamic = escalationOwnerDynamic; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
}
