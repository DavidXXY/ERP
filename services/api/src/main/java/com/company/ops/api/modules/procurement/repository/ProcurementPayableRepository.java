package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.ProcurementPayable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcurementPayableRepository extends JpaRepository<ProcurementPayable, UUID> {

  List<ProcurementPayable> findAllByOrderByDueDateAsc();
  List<ProcurementPayable> findByOrderId(UUID orderId);
  Optional<ProcurementPayable> findByReceiptId(UUID receiptId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select payable from ProcurementPayable payable where payable.id = :id")
  Optional<ProcurementPayable> findByIdForUpdate(@Param("id") UUID id);
}
