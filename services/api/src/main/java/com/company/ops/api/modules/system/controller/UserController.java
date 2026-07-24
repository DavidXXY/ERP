package com.company.ops.api.modules.system.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.system.dto.CreateUserRequest;
import com.company.ops.api.modules.system.dto.ResetPasswordRequest;
import com.company.ops.api.modules.system.dto.UpdateUserRequest;
import com.company.ops.api.modules.system.dto.UserResponse;
import com.company.ops.api.modules.system.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/users", "/api/system/users"})
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('system:user:view')")
  public ApiResponse<Page<UserResponse>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Boolean enabled,
      @RequestParam(required = false) UUID roleId,
      @PageableDefault(size = 20) Pageable pageable) {
    return ApiResponse.ok(userService.listUsers(keyword, enabled, roleId, pageable));
  }

  @GetMapping("/options")
  @PreAuthorize("hasAnyAuthority('system:user:view', 'crm:customer:update', 'crm:opportunity:create', 'crm:opportunity:update', 'crm:followup:create', 'crm:quote:create', 'project:update', 'project:approve')")
  public ApiResponse<List<UserResponse>> options() {
    return ApiResponse.ok(userService.listEnabledUserOptions());
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('system:user:view')")
  public ApiResponse<UserResponse> get(@PathVariable UUID id) {
    return ApiResponse.ok(userService.getUser(id));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('system:user:create')")
  public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
    return ApiResponse.ok(userService.createUser(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('system:user:update')")
  public ApiResponse<UserResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
    return ApiResponse.ok(userService.updateUser(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('system:user:delete')")
  public ApiResponse<Void> delete(@PathVariable UUID id) {
    userService.deleteUser(id);
    return ApiResponse.ok();
  }

  @PostMapping("/{id}/reset-password")
  @PreAuthorize("hasAuthority('system:user:reset-password')")
  public ApiResponse<Void> resetPassword(@PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest request) {
    userService.resetPassword(id, request.newPassword());
    return ApiResponse.ok();
  }
}
