package com.company.ops.api.modules.payroll.repository;

import com.company.ops.api.modules.payroll.domain.PayrollRunLine;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollRunLineRepository extends JpaRepository<PayrollRunLine, UUID> {

  List<PayrollRunLine> findByRunIdOrderByCreatedAtAsc(UUID runId);
}