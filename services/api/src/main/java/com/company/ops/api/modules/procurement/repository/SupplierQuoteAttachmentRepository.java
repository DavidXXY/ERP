package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.SupplierQuoteAttachment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierQuoteAttachmentRepository extends JpaRepository<SupplierQuoteAttachment, UUID> {
  List<SupplierQuoteAttachment> findByQuoteIdOrderByCreatedAtDesc(UUID quoteId);
}
