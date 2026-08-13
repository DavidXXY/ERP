package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.PurchaseOrderChange;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderChangeRepository extends JpaRepository<PurchaseOrderChange, UUID> {
  List<PurchaseOrderChange> findByOrderIdOrderByCreatedAtDesc(UUID orderId);
  List<PurchaseOrderChange> findByOrderIdInOrderByCreatedAtDesc(Collection<UUID> orderIds);
  long countByOrderId(UUID orderId);

  long countByStatusAndSupplierResponseIsNull(String status);

  long countByStatusAndSupplierResponseIsNotNull(String status);
}
