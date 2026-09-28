package com.company.ops.api.modules.sales.repository;

import com.company.ops.api.modules.sales.domain.SalesReturnLine;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesReturnLineRepository extends JpaRepository<SalesReturnLine, UUID> {
  List<SalesReturnLine> findByReturnIdOrderByCreatedAtAsc(UUID returnId);
  List<SalesReturnLine> findByReturnIdIn(Collection<UUID> returnIds);
}
