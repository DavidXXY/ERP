package com.company.ops.api.modules.mobile.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.mobile.dto.MobileWorkbenchResponse;
import com.company.ops.api.modules.mobile.service.MobileWorkbenchService;
import com.company.ops.api.modules.office.dto.OfficeDtos.ApprovalResponse;
import com.company.ops.api.modules.office.service.OfficeService;
import com.company.ops.api.modules.system.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile")
public class MobileWorkbenchController {
  private final MobileWorkbenchService service;
  private final OfficeService officeService;

  public MobileWorkbenchController(MobileWorkbenchService service, OfficeService officeService) {
    this.service = service;
    this.officeService = officeService;
  }

  @GetMapping("/workbench")
  public ApiResponse<MobileWorkbenchResponse> workbench(@AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.ok(service.workbench(principal));
  }

  @GetMapping("/approvals")
  public ApiResponse<Page<ApprovalResponse>> approvals(@PageableDefault(size = 100) Pageable pageable) {
    return ApiResponse.ok(officeService.listMobileApprovals(pageable));
  }
}
