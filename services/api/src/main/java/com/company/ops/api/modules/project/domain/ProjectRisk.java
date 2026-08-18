package com.company.ops.api.modules.project.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

/** 项目风险台账条目，配合利润复盘派生的风险指标形成完整风险跟踪。 */
@Entity
@Table(name = "project_risks")
public class ProjectRisk extends BaseEntity {

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Column(nullable = false, length = 180)
  private String title;

  @Column(length = 1000)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private RiskSeverity severity = RiskSeverity.MEDIUM;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private RiskStatus status = RiskStatus.OPEN;

  @Column(name = "owner_name", length = 80)
  private String ownerName;

  @Column(name = "due_date")
  private LocalDate dueDate;

  @Column(length = 1000)
  private String resolution;

  public UUID getProjectId() { return projectId; }
  public void setProjectId(UUID projectId) { this.projectId = projectId; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public RiskSeverity getSeverity() { return severity; }
  public void setSeverity(RiskSeverity severity) { this.severity = severity; }
  public RiskStatus getStatus() { return status; }
  public void setStatus(RiskStatus status) { this.status = status; }
  public String getOwnerName() { return ownerName; }
  public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
  public LocalDate getDueDate() { return dueDate; }
  public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
  public String getResolution() { return resolution; }
  public void setResolution(String resolution) { this.resolution = resolution; }
}
