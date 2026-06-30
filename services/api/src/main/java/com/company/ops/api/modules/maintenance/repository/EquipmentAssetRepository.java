package com.company.ops.api.modules.maintenance.repository;

import com.company.ops.api.modules.maintenance.domain.EquipmentAsset;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentAssetRepository extends JpaRepository<EquipmentAsset, UUID> {
  List<EquipmentAsset> findAllByOrderByNextMaintenanceDateAsc();
  boolean existsByCode(String code);
}
