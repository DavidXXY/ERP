package com.company.ops.api.modules.project.repository;

import com.company.ops.api.modules.project.domain.ProjectMilestone;
import com.company.ops.api.modules.project.domain.MilestoneStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMilestoneRepository extends JpaRepository<ProjectMilestone, UUID> {

  List<ProjectMilestone> findByProjectIdOrderBySortOrderAsc(UUID projectId);

  List<ProjectMilestone> findByProjectIdIn(java.util.Collection<UUID> projectIds);

  List<ProjectMilestone> findByPlannedDateBeforeAndStatusNot(
      LocalDate date, MilestoneStatus status);

  List<ProjectMilestone> findByPlannedDateBeforeAndStatusNotAndProjectIdIn(
      LocalDate date, MilestoneStatus status, java.util.Collection<UUID> projectIds);
}
