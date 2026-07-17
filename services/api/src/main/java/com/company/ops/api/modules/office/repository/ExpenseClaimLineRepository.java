package com.company.ops.api.modules.office.repository;

import com.company.ops.api.modules.office.domain.ExpenseClaimLine;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseClaimLineRepository extends JpaRepository<ExpenseClaimLine, UUID> {
  List<ExpenseClaimLine> findByExpenseIdOrderByLineNoAsc(UUID expenseId);
  List<ExpenseClaimLine> findByExpenseIdInOrderByExpenseIdAscLineNoAsc(Collection<UUID> expenseIds);
  void deleteByExpenseId(UUID expenseId);
}
