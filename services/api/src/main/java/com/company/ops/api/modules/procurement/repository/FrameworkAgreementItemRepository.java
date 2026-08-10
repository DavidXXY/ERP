package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.FrameworkAgreementItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FrameworkAgreementItemRepository extends JpaRepository<FrameworkAgreementItem, UUID> {
  List<FrameworkAgreementItem> findByAgreementIdOrderByCreatedAtAsc(UUID agreementId);
  void deleteByAgreementId(UUID agreementId);
}
