package com.company.ops.api.modules.finance.repository;

import com.company.ops.api.modules.finance.domain.IntercompanyTransaction;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntercompanyTransactionRepository extends JpaRepository<IntercompanyTransaction, UUID> {
  Page<IntercompanyTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);
  boolean existsByCode(String code);
}