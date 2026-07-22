package com.company.ops.api.modules.crm.repository;

import com.company.ops.api.modules.crm.domain.Receivable;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface ReceivableRepository extends JpaRepository<Receivable, UUID> {

  List<Receivable> findByCustomerIdOrderByDueDateAsc(UUID customerId);

  List<Receivable> findAllByOrderByDueDateAsc();

  boolean existsByCode(String code);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from Receivable r where r.id = :id")
  Optional<Receivable> findByIdForUpdate(UUID id);
}
