package com.company.ops.api.modules.collaboration.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name="biz_project_timesheets")
public class ProjectTimesheet extends BaseEntity {
  @Column(name="assignment_id",nullable=false) private UUID assignmentId;
  @Column(name="project_id",nullable=false) private UUID projectId;
  @Column(name="user_id",nullable=false) private UUID userId;
  @Column(name="work_date",nullable=false) private LocalDate workDate;
  @Column(nullable=false,precision=8,scale=2) private BigDecimal hours;
  @Column(length=500) private String description;
  @Column(nullable=false,length=32) private String status="SUBMITTED";
  @Column(name="submitted_by") private UUID submittedBy;
  @Column(name="reviewed_by") private UUID reviewedBy;
  @Column(name="reviewed_by_name",length=80) private String reviewedByName;
  @Column(name="review_comment",length=500) private String reviewComment;
  @Column(name="reviewed_at") private OffsetDateTime reviewedAt;
  public UUID getAssignmentId(){return assignmentId;} public void setAssignmentId(UUID v){assignmentId=v;}
  public UUID getProjectId(){return projectId;} public void setProjectId(UUID v){projectId=v;}
  public UUID getUserId(){return userId;} public void setUserId(UUID v){userId=v;}
  public LocalDate getWorkDate(){return workDate;} public void setWorkDate(LocalDate v){workDate=v;}
  public BigDecimal getHours(){return hours;} public void setHours(BigDecimal v){hours=v;}
  public String getDescription(){return description;} public void setDescription(String v){description=v;}
  public String getStatus(){return status;} public void setStatus(String v){status=v;}
  public UUID getSubmittedBy(){return submittedBy;} public void setSubmittedBy(UUID v){submittedBy=v;}
  public UUID getReviewedBy(){return reviewedBy;} public void setReviewedBy(UUID v){reviewedBy=v;}
  public String getReviewedByName(){return reviewedByName;} public void setReviewedByName(String v){reviewedByName=v;}
  public String getReviewComment(){return reviewComment;} public void setReviewComment(String v){reviewComment=v;}
  public OffsetDateTime getReviewedAt(){return reviewedAt;} public void setReviewedAt(OffsetDateTime v){reviewedAt=v;}
}
