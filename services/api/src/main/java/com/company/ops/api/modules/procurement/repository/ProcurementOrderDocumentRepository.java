package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.ProcurementOrderDocument;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcurementOrderDocumentRepository
    extends JpaRepository<ProcurementOrderDocument, UUID> {
  List<ProcurementOrderDocument> findByOrderIdOrderByCreatedAtDesc(UUID orderId);
  List<ProcurementOrderDocument> findByOrderIdInOrderByCreatedAtDesc(Collection<UUID> orderIds);
}
