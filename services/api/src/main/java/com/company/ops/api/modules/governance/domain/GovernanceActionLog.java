package com.company.ops.api.modules.governance.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "biz_governance_action_logs")
public class GovernanceActionLog extends BaseEntity {
  @Column(name = "entity_type", nullable = false, length = 32) private String entityType;
  @Column(name = "entity_id", nullable = false) private UUID entityId;
  @Column(name = "entity_no", length = 100) private String entityNo;
  @Column(name = "action_type", nullable = false, length = 32) private String actionType;
  @Column(name = "from_status", length = 32) private String fromStatus;
  @Column(name = "to_status", length = 32) private String toStatus;
  @Column(name = "operator_name", nullable = false, length = 80) private String operatorName;
  @Column(length = 1000) private String note;

  public String getEntityType() { return entityType; } public void setEntityType(String v) { entityType = v; }
  public UUID getEntityId() { return entityId; } public void setEntityId(UUID v) { entityId = v; }
  public String getEntityNo() { return entityNo; } public void setEntityNo(String v) { entityNo = v; }
  public String getActionType() { return actionType; } public void setActionType(String v) { actionType = v; }
  public String getFromStatus() { return fromStatus; } public void setFromStatus(String v) { fromStatus = v; }
  public String getToStatus() { return toStatus; } public void setToStatus(String v) { toStatus = v; }
  public String getOperatorName() { return operatorName; } public void setOperatorName(String v) { operatorName = v; }
  public String getNote() { return note; } public void setNote(String v) { note = v; }
}
