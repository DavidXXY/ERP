package com.company.ops.api.modules.maintenance.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "work_order_mobile_operations")
public class WorkOrderMobileOperation extends BaseEntity {
  @Column(name = "work_order_id", nullable = false) private UUID workOrderId;
  @Column(name = "operation_id", nullable = false, length = 100) private String operationId;
  @Column(name = "operation_type", nullable = false, length = 40) private String operationType;
  @Column(name = "operated_by", nullable = false) private UUID operatedBy;
  public UUID getWorkOrderId() { return workOrderId; }
  public void setWorkOrderId(UUID value) { workOrderId = value; }
  public String getOperationId() { return operationId; }
  public void setOperationId(String value) { operationId = value; }
  public String getOperationType() { return operationType; }
  public void setOperationType(String value) { operationType = value; }
  public UUID getOperatedBy() { return operatedBy; }
  public void setOperatedBy(UUID value) { operatedBy = value; }
}
