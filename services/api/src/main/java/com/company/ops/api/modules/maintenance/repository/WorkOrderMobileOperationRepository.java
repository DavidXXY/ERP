package com.company.ops.api.modules.maintenance.repository;

import com.company.ops.api.modules.maintenance.domain.WorkOrderMobileOperation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkOrderMobileOperationRepository extends JpaRepository<WorkOrderMobileOperation, UUID> {
  boolean existsByOperationId(String operationId);
}
