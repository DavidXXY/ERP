package com.company.ops.api.modules.crm.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.crm.dto.CreateCustomerRequest;
import com.company.ops.api.modules.crm.dto.CustomerDetailResponse;
import com.company.ops.api.modules.crm.dto.CustomerSummaryResponse;
import com.company.ops.api.modules.crm.dto.UpdateCustomerRequest;
import com.company.ops.api.modules.crm.service.CustomerService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crm/customers")
public class CustomerController {

  private final CustomerService customerService;

  public CustomerController(CustomerService customerService) {
    this.customerService = customerService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('crm:customer:view')")
  public ApiResponse<List<CustomerSummaryResponse>> listCustomers() {
    return ApiResponse.ok(customerService.listCustomers());
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
}
