package com.company.ops.api.modules.procurement.repository;
import com.company.ops.api.modules.procurement.domain.ProcurementInquiry; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface ProcurementInquiryRepository extends JpaRepository<ProcurementInquiry,UUID>{List<ProcurementInquiry> findAllByOrderByCreatedAtDesc(); boolean existsByCode(String code);}
