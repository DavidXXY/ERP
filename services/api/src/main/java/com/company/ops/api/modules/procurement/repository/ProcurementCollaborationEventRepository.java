package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.ProcurementCollaborationEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcurementCollaborationEventRepository
    extends JpaRepository<ProcurementCollaborationEvent, UUID> {
  List<ProcurementCollaborationEvent> findAllByOrderByEventDateDescCreatedAtDesc();
  List<ProcurementCollaborationEvent> findByOrderIdOrderByEventDateDesc(UUID orderId);
}
