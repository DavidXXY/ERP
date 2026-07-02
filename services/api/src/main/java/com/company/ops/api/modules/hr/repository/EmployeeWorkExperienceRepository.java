package com.company.ops.api.modules.hr.repository;

import com.company.ops.api.modules.hr.domain.EmployeeWorkExperience;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeWorkExperienceRepository extends JpaRepository<EmployeeWorkExperience, UUID> {
    List<EmployeeWorkExperience> findByEmployeeIdOrderByStartDateDesc(UUID employeeId);
    void deleteByEmployeeId(UUID employeeId);
}
