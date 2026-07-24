package com.company.ops.api.modules.project.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.project.dto.AdvanceProjectStageRequest;
import com.company.ops.api.modules.project.dto.AssignProjectManagerRequest;
import com.company.ops.api.modules.project.dto.CreateProjectCostRequest;
import com.company.ops.api.modules.project.dto.CreateProjectRequest;
import com.company.ops.api.modules.project.dto.ProcessProjectApprovalRequest;
import com.company.ops.api.modules.project.dto.ProjectDetailResponse;
import com.company.ops.api.modules.project.dto.ProjectProfitabilityResponse;
import com.company.ops.api.modules.project.dto.ProjectManagerOption;
import com.company.ops.api.modules.project.dto.ProjectResponse;
import com.company.ops.api.modules.project.service.ProjectService;
import com.company.ops.api.modules.system.service.ApprovalFlowSecurity;
import com.company.ops.api.modules.crm.domain.QuoteStatus;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ApproveQuoteCostRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.QuoteCostRequestResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.QuoteResponse;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.SubmitQuoteCostRequest;
import com.company.ops.api.modules.crm.service.CrmOperationsService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
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
  public ApiResponse<Page<ProjectResponse>> listProjects(@PageableDefault(size = 20) Pageable pageable) {
    return ApiResponse.ok(projectService.listProjects(pageable));
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
  public ApiResponse<List<QuoteResponse>> preSalesSupport() {
    return ApiResponse.ok(crmOperationsService.listQuotes().stream()
        .filter(quote -> quote.status() == QuoteStatus.COST_REQUESTED
            || quote.status() == QuoteStatus.COSTING
            || quote.status() == QuoteStatus.COST_APPROVED)
        .toList());
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

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('project:delete')")
  public ApiResponse<Void> deleteProject(@PathVariable UUID id) {
    projectService.deleteProject(id);
    return ApiResponse.ok(null);
  }
}
