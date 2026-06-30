package com.company.ops.api.modules.office.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity @Table(name = "oa_approval_requests")
public class ApprovalRequest extends BaseEntity {
  @Column(nullable = false, length = 64) private String code;
  @Enumerated(EnumType.STRING) @Column(name = "approval_type", nullable = false, length = 80) private ApprovalType approvalType;
  @Column(nullable = false, length = 180) private String title;
  @Column(name = "source_no", length = 64) private String sourceNo;
  @Column(precision = 14, scale = 2) private BigDecimal amount;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private ApprovalStatus status = ApprovalStatus.PENDING;
  @Column(name = "applicant_name", length = 80) private String applicantName;
  @Column(length = 1000) private String content;
  @Column(name = "approver_name", length = 80) private String approverName;
  @Column(name = "approval_comment", length = 500) private String approvalComment;
  @Column(name = "processed_at") private OffsetDateTime processedAt;
  public String getCode() { return code; } public void setCode(String v) { code = v; }
  public ApprovalType getApprovalType() { return approvalType; } public void setApprovalType(ApprovalType v) { approvalType = v; }
  public String getTitle() { return title; } public void setTitle(String v) { title = v; }
  public String getSourceNo() { return sourceNo; } public void setSourceNo(String v) { sourceNo = v; }
  public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal v) { amount = v; }
  public ApprovalStatus getStatus() { return status; } public void setStatus(ApprovalStatus v) { status = v; }
  public String getApplicantName() { return applicantName; } public void setApplicantName(String v) { applicantName = v; }
  public String getContent() { return content; } public void setContent(String v) { content = v; }
  public String getApproverName() { return approverName; } public void setApproverName(String v) { approverName = v; }
  public String getApprovalComment() { return approvalComment; } public void setApprovalComment(String v) { approvalComment = v; }
  public OffsetDateTime getProcessedAt() { return processedAt; } public void setProcessedAt(OffsetDateTime v) { processedAt = v; }
}
