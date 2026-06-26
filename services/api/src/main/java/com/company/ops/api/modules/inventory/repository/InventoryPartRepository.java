package com.company.ops.api.modules.inventory.repository;

import com.company.ops.api.modules.inventory.domain.InventoryPart;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryPartRepository extends JpaRepository<InventoryPart, UUID> {

  List<InventoryPart> findAllByOrderByCreatedAtDesc();

  boolean existsByCode(String code);
}
