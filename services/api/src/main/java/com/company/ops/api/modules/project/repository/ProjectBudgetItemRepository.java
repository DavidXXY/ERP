package com.company.ops.api.modules.project.repository;

import com.company.ops.api.modules.project.domain.ProjectBudgetItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectBudgetItemRepository extends JpaRepository<ProjectBudgetItem, UUID> {

  List<ProjectBudgetItem> findByProjectIdOrderByCategoryAsc(UUID projectId);
}
