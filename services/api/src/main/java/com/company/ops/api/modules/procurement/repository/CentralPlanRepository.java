package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.CentralPlan;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CentralPlanRepository extends JpaRepository<CentralPlan, UUID> {
  List<CentralPlan> findAllByOrderByPeriodYearDescCreatedAtDesc();
  boolean existsByCode(String code);
}
