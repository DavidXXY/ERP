package com.company.ops.api.modules.inventory.repository;

import com.company.ops.api.modules.inventory.domain.StockMovement;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

  List<StockMovement> findByPartIdOrderByCreatedAtDesc(UUID partId);
}
