package com.company.ops.api.modules.hr.repository;

import com.company.ops.api.modules.hr.domain.EmergencyContact;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, UUID> {
    List<EmergencyContact> findByEmployeeId(UUID employeeId);
    void deleteByEmployeeId(UUID employeeId);
}
