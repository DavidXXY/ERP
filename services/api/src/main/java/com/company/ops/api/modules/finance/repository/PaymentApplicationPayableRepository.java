package com.company.ops.api.modules.finance.repository;

import com.company.ops.api.modules.finance.domain.PaymentApplicationPayable;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentApplicationPayableRepository
    extends JpaRepository<PaymentApplicationPayable, UUID> {

  List<PaymentApplicationPayable> findByApplicationId(UUID applicationId);

  List<PaymentApplicationPayable> findByPayableId(UUID payableId);

  List<PaymentApplicationPayable> findByPayableIdIn(Collection<UUID> payableIds);

  @Query("select ap.payableId, coalesce(sum(ap.allocatedAmount), 0) "
      + "from PaymentApplicationPayable ap where ap.payableId in :payableIds "
      + "and ap.applicationId in (select app.id from PaymentApplication app "
      + "where app.status in :statuses) group by ap.payableId")
  List<Object[]> aggregateReservedByPayableIdIn(
      @Param("payableIds") Collection<UUID> payableIds,
      @Param("statuses") Collection<?> statuses);

  @Modifying
  void deleteByApplicationId(UUID applicationId);
}
