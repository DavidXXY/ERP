package com.company.ops.api.modules.risk.repository;

import com.company.ops.api.modules.risk.domain.RiskRuleConfig;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskRuleConfigRepository extends JpaRepository<RiskRuleConfig, UUID> {
  Optional<RiskRuleConfig> findByRuleCode(String ruleCode);
  List<RiskRuleConfig> findAllByOrderByModuleAscRuleCodeAsc();
}
