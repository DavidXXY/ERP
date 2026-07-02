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
@Table(name = "hr_emergency_contacts")
public class EmergencyContact extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private QualificationEmployee employee;
    @Column(nullable = false, length = 80) private String name;
    @Column(length = 40) private String relationship;
    @Column(length = 40) private String phone;
    @Column(length = 200) private String address;
    @Column(name = "is_primary", nullable = false) private boolean primary = false;
    @Column(length = 1000) private String remark;

    public QualificationEmployee getEmployee() { return employee; } public void setEmployee(QualificationEmployee v) { employee = v; }
    public String getName() { return name; } public void setName(String v) { name = v; }
    public String getRelationship() { return relationship; } public void setRelationship(String v) { relationship = v; }
    public String getPhone() { return phone; } public void setPhone(String v) { phone = v; }
    public String getAddress() { return address; } public void setAddress(String v) { address = v; }
    public boolean isPrimary() { return primary; } public void setPrimary(boolean v) { primary = v; }
    public String getRemark() { return remark; } public void setRemark(String v) { remark = v; }
}
