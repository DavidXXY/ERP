package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.SupplierInvoicePayable;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierInvoicePayableRepository
    extends JpaRepository<SupplierInvoicePayable, UUID> {

  List<SupplierInvoicePayable> findByInvoiceId(UUID invoiceId);

  List<SupplierInvoicePayable> findByPayableId(UUID payableId);

  List<SupplierInvoicePayable> findByPayableIdIn(java.util.Collection<UUID> payableIds);
}
