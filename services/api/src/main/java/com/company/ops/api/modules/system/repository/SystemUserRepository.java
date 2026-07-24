package com.company.ops.api.modules.system.repository;

import com.company.ops.api.modules.system.domain.SystemUser;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SystemUserRepository extends JpaRepository<SystemUser, UUID> {

  @EntityGraph(attributePaths = {"roles", "roles.permissions", "roles.dataScopeOrganizations"})
  Optional<SystemUser> findByUsername(String username);

  @EntityGraph(attributePaths = {"roles", "roles.permissions", "roles.dataScopeOrganizations"})
  @Query("select u from SystemUser u where u.id = :id")
  Optional<SystemUser> findDetailById(@Param("id") UUID id);

  List<SystemUser> findByOrganization_Id(UUID organizationId);

  List<SystemUser> findByOrganization_IdIn(Set<UUID> organizationIds);
  List<SystemUser> findByDisplayNameAndEnabledTrue(String displayName);
  List<SystemUser> findByEnabledTrueOrderByDisplayNameAsc();

  @Query("select distinct u from SystemUser u join u.roles r where r.code = :roleCode and u.enabled = true order by u.displayName asc")
  List<SystemUser> findEnabledByRoleCode(@Param("roleCode") String roleCode);

  long countByOrganization_Id(UUID organizationId);

  long countByRoles_Id(UUID roleId);

  boolean existsByUsername(String username);

  @Query(
      value = """
          select distinct u from SystemUser u
          left join u.roles r
          where (:keyword = ''
            or lower(u.username) like lower(concat('%', :keyword, '%'))
            or lower(u.displayName) like lower(concat('%', :keyword, '%'))
            or lower(coalesce(u.phone, '')) like lower(concat('%', :keyword, '%'))
            or lower(coalesce(u.email, '')) like lower(concat('%', :keyword, '%')))
          and (:enabled is null or u.enabled = :enabled)
          and (:roleId is null or r.id = :roleId)
          """,
      countQuery = """
          select count(distinct u.id) from SystemUser u
          left join u.roles r
          where (:keyword = ''
            or lower(u.username) like lower(concat('%', :keyword, '%'))
            or lower(u.displayName) like lower(concat('%', :keyword, '%'))
            or lower(coalesce(u.phone, '')) like lower(concat('%', :keyword, '%'))
            or lower(coalesce(u.email, '')) like lower(concat('%', :keyword, '%')))
          and (:enabled is null or u.enabled = :enabled)
          and (:roleId is null or r.id = :roleId)
          """
  )
  Page<SystemUser> search(
      @Param("keyword") String keyword,
      @Param("enabled") Boolean enabled,
      @Param("roleId") UUID roleId,
      Pageable pageable
  );
}
