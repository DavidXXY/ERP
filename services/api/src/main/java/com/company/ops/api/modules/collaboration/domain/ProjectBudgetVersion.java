package com.company.ops.api.modules.collaboration.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name="biz_project_budget_versions")
public class ProjectBudgetVersion extends BaseEntity {
  @Column(name="project_id",nullable=false) private UUID projectId;
  @Column(name="version_no",nullable=false) private int versionNo;
  @Column(name="previous_amount",nullable=false,precision=14,scale=2) private BigDecimal previousAmount;
  @Column(name="requested_amount",nullable=false,precision=14,scale=2) private BigDecimal requestedAmount;
  @Column(nullable=false,length=32) private String status="PENDING";
  @Column(nullable=false,length=1000) private String reason;
  @Column(name="requested_by") private UUID requestedBy;
  @Column(name="requested_by_name",nullable=false,length=80) private String requestedByName;
  @Column(name="reviewed_by") private UUID reviewedBy;
  @Column(name="reviewed_by_name",length=80) private String reviewedByName;
  @Column(name="review_comment",length=500) private String reviewComment;
  @Column(name="reviewed_at") private OffsetDateTime reviewedAt;
  @Column(name="effective_at") private OffsetDateTime effectiveAt;
  public UUID getProjectId(){return projectId;} public void setProjectId(UUID v){projectId=v;}
  public int getVersionNo(){return versionNo;} public void setVersionNo(int v){versionNo=v;}
  public BigDecimal getPreviousAmount(){return previousAmount;} public void setPreviousAmount(BigDecimal v){previousAmount=v;}
  public BigDecimal getRequestedAmount(){return requestedAmount;} public void setRequestedAmount(BigDecimal v){requestedAmount=v;}
  public String getStatus(){return status;} public void setStatus(String v){status=v;}
  public String getReason(){return reason;} public void setReason(String v){reason=v;}
  public UUID getRequestedBy(){return requestedBy;} public void setRequestedBy(UUID v){requestedBy=v;}
  public String getRequestedByName(){return requestedByName;} public void setRequestedByName(String v){requestedByName=v;}
  public UUID getReviewedBy(){return reviewedBy;} public void setReviewedBy(UUID v){reviewedBy=v;}
  public String getReviewedByName(){return reviewedByName;} public void setReviewedByName(String v){reviewedByName=v;}
  public String getReviewComment(){return reviewComment;} public void setReviewComment(String v){reviewComment=v;}
  public OffsetDateTime getReviewedAt(){return reviewedAt;} public void setReviewedAt(OffsetDateTime v){reviewedAt=v;}
  public OffsetDateTime getEffectiveAt(){return effectiveAt;} public void setEffectiveAt(OffsetDateTime v){effectiveAt=v;}
}
