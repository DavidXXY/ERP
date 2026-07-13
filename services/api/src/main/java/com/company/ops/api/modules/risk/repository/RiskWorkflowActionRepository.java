package com.company.ops.api.modules.risk.repository;

import com.company.ops.api.modules.risk.domain.RiskWorkflowAction;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskWorkflowActionRepository extends JpaRepository<RiskWorkflowAction, UUID> {
  List<RiskWorkflowAction> findByRiskKeyOrderByCreatedAtDesc(String riskKey);
}
