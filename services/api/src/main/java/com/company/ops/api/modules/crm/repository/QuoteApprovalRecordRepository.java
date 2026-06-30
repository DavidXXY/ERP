package com.company.ops.api.modules.crm.repository;

import com.company.ops.api.modules.crm.domain.QuoteApprovalRecord;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuoteApprovalRecordRepository extends JpaRepository<QuoteApprovalRecord, UUID> {

  Optional<QuoteApprovalRecord> findFirstByQuoteIdOrderByDecidedAtDesc(UUID quoteId);
}
