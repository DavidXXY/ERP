package com.company.ops.api.modules.office.repository;
import com.company.ops.api.modules.office.domain.ApprovalRequest;
import com.company.ops.api.modules.office.domain.ApprovalStatus;
import jakarta.persistence.LockModeType;
import java.util.List; import java.util.Optional; import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository; import org.springframework.data.jpa.repository.Lock; import org.springframework.data.jpa.repository.Query; import org.springframework.data.repository.query.Param;
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, UUID> {
  List<ApprovalRequest> findAllByOrderByCreatedAtDesc(); boolean existsByCode(String code);
  List<ApprovalRequest> findByStatusOrderByCreatedAtDesc(ApprovalStatus status);
  @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select item from ApprovalRequest item where item.id = :id") Optional<ApprovalRequest> findByIdForUpdate(@Param("id") UUID id);
  long countByStatus(String status);
}
