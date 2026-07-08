package com.company.ops.api.modules.inventory.repository;

import com.company.ops.api.modules.inventory.domain.InventoryPart;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryPartRepository extends JpaRepository<InventoryPart, UUID> {

  List<InventoryPart> findAllByOrderByCreatedAtDesc();

  boolean existsByCode(String code);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select part from InventoryPart part where part.id = :id")
  Optional<InventoryPart> findByIdForUpdate(@Param("id") UUID id);

  List<com.company.ops.api.modules.inventory.domain.InventoryPart> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(String code, String name);
}
