package com.company.ops.api.modules.maintenance.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "hr_field_schedules")
public class FieldSchedule extends BaseEntity {
  @Column(name = "user_id", nullable = false) private UUID userId;
  @Column(name = "work_date", nullable = false) private LocalDate workDate;
  @Column(name = "shift_name", nullable = false, length = 80) private String shiftName;
  @Column(name = "site_name", length = 180) private String siteName;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private ScheduleStatus status;
  public UUID getUserId() { return userId; } public void setUserId(UUID value) { userId = value; }
  public LocalDate getWorkDate() { return workDate; } public void setWorkDate(LocalDate value) { workDate = value; }
  public String getShiftName() { return shiftName; } public void setShiftName(String value) { shiftName = value; }
  public String getSiteName() { return siteName; } public void setSiteName(String value) { siteName = value; }
  public ScheduleStatus getStatus() { return status; } public void setStatus(ScheduleStatus value) { status = value; }
}
