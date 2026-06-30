package com.company.ops.api.modules.qualification.repository;

import com.company.ops.api.modules.qualification.domain.QualificationPerformance;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QualificationPerformanceRepository extends JpaRepository<QualificationPerformance, UUID> {
  List<QualificationPerformance> findAllByOrderBySubjectCompanyAscNameAsc();
  Optional<QualificationPerformance> findByExternalId(String externalId);
}
