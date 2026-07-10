package com.company.ops.api.modules.inventory.repository;

import com.company.ops.api.modules.inventory.domain.StockMovement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryMovementRepository extends JpaRepository<StockMovement, UUID> {
  List<StockMovement> findAllByOrderByCreatedAtDesc();

  @Query("SELECT m FROM StockMovement m WHERE m.sourceNo = :code")
  Optional<StockMovement> findByCode(@Param("code") String code);
}
