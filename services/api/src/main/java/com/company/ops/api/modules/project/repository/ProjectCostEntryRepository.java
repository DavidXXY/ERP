package com.company.ops.api.modules.project.repository;

import com.company.ops.api.modules.project.domain.ProjectCostEntry;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;

public interface ProjectCostEntryRepository extends JpaRepository<ProjectCostEntry, UUID> {

  List<ProjectCostEntry> findByProjectIdOrderByIncurredDateDescCreatedAtDesc(UUID projectId);
  Optional<ProjectCostEntry> findBySourceNo(String sourceNo);
  List<ProjectCostEntry> findByProjectIdIn(java.util.Collection<UUID> projectIds);

  @Query("select coalesce(sum(c.amount), 0) from ProjectCostEntry c where c.projectId = :projectId")
  BigDecimal sumAmountByProjectId(@Param("projectId") UUID projectId);
}
