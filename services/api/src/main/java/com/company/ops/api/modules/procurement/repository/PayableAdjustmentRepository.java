package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.PayableAdjustment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayableAdjustmentRepository extends JpaRepository<PayableAdjustment, UUID> {

  List<PayableAdjustment> findByPayableIdOrderByAppliedAtAscCreatedAtAsc(UUID payableId);

  List<PayableAdjustment> findByOrderIdOrderByAppliedAtAsc(UUID orderId);

  boolean existsByCode(String code);
}
