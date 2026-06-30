package com.company.ops.api.modules.inventory.repository;

import com.company.ops.api.modules.inventory.domain.InventoryReturnLine;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReturnLineRepository extends JpaRepository<InventoryReturnLine, UUID> {

  List<InventoryReturnLine> findByReturnIdOrderByCreatedAtAsc(UUID returnId);

  List<InventoryReturnLine> findByReturnIdIn(List<UUID> returnIds);
}
