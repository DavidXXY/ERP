package com.company.ops.api.modules.maintenance.repository;

import com.company.ops.api.modules.maintenance.domain.FieldAttendance;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FieldAttendanceRepository extends JpaRepository<FieldAttendance, UUID> {
  List<FieldAttendance> findAllByOrderByCheckInAtDesc();
  Optional<FieldAttendance> findFirstByWorkOrderIdOrderByCheckInAtDesc(UUID workOrderId);
}
