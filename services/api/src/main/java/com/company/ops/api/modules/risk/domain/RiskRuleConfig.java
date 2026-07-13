package com.company.ops.api.modules.risk.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "risk_rule_configs")
public class RiskRuleConfig extends BaseEntity {
  @Column(name = "rule_code", nullable = false, unique = true, length = 80)
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
  @Column(name = "escalation_owner", length = 80)
  private String escalationOwner;
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
  public String getEscalationOwner() { return escalationOwner; }
  public void setEscalationOwner(String escalationOwner) { this.escalationOwner = escalationOwner; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
}
