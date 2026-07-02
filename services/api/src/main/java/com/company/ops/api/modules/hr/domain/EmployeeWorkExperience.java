package com.company.ops.api.modules.hr.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import com.company.ops.api.modules.qualification.domain.QualificationEmployee;
import java.time.LocalDate;

@Entity
@Table(name = "hr_employee_work_experience")
public class EmployeeWorkExperience extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private QualificationEmployee employee;
    @Column(name = "company_name", nullable = false, length = 200) private String companyName;
    @Column(name = "position_name", length = 120) private String position;
    @Column(name = "start_date") private LocalDate startDate;
    @Column(name = "end_date") private LocalDate endDate;
    @Column(name = "is_current", nullable = false) private boolean current = false;
    @Column(length = 1000) private String description;
    @Column(length = 1000) private String remark;

    public QualificationEmployee getEmployee() { return employee; } public void setEmployee(QualificationEmployee v) { employee = v; }
    public String getCompanyName() { return companyName; } public void setCompanyName(String v) { companyName = v; }
    public String getPosition() { return position; } public void setPosition(String v) { position = v; }
    public LocalDate getStartDate() { return startDate; } public void setStartDate(LocalDate v) { startDate = v; }
    public LocalDate getEndDate() { return endDate; } public void setEndDate(LocalDate v) { endDate = v; }
    public boolean isCurrent() { return current; } public void setCurrent(boolean v) { current = v; }
    public String getDescription() { return description; } public void setDescription(String v) { description = v; }
    public String getRemark() { return remark; } public void setRemark(String v) { remark = v; }
}
