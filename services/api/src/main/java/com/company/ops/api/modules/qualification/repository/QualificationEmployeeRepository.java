package com.company.ops.api.modules.qualification.repository;

import com.company.ops.api.modules.qualification.domain.QualificationEmployee;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QualificationEmployeeRepository extends JpaRepository<QualificationEmployee, UUID> {
  List<QualificationEmployee> findAllByOrderByNameAsc();
  Optional<QualificationEmployee> findByExternalId(String externalId);
  Optional<QualificationEmployee> findBySystemUser_Id(UUID systemUserId);
}
