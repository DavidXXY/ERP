package com.company.ops.api.modules.finance.repository;

import com.company.ops.api.modules.finance.domain.ContractMilestone;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractMilestoneRepository extends JpaRepository<ContractMilestone, UUID> {
  List<ContractMilestone> findByContractIdOrderByCreatedAtAsc(UUID contractId);
}
