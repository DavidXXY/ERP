package com.company.ops.api.modules.finance.repository;

import com.company.ops.api.modules.finance.domain.PaymentApplication;
import com.company.ops.api.modules.finance.domain.PaymentApplicationStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentApplicationRepository extends JpaRepository<PaymentApplication, UUID> {

  List<PaymentApplication> findAllByOrderByCreatedAtDesc();

  List<PaymentApplication> findByPayableIdAndStatusIn(
      UUID payableId,
      Collection<PaymentApplicationStatus> statuses
  );

  boolean existsByCode(String code);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select application from PaymentApplication application where application.id = :id")
  Optional<PaymentApplication> findByIdForUpdate(@Param("id") UUID id);
}
