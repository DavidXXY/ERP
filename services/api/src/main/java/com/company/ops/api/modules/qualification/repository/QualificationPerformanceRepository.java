package com.company.ops.api.modules.qualification.repository;

import com.company.ops.api.modules.qualification.domain.QualificationPerformance;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QualificationPerformanceRepository extends JpaRepository<QualificationPerformance, UUID> {
  boolean existsByAttachmentsJsonContaining(String value);
  List<QualificationPerformance> findAllByOrderBySubjectCompanyAscNameAsc();
  Optional<QualificationPerformance> findByExternalId(String externalId);
  @Query("select p from QualificationPerformance p where "
      + "(:keyword = '' or lower(p.name) like lower(concat('%', :keyword, '%')) "
      + "or lower(coalesce(p.clientName, '')) like lower(concat('%', :keyword, '%')) "
      + "or lower(coalesce(p.contractNo, '')) like lower(concat('%', :keyword, '%'))) "
      + "and (:subjectCompany = '' or p.subjectCompany = :subjectCompany) "
      + "and (:projectType = '' or p.projectType = :projectType) "
      + "and p.id not in :hiddenIds order by p.subjectCompany, p.name")
  Page<QualificationPerformance> search(@Param("keyword") String keyword,
      @Param("subjectCompany") String subjectCompany, @Param("projectType") String projectType,
      @Param("hiddenIds") Collection<UUID> hiddenIds, Pageable pageable);
  @Query("select p.externalId from QualificationPerformance p where p.externalId is not null")
  List<String> findAllExternalIds();

}
