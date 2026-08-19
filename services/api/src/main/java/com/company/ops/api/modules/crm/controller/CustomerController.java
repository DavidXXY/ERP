package com.company.ops.api.modules.crm.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.common.api.PageResponse;
import com.company.ops.api.modules.crm.domain.CustomerLevel;
import com.company.ops.api.modules.crm.domain.RiskStatus;
import com.company.ops.api.modules.crm.dto.CreateCustomerRequest;
import com.company.ops.api.modules.crm.dto.CustomerDetailResponse;
import com.company.ops.api.modules.crm.dto.CustomerPoolStats;
import com.company.ops.api.modules.crm.dto.CustomerSummaryResponse;
import com.company.ops.api.modules.crm.dto.TransferCustomerOwnerRequest;
import com.company.ops.api.modules.crm.dto.UpdateCustomerRequest;
import com.company.ops.api.modules.crm.service.CustomerService;
import com.company.ops.api.modules.crm.service.CrmImportService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crm/customers")
public class CustomerController {

  private final CustomerService customerService;
  private final CrmImportService crmImportService;

  public CustomerController(CustomerService customerService, CrmImportService crmImportService) {
    this.customerService = customerService;
    this.crmImportService = crmImportService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('crm:customer:view')")
  public ApiResponse<List<CustomerSummaryResponse>> listCustomers() {
    return ApiResponse.ok(customerService.listCustomers());
  }

  @GetMapping("/page")
  @PreAuthorize("hasAuthority('crm:customer:view')")
  public ApiResponse<PageResponse<CustomerSummaryResponse>> listCustomersPage(
      @PageableDefault(size = 20) Pageable pageable,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) CustomerLevel level,
      @RequestParam(required = false) RiskStatus riskStatus,
      @RequestParam(required = false) String ownerNames) {
    return ApiResponse.ok(PageResponse.from(
        customerService.listCustomersPage(pageable, keyword, level, riskStatus, ownerNames)));
  }

  @GetMapping("/summary")
  @PreAuthorize("hasAuthority('crm:customer:view')")
  public ApiResponse<CustomerPoolStats> summary() {
    return ApiResponse.ok(customerService.summary());
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('crm:customer:view')")
  public ApiResponse<CustomerDetailResponse> getCustomer(@PathVariable UUID id) {
    return ApiResponse.ok(customerService.getCustomer(id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('crm:customer:create')")
  public ApiResponse<CustomerDetailResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
    return ApiResponse.ok(customerService.createCustomer(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('crm:customer:update')")
  public ApiResponse<CustomerDetailResponse> updateCustomer(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateCustomerRequest request
  ) {
    return ApiResponse.ok(customerService.updateCustomer(id, request));
  }

  @PostMapping("/{id}/transfer-owner")
  @PreAuthorize("hasAuthority('crm:customer:update')")
  public ApiResponse<Void> transferOwner(
      @PathVariable UUID id,
      @Valid @RequestBody TransferCustomerOwnerRequest request
  ) {
    customerService.transferOwner(id, request.ownerName());
    return ApiResponse.ok();
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('crm:customer:delete')")
  public ApiResponse<Void> deleteCustomer(@PathVariable UUID id) {
    customerService.deleteCustomer(id);
    return ApiResponse.ok();
  }
  @PostMapping("/import")
  @PreAuthorize("hasAuthority('crm:customer:create')")
  public ApiResponse<com.company.ops.api.modules.crm.service.CrmImportService.ImportResult> importCustomers(@org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
    return ApiResponse.ok(crmImportService.importCustomers(file));
  }
  @PostMapping("/batch-delete")
  @PreAuthorize("hasAuthority('crm:customer:delete')")
  public ApiResponse<Void> batchDelete(@RequestBody List<java.util.UUID> ids) {
    ids.forEach(id -> customerService.deleteCustomer(id));
    return ApiResponse.ok();
  }

}
