package com.company.ops.api.modules.project.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

/** 项目里程碑/任务（WBS），作为阶段推进之外的交付节点管理。 */
@Entity
@Table(name = "project_milestones")
public class ProjectMilestone extends BaseEntity {

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Column(nullable = false, length = 180)
  private String name;

  @Column(name = "planned_date")
  private LocalDate plannedDate;

  @Column(name = "actual_date")
  private LocalDate actualDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private MilestoneStatus status = MilestoneStatus.PENDING;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  @Column(length = 500)
  private String remark;

  public UUID getProjectId() { return projectId; }
  public void setProjectId(UUID projectId) { this.projectId = projectId; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public LocalDate getPlannedDate() { return plannedDate; }
  public void setPlannedDate(LocalDate plannedDate) { this.plannedDate = plannedDate; }
  public LocalDate getActualDate() { return actualDate; }
  public void setActualDate(LocalDate actualDate) { this.actualDate = actualDate; }
  public MilestoneStatus getStatus() { return status; }
  public void setStatus(MilestoneStatus status) { this.status = status; }
  public int getSortOrder() { return sortOrder; }
  public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
}
