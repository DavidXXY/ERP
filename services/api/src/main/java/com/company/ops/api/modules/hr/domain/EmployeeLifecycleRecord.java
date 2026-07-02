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
@Table(name = "hr_employee_lifecycle_records")
public class EmployeeLifecycleRecord extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private QualificationEmployee employee;
    @Column(name = "lifecycle_type", nullable = false, length = 40) private String lifecycleType;
    @Column(name = "effective_date", nullable = false) private LocalDate effectiveDate;
    @Column(name = "from_organization_id", length = 64) private String fromOrganizationId;
    @Column(name = "from_organization_name", length = 200) private String fromName;
    @Column(name = "from_position", length = 120) private String fromPosition;
    @Column(name = "to_organization_id", length = 64) private String toOrganizationId;
    @Column(name = "to_organization_name", length = 200) private String toName;
    @Column(name = "to_position", length = 120) private String toPosition;
    @Column(name = "reason", length = 1000) private String reason;
    @Column(nullable = false, length = 32) private String status = "PENDING";
    @Column(name = "approved_by", length = 80) private String approvedBy;
    @Column(name = "approved_at") private LocalDate approvedAt;
    @Column(length = 1000) private String remark;

    public QualificationEmployee getEmployee() { return employee; } public void setEmployee(QualificationEmployee v) { employee = v; }
    public String getLifecycleType() { return lifecycleType; } public void setLifecycleType(String v) { lifecycleType = v; }
    public LocalDate getEffectiveDate() { return effectiveDate; } public void setEffectiveDate(LocalDate v) { effectiveDate = v; }
    public String getFromOrganizationId() { return fromOrganizationId; } public void setFromOrganizationId(String v) { fromOrganizationId = v; }
    public String getFromName() { return fromName; } public void setFromName(String v) { fromName = v; }
    public String getFromPosition() { return fromPosition; } public void setFromPosition(String v) { fromPosition = v; }
    public String getToOrganizationId() { return toOrganizationId; } public void setToOrganizationId(String v) { toOrganizationId = v; }
    public String getToName() { return toName; } public void setToName(String v) { toName = v; }
    public String getToPosition() { return toPosition; } public void setToPosition(String v) { toPosition = v; }
    public String getReason() { return reason; } public void setReason(String v) { reason = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
    public String getApprovedBy() { return approvedBy; } public void setApprovedBy(String v) { approvedBy = v; }
    public LocalDate getApprovedAt() { return approvedAt; } public void setApprovedAt(LocalDate v) { approvedAt = v; }
    public String getRemark() { return remark; } public void setRemark(String v) { remark = v; }
}
