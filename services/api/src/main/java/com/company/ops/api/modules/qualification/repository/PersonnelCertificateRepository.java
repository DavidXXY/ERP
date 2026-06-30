package com.company.ops.api.modules.qualification.repository;

import com.company.ops.api.modules.qualification.domain.PersonnelCertificate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonnelCertificateRepository extends JpaRepository<PersonnelCertificate, UUID> {
  List<PersonnelCertificate> findAllByOrderByEmployeeNameAscNameAsc();
  List<PersonnelCertificate> findByEmployeeIdOrderByNameAsc(UUID employeeId);
  Optional<PersonnelCertificate> findByExternalId(String externalId);
}
