package com.company.ops.api.modules.project.repository;

import com.company.ops.api.modules.project.domain.ProjectMilestone;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMilestoneRepository extends JpaRepository<ProjectMilestone, UUID> {

  List<ProjectMilestone> findByProjectIdOrderBySortOrderAsc(UUID projectId);
}
