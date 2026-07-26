package com.company.ops.api.modules.office.repository;
import com.company.ops.api.modules.office.domain.ApprovalRequest;
import com.company.ops.api.modules.office.domain.ApprovalStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection; import java.util.List; import java.util.Optional; import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository; import org.springframework.data.jpa.repository.Lock; import org.springframework.data.jpa.repository.Query; import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable;
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, UUID> {
  List<ApprovalRequest> findAllByOrderByCreatedAtDesc(); boolean existsByCode(String code);
  Page<ApprovalRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
  List<ApprovalRequest> findByStatusOrderByCreatedAtDesc(ApprovalStatus status);
  List<ApprovalRequest> findByProcessedAtIsNotNull();
  @Query(value = """
      select a from ApprovalRequest a
      where a.applicantUserId = :userId
         or (a.applicantUserId is null and a.applicantName = :displayName)
         or a.delegatedUserId = :userId
         or exists (select action.id from ApprovalAction action where action.approvalId = a.id and action.operatorId = :userId)
         or exists (select node.id from ApprovalRuntimeNode node where node.approvalId = a.id and node.nodeStatus = 'PENDING'
              and ((node.assigneeType = 'USER' and node.assigneeId = :userId)
                or (node.assigneeType = 'ROLE' and node.assigneeId in :roleIds)))
      order by a.createdAt desc
      """, countQuery = """
      select count(a) from ApprovalRequest a
      where a.applicantUserId = :userId
         or (a.applicantUserId is null and a.applicantName = :displayName)
         or a.delegatedUserId = :userId
         or exists (select action.id from ApprovalAction action where action.approvalId = a.id and action.operatorId = :userId)
         or exists (select node.id from ApprovalRuntimeNode node where node.approvalId = a.id and node.nodeStatus = 'PENDING'
              and ((node.assigneeType = 'USER' and node.assigneeId = :userId)
                or (node.assigneeType = 'ROLE' and node.assigneeId in :roleIds)))
      """)
  Page<ApprovalRequest> findMobileVisible(
      @Param("userId") UUID userId,
      @Param("displayName") String displayName,
      @Param("roleIds") Collection<UUID> roleIds,
      Pageable pageable);
  @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select item from ApprovalRequest item where item.id = :id") Optional<ApprovalRequest> findByIdForUpdate(@Param("id") UUID id);
  long countByStatus(String status);
}
