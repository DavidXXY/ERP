package com.company.ops.api.modules.crm.repository;

import com.company.ops.api.modules.crm.domain.ContractChangeRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractChangeRequestRepository extends JpaRepository<ContractChangeRequest, UUID> {
    List<ContractChangeRequest> findByContractIdOrderByCreatedAtDesc(UUID contractId);
    List<ContractChangeRequest> findByContractIdAndStatusOrderByCreatedAtDesc(UUID contractId, String status);
}