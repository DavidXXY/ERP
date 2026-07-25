package com.company.ops.api.modules.office.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "oa_travel_applications")
public class TravelApplication extends BaseEntity {
  @Column(nullable = false, length = 64) private String code;
  @Column(name = "applicant_id", nullable = false) private UUID applicantId;
  @Column(name = "applicant_name", nullable = false, length = 80) private String applicantName;
  @Column(name = "department_name", nullable = false, length = 120) private String departmentName;
  @Column(name = "project_id") private UUID projectId;
  @Column(nullable = false, length = 160) private String destination;
  @Column(nullable = false, length = 800) private String purpose;
  @Column(name = "transport_type", nullable = false, length = 60) private String transportType;
  @Column(name = "start_date", nullable = false) private LocalDate startDate;
  @Column(name = "end_date", nullable = false) private LocalDate endDate;
  @Column(name = "travel_days", nullable = false) private Integer travelDays;
  @Column(name = "estimated_amount", nullable = false, precision = 14, scale = 2) private BigDecimal estimatedAmount;
  @Column(name = "companion_names", length = 500) private String companionNames;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private OfficeApplicationStatus status;
  @Column(name = "approval_request_id") private UUID approvalRequestId;

  public String getCode() { return code; } public void setCode(String value) { code = value; }
  public UUID getApplicantId() { return applicantId; } public void setApplicantId(UUID value) { applicantId = value; }
  public String getApplicantName() { return applicantName; } public void setApplicantName(String value) { applicantName = value; }
  public String getDepartmentName() { return departmentName; } public void setDepartmentName(String value) { departmentName = value; }
  public UUID getProjectId() { return projectId; } public void setProjectId(UUID value) { projectId = value; }
  public String getDestination() { return destination; } public void setDestination(String value) { destination = value; }
  public String getPurpose() { return purpose; } public void setPurpose(String value) { purpose = value; }
  public String getTransportType() { return transportType; } public void setTransportType(String value) { transportType = value; }
  public LocalDate getStartDate() { return startDate; } public void setStartDate(LocalDate value) { startDate = value; }
  public LocalDate getEndDate() { return endDate; } public void setEndDate(LocalDate value) { endDate = value; }
  public Integer getTravelDays() { return travelDays; } public void setTravelDays(Integer value) { travelDays = value; }
  public BigDecimal getEstimatedAmount() { return estimatedAmount; } public void setEstimatedAmount(BigDecimal value) { estimatedAmount = value; }
  public String getCompanionNames() { return companionNames; } public void setCompanionNames(String value) { companionNames = value; }
  public OfficeApplicationStatus getStatus() { return status; } public void setStatus(OfficeApplicationStatus value) { status = value; }
  public UUID getApprovalRequestId() { return approvalRequestId; } public void setApprovalRequestId(UUID value) { approvalRequestId = value; }
}
