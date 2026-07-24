package com.company.ops.api.modules.collaboration.repository;
import com.company.ops.api.modules.collaboration.domain.ProjectStaffAssignment;
import java.time.LocalDate; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface ProjectStaffAssignmentRepository extends JpaRepository<ProjectStaffAssignment,UUID>{
  List<ProjectStaffAssignment> findAllByOrderByCreatedAtDesc();
  List<ProjectStaffAssignment> findByProjectId(UUID projectId);
  List<ProjectStaffAssignment> findByProjectIdIn(Collection<UUID> projectIds);
  List<ProjectStaffAssignment> findByUserId(UUID userId);
  List<ProjectStaffAssignment> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(
      LocalDate endDate, LocalDate startDate);
  boolean existsByProjectIdAndUserIdAndRoleName(UUID projectId,UUID userId,String roleName);
}
