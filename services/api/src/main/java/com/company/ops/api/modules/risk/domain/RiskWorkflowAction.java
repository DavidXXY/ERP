package com.company.ops.api.modules.risk.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "risk_workflow_actions")
public class RiskWorkflowAction extends BaseEntity {
  @Column(name = "risk_key", nullable = false, length = 180)
  private String riskKey;
  @Column(name = "from_status", length = 32)
  private String fromStatus;
  @Column(name = "to_status", nullable = false, length = 32)
  private String toStatus;
  @Column(length = 80)
  private String operatorName;
  @Column(length = 80)
  private String owner;
  @Column(length = 1000)
  private String note;
  @Column(length = 500)
  private String reason;

  public String getRiskKey() { return riskKey; }
  public void setRiskKey(String riskKey) { this.riskKey = riskKey; }
  public String getFromStatus() { return fromStatus; }
  public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
  public String getToStatus() { return toStatus; }
  public void setToStatus(String toStatus) { this.toStatus = toStatus; }
  public String getOperatorName() { return operatorName; }
  public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
  public String getOwner() { return owner; }
  public void setOwner(String owner) { this.owner = owner; }
  public String getNote() { return note; }
  public void setNote(String note) { this.note = note; }
  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }
}
