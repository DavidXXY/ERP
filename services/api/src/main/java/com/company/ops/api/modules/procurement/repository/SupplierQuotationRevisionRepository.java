package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.SupplierQuotationRevision;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierQuotationRevisionRepository extends JpaRepository<SupplierQuotationRevision, UUID> {
  List<SupplierQuotationRevision> findByQuoteIdOrderByVersionNoDesc(UUID quoteId);
}
