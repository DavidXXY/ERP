package com.company.ops.api.modules.procurement.repository;
import com.company.ops.api.modules.procurement.domain.ProcurementReturnOrder; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface ProcurementReturnOrderRepository extends JpaRepository<ProcurementReturnOrder,UUID>{List<ProcurementReturnOrder> findAllByOrderByReturnDateDesc();}
