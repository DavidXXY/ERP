package com.company.ops.api.modules.office.repository;

import com.company.ops.api.modules.office.domain.SealApplication;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SealApplicationRepository extends JpaRepository<SealApplication, UUID> {
  List<SealApplication> findAllByOrderByUseDateDescCreatedAtDesc();
  boolean existsByCode(String code);
  Optional<SealApplication> findByApprovalRequestId(UUID approvalRequestId);
}
