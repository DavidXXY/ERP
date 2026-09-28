package com.company.ops.api.modules.system.repository;

import com.company.ops.api.modules.system.domain.LegalEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LegalEntityRepository extends JpaRepository<LegalEntity, UUID> {
  Page<LegalEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
  boolean existsByCode(String code);
}