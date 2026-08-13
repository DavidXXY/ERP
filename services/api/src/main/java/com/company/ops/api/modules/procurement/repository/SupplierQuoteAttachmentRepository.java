package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.SupplierQuoteAttachment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierQuoteAttachmentRepository extends JpaRepository<SupplierQuoteAttachment, UUID> {
  List<SupplierQuoteAttachment> findByQuoteIdOrderByCreatedAtDesc(UUID quoteId);

  long countByQuoteId(UUID quoteId);

  @Query("select coalesce(sum(a.sizeBytes), 0) from SupplierQuoteAttachment a "
      + "where a.quoteId = :quoteId")
  long sumSizeByQuoteId(@Param("quoteId") UUID quoteId);
}
