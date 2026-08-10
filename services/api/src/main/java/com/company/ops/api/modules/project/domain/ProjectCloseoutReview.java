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
@Table(name = "project_closeout_reviews")
public class ProjectCloseoutReview extends BaseEntity {

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private CloseoutReviewStatus status = CloseoutReviewStatus.PENDING;

  @Column(name = "request_comment", length = 500)
  private String requestComment;

  @Column(name = "review_comment", length = 500)
  private String reviewComment;

  @Column(name = "requested_by", length = 80)
  private String requestedBy;

  @Column(name = "requested_at")
  private OffsetDateTime requestedAt;

  @Column(name = "reviewed_by", length = 80)
  private String reviewedBy;

  @Column(name = "reviewed_at")
  private OffsetDateTime reviewedAt;

  public UUID getProjectId() { return projectId; }
  public void setProjectId(UUID projectId) { this.projectId = projectId; }
  public CloseoutReviewStatus getStatus() { return status; }
  public void setStatus(CloseoutReviewStatus status) { this.status = status; }
  public String getRequestComment() { return requestComment; }
  public void setRequestComment(String requestComment) { this.requestComment = requestComment; }
  public String getReviewComment() { return reviewComment; }
  public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }
  public String getRequestedBy() { return requestedBy; }
  public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }
  public OffsetDateTime getRequestedAt() { return requestedAt; }
  public void setRequestedAt(OffsetDateTime requestedAt) { this.requestedAt = requestedAt; }
  public String getReviewedBy() { return reviewedBy; }
  public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
  public OffsetDateTime getReviewedAt() { return reviewedAt; }
  public void setReviewedAt(OffsetDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}
