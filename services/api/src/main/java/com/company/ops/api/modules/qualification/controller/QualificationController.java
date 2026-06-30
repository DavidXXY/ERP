package com.company.ops.api.modules.qualification.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.CompanyQualificationRequest;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.Attachment;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.CompanyQualificationResponse;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.DashboardResponse;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.EmployeeDetailResponse;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.EmployeeContractRequest;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.EmployeeContractResponse;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.EmployeeRequest;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.EmployeeResponse;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.PerformanceRequest;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.PerformanceResponse;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.PersonnelCertificateRequest;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.PersonnelCertificateResponse;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.ReferenceDataResponse;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.TenderEmployeeResponse;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.WarningResponse;
import com.company.ops.api.modules.qualification.service.QualificationService;
import com.company.ops.api.modules.qualification.service.QualificationAttachmentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/qualifications")
public class QualificationController {
  private final QualificationService service;
  private final QualificationAttachmentService attachmentService;
  public QualificationController(QualificationService service, QualificationAttachmentService attachmentService) {
    this.service = service;
    this.attachmentService = attachmentService;
  }

  @GetMapping("/dashboard") @PreAuthorize("hasAuthority('qualification:view')")
  public ApiResponse<DashboardResponse> dashboard() { return ApiResponse.ok(service.dashboard()); }

  @GetMapping("/references") @PreAuthorize("hasAuthority('qualification:view')")
  public ApiResponse<ReferenceDataResponse> references() { return ApiResponse.ok(service.references()); }

  @GetMapping("/companies") @PreAuthorize("hasAuthority('qualification:company:view')")
  public ApiResponse<List<CompanyQualificationResponse>> companies(
      @RequestParam(required = false) String keyword, @RequestParam(required = false) String subjectCompany,
      @RequestParam(required = false) String status) {
    return ApiResponse.ok(service.listCompanies(keyword, subjectCompany, status));
  }
  @PostMapping("/companies") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('qualification:company:manage')")
  public ApiResponse<CompanyQualificationResponse> createCompany(@Valid @RequestBody CompanyQualificationRequest request) {
    return ApiResponse.ok(service.createCompany(request));
  }
  @PutMapping("/companies/{id}") @PreAuthorize("hasAuthority('qualification:company:manage')")
  public ApiResponse<CompanyQualificationResponse> updateCompany(@PathVariable UUID id, @Valid @RequestBody CompanyQualificationRequest request) {
    return ApiResponse.ok(service.updateCompany(id, request));
  }
  @DeleteMapping("/companies/{id}") @PreAuthorize("hasAuthority('qualification:company:manage')")
  public ApiResponse<Void> deleteCompany(@PathVariable UUID id) { service.deleteCompany(id); return ApiResponse.ok(); }

  @GetMapping("/employees") @PreAuthorize("hasAuthority('qualification:employee:view')")
  public ApiResponse<List<EmployeeResponse>> employees(
      @RequestParam(required = false) String keyword, @RequestParam(required = false) String employmentStatus) {
    return ApiResponse.ok(service.listEmployees(keyword, employmentStatus));
  }
  @GetMapping("/employees/{id}") @PreAuthorize("hasAuthority('qualification:employee:view')")
  public ApiResponse<EmployeeDetailResponse> employee(@PathVariable UUID id) { return ApiResponse.ok(service.employeeDetail(id)); }
  @PostMapping("/employees") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('qualification:employee:manage')")
  public ApiResponse<EmployeeResponse> createEmployee(@Valid @RequestBody EmployeeRequest request) { return ApiResponse.ok(service.createEmployee(request)); }
  @PutMapping("/employees/{id}") @PreAuthorize("hasAuthority('qualification:employee:manage')")
  public ApiResponse<EmployeeResponse> updateEmployee(@PathVariable UUID id, @Valid @RequestBody EmployeeRequest request) { return ApiResponse.ok(service.updateEmployee(id, request)); }
  @DeleteMapping("/employees/{id}") @PreAuthorize("hasAuthority('qualification:employee:manage')")
  public ApiResponse<Void> deleteEmployee(@PathVariable UUID id) { service.deleteEmployee(id); return ApiResponse.ok(); }

  @GetMapping("/employees/{id}/contracts") @PreAuthorize("hasAuthority('qualification:employee:view')")
  public ApiResponse<List<EmployeeContractResponse>> employeeContracts(@PathVariable UUID id) {
    return ApiResponse.ok(service.listEmployeeContracts(id));
  }
  @PostMapping("/employees/{id}/contracts") @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('qualification:employee:manage')")
  public ApiResponse<EmployeeContractResponse> createEmployeeContract(
      @PathVariable UUID id, @Valid @RequestBody EmployeeContractRequest request) {
    return ApiResponse.ok(service.createEmployeeContract(id, request));
  }
  @PutMapping("/employee-contracts/{id}") @PreAuthorize("hasAuthority('qualification:employee:manage')")
  public ApiResponse<EmployeeContractResponse> updateEmployeeContract(
      @PathVariable UUID id, @Valid @RequestBody EmployeeContractRequest request) {
    return ApiResponse.ok(service.updateEmployeeContract(id, request));
  }
  @DeleteMapping("/employee-contracts/{id}") @PreAuthorize("hasAuthority('qualification:employee:manage')")
  public ApiResponse<Void> deleteEmployeeContract(@PathVariable UUID id) {
    service.deleteEmployeeContract(id); return ApiResponse.ok();
  }

  @GetMapping("/certificates") @PreAuthorize("hasAuthority('qualification:certificate:view')")
  public ApiResponse<List<PersonnelCertificateResponse>> certificates(
      @RequestParam(required = false) String keyword, @RequestParam(required = false) String specialty,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Boolean companyRegistered) {
    return ApiResponse.ok(service.listCertificates(keyword, specialty, status, companyRegistered));
  }
  @PostMapping("/certificates") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('qualification:certificate:manage')")
  public ApiResponse<PersonnelCertificateResponse> createCertificate(@Valid @RequestBody PersonnelCertificateRequest request) { return ApiResponse.ok(service.createCertificate(request)); }
  @PutMapping("/certificates/{id}") @PreAuthorize("hasAuthority('qualification:certificate:manage')")
  public ApiResponse<PersonnelCertificateResponse> updateCertificate(@PathVariable UUID id, @Valid @RequestBody PersonnelCertificateRequest request) { return ApiResponse.ok(service.updateCertificate(id, request)); }
  @DeleteMapping("/certificates/{id}") @PreAuthorize("hasAuthority('qualification:certificate:manage')")
  public ApiResponse<Void> deleteCertificate(@PathVariable UUID id) { service.deleteCertificate(id); return ApiResponse.ok(); }

  @GetMapping("/performances") @PreAuthorize("hasAuthority('qualification:performance:view')")
  public ApiResponse<List<PerformanceResponse>> performances(
      @RequestParam(required = false) String keyword, @RequestParam(required = false) String subjectCompany,
      @RequestParam(required = false) String projectType) {
    return ApiResponse.ok(service.listPerformances(keyword, subjectCompany, projectType));
  }
  @PostMapping("/performances") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('qualification:performance:manage')")
  public ApiResponse<PerformanceResponse> createPerformance(@Valid @RequestBody PerformanceRequest request) { return ApiResponse.ok(service.createPerformance(request)); }
  @PutMapping("/performances/{id}") @PreAuthorize("hasAuthority('qualification:performance:manage')")
  public ApiResponse<PerformanceResponse> updatePerformance(@PathVariable UUID id, @Valid @RequestBody PerformanceRequest request) { return ApiResponse.ok(service.updatePerformance(id, request)); }
  @DeleteMapping("/performances/{id}") @PreAuthorize("hasAuthority('qualification:performance:manage')")
  public ApiResponse<Void> deletePerformance(@PathVariable UUID id) { service.deletePerformance(id); return ApiResponse.ok(); }

  @GetMapping("/tender") @PreAuthorize("hasAuthority('qualification:tender:view')")
  public ApiResponse<List<TenderEmployeeResponse>> tender(
      @RequestParam(required = false) String keyword, @RequestParam(required = false) List<String> specialties,
      @RequestParam(defaultValue = "false") boolean registeredOnly,
      @RequestParam(defaultValue = "true") boolean availableOnly) {
    return ApiResponse.ok(service.tenderSearch(keyword, specialties, registeredOnly, availableOnly));
  }

  @GetMapping("/warnings") @PreAuthorize("hasAuthority('qualification:warning:view')")
  public ApiResponse<List<WarningResponse>> warnings() { return ApiResponse.ok(service.warnings()); }

  @PostMapping(value = "/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyAuthority('qualification:company:manage','qualification:employee:manage','qualification:certificate:manage','qualification:performance:manage')")
  public ApiResponse<Attachment> uploadAttachment(@RequestPart MultipartFile file,
                                                   @RequestParam(defaultValue = "当前用户") String operatorName) {
    return ApiResponse.ok(attachmentService.store(file, operatorName));
  }
}
