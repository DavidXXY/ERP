package com.company.ops.api.modules.project.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.common.api.PageResponse;
import com.company.ops.api.modules.project.dto.AdvanceProjectStageRequest;
import com.company.ops.api.modules.project.dto.AssignProjectManagerRequest;
import com.company.ops.api.modules.project.dto.ChangeProjectExecutionStatusRequest;
import com.company.ops.api.modules.project.dto.CreateProjectCostRequest;
import com.company.ops.api.modules.project.dto.CreateProjectRequest;
import com.company.ops.api.modules.project.dto.ProcessProjectApprovalRequest;
import com.company.ops.api.modules.project.dto.PrepareChildProjectRequest;
import com.company.ops.api.modules.project.dto.CloseoutReviewRequest;
import com.company.ops.api.modules.project.dto.ProcessCloseoutReviewRequest;
import com.company.ops.api.modules.project.dto.ProjectCloseoutReviewResponse;
import com.company.ops.api.modules.project.dto.RollbackProjectStageRequest;
import com.company.ops.api.modules.project.dto.UpdateProjectCostRequest;
import com.company.ops.api.modules.project.dto.UpdateProjectRequest;
import com.company.ops.api.modules.project.dto.ProjectDetailResponse;
import com.company.ops.api.modules.project.dto.ProjectProfitabilityResponse;
import com.company.ops.api.modules.project.dto.ProjectManagerOption;
import com.company.ops.api.modules.project.dto.ProjectMilestoneRequest;
import com.company.ops.api.modules.project.dto.ProjectMilestoneResponse;
import com.company.ops.api.modules.project.dto.ProjectRiskRequest;
import com.company.ops.api.modules.project.dto.ProjectRiskResponse;
import com.company.ops.api.modules.project.dto.ProjectStaffResponse;
import com.company.ops.api.modules.project.dto.ProjectTimelineEntryResponse;
import com.company.ops.api.modules.project.dto.ProjectResponse;
import com.company.ops.api.modules.project.domain.ProjectApprovalStatus;
import com.company.ops.api.modules.project.domain.ProjectExecutionStatus;
import com.company.ops.api.modules.project.domain.ProjectStage;
import com.company.ops.api.modules.project.service.ProjectService;
import com.company.ops.api.modules.system.service.ApprovalFlowSecurity;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ApproveQuoteCostRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.QuoteCostRequestResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.QuoteResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.SubmitQuoteCostRequest;
import com.company.ops.api.modules.crm.service.CrmOperationsService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

  private final ProjectService projectService;
  private final CrmOperationsService crmOperationsService;
  private final ApprovalFlowSecurity approvalFlowSecurity;

  public ProjectController(ProjectService projectService, CrmOperationsService crmOperationsService,
                           ApprovalFlowSecurity approvalFlowSecurity) {
    this.projectService = projectService;
    this.crmOperationsService = crmOperationsService;
    this.approvalFlowSecurity = approvalFlowSecurity;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('project:view')")
  public ApiResponse<PageResponse<ProjectResponse>> listProjects(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) ProjectApprovalStatus approvalStatus,
      @RequestParam(required = false) ProjectStage stage,
      @RequestParam(required = false) ProjectExecutionStatus executionStatus,
      @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable
  ) {
    return ApiResponse.ok(PageResponse.from(
        projectService.listProjects(keyword, approvalStatus, stage, executionStatus, pageable)));
  }

  @GetMapping("/portfolio")
  @PreAuthorize("hasAuthority('project:view')")
  public ApiResponse<PageResponse<ProjectDetailResponse>> portfolio(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) ProjectApprovalStatus approvalStatus,
      @RequestParam(required = false) ProjectStage stage,
      @RequestParam(required = false) ProjectExecutionStatus executionStatus,
      @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable
  ) {
    return ApiResponse.ok(PageResponse.from(
        projectService.listPortfolio(keyword, approvalStatus, stage, executionStatus, pageable)));
  }

  @GetMapping("/profitability")
  @PreAuthorize("hasAuthority('project:view')")
  public ApiResponse<List<ProjectProfitabilityResponse>> profitability() {
    return ApiResponse.ok(projectService.profitability());
  }

  @GetMapping("/manager-options")
  @PreAuthorize("hasAuthority('project:approve')")
  public ApiResponse<List<ProjectManagerOption>> managerOptions() {
    return ApiResponse.ok(projectService.managerOptions());
  }

  @GetMapping("/manager-assignment-capability")
  @PreAuthorize("hasAuthority('project:view')")
  public ApiResponse<Boolean> managerAssignmentCapability() {
    return ApiResponse.ok(approvalFlowSecurity.canApprove("PROJECT"));
  }

  @GetMapping("/presales-support")
  @PreAuthorize("hasAuthority('project:view')")
  public ApiResponse<List<QuoteResponse>> preSalesSupport(
      @RequestParam(defaultValue = "false") boolean archived) {
    return ApiResponse.ok(crmOperationsService.listPreSalesSupport(archived));
  }

  @PostMapping("/presales-support/{id}/archive")
  @PreAuthorize("hasAuthority('project:approve')")
  public ApiResponse<QuoteResponse> archivePreSales(@PathVariable UUID id) {
    return ApiResponse.ok(crmOperationsService.archiveQuote(id, true));
  }

  @PostMapping("/presales-support/{id}/unarchive")
  @PreAuthorize("hasAuthority('project:approve')")
  public ApiResponse<QuoteResponse> unarchivePreSales(@PathVariable UUID id) {
    return ApiResponse.ok(crmOperationsService.archiveQuote(id, false));
  }

  @PostMapping("/presales-support/{id}/cost")
  @PreAuthorize("hasAuthority('project:cost:create')")
  public ApiResponse<QuoteCostRequestResponse> submitPreSalesCost(
      @PathVariable UUID id,
      @Valid @RequestBody SubmitQuoteCostRequest request
  ) {
    return ApiResponse.ok(crmOperationsService.submitQuoteCost(id, request));
  }

  @PostMapping("/presales-support/{id}/approval")
  @PreAuthorize("hasAuthority('project:approve') and @approvalFlowSecurity.canApprove('PROJECT')")
  public ApiResponse<QuoteCostRequestResponse> approvePreSalesCost(
      @PathVariable UUID id,
      @Valid @RequestBody ApproveQuoteCostRequest request
  ) {
    return ApiResponse.ok(crmOperationsService.approveQuoteCost(id, request));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('project:view')")
  public ApiResponse<ProjectDetailResponse> getProject(@PathVariable UUID id) {
    return ApiResponse.ok(projectService.getProject(id));
  }

  @GetMapping("/{id}/timeline")
  @PreAuthorize("hasAuthority('project:view')")
  public ApiResponse<List<ProjectTimelineEntryResponse>> projectTimeline(@PathVariable UUID id) {
    return ApiResponse.ok(projectService.projectTimeline(id));
  }

  @GetMapping("/{id}/staff")
  @PreAuthorize("hasAuthority('project:view')")
  public ApiResponse<List<ProjectStaffResponse>> projectStaff(@PathVariable UUID id) {
    return ApiResponse.ok(projectService.projectStaff(id));
  }

  @GetMapping("/{id}/milestones")
  @PreAuthorize("hasAuthority('project:view')")
  public ApiResponse<List<ProjectMilestoneResponse>> listMilestones(@PathVariable UUID id) {
    return ApiResponse.ok(projectService.listMilestones(id));
  }

  @PostMapping("/{id}/milestones")
  @PreAuthorize("hasAuthority('project:stage:update')")
  public ApiResponse<ProjectMilestoneResponse> createMilestone(
      @PathVariable UUID id, @Valid @RequestBody ProjectMilestoneRequest request) {
    return ApiResponse.ok(projectService.createMilestone(id, request));
  }

  @PutMapping("/{id}/milestones/{milestoneId}")
  @PreAuthorize("hasAuthority('project:stage:update')")
  public ApiResponse<ProjectMilestoneResponse> updateMilestone(
      @PathVariable UUID id, @PathVariable UUID milestoneId, @Valid @RequestBody ProjectMilestoneRequest request) {
    return ApiResponse.ok(projectService.updateMilestone(id, milestoneId, request));
  }

  @DeleteMapping("/{id}/milestones/{milestoneId}")
  @PreAuthorize("hasAuthority('project:stage:update')")
  public ApiResponse<Void> deleteMilestone(@PathVariable UUID id, @PathVariable UUID milestoneId) {
    projectService.deleteMilestone(id, milestoneId);
    return ApiResponse.ok(null);
  }

  @GetMapping("/{id}/risks")
  @PreAuthorize("hasAuthority('project:view')")
  public ApiResponse<List<ProjectRiskResponse>> listRisks(@PathVariable UUID id) {
    return ApiResponse.ok(projectService.listRisks(id));
  }

  @PostMapping("/{id}/risks")
  @PreAuthorize("hasAuthority('project:stage:update')")
  public ApiResponse<ProjectRiskResponse> createRisk(
      @PathVariable UUID id, @Valid @RequestBody ProjectRiskRequest request) {
    return ApiResponse.ok(projectService.createRisk(id, request));
  }

  @PutMapping("/{id}/risks/{riskId}")
  @PreAuthorize("hasAuthority('project:stage:update')")
  public ApiResponse<ProjectRiskResponse> updateRisk(
      @PathVariable UUID id, @PathVariable UUID riskId, @Valid @RequestBody ProjectRiskRequest request) {
    return ApiResponse.ok(projectService.updateRisk(id, riskId, request));
  }

  @DeleteMapping("/{id}/risks/{riskId}")
  @PreAuthorize("hasAuthority('project:stage:update')")
  public ApiResponse<Void> deleteRisk(@PathVariable UUID id, @PathVariable UUID riskId) {
    projectService.deleteRisk(id, riskId);
    return ApiResponse.ok(null);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('project:create')")
  public ApiResponse<ProjectDetailResponse> createProject(@Valid @RequestBody CreateProjectRequest request) {
    return ApiResponse.ok(projectService.createProject(request));
  }

  @PostMapping("/{id}/approval")
  @PreAuthorize("hasAuthority('project:approve') and @approvalFlowSecurity.canApprove('PROJECT')")
  public ApiResponse<ProjectDetailResponse> processApproval(
      @PathVariable UUID id,
      @Valid @RequestBody ProcessProjectApprovalRequest request
  ) {
    return ApiResponse.ok(projectService.processApproval(id, request));
  }

  @PostMapping("/{id}/manager")
  @PreAuthorize("hasAuthority('project:approve') and @approvalFlowSecurity.canApprove('PROJECT')")
  public ApiResponse<ProjectDetailResponse> assignManager(
      @PathVariable UUID id,
      @Valid @RequestBody AssignProjectManagerRequest request
  ) {
    return ApiResponse.ok(projectService.assignManager(id, request));
  }

  @PutMapping("/{id}/preparation")
  @PreAuthorize("hasAnyAuthority('project:create', 'project:approve', 'project:stage:update')")
  public ApiResponse<ProjectDetailResponse> prepareChildProject(
      @PathVariable UUID id,
      @Valid @RequestBody PrepareChildProjectRequest request
  ) {
    return ApiResponse.ok(projectService.prepareChildProject(id, request));
  }

  @PostMapping("/{id}/stage")
  @PreAuthorize("hasAuthority('project:stage:update')")
  public ApiResponse<ProjectDetailResponse> advanceStage(
      @PathVariable UUID id,
      @Valid @RequestBody AdvanceProjectStageRequest request
  ) {
    return ApiResponse.ok(projectService.advanceStage(id, request));
  }

  @PostMapping("/{id}/costs")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('project:cost:create')")
  public ApiResponse<ProjectDetailResponse> createCost(
      @PathVariable UUID id,
      @Valid @RequestBody CreateProjectCostRequest request
  ) {
    return ApiResponse.ok(projectService.createCost(id, request));
  }

  @PutMapping("/{id}/costs/{costId}")
  @PreAuthorize("hasAuthority('project:cost:create')")
  public ApiResponse<ProjectDetailResponse> updateCost(
      @PathVariable UUID id,
      @PathVariable UUID costId,
      @Valid @RequestBody UpdateProjectCostRequest request
  ) {
    return ApiResponse.ok(projectService.updateCost(id, costId, request));
  }

  @DeleteMapping("/{id}/costs/{costId}")
  @PreAuthorize("hasAuthority('project:cost:create')")
  public ApiResponse<ProjectDetailResponse> deleteCost(
      @PathVariable UUID id,
      @PathVariable UUID costId
  ) {
    return ApiResponse.ok(projectService.deleteCost(id, costId));
  }

  @PostMapping("/{id}/stage/rollback")
  @PreAuthorize("hasAuthority('project:stage:update')")
  public ApiResponse<ProjectDetailResponse> rollbackStage(
      @PathVariable UUID id,
      @Valid @RequestBody RollbackProjectStageRequest request
  ) {
    return ApiResponse.ok(projectService.rollbackStage(id, request));
  }

  @PostMapping("/{id}/closeout/request")
  @PreAuthorize("hasAuthority('project:stage:update')")
  public ApiResponse<ProjectCloseoutReviewResponse> requestCloseout(
      @PathVariable UUID id,
      @Valid @RequestBody CloseoutReviewRequest request
  ) {
    return ApiResponse.ok(projectService.requestCloseout(id, request));
  }

  @PostMapping("/{id}/closeout/review")
  @PreAuthorize("hasAuthority('project:approve')")
  public ApiResponse<ProjectCloseoutReviewResponse> reviewCloseout(
      @PathVariable UUID id,
      @Valid @RequestBody ProcessCloseoutReviewRequest request
  ) {
    return ApiResponse.ok(projectService.reviewCloseout(id, request));
  }

  @GetMapping("/{id}/closeout-review")
  @PreAuthorize("hasAuthority('project:view')")
  public ApiResponse<ProjectCloseoutReviewResponse> getCloseoutReview(@PathVariable UUID id) {
    return ApiResponse.ok(projectService.getCloseoutReview(id));
  }

  @PostMapping("/{id}/execution-status")
  @PreAuthorize("hasAuthority('project:stage:update')")
  public ApiResponse<ProjectDetailResponse> changeExecutionStatus(
      @PathVariable UUID id,
      @Valid @RequestBody ChangeProjectExecutionStatusRequest request
  ) {
    return ApiResponse.ok(projectService.changeExecutionStatus(id, request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('project:create', 'project:approve', 'project:stage:update')")
  public ApiResponse<ProjectDetailResponse> updateProject(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateProjectRequest request
  ) {
    return ApiResponse.ok(projectService.updateProject(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('project:delete')")
  public ApiResponse<Void> deleteProject(@PathVariable UUID id) {
    projectService.deleteProject(id);
    return ApiResponse.ok(null);
  }
}
