package com.company.ops.api.modules.fixedasset.repository;

import com.company.ops.api.modules.fixedasset.domain.FixedAssetDepreciationRun;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FixedAssetDepreciationRunRepository extends JpaRepository<FixedAssetDepreciationRun, UUID> {

  Optional<FixedAssetDepreciationRun> findByPeriod(String period);

  Page<FixedAssetDepreciationRun> findAllByOrderByPeriodDesc(Pageable pageable);
}
