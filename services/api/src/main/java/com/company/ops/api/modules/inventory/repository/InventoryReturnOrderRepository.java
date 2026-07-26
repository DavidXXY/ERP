package com.company.ops.api.modules.inventory.repository;

import com.company.ops.api.modules.inventory.domain.InventoryReturnOrder;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryReturnOrderRepository extends JpaRepository<InventoryReturnOrder, UUID> {

  List<InventoryReturnOrder> findAllByOrderByReturnDateDescCreatedAtDesc();
  Page<InventoryReturnOrder> findAllByOrderByReturnDateDescCreatedAtDesc(Pageable pageable);

  boolean existsByCode(String code);
}
