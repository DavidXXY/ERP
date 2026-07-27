package com.company.ops.api.modules.inventory.repository;

import com.company.ops.api.modules.inventory.domain.InventoryPart;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryPartRepository extends JpaRepository<InventoryPart, UUID> {

  List<InventoryPart> findAllByOrderByCreatedAtDesc();

  Page<InventoryPart> findAllByOrderByCreatedAtDesc(Pageable pageable);

  @Query("select part from InventoryPart part where part.id not in :hiddenIds order by part.createdAt desc")
  List<InventoryPart> findAllVisible(@Param("hiddenIds") Set<UUID> hiddenIds);

  @Query("select part from InventoryPart part where part.id not in :hiddenIds order by part.createdAt desc")
  Page<InventoryPart> findAllVisible(@Param("hiddenIds") Set<UUID> hiddenIds, Pageable pageable);

  boolean existsByCode(String code);

  Optional<InventoryPart> findByCodeIgnoreCase(String code);

  List<InventoryPart> findByNameIgnoreCase(String name);

  List<InventoryPart> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(String code,String name,Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select part from InventoryPart part where part.id = :id")
  Optional<InventoryPart> findByIdForUpdate(@Param("id") UUID id);

  @Query("select coalesce(sum(p.stockQty * p.unitCost), 0) from InventoryPart p")
  BigDecimal sumInventoryValue();

  @Query("select count(p) from InventoryPart p where p.stockQty < p.safetyQty")
  long countLowStock();

  @Query("select p from InventoryPart p where p.stockQty < p.safetyQty order by p.createdAt desc")
  List<InventoryPart> findLowStock();
}
