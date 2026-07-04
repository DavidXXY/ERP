package com.company.ops.api.modules.office.repository;
import com.company.ops.api.modules.office.domain.OutsourceOrder;
import java.util.List; import java.util.Optional; import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OutsourceOrderRepository extends JpaRepository<OutsourceOrder, UUID> {
  List<OutsourceOrder> findAllByOrderByPlannedDateDescCreatedAtDesc(); boolean existsByCode(String code);
  Optional<OutsourceOrder> findByApprovalRequestId(UUID approvalRequestId);
  long countByStatusNotIn(List<String> statuses);
}
