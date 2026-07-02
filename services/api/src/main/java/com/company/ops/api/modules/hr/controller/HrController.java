package com.company.ops.api.modules.hr.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.hr.dto.HrDtos.*;
import com.company.ops.api.modules.hr.service.HrService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hr")
public class HrController {

    private final HrService hrService;

    public HrController(HrService hrService) {
        this.hrService = hrService;
    }

    // ====== Education ======
    @GetMapping("/employees/{employeeId}/educations")
    @PreAuthorize("hasAuthority('qualification:employee:view')")
    public ApiResponse<List<EducationResponse>> listEducations(@PathVariable UUID employeeId) {
        return ApiResponse.ok(hrService.listEducations(employeeId));
    }

    @PostMapping("/employees/{employeeId}/educations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('qualification:employee:manage')")
    public ApiResponse<EducationResponse> createEducation(@PathVariable UUID employeeId,
                                                          @Valid @RequestBody EducationRequest request) {
        return ApiResponse.ok(hrService.createEducation(employeeId, request));
    }

    @PutMapping("/educations/{id}")
    @PreAuthorize("hasAuthority('qualification:employee:manage')")
    public ApiResponse<EducationResponse> updateEducation(@PathVariable UUID id,
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
    public ApiResponse<WorkExperienceResponse> createWorkExperience(@PathVariable UUID employeeId,
                                                                     @Valid @RequestBody WorkExperienceRequest request) {
        return ApiResponse.ok(hrService.createWorkExperience(employeeId, request));
    }

    @PutMapping("/work-experiences/{id}")
    @PreAuthorize("hasAuthority('qualification:employee:manage')")
    public ApiResponse<WorkExperienceResponse> updateWorkExperience(@PathVariable UUID id,
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
    public ApiResponse<EmergencyContactResponse> createEmergencyContact(@PathVariable UUID employeeId,
                                                                         @Valid @RequestBody EmergencyContactRequest request) {
        return ApiResponse.ok(hrService.createEmergencyContact(employeeId, request));
    }

    @PutMapping("/emergency-contacts/{id}")
    @PreAuthorize("hasAuthority('qualification:employee:manage')")
    public ApiResponse<EmergencyContactResponse> updateEmergencyContact(@PathVariable UUID id,
                                                                         @Valid @RequestBody EmergencyContactRequest request) {
        return ApiResponse.ok(hrService.updateEmergencyContact(id, request));
    }

    @DeleteMapping("/emergency-contacts/{id}")
    @PreAuthorize("hasAuthority('qualification:employee:manage')")
    public ApiResponse<Void> deleteEmergencyContact(@PathVariable UUID id) {
        hrService.deleteEmergencyContact(id);
        return ApiResponse.ok();
    }

    // ====== Employee Lifecycle ======
    @GetMapping("/employees/{employeeId}/lifecycles")
    @PreAuthorize("hasAuthority('qualification:employee:view')")
    public ApiResponse<List<LifecycleResponse>> listLifecycles(@PathVariable UUID employeeId) {
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
    public ApiResponse<LifecycleResponse> createLifecycle(@PathVariable UUID employeeId,
                                                           @Valid @RequestBody LifecycleRequest request) {
        return ApiResponse.ok(hrService.createLifecycle(employeeId, request));
    }

    @PostMapping("/lifecycles/{id}/approve")
    @PreAuthorize("hasAuthority('qualification:employee:manage')")
    public ApiResponse<LifecycleResponse> approveLifecycle(@PathVariable UUID id,
                                                            @Valid @RequestBody ApproveRequest request) {
        return ApiResponse.ok(hrService.approveLifecycle(id, request));
    }

    // ====== Leave Request ======
    @GetMapping("/employees/{employeeId}/leaves")
    @PreAuthorize("hasAuthority('workforce:view')")
    public ApiResponse<List<LeaveResponse>> listLeaves(@PathVariable UUID employeeId) {
        return ApiResponse.ok(hrService.listLeaves(employeeId));
    }

    @GetMapping("/leaves")
    @PreAuthorize("hasAuthority('workforce:view')")
    public ApiResponse<List<LeaveResponse>> listAllLeaves() {
        return ApiResponse.ok(hrService.listAllLeaves());
    }

    @PostMapping("/employees/{employeeId}/leaves")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('workforce:view')")
    public ApiResponse<LeaveResponse> createLeave(@PathVariable UUID employeeId,
                                                   @Valid @RequestBody LeaveRequestData request) {
        return ApiResponse.ok(hrService.createLeave(employeeId, request));
    }

    @PostMapping("/leaves/{id}/approve")
    @PreAuthorize("hasAuthority('qualification:employee:manage')")
    public ApiResponse<LeaveResponse> approveLeave(@PathVariable UUID id,
                                                    @Valid @RequestBody ApproveRequest request) {
        return ApiResponse.ok(hrService.approveLeave(id, request));
    }

    // ====== Analytics ======
    @GetMapping("/analytics")
    @PreAuthorize("hasAuthority('qualification:employee:view')")
    public ApiResponse<HrAnalyticsResponse> analytics() {
        return ApiResponse.ok(hrService.analytics());
    }
}
