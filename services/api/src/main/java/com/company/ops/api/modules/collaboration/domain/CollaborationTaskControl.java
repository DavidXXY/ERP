package com.company.ops.api.modules.collaboration.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name="biz_collaboration_task_controls")
public class CollaborationTaskControl extends BaseEntity {
  @Column(name="source_type",nullable=false,length=40) private String sourceType;
  @Column(name="source_id",nullable=false) private UUID sourceId;
  @Column(name="assignee_user_id") private UUID assigneeUserId;
  @Column(name="cc_user_ids",length=2000) private String ccUserIds;
  @Column(nullable=false,length=32) private String status="PENDING";
  @Column(name="due_at") private OffsetDateTime dueAt;
  @Column(name="completed_at") private OffsetDateTime completedAt;
  @Column(name="last_comment",length=1000) private String lastComment;
  @Column(name="reminder_count",nullable=false) private int reminderCount;
  @Column(name="last_reminded_at") private OffsetDateTime lastRemindedAt;
  public String getSourceType(){return sourceType;} public void setSourceType(String v){sourceType=v;}
  public UUID getSourceId(){return sourceId;} public void setSourceId(UUID v){sourceId=v;}
  public UUID getAssigneeUserId(){return assigneeUserId;} public void setAssigneeUserId(UUID v){assigneeUserId=v;}
  public String getCcUserIds(){return ccUserIds;} public void setCcUserIds(String v){ccUserIds=v;}
  public String getStatus(){return status;} public void setStatus(String v){status=v;}
  public OffsetDateTime getDueAt(){return dueAt;} public void setDueAt(OffsetDateTime v){dueAt=v;}
  public OffsetDateTime getCompletedAt(){return completedAt;} public void setCompletedAt(OffsetDateTime v){completedAt=v;}
  public String getLastComment(){return lastComment;} public void setLastComment(String v){lastComment=v;}
  public int getReminderCount(){return reminderCount;} public void setReminderCount(int v){reminderCount=v;}
  public OffsetDateTime getLastRemindedAt(){return lastRemindedAt;} public void setLastRemindedAt(OffsetDateTime v){lastRemindedAt=v;}
}
