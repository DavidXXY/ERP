package com.company.ops.api.modules.inventory.repository;

import com.company.ops.api.modules.inventory.domain.InventoryStocktakeLine;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryStocktakeLineRepository extends JpaRepository<InventoryStocktakeLine, UUID> {
  List<InventoryStocktakeLine> findByStocktakeIdOrderByCreatedAtAsc(UUID stocktakeId);
  List<InventoryStocktakeLine> findByStocktakeIdIn(Collection<UUID> stocktakeIds);
}
