package com.company.ops.api.modules.maintenance.repository;

import com.company.ops.api.modules.maintenance.domain.WorkOrderMaterial;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkOrderMaterialRepository extends JpaRepository<WorkOrderMaterial, UUID> {
  List<WorkOrderMaterial> findByWorkOrderIdOrderByCreatedAtAsc(UUID workOrderId);
}
