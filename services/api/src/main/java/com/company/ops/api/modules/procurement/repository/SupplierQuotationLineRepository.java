package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.SupplierQuotationLine;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierQuotationLineRepository
    extends JpaRepository<SupplierQuotationLine, UUID> {

  List<SupplierQuotationLine> findByQuoteIdOrderByCreatedAtAsc(UUID quoteId);

  Optional<SupplierQuotationLine> findByQuoteIdAndRequestId(UUID quoteId, UUID requestId);
}
