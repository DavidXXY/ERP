package com.company.ops.api.modules.reporting.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "emp_reports")
public class EmployeeReport extends BaseEntity {
  @Column(name = "report_type", nullable = false, length = 16) private String reportType;
  @Column(name = "report_category", nullable = false, length = 24) private String reportCategory = "DAILY";
  @Column(name = "project_id") private UUID projectId;
  @Column(name = "additional_project_id") private UUID additionalProjectId;
  @Column(name = "hours", precision = 5, scale = 2) private BigDecimal hours;
  @Column(name = "report_status", nullable = false, length = 16) private String reportStatus = "PENDING_CONFIRM";
  @Column(name = "approval_id") private UUID approvalId;
  @Column(name = "report_date", nullable = false) private LocalDate reportDate;
  @Column(nullable = false, columnDefinition = "text") private String content;
  @Column(name = "progress_summary", length = 1000) private String progressSummary;
  @Column(name = "plan_next", length = 2000) private String planNext;
  @Column(length = 1000) private String issue;
  @Column(name = "employee_id") private UUID employeeId;
  @Column(name = "employee_name", nullable = false, length = 80) private String employeeName;
  @Column(name = "department_name", length = 120) private String departmentName;
  @Column(name = "cc_user_ids", nullable = false, columnDefinition = "text") private String ccUserIds = "[]";
  @Column(name = "cc_names", length = 500) private String ccNames;

  public String getReportType() { return reportType; } public void setReportType(String v) { reportType = v; }
  public String getReportCategory() { return reportCategory; } public void setReportCategory(String v) { reportCategory = v; }
  public UUID getProjectId() { return projectId; } public void setProjectId(UUID v) { projectId = v; }
  public UUID getAdditionalProjectId() { return additionalProjectId; } public void setAdditionalProjectId(UUID v) { additionalProjectId = v; }
  public BigDecimal getHours() { return hours; } public void setHours(BigDecimal v) { hours = v; }
  public String getReportStatus() { return reportStatus; } public void setReportStatus(String v) { reportStatus = v; }
  public UUID getApprovalId() { return approvalId; } public void setApprovalId(UUID v) { approvalId = v; }
  public LocalDate getReportDate() { return reportDate; } public void setReportDate(LocalDate v) { reportDate = v; }
  public String getContent() { return content; } public void setContent(String v) { content = v; }
  public String getProgressSummary() { return progressSummary; } public void setProgressSummary(String v) { progressSummary = v; }
  public String getPlanNext() { return planNext; } public void setPlanNext(String v) { planNext = v; }
  public String getIssue() { return issue; } public void setIssue(String v) { issue = v; }
  public UUID getEmployeeId() { return employeeId; } public void setEmployeeId(UUID v) { employeeId = v; }
  public String getEmployeeName() { return employeeName; } public void setEmployeeName(String v) { employeeName = v; }
  public String getDepartmentName() { return departmentName; } public void setDepartmentName(String v) { departmentName = v; }
  public String getCcUserIds() { return ccUserIds; } public void setCcUserIds(String v) { ccUserIds = v; }
  public String getCcNames() { return ccNames; } public void setCcNames(String v) { ccNames = v; }
}
