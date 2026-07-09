package com.company.ops.api.modules.inventory.repository;

import com.company.ops.api.modules.inventory.domain.StockMovement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementRepository extends JpaRepository<StockMovement, UUID> {
  List<StockMovement> findAllByOrderByCreatedAtDesc();
  Optional<StockMovement> findByCode(String code);
}
