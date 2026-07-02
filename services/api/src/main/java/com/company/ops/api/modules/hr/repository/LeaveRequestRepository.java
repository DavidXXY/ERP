package com.company.ops.api.modules.hr.repository;

import com.company.ops.api.modules.hr.domain.LeaveRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {
    List<LeaveRequest> findByEmployeeIdOrderByCreatedAtDesc(UUID employeeId);
    List<LeaveRequest> findAllByOrderByCreatedAtDesc();
    long countByStatus(String status);
}
