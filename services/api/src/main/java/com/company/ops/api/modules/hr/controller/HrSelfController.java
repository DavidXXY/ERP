package com.company.ops.api.modules.hr.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.hr.dto.HrDtos.*;
import com.company.ops.api.modules.hr.service.HrService;
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

    public HrSelfController(HrService hrService, QualificationEmployeeRepository employeeRepository) {
        this.hrService = hrService;
        this.employeeRepository = employeeRepository;
    }

    private QualificationEmployee getCurrentEmployee(UUID userId) {
        return employeeRepository.findBySystemUser_Id(userId)
            .orElseThrow(() -> new BusinessException("未找到员工档案，请联系管理员关联账号"));
    }

    // Self profile: employee + education + work + contacts + contracts + certificates
    @GetMapping("/profile")
    public ApiResponse<EmployeeDetailResponse> profile(@AuthenticationPrincipal UserPrincipal principal) {
        var emp = getCurrentEmployee(principal.getId());
        var detail = hrService.getEmployeeDetail(emp.getId());
        return ApiResponse.ok(detail);
    }

    @GetMapping("/leave-balances")
    public ApiResponse<List<LeaveBalanceResponse>> leaveBalances(@AuthenticationPrincipal UserPrincipal principal) {
        var emp = getCurrentEmployee(principal.getId());
        return ApiResponse.ok(hrService.listBalances(emp.getId()));
    }

    @GetMapping("/leaves")
    public ApiResponse<List<LeaveResponse>> leaves(@AuthenticationPrincipal UserPrincipal principal) {
        var emp = getCurrentEmployee(principal.getId());
        return ApiResponse.ok(hrService.listLeaves(emp.getId()));
    }

    @PostMapping("/leaves")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LeaveResponse> createLeave(@AuthenticationPrincipal UserPrincipal principal,
                                                   @Valid @RequestBody LeaveRequestData request) {
        var emp = getCurrentEmployee(principal.getId());
        return ApiResponse.ok(hrService.createLeave(emp.getId(), request));
    }

    @GetMapping("/lifecycles")
    public ApiResponse<List<LifecycleResponse>> lifecycles(@AuthenticationPrincipal UserPrincipal principal) {
        var emp = getCurrentEmployee(principal.getId());
        return ApiResponse.ok(hrService.listLifecycles(emp.getId()));
    }


    @GetMapping("/educations")
    public ApiResponse<List<EducationResponse>> educations(@AuthenticationPrincipal UserPrincipal principal) {
        var emp = getCurrentEmployee(principal.getId());
        return ApiResponse.ok(hrService.listEducations(emp.getId()));
    }

    @GetMapping("/work-experiences")
    public ApiResponse<List<WorkExperienceResponse>> workExperiences(@AuthenticationPrincipal UserPrincipal principal) {
        var emp = getCurrentEmployee(principal.getId());
        return ApiResponse.ok(hrService.listWorkExperiences(emp.getId()));
    }

    @GetMapping("/emergency-contacts")
    public ApiResponse<List<EmergencyContactResponse>> emergencyContacts(@AuthenticationPrincipal UserPrincipal principal) {
        var emp = getCurrentEmployee(principal.getId());
        return ApiResponse.ok(hrService.listEmergencyContacts(emp.getId()));
    }

    @GetMapping("/certificates")
    public ApiResponse<List<PersonnelCertificateResponse>> certificates(@AuthenticationPrincipal UserPrincipal principal) {
        var emp = getCurrentEmployee(principal.getId());
        return ApiResponse.ok(hrService.listEmployeeCertificates(emp.getId()));
    }

    @GetMapping("/contracts")
    public ApiResponse<List<EmployeeContractResponse>> contracts(@AuthenticationPrincipal UserPrincipal principal) {
        var emp = getCurrentEmployee(principal.getId());
        return ApiResponse.ok(hrService.listEmployeeContracts(emp.getId()));
    }

}
