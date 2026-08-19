package com.company.ops.api.modules.crm.repository;

import com.company.ops.api.modules.crm.domain.Customer;
import com.company.ops.api.modules.crm.domain.CustomerLevel;
import com.company.ops.api.modules.crm.domain.RiskStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
  List<Customer> findAllByOrderByCreatedAtDesc();
  List<Customer> findByOwnerUserIdInOrderByCreatedAtDesc(Collection<UUID> ownerUserIds);

  @Query("""
      select c from Customer c
      where c.ownerUserId in :ownerUserIds
        and (:keyword is null
          or lower(c.name) like lower(concat('%', :keyword, '%'))
          or lower(c.code) like lower(concat('%', :keyword, '%'))
          or lower(c.industry) like lower(concat('%', :keyword, '%'))
          or lower(c.ownerName) like lower(concat('%', :keyword, '%')))
        and (:level is null or c.level = :level)
        and (:riskStatus is null or c.riskStatus = :riskStatus)
        and (:ownerNames is empty or c.ownerName in :ownerNames)
      order by c.createdAt desc
  """)
  Page<Customer> searchVisibleByOwnerUserIdIn(
      @Param("ownerUserIds") Collection<UUID> ownerUserIds,
      @Param("keyword") String keyword,
      @Param("level") CustomerLevel level,
      @Param("riskStatus") RiskStatus riskStatus,
      @Param("ownerNames") Collection<String> ownerNames,
      Pageable pageable);

  boolean existsByCode(String code);
  Optional<Customer> findByCode(String code);
  List<Customer> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String name,String code,Pageable pageable);
}
