package com.company.ops.api.modules.maintenance.repository;

import com.company.ops.api.modules.maintenance.domain.EmployeeCertificate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeCertificateRepository extends JpaRepository<EmployeeCertificate, UUID> {
  List<EmployeeCertificate> findAllByOrderByExpiryDateAsc();
  List<EmployeeCertificate> findByUserIdOrderByExpiryDateAsc(UUID userId);
  boolean existsByCertificateNo(String certificateNo);
}
