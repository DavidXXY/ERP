package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.GoodsReceipt;
import java.util.List;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, UUID> {

  long countByOrderId(UUID orderId);

  List<GoodsReceipt> findAllByOrderByReceivedDateDesc();
  List<GoodsReceipt> findByOrderId(UUID orderId);
  List<GoodsReceipt> findByOrderIdIn(Collection<UUID> orderIds);
  java.util.Optional<GoodsReceipt> findByClientRequestId(String clientRequestId);

  @Query("select r.orderId, max(r.receivedDate) from GoodsReceipt r group by r.orderId")
  List<Object[]> findLatestReceivedDateByOrder();
}
