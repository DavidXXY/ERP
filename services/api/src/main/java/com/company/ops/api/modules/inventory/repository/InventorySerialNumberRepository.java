package com.company.ops.api.modules.inventory.repository;

import com.company.ops.api.modules.inventory.domain.InventorySerialNumber;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventorySerialNumberRepository extends JpaRepository<InventorySerialNumber, UUID> {
  List<InventorySerialNumber> findByPartIdOrderBySerialNoAsc(UUID partId);
  boolean existsByPartIdAndSerialNo(UUID partId, String serialNo);
}
