package com.company.ops.api.modules.project.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.project.dto.AdvanceProjectStageRequest;
import com.company.ops.api.modules.project.dto.CreateProjectCostRequest;
import com.company.ops.api.modules.project.dto.CreateProjectRequest;
import com.company.ops.api.modules.project.dto.ProcessProjectApprovalRequest;
import com.company.ops.api.modules.project.dto.ProjectDetailResponse;
import com.company.ops.api.modules.project.dto.ProjectProfitabilityResponse;
import com.company.ops.api.modules.project.dto.ProjectResponse;
import com.company.ops.api.modules.project.service.ProjectService;
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

  public ProjectController(ProjectService projectService) {
    this.projectService = projectService;
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
