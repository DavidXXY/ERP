package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.ProcurementInquiryInvitation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcurementInquiryInvitationRepository extends JpaRepository<ProcurementInquiryInvitation, UUID> {
  List<ProcurementInquiryInvitation> findByInquiryIdOrderByInvitedAtAsc(UUID inquiryId);
  List<ProcurementInquiryInvitation> findBySupplierIdOrderByInvitedAtDesc(UUID supplierId);
  Optional<ProcurementInquiryInvitation> findByInquiryIdAndSupplierId(UUID inquiryId, UUID supplierId);
}
