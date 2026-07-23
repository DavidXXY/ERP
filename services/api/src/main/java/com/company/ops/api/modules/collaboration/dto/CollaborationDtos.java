package com.company.ops.api.modules.collaboration.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public final class CollaborationDtos {
  private CollaborationDtos(){}
  public record ResponsibilityRequest(@NotBlank String sourceType,@NotNull UUID sourceId,UUID ownerUserId,UUID departmentId,List<UUID> collaboratorDepartmentIds){}
  public record HandoverRequest(@NotNull UUID projectId,@NotNull UUID contractId,UUID salesOwnerId,UUID projectManagerId,UUID salesDepartmentId,UUID deliveryDepartmentId,
      @NotBlank String scopeSummary,@NotBlank String paymentTerms,@NotBlank String acceptanceCriteria,@NotBlank String customerContact,
      @NotBlank String technicalSolution,@NotBlank String quotationSummary,String riskNotes){}
  public record AcceptHandoverRequest(String comment){}
  public record StaffAssignmentRequest(@NotNull UUID projectId,@NotNull UUID userId,@NotBlank String roleName,@NotNull @Positive BigDecimal plannedHours,
      @NotNull @PositiveOrZero BigDecimal hourlyCost,@NotNull @DecimalMin("1") @DecimalMax("100") BigDecimal allocationPercent,
      @NotNull LocalDate startDate,@NotNull LocalDate endDate){}
  public record StaffHoursRequest(@NotNull @PositiveOrZero BigDecimal actualHours){}
  public record TodoActionRequest(@NotBlank String action,UUID targetUserId,List<UUID> ccUserIds,String comment,LocalDate dueDate){}
  public record TimesheetSubmitRequest(@NotNull UUID assignmentId,@NotNull LocalDate workDate,@NotNull @DecimalMin("0.25") @DecimalMax("24") BigDecimal hours,@NotBlank @Size(max=500) String description){}
  public record ReviewRequest(@NotBlank String decision,@Size(max=500) String comment){}
  public record PeriodLockRequest(@NotBlank @Pattern(regexp="\\d{4}-(0[1-9]|1[0-2])") String yearMonth,@NotBlank @Size(max=500) String reason){}
  public record BudgetChangeRequest(@NotNull UUID projectId,@NotNull @Positive BigDecimal requestedAmount,@NotBlank @Size(max=1000) String reason){}
  public record DashboardFilter(Integer year,UUID departmentId){}
}
