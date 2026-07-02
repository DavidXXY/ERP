package com.company.ops.api.modules.hr.repository;

import com.company.ops.api.modules.hr.domain.EmployeeEducation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeEducationRepository extends JpaRepository<EmployeeEducation, UUID> {
    List<EmployeeEducation> findByEmployeeIdOrderByStartDateDesc(UUID employeeId);
    void deleteByEmployeeId(UUID employeeId);
}
