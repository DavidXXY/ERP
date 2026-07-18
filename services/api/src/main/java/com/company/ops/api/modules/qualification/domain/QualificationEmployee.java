package com.company.ops.api.modules.qualification.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import com.company.ops.api.common.security.EncryptedStringConverter;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import com.company.ops.api.modules.system.domain.SystemOrganization;
import com.company.ops.api.modules.system.domain.SystemUser;
import java.time.LocalDate;

@Entity
@Table(name = "qual_employees")
public class QualificationEmployee extends BaseEntity {
  @Column(name = "external_id", nullable = false, length = 100) private String externalId;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "system_user_id")
  private SystemUser systemUser;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id")
  private SystemOrganization organization;
  @Column(nullable = false, length = 80) private String name;
  @Column(name = "work_no", length = 64) private String workNo;
  @Column(length = 120) private String department;
  @Column(name = "position_name", length = 120) private String position;
  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "id_card", length = 512) private String idCard;
  @Column(length = 40) private String phone;
  @Column(name = "entry_date") private LocalDate entryDate;
  @Column(name = "employment_status", nullable = false, length = 32) private String employmentStatus = "ACTIVE";
  @Column(name = "contract_start") private LocalDate contractStart;
  @Column(name = "contract_end") private LocalDate contractEnd;
  @Column(name = "social_security_unit", length = 160) private String socialSecurityUnit;
  @Column(name = "social_security_start") private LocalDate socialSecurityStart;
  @Column(name = "social_security_end") private LocalDate socialSecurityEnd;
  @Column(length = 1000) private String remark;

  public String getExternalId() { return externalId; } public void setExternalId(String value) { externalId = value; }
  public SystemUser getSystemUser() { return systemUser; } public void setSystemUser(SystemUser value) { systemUser = value; }
  public SystemOrganization getOrganization() { return organization; } public void setOrganization(SystemOrganization value) { organization = value; }
  public String getName() { return name; } public void setName(String value) { name = value; }
  public String getWorkNo() { return workNo; } public void setWorkNo(String value) { workNo = value; }
  public String getDepartment() { return department; } public void setDepartment(String value) { department = value; }
  public String getPosition() { return position; } public void setPosition(String value) { position = value; }
  public String getIdCard() { return idCard; } public void setIdCard(String value) { idCard = value; }
  public String getPhone() { return phone; } public void setPhone(String value) { phone = value; }
  public LocalDate getEntryDate() { return entryDate; } public void setEntryDate(LocalDate value) { entryDate = value; }
  public String getEmploymentStatus() { return employmentStatus; } public void setEmploymentStatus(String value) { employmentStatus = value; }
  public LocalDate getContractStart() { return contractStart; } public void setContractStart(LocalDate value) { contractStart = value; }
  public LocalDate getContractEnd() { return contractEnd; } public void setContractEnd(LocalDate value) { contractEnd = value; }
  public String getSocialSecurityUnit() { return socialSecurityUnit; } public void setSocialSecurityUnit(String value) { socialSecurityUnit = value; }
  public LocalDate getSocialSecurityStart() { return socialSecurityStart; } public void setSocialSecurityStart(LocalDate value) { socialSecurityStart = value; }
  public LocalDate getSocialSecurityEnd() { return socialSecurityEnd; } public void setSocialSecurityEnd(LocalDate value) { socialSecurityEnd = value; }
  public String getRemark() { return remark; } public void setRemark(String value) { remark = value; }
}
