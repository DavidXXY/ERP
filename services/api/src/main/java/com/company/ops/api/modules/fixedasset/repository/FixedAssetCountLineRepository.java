package com.company.ops.api.modules.fixedasset.repository;

import com.company.ops.api.modules.fixedasset.domain.FixedAssetCountLine;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FixedAssetCountLineRepository extends JpaRepository<FixedAssetCountLine, UUID> {

  List<FixedAssetCountLine> findByCountIdOrderByCreatedAtAsc(UUID countId);

  Optional<FixedAssetCountLine> findByIdAndCountId(UUID id, UUID countId);
}
