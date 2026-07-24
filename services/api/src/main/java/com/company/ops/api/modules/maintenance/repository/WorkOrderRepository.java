package com.company.ops.api.modules.maintenance.repository;

import com.company.ops.api.modules.maintenance.domain.WorkOrder;
import com.company.ops.api.modules.maintenance.domain.WorkOrderStatus;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID> {
  List<WorkOrder> findAllByOrderByCreatedAtDesc();
  List<WorkOrder> findByCreatedAtBetweenOrderByCreatedAtDesc(OffsetDateTime startAt,OffsetDateTime endAt);
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

  @Query("select coalesce(sum(o.costAmount), 0) from WorkOrder o")
  BigDecimal sumCostAmount();

  long countByStatus(WorkOrderStatus status);
  long countByStatusNotIn(java.util.Collection<WorkOrderStatus> statuses);

  @Query("select o.customerId, coalesce(sum(o.billableAmount), 0), coalesce(sum(o.costAmount), 0) " +
      "from WorkOrder o where o.customerId is not null group by o.customerId")
  List<Object[]> aggregateByCustomer();

  @Query("select o.equipmentId, count(o), " +
      "sum(case when o.workType = 'REPAIR' then 1 else 0 end), coalesce(sum(o.costAmount), 0) " +
      "from WorkOrder o where o.equipmentId is not null group by o.equipmentId")
  List<Object[]> aggregateByEquipment();
}
