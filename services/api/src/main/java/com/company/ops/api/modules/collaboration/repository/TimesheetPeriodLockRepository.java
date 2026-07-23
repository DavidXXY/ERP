package com.company.ops.api.modules.collaboration.repository;
import com.company.ops.api.modules.collaboration.domain.TimesheetPeriodLock;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface TimesheetPeriodLockRepository extends JpaRepository<TimesheetPeriodLock,UUID>{
  Optional<TimesheetPeriodLock> findByYearMonth(String yearMonth);
  boolean existsByYearMonth(String yearMonth);
  List<TimesheetPeriodLock> findAllByOrderByYearMonthDesc();
}
