package com.company.ops.api.modules.inventory.repository;

import com.company.ops.api.modules.inventory.domain.InventoryIssueOrder;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryIssueOrderRepository extends JpaRepository<InventoryIssueOrder, UUID> {

  List<InventoryIssueOrder> findAllByOrderByIssueDateDescCreatedAtDesc();
  Page<InventoryIssueOrder> findAllByOrderByIssueDateDescCreatedAtDesc(Pageable pageable);

  boolean existsByCode(String code);
}
