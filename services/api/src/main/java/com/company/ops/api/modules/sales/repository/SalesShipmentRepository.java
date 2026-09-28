package com.company.ops.api.modules.sales.repository;

import com.company.ops.api.modules.sales.domain.SalesShipment;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesShipmentRepository extends JpaRepository<SalesShipment, UUID> {
  Page<SalesShipment> findAllByOrderByCreatedAtDesc(Pageable pageable);
  List<SalesShipment> findByOrderIdIn(Collection<UUID> orderIds);
  boolean existsByCode(String code);
}
