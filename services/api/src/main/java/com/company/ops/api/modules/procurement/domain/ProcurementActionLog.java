package com.company.ops.api.modules.procurement.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "procurement_action_logs")
public class ProcurementActionLog extends BaseEntity {
  @Column(name = "source_type", nullable = false, length = 40)
  private String sourceType;
  @Column(name = "source_id", nullable = false)
  private UUID sourceId;
  @Column(name = "action_type", nullable = false, length = 40)
  private String actionType;
  @Column(name = "operator_name", nullable = false, length = 80)
  private String operatorName;
  @Column(length = 2000)
  private String details;

  public String getSourceType() { return sourceType; }
  public void setSourceType(String value) { sourceType = value; }
  public UUID getSourceId() { return sourceId; }
  public void setSourceId(UUID value) { sourceId = value; }
  public String getActionType() { return actionType; }
  public void setActionType(String value) { actionType = value; }
  public String getOperatorName() { return operatorName; }
  public void setOperatorName(String value) { operatorName = value; }
  public String getDetails() { return details; }
  public void setDetails(String value) { details = value; }
}
