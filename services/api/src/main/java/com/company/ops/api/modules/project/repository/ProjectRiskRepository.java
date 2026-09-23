package com.company.ops.api.modules.project.repository;

import com.company.ops.api.modules.project.domain.ProjectRisk;
import com.company.ops.api.modules.project.domain.RiskStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRiskRepository extends JpaRepository<ProjectRisk, UUID> {

  List<ProjectRisk> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

  long countByProjectIdAndStatus(UUID projectId, RiskStatus status);

  List<ProjectRisk> findByDueDateBeforeAndStatusNot(
      LocalDate date, RiskStatus status);

  List<ProjectRisk> findByDueDateBeforeAndStatusNotAndProjectIdIn(
      LocalDate date, RiskStatus status, java.util.Collection<UUID> projectIds);

  List<ProjectRisk> findByProjectIdIn(java.util.Collection<UUID> projectIds);
}
