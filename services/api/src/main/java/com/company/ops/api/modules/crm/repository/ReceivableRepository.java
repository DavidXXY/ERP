package com.company.ops.api.modules.crm.repository;

import com.company.ops.api.modules.crm.domain.Receivable;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.time.LocalDate;
import java.util.Collection;
import java.math.BigDecimal;

public interface ReceivableRepository extends JpaRepository<Receivable, UUID> {

  List<Receivable> findByCustomerIdOrderByDueDateAsc(UUID customerId);

  List<Receivable> findAllByOrderByDueDateAsc();
  List<Receivable> findByDueDateBetweenOrderByDueDateAsc(LocalDate startDate,LocalDate endDate);

  boolean existsByCode(String code);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from Receivable r where r.id = :id")
  Optional<Receivable> findByIdForUpdate(UUID id);

  List<Receivable> findByContractIdIn(Collection<UUID> contractIds);

  @Query("select r from Receivable r where r.dueDate < :today and r.amount > r.settledAmount")
  List<Receivable> findOverdueOutstanding(LocalDate today);

  @Query("select coalesce(sum(r.settledAmount), 0) from Receivable r")
  BigDecimal sumSettledAmount();

  @Query("select coalesce(sum(r.amount - r.settledAmount), 0) from Receivable r")
  BigDecimal sumOutstandingAmount();

  @Query("select r.customerId, coalesce(sum(r.settledAmount), 0) from Receivable r group by r.customerId")
  List<Object[]> aggregateSettledByCustomer();
}
