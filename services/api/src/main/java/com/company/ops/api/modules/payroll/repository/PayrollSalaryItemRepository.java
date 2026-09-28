package com.company.ops.api.modules.payroll.repository;

import com.company.ops.api.modules.payroll.domain.PayrollSalaryItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollSalaryItemRepository extends JpaRepository<PayrollSalaryItem, UUID> {

  Page<PayrollSalaryItem> findAllByOrderByCreatedAtDesc(Pageable pageable);

  Page<PayrollSalaryItem> findByEmployeeIdOrderByCreatedAtDesc(UUID employeeId, Pageable pageable);

  List<PayrollSalaryItem> findByActiveTrueOrderByEmployeeIdAsc();
}