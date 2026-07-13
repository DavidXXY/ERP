package com.company.ops.api.modules.system.repository;

import com.company.ops.api.modules.system.domain.SystemRole;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SystemRoleRepository extends JpaRepository<SystemRole, UUID> {

  boolean existsByCodeAndTenantId(String code, String tenantId);

  @EntityGraph(attributePaths = "permissions")
  java.util.Optional<com.company.ops.api.modules.system.domain.SystemRole> findByCodeAndTenantId(String code, String tenantId);

  @Query("""
      select distinct r
      from SystemRole r
      left join fetch r.permissions
      where r.code = :code and r.tenantId = :tenantId
      """)
  java.util.Optional<SystemRole> findByCodeAndTenantIdWithPermissions(
      @Param("code") String code,
      @Param("tenantId") String tenantId
  );

  long countByPermissions_Id(UUID permissionId);

  long countByDataScopeOrganizations_Id(UUID organizationId);
}
