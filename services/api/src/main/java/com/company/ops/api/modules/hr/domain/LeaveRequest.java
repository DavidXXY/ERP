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
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_leave_requests")
public class LeaveRequest extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private QualificationEmployee employee;
    @Column(name = "leave_type", nullable = false, length = 40) private String leaveType;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "end_date", nullable = false) private LocalDate endDate;
    @Column(name = "total_days", nullable = false) private double totalDays;
    @Column(length = 1000) private String reason;
    @Column(nullable = false, length = 32) private String status = "PENDING";
    @Column(name = "approved_by", length = 80) private String approvedBy;
    @Column(name = "approved_at") private LocalDateTime approvedAt;
    @Column(name = "approval_remark", length = 1000) private String approvalRemark;

    public QualificationEmployee getEmployee() { return employee; } public void setEmployee(QualificationEmployee v) { employee = v; }
    public String getLeaveType() { return leaveType; } public void setLeaveType(String v) { leaveType = v; }
    public LocalDate getStartDate() { return startDate; } public void setStartDate(LocalDate v) { startDate = v; }
    public LocalDate getEndDate() { return endDate; } public void setEndDate(LocalDate v) { endDate = v; }
    public double getTotalDays() { return totalDays; } public void setTotalDays(double v) { totalDays = v; }
    public String getReason() { return reason; } public void setReason(String v) { reason = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
    public String getApprovedBy() { return approvedBy; } public void setApprovedBy(String v) { approvedBy = v; }
    public LocalDateTime getApprovedAt() { return approvedAt; } public void setApprovedAt(LocalDateTime v) { approvedAt = v; }
    public String getApprovalRemark() { return approvalRemark; } public void setApprovalRemark(String v) { approvalRemark = v; }
}
