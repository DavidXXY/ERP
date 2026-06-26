package com.company.ops.api.modules.project.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.project.dto.CreateProjectRequest;
import com.company.ops.api.modules.project.dto.ProjectResponse;
import com.company.ops.api.modules.project.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
  public ApiResponse<List<ProjectResponse>> listProjects() {
    return ApiResponse.ok(projectService.listProjects());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('project:create')")
  public ApiResponse<ProjectResponse> createProject(@Valid @RequestBody CreateProjectRequest request) {
    return ApiResponse.ok(projectService.createProject(request));
  }
}
