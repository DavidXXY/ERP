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
  @Column(name = "department_name", length = 120) private String departmentName;
  @Column(name = "business_type", length = 80) private String businessType;
  @Column(name = "project_code", length = 80) private String projectCode;
  @Column(name = "supplier_risk", length = 40) private String supplierRisk;
  @Column(name = "customer_level", length = 40) private String customerLevel;
  @Column(name = "approval_mode", length = 20) private String approvalMode;
  @Column(name = "current_step") private Integer currentStep;
  @Column(name = "total_steps") private Integer totalSteps;
  @Column(name = "current_approver_name", length = 120) private String currentApproverName;
  @Column(name = "delegated_user_id") private java.util.UUID delegatedUserId;
  @Column(name = "matched_rule_text", length = 500) private String matchedRuleText;
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
  public String getDepartmentName() { return departmentName; } public void setDepartmentName(String v) { departmentName = v; }
  public String getBusinessType() { return businessType; } public void setBusinessType(String v) { businessType = v; }
  public String getProjectCode() { return projectCode; } public void setProjectCode(String v) { projectCode = v; }
  public String getSupplierRisk() { return supplierRisk; } public void setSupplierRisk(String v) { supplierRisk = v; }
  public String getCustomerLevel() { return customerLevel; } public void setCustomerLevel(String v) { customerLevel = v; }
  public String getApprovalMode() { return approvalMode; } public void setApprovalMode(String v) { approvalMode = v; }
  public Integer getCurrentStep() { return currentStep; } public void setCurrentStep(Integer v) { currentStep = v; }
  public Integer getTotalSteps() { return totalSteps; } public void setTotalSteps(Integer v) { totalSteps = v; }
  public String getCurrentApproverName() { return currentApproverName; } public void setCurrentApproverName(String v) { currentApproverName = v; }
  public java.util.UUID getDelegatedUserId() { return delegatedUserId; } public void setDelegatedUserId(java.util.UUID v) { delegatedUserId = v; }
  public String getMatchedRuleText() { return matchedRuleText; } public void setMatchedRuleText(String v) { matchedRuleText = v; }
}
