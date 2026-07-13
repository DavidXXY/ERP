package com.company.ops.api.modules.risk.repository;

import com.company.ops.api.modules.risk.domain.RiskSnapshot;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskSnapshotRepository extends JpaRepository<RiskSnapshot, UUID> {
  boolean existsBySnapshotDateAndRiskKey(LocalDate snapshotDate, String riskKey);
  List<RiskSnapshot> findBySnapshotDateBetweenOrderBySnapshotDateAsc(LocalDate start, LocalDate end);
  List<RiskSnapshot> findBySnapshotDateOrderBySeverityDesc(LocalDate snapshotDate);
}
