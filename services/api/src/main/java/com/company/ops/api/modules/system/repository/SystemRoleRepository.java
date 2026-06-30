package com.company.ops.api.modules.system.repository;

import com.company.ops.api.modules.system.domain.SystemRole;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemRoleRepository extends JpaRepository<SystemRole, UUID> {

  boolean existsByCodeAndTenantId(String code, String tenantId);

  long countByPermissions_Id(UUID permissionId);

  long countByDataScopeOrganizations_Id(UUID organizationId);
}
