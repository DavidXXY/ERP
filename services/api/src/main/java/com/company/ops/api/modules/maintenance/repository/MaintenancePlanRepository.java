package com.company.ops.api.modules.maintenance.repository;

import com.company.ops.api.modules.maintenance.domain.MaintenancePlan;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenancePlanRepository extends JpaRepository<MaintenancePlan, UUID> {
  List<MaintenancePlan> findAllByOrderByNextDueDateAsc();
  List<MaintenancePlan> findByActiveTrueAndNextDueDateLessThanEqualOrderByNextDueDateAsc(LocalDate date);
  boolean existsByCode(String code);
  boolean existsByAssetIdAndActiveTrue(UUID assetId);
}
