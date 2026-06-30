package com.company.ops.api.modules.system.repository;

import com.company.ops.api.modules.system.domain.SystemPermission;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemPermissionRepository extends JpaRepository<SystemPermission, UUID> {

  boolean existsByCodeAndTenantId(String code, String tenantId);
}
