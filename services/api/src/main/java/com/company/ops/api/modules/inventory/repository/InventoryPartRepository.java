package com.company.ops.api.modules.inventory.repository;

import com.company.ops.api.modules.inventory.domain.InventoryPart;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

public interface InventoryPartRepository extends JpaRepository<InventoryPart, UUID> {

  List<InventoryPart> findAllByOrderByCreatedAtDesc();

  boolean existsByCode(String code);

  Optional<InventoryPart> findByCodeIgnoreCase(String code);
  List<InventoryPart> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(String code,String name,Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select part from InventoryPart part where part.id = :id")
  Optional<InventoryPart> findByIdForUpdate(@Param("id") UUID id);

  @Query("select coalesce(sum(p.stockQty * p.unitCost), 0) from InventoryPart p")
  BigDecimal sumInventoryValue();

  @Query("select count(p) from InventoryPart p where p.stockQty < p.safetyQty")
  long countLowStock();

}
