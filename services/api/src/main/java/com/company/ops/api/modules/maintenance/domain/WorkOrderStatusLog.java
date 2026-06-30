package com.company.ops.api.modules.maintenance.domain;

import com.company.ops.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "work_order_status_logs")
public class WorkOrderStatusLog extends BaseEntity {
  @Column(name = "work_order_id", nullable = false) private UUID workOrderId;
  @Enumerated(EnumType.STRING) @Column(name = "from_status", length = 40) private WorkOrderStatus fromStatus;
  @Enumerated(EnumType.STRING) @Column(name = "to_status", nullable = false, length = 40) private WorkOrderStatus toStatus;
  @Column(name = "operator_name", nullable = false, length = 80) private String operatorName;
  @Column(length = 500) private String remark;
  public UUID getWorkOrderId() { return workOrderId; } public void setWorkOrderId(UUID value) { workOrderId = value; }
  public WorkOrderStatus getFromStatus() { return fromStatus; } public void setFromStatus(WorkOrderStatus value) { fromStatus = value; }
  public WorkOrderStatus getToStatus() { return toStatus; } public void setToStatus(WorkOrderStatus value) { toStatus = value; }
  public String getOperatorName() { return operatorName; } public void setOperatorName(String value) { operatorName = value; }
  public String getRemark() { return remark; } public void setRemark(String value) { remark = value; }
}
