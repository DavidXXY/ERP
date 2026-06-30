package com.company.ops.api.modules.office.repository;
import com.company.ops.api.modules.office.domain.ExpenseClaim; import java.util.List; import java.util.Optional; import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
public interface ExpenseClaimRepository extends JpaRepository<ExpenseClaim, UUID> { List<ExpenseClaim> findAllByOrderByExpenseDateDescCreatedAtDesc(); boolean existsByCode(String code); Optional<ExpenseClaim> findByApprovalRequestId(UUID approvalRequestId); }
