package com.company.ops.api.modules.maintenance.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "hr_employee_certificates")
public class EmployeeCertificate extends BaseEntity {
  @Column(name = "user_id", nullable = false) private UUID userId;
  @Column(name = "certificate_type", nullable = false, length = 120) private String certificateType;
  @Column(name = "certificate_no", nullable = false, length = 120) private String certificateNo;
  @Column(name = "issue_date") private LocalDate issueDate;
  @Column(name = "expiry_date", nullable = false) private LocalDate expiryDate;
  @Column(name = "issuing_authority", length = 180) private String issuingAuthority;
  public UUID getUserId() { return userId; } public void setUserId(UUID value) { userId = value; }
  public String getCertificateType() { return certificateType; } public void setCertificateType(String value) { certificateType = value; }
  public String getCertificateNo() { return certificateNo; } public void setCertificateNo(String value) { certificateNo = value; }
  public LocalDate getIssueDate() { return issueDate; } public void setIssueDate(LocalDate value) { issueDate = value; }
  public LocalDate getExpiryDate() { return expiryDate; } public void setExpiryDate(LocalDate value) { expiryDate = value; }
  public String getIssuingAuthority() { return issuingAuthority; } public void setIssuingAuthority(String value) { issuingAuthority = value; }
}
