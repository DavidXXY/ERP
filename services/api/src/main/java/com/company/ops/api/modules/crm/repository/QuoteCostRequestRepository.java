package com.company.ops.api.modules.crm.repository;

import com.company.ops.api.modules.crm.domain.QuoteCostRequest;
import com.company.ops.api.modules.crm.domain.QuoteCostStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuoteCostRequestRepository extends JpaRepository<QuoteCostRequest, UUID> {

  List<QuoteCostRequest> findByQuoteIdOrderByCreatedAtDesc(UUID quoteId);

  Optional<QuoteCostRequest> findFirstByQuoteIdOrderByCreatedAtDesc(UUID quoteId);

  Optional<QuoteCostRequest> findFirstByQuoteIdAndStatusOrderByCreatedAtDesc(UUID quoteId, QuoteCostStatus status);
}
