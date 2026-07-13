package com.company.ops.api.modules.hr.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import com.company.ops.api.modules.qualification.domain.QualificationEmployee;

@Entity
@Table(name = "hr_leave_balances")
public class LeaveBalance extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private QualificationEmployee employee;
    @Column(name = "leave_type", nullable = false, length = 40) private String leaveType;
    @Column(name = "leave_year", nullable = false) private int year;
    @Column(name = "total_days", nullable = false) private double totalDays = 0;
    @Column(name = "used_days", nullable = false) private double usedDays = 0;

    public QualificationEmployee getEmployee() { return employee; } public void setEmployee(QualificationEmployee v) { employee = v; }
    public String getLeaveType() { return leaveType; } public void setLeaveType(String v) { leaveType = v; }
    public int getYear() { return year; } public void setYear(int v) { year = v; }
    public double getTotalDays() { return totalDays; } public void setTotalDays(double v) { totalDays = v; }
    public double getUsedDays() { return usedDays; } public void setUsedDays(double v) { usedDays = v; }
    public double getRemainingDays() { return totalDays - usedDays; }
}
