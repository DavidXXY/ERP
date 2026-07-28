package com.company.ops.api.modules.collaboration.repository;

import com.company.ops.api.modules.collaboration.domain.ProjectBudgetVersionItem;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectBudgetVersionItemRepository extends JpaRepository<ProjectBudgetVersionItem, UUID> {
  List<ProjectBudgetVersionItem> findByBudgetVersionIdOrderByCategoryAsc(UUID budgetVersionId);
  List<ProjectBudgetVersionItem> findByBudgetVersionIdIn(Collection<UUID> budgetVersionIds);
}
