package com.company.ops.api.modules.hr.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.hr.dto.HrDtos.*;
import com.company.ops.api.modules.hr.service.HrService;
import com.company.ops.api.modules.office.dto.OfficeDtos.ApprovalResponse;
import com.company.ops.api.modules.office.dto.OfficeDtos.ProcessApprovalRequest;
import com.company.ops.api.modules.office.service.OfficeService;
import com.company.ops.api.modules.qualification.domain.QualificationEmployee;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.*;
import com.company.ops.api.modules.qualification.repository.QualificationEmployeeRepository;
import com.company.ops.api.modules.system.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hr/self")
public class HrSelfController {
    private final HrService hrService;
    private final QualificationEmployeeRepository employeeRepository;
    private final OfficeService officeService;

    public HrSelfController(HrService hrService, QualificationEmployeeRepository employeeRepository, OfficeService officeService) {
        this.hrService = hrService;
        this.employeeRepository = employeeRepository;
        this.officeService = officeService;
    }

    // Returns empty if no employee linked - avoids 400 errors disrupting the dashboard
    private java.util.Optional<QualificationEmployee> findEmployee(UUID userId) {
        return employeeRepository.findBySystemUser_Id(userId);
    }

    // List endpoint helper - returns empty list when no employee linked
    private <T> ApiResponse<List<T>> listOrEmpty(UUID userId, java.util.function.Function<QualificationEmployee, List<T>> fn) {
        return findEmployee(userId).map(e -> ApiResponse.ok(fn.apply(e)))
            .orElse(ApiResponse.ok(List.of()));
    }

    // Single endpoint helper - returns null when no employee linked
    private <T> ApiResponse<T> itemOrNull(UUID userId, java.util.function.Function<QualificationEmployee, T> fn) {
        return findEmployee(userId).map(e -> ApiResponse.ok(fn.apply(e)))
            .orElse(ApiResponse.ok(null));
    }

    @GetMapping("/profile")
    public ApiResponse<EmployeeDetailResponse> profile(@AuthenticationPrincipal UserPrincipal principal) {
        var empOpt = findEmployee(principal.id());
        if (empOpt.isEmpty()) return ApiResponse.ok(null);
        return ApiResponse.ok(hrService.getEmployeeDetail(empOpt.get().getId()));
    }

    @GetMapping("/leave-balances")
    public ApiResponse<List<LeaveBalanceResponse>> leaveBalances(@AuthenticationPrincipal UserPrincipal principal) {
        return listOrEmpty(principal.id(), e -> hrService.listBalances(e.getId()));
    }

    @GetMapping("/leaves")
    public ApiResponse<List<LeaveResponse>> leaves(@AuthenticationPrincipal UserPrincipal principal) {
        return listOrEmpty(principal.id(), e -> hrService.listLeaves(e.getId()));
    }

    @PostMapping("/leaves")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LeaveResponse> createLeave(@AuthenticationPrincipal UserPrincipal principal,
                                                   @Valid @RequestBody LeaveRequestData request) {
        var empOpt = findEmployee(principal.id());
        if (empOpt.isEmpty()) {
            throw new com.company.ops.api.common.exception.BusinessException("未找到员工档案，请联系管理员关联账号后再提交请假");
        }
        return ApiResponse.ok(hrService.createLeave(empOpt.get().getId(), request));
    }

    @GetMapping("/lifecycles")
    public ApiResponse<List<LifecycleResponse>> lifecycles(@AuthenticationPrincipal UserPrincipal principal) {
        return listOrEmpty(principal.id(), e -> hrService.listLifecycles(e.getId()));
    }

    @GetMapping("/educations")
    public ApiResponse<List<EducationResponse>> educations(@AuthenticationPrincipal UserPrincipal principal) {
        return listOrEmpty(principal.id(), e -> hrService.listEducations(e.getId()));
    }

    @GetMapping("/work-experiences")
    public ApiResponse<List<WorkExperienceResponse>> workExperiences(@AuthenticationPrincipal UserPrincipal principal) {
        return listOrEmpty(principal.id(), e -> hrService.listWorkExperiences(e.getId()));
    }

    @GetMapping("/emergency-contacts")
    public ApiResponse<List<EmergencyContactResponse>> emergencyContacts(@AuthenticationPrincipal UserPrincipal principal) {
        return listOrEmpty(principal.id(), e -> hrService.listEmergencyContacts(e.getId()));
    }

    @GetMapping("/certificates")
    public ApiResponse<List<PersonnelCertificateResponse>> certificates(@AuthenticationPrincipal UserPrincipal principal) {
        return listOrEmpty(principal.id(), e -> hrService.listEmployeeCertificates(e.getId()));
    }

    @GetMapping("/contracts")
    public ApiResponse<List<EmployeeContractResponse>> contracts(@AuthenticationPrincipal UserPrincipal principal) {
        return listOrEmpty(principal.id(), e -> hrService.listEmployeeContracts(e.getId()));
    }

    @GetMapping("/todos")
    public ApiResponse<java.util.List<com.company.ops.api.modules.hr.dto.HrDtos.TodoItem>> todos(@AuthenticationPrincipal UserPrincipal principal) {
        return listOrEmpty(principal.id(), e -> hrService.listEmployeeTodos(e.getId()));
    }

    @GetMapping("/approvals")
    public ApiResponse<List<ApprovalResponse>> approvals() {
        return ApiResponse.ok(officeService.listMyPendingApprovals());
    }

    @PostMapping("/approvals/{id}/process")
    public ApiResponse<ApprovalResponse> processApproval(@PathVariable UUID id, @Valid @RequestBody ProcessApprovalRequest request) {
        return ApiResponse.ok(officeService.processApproval(id, request));
    }
}
