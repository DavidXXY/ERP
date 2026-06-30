package com.company.ops.api.modules.crm.repository;

import com.company.ops.api.modules.crm.domain.QuoteRevision;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuoteRevisionRepository extends JpaRepository<QuoteRevision, UUID> {

  List<QuoteRevision> findByQuoteIdOrderByVersionNoDesc(UUID quoteId);
}
