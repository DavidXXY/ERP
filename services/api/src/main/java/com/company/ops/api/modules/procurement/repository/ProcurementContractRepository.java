package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.ProcurementContract;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcurementContractRepository extends JpaRepository<ProcurementContract, UUID> {
  List<ProcurementContract> findAllByOrderByCreatedAtDesc();
  List<ProcurementContract> findBySupplierIdOrderByCreatedAtDesc(UUID supplierId);
  List<ProcurementContract> findByOrderIdIn(Collection<UUID> orderIds);
  Optional<ProcurementContract> findFirstByContractNoOrderByVersionNoDesc(String contractNo);
  boolean existsByInquiryIdAndStatusNotIn(UUID inquiryId, List<String> statuses);
  Optional<ProcurementContract> findFirstByInquiryIdAndSupplierIdOrderByCreatedAtDesc(UUID inquiryId, UUID supplierId);
}
