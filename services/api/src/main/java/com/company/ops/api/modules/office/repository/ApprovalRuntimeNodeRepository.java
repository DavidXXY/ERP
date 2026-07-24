package com.company.ops.api.modules.office.repository;

import com.company.ops.api.modules.office.domain.ApprovalRuntimeNode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRuntimeNodeRepository extends JpaRepository<ApprovalRuntimeNode, UUID> {
  List<ApprovalRuntimeNode> findByApprovalIdOrderByStepNoAscCreatedAtAsc(UUID approvalId);
  void deleteByApprovalId(UUID approvalId);
  List<ApprovalRuntimeNode> findByApprovalIdAndStepNoOrderByCreatedAtAsc(UUID approvalId, int stepNo);
  List<ApprovalRuntimeNode> findByApprovalIdAndNodeStatusOrderByStepNoAscCreatedAtAsc(UUID approvalId, String nodeStatus);
  List<ApprovalRuntimeNode> findByNodeStatusAndDueAtBeforeOrderByDueAtAsc(String nodeStatus, OffsetDateTime dueAt);
}
