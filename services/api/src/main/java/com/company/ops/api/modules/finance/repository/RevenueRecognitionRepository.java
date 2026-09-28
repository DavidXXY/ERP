package com.company.ops.api.modules.finance.repository;

import com.company.ops.api.modules.finance.domain.RevenueRecognition;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevenueRecognitionRepository extends JpaRepository<RevenueRecognition, UUID> {
  Page<RevenueRecognition> findAllByOrderByCreatedAtDesc(Pageable pageable);
  List<RevenueRecognition> findByContractIdOrderByCreatedAtAsc(UUID contractId);
  boolean existsByCode(String code);
}
