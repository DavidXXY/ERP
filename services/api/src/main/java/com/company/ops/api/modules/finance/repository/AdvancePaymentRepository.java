package com.company.ops.api.modules.finance.repository;

import com.company.ops.api.modules.finance.domain.AdvancePayment;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdvancePaymentRepository extends JpaRepository<AdvancePayment, UUID> {
  Page<AdvancePayment> findAllByOrderByCreatedAtDesc(Pageable pageable);
  boolean existsByCode(String code);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from AdvancePayment a where a.id = :id")
  Optional<AdvancePayment> findByIdForUpdate(@Param("id") UUID id);
}
