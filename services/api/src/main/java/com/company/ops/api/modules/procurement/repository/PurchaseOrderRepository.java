package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.PurchaseOrder;
import com.company.ops.api.modules.procurement.domain.PurchaseOrderStatus;
import com.company.ops.api.modules.procurement.domain.ProcurementCostType;
import com.company.ops.api.modules.procurement.domain.ApprovalStatus;
import java.util.List;
import java.util.UUID;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {

  List<PurchaseOrder> findAllByOrderByCreatedAtDesc();

  Page<PurchaseOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);

  @Query("""
      SELECT o FROM PurchaseOrder o
      WHERE (:status IS NULL OR o.status = :status)
        AND (:costType IS NULL OR o.costType = :costType)
        AND (:search IS NULL OR o.code LIKE %:search% OR o.partName LIKE %:search%)
      ORDER BY o.createdAt DESC
  """)
  Page<PurchaseOrder> findByFilters(
      @Param("status") PurchaseOrderStatus status,
      @Param("costType") ProcurementCostType costType,
      @Param("search") String search,
      Pageable pageable
  );

  boolean existsByCode(String code);

  boolean existsByRequestId(UUID requestId);
  List<PurchaseOrder> findByRequestId(UUID requestId);
  List<PurchaseOrder> findBySupplierId(UUID supplierId);
  List<PurchaseOrder> findByProjectIdIn(Collection<UUID> projectIds);
  List<PurchaseOrder> findByApprovalStatus(ApprovalStatus approvalStatus);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select order from PurchaseOrder order where order.id = :id")
  java.util.Optional<PurchaseOrder> findByIdForUpdate(@Param("id") UUID id);
}
