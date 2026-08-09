package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.PurchaseOrderChange;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderChangeRepository extends JpaRepository<PurchaseOrderChange, UUID> {
  List<PurchaseOrderChange> findByOrderIdOrderByCreatedAtDesc(UUID orderId);
  long countByOrderId(UUID orderId);
}
