package com.company.ops.api.modules.system.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.system.dto.LoginRequest;
import com.company.ops.api.modules.system.dto.LoginResponse;
import com.company.ops.api.modules.system.security.UserPrincipal;
import com.company.ops.api.modules.system.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return ApiResponse.ok(authService.login(request));
  }

  @GetMapping("/me")
  public ApiResponse<LoginResponse.CurrentUserResponse> currentUser(
      @AuthenticationPrincipal UserPrincipal principal
  ) {
    return ApiResponse.ok(authService.currentUser(principal));
  }
}

