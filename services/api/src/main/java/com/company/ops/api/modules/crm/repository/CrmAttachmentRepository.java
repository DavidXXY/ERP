package com.company.ops.api.modules.crm.repository;

import com.company.ops.api.modules.crm.domain.CrmAttachment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmAttachmentRepository extends JpaRepository<CrmAttachment, UUID> {
  List<CrmAttachment> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, UUID entityId);
  List<CrmAttachment> findByEntityTypeAndEntityIdAndAttachmentType(String entityType, UUID entityId, String attachmentType);
}
