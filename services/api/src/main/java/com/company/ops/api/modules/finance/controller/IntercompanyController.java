package com.company.ops.api.modules.finance.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.common.api.PageResponse;
import com.company.ops.api.modules.finance.dto.IntercompanyDtos.*;
import com.company.ops.api.modules.finance.service.IntercompanyService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance")
public class IntercompanyController {

  private final IntercompanyService service;

  public IntercompanyController(IntercompanyService service) {
    this.service = service;
  }

  @GetMapping("/intercompany-transactions")
  @PreAuthorize("hasAuthority('ledger:entity:manage')")
  public ApiResponse<PageResponse<IntercompanyResponse>> list(@PageableDefault(size = 100) Pageable pageable) {
    return ApiResponse.ok(PageResponse.from(service.list(pageable)));
  }

  @PostMapping("/intercompany-transactions")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('ledger:entity:manage')")
  public ApiResponse<IntercompanyResponse> create(@Valid @RequestBody CreateIntercompanyRequest request) {
    return ApiResponse.ok(service.create(request));
  }
}