package com.company.ops.api.modules.risk.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.risk.dto.RiskWorkflowDtos.RiskItemResponse;
import com.company.ops.api.modules.risk.dto.RiskWorkflowDtos.RiskRuleConfigResponse;
import com.company.ops.api.modules.risk.dto.RiskWorkflowDtos.RiskSummaryResponse;
import com.company.ops.api.modules.risk.dto.RiskWorkflowDtos.UpdateRiskRuleConfigRequest;
import com.company.ops.api.modules.risk.service.RiskCenterService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/risk")
public class RiskCenterController {
  private final RiskCenterService service;

  public RiskCenterController(RiskCenterService service) {
    this.service = service;
  }

  @GetMapping("/items")
  @PreAuthorize("hasAnyAuthority('risk:view','dashboard:view','office:approval:view','office:notification:view','inventory:view','procurement:view','project:view','finance:receivable:view','finance:payable:view','qualification:warning:view','crm:renewal:view')")
  public ApiResponse<List<RiskItemResponse>> listItems() {
    return ApiResponse.ok(service.listItems());
  }

  @GetMapping("/summary")
  @PreAuthorize("hasAnyAuthority('risk:view','dashboard:view','office:approval:view','office:notification:view','inventory:view','procurement:view','project:view','finance:receivable:view','finance:payable:view','qualification:warning:view','crm:renewal:view')")
  public ApiResponse<RiskSummaryResponse> summary(@RequestParam(defaultValue = "14") int days) {
    return ApiResponse.ok(service.summary(days));
  }

  @PostMapping("/snapshots/today")
  @PreAuthorize("hasAnyAuthority('risk:update','risk:view')")
  public ApiResponse<Integer> snapshotToday() {
    return ApiResponse.ok(service.snapshotToday());
  }

  @PostMapping("/escalations/scan")
  @PreAuthorize("hasAnyAuthority('risk:update','risk:view')")
  public ApiResponse<Integer> escalateOverdue() {
    return ApiResponse.ok(service.escalateOverdue());
  }

  @GetMapping("/rules")
  @PreAuthorize("hasAnyAuthority('risk:view','risk:update')")
  public ApiResponse<List<RiskRuleConfigResponse>> listRules() {
    return ApiResponse.ok(service.listRules());
  }

  @PostMapping("/rules")
  @PreAuthorize("hasAuthority('risk:update')")
  public ApiResponse<RiskRuleConfigResponse> saveRule(@Valid @RequestBody UpdateRiskRuleConfigRequest request) {
    return ApiResponse.ok(service.saveRule(request));
  }

  @PutMapping("/rules/{id}")
  @PreAuthorize("hasAuthority('risk:update')")
  public ApiResponse<RiskRuleConfigResponse> updateRule(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateRiskRuleConfigRequest request) {
    return ApiResponse.ok(service.updateRule(id, request));
  }

  @DeleteMapping("/rules/{id}")
  @PreAuthorize("hasAuthority('risk:update')")
  public ApiResponse<Void> deleteRule(@PathVariable UUID id) {
    service.deleteRule(id);
    return ApiResponse.ok();
  }
}
