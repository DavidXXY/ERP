package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.ProcurementShipment;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcurementShipmentRepository
    extends JpaRepository<ProcurementShipment, UUID> {

  List<ProcurementShipment> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

  List<ProcurementShipment> findByOrderIdInOrderByCreatedAtDesc(Collection<UUID> orderIds);

  List<ProcurementShipment> findBySupplierIdOrderByCreatedAtDesc(UUID supplierId);

  List<ProcurementShipment> findByOrderIdAndDeliveryNo(UUID orderId, String deliveryNo);

  long countByOrderId(UUID orderId);
}
