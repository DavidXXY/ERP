package com.company.ops.api.modules.procurement.repository;
import com.company.ops.api.modules.procurement.domain.SupplierQuotation; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface SupplierQuotationRepository extends JpaRepository<SupplierQuotation,UUID>{List<SupplierQuotation> findByInquiryIdOrderByUnitPriceAsc(UUID id);}
