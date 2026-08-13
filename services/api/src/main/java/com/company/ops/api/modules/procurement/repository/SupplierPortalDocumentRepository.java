package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.SupplierPortalDocument;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierPortalDocumentRepository extends JpaRepository<SupplierPortalDocument, UUID> {
  List<SupplierPortalDocument> findBySupplierIdOrderByCreatedAtDesc(UUID supplierId);

  long countBySupplierId(UUID supplierId);

  long countByReviewStatus(String reviewStatus);

  @Query("select coalesce(sum(d.sizeBytes), 0) from SupplierPortalDocument d "
      + "where d.supplierId = :supplierId")
  long sumSizeBySupplierId(@Param("supplierId") UUID supplierId);
}
