package com.company.ops.api.modules.procurement.repository;
import com.company.ops.api.modules.procurement.domain.SupplierInvoice; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface SupplierInvoiceRepository extends JpaRepository<SupplierInvoice,UUID>{List<SupplierInvoice> findAllByOrderByInvoiceDateDesc(); List<SupplierInvoice> findByOrderId(UUID id); boolean existsByInvoiceNo(String no);}
