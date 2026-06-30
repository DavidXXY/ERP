package com.company.ops.api.modules.qualification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class QualificationDtos {
  private QualificationDtos() {}

  public record Attachment(String id, String name, String type, long size, String dataUrl,
                           String uploadedAt, String uploadedBy) {}
  public record CategoryCount(String name, long count) {}

  public record CompanyQualificationRequest(
      @NotBlank @Size(max = 160) String subjectCompany,
      @NotBlank @Size(max = 200) String name,
      @NotBlank @Size(max = 100) String category,
      @Size(max = 160) String level, @Size(max = 180) String certificateNo,
      @Size(max = 240) String issuer, LocalDate issueDate, LocalDate validFrom, LocalDate validTo,
      LocalDate annualReviewDate, LocalDate renewalDate, @Size(max = 4000) String scope,
      List<String> projectTypes, @Size(max = 240) String holderBranch,
      @Size(max = 500) String storageLocation, boolean availableForTender,
      @Size(max = 32) String manualStatus, boolean locked, List<Attachment> attachments,
      @Size(max = 1000) String remark
  ) {}

  public record CompanyQualificationResponse(
      UUID id, String subjectCompany, String name, String category, String level,
      String certificateNo, String issuer, LocalDate issueDate, LocalDate validFrom, LocalDate validTo,
      LocalDate annualReviewDate, LocalDate renewalDate, String scope, List<String> projectTypes,
      String holderBranch, String storageLocation, boolean availableForTender, String manualStatus,
      boolean locked, List<Attachment> attachments, String remark, String status, Long daysLeft
  ) {}

  public record EmployeeRequest(
      @NotBlank @Size(max = 80) String name, @Size(max = 64) String workNo,
      UUID organizationId, @Size(max = 120) String department, @Size(max = 120) String position,
      @Size(max = 32) String idCard, @Size(max = 40) String phone, LocalDate entryDate,
      @NotBlank @Size(max = 32) String employmentStatus, LocalDate contractStart,
      LocalDate contractEnd, @Size(max = 160) String socialSecurityUnit,
      LocalDate socialSecurityStart, LocalDate socialSecurityEnd, @Size(max = 1000) String remark,
      UUID systemUserId
  ) {}

  public record EmployeeAccountResponse(
      UUID id, String username, String displayName, String phone, String email,
      boolean enabled, List<String> roles
  ) {}

  public record EmployeeResponse(
      UUID id, String name, String workNo, UUID organizationId, String organizationName,
      String organizationPath, String department, String position,
      String idCard, String phone, LocalDate entryDate, String employmentStatus, LocalDate contractStart,
      LocalDate contractEnd, String socialSecurityUnit, LocalDate socialSecurityStart,
      LocalDate socialSecurityEnd, String remark, UUID systemUserId, EmployeeAccountResponse account,
      long certificateCount, long validCertificateCount
  ) {}

  public record EmployeeContractRequest(
      @NotBlank @Size(max = 100) String contractNo,
      @NotBlank @Size(max = 80) String contractType,
      LocalDate signDate, @NotNull LocalDate startDate, LocalDate endDate,
      LocalDate probationEndDate, @NotBlank @Size(max = 32) String status,
      List<Attachment> attachments, @Size(max = 1000) String remark
  ) {}

  public record EmployeeContractResponse(
      UUID id, UUID employeeId, String contractNo, String contractType, LocalDate signDate,
      LocalDate startDate, LocalDate endDate, LocalDate probationEndDate, String status,
      List<Attachment> attachments, String remark, Long daysLeft
  ) {}

  public record EmployeeDetailResponse(
      EmployeeResponse employee, List<EmployeeContractResponse> contracts,
      List<PersonnelCertificateResponse> certificates
  ) {}

  public record PersonnelCertificateRequest(
      @NotNull UUID employeeId, @NotBlank @Size(max = 200) String name,
      @Size(max = 100) String type, @Size(max = 180) String certificateNo,
      @Size(max = 180) String specialty, boolean companyRegistered, LocalDate issueDate,
      LocalDate validTo, LocalDate reviewDate, boolean availableForTender,
      @Size(max = 32) String manualStatus, boolean locked, List<Attachment> attachments,
      @Size(max = 1000) String remark
  ) {}

  public record PersonnelCertificateResponse(
      UUID id, UUID employeeId, String employeeName, String name, String type,
      String certificateNo, String specialty, boolean companyRegistered, LocalDate issueDate,
      LocalDate validTo, LocalDate reviewDate, boolean availableForTender, String manualStatus,
      boolean locked, List<Attachment> attachments, String remark, String status, Long daysLeft
  ) {}

  public record PerformanceRequest(
      @NotBlank @Size(max = 160) String subjectCompany,
      @NotBlank @Size(max = 500) String name, @Size(max = 240) String clientName,
      @Size(max = 160) String contractNo, LocalDate contractDate,
      @Size(max = 100) String contractAmount, @Size(max = 160) String projectType,
      List<Attachment> attachments, @Size(max = 1000) String remark
  ) {}

  public record PerformanceResponse(
      UUID id, String subjectCompany, String name, String clientName, String contractNo,
      LocalDate contractDate, String contractAmount, String projectType,
      List<Attachment> attachments, String remark
  ) {}

  public record WarningResponse(
      String sourceType, UUID sourceId, String sourceName, String warningType, String title,
      LocalDate dueDate, long daysLeft, String level, String status
  ) {}

  public record DashboardResponse(
      long companyQualificationCount, long employeeCount, long certificateCount,
      long tenderAvailableCertificateCount, long pendingWarningCount, long expiredCount,
      List<CategoryCount> companyCategoryDistribution,
      List<CategoryCount> certificateSpecialtyDistribution, List<WarningResponse> recentWarnings
  ) {}

  public record ReferenceDataResponse(
      List<String> subjectCompanies, List<String> qualificationCategories,
      List<String> certificateTypes, List<String> specialties, List<String> projectTypes,
      List<EmployeeOption> employees, List<OrganizationOption> organizations
  ) {}
  public record EmployeeOption(UUID id, String name, String workNo) {}
  public record OrganizationOption(UUID id, String name, String fullPath, boolean enabled) {}
  public record TenderEmployeeResponse(
      UUID employeeId, String employeeName, String workNo,
      String department, String position, List<PersonnelCertificateResponse> certificates
  ) {}
}
