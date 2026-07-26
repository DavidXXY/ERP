package com.company.ops.api.modules.maintenance.repository;

import com.company.ops.api.modules.maintenance.domain.WorkOrderAttachment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkOrderAttachmentRepository extends JpaRepository<WorkOrderAttachment, UUID> {
  List<WorkOrderAttachment> findByWorkOrderIdOrderByCreatedAtAsc(UUID workOrderId);
}
