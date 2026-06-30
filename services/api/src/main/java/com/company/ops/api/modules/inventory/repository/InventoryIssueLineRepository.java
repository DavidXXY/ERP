package com.company.ops.api.modules.inventory.repository;

import com.company.ops.api.modules.inventory.domain.InventoryIssueLine;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryIssueLineRepository extends JpaRepository<InventoryIssueLine, UUID> {

  List<InventoryIssueLine> findByIssueIdOrderByCreatedAtAsc(UUID issueId);

  List<InventoryIssueLine> findByIssueIdIn(List<UUID> issueIds);
}
