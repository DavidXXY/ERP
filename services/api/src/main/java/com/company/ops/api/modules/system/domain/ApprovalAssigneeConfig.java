package com.company.ops.api.modules.system.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "approval_assignee_configs")
public class ApprovalAssigneeConfig extends BaseEntity {
  @Column(name = "flow_code", nullable = false, length = 64)
  private String flowCode;
  @Column(name = "flow_name", nullable = false, length = 120)
  private String flowName;
  @Column(name = "assignee_type", nullable = false, length = 20)
  private String assigneeType = "USER";
  @Column(name = "user_id")
  private UUID userId;
  @Column(name = "role_id")
  private UUID roleId;
  @Column(name = "approval_mode", nullable = false, length = 20)
  private String approvalMode = "PARALLEL";
  @Column(name = "sequence_no", nullable = false)
  private int sequenceNo = 1;
  @Column(name = "condition_type", nullable = false, length = 32)
  private String conditionType = "ANY";
  @Column(name = "min_amount", precision = 18, scale = 2)
  private BigDecimal minAmount;
  @Column(name = "max_amount", precision = 18, scale = 2)
  private BigDecimal maxAmount;
  @Column(name = "department_name", length = 120)
  private String departmentName;
  @Column(name = "business_type", length = 80)
  private String businessType;
  @Column(name = "project_code", length = 80)
  private String projectCode;
  @Column(name = "supplier_risk", length = 40)
  private String supplierRisk;
  @Column(name = "customer_level", length = 40)
  private String customerLevel;
  @Column(name = "priority", nullable = false)
  private int priority = 100;
  @Column(name = "remark", length = 500)
  private String remark;
  @Column(nullable = false)
  private boolean enabled = true;

  public String getFlowCode() { return flowCode; }
  public void setFlowCode(String value) { flowCode = value; }
  public String getFlowName() { return flowName; }
  public void setFlowName(String value) { flowName = value; }
  public String getAssigneeType() { return assigneeType; }
  public void setAssigneeType(String value) { assigneeType = value; }
  public UUID getUserId() { return userId; }
  public void setUserId(UUID value) { userId = value; }
  public UUID getRoleId() { return roleId; }
  public void setRoleId(UUID value) { roleId = value; }
  public String getApprovalMode() { return approvalMode; }
  public void setApprovalMode(String value) { approvalMode = value; }
  public int getSequenceNo() { return sequenceNo; }
  public void setSequenceNo(int value) { sequenceNo = value; }
  public String getConditionType() { return conditionType; }
  public void setConditionType(String value) { conditionType = value; }
  public BigDecimal getMinAmount() { return minAmount; }
  public void setMinAmount(BigDecimal value) { minAmount = value; }
  public BigDecimal getMaxAmount() { return maxAmount; }
  public void setMaxAmount(BigDecimal value) { maxAmount = value; }
  public String getDepartmentName() { return departmentName; }
  public void setDepartmentName(String value) { departmentName = value; }
  public String getBusinessType() { return businessType; }
  public void setBusinessType(String value) { businessType = value; }
  public String getProjectCode() { return projectCode; }
  public void setProjectCode(String value) { projectCode = value; }
  public String getSupplierRisk() { return supplierRisk; }
  public void setSupplierRisk(String value) { supplierRisk = value; }
  public String getCustomerLevel() { return customerLevel; }
  public void setCustomerLevel(String value) { customerLevel = value; }
  public int getPriority() { return priority; }
  public void setPriority(int value) { priority = value; }
  public String getRemark() { return remark; }
  public void setRemark(String value) { remark = value; }
  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean value) { enabled = value; }
}
