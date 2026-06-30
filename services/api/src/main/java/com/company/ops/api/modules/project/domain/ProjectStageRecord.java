package com.company.ops.api.modules.project.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_stage_records")
public class ProjectStageRecord extends BaseEntity {

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Enumerated(EnumType.STRING)
  @Column(name = "from_stage", nullable = false, length = 40)
  private ProjectStage fromStage;

  @Enumerated(EnumType.STRING)
  @Column(name = "to_stage", nullable = false, length = 40)
  private ProjectStage toStage;

  @Column(nullable = false)
  private int progress;

  @Column(nullable = false, length = 500)
  private String comment;

  @Column(name = "operator_name", nullable = false, length = 80)
  private String operatorName;

  @Column(name = "changed_at", nullable = false)
  private OffsetDateTime changedAt;

  public UUID getProjectId() {
    return projectId;
  }

  public void setProjectId(UUID projectId) {
    this.projectId = projectId;
  }

  public ProjectStage getFromStage() {
    return fromStage;
  }

  public void setFromStage(ProjectStage fromStage) {
    this.fromStage = fromStage;
  }

  public ProjectStage getToStage() {
    return toStage;
  }

  public void setToStage(ProjectStage toStage) {
    this.toStage = toStage;
  }

  public int getProgress() {
    return progress;
  }

  public void setProgress(int progress) {
    this.progress = progress;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public String getOperatorName() {
    return operatorName;
  }

  public void setOperatorName(String operatorName) {
    this.operatorName = operatorName;
  }

  public OffsetDateTime getChangedAt() {
    return changedAt;
  }

  public void setChangedAt(OffsetDateTime changedAt) {
    this.changedAt = changedAt;
  }
}
