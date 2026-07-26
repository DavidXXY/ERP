package com.company.ops.api.modules.qualification.repository;

import com.company.ops.api.modules.qualification.domain.CompanyQualification;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyQualificationRepository extends JpaRepository<CompanyQualification, UUID> {
  List<CompanyQualification> findAllByOrderBySubjectCompanyAscNameAsc();
  Optional<CompanyQualification> findByExternalId(String externalId);
  @Query("select q from CompanyQualification q where "
      + "(:keyword = '' or lower(q.name) like lower(concat('%', :keyword, '%')) "
      + "or lower(coalesce(q.certificateNo, '')) like lower(concat('%', :keyword, '%')) "
      + "or lower(q.category) like lower(concat('%', :keyword, '%'))) "
      + "and (:subjectCompany = '' or q.subjectCompany = :subjectCompany) "
      + "and q.id not in :hiddenIds "
      + "and (:status = '' "
      + "or (:status = 'VOIDED' and upper(q.manualStatus) = 'VOIDED') "
      + "or (:status = 'LOCKED' and upper(q.manualStatus) <> 'VOIDED' and q.locked = true) "
      + "or (upper(q.manualStatus) <> 'VOIDED' and q.locked = false and ("
      + "(:status = 'UNVERIFIED' and q.validTo is null and q.annualReviewDate is null and q.renewalDate is null) "
      + "or (:status = 'EXPIRED' and (q.validTo < :today or q.annualReviewDate < :today or q.renewalDate < :today)) "
      + "or (:status = 'EXPIRING' and (q.validTo is null or q.validTo >= :today) "
      + "and (q.annualReviewDate is null or q.annualReviewDate >= :today) "
      + "and (q.renewalDate is null or q.renewalDate >= :today) "
      + "and (q.validTo <= :expiringCutoff or q.annualReviewDate <= :expiringCutoff or q.renewalDate <= :expiringCutoff)) "
      + "or (:status = 'VALID' and (q.validTo is not null or q.annualReviewDate is not null or q.renewalDate is not null) "
      + "and (q.validTo is null or q.validTo > :expiringCutoff) "
      + "and (q.annualReviewDate is null or q.annualReviewDate > :expiringCutoff) "
      + "and (q.renewalDate is null or q.renewalDate > :expiringCutoff))))) "
      + "order by q.subjectCompany, q.name")
  Page<CompanyQualification> search(@Param("keyword") String keyword,
      @Param("subjectCompany") String subjectCompany, @Param("status") String status,
      @Param("today") LocalDate today, @Param("expiringCutoff") LocalDate expiringCutoff,
      @Param("hiddenIds") Collection<UUID> hiddenIds, Pageable pageable);
}
