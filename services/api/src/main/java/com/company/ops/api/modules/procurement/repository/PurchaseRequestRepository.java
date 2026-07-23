package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.PurchaseRequest;
import com.company.ops.api.modules.procurement.domain.PurchaseRequestStatus;
import com.company.ops.api.modules.procurement.domain.ApprovalStatus;
import com.company.ops.api.modules.procurement.domain.ProcurementCostType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, UUID> {

  List<PurchaseRequest> findAllByOrderByCreatedAtDesc();

  List<PurchaseRequest> findByBatchIdOrderByLineNoAsc(UUID batchId);

  Page<PurchaseRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

  @Query("""
      SELECT r FROM PurchaseRequest r
      WHERE (:status IS NULL OR r.status = :status)
        AND (:approvalStatus IS NULL OR r.approvalStatus = :approvalStatus)
        AND (:costType IS NULL OR r.costType = :costType)
        AND (:search IS NULL OR r.partName LIKE %:search% OR r.code LIKE %:search% OR r.requesterName LIKE %:search%)
      ORDER BY r.createdAt DESC
  """)
  Page<PurchaseRequest> findByFilters(
      @Param("status") PurchaseRequestStatus status,
      @Param("approvalStatus") ApprovalStatus approvalStatus,
      @Param("costType") ProcurementCostType costType,
      @Param("search") String search,
      Pageable pageable
  );

  boolean existsByCode(String code);
}
