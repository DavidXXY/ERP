package com.company.ops.api.modules.qualification.repository;

import com.company.ops.api.modules.qualification.domain.CompanyQualification;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyQualificationRepository extends JpaRepository<CompanyQualification, UUID> {
  List<CompanyQualification> findAllByOrderBySubjectCompanyAscNameAsc();
  Optional<CompanyQualification> findByExternalId(String externalId);
}
