package com.company.ops.api.modules.crm.repository;

import com.company.ops.api.modules.crm.domain.ReceivableReceipt;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceivableReceiptRepository extends JpaRepository<ReceivableReceipt, UUID> {

  List<ReceivableReceipt> findByReceivableIdOrderByReceivedDateDesc(UUID receivableId);
}
