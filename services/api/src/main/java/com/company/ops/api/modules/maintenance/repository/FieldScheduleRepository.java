package com.company.ops.api.modules.maintenance.repository;

import com.company.ops.api.modules.maintenance.domain.FieldSchedule;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FieldScheduleRepository extends JpaRepository<FieldSchedule, UUID> {
  List<FieldSchedule> findAllByOrderByWorkDateDesc();
  List<FieldSchedule> findByUserIdAndWorkDate(UUID userId, LocalDate workDate);
}
