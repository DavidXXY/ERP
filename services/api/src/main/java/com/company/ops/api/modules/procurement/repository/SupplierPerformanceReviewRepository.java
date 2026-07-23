package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.SupplierPerformanceReview;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierPerformanceReviewRepository extends JpaRepository<SupplierPerformanceReview, UUID> {
  List<SupplierPerformanceReview> findAllByOrderByReviewPeriodDescCreatedAtDesc();
  List<SupplierPerformanceReview> findBySupplierIdOrderByReviewPeriodDesc(UUID supplierId);
  Optional<SupplierPerformanceReview> findBySupplierIdAndReviewPeriod(UUID supplierId, String period);
}
