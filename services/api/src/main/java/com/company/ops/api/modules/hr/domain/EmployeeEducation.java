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
@Table(name = "hr_employee_education")
public class EmployeeEducation extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private QualificationEmployee employee;
    @Column(name = "school_name", nullable = false, length = 200) private String schoolName;
    @Column(name = "degree", length = 40) private String degree;
    @Column(name = "major", length = 200) private String major;
    @Column(name = "start_date") private LocalDate startDate;
    @Column(name = "end_date") private LocalDate endDate;
    @Column(name = "is_highest", nullable = false) private boolean highest = false;
    @Column(length = 1000) private String remark;

    public QualificationEmployee getEmployee() { return employee; } public void setEmployee(QualificationEmployee v) { employee = v; }
    public String getSchoolName() { return schoolName; } public void setSchoolName(String v) { schoolName = v; }
    public String getDegree() { return degree; } public void setDegree(String v) { degree = v; }
    public String getMajor() { return major; } public void setMajor(String v) { major = v; }
    public LocalDate getStartDate() { return startDate; } public void setStartDate(LocalDate v) { startDate = v; }
    public LocalDate getEndDate() { return endDate; } public void setEndDate(LocalDate v) { endDate = v; }
    public boolean isHighest() { return highest; } public void setHighest(boolean v) { highest = v; }
    public String getRemark() { return remark; } public void setRemark(String v) { remark = v; }
}
