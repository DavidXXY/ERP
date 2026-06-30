package com.company.ops.api.modules.maintenance.repository;

import com.company.ops.api.modules.maintenance.domain.WorkOrderStatusLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkOrderStatusLogRepository extends JpaRepository<WorkOrderStatusLog, UUID> {
  List<WorkOrderStatusLog> findByWorkOrderIdOrderByCreatedAtAsc(UUID workOrderId);
}
