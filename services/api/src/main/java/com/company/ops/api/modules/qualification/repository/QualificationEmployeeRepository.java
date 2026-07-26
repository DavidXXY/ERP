package com.company.ops.api.modules.qualification.repository;

import com.company.ops.api.modules.qualification.domain.QualificationEmployee;
import java.util.List;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QualificationEmployeeRepository extends JpaRepository<QualificationEmployee, UUID> {
  List<QualificationEmployee> findAllByOrderByNameAsc();
  List<QualificationEmployee> findByOrganization_IdOrderByNameAsc(UUID organizationId);
  Optional<QualificationEmployee> findByExternalId(String externalId);
  Optional<QualificationEmployee> findBySystemUser_Id(UUID systemUserId);
  List<QualificationEmployee> findByNameContainingIgnoreCase(String name,Pageable pageable);
  @Query(value = "select e from QualificationEmployee e left join fetch e.organization o where "
      + "(:keyword = '' or lower(e.name) like lower(concat('%', :keyword, '%')) "
      + "or lower(coalesce(e.workNo, '')) like lower(concat('%', :keyword, '%')) "
      + "or lower(coalesce(e.department, '')) like lower(concat('%', :keyword, '%')) "
      + "or lower(coalesce(o.name, '')) like lower(concat('%', :keyword, '%'))) "
      + "and (:status = '' or lower(e.employmentStatus) = lower(:status)) "
      + "and (:organizationId is null or o.id = :organizationId) "
      + "and e.id not in :hiddenIds order by e.name",
      countQuery = "select count(e) from QualificationEmployee e left join e.organization o where "
      + "(:keyword = '' or lower(e.name) like lower(concat('%', :keyword, '%')) "
      + "or lower(coalesce(e.workNo, '')) like lower(concat('%', :keyword, '%')) "
      + "or lower(coalesce(e.department, '')) like lower(concat('%', :keyword, '%')) "
      + "or lower(coalesce(o.name, '')) like lower(concat('%', :keyword, '%'))) "
      + "and (:status = '' or lower(e.employmentStatus) = lower(:status)) "
      + "and (:organizationId is null or o.id = :organizationId) "
      + "and e.id not in :hiddenIds")
  Page<QualificationEmployee> search(@Param("keyword") String keyword, @Param("status") String status,
      @Param("organizationId") UUID organizationId, @Param("hiddenIds") Collection<UUID> hiddenIds,
      Pageable pageable);
  long countByOrganization_Id(UUID organizationId);
  long countByEmploymentStatus(String employmentStatus);
  @org.springframework.data.jpa.repository.Query("SELECT COUNT(e) FROM QualificationEmployee e WHERE e.entryDate BETWEEN :start AND :end")
  long countByEntryDateBetween(@org.springframework.data.repository.query.Param("start") java.time.LocalDate start,
                                @org.springframework.data.repository.query.Param("end") java.time.LocalDate end);

}
