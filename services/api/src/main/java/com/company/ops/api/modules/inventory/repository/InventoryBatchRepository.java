package com.company.ops.api.modules.inventory.repository;

import com.company.ops.api.modules.inventory.domain.InventoryBatch;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, UUID> {
  List<InventoryBatch> findByPartIdOrderByReceivedDateAscCreatedAtAsc(UUID partId);
  Optional<InventoryBatch> findByPartIdAndBatchNo(UUID partId, String batchNo);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select b from InventoryBatch b where b.id = :id")
  Optional<InventoryBatch> findByIdForUpdate(@Param("id") UUID id);
}
