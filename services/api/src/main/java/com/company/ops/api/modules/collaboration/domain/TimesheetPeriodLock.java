package com.company.ops.api.modules.collaboration.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name="biz_timesheet_period_locks")
public class TimesheetPeriodLock extends BaseEntity {
  @Column(name="year_month",nullable=false,length=7) private String yearMonth;
  @Column(name="locked_by") private UUID lockedBy;
  @Column(name="locked_by_name",nullable=false,length=80) private String lockedByName;
  @Column(name="locked_at",nullable=false) private OffsetDateTime lockedAt;
  @Column(length=500) private String reason;
  public String getYearMonth(){return yearMonth;} public void setYearMonth(String v){yearMonth=v;}
  public UUID getLockedBy(){return lockedBy;} public void setLockedBy(UUID v){lockedBy=v;}
  public String getLockedByName(){return lockedByName;} public void setLockedByName(String v){lockedByName=v;}
  public OffsetDateTime getLockedAt(){return lockedAt;} public void setLockedAt(OffsetDateTime v){lockedAt=v;}
  public String getReason(){return reason;} public void setReason(String v){reason=v;}
}
