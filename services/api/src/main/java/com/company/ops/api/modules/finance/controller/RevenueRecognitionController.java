package com.company.ops.api.modules.finance.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.common.api.PageResponse;
import com.company.ops.api.modules.finance.dto.RevenueRecognitionDtos.*;
import com.company.ops.api.modules.finance.service.RevenueRecognitionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance")
public class RevenueRecognitionController {

  private final RevenueRecognitionService service;

  public RevenueRecognitionController(RevenueRecognitionService service) {
    this.service = service;
  }

  @GetMapping("/revenue-milestones")
  @PreAuthorize("hasAuthority('finance:revenue:recognize')")
  public ApiResponse<List<MilestoneResponse>> listMilestones(@RequestParam UUID contractId) {
    return ApiResponse.ok(service.listMilestones(contractId));
  }

  @PostMapping("/revenue-milestones")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('finance:revenue:recognize')")
  public ApiResponse<MilestoneResponse> createMilestone(@Valid @RequestBody CreateMilestoneRequest request) {
    return ApiResponse.ok(service.createMilestone(request));
  }

  @PostMapping("/revenue-milestones/{id}/recognize")
  @PreAuthorize("hasAuthority('finance:revenue:recognize')")
  public ApiResponse<MilestoneResponse> recognizeMilestone(
      @PathVariable UUID id, @Valid @RequestBody RecognizeMilestoneRequest request) {
    return ApiResponse.ok(service.recognizeMilestone(id, request));
  }

  @GetMapping("/revenue-recognitions")
  @PreAuthorize("hasAuthority('finance:revenue:recognize')")
  public ApiResponse<PageResponse<RecognitionResponse>> listRecognitions(@PageableDefault(size = 100) Pageable pageable) {
    return ApiResponse.ok(PageResponse.from(service.listRecognitions(pageable)));
  }
}