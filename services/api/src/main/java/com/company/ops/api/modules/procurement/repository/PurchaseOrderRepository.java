package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.PurchaseOrder;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {

  List<PurchaseOrder> findAllByOrderByCreatedAtDesc();

  boolean existsByCode(String code);

  boolean existsByRequestId(UUID requestId);
}
