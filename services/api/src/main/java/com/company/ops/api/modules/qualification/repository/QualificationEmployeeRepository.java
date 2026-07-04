package com.company.ops.api.modules.qualification.repository;

import com.company.ops.api.modules.qualification.domain.QualificationEmployee;
import java.util.List;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QualificationEmployeeRepository extends JpaRepository<QualificationEmployee, UUID> {
  List<QualificationEmployee> findAllByOrderByNameAsc();
  List<QualificationEmployee> findByOrganization_IdOrderByNameAsc(UUID organizationId);
  Optional<QualificationEmployee> findByExternalId(String externalId);
  Optional<QualificationEmployee> findBySystemUser_Id(UUID systemUserId);
  long countByOrganization_Id(UUID organizationId);
  long countByEmploymentStatus(String employmentStatus);
  @org.springframework.data.jpa.repository.Query("SELECT COUNT(e) FROM QualificationEmployee e WHERE e.entryDate BETWEEN :start AND :end")
  long countByEntryDateBetween(@org.springframework.data.repository.query.Param("start") java.time.LocalDate start,
                                @org.springframework.data.repository.query.Param("end") java.time.LocalDate end);
}
