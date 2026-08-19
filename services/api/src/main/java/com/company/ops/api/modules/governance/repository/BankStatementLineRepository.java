package com.company.ops.api.modules.governance.repository;

import com.company.ops.api.modules.governance.domain.BankStatementLine;
import com.company.ops.api.modules.governance.domain.ReconciliationStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BankStatementLineRepository extends JpaRepository<BankStatementLine, UUID> {
  List<BankStatementLine> findAllByOrderByTransactionDateDescCreatedAtDesc();
  Page<BankStatementLine> findAllByOrderByTransactionDateDescCreatedAtDesc(Pageable pageable);
  Page<BankStatementLine> findByReconciliationStatusOrderByTransactionDateDescCreatedAtDesc(ReconciliationStatus status, Pageable pageable);
  Optional<BankStatementLine> findByAccountNoMaskedAndBankReference(String accountNoMasked, String bankReference);
  boolean existsByMatchedBizTypeAndMatchedBizIdAndReconciliationStatus(String type, UUID id, ReconciliationStatus status);
  long countByReconciliationStatusNotAndTransactionDateLessThanEqual(ReconciliationStatus status, LocalDate date);
  long countByReconciliationStatus(ReconciliationStatus status);
}
