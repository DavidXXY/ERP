package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.ProcurementCostAllocation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcurementCostAllocationRepository extends JpaRepository<ProcurementCostAllocation, UUID> {

  List<ProcurementCostAllocation> findAllByOrderByIncurredDateDescCreatedAtDesc();
}
