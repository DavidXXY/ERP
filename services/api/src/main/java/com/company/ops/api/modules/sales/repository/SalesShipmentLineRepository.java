package com.company.ops.api.modules.sales.repository;

import com.company.ops.api.modules.sales.domain.SalesShipmentLine;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesShipmentLineRepository extends JpaRepository<SalesShipmentLine, UUID> {
  List<SalesShipmentLine> findByShipmentIdOrderByCreatedAtAsc(UUID shipmentId);
  List<SalesShipmentLine> findByShipmentIdIn(Collection<UUID> shipmentIds);
}
