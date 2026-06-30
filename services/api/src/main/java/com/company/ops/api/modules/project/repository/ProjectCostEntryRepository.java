package com.company.ops.api.modules.project.repository;

import com.company.ops.api.modules.project.domain.ProjectCostEntry;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectCostEntryRepository extends JpaRepository<ProjectCostEntry, UUID> {

  List<ProjectCostEntry> findByProjectIdOrderByIncurredDateDescCreatedAtDesc(UUID projectId);
}
