package com.company.ops.api.modules.qualification.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "qual_performances")
public class QualificationPerformance extends BaseEntity {
  @Column(name = "external_id", nullable = false, length = 100) private String externalId;
  @Column(name = "subject_company", nullable = false, length = 160) private String subjectCompany;
  @Column(nullable = false, length = 500) private String name;
  @Column(name = "client_name", length = 240) private String clientName;
  @Column(name = "contract_no", length = 160) private String contractNo;
  @Column(name = "contract_date") private LocalDate contractDate;
  @Column(name = "contract_amount", length = 100) private String contractAmount;
  @Column(name = "project_type", length = 160) private String projectType;
  @Column(name = "attachments_json", length = 4000) private String attachmentsJson;
  @Column(length = 1000) private String remark;

  public String getExternalId() { return externalId; } public void setExternalId(String value) { externalId = value; }
  public String getSubjectCompany() { return subjectCompany; } public void setSubjectCompany(String value) { subjectCompany = value; }
  public String getName() { return name; } public void setName(String value) { name = value; }
  public String getClientName() { return clientName; } public void setClientName(String value) { clientName = value; }
  public String getContractNo() { return contractNo; } public void setContractNo(String value) { contractNo = value; }
  public LocalDate getContractDate() { return contractDate; } public void setContractDate(LocalDate value) { contractDate = value; }
  public String getContractAmount() { return contractAmount; } public void setContractAmount(String value) { contractAmount = value; }
  public String getProjectType() { return projectType; } public void setProjectType(String value) { projectType = value; }
  public String getAttachmentsJson() { return attachmentsJson; } public void setAttachmentsJson(String value) { attachmentsJson = value; }
  public String getRemark() { return remark; } public void setRemark(String value) { remark = value; }
}
