package com.company.ops.api.modules.fixedasset.repository;

import com.company.ops.api.modules.fixedasset.domain.FixedAssetDisposal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FixedAssetDisposalRepository extends JpaRepository<FixedAssetDisposal, UUID> {

  Page<FixedAssetDisposal> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
