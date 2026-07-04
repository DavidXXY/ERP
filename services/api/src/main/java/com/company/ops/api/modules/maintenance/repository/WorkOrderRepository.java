package com.company.ops.api.modules.maintenance.repository;

import com.company.ops.api.modules.maintenance.domain.WorkOrder;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
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
  
  @Query("SELECT o.assigneeId, MAX(o.engineerName), COUNT(o), " +
         "SUM(CASE WHEN o.status = 'ACCEPTED' THEN 1 ELSE 0 END), " +
         "COALESCE(SUM(o.laborHours), 0), COALESCE(SUM(o.billableAmount), 0), COALESCE(SUM(o.laborCost), 0) " +
         "FROM WorkOrder o WHERE o.assigneeId IS NOT NULL " +
         "GROUP BY o.assigneeId")
  List<Object[]> aggregateByAssignee();
  
  @Query("SELECT COALESCE(SUM(o.billableAmount), 0) FROM WorkOrder o WHERE o.customerId = :customerId")
  BigDecimal sumBillableByCustomerId(@Param("customerId") UUID customerId);
  
  @Query("SELECT COALESCE(SUM(o.costAmount), 0) FROM WorkOrder o WHERE o.customerId = :customerId")
  BigDecimal sumCostByCustomerId(@Param("customerId") UUID customerId);
}
