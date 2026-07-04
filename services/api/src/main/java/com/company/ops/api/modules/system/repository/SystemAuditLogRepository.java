package com.company.ops.api.modules.system.repository;

import com.company.ops.api.modules.system.domain.SystemAuditLog;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemAuditLogRepository extends JpaRepository<SystemAuditLog, UUID> {
  Page<SystemAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
