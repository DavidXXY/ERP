package com.company.ops.api.modules.office.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "oa_seal_applications")
public class SealApplication extends BaseEntity {
  @Column(nullable = false, length = 64) private String code;
  @Column(name = "applicant_id", nullable = false) private UUID applicantId;
  @Column(name = "applicant_name", nullable = false, length = 80) private String applicantName;
  @Column(name = "department_name", nullable = false, length = 120) private String departmentName;
  @Column(name = "seal_type", nullable = false, length = 60) private String sealType;
  @Column(name = "document_name", nullable = false, length = 240) private String documentName;
  @Column(name = "document_purpose", nullable = false, length = 800) private String documentPurpose;
  @Column(length = 240) private String counterparty;
  @Column(name = "copy_count", nullable = false) private Integer copyCount;
  @Column(name = "use_date", nullable = false) private LocalDate useDate;
  @Column(name = "take_out", nullable = false) private boolean takeOut;
  @Column(name = "expected_return_date") private LocalDate expectedReturnDate;
  @Column(name = "returned_at") private OffsetDateTime returnedAt;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private OfficeApplicationStatus status;
  @Column(name = "approval_request_id") private UUID approvalRequestId;

  public String getCode() { return code; } public void setCode(String value) { code = value; }
  public UUID getApplicantId() { return applicantId; } public void setApplicantId(UUID value) { applicantId = value; }
  public String getApplicantName() { return applicantName; } public void setApplicantName(String value) { applicantName = value; }
  public String getDepartmentName() { return departmentName; } public void setDepartmentName(String value) { departmentName = value; }
  public String getSealType() { return sealType; } public void setSealType(String value) { sealType = value; }
  public String getDocumentName() { return documentName; } public void setDocumentName(String value) { documentName = value; }
  public String getDocumentPurpose() { return documentPurpose; } public void setDocumentPurpose(String value) { documentPurpose = value; }
  public String getCounterparty() { return counterparty; } public void setCounterparty(String value) { counterparty = value; }
  public Integer getCopyCount() { return copyCount; } public void setCopyCount(Integer value) { copyCount = value; }
  public LocalDate getUseDate() { return useDate; } public void setUseDate(LocalDate value) { useDate = value; }
  public boolean isTakeOut() { return takeOut; } public void setTakeOut(boolean value) { takeOut = value; }
  public LocalDate getExpectedReturnDate() { return expectedReturnDate; } public void setExpectedReturnDate(LocalDate value) { expectedReturnDate = value; }
  public OffsetDateTime getReturnedAt() { return returnedAt; } public void setReturnedAt(OffsetDateTime value) { returnedAt = value; }
  public OfficeApplicationStatus getStatus() { return status; } public void setStatus(OfficeApplicationStatus value) { status = value; }
  public UUID getApprovalRequestId() { return approvalRequestId; } public void setApprovalRequestId(UUID value) { approvalRequestId = value; }
}
