package com.company.ops.api.modules.qualification.repository;

import com.company.ops.api.modules.qualification.domain.PersonnelCertificate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PersonnelCertificateRepository extends JpaRepository<PersonnelCertificate, UUID> {
  Optional<PersonnelCertificate> findFirstByAttachmentsJsonContaining(String value);
  List<PersonnelCertificate> findAllByOrderByEmployeeNameAscNameAsc();
  List<PersonnelCertificate> findByEmployeeIdOrderByNameAsc(UUID employeeId);
  Optional<PersonnelCertificate> findByExternalId(String externalId);
  List<PersonnelCertificate> findByEmployee_IdIn(Collection<UUID> employeeIds);
  @Query(value = "select c from PersonnelCertificate c join fetch c.employee e where "
      + "(:keyword = '' or lower(c.name) like lower(concat('%', :keyword, '%')) "
      + "or lower(coalesce(c.certificateNo, '')) like lower(concat('%', :keyword, '%')) "
      + "or lower(e.name) like lower(concat('%', :keyword, '%')) "
      + "or lower(coalesce(c.specialty, '')) like lower(concat('%', :keyword, '%'))) "
      + "and (:specialty = '' or c.specialty = :specialty) "
      + "and (:registered is null or c.companyRegistered = :registered) "
      + "and c.id not in :hiddenIds "
      + "and (:status = '' "
      + "or (:status = 'VOIDED' and upper(c.manualStatus) = 'VOIDED') "
      + "or (:status = 'LOCKED' and upper(c.manualStatus) <> 'VOIDED' and c.locked = true) "
      + "or (upper(c.manualStatus) <> 'VOIDED' and c.locked = false and ("
      + "(:status = 'UNVERIFIED' and c.validTo is null and c.reviewDate is null) "
      + "or (:status = 'EXPIRED' and (c.validTo < :today or c.reviewDate < :today)) "
      + "or (:status = 'EXPIRING' and (c.validTo is null or c.validTo >= :today) "
      + "and (c.reviewDate is null or c.reviewDate >= :today) "
      + "and (c.validTo <= :expiringCutoff or c.reviewDate <= :expiringCutoff)) "
      + "or (:status = 'VALID' and (c.validTo is not null or c.reviewDate is not null) "
      + "and (c.validTo is null or c.validTo > :expiringCutoff) "
      + "and (c.reviewDate is null or c.reviewDate > :expiringCutoff))))) "
      + "order by e.name, c.name",
      countQuery = "select count(c) from PersonnelCertificate c join c.employee e where "
      + "(:keyword = '' or lower(c.name) like lower(concat('%', :keyword, '%')) "
      + "or lower(coalesce(c.certificateNo, '')) like lower(concat('%', :keyword, '%')) "
      + "or lower(e.name) like lower(concat('%', :keyword, '%')) "
      + "or lower(coalesce(c.specialty, '')) like lower(concat('%', :keyword, '%'))) "
      + "and (:specialty = '' or c.specialty = :specialty) "
      + "and (:registered is null or c.companyRegistered = :registered) "
      + "and c.id not in :hiddenIds "
      + "and (:status = '' "
      + "or (:status = 'VOIDED' and upper(c.manualStatus) = 'VOIDED') "
      + "or (:status = 'LOCKED' and upper(c.manualStatus) <> 'VOIDED' and c.locked = true) "
      + "or (upper(c.manualStatus) <> 'VOIDED' and c.locked = false and ("
      + "(:status = 'UNVERIFIED' and c.validTo is null and c.reviewDate is null) "
      + "or (:status = 'EXPIRED' and (c.validTo < :today or c.reviewDate < :today)) "
      + "or (:status = 'EXPIRING' and (c.validTo is null or c.validTo >= :today) "
      + "and (c.reviewDate is null or c.reviewDate >= :today) "
      + "and (c.validTo <= :expiringCutoff or c.reviewDate <= :expiringCutoff)) "
      + "or (:status = 'VALID' and (c.validTo is not null or c.reviewDate is not null) "
      + "and (c.validTo is null or c.validTo > :expiringCutoff) "
      + "and (c.reviewDate is null or c.reviewDate > :expiringCutoff)))))")
  Page<PersonnelCertificate> search(@Param("keyword") String keyword,
      @Param("specialty") String specialty, @Param("status") String status,
      @Param("registered") Boolean registered, @Param("today") LocalDate today,
      @Param("expiringCutoff") LocalDate expiringCutoff,
      @Param("hiddenIds") Collection<UUID> hiddenIds, Pageable pageable);
  @Query("select c.externalId from PersonnelCertificate c where c.externalId is not null")
  List<String> findAllExternalIds();

}
