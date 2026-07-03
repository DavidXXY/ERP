package com.company.ops.api.modules.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class HrDtos {
    private HrDtos() {}

    // Employee education
    public record EducationRequest(
        @NotBlank @Size(max = 200) String schoolName,
        @Size(max = 40) String degree,
        @Size(max = 200) String major,
        LocalDate startDate, LocalDate endDate,
        boolean highest, @Size(max = 1000) String remark
    ) {}
    public record EducationResponse(
        UUID id, UUID employeeId, String schoolName, String degree, String major,
        LocalDate startDate, LocalDate endDate, boolean highest, String remark
    ) {}

    // Work experience
    public record WorkExperienceRequest(
        @NotBlank @Size(max = 200) String companyName,
        @Size(max = 120) String position,
        LocalDate startDate, LocalDate endDate,
        boolean current, @Size(max = 1000) String description,
        @Size(max = 1000) String remark
    ) {}
    public record WorkExperienceResponse(
        UUID id, UUID employeeId, String companyName, String position,
        LocalDate startDate, LocalDate endDate, boolean current,
        String description, String remark
    ) {}

    // Emergency contact
    public record EmergencyContactRequest(
        @NotBlank @Size(max = 80) String name,
        @Size(max = 40) String relationship,
        @Size(max = 40) String phone,
        @Size(max = 200) String address,
        boolean primary, @Size(max = 1000) String remark
    ) {}
    public record EmergencyContactResponse(
        UUID id, UUID employeeId, String name, String relationship,
        String phone, String address, boolean primary, String remark
    ) {}

    // Employee lifecycle
    public record LifecycleRequest(
        @NotBlank @Size(max = 40) String lifecycleType,
        @NotNull LocalDate effectiveDate,
        String fromOrganizationId, String fromOrganizationName, String fromPosition,
        String toOrganizationId, String toOrganizationName, String toPosition,
        @Size(max = 1000) String reason, @Size(max = 1000) String remark
    ) {}
    public record LifecycleResponse(
        UUID id, UUID employeeId, String employeeName, String lifecycleType,
        LocalDate effectiveDate,
        String fromOrganizationId, String fromOrganizationName, String fromPosition,
        String toOrganizationId, String toOrganizationName, String toPosition,
        String reason, String status, String approvedBy,
        LocalDate approvedAt, String remark
    ) {}

    // Leave request
    public record LeaveRequestData(
        @NotBlank @Size(max = 40) String leaveType,
        @NotNull LocalDate startDate, @NotNull LocalDate endDate,
        double totalDays, @Size(max = 1000) String reason
    ) {}
    public record LeaveResponse(
        UUID id, UUID employeeId, String employeeName, String leaveType,
        LocalDate startDate, LocalDate endDate, double totalDays,
        String reason, String status, String approvedBy,
        LocalDateTime approvedAt, String approvalRemark
    ) {}

    // Approval
    public record ApproveRequest(
        boolean approved, @Size(max = 1000) String remark,
        @NotBlank @Size(max = 80) String operatorName
    ) {}

    // HR Analytics
    public record HrAnalyticsResponse(
        long totalEmployees, long activeEmployees, long leftEmployees,
        long newThisMonth, long leavePendingCount,
        List<CategoryCount> educationDistribution,
        List<CategoryCount> statusDistribution,
        List<CategoryCount> organizationDistribution,
        List<LifecycleSummary> recentLifecycles
    ) {}
    public record CategoryCount(String name, long count) {}
    public record LifecycleSummary(String date, String employeeName, String type, String detail) {}


    // Leave balance
    public record LeaveBalanceRequest(
        @NotBlank @Size(max = 40) String leaveType,
        int year, double totalDays, double usedDays
    ) {}
    public record LeaveBalanceResponse(
        UUID id, UUID employeeId, String employeeName, String leaveType,
        int year, double totalDays, double usedDays, double remainingDays
    ) {}
    public record LeaveBalanceBatchInitRequest(
        List<BalanceInitItem> items
    ) {}
    public record BalanceInitItem(
        UUID employeeId, String leaveType, int year, double totalDays
    ) {}
}
