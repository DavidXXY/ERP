package com.company.ops.api.modules.system.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.common.api.PageResponse;
import com.company.ops.api.modules.system.dto.LegalEntityDtos.*;
import com.company.ops.api.modules.system.service.LegalEntityService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/legal-entities")
public class LegalEntityController {

  private final LegalEntityService service;

  public LegalEntityController(LegalEntityService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('ledger:entity:manage')")
  public ApiResponse<PageResponse<LegalEntityResponse>> list(@PageableDefault(size = 100) Pageable pageable) {
    return ApiResponse.ok(PageResponse.from(service.list(pageable)));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('ledger:entity:manage')")
  public ApiResponse<LegalEntityResponse> create(@Valid @RequestBody CreateLegalEntityRequest request) {
    return ApiResponse.ok(service.create(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('ledger:entity:manage')")
  public ApiResponse<LegalEntityResponse> update(@PathVariable UUID id, @Valid @RequestBody CreateLegalEntityRequest request) {
    return ApiResponse.ok(service.update(id, request));
  }
}