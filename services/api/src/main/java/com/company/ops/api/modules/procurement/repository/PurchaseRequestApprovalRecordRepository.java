package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.PurchaseRequestApprovalRecord;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRequestApprovalRecordRepository
    extends JpaRepository<PurchaseRequestApprovalRecord, UUID> {

  Optional<PurchaseRequestApprovalRecord> findFirstByRequestIdOrderByDecidedAtDesc(UUID requestId);
}
