package com.company.ops.api.modules.fixedasset.repository;

import com.company.ops.api.modules.fixedasset.domain.FixedAssetCount;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FixedAssetCountRepository extends JpaRepository<FixedAssetCount, UUID> {

  Page<FixedAssetCount> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
