package com.company.ops.api.modules.project.repository;

import com.company.ops.api.modules.project.domain.Project;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

  List<Project> findAllByOrderByCreatedAtDesc();

  boolean existsByCode(String code);
}
