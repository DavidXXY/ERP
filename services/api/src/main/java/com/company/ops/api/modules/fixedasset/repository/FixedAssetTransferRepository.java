package com.company.ops.api.modules.fixedasset.repository;

import com.company.ops.api.modules.fixedasset.domain.FixedAssetTransfer;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FixedAssetTransferRepository extends JpaRepository<FixedAssetTransfer, UUID> {

  Page<FixedAssetTransfer> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
