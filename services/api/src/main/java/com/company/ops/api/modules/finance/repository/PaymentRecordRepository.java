package com.company.ops.api.modules.finance.repository;

import com.company.ops.api.modules.finance.domain.PaymentRecord;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, UUID> {

  List<PaymentRecord> findAllByOrderByPaidDateDescCreatedAtDesc();

  boolean existsByCode(String code);
}
