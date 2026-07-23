package com.company.ops.api.modules.collaboration.repository;
import com.company.ops.api.modules.collaboration.domain.ProjectHandover;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface ProjectHandoverRepository extends JpaRepository<ProjectHandover,UUID>{
  List<ProjectHandover> findAllByOrderByCreatedAtDesc();
  Optional<ProjectHandover> findByProjectId(UUID projectId);
  boolean existsByProjectId(UUID projectId);
}
