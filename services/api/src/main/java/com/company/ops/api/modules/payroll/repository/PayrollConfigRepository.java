package com.company.ops.api.modules.payroll.repository;

import com.company.ops.api.modules.payroll.domain.PayrollConfig;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollConfigRepository extends JpaRepository<PayrollConfig, UUID> {

  Optional<PayrollConfig> findByConfigKey(String configKey);

  List<PayrollConfig> findAllByOrderByConfigKeyAsc();
}