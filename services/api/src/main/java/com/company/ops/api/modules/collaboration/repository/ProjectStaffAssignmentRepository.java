package com.company.ops.api.modules.collaboration.repository;
import com.company.ops.api.modules.collaboration.domain.ProjectStaffAssignment;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface ProjectStaffAssignmentRepository extends JpaRepository<ProjectStaffAssignment,UUID>{
  List<ProjectStaffAssignment> findAllByOrderByCreatedAtDesc();
  List<ProjectStaffAssignment> findByProjectId(UUID projectId);
  List<ProjectStaffAssignment> findByUserId(UUID userId);
  boolean existsByProjectIdAndUserIdAndRoleName(UUID projectId,UUID userId,String roleName);
}
