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
@Table(name = "hr_employee_contracts")
public class EmployeeContract extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "employee_id", nullable = false)
  private QualificationEmployee employee;
  @Column(name = "contract_no", nullable = false, length = 100) private String contractNo;
  @Column(name = "contract_type", nullable = false, length = 80) private String contractType;
  @Column(name = "sign_date") private LocalDate signDate;
  @Column(name = "start_date", nullable = false) private LocalDate startDate;
  @Column(name = "end_date") private LocalDate endDate;
  @Column(name = "probation_end_date") private LocalDate probationEndDate;
  @Column(nullable = false, length = 32) private String status = "ACTIVE";
  @Column(name = "attachments_json", length = 4000) private String attachmentsJson = "[]";
  @Column(length = 1000) private String remark;

  public QualificationEmployee getEmployee() { return employee; } public void setEmployee(QualificationEmployee value) { employee = value; }
  public String getContractNo() { return contractNo; } public void setContractNo(String value) { contractNo = value; }
  public String getContractType() { return contractType; } public void setContractType(String value) { contractType = value; }
  public LocalDate getSignDate() { return signDate; } public void setSignDate(LocalDate value) { signDate = value; }
  public LocalDate getStartDate() { return startDate; } public void setStartDate(LocalDate value) { startDate = value; }
  public LocalDate getEndDate() { return endDate; } public void setEndDate(LocalDate value) { endDate = value; }
  public LocalDate getProbationEndDate() { return probationEndDate; } public void setProbationEndDate(LocalDate value) { probationEndDate = value; }
  public String getStatus() { return status; } public void setStatus(String value) { status = value; }
  public String getAttachmentsJson() { return attachmentsJson; } public void setAttachmentsJson(String value) { attachmentsJson = value; }
  public String getRemark() { return remark; } public void setRemark(String value) { remark = value; }
}
