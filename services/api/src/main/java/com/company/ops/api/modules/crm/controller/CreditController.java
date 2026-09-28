package com.company.ops.api.modules.crm.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.crm.dto.CreditDtos.*;
import com.company.ops.api.modules.crm.service.CreditService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crm")
public class CreditController {

  private final CreditService creditService;

  public CreditController(CreditService creditService) {
    this.creditService = creditService;
  }

  @GetMapping("/customers/{id}/credit")
  @PreAuthorize("hasAuthority('crm:credit:manage')")
  public ApiResponse<CreditInfoResponse> getCredit(@PathVariable UUID id) {
    return ApiResponse.ok(creditService.getCreditInfo(id));
  }

  @PutMapping("/customers/{id}/credit")
  @PreAuthorize("hasAuthority('crm:credit:manage')")
  public ApiResponse<CreditInfoResponse> setCredit(@PathVariable UUID id, @Valid @RequestBody SetCreditLimitRequest request) {
    return ApiResponse.ok(creditService.setCreditLimit(id, request));
  }
}