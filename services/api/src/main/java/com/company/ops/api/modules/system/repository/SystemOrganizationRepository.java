package com.company.ops.api.modules.system.repository;

import com.company.ops.api.modules.system.domain.SystemOrganization;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemOrganizationRepository extends JpaRepository<SystemOrganization, UUID> {

  List<SystemOrganization> findByTenantIdOrderBySortOrderAsc(String tenantId);

  List<SystemOrganization> findByParentIsNullAndTenantIdOrderBySortOrderAsc(String tenantId);

  @Query("SELECT o FROM SystemOrganization o LEFT JOIN FETCH o.children WHERE o.parent IS NULL AND o.tenantId = :tenantId ORDER BY o.sortOrder ASC")
  List<SystemOrganization> findTreeByTenantId(String tenantId);

  boolean existsByCodeAndTenantId(String code, String tenantId);

  SystemOrganization findByCodeAndTenantId(String code, String tenantId);
}