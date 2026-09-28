package com.company.ops.api.modules.fixedasset.repository;

import com.company.ops.api.modules.fixedasset.domain.FixedAssetDepreciationLine;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FixedAssetDepreciationLineRepository extends JpaRepository<FixedAssetDepreciationLine, UUID> {

  List<FixedAssetDepreciationLine> findByRunIdOrderByCreatedAtAsc(UUID runId);
}
