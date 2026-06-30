package com.company.ops.api.modules.crm.repository;

import com.company.ops.api.modules.crm.domain.QuotePlan;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuotePlanRepository extends JpaRepository<QuotePlan, UUID> {

  List<QuotePlan> findAllByOrderByUpdatedAtDesc();

  boolean existsByCode(String code);
}
