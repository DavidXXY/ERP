package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.SupplierInvoiceSubmission;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierInvoiceSubmissionRepository
    extends JpaRepository<SupplierInvoiceSubmission, UUID> {

  List<SupplierInvoiceSubmission> findBySupplierIdOrderByCreatedAtDesc(UUID supplierId);

  List<SupplierInvoiceSubmission> findAllByOrderByCreatedAtDesc();

  List<SupplierInvoiceSubmission> findByStatusOrderByCreatedAtDesc(String status);

  List<SupplierInvoiceSubmission> findByInvoiceNoIgnoreCaseAndStatus(String invoiceNo, String status);

  long countBySupplierId(UUID supplierId);

  long countByStatus(String status);
}
