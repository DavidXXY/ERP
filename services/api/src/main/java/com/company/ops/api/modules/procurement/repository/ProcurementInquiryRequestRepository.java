package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.ProcurementInquiryRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcurementInquiryRequestRepository
    extends JpaRepository<ProcurementInquiryRequest, UUID> {

  List<ProcurementInquiryRequest> findByInquiryIdOrderByCreatedAtAsc(UUID inquiryId);

  List<ProcurementInquiryRequest> findByRequestId(UUID requestId);

  boolean existsByInquiryIdAndRequestId(UUID inquiryId, UUID requestId);
}

