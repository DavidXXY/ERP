package com.company.ops.api.modules.sales.repository;

import com.company.ops.api.modules.sales.domain.SalesReturnOrder;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesReturnOrderRepository extends JpaRepository<SalesReturnOrder, UUID> {
  Page<SalesReturnOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);
  List<SalesReturnOrder> findByShipmentIdIn(Collection<UUID> shipmentIds);
  boolean existsByCode(String code);
}
