package com.company.ops.api.modules.qualification.repository;

import com.company.ops.api.modules.qualification.domain.EmployeeContract;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeContractRepository extends JpaRepository<EmployeeContract, UUID> {
  List<EmployeeContract> findByEmployeeIdOrderByStartDateDesc(UUID employeeId);
  List<EmployeeContract> findAllByOrderByEndDateAsc();
  boolean existsByContractNo(String contractNo);
  boolean existsByContractNoAndIdNot(String contractNo, UUID id);
}
