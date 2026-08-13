package com.company.ops.api.modules.procurement.repository;
import com.company.ops.api.modules.procurement.domain.SupplierInvoice;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface SupplierInvoiceRepository extends JpaRepository<SupplierInvoice,UUID>{
  List<SupplierInvoice> findAllByOrderByInvoiceDateDesc();
  List<SupplierInvoice> findBySupplierIdOrderByCreatedAtDesc(UUID supplierId);
  List<SupplierInvoice> findByOrderId(UUID id);
  List<SupplierInvoice> findByOrderIdIn(Collection<UUID> orderIds);
  List<SupplierInvoice> findByMatchStatusNot(String matchStatus);
  List<SupplierInvoice> findByPayableId(UUID id);
  boolean existsByInvoiceNo(String no);
  Optional<SupplierInvoice> findByClientRequestId(String clientRequestId);

  @Query("select coalesce(count(i), 0) as invoiceCount, coalesce(sum(i.amount), 0) as invoiceAmount "
      + "from SupplierInvoice i where i.supplierId = :supplierId")
  InvoiceSupplierTotals aggregateBySupplier(@Param("supplierId") UUID supplierId);

  interface InvoiceSupplierTotals {
    Long getInvoiceCount();
    BigDecimal getInvoiceAmount();
  }
}
