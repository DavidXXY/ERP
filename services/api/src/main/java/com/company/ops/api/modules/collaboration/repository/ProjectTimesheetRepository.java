package com.company.ops.api.modules.collaboration.repository;
import com.company.ops.api.modules.collaboration.domain.ProjectTimesheet;
import java.time.LocalDate; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface ProjectTimesheetRepository extends JpaRepository<ProjectTimesheet,UUID>{
  List<ProjectTimesheet> findAllByOrderByWorkDateDesc();
  List<ProjectTimesheet> findByAssignmentIdOrderByWorkDateDesc(UUID assignmentId);
  List<ProjectTimesheet> findByAssignmentIdAndStatus(UUID assignmentId,String status);
  List<ProjectTimesheet> findByUserIdAndWorkDate(UUID userId,LocalDate workDate);
}
