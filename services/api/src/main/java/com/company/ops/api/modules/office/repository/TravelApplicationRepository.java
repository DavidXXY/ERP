package com.company.ops.api.modules.office.repository;

import com.company.ops.api.modules.office.domain.TravelApplication;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TravelApplicationRepository extends JpaRepository<TravelApplication, UUID> {
  List<TravelApplication> findAllByOrderByStartDateDescCreatedAtDesc();
  boolean existsByCode(String code);
  Optional<TravelApplication> findByApprovalRequestId(UUID approvalRequestId);
}
