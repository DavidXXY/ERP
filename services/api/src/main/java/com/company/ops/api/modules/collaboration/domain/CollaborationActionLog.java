package com.company.ops.api.modules.collaboration.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name="biz_collaboration_action_logs")
public class CollaborationActionLog extends BaseEntity {
  @Column(name="source_type",nullable=false,length=40) private String sourceType;
  @Column(name="source_id",nullable=false) private UUID sourceId;
  @Column(name="action_type",nullable=false,length=40) private String actionType;
  @Column(name="operator_user_id") private UUID operatorUserId;
  @Column(name="operator_name",nullable=false,length=80) private String operatorName;
  @Column(name="target_user_id") private UUID targetUserId;
  @Column(length=1000) private String comment;
  public String getSourceType(){return sourceType;} public void setSourceType(String v){sourceType=v;}
  public UUID getSourceId(){return sourceId;} public void setSourceId(UUID v){sourceId=v;}
  public String getActionType(){return actionType;} public void setActionType(String v){actionType=v;}
  public UUID getOperatorUserId(){return operatorUserId;} public void setOperatorUserId(UUID v){operatorUserId=v;}
  public String getOperatorName(){return operatorName;} public void setOperatorName(String v){operatorName=v;}
  public UUID getTargetUserId(){return targetUserId;} public void setTargetUserId(UUID v){targetUserId=v;}
  public String getComment(){return comment;} public void setComment(String v){comment=v;}
}
