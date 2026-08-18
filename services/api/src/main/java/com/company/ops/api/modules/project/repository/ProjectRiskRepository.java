package com.company.ops.api.modules.project.repository;

import com.company.ops.api.modules.project.domain.ProjectRisk;
import com.company.ops.api.modules.project.domain.RiskStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRiskRepository extends JpaRepository<ProjectRisk, UUID> {

  List<ProjectRisk> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

  long countByProjectIdAndStatus(UUID projectId, RiskStatus status);
}
