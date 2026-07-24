package com.company.ops.api.modules.inventory.repository;

import com.company.ops.api.modules.inventory.domain.StockMovement;
import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.util.Collection;
import com.company.ops.api.modules.inventory.domain.StockMovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

  List<StockMovement> findByPartIdOrderByCreatedAtDesc(UUID partId);

  @Query("select m.partId, sum(m.quantity) from StockMovement m "
      + "where m.createdAt >= :since and m.movementType in :types group by m.partId")
  List<Object[]> sumQuantityByPartSince(
      @Param("since") OffsetDateTime since,
      @Param("types") Collection<StockMovementType> types
  );
}
