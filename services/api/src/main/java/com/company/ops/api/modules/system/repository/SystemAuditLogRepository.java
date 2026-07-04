package com.company.ops.api.modules.system.repository;

import com.company.ops.api.modules.system.domain.SystemAuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemAuditLogRepository extends JpaRepository<SystemAuditLog, UUID> {
  List<SystemAuditLog> findAllByOrderByCreatedAtDesc();
}
