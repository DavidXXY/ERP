package com.company.ops.api.modules.inventory.repository;

import com.company.ops.api.modules.inventory.domain.InventoryIssueOrder;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryIssueOrderRepository extends JpaRepository<InventoryIssueOrder, UUID> {

  List<InventoryIssueOrder> findAllByOrderByIssueDateDescCreatedAtDesc();

  boolean existsByCode(String code);
}
