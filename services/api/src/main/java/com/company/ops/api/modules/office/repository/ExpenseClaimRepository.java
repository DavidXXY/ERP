package com.company.ops.api.modules.office.repository;
import com.company.ops.api.modules.office.domain.ExpenseClaim;
import java.math.BigDecimal; import java.util.List; import java.util.Optional; import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository; import org.springframework.data.jpa.repository.Query; import org.springframework.data.repository.query.Param;
public interface ExpenseClaimRepository extends JpaRepository<ExpenseClaim, UUID> {
  List<ExpenseClaim> findAllByOrderByExpenseDateDescCreatedAtDesc(); boolean existsByCode(String code);
  Optional<ExpenseClaim> findByApprovalRequestId(UUID approvalRequestId);
  @Query("SELECT COALESCE(SUM(e.amount), 0) FROM ExpenseClaim e WHERE e.status = :status") BigDecimal sumAmountByStatus(@Param("status") String status);
}
