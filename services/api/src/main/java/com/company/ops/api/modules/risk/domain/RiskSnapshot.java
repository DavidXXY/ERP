package com.company.ops.api.modules.risk.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "risk_snapshots")
public class RiskSnapshot extends BaseEntity {
  @Column(name = "snapshot_date", nullable = false)
  private LocalDate snapshotDate;
  @Column(name = "risk_key", nullable = false, length = 180)
  private String riskKey;
  @Column(nullable = false, length = 40)
  private String module;
  @Column(name = "module_name", nullable = false, length = 80)
  private String moduleName;
  @Column(nullable = false, length = 180)
  private String title;
  @Column(nullable = false, length = 300)
  private String subject;
  @Column(nullable = false, length = 20)
  private String severity;
  @Column(nullable = false, length = 32)
  private String status;
  @Column(name = "workflow_status", nullable = false, length = 32)
  private String workflowStatus;
  @Column(precision = 18, scale = 2)
  private BigDecimal amount;

  public LocalDate getSnapshotDate() { return snapshotDate; }
  public void setSnapshotDate(LocalDate snapshotDate) { this.snapshotDate = snapshotDate; }
  public String getRiskKey() { return riskKey; }
  public void setRiskKey(String riskKey) { this.riskKey = riskKey; }
  public String getModule() { return module; }
  public void setModule(String module) { this.module = module; }
  public String getModuleName() { return moduleName; }
  public void setModuleName(String moduleName) { this.moduleName = moduleName; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getSubject() { return subject; }
  public void setSubject(String subject) { this.subject = subject; }
  public String getSeverity() { return severity; }
  public void setSeverity(String severity) { this.severity = severity; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getWorkflowStatus() { return workflowStatus; }
  public void setWorkflowStatus(String workflowStatus) { this.workflowStatus = workflowStatus; }
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }
}
