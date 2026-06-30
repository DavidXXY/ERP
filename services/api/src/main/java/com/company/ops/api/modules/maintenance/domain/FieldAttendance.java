package com.company.ops.api.modules.maintenance.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "hr_field_attendance")
public class FieldAttendance extends BaseEntity {
  @Column(name = "user_id", nullable = false) private UUID userId;
  @Column(name = "work_order_id", nullable = false) private UUID workOrderId;
  @Column(name = "check_in_at", nullable = false) private OffsetDateTime checkInAt;
  @Column(name = "check_out_at") private OffsetDateTime checkOutAt;
  @Column(name = "check_in_location", nullable = false, length = 300) private String checkInLocation;
  @Column(name = "check_out_location", length = 300) private String checkOutLocation;
  public UUID getUserId() { return userId; } public void setUserId(UUID value) { userId = value; }
  public UUID getWorkOrderId() { return workOrderId; } public void setWorkOrderId(UUID value) { workOrderId = value; }
  public OffsetDateTime getCheckInAt() { return checkInAt; } public void setCheckInAt(OffsetDateTime value) { checkInAt = value; }
  public OffsetDateTime getCheckOutAt() { return checkOutAt; } public void setCheckOutAt(OffsetDateTime value) { checkOutAt = value; }
  public String getCheckInLocation() { return checkInLocation; } public void setCheckInLocation(String value) { checkInLocation = value; }
  public String getCheckOutLocation() { return checkOutLocation; } public void setCheckOutLocation(String value) { checkOutLocation = value; }
}
