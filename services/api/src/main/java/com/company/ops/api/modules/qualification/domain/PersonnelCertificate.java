package com.company.ops.api.modules.qualification.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "qual_personnel_certificates")
public class PersonnelCertificate extends BaseEntity {
  @Column(name = "external_id", nullable = false, length = 100) private String externalId;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "employee_id") private QualificationEmployee employee;
  @Column(nullable = false, length = 200) private String name;
  @Column(name = "certificate_type", length = 100) private String type;
  @Column(name = "certificate_no", length = 180) private String certificateNo;
  @Column(length = 180) private String specialty;
  @Column(name = "company_registered", nullable = false) private boolean companyRegistered;
  @Column(name = "issue_date") private LocalDate issueDate;
  @Column(name = "valid_to") private LocalDate validTo;
  @Column(name = "review_date") private LocalDate reviewDate;
  @Column(name = "available_for_tender", nullable = false) private boolean availableForTender = true;
  @Column(name = "manual_status", nullable = false, length = 32) private String manualStatus = "NORMAL";
  @Column(nullable = false) private boolean locked;
  @Column(name = "attachments_json", length = 4000) private String attachmentsJson;
  @Column(length = 1000) private String remark;

  public String getExternalId() { return externalId; } public void setExternalId(String value) { externalId = value; }
  public QualificationEmployee getEmployee() { return employee; } public void setEmployee(QualificationEmployee value) { employee = value; }
  public String getName() { return name; } public void setName(String value) { name = value; }
  public String getType() { return type; } public void setType(String value) { type = value; }
  public String getCertificateNo() { return certificateNo; } public void setCertificateNo(String value) { certificateNo = value; }
  public String getSpecialty() { return specialty; } public void setSpecialty(String value) { specialty = value; }
  public boolean isCompanyRegistered() { return companyRegistered; } public void setCompanyRegistered(boolean value) { companyRegistered = value; }
  public LocalDate getIssueDate() { return issueDate; } public void setIssueDate(LocalDate value) { issueDate = value; }
  public LocalDate getValidTo() { return validTo; } public void setValidTo(LocalDate value) { validTo = value; }
  public LocalDate getReviewDate() { return reviewDate; } public void setReviewDate(LocalDate value) { reviewDate = value; }
  public boolean isAvailableForTender() { return availableForTender; } public void setAvailableForTender(boolean value) { availableForTender = value; }
  public String getManualStatus() { return manualStatus; } public void setManualStatus(String value) { manualStatus = value; }
  public boolean isLocked() { return locked; } public void setLocked(boolean value) { locked = value; }
  public String getAttachmentsJson() { return attachmentsJson; } public void setAttachmentsJson(String value) { attachmentsJson = value; }
  public String getRemark() { return remark; } public void setRemark(String value) { remark = value; }
}
