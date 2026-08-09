package com.company.ops.api.modules.procurement.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "procurement_approval_rules")
public class ProcurementApprovalRule extends BaseEntity {

  @Column(name = "rule_name", nullable = false, length = 80)
  private String ruleName;

  @Column(name = "min_amount", precision = 14, scale = 2)
  private BigDecimal minAmount;

  @Column(name = "max_amount", precision = 14, scale = 2)
  private BigDecimal maxAmount;

  @Column(name = "approval_level", nullable = false, length = 24)
  private String approvalLevel;

  @Column(name = "required_role_code", length = 64)
  private String requiredRoleCode;

  @Column(nullable = false)
  private boolean enabled = true;

  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder = 0;

  public String getRuleName() { return ruleName; }
  public void setRuleName(String v) { ruleName = v; }
  public BigDecimal getMinAmount() { return minAmount; }
  public void setMinAmount(BigDecimal v) { minAmount = v; }
  public BigDecimal getMaxAmount() { return maxAmount; }
  public void setMaxAmount(BigDecimal v) { maxAmount = v; }
  public String getApprovalLevel() { return approvalLevel; }
  public void setApprovalLevel(String v) { approvalLevel = v; }
  public String getRequiredRoleCode() { return requiredRoleCode; }
  public void setRequiredRoleCode(String v) { requiredRoleCode = v; }
  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean v) { enabled = v; }
  public Integer getSortOrder() { return sortOrder; }
  public void setSortOrder(Integer v) { sortOrder = v; }
}
