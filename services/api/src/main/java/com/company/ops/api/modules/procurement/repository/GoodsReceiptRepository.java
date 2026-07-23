package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.GoodsReceipt;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, UUID> {

  long countByOrderId(UUID orderId);

  List<GoodsReceipt> findAllByOrderByReceivedDateDesc();
  List<GoodsReceipt> findByOrderId(UUID orderId);
  java.util.Optional<GoodsReceipt> findByClientRequestId(String clientRequestId);
}
