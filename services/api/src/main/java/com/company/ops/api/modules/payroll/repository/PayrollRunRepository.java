package com.company.ops.api.modules.payroll.repository;

import com.company.ops.api.modules.payroll.domain.PayrollRun;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, UUID> {

  Optional<PayrollRun> findByPeriod(String period);

  boolean existsByPeriod(String period);

  Page<PayrollRun> findAllByOrderByPeriodDesc(Pageable pageable);

  List<PayrollRun> findAllByOrderByPeriodDesc();
}