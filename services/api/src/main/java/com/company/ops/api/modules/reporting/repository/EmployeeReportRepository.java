package com.company.ops.api.modules.reporting.repository;

import com.company.ops.api.modules.reporting.domain.EmployeeReport;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeReportRepository extends JpaRepository<EmployeeReport, UUID> {
  List<EmployeeReport> findByEmployeeIdOrderByReportDateDescCreatedAtDesc(UUID employeeId);

  @Query("select r from EmployeeReport r where r.employeeId in :employeeIds "
      + "and (:type = '' or r.reportType = :type) "
      + "and (:fromDate is null or r.reportDate >= :fromDate) "
      + "and (:toDate is null or r.reportDate <= :toDate) "
      + "order by r.employeeName asc, r.reportDate desc, r.createdAt desc")
  List<EmployeeReport> findSubordinates(@Param("employeeIds") Collection<UUID> employeeIds,
      @Param("type") String type, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

  @Query("select r from EmployeeReport r where r.ccUserIds like concat('%', :userId, '%') "
      + "and (:type = '' or r.reportType = :type) "
      + "and (:fromDate is null or r.reportDate >= :fromDate) "
      + "and (:toDate is null or r.reportDate <= :toDate) "
      + "order by r.reportDate desc, r.createdAt desc")
  List<EmployeeReport> findCcToMe(@Param("userId") UUID userId, @Param("type") String type,
      @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
}
