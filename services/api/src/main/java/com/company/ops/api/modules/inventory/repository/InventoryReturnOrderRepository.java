package com.company.ops.api.modules.inventory.repository;

import com.company.ops.api.modules.inventory.domain.InventoryReturnOrder;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReturnOrderRepository extends JpaRepository<InventoryReturnOrder, UUID> {

  List<InventoryReturnOrder> findAllByOrderByReturnDateDescCreatedAtDesc();

  boolean existsByCode(String code);
}
