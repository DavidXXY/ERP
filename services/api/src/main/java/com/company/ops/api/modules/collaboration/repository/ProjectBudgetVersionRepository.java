package com.company.ops.api.modules.collaboration.repository;
import com.company.ops.api.modules.collaboration.domain.ProjectBudgetVersion;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface ProjectBudgetVersionRepository extends JpaRepository<ProjectBudgetVersion,UUID>{
  List<ProjectBudgetVersion> findAllByOrderByCreatedAtDesc();
  List<ProjectBudgetVersion> findByProjectIdOrderByVersionNoDesc(UUID projectId);
  Optional<ProjectBudgetVersion> findFirstByProjectIdOrderByVersionNoDesc(UUID projectId);
  boolean existsByProjectIdAndStatus(UUID projectId,String status);
}
