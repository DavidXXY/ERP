package com.company.ops.api.modules.hr.repository;

import com.company.ops.api.modules.hr.domain.EmployeeLifecycleRecord;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeLifecycleRecordRepository extends JpaRepository<EmployeeLifecycleRecord, UUID> {
    List<EmployeeLifecycleRecord> findByEmployeeIdOrderByCreatedAtDesc(UUID employeeId);
    List<EmployeeLifecycleRecord> findAllByOrderByCreatedAtDesc();
    long countByLifecycleTypeAndStatus(String lifecycleType, String status);
}
