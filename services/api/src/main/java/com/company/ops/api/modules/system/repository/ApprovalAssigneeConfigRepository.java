package com.company.ops.api.modules.system.repository;

import com.company.ops.api.modules.system.domain.ApprovalAssigneeConfig;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalAssigneeConfigRepository extends JpaRepository<ApprovalAssigneeConfig, UUID> {
  List<ApprovalAssigneeConfig> findAllByOrderByFlowCodeAscCreatedAtAsc();
  List<ApprovalAssigneeConfig> findByFlowCodeAndEnabledTrue(String flowCode);
  List<ApprovalAssigneeConfig> findByFlowCodeAndVersionNoAndEnabledTrue(String flowCode, int versionNo);
  boolean existsByFlowCodeAndUserId(String flowCode, UUID userId);
}
