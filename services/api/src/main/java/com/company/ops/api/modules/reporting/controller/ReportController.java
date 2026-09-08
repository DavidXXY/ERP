package com.company.ops.api.modules.reporting.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.reporting.dto.ReportingDtos.ReportCreateRequest;
import com.company.ops.api.modules.reporting.dto.ReportingDtos.ReportProjectOption;
import com.company.ops.api.modules.reporting.dto.ReportingDtos.ReportResponse;
import com.company.ops.api.modules.reporting.dto.ReportingDtos.ReportUserOption;
import com.company.ops.api.modules.reporting.service.ReportService;
import com.company.ops.api.modules.system.security.UserPrincipal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

  private final ReportService reportService;

  public ReportController(ReportService reportService) {
    this.reportService = reportService;
  }

  // 员工自助：提交/我的汇报
  @GetMapping("/my")
  public ApiResponse<List<ReportResponse>> myReports(@AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.ok(reportService.listMyReports(principal.id()));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<ReportResponse> create(@AuthenticationPrincipal UserPrincipal principal,
                                            @RequestBody ReportCreateRequest request) {
    return ApiResponse.ok(reportService.createReport(principal.id(), request));
  }

  // 当前员工加入的项目（工程汇报用）
  @GetMapping("/my-projects")
  public ApiResponse<List<ReportProjectOption>> myProjects(@AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.ok(reportService.myProjects(principal.id()));
  }

  // 抄送候选人（可汇报对象）
  @GetMapping("/cc-candidates")
  public ApiResponse<List<ReportUserOption>> ccCandidates(@AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.ok(reportService.candidateCcUsers(principal.id()));
  }

  // 上级收件箱：直属/下级组织员工汇报 + 抄送给我的汇报
  @GetMapping("/received")
  public ApiResponse<List<ReportResponse>> received(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestParam(defaultValue = "") String type,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
    return ApiResponse.ok(reportService.listReceivedReports(principal.id(), type, fromDate, toDate));
  }
}
