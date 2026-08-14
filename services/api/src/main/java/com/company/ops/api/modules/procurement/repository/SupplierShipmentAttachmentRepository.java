package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.SupplierShipmentAttachment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierShipmentAttachmentRepository
    extends JpaRepository<SupplierShipmentAttachment, UUID> {
  List<SupplierShipmentAttachment> findByShipmentIdOrderByCreatedAtDesc(UUID shipmentId);

  long countByShipmentId(UUID shipmentId);

  @Query("select coalesce(sum(a.sizeBytes), 0) from SupplierShipmentAttachment a "
      + "where a.shipmentId = :shipmentId")
  long sumSizeByShipmentId(@Param("shipmentId") UUID shipmentId);
}
