package com.company.ops.api.modules.risk.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.risk.dto.RiskWorkflowDtos.RiskWorkflowActionResponse;
import com.company.ops.api.modules.risk.dto.RiskWorkflowDtos.BatchUpdateRiskWorkflowRequest;
import com.company.ops.api.modules.risk.dto.RiskWorkflowDtos.RiskWorkflowResponse;
import com.company.ops.api.modules.risk.dto.RiskWorkflowDtos.UpdateRiskWorkflowRequest;
import com.company.ops.api.modules.risk.service.RiskWorkflowService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/risk/workflows")
public class RiskWorkflowController {
  private final RiskWorkflowService service;

  public RiskWorkflowController(RiskWorkflowService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAnyAuthority('risk:view','dashboard:view','office:approval:view','inventory:view','procurement:view','project:view','finance:receivable:view','finance:payable:view','qualification:warning:view','crm:renewal:view')")
  public ApiResponse<List<RiskWorkflowResponse>> list() {
    return ApiResponse.ok(service.list());
  }

  @GetMapping("/actions")
  @PreAuthorize("hasAnyAuthority('risk:view','dashboard:view','office:approval:view','inventory:view','procurement:view','project:view','finance:receivable:view','finance:payable:view','qualification:warning:view','crm:renewal:view')")
  public ApiResponse<List<RiskWorkflowActionResponse>> actions(@RequestParam String riskKey) {
    return ApiResponse.ok(service.actions(riskKey));
  }

  @PostMapping
  @PreAuthorize("hasAnyAuthority('risk:update','dashboard:view','office:approval:view','inventory:view','procurement:view','project:view','finance:receivable:view','finance:payable:view','qualification:warning:view','crm:renewal:view')")
  public ApiResponse<RiskWorkflowResponse> update(@Valid @RequestBody UpdateRiskWorkflowRequest request) {
    return ApiResponse.ok(service.update(request));
  }

  @PostMapping("/batch")
  @PreAuthorize("hasAnyAuthority('risk:update','dashboard:view','office:approval:view','inventory:view','procurement:view','project:view','finance:receivable:view','finance:payable:view','qualification:warning:view','crm:renewal:view')")
  public ApiResponse<List<RiskWorkflowResponse>> batchUpdate(@Valid @RequestBody BatchUpdateRiskWorkflowRequest request) {
    return ApiResponse.ok(service.batchUpdate(request));
  }
}
