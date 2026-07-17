package com.company.ops.api.modules.hr.repository;

import com.company.ops.api.modules.hr.domain.LeaveBalance;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, UUID> {
    Optional<LeaveBalance> findByEmployee_IdAndLeaveTypeAndYear(UUID employeeId, String leaveType, int year);
    List<LeaveBalance> findByEmployee_IdOrderByLeaveTypeAsc(UUID employeeId);
    List<LeaveBalance> findByEmployee_IdAndYear(UUID employeeId, int year);
}
