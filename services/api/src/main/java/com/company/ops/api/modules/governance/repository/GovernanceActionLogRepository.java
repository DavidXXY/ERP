package com.company.ops.api.modules.governance.repository;

import com.company.ops.api.modules.governance.domain.GovernanceActionLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GovernanceActionLogRepository extends JpaRepository<GovernanceActionLog, UUID> {
  List<GovernanceActionLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, UUID entityId);
}
