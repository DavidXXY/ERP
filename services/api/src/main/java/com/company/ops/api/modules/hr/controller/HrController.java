package com.company.ops.api.modules.hr.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.hr.dto.HrDtos.EducationRequest;
import com.company.ops.api.modules.hr.dto.HrDtos.EducationResponse;
import com.company.ops.api.modules.hr.dto.HrDtos.WorkExperienceRequest;
import com.company.ops.api.modules.hr.dto.HrDtos.WorkExperienceResponse;
import com.company.ops.api.modules.hr.dto.HrDtos.EmergencyContactRequest;
import com.company.ops.api.modules.hr.dto.HrDtos.EmergencyContactResponse;
import com.company.ops.api.modules.hr.dto.HrDtos.LifecycleRequest;
import com.company.ops.api.modules.hr.dto.HrDtos.LifecycleResponse;
import com.company.ops.api.modules.hr.dto.HrDtos.LeaveRequestData;
import com.company.ops.api.modules.hr.dto.HrDtos.LeaveResponse;
import com.company.ops.api.modules.hr.dto.HrDtos.ApproveRequest;
import com.company.ops.api.modules.hr.dto.HrDtos.HrAnalyticsResponse;
import com.company.ops.api.modules.hr.dto.HrDtos.LeaveBalanceRequest;
import com.company.ops.api.modules.hr.dto.HrDtos.LeaveBalanceResponse;
import com.company.ops.api.modules.hr.service.HrService;
import com.company.ops.api.modules.hr.service.HrExportImportService;
import com.company.ops.api.modules.hr.service.HrExportImportService.ImportResult;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/hr")
public class HrController {

  private final HrService hrService;
  private final HrExportImportService hrExportImportService;

  public HrController(HrService hrService, HrExportImportService hrExportImportService) {
    this.hrService = hrService;
    this.hrExportImportService = hrExportImportService;
  }

  // ====== Analytics ======
  @GetMapping("/analytics")
  @PreAuthorize("hasAuthority('qualification:employee:view')")
  public ApiResponse<HrAnalyticsResponse> analytics() {
    return ApiResponse.ok(hrService.analytics());
  }

  // ====== Education ======
  @GetMapping("/employees/{employeeId}/educations")
  @PreAuthorize("hasAuthority('qualification:employee:view')")
  public ApiResponse<List<EducationResponse>> listEducations(@PathVariable UUID employeeId) {
    return ApiResponse.ok(hrService.listEducations(employeeId));
  }

  @PostMapping("/employees/{employeeId}/educations")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('qualification:employee:manage') and @approvalFlowSecurity.canApprove('LEAVE')")
  public ApiResponse<EducationResponse> createEducation(
      @PathVariable UUID employeeId,
      @Valid @RequestBody EducationRequest request) {
    return ApiResponse.ok(hrService.createEducation(employeeId, request));
  }

  @PutMapping("/educations/{id}")
  @PreAuthorize("hasAuthority('qualification:employee:manage')")
  public ApiResponse<EducationResponse> updateEducation(
      @PathVariable UUID id,
      @Valid @RequestBody EducationRequest request) {
    return ApiResponse.ok(hrService.updateEducation(id, request));
  }

  @DeleteMapping("/educations/{id}")
  @PreAuthorize("hasAuthority('qualification:employee:manage')")
  public ApiResponse<Void> deleteEducation(@PathVariable UUID id) {
    hrService.deleteEducation(id);
    return ApiResponse.ok();
  }

  // ====== Work Experience ======
  @GetMapping("/employees/{employeeId}/work-experiences")
  @PreAuthorize("hasAuthority('qualification:employee:view')")
  public ApiResponse<List<WorkExperienceResponse>> listWorkExperiences(@PathVariable UUID employeeId) {
    return ApiResponse.ok(hrService.listWorkExperiences(employeeId));
  }

  @PostMapping("/employees/{employeeId}/work-experiences")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('qualification:employee:manage')")
  public ApiResponse<WorkExperienceResponse> createWorkExperience(
      @PathVariable UUID employeeId,
      @Valid @RequestBody WorkExperienceRequest request) {
    return ApiResponse.ok(hrService.createWorkExperience(employeeId, request));
  }

  @PutMapping("/work-experiences/{id}")
  @PreAuthorize("hasAuthority('qualification:employee:manage')")
  public ApiResponse<WorkExperienceResponse> updateWorkExperience(
      @PathVariable UUID id,
      @Valid @RequestBody WorkExperienceRequest request) {
    return ApiResponse.ok(hrService.updateWorkExperience(id, request));
  }

  @DeleteMapping("/work-experiences/{id}")
  @PreAuthorize("hasAuthority('qualification:employee:manage')")
  public ApiResponse<Void> deleteWorkExperience(@PathVariable UUID id) {
    hrService.deleteWorkExperience(id);
    return ApiResponse.ok();
  }

  // ====== Emergency Contact ======
  @GetMapping("/employees/{employeeId}/emergency-contacts")
  @PreAuthorize("hasAuthority('qualification:employee:view')")
  public ApiResponse<List<EmergencyContactResponse>> listEmergencyContacts(@PathVariable UUID employeeId) {
    return ApiResponse.ok(hrService.listEmergencyContacts(employeeId));
  }

  @PostMapping("/employees/{employeeId}/emergency-contacts")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('qualification:employee:manage')")
  public ApiResponse<EmergencyContactResponse> createEmergencyContact(
      @PathVariable UUID employeeId,
      @Valid @RequestBody EmergencyContactRequest request) {
    return ApiResponse.ok(hrService.createEmergencyContact(employeeId, request));
  }

  @PutMapping("/emergency-contacts/{id}")
  @PreAuthorize("hasAuthority('qualification:employee:manage')")
  public ApiResponse<EmergencyContactResponse> updateEmergencyContact(
      @PathVariable UUID id,
      @Valid @RequestBody EmergencyContactRequest request) {
    return ApiResponse.ok(hrService.updateEmergencyContact(id, request));
  }

  @DeleteMapping("/emergency-contacts/{id}")
  @PreAuthorize("hasAuthority('qualification:employee:manage')")
  public ApiResponse<Void> deleteEmergencyContact(@PathVariable UUID id) {
    hrService.deleteEmergencyContact(id);
    return ApiResponse.ok();
  }

  // ====== Lifecycle ======
  @GetMapping("/employees/{employeeId}/lifecycles")
  @PreAuthorize("hasAuthority('qualification:employee:view')")
  public ApiResponse<List<LifecycleResponse>> listEmployeeLifecycles(@PathVariable UUID employeeId) {
    return ApiResponse.ok(hrService.listLifecycles(employeeId));
  }

  @GetMapping("/lifecycles")
  @PreAuthorize("hasAuthority('qualification:employee:view')")
  public ApiResponse<List<LifecycleResponse>> listAllLifecycles() {
    return ApiResponse.ok(hrService.listAllLifecycles());
  }

  @PostMapping("/employees/{employeeId}/lifecycles")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('qualification:employee:manage')")
  public ApiResponse<LifecycleResponse> createLifecycle(
      @PathVariable UUID employeeId,
      @Valid @RequestBody LifecycleRequest request) {
    return ApiResponse.ok(hrService.createLifecycle(employeeId, request));
  }

  @PostMapping("/lifecycles/{id}/approve")
  @PreAuthorize("hasAuthority('qualification:employee:manage')")
  public ApiResponse<LifecycleResponse> approveLifecycle(
      @PathVariable UUID id,
      @Valid @RequestBody ApproveRequest request) {
    return ApiResponse.ok(hrService.approveLifecycle(id, request));
  }

  // ====== Leave ======
  @GetMapping("/employees/{employeeId}/leaves")
  @PreAuthorize("hasAuthority('qualification:employee:view')")
  public ApiResponse<List<LeaveResponse>> listEmployeeLeaves(@PathVariable UUID employeeId) {
    return ApiResponse.ok(hrService.listLeaves(employeeId));
  }

  @GetMapping("/leaves")
  @PreAuthorize("hasAuthority('qualification:employee:view')")
  public ApiResponse<List<LeaveResponse>> listAllLeaves() {
    return ApiResponse.ok(hrService.listAllLeaves());
  }

  @PostMapping("/employees/{employeeId}/leaves")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('qualification:employee:manage')")
  public ApiResponse<LeaveResponse> createLeave(
      @PathVariable UUID employeeId,
      @Valid @RequestBody LeaveRequestData request) {
    return ApiResponse.ok(hrService.createLeave(employeeId, request));
  }

  @PostMapping("/leaves/{id}/approve")
  @PreAuthorize("hasAuthority('qualification:employee:manage')")
  public ApiResponse<LeaveResponse> approveLeave(
      @PathVariable UUID id,
      @Valid @RequestBody ApproveRequest request) {
    return ApiResponse.ok(hrService.approveLeave(id, request));
  }

  // ====== Leave Balance ======
  @GetMapping("/employees/{employeeId}/leave-balances")
  @PreAuthorize("hasAuthority('qualification:employee:view')")
  public ApiResponse<List<LeaveBalanceResponse>> listEmployeeLeaveBalances(@PathVariable UUID employeeId) {
    return ApiResponse.ok(hrService.listBalances(employeeId));
  }

  @GetMapping("/leave-balances")
  @PreAuthorize("hasAuthority('qualification:employee:view')")
  public ApiResponse<List<LeaveBalanceResponse>> listAllLeaveBalances() {
    return ApiResponse.ok(hrService.listAllBalances());
  }

  @PostMapping("/employees/{employeeId}/leave-balances")
  @PreAuthorize("hasAuthority('qualification:employee:manage')")
  public ApiResponse<LeaveBalanceResponse> setLeaveBalance(
      @PathVariable UUID employeeId,
      @Valid @RequestBody LeaveBalanceRequest request) {
    return ApiResponse.ok(hrService.setBalance(employeeId, request));
  }

  @PostMapping("/employees/{employeeId}/init-leave-balances")
  @PreAuthorize("hasAuthority('qualification:employee:manage')")
  public ApiResponse<Void> initLeaveBalances(@PathVariable UUID employeeId) {
    hrService.batchInitBalances(employeeId);
    return ApiResponse.ok();
  }

  // ====== Excel Export ======
  @GetMapping("/export/employees")
  @PreAuthorize("hasAuthority('qualification:employee:view')")
  public ResponseEntity<Resource> exportEmployeesExcel() {
    var result = hrExportImportService.exportEmployeesExcel();
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"hr-employees.xlsx\"")
        .body(result);
  }

  @GetMapping("/export/template")
  @PreAuthorize("hasAuthority('qualification:employee:view')")
  public ResponseEntity<Resource> downloadImportTemplate() {
    var result = hrExportImportService.downloadImportTemplate();
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"hr-import-template.xlsx\"")
        .body(result);
  }

  @GetMapping("/export/leave-balance-template")
  @PreAuthorize("hasAuthority('hr:employee:manage')")
  public ResponseEntity<Resource> downloadLeaveBalanceTemplate() {
    var result = hrExportImportService.exportLeaveBalanceTemplate();
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"leave-balance-template.xlsx\"")
        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(result);
  }

  // ====== Excel Import ======
  @PostMapping("/import/leave-balances")
  @PreAuthorize("hasAuthority('hr:employee:manage')")
  public ApiResponse<ImportResult> importLeaveBalances(
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "operatorName", required = false) String operatorName) {
    var result = hrExportImportService.importLeaveBalances(file, operatorName);
    return ApiResponse.ok(result);
  }

  @PostMapping("/import/employees")
  @PreAuthorize("hasAuthority('qualification:employee:manage')")
  public ApiResponse<ImportResult> importEmployeesExcel(
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "operatorName", required = false) String operatorName) {
    var result = hrExportImportService.importEmployeesExcel(file, operatorName);
    return ApiResponse.ok(result);
  }
}
