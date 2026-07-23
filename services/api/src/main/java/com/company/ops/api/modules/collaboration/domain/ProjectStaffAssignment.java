package com.company.ops.api.modules.collaboration.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name="biz_project_staff_assignments")
public class ProjectStaffAssignment extends BaseEntity {
  @Column(name="project_id",nullable=false) private UUID projectId;
  @Column(name="user_id",nullable=false) private UUID userId;
  @Column(name="department_id") private UUID departmentId;
  @Column(name="role_name",nullable=false,length=80) private String roleName;
  @Column(name="planned_hours",nullable=false,precision=12,scale=2) private BigDecimal plannedHours=BigDecimal.ZERO;
  @Column(name="actual_hours",nullable=false,precision=12,scale=2) private BigDecimal actualHours=BigDecimal.ZERO;
  @Column(name="hourly_cost",nullable=false,precision=12,scale=2) private BigDecimal hourlyCost=BigDecimal.ZERO;
  @Column(name="allocation_percent",nullable=false,precision=5,scale=2) private BigDecimal allocationPercent=BigDecimal.valueOf(100);
  @Column(name="start_date",nullable=false) private LocalDate startDate;
  @Column(name="end_date",nullable=false) private LocalDate endDate;
  @Column(name="certificate_status",nullable=false,length=32) private String certificateStatus;
  @Column(nullable=false,length=32) private String status="PLANNED";
  public UUID getProjectId(){return projectId;} public void setProjectId(UUID v){projectId=v;}
  public UUID getUserId(){return userId;} public void setUserId(UUID v){userId=v;}
  public UUID getDepartmentId(){return departmentId;} public void setDepartmentId(UUID v){departmentId=v;}
  public String getRoleName(){return roleName;} public void setRoleName(String v){roleName=v;}
  public BigDecimal getPlannedHours(){return plannedHours;} public void setPlannedHours(BigDecimal v){plannedHours=v;}
  public BigDecimal getActualHours(){return actualHours;} public void setActualHours(BigDecimal v){actualHours=v;}
  public BigDecimal getHourlyCost(){return hourlyCost;} public void setHourlyCost(BigDecimal v){hourlyCost=v;}
  public BigDecimal getAllocationPercent(){return allocationPercent;} public void setAllocationPercent(BigDecimal v){allocationPercent=v;}
  public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate v){startDate=v;}
  public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate v){endDate=v;}
  public String getCertificateStatus(){return certificateStatus;} public void setCertificateStatus(String v){certificateStatus=v;}
  public String getStatus(){return status;} public void setStatus(String v){status=v;}
}
