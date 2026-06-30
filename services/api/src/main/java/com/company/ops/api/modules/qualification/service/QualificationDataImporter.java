package com.company.ops.api.modules.qualification.service;

import com.company.ops.api.modules.qualification.domain.CompanyQualification;
import com.company.ops.api.modules.qualification.domain.PersonnelCertificate;
import com.company.ops.api.modules.qualification.domain.QualificationEmployee;
import com.company.ops.api.modules.qualification.domain.QualificationPerformance;
import com.company.ops.api.modules.qualification.repository.CompanyQualificationRepository;
import com.company.ops.api.modules.qualification.repository.PersonnelCertificateRepository;
import com.company.ops.api.modules.qualification.repository.QualificationEmployeeRepository;
import com.company.ops.api.modules.qualification.repository.QualificationPerformanceRepository;
import com.company.ops.api.modules.system.domain.SystemOrganization;
import com.company.ops.api.modules.system.repository.SystemOrganizationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class QualificationDataImporter {
  private static final Logger log = LoggerFactory.getLogger(QualificationDataImporter.class);
  private final ObjectMapper objectMapper;
  private final QualificationEmployeeRepository employeeRepository;
  private final CompanyQualificationRepository companyRepository;
  private final PersonnelCertificateRepository certificateRepository;
  private final QualificationPerformanceRepository performanceRepository;
  private final SystemOrganizationRepository organizationRepository;

  public QualificationDataImporter(ObjectMapper objectMapper, QualificationEmployeeRepository employeeRepository,
                                   CompanyQualificationRepository companyRepository,
                                   PersonnelCertificateRepository certificateRepository,
                                   QualificationPerformanceRepository performanceRepository,
                                   SystemOrganizationRepository organizationRepository) {
    this.objectMapper = objectMapper;
    this.employeeRepository = employeeRepository;
    this.companyRepository = companyRepository;
    this.certificateRepository = certificateRepository;
    this.performanceRepository = performanceRepository;
    this.organizationRepository = organizationRepository;
  }

  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void importLegacyData() {
    try {
      JsonNode root = objectMapper.readTree(new ClassPathResource("qualification-import.json").getInputStream());
      Map<String, QualificationEmployee> employees = importEmployees(root.path("employees"));
      importCompanyQualifications(root.path("companyQualifications"));
      importCertificates(root.path("certificates"), employees);
      importPerformances(root.path("performances"));
      log.info("Qualification data ready: {} employees, {} certificates, {} company qualifications, {} performances",
          employeeRepository.count(), certificateRepository.count(), companyRepository.count(), performanceRepository.count());
    } catch (IOException exception) {
      throw new IllegalStateException("资质历史数据导入失败", exception);
    }
  }

  private Map<String, QualificationEmployee> importEmployees(JsonNode nodes) {
    Map<String, QualificationEmployee> result = new HashMap<>();
    for (JsonNode node : nodes) {
      String externalId = text(node, "id");
      var existing = employeeRepository.findByExternalId(externalId);
      if (existing.isPresent()) {
        QualificationEmployee employee = existing.get();
        if (employee.getOrganization() == null) {
          SystemOrganization organization = importedOrganization(employee.getDepartment());
          employee.setOrganization(organization);
          if (organization != null) employee.setDepartment(organization.getName());
          employeeRepository.save(employee);
        }
        result.put(externalId, employee);
        continue;
      }
      QualificationEmployee item = new QualificationEmployee();
      item.setExternalId(externalId); item.setName(text(node, "name"));
      String importedDepartment = text(node, "department");
      SystemOrganization organization = importedOrganization(importedDepartment);
      item.setWorkNo(text(node, "workNo")); item.setOrganization(organization);
      item.setDepartment(organization == null ? null : organization.getName()); item.setPosition(text(node, "position"));
      item.setIdCard(text(node, "idCard")); item.setPhone(text(node, "phone")); item.setEntryDate(date(node, "entryDate"));
      item.setEmploymentStatus(normalizeEmployment(text(node, "employmentStatus")));
      item.setContractStart(date(node, "contractStart")); item.setContractEnd(date(node, "contractEnd"));
      item.setSocialSecurityUnit(text(node, "socialSecurityUnit")); item.setSocialSecurityStart(date(node, "socialSecurityStart"));
      item.setSocialSecurityEnd(date(node, "socialSecurityEnd")); item.setRemark(text(node, "remark"));
      result.put(externalId, employeeRepository.save(item));
    }
    return result;
  }

  private void importCompanyQualifications(JsonNode nodes) {
    for (JsonNode node : nodes) {
      String externalId = text(node, "id");
      if (companyRepository.findByExternalId(externalId).isPresent()) continue;
      CompanyQualification item = new CompanyQualification();
      item.setExternalId(externalId); item.setSubjectCompany(text(node, "subjectCompany")); item.setName(text(node, "name"));
      item.setCategory(text(node, "category")); item.setLevel(text(node, "level")); item.setCertificateNo(text(node, "certificateNo"));
      item.setIssuer(text(node, "issuer")); item.setIssueDate(date(node, "issueDate")); item.setValidFrom(date(node, "validFrom"));
      item.setValidTo(date(node, "validTo")); item.setAnnualReviewDate(date(node, "annualReviewDate"));
      item.setRenewalDate(date(node, "renewalDate")); item.setScope(text(node, "scope"));
      item.setProjectTypesJson(json(node.path("projectTypes"))); item.setHolderBranch(text(node, "holderBranch"));
      item.setStorageLocation(text(node, "storageLocation")); item.setAvailableForTender(bool(node, "availableForTender", true));
      item.setManualStatus(normalizeStatus(text(node, "manualStatus"))); item.setLocked(bool(node, "locked", false));
      item.setAttachmentsJson(json(node.path("attachments"))); item.setRemark(text(node, "remark"));
      companyRepository.save(item);
    }
  }

  private void importCertificates(JsonNode nodes, Map<String, QualificationEmployee> employees) {
    for (JsonNode node : nodes) {
      QualificationEmployee employee = employees.get(text(node, "employeeId"));
      if (employee == null) continue;
      String externalId = text(node, "id");
      if (certificateRepository.findByExternalId(externalId).isPresent()) continue;
      PersonnelCertificate item = new PersonnelCertificate();
      item.setExternalId(externalId); item.setEmployee(employee); item.setName(text(node, "name")); item.setType(text(node, "type"));
      item.setCertificateNo(text(node, "certNo")); item.setSpecialty(text(node, "specialty"));
      item.setCompanyRegistered(bool(node, "isCompanyRegistered", false)); item.setIssueDate(date(node, "issueDate"));
      item.setValidTo(date(node, "validTo")); item.setReviewDate(date(node, "reviewDate"));
      item.setAvailableForTender(bool(node, "availableForTender", true)); item.setManualStatus(normalizeStatus(text(node, "manualStatus")));
      item.setLocked(bool(node, "locked", false)); item.setAttachmentsJson(json(node.path("attachments")));
      item.setRemark(text(node, "remark")); certificateRepository.save(item);
    }
  }

  private void importPerformances(JsonNode nodes) {
    for (JsonNode node : nodes) {
      String externalId = text(node, "id");
      if (performanceRepository.findByExternalId(externalId).isPresent()) continue;
      QualificationPerformance item = new QualificationPerformance();
      item.setExternalId(externalId); item.setSubjectCompany(text(node, "subjectCompany")); item.setName(text(node, "name"));
      item.setClientName(text(node, "clientName")); item.setContractNo(text(node, "contractNo"));
      item.setContractDate(date(node, "contractDate")); item.setContractAmount(text(node, "contractAmount"));
      item.setProjectType(text(node, "projectType")); item.setAttachmentsJson(json(node.path("attachments")));
      item.setRemark(text(node, "remark")); performanceRepository.save(item);
    }
  }

  private String text(JsonNode node, String field) { String value = node.path(field).asText("").trim(); return value.isEmpty() ? null : value; }
  private boolean bool(JsonNode node, String field, boolean fallback) { return node.has(field) ? node.path(field).asBoolean(fallback) : fallback; }
  private String json(JsonNode node) { return node.isMissingNode() || node.isNull() ? "[]" : node.toString(); }
  private LocalDate date(JsonNode node, String field) {
    String value = text(node, field); if (value == null) return null;
    try { return LocalDate.parse(value.length() >= 10 ? value.substring(0, 10) : value); }
    catch (DateTimeParseException ignored) { return null; }
  }
  private String normalizeStatus(String value) { return value == null ? "NORMAL" : value.toUpperCase(); }
  private String normalizeEmployment(String value) { return value == null ? "ACTIVE" : value.toUpperCase(); }

  private SystemOrganization importedOrganization(String department) {
    if (department == null || department.isBlank() || "未分配".equals(department)) return null;
    for (SystemOrganization organization : organizationRepository.findByTenantIdOrderBySortOrderAsc("default")) {
      if (department.equals(organization.getName())) return organization;
    }
    String code = department.contains("财务") ? "FINANCE_DEPARTMENT"
        : department.contains("采购") ? "PROCUREMENT_DEPARTMENT"
        : department.contains("市场") ? "MARKETING_DEPARTMENT"
        : department.contains("销售") || department.contains("客户") ? "DEPT_SALES"
        : "TECHNICAL_SERVICE_DEPARTMENT";
    return organizationRepository.findByCodeAndTenantId(code, "default");
  }
}
