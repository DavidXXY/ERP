package com.company.ops.api.modules.procurement.repository;

import com.company.ops.api.modules.procurement.domain.ProcurementInquiryRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;

public interface ProcurementInquiryRequestRepository
    extends JpaRepository<ProcurementInquiryRequest, UUID> {

  List<ProcurementInquiryRequest> findByInquiryIdOrderByCreatedAtAsc(UUID inquiryId);

  List<ProcurementInquiryRequest> findByRequestId(UUID requestId);

  boolean existsByInquiryIdAndRequestId(UUID inquiryId, UUID requestId);
  @Query("select link from ProcurementInquiryRequest link where link.inquiryId in "
      + "(select i.id from ProcurementInquiry i where i.status in :statuses)")
  List<ProcurementInquiryRequest> findByInquiryStatusIn(@Param("statuses") Collection<String> statuses);

}

