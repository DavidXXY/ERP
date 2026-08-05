package com.company.ops.api.modules.maintenance.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "hr_field_schedules")
public class FieldSchedule extends BaseEntity {
  @Column(name = "user_id", nullable = false) private UUID userId;
  @Column(name = "work_order_id") private UUID workOrderId;
  @Column(name = "work_date", nullable = false) private LocalDate workDate;
  @Column(name = "scheduled_at") private OffsetDateTime scheduledAt;
  @Column(name = "shift_name", nullable = false, length = 80) private String shiftName;
  @Column(name = "site_name", length = 180) private String siteName;
  @Column(nullable = false, length = 32) private String status;

  public UUID getUserId() { return userId; }
  public void setUserId(UUID value) { userId = value; }
  public UUID getWorkOrderId() { return workOrderId; }
  public void setWorkOrderId(UUID value) { workOrderId = value; }
  public LocalDate getWorkDate() { return workDate; }
  public void setWorkDate(LocalDate value) { workDate = value; }
  public OffsetDateTime getScheduledAt() { return scheduledAt; }
  public void setScheduledAt(OffsetDateTime value) { scheduledAt = value; }
  public String getShiftName() { return shiftName; }
  public void setShiftName(String value) { shiftName = value; }
  public String getSiteName() { return siteName; }
  public void setSiteName(String value) { siteName = value; }
  public String getStatus() { return status; }
  public void setStatus(String value) { status = value; }
}
