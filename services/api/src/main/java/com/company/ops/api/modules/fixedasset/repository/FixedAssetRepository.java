package com.company.ops.api.modules.fixedasset.repository;

import com.company.ops.api.modules.fixedasset.domain.FixedAsset;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FixedAssetRepository extends JpaRepository<FixedAsset, UUID> {

  Page<FixedAsset> findAllByOrderByCreatedAtDesc(Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from FixedAsset a where a.id = :id")
  Optional<FixedAsset> findByIdForUpdate(@Param("id") UUID id);

  @Query("select a from FixedAsset a where a.status in :statuses "
      + "and (a.lastDepreciatedPeriod is null or a.lastDepreciatedPeriod < :period) "
      + "order by a.createdAt asc")
  List<FixedAsset> findDepreciableAssets(@Param("statuses") List<String> statuses,
      @Param("period") String period);

  List<FixedAsset> findByStatusInOrderByCreatedAtAsc(List<String> statuses);
}
