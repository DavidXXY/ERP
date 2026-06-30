package com.company.ops.api.modules.maintenance.repository;

import com.company.ops.api.modules.maintenance.domain.WorkOrder;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID> {
  List<WorkOrder> findAllByOrderByCreatedAtDesc();
  boolean existsByCode(String code);
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select item from WorkOrder item where item.id = :id")
  Optional<WorkOrder> findByIdForUpdate(@Param("id") UUID id);
}
