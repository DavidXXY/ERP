package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.ProcurementApprovalRule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcurementApprovalRuleRepository extends JpaRepository<ProcurementApprovalRule, UUID> {
  List<ProcurementApprovalRule> findByEnabledTrueOrderBySortOrderAsc();
  List<ProcurementApprovalRule> findAllByOrderBySortOrderAsc();
}
