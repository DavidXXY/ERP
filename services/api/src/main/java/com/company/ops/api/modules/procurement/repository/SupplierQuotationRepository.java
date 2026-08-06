package com.company.ops.api.modules.procurement.repository;
import com.company.ops.api.modules.procurement.domain.SupplierQuotation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierQuotationRepository extends JpaRepository<SupplierQuotation, UUID> {
  List<SupplierQuotation> findByInquiryIdOrderByUnitPriceAsc(UUID id);
  Optional<SupplierQuotation> findByInquiryIdAndSupplierId(UUID inquiryId, UUID supplierId);
}
