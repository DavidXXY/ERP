package com.company.ops.api.modules.risk.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "risk_workflows")
public class RiskWorkflow extends BaseEntity {
  @Column(name = "risk_key", nullable = false, unique = true, length = 180)
  private String riskKey;
  @Column(nullable = false, length = 32)
  private String status = "UNCLAIMED";
  @Column(length = 80)
  private String owner;
  @Column(length = 1000)
  private String note;
  @Column(length = 500)
  private String reason;
  @Column(name = "updated_by_name", length = 80)
  private String updatedByName;
  @Column(name = "processed_at")
  private OffsetDateTime processedAt;

  public String getRiskKey() { return riskKey; }
  public void setRiskKey(String riskKey) { this.riskKey = riskKey; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getOwner() { return owner; }
  public void setOwner(String owner) { this.owner = owner; }
  public String getNote() { return note; }
  public void setNote(String note) { this.note = note; }
  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }
  public String getUpdatedByName() { return updatedByName; }
  public void setUpdatedByName(String updatedByName) { this.updatedByName = updatedByName; }
  public OffsetDateTime getProcessedAt() { return processedAt; }
  public void setProcessedAt(OffsetDateTime processedAt) { this.processedAt = processedAt; }
}
