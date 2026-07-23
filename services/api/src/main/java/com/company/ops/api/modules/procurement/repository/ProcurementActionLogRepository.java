package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.ProcurementActionLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcurementActionLogRepository extends JpaRepository<ProcurementActionLog, UUID> {
  List<ProcurementActionLog> findBySourceTypeAndSourceIdOrderByCreatedAtDesc(
      String sourceType, UUID sourceId);
}
