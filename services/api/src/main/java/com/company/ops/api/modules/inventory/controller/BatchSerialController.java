package com.company.ops.api.modules.inventory.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.inventory.dto.BatchSerialDtos.*;
import com.company.ops.api.modules.inventory.service.BatchSerialService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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
@RequestMapping("/api/inventory")
public class BatchSerialController {

  private final BatchSerialService batchSerialService;

  public BatchSerialController(BatchSerialService batchSerialService) {
    this.batchSerialService = batchSerialService;
  }

  @GetMapping("/batches")
  @PreAuthorize("hasAuthority('inventory:view')")
  public ApiResponse<List<BatchResponse>> listBatches(@RequestParam UUID partId) {
    return ApiResponse.ok(batchSerialService.listBatches(partId));
  }

  @PostMapping("/batches")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('inventory:batch:manage')")
  public ApiResponse<BatchResponse> createBatch(@Valid @RequestBody CreateBatchRequest request) {
    return ApiResponse.ok(batchSerialService.createBatch(request));
  }

  @PostMapping("/batches/{id}/consume")
  @PreAuthorize("hasAuthority('inventory:batch:manage')")
  public ApiResponse<BatchResponse> consumeBatch(@PathVariable UUID id, @Valid @RequestBody ConsumeBatchRequest request) {
    return ApiResponse.ok(batchSerialService.consumeBatch(id, request));
  }

  @GetMapping("/serials")
  @PreAuthorize("hasAuthority('inventory:view')")
  public ApiResponse<List<SerialResponse>> listSerials(@RequestParam UUID partId) {
    return ApiResponse.ok(batchSerialService.listSerials(partId));
  }

  @PostMapping("/serials")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('inventory:batch:manage')")
  public ApiResponse<List<SerialResponse>> registerSerials(@Valid @RequestBody RegisterSerialRequest request) {
    return ApiResponse.ok(batchSerialService.registerSerials(request));
  }

  @PostMapping("/serials/{id}/issue")
  @PreAuthorize("hasAuthority('inventory:batch:manage')")
  public ApiResponse<SerialResponse> issueSerial(@PathVariable UUID id, @Valid @RequestBody SerialActionRequest request) {
    return ApiResponse.ok(batchSerialService.issueSerial(id, request));
  }

  @PostMapping("/serials/{id}/return")
  @PreAuthorize("hasAuthority('inventory:batch:manage')")
  public ApiResponse<SerialResponse> returnSerial(@PathVariable UUID id) {
    return ApiResponse.ok(batchSerialService.returnSerial(id));
  }
}