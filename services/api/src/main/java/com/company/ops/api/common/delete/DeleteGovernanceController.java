package com.company.ops.api.common.delete;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.common.delete.DeleteGovernanceService.DeletedRecordResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/deleted-records")
@PreAuthorize("hasAuthority('system:deleted-records:manage')")
public class DeleteGovernanceController {
  private final DeleteGovernanceService service;

  public DeleteGovernanceController(DeleteGovernanceService service) {
    this.service = service;
  }

  @GetMapping
  public ApiResponse<List<DeletedRecordResponse>> list() {
    return ApiResponse.ok(service.listActive());
  }

  @PostMapping("/{id}/approve")
  public ApiResponse<Void> approve(@PathVariable UUID id) {
    service.approve(id);
    return ApiResponse.ok();
  }

  @PostMapping("/{id}/restore")
  public ApiResponse<Void> restore(@PathVariable UUID id) {
    service.restore(id);
    return ApiResponse.ok();
  }
}
