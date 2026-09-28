package com.company.ops.api.modules.sales.repository;

import com.company.ops.api.modules.sales.domain.SalesOrderLine;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesOrderLineRepository extends JpaRepository<SalesOrderLine, UUID> {
  List<SalesOrderLine> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
  List<SalesOrderLine> findByOrderIdIn(Collection<UUID> orderIds);
}
