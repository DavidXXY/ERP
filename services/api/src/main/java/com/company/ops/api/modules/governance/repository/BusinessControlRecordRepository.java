package com.company.ops.api.modules.governance.repository;

import com.company.ops.api.modules.governance.domain.BusinessControlRecord;
import com.company.ops.api.modules.governance.domain.ControlStatus;
import com.company.ops.api.modules.governance.domain.ControlType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BusinessControlRecordRepository extends JpaRepository<BusinessControlRecord, UUID> {
  List<BusinessControlRecord> findAllByOrderByCreatedAtDesc();
  List<BusinessControlRecord> findByStatusInOrderByCreatedAtDesc(Collection<ControlStatus> statuses);
  @Query("SELECT c FROM BusinessControlRecord c WHERE "
      + "(:type IS NULL OR c.controlType = :type) AND (:status IS NULL OR c.status = :status) AND "
      + "(:keyword = '' OR LOWER(c.controlCode) LIKE LOWER(CONCAT('%', :keyword, '%')) "
      + "OR LOWER(COALESCE(c.businessNo, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) "
      + "OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) "
      + "OR LOWER(c.owner) LIKE LOWER(CONCAT('%', :keyword, '%'))) ORDER BY c.createdAt DESC")
  Page<BusinessControlRecord> search(@Param("type") ControlType type, @Param("status") ControlStatus status,
      @Param("keyword") String keyword, Pageable pageable);
  long countByRiskLevelAndStatusInAndPlannedEndLessThanEqual(
      String riskLevel, Collection<ControlStatus> statuses, LocalDate date);
  @Query("SELECT COUNT(c) FROM BusinessControlRecord c WHERE c.riskLevel = :riskLevel "
      + "AND c.status IN :statuses "
      + "AND ((c.plannedEnd IS NOT NULL AND c.plannedEnd <= :date) "
      + "OR (c.nextReviewOn IS NOT NULL AND c.nextReviewOn <= :date))")
  long countHighRiskDue(@Param("riskLevel") String riskLevel,
      @Param("statuses") Collection<ControlStatus> statuses, @Param("date") LocalDate date);
  long countByControlTypeAndStatusIn(ControlType type, Collection<ControlStatus> statuses);
  boolean existsByControlCode(String code);
  List<BusinessControlRecord> findByControlTypeInOrderByCreatedAtDesc(Collection<ControlType> types);

}
