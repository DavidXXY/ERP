package com.company.ops.api.modules.project.repository;

import com.company.ops.api.modules.project.domain.Project;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

  Page<Project> findAllByOrderByCreatedAtDesc(Pageable pageable);

  List<Project> findAllByOrderByCreatedAtDesc();

  boolean existsByCode(String code);
  Optional<Project> findByCode(String code);

  int countByCodeStartingWith(String prefix);

}
