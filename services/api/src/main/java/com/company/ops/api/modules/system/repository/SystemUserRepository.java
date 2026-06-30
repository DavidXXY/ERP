package com.company.ops.api.modules.system.repository;

import com.company.ops.api.modules.system.domain.SystemUser;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SystemUserRepository extends JpaRepository<SystemUser, UUID> {

  @EntityGraph(attributePaths = {"roles", "roles.permissions"})
  Optional<SystemUser> findByUsername(String username);

  @EntityGraph(attributePaths = {"roles", "roles.permissions"})
  @Query("select u from SystemUser u where u.id = :id")
  Optional<SystemUser> findDetailById(@Param("id") UUID id);

  List<SystemUser> findByOrganization_Id(UUID organizationId);
}
