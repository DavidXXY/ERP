package com.company.ops.api.modules.finance.repository;

import com.company.ops.api.modules.finance.domain.PaymentRecord;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, UUID> {

  List<PaymentRecord> findAllByOrderByPaidDateDescCreatedAtDesc();
  Page<PaymentRecord> findAllByOrderByPaidDateDescCreatedAtDesc(Pageable pageable);

  boolean existsByCode(String code);
}
