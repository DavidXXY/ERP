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
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentApplicationRepository extends JpaRepository<PaymentApplication, UUID> {

  List<PaymentApplication> findAllByOrderByCreatedAtDesc();
  Page<PaymentApplication> findAllByOrderByCreatedAtDesc(Pageable pageable);

  List<PaymentApplication> findByPayableIdAndStatusIn(
      UUID payableId,
      Collection<PaymentApplicationStatus> statuses
  );

  long countByStatus(PaymentApplicationStatus status);

  @Query("select application.payableId, coalesce(sum(application.requestedAmount), 0) "
      + "from PaymentApplication application where application.status in :statuses "
      + "group by application.payableId")
  List<Object[]> aggregateRequestedAmountByPayableAndStatusIn(
      @Param("statuses") Collection<PaymentApplicationStatus> statuses);

  @Query("select application.payableId, coalesce(sum(application.requestedAmount), 0) "
      + "from PaymentApplication application where application.payableId in :payableIds "
      + "and application.status in :statuses group by application.payableId")
  List<Object[]> aggregateRequestedAmountByPayableIdInAndStatusIn(
      @Param("payableIds") Collection<UUID> payableIds,
      @Param("statuses") Collection<PaymentApplicationStatus> statuses);

  @Query("select coalesce(sum(application.requestedAmount), 0) from PaymentApplication application "
      + "where application.payableId = :payableId and application.status in :statuses")
  BigDecimal sumRequestedAmountByPayableAndStatusIn(
      @Param("payableId") UUID payableId,
      @Param("statuses") Collection<PaymentApplicationStatus> statuses);

  boolean existsByCode(String code);

  List<PaymentApplication> findByPayableIdIn(Collection<UUID> payableIds);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select application from PaymentApplication application where application.id = :id")
  Optional<PaymentApplication> findByIdForUpdate(@Param("id") UUID id);
}
