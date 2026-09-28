package com.company.ops.api.modules.finance.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.common.api.PageResponse;
import com.company.ops.api.modules.finance.dto.AdvanceDtos.*;
import com.company.ops.api.modules.finance.service.AdvanceService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance")
public class AdvanceController {

  private final AdvanceService advanceService;

  public AdvanceController(AdvanceService advanceService) {
    this.advanceService = advanceService;
  }

  @GetMapping("/advance-receipts")
  @PreAuthorize("hasAuthority('finance:advance:manage')")
  public ApiResponse<PageResponse<AdvanceReceiptResponse>> listReceipts(@PageableDefault(size = 100) Pageable pageable) {
    return ApiResponse.ok(PageResponse.from(advanceService.listReceipts(pageable)));
  }

  @PostMapping("/advance-receipts")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('finance:advance:manage')")
  public ApiResponse<AdvanceReceiptResponse> createReceipt(@Valid @RequestBody CreateAdvanceReceiptRequest request) {
    return ApiResponse.ok(advanceService.createReceipt(request));
  }

  @PostMapping("/advance-receipts/{receiptId}/apply/{receivableId}")
  @PreAuthorize("hasAuthority('finance:advance:manage')")
  public ApiResponse<AdvanceReceiptResponse> applyReceipt(
      @PathVariable UUID receiptId,
      @PathVariable UUID receivableId,
      @Valid @RequestBody ApplyAdvanceRequest request
  ) {
    return ApiResponse.ok(advanceService.applyReceipt(receiptId, receivableId, request));
  }

  @GetMapping("/advance-payments")
  @PreAuthorize("hasAuthority('finance:advance:manage')")
  public ApiResponse<PageResponse<AdvancePaymentResponse>> listPayments(@PageableDefault(size = 100) Pageable pageable) {
    return ApiResponse.ok(PageResponse.from(advanceService.listPayments(pageable)));
  }

  @PostMapping("/advance-payments")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('finance:advance:manage')")
  public ApiResponse<AdvancePaymentResponse> createPayment(@Valid @RequestBody CreateAdvancePaymentRequest request) {
    return ApiResponse.ok(advanceService.createPayment(request));
  }

  @PostMapping("/advance-payments/{paymentId}/apply/{payableId}")
  @PreAuthorize("hasAuthority('finance:advance:manage')")
  public ApiResponse<AdvancePaymentResponse> applyPayment(
      @PathVariable UUID paymentId,
      @PathVariable UUID payableId,
      @Valid @RequestBody ApplyAdvanceRequest request
  ) {
    return ApiResponse.ok(advanceService.applyPayment(paymentId, payableId, request));
  }
}