package com.company.ops.api.modules.ledger.controller;

import static com.company.ops.api.modules.ledger.dto.LedgerDtos.*;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.common.api.PageResponse;
import com.company.ops.api.modules.ledger.service.LedgerService;
import jakarta.validation.Valid;
import java.util.UUID;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance/ledger")
@PreAuthorize("hasAuthority('finance:ledger:view')")
public class LedgerController {
  private final LedgerService service;
  public LedgerController(LedgerService service) { this.service = service; }

  @GetMapping("/overview") public ApiResponse<LedgerOverview> overview() { return ApiResponse.ok(service.overview()); }
  @GetMapping("/vouchers") public ApiResponse<PageResponse<VoucherResponse>> vouchers(@PageableDefault(size=100) Pageable pageable) { return ApiResponse.ok(PageResponse.from(service.vouchers(pageable))); }
  @GetMapping("/statements") public ApiResponse<FinancialStatements> statements(
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    return ApiResponse.ok(service.statements(from, to));
  }

  @GetMapping("/accounts")
  public ApiResponse<List<AccountResponse>> accounts() { return ApiResponse.ok(service.accounts()); }

  @PostMapping("/accounts")
  @PreAuthorize("hasAuthority('finance:account:manage')")
  public ApiResponse<AccountResponse> createAccount(@Valid @RequestBody SaveAccountRequest request) {
    return ApiResponse.ok(service.saveAccount(null, request));
  }

  @PutMapping("/accounts/{id}")
  @PreAuthorize("hasAuthority('finance:account:manage')")
  public ApiResponse<AccountResponse> updateAccount(@PathVariable UUID id, @Valid @RequestBody SaveAccountRequest request) {
    return ApiResponse.ok(service.saveAccount(id, request));
  }

  @GetMapping("/opening-balances")
  public ApiResponse<List<OpeningBalanceResponse>> openingBalances(@RequestParam int fiscalYear) {
    return ApiResponse.ok(service.openingBalances(fiscalYear));
  }

  @PostMapping("/opening-balances")
  @PreAuthorize("hasAuthority('finance:account:manage')")
  public ApiResponse<OpeningBalanceResponse> saveOpeningBalance(@Valid @RequestBody SaveOpeningBalanceRequest request) {
    return ApiResponse.ok(service.saveOpeningBalance(request));
  }

  @PostMapping("/vouchers")
  @PreAuthorize("hasAuthority('finance:voucher:create')")
  public ApiResponse<VoucherResponse> createDraft(@Valid @RequestBody CreateVoucherRequest request) { return ApiResponse.ok(service.createDraft(request)); }

  @PostMapping("/vouchers/{id}/review")
  @PreAuthorize("hasAuthority('finance:voucher:review')")
  public ApiResponse<VoucherResponse> review(@PathVariable UUID id) { return ApiResponse.ok(service.review(id)); }

  @PostMapping("/vouchers/{id}/post")
  @PreAuthorize("hasAuthority('finance:voucher:post')")
  public ApiResponse<VoucherResponse> post(@PathVariable UUID id) { return ApiResponse.ok(service.postReviewed(id)); }

  @PostMapping("/vouchers/{id}/reverse")
  @PreAuthorize("hasAuthority('finance:voucher:reverse')")
  public ApiResponse<VoucherResponse> reverse(@PathVariable UUID id, @Valid @RequestBody ReverseVoucherRequest request) { return ApiResponse.ok(service.reverse(id, request)); }
}
