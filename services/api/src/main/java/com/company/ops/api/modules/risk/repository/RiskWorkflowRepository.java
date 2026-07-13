package com.company.ops.api.modules.risk.repository;

import com.company.ops.api.modules.risk.domain.RiskWorkflow;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskWorkflowRepository extends JpaRepository<RiskWorkflow, UUID> {
  Optional<RiskWorkflow> findByRiskKey(String riskKey);
  List<RiskWorkflow> findAllByOrderByUpdatedAtDesc();
}
