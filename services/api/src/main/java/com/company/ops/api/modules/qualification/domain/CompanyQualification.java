package com.company.ops.api.modules.qualification.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "qual_company_qualifications")
public class CompanyQualification extends BaseEntity {
  @Column(name = "external_id", nullable = false, length = 100) private String externalId;
  @Column(name = "subject_company", nullable = false, length = 160) private String subjectCompany;
  @Column(nullable = false, length = 200) private String name;
  @Column(nullable = false, length = 100) private String category;
  @Column(name = "level_name", length = 160) private String level;
  @Column(name = "certificate_no", length = 180) private String certificateNo;
  @Column(length = 240) private String issuer;
  @Column(name = "issue_date") private LocalDate issueDate;
  @Column(name = "valid_from") private LocalDate validFrom;
  @Column(name = "valid_to") private LocalDate validTo;
  @Column(name = "annual_review_date") private LocalDate annualReviewDate;
  @Column(name = "renewal_date") private LocalDate renewalDate;
  @Column(name = "scope_text", length = 4000) private String scope;
  @Column(name = "project_types_json", length = 2000) private String projectTypesJson;
  @Column(name = "holder_branch", length = 240) private String holderBranch;
  @Column(name = "storage_location", length = 500) private String storageLocation;
  @Column(name = "available_for_tender", nullable = false) private boolean availableForTender = true;
  @Column(name = "manual_status", nullable = false, length = 32) private String manualStatus = "NORMAL";
  @Column(nullable = false) private boolean locked;
  @Column(name = "attachments_json", length = 4000) private String attachmentsJson;
  @Column(length = 1000) private String remark;

  public String getExternalId() { return externalId; } public void setExternalId(String value) { externalId = value; }
  public String getSubjectCompany() { return subjectCompany; } public void setSubjectCompany(String value) { subjectCompany = value; }
  public String getName() { return name; } public void setName(String value) { name = value; }
  public String getCategory() { return category; } public void setCategory(String value) { category = value; }
  public String getLevel() { return level; } public void setLevel(String value) { level = value; }
  public String getCertificateNo() { return certificateNo; } public void setCertificateNo(String value) { certificateNo = value; }
  public String getIssuer() { return issuer; } public void setIssuer(String value) { issuer = value; }
  public LocalDate getIssueDate() { return issueDate; } public void setIssueDate(LocalDate value) { issueDate = value; }
  public LocalDate getValidFrom() { return validFrom; } public void setValidFrom(LocalDate value) { validFrom = value; }
  public LocalDate getValidTo() { return validTo; } public void setValidTo(LocalDate value) { validTo = value; }
  public LocalDate getAnnualReviewDate() { return annualReviewDate; } public void setAnnualReviewDate(LocalDate value) { annualReviewDate = value; }
  public LocalDate getRenewalDate() { return renewalDate; } public void setRenewalDate(LocalDate value) { renewalDate = value; }
  public String getScope() { return scope; } public void setScope(String value) { scope = value; }
  public String getProjectTypesJson() { return projectTypesJson; } public void setProjectTypesJson(String value) { projectTypesJson = value; }
  public String getHolderBranch() { return holderBranch; } public void setHolderBranch(String value) { holderBranch = value; }
  public String getStorageLocation() { return storageLocation; } public void setStorageLocation(String value) { storageLocation = value; }
  public boolean isAvailableForTender() { return availableForTender; } public void setAvailableForTender(boolean value) { availableForTender = value; }
  public String getManualStatus() { return manualStatus; } public void setManualStatus(String value) { manualStatus = value; }
  public boolean isLocked() { return locked; } public void setLocked(boolean value) { locked = value; }
  public String getAttachmentsJson() { return attachmentsJson; } public void setAttachmentsJson(String value) { attachmentsJson = value; }
  public String getRemark() { return remark; } public void setRemark(String value) { remark = value; }
}
